package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;

import java.io.IOException;

public class TzxBlockSequenceReturn extends AbstractTzxBlock implements FlowManagementBlock {

  public TzxBlockSequenceReturn(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.RETURN_FROM_SEQUENCE.getId());
  }


}
