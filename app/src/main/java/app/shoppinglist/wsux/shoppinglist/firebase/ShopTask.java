package app.shoppinglist.wsux.shoppinglist.firebase;


import android.graphics.Bitmap;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.HashMap;

public class ShopTask extends BaseCollectionItem {

    private static final String TAG = "SHOP_TASK";
    public static final String FIRESTORE_TABLE = "tasks";
    public static final String FIRESTORE_FIELD_TITLE = "title";
    public static final String FIRESTORE_FIELD_CREATOR = "creator";
    public static final String FIRESTORE_FIELD_STATE = "state";
    public static final String FIRESTORE_FIELD_DESCRIPTION = "description";
    public static final String FIRESTORE_FIELD_IMAGE_URL = "image_url";

    public static final int SHOP_TASK_NOT_DONE = 0;
    public static final int SHOP_TASK_DONE = 1;

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

        this.ref = inList.getRef().collection(FIRESTORE_TABLE).document(taskId);
        this.ref.addSnapshotListener(this);
    }

    @Override
    void specificOnEvent(DocumentSnapshot document) {
        title = document.getString(FIRESTORE_FIELD_TITLE);
        state = document.getLong(FIRESTORE_FIELD_STATE);
        creator = document.getString(FIRESTORE_FIELD_CREATOR);
        description = document.getString(FIRESTORE_FIELD_DESCRIPTION);

        if (document.contains(FIRESTORE_FIELD_IMAGE_URL)) {
            imageUrl = document.getString(FIRESTORE_FIELD_IMAGE_URL);

            if (imageUrl != null && imageUrl.length() > 0 && !imageUrl.equals(downloadedImage)) {
                downloadedImage = imageUrl;
                manager.getImageManager().downloadPicture(this, imageUrl);
            }

        }

        setReady();
        reportOnChangeEvent();
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

    public long getState() {
        return state;
    }

    public ShopList getInList() {
        return inList;
    }

    public void setTitle(String title) {

        if (title == null || title.equals(this.title)) {
            return;
        }

        updateField(ref, FIRESTORE_FIELD_TITLE, title);
    }

    public void setDescription(String description) {

        if (description == null || description.equals(this.description)) {
            return;
        }

        updateField(ref, FIRESTORE_FIELD_DESCRIPTION, description);
    }

    public void setState(int state) {

        if (this.state == state) {
            return;
        }

        updateField(ref, FIRESTORE_FIELD_STATE, state);
    }

    @Override
    protected void setReady() {

        if (!isReady()) {
            inList.reportChildChange();
        }

        super.setReady();
    }

    void setImageUrl(String imageUrl) {

        if (this.imageUrl == imageUrl) {
            return;
        }

        if (imageUrl == null) {
            imageUrl = "";
        }

        updateField(ref, FIRESTORE_FIELD_IMAGE_URL, imageUrl);
    }

    private void removeImageUrl() {

        if (imageUrl == null) {
            return;
        }

        imageUrl = null;

        updateField(ref, FIRESTORE_FIELD_IMAGE_URL, FieldValue.delete());

    }

    static Task<DocumentReference> addNewTask(ShopList inList, String title, String description, UserInfo userInfo) {
        inList.manager.reportEvent(FireBaseManager.ON_PROGRESS_START_CREATE);
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(FIRESTORE_FIELD_CREATOR, userInfo.getUserId());
        fields.put(FIRESTORE_FIELD_TITLE, title);
        fields.put(FIRESTORE_FIELD_DESCRIPTION, description);
        fields.put(FIRESTORE_FIELD_STATE, SHOP_TASK_NOT_DONE);
        return inList.getRef().collection(FIRESTORE_TABLE).add(fields);
    }

    public void removeImage() {
        removeImageUrl();
        manager.getUploadManager().deleteImage(this);
    }

    public void remove() {
        manager.reportEvent(FireBaseManager.ON_PROGRESS_START_DELETE);
        ref.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                inList.removeTaskFromList(ShopTask.this);
            }
        });
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
        return String.format("ShopTask: %s", taskId);
    }
}
