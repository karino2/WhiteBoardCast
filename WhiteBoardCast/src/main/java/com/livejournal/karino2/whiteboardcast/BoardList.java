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

            // TODO: toggle code.
            setupToggle();
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



    // temp implementation.
    public void setupToggle() {
        if(size() == 2)
            return;
        addNewBoard();
    }

    public void toggleBoard() {
        if(currentPos == 0)
            currentPos = 1;
        else
            currentPos = 0;
    }

    public Board next() {
        currentPos++;
        if(list.size() == currentPos) {
            addNewBoard();
            return getCurrent();
        }
        return getCurrent();
    }

    public Board prev() {
        if(currentPos == 0)
            return null;
        currentPos--;
        return getCurrent();
    }

}
