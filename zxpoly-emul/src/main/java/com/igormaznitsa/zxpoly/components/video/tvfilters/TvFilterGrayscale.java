package com.igormaznitsa.zxpoly.components.video.tvfilters;

import com.igormaznitsa.zxpoly.components.video.VideoController;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.stream.Stream;

public final class TvFilterGrayscale implements TvFilter {

  private static final TvFilterGrayscale INSTANCE = new TvFilterGrayscale();
  private static final Color[] GRAYSCALE_BORDER_COLORS;

  static {
    GRAYSCALE_BORDER_COLORS = Stream.of(VideoController.PALETTE_ZXPOLY_COLORS)
            .limit(8)
            .map(TvFilterGrayscale::color2gray)
            .toArray(Color[]::new);
  }

  private TvFilterGrayscale() {
  }

  private static Color color2gray(final Color color) {
    final int rgb = color.getRGB();
    final int y = rgb2y((rgb >>> 16) & 0xFF, (rgb >>> 8) & 0xFF, rgb & 0xFF);
    return new Color(y, y, y);
  }

  public static TvFilterGrayscale getInstance() {
    return INSTANCE;
  }

  private static int rgb2y(final int r, final int g, final int b) {
    return Math.min(Math.round(r * 0.4047f + g * 0.5913f + b * 0.2537f), 255);
  }

  private static void fastArgbToGrayscale(final int[] src) {
    int index = src.length;
    while (--index >= 0) {
      final int argb = src[index];
      final int a = (argb >>> 24) & 0xFF;
      final int r = (argb >>> 16) & 0xFF;
      final int g = (argb >>> 8) & 0xFF;
      final int b = argb & 0xFF;

      final int y = rgb2y(r, g, b);

      SHARED_BUFFER_RASTER[index] = (a << 24) | (y << 16) | (y << 8) | y;
    }
  }

  @Override
  public byte[] apply(
          boolean forceCopy,
          byte[] rgbArray512x384,
          int argbBorderColor
  ) {
    final byte[] result =
            forceCopy ? Arrays.copyOf(rgbArray512x384, rgbArray512x384.length) : rgbArray512x384;
    int index = result.length;
    while (--index > 0) {
      int base = index;
      final int b = result[index--] & 0xFF;
      final int g = result[index--] & 0xFF;
      final int r = result[index] & 0xFF;

      final byte y = (byte) rgb2y(r, g, b);

      result[base--] = y;
      result[base--] = y;
      result[base] = y;
    }
    return result;
  }

  @Override
  public Color applyBorderColor(final Color borderColor) {
    final int rgb = borderColor.getRGB();
    final int index = (((rgb >>> 16) & 0xFF) == 0 ? 0 : 4)
            | (((rgb >>> 8) & 0xFF) == 0 ? 0 : 2)
            | ((rgb & 0xFF) == 0 ? 0 : 1);
    return GRAYSCALE_BORDER_COLORS[index];
  }

  @Override
  public BufferedImage apply(
          final BufferedImage srcImageArgb512x384,
          float zoom,
          final int argbBorder,
          final boolean firstInChain
  ) {
    final int[] src = ((DataBufferInt) srcImageArgb512x384.getRaster().getDataBuffer()).getData();

    fastArgbToGrayscale(src);

    return SHARED_BUFFER;
  }

  @Override
  public int[] makePalette() {
    final int[] palette = new int[256];
    for (int y = 0; y < 256; y++) {
      palette[y] = (y << 16) | (y << 8) | y;
    }
    return palette;
  }

  @Override
  public boolean isGifCompatible() {
    return true;
  }
}
