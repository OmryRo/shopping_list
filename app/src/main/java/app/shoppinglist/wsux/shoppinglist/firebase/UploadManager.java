package app.shoppinglist.wsux.shoppinglist.firebase;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.util.Log;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import app.shoppinglist.wsux.shoppinglist.R;

/**
 * this module usage:
 * fireBaseManager.getUploadManager().requestCamera()
 * for starting activity to take a photo
 * fireBaseManager.getUploadManager().requestChoosePicture()
 * for starting activity to take a photo from the gallery.
 * <p>
 * you have to call one of them using the interface OnChooseMediaResultListener
 * by using this interface, if the camera activity or gallery retrived a picture
 * you will get an object of ImageUpload.
 * you can choose to use getImagePreview() to show the picture on Imageview,
 * or to uploadFile, and giving the ShopTask object and an interface so you will
 * know when the upload done.
 */
public class UploadManager {

    private static final String TAG = "UPLOAD_MANAGER";
    private static final String CAMERA_TMP_NAME = ".camerapicture";
    private static final int MAX_WIDTH = 800;
    private static final int MAX_HEIGHT = 800;
    private static final int JPEG_COMPRESS = 70;
    private static final String CAMERA_INTENT_EXTRA_DATA = "data";

    private Activity context;
    private FireBaseManager manager;
    private FirebaseStorage storage;
    private String currentPhotoPath;

    private OnChooseMediaResultListener resultListener;

    UploadManager(Activity context, FireBaseManager manager) {
        this.context = context;
        this.manager = manager;
        this.storage = FirebaseStorage.getInstance();
    }

    private StorageReference getStorageReference(ShopTask shopTask) {
        return storage.getReference(
                String.format(
                        "%s/%s",
                        shopTask.getInList().getListId(),
                        shopTask.getTaskId()
                )
        );
    }

    public void deleteImage(ShopTask shopTask) {
        StorageReference storageRef = getStorageReference(shopTask);
        storageRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                Log.d(TAG, "onSuccess: ");
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: ");
            }
        });


    }

    public boolean requestChoosePicture(OnChooseMediaResultListener resultListener) {
        if (this.resultListener != null) {
            return false;
        }

        this.resultListener = resultListener;
        context.startActivityForResult(createChoosePictureIntent(), FireBaseManager.RC_FILE);
        return true;
    }

    public boolean requestCamera(OnChooseMediaResultListener resultListener) {
        if (this.resultListener != null) {
            return false;
        }

        this.resultListener = resultListener;

        if (!requestCameraPermissions()) {
            return false;
        }

        // todo: currently this function will get a thumbnail size camera picture. which is
        //  not what we are want to do... we have to improve this intent by reserve file location
        //  for saving the picture. for now what we have here is enough.
        //  read more at:
        //      https://stackoverflow.com/questions/47179862/how-to-get-full-size-image-on-camera-intent
        //      https://developer.android.com/training/camera/photobasics
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (cameraIntent.resolveActivity(context.getPackageManager()) == null) {
            this.resultListener = null;
            return false;
        }
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Log.d(TAG, "requestCamera: error cccurred while create the file" + ex.getMessage());
        }

        if (photoFile == null) {
            return false;
        }
        Uri photoURI = FileProvider.getUriForFile(context,
                "app.shoppinglist.wsux.shoppinglist.firebase",
                photoFile);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        context.startActivityForResult(cameraIntent, FireBaseManager.RC_CAMERA);
        return true;
    }

    private File createImageFile() throws IOException {
        File cacheDir = context.getCacheDir();
        File image = File.createTempFile(CAMERA_TMP_NAME, ".jpg", cacheDir);

        // Save a file: path for use with ACTION_VIEW intents
        this.currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    public void onRequestCameraPermissionsResult(String permissions[], int[] grantResults) {
        OnChooseMediaResultListener listener = this.resultListener;
        this.resultListener = null;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestCamera(listener);
        }
    }

    private boolean requestCameraPermissions() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    context,
                    new String[]{Manifest.permission.CAMERA},
                    FireBaseManager.RC_CAMERA_PERMISSION
            );
        } else {
            return true;
        }

        return false;
    }

    private Intent createChoosePictureIntent() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        return Intent.createChooser(intent, context.getString(R.string.request_picture));
    }

    private OnChooseMediaResultListener getMediaResultListener() {
        OnChooseMediaResultListener listener = this.resultListener;
        this.resultListener = null;
        return listener;
    }

    private boolean validateMediaRequestResult(int resultCode, Intent data) {
        return resultCode == Activity.RESULT_OK && data != null && data.getData() != null;
    }

    private boolean validateCameraRequestResult(int resultCode, Intent data) {
        return resultCode == Activity.RESULT_OK && data != null &&
                data.getExtras() != null && data.getExtras().containsKey(CAMERA_INTENT_EXTRA_DATA);
    }

    void onCameraRequestResult(int resultCode, Intent data) {

        OnChooseMediaResultListener listener = getMediaResultListener();
        if (listener == null) {
            return;
        }

        if (validateCameraRequestResult(resultCode, data)) {
            try {
                File file = new File(currentPhotoPath);

                Bitmap bitmap = MediaStore.Images.Media.
                        getBitmap(context.getContentResolver(), Uri.fromFile(file));
                listener.onSelectSuccess(new ImageUpload((Bitmap) data.getExtras().get(CAMERA_INTENT_EXTRA_DATA)));
            } catch (Exception ex) {
                Log.d(TAG, "onCameraRequestResult: "+ex.getMessage());
            }
        } else {
            listener.onSelectFailed(null);
        }

    }

    void onPictureRequestResult(int resultCode, Intent data) {
        OnChooseMediaResultListener listener = getMediaResultListener();
        if (listener == null) {
            return;
        }

        if (validateMediaRequestResult(resultCode, data)) {
            listener.onSelectSuccess(new ImageUpload(data.getData()));
        } else {
            listener.onSelectFailed(null);
        }
    }

    public class ImageUpload {
        private Bitmap bitmap;

        ImageUpload(Uri path) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(context.getContentResolver(), path);
                if (bitmap != null) {
                    this.bitmap = reScale(bitmap);
                }

            } catch (IOException e) {
                Log.e(TAG, "ImageUpload: ", e);
            }
        }

        ImageUpload(Bitmap bitmap) {
            this.bitmap = reScale(bitmap);
        }

        public Bitmap getImagePreview() {
            return bitmap;
        }

        public void uploadFile(final ShopTask shopTask, final OnUploadMediaResultListener listener) {

            manager.reportEvent(FireBaseManager.ON_PICTURE_UPLOAD_STARTED, shopTask);

            Bitmap bitmap = getImagePreview();
            if (bitmap == null) {
                return;
            }

            bitmap = reScale(bitmap);
            byte[] imageBytes = toByteArray(bitmap);

            final StorageReference ref = getStorageReference(shopTask);
            ref.putBytes(imageBytes).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    ref.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                        @Override
                        public void onSuccess(Uri uri) {
                            shopTask.setImageUrl(uri.toString());
                            listener.onUploadSuccess(uri.toString());
                        }
                    });
                }
            })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            listener.onUploadFailed(e);
                        }
                    });
        }

        private Bitmap reScale(Bitmap bitmap) {
            float ratio = Math.min(
                    ((float) MAX_WIDTH) / bitmap.getWidth(),
                    ((float) MAX_HEIGHT) / bitmap.getHeight()
            );

            int width = Math.round(ratio * bitmap.getWidth());
            int height = Math.round(ratio * bitmap.getHeight());

            return Bitmap.createScaledBitmap(bitmap, width, height, true);
        }

        private byte[] toByteArray(Bitmap bitmap) {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_COMPRESS, stream);
            return stream.toByteArray();
        }
    }

    public interface OnChooseMediaResultListener {
        void onSelectSuccess(ImageUpload image);

        void onSelectFailed(Exception e);
    }

    public interface OnUploadMediaResultListener {
        void onUploadSuccess(String documentId);

        void onUploadFailed(Exception e);
    }
}
