package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;

import java.io.IOException;

public class TzxBlockCallSequence extends AbstractTzxBlock {

  private final short[] offsets;

  public TzxBlockCallSequence(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.CALL_SEQUENCE.getId());
    final int number = readWord(inputStream);
    this.offsets = new short[number];
    for (int i = 0; i < number; i++) {
      this.offsets[i] = (short) readWord(inputStream);
    }
  }

  public short[] getOffsets() {
    return offsets;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.offsets.length);
    for (short s : this.offsets) {
      outputStream.writeShort(s, JBBPByteOrder.LITTLE_ENDIAN);
    }
  }

}
