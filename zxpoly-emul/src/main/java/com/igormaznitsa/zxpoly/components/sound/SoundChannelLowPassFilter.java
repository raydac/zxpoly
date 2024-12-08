package com.igormaznitsa.zxpoly.components.sound;

public final class SoundChannelLowPassFilter {

  public static final float COEFF = 1000.0f;
  public static final float OFF = -1.0f;

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
      this.alpha1 = level;
      this.alpha2 = 1.0f - level;
    }
    this.reset();
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
    if (this.active) {
      float filteredValue = alpha1 * (float) nextLevel + this.alpha2 * previousFilteredValue;
      this.previousFilteredValue = filteredValue;
      return Math.max(0, Math.min(255, Math.round(filteredValue)));
    } else {
      return nextLevel;
    }
  }

}
