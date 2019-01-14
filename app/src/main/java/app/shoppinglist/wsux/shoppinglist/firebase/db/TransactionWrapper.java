package app.shoppinglist.wsux.shoppinglist.firebase.db;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Transaction;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

public class TransactionWrapper {

    private FirebaseFirestore db;
    private ResultListener listener;
    private LinkedList<Operation> operations;


    public TransactionWrapper(FirebaseFirestore db, ResultListener listener) {
        this.db = db;
        this.listener = listener;
        this.operations = new LinkedList<>();
    }

    public void apply() {

        TransactionCallbacks transactionCallbacks = new TransactionCallbacks();
        db.runTransaction(transactionCallbacks)
                .addOnSuccessListener(transactionCallbacks)
                .addOnFailureListener(transactionCallbacks);

    }

    TransactionWrapper create(DocumentReference reference, Map<String, Object> parameters) {
        operations.add(new CreateOperation(reference, parameters));
        return this;
    }

    TransactionWrapper update(DocumentReference reference, String key, Object value) {

        Operation lastOperation = operations.size() == 0 ? null : operations.getLast();

        if (lastOperation instanceof UpdateOperation && lastOperation.ref == reference) {

            ((UpdateOperation) lastOperation).addParameter(key, value);

        } else {
            operations.add(new UpdateOperation(reference, key, value));
        }

        return this;
    }

    TransactionWrapper addToList(DocumentReference reference, String key, Object value) {
        return update(reference, key, FieldValue.arrayUnion(value));
    }

    TransactionWrapper removeFromList(DocumentReference reference, String key, Object value) {
        return update(reference, key, FieldValue.arrayRemove(value));
    }

    TransactionWrapper addToMap(DocumentReference reference, String key, String field, Object value) {
        return update(reference, String.format("%s.%s", key, field), value);
    }

    TransactionWrapper removeFromMap(DocumentReference reference, String key, String field) {
        return removeKey(reference, String.format("%s.%s", key, field));
    }

    TransactionWrapper removeKey(DocumentReference reference, String key) {
        return update(reference, key, FieldValue.delete());
    }

    TransactionWrapper delete(DocumentReference reference) {
        Log.d("TESTTESTETEST", "delete: " +reference.getPath());
        operations.add(new DeleteOperation(reference));
        return this;
    }

    private Object runTransaction(Transaction transaction) throws FirebaseFirestoreException {

        Log.d("TESTTESTETEST", "runTransaction: starts");

        for (Operation operation : operations) {
            Log.d("TESTTESTETEST", "runTransaction: prepare " + operation.getClass().toString() + " " + operation.ref.getPath());
            transaction = operation.prepare(transaction);
        }

        for (Operation operation : operations) {
            Log.d("TESTTESTETEST", "runTransaction: execute " + operation.getClass().toString() + " " + operation.ref.getPath());
            transaction = operation.execute(transaction);
        }

        Log.d("TESTTESTETEST", "runTransaction: ends");

        return null;
    }

    private abstract class Operation {
        public DocumentReference ref;
        protected Operation(DocumentReference ref) {
            this.ref = ref;
        }

        public Transaction prepare(Transaction transaction) throws FirebaseFirestoreException {
            return transaction;
        }

        public abstract Transaction execute(Transaction transaction) throws FirebaseFirestoreException;
    }

    private class CreateOperation extends Operation {
        private Map<String, Object> parameters;

        private CreateOperation(DocumentReference ref, Map<String, Object> parameters) {
            super(ref);
            this.parameters = parameters;
        }

        @Override
        public Transaction execute(Transaction transaction) throws FirebaseFirestoreException {
            return transaction.set(ref, parameters);
        }
    }

    private class UpdateOperation extends Operation {
        private Map<String, Object> parameters;

        private UpdateOperation(DocumentReference ref, String key, Object value) {
            super(ref);
            parameters = new HashMap<>();
            addParameter(key, value);
        }

        private void addParameter(String key, Object value) {
            parameters.put(key, value);
        }

        @Override
        public Transaction execute(Transaction transaction) throws FirebaseFirestoreException {
            return transaction.update(ref, parameters);
        }
    }

    private class DeleteOperation extends Operation {

        private boolean exists = false;

        private DeleteOperation(DocumentReference ref) {
            super(ref);
        }

        @Override
        public Transaction prepare(Transaction transaction) throws FirebaseFirestoreException {

            // if not exist anymore, don't do anything.
            // it may throw FirebaesFirestoreException if you don't have premission
            //      to remove, and we would like to stop the action if such exception throws.
            Log.d("TESTTESTETEST", "prepare: " + ref.getPath());
            if (transaction.get(ref).exists()) {
                Log.d("TESTTESTETEST", "prepare: exists!");
                exists = true;
            }
            Log.d("TESTTESTETEST", "prepare: not exists!");

            return transaction;
        }

        @Override
        public Transaction execute(Transaction transaction) throws FirebaseFirestoreException {

            try {
                if (exists) {
                    Log.d("TESTTESTETEST", "execute: trying to remove");
                    transaction.delete(ref);
                }
            } catch (Exception e) {
                Log.e("TESTTESTETEST", "execute: ", e);
                throw e;
            }

            return transaction;
        }
    }

    /**
     * private wrapper to prevent caling the interface functions from outside.
     */
    private class TransactionCallbacks implements Transaction.Function<Object>,
            OnSuccessListener<Object>, OnFailureListener {

        @Nullable
        @Override
        public Object apply(@NonNull Transaction transaction) throws FirebaseFirestoreException {
            return runTransaction(transaction);
        }

        @Override
        public void onSuccess(Object o) {
            listener.onSuccess();
        }

        @Override
        public void onFailure(@NonNull Exception e) {
            listener.onFailure(e);
        }
    }

    public interface ResultListener {
        void onSuccess();

        void onFailure(Exception e);
    }

}
