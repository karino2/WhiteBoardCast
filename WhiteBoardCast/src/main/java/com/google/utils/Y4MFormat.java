// Copyright 2012 Google Inc. All Rights Reserved.
// Author: frkoenig@google.com (Fritz Koenig)
package com.google.utils;

import com.google.libvpx.Rational;


/**
 *
 */
public abstract class Y4MFormat {
  /**
  *
  */
  protected class ChromaType {
    private final String type;

    public ChromaType() {
      this.type = new String("420");
    }

    public ChromaType(String type) {
      this.type = type;
    }

    public boolean is420() {
      return type.equals("420") || type.equals("420jpeg");
    }

    @Override
    public String toString() {
      return type;
    }
  }

  /**
  *
  */
  protected class Interlaced {
    private final char interlaced;

    public Interlaced() {
      interlaced = '?';
    }

    public Interlaced(char interlaced) {
      this.interlaced = interlaced;
    }

    public boolean isProgressive() {
      return (interlaced == '?' || interlaced == 'p');
    }

    public char getMode() {
      return interlaced;
    }

    @Override
    public String toString() {
      if (isProgressive()) {
        return new String("progressive");
      }

      return new String("interlaced");
    }
  }

  protected static final byte[] YUV4MPEG = {'Y', 'U', 'V', '4', 'M', 'P', 'E', 'G'};
  protected static final byte[] FRAME_HEADER = {'F', 'R', 'A', 'M', 'E', '\n'};
  protected static final byte FILE_VERSION = '2';
  protected int width;
  protected int height;
  protected byte[] header;
  protected ChromaType chromaType;
  protected Interlaced interlace;
  protected Rational fps;
  protected Rational par;

  public Rational getPixelAspectRation() {
    return par;
  }

  public int getFrameSize() {
    if (chromaType.is420()) {
      return width * height + 2 * ((width + 1) / 2) * ((height + 1) / 2);
    }

    return 0;
  }
}
