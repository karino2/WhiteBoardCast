package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Color;

/**
 * Created by karino on 7/27/13.
 */
public class Board {

    Bitmap boardBmp;
    Bitmap backgroundBmp;
    UndoList undoList;
    int width;
    int height;

    public Board(int w, int h) {
        boardBmp = null;
        undoList = new UndoList();
        resetCanvas(w, h);
    }

    public Board() {
        boardBmp = null;
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

    public UndoList getUndoList() {
        return undoList;
    }

    public Bitmap getBackgroundBmp() {
        if(backgroundBmp == null) {
            backgroundBmp = getWhiteBackGround(width, height);
        }
        return backgroundBmp;
    }

    public void setBackgroundBmp(Bitmap newBG) {
        backgroundBmp = newBG;
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
