package com.livejournal.karino2.whiteboardcast;

import android.graphics.Canvas;

/**
 * Created by karino on 3/19/14.
 */
public class ColorPicker {
    PanelColor panelColor;
    PanelColor2 panelColor2;
    PanelPalette panelPalette;
    int left = 0;
    int top = 0;

    public ColorPicker(int toolUnit, WhiteBoardCastActivity act, PanelColor.ColorListener listener) {
        panelColor = new PanelColor(toolUnit, listener);
        panelPalette = new PanelPalette(toolUnit, act, panelColor);
        panelColor2 = new PanelColor2(toolUnit, act, panelColor, panelPalette);
    }

    public int width() {
        return panelColor.width()+panelColor2.width();
    }
    public int height() {
        return panelColor.height()+panelPalette.height();
    }

    public void setPosition(int left, int top) {
        this.left = left;
        this.top = top;
    }

    public boolean isInside(int x, int y) {
        return isInsideColorPanel(x, y) ||
                isInsideColorPanel2(x, y) ||
                isInsideColorPalette(x, y);
    }

    private boolean isInsideColorPalette(int x, int y) {
        return isInsideRegion(x, y, panelPaletteLeft(), panelPaletteTop(), panelPalette.width(), panelPalette.height());
    }

    private boolean isInsideColorPanel2(int x, int y) {
        return isInsideRegion(x, y, panelColor2Left(), panelColor2Top(), panelColor2.width(), panelColor2.height());
    }

    private boolean isInsideColorPanel(int x, int y) {
        return isInsideRegion(x, y, panelColorLeft(), panelColorTop(), panelColor.width(), panelColor.height());
    }

    int panelColorLeft() { return left; }
    int panelColorTop() { return top; }
    int panelColor2Left() { return left+panelColor.width(); }
    int panelColor2Top() { return top; }
    int panelPaletteLeft() { return left; }
    int panelPaletteTop() { return top + panelColor.height(); }

    public void onDown(int absX, int absY) {
        if(isInsideColorPanel(absX, absY))
            panelColor.onDown(toColorRelativeLeft(absX), toColorRelativeTop(absY));
        if(isInsideColorPalette(absX, absY))
            panelPalette.onDown(absX-panelPaletteLeft(), absY-panelPaletteTop());
        if(isInsideColorPanel2(absX, absY))
            panelColor2.onDown(absX-panelColor2Left(), absY-panelColor2Top());
    }

    private int toColorRelativeTop(int absY) {
        return absY-panelColorTop();
    }

    private int toColorRelativeLeft(int absX) {
        return absX-panelColorLeft();
    }

    public boolean isSnap() {
        return panelColor.isSnap();
    }

    public void onMove(int absX, int absY) {
        panelColor.onMove(toColorRelativeLeft(absX), toColorRelativeTop(absY));
    }
    public void onUp(int absX, int absY) {
        panelColor.onUp(toColorRelativeLeft(absX), toColorRelativeTop(absY));
        panelColor2.onUp(absX-panelColor2Left(), absY-panelColor2Top());
    }

    public void draw(Canvas canvas) {
        canvas.drawBitmap( panelPalette.view(), panelPaletteLeft(), panelPaletteTop(), null );
        canvas.drawBitmap( panelColor.view(), panelColorLeft(), panelColorTop(), null );
        canvas.drawBitmap( panelColor2.view(), panelColor2Left(), panelColor2Top(), null );
    }

    private boolean isInsideRegion(int x, int y, int left, int top, int width, int height) {
        return x >= left && x <= (left+width) && y >= top && y <= (top+height);
    }
}
