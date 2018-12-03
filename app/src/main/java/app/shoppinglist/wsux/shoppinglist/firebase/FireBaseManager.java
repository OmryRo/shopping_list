package app.shoppinglist.wsux.shoppinglist.firebase;

import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class FireBaseManager {

    private Context context;
    private LoginManager loginManager;
    private FireBaseEventsInterface eventsInterface;

    public FireBaseManager(Context context, FireBaseEventsInterface eventsInterface) {
        this.context = context;
        this.eventsInterface = eventsInterface;
        this.loginManager = new LoginManager(context, new FireBaseLoginEventHandler());
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

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        loginManager.onActivityResult(requestCode, resultCode, data);
    }

    private class FireBaseLoginEventHandler implements LoginManager.LoginStateInterface {
        @Override
        public void onSignIn() {
            eventsInterface.onSignIn(loginManager.getCurrentUserInfo());
        }

        @Override
        public void onSignInFailed(int what) {
            eventsInterface.onSignInFailed(what);
        }

        @Override
        public void onSignOut() {
            eventsInterface.onSignOut();
        }
    }


    public interface FireBaseEventsInterface {
        void onSignIn(UserInfo userInfo);
        void onSignInFailed(int what);
        void onSignOut();
    }
}
