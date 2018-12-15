package app.shoppinglist.wsux.shoppinglist.firebase;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.io.File;

public class FireBaseManager {

    private Activity context;
    private LoginManager loginManager;
    private ShareHandler shareHandler;
    private FireBaseEventsInterface eventsInterface;
    private FirebaseFirestore db;
    private ImageManager imageManager;
    private UploadManager uploadManager;

    public static final int ON_SIGN_IN = 0x0000;
    public static final int ON_SIGN_OUT = 0x0001;
    public static final int ON_SIGN_ERR = 0x0002;
    public static final int ON_USER_LIST_UPDATED = 0x0003;
    public static final int ON_USER_INFO_UPDATED = 0x0004;
    public static final int ON_USER_UPDATE_FAILED = 0x0005;
    public static final int ON_LIST_CREATED = 0x0006;
    public static final int ON_LIST_UPDATED = 0x0007;
    public static final int ON_LIST_FAILURE = 0x0008;
    public static final int ON_LIST_DELETED = 0x0009;
    public static final int ON_TASK_CREATED = 0x000a;
    public static final int ON_TASK_UPDATED = 0x000b;
    public static final int ON_TASK_FAILURE = 0x000c;
    public static final int ON_TASK_DELETED = 0x000d;
    public static final int ON_COLLABORATOR_CREATED = 0x000e;
    public static final int ON_COLLABORATOR_UPDATED = 0x000f;
    public static final int ON_COLLABORATOR_FAILURE = 0x0010;
    public static final int ON_COLLABORATOR_DELETED = 0x0011;
    public static final int ON_SHARE_LIST_FOUND = 0x0012;
    public static final int ON_LAST_LIST_DOWNLOADED = 0x0013;
    public static final int ON_SELECT_PICTURE_START = 0x0014;
    public static final int ON_SELECT_PICTURE_SUCCESS = 0x0015;
    public static final int ON_SELECT_PICTURE_FAILED = 0x0016;
    public static final int ON_SELECT_CAMERA_START = 0x0017;
    public static final int ON_SELECT_CAMERA_SUCCESS = 0x0018;
    public static final int ON_SELECT_CAMERA_FAILED = 0x0019;
    public static final int ON_PICTURE_UPLOAD_STARTED = 0x001a;
    public static final int ON_PICTURE_UPLOAD_SUCCESS = 0x00b;
    public static final int ON_PICTURE_UPLOAD_FAILED = 0x001c;
    public static final int ON_CAMERA_UPLOAD_STARTED = 0x001d;
    public static final int ON_CAMERA_UPLOAD_SUCCESS = 0x001e;
    public static final int ON_CAMERA_UPLOAD_FAILED = 0x001f;

    public static final int RC_SIGN_IN = 0x1000;
    public static final int RC_FILE = 0x1001;
    public static final int RC_CAMERA = 0x1002;
    public static final int RC_CAMERA_PERMISSION = 0x1003;

    public FireBaseManager(Activity context, FireBaseEventsInterface eventsInterface) {
        this.context = context;
        this.eventsInterface = eventsInterface;
        this.db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        this.db.setFirestoreSettings(settings);

        this.loginManager = new LoginManager(context, this);
        this.shareHandler = new ShareHandler(context, this);
        this.imageManager = new ImageManager(context, this);
        this.uploadManager = new UploadManager(context, this);
    }

    public void onCreate() {
        loginManager.onCreate();
    }

    public void onStart() {
        loginManager.onStart();

        UserInfo currentUser = loginManager.getCurrentUserInfo();
        if (currentUser != null) {
            shareHandler.checkIncomingShare(currentUser);
        }
    }

    public LoginManager getLoginManager() {
        return loginManager;
    }

    public ShareHandler getShareHandler() {
        return shareHandler;
    }

    public ImageManager getImageManager() { return imageManager; }

    public UploadManager getUploadManager() { return uploadManager; }

    FirebaseFirestore getDb() {
        return db;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case RC_SIGN_IN:
                loginManager.onSignInIntentReceived(data);
                break;
            case RC_FILE:
                uploadManager.onPictureRequestResult(resultCode, data);
                break;
            case RC_CAMERA:
                uploadManager.onCameraRequestResult(resultCode, data);
                break;
        }

    }

    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {

        switch (requestCode) {
            case RC_CAMERA_PERMISSION:
                uploadManager.onRequestCameraPermissionsResult(permissions, grantResults);
                break;
        }

    }

    void reportEvent(int what) { reportEvent(what, null, null);}
    void reportEvent(int what, Object data) { reportEvent(what, data, null); }
    void reportEvent(int what, Exception e) { reportEvent(what, null, e); }
    void reportEvent(int what, Object data, Exception e) {

        if (what == ON_SIGN_IN) {
            shareHandler.checkIncomingShare((UserInfo) data);
        }

        eventsInterface.onEventOccurred(what, data, e);
    }

    public interface FireBaseEventsInterface {
        void onEventOccurred(int what, Object data, Exception e);
    }
}
