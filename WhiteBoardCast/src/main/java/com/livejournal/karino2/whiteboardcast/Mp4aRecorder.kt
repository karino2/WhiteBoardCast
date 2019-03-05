package com.livejournal.karino2.whiteboardcast

import android.media.*
import android.util.Log
import android.media.MediaCodec


class Mp4aRecorder(val muxer: AudioVideoMuxer, beginMil: Long) : Runnable {

    private val AUDIO_MIME_TYPE = "audio/mp4a-latm"
    private val SAMPLE_RATE = 44100
    private val SAMPLES_PER_FRAME =  1024
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    var audioDurationMill = 0L
    private val TIMEOUT_US=1000L

    var beginMill: Long = beginMil
    set(value) {
        field = value
        audioDurationMill = 0L
    }


    fun resume(pasuedMil: Long) {
        if (state == State.STOPPED) {
            beginMill += pasuedMil
            start()
        } else {
            Log.d("WhiteBoardCast", "AudioRec: resume call but state is not stopped: $state")
        }
    }

    fun start() {
        if (state == State.READY || state == State.STOPPED) {
            state = State.RECORDING
            audioRecorder.startRecording()
        }
    }

    fun pause() {
        stop(false)
    }

    fun stop(forFinalize: Boolean) {
        if (state == State.RECORDING) {
            if(forFinalize)
                readAndEncode(true)

            // drain again to consume all unflushed encoder data.
            encoder.flush()
            drain()

            audioRecorder.stop()

            state = State.STOPPED
        }
    }

    fun stopForFinalize() {
        stop(true)
    }

    fun finalize() {
        if (state == State.RECORDING) {
            stopForFinalize()
        }
        encoder.stop()
        encoder.release()
        audioRecorder.release()
    }


    private enum class State {
        INITIALIZING, READY, RECORDING, ERROR, STOPPED
    }
    private var state = State.INITIALIZING

    val audioRecorder: AudioRecord
    var trackIndex: Int = 0
    val encoder: MediaCodec


    init {
        val min_buffer_size = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)

        var buffer_size = SAMPLES_PER_FRAME * 2
        if (buffer_size < min_buffer_size) {
            buffer_size = (min_buffer_size / SAMPLES_PER_FRAME + 1) * SAMPLES_PER_FRAME * 2
        }

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
//        format.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384)

        encoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    val bufInfo = MediaCodec.BufferInfo()
    var requestStart = false

    fun drain() {
        if(requestStart && !muxer.isReady)
            return
        val bufIndex = encoder.dequeueOutputBuffer(bufInfo, 0)
        if(bufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            trackIndex = muxer.addTrack(encoder.outputFormat)
            muxer.requestStart()
            requestStart = true
            return
        }

        if(bufIndex < 0)
            return

        if(bufInfo.size != 0) {
            val outBuf = encoder.getOutputBuffer(bufIndex)!!
            outBuf.position(bufInfo.offset)
            outBuf.limit(bufInfo.offset+bufInfo.size)
            muxer.writeSampleData(trackIndex, outBuf, bufInfo)
        }
        encoder.releaseOutputBuffer(bufIndex, false)
    }

    fun readAndEncode(endOfStream: Boolean) {
        if (state != State.RECORDING)
            return
        drain()

        val bufIndex = encoder.dequeueInputBuffer(TIMEOUT_US)
        if(bufIndex < 0)
            return

        val curBuf = encoder.getInputBuffer(bufIndex)

        curBuf.clear()
        val readLen = audioRecorder.read(curBuf, SAMPLES_PER_FRAME*2)
        if (readLen > 0) {
            val currentMil = System.currentTimeMillis()
            audioDurationMill = currentMil - beginMill

            val flag = if(endOfStream) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
            encoder.queueInputBuffer(bufIndex, 0, readLen, audioDurationMill * 1000, flag)

            drain()
        }

    }

    private var requestCancel = false
    fun cancel() {
        requestCancel = true
    }


    override fun run() {
        requestCancel = false

        while(!requestCancel) {
            readAndEncode(false)
        }
    }



    fun prepare() {
        if(state == State.INITIALIZING) {
            if (audioRecorder.state != AudioRecord.STATE_INITIALIZED)
                throw RuntimeException("AudioRecord initialization failed")
            encoder.start()

            state = State.READY
        }
    }

    fun lastBlockEndMil(): Long {
        return beginMill+audioDurationMill
    }


}