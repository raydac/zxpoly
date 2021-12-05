package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockGlue extends AbstractTzxBlock {

  private final byte[] data;

  public TzxBlockGlue(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.GLUE.getId());
    this.data = inputStream.readByteArray(9);
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    outputStream.write(this.data);
  }
}
