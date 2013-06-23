// Author: mszal@google.com (Michael Szal)

package com.google.libvorbis;

import com.google.libogg.OggpackBuffer;

public class VorbisBlock extends Common {

  public VorbisBlock() {
    nativePointer = newVorbisBlock();
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

  public long getInternal() {
    return getInternal(nativePointer);
  }

  public long getLocalalloc() {
    return getLocalalloc(nativePointer);
  }

  public long getLocalstore() {
    return getLocalstore(nativePointer);
  }

  public long getLocaltop() {
    return getLocaltop(nativePointer);
  }

  public long getLW() {
    return getLW(nativePointer);
  }

  public int getMode() {
    return getMode(nativePointer);
  }

  public long getNW() {
    return getNW(nativePointer);
  }

  public OggpackBuffer getOpb() {
    long opb = getOpb(nativePointer);
    return new OggpackBuffer(opb);
  }

  public long getPcm() {
    return getPcm(nativePointer);
  }

  public int getPcmend() {
    return getPcmend(nativePointer);
  }

  public AllocChain getReap() {
    long reap = getReap(nativePointer);
    return new AllocChain(reap);
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

  public long getTotaluse() {
    return getTotaluse(nativePointer);
  }

  public VorbisDspState getVd() {
    long vd = getVd(nativePointer);
    return new VorbisDspState(vd);
  }

  public long getW() {
    return getW(nativePointer);
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

  public void setInternal(long internal) {
    setInternal(nativePointer, internal);
  }

  public void setLocalalloc(long localalloc) {
    setLocalalloc(nativePointer, localalloc);
  }

  public void setLocalstore(long localstore) {
    setLocalstore(nativePointer, localstore);
  }

  public void setLocaltop(long localtop) {
    setLocaltop(nativePointer, localtop);
  }

  public void setLW(long lW) {
    setLW(nativePointer, lW);
  }

  public void setMode(int mode) {
    setMode(nativePointer, mode);
  }

  public void setNW(long nW) {
    setNW(nativePointer, nW);
  }

  public void setOpb(OggpackBuffer opb) {
    setOpb(nativePointer, opb.getNativePointer());
  }

  public void setPcm(long pcm) {
    setPcm(nativePointer, pcm);
  }

  public void setPcmend(int pcmend) {
    setPcmend(nativePointer, pcmend);
  }

  public void setReap(AllocChain reap) {
    setReap(nativePointer, reap.getNativePointer());
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

  public void setTotaluse(long totaluse) {
    setTotaluse(nativePointer, totaluse);
  }

  public void setVd(VorbisDspState vd) {
    setVd(nativePointer, vd.getNativePointer());
  }

  public void setW(long w) {
    setW(nativePointer, w);
  }

  protected VorbisBlock(long nativePointer) {
    super(nativePointer);
  }

  @Override
  protected void deleteObject() {
    deleteVorbisBlock(nativePointer);
  }

  private static native void deleteVorbisBlock(long jVorbisBlock);
  private static native int getEofflag(long jVorbisBlock);
  private static native long getFloorBits(long jVorbisBlock);
  private static native long getGlueBits(long jVorbisBlock);
  private static native long getGranulepos(long jVorbisBlock);
  private static native long getInternal(long jVorbisBlock);
  private static native long getLocalalloc(long jVorbisBlock);
  private static native long getLocalstore(long jVorbisBlock);
  private static native long getLocaltop(long jVorbisBlock);
  private static native long getLW(long jVorbisBlock);
  private static native int getMode(long jVorbisBlock);
  private static native long getNW(long jVorbisBlock);
  private static native long getOpb(long jVorbisBlock);
  private static native long getPcm(long jVorbisBlock);
  private static native int getPcmend(long jVorbisBlock);
  private static native long getReap(long jVorbisBlock);
  private static native long getResBits(long jVorbisBlock);
  private static native long getSequence(long jVorbisBlock);
  private static native long getTimeBits(long jVorbisBlock);
  private static native long getTotaluse(long jVorbisBlock);
  private static native long getVd(long jVorbisBlock);
  private static native long getW(long jVorbisBlock);
  private static native long newVorbisBlock();
  private static native void setEofflag(long jVorbisBlock, int eofflag);
  private static native void setFloorBits(long jVorbisBlock, long floorBits);
  private static native void setGlueBits(long jVorbisBlock, long glueBits);
  private static native void setGranulepos(long jVorbisBlock, long granulepos);
  private static native void setInternal(long jVorbisBlock, long internal);
  private static native void setLocalalloc(long jVorbisBlock, long localalloc);
  private static native void setLocalstore(long jVorbisBlock, long localstore);
  private static native void setLocaltop(long jVorbisBlock, long localtop);
  private static native void setLW(long jVorbisBlock, long lW);
  private static native void setMode(long jVorbisBlock, int mode);
  private static native void setNW(long jVorbisBlock, long nW);
  private static native void setOpb(long jVorbisBlock, long opb);
  private static native void setPcm(long jVorbisBlock, long pcm);
  private static native void setPcmend(long jVorbisBlock, long pcmend);
  private static native void setReap(long jVorbisBlock, long reap);
  private static native void setResBits(long jVorbisBlock, long resBits);
  private static native void setSequence(long jVorbisBlock, long sequence);
  private static native void setTimeBits(long jVorbisBlock, long timeBits);
  private static native void setTotaluse(long jVorbisBlock, long totaluse);
  private static native void setVd(long jVorbisBlock, long vd);
  private static native void setW(long jVorbisBlock, long w);
}
