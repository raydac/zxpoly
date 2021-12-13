package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockPauseOrStop extends AbstractTzxBlock implements SoundDataBlock {

  private final int pauseDurationMs;

  public TzxBlockPauseOrStop(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.PAUSE_OR_STOP.getId());
    this.pauseDurationMs = readWord(inputStream);
  }

  @Override
  public int getDataLength() {
    return 0;
  }

  @Override
  public byte[] extractData() throws IOException {
    return new byte[0];
  }

  public int getPauseDurationMs() {
    return this.pauseDurationMs;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.pauseDurationMs);
  }
}
