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
    PanelPalette panelPalette;

	public PanelColor2( int toolUnit, WhiteBoardCastActivity act, PanelColor panelColor1, PanelPalette palette)
	{
		mAct = act;
		mToolUnit = toolUnit;
        panelColor = panelColor1;
        panelPalette = palette;

        Bitmap bmp = BitmapFactory.decodeResource(act.getResources(), R.drawable.eraser_button);
        mChecker = fitHeight(bmp, mToolUnit);

		// Add/Remove
		bmp = BitmapFactory.decodeResource( act.getResources(), R.drawable.op_add );
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
        PanelColor pc = panelColor;
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
        if (panelPalette.mActiveIndex == -1) paint.setAlpha( disableOpaque() );
		c.drawBitmap( mRemove, 0, dy, paint );
	}
	
	public void onDown( int ix, int iy )
	{
		if (iy < mToolUnit)
		{
			if (mType == 0)
			{
                // do nothing
                /*
                panelColor.swapColor();
                */
			}
			else
			{
				// 色モードに変更
                setPen();
			}
		}

		if ((iy >= mToolUnit) && (iy < mToolUnit*2))
		{
			// 消しゴムモード
            setEraser();
		}

		if ((iy >= mToolUnit*2) && (iy < mToolUnit*3))
		{
			// パレット追加
            panelPalette.addColor();
        }
		
		if ((iy >= mToolUnit*3) && (iy < mToolUnit*4))
		{
			// パレット削除
            panelPalette.removeColor();
        }
		
		updatePanel();
	}

    public void setEraser() {
        mType = 1;
        mAct.setEraser();
    }

    public void setPen() {
        mType = 0;
        mAct.setPen();
    }

    public void onUp( int ix, int iy )
	{
	}
	
	public void recycle()
	{
		mView.recycle();
	}

    public static void drawForeBG( int foreColor, int bgColor, Canvas c, int x, int y, int m )
    {
        Paint paint = new Paint();
        Paint paintS = new Paint();
        paintS.setStyle( Style.STROKE );

        int n = (int)(0.55 * m);
        int cx1 = x;
        int cy1 = y;
        int cx2 = cx1 + m/3;
        int cy2 = cy1 + m/3;

        // 背景色
        Rect rb = new Rect( cx2, cy2, cx2 + n, cy2 + n );
        paint.setStyle( Style.FILL );
        paint.setColor( bgColor );
        c.drawRect( rb, paint );

        paintS.setColor( 0xFF404040 );
        c.drawRect( rb, paintS );

        // 前景色
        Rect rf = new Rect( cx1, cy1, cx1 + n, cy1 + n );
        paint.setColor( foreColor );
        c.drawRect( rf, paint );

        paintS.setColor( 0xFF404040 );
        c.drawRect( rf, paintS );
    }
    int getOpBG() {
        return Color.DKGRAY;
    }

    int disableOpaque() {
        return 50; // below 255.
    }

}
