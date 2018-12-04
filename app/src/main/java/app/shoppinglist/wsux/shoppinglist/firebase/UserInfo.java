package app.shoppinglist.wsux.shoppinglist.firebase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class UserInfo {
    private static final String TAG = "USER_INFO";
    public static final String FIRESTORE_TABLE = "users";
    private static final String FIRESTORE_FIELD_LISTS = "lists";
    private static final String FIRESTORE_FIELD_TOKENS = "tokens";

    private FireBaseManager manager;
    private String userId;
    private String email;
    private String displayName;
    private List<String> listNames;
    private HashMap<String, ShopList> lists;
    private HashMap<String, String> tokens;

    UserInfo(FireBaseManager manager, FirebaseUser user) {
        this.manager = manager;
        userId = user.getUid();
        email = user.getEmail();
        displayName = user.getDisplayName();

        listNames = new ArrayList<>();
        lists = new HashMap<>();
        tokens = new HashMap<>();
        this.getData();
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

    public String getToken(String listId) { return tokens.get(listId); }

    public HashMap<String, ShopList> getLists() {
        HashMap<String, ShopList> readyLists = new HashMap<>();
        for (HashMap.Entry<String, ShopList> entry: lists.entrySet()) {
            if (entry.getValue().isReady()) {
                readyLists.put(entry.getKey(), entry.getValue());
            }
        }
        return readyLists;
    }

    public void getData() {
        manager.getDb().collection(FIRESTORE_TABLE).document(userId)
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {

                    DocumentSnapshot document = task.getResult();

                    if (document == null) {
                        return;
                    }

                    if (document.exists()) {

                        listNames = new ArrayList<>();
                        if (document.contains(FIRESTORE_FIELD_LISTS)) {
                            listNames.addAll((List<String>) document.get(FIRESTORE_FIELD_LISTS));

                            for (String listId : listNames) {
                                if (!lists.containsKey(listId)) {
                                    lists.put(listId, new ShopList(manager, UserInfo.this, listId));
                                }
                            }
                        }

                        if (document.contains(FIRESTORE_FIELD_TOKENS)) {
                            tokens.putAll((Map<String, String>) document.get(FIRESTORE_FIELD_TOKENS));
                        }

                        // in case the list is empty..
                        manager.reportEvent(FireBaseManager.ON_USER_LIST_UPDATED);

                    } else {
                        initInfoInDB();
                    }
                }
            }
        });
    }

    public void addOwnedList(String listId) {
        listNames.add(listId);
        updateInDB(FIRESTORE_FIELD_LISTS, listNames);
        lists.put(listId, new ShopList(manager, this, listId));
    }

    public void removeOwnedList(String listId) {
        listNames.remove(listId);
        updateInDB(FIRESTORE_FIELD_LISTS, listNames);
        manager.reportEvent(FireBaseManager.ON_LIST_DELETED, lists.get(listId));
        lists.remove(listId);
    }

    public void addToken(final String listId, String token) {

        if (token.contains(listId)) {
            return;
        }

        tokens.put(listId, token);
        updateInDB(FIRESTORE_FIELD_TOKENS, tokens).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                ShopList.checkListExists(manager, listId, FireBaseManager.ON_SHARE_LIST_FOUND);
            }
        });
    }

    public void removeToken(String listId) {
        tokens.remove(listId);
        updateInDB(FIRESTORE_FIELD_TOKENS, listId);
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
                                        addOwnedList(documentReference.getId());
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
        manager.getDb().collection(FIRESTORE_TABLE).document(userId).set(params);
        manager.reportEvent(FireBaseManager.ON_USER_LIST_UPDATED);
    }

    private Task<Void> updateInDB(String field, Object object) {
        return manager.getDb().collection(FIRESTORE_TABLE).document(userId).update(field, object);
    }
}
