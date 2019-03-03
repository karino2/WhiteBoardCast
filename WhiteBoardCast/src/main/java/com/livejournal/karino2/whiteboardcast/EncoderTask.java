package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaMuxer;

/**
 * Created by karino on 6/26/13.
 */
public class EncoderTask implements Runnable {
    public interface FpsListener {
        void push(long currentFrameMill);
    }

    public interface ErrorListener {
        void postErrorMessage(String msg);
    }

    ErrorListener errorListener;
    Bitmap bitmap;
    FrameRetrieval retrieval;
    int[] pixelBuf;
    AvcVideoEncoder videoEncoder;
    StringBuilder errorBuf;
    int width;
    int height;
    long beginMillis;
    String workVideoPath;

    public EncoderTask(FrameRetrieval frameR, Bitmap parentBmp, String workVideoPath, ErrorListener elistn, long currentMil, AudioVideoMuxer muxer) {
        retrieval = frameR;
        updateBitmap(parentBmp);
        errorBuf = new StringBuilder();
        this.workVideoPath = workVideoPath;
        errorListener = elistn;
        beginMillis = currentMil;

        videoEncoder = new AvcVideoEncoder(width, height, FPS_NUM, FPS_DENOM, muxer);
    }

    static final int FPS_NUM = 24;
    static final int FPS_DENOM = 1;

    public boolean initEncoder(long currentMill) {
        // TODO: remove
        return true;
    }


    int stride;
    public void updateBitmap(Bitmap parentBmp) {
        synchronized (parentBmp) {
            bitmap = Bitmap.createBitmap(parentBmp);
        }
        width = bitmap.getWidth();
        height = bitmap.getHeight();
        int bufLen = width*height;
        stride = width;
        if(pixelBuf == null || pixelBuf.length != bufLen) {
            pixelBuf = new int[bufLen];
            bitmap.getPixels(pixelBuf, 0, width, 0, 0, width, height);
        }
    }

    public boolean encodeFrame(int[] frame, Rect invalRect) {
        long curr = System.currentTimeMillis();
        long diff = curr-beginMillis;
        return videoEncoder.encodeFrames(frame,  invalRect, (int)(diff*FPS_NUM/1000), errorBuf);
    }


    // when call doneEncoder, you do not need to call finalizeEncoder()
    public synchronized boolean doneEncoder() {
        return videoEncoder.doneEncoder(errorBuf);
    }

    public void finalizeEncoder() {
        videoEncoder.finalizeEncoder();
    }

    Rect invalRect = new Rect();
    @Override
    public void run() {
        try {
            doWholeTask();
        }catch(RuntimeException e) {
            errorListener.postErrorMessage("Unknown videoEncoder runtime exception: " + e.getMessage());
        }
    }

    private synchronized void doWholeTask() {
        checkFrameRate();
        retrieval.pullUpdateRegion(pixelBuf, invalRect);
        encodeFrame(pixelBuf, invalRect);
    }

    private void checkFrameRate() {
        if(fpsListener == null)
            return;

        fpsListener.push(System.currentTimeMillis());
    }

    public StringBuilder getErrorBuf() {
        return errorBuf;
    }

    public void stop() {
        // do nothing.
    }

    public void resume(long suspendedDurMil) {
        beginMillis += suspendedDurMil;

    }

    FpsListener fpsListener;
    public void setFpsListener(FpsListener listener) {
        fpsListener = listener;
    }

}
