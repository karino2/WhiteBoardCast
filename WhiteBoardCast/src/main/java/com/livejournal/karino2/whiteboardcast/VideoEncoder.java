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

public interface VideoEncoder {
    boolean doneEncoder(StringBuilder error);
    void finalizeEncoder();
    boolean encodeFrames(int[] srcFrame, Rect invalRect, int framesToEncode, StringBuilder error);
}


