package app.shoppinglist.wsux.shoppinglist.firebase;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ImageReader;
import android.os.AsyncTask;
import android.util.DisplayMetrics;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;

public class ImageManager {

    private static final String FIREBASE_CACHE_DIR = "firebase";
    private static final String COLLABORATOR_FILE_PATH = "nodpi_user_picture_cache_%s";

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
        options.inDensity = DisplayMetrics.DENSITY_DEFAULT;
        return BitmapFactory.decodeFile(file.getAbsolutePath(), options);
    }

    void downloadPicture(BaseCollectionItem item, String pictureUrl) {
        new DownloadPicture(item, pictureUrl).execute("");
    }

    private File getPictureFile(BaseCollectionItem item) {
        if (item.getClass() == Collaborator.class) {
            return getPictureFile((Collaborator) item);

        } else if (item.getClass() == UserInfo.class) {
            return getPictureFile((UserInfo) item);

        } else {
            return null;
        }
    }

    private File getPictureFile(Collaborator collaborator) {
        return new File(getFireBaseCache(), String.format(COLLABORATOR_FILE_PATH, collaborator.getUserId()));
    }

    private File getPictureFile(UserInfo userInfo) {
        return new File(getFireBaseCache(), String.format(COLLABORATOR_FILE_PATH, userInfo.getUserId()));
    }

    class DownloadPicture extends AsyncTask<String, Void, String> {

        private final String RES_OK = "OK";
        private final String RES_FAIL = null;
        
        private BaseCollectionItem item;
        private String pictureUrl;
        private File file;

        DownloadPicture(BaseCollectionItem item, String pictureUrl) {
            super();
            this.item = item;
            this.pictureUrl = pictureUrl;

            file = getPictureFile(item);
        }
        
        private void downloadFile() throws IOException {
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
        }

        @Override
        protected String doInBackground(String... params) {

            if (file.exists()) {
                return RES_FAIL;
            }

            try {
                downloadFile();
            } catch (IOException e) {
                return RES_FAIL;
            }

            return RES_OK;
        }

        @Override
        protected void onPostExecute(String result) {
            item.reportMediaDownloaded();
        }
    }

}
