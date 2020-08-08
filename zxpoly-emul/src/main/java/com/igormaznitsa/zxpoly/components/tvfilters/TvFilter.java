package com.igormaznitsa.zxpoly.components.tvfilters;

import java.awt.Graphics2D;
import java.awt.color.ColorSpace;
import java.awt.image.BufferedImage;

public interface TvFilter {
  ColorSpace SRC_COLOR_SPACE = ColorSpace.getInstance(ColorSpace.CS_LINEAR_RGB);
  BufferedImage SHARED_FILTER_BUFFER = new BufferedImage(512, 384, BufferedImage.TYPE_INT_ARGB);
  Graphics2D SHARED_FILTER_BUFFER_GFX = (Graphics2D) SHARED_FILTER_BUFFER.getGraphics();

  BufferedImage apply(BufferedImage imageArgb512x384, float zoom, boolean makeCopy);
}
