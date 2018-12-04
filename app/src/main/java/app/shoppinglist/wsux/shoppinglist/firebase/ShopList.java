package app.shoppinglist.wsux.shoppinglist.firebase;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ShopList extends BaseCollectionItem {

    private final static String TAG = "SHOP_LIST";
    public static final String FIRESTORE_TABLE = "lists";
    private static final String FIRESTORE_FIELD_AUTHOR = "author";
    private static final String FIRESTORE_FIELD_TITLE = "title";
    private static final String FIRESTORE_FIELD_TASKS = "tasks";
    private static final String FIRESTORE_FIELD_TOKENS = "tokens";
    private static final String FIRESTORE_FIELD_COLLABORATORS = "collaborators";

    private UserInfo userInfo;
    private String listId;
    private boolean isReady;
    private String title;
    private String author;
    private List<String> tasks;
    private Map<String, ShopTask> shopTasks;
    private List<String> tokens;
    private List<String> collaborators;
    private DocumentReference ref;


    ShopList(FireBaseManager manager, UserInfo userInfo, String listId) {
        super(manager);

        this.userInfo = userInfo;
        this.listId = listId;
        this.isReady = false;
        this.shopTasks = new HashMap<>();

        this.ref = manager.getDb().collection(FIRESTORE_TABLE).document(listId);
        this.ref.addSnapshotListener(this);
    }

    public void addNewTask(String title, String description) {
        ShopTask.addNewTask(this, title, description, userInfo)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        tasks.add(documentReference.getId());

                        HashMap<String, Object> params = new HashMap<>();
                        params.put(FIRESTORE_FIELD_TASKS, tasks);

                        ref.update(params)
                                .addOnSuccessListener(ShopList.this)
                                .addOnFailureListener(ShopList.this);
                        manager.reportEvent(FireBaseManager.ON_TASK_CREATED, ShopList.this);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        manager.reportEvent(FireBaseManager.ON_TASK_FAILURE, e);
                    }
                });
    }

    private Task<Void> addToken(String token) {
        tokens.add(token);
        return ref.update(FIRESTORE_FIELD_TOKENS, tokens);
    }

    public void replaceTokenByCollaborators(final String token) {
        collaborators.add(userInfo.getUserId());
        ref.update(FIRESTORE_FIELD_COLLABORATORS, collaborators)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        tokens.remove(token);
                        ref.update(FIRESTORE_FIELD_TOKENS, tokens)
                                .addOnSuccessListener(ShopList.this)
                                .addOnFailureListener(ShopList.this);
                    }
                })
                .addOnFailureListener(this);
    }

    public void removeCollaborators(String userId) {
        collaborators.remove(userId);
        ref.update(FIRESTORE_FIELD_COLLABORATORS, collaborators)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    public void remove() {

    }

    public void setTitle(String newTitle) {
        title = newTitle;
        ref.update(FIRESTORE_FIELD_TITLE, newTitle)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    public boolean isReady() {
        return isReady;
    }

    public String getListId() {
        return listId;
    }

    public String getTitle() {
        return title;
    }

    public HashMap<String, ShopTask> getTasks() {
        HashMap<String, ShopTask> readyTasks = new HashMap<>();
        for (HashMap.Entry<String, ShopTask> entry : shopTasks.entrySet()) {
            readyTasks.put(entry.getKey(), entry.getValue());
        }

        return readyTasks;
    }

    static Task<DocumentReference> createNewList(FireBaseManager manager, UserInfo userInfo, String title) {
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(FIRESTORE_FIELD_AUTHOR, userInfo.getUserId());
        fields.put(FIRESTORE_FIELD_TITLE, title);
        fields.put(FIRESTORE_FIELD_COLLABORATORS, new ArrayList<>());
        fields.put(FIRESTORE_FIELD_TOKENS, new ArrayList<>());
        return manager.getDb().collection(FIRESTORE_TABLE).add(fields);
    }

    DocumentReference getRef() {
        return ref;
    }

    @Override
    void specificOnEvent(DocumentSnapshot document) {

        title = document.getString(FIRESTORE_FIELD_TITLE);
        author = document.getString(FIRESTORE_FIELD_AUTHOR);

        tasks = new ArrayList<>();
        if (document.contains(FIRESTORE_FIELD_TASKS)) {
            tasks.addAll((List<String>) document.get(FIRESTORE_FIELD_TASKS));

            for (String taskId : tasks) {
                if (!shopTasks.containsKey(taskId)) {
                    shopTasks.put(taskId, new ShopTask(manager, this, taskId));
                }
            }

            List<String> tasksToRemove = new ArrayList<>();
            for (String taskId : shopTasks.keySet()) {
                if (!tasks.contains(taskId)) {
                    tasksToRemove.add(taskId);
                }
            }

            for (String taskId : tasksToRemove) {
                manager.reportEvent(FireBaseManager.ON_TASK_DELETED, shopTasks.get(taskId));
                shopTasks.remove(taskId);
            }

        }

        collaborators = new ArrayList<>();
        if (document.contains(FIRESTORE_FIELD_COLLABORATORS)) {
            collaborators.addAll((List<String>) document.get(FIRESTORE_FIELD_COLLABORATORS));
        }

        tokens = new ArrayList<>();
        if (document.contains(FIRESTORE_FIELD_TOKENS)) {
            tokens.addAll((List<String>) document.get(FIRESTORE_FIELD_TOKENS));
        }

        if (!isReady) {
            isReady = true;
            manager.reportEvent(FireBaseManager.ON_USER_LIST_UPDATED, this);
        }

        manager.reportEvent(FireBaseManager.ON_LIST_UPDATED, this);

        if (onChangeListener != null) {
            onChangeListener.onChange();
        }
    }

    @Override
    void specificOnSuccess() {
        manager.reportEvent(FireBaseManager.ON_LIST_UPDATED, this);
    }

    @Override
    void specificOnFailure(Exception e) {
        manager.reportEvent(FireBaseManager.ON_LIST_FAILURE, this, e);
    }
}
