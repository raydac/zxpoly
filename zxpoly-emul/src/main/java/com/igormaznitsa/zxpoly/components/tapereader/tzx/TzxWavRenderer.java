package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.zxpoly.utils.SpectrumUtils;
import com.igormaznitsa.zxpoly.utils.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

public class TzxWavRenderer {
  public static final int WAV_HEADER_LENGTH = 44;
  private static final int IMPULSNUMBER_PILOT_HEADER = 8063;
  private static final int IMPULSNUMBER_PILOT_DATA = 3223;
  private static final int PULSELEN_PILOT = 2168;
  private static final int PULSELEN_SYNC1 = 667;
  private static final int PULSELEN_SYNC2 = 735;
  private static final int PULSELEN_SYNC3 = 954;
  private static final int PULSELEN_ZERO = 855;
  private static final int PULSELEN_ONE = 1710;
  private static final int SIGNAL_HI = 0xFE;
  private static final int SIGNAL_LOW = 0x01;
  private static final int TSTATES_PER_SECOND = 3_500_000;

  private final TzxFile tzxFile;
  private final Freq freq;
  private final List<Repeat> repeatStack = new ArrayList<>();
  private final List<List<Integer>> callStack = new ArrayList<>();
  private final double tstatesPerSample;
  private final Logger logger;

  public TzxWavRenderer(final Freq freq, final TzxFile tzxFile, final Logger logger) {
    this.logger = logger;
    this.freq = Objects.requireNonNull(freq);
    this.tzxFile = Objects.requireNonNull(tzxFile);
    this.tstatesPerSample = (double) this.freq.getFreq() / (double) TSTATES_PER_SECOND;
  }

  private static String extractNameFromTapHeader(final boolean turbo, final byte[] data) {
    final byte[] name = new byte[10];
    if (data.length < 12) return "<UNKNOWN>";
    System.arraycopy(data, turbo ? 1 : 2, name, 0, name.length);

    final StringBuilder result = new StringBuilder();

    for (final char c : new String(name, StandardCharsets.ISO_8859_1).toCharArray()) {
      result.append(Character.isISOControl(c) ? ' ' : c);
    }
    return SpectrumUtils.fromZxString(result.toString());
  }

  public synchronized RenderResult render() throws IOException {
    final List<RenderResult.NamedOffsets> namedOffsets = new ArrayList<>();

    this.repeatStack.clear();
    this.callStack.clear();

    final DataStream dataStream = new DataStream(this.freq, 1024 * 1024);
    final List<AbstractTzxBlock> blockList = this.tzxFile.getBlockList();

    boolean nextLevel = true;

    int blockPointer = 0;
    while (blockPointer < blockList.size()) {
      final AbstractTzxBlock block = blockList.get(blockPointer);

      if (block instanceof AbstractTzxSystemBlock) {
        blockPointer++;
      } else if (block instanceof ITzxBlock) {
        final ITzxBlock interfaceBlock = (ITzxBlock) block;
        RenderResult.NamedOffsets namedOffset = interfaceBlock.renderBlockProcess(this.logger, dataStream.getCounter());
        if (namedOffset!=null){
          namedOffsets.add(namedOffset);
        }
        blockPointer++;
      } else if (block instanceof AbstractTzxFlowManagementBlock) {
        final short[] offsets = ((AbstractTzxFlowManagementBlock) block).getOffsets();
        if (block instanceof TzxBlockCallSequence) {
          final TzxBlockCallSequence callSequence = (TzxBlockCallSequence) block;
          final List<Integer> callIndexes = new ArrayList<>();
          for (final short s : callSequence.getOffsets()) {
            callIndexes.add(blockPointer + s);
          }
          callIndexes.add(blockPointer + 1);
          addCallSeq(callIndexes);
          blockPointer = this.nextCallIndex();
        } else if (block instanceof TzxBlockSequenceReturn) {
          blockPointer = this.nextCallIndex();
        } else if (block instanceof TzxBlockJumpTo) {
          final short offset = offsets[0];
          if (offset == 0) {
            throw new IOException("Detected jump block with zero offset");
          }
          blockPointer += offset;
        } else if (block instanceof TzxBlockLoopStart) {
          final TzxBlockLoopStart startBlock = (TzxBlockLoopStart) block;
          if (startBlock.getRepetitions() <= 0) {
            throw new IOException("Detected zero repetitions");
          }
          this.repeatStack.add(new Repeat(blockPointer, startBlock.getRepetitions() - 1));
          blockPointer++;
        } else if (block instanceof TzxBlockLoopEnd) {
          if (this.repeatStack.isEmpty()) {
            throw new IOException("Unexpected block loop end");
          } else {
            final Repeat lastStart = this.repeatStack.get(this.repeatStack.size() - 1);
            if (lastStart.isZero()) {
              blockPointer++;
            } else {
              lastStart.dec();
              this.removeAllRepeatAfter(lastStart.blockIndex);
              blockPointer = lastStart.blockIndex + 1;
            }
          }
        } else {
          throw new Error("Unexpected management block type: " + block.getClass().getSimpleName());
        }
      } else if (block instanceof AbstractTzxSoundDataBlock) {
        if (block instanceof TzxBlockSetSignalLevel) {
          final TzxBlockSetSignalLevel dataBlock = (TzxBlockSetSignalLevel) block;
          namedOffsets.add(new RenderResult.NamedOffsets("...set.level [" + dataBlock.getLevel() + "]...", WAV_HEADER_LENGTH + dataStream.getCounter()));
          nextLevel = dataBlock.getLevel() > 0;
          blockPointer++;
        } else if (block instanceof TzxBlockStopTapeIf48k) {
          namedOffsets.add(new RenderResult.NamedOffsets("-==STOP TAPE IF ZX48==-", WAV_HEADER_LENGTH + dataStream.getCounter()));
          nextLevel = writePause(nextLevel, dataStream, Duration.ofSeconds(1), DataType.PAUSE);
          nextLevel = writePause(nextLevel, dataStream, Duration.ofSeconds(4), DataType.STOP_TAPE_IF_ZX48);
          blockPointer++;
        } else if (block instanceof TzxBlockPauseOrStop) {
          final TzxBlockPauseOrStop dataBlock = (TzxBlockPauseOrStop) block;

          Duration duration = Duration.ofMillis(dataBlock.getPauseDurationMs());

          if (duration.isZero()) {
            namedOffsets.add(new RenderResult.NamedOffsets("-==STOP TAPE==-", WAV_HEADER_LENGTH + dataStream.getCounter()));
            nextLevel = writePause(nextLevel, dataStream, Duration.ofSeconds(1), DataType.PAUSE);
            nextLevel = writePause(nextLevel, dataStream, Duration.ofSeconds(4), DataType.STOP_TAPE);
          } else {
            namedOffsets.add(new RenderResult.NamedOffsets("...stop-pause [" + dataBlock.getPauseDurationMs() + " ms]", WAV_HEADER_LENGTH + dataStream.getCounter()));
            nextLevel = writePause(nextLevel, dataStream, duration, DataType.PAUSE);
          }

          blockPointer++;
        } else if (block instanceof TzxBlockStandardSpeedData) {
          final TzxBlockStandardSpeedData dataBlock = (TzxBlockStandardSpeedData) block;

          if (dataBlock.getDataLength() == 0) {
            this.logger.warning("Detected zero-length standard speed data block");
          } else {
            final byte[] tapData = dataBlock.extractData();
            final int flag = tapData[0] & 0xFF;

            if (flag < 128) {
              namedOffsets.add(new RenderResult.NamedOffsets(extractNameFromTapHeader(false, tapData), WAV_HEADER_LENGTH + dataStream.getCounter()));
            } else {
              namedOffsets.add(new RenderResult.NamedOffsets("...std.data...", WAV_HEADER_LENGTH + dataStream.getCounter()));
            }

            nextLevel = this.writeTapData(
                    namedOffsets,
                    nextLevel,
                    dataStream,
                    flag < 128 ? IMPULSNUMBER_PILOT_HEADER : IMPULSNUMBER_PILOT_DATA,
                    PULSELEN_PILOT,
                    PULSELEN_SYNC1,
                    PULSELEN_SYNC2,
                    PULSELEN_ZERO,
                    PULSELEN_ONE,
                    8,
                    Duration.ofMillis(dataBlock.getPauseAfterBlockMs()),
                    tapData,
                    DataType.STD_PILOT,
                    DataType.STD_SYNC1,
                    DataType.STD_SYNC2,
                    DataType.STD_DATA);
          }

          blockPointer++;
        } else if (block instanceof TzxBlockTurboSpeedData) {
          final TzxBlockTurboSpeedData dataBlock = (TzxBlockTurboSpeedData) block;

          if (dataBlock.getDataLength() == 0) {
            this.logger.warning("Detected zero-length turbo speed data block");
          } else {
            final byte[] tapData = dataBlock.extractData();
            final int flag = tapData[0] & 0xFF;

            if (flag < 128) {
              namedOffsets.add(new RenderResult.NamedOffsets(extractNameFromTapHeader(true, tapData) + " {turbo}", WAV_HEADER_LENGTH + dataStream.getCounter()));
            } else {
              namedOffsets.add(new RenderResult.NamedOffsets("...turbo.data...", WAV_HEADER_LENGTH + dataStream.getCounter()));
            }

            nextLevel = this.writeTapData(
                    namedOffsets,
                    nextLevel,
                    dataStream,
                    dataBlock.getLengthPilotTone(),
                    dataBlock.getLengthPilotPulse(),
                    dataBlock.getLengthSyncFirstPulse(),
                    dataBlock.getLengthSyncSecondPulse(),
                    dataBlock.getLengthZeroBitPulse(),
                    dataBlock.getLengthOneBitPulse(),
                    dataBlock.getUsedBitsInLastByte(),
                    Duration.ofMillis(dataBlock.getPauseAfterBlockMs()),
                    tapData,
                    DataType.TURBO_PILOT,
                    DataType.TURBO_SYNC1,
                    DataType.TURBO_SYNC2,
                    DataType.TURBO_DATA);
          }
          blockPointer++;
        } else if (block instanceof TzxBlockCSWRecording) {
          //TODO
          throw new IOException("Unsupported TzxBlockCSWRecording block yet");
        } else if (block instanceof TzxBlockDirectRecording) {
          final TzxBlockDirectRecording directRecording = (TzxBlockDirectRecording) block;

          namedOffsets.add(new RenderResult.NamedOffsets("...direct.recording... [pause: " + directRecording.getPauseAfterBlockMs() + " ms]", WAV_HEADER_LENGTH + dataStream.getCounter()));

          nextLevel = this.writeDirectRecording(
                  dataStream,
                  directRecording.getNumberTstatesPerSample(),
                  directRecording.getUsedBitsInLastByte(),
                  Duration.ofMillis(directRecording.getPauseAfterBlockMs()),
                  directRecording.extractData()
          );

          blockPointer++;
        } else if (block instanceof TzxBlockGeneralizedData) {
          final TzxBlockGeneralizedData dataBlock = (TzxBlockGeneralizedData) block;

          namedOffsets.add(new RenderResult.NamedOffsets("...generalized.data... [dSymbols=" + dataBlock.getTotalNumberOfSymbolsInDataStream() + ",dChar=" + Utils.minimalRequiredBitsFor(dataBlock.getNumberOfDataSymbolsInAbcTable() - 1) + ']', WAV_HEADER_LENGTH + dataStream.getCounter()));

          nextLevel = dataBlock.decodeRecordsAsPulses(nextLevel, (ticks, level) -> {
            this.writeSignalLevel(dataStream, ticks, level, DataType.GENERALIZED_DATA);
          });

          blockPointer++;
        } else if (block instanceof TzxBlockKansasCityStandard) {
          //TODO
          throw new IOException("Unsupported TzxBlockKansasCityStandard block yet");
        } else if (block instanceof TzxBlockPureData) {
          final TzxBlockPureData dataBlock = (TzxBlockPureData) block;
          final byte[] tapData = dataBlock.extractData();

          namedOffsets.add(new RenderResult.NamedOffsets("...pure.data... [pause: " + dataBlock.getPauseAfterBlockMs() + " ms]", WAV_HEADER_LENGTH + dataStream.getCounter()));

          nextLevel = this.writeTapData(
                  namedOffsets,
                  nextLevel,
                  dataStream,
                  -1,
                  -1,
                  -1,
                  -1,
                  dataBlock.getLengthZeroBitPulse(),
                  dataBlock.getLengthOneBitPulse(),
                  dataBlock.getUsedBitsInLastByte(),
                  Duration.ofMillis(dataBlock.getPauseAfterBlockMs()),
                  tapData,
                  DataType.PURE_PILOT,
                  DataType.PURE_SYNC1,
                  DataType.PURE_SYNC2,
                  DataType.PURE_DATA
          );

          blockPointer++;
        } else if (block instanceof TzxBlockPureTone) {
          final TzxBlockPureTone dataBlock = (TzxBlockPureTone) block;

          namedOffsets.add(new RenderResult.NamedOffsets("...pure.tone... [pulses=" + dataBlock.getNumberOfPulses() + ']', WAV_HEADER_LENGTH + dataStream.getCounter()));

          for (int i = 0; i < dataBlock.getNumberOfPulses(); i++) {
            this.writeSignalLevel(dataStream, dataBlock.getLengthOfPulseInTstates(), nextLevel, DataType.PURE_TONE);
            nextLevel = !nextLevel;
          }

          blockPointer++;
        } else if (block instanceof TzxBlockVarSequencePulses) {
          final TzxBlockVarSequencePulses dataBlock = (TzxBlockVarSequencePulses) block;

          namedOffsets.add(new RenderResult.NamedOffsets("...seq.pulses...[pulses=" + dataBlock.getPulsesLengths().length + "]", WAV_HEADER_LENGTH + dataStream.getCounter()));

          for (final int pulseLen : dataBlock.getPulsesLengths()) {
            this.writeSignalLevel(dataStream, pulseLen, nextLevel, DataType.SEQ_PULSES);
            nextLevel = !nextLevel;
          }

          blockPointer++;
        } else {
          throw new Error("Unexpected data block: " + block.getClass().getSimpleName());
        }
      } else {
        blockPointer++;
      }
    }

    // add pause in the end
    writePause(nextLevel, dataStream, Duration.ofMillis(500), DataType.PAUSE);

    dataStream.close();

    return new RenderResult(namedOffsets, dataStream);
  }

  private boolean writeTapData(
          final List<RenderResult.NamedOffsets> namedOffsets,
          final boolean nextSignalLevel,
          final DataStream outputStream,
          final int lenPilotTone,
          final int lenPilotPulse,
          final int lenSync1pulse,
          final int lenSync2pulse,
          final int lenZeroBitPulse,
          final int lenOneBitPulse,
          final int bitsInLastByte,
          final Duration pauseAfterBlock,
          final byte[] tapeData,
          final DataType pilotType,
          final DataType sync1Type,
          final DataType sync2Type,
          final DataType dataType
  ) throws IOException {

    boolean nextLevel = nextSignalLevel;

    if (lenPilotTone > 0) {
      for (int i = 0; i < lenPilotTone; i++) {
        writeSignalLevel(outputStream, lenPilotPulse, nextLevel, pilotType);
        nextLevel = !nextLevel;
      }
    }

    if (lenSync1pulse > 0) {
      writeSignalLevel(outputStream, lenSync1pulse, nextLevel, sync1Type);
      nextLevel = !nextLevel;
    }
    if (lenSync2pulse > 0) {
      writeSignalLevel(outputStream, lenSync2pulse, nextLevel, sync2Type);
      nextLevel = !nextLevel;
    }

    for (int i = 0; i < tapeData.length; i++) {
      final boolean lastByte = i == tapeData.length - 1;
      int bitCounter = lastByte ? bitsInLastByte : 8;
      final int nextDataByte = tapeData[i];
      int bitMask = 0x80;
      while (bitCounter > 0) {
        final int signalLength = (nextDataByte & bitMask) == 0 ? lenZeroBitPulse : lenOneBitPulse;
        writeSignalLevel(outputStream, signalLength, nextLevel, dataType);
        nextLevel = !nextLevel;
        writeSignalLevel(outputStream, signalLength, nextLevel, dataType);
        nextLevel = !nextLevel;
        bitMask >>= 1;
        bitCounter--;
      }
    }

    if (!pauseAfterBlock.isZero()) {
      namedOffsets.add(new RenderResult.NamedOffsets("...pause... [" + pauseAfterBlock.toMillis() + " ms]", WAV_HEADER_LENGTH + outputStream.getCounter()));
      nextLevel = writePause(nextLevel, outputStream, pauseAfterBlock, DataType.PAUSE);
    }

    return nextLevel;
  }

  private void removeAllRepeatAfter(final int blockIndex) {
    this.repeatStack.removeIf(next -> next.getBlockIndex() > blockIndex);
  }

  private void addCallSeq(final List<Integer> calls) {
    this.callStack.add(calls);
  }

  private int nextCallIndex() throws IOException {
    if (this.callStack.isEmpty()) {
      throw new IOException("Detected error in call sequence");
    } else {
      final List<Integer> last = this.callStack.remove(this.callStack.size() - 1);
      final int result = last.remove(0);
      if (!last.isEmpty()) {
        this.callStack.add(last);
      }
      return result;
    }
  }

  private boolean writeDirectRecording(
          final DataStream outputStream,
          final int ticksPerSample,
          final int bitsInLastByte,
          final Duration pauseAfterBlock,
          final byte[] data
  ) throws IOException {

    boolean nextLevel = false;

    for (int i = 0; i < data.length; i++) {
      final boolean lastByte = i == data.length - 1;
      int bitCounter = lastByte ? bitsInLastByte : 8;
      final int nextDataByte = data[i];
      int bitMask = 0x80;
      while (bitCounter > 0) {
        nextLevel = (nextDataByte & bitMask) != 0;
        writeSignalLevel(outputStream, ticksPerSample, nextLevel, DataType.DIRECT_DATA);
        bitMask >>= 1;
        bitCounter--;
      }
    }

    if (!pauseAfterBlock.isZero()) {
      nextLevel = writePause(false, outputStream, pauseAfterBlock, DataType.PAUSE);
    }

    return nextLevel;
  }

  private boolean writePause(final boolean nextLevel, final DataStream outputStream, final Duration delay, final DataType dataType) throws IOException {
    if (nextLevel) {
      writeSignalLevel(outputStream, PULSELEN_SYNC3, true, DataType.SYNC3);
    }
    final long ticks = (delay.toMillis() * TSTATES_PER_SECOND) / 1000L;
    writeSignalLevel(outputStream, (int) ticks, false, dataType);
    return true;
  }

  private void writeSignalLevel(final DataStream outputStream, final int pulseTicks, final boolean level, final DataType dataType) throws IOException {
    final int signal = level ? SIGNAL_HI : SIGNAL_LOW;

    long samples = (long) (0.5d + pulseTicks * this.tstatesPerSample);
    while (samples > 0L) {
      outputStream.write(signal, dataType);
      samples--;
    }
  }

  public enum DataType {
    WAV_SPECIFIC,
    PAUSE,
    SYNC3,
    DIRECT_DATA,
    PURE_TONE,
    SEQ_PULSES,
    PURE_DATA,
    PURE_SYNC1,
    PURE_SYNC2,
    PURE_PILOT,
    TURBO_PILOT,
    TURBO_SYNC1,
    TURBO_SYNC2,
    TURBO_DATA,
    STD_PILOT,
    STD_SYNC1,
    STD_SYNC2,
    STD_DATA,
    STOP_TAPE,
    STOP_TAPE_IF_ZX48,
    GENERALIZED_DATA;
  }

  public enum Freq {
    FREQ_11025(11024),
    FREQ_22050(22050),
    FREQ_44100(44100);

    private final int freq;

    Freq(final int freq) {
      this.freq = freq;
    }

    public int getFreq() {
      return this.freq;
    }
  }

  private static final class DataStream {
    private final ByteArrayOutputStream wavBuffer;
    private final ByteArrayOutputStream controlBuffer;
    private final JBBPBitOutputStream wavWriter;
    private final JBBPBitOutputStream controlWriter;
    private final Freq freq;
    private byte[] completedWav;
    private byte[] completedControl;

    DataStream(final Freq freq, final int initialSize) {
      this.freq = freq;
      this.wavBuffer = new ByteArrayOutputStream(initialSize);
      this.controlBuffer = new ByteArrayOutputStream(initialSize);
      this.wavWriter = new JBBPBitOutputStream(this.wavBuffer);
      this.controlWriter = new JBBPBitOutputStream(this.controlBuffer);
    }

    void close() throws IOException {
      this.wavWriter.flush();
      this.wavWriter.close();

      this.controlBuffer.flush();
      this.controlWriter.close();

      final byte[] wavArray = this.wavBuffer.toByteArray();
      final byte[] controlArray = this.controlBuffer.toByteArray();

      final JBBPOut out = JBBPOut.BeginBin(wavArray.length + WAV_HEADER_LENGTH);
      for (int i = 0; i < WAV_HEADER_LENGTH; i++) {
        out.Byte(DataType.WAV_SPECIFIC.ordinal());
      }
      this.completedControl = out.Byte(controlArray).End().toByteArray();

      this.completedWav = JBBPOut.BeginBin(wavArray.length + WAV_HEADER_LENGTH).
              ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).
              Byte("RIFF").
              Int(wavArray.length + 40).
              Byte("WAVE").
              Byte("fmt ").
              Int(16). // Size
                      Short(1). // Audio format
                      Short(1). // Num channels
                      Int(this.freq.getFreq()).// Sample rate
                      Int(this.freq.getFreq()). // Byte rate
                      Short(1). // Block align
                      Short(8). // Bits per sample
                      Byte("data").
              Int(wavArray.length).
              Byte(wavArray).
              End().toByteArray();
    }

    void write(final int wavData, final DataType type) throws IOException {
      this.wavWriter.write(wavData);
      this.controlWriter.write(type.ordinal());
    }

    byte[] getCompletedWav() {
      return Objects.requireNonNull(this.completedWav);
    }

    byte[] getCompletedControl() {
      return Objects.requireNonNull(this.completedControl);
    }

    long getCounter() {
      return this.wavWriter.getCounter();
    }
  }

  public static final class RenderResult {
    private final byte[] wavData;
    private final byte[] controlData;
    private final List<NamedOffsets> namedOffsets;

    RenderResult(final List<NamedOffsets> namedOffsets, final DataStream dataStream) {
      this.namedOffsets = new ArrayList<>(namedOffsets);
      this.wavData = dataStream.getCompletedWav();
      this.controlData = dataStream.getCompletedControl();
    }

    public byte[] getControlData() {
      return this.controlData;
    }

    public byte[] getWavData() {
      return this.wavData;
    }

    public List<NamedOffsets> getNamedOffsets() {
      return this.namedOffsets;
    }

    public static final class NamedOffsets {
      private final String name;
      private final long offsetInWav;

      NamedOffsets(final String name, final long offsetInWav) {
        this.name = name;
        this.offsetInWav = offsetInWav;
      }

      public String getName() {
        return this.name;
      }

      public long getOffsetInWav() {
        return this.offsetInWav;
      }

      @Override
      public String toString() {
        return this.name + " (offset " + this.offsetInWav + " bytes)";
      }
    }
  }

  private static class Repeat {
    private final int blockIndex;
    private int repetitions;

    Repeat(final int blockIndex, final int repeat) {
      this.blockIndex = blockIndex;
      this.repetitions = repeat;
    }

    boolean isZero() {
      return this.repetitions <= 0;
    }

    int getBlockIndex() {
      return this.blockIndex;
    }

    void dec() {
      this.repetitions = Math.max(0, this.repetitions - 1);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (o == null || getClass() != o.getClass()) {
        return false;
      }
      Repeat repeat1 = (Repeat) o;
      return blockIndex == repeat1.blockIndex;
    }

    @Override
    public int hashCode() {
      return this.blockIndex;
    }
  }
}
