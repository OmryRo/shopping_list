package app.shoppinglist.wsux.shoppinglist.firebase;

import android.graphics.Color;

import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;

import java.util.HashMap;

public class Collaborator extends BaseCollectionItem {

    private static final String TAG = "COLLABORATOR";
    public static final String FIRESTORE_TABLE = "collaborators";
    public static final String FIRESTORE_FIELD_NAME = "name";
    public static final String FIRESTORE_FIELD_EMAIL = "email";
    public static final String FIRESTORE_FIELD_MESSAGE = "message";

    private static final int[] COLORS = {
            0xff224f96, 0xff1f918f, 0xff16a03f, 0xff74a518, 0xffa8a51c, 0xffa8721b,
            0xffa8241a, 0xffa81a57, 0xffa81a8d, 0xff821aa8, 0xff461aa8
    };

    private DocumentReference ref;
    private ShopList inList;

    private String userId;
    private String name;
    private String email;
    private String message;
    private int color;

    Collaborator(FireBaseManager manager, ShopList inList, String userId) {
        super(manager);
        this.inList = inList;
        this.userId = userId;

        ref = inList.getRef().collection(FIRESTORE_TABLE).document(userId);
        ref.addSnapshotListener(this);
    }

    static Task<Void> addNewCollaborator(DocumentReference ref, UserInfo userInfo) {
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(FIRESTORE_FIELD_NAME, userInfo.getDisplayName());
        fields.put(FIRESTORE_FIELD_EMAIL, userInfo.getEmail());
        fields.put(FIRESTORE_FIELD_MESSAGE, "");
        return ref.collection(FIRESTORE_TABLE).document(userInfo.getUserId()).set(fields);
    }

    @Override
    void specificOnEvent(DocumentSnapshot document) {
        name = document.getString(FIRESTORE_FIELD_NAME);
        email = document.getString(FIRESTORE_FIELD_EMAIL);
        message = document.getString(FIRESTORE_FIELD_MESSAGE);
        color = COLORS[Math.abs(userId.hashCode()) % COLORS.length];

        setReady();

        inList.reportChildChange();
        manager.reportEvent(FireBaseManager.ON_COLLABORATOR_UPDATED, this);

        if (onChangeListener != null) {
            onChangeListener.onChange();
        }
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
        this.name = name;
        updateField(ref, FIRESTORE_FIELD_NAME, name);
    }

    public void setMessage(String message) {
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
