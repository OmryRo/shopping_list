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

import app.shoppinglist.wsux.shoppinglist.firebase.db.TransactionWrapper;

public abstract class BaseCollectionItem implements
        EventListener<DocumentSnapshot>,
        TransactionWrapper.ResultListener,
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
        manager.reportEvent(FireBaseManager.ON_PROGRESS_START_UPDATE);

        HashMap<String, Object> params = new HashMap<>();
        params.put(field, data);
        return ref.update(params)
                .addOnSuccessListener(this).
                        addOnFailureListener(this);
    }

    public void onQueryError(DocumentSnapshot document, FirebaseFirestoreException e) {
        Log.e(TAG, String.format("onQueryError: %s", this), e);
        this.isReady = false;
    }

    public void onNotFound(DocumentSnapshot document) {
        Log.e(TAG, String.format("onEvent: not exists: %s", this));
        this.isReady = false;
    }

    @Override
    public void onEvent(@Nullable DocumentSnapshot document, @Nullable FirebaseFirestoreException e) {
        Log.d(TAG, "onEvent: " + this);
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
    public void onSuccess(Void eVoid) {
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
