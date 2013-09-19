package com.livejournal.karino2.whiteboardcast;

import java.util.ArrayList;

/**
 * Created by karino on 7/27/13.
 */
public class BoardList {
    ArrayList<Board> list = new ArrayList<Board>();
    int currentPos = 0;
    int width;
    int height;

    public void setSize(int w, int h) {
        if(width != w || height != h) {
            width = w;
            height = h;
            getCurrent().resetCanvas(w, h);
        }
    }

    public BoardList() {
        list.add(new Board());
    }

    public Board getCurrent() {
        return list.get(currentPos);
    }

    public int size() {
        return list.size();
    }

    public void addNewBoard() {
        list.add(new Board(width, height));
    }


    public boolean pagePrev() {
        if(currentPos == 0)
            return false;
        currentPos--;
        return true;
    }

    public void pageNext() {
        if(size() == currentPos+1)
            addNewBoard();
        currentPos++;
    }
}
