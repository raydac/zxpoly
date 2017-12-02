/*
 * Copyright (C) 2017 Raydac Research Group Ltd.
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
package com.igormaznitsa.zxpoly.components;

import com.igormaznitsa.z80.Utils;

public class DisasmLine {

  public final int address;
  public final String text;

  public DisasmLine(final int address, final String text) {
    this.address = address;
    this.text = text == null ? "<UNKNOWN>" : text;
  }

  @Override
  public String toString() {
    return Utils.toHex(this.address) + ' ' + this.text;
  }

}
