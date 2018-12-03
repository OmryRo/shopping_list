package app.shoppinglist.wsux.shoppinglist.firebase;

import com.google.firebase.auth.FirebaseUser;

public class UserInfo {
    private String userId;
    private String email;
    private String displayName;

    UserInfo(FirebaseUser user) {
        userId = user.getUid();
        email = user.getEmail();
        displayName = user.getDisplayName();
    }

    public String getUserId() {
        return userId;
    }

    public String getEmail() {
        return email;
    }

    public String getDisplayName() {
        return displayName;
    }
}
