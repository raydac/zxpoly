package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockPureTone extends AbstractTzxSoundDataBlock {
  private final int lengthOnePulseTstates;
  private final int numberOfPulses;

  public TzxBlockPureTone(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.PURE_TONE.getId());
    this.lengthOnePulseTstates = readWord(inputStream);
    this.numberOfPulses = readWord(inputStream);
  }

  public int getLengthOfPulseInTstates() {
    return this.lengthOnePulseTstates;
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
    writeWord(outputStream, this.lengthOnePulseTstates);
    writeWord(outputStream, this.numberOfPulses);
  }
}
