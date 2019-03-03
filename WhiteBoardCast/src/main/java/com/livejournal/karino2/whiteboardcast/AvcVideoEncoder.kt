package com.livejournal.karino2.whiteboardcast

import android.graphics.Color
import android.graphics.Rect
import android.media.*
import android.media.MediaCodecInfo
import android.util.Log
import java.nio.ByteBuffer


class AvcVideoEncoder(val wholeWidth: Int, val wholeHeight: Int, val frameRate:Int, val scale:Int, val muxer: AudioVideoMuxer) : VideoEncoder {
    companion object {
        const val CSHIFT = 16
        const val CYR = 19595
        const val CYG = 38470
        const val CYB = 7471
        const val CUR = -11059
        const val CUG = -21709
        const val CUB = 32768
        const val CVR = 32768
        const val CVG = -27439
        const val CVB = -5329

    }

    // var frameRate = 30
    val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
    val bitRate = 700000

    lateinit var encoder: MediaCodec
    val bufSize: Int
        get() = wholeWidth*wholeHeight*(1+1/2)

    var trackIndex = 0
    /*
    val ybuf: ByteArray
    val uvbuf: ByteArray
    */
    val yuvBuf: ByteArray

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

    val yuvTempBuf = ByteArray(3)
    val bufInfo = MediaCodec.BufferInfo()

    // default value.  scale = 1000, frameRate = 30
    init {
        val format = MediaFormat.createVideoFormat(mimeType, wholeWidth, wholeHeight)
        val halfWidth = wholeWidth/2
        /*
        ybuf = ByteArray(wholeWidth*wholeHeight)
        uvbuf = ByteArray(halfWidth*wholeHeight/2*2)
        */
        yuvBuf = ByteArray(wholeWidth*wholeHeight + 2*halfWidth*wholeHeight/2)
        framesIndex = 1

        val mcl = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val codec = mcl.findEncoderForFormat(format)

        encoder = MediaCodec.createByCodecName(codec)
        colorFormat = selectColor(encoder.codecInfo, mimeType)

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
        encoder.stop()
        finalizeEncoder()
        return true
    }

    @Synchronized
    override fun finalizeEncoder() {
        encoder.release()
    }



    fun argbBufToYUVImage(srcFrame: IntArray, invalRect: Rect, wholeW: Int, wholeH: Int, colorFormat: Int) {
        fillY(srcFrame, invalRect, wholeW, wholeH)

        var x = invalRect.left
        val w = invalRect.width()
        val top = invalRect.top

        val isSemiPlanar = isSemiPlanarYUV(colorFormat)

        for(halfRow in 0 until invalRect.height()/2){
            val row = halfRow*2
            val y = top+row
            val srcStart = row*w
            val semiDestStart = x+y*wholeW/2
            val planarDestStart = x/2+y/2*wholeW/2

            val yEnd = wholeWidth*wholeHeight

            for(xi in 0 until w/2) {
                oneArgbToYuv(srcFrame[srcStart+xi*2], yuvTempBuf)
                if(isSemiPlanar) {
                    // full-size Y, UV pairs at half resolution
                    yuvBuf[yEnd+semiDestStart+xi] = yuvTempBuf[1]
                    yuvBuf[yEnd+semiDestStart+xi+1] = yuvTempBuf[2]
                } else {
                    // full-size Y, quarter U, quarter V. Not good for cache.
                    yuvBuf[yEnd+planarDestStart+xi] = yuvTempBuf[1]
                    yuvBuf[yEnd+planarDestStart+wholeW/2*wholeH/2+xi] = yuvTempBuf[2]
                }
           }
            /*
            if(isSemiPlanar) {
                outBuf.position(wholeW*wholeH+semiDestStart)
                outBuf.put(uvbuf, semiDestStart, w)
            } else {
                outBuf.position(wholeW*wholeH+planarDestStart)
                outBuf.put(uvbuf, planarDestStart, w/2)
                outBuf.position(wholeW*wholeH+wholeW/2*wholeH/2+planarDestStart)
                outBuf.put(uvbuf, wholeW/2*wholeH/2+planarDestStart, w/2)
            }
            */
        }


    }


    private inline fun oneArgbToYuv(argb: Int, yuv: ByteArray) {
        val r = Color.red(argb)
        val g = Color.green(argb)
        val b = Color.blue(argb)
        yuv[0] = (CYR * r + CYG * g + CYB * b shr CSHIFT).toByte()
        yuv[1] = ((CUR * r + CUG * g + CUB * b shr CSHIFT) + 128).toByte()
        yuv[2] = ((CVR * r + CVG * g + CVB * b shr CSHIFT) + 128).toByte()
    }


    private inline fun fillY(srcFrame: IntArray, invalRect: Rect, wholeW: Int, wholeH: Int) {
        var x = invalRect.left

        val w = invalRect.width()

        val tmpBuf = yuvTempBuf
        val yuv = yuvBuf

        for(row in 0 until invalRect.height()){
            val y = invalRect.top+row
            val srcStart = row*w
            val destStart = x+y*wholeW
            for(xi in 0 until w) {
                oneArgbToYuv(srcFrame[srcStart+xi], tmpBuf)
                yuv[destStart+xi] = tmpBuf[0]
            }
        }



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




    private fun encodeOneFrame(srcFrame: IntArray, invalRect: Rect, endFrame: Int,
                               error: StringBuilder): Boolean {
        val inputBufIndex = encoder.dequeueInputBuffer(TIMEOUT_USEC)
        if(inputBufIndex < 0) {
            return false
        }

        val inputBuf = encoder.getInputBuffer(inputBufIndex)
        argbBufToYUVImage(srcFrame, invalRect, wholeWidth, wholeHeight, colorFormat)
        inputBuf.clear()
        inputBuf.put(yuvBuf)

        // start: framesIndex, end: endFrame.
        val ptsUsec = frameToTime(framesIndex)

        encoder.queueInputBuffer(inputBufIndex, 0, bufSize, ptsUsec, 0)

        return true
    }


}