package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;

import java.io.IOException;

public class TzxBlockLoopEnd extends AbstractTzxFlowManagementBlock {

  public TzxBlockLoopEnd(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.LOOP_END.getId());
  }

  @Override
  public short[] getOffsets() {
    return ZERO;
  }
}
