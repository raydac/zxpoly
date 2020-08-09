package com.igormaznitsa.zxpoly.components.tvfilters;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public final class TvFilterGrayscale implements TvFilter {

  private static final TvFilterGrayscale INSTANCE = new TvFilterGrayscale();

  private TvFilterGrayscale() {
  }

  public static TvFilterGrayscale getInstance() {
    return INSTANCE;
  }

  private static void fastArgbToGrayscale(final int[] src, final int[] dst) {
    int index = src.length;
    while (--index >= 0) {
      final int argb = src[index];
      final int a = (argb >>> 24) & 0xFF;
      final int r = (argb >>> 16) & 0xFF;
      final int g = (argb >>> 8) & 0xFF;
      final int b = argb & 0xFF;

      final int y = Math.min(Math.round(r * 0.4047f + g * 0.5913f + b * 0.2537f), 255);

      dst[index] = (a << 24) | (y << 16) | (y << 8) | y;
    }
  }

  @Override
  public BufferedImage apply(
      final BufferedImage srcImageArgb512x384,
      float zoom,
      final int argbBorder,
      final boolean firstInChain
  ) {
    final int[] src = ((DataBufferInt) srcImageArgb512x384.getRaster().getDataBuffer()).getData();
    final int[] dst = ((DataBufferInt) SHARED_FILTER_BUFFER.getRaster().getDataBuffer()).getData();

    fastArgbToGrayscale(src, dst);

    return SHARED_FILTER_BUFFER;
  }

}
