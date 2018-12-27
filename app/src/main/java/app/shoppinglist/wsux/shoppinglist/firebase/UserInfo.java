package app.shoppinglist.wsux.shoppinglist.firebase;

import android.graphics.Bitmap;
import android.support.annotation.NonNull;
import android.util.Pair;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UserInfo extends BaseCollectionItem {
    private static final String TAG = "USER_INFO";
    public static final String FIRESTORE_TABLE = "users";
    private static final String FIRESTORE_FIELD_LISTS = "lists";
    private static final String FIRESTORE_FIELD_TOKENS = "tokens";
    private static final String FIRESTORE_FIELD_LAST_LIST = "last_list";

    private String userId;
    private String email;
    private String displayName;
    private String pictureURL;
    private String lastList;
    private boolean hasStartedPictureDownload;
    private List<String> listNames;
    private HashMap<String, ShopList> lists;
    private HashMap<String, String> tokens;
    private Pair<String, String> waitingToken;
    private DocumentReference ref;

    UserInfo(FireBaseManager manager, FirebaseUser user) {
        super(manager);
        userId = user.getUid();
        email = user.getEmail();
        displayName = user.getDisplayName();
        pictureURL = user.getPhotoUrl().toString();

        hasStartedPictureDownload = false;
        listNames = new ArrayList<>();
        lists = new HashMap<>();
        tokens = new HashMap<>();

        ref = manager.getDb().collection(FIRESTORE_TABLE).document(userId);
        ref.addSnapshotListener(this);
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPictureURL() {
        return pictureURL;
    }

    public String getToken(String listId) {
        return tokens.get(listId);
    }

    public Bitmap getPicture() {
        return manager.getImageManager().getPicture(this);
    }

    public HashMap<String, ShopList> getLists() {
        HashMap<String, ShopList> readyLists = new HashMap<>();
        for (HashMap.Entry<String, ShopList> entry : lists.entrySet()) {
            if (entry.getValue().isReady() && entry.getValue().isMember()) {
                readyLists.put(entry.getKey(), entry.getValue());
            }
        }
        return readyLists;
    }

    @Override
    public void onNotFound(DocumentSnapshot document) {
        super.onNotFound(document);
        initInfoInDB();
    }

    @Override
    void specificOnEvent(DocumentSnapshot document) {

        if (document.contains(FIRESTORE_FIELD_LAST_LIST)) {
            lastList = document.getString(FIRESTORE_FIELD_LAST_LIST);
        }

        loadListNamesFromDB(document);

        if (document.contains(FIRESTORE_FIELD_TOKENS)) {
            tokens.putAll((Map<String, String>) document.get(FIRESTORE_FIELD_TOKENS));
        }

        setReady();

        if (!hasStartedPictureDownload && pictureURL != null) {
            hasStartedPictureDownload = true;
            manager.getImageManager().downloadPicture(this, pictureURL);
        }

        // in case the list is empty..
        reportChildChange();
    }

    private void loadListNamesFromDB(DocumentSnapshot document) {
        ArrayList<String> listNames = new ArrayList<>();
        if (document.contains(FIRESTORE_FIELD_LISTS)) {
            listNames.addAll((List<String>) document.get(FIRESTORE_FIELD_LISTS));

            for (String listId : listNames) {
                if (!lists.containsKey(listId)) {
                    lists.put(listId, new ShopList(manager, UserInfo.this, listId));
                }
            }
        }
        this.listNames = listNames;
    }

    protected void setReady() {
        super.setReady();

        if (waitingToken != null) {
            addToken(waitingToken.first, waitingToken.second);
            waitingToken = null;
        }
    }

    @Override
    void reportChildChange() {
        manager.reportEvent(FireBaseManager.ON_USER_LIST_UPDATED);
        super.reportChildChange();
    }

    public void addKnownList(String listId) {
        setLastList(listId);
        appendToList(ref, FIRESTORE_FIELD_LISTS, listId);
        reportChildChange();
    }

    public void removeKnownList(String listId) {
        removeFromList(ref, FIRESTORE_FIELD_LISTS, listId);
        manager.reportEvent(FireBaseManager.ON_LIST_DELETED, lists.get(listId));
        lists.remove(listId);
    }

    public void setLastList(String lastList) {

        if (lastList == null || lastList.equals(this.lastList)) {
            return;
        }

        updateField(ref, FIRESTORE_FIELD_LAST_LIST, lastList);
    }

    public String getLastList() {
        return lastList;
    }

    public void addToken(final String listId, String token) {

        if (token.contains(listId) || (lists.containsKey(listId) && lists.get(listId).isMember())) {
            return;
        }

        if (!isReady()) {
            waitingToken = new Pair<>(listId, token);
            return;
        }

        addToMap(ref, FIRESTORE_FIELD_TOKENS, listId, token).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                addKnownList(listId);
            }
        });
    }

    public void removeToken(String listId) {

        if (!tokens.containsKey(listId)) {
            return;
        }

        removeFromMap(ref, FIRESTORE_FIELD_TOKENS, listId);
    }

    public void createNewList(String listTitle) {
        ShopList.createNewList(manager, this, listTitle)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(final DocumentReference documentReference) {
                        Collaborator.addNewCollaborator(documentReference, UserInfo.this)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        addKnownList(documentReference.getId());
                                        manager.reportEvent(FireBaseManager.ON_LIST_CREATED);
                                    }
                                })
                                .addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {
                                        manager.reportEvent(FireBaseManager.ON_LIST_FAILURE, e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        manager.reportEvent(FireBaseManager.ON_LIST_FAILURE, e);
                    }
                });
    }

    private void initInfoInDB() {
        listNames = new ArrayList<>();
        tokens = new HashMap<>();

        Map<String, Object> params = new HashMap<>();
        params.put(FIRESTORE_FIELD_LISTS, listNames);
        params.put(FIRESTORE_FIELD_TOKENS, tokens);
        ref.set(params).addOnSuccessListener(this).addOnFailureListener(this);
    }

    @Override
    void specificOnSuccess() {
        manager.reportEvent(FireBaseManager.ON_USER_INFO_UPDATED, this);
    }

    @Override
    void specificOnFailure(Exception e) {
        manager.reportEvent(FireBaseManager.ON_USER_UPDATE_FAILED, e);
    }

    @Override
    public String toString() {
        return String.format("UserInfo: %s", userId);
    }
}
