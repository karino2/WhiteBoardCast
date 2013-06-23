// Author: mszal@google.com (Michael Szal)

package com.google.libvorbis;

public class AllocChain extends Common {

  public AllocChain() {
    nativePointer = newAllocChain();
  }

  public AllocChain getNext() {
    long next = getNext(nativePointer);
    return new AllocChain(next);
  }

  public long getPtr() {
    return getPtr(nativePointer);
  }

  public void setNext(AllocChain next) {
    setNext(nativePointer, next.getNativePointer());
  }

  public void setPtr(long ptr) {
    setPtr(nativePointer, ptr);
  }

  protected AllocChain(long nativePointer) {
    super(nativePointer);
  }

  @Override
  protected void deleteObject() {
    deleteAllocChain(nativePointer);
  }

  private static native void deleteAllocChain(long jAllocChain);
  private static native long getNext(long jAllocChain);
  private static native long getPtr(long jAllocChain);
  private static native long newAllocChain();
  private static native void setNext(long jAllocChain, long next);
  private static native void setPtr(long jAllocChain, long ptr);
}
