package app.shoppinglist.wsux.shoppinglist.firebase;

import android.graphics.Bitmap;
import android.util.Pair;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.shoppinglist.wsux.shoppinglist.firebase.db.ShopListActions;
import app.shoppinglist.wsux.shoppinglist.firebase.db.TransactionWrapper;
import app.shoppinglist.wsux.shoppinglist.firebase.db.UserInfoActions;

public class UserInfo extends BaseCollectionItem {
    private static final String TAG = "USER_INFO";

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

        ref = UserInfoActions.getRef(manager.getDb(), userId);
        ref.addSnapshotListener(this);
    }

    public DocumentReference getRef(){return ref;}
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

        if (document.contains(UserInfoActions.FIRESTORE_FIELD_LAST_LIST)) {
            lastList = document.getString(UserInfoActions.FIRESTORE_FIELD_LAST_LIST);
        }

        loadListNamesFromDB(document);

        if (document.contains(UserInfoActions.FIRESTORE_FIELD_TOKENS)) {
            tokens.putAll((Map<String, String>) document.get(UserInfoActions.FIRESTORE_FIELD_TOKENS));
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
        if (document.contains(UserInfoActions.FIRESTORE_FIELD_LISTS)) {
            listNames.addAll((List<String>) document.get(UserInfoActions.FIRESTORE_FIELD_LISTS));

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
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        UserInfoActions.addKnownList(transaction, ref, listId).apply();
        reportChildChange();
    }

    public void removeKnownList(String listId) {
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        UserInfoActions.removeKnownList(transaction, ref, listId).apply();
        manager.reportEvent(FireBaseManager.ON_LIST_DELETED, lists.get(listId));
        lists.remove(listId);
    }

    public void setLastList(String lastList) {

        if (lastList == null || lastList.equals(this.lastList)) {
            return;
        }

        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        UserInfoActions.setLastList(transaction, ref, lastList).apply();
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

        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        UserInfoActions.addToken(transaction, ref, listId, token).apply();
        addKnownList(listId);
    }

    public void removeToken(String listId) {

        if (!tokens.containsKey(listId)) {
            return;
        }

        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        UserInfoActions.removeToken(transaction, ref, listId).apply();
    }

    public void createNewList(String listTitle) {

        final DocumentReference listRef = ShopListActions.getNewRef(manager.getDb());

        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), new TransactionWrapper.ResultListener() {
            @Override
            public void onSuccess() {
                createColloberatorDataByRef(listRef);
                manager.reportEvent(FireBaseManager.ON_LIST_CREATED);
            }

            @Override
            public void onFailure(Exception e) {
                UserInfo.this.onFailure(e);
            }
        });

        ShopListActions.createNewList(transaction, listRef, userId, listTitle);
        UserInfoActions.addKnownList(transaction, ref, listRef.getId());
        UserInfoActions.setLastList(transaction, ref, listRef.getId());
        transaction.apply();

    }

    private void createColloberatorDataByRef(DocumentReference listRef) {
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), UserInfo.this);
        ShopListActions.addCollaboratorData(
                transaction, ShopListActions.getRef(manager.getDb(), listRef.getId()), getUserId(), getDisplayName(), getEmail(), getPictureURL());
        transaction.apply();
    }

    private void initInfoInDB() {
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        UserInfoActions.initUser(transaction, ref).apply();
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
