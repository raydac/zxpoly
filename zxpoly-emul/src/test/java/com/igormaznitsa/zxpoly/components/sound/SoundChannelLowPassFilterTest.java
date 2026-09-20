package com.igormaznitsa.zxpoly.components.sound;

import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_AY_A;
import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_AY_B;
import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_AY_C;
import static com.igormaznitsa.zxpoly.components.sound.Beeper.CHANNEL_BEEPER;
import static com.igormaznitsa.zxpoly.components.sound.SoundChannelLowPassFilter.OFF;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SoundChannelLowPassFilterTest {

  @Test
  public void testOffPassesSampleUnchanged() {
    final SoundChannelLowPassFilter filter = new SoundChannelLowPassFilter(0, OFF);

    assertEquals(180, filter.update(4, 180));
    assertEquals(0, filter.update(7, 0));
  }

  @Test
  public void testUnityAlphaPassesSampleUnchanged() {
    final SoundChannelLowPassFilter filter = new SoundChannelLowPassFilter(0, 1.0f);

    assertEquals(200, filter.update(4, 200));
    assertEquals(10, filter.update(4, 10));
  }

  @Test
  public void testZeroAlphaKeepsSilenceAfterReset() {
    final SoundChannelLowPassFilter filter = new SoundChannelLowPassFilter(0, 0.0f);

    assertEquals(0, filter.update(4, 255));
    assertEquals(0, filter.update(4, 255));
  }

  @Test
  public void testFirstOrderStep() {
    final SoundChannelLowPassFilter filter = new SoundChannelLowPassFilter(0, 0.5f);

    assertEquals(50, filter.update(4, 100));
    assertEquals(75, filter.update(4, 100));
  }

  @Test
  public void testZeroTiStatesDoesNotAdvance() {
    final SoundChannelLowPassFilter filter = new SoundChannelLowPassFilter(0, 0.5f);

    assertEquals(50, filter.update(4, 100));
    assertEquals(50, filter.update(0, 100));
    assertEquals(50, filter.update(-1, 255));
    assertEquals(75, filter.update(4, 100));
  }

  @Test
  public void testAlphaAboveOneIsClampedToPassthrough() {
    final SoundChannelLowPassFilter filter = new SoundChannelLowPassFilter(0, 2.0f);

    assertEquals(90, filter.update(4, 90));
  }

  @Test
  public void testResetClearsHistory() {
    final SoundChannelLowPassFilter filter = new SoundChannelLowPassFilter(0, 0.5f);

    filter.update(4, 100);
    filter.reset();

    assertEquals(50, filter.update(4, 100));
  }

  @Test
  public void testSecondUpdateOnSameSampleAdvancesFilterAgain() {
    final SoundChannelLowPassFilter filter = new SoundChannelLowPassFilter(0, 0.5f);

    final int first = filter.update(4, 100);
    final int second = filter.update(4, 100);

    assertEquals(50, first);
    assertEquals(75, second);
    assertNotEquals(first, second);
  }

  @Test
  public void testAbcMixerKeepsCenterChannelBalanced() {
    final int[] values = new int[8];
    values[CHANNEL_BEEPER] = 200;
    values[CHANNEL_AY_B] = 80;

    assertEquals(MixerUtilsABC.mixLeft(values), MixerUtilsABC.mixRight(values));
  }

  @Test
  public void testAbcMixerKeepsSideChannelsOnTheirSpeakers() {
    final int[] values = new int[8];
    values[CHANNEL_AY_A] = 180;
    values[CHANNEL_AY_C] = 40;

    assertNotEquals(MixerUtilsABC.mixLeft(values), MixerUtilsABC.mixRight(values));
  }

  @Test
  public void testAlphaFromLevelGoesFromMildToStrong() {
    final float mild = SoundChannelLowPassFilter.alphaFromLevel(0);
    final float medium = SoundChannelLowPassFilter.alphaFromLevel(50);
    final float strong =
        SoundChannelLowPassFilter.alphaFromLevel(SoundChannelLowPassFilter.LEVEL_MAX);

    assertEquals(0.25f, mild, 1.0e-4f);
    assertEquals(0.012f, strong, 1.0e-4f);
    assertTrue(mild > medium);
    assertTrue(medium > strong);
    assertEquals(mild, SoundChannelLowPassFilter.alphaFromLevel(-10), 0.0f);
    assertEquals(strong, SoundChannelLowPassFilter.alphaFromLevel(10_000), 0.0f);
  }

  @Test
  public void testAlphaFromLevelUsesWholeSliderRange() {
    float previous = Float.POSITIVE_INFINITY;
    for (int level = 0; level <= SoundChannelLowPassFilter.LEVEL_MAX; level++) {
      final float alpha = SoundChannelLowPassFilter.alphaFromLevel(level);
      assertTrue(alpha < previous);
      previous = alpha;
    }
  }
}
