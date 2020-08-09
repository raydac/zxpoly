package com.igormaznitsa.zxpoly.components.tvfilters;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public interface TvFilter {
  BufferedImage SHARED_FILTER_BUFFER = new BufferedImage(512, 384, BufferedImage.TYPE_INT_ARGB);

  default Color applyBorderColor(final Color borderColor) {
    return borderColor;
  }

  default BufferedImage apply(
      final BufferedImage srcImageArgb512x384,
      final float zoom,
      final int argbBorderColor,
      final boolean firstInChain
  ) {
    return srcImageArgb512x384;
  }

  default void apply(
      final Graphics2D gfx,
      final Rectangle imageArea,
      final float zoom
  ) {

  }

}
