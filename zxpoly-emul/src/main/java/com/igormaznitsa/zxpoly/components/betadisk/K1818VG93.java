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
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public final class K1818VG93 {

  private static final boolean TRACE = false;
  private static final boolean TRACE_RW_RD_BYTES = false;

  private final long CYCLE_NANOSECOND = 286L;

  private final long CYCLES_FOR_BUFFER_VALID = 120L; // number of cycles to read-write byte to-from disk
  private final long CYCLES_FOR_NEXT_TRACK = 15000000L / CYCLE_NANOSECOND; // number of cycles to move head to next track
  private final long CYCLES_SECTOR_POSITION = CYCLES_FOR_BUFFER_VALID * 134L;

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

  public static final int STAT_BUSY = 0x01;
  public static final int STAT_INDEX = 0x02;
  public static final int STAT_DRQ = 0x02;
  public static final int STAT_TRK00_OR_LOST = 0x04;
  public static final int STAT_CRCERR = 0x08;
  public static final int STAT_NOTFOUND = 0x10;
  public static final int STAT_RECORDT = 0x20;
  public static final int STAT_HEADL = 0x20;
  public static final int STAT_WRFAULT = 0x20;
  public static final int STAT_WRITEPROTECT = 0x40;
  public static final int ST_NOTREADY = 0x80;

  private TRDOSDisk.Sector sector;
  private final int[] registers = new int[6];
  private int counter;
  private int extraCounter;
  private boolean flagWaitDataRd;
  private boolean flagWaitDataWr;
  private boolean resetIn;
  private boolean firstCommandStep;
  private int side;
  private boolean outwardStepDirection;
  private boolean indexHoleMarker;
  private boolean mfmModulation;

  private long sectorPositioningCycles;
  private long operationTimeOutCycles;
  private volatile long lastBusyOnTime;

  private final AtomicReference<TRDOSDisk> trdosDisk = new AtomicReference<>();

  private final Logger logger;

  public K1818VG93(final Logger logger) {
    this.logger = logger;
    reset();
  }

  public void reset() {
    if (TRACE) {
      logger.info("RESET");
    }

    Arrays.fill(registers, 0);
    this.flagWaitDataRd = false;
    this.flagWaitDataWr = false;
    this.resetIn = false;
    this.firstCommandStep = false;
    this.counter = 0;
  }

  public void activateDisk(final int index, final TRDOSDisk disk) {
    if (TRACE) {
      logger.info("ACTIVATE DISK AT DRIVE '" + (char) ('A' + index) + "': " + (disk == null ? "<NO DISK>" : disk));
    }

    this.trdosDisk.set(disk);
    if (disk == null) {
      this.sector = null;
    }
    else {
      this.sector = disk.findFirstSector(this.side, this.registers[REG_TRACK]);
    }
  }

  public TRDOSDisk getDisk() {
    return this.trdosDisk.get();
  }

  private void resetStatus(final boolean preventDataLostFlag) {
    registers[REG_STATUS] = preventDataLostFlag ? registers[REG_STATUS] & STAT_TRK00_OR_LOST : 0;
  }

  private void onStatus(final int flags) {
    if ((flags & STAT_BUSY) != 0) {
      this.lastBusyOnTime = System.currentTimeMillis();
    }
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
    if (TRACE) {
      logger.info("SET SIDE : " + side);
    }
    this.side = side;
  }

  public int getSide() {
    return this.side;
  }

  public void setMFMModulation(final boolean flag) {
    if (TRACE) {
      logger.info("SET MFM : " + flag);
    }
    this.mfmModulation = flag;
  }

  public boolean isMFMModulation() {
    return this.mfmModulation;
  }

  public int read(final int addr) {
    switch (addr & 0x03) {
      case ADDR_COMMAND_STATE: {
        this.indexHoleMarker = !this.indexHoleMarker;
        switch (registers[REG_COMMAND] >>> 4) {
          case 0b0000:// Auxuliary commands which shows Index marker
          case 0b1101:
          case 0b0100:
          case 0b0101:
          case 0b0110:
          case 0b0111:
          case 0b0010:
          case 0b0011:
          case 0b0001: {
            // set TR00 status
            if (this.registers[REG_TRACK] == 0) {
              onStatus(STAT_TRK00_OR_LOST);
            }
            else {
              offStatus(STAT_TRK00_OR_LOST);
            }
            // change index bit in status
            if (this.trdosDisk.get() == null) {
              onStatus(STAT_INDEX);
            }
            else {
              if (this.indexHoleMarker) {
                onStatus(STAT_INDEX);
              }
              else {
                offStatus(STAT_INDEX);
              }
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
        if (this.flagWaitDataRd) {
          this.flagWaitDataRd = false;
          offStatus(STAT_DRQ);
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
        if (isStatus(STAT_BUSY)) {
          if ((normValue >>> 4) == 0b1101) {
            this.registers[REG_COMMAND] = normValue;
            this.firstCommandStep = true;
            this.flagWaitDataRd = false;
            this.flagWaitDataWr = false;
            onStatus(STAT_BUSY);
          }
        }
        else {
          switch (normValue >>> 4) {
            case 0b1000:
            case 0b1001:
            case 0b1010:
            case 0b1011:
            case 0b1100:
            case 0b1110:
            case 0b1111: {
              resetStatus(false);
            }
            break;
          }
          this.registers[REG_COMMAND] = normValue;
          this.firstCommandStep = true;
          this.flagWaitDataRd = false;
          this.flagWaitDataWr = false;
          onStatus(STAT_BUSY);
        }
      }
      break;
      case ADDR_TRACK: { // track
        if (!isStatus(STAT_BUSY)) {
          registers[REG_TRACK] = normValue;
        }
      }
      break;
      case ADDR_SECTOR: { // sector
        if (!isStatus(STAT_BUSY)) {
          registers[REG_SECTOR] = normValue;
        }
      }
      break;
      case ADDR_DATA: { // data
        if (this.flagWaitDataWr) {
          this.flagWaitDataWr = false;
          offStatus(STAT_DRQ);
        }
        registers[REG_DATA_WR] = normValue;
      }
      break;
      default:
        throw new IllegalArgumentException("Unexpected value");
    }
  }

  public void step(final long mcycleCounter) {
    final TRDOSDisk thefloppy = this.trdosDisk.get();
    if (thefloppy == null) {
      onStatus(ST_NOTREADY);
    }
    else {
      offStatus(ST_NOTREADY);
    }

    final int command = this.registers[REG_COMMAND];
    if (isStatus(STAT_BUSY)) {
      final boolean first = this.firstCommandStep;
      this.firstCommandStep = false;
      switch (command >>> 4) {
        case 0b0000:
          cmdRestore(mcycleCounter, command, first);
          break;
        case 0b0001:
          cmdSeek(mcycleCounter, command, first);
          break;
        case 0b0010:
        case 0b0011:
          cmdStep(mcycleCounter, command, first);
          break;
        case 0b0100:
        case 0b0101:
          cmdStepIn(mcycleCounter, command, first);
          break;
        case 0b0110:
        case 0b0111:
          cmdStepOut(mcycleCounter, command, first);
          break;
        case 0b1000:
        case 0b1001:
          cmdReadSector(mcycleCounter, command, first);
          break;
        case 0b1010:
        case 0b1011:
          cmdWriteSector(mcycleCounter, command, first);
          break;
        case 0b1100:
          cmdReadAddress(mcycleCounter, command, first);
          break;
        case 0b1101:
          cmdForceInterrupt(mcycleCounter, command, first);
          break;
        case 0b1110:
          cmdReadTrack(mcycleCounter, command, first);
          break;
        case 0b1111:
          cmdWriteTrack(mcycleCounter, command, first);
          break;
        default:
          throw new Error("Unexpected value");
      }
    }
  }

  private void loadSector(final int side, final int track, final int sector) {
    final TRDOSDisk floppy = this.trdosDisk.get();
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
    this.flagWaitDataRd = true;
  }

  private void resetDataRegisters() {
    this.registers[REG_DATA_RD] = 0;
    this.registers[REG_DATA_WR] = 0;
    this.flagWaitDataRd = false;
    this.flagWaitDataWr = false;
  }

  private void cmdForceInterrupt(final long mcycles, final int command, final boolean start) {
    if (TRACE) {
      logger.info("cmd FORCE INTERRUPT");
    }

    final TRDOSDisk thedisk = this.trdosDisk.get();
    resetStatus(false);

    if (thedisk == null) {
      onStatus(ST_NOTREADY);
    }
    else {
      this.sector = thedisk.findFirstSector(this.side, this.registers[REG_TRACK]);
      if (this.sector == null) {
        onStatus(STAT_NOTFOUND);
      }
      else {
        if (!this.sector.isCrcOk()) {
          onStatus(STAT_CRCERR);
        }
      }
    }
  }

  private void cmdRestore(final long mcycles, final int command, final boolean start) {
    if (start && TRACE) {
      logger.info("cmd RESTORE");
    }

    final TRDOSDisk thedisk = this.trdosDisk.get();

    resetStatus(false);

    if (start) {
      this.counter = 0;
    }

    if (thedisk == null) {
      onStatus(ST_NOTREADY);
    }
    else {
      if ((command & 0b00001000) != 0) {
        onStatus(STAT_HEADL);
      }
      if (thedisk.isWriteProtect()) {
        onStatus(STAT_WRITEPROTECT);
      }

      if (counter < 0xFF && this.registers[REG_TRACK] > 0) {
        this.registers[REG_TRACK]--;
        this.sector = thedisk.findFirstSector(this.side, this.registers[REG_TRACK]);
        if (this.sector == null) {
          onStatus(STAT_NOTFOUND);
        }
        else {
          if (!this.sector.isCrcOk()) {
            onStatus(STAT_CRCERR);
          }
          onStatus(STAT_BUSY);
        }
      }
      else {
        if (this.registers[REG_TRACK] != 0) {
          this.sector = thedisk.findFirstSector(this.side, this.registers[REG_TRACK]);
          onStatus(STAT_NOTFOUND);
        }
      }

      if (this.sector != null && !this.sector.isCrcOk()) {
        onStatus(STAT_CRCERR);
      }
    }
  }

  private void cmdSeek(final long mcycles, final int command, final boolean start) {
    if (TRACE && start) {
      logger.info("cmd SEEK (side=" + this.side + ", track=" + this.registers[REG_DATA_WR] + ')');
    }

    resetStatus(false);

    final TRDOSDisk thedisk = this.trdosDisk.get();

    if ((command & 0b00001000) != 0) {
      onStatus(STAT_HEADL);
    }

    if (thedisk == null) {
      onStatus(ST_NOTREADY);
    }
    else {
      if (start) {
        this.operationTimeOutCycles = mcycles + CYCLES_FOR_NEXT_TRACK;
      }

      if (thedisk.isWriteProtect()) {
        onStatus(STAT_WRITEPROTECT);
      }

      if (this.registers[REG_TRACK] != this.registers[REG_DATA_WR]) {
        if (mcycles > this.operationTimeOutCycles) {
          if (this.registers[REG_TRACK] < this.registers[REG_DATA_WR]) {
            this.registers[REG_TRACK]++;
          }
          else if (this.registers[REG_TRACK] > this.registers[REG_DATA_WR]) {
            this.registers[REG_TRACK]--;
          }
          this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_NEXT_TRACK);
        }
      }

      boolean end = this.registers[REG_TRACK] == this.registers[REG_DATA_WR];

      this.sector = thedisk.findRandomSector(this.side, this.registers[REG_TRACK]);

      if (this.sector == null) {
        onStatus(STAT_NOTFOUND);
        end = true;
      }
      else {
        if (!this.sector.isCrcOk()) {
          onStatus(STAT_CRCERR);
        }
      }

      if (!end) {
        onStatus(STAT_BUSY);
      }
    }
  }

  private void cmdReadAddress(final long mcycles, final int command, final boolean start) {
    if (start && TRACE) {
      logger.info("start RDADDR (side=" + this.side + ", track=" + this.registers[REG_TRACK] + ')');
    }

    resetStatus(true);

    final TRDOSDisk thedisk = this.trdosDisk.get();

    if (start) {
      // turn sector
      if (this.sector != null) {
        this.sector = thedisk.findSectorAfter(this.sector);
      }
      if (this.sector == null) {
        this.sector = thedisk.findFirstSector(this.side, this.registers[REG_TRACK]);
      }

      this.counter = 6;
      this.sectorPositioningCycles = Math.abs(mcycles + CYCLES_SECTOR_POSITION);
    }

    if (thedisk == null) {
      onStatus(ST_NOTREADY);
    }
    else {
      if (this.sector == null) {
        this.sector = thedisk.findFirstSector(this.side, this.registers[REG_TRACK]);
      }

      if (this.sector == null) {
        onStatus(STAT_NOTFOUND);
        resetDataRegisters();
      }
      else {
        if (mcycles < this.sectorPositioningCycles) {
          onStatus(STAT_BUSY);
        }
        else {
          if (this.sectorPositioningCycles >= 0L) {
            this.sectorPositioningCycles = -1L;
            this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);
          }

          if (!this.sector.isCrcOk()) {
            onStatus(STAT_CRCERR);
          }

          if (!this.flagWaitDataRd) {
            this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);
            switch (--this.counter) {
              case 5: {// track
                if (TRACE) {
                  logger.info("<RDADDR #5 track=" + this.sector.getTrackNumber());
                }
                provideReadData(this.sector.getTrackNumber());
                onStatus(STAT_BUSY);
              }
              break;
              case 4: {// side
                if (TRACE) {
                  logger.info("<RDADDR #4 side=" + this.sector.getSide());
                }
                provideReadData(this.sector.getSide());
                onStatus(STAT_BUSY);
              }
              break;
              case 3: {// sector
                if (TRACE) {
                  logger.info("<RDADDR #3 sector=" + this.sector.getSectorId());
                }
                provideReadData(this.sector.getSectorId());
                onStatus(STAT_BUSY);
              }
              break;
              case 2: {// length
                final int type;
                final int sectorLen = this.sector.size();
                if (sectorLen <= 128) {
                  type = 0;
                }
                else if (sectorLen <= 256) {
                  type = 1;
                }
                else if (sectorLen <= 512) {
                  type = 2;
                }
                else {
                  type = 3;
                }

                provideReadData(type);
                if (TRACE) {
                  logger.info("<RDADDR #2 lengthType=" + type);
                }
                onStatus(STAT_BUSY);
              }
              break;
              case 1: {// crc1
                final int crc = this.sector.getCrc() >>> 8;
                if (TRACE) {
                  logger.info("<RDADDR #1 crcHigh=" + crc);
                }
                provideReadData(crc);
                onStatus(STAT_BUSY);
              }
              break;
              case 0: {// crc2
                final int crc = this.sector.getCrc() & 0xFF;
                if (TRACE) {
                  logger.info("<RDADDR #0 crcLow=" + crc);
                }
                provideReadData(crc);
//              onStatus(STAT_BUSY);
              }
              break;
              default: {
                if (TRACE) {
                  logger.info("<RDADDR ### UNEXPECTED");
                }
              }
            }
          }
          else {
            if (mcycles > this.operationTimeOutCycles) {
              if (TRACE) {
                logger.info("cmd RDADDR has lost data");
              }
              onStatus(STAT_TRK00_OR_LOST);
            }
            onStatus(STAT_DRQ);
            onStatus(STAT_BUSY);
          }
        }
      }
    }
  }

  private void cmdStep(final long mcycles, final int command, final boolean start) {
    if (TRACE && start) {
      logger.info("cmd STEP (outward=" + this.outwardStepDirection + ", side=" + this.side + ", track=" + this.registers[REG_TRACK] + ')');
    }

    final TRDOSDisk thedisk = this.trdosDisk.get();

    resetStatus(false);

    if (thedisk == null) {
      resetDataRegisters();
      onStatus(ST_NOTREADY);
    }
    else {
      if (start) {
        this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);
      }

      if ((command & 0b00001000) != 0) {
        onStatus(STAT_HEADL);
      }
      if (thedisk.isWriteProtect()) {
        onStatus(STAT_WRITEPROTECT);
      }
      if (!this.sector.isCrcOk()) {
        onStatus(STAT_CRCERR);
      }

      if (mcycles < this.operationTimeOutCycles) {
        onStatus(STAT_BUSY);
      }
      else {
        if (this.outwardStepDirection) {
          this.registers[REG_TRACK] = (this.registers[REG_TRACK] + 1) & 0xFF;
        }
        else {
          this.registers[REG_TRACK] = (this.registers[REG_TRACK] - 1) & 0xFF;
        }
        this.sector = thedisk.findRandomSector(this.side, this.registers[REG_TRACK]);
        if (this.sector == null) {
          onStatus(STAT_NOTFOUND);
        }
        else {
          if (!this.sector.isCrcOk()) {
            onStatus(STAT_CRCERR);
          }
        }
      }
    }
  }

  private void cmdStepIn(final long mcycles, final int command, final boolean start) {
    if (TRACE) {
      logger.info("cmd STEPIN (side=" + this.side + ", track=" + this.registers[REG_TRACK] + ')');
    }
    this.outwardStepDirection = false;
    cmdStep(mcycles, command, start);
  }

  private void cmdStepOut(final long mcycles, final int command, final boolean start) {
    if (TRACE) {
      logger.info("cmd STEPOUT (side=" + this.side + ", track=" + this.registers[REG_TRACK] + ')');
    }
    this.outwardStepDirection = true;
    cmdStep(mcycles, command, start);
  }

  private void cmdReadSector(final long mcycles, final int command, final boolean start) {
    final boolean multiOp = (command & 0x10) != 0;
    final int softSide = (command >>> 3) & 1;
    final boolean doCheckSide = (command & 1) != 0;

    if (start && TRACE) {
      logger.info("cmd RDSEC (multi=" + multiOp + ", side=" + this.side + ", track=" + this.registers[REG_TRACK] + ", sector=" + this.registers[REG_SECTOR] + ')');
    }

    resetStatus(true);

    final TRDOSDisk thedisk = this.trdosDisk.get();
    if (thedisk == null) {
      onStatus(ST_NOTREADY);
    }

    loadSector(this.side, this.registers[REG_TRACK], this.registers[REG_SECTOR]);

    if (this.sector != null && doCheckSide && this.sector.getSide() != softSide) {
      this.sector = null;
    }

    if (this.sector == null) {
      onStatus(STAT_NOTFOUND);
    }
    else {
      if (start) {
        this.counter = 0;
        this.sectorPositioningCycles = Math.abs(mcycles + CYCLES_SECTOR_POSITION);
      }

      if (mcycles < this.sectorPositioningCycles) {
        onStatus(STAT_BUSY);
      }
      else {
        if (this.sectorPositioningCycles >= 0L) {
          // start reading of first byte
          this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);
          this.sectorPositioningCycles = -1L;
        }

        if (!this.flagWaitDataRd) {
          if (this.counter >= this.sector.size()) {
            this.registers[REG_SECTOR]++;
            this.sectorPositioningCycles = Math.abs(mcycles + CYCLES_SECTOR_POSITION);
            if (multiOp) {
              if (!this.sector.isLastOnTrack()) {
                this.counter = 0;
                onStatus(STAT_BUSY);
              }
              else {
                if (TRACE) {
                  logger.info("cmd RDSEC completed");
                }
              }
            }
            else {
              if (TRACE) {
                logger.info("cmd RDSEC completed");
              }
            }
          }
          else {
            final int data = this.sector.readByte(this.counter++);
            if (TRACE && TRACE_RW_RD_BYTES) {
              logger.info("<RDSEC #" + Integer.toHexString(this.counter - 1).toUpperCase(Locale.ENGLISH) + "=#" + Integer.toHexString(data).toUpperCase(Locale.ENGLISH));
            }

            if (data < 0) {
              if (TRACE) {
                logger.info("cmd RDSEC has lost data");
              }
              onStatus(STAT_TRK00_OR_LOST);
            }
            else {
              if (mcycles > this.operationTimeOutCycles) {
                if (TRACE) {
                  logger.info("cmd RDSEC has lost data");
                }
                onStatus(STAT_TRK00_OR_LOST);
              }
              this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);
              provideReadData(data);
              onStatus(STAT_DRQ);
              onStatus(STAT_BUSY);
            }
          }
        }
        else {
          if (this.sector == null) {
            resetDataRegisters();
            onStatus(STAT_NOTFOUND);
          }
          else {
            if (mcycles > this.operationTimeOutCycles) {
              if (TRACE) {
                logger.info("cmd RDSEC has lost data");
              }
              onStatus(STAT_TRK00_OR_LOST);
            }
            onStatus(STAT_DRQ);
            onStatus(STAT_BUSY);
          }
        }
      }
    }
  }

  private void cmdWriteSector(final long mcycles, final int command, final boolean start) {
    final boolean multiOp = (command & 0x10) != 0;
    final int softSide = (command >>> 3) & 1;
    final boolean doCheckSide = (command & 1) != 0;

    if (start && TRACE) {
      logger.info("cmd WRSEC (multi=" + multiOp + ", side=" + this.side + ", track=" + this.registers[REG_TRACK] + ", sector=" + this.registers[REG_SECTOR] + ')');
    }

    resetStatus(true);

    final TRDOSDisk thedisk = this.trdosDisk.get();
    if (thedisk == null) {
      onStatus(ST_NOTREADY);
    }
    else {
      if (thedisk.isWriteProtect()) {
        onStatus(STAT_WRITEPROTECT);
      }
    }

    loadSector(this.side, this.registers[REG_TRACK], this.registers[REG_SECTOR]);

    if (this.sector != null && doCheckSide && this.sector.getSide() != softSide) {
      this.sector = null;
    }

    if (this.sector == null) {
      onStatus(STAT_NOTFOUND);
    }
    else {
      if (start) {
        this.counter = 0;
        this.flagWaitDataWr = true;
        this.sectorPositioningCycles = Math.abs(mcycles + CYCLES_SECTOR_POSITION);
      }

      if (mcycles < this.sectorPositioningCycles) {
        onStatus(STAT_BUSY);
      }
      else {
        if (this.sectorPositioningCycles >= 0L) {
          // start reading of first byte
          this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);
          this.sectorPositioningCycles = -1L;
        }

        if (!this.flagWaitDataWr) {
          this.flagWaitDataWr = true;
          if (this.counter >= this.sector.size()) {
            this.registers[REG_SECTOR]++;
            this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);
            if (multiOp) {
              if (!this.sector.isLastOnTrack()) {
                this.counter = 0;
                onStatus(STAT_BUSY);
              }
              else {
                if (TRACE) {
                  logger.info("cmd WRSEC lost data");
                }
              }
            }
          }
          else {
            if (TRACE && TRACE_RW_RD_BYTES) {
              logger.info(">WRSEC #" + Integer.toHexString(this.counter).toUpperCase(Locale.ENGLISH) + "=#" + Integer.toHexString(this.registers[REG_DATA_WR] & 0xFF).toUpperCase(Locale.ENGLISH));
            }
            if (mcycles > this.operationTimeOutCycles) {
              if (TRACE) {
                logger.info("cmd WRSEC lost data");
              }
              resetDataRegisters();
              onStatus(STAT_NOTFOUND);
            }
            else {
              this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);
              if (!this.sector.writeByte(this.counter++, this.registers[REG_DATA_WR])) {
                onStatus(STAT_WRFAULT);
              }
              else {
                if (this.counter < this.sector.size()) {
                  onStatus(STAT_DRQ);
                  onStatus(STAT_BUSY);
                }
                else {
                  if (TRACE) {
                    logger.info("cmd WRSEC completed");
                  }
                }
              }
            }
          }
        }
        else {
          if (this.sector == null) {
            resetDataRegisters();
            onStatus(STAT_NOTFOUND);
          }
          else {
            if (mcycles > this.operationTimeOutCycles) {
              if (TRACE) {
                logger.info("cmd WRSEC lost data");
              }
              onStatus(STAT_TRK00_OR_LOST);
            }
            onStatus(STAT_DRQ);
            onStatus(STAT_BUSY);
          }
        }
      }
    }
  }

  private void cmdReadTrack(final long mcycles, final int command, final boolean start) {
    if (start && TRACE) {
      logger.info("cmd READTRACK (side=" + this.side + ", track=" + this.registers[REG_TRACK] + ')');
    }

    resetStatus(true);

    final TRDOSDisk thedisk = this.trdosDisk.get();
    if (thedisk == null) {
      onStatus(ST_NOTREADY);
    }
    else {
      if (start) {
        logger.warning("Reading track (fake implementration) [" + this.side + ':' + this.registers[REG_TRACK] + ']');
        this.counter = 0;
        this.sector = thedisk.findFirstSector(this.side, this.registers[REG_TRACK]);
        this.extraCounter = 6250;
        this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);
      }
    }

    if (this.sector == null) {
      onStatus(STAT_TRK00_OR_LOST);
    }
    else {
      if (!this.flagWaitDataRd) {
        this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);

        if (this.extraCounter > 0) {
          if (this.counter >= this.sector.size()) {
            this.counter = 0;
            if (!this.sector.isLastOnTrack()) {
              this.sector = thedisk.findSectorAfter(this.sector);
              if (this.sector == null) {
                onStatus(STAT_TRK00_OR_LOST);
              }
              else {
                onStatus(STAT_BUSY);
              }
            }
          }
          else {
            final int data = this.sector.readByte(this.counter++);
            if (TRACE && TRACE_RW_RD_BYTES) {
              logger.info("<RDTRACK #" + Integer.toHexString(this.counter - 1).toUpperCase(Locale.ENGLISH) + "=#" + Integer.toHexString(data).toUpperCase(Locale.ENGLISH));
            }

            this.extraCounter--;
            if (data < 0) {
              onStatus(STAT_TRK00_OR_LOST);
            }
            else {
              provideReadData(data);
              onStatus(STAT_BUSY);
            }
          }
        }
        else {
          if (TRACE) {
            logger.info("cmd RDSEC completed");
          }
        }
      }
      else {
        if (mcycles > this.operationTimeOutCycles) {
          if (TRACE) {
            logger.info("cmd RDSEC lost data");
          }
          onStatus(STAT_TRK00_OR_LOST);
        }
        onStatus(STAT_DRQ);
        onStatus(STAT_BUSY);
      }
    }
  }

  private void cmdWriteTrack(final long mcycles, final int command, final boolean start) {
    if (start && TRACE) {
      logger.info("cmd WRTRACK (side=" + this.side + ", track=" + this.registers[REG_TRACK] + ')');
    }

    resetStatus(true);

    final TRDOSDisk thedisk = this.trdosDisk.get();
    if (thedisk == null) {
      onStatus(ST_NOTREADY);
    }
    else {
      if (start) {
        logger.warning("Writing whole track (fake implementration) [" + this.side + ':' + this.registers[REG_TRACK] + ']');
        this.counter = 0;
        this.sector = thedisk.findFirstSector(this.side, this.registers[REG_TRACK]);
        this.extraCounter = 6250;
        this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);
        this.flagWaitDataWr = true;
      }
    }

    if (this.sector == null) {
      onStatus(STAT_TRK00_OR_LOST);
    }
    else {
      if (!this.flagWaitDataWr) {
        this.operationTimeOutCycles = Math.abs(mcycles + CYCLES_FOR_BUFFER_VALID);
        this.flagWaitDataWr = true;
        if (this.extraCounter > 0) {
          if (this.counter >= this.sector.size()) {
            this.counter = 0;
            if (!this.sector.isLastOnTrack()) {
              this.sector = thedisk.findSectorAfter(this.sector);
              if (this.sector == null) {
                onStatus(STAT_TRK00_OR_LOST);
              }
              else {
                onStatus(STAT_BUSY);
              }
            }
          }
          else {
            if (TRACE && TRACE_RW_RD_BYTES) {
              logger.info(">WRTRACK #" + Integer.toHexString(this.counter).toUpperCase(Locale.ENGLISH) + "=#" + Integer.toHexString(this.registers[REG_DATA_WR] & 0xFF).toUpperCase(Locale.ENGLISH));
            }
            if (!this.sector.writeByte(this.counter++, this.registers[REG_DATA_WR])) {
              onStatus(STAT_WRFAULT);
            }
            else {
              this.extraCounter--;
            }
          }
        }
      }
      else {
        if (mcycles > this.operationTimeOutCycles) {
          onStatus(STAT_TRK00_OR_LOST);
        }
        onStatus(STAT_DRQ);
        onStatus(STAT_BUSY);
      }
    }
  }

  public boolean isMotorOn() {
    return (System.currentTimeMillis() - this.lastBusyOnTime) < 200L;
  }

}
