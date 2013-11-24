package com.livejournal.karino2.multigallery;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.util.ArrayList;

/**
* Created by karino on 11/24/13.
*/
class AlbumLoader {
    ContentResolver resolver;
    MultiGalleryActivity.AlbumItem album;
    public AlbumLoader(ContentResolver resolver, MultiGalleryActivity.AlbumItem album) {
        this.resolver = resolver;
        this.album = album;
    }

    Uri getBaseUri() {
        return MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
    }

    public ArrayList<ImageItem> getMediaItem(int start, int count) {
        ArrayList<ImageItem> images = new ArrayList<ImageItem>();
        Uri uri = getBaseUri().buildUpon()
                .appendQueryParameter("limit", start + "," + count).build();


        Cursor cursor = resolver.query(uri, ImageItem.PROJECTION,
                MediaStore.Images.ImageColumns.BUCKET_ID + " = ?", new String[]{String.valueOf(album.getId())},
                MediaStore.Images.ImageColumns.DATE_TAKEN + " DESC, "
                        + MediaStore.Images.ImageColumns._ID + " DESC");
        try {
            while (cursor.moveToNext()) {
                ImageItem image = new ImageItem(cursor);
                images.add(image);
            }
        } finally {
            cursor.close();
        }
        return images;
    }

    int cachedCount = -1;
    public int getItemCount() {
        if(cachedCount == -1) {
            Cursor cursor = resolver.query(
                    getBaseUri(), new String[]{"count(*)" },
                    MediaStore.Images.ImageColumns.BUCKET_ID + " = ?",
                    new String[]{String.valueOf(album.getId())}, null);
            try {
                cursor.moveToNext();
                cachedCount = cursor.getInt(0);
            } finally {
                cursor.close();
            }

        }
        return cachedCount;
    }
}
