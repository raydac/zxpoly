package com.igormaznitsa.zxpoly.components.video.tvfilters;

public class TvFilterGreenCrt extends AbstractMonochromeOldTvFilter {

  private static final TvFilterGreenCrt INSTANCE = new TvFilterGreenCrt();
  private static final int LEVEL_BASE_R = 0x18;
  private static final float LEVEL_STEP_R = ((float) (0x66 - LEVEL_BASE_R)) / 256.0f;
  private static final int LEVEL_BASE_G = 0x18;
  private static final float LEVEL_STEP_G = ((float) (0xFF - LEVEL_BASE_G)) / 256.0f;
  private static final int LEVEL_BASE_B = 0x18;
  private static final float LEVEL_STEP_B = ((float) (0x33 - LEVEL_BASE_G)) / 256.0f;

  private TvFilterGreenCrt() {
    super();
  }

  public static TvFilterGreenCrt getInstance() {
    return INSTANCE;
  }

  @Override
  protected int y2rgb(final int y) {
    final int r = LEVEL_BASE_R + Math.round(LEVEL_STEP_R * y);
    final int g = LEVEL_BASE_G + Math.round(LEVEL_STEP_G * y);
    final int b = LEVEL_BASE_B + Math.round(LEVEL_STEP_B * y);
    return (r << 16) | (g << 8) | b;
  }

}
