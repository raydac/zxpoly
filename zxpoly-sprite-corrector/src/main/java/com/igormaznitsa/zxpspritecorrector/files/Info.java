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

package com.igormaznitsa.zxpspritecorrector.files;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public class Info {

  private final String name;
  private final char type;
  private final int startAddress;
  private final int length;
  private final int offset;
  private final byte[] extra;
  private final boolean selectable;

  public Info(final String name, final char type, final int startAddress, final int length,
              final int offset, final boolean selectable, final byte... extra) {
    this.name = name;
    this.type = type;
    this.startAddress = startAddress;
    this.length = length;
    this.offset = offset;
    this.extra = extra;
    this.selectable = selectable;
  }

  public Info(final InputStream in, final boolean selectable) throws IOException {
    final JBBPBitInputStream bitin = new JBBPBitInputStream(in, false);
    this.name = new String(bitin.readByteArray(bitin.readByte()), StandardCharsets.US_ASCII);
    this.type = (char) bitin.readUnsignedShort(JBBPByteOrder.BIG_ENDIAN);
    this.startAddress = bitin.readInt(JBBPByteOrder.BIG_ENDIAN);
    this.length = bitin.readInt(JBBPByteOrder.BIG_ENDIAN);
    this.offset = bitin.readInt(JBBPByteOrder.BIG_ENDIAN);
    this.extra = bitin.readByteArray(bitin.readInt(JBBPByteOrder.BIG_ENDIAN));
    this.selectable = selectable;
  }

  public boolean isSelectable() {
    return this.selectable;
  }

  public JBBPOut save(final JBBPOut context) throws IOException {
    context.Byte(name.length()).Byte(name).Short(this.type)
            .Int(this.startAddress, this.length, this.offset).Int(this.extra.length).Byte(this.extra);
    return context;
  }

  public byte[] getExtra() {
    return this.extra;
  }

  public int getOffset() {
    return this.offset;
  }

  public String getName() {
    return this.name;
  }

  public char getType() {
    return this.type;
  }

  public int getStartAddress() {
    return this.startAddress;
  }

  public int getLength() {
    return this.length;
  }

  @Override
  public String toString() {
    final StringBuilder result = new StringBuilder(32);
    result.append("  ");
    result.append(name);
    while (result.length() < 10) {
      result.append(' ');
    }
    while (result.length() < 18) {
      result.append('.');
    }
    result.append('<').append(type).append('>').append(' ').append(length).append(" bytes")
        .append("  ");
    return result.toString();
  }
}
