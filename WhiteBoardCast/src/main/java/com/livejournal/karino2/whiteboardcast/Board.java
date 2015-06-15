package com.livejournal.karino2.whiteboardcast;

import android.content.ContentResolver;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Created by karino on 7/27/13.
 */
public class Board {

    Bitmap boardBmp;
    UndoList undoList;
    int width;
    int height;

    public Board(UndoList.Undoable target, int w, int h) {
        boardBmp = null;
        undoList = new UndoList(target);
        resetCanvas(w, h);
    }

    public Board(UndoList.Undoable target) {
        boardBmp = null;
        width = height = 0;
        undoList = new UndoList(target);
    }

    private File getFile(File folder, int index) {
        return new File(folder, String.format("fg_%04d.png", index));
    }
    public void saveSnapshot(File folder, int index) throws IOException {
        background.saveSnapshot(folder, index);
        if(boardBmp != null) {
            File saveFile = getFile(folder, index);
            saveBitmap(boardBmp, saveFile);
        }
    }
    public void restoreSnapshot(File folder, int index) throws FileNotFoundException {
        background.restoreSnapshot(folder, index);
        File file = getFile(folder, index);
        if(file.exists()) {
            boardBmp = loadBitmap(file);
        }
    }

    static void saveBitmap(Bitmap bitmap, File result) throws IOException {
        OutputStream stream = new FileOutputStream(result);
        bitmap.compress(Bitmap.CompressFormat.PNG, 80, stream);
        stream.close();
    }

    static Bitmap loadBitmap(File bmpFile) throws FileNotFoundException {
        InputStream is = null;
        try {
            is = new FileInputStream(bmpFile);

            Bitmap bmp = BitmapFactory.decodeStream(is);
            return bmp;
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                // What can I do?
                Log.d("WhiteBoardCast", "loadBitmap, IOException on Close: " + e.getMessage());
            }
        }

    }

    public void resetCanvas(int w, int h) {
        width = w;
        height =h;
        boardBmp = null;
    }

    private void createEmptyBmp() {
        boardBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        boardBmp.eraseColor(Color.TRANSPARENT);
    }

    public Bitmap createSynthesizedTempBmp() {
        Bitmap fg = getBoardBmp();
        Bitmap bg = getBackgroundBmp();
        Bitmap res = bg.copy(Bitmap.Config.ARGB_8888, true);
        Canvas cv = new Canvas(res);
        cv.drawBitmap(fg, 0, 0, null);
        return res;
    }

    public Bitmap getBoardBmp() {
        if(boardBmp == null) {
            createEmptyBmp();
        }
        return boardBmp;
    }

    public UndoList getUndoList() {
        return undoList;
    }

    public Bitmap getBackgroundBmp() {
        return background.getBackground(width, height);
    }


    static class BackgroundImage {
        Bitmap background;
        File backgroundFile;
        BackgroundImage() {
            background = null;
            backgroundFile = null;
        }
        BackgroundImage(File file) {
            backgroundFile = file;
            background = null;
        }

        void discardBitmap() {
            background = null;
        }

        BackgroundImage duplicate() {
            return new BackgroundImage(backgroundFile);
        }

        void restoreSnapshot(File folder, int index) {
            File result = getFile(folder, index);
            if(result.exists())
            {
                // backgroundFile and saved snapshot is logically different, but practically it's the same for this purpose.
                backgroundFile = result;
                background = null;
            }
        }

        // save only bmp, not backgroundFile. Because undo information is lost anyway and I need only for pdf export.
        void saveSnapshot(File folder, int index) throws IOException {
            if(background != null) {
                File result = getFile(folder, index);
                saveBitmap(background, result);
            }
        }

        private File getFile(File folder, int index) {
            return new File(folder, String.format("bg_%04d.png", index));
        }

        Bitmap getBackground(int w, int h) {
            if(background != null)
                return background;
            if(backgroundFile != null) {
                try {
                    background = loadBitmap(backgroundFile);
                    return background;
                } catch (FileNotFoundException e) {
                    // What can I do?
                    Log.d("WhiteBoardCast", "getBackground, FileNotFoundException: " + e.getMessage());
                }

            }

            return getWhiteBackGround(w, h);
        }
    }

    BackgroundImage background = new BackgroundImage();

    // The backgroundBitmap of argument will be discard after this method call. Need to dup.
    public BackgroundImage setBackground(BackgroundImage newBG) {
        BackgroundImage prev = background;
        background = newBG.duplicate();
        return prev;
    }



    static Bitmap s_whiteBackGround;
    static Bitmap getWhiteBackGround(int w, int h) {
        if(s_whiteBackGround == null) {
            s_whiteBackGround = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            s_whiteBackGround.eraseColor(Color.WHITE);
        }
        return s_whiteBackGround;
    }


}
