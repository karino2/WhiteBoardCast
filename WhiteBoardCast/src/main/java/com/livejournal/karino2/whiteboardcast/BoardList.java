package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
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

    public static File getBoardSnapshotDirectory() throws IOException {
        File parent = WhiteBoardCastActivity.getFileStoreDirectory();
        File dir = new File(parent, "boards");
        WhiteBoardCastActivity.ensureDirExist(dir);
        return dir;
    }

    public void saveSnapshots() throws IOException {
        File dir = getBoardSnapshotDirectory();
        SlideList.deleteAllFiles(dir);

        for(int i = 0; i < size(); i++) {
            list.get(i).saveSnapshot(dir, i);
        }
        /*
        File boardNum = new File(dir, String.format("boardnum_%04d", size()));
        boardNum.createNewFile();
        */
    }

    int parseBoardNum(File dir) {
        final int[] boardNum = {1};
        dir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String filename) {
                if(filename.startsWith("boardnum_")) {
                    boardNum[0] = Integer.parseInt(filename.substring(9));
                }
                return false;
            }
        });
        return boardNum[0];
    }

    public void restoreSnapshots(int boardNum) throws IOException {
        File dir = getBoardSnapshotDirectory();
        // int boardNum = parseBoardNum(dir);

        for(int i = 0; i < boardNum; i++) {
            if(i != 0 )
                addNewBoard();
            list.get(i).restoreSnapshot(dir, i);
        }
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
