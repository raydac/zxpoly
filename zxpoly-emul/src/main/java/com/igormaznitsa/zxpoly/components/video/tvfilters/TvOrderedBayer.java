package com.igormaznitsa.zxpoly.components.video.tvfilters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public final class TvOrderedBayer implements TvFilter {

  private static final int THRESHOLD = 132;

  private static final int MATRIX[][] = {
      {0, 48, 12, 60, 3, 51, 15, 63},
      {32, 16, 44, 28, 35, 19, 47, 31},
      {8, 56, 4, 52, 11, 59, 7, 55},
      {40, 24, 36, 20, 43, 27, 39, 23},
      {2, 50, 14, 62, 1, 49, 13, 61},
      {34, 18, 46, 30, 33, 17, 45, 29},
      {10, 58, 6, 54, 9, 57, 5, 53},
      {42, 26, 38, 22, 41, 25, 37, 21}
  };

  private static final TvOrderedBayer INSTANCE = new TvOrderedBayer();

  public static TvOrderedBayer getInstance() {
    return INSTANCE;
  }

  private static int getPseudoGray(final int argb) {
    final int r = (argb >> 16) & 0xFF;
    final int g = (argb >> 8) & 0xFF;
    final int b = argb & 0xFF;
    return Math.min(Math.round(r * 0.3747f + g * 0.5013f + b * 0.3737f), 0xFF);
  }

  @Override
  public Color applyBorderColor(final Color borderColor) {
    return borderColor.getRed() < THRESHOLD ? Color.BLACK : Color.WHITE;
  }

  @Override
  public BufferedImage apply(BufferedImage srcImageArgb512x384, float zoom,
                             int argbBorderColor, boolean firstInChain) {
    final int[] src = ((DataBufferInt) srcImageArgb512x384.getRaster().getDataBuffer()).getData();
    final int[] dst = SHARED_BUFFER_RASTER;

    for (int y = 0; y < RASTER_HEIGHT; y++) {
      for (int x = 0; x < RASTER_WIDTH_ARGB_INT; x++) {
        final int pos = y * RASTER_WIDTH_ARGB_INT + x;
        float level = getPseudoGray(src[pos]);
        level += level * MATRIX[x & 7][y & 7] / 64.0f;
        if (level < THRESHOLD) {
          dst[pos] = 0xFF000000;
        } else {
          dst[pos] = 0xFFFFFFFF;
        }
      }
    }

    return SHARED_BUFFER;
  }

  @Override
  public byte[] apply(
      final boolean forceCopy,
      final byte[] rgbArray512x384,
      final int argbBorderColor
  ) {
    final byte[] result =
        forceCopy ? Arrays.copyOf(rgbArray512x384, rgbArray512x384.length) : rgbArray512x384;

    for (int y = 0; y < RASTER_HEIGHT; y++) {
      for (int x = 0; x < RASTER_WIDTH_ARGB_INT; x += 3) {
        int pos = y * RASTER_WIDTH_RGB_BYTE + x * 3;
        int value = result[pos] & 0xFF;
        value += value * MATRIX[x % 4][y % 4] / 17;
        if (value < THRESHOLD) {
          value = 0;
        } else {
          value = 0xFF;
        }
        result[pos++] = (byte) value;
        result[pos++] = (byte) value;
        result[pos] = (byte) value;
      }
    }

    return result;
  }

  @Override
  public void apply(Graphics2D gfx, Rectangle imageArea, float zoom) {

  }
}
