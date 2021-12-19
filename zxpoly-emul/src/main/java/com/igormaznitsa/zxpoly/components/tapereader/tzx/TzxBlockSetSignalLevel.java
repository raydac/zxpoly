package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockSetSignalLevel extends AbstractTzxBlock implements SoundDataBlock {

  private final int signalLevel;

  public TzxBlockSetSignalLevel(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.SET_SIGNAL_LEVEL.getId());
    if (readDWord(inputStream) != 1L) throw new IOException("Unexpected length of set signal block");
    this.signalLevel = inputStream.readByte();
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeDWord(outputStream, 1L);
    outputStream.write(this.signalLevel);
  }

  @Override
  public int getDataLength() {
    return 1;
  }

  public int getLevel() {
    return this.signalLevel;
  }

  @Override
  public byte[] extractData() throws IOException {
    return new byte[]{(byte) this.signalLevel};
  }

  public int getSignalLevel() {
    return signalLevel;
  }
}
