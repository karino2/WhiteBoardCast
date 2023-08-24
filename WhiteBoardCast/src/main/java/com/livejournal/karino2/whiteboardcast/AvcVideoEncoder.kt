package com.livejournal.karino2.whiteboardcast

import android.graphics.Rect
import android.media.*
import android.media.MediaCodecInfo


class AvcVideoEncoder(val wholeWidth: Int, val wholeHeight: Int, val muxer: AudioVideoMuxer, val colorFormat: Int, val encoder: MediaCodec) {

    companion object {
        val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
        val supportedColorFormat = arrayOf(
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar,
                MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar
        )
        var colorFormat =  MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
        // val colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible
        var format: MediaFormat? = null
        var codecName: String = ""

        fun selectColor(codecInfo: MediaCodecInfo, mimeType: String) : Int {
            val caps = codecInfo.getCapabilitiesForType(mimeType)
            caps.colorFormats.forEach {
                if(it in supportedColorFormat) {
                    return it
                }
            }
            throw IllegalArgumentException("No supported format. It's cts violation!")
        }

        fun createInstance(wholeWidth: Int, wholeHeight: Int, muxer: AudioVideoMuxer) : AvcVideoEncoder {
            val (encoder, colorFormat) = createEncoder(wholeWidth, wholeHeight)
            return AvcVideoEncoder(wholeWidth, wholeHeight, muxer, colorFormat, encoder)
        }

        // Enumerate codec takes some times. We need to cache result.
        // I decide to cache in companion object.
        private fun createEncoder(screenWidth: Int, screenHeight: Int) : Pair<MediaCodec, Int> {
            if(format == null) {
                format = MediaFormat.createVideoFormat(mimeType, screenWidth, screenHeight)

                val mcl = MediaCodecList(MediaCodecList.REGULAR_CODECS)
                codecName = mcl.findEncoderForFormat(format)

                val encoder = MediaCodec.createByCodecName(codecName)
                colorFormat = selectColor(encoder.codecInfo, mimeType)

                format!!.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
                format!!.setInteger(MediaFormat.KEY_BIT_RATE, 6000000)
                format!!.setInteger(MediaFormat.KEY_FRAME_RATE, 15) // 15 fps
                format!!.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10) // 10 sec between I-frame
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                return Pair(encoder, colorFormat)
            } else {
                val encoder = MediaCodec.createByCodecName(codecName)
                encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
                return Pair(encoder, colorFormat)
            }



        }
    }

    val bufSize: Int
         get() = (wholeWidth*wholeHeight*3)/2 // 1+1/2

    var trackIndex = 0

    val TIMEOUT_USEC = 10000L



    val bufInfo = MediaCodec.BufferInfo()

    val argbToYuvConverter: ArgbToYuvConverter

    init {
        // Log.d("WhiteBoardCast", "color format ${colorFormat}, w=${wholeWidth}, h=${wholeHeight}")
        argbToYuvConverter = ArgbToYuvConverter(wholeWidth, wholeHeight, isSemiPlanarYUV(colorFormat))
        trackIndex = 0
    }




    fun start() {
        encoder.start()
    }

    @Synchronized
    fun doneEncoder(error: java.lang.StringBuilder?): Boolean {
        writeEndOfStream()
        drain(100000)
        encoder.stop()
        finalizeEncoder()
        return true
    }

    @Synchronized
    fun finalizeEncoder() {
        encoder.release()
    }





    fun isSemiPlanarYUV(colorFormat: Int) : Boolean {
        return when (colorFormat) {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> false
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar, MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar -> true
            else -> throw RuntimeException("unknown format $colorFormat")
        }
    }

    @Synchronized
    fun encodeFrames(srcFrame: IntArray, invalRect: Rect, error: StringBuilder): Boolean {
        if (!encodeOneFrame(srcFrame, invalRect, error))
            return false
        drain()
        return true
    }

    var requestStart = false

    // almost the same as Mp4aRecorder::drain.
    fun drain(timeoutUs: Long = 0L) {
        if(requestStart && !muxer.isReady)
            return

        val bufIndex = encoder.dequeueOutputBuffer(bufInfo, timeoutUs)
        if(bufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            trackIndex = muxer.addTrack(encoder.outputFormat)
            muxer.requestStart()
            return
        }


        if(bufIndex < 0) {
            // This is not a problem if there is not enough data.
            // Log.d("WhiteBoardCast", "fail to dequeue output buffer of video encoder.")
            return
        }

        // Pixel 3 crash for EndOfStream origin writeSampleData.
        // I don't know whether this last frame is necessary or not.
        // But the result seems no problem.
        try {
            val buf = encoder.getOutputBuffer(bufIndex)!!
            buf.position(bufInfo.offset)
            buf.limit(bufInfo.offset + bufInfo.size)
            muxer.writeSampleData(trackIndex, buf, bufInfo)
        }catch(e : IllegalArgumentException) {

        }

        encoder.releaseOutputBuffer(bufIndex, false)
    }


    var beginMill = System.currentTimeMillis()

    private fun writeEmptyFrame(flags: Int = 0) {
        val inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
        if(inputBufIndex < 0) {
            return
        }

        val ptsUsec = (System.currentTimeMillis()-beginMill)*1000
        encoder.queueInputBuffer(inputBufIndex, 0, 0, ptsUsec, flags)
    }

    private fun writeEndOfStream() {
        writeEmptyFrame(MediaCodec.BUFFER_FLAG_END_OF_STREAM)
    }

    private fun encodeOneFrame(srcFrame: IntArray, invalRect: Rect,
                               error: StringBuilder): Boolean {
        if(invalRect.isEmpty) {
            writeEmptyFrame()
            return true
        }


        val inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
        if(inputBufIndex < 0) {
            return false
        }

        val inputBuf = encoder.getInputBuffer(inputBufIndex)!!

        argbToYuvConverter.toYUV(srcFrame, invalRect)
        inputBuf.clear()
        inputBuf.put(argbToYuvConverter.yuvBuf)

        val ptsUsec = (System.currentTimeMillis()-beginMill)*1000

        encoder.queueInputBuffer(inputBufIndex, 0, bufSize, ptsUsec, 0)

        return true
    }
}