package com.igormaznitsa.zxpoly.components.video.tvfilters;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import java.util.stream.IntStream;

public class TvFilterGaussian implements TvFilter {
  private static final TvFilterGaussian INSTANCE = new TvFilterGaussian();

  private static final int[] FILTER = new int[] {1, 2, 1, 2, 4, 2, 1, 2, 1};
  private static final int FILTER_SUM = IntStream.of(FILTER).sum();
  private static final int FILTER_WIDTH = 3;
  private static final int CENTER_OFFSET_X = FILTER_WIDTH / 2;
  private static final int CENTER_OFFSET_Y = FILTER.length / FILTER_WIDTH / 2;
  private static final int PIXEL_INDEX_OFFSET_ARGB_INT = RASTER_WIDTH_ARGB_INT - FILTER_WIDTH;
  private static final int PIXEL_INDEX_OFFSET_RGB_BYTE = RASTER_WIDTH_RGB_BYTE - FILTER_WIDTH * 3;
  private final int[] blurBuffer = new int[RASTER_WIDTH_ARGB_INT * RASTER_HEIGHT];
  private final byte[] blurByteBuffer = new byte[RASTER_WIDTH_ARGB_INT * RASTER_HEIGHT * 3];

  private TvFilterGaussian() {

  }

  public static TvFilterGaussian getInstance() {
    return INSTANCE;
  }

  private static void blur(
      final int[] src,
      final int[] out) {

    for (int h = RASTER_HEIGHT - FILTER.length / FILTER_WIDTH + 1, w =
         RASTER_WIDTH_ARGB_INT - FILTER_WIDTH + 1, y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (int f = 0, pixelIndex = y * RASTER_WIDTH_ARGB_INT + x;
             f < FILTER.length;
             pixelIndex += PIXEL_INDEX_OFFSET_ARGB_INT) {
          for (int fx = 0; fx < FILTER_WIDTH; fx++, f++) {
            final int filterFactor = FILTER[f];
            final int srcArgb = src[pixelIndex++];
            r += ((srcArgb >>> 16) & 0xFF) * filterFactor;
            g += ((srcArgb >>> 8) & 0xFF) * filterFactor;
            b += (srcArgb & 0xFF) * filterFactor;
          }
        }
        r /= FILTER_SUM;
        g /= FILTER_SUM;
        b /= FILTER_SUM;
        out[x + CENTER_OFFSET_X + (y + CENTER_OFFSET_Y) * RASTER_WIDTH_ARGB_INT] =
            (r << 16) | (g << 8) | b | 0xFF000000;
      }
    }
  }

  private static void blur(
      final byte[] rgbSrc,
      final byte[] rgbOut) {

    for (int h = RASTER_HEIGHT - FILTER.length / FILTER_WIDTH + 1, w =
         RASTER_WIDTH_RGB_BYTE - FILTER_WIDTH * 3 + 3, y = 0;
         y < h;
         y++) {
      for (int x = 0; x < w; x += 3) {
        int r = 0;
        int g = 0;
        int b = 0;
        for (int f = 0, pixelIndex = y * RASTER_WIDTH_RGB_BYTE + x;
             f < FILTER.length;
             pixelIndex += PIXEL_INDEX_OFFSET_RGB_BYTE) {
          for (int fx = 0; fx < FILTER_WIDTH; fx++, f++) {
            final int filterFactor = FILTER[f];
            r += (rgbSrc[pixelIndex++] & 0xFF) * filterFactor;
            g += (rgbSrc[pixelIndex++] & 0xFF) * filterFactor;
            b += (rgbSrc[pixelIndex++] & 0xFF) * filterFactor;
          }
        }
        r /= FILTER_SUM;
        g /= FILTER_SUM;
        b /= FILTER_SUM;

        int outIndex = x + CENTER_OFFSET_X * 3 + (y + CENTER_OFFSET_Y) * RASTER_WIDTH_RGB_BYTE;
        rgbOut[outIndex++] = (byte) r;
        rgbOut[outIndex++] = (byte) g;
        rgbOut[outIndex] = (byte) b;
      }
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
    System.arraycopy(result, 0, this.blurByteBuffer, 0, result.length);
    blur(result, this.blurByteBuffer);
    System.arraycopy(blurByteBuffer, 0, result, 0, blurByteBuffer.length);
    return result;
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
