package com.livejournal.karino2.whiteboardcast;

import com.google.libvorbis.VorbisException;
import com.livejournal.karino2.multigallery.MultiGalleryActivity;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.WebView;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class WhiteBoardCastActivity extends Activity implements EncoderTask.ErrorListener, PanelColor.ColorListener {

    static final int DIALOG_ID_ABOUT = 1;
    static final int DIALOG_ID_QUERY_MERGE_AGAIN = 3;
    static final int DIALOG_ID_NEW = 5;
    static final int DIALOG_ID_COPYING = 6;

    static final int REQUEST_PICK_IMAGE = 10;

    private static final String AUDIO_FNAME = "temp.mkv";

    private Presentation presen = new Presentation();

    public void postErrorMessage(final String msg) {
        handler.postDelayed(new Runnable(){

            @Override
            public void run() {
                showError(msg);
            }
        }, 0);
    }

    public void postShowMessage(final String msg) {
        handler.postDelayed(new Runnable(){

            @Override
            public void run() {
                showMessage(msg);
            }
        }, 0);
    }

    public void showError(String msg) {
        Log.d("WhiteBoardCast", msg);
        showMessage(msg);
    }

    public void showMessage(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_LONG ).show();

    }

    boolean encoderInitDone = false;

    Handler handler = new Handler();

    PageScrollAnimator animator;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_whiteboardcast);
        readDebuggableSetting();
        getWhiteBoardCanvas().enableDebug(debuggable);
        animator = new PageScrollAnimator(presen.getScheduleExecutor(), getWhiteBoardCanvas());

        if(workingFileExists()) {
            showDialog(DIALOG_ID_QUERY_MERGE_AGAIN);
        }

        checkCanvasReady();
    }

    Runnable checkCanvasReadyCallback = new Runnable() {

        @Override
        public void run() {
            checkCanvasReady();
        }
    };


    private boolean checkCanvasReady() {
        if(encoderInitDone)
            return true;

        WhiteBoardCanvas wb = getWhiteBoardCanvas();
        long lastResetTime = wb.lastResetCanvas;
        if(lastResetTime == -1) {
            handler.postDelayed(checkCanvasReadyCallback, 100);
            return false;
        }


        long dur = System.currentTimeMillis() - lastResetTime;
        if(dur <= 50) {
            // still canvas size might be changed. wait a little more.
            handler.postDelayed(checkCanvasReadyCallback, 100);
            return false;
        }


        try {
            // This takes some milsec. Block UI thread...
            presen.newEncoderTask(wb, wb.getBitmap(), getWorkVideoPath(), this);
            encoderInitDone = true;
            wb.changeRecStatus();
            return true;
        } catch (IOException e) {
            showError("Fail to get workVideoPath: " + e.getMessage());
            return false;
        }
    }

    private boolean workingFileExists() {
        try {
            File workVideo = new File(getWorkVideoPath());
            if(workVideo.exists())
                return true;
            return false;
        } catch (IOException e) {
            showError("IO Exception while working file check: " + e.getMessage());
            return false;
        }
    }

    boolean debuggable = false;
    private void readDebuggableSetting() {
        PackageManager pm = getPackageManager();
        try {
            ApplicationInfo ai = pm.getApplicationInfo(getPackageName(), 0);
            if((ai.flags & ApplicationInfo.FLAG_DEBUGGABLE) != 0)
            {
                debuggable = true;
            }
        } catch (PackageManager.NameNotFoundException e) {
        }
    }

    public boolean slideAvailable() {
         return presen.slideAvailable();
    }

    public boolean canUndo() {
        WhiteBoardCanvas wb = getWhiteBoardCanvas();
        if(wb == null)
            return false;
        return wb.canUndo();
    }

    public void undo() {
        getWhiteBoardCanvas().undo();
    }

    boolean duringAnimation = false;
    void setDuringAnimation(boolean animation) {
        duringAnimation = animation;
    }


    public void stopRecord() {
        if(!canStop()) {
            Log.d("WhiteBoardCast", "stop record called but not recording. " + presen.recordStatus());
            return;
        }
        // under processing.
        presen.stopRecordBegin();
        getWhiteBoardCanvas().changeRecStatus();
        getWhiteBoardCanvas().stopTimeDraw();
        showMessage("record end, start post process...");

        presen.stopRecord();

        new Thread(new Runnable() {
            @Override
            public void run() {
                if(!presen.afterStop()) {
                    postErrorMessage("fail finalize encode: " + presen.getEncoderTask().getErrorBuf().toString());
                    return;

                }

                postShowMessage("post process done.");
                /*
                handler.post(new Runnable(){
                    @Override
                    public void run() {
                        beginAudioVideoMergeTask();
                    }
                }
                );
                */

            }
        }).start();
    }


    public boolean canStop() {
        return presen.canStopRecord();
    }

    public boolean canRedo() {
        WhiteBoardCanvas wb = getWhiteBoardCanvas();
        if(wb == null)
            return false;
        return wb.canRedo();
    }

    public void redo() {
        getWhiteBoardCanvas().redo();
    }


    public void toggleShowSlides() {
        try {
            getWhiteBoardCanvas().toggleShowSlides();
        } catch (IOException e) {
            showMessage("Toggle slides fail. " + e.getMessage());
        }

    }

    public Presentation.RecordStatus getRecStats() {
        if(!encoderInitDone)
            return Presentation.RecordStatus.SETUP;
        return presen.recordStatus();
    }


    public void pauseRecord() {
        presen.pauseRecord();
        getWhiteBoardCanvas().changeRecStatus();
        getWhiteBoardCanvas().stopTimeDraw();

        showMessage("pause");
    }

    public void resumeRecord() {
        presen.resumeRecord();

        presen.scheduleEncodeTask();
        presen.scheduleAudioRecordTask();
        WhiteBoardCanvas wb = getWhiteBoardCanvas();
        wb.notifyBeginMillChanged(presen.getBeginMill());
        wb.changeRecStatus();
        wb.startTimeDraw();

        showMessage("resume");
    }



    public void startRecord() {
        if(!presen.canStartRecord()) {
            Log.d("WhiteBoardCast", "record start but status is not dormant: " + presen.recordStatus());
            return;
        }
        presen.startRecordFirstPhase();
        getWhiteBoardCanvas().changeRecStatus();
        handler.post(new Runnable() {
            @Override
            public void run() {
                // this method take some times. so get it back to UI first for update overlay.
                startRecordSecondPhase();
            }
        });
    }

    public void pageUp() {
        if(!getWhiteBoardCanvas().beginPagePrev(animator)) {
            showMessage("First page, couldn't go up!");
        }
    }

    public void pageDown() {
        getWhiteBoardCanvas().beginPageNext(animator);
    }

    // TODO: move to Presentation as far as possible.
    private void startRecordSecondPhase() {
        WhiteBoardCanvas wb = getWhiteBoardCanvas();
        wb.invalWholeRegionForEncoder(); // for restart. make it a little heavy.
        long currentMill = System.currentTimeMillis();
        // TODO: set current mill here
        presen.setBeginMillToEncoder(currentMill);
        // presen.newEncoderTask(wb, wb.getBitmap(), getWorkVideoPath(), this);

        if(debuggable)
            presen.getEncoderTask().setFpsListener(getWhiteBoardCanvas().getEncoderFpsCounter());

        presen.newRecorder(currentMill);
        wb.notifyBeginMillChanged(currentMill);
        try {
            presen.setAudioFileName(getWorkAudioPath());
        } catch (IOException e) {
            showError("IOException: Create WhiteBoardCast folder fail: " + e.getMessage());
            return;
        }
        try {
            presen.prepareAudioRecorder();
        } catch (IOException e) {
            showError("IOException: MediaRecoder prepare fail: " + e.getMessage());
            return;
        } catch (VorbisException e) {
            showError("VorbisException: MediaRecoder prepare fail: " + e.getMessage());
            return;
        }

        presen.startRecord();
        wb.changeRecStatus();
        wb.startTimeDraw();
        showMessage("record start");
    }

    public WhiteBoardCanvas getWhiteBoardCanvas() {
        return (WhiteBoardCanvas)findViewById(R.id.fullscreen_content);
    }

    public static void ensureDirExist(File dir) throws IOException {
        if(!dir.exists()) {
            if(!dir.mkdir()){
                throw new IOException();
            }
        }
    }
    public static File getFileStoreDirectory() throws IOException {
        File dir = new File(Environment.getExternalStorageDirectory(), "WhiteBoardCast");
        ensureDirExist(dir);
        return dir;
    }


    public String getResultPath() throws IOException {
        return getFileStoreDirectory().getAbsolutePath() + "/result.webm";
    }

    private String getWorkAudioPath() throws IOException {
        return getFileStoreDirectory().getAbsolutePath() + "/" + AUDIO_FNAME;
    }

    private String getWorkVideoPath() throws IOException {
        return getFileStoreDirectory().getAbsolutePath() + "/temp.mp4";
    }



    private Uri insertLastResultToContentResolver() {
        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyy-MM-dd-HH:mm");
        String title = timeStampFormat.format(new Date()) + " recorded";

        ContentValues content = new ContentValues(4);


        content.put(MediaStore.Video.VideoColumns.TITLE, title);
        content.put(MediaStore.Video.VideoColumns.DATE_ADDED,
                System.currentTimeMillis() / 1000);
        content.put(MediaStore.Video.Media.MIME_TYPE, "video/webm");
        content.put(MediaStore.Video.Media.DATA, presen.getResultFile().getAbsolutePath());
        ContentResolver resolver = getBaseContext().getContentResolver();
        return resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, content);
    }

    private void exportPDF() {
        BoardList boardList = getWhiteBoardCanvas().getBoardList();
        ImagePDFWriter writer = null;
        try {
            File dir = getTemporaryPdfFolder();
            SlideList.deleteAllFiles(dir);

            pdfFile = createTemporaryPDF();
            writer = new ImagePDFWriter(pdfFile, boardList.getWidth(), boardList.getHeight(), boardList.size());
            for(int i = 0; i < boardList.size(); i++) {
                writer.writePage(boardList.getBoard(i).createSynthesizedTempBmp());
            }
            writer.done();
        } catch (IOException e) {
            postErrorMessage("Fail to write pdf: " + e.getMessage());
        }
    }

    private void beginAudioVideoMergeTask() {
        try {
            new AudioVideoMergeTask(this, new AudioVideoMergeTask.NotifyFinishListener() {
                @Override
                public void onFinish() {
                    exportPDF();
                    handler.postDelayed(new Runnable(){
                        @Override
                        public void run() {
                            try {
                                renameAndDeleteWorkFiles();
                                startDetailActivity();
                            } catch (IOException e) {
                                showError("Rename encoded file fail: " + e.getMessage());
                            }
                        }
                    }, 500);
                }
            }).execute(getWorkVideoPath(), getWorkAudioPath(), getResultPath());
        } catch (IOException e) {
            showError("Fail to create WhiteBoardCast folder(2). " + e.getMessage());
        }
    }

    void startDetailActivity()
    {
        Intent intent = new Intent(this, DetailActivity.class);
        intent.putExtra("VIDEO_PATH", presen.getResultFile().getAbsolutePath());
        intent.putExtra("VIDEO_URI", presen.getResultUri().toString());
        intent.putExtra("PDF_PATH", pdfFile.getAbsolutePath());
        startActivity(intent);
    }

    private void renameAndDeleteWorkFiles() throws IOException {
        File result = new File(getResultPath());
        File workVideo = new File(getWorkVideoPath());
        File workAudio = new File(getWorkAudioPath());
        if(!result.exists())
            throw new IOException("no encoded file exists.");

        presen.setResult(getDateNameFile(".webm"));
        result.renameTo(presen.getResultFile());
        workVideo.delete();
        workAudio.delete();

        presen.setResultUri(insertLastResultToContentResolver());


    }

    private File getDateNameFile(String extension) throws IOException {
        File folder = getFileStoreDirectory();
        return getDateNameFile(folder, extension);
    }

    private File createTemporaryPDF() throws IOException {
        File dir = getTemporaryPdfFolder();
        return getDateNameFile(dir, ".pdf");
    }

    private File getTemporaryPdfFolder() throws IOException {
        File parent = getFileStoreDirectory();
        File dir = new File(parent, "temporaryPdf");
        ensureDirExist(dir);
        return dir;
    }

    private File getDateNameFile(File folder, String extension) {
        SimpleDateFormat timeStampFormat = new SimpleDateFormat("yyyyMMddHHmmssSS");
        String filename = timeStampFormat.format(new Date()) + extension;

        return new File(folder, filename);
    }


    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getAction() == KeyEvent.ACTION_DOWN)
        {
            switch(event.getKeyCode()) {
                case KeyEvent.KEYCODE_BACK:
                    if(getWhiteBoardCanvas().handleBackKey())
                        return true;

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
    public void openOptionsMenu() {
        // for large device, android ignore openOptionsMenu (why!?).
        // See http://stackoverflow.com/questions/9996333/openoptionsmenu-function-not-working-in-ics
        Configuration config = getResources().getConfiguration();

        if((config.screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK)
                > Configuration.SCREENLAYOUT_SIZE_LARGE) {

            int originalScreenLayout = config.screenLayout;
            config.screenLayout = Configuration.SCREENLAYOUT_SIZE_LARGE;
            super.openOptionsMenu();
            config.screenLayout = originalScreenLayout;
        } else {
            super.openOptionsMenu();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    protected void onResume() {
        super.onResume();

        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(this);
        getWhiteBoardCanvas().setDisableTouch(pref.getBoolean("DISABLE_TOUCH", false));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean canManageSlides = !canStop();
        menu.findItem(R.id.menu_id_new).setEnabled(canManageSlides);

        return super.onPrepareOptionsMenu(menu);
    }

    public void clearCanvas() {
        getWhiteBoardCanvas().clearCanvas();
    }

    @Override
    public boolean onMenuItemSelected(int featureId, MenuItem item) {
        switch(item.getItemId()) {
            case R.id.menu_id_about:
                showDialog(DIALOG_ID_ABOUT);
                return true;
            case R.id.menu_id_quit:
                finish();
                return true;
            case R.id.menu_id_new:
                showDialog(DIALOG_ID_NEW);
                return true;
            case R.id.menu_id_settings:
                Intent intent = new Intent(this, SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onMenuItemSelected(featureId, item);
    }

    @Override
    protected Dialog onCreateDialog(int id) {
        switch(id) {
            case DIALOG_ID_ABOUT:
                return createAbout();
            case DIALOG_ID_QUERY_MERGE_AGAIN:
                return createQueryMergeWorkingFileDialog();
            case DIALOG_ID_NEW:
                return createNewDialog();

        }
        return super.onCreateDialog(id);
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch(id) {
            case DIALOG_ID_COPYING:
                ImportDialog imp = new ImportDialog(this);
                imp.prepareCopy(getContentResolver(), getWhiteBoardCanvas().getStoredWidth(),
                        getWhiteBoardCanvas().getStoredHeight(), args.getStringArrayList("all_path"), new ImportDialog.FinishListener() {
                            @Override
                            public void onFinish() {
                                try {
                                    setupSlidePresentation();
                                } catch (IOException e) {
                                    showError("Fail to setup presentation: " + e.getMessage());
                                }
                            }
                        });
                return imp;
        }
        return super.onCreateDialog(id, args);
    }

    private void startNewPresentation(){
        newPresentation();
        newDialog.dismiss();
        showMessage("New");
    }

    private void newPresentation() {
        presen = new Presentation();
        getWhiteBoardCanvas().newPresentation();
    }

    private void startNewSlidesPresentation() {
        Intent intent = new Intent(this, MultiGalleryActivity.class);
        startActivityForResult(intent, REQUEST_PICK_IMAGE);
        newDialog.dismiss();
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode) {
            case REQUEST_PICK_IMAGE:
                if(resultCode == RESULT_OK &&
                        data != null &&
                        data.getStringArrayListExtra("all_path").size() != 0){
                    try {
                        presen.clearSlides();
                        Bundle bundle = new Bundle();
                        bundle.putStringArrayList("all_path", data.getStringArrayListExtra("all_path"));
                        showDialog(DIALOG_ID_COPYING, bundle);
                    } catch (IOException e) {
                        showMessage("Fail to clear slides: " + e.getMessage());
                    }
                }
        }
    }


    Dialog newDialog;

    private Dialog createNewDialog() {
        View view = getLayoutInflater().inflate(R.layout.new_dialog, null);
        ((Button)view.findViewById(R.id.buttonPlane)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewPresentation();
            }
        });
        ((Button)view.findViewById(R.id.buttonSlides)).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startNewSlidesPresentation();
            }
        });
        newDialog = new AlertDialog.Builder(this)
                .setView(view)
                .create();
        return newDialog;
    }


    File pdfFile;

    private void setupSlidePresentation() throws IOException {
        newPresentation();

        presen.enableSlide();
        getWhiteBoardCanvas().setSlides(presen.getSlideFiles());
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog) {
        switch(id) {
            case DIALOG_ID_NEW:
                newDialog = dialog;
                break;
        }
        super.onPrepareDialog(id, dialog);
    }


    private Dialog createQueryMergeWorkingFileDialog() {
        return new AlertDialog.Builder(this).setTitle(R.string.query_merge_title)
                .setMessage(R.string.query_merge_body)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        beginAudioVideoMergeTask();
                    }
                })
                .setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                    }
                })
                .create();
    }

    private AlertDialog createAbout() {
        final WebView webView = new WebView(this);
        webView.loadUrl("file:///android_asset/licenses.html");
        return new AlertDialog.Builder(this).setTitle(R.string.about_title)
                .setView(webView)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int whichButton) {
                        dialog.dismiss();
                    }
                }).create();

    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);

    }

    public void setPen() {
        getWhiteBoardCanvas().setPen();
    }

    public void setEraser() {
        getWhiteBoardCanvas().setEraser();
    }

    @Override
    public void setColor(int color) {
        getWhiteBoardCanvas().setPenColor(color);
    }

    // size: 0 to 100
    public void setPenOeEraserSize(int size) {
        getWhiteBoardCanvas().setPenOrEraserSize(size);
    }
}
