package app.shoppinglist.wsux.shoppinglist.firebase;

import android.content.Intent;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import app.shoppinglist.wsux.shoppinglist.R;

public class LoginManager {

    private final static String TAG = "FIRE_BASE_LOGIN_MANAGER";

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private UserInfo currentUserInfo;
    private FireBaseManager manager;


    LoginManager(FireBaseManager manager) {
        this.manager = manager;
    }

    void onCreate() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(manager.getAppContext().getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(manager.getAppContext(), gso);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    void onStart() {
        setCurrentUser(firebaseAuth.getCurrentUser());

        if (currentUser != null) {
            manager.reportEvent(FireBaseManager.ON_SIGN_IN, getCurrentUserInfo());
        } else {
            manager.reportEvent(FireBaseManager.ON_SIGN_ERR);
        }
    }

    private void setCurrentUser(FirebaseUser user) {
        currentUser = user;
        currentUserInfo = user != null ? new UserInfo(manager, user) : null;
    }

    public void requestLogin() {
        manager.reportEvent(FireBaseManager.ON_SIGN_IN_START);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        manager.getAppContext().startActivityForResult(signInIntent, FireBaseManager.RC_SIGN_IN);
    }

    public void requestLogout() {
        manager.reportEvent(FireBaseManager.ON_SIGN_OUT_START);
        setCurrentUser(null);
        firebaseAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener(manager.getAppContext(), new LogoutCompleteListener());
    }

    public void onSignInIntentReceived(Intent data) {
        Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
        handleSignInResult(task);
    }

    private void fireBaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(manager.getAppContext(), new FireBaseAuthWithGoogleOnCompleteListener());
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            fireBaseAuthWithGoogle(account);
        } catch (ApiException e) {
            manager.reportEvent(FireBaseManager.ON_SIGN_ERR, e);
        }
    }

    private class FireBaseAuthWithGoogleOnCompleteListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(Task<AuthResult> task) {
            if (task.isSuccessful()) {
                setCurrentUser(firebaseAuth.getCurrentUser());
                manager.reportEvent(FireBaseManager.ON_SIGN_IN, getCurrentUserInfo());

            } else {
                setCurrentUser(null);
                manager.reportEvent(FireBaseManager.ON_SIGN_ERR, task.getException());
            }
        }
    }

    private class LogoutCompleteListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(Task<Void> task) {
            setCurrentUser(null);
            manager.reportEvent(FireBaseManager.ON_SIGN_OUT);
        }
    }

    public UserInfo getCurrentUserInfo() {
        return currentUserInfo;
    }

}
