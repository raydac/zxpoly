package com.igormaznitsa.zxpoly.components.video.timings;

public enum TimingProfile {
  SPEC128(
          3_546_900,
          36,
          16,
          70908,
          228,
          63,
          311,
          14362,
          58040,
          176,
          296,
          16,
          48,
          new int[]{6, 5, 4, 3, 2, 1, 0, 0}
  );

  public static final int TIMINGSTATE_MASK_TYPE = 0xFF_0000_00;
  public static final int TIMINGSTATE_MASK_ATTR = 0x00_FFFF_00;
  public static final int TIMINGSTATE_MASK_TIME = 0x00_0000_FF;
  public static final int TIMINGSTATE_BORDER = 0x00_0000_00;
  public static final int TIMINGSTATE_PAPER = 0x01_0000_00;

  public static final int ZX_SCREEN_LINES = 192;
  public final int lengthInt;
  public final int lengthNmi;
  public final int clockFreq;
  public final int tstatesFrame;
  public final int tstatesLine;
  public final int upBorderWidth;
  public final int scanLines;
  public final int firstScrByte;
  public final int lastScrUpdate;
  public final int tStatesBorderAreaWidth;
  public final int borderVisibleAreaHeight;
  private final int[] contention;
  public final int borderTopHiddenLines;
  public final int tstatesLeftBorderStart;

  TimingProfile(
          final int clockFreq,
          final int lengthInt,
          final int lengthNmi,
          final int tstatesFrame,
          final int tstatesLine,
          final int upBorderWidth,
          final int scanLines,
          final int firstScrByte,
          final int lastScrUpdate,
          final int tStatesBorderAreaWidth,
          final int borderVisibleAreaHeight,
          final int borderTopHiddenLines,
          final int tstatesLeftBorderStart,
          final int[] contention
  ) {
    this.tStatesBorderAreaWidth = tStatesBorderAreaWidth;
    this.borderVisibleAreaHeight = borderVisibleAreaHeight;
    this.borderTopHiddenLines = borderTopHiddenLines;
    this.tstatesLeftBorderStart = tstatesLeftBorderStart;
    this.lengthNmi = lengthNmi;
    this.lengthInt = lengthInt;
    this.tstatesLine = tstatesLine;
    this.tstatesFrame = tstatesFrame;
    this.upBorderWidth = upBorderWidth;
    this.scanLines = scanLines;
    this.firstScrByte = firstScrByte;
    this.lastScrUpdate = lastScrUpdate;
    this.clockFreq = clockFreq;
    this.contention = contention;
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
      if (t < this.firstScrByte) {
        result[t] = TIMINGSTATE_BORDER;
      } else {
        final int displayOffset = t - this.firstScrByte;
        final int line = displayOffset / this.tstatesLine;
        final int pixel = displayOffset % this.tstatesLine;
        if (line < TimingProfile.ZX_SCREEN_LINES && pixel < this.tstatesLine) {
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
