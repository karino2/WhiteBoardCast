package com.livejournal.karino2.multigallery;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.ThumbnailUtils;
import android.util.FloatMath;
import android.util.Log;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
* Created by karino on 11/24/13.
*/
class MediaLoadRequest implements Runnable {
    interface MediaLoadListener {
        void onThumbnailComing(Bitmap thumbnail);
    }

    MediaLoadListener listener;
    MediaItem item;
    int thumbnailSize;
    MediaLoadRequest(MediaItem item, MediaLoadListener listener, int thumbnailSize) {
        this.listener = listener;
        this.item = item;
        this.thumbnailSize = thumbnailSize;
    }

    void discard() {
        synchronized(listener) {
            listener = null;
        }
    }



    @Override
    public void run() {

        Bitmap thumbnail  = decodeThumbnail(item.getPath(), thumbnailSize);


        MediaLoadListener hd = listener;
        if(hd != null) {
            synchronized (hd) {
                if(listener == null)
                    return;
                listener.onThumbnailComing(thumbnail);
            }
        }
    }

    static final String TAG = "WhiteBoardCast";

    private /* static */ Bitmap decodeThumbnail(String path, int size) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(path);
            FileDescriptor fd = fis.getFD();
            return decodeThumbnailFromFD(fd, size);
        } catch (FileNotFoundException e) {
            Log.d(TAG, "FileNotFound on decodeThumbnail: " + e.getMessage());
            return null;
        } catch (IOException e) {
            Log.d(TAG, "IOException on decodeThumbnail: " + e.getMessage());
            return null;
        } finally {
            if(fis != null){
                try {
                    fis.close();
                } catch (IOException e) {
                    Log.d(TAG, "Close fail on decodeThumbnail: " + e.getMessage());
                }
            }
        }

    }


    public /* static */ int prevPowerOf2(int n) {
        if (n <= 0) throw new IllegalArgumentException();
        return Integer.highestOneBit(n);
    }

    public  /* static */ int computeSampleSizeLarger(float scale) {
        int initialSize = (int) FloatMath.floor(1f / scale);
        if (initialSize <= 1) return 1;

        return initialSize <= 8
                ? prevPowerOf2(initialSize)
                : initialSize / 8 * 8;
    }

    public /* static */ int nextPowerOf2(int n) {
        if (n <= 0 || n > (1 << 30)) throw new IllegalArgumentException("n is invalid: " + n);
        n -= 1;
        n |= n >> 16;
        n |= n >> 8;
        n |= n >> 4;
        n |= n >> 2;
        n |= n >> 1;
        return n + 1;
    }


    public /* static */ int computeSampleSize(float scale) {
        int initialSize = Math.max(1, (int) FloatMath.ceil(1 / scale));
        return initialSize <= 8
                ? nextPowerOf2(initialSize)
                : (initialSize + 7) / 8 * 8;
    }

    private /* static */ Bitmap.Config getConfig(Bitmap bitmap) {
        Bitmap.Config config = bitmap.getConfig();
        if (config == null) {
            config = Bitmap.Config.ARGB_8888;
        }
        return config;
    }


    public /* static */ Bitmap resizeBitmapByScale(
            Bitmap bitmap, float scale) {
        int width = Math.round(bitmap.getWidth() * scale);
        int height = Math.round(bitmap.getHeight() * scale);
        if (width == bitmap.getWidth()
                && height == bitmap.getHeight()) return bitmap;
        Bitmap target = Bitmap.createBitmap(width, height, getConfig(bitmap));
        Canvas canvas = new Canvas(target);
        canvas.scale(scale, scale);
        Paint paint = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.DITHER_FLAG);
        canvas.drawBitmap(bitmap, 0, 0, paint);
        bitmap.recycle();
        return target;
    }


    private /* static */ Bitmap decodeThumbnailFromFD(FileDescriptor fd, int size) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inPreferredConfig = Bitmap.Config.ARGB_8888;

        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFileDescriptor(fd, null, options);

        int w = options.outWidth;
        int h = options.outHeight;

        float scale = (float) size / Math.min(w, h);
        options.inSampleSize = computeSampleSizeLarger(scale);

        final int MAX_PIXEL_COUNT = 640000; // 400 x 1600
        if ((w / options.inSampleSize) * (h / options.inSampleSize) > MAX_PIXEL_COUNT) {
            options.inSampleSize = computeSampleSize(
                    FloatMath.sqrt((float) MAX_PIXEL_COUNT / (w * h)));
        }

        options.inJustDecodeBounds = false;

        Bitmap result = BitmapFactory.decodeFileDescriptor(fd, null, options);
        if (result == null) return null;

        /*
        float scale2 = (float) size / Math.min(result.getWidth(), result.getHeight());
        if (scale <= 0.5) result = resizeBitmapByScale(result, scale);
        */

        return ThumbnailUtils.extractThumbnail(result, thumbnailSize, thumbnailSize);

    }
}
