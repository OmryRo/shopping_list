package app.shoppinglist.wsux.shoppinglist.firebase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;

import java.util.HashMap;

import javax.annotation.Nullable;

public abstract class BaseCollectionItem implements
        EventListener<DocumentSnapshot>,
        OnSuccessListener<Void>,
        OnFailureListener {

    private static final String TAG = "COLLECTION_ITEM";
    protected OnChangeListener onChangeListener;
    protected OnActionListener onActionListener;
    protected OnChildChangeListener onChildChangeListener;
    protected FireBaseManager manager;

    abstract void specificOnEvent(DocumentSnapshot document);

    BaseCollectionItem(FireBaseManager manager) {
        this.manager = manager;
    }

    protected Task<Void> updateField(DocumentReference ref, String field, Object data) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(field, data);
        return ref.update(params).addOnSuccessListener(this).addOnFailureListener(this);
    }

    @Override
    public void onEvent(@Nullable DocumentSnapshot document, @Nullable FirebaseFirestoreException e) {
        if (e != null) {
            Log.e(TAG, "onEvent: ", e);
            return;
        }

        if (!document.exists()) {
            Log.e(TAG, "onEvent: not exists");
            return;
        }

        specificOnEvent(document);

    }

    abstract void specificOnSuccess();

    @Override
    public void onSuccess(Void aVoid) {
        specificOnSuccess();

        if (onActionListener != null) {
            onActionListener.onActionSuccess();
        }
    }

    abstract void specificOnFailure(Exception e);

    @Override
    public void onFailure(@NonNull Exception e) {
        specificOnFailure(e);

        if (onActionListener != null) {
            onActionListener.onActionFailed();
        }
    }

    public void setOnActionListener(OnActionListener onActionListener) {
        this.onActionListener = onActionListener;
    }

    public void setOnChangeListener(OnChangeListener onChangeListener) {
        this.onChangeListener = onChangeListener;
        if (onChangeListener != null) {
            onChangeListener.onChange();
        }
    }

    public void setOnChildChangeListener(OnChildChangeListener onChildChangeListener) {
        this.onChildChangeListener = onChildChangeListener;
        if (onChildChangeListener != null) {
            onChildChangeListener.onChange();
        }
    }

    public void reportChildChange() {
        if (onChildChangeListener != null) {
            onChildChangeListener.onChange();
        }
    }

    public interface OnChildChangeListener {
        void onChange();
    }

    public interface OnChangeListener {
        void onChange();
    }

    public interface OnActionListener {
        void onActionSuccess();
        void onActionFailed();
    }
}
