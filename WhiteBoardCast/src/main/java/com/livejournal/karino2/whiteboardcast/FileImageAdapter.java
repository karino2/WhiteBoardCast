package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListAdapter;

import com.livejournal.karino2.multigallery.CacheEngine;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;

/**
* Created by karino on 11/28/13.
*/
public class FileImageAdapter extends BaseAdapter implements ListAdapter {
    static class IdGenerator {
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

    public File reverseLookUp(long id) {
        return idGen.reverseLookUp(id);
    }


    List<File> files;
    LayoutInflater inflater;
    int width, height;
    public FileImageAdapter(LayoutInflater inflater1, List<File> fs, int itemWidth, int itemHeight) {
        inflater = inflater1;
        files = fs;
        width = itemWidth;
        height = itemHeight;
    }

    public void reload(List<File> newFiles) throws IOException {
        files = newFiles;
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

    CacheEngine cacheEngine = new CacheEngine(10);

    @Override
    public View getView(int i, View convertView, ViewGroup viewGroup) {
        ImageView iv;
        View view;
        if(convertView == null) {
            view = inflater.inflate(R.layout.slide_item, null);
            iv = (ImageView)view.findViewById(R.id.imageView);
            iv.setLayoutParams(new LinearLayout.LayoutParams(width, height));
        } else {
            view = convertView;
            iv = (ImageView)convertView.findViewById(R.id.imageView);
        }
        File f = files.get(i);
        try {
            // a little slow.
            Bitmap bmp = ImportDialog.getThumbnailBitmap(f, cacheEngine);
            iv.setImageBitmap(bmp);
        } catch (IOException e) {
            Log.d("WhiteBoardCast", "cant create thumbnail on FileImageAdapter: " + e.getMessage());
        }
        return view;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }
}
