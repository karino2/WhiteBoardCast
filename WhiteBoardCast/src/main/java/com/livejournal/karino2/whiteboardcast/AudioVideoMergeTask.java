package com.livejournal.karino2.whiteboardcast;

import android.app.ProgressDialog;
import android.content.Context;
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

        LibVpxEncConfig vpxConfig = null;
        LibVpxEnc vpxEncoder = null;
        VorbisEncoderC vorbisEncoder = null;
        VorbisEncConfig vorbisConfig = null;
        MkvWriter mkvWriter = null;

        try {
            WebmReader webmReader = new WebmReader();

            if(!webmReader.open(videoPath)) {
                return new String("Input file is invalid or error while opening.");
            }
            webmReader.initTracks();
            com.google.libwebm.mkvparser.VideoTrack firstTrack = webmReader.getCurrentTrack();


            vpxConfig = new LibVpxEncConfig((int)firstTrack.getWidth(), (int)firstTrack.getHeight());
            vpxEncoder = new LibVpxEnc(vpxConfig);

            // libwebm expects nanosecond units
            vpxConfig.setTimebase(1, 1000000000);
            Rational timeBase = vpxConfig.getTimebase();
            Rational timeMultiplier = timeBase.multiply(new Rational((long)(100000*firstTrack.getFrameRate()), 100000)).reciprocal();
            int framesIn = 1;

            Log.d("WBCast", "audioPath=" + audioPath);

            File pcmFile = new File(audioPath);
            WavReader wavReader = null;
            try {
                wavReader = new WavReader(pcmFile);
            } catch (Exception e) {
                return new String("Error creating wav file:" + audioPath);
            }

            int channels = wavReader.nChannels();
            int sampleRate = wavReader.nSamplesPerSec();
            /*
            int channels = 2;
            int sampleRate = 44100;
            int bitsPerSample = 16;
            */
            int bitsPerSample = wavReader.wBitsPerSample();

            Log.d("WBCast", "channels="+channels +", sampleRate=" + sampleRate + ", bitsPerSample=" + bitsPerSample);
            vorbisConfig = new VorbisEncConfig(channels, sampleRate, bitsPerSample);
            vorbisConfig.setTimebase(1, 1000000000);

            vorbisEncoder = new VorbisEncoderC(vorbisConfig);

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

            long i = 0;
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

            /*
            VideoTrack.StereoMode stereoMode = ;
            if (stereoMode != com.google.libwebm.mkvmuxer.VideoTrack.StereoMode.kMono) {
                muxerTrack.setStereoMode(stereoMode);
            }
            */
            double rate = firstTrack.getFrameRate();
            if (rate > 0) {
                muxerTrack.setFrameRate(rate);
            }

            // Add audio Track
            long newAudioTrackNumber = muxerSegment.addAudioTrack(sampleRate, channels, 0);
            if (newAudioTrackNumber == 0) {
                return new String("Could not add audio track.");
            }

            AudioTrack muxerAudioTrack = (AudioTrack) muxerSegment.getTrackByNumber(newAudioTrackNumber);
            if (muxerAudioTrack == null) {
                return new String("Could not get audio track.");
            }

            byte[] buffer = vorbisEncoder.CodecPrivate();
            if (buffer == null) {
                return new String("Could not get audio private data.");
            }
            if (!muxerAudioTrack.setCodecPrivate(buffer)) {
                return new String("Could not add audio private data.");
            }

            muxerSegment.cuesTrack(newVideoTrackNumber);
            muxerSegment.cuesTrack(newAudioTrackNumber);

            if (!webmReader.initCluster() ) {
                return new String("initCluster fail. invalid webm");
            }
            if (!webmReader.initFrameQueue()) {
                return new String("initFrameQueue fail. invalid webm");
            }


            final int maxSamplesToRead = 1000;
            long[] returnTimestamp = new long[2];
            long vorbisTimestamp = 0;
            byte[] vorbisFrame = null;
            byte[] webmFrame = null;
            VpxCodecCxPkt pkt = null;
            int pktIndex = 0;
            boolean audioDone = false;
            boolean videoDone = false;
            boolean encoding = true;
            while (encoding) {
                // Prime the audio encoder.
                while (vorbisFrame == null) {
                    final int samplesLeft = wavReader.samplesRemaining();
                    final int samplesToRead = Math.min(samplesLeft, maxSamplesToRead);
                    if (samplesToRead > 0) {
                        // Read raw audio data.
                        byte[] pcmArray = null;
                        try {
                            pcmArray = wavReader.readSamples(samplesToRead);
                        } catch (Exception e) {
                            return new String("Could not read audio samples.");
                        }

                        if (!vorbisEncoder.Encode(pcmArray))
                            return new String("Error encoding audio samples.");

                        vorbisFrame = vorbisEncoder.ReadCompressedAudio(returnTimestamp);

                        // Matroska is in nanoseconds.
                        if (vorbisFrame != null) {
                            vorbisTimestamp = returnTimestamp[0] * 1000000;
                        }
                    } else {
                        audioDone = true;
                        break;
                    }
                }

                if (webmFrame == null) {
                    // Read raw video data.
                    webmFrame = webmReader.popFrame();
                    if (webmFrame == null) {
                        videoDone = true;
                    }
                }

                if ((audioDone && videoDone)) break;

                if (!videoDone && (audioDone || webmReader.getBlockTimeNS() <= vorbisTimestamp)) {
                    if (!muxerSegment.addFrame(webmFrame, newVideoTrackNumber, webmReader.getBlockTimeNS(), webmReader.isKey())) {
                        return new String("Could not add video frame.");
                    }
                    webmFrame = webmReader.popFrame();
                    if (webmFrame == null) {
                        videoDone = true;
                    }
                } else if (!audioDone) {
                    if (!muxerSegment.addFrame(vorbisFrame, newAudioTrackNumber, vorbisTimestamp, true)) {
                        return new String("Could not add audio frame.");
                    }

                    // Read the next compressed audio frame.
                    vorbisFrame = vorbisEncoder.ReadCompressedAudio(returnTimestamp);
                    if (vorbisFrame != null) {
                        vorbisTimestamp = returnTimestamp[0] * 1000000;
                    }
                }
            }

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
