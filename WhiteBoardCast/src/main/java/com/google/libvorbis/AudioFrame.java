package com.google.libvorbis;

public class AudioFrame {
  public byte[] buffer;       // compressed data buffer
  public long   size;         // length of compressed data
  public long   pts;          // time stamp to show frame (in timebase units)

  public AudioFrame(long size) {
    this.size = size;
    buffer = new byte[(int) size];
  }
}