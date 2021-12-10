package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockSnapshot extends AbstractTzxBlock implements SoundDataBlock, DeprecatedBlock {
  private final int type;
  private final byte[] data;

  public TzxBlockSnapshot(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.SNAPSHOT.getId());

    this.type = inputStream.read();
    final int length = readThreeByteValue(inputStream);
    this.data = inputStream.readByteArray(length);
  }

  public int getType() {
    return type;
  }

  @Override
  public int getDataLength() {
    return this.data.length;
  }

  @Override
  public byte[] extractData() throws IOException {
    return this.data;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);

    outputStream.write(this.type);
    writeThreeByteValue(outputStream, this.data.length);
    outputStream.write(this.data);
  }
}
