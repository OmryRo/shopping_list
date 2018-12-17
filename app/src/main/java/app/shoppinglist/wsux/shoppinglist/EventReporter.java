package app.shoppinglist.wsux.shoppinglist;

import android.app.Activity;
import android.support.v7.app.AppCompatActivity;

import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;

public class EventReporter implements FireBaseManager.FireBaseEventsInterface {

    private ProgressFragment progressScreen;

    public EventReporter(AppCompatActivity context) {
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

            case FireBaseManager.ON_SIGN_IN:
            case FireBaseManager.ON_SIGN_OUT:
            case FireBaseManager.ON_SIGN_ERR:
            case FireBaseManager.ON_USER_LIST_UPDATED:
            case FireBaseManager.ON_USER_INFO_UPDATED:
            case FireBaseManager.ON_USER_UPDATE_FAILED:
            case FireBaseManager.ON_LIST_CREATED:
            case FireBaseManager.ON_LIST_UPDATED:
            case FireBaseManager.ON_LIST_FAILURE:
            case FireBaseManager.ON_LIST_DELETED:
            case FireBaseManager.ON_TASK_CREATED:
            case FireBaseManager.ON_TASK_UPDATED:
            case FireBaseManager.ON_TASK_FAILURE:
            case FireBaseManager.ON_TASK_DELETED:
            case FireBaseManager.ON_COLLABORATOR_CREATED:
            case FireBaseManager.ON_COLLABORATOR_UPDATED:
            case FireBaseManager.ON_COLLABORATOR_FAILURE:
            case FireBaseManager.ON_COLLABORATOR_DELETED:
            case FireBaseManager.ON_SHARE_LIST_FOUND:
            case FireBaseManager.ON_LAST_LIST_DOWNLOADED:
            case FireBaseManager.ON_PICTURE_UPLOAD_SUCCESS:
            case FireBaseManager.ON_PICTURE_UPLOAD_FAILED:
            case FireBaseManager.ON_CAMERA_UPLOAD_SUCCESS:
            case FireBaseManager.ON_CAMERA_UPLOAD_FAILED:
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
}
