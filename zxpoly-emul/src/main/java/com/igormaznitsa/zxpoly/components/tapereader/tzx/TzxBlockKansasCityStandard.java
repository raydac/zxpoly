package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockKansasCityStandard extends AbstractTzxBlock implements DataBlock {

  private final long blockLength;
  private final int pauseAfterBlockInMs;
  private final int durationPilotPulseTstates;
  private final int pilotTonePulsesNumber;
  private final int durationZeroPulseTstates;
  private final int durationOnePulseTstates;
  private final int numberOfPulsesInBit;
  private final int flags;
  private final byte[] data;

  public TzxBlockKansasCityStandard(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.KANSAS_CITY_STANDARD.getId());

    this.blockLength = readDWord(inputStream);
    this.pauseAfterBlockInMs = readWord(inputStream);
    this.durationPilotPulseTstates = readWord(inputStream);
    this.pilotTonePulsesNumber = readWord(inputStream);
    this.durationZeroPulseTstates = readWord(inputStream);
    this.durationOnePulseTstates = readWord(inputStream);
    this.numberOfPulsesInBit = inputStream.readByte();
    this.flags = inputStream.readByte();

    this.data = inputStream.readByteArray((int) (this.blockLength - 12L));
  }

  public int getNumberOfZeroPulsesInZeroBit() {
    return this.numberOfPulsesInBit >>> 4;
  }

  public int getNumberOfOnePulsesInOneBit() {
    return this.numberOfPulsesInBit & 0x0F;
  }

  public int getNumberOfLeadingBits() {
    return (this.flags >>> 6) & 3;
  }

  public int getValueOfLeadingBits() {
    return (this.flags >>> 5) & 1;
  }

  public int getNumberOfTrailingBits() {
    return (this.flags >>> 3) & 3;
  }

  public int getValueOfTrailingBits() {
    return (this.flags >>> 2) & 1;
  }

  public JBBPBitOrder getEndianness() {
    return (this.flags & 1) == 0 ? JBBPBitOrder.LSB0 : JBBPBitOrder.MSB0;
  }

  public long getBlockLength() {
    return blockLength;
  }

  public int getPauseAfterBlockInMs() {
    return pauseAfterBlockInMs;
  }

  public int getDurationPilotPulseTstates() {
    return durationPilotPulseTstates;
  }

  public int getPilotTonePulsesNumber() {
    return pilotTonePulsesNumber;
  }

  public int getDurationZeroPulseTstates() {
    return durationZeroPulseTstates;
  }

  public int getDurationOnePulseTstates() {
    return durationOnePulseTstates;
  }

  public int getNumberOfPulsesInBit() {
    return numberOfPulsesInBit;
  }

  public int getFlags() {
    return flags;
  }

  @Override
  public int getDataLength() {
    return this.data.length;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);

    writeDWord(outputStream, this.blockLength);
    writeWord(outputStream, this.pauseAfterBlockInMs);
    writeWord(outputStream, this.durationPilotPulseTstates);
    writeWord(outputStream, this.pilotTonePulsesNumber);
    writeWord(outputStream, this.durationZeroPulseTstates);
    writeWord(outputStream, this.durationOnePulseTstates);
    outputStream.write(this.numberOfPulsesInBit);
    outputStream.write(this.flags);
    outputStream.write(this.data);
  }
}
