package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by karino on 9/15/13.
 */
public class HalfBoard {
    public static int TOP = 1;
    public static int BOTTOM = 2;

    Bitmap boardImage;
    Canvas boardCanvas;
    public HalfBoard(int width, int height) {
        boardImage = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        boardImage.eraseColor(Color.WHITE);
        boardCanvas = new Canvas(boardImage);
    }

    public Rect getBoardRect() {
        return new Rect(0, 0, boardImage.getWidth(), boardImage.getHeight());
    }

    public Rect getRectAtWholeScreen(int topOrBottom) {
        if(topOrBottom == TOP)
            return getBoardRect();
        return new Rect(0, boardImage.getHeight(), boardImage.getWidth(), boardImage.getHeight()*2);
    }

    public void detach(Bitmap wholeScreen, int topOrBottom) {
        Paint paint = new Paint();
        boardCanvas.drawBitmap(wholeScreen, getRectAtWholeScreen(topOrBottom), getBoardRect(), paint);
    }

    public void attach(Bitmap wholeScreen, int topOrBottom) {
        Paint paint = new Paint();
        Canvas canvas = new Canvas(wholeScreen);
        canvas.drawBitmap(boardImage, getBoardRect(), getRectAtWholeScreen(topOrBottom), paint);
    }

    public Bitmap getBoardImage() {
        return boardImage;
    }
}
