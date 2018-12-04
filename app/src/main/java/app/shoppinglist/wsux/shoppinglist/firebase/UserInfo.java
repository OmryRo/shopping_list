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

public class UserInfo {
    private static final String TAG = "USER_INFO";
    public static final String FIRESTORE_TABLE = "users";
    private static final String FIRESTORE_FIELD_LISTS = "lists";
    private static final String FIRESTORE_FIELD_TOKENS = "tokens";

    private FireBaseManager manager;
    private String userId;
    private String email;
    private String displayName;
    private HashSet<String> listNames;
    private HashMap<String, ShopList> lists;
    private HashSet<String> tokens;

    UserInfo(FireBaseManager manager, FirebaseUser user) {
        this.manager = manager;
        userId = user.getUid();
        email = user.getEmail();
        displayName = user.getDisplayName();

        listNames = new HashSet<>();
        lists = new HashMap<>();
        tokens = new HashSet<>();
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

                        if (document.contains(FIRESTORE_FIELD_LISTS)) {
                            listNames.addAll((List<String>) document.get(FIRESTORE_FIELD_LISTS));
                            for (String listId : listNames) {
                                lists.put(listId, new ShopList(manager, UserInfo.this, listId));
                            }
                        }

                        if (document.contains(FIRESTORE_FIELD_TOKENS)) {
                            tokens.addAll((List<String>) document.get(FIRESTORE_FIELD_TOKENS));
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
        updateInDB();
        lists.put(listId, new ShopList(manager, this, listId));
    }

    public void removeOwnedList(String listId) {
        listNames.remove(listId);
        updateInDB();
        manager.reportEvent(FireBaseManager.ON_LIST_DELETED, lists.get(listId));
        lists.remove(listId);
    }

    public void addToken(final String listId, String token) {
        tokens.add(token);
        updateInDB().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                addOwnedList(listId);
            }
        });
    }

    public void removeToken(String token) {
        tokens.remove(token);
        updateInDB();
    }

    public void createNewList(String listTitle) {
        ShopList.createNewList(manager, this, listTitle)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
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

    private void initInfoInDB() {
        updateInDB();
        manager.reportEvent(FireBaseManager.ON_USER_LIST_UPDATED);
    }

    private Task<Void> updateInDB() {
        HashMap<String, Object> map = new HashMap<>();
        map.put(FIRESTORE_FIELD_LISTS, new ArrayList<>(listNames));
        map.put(FIRESTORE_FIELD_TOKENS, new ArrayList<>(tokens));

        return manager.getDb().collection(FIRESTORE_TABLE).document(userId).set(map);
    }
}
