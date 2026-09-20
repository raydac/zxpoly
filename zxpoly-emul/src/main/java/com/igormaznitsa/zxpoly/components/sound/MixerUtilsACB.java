package com.igormaznitsa.zxpoly.components.sound;

import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_AY_A;
import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_AY_B;
import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_AY_C;
import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_BEEPER;
import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_COVOX;
import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_TS_A;
import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_TS_B;
import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_TS_C;

/**
 * Mixer set East Europe standard (A left, C center, B right)
 */
public final class MixerUtilsACB extends MixerUtils {
  private MixerUtilsACB() {
    super();
  }

  public static int mixLeft_TS_CVX(final int[] values) {
    final int middle = values[CHANNEL_COVOX]
        + values[CHANNEL_BEEPER]
        + values[CHANNEL_AY_C]
        + values[CHANNEL_TS_C];

    final int left = values[CHANNEL_TS_A] + values[CHANNEL_AY_A];

    return scaleLeft6(left, middle);
  }

  public static int mixRight_TS_CVX(final int[] values) {
    final int middle = values[CHANNEL_COVOX]
        + values[CHANNEL_BEEPER]
        + values[CHANNEL_AY_C]
        + values[CHANNEL_TS_C];

    final int right = values[CHANNEL_AY_B] + values[CHANNEL_TS_B];

    return scaleRight6(right, middle);
  }

  public static int mixLeft_CVX(final int[] values) {
    final int middle = values[CHANNEL_COVOX]
        + values[CHANNEL_BEEPER]
        + values[CHANNEL_AY_C];

    final int left = values[CHANNEL_AY_A];

    return scaleLeft4(left, middle);
  }

  public static int mixRight_CVX(final int[] values) {
    final int middle = values[CHANNEL_COVOX]
        + values[CHANNEL_BEEPER]
        + values[CHANNEL_AY_C];

    final int right = values[CHANNEL_AY_B];

    return scaleRight4(right, middle);
  }

  public static int mixLeft_TS(final int[] values) {
    final int middle = values[CHANNEL_BEEPER]
        + values[CHANNEL_AY_C]
        + values[CHANNEL_TS_C];

    final int left = values[CHANNEL_TS_A] + values[CHANNEL_AY_A];

    return scaleLeft5(left, middle);
  }

  public static int mixRight_TS(final int[] values) {
    final int middle = values[CHANNEL_BEEPER]
        + values[CHANNEL_AY_C]
        + values[CHANNEL_TS_C];

    final int right = values[CHANNEL_AY_B] + values[CHANNEL_TS_B];

    return scaleRight5(right, middle);
  }

  public static int mixLeft(final int[] values) {
    final int middle = values[CHANNEL_BEEPER] + values[CHANNEL_AY_C];
    final int left = values[CHANNEL_AY_A];

    return scaleLeft3(left, middle);
  }

  public static int mixRight(final int[] values) {
    final int middle = values[CHANNEL_BEEPER] + values[CHANNEL_AY_C];
    final int right = values[CHANNEL_AY_B];

    return scaleRight3(right, middle);
  }

}
