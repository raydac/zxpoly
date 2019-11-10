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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import java.io.IOException;

public class ZXEMLSnapshotFormat {

  public static final int MAGIC = 0xC0BA0100;
  public static final int INDEX_CPU0 = 0;
  public static final int INDEX_CPU1 = 1;
  public static final int INDEX_CPU2 = 2;
  public static final int INDEX_CPU3 = 3;
  private static final JBBPParser ZXEML_SNAPSHOT = JBBPParser.prepare("int magic; int flags; ubyte port3D00; ubyte portFE;"
      + "byte [5] cpu0ports; byte [5] cpu1ports; byte [5] cpu2ports; byte [5] cpu3ports;"
      + "short [4] reg_af; short [4] reg_af_alt; short [4] reg_bc; short [4] reg_bc_alt; short [4] reg_de; short [4] reg_de_alt; short [4] reg_hl; short [4] reg_hl_alt; short [4] reg_ix; short [4] reg_iy; short [4] reg_ir;"
      + "byte [4] reg_im; bool [4] iff; bool [4] iff2;"
      + "short [4] reg_pc; short [4] reg_sp;"
      + "pages [4]{ubyte number; page[number]{ubyte index; byte [16384] data;}}");
  @Bin(order = 1, type = BinType.INT)
  public int magic = MAGIC;
  @Bin(order = 2, type = BinType.INT)
  public int flags;
  @Bin(order = 3, type = BinType.UBYTE)
  public int port3D00;
  @Bin(order = 4, type = BinType.UBYTE)
  public int portFE;
  @Bin(order = 5, type = BinType.BYTE_ARRAY)
  public byte[] cpu0ports = new byte[5];
  @Bin(order = 6, type = BinType.BYTE_ARRAY)
  public byte[] cpu1ports = new byte[5];
  @Bin(order = 7, type = BinType.BYTE_ARRAY)
  public byte[] cpu2ports = new byte[5];
  @Bin(order = 8, type = BinType.BYTE_ARRAY)
  public byte[] cpu3ports = new byte[5];
  @Bin(order = 9, type = BinType.SHORT_ARRAY)
  public short[] reg_af = new short[4];
  @Bin(order = 10, type = BinType.SHORT_ARRAY)
  public short[] reg_af_alt = new short[4];
  @Bin(order = 11, type = BinType.SHORT_ARRAY)
  public short[] reg_bc = new short[4];
  @Bin(order = 12, type = BinType.SHORT_ARRAY)
  public short[] reg_bc_alt = new short[4];
  @Bin(order = 13, type = BinType.SHORT_ARRAY)
  public short[] reg_de = new short[4];
  @Bin(order = 14, type = BinType.SHORT_ARRAY)
  public short[] reg_de_alt = new short[4];
  @Bin(order = 15, type = BinType.SHORT_ARRAY)
  public short[] reg_hl = new short[4];
  @Bin(order = 16, type = BinType.SHORT_ARRAY)
  public short[] reg_hl_alt = new short[4];
  @Bin(order = 17, type = BinType.SHORT_ARRAY)
  public short[] reg_ix = new short[4];
  @Bin(order = 18, type = BinType.SHORT_ARRAY)
  public short[] reg_iy = new short[4];
  @Bin(order = 19, type = BinType.SHORT_ARRAY)
  public short[] reg_ir = new short[4];
  @Bin(order = 20, type = BinType.BYTE_ARRAY)
  public byte[] reg_im = new byte[4];
  @Bin(order = 21, type = BinType.BOOL_ARRAY)
  public boolean[] iff = new boolean[4];
  @Bin(order = 22, type = BinType.BOOL_ARRAY)
  public boolean[] iff2 = new boolean[4];
  @Bin(order = 23, type = BinType.SHORT_ARRAY)
  public short[] reg_pc = new short[4];
  @Bin(order = 24, type = BinType.SHORT_ARRAY)
  public short[] reg_sp = new short[4];
  @Bin(order = 25, type = BinType.STRUCT_ARRAY)
  public Pages[] pages = new Pages[] {
      new Pages(new Page[] {new Page(0, new byte[16384])}),
      new Pages(new Page[] {new Page(0, new byte[16384])}),
      new Pages(new Page[] {new Page(0, new byte[16384])}),
      new Pages(new Page[] {new Page(0, new byte[16384])})
  };

  public ZXEMLSnapshotFormat() {
  }

  public ZXEMLSnapshotFormat(final byte[] data) throws IOException {
    this.pages = null;
    ZXEML_SNAPSHOT.parse(data).mapTo(this);
    if (this.magic != MAGIC) {
      throw new IOException("It is not ZXEML snapshot");
    }
  }

  public void setPages(final int cpuIndex, final Pages pages) {
    if (pages == null) {
      throw new NullPointerException("Value must not be null");
    }
    this.pages[cpuIndex] = pages;
  }

  public Pages getPages(final int cpuIndex) {
    return this.pages[cpuIndex];
  }

  public int getFlags() {
    return this.flags;
  }

  public void setFlags(final int value) {
    this.flags = value;
  }

  public void setHL(final int cpuIndex, final int value, final boolean alt) {
    if (alt) {
      reg_hl_alt[cpuIndex] = (short) value;
    } else {
      reg_hl[cpuIndex] = (short) value;
    }
  }

  public int getHL(final int cpuIndex, final boolean alt) {
    return (alt ? reg_de_alt[cpuIndex] : reg_de[cpuIndex]) & 0xFFFF;
  }

  public void setDE(final int cpuIndex, final int value, final boolean alt) {
    if (alt) {
      reg_de_alt[cpuIndex] = (short) value;
    } else {
      reg_de[cpuIndex] = (short) value;
    }
  }

  public int getDE(final int cpuIndex, final boolean alt) {
    return (alt ? reg_de_alt[cpuIndex] : reg_de[cpuIndex]) & 0xFFFF;
  }

  public void setBC(final int cpuIndex, final int value, final boolean alt) {
    if (alt) {
      reg_bc_alt[cpuIndex] = (short) value;
    } else {
      reg_bc[cpuIndex] = (short) value;
    }
  }

  public int getBC(final int cpuIndex, final boolean alt) {
    return (alt ? reg_bc_alt[cpuIndex] : reg_bc[cpuIndex]) & 0xFFFF;
  }

  public void setAF(final int cpuIndex, final int value, final boolean alt) {
    if (alt) {
      reg_af_alt[cpuIndex] = (short) value;
    } else {
      reg_af[cpuIndex] = (short) value;
    }
  }

  public int getAF(final int cpuIndex, final boolean alt) {
    return (alt ? reg_af_alt[cpuIndex] : reg_af[cpuIndex]) & 0xFFFF;
  }

  public void setIFF2(final int cpuIndex, final boolean value) {
    iff2[cpuIndex] = value;
  }

  public boolean isIFF2(final int cpuIndex) {
    return iff2[cpuIndex];
  }

  public void setIFF(final int cpuIndex, final boolean value) {
    iff[cpuIndex] = value;
  }

  public boolean isIFF(final int cpuIndex) {
    return iff[cpuIndex];
  }

  public void setRegIX(final int cpuIndex, final int value) {
    reg_ix[cpuIndex] = (short) value;
  }

  public int getRegIX(final int cpuIndex) {
    return reg_ix[cpuIndex] & 0xFFFF;
  }

  public void setRegIY(final int cpuIndex, final int value) {
    reg_iy[cpuIndex] = (short) value;
  }

  public int getRegIY(final int cpuIndex) {
    return reg_iy[cpuIndex] & 0xFFFF;
  }

  public void setRegIR(final int cpuIndex, final int value) {
    reg_ir[cpuIndex] = (short) value;
  }

  public int getRegIR(final int cpuIndex) {
    return reg_ir[cpuIndex] & 0xFFFF;
  }

  public void setRegPC(final int cpuIndex, final int value) {
    reg_pc[cpuIndex] = (short) value;
  }

  public int getRegPC(final int cpuIndex) {
    return reg_pc[cpuIndex] & 0xFFFF;
  }

  public void setRegSP(final int cpuIndex, final int value) {
    reg_sp[cpuIndex] = (short) value;
  }

  public int getRegSP(final int cpuIndex) {
    return reg_sp[cpuIndex] & 0xFFFF;
  }

  public void setRegIM(final int cpuIndex, final int value) {
    reg_im[cpuIndex] = (byte) value;
  }

  public int getRegIM(final int cpuIndex) {
    return reg_im[cpuIndex] & 0xFF;
  }

  public int getPort3D00() {
    return this.port3D00;
  }

  public void setPort3D00(final int value) {
    this.port3D00 = value;
  }

  public int getPortFE() {
    return this.portFE;
  }

  public void setPortFE(final int value) {
    this.portFE = value;
  }

  public void setModulePorts(final int cpuIndex, final int port7FFD, final int r0, final int r1, final int r2, final int r3) {
    final byte[] data = new byte[] {(byte) port7FFD, (byte) r0, (byte) r1, (byte) r2, (byte) r3};
    switch (cpuIndex) {
      case INDEX_CPU0:
        this.cpu0ports = data;
        break;
      case INDEX_CPU1:
        this.cpu1ports = data;
        break;
      case INDEX_CPU2:
        this.cpu2ports = data;
        break;
      case INDEX_CPU3:
        this.cpu3ports = data;
        break;
      default:
        throw new IllegalArgumentException("Illegal CPU index");
    }
  }

  public byte[] getModulePorts(final int cpuIndex) {
    switch (cpuIndex) {
      case INDEX_CPU0:
        return this.cpu0ports;
      case INDEX_CPU1:
        return this.cpu1ports;
      case INDEX_CPU2:
        return this.cpu2ports;
      case INDEX_CPU3:
        return this.cpu3ports;
      default:
        throw new IllegalArgumentException("Illegal CPU index");
    }
  }

  public byte[] save() throws IOException {
    if (this.pages == null) {
      throw new NullPointerException("Pages must not be null");
    }
    for (final Pages p : this.pages) {
      if (p == null) {
        throw new NullPointerException("Item in Pages contains null");
      }
      for (final Page pp : p.page) {
        if (pp == null) {
          throw new NullPointerException("Detected defined null page");
        }
      }
    }

    return JBBPOut.BeginBin().Bin(this).End().toByteArray();
  }

  public static class Page {

    @Bin(order = 1, type = BinType.UBYTE)
    public int index;
    @Bin(order = 2, type = BinType.BYTE_ARRAY)
    public byte[] data;

    public Page(final int index, final byte[] data) {
      if (index < 0 || index > 7) {
        throw new IllegalArgumentException("Page muste be 0..7");
      }
      if (data == null || data.length != 0x4000) {
        throw new IllegalArgumentException("Data must not be null and must have length 0x4000");
      }
      this.index = index;
      this.data = data.clone();
    }

    public int getIndex() {
      return this.index;
    }

    public byte[] getData() {
      return this.data;
    }
  }

  public static class Pages {

    @Bin(order = 1, type = BinType.UBYTE)
    public int number;
    @Bin(order = 2, type = BinType.STRUCT_ARRAY)
    public Page[] page;

    public Pages(final Page[] page) {
      this.number = page.length;
      this.page = page;
    }

    public Page[] getPages() {
      return this.page;
    }
  }
}
