/*
 * Copyright (C) 2019 Igor Maznitsa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.igormaznitsa.zxpspritecorrector.utils;

import java.awt.Color;
import java.awt.image.IndexColorModel;

public final class ZXPalette {

  public static final Color[] COLORS = new Color[] {
      // normal bright
      new Color(0, 0, 0), // Black
      new Color(0, 0, 190), // Blue
      new Color(190, 0, 0), // Red
      new Color(190, 0, 190),
      new Color(0, 190, 0), // Green
      new Color(0, 190, 190),
      new Color(190, 190, 0),
      new Color(190, 190, 190),
      // high bright
      new Color(16, 16, 16),
      new Color(0, 0, 255),
      new Color(255, 0, 0),
      new Color(255, 0, 255),
      new Color(0, 255, 0),
      new Color(0, 255, 255),
      new Color(255, 255, 0),
      new Color(255, 255, 255)
  };

  private ZXPalette() {

  }

  public static IndexColorModel makeIndexPalette() {
    final byte[] r = new byte[COLORS.length];
    final byte[] g = new byte[COLORS.length];
    final byte[] b = new byte[COLORS.length];
    for (int i = 0; i < COLORS.length; i++) {
      r[i] = (byte) COLORS[i].getRed();
      g[i] = (byte) COLORS[i].getGreen();
      b[i] = (byte) COLORS[i].getBlue();
    }
    return new IndexColorModel(4, COLORS.length, r, g, b);
  }

  public static Color extractInk(final int attribute) {
    final int bright = (attribute & 0x40) == 0 ? 0x08 : 0x00;
    return COLORS[bright | (attribute & 0x7)];
  }

  public static Color extractPaper(final int attribute) {
    final int bright = (attribute & 0x40) == 0 ? 0x08 : 0x00;
    return COLORS[bright | ((attribute >>> 3) & 0x7)];
  }

  public static int calcAttributeAddressZXMode(final int startScreenAddress,
                                               final int screenOffset) {
    final int line = ((screenOffset >>> 5) & 0x07) | ((screenOffset >>> 8) & 0x18);
    final int column = screenOffset & 0x1F;
    final int off = ((line >>> 3) << 8) | (((line & 0x07) << 5) | column);
    return startScreenAddress + 0x1800 + off;
  }

  public static int findNearestColorIndex(final int rgb) {
    int index = -1;
    double lastDistance = Double.MAX_VALUE;

    final double r = (rgb >> 16) & 0xFF;
    final double g = (rgb >> 8) & 0xFF;
    final double b = rgb & 0xFF;

    int currentIndex = 0;
    for (final Color c : COLORS) {
      final double rr = c.getRed();
      final double gg = c.getGreen();
      final double bb = c.getBlue();

      if (Double.compare(r, rr) == 0 && Double.compare(b, bb) == 0 && Double.compare(g, gg) == 0) {
        index = currentIndex;
        break;
      } else {
        final double distance =
            Math.sqrt(Math.pow(rr - r, 2) + Math.pow(gg - g, 2) + Math.pow(bb - b, 2));
        final int comparation = Double.compare(distance, lastDistance);
        if (comparation < 0) {
          index = currentIndex;
          lastDistance = distance;
        }
      }
      currentIndex++;
    }
    return index;
  }
}
