// Author: mszal@google.com (Michael Szal)

package com.google.libvorbis;

public class VorbisComment extends Common {

  public VorbisComment() {
    nativePointer = newVorbisComment();
  }

  public int[] getCommentLengths() {
    return getCommentLengths(nativePointer);
  }

  public int getComments() {
    return getComments(nativePointer);
  }

  public String[] getUserComments() {
    return getUserComments(nativePointer);
  }

  public String getVendor() {
    return getVendor(nativePointer);
  }

  public void setCommentLengths(int[] commentLengths) {
    setCommentLengths(nativePointer, commentLengths);
  }

  public void setCommens(int comments) {
    setComments(nativePointer, comments);
  }

  public void setUserCommens(String[] userComments) {
    setUserComments(nativePointer, userComments);
  }

  public void setVendor(String vendor) {
    setVendor(nativePointer, vendor);
  }

  protected VorbisComment(long nativePointer) {
    super(nativePointer);
  }

  @Override
  protected void deleteObject() {
    deleteVorbisComment(nativePointer);
  }

  private static native void deleteVorbisComment(long jVorbisComment);
  private static native int[] getCommentLengths(long jVorbisComment);
  private static native int getComments(long jVorbisComment);
  private static native String[] getUserComments(long jVorbisComment);
  private static native String getVendor(long jVorbisComment);
  private static native long newVorbisComment();
  private static native void setCommentLengths(long jVorbisComment, int[] jCommentLengths);
  private static native void setComments(long jVorbisComment, int comments);
  private static native void setUserComments(long jVorbisComment, String[] jUserComments);
  private static native void setVendor(long jVorbisComment, String jVendor);
}
