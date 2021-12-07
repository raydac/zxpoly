package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockLoopStart extends AbstractTzxBlock implements FlowManagementBlock {

  private final int repetitions;

  public TzxBlockLoopStart(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.LOOP_START.getId());
    this.repetitions = readWord(inputStream);
  }

  public int getRepetitions() {
    return repetitions;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.repetitions);
  }
}
