// Copyright 2012 Google Inc. All Rights Reserved.
// Author: frkoenig@google.com (Fritz Koenig)
package com.google.utils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import com.google.libvpx.Rational;

/**
 * Write uncompressed data to y4m file.
 */
public class Y4MWriter extends Y4MFormat {
  private FileOutputStream os;

  public Y4MWriter(File file, int width, int height, Rational fps) throws IOException {
    createHeader(width, height, fps);
    os = new FileOutputStream(file);
    os.write(header);
  }

  private void createHeader(int width, int height, Rational fps) throws IOException {
    this.width = width;
    this.height = height;
    this.fps = fps;
    par = new Rational(1, 1);
    chromaType = new ChromaType("420");
    interlace = new Interlaced('p');

    ByteArrayOutputStream headerStream = new ByteArrayOutputStream();
    headerStream.write(YUV4MPEG);
    headerStream.write(FILE_VERSION);
    headerStream.write(' ');

    headerStream.write('W');
    headerStream.write(Integer.toString(width).getBytes("UTF8"));
    headerStream.write(' ');

    headerStream.write('H');
    headerStream.write(Integer.toString(height).getBytes("UTF8"));
    headerStream.write(' ');

    headerStream.write('F');
    headerStream.write(fps.toColonSeparatedString().getBytes("UTF8"));
    headerStream.write(' ');

    headerStream.write('I');
    headerStream.write(interlace.getMode());
    headerStream.write(' ');

    headerStream.write('C');
    headerStream.write(chromaType.toString().getBytes("UTF8"));
    headerStream.write(' ');

    headerStream.write('A');
    headerStream.write(par.toColonSeparatedString().getBytes("UTF8"));
    headerStream.write('\n');

    header = headerStream.toByteArray();
  }

  public void writeFrame(byte[] frame) throws IOException {
    os.write(FRAME_HEADER);
    os.write(frame);
  }

  public void close() {
    try {
      os.flush();
      os.close();
    } catch (IOException e) {
      System.err.println("Error closing " + os + " : " + e);
    }
  }
}
