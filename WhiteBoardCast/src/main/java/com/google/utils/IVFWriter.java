// Copyright 2012 Google Inc. All Rights Reserved.
// Author: frkoenig@google.com (Fritz Koenig)
package com.google.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.google.libvpx.Rational;
import com.google.libvpx.VpxCodecCxPkt;

/**
 * Write VpxCodecCxPkt to file in the ivf format.
 */
public class IVFWriter {
  private FileOutputStream os;
  private File file;
  private int fourcc;
  private int width;
  private int height;
  private long rate;
  private long scale;
  private int frameCount;

  public IVFWriter(File file,
                   int fourcc, int width, int height,
                   Rational timeBase) throws IOException {
    this.file = file;
    os = new FileOutputStream(this.file);
    this.fourcc = fourcc;
    this.width = width;
    this.height = height;
    this.rate = timeBase.den();
    this.scale = timeBase.num();

    byte[] fileHeader = ivfFileHeader(0);
    os.write(fileHeader);
  }

  public void writeFrame(VpxCodecCxPkt encPkt, long timeStamp) throws IOException {
    byte[] frameHeader = ivfFrameHeader(encPkt.sz, timeStamp);
    os.write(frameHeader);
    os.write(encPkt.buffer);
    frameCount++;
  }

  public void close() {
    try {
      os.flush();
      os.close();
      RandomAccessFile ivfFile = new RandomAccessFile(file, "rw");
      ivfFile.seek(0);
      byte[] fileHeader = ivfFileHeader(frameCount);
      ivfFile.write(fileHeader);
      ivfFile.close();
    } catch (IOException e) {
      System.err.println("Error closing " + file + " : " + e);
    }
  }

  private byte[] ivfFileHeader(int frameCount) {
    byte[] header = new byte[32];

    header[0]  = 'D';
    header[1]  = 'K';
    header[2]  = 'I';
    header[3]  = 'F';
    header[4]  = 0;           // version
    header[5]  = 0;
    header[6]  = 32;          // headersize
    header[7]  = 0;
    header[8]  = (byte) (fourcc);
    header[9]  = (byte) (fourcc >> 8);
    header[10] = (byte) (fourcc >> 16);
    header[11] = (byte) (fourcc >> 24);
    header[12] = (byte) (width);
    header[13] = (byte) (width >> 8);
    header[14] = (byte) (height);
    header[15] = (byte) (height >> 8);
    header[16] = (byte) (rate);
    header[17] = (byte) (rate >> 8);
    header[18] = (byte) (rate >> 16);
    header[19] = (byte) (rate >> 24);
    header[20] = (byte) (scale);
    header[21] = (byte) (scale >> 8);
    header[22] = (byte) (scale >> 16);
    header[23] = (byte) (scale >> 24);
    header[24] = (byte) (frameCount);
    header[25] = (byte) (frameCount >> 8);
    header[26] = (byte) (frameCount >> 16);
    header[27] = (byte) (frameCount >> 24);
    header[28] = 0;
    header[29] = 0;
    header[30] = 0;
    header[31] = 0;

    return header;
  }

  private byte[] ivfFrameHeader(long size, long pts) {
    byte[] header = new byte[12];

    header[0]  = (byte) (size);
    header[1]  = (byte) (size >> 8);
    header[2]  = (byte) (size >> 16);
    header[3]  = (byte) (size >> 24);
    header[4]  = (byte) (pts);
    header[5]  = (byte) (pts >> 8);
    header[6]  = (byte) (pts >> 16);
    header[7]  = (byte) (pts >> 24);
    header[8]  = (byte) (pts >> 32);
    header[9]  = (byte) (pts >> 40);
    header[10] = (byte) (pts >> 48);
    header[11] = (byte) (pts >> 56);

    return header;
  }
}
