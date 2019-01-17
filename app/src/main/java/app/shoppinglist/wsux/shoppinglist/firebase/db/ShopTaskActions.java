package app.shoppinglist.wsux.shoppinglist.firebase.db;

import com.google.firebase.firestore.DocumentReference;

import java.util.HashMap;

public class ShopTaskActions {
    public static final String FIRESTORE_TABLE = "dev.tasks";
    public static final String FIRESTORE_FIELD_TITLE = "title";
    public static final String FIRESTORE_FIELD_CREATOR = "creator";
    public static final String FIRESTORE_FIELD_STATE = "state";
    public static final String FIRESTORE_FIELD_DESCRIPTION = "description";
    public static final String FIRESTORE_FIELD_IMAGE_URL = "image_url";

    public static final int SHOP_TASK_NOT_DONE = 0;
    public static final int SHOP_TASK_DONE = 1;

    public static DocumentReference initRef(DocumentReference shopListRef) {
        return shopListRef.collection(FIRESTORE_TABLE).document();
    }

    public static DocumentReference getRef(DocumentReference shopListRef, String taskId) {
        return shopListRef.collection(FIRESTORE_TABLE).document(taskId);
    }

    static TransactionWrapper newTask(
            TransactionWrapper transactionWrapper,
            DocumentReference shopTaskRef,
            String userId,
            String title,
            String description
    ) {
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(FIRESTORE_FIELD_CREATOR, userId);
        fields.put(FIRESTORE_FIELD_TITLE, title);
        fields.put(FIRESTORE_FIELD_DESCRIPTION, description);
        fields.put(FIRESTORE_FIELD_STATE, SHOP_TASK_NOT_DONE);

        return transactionWrapper.create(shopTaskRef, fields);
    }

    public static TransactionWrapper setTitle(
            TransactionWrapper transactionWrapper, DocumentReference ref, String title) {

        return transactionWrapper.update(ref, FIRESTORE_FIELD_TITLE, title);
    }

    public static TransactionWrapper setDescription(
            TransactionWrapper transactionWrapper, DocumentReference ref, String description) {

        return transactionWrapper.update(ref, FIRESTORE_FIELD_DESCRIPTION, description);
    }

    public static TransactionWrapper setState(
            TransactionWrapper transactionWrapper, DocumentReference ref, int state) {

        return transactionWrapper.update(ref, FIRESTORE_FIELD_STATE, state);
    }

    public static TransactionWrapper setImageUrl(
            TransactionWrapper transactionWrapper, DocumentReference ref, String imageUrl) {

        return transactionWrapper.update(ref, FIRESTORE_FIELD_IMAGE_URL, imageUrl);
    }

    public static TransactionWrapper removeImageUrl(
            TransactionWrapper transactionWrapper, DocumentReference ref) {

        return transactionWrapper.removeKey(ref, FIRESTORE_FIELD_IMAGE_URL);
    }

    public static TransactionWrapper remove(
            TransactionWrapper transactionWrapper, DocumentReference ref) {

        return transactionWrapper.delete(ref);
    }
}
