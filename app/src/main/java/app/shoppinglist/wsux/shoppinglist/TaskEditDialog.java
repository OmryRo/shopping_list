package app.shoppinglist.wsux.shoppinglist;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;

import app.shoppinglist.wsux.shoppinglist.firebase.ShopTask;

public class TaskEditDialog extends Dialog implements View.OnClickListener {

    private EditText editNameEt;
    private EditText editNoteEt;
    private ImageView editThumbnailIv;
    private ShopTask shopTask;


    TaskEditDialog(Context context, ShopTask shopTask) {
        super(context);
        setContentView(R.layout.task_edit_dialog);
        this.shopTask = shopTask;

        editNameEt = findViewById(R.id.task_edit_name_tv);
        editNoteEt = findViewById(R.id.task_edit_note_tv);
        editThumbnailIv = findViewById(R.id.task_image_iv);

        findViewById(R.id.task_delete_ib).setOnClickListener(this);
        findViewById(R.id.task_edit_done_ib).setOnClickListener(this);
        findViewById(R.id.task_open_camera_ib).setOnClickListener(this);
        findViewById(R.id.task_open_gallery_ib).setOnClickListener(this);

        updateViews();
    }

    private void updateViews() {
        editNameEt.setText(shopTask.getTitle());
        editNoteEt.setText(shopTask.getDescription());
    }

    private void update() {

        // add more validations
        if (editNameEt.getText().toString().isEmpty()) {
            return;
        }

        shopTask.setTitle(editNameEt.getText().toString());
        shopTask.setDescription(editNoteEt.getText().toString());

        this.dismiss();
    }

    private void delete() {

        // todo: we need to make sure it's not a misclick.

        shopTask.remove();
        this.dismiss();
    }

    private void openCamera() {
        // todo: implement
    }

    private void openGallery() {
        // todo: implement
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.task_delete_ib:
                delete();
                break;
            case R.id.task_edit_done_ib:
                update();
                break;
            case R.id.task_open_camera_ib:
                openCamera();
                break;
            case R.id.task_open_gallery_ib:
                openGallery();
                break;
        }
    }
}
