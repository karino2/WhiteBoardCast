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

            setupBoards();
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

    public static final int THUMBNAIL_NUM = 3;


    public boolean isCurrent(int idx) {
        return idx == currentPos;
    }
    public Board getBoard(int idx) {
        return list.get(idx);
    }

    // temp implementation.
    public void setupBoards() {
        if(size() == THUMBNAIL_NUM)
            return;
        for(int i = 1; i < THUMBNAIL_NUM; i++)
            addNewBoard();
    }


    public void gotoBoard(int boardIdx) {
        if(size() <= boardIdx)
            throw new IllegalArgumentException("boardIdx: " + boardIdx + ", size:" + size());
        currentPos = boardIdx;
    }
}
