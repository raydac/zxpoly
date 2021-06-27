package com.igormaznitsa.zxpoly.components.snd;

import static com.igormaznitsa.zxpoly.components.snd.Beeper.*;

/**
 * Mixer set East Europe standard (A left, C center, B right)
 */
public final class MixerUtilsACB {
  private MixerUtilsACB() {
  }

  public static int mixLeft_TS_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX]);
    mixed += filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);
    mixed += filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_TS_A].update(spentTstates, values[CHANNEL_TS_A])
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    final int maxValue = (4 * 255) / 2 + 2 * 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixRight_TS_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX]);
    mixed += filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);
    mixed += filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B])
            + filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    final int maxValue = (4 * 255) / 2 + 2 * 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixLeft_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX]);
    mixed += filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    final int maxValue = (3 * 255) / 2 + 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixRight_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX]);
    mixed += filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    final int maxValue = (3 * 255) / 2 + 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixLeft_TS(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);
    mixed += filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_TS_A].update(spentTstates, values[CHANNEL_TS_A])
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    final int maxValue = (3 * 255) / 2 + 2 * 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixRight_TS(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);
    mixed += filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B])
            + filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    final int maxValue = (3 * 255) / 2 + 2 * 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixLeft(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    final int maxValue = (2 * 255) / 2 + 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixRight(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    final int maxValue = (2 * 255) / 2 + 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

}
