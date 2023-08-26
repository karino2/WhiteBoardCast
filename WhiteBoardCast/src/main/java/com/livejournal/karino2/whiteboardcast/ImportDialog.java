package com.livejournal.karino2.whiteboardcast;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;

import com.livejournal.karino2.multigallery.CacheEngine;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by karino on 12/3/13.
 */
public class ImportDialog extends ProgressDialog {
    public ImportDialog(Context context) {
        super(context);
    }

    boolean copying = false;
    ArrayList<File> imported = new ArrayList<>();
    List<File> getImportedFiles() { return imported; }

    private WorkFileStore fileStore = null;
    public WorkFileStore getFileStore() {
        if(fileStore == null)
            fileStore = new WorkFileStore(getContext());
        return fileStore;
    }

    private void copyImage(String path) {
        Uri imageUri = Uri.parse(path);
        try {
            File result = new File(getFileStore().getSlideListDirectory(), getNewSequentialFile());

            int resizeFactor = getResizeFactor(imageUri);
            BitmapFactory.Options options;

            options = new BitmapFactory.Options();
            options.inSampleSize = resizeFactor;

            InputStream is = getContentResolver().openInputStream(imageUri);
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
                // currently, always resize even though it sometime not necessary.
                saveWithResized(result, bitmap, screenWidth, screenHeight);
                File thumbnail = getFileStore().getThumbnailFile(result);
                saveWithResized(thumbnail, bitmap, screenWidth/6, screenHeight/6);
                // TODO: check whether this line is necessary
                // slideList.add(result);
            }finally {
                is.close();
            }
            imported.add(result);

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static Bitmap getThumbnailBitmap(WorkFileStore fileStore, File parent, CacheEngine cacheEngine) throws IOException {
        File thumbnail = fileStore.getThumbnailFile(parent);
        Bitmap bmp = cacheEngine.lookup(thumbnail.getAbsolutePath());
        if(bmp == null) {
            if(thumbnail.exists()) {
                bmp = BitmapFactory.decodeFile(thumbnail.getAbsolutePath());
            } else {
                Bitmap parentBmp = BitmapFactory.decodeFile(parent.getAbsolutePath());
                bmp = saveWithResized(thumbnail, parentBmp, parentBmp.getWidth()/6, parentBmp.getHeight()/6);
            }
        }
        cacheEngine.put(thumbnail.getAbsolutePath(), bmp);
        return bmp;
    }

    int calculateResizeFactor(int orgWidth, int orgHeight,
                              int limitWidth, int limitHeight) {
        int widthResizeFactor = Math.max(1, (orgWidth+limitWidth-1)/limitWidth);
        int heightResizeFactor = Math.max(1, (orgHeight+limitHeight-1)/limitHeight);
        int resizeFactor = Math.max(widthResizeFactor, heightResizeFactor);
        return resizeFactor;
    }




    private int getResizeFactor(Uri imageUri) throws IOException {
        InputStream is = getContentResolver().openInputStream(imageUri);
        try {
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(is, null, options);

            return calculateResizeFactor(options.outWidth, options.outHeight, screenWidth, screenHeight);
        }finally {
            is.close();
        }
    }

    public static Bitmap saveWithResized(File result, Bitmap bitmap, int scaleWidth, int scaleHeight) throws IOException {
        Bitmap resizedbitmap = Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, true);


        OutputStream stream = new FileOutputStream(result);
        resizedbitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
        stream.close();
        return resizedbitmap;
    }

    int sequenceId = 0;
    String getNewSequentialFile() {
        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
        return timeStampFormat.format(new Date()) + "_" + sequenceId++ +".png";
    }

    public interface FinishListener {
        void onFinish();
    }

    FinishListener finishListener;
    ContentResolver resolver;
    int screenWidth;
    int screenHeight;

    ContentResolver getContentResolver() { return resolver; }

    public void prepareCopy(ContentResolver resolver, int screenWidth, int screenHeight, final List<String> all_path, FinishListener onFinish) {
        finishListener = onFinish;
        this.resolver = resolver;
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        imported.clear();
        (new AsyncTask<Void, Integer, Void>(){
            @Override
            protected Void doInBackground(Void... params) {
                int count = 1;
                for(String path : all_path) {
                    publishProgress(count++);
                    copyImage(path);
                }
                copying = false;
                return null;
            }

            @Override
            protected void onProgressUpdate(Integer... values) {
                super.onProgressUpdate(values);
                ImportDialog.this.setMessage("Import file " + values[0]);
            }

            @Override
            protected void onPostExecute(Void aVoid) {
                super.onPostExecute(aVoid);
                ImportDialog.this.dismiss();
                finishListener.onFinish();
            }
        }).execute();
    }
}
