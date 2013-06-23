// Author: mszal@google.com (Michael Szal)

package com.google.libvorbis;

import com.google.libogg.OggPacket;

public class Codec {

  public static final int OV_EBADHEADER = -133;
  public static final int OV_EBADLINK = -137;
  public static final int OV_EBADPACKET = -136;
  public static final int OV_EFAULT = -129;
  public static final int OV_EIMPL = -130;
  public static final int OV_EINVAL = -131;
  public static final int OV_ENOSEEK = -138;
  public static final int OV_ENOTAUDIO = -135;
  public static final int OV_ENOTVORBIS = -132;
  public static final int OV_EOF = -2;
  public static final int OV_EREAD = -128;
  public static final int OV_EVERSION = -134;
  public static final int OV_FALSE = -1;
  public static final int OV_HOLE = -3;

  public static int vorbisAnalysis(VorbisBlock vb, OggPacket op) {
    return vorbisAnalysis(vb.getNativePointer(), op.getNativePointer());
  }

  public static int vorbisAnalysisBlockout(VorbisDspState v, VorbisBlock vb) {
    return vorbisAnalysisBlockout(v.getNativePointer(), vb.getNativePointer());
  }

  public static void vorbisAnalysisBuffer(VorbisDspState v, float[][] samples) {
    vorbisAnalysisBuffer(v.getNativePointer(), samples);
  }

  public static int vorbisAnalysisHeaderout(VorbisDspState v, VorbisComment vc, OggPacket op,
      OggPacket opComm, OggPacket opCode) {
    return vorbisAnalysisHeaderout(v.getNativePointer(), vc.getNativePointer(),
        op.getNativePointer(), opComm.getNativePointer(), opCode.getNativePointer());
  }

  public static int vorbisAnalysisInit(VorbisDspState v, VorbisInfo vi) {
    return vorbisAnalysisInit(v.getNativePointer(), vi.getNativePointer());
  }

  public static int vorbisAnalysisWrote(VorbisDspState v, int vals) {
    return vorbisAnalysisWrote(v.getNativePointer(), vals);
  }

  public static int vorbisBitrateAddblock(VorbisBlock vb) {
    return vorbisBitrateAddblock(vb.getNativePointer());
  }

  public static int vorbisBitrateFlushpacket(VorbisDspState v, OggPacket op) {
    return vorbisBitrateFlushpacket(v.getNativePointer(), op.getNativePointer());
  }

  public static int vorbisBlockClear(VorbisBlock vb) {
    return vorbisBlockClear(vb.getNativePointer());
  }

  public static int vorbisBlockInit(VorbisDspState v, VorbisBlock vb) {
    return vorbisBlockInit(v.getNativePointer(), vb.getNativePointer());
  }

  public static void vorbisCommentAdd(VorbisComment vc, String comment) {
    vorbisCommentAdd(vc.getNativePointer(), comment);
  }

  public static void vorbisCommentAddTag(VorbisComment vc, String tag, String contents) {
    vorbisCommentAddTag(vc.getNativePointer(), tag, contents);
  }

  public static void vorbisCommentClear(VorbisComment vc) {
    vorbisCommentClear(vc.getNativePointer());
  }

  public static int vorbisCommentheaderOut(VorbisComment vc, OggPacket op) {
    return vorbisCommentheaderOut(vc.getNativePointer(), op.getNativePointer());
  }

  public static void vorbisCommentInit(VorbisComment vc) {
    vorbisCommentInit(vc.getNativePointer());
  }

  public static String vorbisCommentQuery(VorbisComment vc, String tag, int count) {
    return vorbisCommentQuery(vc.getNativePointer(), tag, count);
  }

  public static int vorbisCommentQueryCount(VorbisComment vc, String tag) {
    return vorbisCommentQueryCount(vc.getNativePointer(), tag);
  }

  public static void vorbisDspClear(VorbisDspState v) {
    vorbisDspClear(v.getNativePointer());
  }

  public static double vorbisGranuleTime(VorbisDspState v, long granulepos) {
    return vorbisGranuleTime(v.getNativePointer(), granulepos);
  }

  public static int vorbisInfoBlockSize(VorbisInfo vi, int zo) {
    return vorbisInfoBlockSize(vi.getNativePointer(), zo);
  }

  public static void vorbisInfoClear(VorbisInfo vi) {
    vorbisInfoClear(vi.getNativePointer());
  }

  public static void vorbisInfoInit(VorbisInfo vi) {
    vorbisInfoInit(vi.getNativePointer());
  }

  public static long vorbisPacketBlocksize(VorbisInfo vi, OggPacket op) {
    return vorbisPacketBlocksize(vi.getNativePointer(), op.getNativePointer());
  }

  public static int vorbisSynthesis(VorbisBlock vb, OggPacket op) {
    return vorbisSynthesis(vb.getNativePointer(), op.getNativePointer());
  }

  public static int vorbisSynthesisBlockin(VorbisDspState v, VorbisBlock vb) {
    return vorbisSynthesisBlockin(v.getNativePointer(), vb.getNativePointer());
  }

  public static int vorbisSynthesisHalfrate(VorbisInfo vi, int flag) {
    return vorbisSynthesisHalfrate(vi.getNativePointer(), flag);
  }

  public static int vorbisSynthesisHalfrateP(VorbisInfo vi) {
    return vorbisSynthesisHalfrateP(vi.getNativePointer());
  }

  public static int vorbisSynthesisHeaderin(VorbisInfo vi, VorbisComment vc, OggPacket op) {
    return vorbisSynthesisHeaderin(vi.getNativePointer(), vc.getNativePointer(),
        op.getNativePointer());
  }

  public static int vorbisSynthesisIdheader(OggPacket op) {
    return vorbisSynthesisIdheader(op.getNativePointer());
  }

  public static int vorbisSynthesisInit(VorbisDspState v, VorbisInfo vi) {
    return vorbisSynthesisInit(v.getNativePointer(), vi.getNativePointer());
  }

  public static int vorbisSynthesisLapout(VorbisDspState v, float[][][] pcm, int channels) {
    return vorbisSynthesisLapout(v.getNativePointer(), pcm, channels);
  }

  public static int vorbisSynthesisPcmout(VorbisDspState v, float[][][] pcm, int channels) {
    return vorbisSynthesisPcmout(v.getNativePointer(), pcm, channels);
  }

  public static int vorbisSynthesisRead(VorbisDspState v, int samples) {
    return vorbisSynthesisRead(v.getNativePointer(), samples);
  }

  public static int vorbisSynthesisRestart(VorbisDspState v) {
    return vorbisSynthesisRestart(v.getNativePointer());
  }

  public static int vorbisSynthesisTrackonly(VorbisBlock vb, OggPacket op) {
    return vorbisSynthesisTrackonly(vb.getNativePointer(), op.getNativePointer());
  }

  public static native String vorbisVersionString();

  private static native int vorbisAnalysis(long vb, long op);
  private static native int vorbisAnalysisBlockout(long v, long vb);

  /**
   * The original function is declared like this:
   *
   * float** vorbis_analysis_buffer(vorbis_dsp_state* v, int vals);
   *
   * You're supposed to call the function with the number of samples you have.  It allocates
   * internal storage for the samples in the form of a 2D array, indexed by channel then sample.
   * You copy your samples into the array, then call vorbis_analysis_wrote to indicate the encoder
   * can analyze the samples.
   *
   * The Java version is simpler.  Just pass the samples you have to the function, then call
   * vorbis_analysis_wrote.
   */
  private static native void vorbisAnalysisBuffer(long v, float[][] samples);
  private static native int vorbisAnalysisHeaderout(long v, long vc, long op, long opComm,
      long opCode);
  private static native int vorbisAnalysisInit(long v, long vi);
  private static native int vorbisAnalysisWrote(long v, int vals);
  private static native int vorbisBitrateAddblock(long vb);
  private static native int vorbisBitrateFlushpacket(long v, long op);
  private static native int vorbisBlockClear(long vb);
  private static native int vorbisBlockInit(long v, long vb);
  private static native void vorbisCommentAdd(long vc, String comment);
  private static native void vorbisCommentAddTag(long vc, String tag, String contents);
  private static native void vorbisCommentClear(long vc);
  private static native int vorbisCommentheaderOut(long vc, long op);
  private static native void vorbisCommentInit(long vc);
  private static native String vorbisCommentQuery(long vc, String tag, int count);
  private static native int vorbisCommentQueryCount(long vc, String tag);
  private static native void vorbisDspClear(long v);
  private static native double vorbisGranuleTime(long v, long granulepos);
  private static native int vorbisInfoBlockSize(long vi, int zo);
  private static native void vorbisInfoClear(long vi);
  private static native void vorbisInfoInit(long vi);
  private static native long vorbisPacketBlocksize(long vi, long op);
  private static native int vorbisSynthesis(long vb, long op);
  private static native int vorbisSynthesisBlockin(long v, long vb);
  private static native int vorbisSynthesisHalfrate(long vi, int flag);
  private static native int vorbisSynthesisHalfrateP(long vi);
  private static native int vorbisSynthesisHeaderin(long vi, long vc, long op);
  private static native int vorbisSynthesisIdheader(long op);
  private static native int vorbisSynthesisInit(long v, long vi);

  /**
   * For vorbisSynthesisLapout and Pcmout, the original functions are declared like this:
   *
   * int vorbis_synthesis_lapout(vorbis_dsp_state* v, float*** pcm);
   *
   * After calling the function, *pcm will be a 2D array of samples, indexed by channel then sample.
   * The return value is the number of samples (length of the inner array).  But you can also call
   * the function with pcm = NULL, in which case it returns the number of samples available but does
   * not get any samples.
   *
   * These Java functions are similar, but you must pass in a float[][][] and the number of
   * channels (available in the vorbis_info struct).  The first element in the float array will be
   * changed to point to a 2D array of samples.  You can still pass in null for the array to get the
   * number of samples.
   */
  private static native int vorbisSynthesisLapout(long v, float[][][] pcm, int channels);
  private static native int vorbisSynthesisPcmout(long v, float[][][] pcm, int channels);
  private static native int vorbisSynthesisRead(long v, int samples);
  private static native int vorbisSynthesisRestart(long v);
  private static native int vorbisSynthesisTrackonly(long vb, long op);
}
