package com.livejournal.karino2.whiteboardcast

import android.graphics.Rect

class ArgbToYuvConverter(val width: Int, val height: Int, val isSemiPlanar: Boolean){
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
    val yuvBuf: ByteArray = ByteArray(width*height + 2*(width/2)*(height/2))

    val yuvTempBuf = ByteArray(3)

    fun toYUV(srcFrame: IntArray, invalRect: Rect) {
        fillY(srcFrame, invalRect)

        val xodd = invalRect.left %2
        val startX = invalRect.left-xodd
        val invalW = Math.min(width, invalRect.width()+xodd)
        val invalH = invalRect.height()
        val startY = invalRect.top
        val yEnd = width*height
        val qsize = width/2*height/2

        for(halfRow in 0 until invalH/2){
            val row = halfRow*2
            val y = startY+row
            val srcStart = y* width +startX
            val semiDestStart = (y/2)* width +startX // (y/2)*(2*width/2)+2*(x/2)
            val planarDestStart = startX/2 +(y/2)* (width /2) // x/2+(y/2)*(width/2)

            for(halfDX in 0 until invalW/2) {
                val dx = halfDX*2

                // oneArgbToYuv(srcFrame[srcStart+dx], yuvTempBuf)
                val argb = srcFrame[srcStart+dx]
                val r = red(argb)
                val g = green(argb)
                val b = blue(argb)
                val u = rgbToU(r, g, b)
                val v = rgbToV(r, g, b)


                if(isSemiPlanar) {
                    // full-size Y, UV pairs at half resolution
                    yuvBuf[yEnd+semiDestStart+dx] = u
                    yuvBuf[yEnd+semiDestStart+dx+1] = v
                } else {
                    // full-size Y, quarter U, quarter V. Not good for cache.
                    yuvBuf[yEnd+planarDestStart+halfDX] = u
                    yuvBuf[yEnd+planarDestStart+qsize+halfDX] = v
                }
            }
        }
    }

    inline fun red(color: Int): Int {
        return color shr 16 and 0xFF
    }

    inline fun green(color: Int): Int {
        return color shr 8 and 0xFF
    }

    inline fun blue(color: Int): Int {
        return color and 0xFF
    }

    private inline fun oneArgbToY(argb: Int) : Byte {
        val r = red(argb)
        val g = green(argb)
        val b = blue(argb)

        return (CYR * r + CYG * g + CYB * b shr CSHIFT).toByte()
    }

    private inline fun rgbToU(r: Int, g: Int, b: Int) : Byte {
        return ((CUR * r + CUG * g + CUB * b shr CSHIFT) + 128).toByte()
    }
    private inline fun rgbToV(r: Int, g: Int, b: Int) : Byte {
        return ((CVR * r + CVG * g + CVB * b shr CSHIFT) + 128).toByte()
    }




    private fun fillY(srcFrame: IntArray, invalRect: Rect) {
        var startX = invalRect.left
        val startY = invalRect.top

        val invalW = invalRect.width()

        for(invalRow in 0 until invalRect.height()){
            val y = startY+invalRow
            val start = y* width +startX
            for(xi in 0 until invalW) {
                yuvBuf[start+xi] = oneArgbToY(srcFrame[start+xi])
            }
        }
   }


}