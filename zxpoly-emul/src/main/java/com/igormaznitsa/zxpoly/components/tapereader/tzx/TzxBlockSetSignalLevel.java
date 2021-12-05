package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockSetSignalLevel extends AbstractTzxBlock {

  private final int signalLevel;

  public TzxBlockSetSignalLevel(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.SET_SIGNAL_LEVEL.getId());
    if (readDWord(inputStream) != 1L) throw new IOException("Unexpected length of set signal block");
    this.signalLevel = inputStream.readByte();
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeDWord(outputStream, 1L);
    outputStream.write(this.signalLevel);
  }

  public int getSignalLevel() {
    return signalLevel;
  }
}
