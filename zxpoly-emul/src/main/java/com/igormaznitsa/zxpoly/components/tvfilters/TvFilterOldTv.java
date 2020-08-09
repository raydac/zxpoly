package com.igormaznitsa.zxpoly.components.tvfilters;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public final class TvFilterOldTv implements TvFilter {

  private static final TvFilterOldTv INSTANCE = new TvFilterOldTv();

  private TvFilterOldTv() {

  }

  private static int rgb2y(final int r, final int g, final int b) {
    return (2449 * r + 4809 * g + 934 * b + 1024) >> 11;
  }

  private static int rgb2u(final int r, final int g, final int b) {
    return (4096 * b - 1383 * r - 2713 * g + 1024) >> 11;
  }

  private static int rgb2v(final int r, final int g, final int b) {
    return (4096 * r - 3430 * g - 666 * b + 1024) >> 11;
  }

  private static int yuv2rgb(final int y, final int u, final int v) {
    final int r = Math.min(255, Math.abs(8192 * y + 11485 * v + 16384) >> 15);
    final int g = Math.min(255, Math.abs(8192 * y - 2819 * u - 5850 * v + 16384) >> 15);
    final int b = Math.min(255, Math.abs(8192 * y + 14516 * u + 16384) >> 15);
    return (r << 16) | (g << 8) | b;
  }

  public static TvFilterOldTv getInstance() {
    return INSTANCE;
  }

  @Override
  public BufferedImage apply(
      final BufferedImage srcImageArgb512x384,
      final float zoom,
      final int argbBorder,
      boolean firstInChain
  ) {
    final BufferedImage image;
    final int[] argbBuffer;
    if (firstInChain) {
      image = SHARED_BUFFER;
      argbBuffer = SHARED_BUFFER_RASTER;
      System
          .arraycopy(((DataBufferInt) srcImageArgb512x384.getRaster().getDataBuffer()).getData(), 0,
              argbBuffer, 0, argbBuffer.length);
    } else {
      image = srcImageArgb512x384;
      argbBuffer = ((DataBufferInt) srcImageArgb512x384.getRaster().getDataBuffer()).getData();
    }

    final int brdrR = (argbBorder >> 16) & 0xFF;
    final int brdrG = (argbBorder >> 8) & 0xFF;
    final int brdrB = argbBorder & 0xFF;

    final int borderY = rgb2y(brdrR, brdrG, brdrB);
    final int borderU = rgb2u(brdrR, brdrG, brdrB);
    final int borderV = rgb2v(brdrR, brdrG, brdrB);

    final int width = image.getWidth();
    final int height = image.getHeight();

    for (int y = 0; y < height; y++) {
      final int offset = y * width;
      int pu = borderU;
      int pv = borderV;
      int py = borderY;

      final boolean yodd = (y & 1) != 0;

      for (int x = 0; x < width; x++) {
        final boolean xodd = (x & 1) != 0;

        final int pos = offset + x;
        final int argb = argbBuffer[pos];
        final int r = (argb >>> 16) & 0xFF;
        final int g = (argb >>> 8) & 0xFF;
        final int b = argb & 0xFF;

        int yc = rgb2y(r, g, b);
        final int uc = rgb2u(r, g, b);
        final int vc = rgb2v(r, g, b);

        if (yodd) {
          yc = Math.round(0.87f * yc);
        }

        yc = (yc + py) / 2;

        final int resultRgb;
        if (xodd) {
          resultRgb = yuv2rgb(yc, (pu + uc) / 2, (pv + vc) / 2);
        } else {
          resultRgb = yuv2rgb(yc, uc, vc);
        }
        pu = (pu + uc * 2) / 3;
        pv = (pv + vc * 2) / 3;
        py = yc;

        argbBuffer[pos] = resultRgb | (argb & 0xFF000000);
      }
    }

    return image;
  }
}
