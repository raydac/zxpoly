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
package com.igormaznitsa.zxpoly.components.betadisk;

import com.igormaznitsa.zxpoly.components.IODevice;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZXPolyModule;
import java.util.logging.Logger;

public class BetaDiscInterface implements IODevice {

  private static final Logger log = Logger.getLogger("BD");

  private final Motherboard board;
  private final K1818VG93 vg93;
  private int ffPort;

  public BetaDiscInterface(final Motherboard board) {
    this.board = board;
    this.vg93 = new K1818VG93(log);
  }

  public void setDisk(final TRDOSDisk drive) {
    this.vg93.setDisk(drive);
  }

  @Override
  public int readIO(final ZXPolyModule module, final int port) {
    if (module.isTRDOSActive()) {
      switch (port & 0xFF) {
        case 0x1F: {
          return vg93.read(K1818VG93.ADDR_COMMAND_STATE);
        }
        case 0x3F: {
          return vg93.read(K1818VG93.ADDR_TRACK);
        }
        case 0x5F: {
          return vg93.read(K1818VG93.ADDR_SECTOR);
        }
        case 0x7F: {
          return vg93.read(K1818VG93.ADDR_DATA);
        }
        case 0xFF: {
          final int stat = vg93.read(K1818VG93.ADDR_COMMAND_STATE);
          return (((stat & K1818VG93.STAT_BUSY) == 0 ? 0x80 : 0) | ((stat & K1818VG93.STAT_DRQ) == 0 ? 0: 0x40)) | 0b00111111;
        }
      }
    }
    return 0;
  }

  @Override
  public void writeIO(final ZXPolyModule module, final int port, final int value) {
    if (module.isTRDOSActive()) {
      switch (port & 0xFF) {
        case 0x1F: {
          vg93.write(K1818VG93.ADDR_COMMAND_STATE, value);
        }
        break;
        case 0x3F: {
          vg93.write(K1818VG93.ADDR_TRACK, value);
        }
        break;
        case 0x5F: {
          vg93.write(K1818VG93.ADDR_SECTOR, value);
        }
        break;
        case 0x7F: {
          vg93.write(K1818VG93.ADDR_DATA, value);
        }
        break;
        case 0xFF: {
          setSystemReg(value);
        }
        break;
      }
    }
  }

  private void setSystemReg(final int value) {
    this.ffPort = value;
    this.vg93.setResetIn((this.ffPort & 0b00000100) != 0);
    this.vg93.setSide((this.ffPort & 0b00010000)==0? 1 : 0);
  }

  @Override
  public Motherboard getMotherboard() {
    return this.board;
  }

  @Override
  public void preStep(final boolean signalReset, final boolean signalInt) {
    if (signalReset) {
      this.vg93.reset();
    }

    if ((this.ffPort & 0b00001000)!=0){
      this.vg93.step();
    }
  }

  @Override
  public String getName() {
    return "BetaDiscInterface";
  }

}
