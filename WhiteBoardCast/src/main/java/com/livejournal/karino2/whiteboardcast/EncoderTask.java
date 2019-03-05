package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.media.MediaMuxer;

/**
 * Created by karino on 6/26/13.
 */
public class EncoderTask implements Runnable {
    public void setBeginMill(long newMill) {
        beginMillis = newMill;
        videoEncoder.setBeginMill(newMill);
    }

    public void startEncoder() {
        videoEncoder.start();
    }

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
    long beginMillis = 0;
    String workVideoPath;
    AudioVideoMuxer muxer;

    public EncoderTask(FrameRetrieval frameR, Bitmap parentBmp, String workVideoPath, ErrorListener elistn, AudioVideoMuxer in_muxer) {
        muxer = in_muxer;
        retrieval = frameR;
        updateBitmap(parentBmp);
        errorBuf = new StringBuilder();
        this.workVideoPath = workVideoPath;
        errorListener = elistn;

        createEncoder();
    }

    private void createEncoder() {
        videoEncoder = AvcVideoEncoder.Companion.createInstance(width, height, muxer);
    }

    static final int FPS_NUM = 24;

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
        return videoEncoder.encodeFrames(frame,  invalRect, errorBuf);
    }


    // when call doneEncoder, you do not need to call finalizeEncoder()
    public synchronized void doneEncoder() {
        videoEncoder.doneEncoder(errorBuf);
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

    public void pause() {
        // do nothing.
    }

    public void stop() {
        // do nothing.
    }

    public void resume(long suspendedDurMil) {
        beginMillis += suspendedDurMil;
        videoEncoder.setBeginMill(beginMillis);
    }

    FpsListener fpsListener;
    public void setFpsListener(FpsListener listener) {
        fpsListener = listener;
    }

}
