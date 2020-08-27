package com.igormaznitsa.zxpoly.components.video.tvfilters;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.stream.IntStream;

public class TvFilterGaussian implements TvFilter {
  private static final TvFilterGaussian INSTANCE = new TvFilterGaussian();

  private static final int RASTER_WIDTH = 512;
  private static final int RASTER_HEIGHT = 384;
  private static final int[] FILTER = new int[] {1, 2, 1, 2, 4, 2, 1, 2, 1};
  private static final int FILTER_SUM = IntStream.of(FILTER).sum();
  private static final int FILTER_WIDTH = 3;
  private static final int CENTER_OFFSET_X = FILTER_WIDTH / 2;
  private static final int CENTER_OFFSET_Y = FILTER.length / FILTER_WIDTH / 2;
  private static final int PIXEL_INDEX_OFFSET = RASTER_WIDTH - FILTER_WIDTH;
  private final int[] blurBuffer = new int[RASTER_WIDTH * RASTER_HEIGHT];

  private TvFilterGaussian() {

  }

  public static TvFilterGaussian getInstance() {
    return INSTANCE;
  }

  private static void blur(
      final int[] src,
      final int[] out) {

    // apply filter
    for (int h = RASTER_HEIGHT - FILTER.length / FILTER_WIDTH + 1, w =
         RASTER_WIDTH - FILTER_WIDTH + 1, y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (int f = 0, pixelIndex = y * RASTER_WIDTH + x;
             f < FILTER.length;
             pixelIndex += PIXEL_INDEX_OFFSET) {
          for (int fx = 0; fx < FILTER_WIDTH; fx++, pixelIndex++, f++) {
            final int srcArgb = src[pixelIndex];
            final int filterFactor = FILTER[f];
            r += ((srcArgb >>> 16) & 0xFF) * filterFactor;
            g += ((srcArgb >>> 8) & 0xFF) * filterFactor;
            b += (srcArgb & 0xFF) * filterFactor;
          }
        }
        r /= FILTER_SUM;
        g /= FILTER_SUM;
        b /= FILTER_SUM;
        out[x + CENTER_OFFSET_X + (y + CENTER_OFFSET_Y) * RASTER_WIDTH] =
            (r << 16) | (g << 8) | b | 0xFF000000;
      }
    }
  }

  @Override
  public BufferedImage apply(
      final BufferedImage srcImageArgb512x384,
      final float zoom,
      final int argbBorderColor,
      final boolean firstInChain) {

    final BufferedImage result;
    final int[] resultRaster;

    if (firstInChain) {
      result = SHARED_BUFFER;
      resultRaster = SHARED_BUFFER_RASTER;
      final int[] srcRaster =
          ((DataBufferInt) srcImageArgb512x384.getRaster().getDataBuffer()).getData();
      System.arraycopy(srcRaster, 0, resultRaster, 0, resultRaster.length);
    } else {
      result = srcImageArgb512x384;
      resultRaster = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();
    }

    blur(resultRaster, blurBuffer);

    System.arraycopy(blurBuffer, 0, resultRaster, 0, blurBuffer.length);

    return result;
  }
}