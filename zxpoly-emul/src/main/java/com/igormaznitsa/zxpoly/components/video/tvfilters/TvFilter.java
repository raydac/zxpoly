package com.igormaznitsa.zxpoly.components.video.tvfilters;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;

public interface TvFilter {
  BufferedImage SHARED_BUFFER =
          new BufferedImage(512, 384, BufferedImage.TYPE_INT_ARGB);
  int[] SHARED_BUFFER_RASTER =
          ((DataBufferInt) SHARED_BUFFER.getRaster().getDataBuffer()).getData();

  int RASTER_WIDTH_ARGB_INT = 512;
  int RASTER_WIDTH_RGB_BYTE = RASTER_WIDTH_ARGB_INT * 3;
  int RASTER_HEIGHT = 384;

  default int[] makePalette() {
    return null;
  }

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

  default byte[] apply(
          final boolean forceCopy,
          final byte[] rgbArray512x384,
          final int argbBorderColor
  ) {
    return rgbArray512x384;
  }

  default void apply(
          final Graphics2D gfx,
          final Rectangle imageArea,
          final float zoom
  ) {

  }

  default boolean isGifCompatible() {
    return false;
  }

}
