package app.shoppinglist.wsux.shoppinglist.firebase.db;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

public class ShopListActions {
    public static final String FIRESTORE_TABLE = "dev.lists";
    public static final String FIRESTORE_FIELD_AUTHOR = "author";
    public static final String FIRESTORE_FIELD_TITLE = "title";
    public static final String FIRESTORE_FIELD_TASKS = "tasks";
    public static final String FIRESTORE_FIELD_TOKENS = "tokens";
    public static final String FIRESTORE_FIELD_COLLABORATORS = "collaborators";
    private static final long TIME_IN_A_WEEK_IN_MILIES_FACTOR = 604800000;

    public static DocumentReference getNewRef(FirebaseFirestore db) {
        return db.collection(FIRESTORE_TABLE).document();
    }

    public static DocumentReference getRef(FirebaseFirestore db, String listId) {
        return db.collection(FIRESTORE_TABLE).document(listId);
    }

    public static TransactionWrapper setAuthor(
            TransactionWrapper transaction, DocumentReference ref, String author) {

        return transaction.update(ref, FIRESTORE_FIELD_AUTHOR, author);
    }

    public static TransactionWrapper setTitle(
            TransactionWrapper transaction, DocumentReference ref, String title) {

        return transaction.update(ref, FIRESTORE_FIELD_TITLE, title);
    }

    public static TransactionWrapper addTask(
            TransactionWrapper transaction,
            DocumentReference ref,
            String userid,
            String title,
            String description
    ) {

        DocumentReference taskRef = ShopTaskActions.initRef(ref);
        ShopTaskActions.newTask(transaction, taskRef, userid, title, description);
        return transaction.addToList(ref, FIRESTORE_FIELD_TASKS, taskRef.getId());
    }

    public static TransactionWrapper addToken(
            TransactionWrapper transaction, DocumentReference ref, String token) {

        Date dateInWeek = new Date();
        dateInWeek.setTime(System.currentTimeMillis() + TIME_IN_A_WEEK_IN_MILIES_FACTOR);
        return transaction.addToMap(ref, FIRESTORE_FIELD_TOKENS, token, new Timestamp(dateInWeek));
    }

    public static TransactionWrapper removeToken(
            TransactionWrapper transaction, DocumentReference ref, String token) {

        return transaction.removeFromMap(ref, FIRESTORE_FIELD_TOKENS, token);
    }

    public static TransactionWrapper removeTask(
            TransactionWrapper transaction, DocumentReference listRef, DocumentReference taskRef) {

        ShopTaskActions.remove(transaction, taskRef);
        return transaction.removeFromList(listRef, FIRESTORE_FIELD_TASKS, taskRef.getId());
    }

    public static TransactionWrapper addCollaborator(
            TransactionWrapper transaction, DocumentReference listRef, String userId) {

        return transaction.addToList(listRef, FIRESTORE_FIELD_COLLABORATORS, userId);
    }


    public static TransactionWrapper removeCollaborators(
            TransactionWrapper transaction, DocumentReference listRef, String userId) {

        transaction.removeFromList(listRef, FIRESTORE_FIELD_COLLABORATORS, userId);
        return transaction;
    }

    public static TransactionWrapper createNewList(
            TransactionWrapper transaction,
            DocumentReference ref,
            String userId,
            String title
    ) {
        HashMap<String, Object> fields = new HashMap<>();
        fields.put(FIRESTORE_FIELD_AUTHOR, userId);
        fields.put(FIRESTORE_FIELD_TITLE, title);
        fields.put(FIRESTORE_FIELD_COLLABORATORS, new ArrayList<>());
        fields.put(FIRESTORE_FIELD_TOKENS, new HashMap<>());
        return transaction.create(ref, fields);
    }


    public static TransactionWrapper addCollaboratorData(
            TransactionWrapper transactionWrapper,
            DocumentReference listRef,
            String userId,
            String name,
            String email,
            String picture
    ) {
        DocumentReference collaboratorRef = CollaboratorActions.getRef(listRef, userId);
        CollaboratorActions.newCollaborator(
                transactionWrapper,
                collaboratorRef,
                name, email, picture);

        return transactionWrapper;
    }
}
