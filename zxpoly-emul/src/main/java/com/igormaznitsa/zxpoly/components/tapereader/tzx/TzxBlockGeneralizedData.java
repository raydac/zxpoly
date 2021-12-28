package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOrder;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.zxpoly.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import static com.igormaznitsa.jbbp.utils.JBBPUtils.reverseBitsInByte;
import static com.igormaznitsa.zxpoly.utils.Utils.minimalRequiredBitsFor;

public class TzxBlockGeneralizedData extends AbstractTzxSoundDataBlock {

  private final long blockLength;
  private final int pauseAfterBlockMs;
  private final long totalNumberOfSymbolsInPilotSyncBlock;
  private final int maximumNumberOfPulsesPerPilotSyncSymbol;

  private final int numberOfPilotSyncSymbolsInAbcTable;

  private final SymDefRecord[] pilotAndSyncDefTable;
  private final PrleRecord[] pilotAndSyncDataStream;

  private final SymDefRecord[] dataSymbolsDefTable;
  private final byte[] dataStream;
  private final long totalNumberOfSymbolsInDataStream;
  private final int maximumNumberOfPulsesPerDataSymbol;
  private final int numberOfDataSymbolsInAbcTable;

  public TzxBlockGeneralizedData(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.GENERALIZED_DATA_BLOCK.getId());

    this.blockLength = readDWord(inputStream);
    this.pauseAfterBlockMs = readWord(inputStream);
    this.totalNumberOfSymbolsInPilotSyncBlock = readDWord(inputStream);
    this.maximumNumberOfPulsesPerPilotSyncSymbol = inputStream.readByte();

    final int readAsp = inputStream.readByte();
    this.numberOfPilotSyncSymbolsInAbcTable = readAsp == 0 ? 256 : readAsp;

    this.totalNumberOfSymbolsInDataStream = readDWord(inputStream);
    this.maximumNumberOfPulsesPerDataSymbol = inputStream.readByte();

    final int readAsd = inputStream.readByte();
    this.numberOfDataSymbolsInAbcTable = readAsd == 0 ? 256 : readAsd;

    if (this.totalNumberOfSymbolsInPilotSyncBlock > 0) {
      this.pilotAndSyncDefTable = new SymDefRecord[(int) this.numberOfPilotSyncSymbolsInAbcTable];
      for (int i = 0; i < this.pilotAndSyncDefTable.length; i++) {
        this.pilotAndSyncDefTable[i] = new SymDefRecord(this.maximumNumberOfPulsesPerPilotSyncSymbol, inputStream);
      }
      this.pilotAndSyncDataStream = new PrleRecord[(int) this.totalNumberOfSymbolsInPilotSyncBlock];
      for (int i = 0; i < this.pilotAndSyncDataStream.length; i++) {
        this.pilotAndSyncDataStream[i] = new PrleRecord(inputStream);
      }
    } else {
      this.pilotAndSyncDefTable = new SymDefRecord[0];
      this.pilotAndSyncDataStream = new PrleRecord[0];
    }

    if (this.totalNumberOfSymbolsInDataStream > 0) {
      this.dataSymbolsDefTable = new SymDefRecord[numberOfDataSymbolsInAbcTable];
      for (int i = 0; i < this.dataSymbolsDefTable.length; i++) {
        this.dataSymbolsDefTable[i] = new SymDefRecord(maximumNumberOfPulsesPerDataSymbol, inputStream);
      }

      final int bitsPerDataSymbol = Utils.minimalRequiredBitsFor(this.numberOfDataSymbolsInAbcTable - 1);
      final int expectedDataLength = (int) ((bitsPerDataSymbol * this.totalNumberOfSymbolsInDataStream) + 7) / 8;

      this.dataStream = inputStream.readByteArray(expectedDataLength);
    } else {
      this.dataSymbolsDefTable = new SymDefRecord[0];
      this.dataStream = new byte[0];
    }
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);

    writeDWord(outputStream, this.blockLength);
    writeWord(outputStream, this.pauseAfterBlockMs);

    writeDWord(outputStream, this.totalNumberOfSymbolsInPilotSyncBlock);
    outputStream.write(this.maximumNumberOfPulsesPerPilotSyncSymbol);
    outputStream.write(this.numberOfPilotSyncSymbolsInAbcTable == 256 ? 0 : this.numberOfPilotSyncSymbolsInAbcTable);

    writeDWord(outputStream, totalNumberOfSymbolsInDataStream);
    outputStream.write(maximumNumberOfPulsesPerDataSymbol);
    outputStream.write(numberOfDataSymbolsInAbcTable == 256 ? 0 : numberOfDataSymbolsInAbcTable);

    if (this.totalNumberOfSymbolsInPilotSyncBlock > 0L) {
      for (final SymDefRecord s : this.pilotAndSyncDefTable) {
        s.write(outputStream);
      }
      for (final PrleRecord p : this.pilotAndSyncDataStream) {
        p.write(outputStream);
      }
    }

    if (this.totalNumberOfSymbolsInDataStream > 0L) {
      for (final SymDefRecord s : this.dataSymbolsDefTable) {
        s.write(outputStream);
      }
      outputStream.write(this.dataStream);
    }
  }

  public long getTotalNumberOfSymbolsInPilotSyncBlock() {
    return totalNumberOfSymbolsInPilotSyncBlock;
  }

  public int getMaximumNumberOfPulsesPerPilotSyncSymbol() {
    return maximumNumberOfPulsesPerPilotSyncSymbol;
  }

  public int getNumberOfPilotSyncSymbolsInAbcTable() {
    return numberOfPilotSyncSymbolsInAbcTable;
  }

  public SymDefRecord[] getPilotAndSyncDefTable() {
    return pilotAndSyncDefTable;
  }

  public PrleRecord[] getPilotAndSyncDataStream() {
    return pilotAndSyncDataStream;
  }

  public SymDefRecord[] getDataSymbolsDefTable() {
    return dataSymbolsDefTable;
  }

  public byte[] getDataStream() {
    return dataStream;
  }

  public long getTotalNumberOfSymbolsInDataStream() {
    return totalNumberOfSymbolsInDataStream;
  }

  public int getMaximumNumberOfPulsesPerDataSymbol() {
    return maximumNumberOfPulsesPerDataSymbol;
  }

  public int getNumberOfDataSymbolsInAbcTable() {
    return numberOfDataSymbolsInAbcTable;
  }

  public long getBlockLength() {
    return this.blockLength;
  }

  public int getPauseAfterBlockMs() {
    return this.pauseAfterBlockMs;
  }

  @Override
  public int getDataLength() {
    return this.dataStream == null ? 0 : this.dataStream.length;
  }

  @Override
  public byte[] extractData() throws IOException {
    return this.dataStream == null ? new byte[0] : this.dataStream;
  }

  public boolean decodeRecordsAsPulses(final boolean nextPulseLevel, final PulseWriter pulseWriter) throws IOException {
    boolean pulseLevel = nextPulseLevel;

    if (this.totalNumberOfSymbolsInPilotSyncBlock > 0) {
      for (final PrleRecord record : this.pilotAndSyncDataStream) {
        for (int i = 0; i < record.numberOfRepetitions; i++) {
          pulseLevel = this.pilotAndSyncDefTable[record.symbol].decodeRecordsAsPulses(pulseLevel, pulseWriter);
        }
      }
    }

    if (this.totalNumberOfSymbolsInDataStream > 0) {
      final JBBPBitNumber charBits = JBBPBitNumber.decode(minimalRequiredBitsFor(this.numberOfDataSymbolsInAbcTable - 1));
      final JBBPBitInputStream inputStream = new JBBPBitInputStream(new ByteArrayInputStream(this.dataStream), JBBPBitOrder.MSB0);

      for (int i = 0; i < this.totalNumberOfSymbolsInDataStream; i++) {
        final int symbolIndex = reverseBitsInByte(charBits, (byte) inputStream.readBits(charBits)) & 0xFF;
        final SymDefRecord pulseRecord = this.dataSymbolsDefTable[symbolIndex];
        pulseLevel = pulseRecord.decodeRecordsAsPulses(pulseLevel, pulseWriter);
      }
    }

    return pulseLevel;
  }

  public static final class PrleRecord {
    private final int symbol;
    private final int numberOfRepetitions;

    private PrleRecord(final JBBPBitInputStream inputStream) throws IOException {
      this.symbol = inputStream.readByte();
      this.numberOfRepetitions = readWord(inputStream);
    }

    public void write(final JBBPBitOutputStream outputStream) throws IOException {
      outputStream.write(this.symbol);
      writeWord(outputStream, this.numberOfRepetitions);
    }

    public int getSymbol() {
      return symbol;
    }

    public int getNumberOfRepetitions() {
      return numberOfRepetitions;
    }
  }

  public static final class SymDefRecord {
    public static final int POLARITY_OPPOSITE = 0;
    public static final int POLARITY_SAME = 1;
    public static final int POLARITY_LOW = 2;
    public static final int POLARITY_HIGH = 3;
    private final int symbolFlags;
    private final int[] pulseLengths;

    private SymDefRecord(final int maxp, final JBBPBitInputStream inputStream) throws IOException {
      this.symbolFlags = inputStream.readByte();
      this.pulseLengths = readWordArray(inputStream, maxp);
    }

    public int getPolarity() {
      return this.symbolFlags & 3;
    }

    private boolean decodeRecordsAsPulses(final boolean nextPulseLevel, final PulseWriter pulseWriter) throws IOException {
      boolean pulseLevel = nextPulseLevel;
      switch (this.getPolarity()) {
        case POLARITY_OPPOSITE:
          break;
        case POLARITY_SAME:
          pulseLevel = !pulseLevel;
          break;
        case POLARITY_LOW:
          pulseLevel = false;
          break;
        case POLARITY_HIGH:
          pulseLevel = true;
          break;
      }

      for (final int pulses : this.pulseLengths) {
        if (pulses > 0) {
          pulseWriter.writePulse(pulses, pulseLevel);
          pulseLevel = !pulseLevel;
        } else {
          break;
        }
      }
      return pulseLevel;
    }

    public void write(final JBBPBitOutputStream outputStream) throws IOException {
      outputStream.write(this.symbolFlags);
      writeWordArray(outputStream, this.pulseLengths);
    }

    public int getSymbolFlags() {
      return symbolFlags;
    }

    public int[] getPulseLengths() {
      return pulseLengths;
    }
  }
}
