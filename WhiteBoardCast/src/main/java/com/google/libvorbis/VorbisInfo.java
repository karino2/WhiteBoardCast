// Author: mszal@google.com (Michael Szal)

package com.google.libvorbis;

public class VorbisInfo extends Common {

  public VorbisInfo() {
    nativePointer = newVorbisInfo();
  }

  public long getBitrateLower() {
    return getBitrateLower(nativePointer);
  }

  public long getBitrateNominal() {
    return getBitrateNominal(nativePointer);
  }

  public long getBitrateUpper() {
    return getBitrateUpper(nativePointer);
  }

  public long getBitrateWindow() {
    return getBitrateWindow(nativePointer);
  }

  public int getChannels() {
    return getChannels(nativePointer);
  }

  public long getCodecSetup() {
    return getCodecSetup(nativePointer);
  }

  public long getRate() {
    return getRate(nativePointer);
  }

  public int getVersion() {
    return getVersion(nativePointer);
  }

  public void setBitrateLower(long bitrateLower) {
    setBitrateLower(nativePointer, bitrateLower);
  }

  public void setBitrateNominal(long bitrateNominal) {
    setBitrateNominal(nativePointer, bitrateNominal);
  }

  public void setBitrateUpper(long bitrateUpper) {
    setBitrateUpper(nativePointer, bitrateUpper);
  }

  public void setBitrateWindow(long bitrateWindow) {
    setBitrateWindow(nativePointer, bitrateWindow);
  }

  public void setChannels(int channels) {
    setChannels(nativePointer, channels);
  }

  public void setCodecSetup(long codecSetup) {
    setCodecSetup(nativePointer, codecSetup);
  }

  public void setRate(long rate) {
    setRate(nativePointer, rate);
  }

  public void setVersion(int version) {
    setVersion(nativePointer, version);
  }

  protected VorbisInfo(long nativePointer) {
    super(nativePointer);
  }

  @Override
  protected void deleteObject() {
    deleteVorbisInfo(nativePointer);
  }

  private static native void deleteVorbisInfo(long jVorbisInfo);
  private static native long getBitrateLower(long jVorbisInfo);
  private static native long getBitrateNominal(long jVorbisInfo);
  private static native long getBitrateUpper(long jVorbisInfo);
  private static native long getBitrateWindow(long jVorbisInfo);
  private static native int getChannels(long jVorbisInfo);
  private static native long getCodecSetup(long jVorbisInfo);
  private static native long getRate(long jVorbisInfo);
  private static native int getVersion(long jVorbisInfo);
  private static native long newVorbisInfo();
  private static native void setBitrateLower(long jVorbisInfo, long bitrateLower);
  private static native void setBitrateNominal(long jVorbisInfo, long bitrateNominal);
  private static native void setBitrateUpper(long jVorbisInfo, long bitrateUpper);
  private static native void setBitrateWindow(long jVorbisInfo, long bitrateWindow);
  private static native void setChannels(long jVorbisInfo, int channels);
  private static native void setCodecSetup(long jVorbisInfo, long codecSetup);
  private static native void setRate(long jVorbisInfo, long rate);
  private static native void setVersion(long jVorbisInfo, int version);
}
