package com.livejournal.karino2.whiteboardcast;

import android.app.ProgressDialog;
import android.content.Context;
import android.media.AudioRecord;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.google.libvorbis.VorbisEncConfig;
import com.google.libvorbis.VorbisEncoderC;
import com.google.libvpx.LibVpxEnc;
import com.google.libvpx.LibVpxEncConfig;
import com.google.libvpx.Rational;
import com.google.libvpx.VpxCodecCxPkt;
import com.google.libwebm.mkvmuxer.AudioTrack;
import com.google.libwebm.mkvmuxer.MkvWriter;
import com.google.libwebm.mkvmuxer.Segment;
import com.google.libwebm.mkvmuxer.SegmentInfo;
import com.google.libwebm.mkvmuxer.VideoTrack;
import com.google.utils.WavReader;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * Created by karino on 6/28/13.
 */
public class AudioVideoMergeTask extends AsyncTask<String, Integer, String> {

    private Context context;

    private ProgressDialog dialog;
    public AudioVideoMergeTask(Context ctx) {
        context = ctx;
        dialog = new ProgressDialog(ctx);
    }

    @Override
    protected void onPreExecute() {
        dialog.setTitle("Please wait");
        dialog.show();
    }

    class PCMReader {
        private FileInputStream is = null;
        PCMReader(File file) throws IOException {
            is = new FileInputStream(file);
        }

        private int channelNum = 2;
        private int bps = 16;

        byte[] readSamples(int numSamples) throws IOException {
            final int bytesToRead = numSamples * channelNum * bps;
            byte[] dataArray = new byte[bytesToRead];
            if (is.read(dataArray) == -1)
                throw new IOException("Error reading samples.");
            return dataArray;
        }

        int samplesRemaining() {
            int samplesLeft = 0;
            try {
                samplesLeft = (is.available()) / (channelNum * bps);
            } catch (Exception e) {
                return 0;
            }
            return samplesLeft;
        }

    }


    @Override
    protected String doInBackground(String... files) {
        String videoPath = files[0];
        String audioPath = files[1];
        String resultPath = files[2];

        MkvWriter mkvWriter = null;

        try {
            WebmReader webmReader = new WebmReader();

            if(!webmReader.open(videoPath)) {
                return new String("Input file is invalid or error while opening.");
            }
            webmReader.initTracks();
            com.google.libwebm.mkvparser.VideoTrack firstTrack = (com.google.libwebm.mkvparser.VideoTrack)webmReader.getCurrentTrack();


            WebmReader audioReader = new WebmReader();
            if(!audioReader.open(audioPath)) {
                return new String("Error opening ogg file:" + audioPath);
            }
            audioReader.initTracks();

            mkvWriter = new MkvWriter();
            if (!mkvWriter.open(resultPath)) {
                return new String("WebM Output name is invalid or error while opening.");
            }

            Segment muxerSegment = new Segment();
            if (!muxerSegment.init(mkvWriter)) {
                return new String("Could not initialize muxer segment.");
            }

            SegmentInfo muxerSegmentInfo = muxerSegment.getSegmentInfo();
            muxerSegmentInfo.setWritingApp("MergeAudioVideo");

            long newVideoTrackNumber = 0;

            String trackName = firstTrack.getNameAsUtf8();
            long width = firstTrack.getWidth();
            long height = firstTrack.getHeight();
            newVideoTrackNumber = muxerSegment.addVideoTrack((int) width, (int) height, 0);
            if (newVideoTrackNumber == 0) {
                return new String("Could not add video track.");
            }

            VideoTrack muxerTrack = (VideoTrack) muxerSegment.getTrackByNumber(newVideoTrackNumber);
            if (muxerTrack == null) {
                return new String("Could not get video track.");
            }

            if (trackName != null) {
                muxerTrack.setName(trackName);
            }
            double rate = firstTrack.getFrameRate();
            if (rate > 0) {
                muxerTrack.setFrameRate(rate);
            }

            com.google.libwebm.mkvparser.AudioTrack firstAudioTrack = (com.google.libwebm.mkvparser.AudioTrack)audioReader.getCurrentTrack();

            long newAudioTrackNumber = muxerSegment.addAudioTrack((int)firstAudioTrack.getSamplingRate(), (int)firstAudioTrack.getChannels(), 0);
            if (newAudioTrackNumber == 0) {
                return new String("Could not add audio track.");
            }

            AudioTrack muxerAudioTrack = (AudioTrack) muxerSegment.getTrackByNumber(newAudioTrackNumber);
            if (muxerAudioTrack == null) {
                return new String("Could not get audio track.");
            }

            long[] outputPrivateSize = {0};
            byte[] privateData = firstAudioTrack.getCodecPrivate(outputPrivateSize);
            long privateSize = outputPrivateSize[0];
            if (privateSize > 0 && !muxerAudioTrack.setCodecPrivate(privateData)) {
                return new String("Could not add audio private data.");
            }

            long bitDepth = firstAudioTrack.getBitDepth();
            if (bitDepth > 0) {
                muxerAudioTrack.setBitDepth(bitDepth);
            }


            muxerSegment.cuesTrack(newVideoTrackNumber);
            muxerSegment.cuesTrack(newAudioTrackNumber);

            if (!webmReader.initCluster() ) {
                return new String("initCluster fail. invalid webm");
            }
            if (!webmReader.initFrameQueue()) {
                return new String("initFrameQueue fail. invalid webm");
            }
            if (!audioReader.initCluster() ) {
                return new String("initCluster fail. invalid audio mkv");
            }
            if (!audioReader.initFrameQueue()) {
                return new String("initFrameQueue fail. invalid audio mkv");
            }


            byte[] vorbisFrame = null;
            byte[] webmFrame = null;
            boolean audioDone = false;
            boolean videoDone = false;
            boolean encoding = true;
            vorbisFrame = audioReader.popFrame();
            webmFrame = webmReader.popFrame();
            while (encoding) {
                if(vorbisFrame == null) {
                    audioDone = true;
                }
                if(webmFrame == null) {
                    videoDone = true;
                }
                if(!audioDone && !videoDone) {
                    if(webmReader.getBlockTimeNS() > audioReader.getBlockTimeNS()) {
                        while(!audioDone && webmReader.getBlockTimeNS() > audioReader.getBlockTimeNS()) {
                            if (!muxerSegment.addFrame(vorbisFrame, newAudioTrackNumber, audioReader.getBlockTimeNS(), audioReader.isKey())) {
                                return new String("Could not add audio frame1.");
                            }
                            vorbisFrame = audioReader.popFrame();
                            if(vorbisFrame == null) {
                                audioDone = true;
                            }
                        }
                    } else {
                        while(!videoDone && webmReader.getBlockTimeNS() <= audioReader.getBlockTimeNS()) {
                            if (!muxerSegment.addFrame(webmFrame, newVideoTrackNumber, webmReader.getBlockTimeNS(), webmReader.isKey())) {
                                return new String("Could not add video frame1.");
                            }
                            webmFrame = webmReader.popFrame();
                            if(webmFrame == null) {
                                videoDone = true;
                            }
                        }
                    }
                }else if(!audioDone) {
                    if (!muxerSegment.addFrame(vorbisFrame, newAudioTrackNumber, audioReader.getBlockTimeNS(), audioReader.isKey())) {
                        return new String("Could not add audio frame2.");
                    }
                    vorbisFrame = audioReader.popFrame();
                }else if(!videoDone) {
                    if (!muxerSegment.addFrame(webmFrame, newVideoTrackNumber, webmReader.getBlockTimeNS(), webmReader.isKey())) {
                        return new String("Could not add video frame2.");
                    }
                    webmFrame = webmReader.popFrame();
                } else {
                    encoding = false;
                    break;
                }

            }
            audioReader.close();
            webmReader.close();

            if (!muxerSegment.finalizeSegment()) {
                return new String("Finalization of segment failed.");
            }

        } catch (Exception e) {
            Log.d("WBCast", e.getMessage() );
            return new String("Caught error in main encode loop. " + e.getMessage());
        } finally {
            if (mkvWriter != null) {
                mkvWriter.close();
            }
        }

        return new String("Success!");
    }

    protected void onProgressUpdate(Integer... progress) {
        // later.
    }

    protected void onPostExecute(String result) {
        Toast.makeText(context, result, Toast.LENGTH_LONG ).show();
        dialog.dismiss();
    }
}
