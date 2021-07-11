package com.igormaznitsa.zxpoly.components.snd;

import javax.sound.sampled.AudioFormat;
import java.util.Arrays;

import static com.igormaznitsa.zxpoly.components.Timings.TSTATES_PER_FRAME;
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

  public byte[] nextBuffer(final int fillLevelL, final int fillLevelR) {
    if (this.lastWrittenPosition < SND_BUFFER_SIZE - FRAME_SIZE) {
      this.fillCurrentSndBuffer(this.lastWrittenPosition, SND_BUFFER_SIZE, fillLevelL, fillLevelR);
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
    return ((tstatesIntCounter * SAMPLES_PER_INT + TSTATES_PER_FRAME / 2)
            / TSTATES_PER_FRAME);
  }

  public void setValue(final int deltaTstates, final int levelLeft, final int levelRight) {
    this.tstatesIntCounter += deltaTstates;
    int position = calculatePosition(tstatesIntCounter) * 4;

    if (position < SND_BUFFER_SIZE) {
      fillCurrentSndBuffer(this.lastWrittenPosition, position + FRAME_SIZE, levelLeft, levelRight);
      this.lastWrittenPosition = position;
    }
  }

  private void fillCurrentSndBuffer(int fromInclusive, final int toExclusive, final int levelLeft, final int levelRight) {
    final byte lowL = (byte) levelLeft;
    final byte highL = (byte) (levelLeft >> 8);
    final byte lowR = (byte) levelRight;
    final byte highR = (byte) (levelRight >> 8);

    while (fromInclusive < toExclusive) {
      if (((fromInclusive >> 1) & 1) == 0) {
        this.soundBuffer[fromInclusive++] = lowL;
        this.soundBuffer[fromInclusive++] = highL;
      } else {
        this.soundBuffer[fromInclusive++] = lowR;
        this.soundBuffer[fromInclusive++] = highR;
      }
    }
  }

  public void reset() {
    this.resetPosition();
  }
}
