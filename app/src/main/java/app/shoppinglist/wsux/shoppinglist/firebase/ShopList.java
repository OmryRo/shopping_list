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
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.shoppinglist.wsux.shoppinglist.firebase.db.ShopListActions;
import app.shoppinglist.wsux.shoppinglist.firebase.db.ShopTaskActions;
import app.shoppinglist.wsux.shoppinglist.firebase.db.TransactionWrapper;

public class ShopList extends BaseCollectionItem {

    private final static String TAG = "SHOP_LIST";

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

        this.ref = ShopListActions.getRef(manager.getDb(), listId);
        this.ref.addSnapshotListener(this);
    }

    public void addNewTask(String title, String description) {

        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), new TransactionWrapper.ResultListener() {
            @Override
            public void onSuccess() {
                manager.reportEvent(FireBaseManager.ON_TASK_CREATED, ShopList.this);
            }

            @Override
            public void onFailure(Exception e) {
                manager.reportEvent(FireBaseManager.ON_TASK_FAILURE, e);
            }
        });

        ShopTaskActions.newTask(transaction, ref, userInfo.getUserId(), title, description);
        transaction.apply();
    }

    Task<Void> addToken(String token) {
        Date dateInWeek = new Date();
        dateInWeek.setTime(System.currentTimeMillis() + TIME_IN_A_WEEK_IN_MILIES_FACTOR);
        return addToMap(ref, FIRESTORE_FIELD_TOKENS, token, new Timestamp(dateInWeek));
    }

    public void replaceTokenByCollaborators(final String token) {

        appendToList(ref, FIRESTORE_FIELD_COLLABORATORS, userInfo.getUserId())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Collaborator.addNewCollaborator(ref, userInfo).addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                removeFromMap(ref, FIRESTORE_FIELD_TOKENS, token).addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void aVoid) {
                                        userInfo.removeToken(listId);
                                    }
                                });
                            }
                        });
                    }
                });
    }

    public void removeCollaborators(String userId) {
        removeCollaborators(userId, false);
    }

    public void removeCollaborators(String userId, boolean removeData) {
        if (!collaborators.contains(userId)) {
            return;
        }

        removeFromList(ref, FIRESTORE_FIELD_COLLABORATORS, userId);
        removeCollaboratorData(userId);
    }

    /**
     * this function doesn't remove the task from the collection!
     * call shopTask.remove() instead.
     */
    void removeTaskFromList(final ShopTask shopTask) {
        removeFromList(ref, FIRESTORE_FIELD_TASKS, shopTask.getTaskId());
        shopTasks.remove(shopTask.getTaskId());

        if (onChildChangeListener != null) {
            onChildChangeListener.onChildChange();
        }
    }

    public boolean remove() {
        return removeListAsAuthor() || quitListAsAuthor() || quitListAsCollaborator();
    }

    private boolean removeListAsAuthor() {
        if (!userInfo.getUserId().equals(author) || collaborators.size() != 0) {
            return false;
        }

        manager.reportEvent(FireBaseManager.ON_PROGRESS_START_DELETE);

        removeAllTasks();
        removeCollaboratorData(userInfo.getUserId());

        ref.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                setNotReady();
                removeAllListeners();
                userInfo.removeKnownList(listId);
                userInfo.reportChildChange();
            }
        }).addOnFailureListener(this);

        return true;
    }

    private boolean quitListAsAuthor() {
        if (!userInfo.getUserId().equals(author) || collaborators.size() == 0) {
            return false;
        }

        manager.reportEvent(FireBaseManager.ON_PROGRESS_START_QUIT);

        String firstCollaboratorFound = collaborators.get(0);
        removeCollaborators(firstCollaboratorFound);
        removeCollaboratorData(userInfo.getUserId());

        if (author.equals(firstCollaboratorFound)) {
            return remove();
        }

        setAuthor(author);
        removeAllListeners();
        userInfo.removeKnownList(listId);

        return true;
    }

    private boolean quitListAsCollaborator() {
        if (!collaborators.contains(userInfo.getUserId())) {
            return false;
        }

        manager.reportEvent(FireBaseManager.ON_PROGRESS_START_QUIT);

        removeCollaboratorData(userInfo.getUserId());
        removeCollaborators(userInfo.getUserId());

        removeAllListeners();
        userInfo.removeKnownList(listId);

        return true;
    }

    private void removeAllTasks() {
        for (ShopTask task : shopTasks.values()) {
            task.remove();
        }
    }

    private void removeCollaboratorData(String userId) {
        Collaborator collaborator = collaboratorsData.get(userInfo.getUserId());
        if (collaborator != null) {
            collaborator.remove();
        }
    }

    public void setTitle(String newTitle) {

        if (newTitle == null || newTitle.equals(title)) {
            return;
        }

        updateField(ref, FIRESTORE_FIELD_TITLE, newTitle);
    }

    public String getAuthor() {
        return author;
    }

    public boolean isAuthor() {
        return userInfo.getUserId().equals(author);
    }

    private void setAuthor(String author) {

        if (author == null || author.equals(this.author)) {
            return;
        }

        updateField(ref, FIRESTORE_FIELD_AUTHOR, author);
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
        manager.reportEvent(FireBaseManager.ON_PROGRESS_START_CREATE);
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

        title = document.getString(ShopListActions.FIRESTORE_FIELD_TITLE);
        author = document.getString(ShopListActions.FIRESTORE_FIELD_AUTHOR);

        loadTasksFromDB(document);
        refreshShopTaskData();

        loadCollaboratorsFromDB(document);
        refreshCollaboratorData();

        loadTokensFromDB(document);

        isMember = userInfo.getUserId().equals(author) || collaborators.contains(userInfo.getUserId());
        setReady();

        reportChanges();
    }

    private void loadTokensFromDB(DocumentSnapshot document) {
        HashMap<String, Object> tokens = new HashMap<>();
        if (document.contains(ShopListActions.FIRESTORE_FIELD_TOKENS)) {
            tokens.putAll((HashMap<String, Object>) document.get(ShopListActions.FIRESTORE_FIELD_TOKENS));
        }
        this.tokens = tokens;
    }

    private void loadCollaboratorsFromDB(DocumentSnapshot document) {
        List<String> collaborators = new ArrayList<>();
        if (document.contains(ShopListActions.FIRESTORE_FIELD_COLLABORATORS)) {
            collaborators.addAll((List<String>) document.get(ShopListActions.FIRESTORE_FIELD_COLLABORATORS));
        }
        this.collaborators = collaborators;
    }

    private void loadTasksFromDB(DocumentSnapshot document) {
        ArrayList<String> tasks = new ArrayList<>();
        if (document.contains(ShopListActions.FIRESTORE_FIELD_TASKS)) {
            tasks.addAll((List<String>) document.get(ShopListActions.FIRESTORE_FIELD_TASKS));
        }
        this.tasks = tasks;
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
        userInfo.removeKnownList(listId);
    }


    public void onQueryError(DocumentSnapshot document, FirebaseFirestoreException e) {
        super.onQueryError(document, e);

        if (e.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
            manager.reportEvent(FireBaseManager.ON_LIST_REMOVED_FROM, this, e);
            userInfo.removeKnownList(listId);
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

    @Override
    public String toString() {
        return String.format("ShopList: %s", listId);
    }

}
