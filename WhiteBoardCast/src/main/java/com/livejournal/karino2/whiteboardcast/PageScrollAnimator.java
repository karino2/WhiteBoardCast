package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.Log;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class PageScrollAnimator implements Runnable{
    public enum Direction{
        Prev, Next;
    }

    public static interface Animatee {
        void start();
        void updateScreen();
        void done(Direction dir);
    }

    long beginTick;
    static final long ANIMATION_MILL = 1 * 1000;
    static final int FPS = 20;
    Bitmap prevPage;
    Bitmap nextPage;
    ScheduledExecutorService executor;
    Canvas destCanvas;
    Animatee target;
    Paint blackPaint;
    Direction scrollDirection;


    public PageScrollAnimator(ScheduledExecutorService execut, Animatee target) {
        executor = execut;
        this.target = target;
        scrollDirection = Direction.Next;

        blackPaint = new Paint();
        blackPaint.setColor(Color.BLACK);
    }

    public void setExecutor(ScheduledExecutorService executor1) {
        executor = executor1;
    }

    ScheduledFuture<?> future;
    Bitmap destBmpForLock;
    public void start(Bitmap prev, Bitmap next, Bitmap dest, Direction dir) {
        destBmpForLock = dest;
        prevPage = prev;
        nextPage = next;
        scrollDirection = dir;
        destCanvas = new Canvas(dest);


        beginTick = System.currentTimeMillis();
        target.start();
        future = executor.scheduleAtFixedRate(this, 0, 1000/FPS, TimeUnit.MILLISECONDS);
    }

    Rect srcRegion = new Rect();
    Rect destRegion = new Rect();
    public void run() {
        long dur = System.currentTimeMillis() - beginTick;
        try {
            if(dur >= ANIMATION_MILL) {
                tickLast();
                target.done(scrollDirection);
                future.cancel(false);

                return;
            }
            double rate = (1.0*dur)/ANIMATION_MILL;

            int height = prevPage.getHeight();
            int width = prevPage.getWidth();

            if(scrollDirection == Direction.Next) {
                tickNext(rate, height, width);
            }else {
                tickPrev(rate, height, width);
            }
        }catch(Exception e) {
            Log.d("WhiteBoardCast", "exception at animation thread: " + e.getMessage());
        }
    }

    private void tickLast() {
        int width = prevPage.getWidth();
        int height = prevPage.getHeight();
        if(scrollDirection == Direction.Next)
            tickNextLast(width, height);
        else
            tickPrevLast(width, height);
    }

    void clearDestCanvasSafe() {
        synchronized(destBmpForLock) {
            destCanvas.drawColor(Color.WHITE);
        }
    }

    void drawBmpDestCanvasSafe(Bitmap bmp, Rect src, Rect dest, Paint paint) {
        synchronized(destBmpForLock) {
            destCanvas.drawBitmap(bmp, src, dest, paint);
        }
    }

    void drawRectDestCanvasSafe(Rect rect, Paint paint) {
        synchronized(destBmpForLock) {
            destCanvas.drawRect(rect, paint);
        }
    }

    private void tickPrev(double rate, int height, int width) {
        if(height*rate+1 >= height) {
            tickPrevLast(height, width);
            return;
        }

        int border = (int)(height*rate);

        clearDestCanvasSafe();

        srcRegion.set(0, height-border, width, height);
        destRegion.set(0, 0, width, border);
        drawBmpDestCanvasSafe(prevPage, srcRegion, destRegion, null);

        srcRegion.set(0, border, width, border+1);
        drawRectDestCanvasSafe(srcRegion, blackPaint);

        srcRegion.set(0, 0, width, height-border-1);
        destRegion.set(0, border+1, width, height);
        drawBmpDestCanvasSafe(nextPage, srcRegion, destRegion, null);
        target.updateScreen();
    }

    private void tickPrevLast(int height, int width) {
        srcRegion.set(0, 0, width, height);
        destRegion.set(0, 0, width, height);
        clearDestCanvasSafe();
        drawBmpDestCanvasSafe(prevPage, srcRegion, destRegion, null);
    }


    private void tickNext(double rate, int height, int width) {
        if(height*rate+1 >= height) {
            tickNextLast(height, width);
            return;
        }
        clearDestCanvasSafe();

        int border = height - (int)(height*rate);
        srcRegion.set(0, height-border+1, width, height);
        destRegion.set(0, 0, width, border-1);
        drawBmpDestCanvasSafe(prevPage, srcRegion, destRegion, null);

        srcRegion.set(0, border-1, width, border);
        drawRectDestCanvasSafe(srcRegion, blackPaint);

        srcRegion.set(0, 0, width, height-border);
        destRegion.set(0, border, width, height);
        drawBmpDestCanvasSafe(nextPage, srcRegion, destRegion, null);
        target.updateScreen();
    }

    private void tickNextLast(int height, int width) {
        srcRegion.set(0, 0, width, height);
        destRegion.set(0, 0, width, height);
        clearDestCanvasSafe();
        drawBmpDestCanvasSafe(nextPage, srcRegion, destRegion, null);
    }

}