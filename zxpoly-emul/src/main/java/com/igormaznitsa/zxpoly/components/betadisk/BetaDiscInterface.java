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
import java.util.concurrent.atomic.AtomicReferenceArray;
import java.util.logging.Logger;

public class BetaDiscInterface implements IODevice {

  public static final int DRIVE_A = 0;
  public static final int DRIVE_B = 1;
  public static final int DRIVE_C = 2;
  public static final int DRIVE_D = 3;

  private static final Logger LOGGER = Logger.getLogger("BD");

  private long mcycleCounter = 0L;

  private final Motherboard board;
  private final K1818VG93 vg93;
  private int ffPort;

  private final AtomicReferenceArray<TRDOSDisk> diskDrives = new AtomicReferenceArray<>(4);

  public BetaDiscInterface(final Motherboard board) {
    this.board = board;
    this.vg93 = new K1818VG93(LOGGER);
  }

  public void insertDiskIntoDrive(final int driveIndex, final TRDOSDisk disk) {
    this.diskDrives.set(driveIndex, disk);
    tuneControllerToDisk();
  }

  private void tuneControllerToDisk() {
    final int driveIndex = this.ffPort & 0x3;
    this.vg93.activateDisk(driveIndex, this.diskDrives.get(driveIndex));
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
          return (((stat & K1818VG93.STAT_BUSY) == 0 ? 0x80 : 0) | ((stat & K1818VG93.STAT_DRQ) == 0 ? 0 : 0x40)) | 0b00111111;
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
    this.vg93.setSide((this.ffPort & 0b00010000) == 0 ? 1 : 0);
    this.vg93.setMFMModulation((this.ffPort & 0b00010000) == 0);
    tuneControllerToDisk();
  }

  public boolean isActive() {
    return this.vg93.isMotorOn();
  }

  @Override
  public Motherboard getMotherboard() {
    return this.board;
  }

  @Override
  public void preStep(final boolean signalReset, final boolean signalInt) {
    if (signalReset) {
      doReset();
    }

    if ((this.ffPort & 0b00001000) != 0) {
      this.vg93.step(this.mcycleCounter);
    }
  }

  @Override
  public String getName() {
    return "BetaDiscInterface";
  }

  @Override
  public void postStep(final long spentMachineCyclesForStep) {
    this.mcycleCounter = Math.abs(this.mcycleCounter + spentMachineCyclesForStep);
  }

  @Override
  public void doReset() {
    this.mcycleCounter = 0L;
    this.vg93.reset();
  }

  @Override
  public String toString() {
    return this.getName();
  }

}
