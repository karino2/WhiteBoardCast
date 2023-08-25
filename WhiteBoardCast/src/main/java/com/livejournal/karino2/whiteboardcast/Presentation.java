package com.livejournal.karino2.whiteboardcast;

import android.graphics.Bitmap;
import android.media.MediaMuxer;
import android.net.Uri;

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

    WorkFileStore fileStore;

    Presentation(WorkFileStore fstore) {
        fileStore = fstore;
    }

    public void stopRecord() {
        recStats = RecordStatus.DONE;
    }

    public void stopRecordBegin() {
        recStats = RecordStatus.DONE_PROCESS;
    }

    public void afterStop() {
        if(videoEncodeFuture != null)
            videoEncodeFuture.cancel(false);
        videoEncodeFuture = null;
        stopAudioEncoderTask();

        recorder.finalizeAndRelease();

        encoderTask.doneEncoder();
        muxer.stop();
        muxer.release();
        encoderTask = null;
        muxer = null;
    }


    public void newRecorder(long currentMill) {
        recorder = new Mp4aRecorder(muxer, currentMill);
    }


    public void prepareAudioRecorder() throws IOException {
        recorder.prepare();
    }

    public void startRecord() {
        // to ensure muxer addTrack, first input something to inputBuffer.
        // In release build, it takes about 60msec. Not noticeable to user.
        // But in debug buid, it takes 2 sec. It's confusing if we skip this code and wait 2 sec after start recording.
        // It's better wait UI if it didn't start actually.
        firstEncodeOnce();

        recorder.start();
        scheduleEncodeTask();
        scheduleAudioRecordTask();
        recStats = RecordStatus.RECORDING;
    }

    public void startRecordFirstPhase() {
        recStats = RecordStatus.SETUP;
    }

    public void clearSlides() throws IOException {
        getSlideList().deleteAll();
    }


    public void startEncoder(long newMill) {
        encoderTask.setBeginMill(newMill);
        encoderTask.startEncoder();
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

    Future<?> videoEncodeFuture = null;
    Thread recorderThread = null;



    public void newEncoderTask(FrameRetrieval frameR, Bitmap parentBmp, String workVideoPath, EncoderTask.ErrorListener elistn) throws IOException {
        muxer = new AudioVideoMuxer(new MediaMuxer(workVideoPath, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4));
        encoderTask = new EncoderTask(frameR, parentBmp, workVideoPath, elistn, muxer);
    }

    // second time recording.
    // creating encoder here harm user experience, but second time, codec enumeratoin is not necessary.
    // So I guess second time might be acceptable.
    public void ensureEncoderTask(FrameRetrieval frameR, Bitmap parentBmp, String workVideoPath, EncoderTask.ErrorListener elistn) throws IOException {
        if(muxer == null) {
            newEncoderTask(frameR, parentBmp, workVideoPath, elistn);
        }
    }

    public void pauseRecord() {
        recStats = RecordStatus.PAUSE;

        videoEncodeFuture.cancel(false);
        videoEncodeFuture = null;
        stopAudioEncoderTask();

        recorder.pause();
        encoderTask.pause();

    }

    private void stopAudioEncoderTask() {
        recorder.cancel();
        if(recorderThread != null) {
            try {
                recorderThread.join();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        recorderThread = null;
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

        scheduleEncodeTask();
        scheduleAudioRecordTask();
    }

    public void firstEncodeOnce() {
        // to make muxer ready.
        encoderTask.firstEncodeOnceBlocking();
    }

    private final int FPS = 12;
    //    private final int FPS = 6;
    //    private final int FPS = 30;
    public void scheduleEncodeTask() {
        videoEncodeFuture = getScheduleExecutor().scheduleAtFixedRate(encoderTask, 0, 1000 / FPS, TimeUnit.MILLISECONDS);
    }

    public void scheduleAudioRecordTask() {
        // to make muxer ready.
        recorder.drain();
        recorderThread = new Thread(recorder);
        recorderThread.start();
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
            slideList = new SlideList(fileStore);
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
