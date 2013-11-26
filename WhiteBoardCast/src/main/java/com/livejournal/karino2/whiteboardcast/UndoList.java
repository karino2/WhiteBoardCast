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
        Rect undo(Bitmap target) {
            overwriteByBmp(target, decodeUndoBmp());
            return new Rect(x, y, x+width, y+height);
        }

        private void overwriteByBmp(Bitmap target, Bitmap bmp) {
            int[] buf = new int[bmp.getWidth()*bmp.getHeight()];
            bmp.getPixels(buf, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
            target.setPixels(buf, 0, bmp.getWidth(), x, y, bmp.getWidth(), bmp.getHeight());
        }

        Rect redo(Bitmap target) {
            overwriteByBmp(target, decodeRedoBmp());
            return new Rect(x, y, x+width, y+height);
        }

    }
    ArrayList<UndoCommand> commandList = new ArrayList<UndoCommand>();
    int currentPos = -1;

    Undoable undoTarget;
    public UndoList(Undoable target) {
        undoTarget = target;
    }

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

    public void undo() {
        if(!canUndo())
            return;
        Rect rect = commandList.get(currentPos).undo(undoTarget.getCommittedBitmap());
        currentPos--;
        undoTarget.invalCommitedBitmap(rect);
    }

    interface Undoable {
        Bitmap getCommittedBitmap();
        void invalCommitedBitmap(Rect undoInval);
    }

    public void redo() {
        if(!canRedo())
            return;
        currentPos++;
        Rect undoInval = commandList.get(currentPos).redo(undoTarget.getCommittedBitmap());
        undoTarget.invalCommitedBitmap(undoInval);
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
