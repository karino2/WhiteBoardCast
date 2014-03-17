package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Style;
import android.graphics.Rect;
import android.graphics.Bitmap.Config;

public class PanelColor2
{
	private WhiteBoardCastActivity mAct = null;
	private Bitmap mView = null;
	private int mToolUnit = 10;
	
	private Bitmap mChecker = null;
	private int mType = 0; // 0:色   1:透明
	
	private Bitmap mAdd = null;
	private Bitmap mRemove = null;
    private PanelColor panelColor;

	public PanelColor2( int toolUnit, WhiteBoardCastActivity act, PanelColor panelColor1)
	{
		mAct = act;
		mToolUnit = toolUnit;
        panelColor = panelColor1;

		int m = (int)(0.75 * mToolUnit);
		mChecker = Bitmap.createBitmap( m, m, Config.ARGB_8888 );
		fillChecker( mChecker, 0xFFFFFFFF, 0xFFC0C0C0, 8 );

		// Add/Remove
		Bitmap bmp = BitmapFactory.decodeResource( act.getResources(), R.drawable.op_add );
		mAdd = fitHeight(bmp, mToolUnit);
		
		bmp = BitmapFactory.decodeResource( act.getResources(), R.drawable.op_remove );
		mRemove = fitHeight(bmp, mToolUnit);

		// View
		mView = Bitmap.createBitmap( mToolUnit, mToolUnit * 4, Config.ARGB_8888 );
		updatePanel();
	}

    public static Bitmap fitHeight( Bitmap src, int height )
    {
        float m = (float)height / src.getHeight(); // 拡大・縮小率
        return fitM( src, m );
    }

    public static Bitmap fitM( Bitmap src, float m )
    {
        int w = (int)(m * src.getWidth());
        int h = (int)(m * src.getHeight());
        Bitmap tmp = Bitmap.createBitmap( w, h, Config.ARGB_8888 );
        tmp.eraseColor( 0 );

        Canvas c = new Canvas( tmp );
        Paint p = new Paint();
        p.setFilterBitmap( true );

        Matrix mt = new Matrix();
        mt.postScale( m, m );
        c.drawBitmap( src, mt, p );

        return tmp;
    }



    // TODO: remove this.
    // 市松模様で塗る (size == 2の倍数)
    public static void fillChecker( Bitmap dest, int c0, int c1, int size )
    {
        int w = dest.getWidth();
        int h = dest.getHeight();

        int mask = size - 1;
        int hsize = size / 2;

        int[] pixels = new int[w];
        for (int j=0; j<h; j++)
        {
            int by = 0;
            int my = j & mask;
            if (my < hsize) by = 1;

            for (int i=0; i<w; i++)
            {
                int bx = 0;
                int mx = i & mask;
                if (mx < hsize) bx = 1;

                if (((bx + by) & 1) == 0) pixels[i] = c0; else pixels[i] = c1;
            }

            // ライン単位で書き込み
            dest.setPixels( pixels, 0, w, 0, j, w, 1 );
        }
    }


    public Bitmap view(){ return mView;	}
	public int width(){ return mView.getWidth(); }
	public int height(){ return mView.getHeight(); }
	
	public void updatePanel()
	{
		mView.eraseColor( 0xFFF0F0F0 );

		Paint paint = new Paint();
		Paint paintS = new Paint();
		paintS.setStyle( Style.STROKE );
		
		Canvas c = new Canvas( mView );
		
		// 前景・背景色
		PanelColor pc = panelColor();
		int foreColor = pc.currentColor();
		int bgColor = pc.bgColor();
		
		int dy = 0;
		Rect r = new Rect( 2, dy + 2, mToolUnit - 2, dy + mToolUnit - 2 );
		drawForeBG(foreColor, bgColor, c, r.left, r.top, mToolUnit);

		// ハイライト
		int colorHilight = 0xFFFF4E4E;
		if (mType == 0)
		{
			paintS.setStrokeWidth( 2 );
			paintS.setColor( colorHilight );

			r = new Rect( 1, dy+1, mToolUnit-1, dy + mToolUnit-1 );
			c.drawRect( r, paintS );
		}
		
		// 透明色
		int dx = mToolUnit/2 - mChecker.getWidth()/2;
		dy = mToolUnit + dx;
		r = new Rect( dx, dy, dx + mChecker.getWidth(), dy + mChecker.getHeight() );
		c.drawBitmap( mChecker, r.left, r.top, null );
		
		paintS.setStrokeWidth( 1 );
		paintS.setColor( 0xFF404040 );
		c.drawRect( r, paintS );

		// ハイライト
		if (mType == 1)
		{
			paintS.setStrokeWidth( 2 );
			paintS.setColor( colorHilight );
		
			dy = mToolUnit;
			r = new Rect( 1, dy+1, mToolUnit-1, dy + mToolUnit-1 );
			c.drawRect( r, paintS );
		}
		
		// Add, Remove
		paint.setColor( getOpBG() );
		dx = 0;
		dy = mToolUnit*2;
		c.drawRect( new Rect( dx+1, dy+1, dx+mToolUnit-1, dy+mToolUnit-1), paint );
		c.drawBitmap( mAdd, 0, dy, null );

		dy = mToolUnit*3;
		c.drawRect( new Rect( dx+1, dy+1, dx+mToolUnit-1, dy+mToolUnit-1), paint );
		if (panelPaletteActiveIndex() == -1) paint.setAlpha( disableOpaque() );
		c.drawBitmap( mRemove, 0, dy, paint );
	}
	
	public void onDown( int ix, int iy )
	{
		if (iy < mToolUnit)
		{
			if (mType == 0)
			{
				// 色入れ替え
				panelColor().swapColor();
			}
			else
			{
				// 色モードに変更
				mType = 0;
                setPenOrEraser(0);
			}
		}

		if ((iy >= mToolUnit) && (iy < mToolUnit*2))
		{
			// 消しゴムモード
			mType = 1;
            setPenOrEraser(1);
		}

		if ((iy >= mToolUnit*2) && (iy < mToolUnit*3))
		{
			// パレット追加
			panelPaletteAddColor();
		}
		
		if ((iy >= mToolUnit*3) && (iy < mToolUnit*4))
		{
			// パレット削除
			panelPaletteRemoveColor();
		}
		
		updatePanel();
	}

	public void onUp( int ix, int iy )
	{
	}
	
	public void recycle()
	{
		mView.recycle();
	}

    // DONE 

    // mAct.mView.UI().panelColor()
    PanelColor panelColor() {
        return panelColor;
    }

    // TODO: implement below.
    // 		UITablet.drawForeBG( foreColor, bgColor, c, r.left, r.top, mToolUnit );
    void drawForeBG(int foreColor, int bgColor, Canvas canvas, int left, int top, int unit) {
        showMessage("drawForeBG");
    }
    // UITablet.OpBG
    int getOpBG() {
        return Color.RED;
    }
    // panelPalette().mActiveIndex
    int panelPaletteActiveIndex() {
        return -1;
    }
    // UITablet.DisableOpaque
    int disableOpaque() {
        return 200; // below 255.
    }

    // 0 is pen, 1 is eraser.
    // MainActivity.nSetBrushDraw
    void setPenOrEraser(int mode) {
        showMessage("setPenOrEraser: " + mode);
    }

    // panelPalette().addColor(int color)
    void panelPaletteAddColor() {
        showMessage("addColor");
    }
    void panelPaletteRemoveColor() {
        showMessage("removeColor");
    }

    void showMessage(String msg) {
        mAct.showMessage(msg);
    }
}
