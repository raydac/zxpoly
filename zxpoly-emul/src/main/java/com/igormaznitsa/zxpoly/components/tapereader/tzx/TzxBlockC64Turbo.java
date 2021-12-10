package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockC64Turbo extends AbstractTzxBlock implements DeprecatedBlock, SoundDataBlock {

  private final long blockLength;
  private final int zeroBitPulse;
  private final int oneBitPulse;
  private final int additionalBitsInBytes;
  private final int numberOfLeadInBytes;
  private final int leadInByte;
  private final int usedBitsInLastByte;
  private final int generalPurpose;
  private final int numberOfTrailingBytes;
  private final int trailingByte;
  private final int pauseAfterBlockInMs;
  private final byte[] data;

  public TzxBlockC64Turbo(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.C64TURBO.getId());

    this.blockLength = readDWord(inputStream);
    this.zeroBitPulse = readWord(inputStream);
    this.oneBitPulse = readWord(inputStream);
    this.additionalBitsInBytes = inputStream.readByte();
    this.numberOfLeadInBytes = readWord(inputStream);
    this.leadInByte = inputStream.readByte();
    this.usedBitsInLastByte = inputStream.readByte();
    this.generalPurpose = inputStream.readByte();
    this.numberOfTrailingBytes = readWord(inputStream);
    this.trailingByte = inputStream.readByte();
    this.pauseAfterBlockInMs = readWord(inputStream);

    final int length = readThreeByteValue(inputStream);
    this.data = inputStream.readByteArray(length);
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);

    writeDWord(outputStream, this.blockLength);
    writeWord(outputStream, this.zeroBitPulse);
    writeWord(outputStream, this.oneBitPulse);
    outputStream.write(this.additionalBitsInBytes);
    writeWord(outputStream, this.numberOfLeadInBytes);
    outputStream.write(this.leadInByte);
    outputStream.write(this.usedBitsInLastByte);
    outputStream.write(this.generalPurpose);
    writeWord(outputStream, this.numberOfTrailingBytes);
    outputStream.write(this.trailingByte);
    writeWord(outputStream, this.pauseAfterBlockInMs);

    writeThreeByteValue(outputStream, this.data.length);
    outputStream.write(this.data);
  }

  public long getBlockLength() {
    return blockLength;
  }

  public int getZeroBitPulse() {
    return zeroBitPulse;
  }

  public int getOneBitPulse() {
    return oneBitPulse;
  }

  public int getAdditionalBitsInBytes() {
    return additionalBitsInBytes;
  }

  public int getNumberOfLeadInBytes() {
    return numberOfLeadInBytes;
  }

  public int getLeadInByte() {
    return leadInByte;
  }

  public int getUsedBitsInLastByte() {
    return usedBitsInLastByte;
  }

  public int getGeneralPurpose() {
    return generalPurpose;
  }

  public int getNumberOfTrailingBytes() {
    return numberOfTrailingBytes;
  }

  public int getTrailingByte() {
    return trailingByte;
  }

  public int getPauseAfterBlockInMs() {
    return pauseAfterBlockInMs;
  }

  @Override
  public int getDataLength() {
    return this.data.length;
  }

  @Override
  public byte[] extractData() throws IOException {
    return this.data;
  }
}
