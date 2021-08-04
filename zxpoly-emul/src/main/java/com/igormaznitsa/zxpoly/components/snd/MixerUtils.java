package com.igormaznitsa.zxpoly.components.snd;

public abstract class MixerUtils {

  private static final float SCALE_FACTOR = Short.MAX_VALUE / 255.0f;

  protected static int scaleLeft(final int left, final int center) {
    final float mixed = left * 0.5f + center * 0.45f;
    return Math.min(Short.MAX_VALUE, Math.round(mixed * SCALE_FACTOR));
  }

  protected static int scaleRight(final int right, final int center) {
    final float mixed = right * 0.5f + center * 0.45f;
    return Math.min(Short.MAX_VALUE, Math.round(mixed * SCALE_FACTOR));
  }

}
