package com.igormaznitsa.zxpoly.components.video.timings;

public enum TimingProfile {
  SPEC48(
          0,
          32,
          16,
          3_500_000,
          128,
          32,
          16,
          32,
          16,
          8,
          56,
          56,
          new int[]{6, 5, 4, 3, 2, 1, 0, 0}
  ),
  SPEC128(
          0,
          32,
          16,
          3_546_900,
          128,
          34,
          16,
          32,
          18,
          8,
          55,
          56,
          new int[]{6, 5, 4, 3, 2, 1, 0, 0}
  );

  public static final int TIMINGSTATE_MASK_TYPE = 0xFF_0000_00;
  public static final int TIMINGSTATE_MASK_ATTR = 0x00_FFFF_00;
  public static final int TIMINGSTATE_MASK_TIME = 0x00_0000_FF;
  public static final int TIMINGSTATE_BORDER = 0x00_0000_00;
  public static final int TIMINGSTATE_PAPER = 0x01_0000_00;


  public static final int ZX_SCREEN_LINES = 192;
  public final int cpuFreq;
  public final int vsyncLines;
  public final int topBorderVisibleScanlines;
  public final int bottomBorderVisibleScanlines;
  public final int ulaTiStatesVideo;
  public final int ulaTiStatesRightBorder;
  public final int ulaTiStatesHSync;
  public final int ulaTiStatesBlank;
  public final int ulaTiStatesLeftBorder;
  public final int frameScanlines;
  public final int ulaScanLineTacts;
  public final int ulaFrameTiStates;
  public final int ulaTiStatesInt;
  public final int ulaTiStatesNmi;
  public final int ulaTiStatesIntOffset;
  public final int ulaTiStatesFirstByteOnScreen;
  private final int[] contention;

  TimingProfile(
          final int ulaTiStatesIntOffset,
          final int ulaTiStatesInt,
          final int ulaTiStatesNmi,
          final int cpuFreq,
          final int ulaTiStatesVideo,
          final int ulaTiStatesRightBorder,
          final int ulaTiStatesHSync,
          final int ulaTiStatesBlank,
          final int ulaTiStatesLeftBorder,
          final int vsyncLines,
          final int topBorderVisibleScanlines,
          final int bottomBorderVisibleScanlines,
          final int[] contention
  ) {
    this.ulaTiStatesIntOffset = ulaTiStatesIntOffset;
    this.contention = contention;
    this.ulaTiStatesInt = ulaTiStatesInt;
    this.ulaTiStatesNmi = ulaTiStatesNmi;
    this.cpuFreq = cpuFreq;
    this.vsyncLines = vsyncLines;
    this.topBorderVisibleScanlines = topBorderVisibleScanlines;
    this.bottomBorderVisibleScanlines = bottomBorderVisibleScanlines;
    this.ulaTiStatesVideo = ulaTiStatesVideo;
    this.ulaTiStatesRightBorder = ulaTiStatesRightBorder;
    this.ulaTiStatesHSync = ulaTiStatesHSync;
    this.ulaTiStatesBlank = ulaTiStatesBlank;
    this.ulaTiStatesLeftBorder = ulaTiStatesLeftBorder;

    this.frameScanlines = this.vsyncLines + this.topBorderVisibleScanlines + this.bottomBorderVisibleScanlines + ZX_SCREEN_LINES;

    this.ulaScanLineTacts = this.ulaTiStatesVideo + this.ulaTiStatesRightBorder + this.ulaTiStatesHSync + this.ulaTiStatesBlank + this.ulaTiStatesLeftBorder;
    this.ulaFrameTiStates = this.ulaScanLineTacts * this.frameScanlines;

    this.ulaTiStatesFirstByteOnScreen = (this.vsyncLines + this.topBorderVisibleScanlines) * this.ulaScanLineTacts;
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

    final int[] result = new int[this.ulaFrameTiStates];

    for (int t = 0; t < this.ulaFrameTiStates; t++) {
      if (t < this.ulaTiStatesFirstByteOnScreen) {
        result[t] = TIMINGSTATE_BORDER;
      } else {
        final int displayOffset = t - this.ulaTiStatesFirstByteOnScreen;
        final int line = displayOffset / this.ulaScanLineTacts;
        final int pixel = displayOffset % this.ulaScanLineTacts;
        if (line < TimingProfile.ZX_SCREEN_LINES && pixel < this.ulaTiStatesVideo) {
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
