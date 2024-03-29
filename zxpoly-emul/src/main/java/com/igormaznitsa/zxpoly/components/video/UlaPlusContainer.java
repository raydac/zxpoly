package com.igormaznitsa.zxpoly.components.video;

import java.awt.Color;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerArray;

public final class UlaPlusContainer {

  public static final int PORT_REGISTER = 0xBF3B;
  public static final int PORT_DATA = 0xFF3B;
  public static final int PORT_TIMEX_FF = 0x00FF;

  private final byte[] palette = new byte[64];
  private final AtomicIntegerArray paletteRgb = new AtomicIntegerArray(64);
  private final Color[] paletteColor = new Color[64];
  private final boolean enabled;
  private volatile int register;
  private volatile int mode;

  private volatile int portFF;

  public UlaPlusContainer(final boolean enabled) {
    this.enabled = enabled;
    this.reset();
  }

  private static int extendColorTo8bits(final int triple) {
    return (triple << 5) | (triple << 2) | (triple >> 1);
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

  public int getPortFF() {
    return this.portFF;
  }

  public void setPortFF(final int value) {
    this.portFF = value & 0xFF;
  }

  public int getMode() {
    return this.mode;
  }

  public boolean isEnabled() {
    return this.enabled;
  }

  public void reset() {
    this.portFF = 0;
    this.mode = 0;
    this.register = 0;

    for (int i = 0; i < this.paletteRgb.length(); i++) {
      this.paletteRgb.set(i, 0xFF000000);
    }
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


  public Color findColorForIndex(final int index) {
    return this.paletteColor[index & 63];
  }

  public int findInkRgbForAttribute(final int attribute) {
    final int index = ((attribute >> 2) & 0b11_0000) | (attribute & 7);
    return this.paletteRgb.get(index);
  }

  public int findPaperRgbForAttribute(final int attribute) {
    final int index = ((attribute >> 2) & 0b11_0000) | ((attribute >> 3) & 7) | 8;
    return this.paletteRgb.get(index);
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
          final int rgb = grbToRgb(value);
          this.paletteRgb.set(subgroup, rgb);
          this.paletteColor[subgroup] = new Color(rgb, true);
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
    for (int i = 0; i < Math.min(palette.length, 64); i++) {
      final int item = palette[i] & 0xFF;
      this.palette[i] = (byte) item;
      final int rgb = grbToRgb(item);
      this.paletteRgb.set(i, rgb);
      this.paletteColor[i] = new Color(rgb, true);
    }
  }

  public byte[] getPalette() {
    return this.palette.clone();
  }

  public int[] getArgbPalette() {
    final int[] result = new int[this.paletteRgb.length()];
    for (int i = 0; i < this.paletteRgb.length(); i++) {
      result[i] = this.paletteRgb.get(i);
    }
    return result;
  }
}
