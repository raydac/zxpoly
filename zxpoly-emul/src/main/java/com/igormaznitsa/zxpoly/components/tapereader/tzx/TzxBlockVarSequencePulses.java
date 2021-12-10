package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockVarSequencePulses extends AbstractTzxBlock implements SoundDataBlock {
  private final int[] pulsesLengths;

  public TzxBlockVarSequencePulses(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.VAR_SEQUENCE_PULSES.getId());
    final int pulses = inputStream.readByte();
    this.pulsesLengths = readWordArray(inputStream, pulses);
  }

  public int[] getPulsesLengths() {
    return pulsesLengths;
  }

  @Override
  public byte[] extractData() throws IOException {
    return new byte[0];
  }

  @Override
  public int getDataLength() {
    return this.pulsesLengths.length * 2;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    outputStream.write(this.pulsesLengths.length);
    writeWordArray(outputStream, this.pulsesLengths);
  }
}
