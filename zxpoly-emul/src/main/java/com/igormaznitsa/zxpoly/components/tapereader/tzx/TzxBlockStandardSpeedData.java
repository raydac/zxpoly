package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockStandardSpeedData extends AbstractTzxBlock implements SoundDataBlock {
  private final int pauseAfterBlockMs;
  private final byte[] data;

  public TzxBlockStandardSpeedData(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.STANDARD_SPEED_DATA_BLOCK.getId());
    this.pauseAfterBlockMs = readWord(inputStream);
    final int dataLength = readWord(inputStream);
    this.data = inputStream.readByteArray(dataLength);
  }

  @Override
  public byte[] extractData() throws IOException {
    return this.data;
  }

  public int getPauseAfterBlockMs() {
    return this.pauseAfterBlockMs;
  }

  @Override
  public int getDataLength() {
    return this.data.length;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.pauseAfterBlockMs);
    writeWord(outputStream, this.data.length);
    outputStream.write(this.data);
  }
}
