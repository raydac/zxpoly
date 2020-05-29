/*
 * Copyright (C) 2014-2019 Igor Maznitsa
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

package com.igormaznitsa.zxpoly.utils;

import java.awt.Desktop;
import java.awt.Image;
import java.io.Closeable;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.SystemUtils;

public final class Utils {

  public static final Logger LOGGER = Logger.getLogger("Utils");

  private Utils() {
  }

  public static void browseLink(final URL url) {
    if (Desktop.isDesktopSupported()) {
      final Desktop desktop = Desktop.getDesktop();
      if (desktop.isSupported(Desktop.Action.BROWSE)) {
        try {
          desktop.browse(url.toURI());
        } catch (Exception x) {
          LOGGER.warning("Can't browse URL in Desktop: " + x.getMessage());
        }
      } else if (SystemUtils.IS_OS_LINUX) {
        final Runtime runtime = Runtime.getRuntime();
        try {
          runtime.exec("xdg-open " + url);
        } catch (IOException e) {
          LOGGER.warning("Can't browse URL under Linux: " + e.getMessage());
        }
      } else if (SystemUtils.IS_OS_MAC) {
        final Runtime runtime = Runtime.getRuntime();
        try {
          runtime.exec("open " + url);
        } catch (IOException e) {
          LOGGER.warning("Can't browse URL on MAC: " + e.getMessage());
        }
      }
    }
  }

  public static void closeQuietly(final Closeable closeable) {
    if (closeable != null) {
      try {
        closeable.close();
      } catch (Exception ex) {
        // DO NOTHING
      }
    }
  }

  public static int[] alignPaletteColors(final int[] dstPalette, final int[] etalPalette) {
    final int[] result = dstPalette.clone();

    for (int i = 0; i < result.length; i++) {
      final int r = (result[i] >>> 16) & 0xFF;
      final int g = (result[i] >>> 8) & 0xFF;
      final int b = result[i] & 0xFF;
      double dist = Double.MAX_VALUE;
      for (final int value : etalPalette) {
        final int rr = (value >>> 16) & 0xFF;
        final int gg = (value >>> 8) & 0xFF;
        final int bb = value & 0xFF;
        final double cdist = Math.sqrt(Math.pow((double) rr - r, 2) + Math.pow((double) gg - g, 2) +
            Math.pow((double) bb - b, 2));
        if (cdist < dist) {
          dist = cdist;
          result[i] = value;
        }
      }
    }
    return result;
  }

  public static int[] readRawPalette(final InputStream in, final boolean close) {
    final int[] result = new int[256];

    try {
      for (int i = 0; i < 256; i++) {
        final int red = in.read();
        final int green = in.read();
        final int blue = in.read();
        if (red < 0 || green < 0 || blue < 0) {
          throw new EOFException();
        }
        result[i] = 0xFF000000 | (red << 16) | (green << 8) | blue;
      }
    } catch (IOException ex) {
      throw new RuntimeException(ex);
    } finally {
      if (close) {
        try {
          in.close();
        } catch (Exception ex) {
          //NOTHING
        }
      }
    }
    return result;
  }

  public static void assertUiThread() {
    if (!SwingUtilities.isEventDispatchThread()) {
      throw new Error("Detected call from outside of event dispatch thread!");
    }
  }

  public static Image loadIcon(final String name) {
    try (InputStream resource = findResourceOrError("com/igormaznitsa/zxpoly/icons/" + name)) {
      return ImageIO.read(resource);
    } catch (IOException ex) {
      throw new Error("Can't read resource icon [" + name + ']');
    }
  }

  public static InputStream findResourceOrError(final String resource) {
    final InputStream result = Utils.class.getClassLoader().getResourceAsStream(resource);
    if (result == null) {
      throw new IllegalArgumentException("Can't find resource for path [" + resource + ']');
    }
    return result;
  }
}
