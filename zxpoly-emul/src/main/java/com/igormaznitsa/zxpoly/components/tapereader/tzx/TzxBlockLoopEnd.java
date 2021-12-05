package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;

import java.io.IOException;

public class TzxBlockLoopEnd extends AbstractTzxBlock {

  public TzxBlockLoopEnd(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.LOOP_END.getId());
  }


}
