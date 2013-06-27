package com.livejournal.karino2.whiteboardcast;

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
    public boolean doneEncoder(StringBuilder error) {
        try {
            if (!muxerSegment.finalizeSegment()) {
                error.append("Finalization of segment failed.");
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

    public boolean encodeFrames(int[] srcFrame,int framesToEncode, long fourcc, StringBuilder error) {
        while (framesIn < framesToEncode) {
            if(!encodeOneFrame(srcFrame, fourcc, error))
                return false;
            framesIn++;
        }
        return true;
    }

    LibVpxEncConfig encoderConfig = null;
    LibVpxEnc encoder = null;
    MkvWriter mkvWriter = null;
    Segment muxerSegment = null;
    int framesIn = 1;
    Rational timeMultiplier = null;
    long newVideoTrackNumber = 0;

    public boolean initEncoder(String webmOutputName,
                               int width, int height, int rate, int scale,
                               StringBuilder error) {
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
                error.append("WebM Output name is invalid or error while opening.");
                return false;
            }

            muxerSegment = new Segment();
            if (!muxerSegment.init(mkvWriter)) {
                error.append("Could not initialize muxer segment.");
                return false;
            }

            SegmentInfo muxerSegmentInfo = muxerSegment.getSegmentInfo();
            muxerSegmentInfo.setWritingApp("y4mEncodeSample");

            newVideoTrackNumber = muxerSegment.addVideoTrack(width, height, 0);
            if (newVideoTrackNumber == 0) {
                error.append("Could not add video track.");
                return false;
            }
        }
        catch (LibVpxException e) {
            error.append("Encoder error : " + e);
            return false;
        }
        return true;
    }

    private boolean encodeOneFrame(int[] srcFrame, long fourcc,
                                   StringBuilder error) {
        long frameStart = timeMultiplier.multiply(framesIn - 1).toLong();
        long nextFrameStart = timeMultiplier.multiply(framesIn).toLong();

        ArrayList<VpxCodecCxPkt> encPkt = null;
        try {
            encPkt = encoder.convertIntEncodeFrame(
                    srcFrame, frameStart, nextFrameStart - frameStart, fourcc);
            for (int i = 0; i < encPkt.size(); i++) {
                VpxCodecCxPkt pkt = encPkt.get(i);
                final boolean isKey = (pkt.flags & 0x1) == 1;

                if (!muxerSegment.addFrame(pkt.buffer, newVideoTrackNumber, pkt.pts, isKey)) {
                    error.append("Could not add frame.");
                    return false;
                }
            }
        } catch (LibVpxException e) {
            error.append("Encoder error : " + e);
            return false;
        }
        return true;
    }

    static public boolean encodeIntRgbFrameExample(String webmOutputName,
                                                   int[] srcFrame, long fourcc, int width, int height, int rate, int scale,
                                                   int framesToEncode, StringBuilder error) {
        LibVpxEncConfig encoderConfig = null;
        LibVpxEnc encoder = null;
        MkvWriter mkvWriter = null;

        try {
            encoderConfig = new LibVpxEncConfig(width, height);
            encoder = new LibVpxEnc(encoderConfig);

            // libwebm expects nanosecond units
            encoderConfig.setTimebase(1, 1000000000);
            Rational timeBase = encoderConfig.getTimebase();
            Rational frameRate = new Rational(rate, scale);
            Rational timeMultiplier = timeBase.multiply(frameRate).reciprocal();
            int framesIn = 1;

            mkvWriter = new MkvWriter();
            if (!mkvWriter.open(webmOutputName)) {
                error.append("WebM Output name is invalid or error while opening.");
                return false;
            }

            Segment muxerSegment = new Segment();
            if (!muxerSegment.init(mkvWriter)) {
                error.append("Could not initialize muxer segment.");
                return false;
            }

            SegmentInfo muxerSegmentInfo = muxerSegment.getSegmentInfo();
            muxerSegmentInfo.setWritingApp("y4mEncodeSample");

            long newVideoTrackNumber = muxerSegment.addVideoTrack(width, height, 0);
            if (newVideoTrackNumber == 0) {
                error.append("Could not add video track.");
                return false;
            }

            while (framesIn < framesToEncode) {
                long frameStart = timeMultiplier.multiply(framesIn - 1).toLong();
                long nextFrameStart = timeMultiplier.multiply(framesIn).toLong();

                ArrayList<VpxCodecCxPkt> encPkt = encoder.convertIntEncodeFrame(
                        srcFrame, frameStart, nextFrameStart - frameStart, fourcc);
                for (int i = 0; i < encPkt.size(); i++) {
                    VpxCodecCxPkt pkt = encPkt.get(i);
                    final boolean isKey = (pkt.flags & 0x1) == 1;

                    if (!muxerSegment.addFrame(pkt.buffer, newVideoTrackNumber, pkt.pts, isKey)) {
                        error.append("Could not add frame.");
                        return false;
                    }
                }

                ++framesIn;
            }

            if (!muxerSegment.finalizeSegment()) {
                error.append("Finalization of segment failed.");
                return false;
            }

        } catch (LibVpxException e) {
            error.append("Encoder error : " + e);
            return false;
        } finally {
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

        return true;
    }
}
