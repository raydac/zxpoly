package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockEmulationinfo extends AbstractTzxBlock implements InformationBlock {

  private final int generalEmulationFlags;
  private final int screenRefreshDelay;
  private final int interruptFreqHz;
  private final int reserved;

  public TzxBlockEmulationinfo(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.EMULATION_INFO.getId());

    this.generalEmulationFlags = readWord(inputStream);
    this.screenRefreshDelay = inputStream.readByte();
    this.interruptFreqHz = readWord(inputStream);
    this.reserved = readThreeByteValue(inputStream);
  }

  public int getGeneralEmulationFlags() {
    return generalEmulationFlags;
  }

  public int getScreenRefreshDelay() {
    return screenRefreshDelay;
  }

  public int getInterruptFreqHz() {
    return interruptFreqHz;
  }

  public int getReserved() {
    return reserved;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);

    writeDWord(outputStream, this.generalEmulationFlags);
    outputStream.write(this.screenRefreshDelay);
    writeWord(outputStream, this.interruptFreqHz);
    writeThreeByteValue(outputStream, this.reserved);
  }
}
