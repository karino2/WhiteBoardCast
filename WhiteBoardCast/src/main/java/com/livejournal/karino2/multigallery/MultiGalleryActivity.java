package com.livejournal.karino2.multigallery;

import android.app.Activity;
import android.content.ContentResolver;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.media.ThumbnailUtils;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.util.FloatMath;
import android.util.Log;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ListAdapter;

import com.livejournal.karino2.whiteboardcast.*;

import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiGalleryActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_gallery);

        startAlbumSetLoad();

    }

    private void startAlbumSetLoad() {
        GridView grid = getGridView();
        grid.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                MediaHolder holder = (MediaHolder)view.getTag();
                if(holder != null && holder.getItem() != null) {
                    setAlbum((AlbumItem)holder.getItem());
                }
            }
        });

        getExecutor().submit(new AlbumSetLoadTask());
    }

    private GridView getGridView() {
        return (GridView)findViewById(R.id.grid);
    }

    boolean isAlbum = false;

    private void setAlbum(AlbumItem album) {
        AlbumLoader loader = new AlbumLoader(getContentResolver(), album);
        AlbumSlidingWindow slidingWindow = new AlbumSlidingWindow(loader);
        AlbumAdapter adapter = new AlbumAdapter(slidingWindow);
        discardAllPendingRequest();
        getGridView().setAdapter(adapter);
        isAlbum = true;
    }

    int getThumbnailSize() {
        return (int)TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, THUMBNAIL_SIZE, getResources().getDisplayMetrics());
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.multi_gallery, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        if(id == android.R.id.home && isAlbum) {
            finishAlbumAndStartAlbumSet();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void finishAlbumAndStartAlbumSet() {
        // TODO: finish album pending load, etc.
        isAlbum = false;
        startAlbumSetLoad();
    }


    @Override
    public void onBackPressed() {
        if(isAlbum) {
            finishAlbumAndStartAlbumSet();
            return;
        }
        super.onBackPressed();
    }

    class AlbumItem implements MediaItem {
        int id;
        String name;
        String path;
        AlbumItem(int id, String name, String path) {
            this.id = id; this.name = name; this.path = path;
        }
        public String getPath() {
            return path;
        }

        public int getId() {
            return id;
        }
    }

    static final String TOP_PATH = "/local/image";

    class AlbumSetLoadTask implements Runnable{

        ContentResolver resolver;
        AlbumSetLoadTask() {
            this.resolver = getContentResolver();
        }

        @Override
        public void run() {
            ArrayList<MediaItem> albums = new ArrayList<MediaItem>();

            Cursor cursor = getContentResolver().query(
                    MediaStore.Files.getContentUri("external"), new String[]{
                    MediaStore.Images.ImageColumns.BUCKET_ID,
                    MediaStore.Files.FileColumns.MEDIA_TYPE,
                    MediaStore.Images.ImageColumns.BUCKET_DISPLAY_NAME,
                    MediaStore.Images.Media.DATA},
                    "1) GROUP BY 1,(2", null, "MAX(datetaken) DESC");
            try {
                while (cursor.moveToNext()) {
                    if(((1 << MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE) & (1 << cursor.getInt(1))) != 0){
                        albums.add(new AlbumItem(cursor.getInt(0), cursor.getString(2), cursor.getString(3)));
                    }
                }

                // TODO: reorder here.
                notifyAlbumsComing(albums);
            }finally {
                cursor.close();
            }

        }
    }


    Handler handler = new Handler();

    private void notifyAlbumsComing(final ArrayList<MediaItem> albums) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                GridView grid = getGridView();
                AlbumSetAdapter adapter = new AlbumSetAdapter(albums);
                grid.setAdapter(adapter);
            }
        });

    }

    ExecutorService executor;
    ExecutorService getExecutor() {
        if(executor == null) {
           executor = Executors.newFixedThreadPool(Math.max(1, Runtime.getRuntime().availableProcessors()-1));
        }
        return executor;

    }

    class CacheEngine {
        Bitmap lookup(MediaItem item) {
            return null;
        }
        void put(MediaItem item, Bitmap thumbnail) {

        }
    }

    static final int THUMBNAIL_SIZE = 254; //  200;

    CacheEngine cache = new CacheEngine();

    interface MediaLoadListener {
        void onThumbnailComing(Bitmap thumbnail);
    }

    class MediaLoadRequest implements Runnable {

        MediaLoadListener listener;
        MediaItem item;
        MediaLoadRequest(MediaItem item, MediaLoadListener listener) {
            this.listener = listener;
            this.item = item;
        }

        void discard() {
            synchronized(listener) {
                listener = null;
            }
        }



        @Override
        public void run() {

            Bitmap thumbnail  = decodeThumbnail(item.getPath(), getThumbnailSize());


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

            return ThumbnailUtils.extractThumbnail(result, getThumbnailSize(), getThumbnailSize());

        }
    }

    class AlbumSlidingWindow {
        final int CACHE_SIZE =  96;
        ImageItem[] entries;
        int contentStart;
        int contentEnd;
        AlbumLoader loader;
        AlbumSlidingWindow(AlbumLoader loader) {
            this.loader = loader;
            entries = new ImageItem[CACHE_SIZE];
            contentStart = contentEnd = 0;
        }

        private ImageItem get(int slotIndex) {
            if(!isContentSlot(slotIndex)) {
                throw new RuntimeException("invalid slot:" + slotIndex + ", (" + contentStart + ", " + contentEnd + ")");
            }
            return entries[slotIndex % entries.length];
        }

        boolean isContentSlot(int slotIndex) {
            return slotIndex >= contentStart && slotIndex < contentEnd;
        }

        public /* static */ int clamp(int x, int min, int max) {
            if (x > max) return max;
            if (x < min) return min;
            return x;
        }

        public ImageItem requestSlot(int slotIndex) {
            if(isContentSlot(slotIndex))
                return get(slotIndex);


            ImageItem[] data = entries;
            int contentStart = clamp(slotIndex - data.length / 2,
                    0, Math.max(0, size() - data.length));
            int contentEnd = Math.min(contentStart + data.length, size());
            setContentWindow(contentStart, contentEnd);

            return get(slotIndex);
        }


        void setContentWindow(int argContentStart, int argContentEnd) {
            if(contentStart == argContentStart && contentEnd == argContentEnd) return;

            if(argContentStart >= contentEnd || contentStart >= argContentEnd) {
                for(int i = contentStart, n=contentEnd; i<n; ++i ) {
                   freeSlotContent(i);
                }

                // block here. I think this is fast enough.
                ArrayList<ImageItem> items = loader.getMediaItem(argContentStart, argContentEnd);

                for(int i = argContentStart; i < argContentEnd; ++i) {
                    putSlotContent(i, items.get(i - argContentStart));
                }
            } else if(argContentStart > contentStart){
                for (int i = contentStart; i < argContentStart; ++i) {
                    freeSlotContent(i);
                }

                // block here. I think this is fast enough.
                ArrayList<ImageItem> items = loader.getMediaItem(contentEnd, argContentEnd);

                for (int i = contentEnd; i < argContentEnd; ++i) {
                    putSlotContent(i, items.get(i - contentEnd));
                }
            } else /* argContentStart < contentStart */ {
                for (int i = argContentEnd, n = contentEnd; i < n; ++i) {
                    freeSlotContent(i);
                }

                // block here. I think this is fast enough.
                ArrayList<ImageItem> items = loader.getMediaItem(argContentStart, contentStart);

                for (int i = argContentStart, n = contentStart; i < n; ++i) {
                    putSlotContent(i, items.get(i - argContentStart));
                }
            }
            contentStart = argContentStart;
            contentEnd = argContentEnd;

        }

        private void putSlotContent(int slotIndex, ImageItem imageItem) {
            entries[slotIndex%entries.length] = imageItem;

        }

        private void freeSlotContent(int slotIndex) {
            entries[slotIndex%entries.length] = null;
        }


        public int size() {
            return loader.getItemCount();
        }
    }

    static Bitmap loadingImage;
    public static Bitmap getLoadingBitmap(int thumbnailSize) {
        if(loadingImage == null) {
            loadingImage = Bitmap.createBitmap(thumbnailSize, thumbnailSize, Bitmap.Config.ARGB_8888);
            loadingImage.eraseColor(Color.CYAN); // for debug.
        }
        return loadingImage;
    }

    Set<MediaLoadRequest> pendingRequest = new HashSet<MediaLoadRequest>();
    void addToPendingSet(MediaLoadRequest newReq) {
        pendingRequest.add(newReq);
    }

    void removePendingSet(MediaLoadRequest req) {
        pendingRequest.remove(req);
    }

    void discardAllPendingRequest() {
        for(MediaLoadRequest req : pendingRequest) {
            req.discard();
        }
        pendingRequest.clear();
    }

    class MediaHolder implements MediaLoadListener{
        ImageView target;
        MediaItem item;
        Bitmap thumbnail;
        MediaLoadRequest request;
        public MediaHolder(MediaItem item, ImageView iv) {
            this.item = item;
            target = iv;
        }

        public MediaItem getItem() { return item; }

        public void recycle(MediaItem item) {
            if(request != null) {
                request.discard();
                removePendingSet(request);
                request = null;
            }
            this.item = item;
        }


        public void beginLoad() {
            Bitmap thumbnail = cache.lookup(item);
            if(thumbnail != null) {
                onThumbnailComing(thumbnail);
                return;
            }
            target.setImageBitmap(getLoadingBitmap(getThumbnailSize()));
            request = new MediaLoadRequest(getItem(), this);
            addToPendingSet(request);
            getExecutor().submit(request);
        }


        public void onThumbnailComing(Bitmap thumbnail) {
            removePendingSet(request);
            request = null;
            this.thumbnail = thumbnail;
            handler.post(new Runnable() {
                @Override
                public void run() {
                    target.setImageBitmap(MediaHolder.this.thumbnail);
                }
            });
        }
    }

    public class AlbumSetAdapter extends BaseAdapter implements ListAdapter {
        ArrayList<MediaItem> albums;
        public AlbumSetAdapter(ArrayList<MediaItem> albums) {
            this.albums = albums;
        }

        @Override
        public int getCount() {
            return albums.size();
        }

        @Override
        public Object getItem(int i) {
            return albums.get(i);
        }

        @Override
        public long getItemId(int i) {
            return (long)i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ImageView iv;
            MediaItem item = (MediaItem)getItem(i);
            if(view == null) {
                iv = new ImageView(MultiGalleryActivity.this);
                MediaHolder holder = new MediaHolder(item, iv);
                iv.setTag(holder);
                holder.beginLoad();
            } else {
                MediaHolder holder = (MediaHolder)view.getTag();
                holder.recycle(item);
                holder.beginLoad();
                iv = (ImageView)view;
            }
            return iv;
        }
    }

    public class AlbumAdapter extends BaseAdapter implements ListAdapter {
        AlbumSlidingWindow slidingWindow;
        public AlbumAdapter(AlbumSlidingWindow albumWindow) {
            this.slidingWindow = albumWindow;
        }

        @Override
        public int getCount() {
            return slidingWindow.size();
        }

        @Override
        public Object getItem(int i) {
            return slidingWindow.requestSlot(i);
        }

        @Override
        public long getItemId(int i) {
            return (long)i;
        }

        // TODO: almost the same as AlbumSetAdapter.
        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            ImageView iv;
            MediaItem item = (MediaItem)getItem(i);
            if(view == null) {
                iv = new ImageView(MultiGalleryActivity.this);
                MediaHolder holder = new MediaHolder(item, iv);
                iv.setTag(holder);
                holder.beginLoad();
            } else {
                MediaHolder holder = (MediaHolder)view.getTag();
                // might need to recycle for SlidingWindow, but not yet.
                holder.recycle(item);
                holder.beginLoad();
                iv = (ImageView)view;
            }
            return iv;
        }
    }

}
