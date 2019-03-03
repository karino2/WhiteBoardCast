package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.media.MediaMuxer;
import android.net.Uri;

import com.google.libvorbis.VorbisException;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by karino on 11/28/13.
 */
public class Presentation {
    Mp4aRecorder recorder;
    AudioVideoMuxer muxer;

    public void stopRecord() {
        recorder.stop();
        recorder.release();

        muxer.stop();
        muxer.release();

        recStats = RecordStatus.DONE;
    }

    public void stopRecordBegin() {
        recStats = RecordStatus.DONE_PROCESS;
    }

    public boolean afterStop() {
        if(future != null)
            future.cancel(false);
        future = null;
        return encoderTask.doneEncoder();
    }


    public void newRecorder(long currentMill) {
        recorder = new Mp4aRecorder(muxer, currentMill);
        // recorder = new VorbisMediaRecorder();
        // recorder.setBeginMill(currentMill);
    }

    public void setAudioFileName(String fileName) {
        // TODO: remove this.
        // recorder.setOutputFile(fileName);
    }

    public void prepareAudioRecorder() throws IOException, VorbisException {
        recorder.prepare();
    }

    public void startRecord() {
        // muxer.start();
        recorder.start();

        scheduleEncodeTask();
        recStats = RecordStatus.RECORDING;
    }

    public void startRecordFirstPhase() {
        recStats = RecordStatus.SETUP;
    }

    public void clearSlides() throws IOException {
        getSlideList().deleteAll();
    }

    public enum RecordStatus {
        DORMANT, SETUP, RECORDING, PAUSE, DONE_PROCESS, DONE
    }

    RecordStatus recStats = RecordStatus.DORMANT;

    public RecordStatus recordStatus() {
        return recStats;
    }


    public boolean canStartRecord() {
        return recStats == RecordStatus.DORMANT ||
                recStats == RecordStatus.DONE;
    }

    public boolean canStopRecord() {
        return recStats == RecordStatus.RECORDING ||
                recStats == RecordStatus.PAUSE;
    }

    private EncoderTask encoderTask = null;
    public EncoderTask getEncoderTask() {
        return encoderTask;
    }

    Future<?> future = null;



    public void newEncoderTask(FrameRetrieval frameR, Bitmap parentBmp, String workVideoPath, EncoderTask.ErrorListener elistn, long currentMil) throws IOException {
        muxer = new AudioVideoMuxer(new MediaMuxer(workVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4));
        encoderTask = new EncoderTask(frameR, parentBmp, workVideoPath, elistn, currentMil, muxer);
    }

    public void pauseRecord() {
        recStats = RecordStatus.PAUSE;

        future.cancel(false);
        future = null;
        recorder.stop();
        encoderTask.stop();

    }

    public long getBeginMill() {
        return recorder.getBeginMill();
    }

    public void resumeRecord() {
        recStats = RecordStatus.RECORDING;
        long suspendedBegin = recorder.lastBlockEndMil();
        long suspendedDur = System.currentTimeMillis() - suspendedBegin;
        recorder.resume(suspendedDur);
        encoderTask.resume(suspendedDur);

    }

    private final int FPS = 12;
    //    private final int FPS = 6;
//    private final int FPS = 30;
    public void scheduleEncodeTask() {
        future = getScheduleExecutor().scheduleAtFixedRate(encoderTask, 0, 1000 / FPS, TimeUnit.MILLISECONDS);

    }

    ScheduledExecutorService scheduleExecuter = null;
    ScheduledExecutorService getScheduleExecutor() {
        if(scheduleExecuter == null) {
            scheduleExecuter = Executors.newScheduledThreadPool(2);
        }
        return scheduleExecuter;
    }

    SlideList slideList;
    SlideList getSlideList() throws IOException {
        if(slideList == null) {
            slideList = SlideList.createSlideListWithDefaultFolder();
        }
        return slideList;
    }

    boolean slideEnabled = false;
    public void enableSlide() {
        slideEnabled = true;
    }

    public boolean slideAvailable() {
        return slideEnabled;

    }

    List<File> getSlideFiles() throws IOException {
        return getSlideList().getFiles();
    }


    public File getResultFile() {
        return lastResult;
    }
    public void setResult(File file) {
        lastResult = file;
    }

    public void renameResult(File newNameFile) {
        lastResult.renameTo(newNameFile);
        lastResult = newNameFile;
    }

    File lastResult = null;
    Uri lastResultUri = null;

    public Uri getResultUri() {
        return lastResultUri;
    }

    public void setResultUri(Uri resultUri) {
        this.lastResultUri = resultUri;
    }


}
