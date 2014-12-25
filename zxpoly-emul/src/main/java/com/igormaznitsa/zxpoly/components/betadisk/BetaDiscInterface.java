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
  private final VG93 vg93;
  private int ffPort;

  public BetaDiscInterface(final Motherboard board) {
    this.board = board;
    this.vg93 = new VG93();
  }

  public void setDisk(final Floppy drive) {
    this.vg93.setDisk(drive);
  }

  @Override
  public int readIO(final ZXPolyModule module, final int port) {
    if (module.isTRDOSActive()) {
      switch (port & 0xFF) {
        case 0x1F: {
          return vg93.getStatusReg();
        }
        case 0x3F: {
          return vg93.getTrackReg();
        }
        case 0x5F: {
          return vg93.getSectorReg();
        }
        case 0x7F: {
          return vg93.getDataReg();
        }
        case 0xFF: {
          final int stat = this.vg93.getStatusReg();
          return (((stat & 1) == 0 ? 0x80 : 0) | ((stat & 2) == 0 ? 0x40 : 0));
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
          vg93.setCommandReg(value);
        }
        break;
        case 0x3F: {
          vg93.setTrackReg(value);
        }
        break;
        case 0x5F: {
          vg93.setSectorReg(value);
        }
        break;
        case 0x7F: {
          vg93.setDataReg(value);
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
    if ((this.ffPort & 4) == 0) {
      this.vg93.setResetSignal(true);
    }
    else {
      this.vg93.setResetSignal(false);
    }
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
  }

  @Override
  public String getName() {
    return "BetaDiscInterface";
  }

}
