package app.shoppinglist.wsux.shoppinglist.firebase;

import android.util.Log;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import app.shoppinglist.wsux.shoppinglist.firebase.db.CollaboratorActions;
import app.shoppinglist.wsux.shoppinglist.firebase.db.ShopListActions;
import app.shoppinglist.wsux.shoppinglist.firebase.db.TransactionWrapper;
import app.shoppinglist.wsux.shoppinglist.firebase.db.UserInfoActions;

public class ShopList extends BaseCollectionItem {

    private final static String TAG = "SHOP_LIST";

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

        manager.reportEvent(FireBaseManager.ON_PROGRESS_START_CREATE);

        TransactionWrapper transaction = new TransactionWrapper(
                manager.getDb(), new TransactionWrapper.ResultListener() {
            @Override
            public void onSuccess() {
                manager.reportEvent(FireBaseManager.ON_TASK_CREATED, ShopList.this);
            }

            @Override
            public void onFailure(Exception e) {
                manager.reportEvent(FireBaseManager.ON_TASK_FAILURE, e);
            }
        });

        ShopListActions.addTask(transaction, ref, userInfo.getUserId(), title, description);
        transaction.apply();
    }

    void addToken(String token) {
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        ShopListActions.addToken(transaction, ref, token).apply();
    }

    public void replaceTokenByCollaborators(final String token) {
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        ShopListActions.addCollaborator(transaction, ref, userInfo.getUserId());
        ShopListActions.addCollaboratorData(
                transaction,
                ref,
                userInfo.getUserId(),
                userInfo.getDisplayName(),
                userInfo.getEmail(),
                userInfo.getPictureURL()
        );
        ShopListActions.removeToken(transaction, ref, token);
        UserInfoActions.removeToken(transaction, userInfo.getRef(), listId).apply();
    }

    public void removeCollaborators(String userId) {
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        removeCollaborators(transaction, userId, false);
        transaction.apply();
    }

    public void removeCollaborators(TransactionWrapper transaction, String userId, boolean removeData) {
        if (!collaborators.contains(userId)) {
            return;
        }

        ShopListActions.removeCollaborators(transaction, ref, userId);

        if (removeData) {
            CollaboratorActions.remove(transaction, CollaboratorActions.getRef(ref, userId));
        }

    }

    void removeTaskFromList(final ShopTask shopTask) {

        TransactionWrapper transaction = new TransactionWrapper(
                manager.getDb(), new TransactionWrapper.ResultListener() {
            @Override
            public void onSuccess() {
                if (onChildChangeListener != null) {
                    onChildChangeListener.onChildChange();
                }

//                this.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                onFailure(e);
            }
        });

        ShopListActions.removeTask(transaction, ref, shopTask.getRef()).apply();
    }

    public boolean remove() {
        return removeListAsAuthor() || quitListAsAuthor() || quitListAsCollaborator();
    }

    private boolean removeListAsAuthor() {
        if (!userInfo.getUserId().equals(author) || collaborators.size() != 0) {
            return false;
        }

        manager.reportEvent(FireBaseManager.ON_PROGRESS_START_DELETE);
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        removeAllTasks(transaction);
        removeCollaboratorData(transaction, userInfo.getUserId());
//        setNotReady();
//        removeAllListeners();
//        ref.delete();
        ShopListActions.remove(transaction, ref);
        Log.d(TAG, "removeListAsAuthor: "+ author);
//        UserInfoActions.removeKnownList(transaction, ref, listId);
        transaction.apply();
        userInfo.reportChildChange();

        return true;
    }

    private boolean quitListAsAuthor() {
        if (!userInfo.getUserId().equals(author) || collaborators.size() == 0) {
            return false;
        }

        manager.reportEvent(FireBaseManager.ON_PROGRESS_START_QUIT);
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        String firstCollaboratorFound = collaborators.get(0);
        removeCollaborators(transaction, firstCollaboratorFound, true);
        Log.d(TAG, "quitListAsAuthor: "+ firstCollaboratorFound +" new Author: "+ author);
//        removeCollaboratorData(transaction, userInfo.getUserId());

        if (author.equals(firstCollaboratorFound)) {
            return remove();
        }
        ShopListActions.setAuthor(transaction, ref, author);
//        setAuthor(author);
//        removeAllListeners();
        UserInfoActions.removeKnownList(transaction, ref, listId);
//        userInfo.removeKnownList(listId);
        transaction.apply();
        return true;
    }

    private boolean quitListAsCollaborator() {
        if (!collaborators.contains(userInfo.getUserId())) {
            return false;
        }

        manager.reportEvent(FireBaseManager.ON_PROGRESS_START_QUIT);
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
//        removeCollaboratorData(transaction, userInfo.getUserId());
//        removeCollaborators(transaction, userInfo.getUserId());
        removeCollaborators(transaction, userInfo.getUserId(), true);
//        removeAllListeners();
        UserInfoActions.removeKnownList(transaction, ref, listId).apply();

        return true;
    }

    private void removeAllTasks(TransactionWrapper transaction) {
        for (ShopTask task : shopTasks.values()) {
            ShopListActions.removeTask(transaction, ref, task.getRef());
//            task.remove();
        }
    }

    private void removeCollaboratorData(TransactionWrapper transaction, String userId) {
        Collaborator collaborator = collaboratorsData.get(userId);
        if (collaborator != null) {
            CollaboratorActions.remove(transaction, CollaboratorActions.getRef(ref, userId));
//                        collaborator.remove();
        }
    }

    public void setTitle(String newTitle) {

        if (newTitle == null || newTitle.equals(title)) {
            return;
        }

        TransactionWrapper transaction= new TransactionWrapper(manager.getDb(), this);
        ShopListActions.setTitle(transaction, ref, newTitle).apply();
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

        TransactionWrapper transaction= new TransactionWrapper(manager.getDb(), this);
        ShopListActions.setAuthor(transaction, ref, author).apply();
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
                collaboratorsData.put(
                        collaboratorId, new Collaborator(manager, this, collaboratorId));
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

        isMember = userInfo.getUserId().equals(author) ||
                collaborators.contains(userInfo.getUserId());
        setReady();

        reportChanges();
    }

    private void loadTokensFromDB(DocumentSnapshot document) {
        HashMap<String, Object> tokens = new HashMap<>();
        if (document.contains(ShopListActions.FIRESTORE_FIELD_TOKENS)) {
            tokens.putAll(
                    (HashMap<String, Object>) document.get(ShopListActions.FIRESTORE_FIELD_TOKENS));
        }

        this.tokens = tokens;
    }

    private void loadCollaboratorsFromDB(DocumentSnapshot document) {
        List<String> collaborators = new ArrayList<>();
        if (document.contains(ShopListActions.FIRESTORE_FIELD_COLLABORATORS)) {
            collaborators.addAll(
                    (List<String>) document.get(ShopListActions.FIRESTORE_FIELD_COLLABORATORS));
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
