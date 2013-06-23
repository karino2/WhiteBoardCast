// Copyright 2013 Google Inc. All Rights Reserved.
package com.google.libvorbis;

/**
 * JNI interface to C struct used for configuring
 * the libvorbis encoder.
 */
public class VorbisEncConfig extends Common {
  public VorbisEncConfig(int channels, int sampleRate, int bitsPerSample) throws VorbisException {
    nativePointer = vorbisEncAllocCfg();
    if (nativePointer == 0) {
      throw new VorbisException("Can not allocate JNI encoder configure object");
    }

    vorbisEncSetChannels(nativePointer, (short)channels);
    vorbisEncSetSampleRate(nativePointer, sampleRate);
    vorbisEncSetBitsPerSample(nativePointer, (short)bitsPerSample);
  }

  public void close() {
    vorbisEncFreeCfg(nativePointer);
  }

  public long handle() {
    return nativePointer;
  }

  public void setFormatTag(int format_tag) {
    vorbisEncSetFormatTag(nativePointer, format_tag);
  }
  public void setChannels(short channels) {
    vorbisEncSetChannels(nativePointer, channels);
  }
  public void setSampleRate(int sample_rate) {
    vorbisEncSetSampleRate(nativePointer, sample_rate);
  }
  public void setBitsPerSample(short bits_per_sample) {
    vorbisEncSetBitsPerSample(nativePointer, bits_per_sample);
  }

  public void setTimebase(long numerator, long denominator) {
    vorbisEncSetTimebase(nativePointer, numerator, denominator);
  }

  public void setAverageBitrate(int average_bitrate) {
    vorbisEncSetAverageBitrate(nativePointer, average_bitrate);
  }
  public void setMinimumBitrate(int minimum_bitrate) {
    vorbisEncSetMinimumBitrate(nativePointer, minimum_bitrate);
  }
  public void setMaximumBitrate(int maximum_bitrate) {
    vorbisEncSetMaximumBitrate(nativePointer, maximum_bitrate);
  }

  public void setBitrateBasedQuality(boolean bitrate_based_quality) {
    vorbisEncSetBitrateBasedQuality(nativePointer, bitrate_based_quality);
  }
  public void setImpulseBlockBias(double impulse_block_bias) {
    vorbisEncSetImpulseBlockBias(nativePointer, impulse_block_bias);
  }
  public void setLowpassFrequency(double lowpass_frequency) {
    vorbisEncSetLowpassFrequency(nativePointer, lowpass_frequency);
  }

  public int getFormatTag() {
    return vorbisEncGetFormatTag(nativePointer);
  }
  public short getChannels() {
    return vorbisEncGetChannels(nativePointer);
  }
  public int getSampleRate() {
    return vorbisEncGetSampleRate(nativePointer);
  }
  public int getBytesPerSecond() {
    return vorbisEncGetBytesPerSecond(nativePointer);
  }
  public short getBlockAlign() {
    return vorbisEncGetBlockAlign(nativePointer);
  }
  public short getBitsPerSample() {
    return vorbisEncGetBitsPerSample(nativePointer);
  }

  public int getAverageBitrate() {
    return vorbisEncGetAverageBitrate(nativePointer);
  }
  public int getMinimumBitrate() {
    return vorbisEncGetMinimumBitrate(nativePointer);
  }
  public int getMaximumBitrate() {
    return vorbisEncGetMaximumBitrate(nativePointer);
  }

  public boolean getBitrateBasedQuality() {
    return vorbisEncGetBitrateBasedQuality(nativePointer);
  }
  public double getImpulseBlockBias() {
    return vorbisEncGetImpulseBlockBias(nativePointer);
  }
  public double getLowpassFrequency() {
    return vorbisEncGetLowpassFrequency(nativePointer);
  }

  @Override
  protected void deleteObject() {
    vorbisEncFreeCfg(nativePointer);
  }

  private native long vorbisEncAllocCfg();
  private native void vorbisEncFreeCfg(long cfg);

  private native void vorbisEncSetFormatTag(long cfg, int format_tag);
  private native void vorbisEncSetChannels(long cfg, short channels);
  private native void vorbisEncSetSampleRate(long cfg, int sample_rate);
  private native void vorbisEncSetBitsPerSample(long cfg, short bits_per_sample);

  private native void vorbisEncSetTimebase(long cfg, long numerator, long denominator);

  private native void vorbisEncSetAverageBitrate(long cfg, int average_bitrate);
  private native void vorbisEncSetMinimumBitrate(long cfg, int minimum_bitrate);
  private native void vorbisEncSetMaximumBitrate(long cfg, int maximum_bitrate);

  private native void vorbisEncSetBitrateBasedQuality(long cfg, boolean bitrate_based_quality);
  private native void vorbisEncSetImpulseBlockBias(long cfg, double impulse_block_bias);
  private native void vorbisEncSetLowpassFrequency(long cfg, double lowpass_frequency);

  private native int vorbisEncGetFormatTag(long cfg);
  private native short vorbisEncGetChannels(long cfg);
  private native int vorbisEncGetSampleRate(long cfg);
  private native int vorbisEncGetBytesPerSecond(long cfg);
  private native short vorbisEncGetBlockAlign(long cfg);
  private native short vorbisEncGetBitsPerSample(long cfg);

  private native int vorbisEncGetAverageBitrate(long cfg);
  private native int vorbisEncGetMinimumBitrate(long cfg);
  private native int vorbisEncGetMaximumBitrate(long cfg);

  private native boolean vorbisEncGetBitrateBasedQuality(long cfg);
  private native double vorbisEncGetImpulseBlockBias(long cfg);
  private native double vorbisEncGetLowpassFrequency(long cfg);
}
