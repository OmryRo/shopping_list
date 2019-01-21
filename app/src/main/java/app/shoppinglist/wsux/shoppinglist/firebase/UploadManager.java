package app.shoppinglist.wsux.shoppinglist.firebase;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Environment;
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

    private FireBaseManager manager;
    private FirebaseStorage storage;
    private File currentFileImage;

    private OnChooseMediaResultListener resultListener;

    UploadManager(FireBaseManager manager) {
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
        manager.getAppContext().startActivityForResult(createChoosePictureIntent(), FireBaseManager.RC_FILE);
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

        try {
            currentFileImage = createImageFile();
        } catch (IOException e) {
            Log.e(TAG, "requestCamera: error cccurred while create the file", e);
            this.resultListener = null;
            return false;
        }

        Intent cameraIntent = createCameraIntent();
        if (cameraIntent == null) {
            return false;
        }

        manager.getAppContext().startActivityForResult(cameraIntent, FireBaseManager.RC_CAMERA);
        return true;
    }

    private Intent createCameraIntent(){
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Context context = manager.getAppContext();

        if (cameraIntent.resolveActivity(context.getPackageManager()) == null) {
            this.resultListener = null;
            return null;
        }

        final String authority = context.getPackageName() + ".fileprovider";
        Uri photoURI = FileProvider.getUriForFile(context, authority, currentFileImage);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
        return cameraIntent;
    }
    private File createImageFile() throws IOException {
        File cacheDir = manager.getAppContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        return File.createTempFile(CAMERA_TMP_NAME, ".jpg", cacheDir);
    }

    public void onRequestCameraPermissionsResult(String permissions[], int[] grantResults) {
        OnChooseMediaResultListener listener = this.resultListener;
        this.resultListener = null;

        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestCamera(listener);
        }
    }

    private boolean requestCameraPermissions() {
        Activity context = manager.getAppContext();
        if (ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {

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
        return Intent.createChooser(intent, manager.getAppContext().getString(R.string.request_picture));
    }

    private OnChooseMediaResultListener getMediaResultListener() {
        OnChooseMediaResultListener listener = this.resultListener;
        this.resultListener = null;
        return listener;
    }

    private boolean validateMediaRequestResult(int resultCode, Intent data) {
        return resultCode == Activity.RESULT_OK && data != null && data.getData() != null;
    }

    void onCameraRequestResult(int resultCode, Intent data) {

        OnChooseMediaResultListener listener = getMediaResultListener();
        if (listener == null) {
            return;
        }

        if (resultCode == Activity.RESULT_OK) {
            listener.onSelectSuccess(new ImageUpload(Uri.fromFile(currentFileImage)));

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

    private static int getDegree(int orientation) {
        int degrees = 0;
        switch (orientation){
            case ExifInterface.ORIENTATION_ROTATE_90:
                degrees = 90;
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                degrees = 180;
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                degrees = 270;
                break;
        }
        return degrees;
    }

    private static Bitmap rotateImage(Bitmap img, int orientation) {

        int degree = getDegree(orientation);
        if (degree == 0) {
            return img;
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        return Bitmap.createBitmap(
                img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
    }

    public class ImageUpload {
        private Bitmap bitmap;

        ImageUpload(Uri path) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(
                        manager.getAppContext().getContentResolver(), path);
                if (bitmap != null) {
                    this.bitmap = reScale(bitmap);
                }

            } catch (IOException e) {
                Log.e(TAG, "ImageUpload: ", e);
            }
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

            Bitmap image = Bitmap.createScaledBitmap(bitmap, width, height, true);

            return currentFileImage == null ? image : rotate(image);
        }

        private Bitmap rotate(Bitmap img)
        {
            ExifInterface exit = null;
            try {
                exit = new ExifInterface(currentFileImage.getAbsolutePath());
            } catch (IOException e) {
                Log.e(TAG, "rotate image error", e);
                return img;
            }

            int orientation = exit.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            return rotateImage(img, orientation);
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
