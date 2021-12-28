package com.igormaznitsa.zxpoly.components.video.tvfilters;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;

public final class TvFilterBlackWhite implements TvFilter {

  private static final int[][] MATRIX = {
          {0, 48, 12, 60, 3, 51, 15, 63},
          {32, 16, 44, 28, 35, 19, 47, 31},
          {8, 56, 4, 52, 11, 59, 7, 55},
          {40, 24, 36, 20, 43, 27, 39, 23},
          {2, 50, 14, 62, 1, 49, 13, 61},
          {34, 18, 46, 30, 33, 17, 45, 29},
          {10, 58, 6, 54, 9, 57, 5, 53},
          {42, 26, 38, 22, 41, 25, 37, 21}
  };

  private static final TvFilterBlackWhite INSTANCE = new TvFilterBlackWhite();

  public static TvFilterBlackWhite getInstance() {
    return INSTANCE;
  }

  private static int toBitPair(final int color) {
    if (color < 64) {
      return 0;
    }
    if (color < 128) {
      return 1;
    }
    if (color < 192) {
      return 2;
    }
    return 3;
  }

  private static int getLevel64(final int argb) {
    final int r = (argb >> 16) & 0xFF;
    final int g = (argb >> 8) & 0xFF;
    final int b = argb & 0xFF;
    final int bits = (toBitPair(g) << 4) | (toBitPair(r) << 2) | toBitPair(b);
    return bits == 63 ? 64 : bits;
  }

  private static int getLevel64(final int r, final int g, final int b) {
    final int bits = (toBitPair(g) << 4) | (toBitPair(r) << 2) | toBitPair(b);
    return bits == 63 ? 64 : bits;
  }


  @Override
  public Color applyBorderColor(final Color borderColor) {
    return borderColor.getRed() < 128 ? Color.BLACK : Color.WHITE;
  }

  @Override
  public BufferedImage apply(
          final BufferedImage srcImageArgb512x384,
          final float zoom,
          final int argbBorderColor,
          final boolean firstInChain
  ) {
    final int[] src = ((DataBufferInt) srcImageArgb512x384.getRaster().getDataBuffer()).getData();
    final int[] dst = SHARED_BUFFER_RASTER;

    for (int y = 0; y < RASTER_HEIGHT; y++) {
      for (int x = 0; x < RASTER_WIDTH_ARGB_INT; x++) {
        final int pos = y * RASTER_WIDTH_ARGB_INT + x;
        if (MATRIX[x & 7][y & 7] < getLevel64(src[pos])) {
          dst[pos] = 0xFFFFFFFF;
        } else {
          dst[pos] = 0xFF000000;
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
      for (int x = 0; x < RASTER_WIDTH_ARGB_INT; x++) {
        final int pos = y * RASTER_WIDTH_RGB_BYTE + (x * 3);
        int tpos = pos;
        final int r = result[tpos++] & 0xFF;
        final int g = result[tpos++] & 0xFF;
        final int b = result[tpos] & 0xFF;

        final byte level;
        if (MATRIX[x & 7][y & 7] < getLevel64(r, g, b)) {
          level = (byte) 0xFF;
        } else {
          level = 0;
        }
        tpos = pos;
        result[tpos++] = level;
        result[tpos++] = level;
        result[tpos] = level;
      }
    }

    return result;
  }

  @Override
  public int[] makePalette() {
    return new int[]{0, 0xFFFFFF};
  }

  @Override
  public boolean isGifCompatible() {
    return true;
  }
}
