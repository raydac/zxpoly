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

package com.igormaznitsa.zxpspritecorrector.components;

import java.awt.Dimension;

/**
 *
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 */
public enum VideoMode {
  ZXPOLY(256, 192),
  ZX_512x384(512, 384);

  private static final int [] ZX_Y_DECODE_TABLE = new int [192];
  
  static {
    for (int y = 0; y < 192; y++) {
      ZX_Y_DECODE_TABLE[y] = calcYofAddress(y<<5);
    }
  }

  public static int calcYofAddress(final int address) {
    return (((address & 0x00e0)) >> 2) + (((address & 0x0700)) >> 8) + (((address & 0x1800)) >> 5);
  }

  public static int linearYtoZXY(final int y){
    return ZX_Y_DECODE_TABLE[y];
  }
  
  private final Dimension size; 
  
  private VideoMode(final int width, final int height){
    this.size = new Dimension(width, height);
  }
  
  public Dimension getSize(){
    return this.size;
  }
}
