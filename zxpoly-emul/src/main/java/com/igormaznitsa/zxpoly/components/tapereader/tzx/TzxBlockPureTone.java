package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockPureTone extends AbstractTzxSoundDataBlock {
  private final int lengthOnePlusTstates;
  private final int numberOfPulses;

  public TzxBlockPureTone(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.PURE_TONE.getId());
    this.lengthOnePlusTstates = readWord(inputStream);
    this.numberOfPulses = readWord(inputStream);
  }

  public int getLengthOfPulseInTstates() {
    return this.lengthOnePlusTstates;
  }

  public int getNumberOfPulses() {
    return this.numberOfPulses;
  }

  @Override
  public int getDataLength() {
    return this.numberOfPulses;
  }

  @Override
  public byte[] extractData() throws IOException {
    return new byte[0];
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.lengthOnePlusTstates);
    writeWord(outputStream, this.numberOfPulses);
  }
}
