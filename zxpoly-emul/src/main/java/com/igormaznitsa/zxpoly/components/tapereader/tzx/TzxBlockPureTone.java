package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockPureTone extends AbstractTzxBlock implements DataBlock {
  private final int lengthOnePluseTstates;
  private final int numberOfPulses;

  public TzxBlockPureTone(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.PURE_TONE.getId());
    this.lengthOnePluseTstates = readWord(inputStream);
    this.numberOfPulses = readWord(inputStream);
  }

  public int getLengthOfPulseInTstates() {
    return this.lengthOnePluseTstates;
  }

  public int getNumberOfPulses() {
    return this.numberOfPulses;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.lengthOnePluseTstates);
    writeWord(outputStream, this.numberOfPulses);
  }
}
