package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import java.io.IOException;

public abstract class AbstractTzxSoundDataBlock extends AbstractTzxBlock {

  public AbstractTzxSoundDataBlock(final int id) {
    super(id);
  }

  public abstract int getDataLength();

  public abstract byte[] extractData() throws IOException;
}
