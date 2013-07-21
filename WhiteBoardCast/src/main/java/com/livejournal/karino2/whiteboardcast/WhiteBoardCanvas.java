package com.livejournal.karino2.whiteboardcast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

/**
 * Created by karino on 6/26/13.
 */
public class WhiteBoardCanvas extends View implements FrameRetrieval  {

    Bitmap viewBmp;
    Bitmap commitedBmp;
    private Canvas mCanvas;
    Canvas commitedCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint       mPaint;
    private Paint mCursorPaint;
    private Rect invalRegion;

    FloatingOverlay overlay;

    static final int DEFAULT_PEN_WIDTH = 6;
    static final int ERASER_WIDTH = 60;
    UndoList undoList = new UndoList();

    public WhiteBoardCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        overlay = new FloatingOverlay((WhiteBoardCastActivity)context, 0);

        mPaint = new Paint();
        mPaint.setColor(Color.DKGRAY);
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(DEFAULT_PEN_WIDTH);
        invalRegion = new Rect(0, 0, 0, 0);

        mCursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCursorPaint.setStyle(Paint.Style.STROKE);
        mCursorPaint.setPathEffect(new DashPathEffect(new float[]{5, 2}, 0));
    }

    int mWidth;
    int mHeight;
    int mX1, mX2, mY1, mY2;
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        // align for livbpx.
        // Log.d("WBCast", "before, w,h=" + w+ "," + h);
        w = (w+15) & ~15;
        h = (h+15) & ~15;
        // Log.d("WBCast", "after, w,h=" + w+ "," + h);
        mCenterX = ((float)w)/2F;
        mCenterY = ((float)h)/2F;
        mWidth = w;
        mHeight = h;
        mX1 = CROSS_SIZE*2;
        mY1 = CROSS_SIZE*2;
        mX2 = mWidth-(CROSS_SIZE*2);
        mY2 = mHeight-(CROSS_SIZE*2);
        resetCanvas(w, h);
        overlay.onSize(w, h);
    }

    public void resetCanvas() {
        resetCanvas(viewBmp.getWidth(), viewBmp.getHeight());
        invalidate();
    }
    public void resetCanvas(int w, int h) {
        viewBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        viewBmp.eraseColor(Color.WHITE);
        commitedBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        commitedBmp.eraseColor(Color.WHITE);
        mCanvas = new Canvas(viewBmp);
        commitedCanvas = new Canvas(commitedBmp);
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFFFFFFF);

        synchronized(viewBmp) {
            canvas.drawBitmap(viewBmp, 0, 0, mBitmapPaint);
        }

        // canvas.drawPath(mPath, mPaint);

        // should write to viewBmp. but later.
        if(mBrushCursorRegion.width() >= 0.1)
            canvas.drawOval(mBrushCursorRegion, mCursorPaint);

        overlay.onDraw(canvas);

        drawFps(canvas);

    }

    Paint fpsPaint = new Paint();
    private void drawFps(Canvas canvas) {
        if(!showFpsBar)
            return;
        drawFpsBar(canvas, encoderFpsCounter.cycleFps(), 5, 0xFF0000FF);
        drawFpsBar(canvas, paintFpsCounter.cycleFps(), 5+12, 0xFFFF0000);
    }

    private void drawFpsBar(Canvas canvas, int fps, int py, int fgColor) {
        if(fps == -1)
            return;
        fpsPaint.setColor(fgColor);
        final int px = 25;
        int m = 5;
        canvas.drawRect( new Rect( px, py, px + fps*m, py + 8), fpsPaint);

        fpsPaint.setColor(0xFFFFFF00);
        for (int i=0; i<=fps/10; i++)
        {
            canvas.drawRect( new Rect( px + i*m*10, py, px + i*m*10 + 2, py + 8), fpsPaint);
        }
    }

    private final int CROSS_SIZE = 20;
    private void drawCross(Canvas canvas, float x, float y) {
        canvas.drawLine(x-CROSS_SIZE, y, x+CROSS_SIZE, y, mCursorPaint);
        canvas.drawLine(x, y-CROSS_SIZE, x, y+CROSS_SIZE, mCursorPaint);
    }

    private float mCenterX, mCenterY;

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    RectF mBrushCursorRegion = new RectF(0f, 0f, 0f, 0f);

    private float getCursorSize() {
        return (float)penWidth;
    }

    private void setBrushCursorPos(float x, float y)
    {
        mBrushCursorRegion.set(x-getCursorSize()/2, y-getCursorSize()/2,
                x+getCursorSize()/2, y+getCursorSize()/2);

    }

    private void eraseBrushCursor() {
        mBrushCursorRegion.set(0f, 0f, 0f, 0f);
    }


    boolean mDownHandled = false;
    private RectF invalF = new RectF();
    private Rect tmpInval = new Rect();

    private boolean overTolerance(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        return (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE);
    }

    public boolean onTouchEvent(MotionEvent event) {

        float x = event.getX();
        float y = event.getY();
        setBrushCursorPos(x, y);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(overlay.onTouchDown(x, y)) {
                    invalidate();
                    break;
                }
                mDownHandled = true;
                mPath.reset();
                mPath.moveTo(x, y);
                mX = x;
                mY = y;
                invalidate();
                break;
            case MotionEvent.ACTION_MOVE:
                paintFpsCounter.push(System.currentTimeMillis());
                if(overlay.onTouchMove(x, y)) {
                    invalidate();
                    break;
                }
                if(!mDownHandled)
                    break;
                if (overTolerance(x, y)) {
                    for(int i = 0; i < event.getHistorySize(); i++) {
                        float hx = event.getHistoricalX(i);
                        float hy = event.getHistoricalY(i);
                        if(overTolerance(hx, hy)) {
                            mPath.quadTo(mX, mY, (hx + mX)/2, (hy + mY)/2);
                            mX = hx;
                            mY = hy;
                        }
                    }
                    updateInvalRegion();
                    mCanvas.drawPath(mPath, mPaint);
                }
                // no tolerance
                /*
                mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                mX = x;
                mY = y;
                updateInvalRegion();
                mCanvas.drawPath(mPath, mPaint);
                */
                // no tolerance done.

                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                eraseBrushCursor();
                if(overlay.onTouchUp(x, y)) {
                    invalidate();
                    break;
                }
                if(!mDownHandled)
                    break;
                mDownHandled = false;
                mPath.lineTo(mX, mY);

                boolean canUndoBefore = undoList.canUndo();

                Rect region = pathBound();
                Bitmap undo = Bitmap.createBitmap(commitedBmp, region.left, region.top, region.width(), region.height() );
                commitedCanvas.drawPath(mPath, mPaint);
                Bitmap redo = Bitmap.createBitmap(commitedBmp, region.left, region.top, region.width(), region.height());
                undoList.pushUndoCommand(region.left, region.top, undo, redo);
                invalRegion.union(region);
                mCanvas.drawPath(mPath, mPaint);
                mPath.reset();
                if(undoList.canUndo() != canUndoBefore) {
                    overlay.changeUndoStatus();
                }
                invalidate();
                break;
        }
        return true;

    }



    private Rect pathBound() {
        mPath.computeBounds(invalF, false);
        invalF.roundOut(tmpInval);
        widen(tmpInval);
        return tmpInval;
    }


    private void updateInvalRegion() {
        invalRegion.union(pathBound());
    }

    private void widen(Rect tmpInval) {
        int penWidth = (int)getCursorSize();
        int newLeft = Math.max(0, tmpInval.left- penWidth);
        int newTop = Math.max(0, tmpInval.top - penWidth);
        int newRight = Math.min(mWidth, tmpInval.right+ penWidth);
        int newBottom = Math.min(mHeight, tmpInval.bottom+ penWidth);
        tmpInval.set(newLeft, newTop, newRight, newBottom);
    }

    public Bitmap getBitmap() { return viewBmp;}

    public void setWholeAreaInvalidate() {
        invalRegion.set(0, 0, mWidth, mHeight);
    }

    public boolean canUndo() {
        return undoList.canUndo();
    }

    public void undo() {
        Rect undoInval = undoList.undo(commitedCanvas, mPaint);
        afterUndoRedo(undoInval);
    }

    public void redo() {
        Rect undoInval = undoList.redo(commitedCanvas, mPaint);
        afterUndoRedo(undoInval);
    }

    private void afterUndoRedo(Rect undoInval) {
        synchronized (viewBmp) {
            mCanvas.drawBitmap(commitedBmp, undoInval, undoInval, mPaint);
            invalRegion.union(undoInval);
        }
        invalidate(undoInval);
    }


    @Override
    public void pullUpdateRegion(int[] pixelBufs, Rect inval) {
        synchronized(viewBmp) {
            inval.set(invalRegion);
            int stride = viewBmp.getWidth();
            int offset = inval.left+inval.top*stride;
            viewBmp.getPixels(pixelBufs, offset, stride,  inval.left, inval.top, inval.width(), inval.height());

            invalRegion.set(0, 0, 0, 0);
        }
    }

    public void clearCanvas() {
        boolean canUndoBefore = undoList.canUndo();
        Bitmap undo = Bitmap.createBitmap(commitedBmp, 0, 0, viewBmp.getWidth(), viewBmp.getHeight() );

        commitedBmp.eraseColor(Color.WHITE);
        viewBmp.eraseColor(Color.WHITE);
        invalRegion.set(0, 0, viewBmp.getWidth(), viewBmp.getHeight());

        Bitmap redo = Bitmap.createBitmap(commitedBmp, 0, 0, viewBmp.getWidth(), viewBmp.getHeight() );
        undoList.pushUndoCommand(0, 0, undo, redo);
        if(undoList.canUndo() != canUndoBefore) {
            overlay.changeUndoStatus();
        }
    }

    private int penWidth = DEFAULT_PEN_WIDTH;

    private void setPenWidth(int width) {
        mPaint.setStrokeWidth(width);
        penWidth = width;
    }

    public void setPenOrEraser(int penIndex) {
        setPenWidth(DEFAULT_PEN_WIDTH);

        switch(penIndex) {
            case FloatingOverlay.PEN_INDEX_BLACK:
                mPaint.setColor(Color.DKGRAY);
                break;
            case FloatingOverlay.PEN_INDEX_BLUE:
                mPaint.setColor(Color.BLUE);
                break;
            case FloatingOverlay.PEN_INDEX_RED:
                mPaint.setColor(Color.RED);
                break;
            case FloatingOverlay.PEN_INDEX_GREEN:
                mPaint.setColor(Color.GREEN);
                break;
            case FloatingOverlay.PEN_INDEX_ERASER:
                mPaint.setColor(Color.WHITE);
                setPenWidth(ERASER_WIDTH);
                break;
        }
    }

    public void changeRecStatus(WhiteBoardCastActivity.RecordStatus recStats) {
        overlay.changeRecStatus();
        invalidate();
    }

    public boolean canRedo() {
        return undoList.canRedo();
    }

    FpsCounter paintFpsCounter = new FpsCounter(3);
    FpsCounter encoderFpsCounter = new FpsCounter(12);
    private boolean showFpsBar = false;

    public void enableDebug(boolean enabled) {
        showFpsBar = enabled;
    }

    public EncoderTask.FpsListener getEncoderFpsCounter() {
        return new EncoderTask.FpsListener() {
            @Override
            public void push(long currentFrameMill) {
                encoderFpsCounter.push(currentFrameMill);
            }
        };
    }

}
