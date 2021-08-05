package com.igormaznitsa.zxpoly.components.snd;

public abstract class MixerUtils {

  private static final float SCALE_FACTOR_6 = Short.MAX_VALUE / (255.0f * 6);
  private static final float SCALE_FACTOR_5 = Short.MAX_VALUE / (255.0f * 5);
  private static final float SCALE_FACTOR_4 = Short.MAX_VALUE / (255.0f * 4);
  private static final float SCALE_FACTOR_3 = Short.MAX_VALUE / (255.0f * 3);

  protected static int scaleLeft6(final int left, final int center) {
    final float mixed = left * 0.5f + center * 0.45f;
    return Math.min(Short.MAX_VALUE, Math.round(mixed * SCALE_FACTOR_6));
  }

  protected static int scaleRight6(final int right, final int center) {
    final float mixed = right * 0.5f + center * 0.45f;
    return Math.min(Short.MAX_VALUE, Math.round(mixed * SCALE_FACTOR_6));
  }

  protected static int scaleLeft5(final int left, final int center) {
    final float mixed = left * 0.5f + center * 0.45f;
    return Math.min(Short.MAX_VALUE, Math.round(mixed * SCALE_FACTOR_5));
  }

  protected static int scaleRight5(final int right, final int center) {
    final float mixed = right * 0.5f + center * 0.45f;
    return Math.min(Short.MAX_VALUE, Math.round(mixed * SCALE_FACTOR_5));
  }

  protected static int scaleLeft4(final int left, final int center) {
    final float mixed = left * 0.5f + center * 0.45f;
    return Math.min(Short.MAX_VALUE, Math.round(mixed * SCALE_FACTOR_4));
  }

  protected static int scaleRight4(final int right, final int center) {
    final float mixed = right * 0.5f + center * 0.45f;
    return Math.min(Short.MAX_VALUE, Math.round(mixed * SCALE_FACTOR_4));
  }

  protected static int scaleLeft3(final int left, final int center) {
    final float mixed = left * 0.5f + center * 0.45f;
    return Math.min(Short.MAX_VALUE, Math.round(mixed * SCALE_FACTOR_3));
  }

  protected static int scaleRight3(final int right, final int center) {
    final float mixed = right * 0.5f + center * 0.45f;
    return Math.min(Short.MAX_VALUE, Math.round(mixed * SCALE_FACTOR_3));
  }

}
