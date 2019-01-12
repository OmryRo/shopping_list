package app.shoppinglist.wsux.shoppinglist.firebase;

import android.graphics.Bitmap;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import app.shoppinglist.wsux.shoppinglist.firebase.db.ShopTaskActions;
import app.shoppinglist.wsux.shoppinglist.firebase.db.TransactionWrapper;

public class ShopTask extends BaseCollectionItem implements Comparable<ShopTask> {

    private static final String TAG = "SHOP_TASK";

    private DocumentReference ref;
    private ShopList inList;

    private String taskId;
    private String title;
    private long state;
    private String creator;
    private String description;
    private String imageUrl;

    private String downloadedImage;

    ShopTask(FireBaseManager manager, ShopList inList, String taskId) {
        super(manager);
        this.inList = inList;
        this.taskId = taskId;

        this.ref = ShopTaskActions.getRef(inList.getRef(), taskId);
        this.ref.addSnapshotListener(this);
    }

    @Override
    void specificOnEvent(DocumentSnapshot document) {
        fillDataFromDb(document);

        if (document.contains(ShopTaskActions.FIRESTORE_FIELD_IMAGE_URL)) {
            downloadImageFromFireStoreUrl(document);
        }

        setReady();
        reportOnChangeEvent();
    }

    private void downloadImageFromFireStoreUrl(DocumentSnapshot document) {
        imageUrl = document.getString(ShopTaskActions.FIRESTORE_FIELD_IMAGE_URL);

        if (isValidImageUrl()) {
            downloadedImage = imageUrl;
            manager.getImageManager().downloadPicture(this, imageUrl);
        }
    }

    private boolean isValidImageUrl() {
        return imageUrl != null && imageUrl.length() > 0 && !imageUrl.equals(downloadedImage);
    }

    private void fillDataFromDb(DocumentSnapshot document) {
        title = document.getString(ShopTaskActions.FIRESTORE_FIELD_TITLE);
        state = document.getLong(ShopTaskActions.FIRESTORE_FIELD_STATE);
        creator = document.getString(ShopTaskActions.FIRESTORE_FIELD_CREATOR);
        description = document.getString(ShopTaskActions.FIRESTORE_FIELD_DESCRIPTION);
    }

    private void reportOnChangeEvent() {
        manager.reportEvent(FireBaseManager.ON_TASK_UPDATED, this);

        inList.reportChildChange();

        if (onChangeListener != null) {
            onChangeListener.onChange();
        }
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getTaskId() {
        return taskId;
    }

    public Bitmap getPicture() {
        return manager.getImageManager().getPicture(this);
    }

    String getPictureUrl() { return imageUrl; }

    public boolean hasPicture() {
        return imageUrl != null && imageUrl.length() > 0;
    }

    public Collaborator getCreator() {
        return inList.getCollaborators().get(creator);
    }

    public boolean isDone() {
        return state == ShopTaskActions.SHOP_TASK_DONE;
    }

    public ShopList getInList() {
        return inList;
    }

    DocumentReference getRef() { return ref; }

    public void setTitle(String title) {

        if (title == null || title.equals(this.title)) {
            return;
        }

        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        ShopTaskActions.setTitle(transaction, ref, title).apply();
    }

    public void setDescription(String description) {

        if (description == null || description.equals(this.description)) {
            return;
        }

        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        ShopTaskActions.setDescription(transaction, ref, description).apply();
    }

    public void setState(boolean isDone) {
        setState(isDone ? ShopTaskActions.SHOP_TASK_DONE : ShopTaskActions.SHOP_TASK_NOT_DONE);
    }

    private void setState(int state) {

        if (this.state == state) {
            return;
        }

        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        ShopTaskActions.setState(transaction, ref, state).apply();
    }

    @Override
    protected void setReady() {

        if (!isReady()) {
            inList.reportChildChange();
        }

        super.setReady();
    }

    void setImageUrl(String imageUrl) {

        if (imageUrl == null) {
            imageUrl = "";
        }

        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        ShopTaskActions.setImageUrl(transaction, ref, imageUrl).apply();
    }

    private void removeImageUrl() {
        
        if (imageUrl == null) {
            return;
        }

        imageUrl = null;
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        ShopTaskActions.removeImageUrl(transaction, ref).apply();
    }

    public void removeImage() {
        if (imageUrl == null) {
            return;
        }

        removeImageUrl();
        manager.getUploadManager().deleteImage(this);
    }

    public void remove() {
        inList.removeTaskFromList(this);
        removeAllListeners();
        setNotReady();
    }

    @Override
    void specificOnSuccess() {
        manager.reportEvent(FireBaseManager.ON_TASK_UPDATED, this);
    }

    @Override
    void specificOnFailure(Exception e) {
        manager.reportEvent(FireBaseManager.ON_TASK_FAILURE, this, e);
    }

    @Override
    public String toString() {
        return String.format("ShopTask: %s -> %s", inList.getListId(), taskId);
    }

    @Override
    public int compareTo(ShopTask other) {

        return state == other.state ? title.compareTo(other.title) : (int) (state - other.state);
    }
}
