package com.igormaznitsa.zxpoly.components.sound;

public final class SoundChannelLowPassFilter {

  public static final int LEVEL_MAX = 100;
  public static final int LEVEL_DEFAULT = 30;
  public static final float OFF = -1.0f;

  private static final int SAMPLE_MIN = 0;
  private static final int SAMPLE_MAX = 255;
  private static final float ALPHA_MILD = 0.25f;
  private static final float ALPHA_STRONG = 0.012f;

  private final float alpha1;
  private final float alpha2;
  private final int channelIndex;
  private final boolean active;
  private float previousFilteredValue;

  public SoundChannelLowPassFilter(final int channelIndex, final float level) {
    this.channelIndex = channelIndex;
    if (level < 0.0f) {
      this.active = false;
      this.alpha1 = 0.0f;
      this.alpha2 = 0.0f;
    } else {
      this.active = true;
      this.alpha1 = Math.clamp(level, 0.0f, 1.0f);
      this.alpha2 = 1.0f - this.alpha1;
    }
    this.reset();
  }

  public static float alphaFromLevel(final int level) {
    final float strength = Math.clamp(level, 0, LEVEL_MAX) / (float) LEVEL_MAX;
    return (float) (ALPHA_MILD * Math.pow(ALPHA_STRONG / (double) ALPHA_MILD, strength));
  }

  public boolean isActive() {
    return this.active;
  }

  public int getChannelIndex() {
    return this.channelIndex;
  }

  public void reset() {
    this.previousFilteredValue = 0.0f;
  }

  public int update(final int spentTiStates, final int nextLevel) {
    if (!this.active) {
      return nextLevel;
    }
    if (spentTiStates <= 0) {
      return this.clampSample(this.previousFilteredValue);
    }

    final float filteredValue =
        this.alpha1 * (float) nextLevel + this.alpha2 * this.previousFilteredValue;
    this.previousFilteredValue = filteredValue;
    return this.clampSample(filteredValue);
  }

  private int clampSample(final float value) {
    return Math.clamp(Math.round(value), SAMPLE_MIN, SAMPLE_MAX);
  }

}
