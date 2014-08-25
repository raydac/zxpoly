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

import java.awt.Color;

public final class ZXPalette {
  public static final Color[] COLORS = new Color[]{
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
    new Color(0, 0, 0),
    new Color(0, 0, 255),
    new Color(255, 0, 0),
    new Color(255, 0, 255),
    new Color(0, 255, 0),
    new Color(0, 255, 255),
    new Color(255, 255, 0),
    new Color(255, 255, 255)
  };
  
  private ZXPalette(){
    
  }
}
