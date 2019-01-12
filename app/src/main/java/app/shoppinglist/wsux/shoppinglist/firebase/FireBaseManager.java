package app.shoppinglist.wsux.shoppinglist.firebase;

import android.app.Activity;
import android.content.Intent;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

public class FireBaseManager {

    Activity context;
    private LoginManager loginManager;
    private ShareHandler shareHandler;
    private FireBaseEventsInterface eventsInterface;
    private FirebaseFirestore db;
    private ImageManager imageManager;
    private UploadManager uploadManager;

    public static final int ON_SIGN_IN = 0x1000;
    public static final int ON_SIGN_OUT = 0x1001;
    public static final int ON_SIGN_ERR = 0x1002;
    public static final int ON_SIGN_IN_START = 0x1010;
    public static final int ON_SIGN_OUT_START = 0x1011;
    public static final int ON_USER_LIST_UPDATED = 0x2000;
    public static final int ON_USER_INFO_UPDATED = 0x2001;
    public static final int ON_USER_UPDATE_FAILED = 0x2002;
    public static final int ON_LIST_CREATED = 0x3000;
    public static final int ON_LIST_UPDATED = 0x3001;
    public static final int ON_LIST_FAILURE = 0x3002;
    public static final int ON_LIST_DELETED = 0x3003;
    public static final int ON_LIST_REMOVED_FROM = 0x3004;
    public static final int ON_TASK_CREATED = 0x4000;
    public static final int ON_TASK_UPDATED = 0x4001;
    public static final int ON_TASK_FAILURE = 0x4002;
    public static final int ON_TASK_DELETED = 0x4003;
    public static final int ON_COLLABORATOR_CREATED = 0x5001;
    public static final int ON_COLLABORATOR_UPDATED = 0x5002;
    public static final int ON_COLLABORATOR_FAILURE = 0x5003;
    public static final int ON_COLLABORATOR_DELETED = 0x5004;
    public static final int ON_SHARE_LIST_FOUND = 0x6000;
    public static final int ON_LAST_LIST_DOWNLOADED = 0x6001;
    public static final int ON_SELECT_PICTURE_START = 0x7000;
    public static final int ON_SELECT_PICTURE_SUCCESS = 0x7001;
    public static final int ON_SELECT_PICTURE_FAILED = 0x7002;
    public static final int ON_SELECT_CAMERA_START = 0x7010;
    public static final int ON_SELECT_CAMERA_SUCCESS = 0x7011;
    public static final int ON_SELECT_CAMERA_FAILED = 0x7012;
    public static final int ON_PICTURE_UPLOAD_STARTED = 0x7020;
    public static final int ON_PICTURE_UPLOAD_SUCCESS = 0x7021;
    public static final int ON_PICTURE_UPLOAD_FAILED = 0x7022;
    public static final int ON_CAMERA_UPLOAD_STARTED = 0x7030;
    public static final int ON_CAMERA_UPLOAD_SUCCESS = 0x7031;
    public static final int ON_CAMERA_UPLOAD_FAILED = 0x7032;
    public static final int ON_PROGRESS_START_UPDATE = 0x8000;
    public static final int ON_PROGRESS_START_DELETE = 0x8001;
    public static final int ON_PROGRESS_START_CREATE = 0x8002;
    public static final int ON_PROGRESS_START_LOAD = 0x8003;
    public static final int ON_PROGRESS_START_DOWNLOAD = 0x8004;
    public static final int ON_PROGRESS_START_QUIT = 0x8005;

    public static final int RC_SIGN_IN = 0x1000;
    public static final int RC_FILE = 0x1001;
    public static final int RC_CAMERA = 0x1002;
    public static final int RC_CAMERA_PERMISSION = 0x1003;

    public FireBaseManager(Activity context, FireBaseEventsInterface eventsInterface) {
        this.context = context;
        this.eventsInterface = eventsInterface;

        initFireStore();
        initHandlers();
    }

    Activity getAppContext() {
        return context;
    }

    private void initFireStore() {
        this.db = FirebaseFirestore.getInstance();
        FirebaseFirestoreSettings settings = new FirebaseFirestoreSettings.Builder()
                .setTimestampsInSnapshotsEnabled(true)
                .build();
        this.db.setFirestoreSettings(settings);
    }

    private void initHandlers() {
        this.loginManager = new LoginManager(this);
        this.shareHandler = new ShareHandler(this);
        this.imageManager = new ImageManager(this);
        this.uploadManager = new UploadManager(this);
    }

    public void onCreate() {
        loginManager.onCreate();
    }

    public void onStart(Activity context) {
        this.context = context;

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

    public ImageManager getImageManager() {
        return imageManager;
    }

    public UploadManager getUploadManager() {
        return uploadManager;
    }

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

    void reportEvent(int what) {
        reportEvent(what, null, null);
    }

    void reportEvent(int what, Object data) {
        reportEvent(what, data, null);
    }

    void reportEvent(int what, Exception e) {
        reportEvent(what, null, e);
    }

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
