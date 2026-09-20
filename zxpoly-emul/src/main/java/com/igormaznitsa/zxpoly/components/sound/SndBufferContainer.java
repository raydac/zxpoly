package com.igormaznitsa.zxpoly.components.sound;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;

import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import javax.sound.sampled.AudioFormat;

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
  private final TimingProfile timingProfile;
  private byte[] soundBuffer;
  private int bufferIndex;
  private int tiStatesIntCounter = 0;
  private int lastWrittenPosition = 0;

  public SndBufferContainer(final TimingProfile timingProfile) {
    this.timingProfile = timingProfile;
    this.allSndBuffers = new byte[BUFFERS_NUMBER][];
    for (int i = 0; i < BUFFERS_NUMBER; i++) {
      this.allSndBuffers[i] = new byte[SND_BUFFER_SIZE];
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
    this.tiStatesIntCounter = 0;
    this.lastWrittenPosition = 0;
  }

  private int bytePositionForTstates(final int tstates) {
    final long frame = this.timingProfile.tstatesFrame;
    if (frame <= 0L) {
      return -1;
    }
    final long sampleIndex = (tstates * (long) SAMPLES_PER_INT + frame / 2L) / frame;
    if (sampleIndex < 0L || sampleIndex > SAMPLES_PER_INT) {
      return -1;
    }
    return (int) (sampleIndex * FRAME_SIZE);
  }

  public void setValue(final int deltaTiStates, final int levelLeft, final int levelRight) {
    if (deltaTiStates <= 0) {
      return;
    }

    this.tiStatesIntCounter += deltaTiStates;
    final int position = this.bytePositionForTstates(this.tiStatesIntCounter);
    if (position < 0 || position >= SND_BUFFER_SIZE || this.lastWrittenPosition < 0) {
      return;
    }

    this.fillCurrentSndBuffer(this.lastWrittenPosition, position + FRAME_SIZE, levelLeft,
        levelRight);
    this.lastWrittenPosition = position;
  }

  private void fillCurrentSndBuffer(int fromInclusive, final int toExclusive, final int levelLeft, final int levelRight) {
    fromInclusive = Math.clamp(fromInclusive, 0, SND_BUFFER_SIZE);
    final int end = Math.clamp(toExclusive, fromInclusive, SND_BUFFER_SIZE);

    final byte lowL = (byte) levelLeft;
    final byte highL = (byte) (levelLeft >> 8);
    final byte lowR = (byte) levelRight;
    final byte highR = (byte) (levelRight >> 8);

    final byte[] ptr = this.soundBuffer;
    boolean flag = ((fromInclusive >> 1) & 1) == 0;

    while (fromInclusive < end) {
      if (flag) {
        ptr[fromInclusive++] = lowL;
        ptr[fromInclusive++] = highL;
      } else {
        ptr[fromInclusive++] = lowR;
        ptr[fromInclusive++] = highR;
      }
      flag = !flag;
    }
  }

  public void reset() {
    this.resetPosition();
  }
}
