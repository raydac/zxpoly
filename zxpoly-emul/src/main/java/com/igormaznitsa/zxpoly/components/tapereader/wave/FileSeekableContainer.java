package com.igormaznitsa.zxpoly.components.tapereader.wave;

import java.io.IOException;
import java.io.RandomAccessFile;

public class FileSeekableContainer implements SeekableContainer {
  private final RandomAccessFile file;

  public FileSeekableContainer(final RandomAccessFile file) {
    this.file = file;
  }

  @Override
  public void close() throws IOException {
    this.file.close();
  }

  @Override
  public long length() throws IOException {
    return this.file.length();
  }

  @Override
  public long getFilePointer() throws IOException {
    return this.file.getFilePointer();
  }

  @Override
  public int readUnsignedShort() throws IOException {
    return this.file.readUnsignedShort();
  }

  @Override
  public void readFully(byte[] wavData) throws IOException {
    this.file.readFully(wavData);
  }

  @Override
  public int readInt() throws IOException {
    return this.file.readInt();
  }

  @Override
  public void seek(final long pos) throws IOException {
    this.file.seek(pos);
  }

  @Override
  public int skipBytes(final int bytes) throws IOException {
    return this.file.skipBytes(bytes);
  }
}
