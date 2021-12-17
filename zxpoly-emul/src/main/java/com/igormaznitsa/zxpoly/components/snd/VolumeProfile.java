package com.igormaznitsa.zxpoly.components.snd;

public enum VolumeProfile {
  EXPONENTIAL(new int[]{0x00, 0x03, 0x05, 0x07, 0x0A, 0x0F, 0x15, 0x23, 0x29, 0x41, 0x5B, 0x72, 0x90, 0xB5, 0xD8, 0xFF}),
  EXPONENTIAL_INVERSE(new int[]{0xFF, 0xD8, 0xB5, 0x90, 0x72, 0x5B, 0x41, 0x29, 0x23, 0x15, 0x0F, 0x0A, 0x07, 0x05, 0x03, 0x00}),
  LINEAR(new int[]{0, 17, 34, 51, 68, 85, 102, 119, 136, 153, 170, 187, 204, 221, 238, 255}),
  LINEAR_INVERSE(new int[]{255, 238, 221, 204, 187, 170, 153, 136, 119, 102, 85, 68, 51, 34, 17, 0});
  private final int[] levels;

  VolumeProfile(final int[] levels) {
    this.levels = levels;
  }

  public int[] getLevels() {
    return this.levels;
  }
}
