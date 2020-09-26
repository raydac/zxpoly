package com.igormaznitsa.zxpoly.components.tapereader.wave;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

public final class InMemoryWavFile {

  private final byte[] wavData;
  private final int audioFormat;
  private final int numChannels;
  private final int sampleRate;
  private final int byteRate;
  private final int blockAlign;
  private final int bitsPerSample;

  private final double tstatesPerBlock;

  public InMemoryWavFile(final File file, final long tstatesPerSecond) throws IOException {
    try (final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "r")) {
      final long fileSize = file.length();

      if (!is("RIFF", readChunkId(randomAccessFile))) {
        throw new IOException("It is not RIFF container");
      }
      final int chunkSize = readInt(randomAccessFile);
      if (chunkSize < 36) {
        throw new IOException("Wrong container length in WAV file");
      }
      if (!is("WAVE", readChunkId(randomAccessFile))) {
        throw new IOException("It is not WAV file");
      }

      final long startSubchunksPos = randomAccessFile.getFilePointer();
      if (!posToChunk(randomAccessFile, "fmt ", startSubchunksPos, fileSize)) {
        throw new IOException("Can't find 'fmt' chunk");
      }

      final long formatChunkSize = readChunkSize(randomAccessFile);
      if (formatChunkSize < 16) {
        throw new IOException("'fmt' chunk has size less than 16");
      }
      this.audioFormat = readShort(randomAccessFile);
      this.numChannels = readShort(randomAccessFile);
      this.sampleRate = readInt(randomAccessFile);
      this.byteRate = readInt(randomAccessFile);
      this.blockAlign = readShort(randomAccessFile);
      this.bitsPerSample = readShort(randomAccessFile);

      if (this.audioFormat != 1) {
        throw new IOException("Only integer PCM format is supported: " + this.audioFormat);
      }

      if (this.bitsPerSample != 8 && this.bitsPerSample != 16 && this.bitsPerSample != 24 &&
          this.bitsPerSample != 32) {
        throw new IOException(
            "Unsupported bit sample size: " + this.bitsPerSample);
      }

      if (!posToChunk(randomAccessFile, "data", startSubchunksPos, fileSize)) {
        throw new IOException("Can't find chunk 'data'");
      }

      final long dataLength = readChunkSize(randomAccessFile);
      if (dataLength >= Integer.MAX_VALUE) {
        throw new IOException("Too big WAV file to be cached: " + dataLength + " bytes");
      }
      this.wavData = new byte[(int) dataLength];

      randomAccessFile.readFully(this.wavData);

      this.tstatesPerBlock = (double) this.sampleRate / (double) tstatesPerSecond;
    }
  }

  private static int readChunkId(final RandomAccessFile file) throws IOException {
    return file.readInt();
  }

  private static long readChunkSize(final RandomAccessFile file) throws IOException {
    return (long) readInt(file) & 0xFFFFFFFFL;
  }

  private static int readInt(final RandomAccessFile file) throws IOException {
    int value = file.readInt();
    int result = 0;
    for (int i = 0; i < 4; i++) {
      result = (value & 0xFF) | (result << 8);
      value >>>= 8;
    }
    return result;
  }

  private static int readShort(final RandomAccessFile file) throws IOException {
    final int value = file.readUnsignedShort();
    return ((value & 0xFF) << 8) | (value >>> 8);
  }

  private static boolean is(final String text, final int value) {
    final int textAsInt = ((text.charAt(0) & 0xFF) << 24)
        | ((text.charAt(1) & 0xFF) << 16)
        | ((text.charAt(2) & 0xFF) << 8)
        | (text.charAt(3) & 0xFF);
    return textAsInt == value;
  }

  private static boolean posToChunk(
      final RandomAccessFile file,
      final String chunkName,
      final long from,
      final long length
  ) throws IOException {
    file.seek(from);
    while (file.getFilePointer() < length) {
      if (is(chunkName, readChunkId(file))) {
        return true;
      }
      final int chunkSize = readInt(file);
      file.skipBytes(chunkSize);
    }
    return false;
  }

  public int getBitsPerSample() {
    return this.bitsPerSample;
  }

  private int readUnsignedByteAt(final long pos) {
    return this.wavData[(int) pos] & 0xFF;
  }

  private int readSignedShort(final long pos) {
    int p = (int) pos;
    final int a = this.wavData[p++] & 0xFF;
    final int b = this.wavData[p] & 0xFF;
    return (short) ((b << 8) | a);
  }

  private int readPcm24(final long pos) {
    int p = (int) pos;
    final int a = this.wavData[p++] & 0xFF;
    final int b = this.wavData[p++] & 0xFF;
    final int c = this.wavData[p];
    return (c << 16) | (b << 8) | a;
  }

  private int readPcm32(final long pos) {
    int p = (int) pos;
    final int a = this.wavData[p++] & 0xFF;
    final int b = this.wavData[p++] & 0xFF;
    final int c = this.wavData[p++] & 0xFF;
    final int d = this.wavData[p];
    return (d << 24) | (c << 16) | (b << 8) | a;
  }

  public long readAtPosition(final long tstatePosition) {
    long blockPosition =
        Math.round(this.tstatesPerBlock * tstatePosition) * this.blockAlign;

    if (this.numChannels == 1) {
      switch (this.bitsPerSample) {
        case 8:
          return this.readUnsignedByteAt(blockPosition);
        case 16:
          return this.readSignedShort(blockPosition);
        case 24:
          return this.readPcm24(blockPosition);
        case 32:
          return this.readPcm32(blockPosition);
        default:
          throw new Error("Unexpected bitness");
      }
    } else {
      long result = 0;
      for (int i = 0; i < this.numChannels; i++) {
        final int next;
        switch (this.bitsPerSample) {
          case 8:
            next = this.readUnsignedByteAt(blockPosition);
            break;
          case 16:
            next = this.readSignedShort(blockPosition);
            break;
          case 24:
            next = this.readPcm24(blockPosition);
            break;
          case 32:
            next = this.readPcm32(blockPosition);
            break;
          default:
            throw new Error("Unexpected bitness");
        }
        result += next;
        blockPosition += this.blockAlign;
      }
      return result;
    }
  }

  public void dispose() {

  }

  public int getBlockAlign() {
    return this.blockAlign;
  }

  public int getByteRate() {
    return this.byteRate;
  }

  public int getSampleRate() {
    return this.sampleRate;
  }

  public int getNumChannels() {
    return this.numChannels;
  }

  public int getAudioFormat() {
    return this.audioFormat;
  }
}
