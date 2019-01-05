package app.shoppinglist.wsux.shoppinglist.firebase.db;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

public class ShopListActions {
    public static final String FIRESTORE_TABLE = "lists";
    public static final String FIRESTORE_FIELD_AUTHOR = "author";
    public static final String FIRESTORE_FIELD_TITLE = "title";
    public static final String FIRESTORE_FIELD_TASKS = "tasks";
    public static final String FIRESTORE_FIELD_TOKENS = "tokens";
    public static final String FIRESTORE_FIELD_COLLABORATORS = "collaborators";

    public static DocumentReference getRef(FirebaseFirestore db, String listId) {
        return db.collection(FIRESTORE_TABLE).document(listId);
    }

    public static TransactionWrapper setAuthor(TransactionWrapper transactionWrapper,
                                               DocumentReference ref, String author) {
        return transactionWrapper.update(ref, FIRESTORE_FIELD_AUTHOR, author);
    }

    public static TransactionWrapper setTitle(TransactionWrapper transactionWrapper,
                                               DocumentReference ref, String title) {
        return transactionWrapper.update(ref, FIRESTORE_FIELD_TITLE, title);
    }

    public static TransactionWrapper addTask(
            TransactionWrapper transactionWrapper,
            DocumentReference ref,
            String userid,
            String title,
            String description
    ) {

        DocumentReference taskRef = ref.collection(ShopTaskActions.FIRESTORE_TABLE).document();
        ShopTaskActions.newTask(transactionWrapper, taskRef, userid, title, description);
        return transactionWrapper.addToList(ref, FIRESTORE_FIELD_TASKS, taskRef.getId());
    }
    //
}
