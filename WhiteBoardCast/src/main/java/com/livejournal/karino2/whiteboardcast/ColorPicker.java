package com.livejournal.karino2.whiteboardcast;

import android.graphics.Canvas;

/**
 * Created by karino on 3/19/14.
 */
public class ColorPicker {
    PanelColor panelColor;
    PanelColor2 panelColor2;
    PanelPalette panelPalette;
    PanelSlider panelSlider;
    int left = 0;
    int top = 0;

    PanelColor.ColorListener listener;

    public ColorPicker(int toolUnit, WhiteBoardCastActivity act, PanelColor.ColorListener listener) {
        this.listener = listener;
        panelColor = new PanelColor(toolUnit, new PanelColor.ColorListener() {
            @Override
            public void setColor(int color) {
                notifyColorSetFromPanelColor(color);
            }
        });
        panelPalette = new PanelPalette(toolUnit, act, panelColor, new PanelColor.ColorListener() {
            @Override
            public void setColor(int color) {
                notifyColorSetFromPalette(color);
            }
        });
        panelColor2 = new PanelColor2(toolUnit, act, panelColor, panelPalette);
        panelSlider = new PanelSlider(toolUnit, act);
    }

    void notifyColorSetFromPanelColor(int color) {
        listener.setColor(color);
        panelColor2.setPen();
        panelColor2.updatePanel();
    }

    void notifyColorSetFromPalette(int color) {
        panelColor.setColor(color);
        panelColor2.setPen();
        panelColor2.updatePanel();
        listener.setColor(color);
    }


    public int width() {
        return panelColor.width()+panelColor2.width();
    }
    public int height() {
        return panelColor.height()+panelSlider.height()+panelPalette.height();
    }

    public void setPosition(int left, int top) {
        this.left = left;
        this.top = top;
    }

    public boolean isInside(int x, int y) {
        return isInsidePanelColor(x, y) ||
                isInsidePanelColor2(x, y) ||
                isInsidePanelSlider(x, y) ||
                isInsidePanelPalette(x, y);
    }

    public boolean isInsidePanelPalette(int x, int y) {
        return isInsideRegion(x, y, panelPaletteLeft(), panelPaletteTop(), panelPalette.width(), panelPalette.height());
    }

    public boolean isInsidePanelSlider(int x, int y) {
        return isInsideRegion(x, y, panelSliderLeft(), panelSliderTop(), panelSlider.width(), panelSlider.height());
    }

    public boolean isInsidePanelColor2(int x, int y) {
        return isInsideRegion(x, y, panelColor2Left(), panelColor2Top(), panelColor2.width(), panelColor2.height());
    }

    public boolean isInsidePanelColor(int x, int y) {
        return isInsideRegion(x, y, panelColorLeft(), panelColorTop(), panelColor.width(), panelColor.height());
    }


    int panelColorLeft() { return left; }
    int panelColorTop() { return top; }
    int panelColor2Left() { return left+panelColor.width(); }
    int panelColor2Top() { return top; }
    int panelPaletteLeft() { return left; }
    int panelPaletteTop() { return top + panelColor.height(); }
    int panelSliderLeft() { return left; }
    int panelSliderTop() { return top+panelColor.height()+panelPalette.height();}

    public void onDown(int absX, int absY) {
        if(isInsidePanelColor(absX, absY))
            panelColor.onDown(toColorRelativeLeft(absX), toColorRelativeTop(absY));
        if(isInsidePanelPalette(absX, absY))
            panelPalette.onDown(absX-panelPaletteLeft(), absY-panelPaletteTop());
        if(isInsidePanelSlider(absX, absY))
            panelSlider.onDown(toSliderRelativeLeft(absX), toSliderRelativeTop(absY));
        if(isInsidePanelColor2(absX, absY))
            panelColor2.onDown(absX-panelColor2Left(), absY-panelColor2Top());
    }

    public int toSliderRelativeTop(int absY) {
        return absY-panelSliderTop();
    }

    public int toSliderRelativeLeft(int absX) {
        return absX-panelSliderLeft();
    }

    private int toColorRelativeTop(int absY) {
        return absY-panelColorTop();
    }

    private int toColorRelativeLeft(int absX) {
        return absX-panelColorLeft();
    }

    public boolean isSnap() {
        return panelColor.isSnap() || panelSlider.isSnap();
    }

    public void onMove(int absX, int absY) {
        panelColor.onMove(toColorRelativeLeft(absX), toColorRelativeTop(absY));
        panelSlider.onMove(toSliderRelativeLeft(absX), toSliderRelativeTop(absY));
    }
    public void onUp(int absX, int absY) {
        panelColor.onUp(toColorRelativeLeft(absX), toColorRelativeTop(absY));
        panelColor2.onUp(absX-panelColor2Left(), absY-panelColor2Top());
        panelSlider.onUp();
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap( panelPalette.view(), panelPaletteLeft(), panelPaletteTop(), null );
        canvas.drawBitmap( panelSlider.view(), panelSliderLeft(), panelSliderTop(), null);
        canvas.drawBitmap( panelColor.view(), panelColorLeft(), panelColorTop(), null );
        canvas.drawBitmap( panelColor2.view(), panelColor2Left(), panelColor2Top(), null );
    }

    private boolean isInsideRegion(int x, int y, int left, int top, int width, int height) {
        return x >= left && x <= (left+width) && y >= top && y <= (top+height);
    }
}
