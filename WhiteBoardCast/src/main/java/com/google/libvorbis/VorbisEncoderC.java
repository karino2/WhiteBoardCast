package com.google.libvorbis;

public class VorbisEncoderC extends Common {
  public VorbisEncoderC(VorbisEncConfig cfg) throws VorbisException {
    nativePointer = newVorbisEncoder();
    if (nativePointer == 0) {
      throw new VorbisException("Can not allocate JNI codec object");
    }

    if (!Init(nativePointer, cfg.handle())) {
      throw new VorbisException("Can not initialize Vorbis encoder");
    }
  }

  public boolean Encode(byte[] jBuffer) {
    return Encode(nativePointer, jBuffer, jBuffer.length);
  }

  // Deprecated. Please use ReadCompressedFrame.
  public byte[] ReadCompressedAudio(long[] timestamp) {
    return ReadCompressedAudio(nativePointer, timestamp);
  }

  public AudioFrame ReadCompressedFrame() {
    return ReadCompressedFrame(nativePointer);
  }

  public byte[] CodecPrivate() {
    return CodecPrivate(nativePointer);
  }

  public int GetChannels() {
    return GetChannels(nativePointer);
  }

  public int GetSampleRate() {
    return GetSampleRate(nativePointer);
  }

  public int GetBitsPerSample() {
    return GetBitsPerSample(nativePointer);
  }

  public int GetAverageBitrate() {
    return GetAverageBitrate(nativePointer);
  }

  public int GetMinimumBitrate() {
    return GetMinimumBitrate(nativePointer);
  }

  public int GetMaximumBitrate() {
    return GetMaximumBitrate(nativePointer);
  }

  public void SetChannels(int channels) {
    SetChannels(nativePointer, channels);
  }

  public void SetSampleRate(int sample_rate) {
    SetSampleRate(nativePointer, sample_rate);
  }

  public void SetBitsPerSample(int bits_per_sample) {
    SetBitsPerSample(nativePointer, bits_per_sample);
  }

  public void SetAverageBitrate(int bitrate) {
    SetAverageBitrate(nativePointer, bitrate);
  }

  public void SetMinimumBitrate(int bitrate) {
    SetMinimumBitrate(nativePointer, bitrate);
  }

  public void SetMaximumBitrate(int bitrate) {
    SetMaximumBitrate(nativePointer, bitrate);
  }

  @Override
  protected void deleteObject() {
    deleteVorbisEncoder(nativePointer);
  }

  private static native void deleteVorbisEncoder(long jVorbisEncoder);
  private static native long newVorbisEncoder();
  private static native boolean Init(long jVorbisEncoder, long jVorbisEncoderConfig);
  private static native boolean Encode(long jVorbisEncoder, byte[] jBuffer, int length);
  private static native byte[] ReadCompressedAudio(long jVorbisEncoder, long[] timestamp);
  private static native AudioFrame ReadCompressedFrame(long jVorbisEncoder);
  private static native byte[] CodecPrivate(long jVorbisEncoder);

  private static native int GetChannels(long jVorbisEncoder);
  private static native int GetSampleRate(long jVorbisEncoder);
  private static native int GetBitsPerSample(long jVorbisEncoder);
  private static native int GetAverageBitrate(long jVorbisEncoder);
  private static native int GetMinimumBitrate(long jVorbisEncoder);
  private static native int GetMaximumBitrate(long jVorbisEncoder);

  private static native void SetChannels(long jVorbisEncoder, int channels);
  private static native void SetSampleRate(long jVorbisEncoder, int sample_rate);
  private static native void SetBitsPerSample(long jVorbisEncoder, int bits_per_sample);
  private static native void SetAverageBitrate(long jVorbisEncoder, int bitrate);
  private static native void SetMinimumBitrate(long jVorbisEncoder, int bitrate);
  private static native void SetMaximumBitrate(long jVorbisEncoder, int bitrate);
}
