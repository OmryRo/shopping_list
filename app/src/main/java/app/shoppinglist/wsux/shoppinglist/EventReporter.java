package app.shoppinglist.wsux.shoppinglist;

import android.support.design.widget.Snackbar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;

public class EventReporter implements FireBaseManager.FireBaseEventsInterface {

    private ProgressFragment progressScreen;
    private AppCompatActivity context;
    private Snackbar lastSnackbar;

    public EventReporter(AppCompatActivity context) {
        this.context = context;
        progressScreen = (ProgressFragment) context.getSupportFragmentManager()
                .findFragmentById(R.id.progress_bar_fragment);
    }

    @Override
    public void onEventOccurred(int what, Object data, Exception e) {
        switch (what) {
            case FireBaseManager.ON_SIGN_IN_START:
                progressScreen.show(ProgressFragment.TYPE_SIGN_IN);
                break;

            case FireBaseManager.ON_SIGN_OUT_START:
                progressScreen.show(ProgressFragment.TYPE_SIGN_OUT);
                break;

            case FireBaseManager.ON_PROGRESS_START_UPDATE:
                progressScreen.show(ProgressFragment.TYPE_UPDATING);
                break;

            case FireBaseManager.ON_PROGRESS_START_DELETE:
                progressScreen.show(ProgressFragment.TYPE_DELETING);
                break;

            case FireBaseManager.ON_PROGRESS_START_CREATE:
                progressScreen.show(ProgressFragment.TYPE_CREATING);
                break;

            case FireBaseManager.ON_PROGRESS_START_LOAD:
                progressScreen.show(ProgressFragment.TYPE_LOADING);
                break;

            case FireBaseManager.ON_PROGRESS_START_QUIT:
                progressScreen.show(ProgressFragment.TYPE_QUITING);
                break;

            case FireBaseManager.ON_PROGRESS_START_DOWNLOAD:
            case FireBaseManager.ON_PICTURE_UPLOAD_STARTED:
            case FireBaseManager.ON_CAMERA_UPLOAD_STARTED:
                progressScreen.show(ProgressFragment.TYPE_UPLOADING);
                break;

            case FireBaseManager.ON_SIGN_ERR:
            case FireBaseManager.ON_USER_UPDATE_FAILED:
            case FireBaseManager.ON_LIST_CREATED:
            case FireBaseManager.ON_LIST_FAILURE:
            case FireBaseManager.ON_LIST_DELETED:
            case FireBaseManager.ON_TASK_FAILURE:
            case FireBaseManager.ON_COLLABORATOR_FAILURE:
            case FireBaseManager.ON_COLLABORATOR_DELETED:
            case FireBaseManager.ON_PICTURE_UPLOAD_FAILED:
            case FireBaseManager.ON_CAMERA_UPLOAD_FAILED:
            case FireBaseManager.ON_LIST_REMOVED_FROM:
                popSneakBar(what, data, e);

            case FireBaseManager.ON_SIGN_IN:
            case FireBaseManager.ON_SIGN_OUT:
            case FireBaseManager.ON_TASK_CREATED:
            case FireBaseManager.ON_TASK_DELETED:
            case FireBaseManager.ON_USER_LIST_UPDATED:
            case FireBaseManager.ON_USER_INFO_UPDATED:
            case FireBaseManager.ON_LIST_UPDATED:
            case FireBaseManager.ON_TASK_UPDATED:
            case FireBaseManager.ON_COLLABORATOR_CREATED:
            case FireBaseManager.ON_COLLABORATOR_UPDATED:
            case FireBaseManager.ON_SHARE_LIST_FOUND:
            case FireBaseManager.ON_LAST_LIST_DOWNLOADED:
            case FireBaseManager.ON_PICTURE_UPLOAD_SUCCESS:
            case FireBaseManager.ON_CAMERA_UPLOAD_SUCCESS:
                progressScreen.hide();
                break;

            case FireBaseManager.ON_SELECT_PICTURE_START:
            case FireBaseManager.ON_SELECT_PICTURE_SUCCESS:
            case FireBaseManager.ON_SELECT_PICTURE_FAILED:
            case FireBaseManager.ON_SELECT_CAMERA_START:
            case FireBaseManager.ON_SELECT_CAMERA_SUCCESS:
            case FireBaseManager.ON_SELECT_CAMERA_FAILED:
                break;
        }
    }

    private void popSneakBar(int what, Object data, Exception e) {
        Snackbar snackbar = null;

        switch (what) {
            case FireBaseManager.ON_SIGN_ERR:
                snackbar = createSneakBarOnSignErr(e);
                break;
            case FireBaseManager.ON_USER_UPDATE_FAILED:
                snackbar = createSneakBarOnUserUpdateFailure(e);
                break;
            case FireBaseManager.ON_LIST_FAILURE:
                snackbar = createSneakBarOnListFailure(e);
                break;
            case FireBaseManager.ON_TASK_CREATED:
                snackbar = createSneakBarOnTaskCreated();
                break;
            case FireBaseManager.ON_TASK_FAILURE:
                snackbar = createSneakBarOnTaskFailure(e);
                break;
            case FireBaseManager.ON_COLLABORATOR_FAILURE:
                snackbar = createSneakBarOnCollaboratorFailure(e);
                break;
            case FireBaseManager.ON_PICTURE_UPLOAD_FAILED:
                snackbar = createSneakBarOnPictureUploadFailed(e);
                break;
            case FireBaseManager.ON_CAMERA_UPLOAD_FAILED:
                snackbar = createSneakBarOnCameraUploadFailed(e);
                break;
            case FireBaseManager.ON_LIST_CREATED:
                snackbar = createSneakBarOnListCreated();
                break;
            case FireBaseManager.ON_LIST_DELETED:
                snackbar = createSneakBarOnListDeleted();
                break;
            case FireBaseManager.ON_TASK_DELETED:
                snackbar = createSneakBarOnTaskDeleted();
                break;
            case FireBaseManager.ON_COLLABORATOR_DELETED:
                snackbar = createSneakBarOnCollaboratorDeleted();
                break;
            case FireBaseManager.ON_LIST_REMOVED_FROM:
                snackbar = createSneakBarOnListRemovedFrom();
                break;
        }

        if (snackbar != null) {

            if (lastSnackbar != null) {
                lastSnackbar.dismiss();
            }

            snackbar.show();
        }
    }

    private Snackbar createSneakBarOnSignErr(Exception e) {
        return createSneakBarFailure(R.string.sign_in_failed, e);
    }

    private Snackbar createSneakBarOnUserUpdateFailure(Exception e) {
        return createSneakBarFailure(R.string.user_update_failed, e);
    }

    private Snackbar createSneakBarOnListFailure(Exception e) {
        return createSneakBarFailure(R.string.list_action_failed, e);
    }

    private Snackbar createSneakBarOnTaskCreated() {
        return createSneakBarSuccess(R.string.task_created);
    }

    private Snackbar createSneakBarOnTaskFailure(Exception e) {
        return createSneakBarFailure(R.string.task_action_failed, e);
    }

    private Snackbar createSneakBarOnCollaboratorFailure(Exception e) {
        return createSneakBarFailure(R.string.collaborator_action_failed, e);
    }

    private Snackbar createSneakBarOnPictureUploadFailed(Exception e) {
        return createSneakBarFailure(R.string.picture_upload_failed, e);
    }

    private Snackbar createSneakBarOnCameraUploadFailed(Exception e) {
        return createSneakBarFailure(R.string.camera_upload_failed, e);
    }

    private Snackbar createSneakBarOnListCreated() {
        return createSneakBarSuccess(R.string.list_created);
    }

    private Snackbar createSneakBarOnListDeleted() {
        return createSneakBarSuccess(R.string.list_deleted);
    }

    private Snackbar createSneakBarOnTaskDeleted() {
        return createSneakBarSuccess(R.string.task_deleted);
    }

    private Snackbar createSneakBarOnCollaboratorDeleted() {
        return createSneakBarSuccess(R.string.task_created);
    }

    private Snackbar createSneakBarOnListRemovedFrom() {
        return createSneakBarWarning(R.string.removed_from_list);
    }

    private Snackbar createSneakBarSuccess(int message) {
        Snackbar snackbar = Snackbar
                .make(context.findViewById(R.id.root_layout), message, Snackbar.LENGTH_SHORT);
        snackbar.getView().setBackgroundResource(R.color.sneakBackgroundColorSuccess);
        return snackbar;
    }

    private Snackbar createSneakBarWarning(int message) {
        Snackbar snackbar = Snackbar
                .make(context.findViewById(R.id.root_layout), message, Snackbar.LENGTH_SHORT);
        snackbar.getView().setBackgroundResource(R.color.sneakBackgroundColorWarning);
        return snackbar;
    }

    private Snackbar createSneakBarFailure(int message, final Exception e) {

        // the user can't do any thing with the information about the error message received.
        // the most he can get from it, is that the app is shitty and doesn't work.
        // so we should give him the impression the problem is with the network, that most likely
        // contributed to this error, so in this way the user will try to fix it on his side
        // instead of raging and uninstall.
        if (!BuildConfig.DEBUG) {
            message = R.string.communication_error;
        }

        Snackbar snackbar = Snackbar
                .make(context.findViewById(R.id.root_layout), message, Snackbar.LENGTH_SHORT);
        snackbar.getView().setBackgroundResource(R.color.sneakBackgroundColorError);

        // we don't want the user to see the errors, unless he's one of the developers.
        if (e != null && BuildConfig.DEBUG) {
            snackbar.setAction(context.getString(R.string.details_button), new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    new AlertDialog.Builder(context)
                            .setTitle(context.getString(R.string.error_message, e.getMessage()))
                            .create().show();
                }
            });
        }

        return snackbar;
    }
}
