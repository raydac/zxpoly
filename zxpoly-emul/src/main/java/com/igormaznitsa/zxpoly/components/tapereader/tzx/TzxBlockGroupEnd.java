package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;

import java.io.IOException;

public class TzxBlockGroupEnd extends AbstractTzxBlock implements InformationBlock {

  public TzxBlockGroupEnd(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.GROUP_END.getId());
  }


}
