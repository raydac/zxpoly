package com.igormaznitsa.zxpoly.components.video.timings;

public enum TimingProfile {
  SCORPION(
          224,
          64,
          61,
          70784,
          48,
          48,
          16,
          16,
          61,
          32
  ),
  PENTAGON(
          224,
          80,
          65,
          71680,
          48,
          48,
          16,
          16,
          0,
          32
  ),
  SPEC48(
          224,
          64,
          64,
          69888,
          48,
          48,
          16,
          16,
          64,
          32
  ),
  SPEC128(

          228,
          63,
          64,
          70908,
          48,
          48,
          16,
          16,
          64 + 2,
          36
  );

  public final int ulaLineTime;
  public final int ulaFirstPaperLine;
  public final int ulaFirstPaperTact;
  public final int ulaFrameTact;
  public final int ulaBorderLinesTop;
  public final int ulaBorderLinesBottom;
  public final int ulaBorderTiLeft;
  public final int ulaBorderTiRight;
  public final int ulaIntBegin;
  public final int ulaIntLength;

  public final int ulaVisibleRows;
  public final int ulaTotalRows;
  public final int ulaFirstVisibleRow;

  public final int tstatesInFramePaperStart;
  public final int tstatesInBottomBorderStart;
  public final int tstatesPaperLineTime;
  public final int tstatesBorderStart;

  TimingProfile(
          final int ulaLineTime,
          final int ulaFirstPaperLine,
          final int ulaFirstPaperTact,
          final int ulaFrameTact,
          final int ulaBorderLinesTop,
          final int ulaBorderLinesBottom,
          final int ulaBorderTiLeft,
          final int ulaBorderTiRight,
          final int ulaIntBegin,
          final int ulaIntLength
  ) {
    this.ulaLineTime = ulaLineTime;
    this.ulaFirstPaperLine = ulaFirstPaperLine;
    this.ulaFirstPaperTact = ulaFirstPaperTact;
    this.ulaFrameTact = ulaFrameTact;
    this.ulaBorderLinesTop = ulaBorderLinesTop;
    this.ulaBorderLinesBottom = ulaBorderLinesBottom;
    this.ulaBorderTiLeft = ulaBorderTiLeft;
    this.ulaBorderTiRight = ulaBorderTiRight;
    this.ulaIntBegin = ulaIntBegin;
    this.ulaIntLength = ulaIntLength;

    this.ulaVisibleRows = ulaBorderLinesTop + ulaBorderLinesBottom + 192;
    this.ulaTotalRows = ulaFrameTact / ulaLineTime;
    this.ulaFirstVisibleRow = this.ulaTotalRows - this.ulaVisibleRows;

    this.tstatesPaperLineTime = 128;
    this.tstatesBorderStart = (this.ulaFirstPaperLine - this.ulaBorderLinesTop) * ulaLineTime;
    this.tstatesInFramePaperStart = this.ulaFirstPaperLine * this.ulaLineTime + this.ulaFirstPaperTact;
    this.tstatesInBottomBorderStart = (this.ulaFirstPaperLine + 193) * this.ulaLineTime;
  }
}
