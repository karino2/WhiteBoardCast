package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by karino on 7/27/13.
 */
public class Board {
    Bitmap boardBmp;
    UndoList undoList;
    Bitmap samnail;

    public Board(int w, int h) {
        boardBmp = null;
        samnail = null;
        undoList = new UndoList();
        resetCanvas(w, h);
    }

    public Board() {
        boardBmp = null;
        samnail = null;
        undoList = new UndoList();
    }

    public void resetCanvas(int w, int h) {
        boardBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        boardBmp.eraseColor(Color.WHITE);
    }

    public Bitmap getBoardBmp() {
        return boardBmp;
    }

    public void invalidateSamnail() {
        samnail = null;
    }

    final int SAMNAIL_WIDTH = 128;
    final int SAMNAIL_HEIGHT = 128;

    public Bitmap getSamnail() {
        if(samnail == null)
            samnail = Bitmap.createScaledBitmap(boardBmp, SAMNAIL_WIDTH, SAMNAIL_HEIGHT, true);
        return samnail;
    }


    public UndoList getUndoList() {
        return undoList;
    }




}
