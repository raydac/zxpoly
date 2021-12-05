package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockTurboSpeedData extends AbstractTzxBlock {
  private final int pauseAfterBlockMs;
  private final byte[] data;
  private final int lengthPilotPulse;
  private final int lengthSyncFirstPulse;
  private final int lengthSyncSecondPulse;
  private final int lengthZeroBitPulse;
  private final int lengthOneBitPulse;
  private final int lengthPilotTone;
  private final int usedBitsInLastByte;

  public TzxBlockTurboSpeedData(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.TURBO_SPEED_DATA_BLOCK.getId());

    this.lengthPilotPulse = readWord(inputStream);
    this.lengthSyncFirstPulse = readWord(inputStream);
    this.lengthSyncSecondPulse = readWord(inputStream);
    this.lengthZeroBitPulse = readWord(inputStream);
    this.lengthOneBitPulse = readWord(inputStream);
    this.lengthPilotTone = readWord(inputStream);
    this.usedBitsInLastByte = inputStream.readByte();

    this.pauseAfterBlockMs = readWord(inputStream);
    final int dataLength = readThreeByteValue(inputStream);
    this.data = inputStream.readByteArray(dataLength);
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.lengthPilotPulse);
    writeWord(outputStream, this.lengthSyncFirstPulse);
    writeWord(outputStream, this.lengthSyncSecondPulse);
    writeWord(outputStream, this.lengthZeroBitPulse);
    writeWord(outputStream, this.lengthOneBitPulse);
    writeWord(outputStream, this.lengthPilotTone);
    outputStream.write(this.usedBitsInLastByte);
    writeWord(outputStream, this.pauseAfterBlockMs);
    writeThreeByteValue(outputStream, this.data.length);
    outputStream.write(this.data);
  }

  public int getPauseAfterBlockMs() {
    return pauseAfterBlockMs;
  }

  public byte[] getData() {
    return data;
  }

  public int getLengthPilotPulse() {
    return lengthPilotPulse;
  }

  public int getLengthSyncFirstPulse() {
    return lengthSyncFirstPulse;
  }

  public int getLengthSyncSecondPulse() {
    return lengthSyncSecondPulse;
  }

  public int getLengthZeroBitPulse() {
    return lengthZeroBitPulse;
  }

  public int getLengthOneBitPulse() {
    return lengthOneBitPulse;
  }

  public int getLengthPilotTone() {
    return lengthPilotTone;
  }

  public int getUsedBitsInLastByte() {
    return usedBitsInLastByte;
  }
}
