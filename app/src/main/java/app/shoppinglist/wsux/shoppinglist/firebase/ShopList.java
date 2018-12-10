package app.shoppinglist.wsux.shoppinglist.firebase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
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

    private static final long TIME_IN_A_WEEK_IN_MILIES_FACTOR = 604800000;

    private UserInfo userInfo;
    private String listId;
    private boolean isMember;
    private String title;
    private String author;
    private List<String> tasks;
    private Map<String, ShopTask> shopTasks;
    private Map<String, Object> tokens;
    private List<String> collaborators;
    private Map<String, Collaborator> collaboratorsData;
    private DocumentReference ref;


    private boolean isReportedForShare;
    private boolean isReportedForReady;


    ShopList(FireBaseManager manager, UserInfo userInfo, String listId) {
        super(manager);

        this.userInfo = userInfo;
        this.listId = listId;
        this.isMember = false;
        this.shopTasks = new HashMap<>();
        this.collaboratorsData = new HashMap<>();

        this.isReportedForReady = false;
        this.isReportedForShare = false;

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
        Date dateInWeek = new Date();
        dateInWeek.setTime(System.currentTimeMillis() + TIME_IN_A_WEEK_IN_MILIES_FACTOR);
        tokens.put(token, new Timestamp(dateInWeek));
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
        if (!collaborators.contains(userId)) {
            return;
        }

        collaborators.remove(userId);
        updateField(ref, FIRESTORE_FIELD_COLLABORATORS, collaborators);
    }

    public void remove() {

    }

    public void setTitle(String newTitle) {

        if (newTitle == null || newTitle.equals(title)) {
            return;
        }

        title = newTitle;
        updateField(ref, FIRESTORE_FIELD_TITLE, newTitle);
    }

    public boolean isMember() {
        return isMember;
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
        fields.put(FIRESTORE_FIELD_TOKENS, new HashMap<>());
        return manager.getDb().collection(FIRESTORE_TABLE).add(fields);
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

        ArrayList<String> tasks = new ArrayList<>();
        if (document.contains(FIRESTORE_FIELD_TASKS)) {
            tasks.addAll((List<String>) document.get(FIRESTORE_FIELD_TASKS));
        }
        this.tasks = tasks;
        refreshShopTaskData();

        ArrayList collaborators = new ArrayList<>();
        if (document.contains(FIRESTORE_FIELD_COLLABORATORS)) {
            collaborators.addAll((List<String>) document.get(FIRESTORE_FIELD_COLLABORATORS));
        }
        this.collaborators = collaborators;
        refreshCollaboratorData();

        HashMap<String, Object> tokens = new HashMap<>();
        if (document.contains(FIRESTORE_FIELD_TOKENS)) {
            tokens.putAll((HashMap<String, Object>) document.get(FIRESTORE_FIELD_TOKENS));
        }
        this.tokens = tokens;

        isMember = userInfo.getUserId().equals(author) || collaborators.contains(userInfo.getUserId());
        setReady();

        reportChanges();
    }

    private void reportChanges() {
        if (!isReportedForShare && !isMember) {
            isReportedForShare = true;
            manager.reportEvent(FireBaseManager.ON_SHARE_LIST_FOUND, this);

        } else if (!isReportedForReady && isMember) {
            isReportedForReady = true;
            manager.reportEvent(FireBaseManager.ON_USER_LIST_UPDATED, this);

        } else {
            manager.reportEvent(FireBaseManager.ON_LIST_UPDATED, this);
        }

        userInfo.reportChildChange();
        if (onChangeListener != null) {
            onChangeListener.onChange();
        }
    }

    @Override
    protected void setReady() {

        if (!isReady() && listId.equals(userInfo.getLastList())) {
            manager.reportEvent(FireBaseManager.ON_LAST_LIST_DOWNLOADED, this);
        }

        super.setReady();
    }

    @Override
    public void onNotFound(DocumentSnapshot document) {
        super.onNotFound(document);
        isMember = false;
        userInfo.reportChildChange();
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
