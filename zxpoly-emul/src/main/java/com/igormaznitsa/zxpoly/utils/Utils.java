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

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;

public final class Utils {

  private Utils() {
  }

  public static int[] readPal(final InputStream in, final boolean close) {
    final Pattern pattern = Pattern.compile("^\\s*(\\d+)\\s*(\\d+)\\s+(\\d+)\\s*$");

    final int[] result = new int[768];
    int index = 0;

    final List<String> lines;
    try {
      lines = IOUtils.readLines(in, StandardCharsets.US_ASCII);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }

    for (final String line : lines) {
      if (index >= result.length) {
        break;
      }
      if (line.trim().isEmpty()) {
        continue;
      }
      final Matcher matcher = pattern.matcher(line);
      if (matcher.find()) {
        final int r = Integer.parseInt(matcher.group(1));
        final int g = Integer.parseInt(matcher.group(2));
        final int b = Integer.parseInt(matcher.group(3));
        result[index++] = 0xFF000000 | (r << 24) | (g << 16) | b;
      }
    }

    if (close) {
      try {
        in.close();
      } catch (Exception ex) {
        //NOTHING
      }
    }

    return result;
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
