package app.shoppinglist.wsux.shoppinglist.firebase;

import android.app.Activity;
import android.content.Context;
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

    private final static int RC_SIGN_IN = 999;
    private final static String TAG = "FIRE_BASE_LOGIN_MANAGER";

    private Context context;
    private LoginStateInterface loginStateInterface;

    private GoogleSignInClient googleSignInClient;
    private FirebaseAuth firebaseAuth;
    private FirebaseUser currentUser;
    private UserInfo currentUserInfo;


    LoginManager(Context context, LoginStateInterface loginStateInterface) {
        this.context = context;
        this.loginStateInterface = loginStateInterface;
    }

    void onCreate() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(context.getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(context, gso);
        firebaseAuth = FirebaseAuth.getInstance();
    }

    void onStart() {
        setCurrentUser(firebaseAuth.getCurrentUser());

        if (currentUser != null) {
            loginStateInterface.onSignIn();
        } else {
            loginStateInterface.onSignInFailed(-1);
        }
    }

    private void setCurrentUser(FirebaseUser user) {
        currentUser = user;
        currentUserInfo = user != null ? new UserInfo(user) : null;
    }

    public void requestLogin() {
        Intent signInIntent = googleSignInClient.getSignInIntent();
        ((Activity) context).startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    public void requestLogout() {
        setCurrentUser(null);
        firebaseAuth.signOut();
        googleSignInClient.signOut().addOnCompleteListener((Activity) context, new LogoutCompleteListener());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case RC_SIGN_IN:
                Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                handleSignInResult(task);
                break;
        }

    }

    private void fireBaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener((Activity) context, new FireBaseAuthWithGoogleOnCompleteListener());
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            fireBaseAuthWithGoogle(account);
        } catch (ApiException e) {
            loginStateInterface.onSignInFailed(e.getStatusCode());
        }
    }

    private class FireBaseAuthWithGoogleOnCompleteListener implements OnCompleteListener<AuthResult> {
        @Override
        public void onComplete(Task<AuthResult> task) {
            if (task.isSuccessful()) {
                setCurrentUser(firebaseAuth.getCurrentUser());
                loginStateInterface.onSignIn();

            } else {
                setCurrentUser(null);
                loginStateInterface.onSignInFailed(0);
            }
        }
    }

    private class LogoutCompleteListener implements OnCompleteListener<Void> {
        @Override
        public void onComplete(Task<Void> task) {
            setCurrentUser(null);
            loginStateInterface.onSignOut();
        }
    }

    public UserInfo getCurrentUserInfo() {
        return currentUserInfo;
    }

    interface LoginStateInterface {
        void onSignIn();
        void onSignInFailed(int what);
        void onSignOut();
    }

}
