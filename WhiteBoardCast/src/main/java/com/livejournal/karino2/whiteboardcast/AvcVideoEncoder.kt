package com.livejournal.karino2.whiteboardcast

import android.graphics.Color
import android.graphics.Rect
import android.media.*
import android.media.MediaCodecInfo
import android.util.Log
import java.nio.ByteBuffer


class AvcVideoEncoder : VideoEncoder {
    @Synchronized
    override fun doneEncoder(error: java.lang.StringBuilder?): Boolean {
        encoder.stop()
        return true
    }

    @Synchronized
    override fun finalizeEncoder() {
        encoder.release()
    }

    var colorFormat =  MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar
    // val colorFormat = MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible



    fun argbBufToYUVImage(srcFrame: IntArray, invalRect: Rect, wholeW: Int, wholeH: Int, colorFormat: Int, outBuf: ByteBuffer) {
        fillY(srcFrame, invalRect, wholeW, wholeH, outBuf)

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

            for(xi in 0 until w/2) {
                oneArgbToYuv(srcFrame[srcStart+xi*2], yuvTempBuf)
                if(isSemiPlanar) {
                    // full-size Y, UV pairs at half resolution
                    uvbuf[semiDestStart+xi] = yuvTempBuf[1]
                    uvbuf[semiDestStart+xi+1] = yuvTempBuf[2]
                } else {
                    // full-size Y, quarter U, quarter V. Not good for cache.
                    uvbuf[planarDestStart+xi] = yuvTempBuf[1]
                    uvbuf[planarDestStart+wholeW/2*wholeH/2+xi] = yuvTempBuf[2]
                }
           }
            if(isSemiPlanar) {
                outBuf.position(wholeW*wholeH+semiDestStart)
                outBuf.put(uvbuf, semiDestStart, w)
            } else {
                outBuf.position(wholeW*wholeH+planarDestStart)
                outBuf.put(uvbuf, planarDestStart, w/2)
                outBuf.position(wholeW*wholeH+wholeW/2*wholeH/2+planarDestStart)
                outBuf.put(uvbuf, wholeW/2*wholeH/2+planarDestStart, w/2)
            }
        }

    }

    private val CSHIFT = 16
    private val CYR = 19595
    private val CYG = 38470
    private val CYB = 7471
    private val CUR = -11059
    private val CUG = -21709
    private val CUB = 32768
    private val CVR = 32768
    private val CVG = -27439
    private val CVB = -5329

    private fun oneArgbToYuv(argb: Int, yuv: ByteArray) {
        val r = Color.red(argb)
        val g = Color.green(argb)
        val b = Color.blue(argb)
        yuv[0] = (CYR * r + CYG * g + CYB * b shr CSHIFT) as Byte
        yuv[1] = ((CUR * r + CUG * g + CUB * b shr CSHIFT) + 128) as Byte
        yuv[2] = ((CVR * r + CVG * g + CVB * b shr CSHIFT) + 128) as Byte
    }

    val yuvTempBuf = ByteArray(3)

    private fun fillY(srcFrame: IntArray, invalRect: Rect, wholeW: Int, wholeH: Int, outBuf: ByteBuffer) {
        var x = invalRect.left

        val w = invalRect.width()
        for(row in 0 until invalRect.height()){
            val y = invalRect.top+row
            val srcStart = row*w
            val destStart = x+y*wholeW
            for(xi in 0 until w) {
                oneArgbToYuv(srcFrame[srcStart+xi], yuvTempBuf)
                ybuf[destStart+xi] = yuvTempBuf[0]
            }
            outBuf.position(destStart)
            outBuf.put(ybuf, destStart, w)
        }



    }


    val supportedColorFormat = arrayOf(
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Planar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420SemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420PackedSemiPlanar,
            MediaCodecInfo.CodecCapabilities.COLOR_TI_FormatYUV420PackedSemiPlanar
        )


    val mimeType = MediaFormat.MIMETYPE_VIDEO_AVC
    var frameRate = 30
    val bitRate = 700000

    lateinit var muxer : MediaMuxer
    lateinit var codec: MediaCodec
    lateinit var ybuf: ByteArray
    lateinit var uvbuf: ByteArray
    var wholeWidth = 0
    var wholeHeight = 0

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

    lateinit var encoder: MediaCodec
    val bufSize: Int
    get() = wholeWidth*wholeHeight*(1+1/2)

    var scale = 1000
    var trackIndex = 0

    // default value.  scale = 1000, frameRate = 30
    override fun initEncoder(fileOutputName: String, width:Int, height:Int, inRate:Int, inScale:Int, error: StringBuilder): Boolean
    {
        wholeWidth = width
        wholeHeight = height

        scale = inScale
        frameRate = inRate

        val format = MediaFormat.createVideoFormat(mimeType, width, height)
        muxer = MediaMuxer(fileOutputName, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        ybuf = ByteArray(width)
        val halfWidth = width/2
        uvbuf = ByteArray(halfWidth*height/2*2)
        framesIndex = 1

        val mcl = MediaCodecList(MediaCodecList.REGULAR_CODECS)
        val codec = mcl.findEncoderForFormat(format)

        encoder = MediaCodec.createEncoderByType(codec)
        colorFormat = selectColor(encoder.codecInfo, mimeType)

        format.setInteger(MediaFormat.KEY_COLOR_FORMAT, colorFormat)
        format.setInteger(MediaFormat.KEY_BIT_RATE, 6000000)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, 15) // 15 fps
        format.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 10) // 10 sec between I-frame
        trackIndex = muxer.addTrack(format)

        encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        encoder.start()

        return true
    }

    @Synchronized
    override fun encodeFrames(srcFrame: IntArray, invalRect: Rect, framesToEncode: Int, error: StringBuilder): Boolean {
        if (!encodeOneFrame(srcFrame, invalRect, framesToEncode, error))
            return false
        drain()
        framesIndex = framesToEncode
        return true
    }

    val bufInfo = MediaCodec.BufferInfo()

    // almost the same as MpfaRecorder::drain.
    fun drain() {
        val bufIndex = encoder.dequeueOutputBuffer(bufInfo, TIMEOUT_USEC)
        if(bufIndex < 0) {
            Log.d("WhiteBoardCast", "fail to dequeue output buffer of video encoder.")
            return
        }
        val buf = encoder.getOutputBuffer(bufIndex)
        buf.position(bufInfo.offset)
        buf.limit(bufInfo.offset+bufInfo.size)

        synchronized(muxer){
            muxer.writeSampleData(trackIndex, buf, bufInfo)
        }
        encoder.releaseOutputBuffer(bufIndex, false)
    }

    val TIMEOUT_USEC = 10000L
    var framesIndex = 1
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
        argbBufToYUVImage(srcFrame, invalRect, wholeWidth, wholeHeight, colorFormat, inputBuf)

        // start: framesIndex, end: endFrame.
        val ptsUsec = frameToTime(framesIndex)

        encoder.queueInputBuffer(inputBufIndex, 0, bufSize, ptsUsec, 0)

        return true
    }


}