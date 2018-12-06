package app.shoppinglist.wsux.shoppinglist.firebase;

import android.content.Context;
import android.content.Intent;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreSettings;

import java.io.File;

public class FireBaseManager {

    private Context context;
    private LoginManager loginManager;
    private ShareHandler shareHandler;
    private FireBaseEventsInterface eventsInterface;
    private FirebaseFirestore db;
    private ImageManager imageManager;

    public static final int ON_SIGN_IN = 0;
    public static final int ON_SIGN_OUT = 1;
    public static final int ON_SIGN_ERR = 2;
    public static final int ON_USER_LIST_UPDATED = 3;
    public static final int ON_USER_INFO_UPDATED = 4;
    public static final int ON_USER_UPDATE_FAILED = 5;
    public static final int ON_LIST_CREATED = 6;
    public static final int ON_LIST_UPDATED = 7;
    public static final int ON_LIST_FAILURE = 8;
    public static final int ON_LIST_DELETED = 9;
    public static final int ON_TASK_CREATED = 10;
    public static final int ON_TASK_UPDATED = 11;
    public static final int ON_TASK_FAILURE = 12;
    public static final int ON_TASK_DELETED = 13;
    public static final int ON_COLLABORATOR_CREATED = 14;
    public static final int ON_COLLABORATOR_UPDATED = 15;
    public static final int ON_COLLABORATOR_FAILURE = 16;
    public static final int ON_COLLABORATOR_DELETED = 17;
    public static final int ON_SHARE_LIST_FOUND = 18;

    public FireBaseManager(Context context, FireBaseEventsInterface eventsInterface) {
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
    }

    public void onCreate() {
        loginManager.onCreate();
    }

    public void onStart() {
        loginManager.onStart();
    }

    public LoginManager getLoginManager() {
        return loginManager;
    }

    public ShareHandler getShareHandler() {
        return shareHandler;
    }

    public ImageManager getImageManager() { return imageManager; }

    FirebaseFirestore getDb() {
        return db;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        loginManager.onActivityResult(requestCode, resultCode, data);
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
