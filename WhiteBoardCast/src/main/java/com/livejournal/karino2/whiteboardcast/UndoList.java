package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by karino on 7/4/13.
 */
public class UndoList {

    class UndoCommand {
        byte[] undoBuf;
        byte[] redoBuf;
        int x, y, width, height;

        byte[] convertToPngBytes(Bitmap bmp) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            return os.toByteArray();
        }

        UndoCommand(int x, int y, Bitmap undo, Bitmap redo) {
            this.x = x;
            this.y = y;
            width = undo.getWidth();
            height = undo.getHeight();
            undoBuf = convertToPngBytes(undo);
            redoBuf = convertToPngBytes(redo);
        }

        Bitmap decodeUndoBmp() {
            return decodePng(undoBuf);
        }
        Bitmap decodeRedoBmp() {
            return decodePng(redoBuf);
        }

        private Bitmap decodePng(byte[] buf) {
            ByteArrayInputStream is = new ByteArrayInputStream(buf);
            return BitmapFactory.decodeStream(is);
        }

        int getByteSize() {
            return undoBuf.length+redoBuf.length;
        }
        Rect undo(Canvas target, Paint paint) {
            target.drawBitmap(decodeUndoBmp(), x, y, paint );
            return new Rect(x, y, x+width, y+height);
        }
        Rect redo(Canvas target, Paint paint) {
            target.drawBitmap(decodeRedoBmp(), x, y, paint );
            return new Rect(x, y, x+width, y+height);
        }

    }
    ArrayList<UndoCommand> commandList = new ArrayList<UndoCommand>();
    int currentPos = -1;

    public void pushUndoCommand(int x, int y, Bitmap undo, Bitmap redo) {
        discardLaterCommand();
        commandList.add(new UndoCommand(x, y, undo, redo));
        currentPos++;
        discardUntilSizeFit();
    }

    public boolean canUndo() {
        return currentPos >= 0;
    }
    public boolean canRedo() {
        return currentPos < commandList.size()-1;
    }

    public Rect undo(Canvas target, Paint paint) {
        if(!canUndo())
            return null;
        Rect rect = commandList.get(currentPos).undo(target, paint);
        currentPos--;
        return rect;
    }

    public Rect redo(Canvas target, Paint paint) {
        if(!canRedo())
            return null;
        currentPos++;
        return commandList.get(currentPos).redo(target, paint);
    }


    private int getCommandsSize() {
        int res = 0;
        for(UndoCommand cmd : commandList) {
            res += cmd.getByteSize();
        }
        return res;
    }

    final int COMMAND_MAX_SIZE = 1024*1024; // 1M

    private void discardUntilSizeFit() {
        // currentPos ==0, then do not remove even though it bigger than threshold (I guess never happen, though).
        while(currentPos > 0 && getCommandsSize() > COMMAND_MAX_SIZE) {
            commandList.remove(0);
            currentPos--;
        }
    }

    private void discardLaterCommand() {
        for(int i = commandList.size()-1; i > currentPos; i--) {
            commandList.remove(i);
        }
    }
}