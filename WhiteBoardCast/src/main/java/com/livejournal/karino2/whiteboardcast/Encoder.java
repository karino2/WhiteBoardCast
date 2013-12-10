package com.livejournal.karino2.whiteboardcast;

import android.graphics.Rect;
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

/**
 * Created by karino on 6/27/13.
 */
public class Encoder {

    public synchronized boolean doneEncoder(StringBuilder error) {
        return doneEncoderCore(error);
    }

    void showError(StringBuilder errorDest, String msg) {
        errorDest.append(msg);
        Log.d("WhiteBoardCast", msg);
    }

    private boolean doneEncoderCore(StringBuilder error) {
        try {
            if (!muxerSegment.finalizeSegment()) {
                showError(error, "Finalization of segment failed.");
                return false;
            }
        } finally {
            finalizeEncoder();
        }
        return true;
    }

    public void finalizeEncoder() {
        if (encoder != null) {
            encoder.close();
        }
        if (encoderConfig != null) {
            encoderConfig.close();
        }
        if (mkvWriter != null) {
            mkvWriter.close();
        }
    }


    public synchronized boolean encodeFrames(int[] srcFrame, Rect invalRect, long framesToEncode, long fourcc, StringBuilder error) {
        if(!encodeOneFrame(srcFrame, invalRect, framesToEncode, fourcc, error))
            return false;
        framesIn = framesToEncode;

        return true;
    }

    LibVpxEncConfig encoderConfig = null;
    LibVpxEnc encoder = null;
    MkvWriter mkvWriter = null;
    Segment muxerSegment = null;
    long framesIn = 1;
    Rational timeMultiplier = null;
    long newVideoTrackNumber = 0;

    public boolean initEncoder(String webmOutputName,
                               int width, int height, int rate, int scale,
                               StringBuilder error) {
        // Log.d("WBCast", "width, height=" + width + "," + height);
        try {
            encoderConfig = new LibVpxEncConfig(width, height);
            encoder = new LibVpxEnc(encoderConfig);

            // libwebm expects nanosecond units
            encoderConfig.setTimebase(1, 1000000000);
            Rational timeBase = encoderConfig.getTimebase();
            Rational frameRate = new Rational(rate, scale);
            timeMultiplier = timeBase.multiply(frameRate).reciprocal();
            framesIn = 1;

            mkvWriter = new MkvWriter();
            if (!mkvWriter.open(webmOutputName)) {
                showError(error, "WebM Output name is invalid or error while opening.");
                return false;
            }

            muxerSegment = new Segment();
            if (!muxerSegment.init(mkvWriter)) {
                showError(error, "Could not initialize muxer segment.");
                return false;
            }

            SegmentInfo muxerSegmentInfo = muxerSegment.getSegmentInfo();
            muxerSegmentInfo.setWritingApp("y4mEncodeSample");

            newVideoTrackNumber = muxerSegment.addVideoTrack(width, height, 0);
            if (newVideoTrackNumber == 0) {
                showError(error, "Could not add video track.");
                return false;
            }
        }
        catch (LibVpxException e) {
            showError(error, "Encoder error : " + e);
            return false;
        }
        return true;
    }

    private boolean encodeOneFrame(int[] srcFrame, Rect invalRect, long endFrame, long fourcc,
                                   StringBuilder error) {
        long frameStart = timeMultiplier.multiply(framesIn - 1).toLong();
        long nextFrameStart = timeMultiplier.multiply(endFrame).toLong();

        if(endFrame > 0x0ffff) {
            Log.d("WhiteBoardCast",
                    String.format("endframe bigger than 4: %x, %x, %x, %x, %x", frameStart, nextFrameStart, framesIn, endFrame,  nextFrameStart-frameStart));
        }
        /*
        long test1 = timeMultiplier.multiply(0x7fffffffL).toLong();
        long test2 = timeMultiplier.multiply(0x80000000L).toLong();
        long test3 = test2-test1;
        */

        ArrayList<VpxCodecCxPkt> encPkt = null;
        try {
            encPkt = encoder.convertIntEncodeFrameRegion(
                    srcFrame, invalRect.left, invalRect.top, invalRect.width(), invalRect.height(),
                    frameStart, nextFrameStart - frameStart, fourcc);
            for (int i = 0; i < encPkt.size(); i++) {
                VpxCodecCxPkt pkt = encPkt.get(i);
                final boolean isKey = (pkt.flags & 0x1) == 1;

                if(pkt.pts < 0) {
                    Log.d("WhiteBoardCast",
                            String.format("pkt negative: %d, %d, %d, %x, %x, %x, %x, %x, %x", pkt.pts, -pkt.pts,frameStart ,pkt.pts, frameStart, nextFrameStart, framesIn, endFrame,  nextFrameStart-frameStart));
                }
                if (!muxerSegment.addFrame(pkt.buffer, newVideoTrackNumber, pkt.pts, isKey)) {
                    showError(error, String.format("Could not add frame. %d, %d, %x, %x", pkt.pts, frameStart, pkt.pts, frameStart));
                    return false;
                }
            }
        } catch (LibVpxException e) {
            showError(error, "Encoder error : " + e);
            return false;
        }
        return true;
    }

}
