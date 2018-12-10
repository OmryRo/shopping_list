package app.shoppinglist.wsux.shoppinglist.firebase;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Date;
import java.util.HashMap;

public class Collaborator extends BaseCollectionItem {

    private static final String TAG = "COLLABORATOR";
    public static final String FIRESTORE_TABLE = "collaborators";
    public static final String FIRESTORE_FIELD_NAME = "name";
    public static final String FIRESTORE_FIELD_EMAIL = "email";
    public static final String FIRESTORE_FIELD_MESSAGE = "message";
    public static final String FIRESTORE_FIELD_TTL = "ttl";
    public static final String FIRESTORE_FIELD_PICTURE = "picture";

    private static final long TIME_IN_A_DAY = 86400000;
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
        ref = inList.getRef().collection(FIRESTORE_TABLE).document(userId);
        ref.addSnapshotListener(this);
    }

    static Task<Void> addNewCollaborator(DocumentReference ref, UserInfo userInfo) {
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(FIRESTORE_FIELD_NAME, userInfo.getDisplayName());
        fields.put(FIRESTORE_FIELD_EMAIL, userInfo.getEmail());
        fields.put(FIRESTORE_FIELD_MESSAGE, "");
        fields.put(FIRESTORE_FIELD_PICTURE, userInfo.getPictureURL());
        fields.put(FIRESTORE_FIELD_TTL, getTTLObject());
        return ref.collection(FIRESTORE_TABLE).document(userInfo.getUserId()).set(fields);
    }

    private static Timestamp getTTLObject() {
        Date tomorrow = new Date();
        tomorrow.setTime(System.currentTimeMillis() + TIME_IN_A_DAY);
        return new Timestamp(tomorrow);
    }

    @Override
    void specificOnEvent(DocumentSnapshot document) {
        name = document.getString(FIRESTORE_FIELD_NAME);
        email = document.getString(FIRESTORE_FIELD_EMAIL);
        message = document.getString(FIRESTORE_FIELD_MESSAGE);

        if (document.contains(FIRESTORE_FIELD_PICTURE)) {
            pictureURL = document.getString(FIRESTORE_FIELD_PICTURE);
        }

        if (document.contains(FIRESTORE_FIELD_TTL)) {
            ttl = document.getTimestamp(FIRESTORE_FIELD_TTL);
        }

        color = COLORS[Math.abs(userId.hashCode()) % COLORS.length];

        if (!hasStartedPictureDownload && pictureURL != null) {
            hasStartedPictureDownload = true;
            manager.getImageManager().downloadPicture(this, pictureURL);
        }
        setReady();

        inList.reportChildChange();
        manager.reportEvent(FireBaseManager.ON_COLLABORATOR_UPDATED, this);

        if (onChangeListener != null) {
            onChangeListener.onChange();
        }

        if (isUpdateRequire()) {
            updateData();
        }
    }

    private boolean isUpdateRequire() {
        return ttl == null || ttl.getSeconds() < System.currentTimeMillis() / 1000;
    }

    private void updateData() {
        UserInfo currentUser = manager.getLoginManager().getCurrentUserInfo();

        if (!currentUser.getUserId().equals(userId)) {
            return;
        }

        name = currentUser.getDisplayName();
        email = currentUser.getEmail();
        pictureURL = currentUser.getPictureURL();
        ttl = getTTLObject();
        ref.update(
                FIRESTORE_FIELD_NAME, name,
                FIRESTORE_FIELD_EMAIL, email,
                FIRESTORE_FIELD_PICTURE, pictureURL,
                FIRESTORE_FIELD_TTL, ttl)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
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

        this.name = name;
        updateField(ref, FIRESTORE_FIELD_NAME, name);
    }

    public void setMessage(String message) {
        if (message == null || message.equals(this.message)) {
            return;
        }

        this.message = message;
        updateField(ref, FIRESTORE_FIELD_MESSAGE, message);
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
}
