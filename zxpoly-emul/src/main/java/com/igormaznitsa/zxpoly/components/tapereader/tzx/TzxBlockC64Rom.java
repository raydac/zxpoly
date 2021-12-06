package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockC64Rom extends AbstractTzxBlock implements DeprecatedBlock, DataBlock {

  private final long blockLength;
  private final int pilotTonePulseEndAddress;
  private final int numberOfWavesInPilotTone;
  private final int syncFirstWavePulseEndAddress;
  private final int syncSecondWavePulseEndAddress;
  private final int zeroBitFirstWavePulseEndAddress;
  private final int zeroBitSecondWavePulseEndAddress;
  private final int oneBitFirstWavePulseEndAddress;
  private final int oneBitSecondWavePulseEndAddress;
  private final int xorChecksumForDataByte;
  private final int finishByteFirstWavePulseEnndAddress;
  private final int finishByteSecondWavePulseEnndAddress;
  private final int finishDataFirstWavePulseEnndAddress;
  private final int finishDataSecondWavePulseEnndAddress;
  private final int trailingTonePulseEndAddress;
  private final int usedBitsInLastByte;
  private final int generalPurpose;
  private final int pauseAfterBlockInMs;
  private final byte[] data;

  public TzxBlockC64Rom(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.C64ROM.getId());

    this.blockLength = readDWord(inputStream);
    this.pilotTonePulseEndAddress = readWord(inputStream);
    this.numberOfWavesInPilotTone = readWord(inputStream);
    this.syncFirstWavePulseEndAddress = readWord(inputStream);
    this.syncSecondWavePulseEndAddress = readWord(inputStream);
    this.zeroBitFirstWavePulseEndAddress = readWord(inputStream);
    this.zeroBitSecondWavePulseEndAddress = readWord(inputStream);
    this.oneBitFirstWavePulseEndAddress = readWord(inputStream);
    this.oneBitSecondWavePulseEndAddress = readWord(inputStream);
    this.xorChecksumForDataByte = inputStream.readByte();
    this.finishByteFirstWavePulseEnndAddress = readWord(inputStream);
    this.finishByteSecondWavePulseEnndAddress = readWord(inputStream);
    this.finishDataFirstWavePulseEnndAddress = readWord(inputStream);
    this.finishDataSecondWavePulseEnndAddress = readWord(inputStream);
    this.trailingTonePulseEndAddress = readWord(inputStream);
    this.usedBitsInLastByte = inputStream.readByte();
    this.generalPurpose = inputStream.readByte();
    this.pauseAfterBlockInMs = readWord(inputStream);

    final int length = readThreeByteValue(inputStream);
    this.data = inputStream.readByteArray(length);
  }

  public long getBlockLength() {
    return blockLength;
  }

  public int getPilotTonePulseEndAddress() {
    return pilotTonePulseEndAddress;
  }

  public int getNumberOfWavesInPilotTone() {
    return numberOfWavesInPilotTone;
  }

  public int getSyncFirstWavePulseEndAddress() {
    return syncFirstWavePulseEndAddress;
  }

  public int getSyncSecondWavePulseEndAddress() {
    return syncSecondWavePulseEndAddress;
  }

  public int getZeroBitFirstWavePulseEndAddress() {
    return zeroBitFirstWavePulseEndAddress;
  }

  public int getZeroBitSecondWavePulseEndAddress() {
    return zeroBitSecondWavePulseEndAddress;
  }

  public int getOneBitFirstWavePulseEndAddress() {
    return oneBitFirstWavePulseEndAddress;
  }

  public int getOneBitSecondWavePulseEndAddress() {
    return oneBitSecondWavePulseEndAddress;
  }

  public int getXorChecksumForDataByte() {
    return xorChecksumForDataByte;
  }

  public int getFinishByteFirstWavePulseEnndAddress() {
    return finishByteFirstWavePulseEnndAddress;
  }

  public int getFinishByteSecondWavePulseEnndAddress() {
    return finishByteSecondWavePulseEnndAddress;
  }

  public int getFinishDataFirstWavePulseEnndAddress() {
    return finishDataFirstWavePulseEnndAddress;
  }

  public int getFinishDataSecondWavePulseEnndAddress() {
    return finishDataSecondWavePulseEnndAddress;
  }

  public int getTrailingTonePulseEndAddress() {
    return trailingTonePulseEndAddress;
  }

  public int getUsedBitsInLastByte() {
    return usedBitsInLastByte;
  }

  public int getGeneralPurpose() {
    return generalPurpose;
  }

  public int getPauseAfterBlockInMs() {
    return pauseAfterBlockInMs;
  }

  public byte[] getData() {
    return data;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);

    writeDWord(outputStream, this.blockLength);
    writeWord(outputStream, this.pilotTonePulseEndAddress);
    writeWord(outputStream, this.numberOfWavesInPilotTone);
    writeWord(outputStream, this.syncFirstWavePulseEndAddress);
    writeWord(outputStream, this.syncSecondWavePulseEndAddress);
    writeWord(outputStream, this.zeroBitFirstWavePulseEndAddress);
    writeWord(outputStream, this.zeroBitSecondWavePulseEndAddress);
    writeWord(outputStream, this.oneBitFirstWavePulseEndAddress);
    writeWord(outputStream, this.oneBitSecondWavePulseEndAddress);
    outputStream.write(this.xorChecksumForDataByte);
    writeWord(outputStream, this.finishByteFirstWavePulseEnndAddress);
    writeWord(outputStream, this.finishByteSecondWavePulseEnndAddress);
    writeWord(outputStream, this.finishDataFirstWavePulseEnndAddress);
    writeWord(outputStream, this.finishDataSecondWavePulseEnndAddress);
    writeWord(outputStream, this.trailingTonePulseEndAddress);
    outputStream.write(this.usedBitsInLastByte);
    outputStream.write(this.generalPurpose);
    writeWord(outputStream, this.pauseAfterBlockInMs);

    writeThreeByteValue(outputStream, this.data.length);
    outputStream.write(this.data);
  }
}
