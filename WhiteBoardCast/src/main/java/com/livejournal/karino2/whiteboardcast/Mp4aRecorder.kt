package com.livejournal.karino2.whiteboardcast

import android.media.*
import android.util.Log
import java.nio.ByteBuffer
import android.media.MediaCodec


class Mp4aRecorder(val muxer: MediaMuxer) {
    private val AUDIO_MIME_TYPE = "audio/mp4a-latm"
    private val SAMPLE_RATE = 44100
    private val SAMPLES_PER_FRAME = 1024
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    internal val TIMER_INTERVAL = 120

    var beginMill = 0L
    fun resume(pasuedMil: Long) {
        if (state == State.STOPPED) {
            beginMill += pasuedMil
            start()
        } else {
            Log.d("WhiteBoardCast", "VorbisRec: resume call but state is not stopped: $state")
        }
    }

    fun start() {
        if (state == State.READY || state == State.STOPPED) {
            audioRecorder.startRecording()
            val buf = ensureByteBuffer() ?: return // fatal situation. how to recover?
            buf.clear()
            audioRecorder.read(buf, buf.remaining())
            state = State.RECORDING
        }
    }

    fun stop() {
        if (state == State.RECORDING) {
            updateListener.onPeriodicNotification(audioRecorder)

            // drain again to consume all unflushed encoder data.
            encoder.flush()
            drain()

            audioRecorder.stop()

            state = State.STOPPED
        }
    }

    fun release() {
        if (state == State.RECORDING) {
            stop()
        }
        // may be we should queueInputBuffer if exist.
        releaseCurrentBuffer()
        encoder.stop()
        encoder.release()
        audioRecorder.release()
    }


    private enum class State {
        INITIALIZING, READY, RECORDING, ERROR, STOPPED
    }
    private var state = State.INITIALIZING

    val audioRecorder: AudioRecord
    val trackIndex: Int
    val encoder: MediaCodec

    init {
        val min_buffer_size = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        var buffer_size = SAMPLES_PER_FRAME * 10
        if (buffer_size < min_buffer_size)
            buffer_size = (min_buffer_size / SAMPLES_PER_FRAME + 1) * SAMPLES_PER_FRAME * 2
        audioRecorder = AudioRecord(MediaRecorder.AudioSource.MIC,
                SAMPLE_RATE,
                CHANNEL_CONFIG,
                AUDIO_FORMAT,
                buffer_size)


        val format = MediaFormat()
        format.setString(MediaFormat.KEY_MIME, AUDIO_MIME_TYPE)
        format.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        format.setInteger(MediaFormat.KEY_SAMPLE_RATE, 44100)
        format.setInteger(MediaFormat.KEY_CHANNEL_COUNT, 1)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 128000)
        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)

        trackIndex = muxer.addTrack(format)
        encoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    var currentBufIndex = -1
    var currentBuffer : ByteBuffer? = null
    val TIMEOUT_USEC = 10000L

    fun ensureByteBuffer() : ByteBuffer? {
        if(currentBuffer == null) {
            val bufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
            if(bufIndex < 0) {
                Log.d("WhiteBoardCast", "audio encoder input buffer not available.")
                return null
            }
            currentBufIndex = bufIndex
            currentBuffer = encoder.getInputBuffer(bufIndex)
        }
        return currentBuffer
    }

    fun releaseCurrentBuffer() {
        currentBuffer = null
        currentBufIndex = -1
    }

    internal var updateListener: AudioRecord.OnRecordPositionUpdateListener = object : AudioRecord.OnRecordPositionUpdateListener {

        override fun onMarkerReached(audioRecord: AudioRecord) {
            // do nothing.
        }

        override fun onPeriodicNotification(audioRecord: AudioRecord) {
            val curBuf = ensureByteBuffer() ?: return // no avaiable encode input buffer. just ignore this frame.

            curBuf.clear()

            val readLen = audioRecord.read(curBuf, curBuf.remaining())
            if (readLen > 0) {
                val currentMil = System.currentTimeMillis()

                encoder.queueInputBuffer(currentBufIndex, 0, curBuf.position(),(currentMil-beginMill)*1000 , 0)
                releaseCurrentBuffer()

                drain()
            }

        }
    }

    val encoderInfo = MediaCodec.BufferInfo()

    fun drain() {
        val bufIndex = encoder.dequeueOutputBuffer(encoderInfo, TIMEOUT_USEC)
        if(bufIndex < 0)
            return


        val outBuf = encoder.getOutputBuffer(bufIndex)!!
        outBuf.position(encoderInfo.offset)
        outBuf.limit(encoderInfo.offset+encoderInfo.size)
        synchronized(muxer){
            muxer.writeSampleData(trackIndex, outBuf, encoderInfo)
        }
        encoder.releaseOutputBuffer(bufIndex, false)
    }

    fun prepare() {
        if(state == State.INITIALIZING) {
            if (audioRecorder.state != AudioRecord.STATE_INITIALIZED)
                throw RuntimeException("AudioRecord initialization failed")
            val framePeriod =  SAMPLE_RATE * TIMER_INTERVAL / 1000
            encoder.start()


            audioRecorder.setRecordPositionUpdateListener(updateListener)
            audioRecorder.positionNotificationPeriod = framePeriod
        }
    }


}