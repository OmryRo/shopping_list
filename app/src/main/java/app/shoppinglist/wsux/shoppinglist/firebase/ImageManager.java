package app.shoppinglist.wsux.shoppinglist.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ImageReader;
import android.os.AsyncTask;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ImageManager {

    private static final String FIREBASE_CACHE_DIR = "firebase";
    private static final String COLLABORATOR_FILE_PATH = "user_picture_cache_%s";

    private Context context;
    private FireBaseManager fireBaseManager;

    ImageManager(Context context, FireBaseManager fireBaseManager) {
        this.context = context;
        this.fireBaseManager = fireBaseManager;
    }

    private File getFireBaseCache() {
        File firebaseCache = new File(context.getCacheDir(), FIREBASE_CACHE_DIR);

        if (!firebaseCache.exists()) {
            firebaseCache.mkdir();
        }

        return firebaseCache;
    }

    public Bitmap getPicture(BaseCollectionItem item) {
        File file = getPictureFile(item);

        if (!file.exists()) {
            return null;
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;
        options.inScaled = false;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    void downloadPicture(BaseCollectionItem item, String pictureUrl) {
        new DownloadPicture(item, pictureUrl).execute("");
    }

    private File getPictureFile(BaseCollectionItem item) {
        if (item.getClass() == Collaborator.class) {
            return getPictureFile((Collaborator) item);
        } else {
            return null;
        }
    }

    private File getPictureFile(Collaborator collaborator) {
        return new File(getFireBaseCache(), String.format(COLLABORATOR_FILE_PATH, collaborator.getUserId()));
    }

    class DownloadPicture extends AsyncTask<String, Void, String> {

        private BaseCollectionItem item;
        private String pictureUrl;
        private File file;

        DownloadPicture(BaseCollectionItem item, String pictureUrl) {
            super();
            this.item = item;
            this.pictureUrl = pictureUrl;

            file = getPictureFile(item);
        }

        @Override
        protected String doInBackground(String... params) {

            if (file.exists()) {
                return null;
            }

            try {

                URL url = new URL(pictureUrl);
                URLConnection urlConnection = url.openConnection();
                InputStream inputStream = urlConnection.getInputStream();
                FileOutputStream fileOutput = new FileOutputStream(file);
                byte[] buffer = new byte[1024];

                int bufferLength = 0;
                while ( (bufferLength = inputStream.read(buffer)) > 0 ) {
                    fileOutput.write(buffer, 0, bufferLength);
                }

                fileOutput.close();
                inputStream.close();

            } catch (IOException e) {
                return null;
            }

            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            item.reportChildChange();
        }
    }

}
