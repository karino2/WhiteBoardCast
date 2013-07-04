package com.livejournal.karino2.whiteboardcast;

import com.google.libvorbis.VorbisException;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.Toast;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class WhiteBoardCastActivity extends Activity {


    private static final String AUDIO_FNAME = "temp.mkv";

    private ScheduledExecutorService scheduleExecuter = null;
    private EncoderTask encoderTask = null;

    public void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG ).show();

    }


    Handler handler = new Handler();
    VorbisMediaRecorder recorder;
    Future<?> futuer = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_whiteboardcast);

    }


    public void stopRecord() {
        if(recStats != RecordStatus.RECORDING) {
            Log.d("WBCast", "stop record called but not recording. " + recStats);
            return;
        }
        // under processing.
        changeRecStatus(RecordStatus.DONE_PROCESS);
        showMessage("record end, start post process...");
        scheduleExecuter.shutdown();
        scheduleExecuter = Executors.newSingleThreadScheduledExecutor();
        recorder.stop();
        recorder.release();
        changeRecStatus(RecordStatus.DONE);

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

    public enum RecordStatus {
        DORMANT, SETUP, RECORDING, PAUSE, DONE_PROCESS, DONE
    }
    RecordStatus recStats = RecordStatus.DORMANT;

    public RecordStatus getRecStats() {
        return recStats;
    }

    void changeRecStatus(RecordStatus stats) {
        recStats = stats;
        getWhiteBoardCanvas().changeRecStatus(stats);
    }

    public void pauseRecord() {
        changeRecStatus(RecordStatus.PAUSE);
        futuer.cancel(false);
        futuer = null;
        recorder.stop();
        encoderTask.stop();
        showMessage("pause");
    }

    public void resumeRecord() {
        long suspendedBegin = recorder.lastBlockEndMil();
        long suspendedDur = System.currentTimeMillis() - suspendedBegin;
        recorder.resume(suspendedDur);
        encoderTask.resume(suspendedDur);
        scheduleEncodeTask();
        changeRecStatus(RecordStatus.RECORDING);
        showMessage("resume");
    }



    public void startRecord() {
        if(recStats != RecordStatus.DORMANT &&
                recStats != RecordStatus.DONE) {
            Log.d("WBCast", "record start but status is not dormant: " + recStats);
            return;
        }
        changeRecStatus(RecordStatus.SETUP);
        scheduleExecuter = Executors.newSingleThreadScheduledExecutor();
        handler.post(new Runnable() {
            @Override
            public void run() {
                // this method take some times. so get it back to UI first for update overlay.
                startRecordSecondPhase();
            }
        });
    }

    private void startRecordSecondPhase() {
        WhiteBoardCanvas wb = getWhiteBoardCanvas();
        wb.setWholeAreaInvalidate(); // for restart. make it a little heavy.
        encoderTask = new EncoderTask(wb, wb.getBitmap());
        long currentMill = System.currentTimeMillis();

        if(!encoderTask.initEncoder(currentMill)) {
            showMessage("init encode fail");
            return;
        }
        recorder = new VorbisMediaRecorder();
        recorder.setBeginMill(currentMill);
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

        scheduleEncodeTask();
        changeRecStatus(RecordStatus.RECORDING);
        showMessage("record start");
    }

    private void scheduleEncodeTask() {
        futuer = scheduleExecuter.scheduleAtFixedRate(encoderTask, 0, 1000 / FPS, TimeUnit.MILLISECONDS);
    }

    private WhiteBoardCanvas getWhiteBoardCanvas() {
        return (WhiteBoardCanvas)findViewById(R.id.fullscreen_content);
    }

    public String getResultPath() {
        return Environment.getExternalStorageDirectory() + "/result.webm";
    }

    private void openVideo() {
        new MediaScannerConnection.MediaScannerConnectionClient() {
            private MediaScannerConnection msc = null;
            {
                msc = new MediaScannerConnection(
                        getApplicationContext(), this);
                msc.connect();
            }

            public void onMediaScannerConnected() {
                msc.scanFile(getResultPath(), null);
            }

            public void onScanCompleted(String path, Uri uri) {
                msc.disconnect();
                handler.postDelayed(new Runnable(){
                    @Override
                    public void run() {
                        openVideo2();
                    }
                }, 100);

            }
        };

    }

    private void openVideo2() {
        Intent i = new Intent(Intent.ACTION_SEND, Uri.parse(getResultPath()));
        i.setType("video/*");
        // i.setType("video/x-matroska");
                /*
                i.setType("video/webm");
                i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(resultPath)));
                */
        // startActivity(i);
        startActivity(Intent.createChooser(i, "Open result video"));
    }

    private void beginAudioVideoMergeTask() {
        new AudioVideoMergeTask(this, new AudioVideoMergeTask.NotifyFinishListener() {
            @Override
            public void onFinish() {
                handler.postDelayed(new Runnable(){
                    @Override
                    public void run() {
                        openVideo2();
                    }
                }, 500);
            }
        }).execute(Environment.getExternalStorageDirectory() + "/temp.webm", Environment.getExternalStorageDirectory() + "/" + AUDIO_FNAME, getResultPath());
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN)
        {
            switch(event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    new AlertDialog.Builder(this)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                        .setMessage(R.string.query_back_message)
                        .setCancelable(true)
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener(){
                            public void onClick(DialogInterface dialog, int which) {
                                finish();
                            }})
                            .setNegativeButton(android.R.string.no, null)
                            .show();
                    return true;
            }
        }
        return super.dispatchKeyEvent(event);
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
            case R.id.menu_id_merge:
                beginAudioVideoMergeTask();
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
