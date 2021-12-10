package com.igormaznitsa.zxpoly.components.tapereader.wave;

import java.io.EOFException;
import java.io.IOException;

public class ByteArraySeekableContainer implements SeekableContainer {

  private final byte[] data;
  private int pointer;

  public ByteArraySeekableContainer(final byte[] data) {
    this.data = data;
  }

  @Override
  public long length() throws IOException {
    return this.data.length;
  }

  @Override
  public long getFilePointer() throws IOException {
    return this.pointer;
  }

  private int read() {
    if (this.pointer < this.data.length) {
      return this.data[this.pointer++] & 0xFF;
    } else {
      return -1;
    }
  }

  @Override
  public int readUnsignedShort() throws IOException {
    int ch1 = this.read();
    int ch2 = this.read();
    if ((ch1 | ch2) < 0) throw new EOFException();
    return (ch1 << 8) | ch2;
  }

  @Override
  public void readFully(final byte[] wavData) throws IOException {
    for (int i = 0; i < wavData.length; i++) {
      final int value = this.read();
      if (value < 0) throw new EOFException();
      wavData[i] = (byte) value;
    }
  }

  @Override
  public int readInt() throws IOException {
    final int ch1 = this.read();
    final int ch2 = this.read();
    final int ch3 = this.read();
    final int ch4 = this.read();
    if ((ch1 | ch2 | ch3 | ch4) < 0) throw new EOFException();
    return (ch1 << 24) | (ch2 << 16) | (ch3 << 8) | ch4;
  }

  @Override
  public void seek(final long pos) throws IOException {
    if (pos < 0) {
      throw new IOException("Negative seek offset");
    } else {
      this.pointer = (int) pos;
    }
  }

  @Override
  public int skipBytes(int bytes) throws IOException {
    int counter = 0;
    for (int i = 0; i < bytes; i++) {
      final int value = this.read();
      if (value >= 0) {
        counter++;
      } else {
        break;
      }
    }
    return counter;
  }

  @Override
  public void close() throws IOException {

  }
}
