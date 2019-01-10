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
package com.igormaznitsa.z80;

import java.util.Locale;

public enum Utils {

  ;
  public static String toHex(final int value) {
    final String h = Integer.toHexString(value).toUpperCase(Locale.ENGLISH);
    return '#' + (h.length() < 4 ? "0000".substring(0, 4 - h.length()) + h : h);
  }

  public static String toHexByte(final byte value) {
    final String h = Integer.toHexString(value & 0xFF).toUpperCase(Locale.ENGLISH);
    return '#' + (h.length() < 2 ? "0" : "") + h;
  }

}
