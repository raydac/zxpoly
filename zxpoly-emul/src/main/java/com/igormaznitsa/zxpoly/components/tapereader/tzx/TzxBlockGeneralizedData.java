package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockGeneralizedData extends AbstractTzxBlock {

  private final long blockLength;
  private final int pauseAfterBlockMs;
  private final long totalNumberOfSymbolsInPilotSyncBlock;
  private final int maximumNumberOfPulsesPerPilotSyncSymbol;

  private final int numberOfPilotSyncSymbolsInAbcTable;

  private final Symdef[] pilotAndSyncDefTable;
  private final Prle[] pilotAndSyncDataStream;

  private final Symdef[] dataSymbolsDefTable;
  private final byte[] dataStream;
  private final long totalNumberOfSymbolsInDataStream;
  private final int maximumNumberOfPulsesPerDataSymbol;
  private final int numberOfDataSymbolsInAbcTable;

  public TzxBlockGeneralizedData(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.GENERALIZED_DATA_BLOCK.getId());

    this.blockLength = readDWord(inputStream);
    this.pauseAfterBlockMs = readWord(inputStream);
    this.totalNumberOfSymbolsInPilotSyncBlock = readDWord(inputStream);
    this.maximumNumberOfPulsesPerPilotSyncSymbol = inputStream.readByte();

    final int readAsp = inputStream.readByte();
    this.numberOfPilotSyncSymbolsInAbcTable = readAsp == 0 ? 256 : readAsp;

    this.totalNumberOfSymbolsInDataStream = readDWord(inputStream);
    this.maximumNumberOfPulsesPerDataSymbol = inputStream.readByte();
    this.numberOfDataSymbolsInAbcTable = inputStream.readByte();

    if (this.totalNumberOfSymbolsInPilotSyncBlock > 0) {
      this.pilotAndSyncDefTable = new Symdef[(int) this.numberOfPilotSyncSymbolsInAbcTable];
      for (int i = 0; i < this.pilotAndSyncDefTable.length; i++) {
        this.pilotAndSyncDefTable[i] = new Symdef(this.maximumNumberOfPulsesPerPilotSyncSymbol, inputStream);
      }
      this.pilotAndSyncDataStream = new Prle[(int) this.totalNumberOfSymbolsInPilotSyncBlock];
      for (int i = 0; i < this.pilotAndSyncDataStream.length; i++) {
        this.pilotAndSyncDataStream[i] = new Prle(inputStream);
      }
    } else {
      this.pilotAndSyncDefTable = new Symdef[0];
      this.pilotAndSyncDataStream = new Prle[0];
    }

    if (totalNumberOfSymbolsInDataStream > 0) {
      final long restSectionLength = this.blockLength - (0x12
              + (this.totalNumberOfSymbolsInPilotSyncBlock > 0L ? 1 : 0) * ((2L * this.maximumNumberOfPulsesPerPilotSyncSymbol + 1) * readAsp)
              + this.totalNumberOfSymbolsInPilotSyncBlock * 3L + (2L * maximumNumberOfPulsesPerDataSymbol + 1) * numberOfDataSymbolsInAbcTable) + 4;

      this.dataSymbolsDefTable = new Symdef[numberOfDataSymbolsInAbcTable];
      for (int i = 0; i < this.dataSymbolsDefTable.length; i++) {
        this.dataSymbolsDefTable[i] = new Symdef(maximumNumberOfPulsesPerDataSymbol, inputStream);
      }
      this.dataStream = inputStream.readByteArray((int) restSectionLength);
    } else {
      this.dataSymbolsDefTable = new Symdef[0];
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
      for (final Symdef s : this.pilotAndSyncDefTable) {
        s.write(outputStream);
      }
      for (final Prle p : this.pilotAndSyncDataStream) {
        p.write(outputStream);
      }
    }

    if (this.totalNumberOfSymbolsInDataStream > 0L) {
      for (final Symdef s : this.dataSymbolsDefTable) {
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

  public Symdef[] getPilotAndSyncDefTable() {
    return pilotAndSyncDefTable;
  }

  public Prle[] getPilotAndSyncDataStream() {
    return pilotAndSyncDataStream;
  }

  public Symdef[] getDataSymbolsDefTable() {
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

  public static final class Prle {
    private final int symbol;
    private final int numberOfRepetitions;

    private Prle(final JBBPBitInputStream inputStream) throws IOException {
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

  public static final class Symdef {
    private final int symbolFlags;
    private final int[] pulseLengths;

    private Symdef(final int maxp, final JBBPBitInputStream inputStream) throws IOException {
      this.symbolFlags = inputStream.readByte();
      this.pulseLengths = readWordArray(inputStream, maxp);
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
