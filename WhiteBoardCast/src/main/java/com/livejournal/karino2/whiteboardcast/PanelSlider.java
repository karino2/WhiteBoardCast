package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;

/**
 * Created by karino on 3/24/14.
 */
public class PanelSlider {

    WhiteBoardCastActivity activity;
    int toolUnit;
    Bitmap mView;
    Paint paint;
    Canvas canvas;
    Rect region;


    public PanelSlider( int toolUnit, WhiteBoardCastActivity act)
    {
        this.toolUnit = toolUnit;
        activity = act;
        paint = new Paint();

        mView = Bitmap.createBitmap( toolUnit*4, toolUnit, Bitmap.Config.ARGB_8888 );
        canvas = new Canvas(mView);
        region = new Rect();
        updatePanel();
    }

    public int width(){ return mView.getWidth(); }
    public int height(){ return mView.getHeight(); }
    public Bitmap view() { return mView; }


    // 0 to 100.
    int pos = WhiteBoardCanvas.DEFAULT_PEN_WIDTH;
    public int getSize() {
        return pos;
    }

    int beginX, beginPos;
    boolean dragging = false;
    public void onDown( int ix, int iy )
    {
        dragging = true;
        beginX = ix;
        beginPos = getSize();
    }


    public boolean isSnap() {
        return dragging;
    }
    public void onMove(int ix, int iy) {
        if(!dragging)
            return;

        int deltaX = 100*(ix-beginX)/width();
        pos = Math.min(100, Math.max(0, beginPos+deltaX));
        updatePanel();
    }

    public boolean onUp() {
        boolean needInval = dragging;
        if(dragging)
            activity.setPenOeEraserSize(pos);
        dragging = false;
        return needInval;
    }

    private void updatePanel() {
        mView.eraseColor( 0xFFc0c0c0 );
        int pos = getSize();

        int dy = 0;
        int rectRight = Math.max(2, width()*pos/100 - 2);
        region.set(2, dy + 2, rectRight, dy + height() - 2);

        paint.setColor(Color.WHITE);
        canvas.drawRect(region, paint);

        paint.setColor(Color.BLACK);
        paint.setTextSize(toolUnit/2);
        if(width()-rectRight > (15*toolUnit)/20) {
            canvas.drawText(String.valueOf(pos), (float) rectRight, (float) height(), paint);
        } else {
            canvas.drawText(String.valueOf(pos), (float) rectRight - (15 * toolUnit) / 20, (float) height(), paint);
        }


    }

    public void setPos(int size) {
        pos = size;
        updatePanel();
    }
}
