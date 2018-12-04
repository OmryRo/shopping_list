package app.shoppinglist.wsux.shoppinglist.firebase;

import android.support.annotation.NonNull;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.lang.reflect.Array;
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
    private Map<String, Collaborator> collaboratorsData;
    private DocumentReference ref;


    ShopList(FireBaseManager manager, UserInfo userInfo, String listId) {
        super(manager);

        this.userInfo = userInfo;
        this.listId = listId;
        this.isReady = false;
        this.shopTasks = new HashMap<>();
        this.collaboratorsData = new HashMap<>();

        this.ref = manager.getDb().collection(FIRESTORE_TABLE).document(listId);
        this.ref.addSnapshotListener(this);
    }

    public void addNewTask(String title, String description) {
        ShopTask.addNewTask(this, title, description, userInfo)
                .addOnSuccessListener(new OnSuccessListener<DocumentReference>() {
                    @Override
                    public void onSuccess(DocumentReference documentReference) {
                        tasks.add(documentReference.getId());
                        updateField(ref, FIRESTORE_FIELD_TASKS, tasks);
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

    Task<Void> addToken(String token) {
        tokens.add(token);
        return updateField(ref, FIRESTORE_FIELD_TOKENS, tokens);
    }

    public void replaceTokenByCollaborators(final String token) {
        collaborators.add(userInfo.getUserId());
        ref.update(FIRESTORE_FIELD_COLLABORATORS, collaborators)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Collaborator.addNewCollaborator(ref, userInfo).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                tokens.remove(token);
                                updateField(ref, FIRESTORE_FIELD_TOKENS, tokens).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        userInfo.removeToken(listId);
                                    }
                                }).addOnFailureListener(ShopList.this);
                            }
                        }).addOnFailureListener(ShopList.this);
                    }
                })
                .addOnFailureListener(this);
    }

    public void removeCollaborators(String userId) {
        collaborators.remove(userId);
        updateField(ref, FIRESTORE_FIELD_COLLABORATORS, collaborators);
    }

    public void remove() {

    }

    public void setTitle(String newTitle) {
        title = newTitle;
        updateField(ref, FIRESTORE_FIELD_TITLE, newTitle);
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
            if (entry.getValue().isReady()) {
                readyTasks.put(entry.getKey(), entry.getValue());
            }
        }

        return readyTasks;
    }

    public HashMap<String, Collaborator> getCollaborators() {
        HashMap<String, Collaborator> readyCollaborators = new HashMap<>();
        for (HashMap.Entry<String, Collaborator> entry : collaboratorsData.entrySet()) {
            if (entry.getValue().isReady()) {
                readyCollaborators.put(entry.getKey(), entry.getValue());
            }
        }

        return readyCollaborators;
    }

    static Task<DocumentReference> createNewList(FireBaseManager manager, UserInfo userInfo, String title) {
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(FIRESTORE_FIELD_AUTHOR, userInfo.getUserId());
        fields.put(FIRESTORE_FIELD_TITLE, title);
        fields.put(FIRESTORE_FIELD_COLLABORATORS, new ArrayList<>());
        fields.put(FIRESTORE_FIELD_TOKENS, new ArrayList<>());
        return manager.getDb().collection(FIRESTORE_TABLE).add(fields);
    }

    static void checkListExists(final FireBaseManager manager, String listId, final int reportMessage) {
        manager.getDb()
                .collection(FIRESTORE_TABLE)
                .document(listId)
                .get()
                .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot document) {
                        if (document.exists()) {
                            manager.reportEvent(reportMessage,
                                    new String[] {
                                            document.getId(),
                                            document.getString(FIRESTORE_FIELD_TITLE),
                                    });
                        }
                    }
                });
    }

    DocumentReference getRef() {
        return ref;
    }

    private void refreshShopTaskData() {
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

    private void refreshCollaboratorData() {
        if (!collaboratorsData.containsKey(author)) {
            collaboratorsData.put(author, new Collaborator(manager, this, author));
        }

        for (String collaboratorId : collaborators) {
            if (!collaboratorsData.containsKey(collaboratorId)) {
                collaboratorsData.put(collaboratorId, new Collaborator(manager, this, collaboratorId));
            }
        }
    }

    @Override
    void specificOnEvent(DocumentSnapshot document) {

        title = document.getString(FIRESTORE_FIELD_TITLE);
        author = document.getString(FIRESTORE_FIELD_AUTHOR);

        tasks = new ArrayList<>();
        if (document.contains(FIRESTORE_FIELD_TASKS)) {
            tasks.addAll((List<String>) document.get(FIRESTORE_FIELD_TASKS));
            refreshShopTaskData();
        }

        collaborators = new ArrayList<>();
        if (document.contains(FIRESTORE_FIELD_COLLABORATORS)) {
            collaborators.addAll((List<String>) document.get(FIRESTORE_FIELD_COLLABORATORS));
            refreshCollaboratorData();
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
