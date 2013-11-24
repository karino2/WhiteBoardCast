package com.livejournal.karino2.multigallery;

import android.database.Cursor;
import android.provider.MediaStore;

/**
* Created by karino on 11/24/13.
*/
class ImageItem implements MediaItem{
    // Must preserve order between these indices and the order of the terms in
    // the following PROJECTION array.
    private static final int INDEX_ID = 0;
    private static final int INDEX_CAPTION = 1;
    private static final int INDEX_MIME_TYPE = 2;
    private static final int INDEX_LATITUDE = 3;
    private static final int INDEX_LONGITUDE = 4;
    private static final int INDEX_DATE_TAKEN = 5;
    private static final int INDEX_DATE_ADDED = 6;
    private static final int INDEX_DATE_MODIFIED = 7;
    private static final int INDEX_DATA = 8;
    private static final int INDEX_ORIENTATION = 9;
    private static final int INDEX_BUCKET_ID = 10;
    private static final int INDEX_SIZE = 11;
    private static final int INDEX_WIDTH = 12;
    private static final int INDEX_HEIGHT = 13;

    static final String[] PROJECTION =  {
            MediaStore.Images.ImageColumns._ID,           // 0
            MediaStore.Images.ImageColumns.TITLE,         // 1
            MediaStore.Images.ImageColumns.MIME_TYPE,     // 2
            MediaStore.Images.ImageColumns.LATITUDE,      // 3
            MediaStore.Images.ImageColumns.LONGITUDE,     // 4
            MediaStore.Images.ImageColumns.DATE_TAKEN,    // 5
            MediaStore.Images.ImageColumns.DATE_ADDED,    // 6
            MediaStore.Images.ImageColumns.DATE_MODIFIED, // 7
            MediaStore.Images.ImageColumns.DATA,          // 8
            MediaStore.Images.ImageColumns.ORIENTATION,   // 9
            MediaStore.Images.ImageColumns.BUCKET_ID,     // 10
            MediaStore.Images.ImageColumns.SIZE,          // 11
            "0",                        // 12
            "0"                         // 13
    };

    public int id;
    public String caption;
    public String mimeType;
    public long fileSize;
    /*
    public double latitude = INVALID_LATLNG;
    public double longitude = INVALID_LATLNG;
    */
    public long dateTakenInMs;
    public long dateAddedInSec;
    public long dateModifiedInSec;
    public String filePath;
    public int bucketId;
    public int width;
    public int height;

    public String getPath() {
        return filePath;
    }
    ImageItem(Cursor cursor) {
        id = cursor.getInt(INDEX_ID);
        caption = cursor.getString(INDEX_CAPTION);
        mimeType = cursor.getString(INDEX_MIME_TYPE);
        /*
        latitude = cursor.getDouble(INDEX_LATITUDE);
        longitude = cursor.getDouble(INDEX_LONGITUDE);
        */
        dateTakenInMs = cursor.getLong(INDEX_DATE_TAKEN);
        dateAddedInSec = cursor.getLong(INDEX_DATE_ADDED);
        dateModifiedInSec = cursor.getLong(INDEX_DATE_MODIFIED);
        filePath = cursor.getString(INDEX_DATA);
        // rotation = cursor.getInt(INDEX_ORIENTATION);
        bucketId = cursor.getInt(INDEX_BUCKET_ID);
        fileSize = cursor.getLong(INDEX_SIZE);
        width = cursor.getInt(INDEX_WIDTH);
        height = cursor.getInt(INDEX_HEIGHT);
    }

}
