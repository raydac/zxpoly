package com.igormaznitsa.zxpoly.components.video;

import java.awt.Color;
import java.util.Arrays;

public final class UlaPlusContainer {
  private final byte[] palette = new byte[64];
  private final int[] paletteRgb = new int[64];
  private final Color[] paletteColor = new Color[64];
  private final boolean enabled;
  private int register;
  private int mode;

  public UlaPlusContainer(final boolean enabled) {
    this.enabled = enabled;
    this.reset();
  }

  private static int extendColorTo8bits(final int triple) {
    switch (triple) {
      case 0:
        return 0x00;
      case 1:
        return 0x24;
      case 2:
        return 0x48;
      case 3:
        return 0x6C;
      case 4:
        return 0x90;
      case 5:
        return 0xB4;
      case 6:
        return 0xD8;
      default:
        return 0xFF;
    }
  }

  private static int grbToRgb(final int grb) {
    int b = grb & 3;
    b = b == 0 ? 0 : (b << 1) | 1;

    int r = (grb >> 2) & 7;
    int g = (grb >> 5) & 7;

    return 0xFF_00_00_00
        | (extendColorTo8bits(r) << 16)
        | (extendColorTo8bits(g) << 8)
        | extendColorTo8bits(b);
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void reset() {
    this.mode = 0;
    this.register = 0;

    Arrays.fill(this.paletteRgb, 0xFF000000);
    Arrays.fill(this.palette, (byte) 0);
    Arrays.fill(this.paletteColor, new Color(0xFF000000, true));

//    this.loadPalette(new byte[]{
//        0b00000000,
//        0b00000010,
//        0b00010100,
//        0b00010110,
//        (byte)0b10100000,
//        (byte)0b10100010,
//        (byte)0b10110100,
//        (byte)0b10110110,
//        0b00000000,
//        0b00000011,
//        0b00011100,
//        0b00011111,
//        (byte)0b11100000,
//        (byte)0b11100011,
//        (byte)0b11111100,
//        (byte)0b11111111,
//        (byte)0b10110110,
//        (byte)0b10110100,
//        (byte)0b10100010,
//        (byte)0b10100000,
//        0b00010110,
//        0b00010100,
//        0b00000010,
//        0b00000000,
//        (byte)0b11111111,
//        (byte)0b11111100,
//        (byte)0b11100011,
//        (byte)0b11100000,
//        0b00011111,
//        0b00011100,
//        0b00000011,
//        0b00000000
//    });
  }

  public int getRegister() {
    return this.register;
  }

  public void setRegister(final int address) {
    if (this.enabled) {
      this.register = address & 0xFF;
    }
  }

  public boolean isActive() {
    return this.enabled && (this.mode & 1) != 0;
  }

  public void setActive(final boolean flag) {
    this.mode = flag ? 1 : 0;
  }

  public int findInkRgbForAttribute(final int attribute) {
    final int index = (attribute & 7) | (attribute >> 3);
    return this.paletteRgb[index];
  }

  public Color findColorForIndex(final int index) {
    return this.paletteColor[index & 63];
  }

  public int findPaperRgbForAttribute(final int attribute) {
    final int index = (attribute >> 3) & 0b11_111;
    return this.paletteRgb[index];
  }

  public int getData() {
    final int result;

    final int group = (this.register >> 6) & 3;
    final int subgroup = this.register & 0b11_1111;

    switch (group) {
      case 0: {
        // palette
        result = this.palette[subgroup] & 0xFF;
      }
      break;
      case 1: {
        // mode
        result = this.mode;
      }
      break;
      default:
        result = 0;
        break;
    }
    return result;
  }

  public void setData(final int value) {
    if (this.enabled) {
      final int group = (this.register >> 6) & 3;
      final int subgroup = this.register & 0b11_1111;

      switch (group) {
        case 0: {
          // palette
          this.palette[subgroup] = (byte) value;
          this.paletteRgb[subgroup] = grbToRgb(value);
          this.paletteColor[subgroup] = new Color(this.paletteRgb[subgroup], true);
        }
        break;
        case 1: {
          // mode
          this.mode = value & 0xFF;
        }
        break;
      }
    }
  }

  public void loadPalette(final byte[] palette) {
    for (int i = 0; i < (palette.length & 63); i++) {
      this.palette[i] = palette[i];
      this.paletteRgb[i] = grbToRgb(palette[i] & 0xFF);
      this.paletteColor[i] = new Color(this.paletteRgb[i], true);
    }
  }
}
