package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.igormaznitsa.jbbp.io.JBBPOut.BeginBin;

public class TzxWavRenderer {
  private static final int IMPULSNUMBER_PILOT_HEADER = 8063;
  private static final int IMPULSNUMBER_PILOT_DATA = 3223;

  private static final int PULSELEN_PILOT = 2168;
  private static final int PULSELEN_SYNC1 = 667;
  private static final int PULSELEN_SYNC2 = 735;
  private static final int PULSELEN_ZERO = 855;
  private static final int PULSELEN_ONE = 1710;


  private final TzxFile tzxFile;
  private final Freq freq;
  private final List<Repeat> repeatStack = new ArrayList<>();
  private final List<List<Integer>> callStack = new ArrayList<>();

  public TzxWavRenderer(final Freq freq, final TzxFile tzxFile) {
    this.freq = Objects.requireNonNull(freq);
    this.tzxFile = Objects.requireNonNull(tzxFile);
  }

  private Optional<Repeat> findRepeat(final int blockIndex) {
    return this.repeatStack.stream().filter(x -> x.getBlockIndex() == blockIndex).findFirst();
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

  public synchronized byte[] render() throws IOException {
    this.repeatStack.clear();
    this.callStack.clear();

    final ByteArrayOutputStream dataTargetStream = new ByteArrayOutputStream(1024 * 1024);
    final JBBPOut out = BeginBin(JBBPByteOrder.LITTLE_ENDIAN);
    final List<AbstractTzxBlock> blockList = this.tzxFile.getBlockList();

    int blockPointer = 0;
    while (blockPointer < blockList.size()) {
      final AbstractTzxBlock block = blockList.get(blockPointer);
      if (block instanceof FlowManagementBlock) {
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
        if (block instanceof TzxBlockStandardSpeedData) {
          final TzxBlockStandardSpeedData dataBlock = (TzxBlockStandardSpeedData) block;
          final byte[] tapData = dataBlock.extractData();
          this.writeTapBlockData(dataTargetStream,
                  (tapData[0] & 0xFF) < 128 ? IMPULSNUMBER_PILOT_HEADER : IMPULSNUMBER_PILOT_DATA,
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
          this.writeTapBlockData(dataTargetStream,
                  dataBlock.getLengthPilotTone(),
                  dataBlock.getLengthPilotPulse(),
                  dataBlock.getLengthSyncFirstPulse(),
                  dataBlock.getLengthSyncSecondPulse(),
                  dataBlock.getLengthZeroBitPulse(),
                  dataBlock.getLengthOneBitPulse(),
                  dataBlock.getUsedBitsInLastByte(),
                  Duration.ofMillis(dataBlock.getPauseAfterBlockMs()),
                  dataBlock.extractData());
          blockPointer++;
        } else if (block instanceof TzxBlockCSWRecording) {
          //TODO
          throw new IOException("Unsupported block yet");
        } else if (block instanceof TzxBlockDirectRecording) {
          //TODO
          throw new IOException("Unsupported block yet");
        } else if (block instanceof TzxBlockGeneralizedData) {
          //TODO
          throw new IOException("Unsupported block yet");
        } else if (block instanceof TzxBlockKansasCityStandard) {
          //TODO
          throw new IOException("Unsupported block yet");
        } else if (block instanceof TzxBlockPureData) {
          //TODO
          throw new IOException("Unsupported block yet");
        } else if (block instanceof TzxBlockPureTone) {
          //TODO
          throw new IOException("Unsupported block yet");
        } else if (block instanceof TzxBlockVarSequencePulses) {
          //TODO
          throw new IOException("Unsupported block yet");
        } else {
          throw new Error("Unexpected data block: " + block.getClass().getSimpleName());
        }
      } else {
        blockPointer++;
      }
    }

    return out.
            Byte("RIFF").
            Int(dataTargetStream.size() + 40).
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
            Int(dataTargetStream.size()).
            Byte(dataTargetStream.toByteArray()).End().toByteArray();

  }

  private void writeTapBlockData(
          final OutputStream outputStream,
          final int lenPilotTone,
          final int lenPilotPulse,
          final int lenSync1pulse,
          final int lenSync2pulse,
          final int lenZeroBitPulse,
          final int lenOneBitPulse,
          final int bitsInLastByte,
          final Duration pauseAfterBlock,
          final byte[] data
  ) throws IOException {

  }

  public enum Freq {
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
