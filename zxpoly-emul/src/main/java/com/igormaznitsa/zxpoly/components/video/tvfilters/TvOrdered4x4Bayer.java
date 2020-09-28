package com.igormaznitsa.zxpoly.components.video.tvfilters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public final class TvOrdered4x4Bayer implements TvFilter {

  private static final int THRESHOLD = 128;

  private static final int MATRIX[][] = {
      {1, 9, 3, 11},
      {13, 5, 15, 7},
      {4, 12, 2, 10},
      {16, 8, 14, 6}};

  private static final TvOrdered4x4Bayer INSTANCE = new TvOrdered4x4Bayer();

  public static TvOrdered4x4Bayer getInstance() {
    return INSTANCE;
  }

  private static int getPseudoGray(final int argb) {
    final int r = (argb >> 16) & 0xFF;
    final int g = (argb >> 8) & 0xFF;
    final int b = argb & 0xFF;
    return Math.min(Math.round(r * 0.4047f + g * 0.5913f + b * 0.2537f), 0xFF);
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
        int level = getPseudoGray(src[pos]);
        level += level * MATRIX[x % 4][y % 4] / 17;
        if (level < THRESHOLD) {
          level = 0xFF000000;
        } else {
          level = 0xFFFFFFFF;
        }
        dst[pos] = level;
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
