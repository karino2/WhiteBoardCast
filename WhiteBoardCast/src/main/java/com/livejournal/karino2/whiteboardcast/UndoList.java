package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Handler;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Created by karino on 7/4/13.
 */
public class UndoList {
    interface UndoCommand {
        void undo(Undoable undoTarget);
        void redo(Undoable undoTarget);
        int getByteSize();
    }


    class BitmapUndoCommand implements UndoCommand {
        byte[] undoBuf;
        byte[] redoBuf;
        Bitmap undoBmp;
        Bitmap redoBmp;
        int x, y, width, height;
        Object undoBmpLock = new Object();

        byte[] convertToPngBytes(Bitmap bmp) {
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            bmp.compress(Bitmap.CompressFormat.PNG, 100, os);
            return os.toByteArray();
        }

        BitmapUndoCommand(int x, int y, Bitmap undo, Bitmap redo) {
            this.x = x;
            this.y = y;
            width = undo.getWidth();
            height = undo.getHeight();
            undoBmp = undo;
            redoBmp = redo;
        }

        void doCompression() {
            undoBuf = convertToPngBytes(undoBmp);
            redoBuf = convertToPngBytes(redoBmp);
            synchronized (undoBmpLock) {
                undoBmp = null;
                redoBmp = null;
            }
            discardUntilSizeFit();
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

        public int getByteSize() {
            synchronized (undoBmpLock) {
                if(undoBmp != null)
                    return 0; // now compressing. wait until compress finish.
            }
            return undoBuf.length+redoBuf.length;
        }
        public void undo(Undoable undoTarget) {
            overwriteByBmp(undoTarget.getCommittedBitmap(), decodeUndoBmp());
            undoTarget.invalCommitedBitmap(new Rect(x, y, x+width, y+height));
        }

        private void overwriteByBmp(Bitmap target, Bitmap bmp) {
            int[] buf = new int[bmp.getWidth()*bmp.getHeight()];
            bmp.getPixels(buf, 0, bmp.getWidth(), 0, 0, bmp.getWidth(), bmp.getHeight());
            target.setPixels(buf, 0, bmp.getWidth(), x, y, bmp.getWidth(), bmp.getHeight());
        }

        public void redo(Undoable redoTarget) {
            overwriteByBmp(redoTarget.getCommittedBitmap(), decodeRedoBmp());
            redoTarget.invalCommitedBitmap(new Rect(x, y, x+width, y+height));
        }

    }


    ArrayList<UndoCommand> commandList = new ArrayList<UndoCommand>();
    int currentPos = -1;


    boolean waitUndo = false;
    ExecutorService undoExecutor;
    Undoable undoTarget;
    public UndoList(Undoable target) {
        undoTarget = target;
        undoExecutor = Executors.newSingleThreadExecutor();
    }

    class CompressTask implements Runnable {
        BitmapUndoCommand target;
        CompressTask(BitmapUndoCommand command) {
            target = command;
        }

        @Override
        public void run() {
            target.doCompression();
            discardUntilSizeFit();
        }
    }

    public void pushBitmapUndoCommand(int x, int y, Bitmap undo, Bitmap redo) {
        BitmapUndoCommand command = new BitmapUndoCommand(x, y, undo, redo);
        pushUndoCommand(command);
        undoExecutor.submit(new CompressTask(command));
    }

    public void pushUndoCommand(UndoCommand command) {
        discardLaterCommand();
        commandList.add(command);
        currentPos++;
        discardUntilSizeFit();
    }

    synchronized int getCurrentPos() {
        return currentPos;
    }

    synchronized void incrementCurrentPos() {
        currentPos++;
    }

    synchronized void decrementCurrentPos() {
        currentPos--;
    }

    public boolean canUndo() {
        if(waitUndo)
            return false;
        return getCurrentPos() >= 0;
    }
    public boolean canRedo() {
        if(waitUndo)
            return false;
        return getCurrentPos() < commandList.size()-1;
    }

    Handler handler = new Handler();
    public void undo() {
        if(!canUndo())
            return;
        waitUndo = true;
        scheduleUndoCommand(new Runnable(){
            @Override
            public void run() {
                commandList.get(getCurrentPos()).undo(undoTarget);
                decrementCurrentPos();
                waitUndoDone();
            }
        });
    }

    private void waitUndoDone() {
        waitUndo = false;
        undoTarget.changeUndoStatus();
    }

    private void scheduleUndoCommand(final Runnable doUndo) {
        undoExecutor.submit(new Runnable() {
            @Override
            public void run() {
                handler.post(doUndo);
            }
        });
    }

    interface Undoable {
        Bitmap getCommittedBitmap();
        void invalCommitedBitmap(Rect undoInval);
        void changeUndoStatus();
    }

    public void redo() {
        if(!canRedo())
            return;
        waitUndo = true;
        scheduleUndoCommand(new Runnable(){
            @Override
            public void run() {
                incrementCurrentPos();
                commandList.get(getCurrentPos()).redo(undoTarget);
                waitUndoDone();
            }
        });
    }


    private int getCommandsSize() {
        int res = 0;
        for(UndoCommand cmd : commandList) {
            res += cmd.getByteSize();
        }
        return res;
    }

    final int COMMAND_MAX_SIZE = 1024*1024; // 1M

    private synchronized void discardUntilSizeFit() {
        // currentPos ==0, then do not remove even though it bigger than threshold (I guess never happen, though).
        while(currentPos > 0 && getCommandsSize() > COMMAND_MAX_SIZE) {
            commandList.remove(0);
            currentPos--;
        }
    }

    private synchronized void discardLaterCommand() {
        for(int i = commandList.size()-1; i > currentPos; i--) {
            commandList.remove(i);
        }
    }
}
