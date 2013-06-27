package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Rect;

/**
 * Created by karino on 6/26/13.
 */
public interface FrameRetrieval {
    public void pullUpdateRegion(int[] pixelBufs, Rect inval);
}
