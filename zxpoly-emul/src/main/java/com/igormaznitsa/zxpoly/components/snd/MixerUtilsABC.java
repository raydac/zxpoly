package com.igormaznitsa.zxpoly.components.snd;

import static com.igormaznitsa.zxpoly.components.snd.Beeper.*;

/**
 * Mixer set AY west-Europe (A left, B center, C right)
 */
public final class MixerUtilsABC {
  private MixerUtilsABC() {

  }

  public static int mixLeft_TS_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX]);
    mixed += filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);
    mixed += filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_TS_A].update(spentTstates, values[CHANNEL_TS_A])
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    final int maxValue = (4 * 255) / 2 + 2 * 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixRight_TS_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX]);
    mixed += filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);
    mixed += filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C])
            + filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    final int maxValue = (4 * 255) / 2 + 2 * 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixLeft_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX]);
    mixed += filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    final int maxValue = (3 * 255) / 2 + 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixRight_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX]);
    mixed += filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);

    final int maxValue = (3 * 255) / 2 + 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixLeft_TS(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);
    mixed += filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_TS_A].update(spentTstates, values[CHANNEL_TS_A])
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    final int maxValue = (3 * 255) / 2 + 2 * 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixRight_TS(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);
    mixed += filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C])
            + filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    final int maxValue = (3 * 255) / 2 + 2 * 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixLeft(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    final int maxValue = (2 * 255) / 2 + 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

  public static int mixRight(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    int mixed = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER]);
    mixed += filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    mixed = (mixed >> 1)
            + filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);

    final int maxValue = (2 * 255) / 2 + 255;
    return mixed * (Short.MAX_VALUE / maxValue) - (maxValue / 2);
  }

}
