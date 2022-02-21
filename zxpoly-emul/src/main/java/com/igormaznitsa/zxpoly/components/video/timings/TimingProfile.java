package com.igormaznitsa.zxpoly.components.video.timings;

public enum TimingProfile {
  PENTAGON128(
          3_500_000,
          0,
          32,
          16,
          16,
          48,
          64,
          128,
          32,
          16,
          16,
          32,
          new int[]{0, 0, 0, 0, 0, 0, 0, 0}
  ),
  SPECTRUM128(
          3_546_900,
          64 + 2,
          36,
          16,
          8,
          55,
          56,
          128,
          26,
          14,
          34,
          26,
          new int[]{6, 5, 4, 3, 2, 1, 0, 0}
  );

  private static final int ZX_SCREEN_LINES = 192;
  public final int ulaIntBegin;
  public final int lengthInt;
  public final int lengthNmi;
  public final int clockFreq;
  public final int tstatesFrame;
  public final int scanLines;
  public final int tstatesPerLine;
  public final int tstatesStartScreen;
  public final int tstatesPerVideo;
  public final int tstatesPerBorderLeft;
  public final int tstatesPerBorderRight;
  public final int tstatesPerHSync;
  public final int tstatesPerHBlank;
  public final int linesBorderTop;
  public final int linesBorderBottom;
  public final int linesPerVSync;
  public final int ulaTstatesScreenWidth;
  public final int ulaFirstPaperTact;
  public final int ulaFirstPaperLine;
  public final int displayRows;
  private final int[] contention;

  TimingProfile(
          final int clockFreq,
          final int ulaIntBegin,
          final int lengthInt,
          final int lengthNmi,
          final int linesVsync,
          final int linesBorderTop,
          final int linesBorderBottom,
          final int tstatesPerVideo,
          final int tstatesPerBorderLeft,
          final int tstatesPerHSync,
          final int tstatesPerHBlank,
          final int tstatesPerBorderRight,
          final int[] contention
  ) {
    this.ulaIntBegin = ulaIntBegin;
    this.lengthNmi = lengthNmi;
    this.lengthInt = lengthInt;
    this.clockFreq = clockFreq;
    this.contention = contention;

    this.linesPerVSync = linesVsync;
    this.scanLines = linesVsync + linesBorderTop + ZX_SCREEN_LINES + linesBorderBottom;
    this.tstatesPerLine = tstatesPerVideo + tstatesPerBorderLeft + tstatesPerHSync + tstatesPerHBlank + tstatesPerBorderRight;
    this.tstatesFrame = this.tstatesPerLine * this.scanLines;

    this.tstatesPerVideo = tstatesPerVideo;
    this.tstatesPerHSync = tstatesPerHSync;
    this.tstatesPerHBlank = tstatesPerHBlank;
    this.tstatesPerBorderLeft = tstatesPerBorderLeft;
    this.tstatesPerBorderRight = tstatesPerBorderRight;
    this.linesBorderBottom = linesBorderBottom;
    this.linesBorderTop = linesBorderTop;

    this.ulaTstatesScreenWidth = (this.tstatesPerBorderLeft + this.tstatesPerVideo + this.tstatesPerBorderRight) << 1;
    this.displayRows = this.linesBorderTop + ZX_SCREEN_LINES + this.linesBorderBottom;
    this.ulaFirstPaperTact = this.tstatesPerHBlank + this.tstatesPerHSync + this.tstatesPerBorderLeft;
    this.ulaFirstPaperLine = this.linesPerVSync + this.linesBorderTop;

    this.tstatesStartScreen = this.tstatesPerLine * (linesVsync + linesBorderTop) + this.ulaFirstPaperTact;
  }

  private static int calcAddressAttribute(int sx, int sy) {
    sx >>= 2;
    var ap = sx | ((sy >> 3) << 5);
    return 6144 + ap;
  }

  private static int calcAddressPixelSource(int sx, int sy) {
    sx >>= 2;
    var vp = sx | (sy << 5);
    return (vp & 0x181F) | ((vp & 0x0700) >> 3) | ((vp & 0x00E0) << 3);
  }

  public UlaTact[] makeUlaFrame() {
    final UlaTact[] result = new UlaTact[this.tstatesFrame];

    for (int t = 0; t < this.tstatesFrame; t++) {
      final int frameLine = t / this.tstatesPerLine;
      final int framePixel = t % this.tstatesPerLine;

      result[t] = makeTact(t, frameLine, framePixel);
    }

    return result;
  }

  private int makeContention(final int t) {
    int shifted = (t + 1) + this.ulaIntBegin;
    // check overflow
    if (shifted < 0) {
      shifted += this.tstatesFrame;
    }
    shifted %= this.tstatesFrame;

    int line = shifted / this.tstatesPerLine;
    int pix = shifted % this.tstatesPerLine;
    if (line < this.ulaFirstPaperLine || line >= (this.ulaFirstPaperLine + ZX_SCREEN_LINES)) {
      return 0;
    }
    int scrPix = pix - this.ulaFirstPaperTact;
    if (scrPix < 0 || scrPix >= this.tstatesPerVideo) {
      return 0;
    }
    return this.contention[scrPix % 8];
  }

  private UlaTact makeTact(int item, int line, int pix) {
    int pitchWidth = this.ulaTstatesScreenWidth;

    int scrPix = pix - this.ulaFirstPaperTact;
    int scrLin = line - this.ulaFirstPaperLine;

    int resultUlaAction = UlaTact.TYPE_NONE;
    int resultUlaAddressAttribute = 0;
    int resultUlaAddressPixel = 0;
    int resultLineOffset = 0;

    if ((line >= (this.ulaFirstPaperLine - this.linesBorderTop)) && (line < (this.ulaFirstPaperLine + ZX_SCREEN_LINES + this.linesBorderBottom)) &&
            (pix >= (this.ulaFirstPaperTact - this.tstatesPerBorderLeft)) && (pix < (this.ulaFirstPaperTact + this.tstatesPerVideo + this.tstatesPerBorderRight))) {
      // visibleArea (vertical)
      if ((line >= this.ulaFirstPaperLine) && (line < (this.ulaFirstPaperLine + ZX_SCREEN_LINES)) &&
              (pix >= this.ulaFirstPaperTact) && (pix < (this.ulaFirstPaperTact + this.tstatesPerVideo))) {
        // pixel area
        switch (scrPix & 7) {
          case 0:
            resultUlaAction = UlaTact.TYPE_SHIFT1_AND_FETCH_B2;   // shift 1 + fetch B2
            // +4 = prefetch!
            resultUlaAddressPixel = calcAddressPixelSource(scrPix + 4, scrLin);
            break;
          case 1:
            resultUlaAction = UlaTact.TYPE_SHIFT1_AND_FETCH_A2;   // shift 1 + fetch A2
            // +3 = prefetch!
            resultUlaAddressAttribute = calcAddressAttribute(scrPix + 3, scrLin);
            break;
          case 2:
            resultUlaAction = UlaTact.TYPE_SHIFT1;   // shift 1
            break;
          case 3:
            resultUlaAction = UlaTact.TYPE_SHIFT1_LAST;   // shift 1 (last)
            break;
          case 4:
          case 5:
            resultUlaAction = UlaTact.TYPE_SHIFT2;   // shift 2
            break;
          case 6:
            if (pix < (this.ulaFirstPaperTact + this.tstatesPerVideo - 2)) {
              resultUlaAction = UlaTact.TYPE_SHIFT2_AND_FETCH_B1;   // shift 2 + fetch B2
            } else {
              resultUlaAction = UlaTact.TYPE_SHIFT2;             // shift 2
            }

            // +2 = prefetch!
            resultUlaAddressPixel = calcAddressPixelSource(scrPix + 2, scrLin);
            break;
          case 7:
            if (pix < (this.ulaFirstPaperTact + this.tstatesPerVideo - 2)) {
              //???
              resultUlaAction = UlaTact.TYPE_SHIFT2_AND_FETCH_A1;   // shift 2 + fetch A2
            } else {
              resultUlaAction = UlaTact.TYPE_SHIFT2;             // shift 2
            }

            // +1 = prefetch!
            resultUlaAddressAttribute = calcAddressAttribute(scrPix + 1, scrLin);
            break;
        }
      } else if ((line >= this.ulaFirstPaperLine) && (line < (this.ulaFirstPaperLine + ZX_SCREEN_LINES)) &&
              (pix == (this.ulaFirstPaperTact - 2)))  // border & fetch B1
      {
        resultUlaAction = UlaTact.TYPE_BORDER_FETCH_B1; // border & fetch B1
        // +2 = prefetch!
        resultUlaAddressPixel = calcAddressPixelSource(scrPix + 2, scrLin);
      } else if ((line >= this.ulaFirstPaperLine) && (line < (this.ulaFirstPaperLine + ZX_SCREEN_LINES)) &&
              (pix == (this.ulaFirstPaperTact - 1)))  // border & fetch A1
      {
        resultUlaAction = UlaTact.TYPE_BORDER_FETCH_A1; // border & fetch A1
        // +1 = prefetch!
        resultUlaAddressAttribute = calcAddressAttribute(scrPix + 1, scrLin);
      } else {
        resultUlaAction = UlaTact.TYPE_BORDER; // border
      }

      int wy = line - (this.ulaFirstPaperLine - this.linesBorderTop);
      int wx = (pix - (this.ulaFirstPaperTact - this.tstatesPerBorderLeft)) * 2;
      resultLineOffset = wy * pitchWidth + wx;
    }
    return new UlaTact(
            resultUlaAction,
            resultLineOffset,
            resultUlaAddressPixel,
            resultUlaAddressAttribute,
            makeContention(item));
  }

  public static class UlaTact {
    public static final int TYPE_NONE = 0;
    public static final int TYPE_BORDER = 1;
    public static final int TYPE_BORDER_FETCH_B1 = 2;
    public static final int TYPE_BORDER_FETCH_A1 = 3;
    public static final int TYPE_SHIFT1 = 4;
    public static final int TYPE_SHIFT1_AND_FETCH_B2 = 5;
    public static final int TYPE_SHIFT1_AND_FETCH_A2 = 6;
    public static final int TYPE_SHIFT1_LAST = 7;
    public static final int TYPE_SHIFT2 = 8;
    public static final int TYPE_SHIFT2_AND_FETCH_B1 = 9;
    public static final int TYPE_SHIFT2_AND_FETCH_A1 = 10;

    public final int type;
    public final int lineOffset;
    public final int addressPixel;
    public final int addressAttribute;
    public final int contention;

    private UlaTact(final int type,
                    final int lineOffset,
                    final int addressPixel,
                    final int addressAttribute,
                    final int contention) {
      this.type = type;
      this.lineOffset = lineOffset;
      this.addressPixel = addressPixel;
      this.addressAttribute = addressAttribute;
      this.contention = contention;
    }
  }
}
