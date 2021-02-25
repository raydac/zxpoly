package com.igormaznitsa.zxpoly.components.video.tvfilters;

public class TvFilterAmberCrt extends AbstractMonochromeOldTvFilter {

  private static final TvFilterAmberCrt INSTANCE = new TvFilterAmberCrt();
  private static final int LEVEL_BASE_R = 0x18;
  private static final float LEVEL_STEP_R = ((float) (0xFF - LEVEL_BASE_R)) / 256.0f;
  private static final int LEVEL_BASE_G = 0x18;
  private static final float LEVEL_STEP_G = ((float) (0xCC - LEVEL_BASE_G)) / 256.0f;
  private static final int LEVEL_BASE_B = 0x18;
  private static final float LEVEL_STEP_B = ((float) (0x38 - LEVEL_BASE_G)) / 256.0f;

  private TvFilterAmberCrt() {
    super();
  }

  public static TvFilterAmberCrt getInstance() {
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
