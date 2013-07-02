package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.DisplayMetrics;

import java.util.ArrayList;

/**
 * Created by karino on 7/2/13.
 */
public class FloatingOverlay {

    public static void highQualityStretch( Bitmap src, Bitmap dest )
    {
        dest.eraseColor( 0 );

        Canvas canvas = new Canvas( dest );
        Paint paint = new Paint();
        paint.setFilterBitmap( true );

        Matrix m = new Matrix();
        m.postScale( (float)dest.getWidth()/src.getWidth(), (float)dest.getHeight()/src.getHeight() );
        canvas.drawBitmap( src, m, paint );
    }
    private Bitmap floatResource( int id, int w, int h )
    {
        Bitmap dest = Bitmap.createBitmap( w, h, Config.ARGB_8888 );
        Bitmap bmp = BitmapFactory.decodeResource( activity.getResources(), id );
        highQualityStretch( bmp, dest );
        return dest;
    }


    Bitmap toolBarPanel = null;
    Bitmap toolBar = null;

    int toolHeight = 50;
    float dpi = 150;

    public int toolX = 4;
    public int toolY = 4;

    private int width = 32;
    private int height = 32;

    boolean touching;
    int touchOfsX = 0;
    int touchOfsY = 0;
    boolean dragging = false;

    final int TOOLBAR_HANDLE = 0;
    final int TOOLBAR_RECORD = 1;
    final int TOOLBAR_PEN = 2;
    final int TOOLBAR_UNDO = 3;
    final int TOOLBAR_REDO = 4;
    final int TOOLBAR_DONE = 5;
    final int TOOLBAR_CLEAR = 6;
    final int TOOLBAR_MENU = 7;

    public static final int PEN_INDEX_BLACK = 0;
    public static final int PEN_INDEX_RED = 1;
    public static final int PEN_INDEX_BLUE = 2;
    public static final int PEN_INDEX_GREEN = 3;
    public static final int PEN_INDEX_ERASER = 4;


    Bitmap recordButton;
    Bitmap penButton;
    Bitmap undoButton;
    Bitmap redoButton;
    Bitmap doneButton;
    Bitmap clearButton;
    Bitmap menuButton;

    Bitmap pullDownBG;
    Bitmap pullDownHiglight;
    Bitmap toolPen;
    ArrayList<Bitmap> toolPenIcon;

    void initToolbarImage() {
        toolBar = Bitmap.createBitmap(toolHeight *8, toolHeight, Config.ARGB_8888);
        toolBarPanel = floatResource(R.drawable.float_base, toolHeight *8, toolHeight);

        recordButton = floatResource(R.drawable.rec_button, toolHeight, toolHeight);
        penButton = floatResource(R.drawable.pen_button, toolHeight, toolHeight);
        undoButton = floatResource(R.drawable.undo_button, toolHeight, toolHeight);
        redoButton = floatResource(R.drawable.redo_button, toolHeight, toolHeight);
        doneButton = floatResource(R.drawable.done_button, toolHeight, toolHeight);
        clearButton = floatResource(R.drawable.clear_button, toolHeight, toolHeight);
        menuButton = floatResource(R.drawable.menu_button, toolHeight, toolHeight);

        pullDownBG = floatResource(R.drawable.pd_bg, toolHeight, toolHeight);
        pullDownHiglight = floatResource(R.drawable.pd_hilight, toolHeight, toolHeight);

        toolPenIcon = new ArrayList<Bitmap>();
        toolPenIcon.add(floatResource(R.drawable.pen_black_button, toolHeight, toolHeight));
        toolPenIcon.add(floatResource(R.drawable.pen_red_button, toolHeight, toolHeight));
        toolPenIcon.add(floatResource(R.drawable.pen_blue_button, toolHeight, toolHeight));
        toolPenIcon.add(floatResource(R.drawable.pen_green_button, toolHeight, toolHeight));
        toolPenIcon.add(floatResource(R.drawable.eraser_button, toolHeight, toolHeight));
        toolPen = Bitmap.createBitmap(toolHeight, toolHeight*toolPenIcon.size(), Config.ARGB_8888);



        updateToolbarImage();
    }

    int subIndex = -1;
    int penIndex = 0;

    private void updateToolbarImage() {
        Paint paint = new Paint();
        toolBar.eraseColor(0);
        Canvas canvas = new Canvas(toolBar);
        canvas.drawBitmap(toolBarPanel, 0, 0, null);
        canvas.drawBitmap(recordButton, toolHeight, 0, null);
        canvas.drawBitmap(toolPenIcon.get(penIndex), toolHeight *2, 0, null);
        canvas.drawBitmap(undoButton, toolHeight *3, 0, null);
        canvas.drawBitmap(redoButton, toolHeight *4, 0, null);
        canvas.drawBitmap(doneButton, toolHeight *5, 0, null);
        canvas.drawBitmap(clearButton, toolHeight *6, 0, null);
        canvas.drawBitmap(menuButton, toolHeight *7, 0, null);

        toolPen.eraseColor(0);
        canvas = new Canvas(toolPen);
        for(int i = 0; i < toolPenIcon.size(); i++) {
            canvas.drawBitmap(pullDownBG, 0, toolHeight*i, paint);
            canvas.drawBitmap(toolPenIcon.get(i), 0, toolHeight*i, paint);
            if(i == subIndex) canvas.drawBitmap(pullDownHiglight, 0, toolHeight*i, paint);
        }
    }

    WhiteBoardCastActivity activity;

    public FloatingOverlay(WhiteBoardCastActivity act,  float toolHeightCm) {
        activity = act;

        DisplayMetrics metrics = new DisplayMetrics();
        act.getWindowManager().getDefaultDisplay().getMetrics( metrics );

        dpi = (metrics.xdpi + metrics.ydpi) / 2;
        if (dpi < 10) dpi = 10;

        if (toolHeightCm <= 0)
        {
            // auto size.
            toolHeightCm = 0.65f;

            // make toolbar bigger for tablet.
            float w = metrics.widthPixels / dpi;
            float h = metrics.heightPixels / dpi;
            double len = Math.sqrt( w*w + h*h );
            if (len >= 7) toolHeightCm = 0.80f;

        }

        toolHeight = (int)(dpi * toolHeightCm / 2.54f) + 1;
        if (toolHeight < 10) toolHeight = 10;
        if (toolHeight > 512) toolHeight = 512;

        initToolbarImage();


    }


    public void onSize( int w, int h )
    {
        width = w;
        height = h;
        forceToolPos();
    }


    private void forceToolPos()
    {
        Bitmap src = toolBar;
        if (toolX + src.getWidth() > width) toolX = width - src.getWidth();
        if (toolY + src.getHeight() > height) toolY = height - src.getHeight();

        if (toolX < 0) toolX = 0;
        if (toolY < 0) toolY = 0;
    }

    Rect popupRect = new Rect();

    public void onDraw( Canvas canvas )
    {
        forceToolPos();
        canvas.drawBitmap( toolBar, toolX, toolY, null );

        if(penDown) {
            Bitmap src = toolPen;
            int x = toolX + toolHeight * TOOLBAR_PEN;
            int y = toolY - src.getHeight();
            if (toolY < height/2) y = toolY + toolHeight;
            canvas.drawBitmap( src, x, y, null );

            popupRect.set( x, y, x + src.getWidth(), y + src.getHeight() );
        }
    }

    public Bitmap toolBitmap() { return toolBar; }

    private int insideIndex( float gx, float gy )
    {
        Bitmap src = toolBitmap();
        if (gx < toolX) return -1;
        if (gy < toolY) return -1;
        if (toolX + src.getWidth() < gx) return -1;
        if (toolY + src.getHeight() < gy) return -1;

        return (int)((gx - toolX) / toolHeight);
    }

    boolean penDown = false;


    // if return true, should not respond outsize.
    public boolean onTouchDown( float gx, float gy )
    {
        int ix = (int)gx;
        int iy = (int)gy;

        int idx = insideIndex( gx, gy );
        touching = (idx != -1);
        dragging = (idx == TOOLBAR_HANDLE);
        if (touching)
        {
            touchOfsX = (int)gx - toolX;
            touchOfsY = (int)gy - toolY;
        }

        if (idx == TOOLBAR_RECORD) {
            activity.startRecord();
        } else if (idx == TOOLBAR_DONE) {
            activity.stopRecord();
        } else if (idx == TOOLBAR_MENU) {
            activity.toggleMenu();
        } else if (idx == TOOLBAR_CLEAR) {
            activity.clearCanvas();
        } else if (idx == TOOLBAR_PEN) {
            penDown = true;
            updateToolbarImage();
        }

        return touching;
    }
    public boolean onTouchMove( float gx, float gy )
    {
        int ix = (int)gx;
        int iy = (int)gy;

        subIndex = -1;

        if (dragging)
        {
            toolX = (int)gx - touchOfsX;
            toolY = (int)gy - touchOfsY;
            forceToolPos();
        }

        if (penDown)
        {
            if (popupRect.contains( ix, iy )) subIndex = (iy - popupRect.top) / toolHeight;
            updateToolbarImage();
        }

        return touching;
    }

    public boolean onTouchUp( float gx, float gy )
    {
        int ix = (int)gx;
        int iy = (int)gy;
        boolean res = touching;

        if(penDown && subIndex != -1) {
            penIndex = subIndex;
            updateToolbarImage();
            activity.setPenOrEraser(penIndex);
        }

        penDown = false;

        cancelOperation();

        return res;
    }

    public void cancelOperation()
    {
        touching = false;
        dragging = false;
    }
 }
