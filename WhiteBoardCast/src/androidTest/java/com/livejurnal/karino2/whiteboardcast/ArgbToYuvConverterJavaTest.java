package com.livejurnal.karino2.whiteboardcast;

import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class ArgbToYuvConverterJavaTest {
    // To compare result of cts to my kotlin version, I want to check cts result.
    // https://android.googlesource.com/platform/cts/+/3b12c3c/tests/tests/graphics/src/android/graphics/cts/YuvImageTest.java
    private static final int CSHIFT = 16;
    private static final int CYR = 19595;
    private static final int CYG = 38470;
    private static final int CYB = 7471;
    private static final int CUR = -11059;
    private static final int CUG = -21709;
    private static final int CUB = 32768;
    private static final int CVR = 32768;
    private static final int CVG = -27439;
    private static final int CVB = -5329;

    byte[] buf = new byte[3];

    private void argb2yuv(int argb, byte[] yuv) {
        int r = Color.red(argb);
        int g = Color.green(argb);
        int b = Color.blue(argb);
        yuv[0] = (byte) ((CYR * r + CYG * g + CYB * b) >> CSHIFT);
        yuv[1] = (byte) (((CUR * r + CUG * g + CUB * b) >> CSHIFT) + 128);
        yuv[2] = (byte) (((CVR * r + CVG * g + CVB * b) >> CSHIFT) + 128);
    }

    @Test
    public void testArgvToYuv() {
        int color = Color.RED;

        argb2yuv(color, buf);

        System.out.println("hoge java" + buf[0] + ","+buf[1]+ ","+ buf[2]);
    }

}
