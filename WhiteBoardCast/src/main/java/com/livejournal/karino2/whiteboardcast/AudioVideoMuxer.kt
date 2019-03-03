package com.livejournal.karino2.whiteboardcast

import android.media.MediaCodec
import android.media.MediaFormat
import android.media.MediaMuxer
import java.nio.ByteBuffer


/*
Muxer should start() after addTrack is called.
But addTrack should call when encoder dequeueOutputBuffer is called.
So it's a little tricky to sync when we should call start().

I write wrapper class which count request start and call start appropriately.
 */
class AudioVideoMuxer(val muxer: MediaMuxer) {
    private var requestStartCount = 0

    fun addTrack(format: MediaFormat) = muxer.addTrack(format)
    fun stop() = muxer.stop()
    fun release() = muxer.release()

    var onceReady = false

    val isReady: Boolean
    get() {
        if(onceReady) return true

        synchronized(muxer) {
            onceReady = requestStartCount == 2
            return onceReady
        }
    }

    fun writeSampleData(trackIndex: Int, byteBuffer: ByteBuffer, bufInfo: MediaCodec.BufferInfo) {
        synchronized(muxer) {
            muxer.writeSampleData(trackIndex, byteBuffer, bufInfo)
        }
    }


    fun requestStart() {
        synchronized(muxer) {
            requestStartCount++
            if(requestStartCount == 2) {
                muxer.start()
            }
        }
    }

}