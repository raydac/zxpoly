package com.igormaznitsa.zxpoly.components.video.timings;

import com.igormaznitsa.zxpoly.components.video.VideoController;

public enum TimingProfile {
  SPEC128(
          3_546_900,
          36,
          16,
          8,
          55,
          56,
          128,
          26,
          24,
          24,
          26,
          new int[]{6, 5, 4, 3, 2, 1, 0, 0}
  );

  public static final int TIMINGSTATE_MASK_TYPE = 0xFF_0000_00;
  public static final int TIMINGSTATE_MASK_ATTR = 0x00_FFFF_00;
  public static final int TIMINGSTATE_MASK_TIME = 0x00_0000_FF;
  public static final int TIMINGSTATE_BORDER = 0x00_0000_00;
  public static final int TIMINGSTATE_PAPER = 0x01_0000_00;

  public final int lengthInt;
  public final int lengthNmi;
  public final int clockFreq;
  public final int tstatesFrame;
  private static final int ZX_SCREEN_LINES = 192;
  public final int scanLines;
  public final int tstatesPerLine;
  public final int tstatesStartScreen;

  private final int[] contention;
  public final int tstatesFirstAfterScreen;
  public final int tstatesVideo;
  public final int tstatesBorderLeft;
  public final int tstatesBorderRight;
  public final int tstatesHSync;
  public final int tstatesBlank;
  public final int linesBorderTop;
  public final int linesBorderBottom;
  public final int linesVSync;

  TimingProfile(
          final int clockFreq,
          final int lengthInt,
          final int lengthNmi,
          final int linesVsync,
          final int linesBorderTop,
          final int linesBorderBottom,
          final int tstatesVideo,
          final int tstatesBorderLeft,
          final int tstatesHSync,
          final int tstatesBlank,
          final int tstatesBorderRight,
          final int[] contention
  ) {
    this.lengthNmi = lengthNmi;
    this.lengthInt = lengthInt;
    this.clockFreq = clockFreq;
    this.contention = contention;

    this.linesVSync = linesVsync;
    this.scanLines = linesVsync + linesBorderTop + ZX_SCREEN_LINES + linesBorderBottom;
    this.tstatesPerLine = tstatesVideo + tstatesBorderLeft + tstatesHSync + tstatesBlank + tstatesBorderRight;
    this.tstatesFrame = this.tstatesPerLine * this.scanLines;
    this.tstatesStartScreen = this.tstatesPerLine * (linesVsync + linesBorderTop);
    this.tstatesFirstAfterScreen = this.tstatesStartScreen + ((ZX_SCREEN_LINES - 1) * this.tstatesPerLine) + tstatesVideo;

    this.tstatesVideo = tstatesVideo;
    this.tstatesHSync = tstatesHSync;
    this.tstatesBlank = tstatesBlank;
    this.tstatesBorderLeft = tstatesBorderLeft;
    this.tstatesBorderRight = tstatesBorderRight;
    this.linesBorderBottom = linesBorderBottom;
    this.linesBorderTop = linesBorderTop;
  }

  /**
   * Generates timing info for frame ticks
   * <p>
   * TTTTTTTT_AAAAAAAAAAAAAAAA_CCCCCCCC
   * \------/ \--------------/ \------/
   * type    attribute addr   contend
   *
   * @return generated int array in packed value format
   */
  public int[] makeTimeFrameDelays() {
    final int[] lineAttrOffset = new int[256];
    int i = 0;
    for (int p = 0; p < 4; p++) {
      for (int y = 0; y < 8; y++) {
        for (int o = 0; o < 8; o++, i++) {
          lineAttrOffset[i] = 0x1800 + (p * 8 + y) * 32;
        }
      }
    }

    final int[] result = new int[this.tstatesFrame];

    for (int t = 0; t < this.tstatesFrame; t++) {
      if (t < this.tstatesStartScreen) {
        result[t] = TIMINGSTATE_BORDER;
      } else {
        final int displayOffset = t - this.tstatesStartScreen;
        final int line = displayOffset / this.tstatesPerLine;
        final int pixel = displayOffset % this.tstatesPerLine;
        if (line < VideoController.ZXSCREEN_ROWS && pixel < this.tstatesPerLine) {
          final int delay = this.contention[pixel % this.contention.length];
          final int attribute = lineAttrOffset[line] + pixel / 4;
          result[t] = TIMINGSTATE_PAPER | (attribute << 8) | delay;
        } else {
          result[t] = TIMINGSTATE_BORDER;
        }
      }
    }

    return result;
  }
}
