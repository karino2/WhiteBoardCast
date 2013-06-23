// Author: mszal@google.com (Michael Szal)

package com.google.libogg;

public class OggpackBuffer extends Common {

  public OggpackBuffer() {
    nativePointer = newOggpackBuffer();
  }

  public OggpackBuffer(long nativePointer) {
    super(nativePointer);
  }

  public long getBuffer() {
    return getBuffer(nativePointer);
  }

  public int getEndbit() {
    return getEndbit(nativePointer);
  }

  public long getEndbyte() {
    return getEndbyte(nativePointer);
  }

  public long getPtr() {
    return getPtr(nativePointer);
  }

  public long getStorage() {
    return getStorage(nativePointer);
  }

  public void setBuffer(long buffer) {
    setBuffer(nativePointer, buffer);
  }

  public void setEndbit(int endbit) {
    setEndbit(nativePointer, endbit);
  }

  public void setEndbyte(long endbyte) {
    setEndbyte(nativePointer, endbyte);
  }

  public void setPtr(long ptr) {
    setPtr(nativePointer, ptr);
  }

  public void setStorage(long storage) {
    setStorage(nativePointer, storage);
  }

  @Override
  protected void deleteObject() {
    deleteOggpackBuffer(nativePointer);
  }

  private static native void deleteOggpackBuffer(long jOggpackBuffer);
  private static native long getBuffer(long jOggpackBuffer);
  private static native int getEndbit(long jOggpackBuffer);
  private static native long getEndbyte(long jOggpackBuffer);
  private static native long getPtr(long jOggpackBuffer);
  private static native long getStorage(long jOggpackBuffer);
  private static native long newOggpackBuffer();
  private static native void setBuffer(long jOggpackBuffer, long buffer);
  private static native void setEndbit(long jOggpackBuffer, int endbit);
  private static native void setEndbyte(long jOggpackBuffer, long endbyte);
  private static native void setPtr(long jOggpackBuffer, long ptr);
  private static native void setStorage(long jOggpackBuffer, long storage);
}
