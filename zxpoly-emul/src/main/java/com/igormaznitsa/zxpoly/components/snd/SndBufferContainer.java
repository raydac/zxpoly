package com.igormaznitsa.zxpoly.components.snd;

import static com.igormaznitsa.zxpoly.components.Motherboard.TSTATES_PER_INT;
import static java.util.Arrays.fill;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;


import java.util.Arrays;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.SourceDataLine;

final class SndBufferContainer {
  private static final int SND_FREQ = 48000;

  static final AudioFormat AUDIO_FORMAT = new AudioFormat(
      PCM_SIGNED,
      SND_FREQ,
      16,
      2,
      4,
      SND_FREQ,
      false
  );

  public static final int BUFFERS_NUMBER = 5;

  private static final int SAMPLES_PER_INT = SND_FREQ / 50;
  private static final int SND_BUFFER_LENGTH =
      SAMPLES_PER_INT * AUDIO_FORMAT.getChannels() * AUDIO_FORMAT.getSampleSizeInBits() / 8;
  private final byte[][] allSndBuffers;
  private byte[] soundBuffer;
  private int bufferIndex;

  private int tstatesIntCounter = 0;
  private int lastWrittenPosition = 0;

  public SndBufferContainer() {
    this.allSndBuffers = new byte[BUFFERS_NUMBER][];
    for (int i = 0; i < BUFFERS_NUMBER; i++) {
      this.allSndBuffers[i] = new byte[SND_BUFFER_LENGTH];
      Arrays.fill(this.allSndBuffers[i], (byte) 0xFF);
    }
    this.soundBuffer = this.allSndBuffers[this.bufferIndex];
  }

  public void writeCurrent(final SourceDataLine line) {
    line.write(this.soundBuffer, 0, SND_BUFFER_LENGTH);
  }

  public byte[] nextBuffer(final int fillLevel) {
    if (this.lastWrittenPosition < SND_BUFFER_LENGTH - 4) {
      this.fillCurrentSndBuffer(this.lastWrittenPosition, fillLevel);
    }
    final byte[] result = this.soundBuffer;
    this.bufferIndex++;
    if (this.bufferIndex == BUFFERS_NUMBER) {
      this.bufferIndex = 0;
    }
    this.soundBuffer = this.allSndBuffers[this.bufferIndex];
    this.fillCurrentSndBuffer(0, fillLevel);
    return result;
  }

  public void resetPosition() {
    this.tstatesIntCounter = 0;
    this.lastWrittenPosition = 0;
  }

  public void setValue(final int deltaTstates, final int level) {
    this.tstatesIntCounter += deltaTstates;
    int position = ((tstatesIntCounter * SAMPLES_PER_INT + TSTATES_PER_INT / 2)
        / TSTATES_PER_INT) * 4;

    if (position < SND_BUFFER_LENGTH) {
      if (position - this.lastWrittenPosition < 8) {
        final byte low = (byte) level;
        final byte high = (byte) (level >> 8);
        this.soundBuffer[position++] = low;
        this.soundBuffer[position++] = high;
        this.soundBuffer[position++] = low;
        this.soundBuffer[position++] = high;
      } else {
        fillCurrentSndBuffer(position, level);
      }
      this.lastWrittenPosition = position;
    }
  }

  private void fillCurrentSndBuffer(int fromIndex, final int value) {
    final byte low = (byte) value;
    final byte high = (byte) (value >> 8);

    if (low == high) {
      fill(this.soundBuffer, fromIndex, SND_BUFFER_LENGTH, low);
    } else {
      while (fromIndex < SND_BUFFER_LENGTH) {
        this.soundBuffer[fromIndex++] = low;
        this.soundBuffer[fromIndex++] = high;
      }
    }
  }

  public int getLength() {
    return SND_BUFFER_LENGTH;
  }

  public void reset() {
    this.resetPosition();
  }
}
