/*
 * Copyright (C) 2014-2019 Igor Maznitsa
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

package com.igormaznitsa.zxpoly.formats;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class ZxEmlSnapshotFormat extends ZXPParser {

  public static final int MAGIC = 0xC0BA0100;
  public static final int INDEX_CPU0 = 0;
  public static final int INDEX_CPU1 = 1;
  public static final int INDEX_CPU2 = 2;
  public static final int INDEX_CPU3 = 3;

  public ZxEmlSnapshotFormat() {
    super();

    this.setMAGIC(MAGIC);

    this.cpu0ports = new byte[5];
    this.cpu1ports = new byte[5];
    this.cpu2ports = new byte[5];
    this.cpu3ports = new byte[5];

    this.reg_af = new short[4];
    this.reg_af_alt = new short[4];
    this.reg_bc = new short[4];
    this.reg_bc_alt = new short[4];
    this.reg_de = new short[4];
    this.reg_de_alt = new short[4];
    this.reg_hl = new short[4];
    this.reg_hl_alt = new short[4];
    this.reg_ix = new short[4];
    this.reg_iy = new short[4];
    this.reg_ir = new short[4];
    this.reg_im = new byte[4];
    this.iff = new boolean[4];
    this.iff2 = new boolean[4];
    this.reg_pc = new short[4];
    this.reg_sp = new short[4];

    this.pages = new PAGES[4];
    for (int j = 0; j < this.pages.length; j++) {
      this.pages[j] = new PAGES(this);
      this.pages[j].number = 8;
      this.pages[j].page = new PAGES.PAGE[8];
      for (int i = 0; i < this.pages[j].page.length; i++) {
        this.pages[j].page[i] = new PAGES.PAGE(this);
        this.pages[j].page[i].index = (char) i;
        this.pages[j].page[i].data = new byte[16384];
      }
    }
  }

  public ZxEmlSnapshotFormat(final byte[] data) throws IOException {
    this.read(new JBBPBitInputStream(new ByteArrayInputStream(data), false));
    if (this.getMAGIC() != MAGIC) {
      throw new IOException("It is not ZXEML snapshot");
    }
  }

  public void setModulePorts(final int cpuIndex, final int port7FFD, final int r0, final int r1, final int r2, final int r3) {
    final byte[] data = new byte[]{(byte) port7FFD, (byte) r0, (byte) r1, (byte) r2, (byte) r3};
    switch (cpuIndex) {
      case INDEX_CPU0:
        this.setCPU0PORTS(data);
        break;
      case INDEX_CPU1:
        this.setCPU1PORTS(data);
        break;
      case INDEX_CPU2:
        this.setCPU2PORTS(data);
        break;
      case INDEX_CPU3:
        this.setCPU3PORTS(data);
        break;
      default:
        throw new IllegalArgumentException("Illegal CPU index");
    }
  }

  public byte[] getModulePorts(final int cpuIndex) {
    switch (cpuIndex) {
      case INDEX_CPU0:
        return this.getCPU0PORTS();
      case INDEX_CPU1:
        return this.getCPU1PORTS();
      case INDEX_CPU2:
        return this.getCPU2PORTS();
      case INDEX_CPU3:
        return this.getCPU3PORTS();
      default:
        throw new IllegalArgumentException("Illegal CPU index");
    }
  }

  public byte[] save() throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream bout = new JBBPBitOutputStream(buffer);
    this.write(bout);
    bout.close();
    return buffer.toByteArray();
  }
}
