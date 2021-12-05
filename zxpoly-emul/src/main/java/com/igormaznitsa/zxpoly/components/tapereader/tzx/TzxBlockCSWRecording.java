package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockCSWRecording extends AbstractTzxBlock {

  private final long blockLength;
  private final int pauseAfterBlockMs;
  private final int samplingRate;
  private final int compressionType;
  private final long numberStoredPulses;
  private final byte[] data;

  public TzxBlockCSWRecording(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.CSW_RECORDING_BLOCK.getId());

    this.blockLength = readDWord(inputStream);
    this.pauseAfterBlockMs = readWord(inputStream);
    this.samplingRate = readThreeByteValue(inputStream);
    this.compressionType = inputStream.readByte();
    this.numberStoredPulses = readDWord(inputStream);
    this.data = inputStream.readByteArray((int) (blockLength - 10));
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeDWord(outputStream, this.blockLength);
    writeWord(outputStream, this.pauseAfterBlockMs);
    writeThreeByteValue(outputStream, this.samplingRate);
    outputStream.write(this.compressionType);
    writeDWord(outputStream, this.numberStoredPulses);
    outputStream.write(this.data);
  }

  public long getBlockLength() {
    return blockLength;
  }

  public int getPauseAfterBlockMs() {
    return pauseAfterBlockMs;
  }

  public int getSamplingRate() {
    return samplingRate;
  }

  public int getCompressionType() {
    return compressionType;
  }

  public long getNumberStoredPulses() {
    return numberStoredPulses;
  }

  public byte[] getData() {
    return data;
  }
}
