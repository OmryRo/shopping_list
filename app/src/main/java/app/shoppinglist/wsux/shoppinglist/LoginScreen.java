package app.shoppinglist.wsux.shoppinglist;

import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;


public class LoginScreen extends Fragment implements View.OnClickListener {

    public LoginScreen() {}
    private FireBaseManager fireBaseManager;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View loginScreenLayout = inflater.inflate(R.layout.fragment_login_screen, container, false);
        loginScreenLayout.findViewById(R.id.sign_in_button).setOnClickListener(this);

        return loginScreenLayout;
    }

    public void setFirebaseManager(FireBaseManager firebaseManager) {
        this.fireBaseManager = firebaseManager;
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.sign_in_button:
                onSignInButtonClick();
                break;
        }
    }

    private void onSignInButtonClick() {
        fireBaseManager.getLoginManager().requestLogin();
    }
}
