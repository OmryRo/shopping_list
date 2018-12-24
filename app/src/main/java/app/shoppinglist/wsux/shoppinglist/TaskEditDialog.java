package app.shoppinglist.wsux.shoppinglist;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;

import app.shoppinglist.wsux.shoppinglist.firebase.FireBaseManager;
import app.shoppinglist.wsux.shoppinglist.firebase.ShopTask;
import app.shoppinglist.wsux.shoppinglist.firebase.UploadManager;

public class TaskEditDialog extends Dialog implements View.OnClickListener,
        UploadManager.OnChooseMediaResultListener, UploadManager.OnUploadMediaResultListener {

    private static final String TAG = "TASK_EDIT_DIALOG";

    private EditText editNameEt;
    private EditText editNoteEt;
    private ImageView editThumbnailIv;
    private ShopTask shopTask;
    private FireBaseManager fireBaseManager;
    private UploadManager.ImageUpload imageUpload;
    private LinearLayout imageSelectorLayout;

    TaskEditDialog(Context context, ShopTask shopTask, FireBaseManager fireBaseManager) {
        super(context);

        this.shopTask = shopTask;
        this.fireBaseManager = fireBaseManager;

        setViews();
        updateViewsData();
    }

    private void setViews() {
        setContentView(R.layout.task_edit_dialog);

        editNameEt = findViewById(R.id.task_edit_name_tv);
        editNoteEt = findViewById(R.id.task_edit_note_tv);
        editThumbnailIv = findViewById(R.id.task_edit_image_iv);
        imageSelectorLayout = findViewById(R.id.task_edit_image_menu);

        findViewById(R.id.task_delete_ib).setOnClickListener(this);
        findViewById(R.id.task_edit_done_ib).setOnClickListener(this);
        findViewById(R.id.task_open_camera_ib).setOnClickListener(this);
        findViewById(R.id.task_open_gallery_ib).setOnClickListener(this);
        findViewById(R.id.task_edit_image_container).setOnClickListener(this);
        findViewById(R.id.task_delete_image_ib).setOnClickListener(this);

    }

    private void updateViewsData() {
        editNameEt.setText(shopTask.getTitle());
        editNoteEt.setText(shopTask.getDescription());
        updateImageViewData();
    }

    private void updateImageViewData() {

        Bitmap taskImage;
        if (imageUpload == null) {
            taskImage = shopTask.getPicture();
        } else {
            taskImage = imageUpload.getImagePreview();
        }

        if (taskImage == null) {
            editThumbnailIv.setImageResource(R.drawable.luncher_icon);
        } else {
            editThumbnailIv.setImageBitmap(taskImage);
        }

    }

    private void update() {

        // add more validations
        if (editNameEt.getText().toString().isEmpty()) {
            return;
        }

        shopTask.setTitle(editNameEt.getText().toString());
        shopTask.setDescription(editNoteEt.getText().toString());

        if (imageUpload != null) {
            imageUpload.uploadFile(shopTask, this);
        }

        this.dismiss();
    }

    private void delete() {

        // todo: we need to make sure it's not a misclick.

        shopTask.remove();
        this.dismiss();
    }

    private void openCamera() {
        fireBaseManager.getUploadManager().requestCamera(this);
        hideImageMenu();
    }

    private void openGallery() {
        fireBaseManager.getUploadManager().requestChoosePicture(this);
        hideImageMenu();
    }

    @Override
    public void onClick(View view) {
        Log.d(TAG, "onClick: "+ view.getId());
        switch (view.getId()) {
            case R.id.task_delete_image_ib:
                deleteImage();
                break;
            case R.id.task_delete_ib:
                delete();
                break;
            case R.id.task_edit_done_ib:
                update();
                break;
            case R.id.task_edit_image_container:
                toggleImageMenu();
                break;
            case R.id.task_open_camera_ib:
                openCamera();
                break;
            case R.id.task_open_gallery_ib:
                openGallery();
                break;
        }
    }

    private void deleteImage() {
        editThumbnailIv.setImageResource(R.drawable.luncher_icon);
        if (shopTask.hasPicture()) {
            shopTask.removeImage();
        }
        hideImageMenu();
    }

    private void toggleImageMenu() {
        boolean isVisible = imageSelectorLayout.getVisibility() == View.VISIBLE;
        imageSelectorLayout.setVisibility(isVisible ? View.GONE : View.VISIBLE);
    }

    private void hideImageMenu() {
        imageSelectorLayout.setVisibility(View.GONE);
    }

    @Override
    public void onSelectSuccess(UploadManager.ImageUpload image) {
        if (image == null || image.getImagePreview() == null) {
            return;
        }

        this.imageUpload = image;
        updateImageViewData();
    }

    @Override
    public void onSelectFailed(Exception e) {
        Log.e(TAG, "onSelectFailed: ", e);
    }

    @Override
    public void onUploadSuccess(String documentId) {
        // todo: upload progress
        this.dismiss();
    }

    @Override
    public void onUploadFailed(Exception e) {
        // todo: handle upload error.
        Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_SHORT).show();
    }
}
