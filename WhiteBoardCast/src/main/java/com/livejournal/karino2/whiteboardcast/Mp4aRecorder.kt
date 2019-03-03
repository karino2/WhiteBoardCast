package com.livejournal.karino2.whiteboardcast

import android.media.*
import android.util.Log
import java.nio.ByteBuffer
import android.media.MediaCodec


class Mp4aRecorder(val muxer: AudioVideoMuxer, beginMil: Long) {
    private val AUDIO_MIME_TYPE = "audio/mp4a-latm"
    private val SAMPLE_RATE = 44100
    private val SAMPLES_PER_FRAME = 1024
    private val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
    private val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
    var audioDurationMill = 0L

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
            Log.d("WhiteBoardCast", "VorbisRec: resume call but state is not stopped: $state")
        }
    }

    fun start() {
        if (state == State.READY || state == State.STOPPED) {
            state = State.RECORDING
            audioRecorder.startRecording()
            val buf = ensureByteBuffer() ?: return // fatal situation. how to recover?
            buf.clear()
            audioRecorder.read(buf, buf.remaining())
        }
    }

    fun stop() {
        if (state == State.RECORDING) {
            handleNewAudioData(audioRecorder, true)

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
    var trackIndex: Int = 0
    val encoder: MediaCodec

    var framePeriod = 0


    init {
        val min_buffer_size = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        var buffer_size = SAMPLES_PER_FRAME * 10
        if (buffer_size < min_buffer_size) {
            buffer_size = (min_buffer_size / SAMPLES_PER_FRAME + 1) * SAMPLES_PER_FRAME * 2
        }

        val channel = 1
        framePeriod = buffer_size / (2 * SAMPLES_PER_FRAME * channel / 8)

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

        encoder = MediaCodec.createEncoderByType(AUDIO_MIME_TYPE)
        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    }

    val TIMEOUT_USEC = 10000L

    var currentBufIndex = -1
    var currentBuffer : ByteBuffer? = null
    private fun ensureByteBuffer() : ByteBuffer? {
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

    private fun releaseCurrentBuffer() {
        currentBuffer = null
        currentBufIndex = -1
    }

    private fun handleNewAudioData(audioRecord: AudioRecord, endOfStream: Boolean) {
        if (state != State.RECORDING)
            return

        val curBuf = ensureByteBuffer()
                ?: return // no avaiable encode input buffer. just ignore this frame.

        curBuf.clear()
        val readLen = audioRecord.read(curBuf, SAMPLES_PER_FRAME)
        if (readLen > 0) {
            val currentMil = System.currentTimeMillis()
            audioDurationMill = currentMil - beginMill

            val flag = if(endOfStream) MediaCodec.BUFFER_FLAG_END_OF_STREAM else 0
            encoder.queueInputBuffer(currentBufIndex, 0, readLen, audioDurationMill * 1000, flag)
            releaseCurrentBuffer()

            drain()
        }
    }

    val bufInfo = MediaCodec.BufferInfo()
    var requestStart = false

    fun drain() {
        if(requestStart && !muxer.isReady)
            return
        val bufIndex = encoder.dequeueOutputBuffer(bufInfo, TIMEOUT_USEC)
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

    fun prepare() {
        if(state == State.INITIALIZING) {
            if (audioRecorder.state != AudioRecord.STATE_INITIALIZED)
                throw RuntimeException("AudioRecord initialization failed")
            encoder.start()


            audioRecorder.setRecordPositionUpdateListener(object : AudioRecord.OnRecordPositionUpdateListener {

                override fun onMarkerReached(audioRecord: AudioRecord) {
                    // do nothing.
                }

                override fun onPeriodicNotification(audioRecord: AudioRecord) {
                    handleNewAudioData(audioRecord, false)
                }
            })
            audioRecorder.positionNotificationPeriod = framePeriod

            state = State.READY
        }
    }

    fun lastBlockEndMil(): Long {
        return beginMill+audioDurationMill
    }


}