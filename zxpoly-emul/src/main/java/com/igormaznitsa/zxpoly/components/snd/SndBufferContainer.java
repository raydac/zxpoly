package com.igormaznitsa.zxpoly.components.snd;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;

import static com.igormaznitsa.zxpoly.components.Motherboard.TSTATES_PER_INT;
import static java.util.Arrays.fill;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

final class SndBufferContainer {
  public static final int SND_FREQ = 48000;
  public static final int FRAME_SIZE = 4;
  public static final int CHANNELS_NUM = 2;
  public static final int SAMPLE_SIZE_BITS = 16;

  public static final int BUFFERS_NUMBER = 5;
  static final AudioFormat AUDIO_FORMAT = new AudioFormat(
      PCM_SIGNED,
      SND_FREQ,
      SAMPLE_SIZE_BITS,
      CHANNELS_NUM,
      FRAME_SIZE,
      SND_FREQ,
      false
  );
  private static final int SAMPLES_PER_INT = SND_FREQ / 50;
  public static final int SND_BUFFER_SIZE = SAMPLES_PER_INT * FRAME_SIZE;
  private final byte[][] allSndBuffers;
  private byte[] soundBuffer;
  private int bufferIndex;

  private int tstatesIntCounter = 0;
  private int lastWrittenPosition = 0;

  public SndBufferContainer() {
    this.allSndBuffers = new byte[BUFFERS_NUMBER][];
    for (int i = 0; i < BUFFERS_NUMBER; i++) {
      this.allSndBuffers[i] = new byte[SND_BUFFER_SIZE];
      Arrays.fill(this.allSndBuffers[i], (byte) 0xFF);
    }
    this.soundBuffer = this.allSndBuffers[this.bufferIndex];
  }

  public void writeCurrent(final SourceDataLine line) {
    line.write(this.soundBuffer, 0, SND_BUFFER_SIZE);
  }

  public byte[] nextBuffer(final int fillLevel) {
    if (this.lastWrittenPosition < SND_BUFFER_SIZE - FRAME_SIZE) {
      this.fillCurrentSndBuffer(this.lastWrittenPosition, SND_BUFFER_SIZE, fillLevel);
    }
    final byte[] result = this.soundBuffer;
    this.bufferIndex++;
    if (this.bufferIndex == BUFFERS_NUMBER) {
      this.bufferIndex = 0;
    }
    this.soundBuffer = this.allSndBuffers[this.bufferIndex];
    return result;
  }

  public void resetPosition() {
    this.tstatesIntCounter = 0;
    this.lastWrittenPosition = 0;
  }

  public static int calculatePosition(final int tstatesIntCounter) {
    return ((tstatesIntCounter * SAMPLES_PER_INT + TSTATES_PER_INT / 2)
            / TSTATES_PER_INT);
  }

  public void setValue(final int deltaTstates, final int level) {
    this.tstatesIntCounter += deltaTstates;
    int position = calculatePosition(tstatesIntCounter) * 4;

    if (position < SND_BUFFER_SIZE) {
      if (position - this.lastWrittenPosition < 8) {
        final byte low = (byte) level;
        final byte high = (byte) (level >> 8);
        this.soundBuffer[position++] = low;
        this.soundBuffer[position++] = high;
        this.soundBuffer[position++] = low;
        this.soundBuffer[position++] = high;
      } else {
        fillCurrentSndBuffer(this.lastWrittenPosition, position + FRAME_SIZE, level);
      }
      this.lastWrittenPosition = position;
    }
  }

  private void fillCurrentSndBuffer(int fromInclusive, final int toExclusive, final int value) {
    final byte low = (byte) value;
    final byte high = (byte) (value >> 8);

    if (low == high) {
      fill(this.soundBuffer, fromInclusive, SND_BUFFER_SIZE, low);
    } else {
      while (fromInclusive < toExclusive) {
        this.soundBuffer[fromInclusive++] = low;
        this.soundBuffer[fromInclusive++] = high;
      }
    }
  }

  public void reset() {
    this.resetPosition();
  }
}
