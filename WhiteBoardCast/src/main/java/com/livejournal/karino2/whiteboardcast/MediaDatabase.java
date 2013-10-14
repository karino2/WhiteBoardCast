package com.livejournal.karino2.whiteboardcast;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

import java.io.File;
import java.io.IOException;

/**
 * Created by karino on 10/14/13.
 */
public class MediaDatabase {
    ContentResolver resolver;
    public MediaDatabase(ContentResolver resolver1) {
        resolver = resolver1;
    }

    public long uriToId(String uri) throws IOException {
        Cursor cursor = resolver.query(Uri.parse(uri), new String[]{"_id"}, null, null, null);
        cursor.moveToFirst();
        return cursor.getLong(0);
        /*
        File dir = WhiteBoardCastActivity.getFileStoreDirectory();
        Cursor cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        "_id",
                        MediaStore.Video.VideoColumns.TITLE,
                        MediaStore.Video.VideoColumns.DATE_ADDED,
                        MediaStore.Video.Media.MIME_TYPE,
                        MediaStore.Video.Media.DATA
                }, MediaStore.Video.Media.MIME_TYPE+  " = ? and " + MediaStore.Video.Media.DATA + " = ? and " + MediaStore.Video.Media.DATA + " like '%"+ dir.getAbsolutePath()+ "%'", new String[]{"video/webm", uri},  MediaStore.Video.VideoColumns.DATE_ADDED + " ASC");
                */
    }

    public Cursor getAllVideos() throws IOException {
        File dir = WhiteBoardCastActivity.getFileStoreDirectory();
        Cursor cursor = resolver.query(MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                new String[]{
                        "_id",
                        MediaStore.Video.VideoColumns.TITLE,
                        MediaStore.Video.VideoColumns.DATE_ADDED,
                        MediaStore.Video.Media.MIME_TYPE,
                        MediaStore.Video.Media.DATA
                }, MediaStore.Video.Media.MIME_TYPE+  " = ? and " + MediaStore.Video.Media.DATA + " like '%"+ dir.getAbsolutePath()+ "%'", new String[]{"video/webm"},  MediaStore.Video.VideoColumns.DATE_ADDED + " ASC");
        return cursor;
    }
}
