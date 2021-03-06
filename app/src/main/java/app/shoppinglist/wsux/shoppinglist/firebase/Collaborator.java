package app.shoppinglist.wsux.shoppinglist.firebase;

import android.graphics.Bitmap;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import app.shoppinglist.wsux.shoppinglist.firebase.db.CollaboratorActions;
import app.shoppinglist.wsux.shoppinglist.firebase.db.TransactionWrapper;

public class Collaborator extends BaseCollectionItem {

    private static final String TAG = "COLLABORATOR";
    private static final int[] COLORS = {
            0xff224f96, 0xff1f918f, 0xff16a03f, 0xff74a518, 0xffa8a51c, 0xffa8721b,
            0xffa8241a, 0xffa81a57, 0xffa81a8d, 0xff821aa8, 0xff461aa8
    };

    private boolean hasStartedPictureDownload;
    private DocumentReference ref;
    private ShopList inList;

    private String userId;
    private String name;
    private String email;
    private String message;
    private String pictureURL;
    private Timestamp ttl;
    private int color;

    Collaborator(FireBaseManager manager, ShopList inList, String userId) {
        super(manager);
        this.inList = inList;
        this.userId = userId;

        hasStartedPictureDownload = false;
        ref = CollaboratorActions.getRef(inList.getRef(), userId);
        ref.addSnapshotListener(this);
    }

    /*
     *  Note that this function's purpose is to assign values in the class's fields
     *  Splitting it to sub-functions will just make it less readable
     */
    @Override
    void specificOnEvent(DocumentSnapshot document) {
        getUserInfo(document);
        getPictureUrl(document);
        getTtl(document);

        color = COLORS[Math.abs(userId.hashCode()) % COLORS.length];

        setReady();
        reportEventChange();
    }

    private void reportEventChange() {
        inList.reportChildChange();
        manager.reportEvent(FireBaseManager.ON_COLLABORATOR_UPDATED, this);

        if (onChangeListener != null) {
            onChangeListener.onChange();
        }

        if (isUpdateRequire()) {
            updateData();
        }
    }

    private void getTtl(DocumentSnapshot document) {
        if (document.contains(CollaboratorActions.FIRESTORE_FIELD_TTL)) {
            ttl = document.getTimestamp(CollaboratorActions.FIRESTORE_FIELD_TTL);
        }
    }

    private void getPictureUrl(DocumentSnapshot document) {
        if (document.contains(CollaboratorActions.FIRESTORE_FIELD_PICTURE)) {
            pictureURL = document.getString(CollaboratorActions.FIRESTORE_FIELD_PICTURE);
        }
        if (!hasStartedPictureDownload && pictureURL != null) {
            hasStartedPictureDownload = true;
            manager.getImageManager().downloadPicture(this, pictureURL);
        }
    }

    private void getUserInfo(DocumentSnapshot document) {
        name = document.getString(CollaboratorActions.FIRESTORE_FIELD_NAME);
        email = document.getString(CollaboratorActions.FIRESTORE_FIELD_EMAIL);
        message = document.getString(CollaboratorActions.FIRESTORE_FIELD_MESSAGE);
    }

    private boolean isUpdateRequire() {
        return ttl == null || ttl.getSeconds() < System.currentTimeMillis() / 1000;
    }

    private void updateData() {
        UserInfo currentUser = manager.getLoginManager().getCurrentUserInfo();

        if (!currentUser.getUserId().equals(userId)) {
            return;
        }

        getCurrentInfo(currentUser);
        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        setOnCollaborator(transaction);

        transaction.apply();

    }

    private void getCurrentInfo(UserInfo currentUser) {
        name = currentUser.getDisplayName();
        email = currentUser.getEmail();
        pictureURL = currentUser.getPictureURL();
    }

    private void setOnCollaborator(TransactionWrapper transaction) {
        CollaboratorActions.setName(transaction, ref, name);
        CollaboratorActions.setEmail(transaction, ref, email);
        CollaboratorActions.setPictureUrl(transaction, ref, pictureURL);
        CollaboratorActions.setTTL(transaction, ref);
    }

    public Bitmap getPicture() {
        return manager.getImageManager().getPicture(this);
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }

    public String getMessage() {
        return message;
    }

    public int getColor() {
        return color;
    }

    public void setName(String name) {
        if (name == null || name.equals(this.name)) {
            return;
        }

        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        CollaboratorActions.setName(transaction, ref, name).apply();
    }

    public void setMessage(String message) {
        if (message == null || message.equals(this.message)) {
            return;
        }

        TransactionWrapper transaction = new TransactionWrapper(manager.getDb(), this);
        CollaboratorActions.setMessage(transaction, ref, message).apply();
    }

    public ShopList getInList() {
        return inList;
    }

    @Override
    void specificOnSuccess() {
        manager.reportEvent(FireBaseManager.ON_COLLABORATOR_UPDATED, this);
    }

    @Override
    void specificOnFailure(Exception e) {
        manager.reportEvent(FireBaseManager.ON_COLLABORATOR_FAILURE, this, e);
    }

    public void remove(final RemoveListener listener) {
        TransactionWrapper transaction =
                new TransactionWrapper(manager.getDb(), new TransactionWrapper.ResultListener() {
            @Override
            public void onSuccess() {
                if (listener != null) {
                    listener.onCollaboratorRemoved();
                }
                Collaborator.this.onSuccess();
            }

            @Override
            public void onFailure(Exception e) {
                Collaborator.this.onFailure(e);
            }
        });

        inList.removeCollaborator(transaction, userId, true).apply();
    }

    @Override
    public String toString() {
        return String.format("Collaborator: %s -> %s", inList.getListId(), userId);
    }

    public interface RemoveListener {
        void onCollaboratorRemoved();
    }
}
