// Author: mszal@google.com (Michael Szal)

package com.google.libvorbis;

public class VorbisDspState extends Common {

  public VorbisDspState() {
    nativePointer = newVorbisDspState();
  }

  public int getAnalysisp() {
    return getAnalysisp(nativePointer);
  }

  public long getBackendState() {
    return getBackendState(nativePointer);
  }

  public long getCenterW() {
    return getCenterW(nativePointer);
  }

  public int getEofflag() {
    return getEofflag(nativePointer);
  }

  public long getFloorBits() {
    return getFloorBits(nativePointer);
  }

  public long getGlueBits() {
    return getGlueBits(nativePointer);
  }

  public long getGranulepos() {
    return getGranulepos(nativePointer);
  }

  public long getLW() {
    return getLW(nativePointer);
  }

  public long getNW() {
    return getNW(nativePointer);
  }

  public long getPcm() {
    return getPcm(nativePointer);
  }

  public int getPcmCurrent() {
    return getPcmCurrent(nativePointer);
  }

  public long getPcmret() {
    return getPcmret(nativePointer);
  }

  public int getPcmReturned() {
    return getPcmReturned(nativePointer);
  }

  public int getPcmStorage() {
    return getPcmStorage(nativePointer);
  }

  public int getPreextrapolate() {
    return getPreextrapolate(nativePointer);
  }

  public long getResBits() {
    return getResBits(nativePointer);
  }

  public long getSequence() {
    return getSequence(nativePointer);
  }

  public long getTimeBits() {
    return getTimeBits(nativePointer);
  }

  public VorbisInfo getVi() {
    long vi = getVi(nativePointer);
    return new VorbisInfo(vi);
  }

  public long getW() {
    return getW(nativePointer);
  }

  public void setAnalysisp(int analysisp) {
    setAnalysisp(nativePointer, analysisp);
  }

  public void setBackendState(long backendState) {
    setBackendState(nativePointer, backendState);
  }

  public void setCenterW(long centerW) {
    setCenterW(nativePointer, centerW);
  }

  public void setEofflag(int eofflag) {
    setEofflag(nativePointer, eofflag);
  }

  public void setFloorBits(long floorBits) {
    setFloorBits(nativePointer, floorBits);
  }

  public void setGlueBits(long glueBits) {
    setGlueBits(nativePointer, glueBits);
  }

  public void setGranulepos(long granulepos) {
    setGranulepos(nativePointer, granulepos);
  }

  public void setLW(long lW) {
    setLW(nativePointer, lW);
  }

  public void setNW(long nW) {
    setNW(nativePointer, nW);
  }

  public void setPcm(long pcm) {
    setPcm(nativePointer, pcm);
  }

  public void setPcmCurrent(int pcmCurrent) {
    setPcmCurrent(nativePointer, pcmCurrent);
  }

  public void setPcmret(long pcmret) {
    setPcmret(nativePointer, pcmret);
  }

  public void setPcmReturned(int pcmReturned) {
    setPcmReturned(nativePointer, pcmReturned);
  }

  public void setPcmStorage(int pcmStorage) {
    setPcmStorage(nativePointer, pcmStorage);
  }

  public void setPreextrapolate(int preextrapolate) {
    setPreextrapolate(nativePointer, preextrapolate);
  }

  public void setResBits(long resBits) {
    setResBits(nativePointer, resBits);
  }

  public void setSequence(long sequence) {
    setSequence(nativePointer, sequence);
  }

  public void setTimeBits(long timeBits) {
    setTimeBits(nativePointer, timeBits);
  }

  public void setVi(long vi) {
    setVi(nativePointer, vi);
  }

  public void setW(long w) {
    setW(nativePointer, w);
  }

  protected VorbisDspState(long nativePointer) {
    super(nativePointer);
  }

  @Override
  protected void deleteObject() {
    deleteVorbisDspState(nativePointer);
  }

  private static native void deleteVorbisDspState(long jVorbisDspState);
  private static native int getAnalysisp(long jVorbisDspState);
  private static native long getBackendState(long jVorbisDspState);
  private static native long getCenterW(long jVorbisDspState);
  private static native int getEofflag(long jVorbisDspState);
  private static native long getFloorBits(long jVorbisDspState);
  private static native long getGlueBits(long jVorbisDspState);
  private static native long getGranulepos(long jVorbisDspState);
  private static native long getLW(long jVorbisDspState);
  private static native long getNW(long jVorbisDspState);
  private static native long getPcm(long jVorbisDspState);
  private static native int getPcmCurrent(long jVorbisDspState);
  private static native long getPcmret(long jVorbisDspState);
  private static native int getPcmReturned(long jVorbisDspState);
  private static native int getPcmStorage(long jVorbisDspState);
  private static native int getPreextrapolate(long jVorbisDspState);
  private static native long getResBits(long jVorbisDspState);
  private static native long getSequence(long jVorbisDspState);
  private static native long getTimeBits(long jVorbisDspState);
  private static native long getVi(long jVorbisDspState);
  private static native long getW(long jVorbisDspState);
  private static native long newVorbisDspState();
  private static native void setAnalysisp(long jVorbisDspState, int analysisp);
  private static native void setBackendState(long jVorbisDspState, long backendState);
  private static native void setCenterW(long jVorbisDspState, long centerW);
  private static native void setEofflag(long jVorbisDspState, int eofflag);
  private static native void setFloorBits(long jVorbisDspState, long floorBits);
  private static native void setGlueBits(long jVorbisDspState, long glueBits);
  private static native void setGranulepos(long jVorbisDspState, long granulepos);
  private static native void setLW(long jVorbisDspState, long lW);
  private static native void setNW(long jVorbisDspState, long nW);
  private static native void setPcm(long jVorbisDspState, long pcm);
  private static native void setPcmCurrent(long jVorbisDspState, int pcmCurrent);
  private static native void setPcmret(long jVorbisDspState, long pcmret);
  private static native void setPcmReturned(long jVorbisDspState, int pcmReturned);
  private static native void setPcmStorage(long jVorbisDspState, int pcmStorage);
  private static native void setPreextrapolate(long jVorbisDspState, int preextrapolate);
  private static native void setResBits(long jVorbisDspState, long resBits);
  private static native void setSequence(long jVorbisDspState, long sequence);
  private static native void setTimeBits(long jVorbisDspState, long timeBits);
  private static native void setVi(long jVorbisDspState, long vi);
  private static native void setW(long jVorbisDspState, long w);
}
