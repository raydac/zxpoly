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

  private static final byte [] LINEAR_TO_ZX_Y = new byte[192];
  private static final byte [] ZX_Y_TO_LINEAR = new byte[192];
  
  static {
    int a = 16384;
    for(int y = 0;y<192;y++){
      final int zxy = extractYFromAddress(a);
      LINEAR_TO_ZX_Y[y] = (byte)zxy;
      ZX_Y_TO_LINEAR[zxy] = (byte)y;
      a+=32;
    }
  }
  
  public static int zxy2y(final int y){
    return ZX_Y_TO_LINEAR[y] & 0xFF;
  }
  
  public static int y2zxy(final int y){
    return LINEAR_TO_ZX_Y[y] & 0xFF;
  }
  
  public static int extractYFromAddress(final int address) {
    return ((address & 0x1800)>>5) | ((address & 0x700)>>8) | ((address & 0xE0)>>2);
  }

  public static int extractXFromAddress(final int address){
    return address & 0x1F;
  }
  
  private final Dimension size; 
  
  private VideoMode(final int width, final int height){
    this.size = new Dimension(width, height);
  }
  
  public Dimension getSize(){
    return this.size;
  }
}
