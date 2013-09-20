package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Color;

import java.util.ArrayList;

/**
 * Created by karino on 7/27/13.
 */
public class BoardList {
    ArrayList<Board> list = new ArrayList<Board>();
    int currentPos = 0;
    int width;
    int height;

    Bitmap emptyPage;

    public void setSize(int w, int h) {
        if(width != w || height != h) {
            width = w;
            height = h;
            getCurrent().resetCanvas(w, h);
            emptyPage = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            emptyPage.eraseColor(Color.WHITE);
        }
    }

    public BoardList() {
        list.add(new Board());
    }

    public Board getCurrent() {
        return list.get(currentPos);
    }

    public Bitmap getPrevBmp() {
        return list.get(currentPos-1).getBoardBmp();
    }

    public Bitmap getNextBmp() {
        if(isLastPage())
            return emptyPage;
        return list.get(currentPos+1).getBoardBmp();
    }

    public int size() {
        return list.size();
    }

    public void addNewBoard() {
        list.add(new Board(width, height));
    }

    public boolean hasPrevPage() {
        return currentPos >= 1;
    }

    public boolean pagePrev() {
        if(!hasPrevPage())
            return false;
        currentPos--;
        return true;
    }

    public void pageNext() {
        if(isLastPage())
            addNewBoard();
        currentPos++;
    }

    private boolean isLastPage() {
        return size() == currentPos+1;
    }
}
