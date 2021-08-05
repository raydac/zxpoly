package com.igormaznitsa.zxpoly.components.snd;

import static com.igormaznitsa.zxpoly.components.snd.Beeper.*;

/**
 * Mixer set East Europe standard (A left, C center, B right)
 */
public final class MixerUtilsACB extends MixerUtils {
  private MixerUtilsACB() {
    super();
  }

  public static int mixLeft_TS_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX])
            + filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C])
            + filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    final int left = filters[CHANNEL_TS_A].update(spentTstates, values[CHANNEL_TS_A])
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    return scaleLeft6(left, middle);
  }

  public static int mixRight_TS_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX])
            + filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C])
            + filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    final int right = filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B])
            + filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    return scaleRight6(right, middle);
  }

  public static int mixLeft_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX])
            + filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);

    final int left = filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    return scaleLeft4(left, middle);
  }

  public static int mixRight_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX])
            + filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);

    final int right = filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    return scaleRight4(right, middle);
  }

  public static int mixLeft_TS(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C])
            + filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    final int left = filters[CHANNEL_TS_A].update(spentTstates, values[CHANNEL_TS_A])
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    return scaleLeft5(left, middle);
  }

  public static int mixRight_TS(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C])
            + filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    final int right = filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B])
            + filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    return scaleRight5(right, middle);
  }

  public static int mixLeft(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);
    final int left = filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    return scaleLeft3(left, middle);
  }

  public static int mixRight(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);

    final int right = filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    return scaleRight3(right, middle);
  }

}
