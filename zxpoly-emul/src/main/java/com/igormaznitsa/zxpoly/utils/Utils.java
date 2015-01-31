/*
 * Copyright (C) 2014 Raydac Research Group Ltd.
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
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;

public enum Utils {

  ;

  public static Image loadIcon(final String name) {
    final InputStream resource = findResourceOrError("com/igormaznitsa/zxpoly/icons/" + name);
    try {
      return ImageIO.read(resource);
    }
    catch (IOException ex) {
      throw new Error("Can't read resource icon [" + name + ']');
    }
    finally {
      IOUtils.closeQuietly(resource);
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
