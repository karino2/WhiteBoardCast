package com.livejournal.karino2.whiteboardcast;

import com.google.libvorbis.VorbisException;
import com.google.libwebm.mkvmuxer.MkvMuxer;
import com.livejournal.karino2.whiteboardcast.util.SystemUiHider;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ProgressDialog;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.Timer;

public class WhiteBoardCastActivity extends Activity {


    private static final String AUDIO_FNAME = "temp.mkv";

    private Timer timer = null;
    private EncoderTask encoderTask = null;

    public void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG ).show();

    }


    Handler handler = new Handler();
    VorbisMediaRecorder recorder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whiteboardcast);

    }

    public void stopRecord() {
        showMessage("record end, start post process...");
        timer.cancel();
        recorder.stop();
        recorder.release();

        if(!encoderTask.doneEncoder(new Encoder.FinalizeListener(){
            @Override
            public void done() {
                beginAudioVideoMergeTask();
                handler.postDelayed(new Runnable(){
                    @Override
                    public void run() {
                        // for debug.
                        if(encoderTask.getErrorBuf().length() != 0) {
                            showMessage("deb error: " + encoderTask.getErrorBuf().toString());
                        }

                    }
                }, 0);

            }
        })) {
            showMessage("done encoder fail");
        }
    }

    public void startRecord() {
        timer = new Timer();
        WhiteBoardCanvas wb = getWhiteBoardCanvas();
        encoderTask = new EncoderTask(wb, wb.getBitmap());
        if(!encoderTask.initEncoder()) {
            showMessage("init encode fail");
            return;
        }
        recorder = new VorbisMediaRecorder();

        recorder.setOutputFile(Environment.getExternalStorageDirectory() + "/" + AUDIO_FNAME);
        try {
            recorder.prepare();
        } catch (IOException e) {
            showMessage("IOException: MediaRecoder prepare fail: " + e.getMessage());
            return;
        } catch (VorbisException e) {
            showMessage("VorbisException: MediaRecoder prepare fail: " + e.getMessage());
            return;
        }
        recorder.start();

        timer.scheduleAtFixedRate(encoderTask, 0, 1000/FPS);
        showMessage("record start");
    }

    private WhiteBoardCanvas getWhiteBoardCanvas() {
        return (WhiteBoardCanvas)findViewById(R.id.fullscreen_content);
    }

    private void beginAudioVideoMergeTask() {
        new AudioVideoMergeTask(this).execute(Environment.getExternalStorageDirectory() + "/temp.webm", Environment.getExternalStorageDirectory() + "/" + AUDIO_FNAME, Environment.getExternalStorageDirectory() + "/result.webm");
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN &&
                event.getKeyCode() == KeyEvent.KEYCODE_MENU) {
            toggleMenu();
            return true;
        }
        return super.dispatchKeyEvent(event);
    }

    public void toggleMenu() {
        openOptionsMenu();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    public void clearCanvas() {
        getWhiteBoardCanvas().clearCanvas();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_id_about:
                showMessage("menu about!");
                return true;
            case R.id.menu_id_quit:
                finish();
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    private final int FPS = 12;

    private Button findButton(int id) {
        return (Button)findViewById(id);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    public void setPenOrEraser(int penIndex) {
        getWhiteBoardCanvas().setPenOrEraser(penIndex);
    }
}
