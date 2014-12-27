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

import com.igormaznitsa.zxpoly.components.betadisk.TRDOSDisk.Sector;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public class VG93 {

  public static final int ADDR_COMMAND_STATE = 0;
  public static final int ADDR_TRACK = 1;
  public static final int ADDR_SECTOR = 2;
  public static final int ADDR_DATA = 3;

  private static final int REG_COMMAND = 0x00;
  private static final int REG_STATUS = 0x01;
  private static final int REG_TRACK = 0x02;
  private static final int REG_SECTOR = 0x03;
  private static final int REG_DATA_WR = 0x04;
  private static final int REG_DATA_RD = 0x05;

  public static final int ST_BUSY = 0x01;
  public static final int ST_INDEX = 0x02;
  public static final int ST_DRQ = 0x02;
  public static final int ST_TRK00 = 0x04;
  public static final int ST_LOST = 0x04;
  public static final int ST_CRCERR = 0x08;
  public static final int ST_NOTFOUND = 0x10;
  public static final int ST_SEEKERR = 0x10;
  public static final int ST_RECORDT = 0x20;
  public static final int ST_HEADL = 0x20;
  public static final int ST_WRFAULT = 0x20;
  public static final int ST_WRITEP = 0x40;
  public static final int ST_NOTRDY = 0x80;

  private TRDOSDisk.Sector sector;
  private final int[] registers = new int[6];
  private int counter;
  private boolean dataRequest;
  private boolean resetIn;
  private boolean firstCommandStep;
  private int side;
  private boolean outwardStepDirection;

  private final AtomicReference<TRDOSDisk> currentDisk = new AtomicReference<>();

  public VG93() {
    reset();
  }

  public final void reset() {
    Arrays.fill(registers, 0);
    this.dataRequest = false;
    this.resetIn = false;
    this.firstCommandStep = false;
    this.counter = 0;
  }

  public final void setDisk(final TRDOSDisk disk) {
    this.currentDisk.set(disk);
    if (disk == null) {
      this.sector = null;
    }
    else {
      loadSector(this.side, registers[REG_TRACK], registers[REG_SECTOR]);
    }
  }

  private void onStatus(final int flags) {
    registers[REG_STATUS] |= flags;
  }

  private void offStatus(final int flags) {
    registers[REG_STATUS] = (registers[REG_STATUS] & ~flags) & 0xFF;
  }

  private boolean isStatus(final int flags) {
    return (registers[REG_STATUS] & flags) != 0;
  }

  public void setResetIn(final boolean signal) {
    if (!this.resetIn && signal) {
      reset();
    }
    this.resetIn = signal;
  }

  public void setSide(final int side) {
    this.side = side;
  }

  public int read(final int addr) {
    switch (addr & 0x03) {
      case ADDR_COMMAND_STATE: {
        switch (registers[REG_COMMAND] >>> 4) {
          case 0b0000:
          case 0b0001:
          case 0b0010:
          case 0b0011:
          case 0b0100:
          case 0b0101:
          case 0b1000:
          case 0b1001: {
            // change index 
            if (this.currentDisk.get() != null) {
              if (isStatus(ST_INDEX)) {
                offStatus(ST_INDEX);
              }
              else {
                onStatus(ST_INDEX);
              }
            }
            else {
              onStatus(ST_INDEX);
            }
          }
          break;
        }
        return registers[REG_STATUS] & 0xFF;
      }
      case ADDR_TRACK:
        return registers[REG_TRACK] & 0xFF;
      case ADDR_SECTOR:
        return registers[REG_SECTOR] & 0xFF;
      case ADDR_DATA:
        if (this.dataRequest) {
          this.dataRequest = false;
          offStatus(ST_DRQ);
        }
        return registers[REG_DATA_RD] & 0xFF;
      default:
        throw new Error("Unexpected value");
    }
  }

  public void write(final int addr, final int value) {
    final int normValue = value & 0xFF;
    switch (addr & 0x03) {
      case ADDR_COMMAND_STATE: { // command
        if (isStatus(ST_BUSY)) {
          if ((normValue >>> 4) == 0b1101) {
            this.dataRequest = false;
            this.registers[REG_COMMAND] = normValue;
            this.firstCommandStep = true;
            onStatus(ST_BUSY);
          }
        }
        else {
          this.dataRequest = false;
          this.registers[REG_COMMAND] = normValue;
          this.firstCommandStep = true;
          onStatus(ST_BUSY);
        }
      }
      break;
      case ADDR_TRACK: { // track
        if (!isStatus(ST_BUSY)) {
          registers[REG_TRACK] = normValue;
        }
      }
      break;
      case ADDR_SECTOR: { // sector
        if (!isStatus(ST_BUSY)) {
          registers[REG_SECTOR] = normValue;
        }
      }
      break;
      case ADDR_DATA: { // data
        this.dataRequest = true;
        registers[REG_DATA_WR] = normValue;
      }
      break;
      default:
        throw new IllegalArgumentException("Unexpected value");
    }
  }

  public void step() {
    final TRDOSDisk thefloppy = this.currentDisk.get();
    if (thefloppy == null) {
      onStatus(ST_NOTRDY);
    }
    else {
      offStatus(ST_NOTRDY);
    }

    final int command = this.registers[REG_COMMAND];
    if (isStatus(ST_BUSY)) {
      final boolean first = this.firstCommandStep;
      this.firstCommandStep = false;
      switch (command >>> 4) {
        case 0b0000:
          cmdRestore(command, first);
          break;
        case 0b0001:
          cmdSeek(command, first);
          break;
        case 0b0010:
        case 0b0011:
          cmdStep(command, first);
          break;
        case 0b0100:
        case 0b0101:
          cmdStepIn(command, first);
          break;
        case 0b0110:
        case 0b0111:
          cmdStepOut(command, first);
          break;
        case 0b1000:
        case 0b1001:
          cmdReadSector(command, first);
          break;
        case 0b1010:
        case 0b1011:
          cmdWriteSector(command, first);
          break;
        case 0b1100:
          cmdReadAddress(command, first);
          break;
        case 0b1101:
          cmdForceInterrupt(command, first);
          break;
        case 0b1110:
          cmdWriteTrack(command, first);
          break;
        case 0b1111:
          cmdReadTrack(command, first);
          break;
        default:
          throw new Error("Unexpected value");
      }
    }

    if (this.dataRequest) {
      onStatus(ST_DRQ);
    }
  }

  private void updateWriteProtectStatus() {
    final TRDOSDisk thefloppy = this.currentDisk.get();
    if (thefloppy == null || !thefloppy.isWriteProtect()) {
      offStatus(ST_WRITEP);
    }
    else {
      onStatus(ST_WRITEP);
    }
  }

  private void prepareStatusTypeI(final boolean headLoaded) {
    updateWriteProtectStatus();

    if (headLoaded) {
      onStatus(ST_HEADL);
    }
    else {
      offStatus(ST_HEADL);
    }

    offStatus(ST_SEEKERR | ST_CRCERR);

    if (this.registers[REG_TRACK] == 0) {
      onStatus(ST_TRK00);
    }
    else {
      offStatus(ST_TRK00);
    }
  }

  private void loadSector(final int side, final int track, final int sector) {
    final TRDOSDisk floppy = this.currentDisk.get();
    final Sector thesector;
    if (floppy == null) {
      thesector = null;
    }
    else {
      thesector = floppy.findSector(side, track, sector);
    }
    this.sector = thesector;
  }

  private void provideReadData(final int data) {
    this.registers[REG_DATA_RD] = data & 0xFF;
    this.dataRequest = true;
  }

  private void resetDataRegisters() {
    this.registers[REG_DATA_RD] = 0;
    this.registers[REG_DATA_WR] = 0;
  }

  private void cmdForceInterrupt(final int command, final boolean start) {
    offStatus(ST_BUSY);
  }

  private void cmdRestore(final int command, final boolean start) {
    if (start) {
      this.counter = 0xFF;
      resetDataRegisters();
    }
    else {
      this.counter--;
      if (this.registers[REG_TRACK] > 0) {
        this.registers[REG_TRACK]--;
      }
      else {
        this.registers[REG_TRACK] = 0;
      }
    }

    loadSector(this.side, 0, 0);
    prepareStatusTypeI((command & 0b00001000) != 0);

    if (this.counter <= 0 || this.registers[REG_TRACK] == 0 || this.sector == null) {
      offStatus(ST_BUSY);
    }
  }

  private void cmdSeek(final int command, final boolean start) {
    prepareStatusTypeI((command & 0b00001000) != 0);

    final TRDOSDisk thedisk = this.currentDisk.get();

    this.sector = thedisk == null ? null : thedisk.findFirstSector(this.side, this.registers[REG_DATA_WR]);
    if (this.sector == null) {
      onStatus(ST_SEEKERR);
      resetDataRegisters();
    }
    else {
      this.registers[REG_TRACK] = this.sector.getTrack() & 0xFF;
      this.registers[REG_DATA_RD] = this.registers[REG_TRACK];
    }
    offStatus(ST_BUSY);
  }

  private void cmdReadAddress(final int command, final boolean start) {
    if (start) {
      this.counter = 6;
      this.dataRequest = false;
    }

    offStatus(ST_WRITEP | ST_CRCERR | ST_LOST | ST_RECORDT);

    if (this.counter <= 0 || this.sector == null) {
      onStatus(ST_SEEKERR);
      offStatus(ST_BUSY);
      resetDataRegisters();
    }
    else {
      if (!this.dataRequest) {
        this.counter--;
      }
      if (this.sector == null) {
        offStatus(ST_BUSY);
      }
      else {
        switch (this.counter--) {
          case 5: {// track
            provideReadData(this.sector.getTrack());
          }
          break;
          case 4: {// side
            provideReadData(this.sector.getSide());
          }
          break;
          case 3: {// sector
            provideReadData(this.sector.getSector() + 1);
          }
          break;
          case 2: {// length
            final int sectorLen = this.sector.size();
            if (sectorLen <= 128) {
              provideReadData(0);
            }
            else if (sectorLen <= 256) {
              provideReadData(1);
            }
            else if (sectorLen <= 512) {
              provideReadData(2);
            }
            else {
              provideReadData(3);
            }
          }
          break;
          case 1: {// crc1
            provideReadData(this.sector.getCrc() >> 8);
          }
          break;
          case 0: {// crc2
            provideReadData(this.sector.getCrc() & 0xFF);
            offStatus(ST_BUSY);
          }
          break;
          default: {
            resetDataRegisters();
            offStatus(ST_BUSY);
          }
          break;
        }
      }
    }
  }

  private void cmdStep(final int command, final boolean start) {
    if (this.outwardStepDirection) {
      this.registers[REG_TRACK] = (this.registers[REG_TRACK] + 1) & 0xFF;
    }
    else {
      this.registers[REG_TRACK] = (this.registers[REG_TRACK] - 1) & 0xFF;
    }
    prepareStatusTypeI((command & 0b00001000) != 0);
    provideReadData(0);
  }

  private void cmdStepIn(final int command, final boolean start) {
    this.outwardStepDirection = false;
    cmdStep(command, start);
  }

  private void cmdStepOut(final int command, final boolean start) {
    this.outwardStepDirection = true;
    cmdStep(command, start);
  }

  private void cmdReadSector(final int command, final boolean start) {
    final boolean multiply = (command & 0b00010000) != 0;

    if (start) {
      this.counter = 0;
    }

    loadSector(this.side, this.registers[REG_TRACK], this.registers[REG_SECTOR]);
    offStatus(ST_WRITEP | ST_WRFAULT | ST_CRCERR | ST_SEEKERR | ST_LOST);

    if (this.sector != null) {
      if (!this.dataRequest) {
        boolean process = true;
        if (this.counter >= this.sector.size()) {
          if (multiply){
            if (this.sector.isLastOnTrack()){
              offStatus(ST_BUSY);
              process = false;
            }else{
              this.registers[REG_SECTOR]++;
              loadSector(this.side, this.registers[REG_TRACK], this.registers[REG_SECTOR]);
              if (this.sector == null){
                resetDataRegisters();
                offStatus(ST_BUSY);
                onStatus(ST_SEEKERR);
                process = false;
              }
            }
          }else{
            process = false;
            offStatus(ST_BUSY);
          }
        }
       
        
        if (process){
          final int data = this.sector.readByte(this.counter++);
          if (data < 0) {
            onStatus(ST_LOST | ST_NOTFOUND);
            offStatus(ST_DRQ | ST_BUSY);
            resetDataRegisters();
          }
          else {
            provideReadData(data);
            onStatus(ST_DRQ);
          }
        }
      }
    }
    else {
      resetDataRegisters();
      offStatus(ST_BUSY);
      onStatus(ST_SEEKERR);
    }
  }

  private void cmdWriteSector(final int command, final boolean start) {
    final boolean multiply = (command & 0b00010000) != 0;

    if (start) {
      this.counter = 0;
    }

    loadSector(this.side, this.registers[REG_TRACK], this.registers[REG_SECTOR]);
    offStatus(ST_WRITEP | ST_WRFAULT | ST_CRCERR | ST_SEEKERR | ST_LOST);

    if (this.sector != null) {
      if (this.sector.isWriteProtect()){
        onStatus(ST_WRITEP | ST_WRFAULT);
        offStatus(ST_BUSY);
      }else
      if (!this.dataRequest) {
        boolean process = true;
        if (this.counter >= this.sector.size()) {
          if (multiply) {
            if (this.sector.isLastOnTrack()) {
              offStatus(ST_BUSY);
              process = false;
            }
            else {
              this.registers[REG_SECTOR]++;
              loadSector(this.side, this.registers[REG_TRACK], this.registers[REG_SECTOR]);
              if (this.sector == null) {
                resetDataRegisters();
                offStatus(ST_BUSY);
                onStatus(ST_SEEKERR);
                process = false;
              }
            }
          }
          else {
            process = false;
            offStatus(ST_BUSY);
          }
        }

        if (process) {
          final boolean saved = this.sector.writeByte(this.counter++, registers[REG_DATA_WR]);
          if (!saved) {
            onStatus(ST_LOST | ST_NOTFOUND);
            offStatus(ST_DRQ | ST_BUSY);
            resetDataRegisters();
          }
          else {
            onStatus(ST_DRQ);
          }
        }
      }
    }
    else {
      resetDataRegisters();
      offStatus(ST_BUSY);
      onStatus(ST_SEEKERR);
    }
  }

  private void cmdReadTrack(final int command, final boolean start) {
    throw new Error("Unsupported yet");
  }

  private void cmdWriteTrack(final int command, final boolean start) {
    throw new Error("Unsupported yet");
  }

}
