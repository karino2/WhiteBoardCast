package com.google.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * This class reads a PCM wav file.
 */
public class WavReader {
  public final int kWaveFormatExSizePCM = 18;

  private FileInputStream is = null;
  private int filesize;

  private short wFormatTag;
  private short nChannels;
  private int nSamplesPerSec;
  private int nAvgBytesPerSec;
  private short nBlockAlign;
  private short wBitsPerSample;

  private int bytesPerSample;

  public WavReader(File file) throws IOException {
    reset();
    is = new FileInputStream(file);
    readHeader();
  }

  public void close() throws IOException {
    reset();
  }

  public short wFormatTag() {
    return wFormatTag;
  }

  public short nChannels() {
    return nChannels;
  }

  public int nSamplesPerSec() {
    return nSamplesPerSec;
  }

  public int nAvgBytesPerSec() {
    return nAvgBytesPerSec;
  }

  public short nBlockAlign() {
    return nBlockAlign;
  }

  public short wBitsPerSample() {
    return wBitsPerSample;
  }

  public int samplesRemaining() {
    int samplesLeft = 0;
    try {
      samplesLeft = (is.available()) / (nChannels * bytesPerSample);
    } catch (Exception e) {
      return 0;
    }
    return samplesLeft;
  }

  public byte[] readSamples(int numSamples) throws IOException {
    final int samplesLeft = samplesRemaining();
    if (samplesLeft < numSamples)
      throw new IOException("Error too few samples.");

    final int bytesToRead = numSamples * nChannels * bytesPerSample;
    byte[] dataArray = new byte[bytesToRead];
    if (is.read(dataArray) == -1)
      throw new IOException("Error reading samples.");
    return dataArray;
  }

  private void reset() throws IOException {
    filesize = 0;
    wFormatTag = 0;
    nChannels = 0;
    nSamplesPerSec = 0;
    nAvgBytesPerSec = 0;
    nBlockAlign = 0;
    wBitsPerSample = 0;
    bytesPerSample = 0;
    if (is != null)
      is.close();
  }

  private void readHeader() throws IOException {
    byte[] chunkIDArray = new byte[4];
    byte[] chunkDataSizeArray = new byte[4];

    filesize = is.available();

    while (is.available() > 0) {
      if (is.read(chunkIDArray) == -1)
        throw new IOException("Error reading ChunkID.");
      if (is.read(chunkDataSizeArray) == -1)
        throw new IOException("Error reading Chunk Data Size.");

      ByteBuffer chunkDataSizeBuffer = ByteBuffer.wrap(chunkDataSizeArray);
      chunkDataSizeBuffer.order(ByteOrder.LITTLE_ENDIAN);
      int chunkDataSize = chunkDataSizeBuffer.getInt(0);

      String chunkID = new String(chunkIDArray, "UTF-8");
      if (chunkID.equals("RIFF")) {
        if (chunkDataSize != filesize - 8) {
          throw new IOException("Error WAVE chunk size is incorrect.");
        }
        // Skip past "WAVE"
        is.skip(4);
      } else if (chunkID.equals("fmt ")) {
        if (chunkDataSize > kWaveFormatExSizePCM) {
          throw new IOException("Error only PCM format is supported.");
        }

        byte[] chunkArray = new byte[chunkDataSize];
        if (is.read(chunkArray) == -1)
          throw new IOException("Error reading fmt chunk.");

        ByteBuffer buf = ByteBuffer.wrap(chunkArray);
        buf.order(ByteOrder.LITTLE_ENDIAN);

        wFormatTag = buf.getShort(0);
        nChannels = buf.getShort(2);
        nSamplesPerSec = buf.getInt(4);
        nAvgBytesPerSec = buf.getInt(8);
        nBlockAlign = buf.getShort(12);
        wBitsPerSample = buf.getShort(14);
        if (wFormatTag != 1) {
          throw new IOException("Error only PCM format is supported.");
        }
        bytesPerSample = wBitsPerSample / 8;
      } else if (chunkID.equals("data")) {
        return;
      } else {
        // Skip unknown chunks
        is.skip(chunkDataSize);
      }
    }
  }
}

