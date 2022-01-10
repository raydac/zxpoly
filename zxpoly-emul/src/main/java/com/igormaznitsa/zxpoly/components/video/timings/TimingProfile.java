package com.igormaznitsa.zxpoly.components.video.timings;

public enum TimingProfile {
  SPEC48(
          16,
          3_500_000,
          128,
          32,
          16,
          32,
          16,
          8,
          56,
          56
  ),
  SPEC128(
          16,
          3_546_900,
          128,
          34,
          16,
          32,
          18,
          8,
          55,
        56
);

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
  public final int ulaInterruptTiStates;
  public final int ulaTiStatesFirstByteOnScreen;

  TimingProfile(
          final int ulaInterruptTiStates,
          final int cpuFreq,
          final int ulaTiStatesVideo,
          final int ulaTiStatesRightBorder,
          final int ulaTiStatesHSync,
          final int ulaTiStatesBlank,
          final int ulaTiStatesLeftBorder,
          final int vsyncLines,
          final int topBorderVisibleScanlines,
          final int bottomBorderVisibleScanlines
  ) {
    this.ulaInterruptTiStates = ulaInterruptTiStates;
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
}
