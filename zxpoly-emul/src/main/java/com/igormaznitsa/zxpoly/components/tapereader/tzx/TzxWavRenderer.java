package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.igormaznitsa.jbbp.io.JBBPOut.BeginBin;

public class TzxWavRenderer {
  public static final long WAV_HEADER_LENGTH = 44L;
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

  public TzxWavRenderer(final Freq freq, final TzxFile tzxFile) {
    this.freq = Objects.requireNonNull(freq);
    this.tzxFile = Objects.requireNonNull(tzxFile);
    this.tstatesPerSample = (double) this.freq.getFreq() / (double) TSTATES_PER_SECOND;
  }

  private static String extractNameFromTapHeader(final byte[] data) {
    final byte[] name = new byte[10];
    if (data.length < 12) return "<UNKNOWN>";
    System.arraycopy(data, 2, name, 0, name.length);

    final StringBuilder result = new StringBuilder();

    for (final char c : new String(name, StandardCharsets.ISO_8859_1).toCharArray()) {
      result.append(Character.isISOControl(c) ? ' ' : c);
    }
    return result.toString();
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

  public synchronized RenderResult render() throws IOException {
    final List<RenderResult.NamedOffsets> namedOffsets = new ArrayList<>();

    this.repeatStack.clear();
    this.callStack.clear();

    final ByteArrayOutputStream bufferArray = new ByteArrayOutputStream(5 * 1024 * 1024);
    final JBBPBitOutputStream dataTargetStream = new JBBPBitOutputStream(bufferArray);
    final JBBPOut out = BeginBin(JBBPByteOrder.LITTLE_ENDIAN);
    final List<AbstractTzxBlock> blockList = this.tzxFile.getBlockList();

    boolean nextLevel = true;

    int blockPointer = 0;
    while (blockPointer < blockList.size()) {
      final AbstractTzxBlock block = blockList.get(blockPointer);

      if (block instanceof InformationBlock) {
        if (block instanceof TzxBlockGroupStart) {
          final TzxBlockGroupStart groupStart = (TzxBlockGroupStart) block;
          namedOffsets.add(new RenderResult.NamedOffsets("GROUP: " + groupStart.getGroupName(), WAV_HEADER_LENGTH + dataTargetStream.getCounter()));
        }
        blockPointer++;
      } else if (block instanceof FlowManagementBlock) {
        final short[] offsets = ((FlowManagementBlock) block).getOffsets();
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
      } else if (block instanceof SoundDataBlock) {
        if (block instanceof TzxBlockStopTapeIf48k) {
          namedOffsets.add(new RenderResult.NamedOffsets("<<STOP TAPE>>", WAV_HEADER_LENGTH + dataTargetStream.getCounter()));
          nextLevel = this.writePause(nextLevel, dataTargetStream, Duration.ofSeconds(5));

          blockPointer++;
        } else if (block instanceof TzxBlockPauseOrStop) {
          final TzxBlockPauseOrStop dataBlock = (TzxBlockPauseOrStop) block;

          Duration duration = Duration.ofMillis(dataBlock.getPauseDurationMs());

          if (duration.isZero()) {
            namedOffsets.add(new RenderResult.NamedOffsets("<<STOP TAPE>>", WAV_HEADER_LENGTH + dataTargetStream.getCounter()));
            duration = Duration.ofSeconds(10);
          } else {
            namedOffsets.add(new RenderResult.NamedOffsets("__stop-pause__[" + dataBlock.getPauseDurationMs() + " ms]", WAV_HEADER_LENGTH + dataTargetStream.getCounter()));
          }
          nextLevel = writePause(nextLevel, dataTargetStream, duration);

          blockPointer++;
        } else if (block instanceof TzxBlockStandardSpeedData) {
          final TzxBlockStandardSpeedData dataBlock = (TzxBlockStandardSpeedData) block;

          final byte[] tapData = dataBlock.extractData();
          final int flag = tapData[0] & 0xFF;

          if (flag < 128) {
            namedOffsets.add(new RenderResult.NamedOffsets(extractNameFromTapHeader(tapData), WAV_HEADER_LENGTH + dataTargetStream.getCounter()));
          } else {
            namedOffsets.add(new RenderResult.NamedOffsets("__data__", WAV_HEADER_LENGTH + dataTargetStream.getCounter()));
          }

          nextLevel = this.writeTapData(
                  namedOffsets,
                  nextLevel,
                  dataTargetStream,
                  flag < 128 ? IMPULSNUMBER_PILOT_HEADER : IMPULSNUMBER_PILOT_DATA,
                  PULSELEN_PILOT,
                  PULSELEN_SYNC1,
                  PULSELEN_SYNC2,
                  PULSELEN_ZERO,
                  PULSELEN_ONE,
                  8,
                  Duration.ofMillis(dataBlock.getPauseAfterBlockMs()),
                  tapData);

          blockPointer++;
        } else if (block instanceof TzxBlockTurboSpeedData) {
          final TzxBlockTurboSpeedData dataBlock = (TzxBlockTurboSpeedData) block;

          final byte[] tapData = dataBlock.extractData();
          final int flag = tapData[0] & 0xFF;

          if (flag < 128) {
            namedOffsets.add(new RenderResult.NamedOffsets(extractNameFromTapHeader(tapData) + " {turbo}", WAV_HEADER_LENGTH + dataTargetStream.getCounter()));
          } else {
            namedOffsets.add(new RenderResult.NamedOffsets("__data__ {turbo}", WAV_HEADER_LENGTH + dataTargetStream.getCounter()));
          }

          nextLevel = this.writeTapData(
                  namedOffsets,
                  nextLevel,
                  dataTargetStream,
                  dataBlock.getLengthPilotTone(),
                  dataBlock.getLengthPilotPulse(),
                  dataBlock.getLengthSyncFirstPulse(),
                  dataBlock.getLengthSyncSecondPulse(),
                  dataBlock.getLengthZeroBitPulse(),
                  dataBlock.getLengthOneBitPulse(),
                  dataBlock.getUsedBitsInLastByte(),
                  Duration.ofMillis(dataBlock.getPauseAfterBlockMs()),
                  tapData);

          blockPointer++;
        } else if (block instanceof TzxBlockCSWRecording) {
          //TODO
          throw new IOException("Unsupported block yet");
        } else if (block instanceof TzxBlockDirectRecording) {
          final TzxBlockDirectRecording directRecording = (TzxBlockDirectRecording) block;

          namedOffsets.add(new RenderResult.NamedOffsets("__direct recording__(pause: " + directRecording.getPauseAfterBlockMs() + " ms)", WAV_HEADER_LENGTH + dataTargetStream.getCounter()));

          nextLevel = this.writeDirectRecording(
                  dataTargetStream,
                  directRecording.getNumberTstatesPerSample(),
                  directRecording.getUsedBitsInLastByte(),
                  Duration.ofMillis(directRecording.getPauseAfterBlockMs()),
                  directRecording.extractData()
          );

          blockPointer++;
        } else if (block instanceof TzxBlockGeneralizedData) {
          //TODO
          throw new IOException("Unsupported block yet");
        } else if (block instanceof TzxBlockKansasCityStandard) {
          //TODO
          throw new IOException("Unsupported block yet");
        } else if (block instanceof TzxBlockPureData) {
          final TzxBlockPureData dataBlock = (TzxBlockPureData) block;
          final byte[] tapData = dataBlock.extractData();

          namedOffsets.add(new RenderResult.NamedOffsets("__pure_data__(pause: " + dataBlock.getPauseAfterBlockMs() + " ms)", WAV_HEADER_LENGTH + dataTargetStream.getCounter()));

          nextLevel = this.writeTapData(
                  namedOffsets,
                  nextLevel,
                  dataTargetStream,
                  -1,
                  -1,
                  -1,
                  -1,
                  dataBlock.getLengthZeroBitPulse(),
                  dataBlock.getLengthOneBitPulse(),
                  dataBlock.getUsedBitsInLastByte(),
                  Duration.ofMillis(dataBlock.getPauseAfterBlockMs()),
                  tapData);

          blockPointer++;
        } else if (block instanceof TzxBlockPureTone) {
          final TzxBlockPureTone dataBlock = (TzxBlockPureTone) block;

          namedOffsets.add(new RenderResult.NamedOffsets("__pure_tone__[" + dataBlock.getNumberOfPulses() + ']', WAV_HEADER_LENGTH + dataTargetStream.getCounter()));

          for (int i = 0; i < dataBlock.getNumberOfPulses(); i++) {
            this.writeSignalLevel(dataTargetStream, dataBlock.getLengthOfPulseInTstates(), nextLevel);
            nextLevel = !nextLevel;
          }

          blockPointer++;
        } else if (block instanceof TzxBlockVarSequencePulses) {
          final TzxBlockVarSequencePulses dataBlock = (TzxBlockVarSequencePulses) block;

          namedOffsets.add(new RenderResult.NamedOffsets("__pulses_seq__[" + dataBlock.getPulsesLengths().length + "]", WAV_HEADER_LENGTH + dataTargetStream.getCounter()));

          for (final int pulseLen : dataBlock.getPulsesLengths()) {
            this.writeSignalLevel(dataTargetStream, pulseLen, nextLevel);
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
    writePause(nextLevel, dataTargetStream, Duration.ofMillis(500));

    dataTargetStream.flush();

    final byte[] wav = out.
            Byte("RIFF").
            Int(bufferArray.size() + 40).
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
            Int(bufferArray.size()).
            Byte(bufferArray.toByteArray()).End().toByteArray();

    return new RenderResult(namedOffsets, wav);
  }

  private boolean writeTapData(
          final List<RenderResult.NamedOffsets> namedOffsets,
          final boolean nextSignalLevel,
          final JBBPBitOutputStream outputStream,
          final int lenPilotTone,
          final int lenPilotPulse,
          final int lenSync1pulse,
          final int lenSync2pulse,
          final int lenZeroBitPulse,
          final int lenOneBitPulse,
          final int bitsInLastByte,
          final Duration pauseAfterBlock,
          final byte[] tapeData
  ) throws IOException {

    boolean nextLevel = nextSignalLevel;

    if (lenPilotTone > 0) {
      for (int i = 0; i < lenPilotTone; i++) {
        writeSignalLevel(outputStream, lenPilotPulse, nextLevel);
        nextLevel = !nextLevel;
      }
    }

    if (lenSync1pulse > 0) {
      writeSignalLevel(outputStream, lenSync1pulse, nextLevel);
      nextLevel = !nextLevel;
    }
    if (lenSync2pulse > 0) {
      writeSignalLevel(outputStream, lenSync2pulse, nextLevel);
      nextLevel = !nextLevel;
    }

    for (int i = 0; i < tapeData.length; i++) {
      final boolean lastByte = i == tapeData.length - 1;
      int bitCounter = lastByte ? bitsInLastByte : 8;
      final int nextDataByte = tapeData[i];
      int bitMask = 0x80;
      while (bitCounter > 0) {
        final int signalLength = (nextDataByte & bitMask) == 0 ? lenZeroBitPulse : lenOneBitPulse;
        writeSignalLevel(outputStream, signalLength, nextLevel);
        nextLevel = !nextLevel;
        writeSignalLevel(outputStream, signalLength, nextLevel);
        nextLevel = !nextLevel;
        bitMask >>= 1;
        bitCounter--;
      }
    }

    if (!pauseAfterBlock.isZero()) {
      namedOffsets.add(new RenderResult.NamedOffsets("__pause__ [" + pauseAfterBlock.toMillis() + " ms]", WAV_HEADER_LENGTH + outputStream.getCounter()));
      nextLevel = writePause(nextLevel, outputStream, pauseAfterBlock);
    }

    return nextLevel;
  }

  private boolean writeDirectRecording(
          final JBBPBitOutputStream outputStream,
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
        writeSignalLevel(outputStream, ticksPerSample, nextLevel);
        bitMask >>= 1;
        bitCounter--;
      }
    }

    if (!pauseAfterBlock.isZero()) {
      nextLevel = writePause(false, outputStream, pauseAfterBlock);
    }

    return nextLevel;
  }

  private boolean writePause(final boolean nextLevel, final JBBPBitOutputStream outputStream, final Duration delay) throws IOException {
    if (nextLevel) {
      writeSignalLevel(outputStream, PULSELEN_SYNC3, true);
    }
    final long ticks = (delay.toMillis() * TSTATES_PER_SECOND) / 1000L;
    writeSignalLevel(outputStream, (int) ticks, false);
    return true;
  }

  private void writeSignalLevel(final JBBPBitOutputStream outputStream, final int pulseTicks, final boolean level) throws IOException {
    final int signal = level ? SIGNAL_HI : SIGNAL_LOW;

    long samples = (long) (0.5d + pulseTicks * this.tstatesPerSample);
    while (samples > 0L) {
      outputStream.write(signal);
      samples--;
    }
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

  public static final class RenderResult {
    private final byte[] wav;
    private final List<NamedOffsets> namedOffsets;

    RenderResult(final List<NamedOffsets> namedOffsets, final byte[] wav) {
      this.wav = wav;
      this.namedOffsets = new ArrayList<>(namedOffsets);
    }

    public byte[] getWav() {
      return this.wav;
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
