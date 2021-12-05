package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;

import java.io.IOException;

public abstract class AbstractTzxBlock {
  private final int id;

  public AbstractTzxBlock(final int id) {
    this.id = id;
  }

  protected static int readThreeByteValue(final JBBPBitInputStream inputStream) throws IOException {
    final int len2 = inputStream.readByte();
    final int len1 = inputStream.readByte();
    final int len0 = inputStream.readByte();
    return (len0 << 16) | (len1 << 8) | len2;
  }

  protected static void writeThreeByteValue(final JBBPBitOutputStream outputStream, final int value) throws IOException {
    final int len2 = (value >> 16) & 0xFF;
    final int len1 = (value >> 8) & 0xFF;
    final int len0 = value & 0xFF;
    outputStream.write(len0);
    outputStream.write(len1);
    outputStream.write(len2);
  }

  protected static void writeWord(final JBBPBitOutputStream outputStream, final int value) throws IOException {
    outputStream.writeShort(value, JBBPByteOrder.LITTLE_ENDIAN);
  }

  protected static int readWord(final JBBPBitInputStream inputStream) throws IOException {
    return inputStream.readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN);
  }

  protected static int[] readWordArray(final JBBPBitInputStream inputStream, final int size) throws IOException {
    final int[] result = new int[size];
    for (int i = 0; i < size; i++) {
      result[i] = inputStream.readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN);
    }
    return result;
  }

  protected static void writeWordArray(final JBBPBitOutputStream outputStream, final int[] array) throws IOException {
    for (int i = 0; i < array.length; i++) {
      writeWord(outputStream, array[i]);
    }
  }

  protected static long readDWord(final JBBPBitInputStream inputStream) throws IOException {
    return (long) inputStream.readInt(JBBPByteOrder.LITTLE_ENDIAN) & 0xFFFFFFFFL;
  }

  protected static void writeDWord(final JBBPBitOutputStream outputStream, final long value) throws IOException {
    outputStream.writeInt((int) value, JBBPByteOrder.LITTLE_ENDIAN);
  }

  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    outputStream.write(this.id);
  }

  public int getId() {
    return this.id;
  }
}
