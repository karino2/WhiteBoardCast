package com.livejournal.karino2.whiteboardcast

import android.graphics.Rect
import android.media.*
import android.media.MediaCodecInfo
import android.util.Log


class AvcVideoEncoder(val wholeWidth: Int, val wholeHeight: Int, val frameRate:Int, val scale:Int, val muxer: AudioVideoMuxer) : VideoEncoder {

    // var frameRate = 30
    val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
    val bitRate = 700000

    var encoder: MediaCodec
    val bufSize: Int
         get() = (wholeWidth*wholeHeight*3)/2 // 1+1/2

    var trackIndex = 0

    val TIMEOUT_USEC = 10000L
    var framesIndex = 1

    var colorFormat =  MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
    // val colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible

    val supportedColorFormat = arrayOf(
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar
    )

    val bufInfo = MediaCodec.BufferInfo()

    val argbToYuvConverter: ArgbToYuvConverter

    // default value.  scale = 1000, frameRate = 30
    init {
        val format = MediaFormat.createVideoFormat(mimeType, wholeWidth, wholeHeight)
        val halfWidth = wholeWidth/2
        framesIndex = 1

        val mcl = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val codec = mcl.findEncoderForFormat(format)

        encoder = MediaCodec.createByCodecName(codec)
        colorFormat = selectColor(encoder.codecInfo, mimeType)
        Log.d("WhiteBoardCast", "color format ${colorFormat}, w=${wholeWidth}, h=${wholeHeight}")
        argbToYuvConverter = ArgbToYuvConverter(wholeWidth, wholeHeight, isSemiPlanarYUV(colorFormat))

        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15) // 15 fps
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10) // 10 sec between I-frame
        trackIndex = 0

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()
    }

    @Synchronized
    override fun doneEncoder(error: java.lang.StringBuilder?): Boolean {
        writeEndOfStream()
        drain()
        encoder.stop()
        finalizeEncoder()
        return true
    }

    @Synchronized
    override fun finalizeEncoder() {
        encoder.release()
    }




    fun selectColor(codecInfo: MediaCodecInfo, mimeType: String) : Int {
        val caps = codecInfo.getCapabilitiesForType(mimeType)
        caps.colorFormats.forEach {
            if(it in supportedColorFormat) {
                return it
            }
        }
        throw IllegalArgumentException("No supported format. It's cts violation!")
    }

    fun isSemiPlanarYUV(colorFormat: Int) : Boolean {
        return when (colorFormat) {
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar -> false
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar, MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar -> true
            else -> throw RuntimeException("unknown format $colorFormat")
        }
    }




    @Synchronized
    override fun encodeFrames(srcFrame: IntArray, invalRect: Rect, framesToEncode: Int, error: StringBuilder): Boolean {
        if (!encodeOneFrame(srcFrame, invalRect, framesToEncode, error))
            return false
        drain()
        framesIndex = framesToEncode
        return true
    }

    var requestStart = false


    // almost the same as Mp4aRecorder::drain.
    fun drain() {
        if(requestStart && !muxer.isReady)
            return

        val bufIndex = encoder.dequeueOutputBuffer(bufInfo, TIMEOUT_USEC)
        if(bufIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
            trackIndex = muxer.addTrack(encoder.outputFormat)
            muxer.requestStart()
            return
        }


        if(bufIndex < 0) {
            Log.d("WhiteBoardCast", "fail to dequeue output buffer of video encoder.")
            return
        }
        val buf = encoder.getOutputBuffer(bufIndex)
        buf.position(bufInfo.offset)
        buf.limit(bufInfo.offset+bufInfo.size)
        muxer.writeSampleData(trackIndex, buf, bufInfo)

        encoder.releaseOutputBuffer(bufIndex, false)
    }

    fun frameToTime(frame: Int) : Long {
        return 132+frame*1000L*scale/frameRate
    }


    val tempBegin = System.currentTimeMillis()

    private fun writeEndOfStream() {
        val inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
        if(inputBufIndex < 0) {
            return
        }

        val inputBuf = encoder.getInputBuffer(inputBufIndex)
        val ptsUsec = (System.currentTimeMillis()-tempBegin)*1000

        encoder.queueInputBuffer(inputBufIndex, 0, 0, ptsUsec, MediaCodec.BUFFER_FLAG_END_OF_STREAM)

    }

    private fun encodeOneFrame(srcFrame: IntArray, invalRect: Rect, endFrame: Int,
                               error: StringBuilder): Boolean {
        if(invalRect.isEmpty())
            return true


        val inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
        if(inputBufIndex < 0) {
            return false
        }

        val inputBuf = encoder.getInputBuffer(inputBufIndex)

        Log.d("WhiteBoardCast", "yuv convert begin.")

        argbToYuvConverter.toYUV(srcFrame, invalRect)
        Log.d("WhiteBoardCast", "yuv convert end, " + argbToYuvConverter.yuvBuf[1280*30+30])
        inputBuf.clear()
        inputBuf.put(argbToYuvConverter.yuvBuf)

        // start: framesIndex, end: endFrame.
//        val ptsUsec = frameToTime(framesIndex)

        val ptsUsec = (System.currentTimeMillis()-tempBegin)*1000

        encoder.queueInputBuffer(inputBufIndex, 0, bufSize, ptsUsec, 0)

        return true
    }


}