package com.livejournal.karino2.whiteboardcast;

import android.app.ListActivity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.ActionMode;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.livejournal.karino2.multigallery.MultiGalleryActivity;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

public class SlideListActivity extends ListActivity {
    final int REQUEST_PICK_IMAGE = 1;

    int screenWidth, screenHeight;
    FileImageAdapter adapter;

    void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        // Make the newly clicked item the currently selected one.
        getListView().setItemChecked(position, true);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_list);
        WindowManager wm = (WindowManager)getSystemService(Context.WINDOW_SERVICE);
        Display disp = wm.getDefaultDisplay();
        Point windowSize = new Point();
        disp.getSize(windowSize);
        screenWidth = windowSize.x;
        screenHeight = windowSize.y;
        // use this for image copying. so always treat as landscape.
        if(screenWidth<screenHeight) {
            screenWidth = windowSize.y;
            screenHeight = windowSize.x;
        }

        getListView().setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
        getListView().setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {
            @Override
            public void onItemCheckedStateChanged(ActionMode actionMode, int position, long id, boolean checked) {
                // actionMode.getMenu().clear();
                /*
                long[] ids = getListView().getCheckedItemIds();
                if(ids.length >= 2) {

                }
                */
                showMessage("check state changed: " + selectedIdDump());
            }

            @Override
            public boolean onCreateActionMode(ActionMode actionMode, Menu menu) {
                MenuInflater inflater = actionMode.getMenuInflater();
                inflater.inflate(R.menu.slide_list_ctx, menu);
                return true;
            }

            @Override
            public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
                return false;
            }

            String selectedIdDump() {
                long[] ids = getListView().getCheckedItemIds();
                StringBuffer buf = new StringBuffer();
                for(long id : ids) {
                    buf.append(id);
                    buf.append(",");
                }
                return buf.toString();
            }

            int[] getSelectedIndexes() throws IOException {
                long[] idsLong = getListView().getCheckedItemIds();
                int[] ids = new int[idsLong.length];
                for(int i = 0; i < ids.length; i++) {
                    File key = idGen.reverseLookUp(idsLong[i]);
                    ids[i] = getSlideFiles().indexOf(key);
                }
                return ids;
            }

            @Override
            public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
                switch(menuItem.getItemId()) {
                    case R.id.action_up:
                        try {
                            slideList.upFiles(getSelectedIndexes());
                            adapter.reload();
                        } catch (IOException e) {
                            showError("reload adapter fail on up: " + e.getMessage());
                        }
                        return true;
                    case R.id.action_down:
                        try {
                            slideList.downFiles(getSelectedIndexes());
                            adapter.reload();
                        } catch (IOException e) {
                            showError("reload adapter fail on down: " + e.getMessage());
                        }
                        return true;
                    case R.id.action_delete:
                        try {
                            slideList.deleteFiles(getSelectedIndexes());
                            actionMode.finish();
                            adapter.reload();
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                        return true;
                }
                return false;
            }

            @Override
            public void onDestroyActionMode(ActionMode actionMode) {

            }
        });

        try {
            adapter = new FileImageAdapter(this, getSlideFiles(), windowSize.x, windowSize.y/6);
            setListAdapter(adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Show the Up button in the action bar.
        setupActionBar();
    }

    private void showError(String msg) {
        Log.d("WhiteBoardCast", msg);
        showMessage(msg);
    }

    class IdGenerator {
        HashMap<File, Long> idStore = new HashMap<File, Long>();
        long lastId = 1;
        long getId(File key) {
            if(idStore.containsKey(key)) {
                return (long)idStore.get(key);
            }
            idStore.put(key, lastId++);
            return (long)idStore.get(key);
        }
        File reverseLookUp(long id) {
            if(idStore.containsValue(id)) {
                for(java.util.Map.Entry<File,Long> entry :  idStore.entrySet()) {
                    if(id == entry.getValue()) {
                        return entry.getKey();
                    }
                }
            }
            return null;
        }
    }
    IdGenerator idGen = new IdGenerator();

    public class FileImageAdapter extends BaseAdapter implements ListAdapter {
        List<File> files;
        ListActivity context;
        int width, height;
        public FileImageAdapter(ListActivity ctx, List<File> fs, int itemWidth, int itemHeight) {
            context = ctx;
            files = fs;
            width = itemWidth;
            height = itemHeight;
        }

        public void reload() throws IOException {
            files = reloadSlideFiles();
            notifyDataSetChanged();
        }


        @Override
        public int getCount() {
            return files.size();
        }

        @Override
        public Object getItem(int i) {
            return files.get(i);
        }

        @Override
        public long getItemId(int i) {
            return idGen.getId(files.get(i));
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            ImageView iv;
            View view;
            if(convertView == null) {
                view = context.getLayoutInflater().inflate(R.layout.slide_item, null);
                iv = (ImageView)view.findViewById(R.id.imageView);
                iv.setLayoutParams(new LinearLayout.LayoutParams(width, height));
            } else {
                view = convertView;
                iv = (ImageView)convertView.findViewById(R.id.imageView);
            }
            File f = files.get(i);
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
            iv.setImageBitmap(bmp);
            return view;
        }

        @Override
        public boolean hasStableIds() {
            return true;
        }
    }

    SlideList slideList;

    SlideList getSlideList() throws IOException {
        if(slideList == null) {
            slideList = SlideListSerializer.createSlideListWithDefaultFolder();
            slideList.syncListedActual();
        }
        return slideList;
    }
    public List<File> getSlideFiles() throws IOException {
        return getSlideList().getFiles();
    }

    public List<File> reloadSlideFiles() throws IOException {
        return getSlideFiles();
    }


    public static File getSlideListDirectory() throws IOException {
        File parent = WhiteBoardCastActivity.getFileStoreDirectory();
        File dir = new File(parent, "slides");
        WhiteBoardCastActivity.ensureDirExist(dir);
        return dir;
    }

    /**
     * Set up the {@link android.app.ActionBar}.
     */
    private void setupActionBar() {
        
        getActionBar().setDisplayHomeAsUpEnabled(true);
        
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.slide_list, menu);
        return true;
    }
    

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                NavUtils.navigateUpFromSameTask(this);
                return true;
            case R.id.action_add:
                /*
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
                */
                Intent intent = new Intent(this, MultiGalleryActivity.class);
                startActivityForResult(intent, REQUEST_PICK_IMAGE);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onStop() {
        super.onStop();
        try {
            SlideListSerializer serializer = new SlideListSerializer(getSlideListDirectory());
            serializer.save(slideList.getFiles());
        } catch (IOException e) {
            showError("IO error when stop: " + e.getMessage());
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_PICK_IMAGE:
                if(resultCode == RESULT_OK){
                    copyImage(data);
                    try {
                        adapter.reload();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
        }
    }

    int calculateResizeFactor(int orgWidth, int orgHeight,
                                     int limitWidth, int limitHeight) {
        int widthResizeFactor = Math.max(1, (orgWidth+limitWidth-1)/limitWidth);
        int heightResizeFactor = Math.max(1, (orgHeight+limitHeight-1)/limitHeight);
        int resizeFactor = Math.max(widthResizeFactor, heightResizeFactor);
        return resizeFactor;
    }


    int sequenceId = 0;
    String getNewSequentialFile() {
        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
        return timeStampFormat.format(new Date()) + "_" + sequenceId++ +".png";
    }

    private void copyImage(Intent data) {
        Uri imageUri = data.getData();
        try {
            File result = new File(getSlideListDirectory(), getNewSequentialFile());

            int resizeFactor = getResizeFactor(imageUri);
            BitmapFactory.Options options;

            options = new BitmapFactory.Options();
            options.inSampleSize = resizeFactor;

            InputStream is = getContentResolver().openInputStream(imageUri);
            try {
                Bitmap bitmap = BitmapFactory.decodeStream(is, null, options);
                // currently, always resize even though it sometime not necessary.
                Bitmap resizedbitmap = Bitmap.createScaledBitmap(bitmap, screenWidth, screenHeight, true);


                OutputStream stream = new FileOutputStream(result);
                resizedbitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
                stream.close();
                slideList.add(result);
            }finally {
                is.close();
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
}
