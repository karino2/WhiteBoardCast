// Copyright 2012 Google Inc. All Rights Reserved.
// Author: frkoenig@google.com (Fritz Koenig)
package com.google.utils;

import java.io.File;

import com.google.libvpx.LibVpxException;
import com.google.libvpx.Rational;
import com.google.libvpx.VpxCodecCxPkt;

/**
 *
 */
public class WebmWriter {
  static {
    System.loadLibrary("vpxJNI");
  }
  private long webmCfgObj;
  private Rational timeBase;

  private native long vpxCodecWebmAllocCfg();
  private native void vpxCodecWebmFreeCfg(long glob);

  private native boolean vpxCodecWebmOpenFile(long glob, String fileName);
  private native void vpxCodecWebmWriteWebmFileHeader(long glob,
                                                      int width, int height, Rational fps);
  private native void vpxCodecWebmWriteWebmBlock(long glob, VpxCodecCxPkt encPkt, long pts);

  private native void vpxCodecWebmWriteWebmFileFooter(long glob, long hash);

  public WebmWriter(File file,
                    int width, int height,
                    Rational timeBase, Rational frameRate) throws LibVpxException {
    webmCfgObj = vpxCodecWebmAllocCfg();

    if (webmCfgObj == 0) {
      throw new LibVpxException("Can not allocate JNI webm object");
    }

    if (vpxCodecWebmOpenFile(webmCfgObj, file.toString())) {
      throw new LibVpxException("Can not open " + file + " for writing");
    }

    this.timeBase = timeBase;
    vpxCodecWebmWriteWebmFileHeader(webmCfgObj, width, height, frameRate);
  }

  public void close() {
    vpxCodecWebmWriteWebmFileFooter(webmCfgObj, 0);
    vpxCodecWebmFreeCfg(webmCfgObj);
  }

  public void writeFrame(VpxCodecCxPkt encPkt) {
    long ptsMs = timeBase.multiply((int) (encPkt.pts * 1000)).toLong();
    vpxCodecWebmWriteWebmBlock(webmCfgObj, encPkt, ptsMs);
  }
}
