package com.livejournal.karino2.multigallery;

import android.graphics.Bitmap;

import java.util.ArrayList;
import java.util.HashMap;

/**
* Created by karino on 11/29/13.
*/
public class CacheEngine {
    ArrayList<String> keys = new ArrayList<String>();
    HashMap<String, Bitmap> entries = new HashMap<String, Bitmap>();

    int cacheSize = 100; // default size

    CacheEngine() {
    }

    public CacheEngine(int size) {
        cacheSize = size;
    }

    Bitmap lookupByMediaItem(MediaItem item) {
        return lookup(item.getPath());
    }

    public Bitmap lookup(String path) {
        if(entries.containsKey(path))
            return entries.get(path);
        return null;
    }

    void put(MediaItem item, Bitmap thumbnail) {
        put(item.getPath(), thumbnail);
    }

    public void put(String path, Bitmap thumbnail) {
        if(keys.contains(path)) {
            keys.remove(path);
            keys.add(path);
            return;
        }

        keys.add(path);
        entries.put(path, thumbnail);

        if(keys.size() > cacheSize) {
            String first = keys.get(0);
            entries.remove(first);
            keys.remove(0);
        }
    }
}
