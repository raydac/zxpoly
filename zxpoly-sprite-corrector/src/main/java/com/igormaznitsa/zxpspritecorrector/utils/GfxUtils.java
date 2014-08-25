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
package com.igormaznitsa.zxpspritecorrector.utils;

import java.awt.Image;
import javax.imageio.ImageIO;

public enum GfxUtils {

  ;
        
   public static Image loadImage(final String iconFile) {
    try {
      return ImageIO.read(ClassLoader.getSystemResourceAsStream("com/igormaznitsa/zxpspritecorrector/icons/" + iconFile));
    }
    catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }
}
