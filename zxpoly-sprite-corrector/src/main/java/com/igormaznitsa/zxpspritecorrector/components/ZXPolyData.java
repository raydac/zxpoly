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

package com.igormaznitsa.zxpspritecorrector.components;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.plugins.AbstractFilePlugin;
import java.awt.Rectangle;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class ZXPolyData {

  public static final int ZXPOLY_0 = 0;
  public static final int ZXPOLY_1 = 1;
  public static final int ZXPOLY_2 = 2;
  public static final int ZXPOLY_3 = 3;
  private static final JBBPParser PARSER = JBBPParser.prepare(
      "int pluginId;"
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
  private final byte[] basedata;
  private final byte[] mask;
  private final byte[][] zxpoly;
  private final AbstractFilePlugin basePlugin;

  private final Info info;

  public ZXPolyData(final Info info, final AbstractFilePlugin basePlugin, final byte[] data, final byte[] mask, byte[][] zxpolyPlanes) {
    if (data.length != mask.length) {
      throw new IllegalArgumentException("Mask array must have same size as data array: " + data.length + "!=" + mask.length);
    }
    this.basedata = data.clone();
    this.mask = mask.clone();
    this.zxpoly = new byte[4][];
    for (int i = 0; i < this.zxpoly.length; i++) {
      if (this.basedata.length != zxpolyPlanes[i].length) {
        throw new IllegalArgumentException("Detected illegal size of ZXPoly plane: " + i + ", expected size " + this.basedata.length + " bytes");
      }
      this.zxpoly[i] = zxpolyPlanes[i].clone();
    }
    this.info = info;
    this.basePlugin = basePlugin;
  }

  public ZXPolyData(final Info info, final AbstractFilePlugin basePlugin, final byte[] array) {
    this(info, basePlugin, array, new byte[array.length], new byte[4][array.length]);
  }

  public ZXPolyData(final InputStream in, final List<AbstractFilePlugin> plugins) throws IOException {
    @Bin
    final class Parsed {

      int pluginId;
      byte[] info;
      byte[] array;
      byte[] mask;
      byte[] zxpoly0;
      byte[] zxpoly1;
      byte[] zxpoly2;
      byte[] zxpoly3;
    }

    final JBBPBitInputStream inStream = new JBBPBitInputStream(in);
    if (inStream.readLong(JBBPByteOrder.BIG_ENDIAN) != 0xABBAFAFABABE0123L) {
      throw new IOException("It is not a valid data block");
    }

    final Parsed parsed = PARSER.parse(inStream).mapTo(new Parsed());

    this.info = new Info(new ByteArrayInputStream(parsed.info));
    this.basedata = parsed.array;
    this.mask = parsed.mask;
    this.zxpoly = new byte[4][];
    this.zxpoly[0] = parsed.zxpoly0;
    this.zxpoly[1] = parsed.zxpoly1;
    this.zxpoly[2] = parsed.zxpoly2;
    this.zxpoly[3] = parsed.zxpoly3;

    AbstractFilePlugin plugin = null;
    for (final AbstractFilePlugin p : plugins) {
      if (parsed.pluginId == uid2int(p.getPluginUID())) {
        plugin = p;
        break;
      }
    }
    if (plugin == null) {
      throw new IOException("Can't find a plugin for UID [" + parsed.pluginId + ']');
    }
    this.basePlugin = plugin;

  }

  private static int uid2int(final String str) {
    if (str.length() != 4) {
      throw new IllegalArgumentException("Plugin UID must have 4 chars [" + str + ']');
    }

    int result = 0;
    for (int i = 0; i < 4; i++) {
      result = (result << 8) | (str.charAt(i) & 0xFF);
    }
    return result;
  }

  public UndoBlock makeUndo(final Rectangle selectedArea) {
    return new UndoBlock(selectedArea, this.mask, this.zxpoly);
  }

  public void restoreFromUndo(final UndoBlock undo) {
    System.arraycopy(undo.mask, 0, this.mask, 0, undo.mask.length);
    for (int i = 0; i < 4; i++) {
      System.arraycopy(undo.zxpoly[i], 0, this.zxpoly[i], 0, undo.zxpoly[i].length);
    }
  }

  public byte[] getAsArray() throws IOException {
    final byte[] packedInfo = this.info.save(JBBPOut.BeginBin()).End().toByteArray();

    return JBBPOut.BeginBin().
        Long(0xABBAFAFABABE0123L).
        Int(uid2int(this.basePlugin.getPluginUID())).
        Short(packedInfo.length).
        Byte(packedInfo).
        Int(this.basedata.length).
        Byte(this.basedata).
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
    return this.basedata[address] & 0xFF;
  }

  public byte[] getDataForCPU(final int cpuIndex) {
    final byte[] result = this.zxpoly[cpuIndex].clone();

    for (int i = 0; i < this.basedata.length; i++) {
      final byte maskdata = this.mask[i];
      final byte basedata = this.basedata[i];
      if (maskdata == 0) {
        result[i] = basedata;
      } else {
        result[i] = (byte) ((basedata & (~maskdata)) | (result[i] & maskdata));
      }
    }

    return result;
  }

  public AbstractFilePlugin getPlugin() {
    return this.basePlugin;
  }

  public void clear() {
    Arrays.fill(this.mask, (byte) 0);
    for (final byte[] b : this.zxpoly) {
      Arrays.fill(b, (byte) 0);
    }
  }

  public int length() {
    return this.basedata.length;
  }

  public void copyPlansFromBase() {
    for (final byte[] plane : this.zxpoly) {
      System.arraycopy(this.basedata, 0, plane, 0, this.basedata.length);
    }
    Arrays.fill(this.mask, (byte) 0xFF);
  }

  public static class UndoBlock {

    private final Rectangle selectedArea;
    private final byte[] mask;
    private final byte[][] zxpoly;

    private UndoBlock(final Rectangle selectedArea, final byte[] mask, final byte[][] zxpoly) {
      this.selectedArea = selectedArea == null ? null : new Rectangle(selectedArea);
      this.mask = mask.clone();
      this.zxpoly = new byte[4][];
      for (int i = 0; i < 4; i++) {
        this.zxpoly[i] = zxpoly[i].clone();
      }
    }

    public Rectangle getSelectedArea() {
      return this.selectedArea;
    }
  }
}
