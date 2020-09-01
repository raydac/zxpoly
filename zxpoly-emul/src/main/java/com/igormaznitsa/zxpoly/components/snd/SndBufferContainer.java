package com.igormaznitsa.zxpoly.components.snd;

import static com.igormaznitsa.zxpoly.components.video.VideoController.TSTATES_PER_INT;
import static java.util.Arrays.fill;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;


import javax.sound.sampled.AudioFormat;

final class SndBufferContainer {
  private static final int SND_FREQ = 44100;

  static final AudioFormat AUDIO_FORMAT = new AudioFormat(
      PCM_SIGNED,
      SND_FREQ,
      16,
      2,
      4,
      SND_FREQ,
      false
  );

  private static final int SAMPLES_PER_INT = SND_FREQ / 50;
  private static final int SND_BUFFER_LENGTH =
      SAMPLES_PER_INT * AUDIO_FORMAT.getChannels() * AUDIO_FORMAT.getSampleSizeInBits() / 8;
  private final byte[] soundBuffer;

  private int tstatesIntCounter = 0;
  private int lastWrittenPosition = 0;

  public SndBufferContainer() {
    this.soundBuffer = new byte[SND_BUFFER_LENGTH];
  }

  public byte[] makeClone(final int fillLevel) {
    if (this.lastWrittenPosition < SND_BUFFER_LENGTH - 4) {
      this.fillSndBuffer(this.lastWrittenPosition, fillLevel);
    }
    final byte[] clone = this.soundBuffer.clone();
    this.fillSndBuffer(0, fillLevel);
    return clone;
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
        fillSndBuffer(position, level);
      }
      this.lastWrittenPosition = position;
    }
  }

  private void fillSndBuffer(int fromIndex, final int value) {
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
