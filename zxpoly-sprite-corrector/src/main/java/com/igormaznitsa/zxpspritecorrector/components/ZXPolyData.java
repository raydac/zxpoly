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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.zxpspritecorrector.files.AbstractFilePlugin;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import java.io.*;
import java.util.*;

public class ZXPolyData {

  private static final JBBPParser PARSER = JBBPParser.prepare(
          "long magic;"
          + "int pluginId;"
          + "ushort infoLength;"
          + "byte [infoLength] info;"
          + "int length;"
          + "byte [length] array;"
          + "byte [length] mask;"
          + "byte [length] zxpoly0;"
          + "byte [length] zxpoly1;"
          + "byte [length] zxpoly2;"
          + "byte [length] zxpoly3;"
          );

  public static final int ZXPOLY_0 = 0;
  public static final int ZXPOLY_1 = 1;
  public static final int ZXPOLY_2 = 2;
  public static final int ZXPOLY_3 = 3;

  private final byte[] array;
  private final byte[] mask;
  private final byte[][] zxpoly;
  private final AbstractFilePlugin basePlugin;

  private final Info info;

  public ZXPolyData(final Info info, final AbstractFilePlugin basePlugin, final byte[] array) {
    this.array = array.clone();
    this.mask = new byte[this.array.length];
    this.zxpoly = new byte[4][this.array.length];
    this.info = info;
    this.basePlugin = basePlugin;
  }

  public ZXPolyData(final InputStream in, final List<AbstractFilePlugin> plugins) throws IOException {
    @Bin
    final class Parsed {
      byte[] info;
      byte[] array;
      byte[] mask;
      byte[] zxpoly0;
      byte[] zxpoly1;
      byte[] zxpoly2;
      byte[] zxpoly3;
      int pluginId;
    }

    final JBBPBitInputStream inStream = new JBBPBitInputStream(in);
    try {
      if (inStream.readLong(JBBPByteOrder.BIG_ENDIAN) != 0xABBAFAFABABE0123L) {
        throw new IOException("It is not a valid data block");
      }

      final Parsed parsed = PARSER.parse(inStream).mapTo(Parsed.class);

      this.info = new Info(new ByteArrayInputStream(parsed.info));
      this.array = parsed.array;
      this.mask = parsed.mask;
      this.zxpoly = new byte[4][];
      this.zxpoly[0] = parsed.zxpoly0;
      this.zxpoly[1] = parsed.zxpoly1;
      this.zxpoly[2] = parsed.zxpoly2;
      this.zxpoly[3] = parsed.zxpoly3;

      AbstractFilePlugin plugin = null;
      for (AbstractFilePlugin p : plugins) {
        if (parsed.pluginId == p.getUID()) {
          plugin = p;
          break;
        }
      }
      if (plugin == null) {
        throw new IOException("Can't find a plugin for UID [" + parsed.pluginId + ']');
      }
      this.basePlugin = plugin;
    }
    finally {
      JBBPUtils.closeQuietly(inStream);
    }
  }

  public byte[] getAsArray() throws IOException {
    final byte [] packedInfo = this.info.save(JBBPOut.BeginBin()).End().toByteArray();
    
    return JBBPOut.BeginBin().
            Long(0xABBAFAFABABE0123L).
            Int(this.basePlugin.getUID()).
            Short(packedInfo.length).
            Byte(packedInfo).
            Int(this.array.length).
            Byte(this.array).
            Byte(this.mask).
            Byte(this.zxpoly[0]).
            Byte(this.zxpoly[1]).
            Byte(this.zxpoly[2]).
            Byte(this.zxpoly[3]).End().toByteArray();
  }

  public Info getInfo() {
    return this.info;
  }

  public void setZXPolyData(final int address, final int mask, final int zxpoly0, final int zxpoly1, final int zxpoly2, final int zxpoly3) {
    this.zxpoly[ZXPOLY_0][address] = (byte) (zxpoly0 & mask);
    this.zxpoly[ZXPOLY_1][address] = (byte) (zxpoly1 & mask);
    this.zxpoly[ZXPOLY_2][address] = (byte) (zxpoly2 & mask);
    this.zxpoly[ZXPOLY_3][address] = (byte) (zxpoly3 & mask);
    this.mask[address] = (byte) mask;
  }

  public int getMask(final int address) {
    return this.mask[address] & 0xFF;
  }

  public int getPackedZxPolyData3012(final int address) {
    return ((this.zxpoly[ZXPOLY_3][address] & 0xFF) << 24) | ((this.zxpoly[ZXPOLY_0][address] & 0xFF) << 16) | ((this.zxpoly[ZXPOLY_1][address] & 0xFF) << 8) | (this.zxpoly[ZXPOLY_2][address] & 0xFF);
  }

  public int getBaseData(final int address) {
    return this.array[address];
  }

  public void clear() {
    Arrays.fill(this.mask, (byte) 0);
    for (final byte[] b : this.zxpoly) {
      Arrays.fill(b, (byte) 0);
    }
  }

  public int length() {
    return this.array.length;
  }

}
