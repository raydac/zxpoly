package com.igormaznitsa.zxpoly.components.snd;

import static com.igormaznitsa.zxpoly.components.snd.Beeper.*;

/**
 * Mixer set AY west-Europe (A left, B center, C right)
 */
public final class MixerUtilsABC extends MixerUtils {
  private MixerUtilsABC() {
    super();
  }

  public static int mixLeft_TS_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX])
            + filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B])
            + filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    final int left = filters[CHANNEL_TS_A].update(spentTstates, values[CHANNEL_TS_A])
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    return scaleLeft6(left, middle);
  }

  public static int mixRight_TS_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX])
            + filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B])
            + filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    final int right = filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C])
            + filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    return scaleRight6(right, middle);
  }

  public static int mixLeft_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX])
            + filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    final int left = filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    return scaleLeft4(left, middle);
  }

  public static int mixRight_CVX(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_COVOX].update(spentTstates, values[CHANNEL_COVOX])
            + filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    final int right = filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);

    return scaleRight4(right, middle);
  }

  public static int mixLeft_TS(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B])
            + filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    final int left = filters[CHANNEL_TS_A].update(spentTstates, values[CHANNEL_TS_A])
            + filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    return scaleLeft5(left, middle);
  }

  public static int mixRight_TS(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B])
            + filters[CHANNEL_TS_B].update(spentTstates, values[CHANNEL_TS_B]);

    final int right = filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C])
            + filters[CHANNEL_TS_C].update(spentTstates, values[CHANNEL_TS_C]);

    return scaleRight5(right, middle);
  }

  public static int mixLeft(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    final int left = filters[CHANNEL_AY_A].update(spentTstates, values[CHANNEL_AY_A]);

    return scaleLeft3(left, middle);
  }

  public static int mixRight(final int[] values, final SoundChannelValueFilter[] filters, final int spentTstates) {
    final int middle = filters[CHANNEL_BEEPER].update(spentTstates, values[CHANNEL_BEEPER])
            + filters[CHANNEL_AY_B].update(spentTstates, values[CHANNEL_AY_B]);

    final int right = filters[CHANNEL_AY_C].update(spentTstates, values[CHANNEL_AY_C]);

    return scaleRight3(right, middle);
  }

}
