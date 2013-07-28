package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by karino on 7/27/13.
 */
public class Board {
    static Bitmap s_emptyThumbnail;
    static Bitmap getEmptyThumbnail(int w, int h) {
        if(s_emptyThumbnail == null) {
            s_emptyThumbnail = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            s_emptyThumbnail.eraseColor(Color.WHITE);
        }
        return s_emptyThumbnail;
    }


    Bitmap boardBmp;
    UndoList undoList;
    Bitmap thumbnail;
    int width;
    int height;

    public Board(int w, int h) {
        boardBmp = null;
        thumbnail = null;
        undoList = new UndoList();
        resetCanvas(w, h);
    }

    public Board() {
        boardBmp = null;
        thumbnail = null;
        width = height = 0;
        undoList = new UndoList();
    }

    public void resetCanvas(int w, int h) {
        width = w;
        height =h;
        boardBmp = null;
    }

    private void createEmpyBmp() {
        boardBmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        boardBmp.eraseColor(Color.WHITE);
    }

    public Bitmap getBoardBmp() {
        if(boardBmp == null) {
            createEmpyBmp();
        }
        return boardBmp;
    }

    public void invalidateThumbnail() {
        thumbnail = null;
    }

    public Bitmap getThumbnail(int width, int height) {
        if(thumbnail == null) {
            if(boardBmp == null) {
                thumbnail = getEmptyThumbnail(width, height);
            } else {
                thumbnail = Bitmap.createScaledBitmap(boardBmp, width, height, true);
            }
        }
        return thumbnail;
    }


    public UndoList getUndoList() {
        return undoList;
    }




}
