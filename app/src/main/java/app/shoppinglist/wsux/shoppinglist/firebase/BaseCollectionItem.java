package app.shoppinglist.wsux.shoppinglist.firebase;

import android.support.annotation.NonNull;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;

import javax.annotation.Nullable;

public abstract class BaseCollectionItem implements
        EventListener<DocumentSnapshot>,
        OnSuccessListener<Void>,
        OnFailureListener {

    private static final String TAG = "COLLECTION_ITEM";
    private boolean isReady = false;
    protected OnChangeListener onChangeListener;
    protected OnActionListener onActionListener;
    protected OnChildChangeListener onChildChangeListener;
    protected OnMediaDownload onMediaDownload;
    protected FireBaseManager manager;

    abstract void specificOnEvent(DocumentSnapshot document);

    BaseCollectionItem(FireBaseManager manager) {
        this.manager = manager;
    }

    protected Task<Void> updateField(DocumentReference ref, String field, Object data) {
        HashMap<String, Object> params = new HashMap<>();
        params.put(field, data);
        return ref.update(params)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    protected Task<Void> appendToList(DocumentReference ref, String field, Object data) {
        return ref.update(field, FieldValue.arrayUnion(data))
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    protected Task<Void> removeFromList(DocumentReference ref, String field, Object data) {
        return ref.update(field, FieldValue.arrayRemove(data))
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    protected Task<Void> addToMap(DocumentReference ref, String field, String key, Object data) {
        return ref.update(String.format("%s.%s", field, key), data)
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    protected Task<Void> removeFromMap(DocumentReference ref, String field, String key) {
        return ref.update(String.format("%s.%s", field, key), FieldValue.delete())
                .addOnSuccessListener(this)
                .addOnFailureListener(this);
    }

    public void onQueryError(DocumentSnapshot document, FirebaseFirestoreException e) {
        Log.e(TAG, "onQueryError: ", e);
    }
    public void onNotFound(DocumentSnapshot document) {
        Log.e(TAG, "onEvent: not exists");
    }

    @Override
    public void onEvent(@Nullable DocumentSnapshot document, @Nullable FirebaseFirestoreException e) {
        if (e != null) {
            onQueryError(document, e);
            return;
        }

        if (!document.exists()) {
            onNotFound(document);
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

    public void setOnMediaDownload(OnMediaDownload onMediaDownload) {
        this.onMediaDownload = onMediaDownload;
        if (onMediaDownload != null) {
            onMediaDownload.onMediaDownload();
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
            onChildChangeListener.onChildChange();
        }
    }

    public void removeAllListeners() {
        onMediaDownload = null;
        onActionListener = null;
        onChangeListener = null;
        onChildChangeListener = null;
    }

    protected void reportOnChange() {
        if (onChangeListener != null) {
            onChangeListener.onChange();
        }
    }

    void reportChildChange() {
        if (onChildChangeListener != null) {
            onChildChangeListener.onChildChange();
        }
    }

    void reportMediaDownloaded() {
        if (onMediaDownload != null) {
            onMediaDownload.onMediaDownload();
        }
    }

    public boolean isReady() {
        return isReady;
    }

    protected void setReady() {
        isReady = true;
        reportOnChange();
    }

    protected void setNotReady() {
        isReady = false;
    }

    public interface OnChildChangeListener {
        void onChildChange();
    }

    public interface OnChangeListener {
        void onChange();
    }

    public interface OnActionListener {
        void onActionSuccess();
        void onActionFailed();
    }

    public interface OnMediaDownload {
        void onMediaDownload();
    }
}
