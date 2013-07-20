package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Rect;
import android.os.Environment;
import android.util.Log;

import com.google.libvpx.LibVpxEnc;
import com.google.libvpx.LibVpxEncConfig;
import com.google.libvpx.LibVpxException;
import com.google.libvpx.Rational;
import com.google.libvpx.VpxCodecCxPkt;
import com.google.libwebm.mkvmuxer.MkvWriter;
import com.google.libwebm.mkvmuxer.Segment;
import com.google.libwebm.mkvmuxer.SegmentInfo;

import java.util.ArrayList;
import java.util.TimerTask;

/**
 * Created by karino on 6/26/13.
 */
public class EncoderTask implements Runnable {
    public interface FpsListener {
        void push(long currentFrameMill);
    }

    Bitmap bitmap;
    FrameRetrieval retrieval;
    int[] pixelBuf;
    Encoder encoder;
    StringBuilder errorBuf;
    int width;
    int height;
    long beginMillis=0;
    String workVideoPath;

    public EncoderTask(FrameRetrieval frameR, Bitmap parentBmp, String workVideoPath) {
        retrieval = frameR;
        updateBitmap(parentBmp);
        errorBuf = new StringBuilder();
        encoder = new Encoder();
        this.workVideoPath = workVideoPath;
    }

    static final int FPS_NUM = 24;
    static final int FPS_DENOM = 1;

    public boolean initEncoder(long currentMill) {
        boolean res = encoder.initEncoder(workVideoPath, width, height, FPS_NUM, FPS_DENOM, errorBuf);
        beginMillis = currentMill;
        return res;
    }


    int stride;
    public void updateBitmap(Bitmap parentBmp) {
        bitmap = Bitmap.createBitmap(parentBmp);
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
        return encoder.encodeFrames(frame,  invalRect, (int)(diff*FPS_NUM/1000), LibVpxEnc.FOURCC_ARGB, errorBuf);
    }


    // when call doneEncoder, you do not need to call finalizeEncoder()
    public boolean doneEncoder(Encoder.FinalizeListener listener) {
        return encoder.doneEncoder(errorBuf, listener);
    }

    public void finalizeEncoder() {
        encoder.finalizeEncoder();
    }

    Rect invalRect = new Rect();
    // scheduleAtFixedRate(TimerTask task, long delay, long period)
    @Override
    public void run() {
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
