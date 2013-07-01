package com.livejournal.karino2.whiteboardcast;

import android.util.Log;

import com.google.libvorbis.VorbisBlock;
import com.google.libvorbis.VorbisFile;

import java.io.IOException;
import java.util.Arrays;

/**
 * Created by karino on 6/30/13.
 */
public class VorbisReader {
    byte[] buffer;
    public VorbisReader() {
        buffer = new byte[4096];
    }

    public void open(String path) throws IOException {
        int ret = VorbisFile.Open(path);

        if(ret != 0) {
            Log.d("WBCast", "open ret=" + ret);
            throw new IOException(path);
        }
    }
    public void close() {
        VorbisFile.Clear();
    }

    public byte[] readSamples() {
        long size = VorbisFile.Read(buffer);
        if(size <= 0)
            return null; // means EOF
        // currently, I'll dup for safety. I'll change here after stabilize other part.
        return Arrays.copyOf(buffer, (int)size);
    }

}
