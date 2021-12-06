package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockSnapshot extends AbstractTzxBlock implements DataBlock, DeprecatedBlock {
  private final int type;
  private final byte[] data;

  public TzxBlockSnapshot(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.SNAPSHOT.getId());

    this.type = inputStream.read();
    final int length = readThreeByteValue(inputStream);
    this.data = inputStream.readByteArray(length);
  }

  public int getType() {
    return type;
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);

    outputStream.write(this.type);
    writeThreeByteValue(outputStream, this.data.length);
    outputStream.write(this.data);
  }
}
