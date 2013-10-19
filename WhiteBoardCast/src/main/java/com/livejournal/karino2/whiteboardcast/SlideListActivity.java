package com.livejournal.karino2.whiteboardcast;

import android.app.ListActivity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import android.view.MenuItem;
import android.support.v4.app.NavUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.SimpleAdapter;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

public class SlideListActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_slide_list);

        try {
            FileImageAdapter adapter = new FileImageAdapter(this, getSlideFiles());
            setListAdapter(adapter);
        } catch (IOException e) {
            e.printStackTrace();
        }


        // Show the Up button in the action bar.
        setupActionBar();
    }

    public class FileImageAdapter extends BaseAdapter implements ListAdapter {
        File[] files;
        Context context;
        public FileImageAdapter(Context ctx, File[] fs) {
            context = ctx;
            files = fs;
        }

        @Override
        public int getCount() {
            return files.length;
        }

        @Override
        public Object getItem(int i) {
            return files[i];
        }

        @Override
        public long getItemId(int i) {
            return (long)i;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            ImageView iv;
            if(convertView == null) {
                iv = new ImageView(context);
            } else {
                iv = (ImageView)convertView;
            }
            File f = files[i];
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inSampleSize = 4;
            Bitmap bmp = BitmapFactory.decodeFile(f.getAbsolutePath(), options);
            iv.setImageBitmap(bmp);
            return iv;
        }
    }

    File[] slideFiles;
    public File[] getSlideFiles() throws IOException {
        if(slideFiles == null) {
            File dir = getSlideListDirectory();
            slideFiles = dir.listFiles(new FilenameFilter(){
                @Override
                public boolean accept(File dir, String filename) {
                    if(filename.endsWith(".png") || filename.endsWith(".PNG") ||
                            filename.endsWith(".jpg") || filename.endsWith(".JPG"))
                        return true;
                    return false;
                }
            });
            Arrays.sort(slideFiles, new Comparator<File>() {
                public int compare(File f1, File f2) {
                    return Long.valueOf(f2.lastModified()).compareTo(f1.lastModified());
                }

            });

        }
        return slideFiles;
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
        }
        return super.onOptionsItemSelected(item);
    }

}
