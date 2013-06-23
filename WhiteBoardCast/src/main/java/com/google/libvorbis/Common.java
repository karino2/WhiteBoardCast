// Author: mszal@google.com (Michael Szal)

package com.google.libvorbis;

public abstract class Common {

  static {
    System.loadLibrary("vorbisJNI");
  }

  protected long nativePointer;
  protected boolean ownMemory;

  public long getNativePointer() {
    return nativePointer;
  }

  protected Common() {
    nativePointer = 0;
    ownMemory = true;
  }

  protected Common(long nativePointer) {
    this.nativePointer = nativePointer;
    ownMemory = false;
  }

  protected abstract void deleteObject();

  @Override
  protected void finalize() {
    if (ownMemory) {
      deleteObject();
    }
    nativePointer = 0;
    ownMemory = false;
  }
}
