package com.livejournal.karino2.whiteboardcast;

import java.util.ArrayList;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Bitmap.Config;
import android.graphics.Color;
import android.graphics.Paint.Style;
import android.graphics.Paint;
import android.graphics.Rect;
import android.preference.PreferenceManager;

public class PanelPalette
{
	private WhiteBoardCastActivity mAct = null;
	private Bitmap mView = null;
	private int mToolUnit = 10;
	private int mToolUnitH = 5;
	public static final int W = 4;
	public static final int H = 2;
	
	private ArrayList<Integer> mColors = null;
	public int mTouchColor = 0; // タッチした色
	public int mActiveIndex = -1; // アクティブなパレット

    PanelColor panelColor;
    PanelColor.ColorListener colorListener;
	
	public PanelPalette( int toolUnit, WhiteBoardCastActivity act, PanelColor panelCol, PanelColor.ColorListener listener)
	{
        panelColor = panelCol;
		mAct = act;
		mToolUnit = toolUnit;
		mToolUnitH = toolUnit;
        colorListener = listener;

		mColors = new ArrayList<Integer>();

		openPalette();
		
		onResize();
	}


	public Bitmap view(){ return mView;	}
	public int width(){ return mView.getWidth(); }
	public int height(){ return mView.getHeight(); }
	
	public int widthNum(){ return W * 2; }
	
	public void openPalette()
	{
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences( mAct );
		
		int num = settings.getInt( "PalNum", -1 );
		if (num == -1) {
            num = initialColorSetup(settings);
        }
		
		mColors.clear();
		for (int i=0; i<num; i++)
		{
			String s = "Pal" + String.valueOf( i );
			int color = settings.getInt( s, 0 );
			mColors.add( color );
		}
	}

    private int initialColorSetup(SharedPreferences settings) {
        int num = 5;
        settings.edit()
                .putInt("PalNum", num)
                .putInt("Pal0", Color.DKGRAY)
                .putInt("Pal1", 0xffeeeeaa)
                .putInt("Pal2", Color.BLUE)
                .putInt("Pal3", Color.RED)
                .putInt("Pal4", Color.GREEN)
                .commit();
        return num;
    }

    public void savePalette()
	{
		SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences( mAct );
		SharedPreferences.Editor editor = settings.edit();
		
		editor.putInt( "PalNum", mColors.size() );
		for (int i=0; i<mColors.size(); i++)
		{
			String s = "Pal" + String.valueOf( i );
			editor.putInt( s, mColors.get( i ) );
		}

		editor.commit();
	}
	
	public void onResize()
	{
		// int n = W * 4; // 一行のパレット数
        int n = W ;
		int h = (mColors.size() / n) + 1;
		
		recycle();
		mView = Bitmap.createBitmap( mToolUnit * W, mToolUnit * h, Config.ARGB_8888 );
		updatePanel();
	}
	
	public void addColor()
	{
		int color = panelColor.currentColor();
		mColors.add( color );
		mActiveIndex = mColors.size() - 1;
		onResize(); // パネルサイズ更新の可能性
		savePalette();
	}

	public void removeColor()
	{
		if (mActiveIndex < 0) return;
		if (mActiveIndex >= mColors.size()) return;
		
		mColors.remove( mActiveIndex );
		
		if (mActiveIndex >= mColors.size()) mActiveIndex = - 1;
		if (mColors.size() == 0) mActiveIndex = -1;
		onResize();
		savePalette();
	}

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

    public void updatePanel()
	{
		fillChecker(mView, 0xFFFFFFFF, 0xFFD0D0D0, 8);
		
		Canvas canvas = new Canvas( mView );
		Paint paint = new Paint();

		int m = mToolUnitH;
		for (int i=0; i<mColors.size(); i++)
		{
			int nx = i % W;
			int ny = i / W;
			int x = nx * m;
			int y = ny * m;
			
			Rect rect = new Rect( x+2, y+2, x+m-2, y+m-2 );
			paint.setColor( mColors.get( i ) );
			canvas.drawRect( rect, paint );
			
			// ハイライト
			if (mActiveIndex == i)
			{
				Paint ps = new Paint();
				ps.setStyle( Style.STROKE );

				ps.setColor( 0xFF000000 );
				canvas.drawRect( rect, ps );

				ps.setColor( 0xFFFFFFFF );
				rect = new Rect( x+3, y+3, x+m-3, y+m-3 );
				canvas.drawRect( rect, ps );
			}
		}
	}
	
	public void onDown( int ix, int iy )
	{
		int m = mToolUnitH;
		int nx = ix / m;
		int ny = iy / m;
		int index = (ny * W ) + nx;
		
		if (index < 0) return;
		if (index >= mColors.size()) return;
		
		mTouchColor = mColors.get( index );
		mActiveIndex = index;

        colorListener.setColor(mTouchColor);
		updatePanel();
	}

	public void recycle()
	{
		if (mView != null) mView.recycle();
		mView = null;
	}
}
