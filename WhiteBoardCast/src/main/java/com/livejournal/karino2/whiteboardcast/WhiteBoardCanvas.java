package com.livejournal.karino2.whiteboardcast;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Handler;
import android.util.AttributeSet;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListPopupWindow;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by karino on 6/26/13.
 */
public class WhiteBoardCanvas extends View implements FrameRetrieval, PageScrollAnimator.Animatee , UndoList.Undoable {

    // penCanvasBmp + BGBmp = viewBmp.
    Bitmap viewBmp;
    Bitmap penCanvasBmp;

    private Canvas penCanvas;
    Canvas viewCanvas;
    Bitmap cursorBackupBmp;
    int[] cursorBackupPixels;
    Canvas cursorBackupCanvas;


    Canvas committedCanvas;
    private Path mPath;
    private Paint mBitmapPaint;
    private Paint       mPaint;
    private Paint overwritePaint;
    private Paint mCursorPaint;
    private Paint mPointerPaint;
    private Paint fillPaint;
    private Rect invalRegion;

    BoardList boardList;

    FloatingOverlay overlay;

    public static final int DEFAULT_PEN_WIDTH = 6;
    static final int ERASER_WIDTH = 60;

    int penSize;
    int eraserSize;
    private int currentPenOrEraserSize = DEFAULT_PEN_WIDTH;

    private boolean isAnimating = false;

    private List<File> slides = new ArrayList<File>(); // null object.
    private boolean disableTouch;

    Paint timePaint;
    int toolUnit = 50;

    public WhiteBoardCanvas(Context context, AttributeSet attrs) {
        super(context, attrs);
        mPath = new Path();
        mBitmapPaint = new Paint(Paint.DITHER_FLAG);

        overlay = new FloatingOverlay((WhiteBoardCastActivity)context, 0);
        toolUnit = overlay.getToolUnit();

        penSize = DEFAULT_PEN_WIDTH;
        eraserSize = ERASER_WIDTH;

        mPaint = new Paint();
        mPaint.setColor(ColorPicker.getDefaultColor());
        mPaint.setAntiAlias(true);
        mPaint.setDither(true);
        mPaint.setStyle(Paint.Style.STROKE);
        mPaint.setStrokeJoin(Paint.Join.ROUND);
        mPaint.setStrokeCap(Paint.Cap.ROUND);
        mPaint.setStrokeWidth(penSize);
        invalRegion = new Rect(0, 0, 0, 0);

        timePaint = new Paint();
        // timePaint.setColor(0xAAeeeeFF);
        timePaint.setColor(0xAAAAAAFF);

        float pixel = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 50, getResources().getDisplayMetrics());
        timePaint.setTextSize(pixel);
        /*
        timePaint.setTextSize(50);
        */


        overwritePaint = new Paint();
        overwritePaint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.SRC));

        fillPaint = new Paint();
        fillPaint.setColor(Color.WHITE);
        fillPaint.setStyle(Paint.Style.FILL);

        mCursorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mCursorPaint.setStyle(Paint.Style.STROKE);
        mCursorPaint.setPathEffect(new DashPathEffect(new float[]{5, 2}, 0));

        mPointerPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPointerPaint.setStrokeWidth(4);
        mPointerPaint.setStyle(Paint.Style.STROKE);
        mPointerPaint.setColor(Color.argb(0xff, 0xff, 0x80, 0x80));



        boardList = new BoardList(this);

        setupCursorBackupStore(ERASER_WIDTH*2);
    }

    public void setupCursorBackupStore(int size) {
        cursorBackupPixels = new int[size*size];
        cursorBackupBmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        cursorBackupBmp.eraseColor(Color.TRANSPARENT);
        cursorBackupCanvas = new Canvas(cursorBackupBmp);
    }

    public BoardList getBoardList() {
        return boardList;
    }


    int mWidth;
    int mHeight;
    int mX1, mX2, mY1, mY2;
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        timeMeasured = false;

        // align for livbpx. May be we donot need this anymore.
        // Log.d("WBCast", "before, w,h=" + w+ "," + h);
        w = (w+15) & ~15;
        h = (h+15) & ~15;
        // Log.d("WBCast", "after, w,h=" + w+ "," + h);
        if( w <= mWidth)
            return; // when activity changing, some Android version call this when orientation changed (for other Activity!). just ignore.
        mWidth = w;
        mHeight = h;
        mX1 = CROSS_SIZE*2;
        mY1 = CROSS_SIZE*2;
        mX2 = mWidth-(CROSS_SIZE*2);
        mY2 = mHeight-(CROSS_SIZE*2);
        resetCanvas(w, h);
        overlay.onSize(w, h);
    }

    public long lastResetCanvas = -1;

    public void resetCanvas(int w, int h) {
        lastResetCanvas = System.currentTimeMillis();

        penCanvasBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        penCanvasBmp.eraseColor(Color.TRANSPARENT);
        viewBmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        viewBmp.eraseColor(Color.WHITE);
        viewCanvas = new Canvas(viewBmp);

        boardList.setSize(w, h);
        getCommittedBitmap().eraseColor(Color.TRANSPARENT);
        penCanvas = new Canvas(penCanvasBmp);
        committedCanvas = new Canvas(getCommittedBitmap());
    }


    long lastDrawn = 0;

    // every 1 seconds
    final long TIME_CHECK_INTERVAL = 500;
    final long TIME_DRAW_INTERVAL = 1000;

    boolean stopTimeInvalChecker = false;
    Runnable timeInvalChecker = new Runnable() {

        @Override
        public void run() {
            if(stopTimeInvalChecker)
                return;

            long dur = System.currentTimeMillis() - beginMill;
            if(lastDrawn == 0 || dur >= nextDrawnDuration) {
                invalidateTime();
            }
            handler.postDelayed(timeInvalChecker, Math.max(100, nextDrawnDuration-dur));
        }
    };


    RectF timeRegion = new RectF(0f, 0f, 0f, 0f);
    boolean timeMeasured = false;
    void ensureTimeRect() {
        if(!timeMeasured) {
            float w = viewCanvas.getWidth();
            float textWidth = timePaint.measureText("88:88:88");
            timeRegion.set(w-(textWidth+toolUnit*1.2f), 0, w, timePaint.getTextSize());
            timeMeasured = true;
        }
    }

    void invalidateTime() {
        ensureTimeRect();
        invalidate((int)timeRegion.left, (int)timeRegion.top, (int)timeRegion.right, (int)timeRegion.bottom);
    }

    public void startTimeDraw() {
        stopTimeInvalChecker = false;
        handler.postDelayed(timeInvalChecker, TIME_CHECK_INTERVAL);
    }

    public void stopTimeDraw() {
        stopTimeInvalChecker = true;
    }

    protected void onDraw(Canvas canvas) {
        canvas.drawColor(0xFFFFFFFF);

        lastDrawn = System.currentTimeMillis();

        if(isAnimating) {
            synchronized(viewBmp) {
                canvas.drawBitmap(viewBmp, 0, 0, mBitmapPaint);
            }
            return;
        }

        // drawToViewBmp();
        canvas.drawBitmap(viewBmp, 0, 0, mBitmapPaint);


        overlay.onDraw(canvas);

        drawFps(canvas);

        drawTime(canvas);

    }


    private void drawToViewBmp(Rect region) {
        drawToViewBmp(region, penCanvasBmp);

    }

    private void drawToViewBmp(Rect region, Bitmap fg) {
        synchronized(viewBmp) {
            viewCanvas.drawRect(region, fillPaint);
            viewCanvas.drawBitmap(getCurrentBackground(), region, region, mBitmapPaint);
            viewCanvas.drawBitmap(fg, region, region, mBitmapPaint);
        }
    }

    private void drawToViewBmp() {
        Rect whole = new Rect(0, 0, mWidth, mHeight);
        drawToViewBmp(whole);
    }

    Paint fpsPaint = new Paint();
    private void drawFps(Canvas canvas) {
        if(!showFpsBar)
            return;
        drawFpsBar(canvas, encoderFpsCounter.cycleFps(), 5, 0xFF0000FF);
        drawFpsBar(canvas, paintFpsCounter.cycleFps(), 5+12, 0xFFFF0000);
    }

    long beginMill = 0;
    long lastDrawnDuration = 0;
    long nextDrawnDuration = 0;

    public void notifyBeginMillChanged(long newBeginMill) {
        beginMill = newBeginMill;
    }

    void drawTime(Canvas canvas) {
        long hours = 0;
        long minutes = 0;
        long secs = 0;

        if(!stopTimeInvalChecker && beginMill != 0) {
            lastDrawnDuration = System.currentTimeMillis() - beginMill;
        }

        if(lastDrawnDuration != 0) {
            hours = lastDrawnDuration/(60*60*1000);
            minutes = (lastDrawnDuration%(60*60*1000))/(60*1000);
            secs = (lastDrawnDuration%(60*1000))/1000;

            // I assume draw time interval is now 1 sec.
            nextDrawnDuration = lastDrawnDuration- lastDrawnDuration%TIME_DRAW_INTERVAL+TIME_DRAW_INTERVAL;
        }


        String result = String.format("%02d:%02d:%02d", hours, minutes, secs);

        float w = canvas.getWidth();
        float textWidth = timePaint.measureText(result);
        canvas.drawText(result, w-(textWidth+toolUnit*1.2f), timePaint.getTextSize(), timePaint);
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
    private void drawPointer(Canvas canvas, float x, float y) {
        canvas.drawLine(x-CROSS_SIZE, y, x+CROSS_SIZE, y, mPointerPaint);
        canvas.drawLine(x, y-CROSS_SIZE, x, y+CROSS_SIZE, mPointerPaint);
    }

    private float mX, mY;
    private static final float TOUCH_TOLERANCE = 4;

    RectF mBrushCursorRegion = new RectF(0f, 0f, 0f, 0f);
    Rect lastBrushCursorRegion = new Rect(0, 0, 0, 0);

    boolean isRectValid(Rect region) {
        return region.width() != 0;
    }

    boolean isRectFValid(RectF region) {
        return region.width() >= 0.1;
    }

    static final int CURSOR_MARGIN = 2;

    void backupCursorRegion(RectF region) {
        region.roundOut(lastBrushCursorRegion);
        widen(lastBrushCursorRegion, CURSOR_MARGIN);
        fitInsideScreen(lastBrushCursorRegion);
        if(lastBrushCursorRegion.width() <=0 || lastBrushCursorRegion.height() <= 0) {
            makeRegionInvalid(lastBrushCursorRegion);
            return;
        }
        /*
        penCanvasBmp.getPixels(cursorBackupPixels, 0, penCursorWidth,
                lastBrushCursorRegion.left, lastBrushCursorRegion.top,
                lastBrushCursorRegion.width(), lastBrushCursorRegion.height());
                */
        Rect dest = new Rect(0, 0, lastBrushCursorRegion.width(), lastBrushCursorRegion.height());
        // TODO: too slow?
        /*
        If we draw red cross brush cursor, the garbage appear if we do not erase here.
        I guess this is because drawBitmap of transparent color does not overwrite red area
        (though I specify poter duff to overwrite).
        Anyway, I try erase color every time.
         */
        cursorBackupBmp.eraseColor(Color.TRANSPARENT);
        cursorBackupCanvas.drawBitmap(penCanvasBmp, lastBrushCursorRegion, dest, overwritePaint);
    }


    private void revertBrushDrawnRegionIfNecessary() {
        if(!isRectValid(lastBrushCursorRegion))
            return;
        Rect srcRegion = new Rect(0, 0, lastBrushCursorRegion.width(), lastBrushCursorRegion.height());
        penCanvas.drawBitmap(cursorBackupBmp, srcRegion, lastBrushCursorRegion, overwritePaint);
        /*
        penCanvasBmp.setPixels(cursorBackupPixels, 0, penCursorWidth,
                lastBrushCursorRegion.left, lastBrushCursorRegion.top,
                lastBrushCursorRegion.width(), lastBrushCursorRegion.height());
                */

        drawToViewBmp(lastBrushCursorRegion);

        invalViewBmpRegion(lastBrushCursorRegion);

        makeRegionInvalid(lastBrushCursorRegion);
    }

    private void invalViewBmpRegionF(RectF regionF) {
        Rect region = new Rect();
        regionF.roundOut(region);
        invalViewBmpRegion(region);
    }

    void fitInsideScreen(Rect region) {
        if(region.intersect(0, 0, mWidth, mHeight)) {
            region.left = Math.max(0, region.left);
            region.top = Math.max(0, region.top);
            region.right = Math.min(mWidth, region.right);
            region.bottom = Math.min(mHeight, region.bottom);
        }
    }

    private void invalViewBmpRegion(Rect region) {
        invalRegionForEncoder(region);
        invalidate(region.left, region.top, region.right, region.bottom);
    }

    private void invalRegionForEncoder(Rect region) {
        invalRegion.union(region);
        fitInsideScreen(invalRegion);
    }


    void drawBrushCursorIfNecessary() {
        revertBrushDrawnRegionIfNecessary();

        if(isRectFValid(mBrushCursorRegion)) {
            backupCursorRegion(mBrushCursorRegion);
            if(isPointerMode()) {
                drawPointer(penCanvas, mBrushCursorRegion.centerX(), mBrushCursorRegion.centerY());
            } else {
                penCanvas.drawOval(mBrushCursorRegion, mCursorPaint);
            }

            Rect region = new Rect();
            mBrushCursorRegion.roundOut(region);
            drawToViewBmp(region);

            invalViewBmpRegionF(mBrushCursorRegion);
        }
    }

    private float getCursorSize() {
        if(isPointerMode()) {
            return (float)(CROSS_SIZE*2+8);
        } else {
            return (float) currentPenOrEraserSize;
        }
    }

    private void setBrushCursorPos(float x, float y)
    {
        mBrushCursorRegion.set(x - getCursorSize() / 2, y - getCursorSize() / 2,
                x + getCursorSize() / 2, y + getCursorSize() / 2);
    }

    void makeRegionInvalid(Rect region)
    {
        region.set(0, 0, 0, 0);
    }
    void makeRegionInvalidF(RectF region) {
        region.set(0f, 0f, 0f, 0f);
    }
    
    private void eraseBrushCursor() {
        makeRegionInvalidF(mBrushCursorRegion);
        revertBrushDrawnRegionIfNecessary();
    }


    boolean mDownHandled = false;
    private RectF invalF = new RectF();
    private Rect tmpInval = new Rect();

    private boolean overTolerance(float x, float y) {
        float dx = Math.abs(x - mX);
        float dy = Math.abs(y - mY);
        return (dx >= TOUCH_TOLERANCE || dy >= TOUCH_TOLERANCE);
    }

    @Override
    public boolean onHoverEvent(MotionEvent event) {
        if(isAnimating)
            return super.onHoverEvent(event);
        if(event.getAction() == MotionEvent.ACTION_HOVER_EXIT) {
            eraseBrushCursor();
        }else {
            float x = event.getX();
            float y = event.getY();
            drawBrush(x, y);
        }
        return super.onHoverEvent(event);
    }

    private void drawBrush(float x, float y) {
        setBrushCursorPos(x, y);
        drawBrushCursorIfNecessary();
    }

    static final int TOOL_TYPE_FINGER = 1;
    // static final int TOOL_TYPE_STYLUS = 2;
    public boolean isFinger(android.view.MotionEvent event) {
        return TOOL_TYPE_FINGER == event.getToolType(0);
    }


    public boolean onTouchEvent(MotionEvent event) {
        if(isAnimating)
            return true;

        float x = event.getX();
        float y = event.getY();
        revertBrushDrawnRegionIfNecessary();
        setBrushCursorPos(x, y);
        onTouchWithoutCursor(event, x, y);
        drawBrushCursorIfNecessary();
        return true;

    }

    private void onTouchWithoutCursor(MotionEvent event, float x, float y) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if(disableTouch && isFinger(event))
                    return;

                if(overlay.onTouchDown(x, y)) {
                    /*
                        Overlay operation might cause pen size change.
                        Then we need to update mBrushCursorRegion.
                        But most of the time, during overlay operation, we do not need brush cursor.
                        So just erase here.
                     */
                    eraseBrushCursor();
                    invalidate();
                    break;
                }
                if(isPointerMode()) {
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
                if(disableTouch && isFinger(event))
                    return;

                paintFpsCounter.push(System.currentTimeMillis());
                if(overlay.onTouchMove(x, y)) {
                    // See action down comment.
                    eraseBrushCursor();
                    invalidate();
                    break;
                }
                if(isPointerMode()) {
                    break;
                }
                if(!mDownHandled)
                    break;
                if (overTolerance(x, y)) {
                    int historySize = event.getHistorySize();
                    if(historySize == 0) {
                        mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                        mX = x;
                        mY = y;
                    }else {
                        for(int i = 0; i < event.getHistorySize(); i++) {
                            float hx = event.getHistoricalX(i);
                            float hy = event.getHistoricalY(i);
                            if(overTolerance(hx, hy)) {
                                mPath.quadTo(mX, mY, (hx + mX)/2, (hy + mY)/2);
                                mX = hx;
                                mY = hy;
                            }
                        }
                    }
                    penCanvas.drawPath(mPath, mPaint);
                    drawToViewBmp(pathBound());
                    updateInvalRegionForEncoder();
                }
                // no tolerance
                /*
                mPath.quadTo(mX, mY, (x + mX)/2, (y + mY)/2);
                mX = x;
                mY = y;
                updateInvalRegionForEncoder();
                penCanvas.drawPath(mPath, mPaint);
                */
                // no tolerance done.

                invalidate(pathBound());
                break;
            case MotionEvent.ACTION_UP:
                eraseBrushCursor();
                if(disableTouch && isFinger(event))
                    return;
                if(overlay.onTouchUp(x, y)) {
                    // See action down comment.
                    eraseBrushCursor();
                    invalidate();
                    break;
                }
                if(isPointerMode()) {
                    invalidate();
                    break;
                }
                if(!mDownHandled)
                    break;
                mDownHandled = false;
                mPath.lineTo(mX, mY);

                boolean canUndoBefore = getUndoList().canUndo();

                Rect region = pathBound();
                Bitmap undo = Bitmap.createBitmap(getCommittedBitmap(), region.left, region.top, region.width(), region.height() );
                committedCanvas.drawPath(mPath, mPaint);
                Bitmap redo = Bitmap.createBitmap(getCommittedBitmap(), region.left, region.top, region.width(), region.height());

                pushUndoCommand(region, undo, redo);

                invalRegionForEncoder(region);
                penCanvas.drawPath(mPath, mPaint);
                mPath.reset();
                if(getUndoList().canUndo() != canUndoBefore) {
                    overlay.changeUndoStatus();
                }
                invalidate();
                break;
        }
    }

    private void pushUndoCommand(Rect region, Bitmap undo, Bitmap redo) {
        getUndoList().pushBitmapUndoCommand(region.left, region.top, undo, redo);
    }


    private Rect pathBound() {
        mPath.computeBounds(invalF, false);
        invalF.roundOut(tmpInval);
        widenPenWidth(tmpInval);
        fitInsideScreen(tmpInval);
        return tmpInval;
    }


    private void updateInvalRegionForEncoder() {
        invalRegionForEncoder(pathBound());
    }

    private void widenPenWidth(Rect tmpInval) {
        int penWidth = (int)getCursorSize();
        widen(tmpInval, penWidth);
    }

    private void widen(Rect tmpInval, int width) {
        int newLeft = Math.max(0, tmpInval.left- width);
        int newTop = Math.max(0, tmpInval.top - width);
        int newRight = Math.min(mWidth, tmpInval.right+ width);
        int newBottom = Math.min(mHeight, tmpInval.bottom+ width);
        tmpInval.set(newLeft, newTop, newRight, newBottom);
    }

    public Bitmap getBitmap() { return viewBmp;}

    boolean mIsPointerMode = false;
    public boolean isPointerMode() {
        return mIsPointerMode;
    }

    public void togglePointerMode() {
        mIsPointerMode = !mIsPointerMode;
        ensureCursorBackupSize((int)getCursorSize());
        invalidate();
    }

    public boolean canUndo() {
        return getUndoList().canUndo();
    }

    public void undo() {
        getUndoList().undo();
    }

    public void redo() {
        getUndoList().redo();
    }

    private void overwriteByBmp(Bitmap target, Bitmap bmp, Rect region) {
        int[] buf = new int[region.width()*region.height()];
        bmp.getPixels(buf, 0, region.width(), region.left, region.top, region.width(), region.height());
        target.setPixels(buf, 0, region.width(), region.left, region.top, region.width(), region.height());
    }

    @Override
    public void invalCommitedBitmap(Rect undoInval) {
        drawToViewBmp(undoInval, getCommittedBitmap());
        overwriteByBmp(penCanvasBmp, getCommittedBitmap(), undoInval);
        invalRegionForEncoder(undoInval);
        invalidate(undoInval);
    }

    @Override
    public void changeUndoStatus() {
        overlay.changeUndoStatus();
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
        boolean canUndoBefore = getUndoList().canUndo();
        Bitmap undo = Bitmap.createBitmap(getCommittedBitmap(), 0, 0, viewBmp.getWidth(), viewBmp.getHeight() );

        getCommittedBitmap().eraseColor(Color.TRANSPARENT);
        penCanvasBmp.eraseColor(Color.TRANSPARENT);
        afterChangeBGImage();

        Bitmap redo = Bitmap.createBitmap(getCommittedBitmap(), 0, 0, viewBmp.getWidth(), viewBmp.getHeight() );
        getUndoList().pushBitmapUndoCommand(0, 0, undo, redo);
        if(getUndoList().canUndo() != canUndoBefore) {
            overlay.changeUndoStatus();
        }
    }

    public void invalWholeRegionForEncoder() {
        invalRegion.set(0, 0, viewBmp.getWidth(), viewBmp.getHeight());
    }


    public void setPenColor(int color) {
        mPaint.setColor(color);
    }

    public void setPen() {
        mPaint.setXfermode(null);
        setCurrentPenOrEraserSizeInternal(penSize);
        overlay.setSliderPos(penSize);
    }

    public void setEraser() {
        mPaint.setXfermode(new PorterDuffXfermode(
                PorterDuff.Mode.CLEAR));
        setCurrentPenOrEraserSizeInternal(eraserSize);
        overlay.setSliderPos(eraserSize);
    }

    void setCurrentPenOrEraserSizeInternal(int width) {
        currentPenOrEraserSize = width;
        mPaint.setStrokeWidth(currentPenOrEraserSize);
    }

    public void changeRecStatus() {
        overlay.changeRecStatus();
        invalidate();
    }


    public boolean canRedo() {
        return getUndoList().canRedo();
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

    private Board getCurrentBoard() {
        return boardList.getCurrent();
    }

    @Override
    public Bitmap getCommittedBitmap() {
        return getCurrentBoard().getBoardBmp();
    }


    private Bitmap getCurrentBackground() {
        return getCurrentBoard().getBackgroundBmp();
    }

    private UndoList getUndoList() {
        return getCurrentBoard().getUndoList();
    }

    private void afterChangeBoard() {
        synchronized (viewBmp) {
            invalWholeRegionForEncoder();
            Rect r = new Rect(0, 0, mWidth, mHeight);
            viewCanvas.drawRect(r, fillPaint);
            viewCanvas.drawBitmap(getCurrentBackground(), 0, 0, mBitmapPaint);
            viewCanvas.drawBitmap(getCommittedBitmap(), 0, 0, mBitmapPaint);
        }
        // TODO: slow.
        penCanvasBmp = getCommittedBitmap().copy(Bitmap.Config.ARGB_8888, true);
        penCanvas = new Canvas(penCanvasBmp);
        committedCanvas = new Canvas(getCommittedBitmap());

        overlay.changeUndoStatus();

        invalidate();
    }

    boolean pageUp() {
        isAnimating = false;
        if(boardList.pagePrev()) {
            afterChangeBoard();
            return true;
        }
        return false;
    }

    void pageDown() {
        isAnimating = false;
        boardList.pageNext();
       afterChangeBoard();
    }

    void updateScreenUIThread() {
        invalWholeRegionForEncoder();
        invalidate();
    }

    @Override
    public void start() {
        isAnimating = true;
    }

    Handler handler = new Handler();
    Runnable updateScreenRunnable = new Runnable() {
        @Override
        public void run() {
            updateScreenUIThread();
        }
    };
    @Override
    public void updateScreen() {
        handler.post(updateScreenRunnable);
    }

    @Override
    public void done(PageScrollAnimator.Direction dir) {
        if(dir == PageScrollAnimator.Direction.Next) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    pageDown();
                }
            });
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    pageUp();
                }
            });
        }
    }

    public boolean beginPagePrev(PageScrollAnimator animator) {
        if(!boardList.hasPrevPage())
            return false;
        animator.start(boardList.createPrevSynthesizedBmp(), getCurrentBoard().createSynthesizedTempBmp(), viewBmp, PageScrollAnimator.Direction.Prev);
        return true;
    }

    public void beginPageNext(PageScrollAnimator animator) {
        animator.start(getCurrentBoard().createSynthesizedTempBmp(), boardList.createNextSynthesizedBmp(),  viewBmp, PageScrollAnimator.Direction.Next);
    }

    public void newPresentation() {
        boardList = new BoardList(this);
        if(popup != null) {
            popup.dismiss();
        }
        popup = null;
        popupShown = false;
        resetCanvas(mWidth, mHeight);
        afterChangeBoard();
    }

    public int getStoredWidth() {
        return mWidth;
    }

    public int getStoredHeight() {
        return mHeight;
    }

    void setPenSize(int size) {
        ensureCursorBackupSize(size);
        penSize = size;
        setCurrentPenOrEraserSizeInternal(penSize);
    }

    void setEraserSize(int size) {
        ensureCursorBackupSize(size);
        eraserSize = size;
        setCurrentPenOrEraserSizeInternal(eraserSize);
    }

    private boolean isEraser() {
        return overlay.isEraser();
    }

    public void setPenOrEraserSize(int size) {
        if(isEraser()) {
            setEraserSize(size);
        } else {
            setPenSize(size);
        }
    }

    public void ensureCursorBackupSize(int size) {
        /*
        Now we call erase color every time brush cursor drawn.
        So we shrink cursorBackupBmp here if it's too large compare to current requirement.
         */
        if(cursorBackupBmp.getWidth() < size || cursorBackupBmp.getWidth() > 2*(size*2)) {
            setupCursorBackupStore(size*2);
        }
    }

    public boolean handleBackKey() {
        boolean handled = overlay.handleBack();
        if(handled)
            invalidate();
        return handled;
    }

    public void setDisableTouch(boolean disableTouch) {
        this.disableTouch = disableTouch;
    }

    class InsertBGUndoRedoCommand implements UndoList.UndoCommand {
        Board.BackgroundImage prev;
        Board.BackgroundImage cur;
        InsertBGUndoRedoCommand(Board.BackgroundImage cur, Board.BackgroundImage prev) {
            this.prev = prev;
            this.cur = cur;
        }

        @Override
        public void undo(UndoList.Undoable undoTarget) {
            getCurrentBoard().setBackground(prev);
            afterChangeBGImage();
        }

        @Override
        public void redo(UndoList.Undoable undoTarget) {
            getCurrentBoard().setBackground(cur);
            afterChangeBGImage();
        }

        @Override
        public int getByteSize() {
            return 0;
        }
    }


    public void insertNewBGFile(File file) {
        Board.BackgroundImage newBackground = new Board.BackgroundImage(file);
        Board.BackgroundImage prev = getCurrentBoard().setBackground(newBackground);
        prev.discardBitmap();
        newBackground.discardBitmap();
        getUndoList().pushUndoCommand(new InsertBGUndoRedoCommand(newBackground, prev));
        overlay.changeUndoStatus();
        afterChangeBGImage();
    }

    private void afterChangeBGImage() {
        drawToViewBmp();
        invalWholeRegionForEncoder();
        invalidate();
    }

    public void setSlides(List<File> slides) {
        this.slides = slides;
        overlay.changeSlidesStatus();
    }


    FileImageAdapter slideAdapter;
    ListPopupWindow popup;
    ListPopupWindow getSlideWindow() {
        if(popup == null) {
            popup = new ListPopupWindow(getContext());
            popup.setAnchorView(this);
            popup.setHorizontalOffset(mWidth - mWidth / 6);
            popup.setVerticalOffset(-mHeight);
            slideAdapter = new FileImageAdapter(LayoutInflater.from(getContext()), slides, mWidth / 6, mHeight / 6);
            popup.setAdapter(slideAdapter);
            popup.setWidth(mWidth/6);
            popup.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    File slide = slideAdapter.reverseLookUp(id);
                    insertNewBGFile(slide);
                    hideSlideWindow();
                }
            });
        }
        return popup;
    }


    boolean popupShown = false;

    public void toggleShowSlides() throws IOException {
        if(popupShown) {
            hideSlideWindow();
        }
        else {
            showSlideWindow();
        }
    }

    private void showSlideWindow() {
        popupShown = true;
        getSlideWindow().show();
    }

    private void hideSlideWindow() {
        popupShown = false;
        getSlideWindow().dismiss();
    }



}
