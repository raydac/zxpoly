package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;

import java.io.IOException;

public class TzxBlockSequenceReturn extends AbstractTzxFlowManagementBlock {

  public TzxBlockSequenceReturn(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.RETURN_FROM_SEQUENCE.getId());
  }

  @Override
  public short[] getOffsets() {
    return ZERO;
  }
}
