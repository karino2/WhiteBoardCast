// Copyright 2012 Google Inc. All Rights Reserved.
// Author: frkoenig@google.com (Fritz Koenig)
package com.google.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;

import com.google.libvpx.Rational;

/**
 * Return raw bitstream from IVF file.
 *
 * Pts is thrown away currently.
 */
public class IVFReader {
  private FileInputStream is;
  private int fourcc;
  private int width;
  private int height;
  private Rational fps;
  private int frameCount;
  private int currentFrame;
  private static byte[] cDKIF = {'D', 'K', 'I', 'F'};

  public IVFReader(File file) throws IOException {
    is = new FileInputStream(file);
    readHeader();
  }

  public byte[] readNextFrame() {
    try {
      byte[] headerBuffer = readBuffer(12);
      if (headerBuffer == null) {
        return null;
      }
      ByteBuffer buf = ByteBuffer.wrap(headerBuffer);
      buf.order(ByteOrder.LITTLE_ENDIAN);

      int frameLength = buf.getInt();
      long pts = buf.getLong(4);

      currentFrame++;

      return readBuffer(frameLength);
    } catch (IOException e) {
      return null;
    }
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
  }

  public Rational getFps() {
    return fps;
  }

  private byte[] readHeader() throws IOException {
    byte[] headerBuffer = readBuffer(32);

    ByteBuffer buf = ByteBuffer.wrap(headerBuffer);
    buf.order(ByteOrder.LITTLE_ENDIAN);

    byte[] checkHeader = new byte[cDKIF.length];
    System.arraycopy(headerBuffer, 0, checkHeader, 0, cDKIF.length);
    if (!Arrays.equals(checkHeader, cDKIF)) {
      throw new IOException("Incomplete magic for IVF file.");
    }

    int version = buf.getShort(4);
    if (version != 0) {
      throw new IOException("Unrecognized IVF version! This file may not" +
                            " decode properly.");
    }

    fourcc = buf.getInt(8);
    width = buf.getShort(12);
    height = buf.getShort(14);
    fps = new Rational(buf.getInt(16), buf.getInt(20));
    frameCount = buf.getInt(24);

    return headerBuffer;
  }

  private byte[] readBuffer(int size) throws IOException {
    byte[] bytes = new byte[size];

    // Read in the bytes
    int offset = 0;
    int numRead = 0;
    while (offset < bytes.length
            && (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
      offset += numRead;
    }

    if (numRead != size) {
      return null;
    }

    return bytes;
  }
}
