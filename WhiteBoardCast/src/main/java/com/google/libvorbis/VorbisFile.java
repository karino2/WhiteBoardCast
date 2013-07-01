package com.google.libvorbis;

/**
 * Created by karino on 7/1/13.
 */
public class VorbisFile {
    static {
        System.loadLibrary("vorbisJNI");
    }

    public static native int Open(String path);
    public static native long Read(byte[] outPcmArray);
    public static native void Clear();
}
