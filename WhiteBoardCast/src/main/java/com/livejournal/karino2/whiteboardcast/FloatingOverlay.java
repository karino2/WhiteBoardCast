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

    boolean touchingToolBar;
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
    final int TOOLBAR_POP = 7;
    final int TOOLBAR_MENU = 8;
    final int TOOLBAR_BUTTON_NUM = TOOLBAR_MENU+1;

    static final int REC_NORMAL = 0;
    static final int REC_SETUP = 1;
    static final int REC_PAUSE = 2;


    Rect upButtonRect;
    Rect downButtonRect;
    Bitmap upButton;
    Bitmap downButton;

    ArrayList<Bitmap> recIcon;
    Bitmap undoButton;
    Bitmap redoButton;
    Bitmap doneButton;
    Bitmap clearButton;
    Bitmap popButton;
    Bitmap menuButton;

    Bitmap pullDownBG;
    Bitmap pullDownHiglight;
    Bitmap toolPen;

    boolean pickerShowAbove = true;
    boolean pickerVisible = false;

    void initToolbarImage() {
        toolBar = Bitmap.createBitmap(toolHeight *TOOLBAR_BUTTON_NUM, toolHeight, Config.ARGB_8888);
        toolBarPanel = floatResource(R.drawable.float_base, toolHeight *TOOLBAR_BUTTON_NUM, toolHeight);

        undoButton = floatResource(R.drawable.undo_button, toolHeight, toolHeight);
        redoButton = floatResource(R.drawable.redo_button, toolHeight, toolHeight);
        doneButton = floatResource(R.drawable.done_button, toolHeight, toolHeight);
        clearButton = floatResource(R.drawable.clear_button, toolHeight, toolHeight);
        popButton = floatResource(R.drawable.slides_button, toolHeight, toolHeight);
        menuButton = floatResource(R.drawable.menu_button, toolHeight, toolHeight);

        pullDownBG = floatResource(R.drawable.pd_bg, toolHeight, toolHeight);
        pullDownHiglight = floatResource(R.drawable.pd_hilight, toolHeight, toolHeight);

        toolPen = floatResource(R.drawable.pen_button, toolHeight, toolHeight);

        recIcon = new ArrayList<Bitmap>();
        recIcon.add(floatResource(R.drawable.rec_button, toolHeight, toolHeight));
        recIcon.add(floatResource(R.drawable.rec_setupping, toolHeight, toolHeight));
        recIcon.add(floatResource(R.drawable.pause_button, toolHeight, toolHeight));

        updateToolbarImage();
    }


    private void initPageUpDownImage() {
        upButton = floatResource(R.drawable.page_up_button, toolHeight, toolHeight);
        downButton = floatResource(R.drawable.page_down_button, toolHeight, toolHeight);
    }

    private void updateToolbarImage() {
        toolBar.eraseColor(0);
        Canvas canvas = new Canvas(toolBar);
        canvas.drawBitmap(toolBarPanel, 0, 0, null);

        canvas.drawBitmap(getRecIcon(), toolHeight, 0, null);

        canvas.drawBitmap(toolPen, toolHeight * 2, 0, null);

        Paint undoPaint = activity.canUndo()? null: disablePaint;
        Paint redoPaint = activity.canRedo()? null: disablePaint;
        canvas.drawBitmap(undoButton, toolHeight * 3, 0, undoPaint);
        canvas.drawBitmap(redoButton, toolHeight * 4, 0, redoPaint);

        Paint doenPaint = activity.canStop()? null: disablePaint;
        canvas.drawBitmap(doneButton, toolHeight * 5, 0, doenPaint);
        canvas.drawBitmap(clearButton, toolHeight * 6, 0, null);

        Paint popPaint = activity.slideAvailable()? null: disablePaint;
        canvas.drawBitmap(popButton, toolHeight * 7, 0, popPaint);
        canvas.drawBitmap(menuButton, toolHeight * 8, 0, null);

    }


    WhiteBoardCastActivity activity;
    ColorPicker picker;
    Paint disablePaint;


    public FloatingOverlay(WhiteBoardCastActivity act,  float toolHeightCm) {
        activity = act;


        DisplayMetrics metrics = new DisplayMetrics();
        act.getWindowManager().getDefaultDisplay().getMetrics(metrics);

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

        upButtonRect = new Rect();
        downButtonRect = new Rect();

        disablePaint = new Paint();
        disablePaint.setAlpha(32);


        initPageUpDownImage();
        initToolbarImage();

        picker = new ColorPicker(toolHeight, activity, activity);


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

        int ypos = pickerYPos();
        if(ypos < 0 || ypos +picker.height() > height)
            pickerShowAbove = !pickerShowAbove;
        picker.setPosition(toolX, pickerYPos());
    }

    int pickerYPos() {
        if(pickerShowAbove)
            return toolY-picker.height();
        return toolY+toolHeight;
    }

    public void onDraw( Canvas canvas )
    {
        drawPageUpDownButton(canvas);
        drawToolBar(canvas);

        if(pickerVisible)
            picker.draw(canvas);
    }

    private void drawPageUpDownButton(Canvas canvas) {
        Paint semiTransparentPaint = new Paint();
        semiTransparentPaint.setAlpha(128);
        int pageUpDownX = width - upButton.getWidth();
        int pageDownY = height - upButton.getHeight();

        canvas.drawBitmap( upButton, pageUpDownX, 0, semiTransparentPaint );
        upButtonRect.set(pageUpDownX, 0, pageUpDownX+upButton.getWidth(), upButton.getHeight());

        canvas.drawBitmap( downButton, pageUpDownX, pageDownY, semiTransparentPaint);
        downButtonRect.set(pageUpDownX, pageDownY, pageUpDownX+downButton.getWidth(), pageDownY+downButton.getHeight());
    }

    private void drawToolBar(Canvas canvas) {
        forceToolPos();
        canvas.drawBitmap( toolBar, toolX, toolY, null );
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

    // if return true, should not respond outsize.
    public boolean onTouchDown( float gx, float gy )
    {
        int ix = (int)gx;
        int iy = (int)gy;


        if(pickerVisible && picker.isInside(ix, iy))
        {
            picker.onDown(ix, iy);
            return true;
        }

        int idx = insideIndex( gx, gy );
        touchingToolBar = (idx != -1);
        if (!touchingToolBar) {
            if(upButtonRect.contains(ix, iy)) {
                activity.pageUp();
                return true;
            } else if(downButtonRect.contains(ix, iy)) {
                activity.pageDown();
                return true;
            }
        }

        dragging = (idx == TOOLBAR_HANDLE);
        if (touchingToolBar)
        {
            touchOfsX = (int)gx - toolX;
            touchOfsY = (int)gy - toolY;
        }

        if (idx == TOOLBAR_RECORD) {
            handleRecordButtonPressed();
        } else if (idx == TOOLBAR_DONE) {
            if(activity.canStop()) {
                activity.stopRecord();
                updateToolbarImage();
            }
        } else if (idx == TOOLBAR_MENU) {
            activity.openOptionsMenu();
        } else if (idx == TOOLBAR_POP) {
            if(activity.slideAvailable()) {
                activity.toggleShowSlides();
                updateToolbarImage();
            }
        } else if (idx == TOOLBAR_CLEAR) {
            activity.clearCanvas();
        } else if (idx == TOOLBAR_PEN) {
            pickerVisible = !pickerVisible;
        } else if (idx == TOOLBAR_UNDO) {
            if(activity.canUndo()) {
                activity.undo();
                updateToolbarImage();
            }
        } else if (idx == TOOLBAR_REDO) {
            if(activity.canRedo()) {
                activity.redo();
                updateToolbarImage();
            }
        }

        return touchingToolBar;
    }

    private void handleRecordButtonPressed() {
        Presentation.RecordStatus stats = activity.getRecStats();

        switch(stats) {
            case DORMANT:
            case DONE:
                activity.startRecord();
                break;
            case PAUSE:
                activity.resumeRecord();
                break;
            case RECORDING:
                activity.pauseRecord();
                break;
            case SETUP:
            case DONE_PROCESS:
                break; // do nothing.
        }
    }

    public boolean onTouchMove( float gx, float gy )
    {
        int ix = (int)gx;
        int iy = (int)gy;

        if(picker.isSnap()) {
            picker.onMove(ix, iy);
            return true;
        }

        if (dragging)
        {
            toolX = (int)gx - touchOfsX;
            toolY = (int)gy - touchOfsY;
            forceToolPos();
        }

        return touchingToolBar;
    }

    public boolean onTouchUp( float gx, float gy )
    {
        int ix = (int)gx;
        int iy = (int)gy;
        boolean res = touchingToolBar;

        // no side effect.
        picker.onUp(ix, iy);

        cancelOperation();

        return res;
    }

    public void cancelOperation()
    {
        touchingToolBar = false;
        dragging = false;
    }

    public void changeRecStatus() {
        updateToolbarImage();
    }

    public void changeUndoStatus() {
        updateToolbarImage();
    }

    public Bitmap getRecIcon() {
        Presentation.RecordStatus stats = activity.getRecStats();

        switch(stats) {
            case SETUP:
            case DONE_PROCESS:
                return recIcon.get(REC_SETUP);
            case RECORDING:
                return recIcon.get(REC_PAUSE);
            case DONE:
            case DORMANT:
            case PAUSE:
            default:
                return recIcon.get(REC_NORMAL);
        }
    }

    public void changeSlidesStatus() {
        updateToolbarImage();
    }

    public boolean isEraser() {
        return picker.isEraser();
    }

    public void setSliderPos(int size) {
        picker.setSliderPos(size);
    }
}
