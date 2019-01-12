package app.shoppinglist.wsux.shoppinglist.firebase.db;

import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class UserInfoActions {
    public static final String FIRESTORE_TABLE = "dev.users";
    public static final String FIRESTORE_FIELD_LISTS = "lists";
    public static final String FIRESTORE_FIELD_TOKENS = "tokens";
    public static final String FIRESTORE_FIELD_LAST_LIST = "last_list";

    public static DocumentReference getRef(FirebaseFirestore db, String userId) {
        return db.collection(FIRESTORE_TABLE).document(userId);
    }

    public static TransactionWrapper addToken(
            TransactionWrapper transaction, DocumentReference ref, String listId, String token) {

        return transaction.addToMap(ref, FIRESTORE_FIELD_TOKENS, listId, token);
    }

    public static TransactionWrapper addKnownList(
            TransactionWrapper transaction, DocumentReference ref, String listId) {

        return transaction.addToList(ref, FIRESTORE_FIELD_LISTS, listId);
    }

    public static TransactionWrapper removeKnownList(
            TransactionWrapper transaction, DocumentReference ref, String listId) {

        return transaction.removeFromList(ref, FIRESTORE_FIELD_LISTS, listId);
    }

    public static TransactionWrapper removeToken(
            TransactionWrapper transaction, DocumentReference ref, String listId) {

        return transaction.removeFromMap(ref, FIRESTORE_FIELD_TOKENS, listId);
    }

    public static TransactionWrapper setLastList(
            TransactionWrapper transaction, DocumentReference ref, String lastList) {

        return transaction.update(ref, FIRESTORE_FIELD_LAST_LIST, lastList);
    }

    public static TransactionWrapper clearLastList(
            TransactionWrapper transaction, DocumentReference ref) {

        return transaction.removeKey(ref, FIRESTORE_FIELD_LAST_LIST);
    }

    public static TransactionWrapper initUser(
            TransactionWrapper transaction, DocumentReference ref) {

        ArrayList<Object> listNames = new ArrayList<>();
        HashMap<Object, Object> tokens = new HashMap<>();

        Map<String, Object> params = new HashMap<>();
        params.put(UserInfoActions.FIRESTORE_FIELD_LISTS, listNames);
        params.put(UserInfoActions.FIRESTORE_FIELD_TOKENS, tokens);

        return transaction.create(ref, params);
    }
}
