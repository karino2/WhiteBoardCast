package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

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

    UndoList.Undoable undoTarget;

    public BoardList(UndoList.Undoable undoTarget) {
        this.undoTarget = undoTarget;
        list.add(new Board(undoTarget));
    }

    public Board getCurrent() {
        return getBoard(currentPos);
    }

    public Bitmap createPrevSynthesizedBmp() {
        return list.get(currentPos-1).createSynthesizedTempBmp();
    }

    public Bitmap createNextSynthesizedBmp() {
        if(isLastPage())
            return emptyPage;
        return list.get(currentPos + 1).createSynthesizedTempBmp();
    }

    public int size() {
        return list.size();
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public void addNewBoard() {
        list.add(new Board(undoTarget, width, height));
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

    public Board getBoard(int pos) {
        return list.get(pos);
    }
}
