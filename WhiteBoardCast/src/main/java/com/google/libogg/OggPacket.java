// Author: mszal@google.com (Michael Szal)

package com.google.libogg;

public class OggPacket extends Common {

  public OggPacket() {
    nativePointer = newOggPacket();
  }

  public long getBOS() {
    return getBOS(nativePointer);
  }

  public long getBytes() {
    return getBytes(nativePointer);
  }

  public long getEOS() {
    return getEOS(nativePointer);
  }

  public long getGranulepos() {
    return getGranulepos(nativePointer);
  }

  public long getPacket() {
    return getPacket(nativePointer);
  }

  public byte[] getPacketData() {
    return getPacketData(nativePointer);
  }

  public long getPacketno() {
    return getPacketno(nativePointer);
  }

  public void setBOS(long bOS) {
    setBOS(nativePointer, bOS);
  }

  public void setBytes(long bytes) {
    setBytes(nativePointer, bytes);
  }

  public void setEOS(long eOS) {
    setEOS(nativePointer, eOS);
  }

  public void setGranulepos(long granulepos) {
    setGranulepos(nativePointer, granulepos);
  }

  public void setPacket(long packet) {
    setPacket(nativePointer, packet);
  }

  public void setPacketno(long packetno) {
    setPacketno(nativePointer, packetno);
  }

  protected OggPacket(long nativePointer) {
    super(nativePointer);
  }

  @Override
  protected void deleteObject() {
    deleteOggPacket(nativePointer);
  }

  private static native void deleteOggPacket(long jOggPacket);
  private static native long getBOS(long jOggPacket);
  private static native long getBytes(long jOggPacket);
  private static native long getEOS(long jOggPacket);
  private static native long getGranulepos(long jOggPacket);
  private static native long getPacket(long jOggPacket);
  private static native byte[] getPacketData(long jOggPacket);
  private static native long getPacketno(long jOggPacket);
  private static native long newOggPacket();
  private static native void setBOS(long jOggPacket, long bOS);
  private static native void setBytes(long jOggPacket, long bytes);
  private static native void setEOS(long jOggPacket, long eOS);
  private static native void setGranulepos(long jOggPacket, long granulepos);
  private static native void setPacket(long jOggPacket, long packet);
  private static native void setPacketno(long jOggPacket, long packetno);
}
