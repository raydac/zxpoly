package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockPauseOrStop extends AbstractTzxBlock implements FlowManagementBlock {

  private final int pauseDurationMs;

  public TzxBlockPauseOrStop(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.PAUSE_OR_STOP.getId());
    this.pauseDurationMs = readWord(inputStream);
  }

  public int getPauseDurationMs() {
    return pauseDurationMs;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.pauseDurationMs);
  }
}
