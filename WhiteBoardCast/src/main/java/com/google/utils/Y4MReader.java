// Copyright 2012 Google Inc. All Rights Reserved.
// Author: frkoenig@google.com (Fritz Koenig)
package com.google.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.StringTokenizer;

import com.google.libvpx.Rational;

/**
 *
 */
public class Y4MReader extends Y4MFormat {
  private FileInputStream is;

  public Y4MReader(File file) throws IOException {
    is = new FileInputStream(file);
    try {
      parseHeader(readHeader());
    } catch (IOException e) {
      throw new IOException(e.getMessage() + " : " + file.getName());
    }
  }

  // TODO(frkoenig) : consider throwing OutOfMemory.
  private byte[] readHeader() throws IOException {
    byte[] headerBuffer = new byte[80];

    // Read in the bytes
    int pos = 0;
    int numRead = 0;
    while (pos < (headerBuffer.length - 1)
            && (numRead = is.read(headerBuffer, pos, 1)) >= 0) {
      if (headerBuffer[pos] == '\n') {
        break;
      }
      pos += numRead;
    }

    headerBuffer[pos] = '\0';

    // Ensure all the bytes have been read in
    if (pos == (headerBuffer.length - 1)) {
      throw new IOException("Error parsing header; not a YUV2MPEG2 file?");
    }

    return headerBuffer;
  }

  public void parseHeader(byte[] header) throws IOException {
    this.header = header;

    byte[] checkHeader = new byte[YUV4MPEG.length];
    System.arraycopy(this.header, 0, checkHeader, 0, YUV4MPEG.length);
    if (!Arrays.equals(checkHeader, YUV4MPEG)) {
      throw new IOException("Incomplete magic for YUV4MPEG file.");
    }

    if (this.header[8] != FILE_VERSION) {
      throw new IOException("Incorrect YUV input file version; YUV4MPEG2 required.");
    }

    chromaType = new ChromaType();
    interlace = new Interlaced('?');
    par = new Rational(1, 1);

    String tags = new String(this.header.clone());
    StringTokenizer st = new StringTokenizer(tags.trim());
    while (st.hasMoreTokens()) {
      String token = st.nextToken();
      String[] parts;

      switch (token.charAt(0)) {
        case 'W':
          width = Integer.parseInt(token.substring(1));
          break;
        case 'H':
          height = Integer.parseInt(token.substring(1));
          break;
        case 'F':
          parts = token.substring(1).split(":");
          fps = new Rational(parts[0], parts[1]);
          break;
        case 'I':
          interlace = new Interlaced(token.charAt(1));
          break;
        case 'A':
          parts = token.substring(1).split(":");
          par = new Rational(parts[0], parts[1]);
          break;
        case 'C':
          chromaType = new ChromaType(token.substring(1));
          break;
        default:
          // Ignore unknown tags
          break;
      }
    }
    // TODO(frkoenig): throw something here is h/w/fps are == 0
  }

  public byte[] getUncompressedFrame() throws IOException {
    byte[] header = readBuffer(6);

    if (header == null) {
      return null;
    }

    if (!Arrays.equals(header, FRAME_HEADER)) {
      throw new IOException("Loss of framing in Y4M input data.");
    }

    final int size = getFrameSize();
    if (size == 0) {
      throw new IOException("Frame size is 0.");
    }

    byte[] frame = readBuffer(size);

    return frame;
  }

  public Rational getFrameRate() {
    return fps;
  }

  public int getWidth() {
    return width;
  }

  public int getHeight() {
    return height;
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
