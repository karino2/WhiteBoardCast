package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RectF;

public class PanelColor
{
    public interface ColorListener {
        void setColor(int color);
    }


	private Bitmap mView = null;
	private int mToolUnit = 10;
	private boolean mTouching = false;
	private int mBGColor = 0xFFFFFFFF;

	// 色相位置 (前景)
	private double mWheelRad = 0;
	private int mWheelX = 0;
	private int mWheelY = 0;

	// 色相位置 (背景)
	private double mWheelRad2 = 0;
	private int mWheelX2 = 0;
	private int mWheelY2 = 0;
	
	// ホイール幅
	private int mWheelH = 10;

	private boolean mHueChanging = false;
	private boolean mSVChanging = false;

	private Bitmap mColorWheel = null; // 色相環 
	private Bitmap mColorGrad = null; // SV


    ColorListener listener;
	public PanelColor( int toolUnit , ColorListener colorListener)
	{
        listener = colorListener;
		mToolUnit = toolUnit;
		
		mView = Bitmap.createBitmap( mToolUnit * 4, mToolUnit * 4, Config.ARGB_8888 );
		mView.eraseColor( 0xFFF0F0F0 );
		
		mWheelH = (int)(0.65 * mToolUnit);
		int gw = (int)( (double)(height() - mWheelH*2) / Math.sqrt(2) );
		mColorWheel = Bitmap.createBitmap( height(), height(), Config.ARGB_8888 );
		mColorGrad = Bitmap.createBitmap( gw, gw, Config.ARGB_8888 );
		mWheelY = gw - 1;
		
		Paint paint = new Paint();
		paint.setAntiAlias( true );

		// 色相環
		updateHue( paint );
		updateSV();
		updatePanel();
	}
	
	public Bitmap view(){ return mView;	}
	public int width(){ return mView.getWidth(); }
	public int height(){ return mView.getHeight(); }

	public static int blend( int destcolor, int srcColor, int alpha )
	{
		int dr = (destcolor >> 16) & 255;
		int dg = (destcolor >> 8) & 255;
		int db = (destcolor >> 0) & 255;
		
		int sr = (srcColor >> 16) & 255;
		int sg = (srcColor >> 8) & 255;
		int sb = (srcColor >> 0) & 255;
		
		int alpha2 = 255 - alpha;
		int r = (sr * alpha2 + dr * alpha) / 255;
		int g = (sg * alpha2 + dg * alpha) / 255;
		int b = (sb * alpha2 + db * alpha) / 255;
		
		return 0xFF000000 + (r << 16) + (g << 8) + b;
	}

	// 現在の色
	public int currentColor()
	{
		int col = mColorGrad.getPixel( mWheelX, mWheelY );
		return col;
	}
	
	public int currentR()
	{
		return (currentColor() >> 16) & 0xFF;
	}

	public int currentG()
	{
		return (currentColor() >> 8) & 0xFF;
	}

	public int currentB()
	{
		return (currentColor() >> 0) & 0xFF;
	}
	
	// 背景色
	public int bgColor()
	{
		return mBGColor;
	}
	
	public int bgR()
	{
		return (mBGColor >> 16) & 0xFF;
	}

	public int bgG()
	{
		return (mBGColor >> 8) & 0xFF;
	}

	public int bgB()
	{
		return (mBGColor >> 0) & 0xFF;
	}

    // TODO: remove this.
	// 入れ替え
	public void swapColor()
	{
		// 色を入れ替え
		mBGColor = currentColor();

		// 座標入れ替え
		double tmpd = mWheelRad;
		mWheelRad = mWheelRad2; mWheelRad2 = tmpd;
	
		int tmp = mWheelX;
		mWheelX = mWheelX2; mWheelX2 = tmp;
		
		tmp = mWheelY;
		mWheelY = mWheelY2; mWheelY2 = tmp;
		
		// 画面更新
		updateSV();
		updatePanel();
        setCurrentColor();
        /*
		MainActivity.nSetColor( currentR(), currentG(), currentB() );
		MainActivity.nSetColorBG( bgR(), bgG(), bgB() );
		*/
	}

    public void setCurrentColor() {
        listener.setColor(Color.argb(0xff, currentR(), currentG(), currentB()) );
    }

    public static Point nearestPos( Bitmap bmp, int color )
    {
        Point p = new Point();

        int len = 999999999;
        int sr = (color & 0x00FF0000) >> 16;
        int sg = (color & 0x0000FF00) >> 8;
        int sb = (color & 0x000000FF);

        for (int j=0; j<bmp.getHeight(); j++)
        {
            for (int i=0; i<bmp.getWidth(); i++)
            {
                int c = bmp.getPixel( i, j );
                int r = (c & 0x00FF0000) >> 16;
                int g = (c & 0x0000FF00) >> 8;
                int b = (c & 0x000000FF);
                int dif = (r - sr)*(r - sr) + (g - sg)*(g - sg) + (b - sb)*(b - sb);
                if (dif < len)
                {
                    p.x = i;
                    p.y = j;
                    len = dif;
                }
            }
        }

        return p;
    }

	
	// 色をセット
	public void setColor( int color )
	{
		float[] hsv = { 0, 0, 0 };
		Color.colorToHSV( color, hsv );
		mWheelRad = hsv[0] * 2 * Math.PI / 360;
		updateSV();
		
		Point p = nearestPos(mColorGrad, color);
		mWheelX = p.x;
		mWheelY = p.y;
		
		// 画面更新
		updatePanel();
        setCurrentColor();
	}
	
	// 現在のHue位相
	private int currentHue()
	{
		double rad = mWheelRad;
		if (rad < 0) rad += Math.PI*2;
		
		float[] hsv = { (float)(360*rad / (Math.PI*2)), 1,1 };
		int c = Color.HSVToColor( hsv );
		return c;
	}

	private void updateHue( Paint paint )
	{
		mColorWheel.eraseColor( 0xFF000000 );
		
		Canvas canvas2 = new Canvas( mColorWheel );
		paint.setColor( 0xFFFFFFFF );
		RectF rect = new RectF( 0, 0, height(), height() );
		canvas2.drawOval( rect, paint );

		paint.setColor( 0xFF000000 );
		rect = new RectF( mWheelH , mWheelH, height() - mWheelH, height() - mWheelH );
		canvas2.drawOval( rect, paint );
		
		int wheelWidth = mColorWheel.getWidth();
		int[] pixels = new int[ wheelWidth ];
		int mx = mColorWheel.getWidth()/2;
		int my = mColorWheel.getHeight()/2;
		
		// Hue
		for (int j=0; j<mColorWheel.getHeight(); j++)
		{
			mColorWheel.getPixels( pixels, 0, wheelWidth, 0, j, wheelWidth, 1 );
			
			for (int i=0; i<mColorWheel.getWidth(); i++)
			{
				//int sc = mColorWheel.getPixel( i,  j );
				int sc = pixels[i];
				
				if (sc == 0xFF000000)
				{
					// 完全透明部
					sc = 0x00000000;
					//mColorWheel.setPixel( i,  j, sc );
					pixels[i] = sc;
					continue;
				}
				
				// HSV
				double rad = Math.atan2( j-my, i-mx );
				if (rad < 0) rad += Math.PI*2;
				float[] f = { (float)(360*rad / (Math.PI*2)), 1,1 };
				int c = Color.HSVToColor( f );
				sc = sc << 24; // B -> Alpha
				c = c & 0x00FFFFFF;
				sc = sc | c;
				
				//mColorWheel.setPixel( i,  j, sc );
				pixels[i] = sc;
			}
			
			mColorWheel.setPixels( pixels, 0, wheelWidth, 0, j, wheelWidth, 1 );
		}
	}
	
	private void updateSV()
	{
		mColorGrad.eraseColor( 0xFFFFFFFF );
		int hue = currentHue();

		int gradWidth = mColorGrad.getWidth();
		int[] pixels = new int[ gradWidth ];

		// 補完用
		for (int i=0; i<mColorGrad.getWidth(); i++)
		{
			// 最上列
			int a = 255 * i / mColorGrad.getWidth();
			int color = blend( hue, 0xFFFFFFFF, a );
			mColorGrad.setPixel( i, 0, color );
		}
		for (int i=0; i<mColorGrad.getHeight(); i++)
		{
			// 最左列
			int a = 255 * i / mColorGrad.getHeight();
			int color = blend( 0xFF000000, mColorGrad.getPixel( 0, 0 ), a );
			mColorGrad.setPixel( 0, i, color );
		}
		for (int i=0; i<mColorGrad.getHeight(); i++)
		{
			// 最右列
			int a = 255 * i / mColorGrad.getHeight();
			int x = mColorGrad.getWidth() - 1;
			int color = blend( 0xFF000000, mColorGrad.getPixel( x, 0 ), a );
			mColorGrad.setPixel( x, i, color );
		}
		
		// 補完
		for (int j=1; j<mColorGrad.getHeight(); j++)
		{
			int c2 = mColorGrad.getPixel( mColorGrad.getWidth()-1, j );
			int c1 = mColorGrad.getPixel( 0, j );
			
			mColorGrad.getPixels( pixels, 1, gradWidth, 1, j, gradWidth-2, 1 );
			for (int i=1; i<mColorGrad.getWidth()-1; i++)
			{
				int a = 255 * i / mColorGrad.getHeight();
				int color = blend( c2, c1, a );
				
				//mColorGrad.setPixel( i, j, color );
				pixels[i] = color;
			}
			mColorGrad.setPixels( pixels, 1, gradWidth, 1, j, gradWidth-2, 1 );
		}
	}
	
	public void updatePanel()
	{
		mView.eraseColor( 0 );

		Paint paint = new Paint();
		paint.setAntiAlias( true );
		Canvas canvas = new Canvas( mView );

		// ベース
		RectF rect = new RectF( 4, 4, width()-4, height()-4 );
		paint.setColor( 0xC0E6E6E6 );
		canvas.drawRect( rect, paint );
		
		// 色
		rect = new RectF( 8, 8, height()-8, height()-8 );
		paint.setColor( 0xFFE0E0E0 );
		canvas.drawRect( rect, paint );

		// 色相環合成
		canvas.drawBitmap( mColorWheel, 0,0, paint );
		
		// SV合成
		int svx = height()/2 - mColorGrad.getWidth()/2;
		int svy = height()/2 - mColorGrad.getHeight()/2;
		canvas.drawBitmap( mColorGrad, svx, svy, paint );
		
		// 色相位置合成
		int gw = mColorGrad.getWidth()/2;
		int gh = mColorGrad.getHeight()/2;
		float px = (height()/2 - mWheelH/2) * (float)Math.cos( (float)mWheelRad );
		float py = (height()/2 - mWheelH/2) * (float)Math.sin( (float)mWheelRad );
		canvas.drawCircle( svx + gw + px, svy + gh + py, mWheelH/4, paint );

		// SV位置合成
		canvas.drawCircle( svx + mWheelX, svy + mWheelY, mWheelH/4, paint );
	}
	
	// 色相ホイールの中？
	private boolean insideHue( float x, float y )
	{
		x -= width()/2; // Hue中心からの座標
		y -= height()/2;
		
		double dist = x*x + y*y;
		if (dist != 0) dist = Math.sqrt( dist );
		
		int max = height()/2 + mWheelH/3; // 外は甘めに
		int min = height()/2 - mWheelH;
		if ((min <= dist) && (dist <= max)) return true;
		return false;
	}

	// SVグラデの中？
	private boolean insideSV( float x, float y )
	{
		x -= (width()/2 - mColorGrad.getWidth()/2); // SV座標に
		y -= (height()/2 - mColorGrad.getHeight()/2);
		
		if (x < 0) return false;
		if (y < 0) return false;
		if (x >= mColorGrad.getWidth()) return false;
		if (y >= mColorGrad.getHeight()) return false;
		return true;
	}

	private double getWheelRad( float x, float y )
	{
		float px = x - width()/2;
		float py = y - height()/2;
		
		return Math.atan2( py, px );
	}

    public boolean isSnap() {
        return mTouching;
    }
	
	public void onDown( int ix, int iy )
	{
		mTouching = true;

		// 色範囲？
		if (insideHue( ix, iy ))
		{
			mHueChanging = true;					
			mWheelRad = getWheelRad( ix, iy );
		}
		
		if (insideSV( ix, iy ))
		{
			mSVChanging = true;
		}
		
		// 範囲内なら移動処理を (表示更新のため)
		onMove( ix, iy );
	}

	public void onMove( int ix, int iy )
	{
		if (!mTouching) return;

		// 色Hue
		if (mHueChanging)
		{
			mWheelRad = getWheelRad( ix, iy );
			updateSV();
			updatePanel();
		}
		
		// 色SV
		if (mSVChanging)
		{
			ix -= (width()/2 - mColorGrad.getWidth()/2); // SV座標に
			iy -= (height()/2 - mColorGrad.getHeight()/2);
			
			if (ix < 0) ix = 0;
			if (iy < 0) iy = 0;
			if (ix >= mColorGrad.getWidth()) ix = mColorGrad.getWidth() - 1;
			if (iy >= mColorGrad.getHeight()) iy = mColorGrad.getHeight() - 1;
			mWheelX = ix;
			mWheelY = iy;
			
			updateSV();
			updatePanel();
		}

        setCurrentColor();
	}

	public void onUp( int ix, int iy )
	{
		mTouching = false;
		
		mHueChanging = false;
		mSVChanging = false;
	}
	
	public void recycle()
	{
		mView.recycle();
		mColorWheel.recycle();
		mColorGrad.recycle();
	}
}
