package com.igormaznitsa.zxpoly.components.tapereader.wave;

import java.io.Closeable;
import java.io.IOException;

public interface SeekableContainer extends Closeable {
  long length() throws IOException;

  long getFilePointer() throws IOException;

  int readUnsignedShort() throws IOException;

  void readFully(byte[] wavData) throws IOException;

  int readInt() throws IOException;

  void seek(long pos) throws IOException;

  int skipBytes(int bytes) throws IOException;
}
