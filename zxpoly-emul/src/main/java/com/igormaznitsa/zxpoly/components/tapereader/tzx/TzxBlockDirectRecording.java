package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockDirectRecording extends AbstractTzxBlock {

  private final int numberTstatesPerSample;
  private final int pauseAfterBlockMs;
  private final int usedBitsInLastByte;
  private final byte[] data;

  public TzxBlockDirectRecording(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.DIRECT_RECORDING_BLOCK.getId());

    this.numberTstatesPerSample = readWord(inputStream);
    this.pauseAfterBlockMs = readWord(inputStream);
    this.usedBitsInLastByte = inputStream.readByte();

    final int dataLength = readThreeByteValue(inputStream);
    this.data = inputStream.readByteArray(dataLength);
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.numberTstatesPerSample);
    writeWord(outputStream, this.pauseAfterBlockMs);
    outputStream.write(this.usedBitsInLastByte);
    writeThreeByteValue(outputStream, this.data.length);
    outputStream.write(this.data);
  }

  public int getNumberTstatesPerSample() {
    return numberTstatesPerSample;
  }

  public int getPauseAfterBlockMs() {
    return pauseAfterBlockMs;
  }

  public int getUsedBitsInLastByte() {
    return usedBitsInLastByte;
  }

  public byte[] getData() {
    return data;
  }
}
