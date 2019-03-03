package com.livejournal.karino2.whiteboardcast

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.support.test.InstrumentationRegistry
import android.support.test.runner.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ArgbToYuvConverterTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
        val appContext = InstrumentationRegistry.getTargetContext()
        assertEquals("com.livejournal.karino2.whiteboardcast", appContext.packageName)
    }

    @Test
    fun convert1280x800() {
        val width = 1280
        val height = 800

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.RED)

        val buf = IntArray(width * height)
        bitmap.getPixels(buf, 0, width, 0, 0, width, height)

        val invalRect = Rect(0, 0, width, height)


        val conv = ArgbToYuvConverter(width, height, true)

        val begin = System.currentTimeMillis()
        conv.toYUV(buf, invalRect)
        val dur = System.currentTimeMillis() - begin

        System.out.println("hoge")

        println("dur: ${dur}msec ${conv.yuvBuf[0]}, ${conv.yuvBuf[1280*800+0]}, ${conv.yuvBuf[1280*800+1]}")
        // assertEquals(0.3, dur)
    }
}
