package com.igormaznitsa.zxpoly.components.video.tvfilters;

import com.igormaznitsa.zxpoly.components.video.VideoController;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public abstract class AbstractMonochromeOldTvFilter extends TvFilterOldTv {

  private final Color[] borderColors;

  AbstractMonochromeOldTvFilter() {
    super();
    this.borderColors = this.precalculateEightBorderColors();
  }

  @Override
  public int[] makePalette() {
    final int[] result = new int[256];
    for (int y = 0; y < 256; y++) {
      result[y] = y2rgb(y);
    }
    return result;
  }

  public static int rgb2y(final int r, final int g, final int b) {
    return Math.min(Math.round(r * 0.4047f + g * 0.5913f + b * 0.2537f), 255);
  }

  private Color[] precalculateEightBorderColors() {
    final Color[] result = new Color[8];
    for (int i = 0; i < 8; i++) {
      final Color color = VideoController.PALETTE_ZXPOLY_COLORS[i];
      final int y = rgb2y(color.getRed(), color.getGreen(), color.getBlue());
      result[i] = new Color(y2rgb(y));
    }
    return result;
  }

  @Override
  public boolean isGifCompatible() {
    return true;
  }

  protected abstract int y2rgb(final int y);

  @Override
  public final BufferedImage apply(BufferedImage srcImageArgb512x384, float zoom, int argbBorderColor, boolean firstInChain) {
    final BufferedImage result = super.apply(srcImageArgb512x384, zoom, argbBorderColor, firstInChain);
    final int[] argbBuffer = ((DataBufferInt) result.getRaster().getDataBuffer()).getData();
    for (int i = 0; i < argbBuffer.length; i++) {
      final int argb = argbBuffer[i];

      final int a = (argb >>> 24) & 0xFF;
      final int r = (argb >>> 16) & 0xFF;
      final int g = (argb >>> 8) & 0xFF;
      final int b = argb & 0xFF;

      final int rgb = y2rgb(rgb2y(r, g, b));

      argbBuffer[i] = (a << 24) | rgb;
    }
    return result;
  }

  @Override
  public final byte[] apply(boolean forceCopy, byte[] rgbArray512x384, int argbBorderColor) {
    final byte[] result = super.apply(forceCopy, rgbArray512x384, argbBorderColor);
    for (int i = 0; i < result.length; ) {
      int resultOffset = i;
      final int r = result[i++] & 0xFF;
      final int g = result[i++] & 0xFF;
      final int b = result[i++] & 0xFF;
      final int rgb = y2rgb(rgb2y(r, g, b));
      result[resultOffset++] = (byte) (rgb >> 16);
      result[resultOffset++] = (byte) (rgb >> 8);
      result[resultOffset] = (byte) rgb;
    }
    return result;
  }

  @Override
  public Color applyBorderColor(final Color borderColor) {
    final int rgb = borderColor.getRGB();
    final int index = (((rgb >> 16) & 0xFF) == 0 ? 0 : 0b100)
            | (((rgb >> 8) & 0xFF) == 0 ? 0 : 0b010)
            | ((rgb & 0xFF) == 0 ? 0 : 0b001);
    return this.borderColors[index];
  }
}
