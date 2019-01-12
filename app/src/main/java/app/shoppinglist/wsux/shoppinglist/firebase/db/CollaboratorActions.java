package app.shoppinglist.wsux.shoppinglist.firebase.db;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;

import java.util.Date;
import java.util.HashMap;


public class CollaboratorActions {
    public static final String FIRESTORE_TABLE = "dev.collaborators";
    public static final String FIRESTORE_FIELD_NAME = "name";
    public static final String FIRESTORE_FIELD_EMAIL = "email";
    public static final String FIRESTORE_FIELD_MESSAGE = "message";
    public static final String FIRESTORE_FIELD_TTL = "ttl";
    public static final String FIRESTORE_FIELD_PICTURE = "picture";

    private static final long TIME_IN_A_DAY = 86400000;

    public static DocumentReference getRef(DocumentReference shopListRef, String userId) {
        return shopListRef.collection(FIRESTORE_TABLE).document(userId);
    }

    static TransactionWrapper newCollaborator(
            TransactionWrapper transactionWrapper,
            DocumentReference ref,
            String name,
            String email,
            String picture,
            String message
    ) {
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(FIRESTORE_FIELD_NAME, name);
        fields.put(FIRESTORE_FIELD_EMAIL, email);
        fields.put(FIRESTORE_FIELD_PICTURE, picture);
        fields.put(FIRESTORE_FIELD_TTL, createTTLObject());
        fields.put(FIRESTORE_FIELD_MESSAGE, message);

        return transactionWrapper.create(ref, fields);
    }

    public static TransactionWrapper setName(
            TransactionWrapper transaction, DocumentReference ref, String name) {

        return transaction.update(ref, FIRESTORE_FIELD_NAME, name);
    }

    public static TransactionWrapper setMessage(
            TransactionWrapper transaction, DocumentReference ref, String message) {

        return transaction.update(ref, FIRESTORE_FIELD_MESSAGE, message);
    }

    public static TransactionWrapper setEmail(
            TransactionWrapper transaction, DocumentReference ref, String email) {

        return transaction.update(ref, FIRESTORE_FIELD_EMAIL, email);
    }

    public static TransactionWrapper setPictureUrl(
            TransactionWrapper transaction, DocumentReference ref, String pictureURL) {

        return transaction.update(ref, FIRESTORE_FIELD_PICTURE, pictureURL);
    }

    public static TransactionWrapper setTTL(
            TransactionWrapper transaction, DocumentReference ref) {

        return transaction.update(ref, FIRESTORE_FIELD_TTL, createTTLObject());
    }

    public static TransactionWrapper remove(
            TransactionWrapper transaction, DocumentReference ref) {

        return transaction.delete(ref);
    }

    private static Timestamp createTTLObject() {
        Date tomorrow = new Date();
        tomorrow.setTime(System.currentTimeMillis() + TIME_IN_A_DAY);
        return new Timestamp(tomorrow);
    }
}
