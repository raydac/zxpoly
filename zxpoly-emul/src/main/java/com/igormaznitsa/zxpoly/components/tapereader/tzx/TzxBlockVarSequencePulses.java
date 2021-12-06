package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockVarSequencePulses extends AbstractTzxBlock implements DataBlock {
  private final int[] pulsesLengths;

  public TzxBlockVarSequencePulses(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.VAR_SEQUENCE_PULSES.getId());
    final int pulses = inputStream.readByte();
    this.pulsesLengths = readWordArray(inputStream, pulses);
  }

  public int[] getPulsesLengths() {
    return pulsesLengths;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    outputStream.write(this.pulsesLengths.length);
    writeWordArray(outputStream, this.pulsesLengths);
  }
}
