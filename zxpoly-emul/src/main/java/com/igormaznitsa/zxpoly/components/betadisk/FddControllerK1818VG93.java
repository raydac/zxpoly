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

package com.igormaznitsa.zxpoly.components.betadisk;

import com.igormaznitsa.zxpoly.components.betadisk.TrDosDisk.Sector;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.igormaznitsa.zxpoly.components.Motherboard.CPU_FREQ;
import static java.time.Duration.ofMillis;
import static java.time.Duration.ofSeconds;

public final class FddControllerK1818VG93 {

  static final int ADDR_COMMAND_STATE = 0;
  static final int ADDR_TRACK = 1;
  static final int ADDR_SECTOR = 2;
  static final int ADDR_DATA = 3;
  static final int STAT_BUSY = 0x01;
  static final int STAT_DRQ = 0x02;
  private static final long DELAY_FDD_MOTOR_ON_MS = 2000L;
  private static final int STAT_INDEX = 0x02;
  private static final int STAT_TRK00_OR_LOST = 0x04;
  private static final int STAT_CRCERR = 0x08;
  private static final int STAT_NOTFOUND = 0x10;
  private static final int STAT_HEADL = 0x20;
  private static final int STAT_WRFAULT = 0x20;
  private static final int STAT_WRITEPROTECT = 0x40;
  private static final int ST_NOTREADY = 0x80;
  private static final long TSTATE_NANOSECOND_LENGTH =
          Math.round((double) ofSeconds(1).toNanos() / (double) CPU_FREQ);
  private static final long TSTATES_HEAD_TO_NEXT_TRACK =
          ofMillis(12).toNanos() / TSTATE_NANOSECOND_LENGTH;
  private static final long TSTATES_PER_BUFFER_VALID = 128L;
  private static final long TSTATES_SECTOR_POSITIONING = TSTATES_PER_BUFFER_VALID;
  private static final int REG_COMMAND = 0x00;
  private static final int REG_STATUS = 0x01;
  private static final int REG_TRACK = 0x02;
  private static final int REG_SECTOR = 0x03;
  private static final int REG_DATA_WR = 0x04;
  private static final int REG_DATA_RD = 0x05;
  private static final int COMMAND_FLAG_MULTIPLE_RECORDS = 0b0001_0000;
  private final int[] registers = new int[6];
  private final AtomicReference<TrDosDisk> trdosDisk = new AtomicReference<>();
  private final Logger logger;
  private TrDosDisk.Sector sector;
  private int counter;
  private int extraCounter;
  private boolean flagWaitDataRd;
  private boolean flagWaitDataWr;
  private boolean resetIn;
  private boolean firstCommandStep;
  private int head;
  private boolean outwardStepDirection;
  private boolean indexHoleMarker;
  private boolean mfmModulation;
  private long sectorPositioningCycles;
  private long operationTimeOutCycles;
  private volatile long lastBusyOnTime;
  private Object tempAuxiliaryObject;

  public FddControllerK1818VG93(final Logger logger) {
    this.logger = logger;
    reset();
  }

  public void reset() {
    Arrays.fill(registers, 0);
    this.flagWaitDataRd = false;
    this.flagWaitDataWr = false;
    this.resetIn = false;
    this.firstCommandStep = false;
    this.counter = 0;
  }

  public void activateDisk(final int index, final TrDosDisk disk) {
    this.trdosDisk.set(disk);
    if (disk == null) {
      this.sector = null;
    } else {
      this.sector = disk.findFirstSector(this.head, this.registers[REG_TRACK]);
    }
  }

  private TrDosDisk getDisk() {
    return this.trdosDisk.get();
  }

  private void resetStatus(final boolean preventDataLostFlag) {
    registers[REG_STATUS] = preventDataLostFlag ? registers[REG_STATUS] & STAT_TRK00_OR_LOST : 0;
  }

  private void setInternalFlag(final int flags) {
    if ((flags & STAT_BUSY) != 0) {
      this.lastBusyOnTime = System.currentTimeMillis();
    }
    registers[REG_STATUS] |= flags;
  }

  private void resetInternalFlag(final int flags) {
    registers[REG_STATUS] = (registers[REG_STATUS] & ~flags) & 0xFF;
  }

  private boolean isFlag(final int flags) {
    return (registers[REG_STATUS] & flags) != 0;
  }

  public void setResetIn(final boolean signal) {
    if (!this.resetIn && signal) {
      reset();
    }
    this.resetIn = signal;
  }

  public int getHead() {
    return this.head;
  }

  public void setHead(final int head) {
    this.head = head;
  }

  public boolean isMFMModulation() {
    return this.mfmModulation;
  }

  public void setMFMModulation(final boolean flag) {
    this.mfmModulation = flag;
  }

  public int read(final int addr) {
    switch (addr & 0x03) {
      case ADDR_COMMAND_STATE: {
        this.indexHoleMarker = !this.indexHoleMarker;
        switch (registers[REG_COMMAND] >>> 4) {
          case 0b0000:
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
              setInternalFlag(STAT_TRK00_OR_LOST);
            } else {
              resetInternalFlag(STAT_TRK00_OR_LOST);
            }
            // change index bit in status
            if (this.trdosDisk.get() == null) {
              setInternalFlag(STAT_INDEX);
            } else {
              if (this.indexHoleMarker) {
                setInternalFlag(STAT_INDEX);
              } else {
                resetInternalFlag(STAT_INDEX);
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
          resetInternalFlag(STAT_DRQ);
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
        if (isFlag(STAT_BUSY)) {
          if ((normValue >>> 4) == 0b1101) {
            this.registers[REG_COMMAND] = normValue;
            this.firstCommandStep = true;
            this.flagWaitDataRd = false;
            this.flagWaitDataWr = false;
            setInternalFlag(STAT_BUSY);
          }
        } else {
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
          setInternalFlag(STAT_BUSY);
        }

        if (this.firstCommandStep) {
          logger.log(Level.INFO,
                  "FDD cmd (" + toBinByte(normValue) + "): " + commandAsText(normValue));
        }
      }
      break;
      case ADDR_TRACK: { // track
        if (!isFlag(STAT_BUSY)) {
          registers[REG_TRACK] = normValue;
        }
      }
      break;
      case ADDR_SECTOR: { // sector
        if (!isFlag(STAT_BUSY)) {
          registers[REG_SECTOR] = normValue;
        }
      }
      break;
      case ADDR_DATA: { // data
        if (this.flagWaitDataWr) {
          this.flagWaitDataWr = false;
          resetInternalFlag(STAT_DRQ);
        }
        registers[REG_DATA_WR] = normValue;
      }
      break;
      default:
        throw new IllegalArgumentException("Unexpected value");
    }
  }

  private String toBinByte(int value) {
    final StringBuilder buffer = new StringBuilder(8);
    for (int i = 0; i < 8; i++) {
      buffer.append((value & 0x80) == 0 ? '0' : '1');
      value <<= 1;
    }
    return buffer.toString();
  }

  private String commandAsText(final int command) {
    final int track = this.registers[REG_TRACK];
    final int theSector = this.registers[REG_SECTOR];
    final int theHead = this.head;

    final String addr = track + ":" + theHead + ":" + theSector;

    final int high = command >>> 4;
    switch (high) {
      case 0b0000: {
        return "RESTORE";
      }
      case 0b0001: {
        return "SEEK (track=" + track + ", head=" + this.head + ")";
      }
      case 0b0010:
      case 0b0011: {
        return "STEP";
      }
      case 0b0100:
      case 0b0101: {
        return "STEP IN";
      }
      case 0b0110:
      case 0b0111: {
        return "STEP OUT";
      }
      case 0b1000:
      case 0b1001: {
        return "RD.SECTOR" + ((high & 1) == 0 ? "(S)" : "(M)") + ' ' + addr;
      }
      case 0b1010:
      case 0b1011: {
        return "WR.SECTOR" + ((high & 1) == 0 ? "(S)" : "(M)") + ' ' + addr;
      }
      case 0b1100: {
        return "RD.ADDR";
      }
      case 0b1110: {
        return "RD.TRACK (track=" + track + ")";
      }
      case 0b1111: {
        return "WR.TRACK (track=" + track + ")";
      }
      case 0b1101: {
        return "FRC.INTERRUPT";
      }
      default: {
        return "UNKNOWN " + Integer.toBinaryString(high);
      }
    }
  }

  public void step(final long tstatesCounter) {
    final TrDosDisk thefloppy = this.trdosDisk.get();
    if (thefloppy == null) {
      setInternalFlag(ST_NOTREADY);
    } else {
      resetInternalFlag(ST_NOTREADY);
    }

    final int command = this.registers[REG_COMMAND];
    if (isFlag(STAT_BUSY)) {
      final boolean first = this.firstCommandStep;
      this.firstCommandStep = false;
      switch (command >>> 4) {
        case 0b0000:
          cmdRestore(tstatesCounter, command, first);
          break;
        case 0b0001:
          cmdSeek(tstatesCounter, command, first);
          break;
        case 0b0010:
        case 0b0011:
          cmdStep(tstatesCounter, command, first);
          break;
        case 0b0100:
        case 0b0101:
          cmdStepIn(tstatesCounter, command, first);
          break;
        case 0b0110:
        case 0b0111:
          cmdStepOut(tstatesCounter, command, first);
          break;
        case 0b1000:
        case 0b1001:
          cmdReadSector(tstatesCounter, command, first);
          break;
        case 0b1010:
        case 0b1011:
          cmdWriteSector(tstatesCounter, command, first);
          break;
        case 0b1100:
          cmdReadAddress(tstatesCounter, command, first);
          break;
        case 0b1101:
          cmdForceInterrupt(tstatesCounter, command, first);
          break;
        case 0b1110:
          cmdReadTrack(tstatesCounter, command, first);
          break;
        case 0b1111:
          cmdWriteTrack(tstatesCounter, command, first);
          break;
        default:
          throw new Error("Unexpected value");
      }
    }
  }

  private void loadSector(final int side, final int track, final int sector) {
    final TrDosDisk floppy = this.trdosDisk.get();
    final Sector foundSector;
    if (floppy == null) {
      foundSector = null;
    } else {
      foundSector = floppy.findSector(side, track, sector);
    }
    this.sector = foundSector;
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

  private void cmdForceInterrupt(final long tstatesCounter, final int command,
                                 final boolean start) {
    final TrDosDisk currentDisk = this.trdosDisk.get();
    resetStatus(false);

    if (currentDisk == null) {
      setInternalFlag(ST_NOTREADY);
    } else {
      this.sector = currentDisk.findFirstSector(this.head, this.registers[REG_TRACK]);
      if (this.sector == null) {
        setInternalFlag(STAT_NOTFOUND);
      } else {
        if (!this.sector.isCrcOk()) {
          setInternalFlag(STAT_CRCERR);
        }
      }
    }
  }

  private void cmdRestore(final long tstatesCounter, final int command, final boolean start) {
    final TrDosDisk currentDisk = this.trdosDisk.get();

    resetStatus(false);

    if (start) {
      this.counter = 0;
    }

    if (currentDisk == null) {
      setInternalFlag(ST_NOTREADY);
    } else {
      if ((command & 0b00001000) != 0) {
        setInternalFlag(STAT_HEADL);
      }
      if (currentDisk.isWriteProtect()) {
        setInternalFlag(STAT_WRITEPROTECT);
      }

      if (counter < 0xFF && this.registers[REG_TRACK] > 0) {
        this.registers[REG_TRACK]--;
        this.sector = currentDisk.findFirstSector(this.head, this.registers[REG_TRACK]);
        if (this.sector == null) {
          setInternalFlag(STAT_NOTFOUND);
        } else {
          this.registers[REG_SECTOR] = 1;
          if (!this.sector.isCrcOk()) {
            setInternalFlag(STAT_CRCERR);
          }
          setInternalFlag(STAT_BUSY);
        }
      } else {
        if (this.registers[REG_TRACK] != 0) {
          this.sector = currentDisk.findFirstSector(this.head, this.registers[REG_TRACK]);
          if (this.sector != null)
            this.registers[REG_SECTOR] = 1;
          setInternalFlag(STAT_NOTFOUND);
        }
      }

      if (this.sector != null && !this.sector.isCrcOk()) {
        setInternalFlag(STAT_CRCERR);
      }
    }
  }

  private void cmdSeek(final long tstatesCounter, final int command, final boolean start) {
    resetStatus(false);

    final TrDosDisk currentDisk = this.trdosDisk.get();

    if ((command & 0b00001000) != 0) {
      setInternalFlag(STAT_HEADL);
    }

    if (currentDisk == null) {
      setInternalFlag(ST_NOTREADY);
    } else {
      if (start) {
        this.operationTimeOutCycles = tstatesCounter + TSTATES_HEAD_TO_NEXT_TRACK;
      }

      if (currentDisk.isWriteProtect()) {
        setInternalFlag(STAT_WRITEPROTECT);
      }

      if (this.registers[REG_TRACK] != this.registers[REG_DATA_WR]) {
        if (tstatesCounter >= this.operationTimeOutCycles) {
          if (this.registers[REG_TRACK] < this.registers[REG_DATA_WR]) {
            this.registers[REG_TRACK]++;
          } else {
            this.registers[REG_TRACK]--;
          }
          this.sector = currentDisk.findFirstSector(this.head, this.registers[REG_TRACK]);
          if (this.sector != null) {
            this.registers[REG_SECTOR] = 1;
          }
          logger.info("FDD head moved to track " + this.registers[REG_TRACK] + ", target track is "
                  + this.registers[REG_DATA_WR]);
          this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_HEAD_TO_NEXT_TRACK);
        }
      }

      boolean completed = this.registers[REG_TRACK] == this.registers[REG_DATA_WR];

      this.sector = currentDisk.findRandomSector(this.head, this.registers[REG_TRACK]);

      if (this.sector == null) {
        setInternalFlag(STAT_NOTFOUND);
        completed = true;
      } else {
        if (!this.sector.isCrcOk()) {
          setInternalFlag(STAT_CRCERR);
        }
      }

      if (!completed) {
        setInternalFlag(STAT_BUSY);
      }
    }
  }

  private void cmdReadAddress(final long tstatesCounter, final int command, final boolean start) {
    resetStatus(true);

    final TrDosDisk thedisk = this.trdosDisk.get();

    if (thedisk == null) {
      setInternalFlag(ST_NOTREADY);
    } else {
      if (start) {
        // turn sector
        if (this.sector != null) {
          this.sector = thedisk.findSectorAfter(this.sector);
        }
        if (this.sector == null) {
          this.sector = thedisk.findFirstSector(this.head, this.registers[REG_TRACK]);
        }

        this.counter = 6;
        this.sectorPositioningCycles = Math.abs(tstatesCounter + TSTATES_SECTOR_POSITIONING);

        this.extraCounter = 0;
      }

      if (this.sector == null) {
        this.sector = thedisk.findFirstSector(this.head, this.registers[REG_TRACK]);
      }

      if (this.sector == null) {
        setInternalFlag(STAT_NOTFOUND);
        resetDataRegisters();
      } else {
        if (tstatesCounter < this.sectorPositioningCycles) {
          setInternalFlag(STAT_BUSY);
        } else {
          if (this.sectorPositioningCycles >= 0L) {
            this.sectorPositioningCycles = -1L;
            this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);
          }

          if (!this.sector.isCrcOk()) {
            setInternalFlag(STAT_CRCERR);
          } else {
            if (!this.flagWaitDataRd) {
              this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);
              if (this.counter > 0) {
                switch (this.counter) {
                  case 6: {// track
                    final int trackNum = this.sector.getTrackNumber();
                    provideReadData(trackNum);
                    setInternalFlag(STAT_DRQ);
                    this.extraCounter |= (trackNum << 16);
                  }
                  break;
                  case 5: {// side
                    final int side = this.sector.getSide();
                    provideReadData(side);
                    setInternalFlag(STAT_DRQ);
                    this.extraCounter |= (side << 8);
                  }
                  break;
                  case 4: {// sector
                    final int sectorId = this.sector.getPhysicalIndex();
                    provideReadData(sectorId);
                    setInternalFlag(STAT_DRQ);
                    this.extraCounter |= sectorId;
                  }
                  break;
                  case 3: {// length
                    final int type;
                    final int sectorLen = this.sector.size();
                    if (sectorLen <= 128) {
                      type = 0;
                    } else if (sectorLen <= 256) {
                      type = 1;
                    } else if (sectorLen <= 512) {
                      type = 2;
                    } else {
                      type = 3;
                    }

                    provideReadData(type);
                    setInternalFlag(STAT_DRQ);
                  }
                  break;
                  case 2: {// crc1
                    final int crc = this.sector.getCrc() >>> 8;
                    provideReadData(crc);
                    setInternalFlag(STAT_DRQ);
                  }
                  break;
                  case 1: {// crc2
                    final int crc = this.sector.getCrc() & 0xFF;
                    provideReadData(crc);
                    setInternalFlag(STAT_DRQ);
                  }
                  break;
                  default:
                    throw new Error("Unexpected counter state");
                }
                this.counter--;
                if (this.counter > 0) {
                  setInternalFlag(STAT_BUSY);
                } else {
                  final int track = (this.extraCounter >> 16) & 0xFF;
                  final int side = (this.extraCounter >> 8) & 0xFF;
                  final int sector = this.extraCounter & 0xFF;
                  logger.info("FOUND.ADDR " + track + ':' + side + ':' + sector);
                }
              }
            } else {
              if (tstatesCounter > this.operationTimeOutCycles) {
                setInternalFlag(STAT_TRK00_OR_LOST);
              } else {
                setInternalFlag(STAT_DRQ);
                setInternalFlag(STAT_BUSY);
              }
            }
          }
        }
      }
    }
  }

  private void cmdStep(final long tstatesCounter, final int command, final boolean start) {
    final TrDosDisk thedisk = this.trdosDisk.get();

    resetStatus(false);

    if (thedisk == null) {
      resetDataRegisters();
      setInternalFlag(ST_NOTREADY);
    } else {
      if (start) {
        this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);
      }

      if ((command & 0b00001000) != 0) {
        setInternalFlag(STAT_HEADL);
      }
      if (thedisk.isWriteProtect()) {
        setInternalFlag(STAT_WRITEPROTECT);
      }
      if (!this.sector.isCrcOk()) {
        setInternalFlag(STAT_CRCERR);
      }

      if (tstatesCounter < this.operationTimeOutCycles) {
        setInternalFlag(STAT_BUSY);
      } else {
        int curtrack = this.registers[REG_TRACK];
        if (this.outwardStepDirection) {
          curtrack = (curtrack + 1) & 0xFF;
        } else {
          curtrack = (curtrack - 1) & 0xFF;
        }
        this.sector = thedisk.findRandomSector(this.head, curtrack);

        if ((command & 0x10) != 0) {
          this.registers[REG_TRACK] = curtrack;
        }

        if (this.sector == null) {
          setInternalFlag(STAT_NOTFOUND);
        } else {
          if (!this.sector.isCrcOk()) {
            setInternalFlag(STAT_CRCERR);
          }
        }
      }
    }
  }

  private void cmdStepIn(final long tstatesCounter, final int command, final boolean start) {
    this.outwardStepDirection = false;
    cmdStep(tstatesCounter, command, start);
  }

  private void cmdStepOut(final long tstatesCounter, final int command, final boolean start) {
    this.outwardStepDirection = true;
    cmdStep(tstatesCounter, command, start);
  }

  private void cmdReadSector(final long tstatesCounter, final int command, final boolean start) {
    final boolean multiSectors = (command & COMMAND_FLAG_MULTIPLE_RECORDS) != 0;
    final int sideNumber = (command >>> 3) & 1;
    final boolean doCheckSide = (command & 2) != 0;

    resetStatus(true);

    final TrDosDisk currentDisk = this.trdosDisk.get();
    if (currentDisk == null) {
      setInternalFlag(ST_NOTREADY);
    } else {
      loadSector(this.head, this.registers[REG_TRACK], this.registers[REG_SECTOR]);

      if (this.sector != null && doCheckSide && this.sector.getSide() != sideNumber) {
        this.sector = null;
      }

      if (this.sector == null) {
        resetDataRegisters();
        setInternalFlag(STAT_NOTFOUND);
      } else {
        if (start) {
          this.counter = 0;
          this.sectorPositioningCycles = Math.abs(tstatesCounter + TSTATES_SECTOR_POSITIONING);
        }

        if (tstatesCounter < this.sectorPositioningCycles) {
          setInternalFlag(STAT_BUSY);
        } else {
          if (this.sectorPositioningCycles >= 0L) {
            // start reading of first byte
            this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);
            this.sectorPositioningCycles = -1L;
          }

          if (this.flagWaitDataRd) {
            if (tstatesCounter > this.operationTimeOutCycles) {
              setInternalFlag(STAT_TRK00_OR_LOST);
            } else {
              setInternalFlag(STAT_DRQ);
              setInternalFlag(STAT_BUSY);
            }
          } else {
            if (this.counter >= this.sector.size()) {
              this.registers[REG_SECTOR] = (this.registers[REG_SECTOR] + 1) & 0xFF;
              this.sectorPositioningCycles = Math.abs(tstatesCounter + TSTATES_SECTOR_POSITIONING);
              if (multiSectors) {
                if (!this.sector.isLastOnTrack()) {
                  this.counter = 0;
                  setInternalFlag(STAT_BUSY);
                }
              }
            } else {
              final int data = this.sector.readByte(this.counter++);
              this.sectorPositioningCycles = Math.abs(tstatesCounter + TSTATES_SECTOR_POSITIONING);

              if (data < 0) {
                setInternalFlag(STAT_TRK00_OR_LOST);
              } else {
                if (tstatesCounter > this.operationTimeOutCycles) {
                  setInternalFlag(STAT_TRK00_OR_LOST);
                } else {
                  this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);
                  provideReadData(data);
                  setInternalFlag(STAT_DRQ);
                  setInternalFlag(STAT_BUSY);
                }
              }
            }
          }
        }
      }
    }
  }

  private void cmdWriteSector(final long tstatesCounter, final int command, final boolean start) {
    final boolean multiOp = (command & COMMAND_FLAG_MULTIPLE_RECORDS) != 0;
    final int softSide = (command >>> 3) & 1;
    final boolean doCheckSide = (command & 1) != 0;

    resetStatus(true);

    final TrDosDisk thedisk = this.trdosDisk.get();
    if (thedisk == null) {
      setInternalFlag(ST_NOTREADY);
    } else {
      if (thedisk.isWriteProtect()) {
        setInternalFlag(STAT_WRITEPROTECT);
      }
    }

    loadSector(this.head, this.registers[REG_TRACK], this.registers[REG_SECTOR]);

    if (this.sector != null && doCheckSide && this.sector.getSide() != softSide) {
      this.sector = null;
    }

    if (this.sector == null) {
      resetDataRegisters();
      setInternalFlag(STAT_NOTFOUND);
    } else {
      if (start) {
        this.counter = 0;
        this.flagWaitDataWr = true;
        this.sectorPositioningCycles = Math.abs(tstatesCounter + TSTATES_SECTOR_POSITIONING);
      }

      if (tstatesCounter < this.sectorPositioningCycles) {
        setInternalFlag(STAT_BUSY);
      } else {
        if (this.sectorPositioningCycles >= 0L) {
          // start reading of first byte
          this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);
          this.sectorPositioningCycles = -1L;
        }

        if (!this.flagWaitDataWr) {
          this.flagWaitDataWr = true;
          if (this.counter >= this.sector.size()) {
            this.registers[REG_SECTOR] = (this.registers[REG_SECTOR] + 1) & 0xFF;
            this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);
            if (multiOp) {
              if (!this.sector.isLastOnTrack()) {
                this.counter = 0;
                setInternalFlag(STAT_BUSY);
              }
            }
          } else {
            if (tstatesCounter > this.operationTimeOutCycles) {
              resetDataRegisters();
              setInternalFlag(STAT_NOTFOUND);
            } else {
              this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);
              if (!this.sector.writeByte(this.counter++, this.registers[REG_DATA_WR])) {
                setInternalFlag(STAT_WRFAULT);
              } else {
                if (this.counter < this.sector.size()) {
                  setInternalFlag(STAT_DRQ);
                  setInternalFlag(STAT_BUSY);
                }
              }
            }
          }
        } else {
          if (tstatesCounter > this.operationTimeOutCycles) {
            setInternalFlag(STAT_TRK00_OR_LOST);
          }
          setInternalFlag(STAT_DRQ);
          setInternalFlag(STAT_BUSY);
        }
      }
    }
  }

  private void cmdReadTrack(final long tstatesCounter, final int command, final boolean start) {
    resetStatus(true);

    final TrDosDisk currentDisk = this.trdosDisk.get();
    if (currentDisk == null) {
      setInternalFlag(ST_NOTREADY);
    } else {
      if (start) {
        this.counter = 0;
        final TrackHelper helper = this.mfmModulation ? new MFMTrackHelper(this.getDisk()) :
                new FMTracHelper(this.getDisk());
        helper.prepareTrackForRead(this.head, this.registers[REG_TRACK]);

        this.tempAuxiliaryObject = helper;
        this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);
      }

      if (!this.flagWaitDataRd) {
        this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);

        final TrackHelper helper = (TrackHelper) this.tempAuxiliaryObject;

        if (!helper.isCompleted()) {
          final int data = helper.readNextTrackData();

          if (data >= 0) {
            provideReadData(data);
            setInternalFlag(STAT_BUSY);
          }
        }
      } else {
        if (tstatesCounter > this.operationTimeOutCycles) {
          setInternalFlag(STAT_TRK00_OR_LOST);
        }
        setInternalFlag(STAT_DRQ);
        setInternalFlag(STAT_BUSY);
      }
    }
  }

  private void cmdWriteTrack(final long tstatesCounter, final int command, final boolean start) {
    resetStatus(false);

    final TrDosDisk currentDisk = this.trdosDisk.get();

    if (currentDisk == null) {
      setInternalFlag(ST_NOTREADY);
    } else {
      if (start) {
        this.counter = 0;
        this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);
        this.flagWaitDataWr = true;
        this.tempAuxiliaryObject = this.mfmModulation ? new MFMTrackHelper(this.getDisk()) :
                new FMTracHelper(this.getDisk());
      }

      if (!this.flagWaitDataWr) {
        this.operationTimeOutCycles = Math.abs(tstatesCounter + TSTATES_PER_BUFFER_VALID);
        this.flagWaitDataWr = true;

        final int data = this.registers[REG_DATA_WR] & 0xFF;
        final TrackHelper helper = (TrackHelper) this.tempAuxiliaryObject;

        if (!helper.isCompleted()) {
          try {
            helper.writeNextDataByte(data);
            setInternalFlag(STAT_BUSY);
            setInternalFlag(STAT_DRQ);
          } catch (IOException ex) {
            setInternalFlag(STAT_WRFAULT);
          }
        }
      } else {
        if (tstatesCounter > this.operationTimeOutCycles) {
          setInternalFlag(STAT_TRK00_OR_LOST);
        }
        setInternalFlag(STAT_DRQ);
        setInternalFlag(STAT_BUSY);
      }
    }
  }

  public boolean isMotorOn() {
    return (System.currentTimeMillis() - this.lastBusyOnTime) < DELAY_FDD_MOTOR_ON_MS;
  }

  private static abstract class TrackHelper {

    final boolean mfm;
    final TrDosDisk disk;
    State state;
    int trackIndex;
    int headIndex;
    int sectorIndex;
    int sectorSize;
    int expectedData;
    int dataByteIndex;
    int trackBytePosition;
    int trackTotalBytes;
    byte[] trackReadBuffer;

    TrackHelper(final TrDosDisk disk, final boolean mfm) {
      this.disk = disk;
      this.state = State.WAIT_ADDRESS;
      this.mfm = mfm;
      this.trackBytePosition = 0;
      this.trackTotalBytes = mfm ? 6450 : 6450;
    }

    protected int writeCrc(final Sector sector, final ByteArrayOutputStream buffer) {
      buffer.write(0xFE);

      buffer.write(sector.getTrackNumber());
      buffer.write(sector.getSide());
      buffer.write(sector.getPhysicalIndex());

      buffer.write(getSectorSizeCode(sector.size()));
      final int crc = sector.getCrc();

      buffer.write(crc);
      buffer.write(crc >>> 8);
      return crc;
    }

    final boolean writeNextDataByte(final int dataByte) throws IOException {
      switch (this.state) {
        case WAIT_ADDRESS: {
          if (dataByte == 0xFE) {
            this.state = State.ADDR_TRACK;
          }
        }
        break;
        case ADDR_TRACK: {
          this.trackIndex = dataByte;
          this.state = State.ADDR_HEAD;
        }
        break;
        case ADDR_HEAD: {
          this.headIndex = dataByte;
          this.state = State.ADDR_SECTOR;
        }
        break;
        case ADDR_SECTOR: {
          this.sectorIndex = dataByte;
          this.state = State.ADDR_SIZE;
        }
        break;
        case ADDR_SIZE: {
          this.sectorSize = dataByte;
          switch (this.sectorSize) {
            case 0:
              this.expectedData = 128;
              break;
            case 1:
              this.expectedData = 256;
              break;
            case 2:
              this.expectedData = 512;
              break;
            case 3:
              this.expectedData = 1024;
              break;
          }
          this.state = State.WAIT_DATA;
        }
        break;
        case WAIT_DATA: {
          if (dataByte == 0xFB) {
            this.state = State.DATA;
            this.dataByteIndex = 0;
          }
        }
        break;
        case DATA: {
          final TrDosDisk.Sector sector =
                  this.disk.findSector(this.headIndex, this.trackIndex, this.sectorIndex);
          if (sector == null) {
            throw new IOException(
                    "Can't find sector: " + this.trackIndex + ':' + this.headIndex + ':'
                            + this.sectorIndex);
          }
          if (!sector.writeByte(this.dataByteIndex, dataByte)) {
            throw new IOException(
                    "Can't write " + this.dataByteIndex + " byte to sector: " + this.trackIndex + ':'
                            + this.headIndex + ':' + this.sectorIndex);
          }
          this.expectedData--;
          this.dataByteIndex++;
          if (this.expectedData == 0) {
            this.state = State.WAIT_ADDRESS;
          }
        }
        break;
      }

      this.trackBytePosition++;
      return this.trackBytePosition < this.trackTotalBytes;
    }

    int getSectorSizeCode(final int size) {
      if (size <= 128) {
        return 0;
      }
      if (size <= 256) {
        return 1;
      }
      if (size <= 512) {
        return 2;
      }
      return 3;
    }

    abstract void prepareTrackForRead(int headIndex, int trackIndex);

    int readNextTrackData() {
      return this.trackBytePosition < this.trackTotalBytes
              ? this.trackReadBuffer[this.trackBytePosition++] : -1;
    }

    boolean isCompleted() {
      return this.trackBytePosition >= this.trackTotalBytes;
    }

    enum State {
      WAIT_ADDRESS,
      ADDR_TRACK,
      ADDR_HEAD,
      ADDR_SECTOR,
      ADDR_SIZE,
      WAIT_DATA,
      DATA
    }
  }

  private static class FMTracHelper extends TrackHelper {

    FMTracHelper(final TrDosDisk disk) {
      super(disk, false);
    }

    @Override
    void prepareTrackForRead(final int headIndex, final int trackIndex) {
      Sector sector = this.disk.findFirstSector(headIndex, trackIndex);

      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      for (int i = 0; i < 40; i++) {
        buffer.write(0xFF);
      }
      for (int i = 0; i < 40; i++) {
        buffer.write(0x00);
      }
      buffer.write(0xFC);
      for (int i = 0; i < 26; i++) {
        buffer.write(0xFF);
      }

      while (sector != null) {
        for (int i = 0; i < 6; i++) {
          buffer.write(0x00);
        }
        final int crc = writeCrc(sector, buffer);

        for (int i = 0; i < 11; i++) {
          buffer.write(0xFF);
        }
        for (int i = 0; i < 6; i++) {
          buffer.write(0x00);
        }

        buffer.write(0xFB);

        for (int i = 0; i < sector.size(); i++) {
          buffer.write(sector.readByte(i));
        }

        buffer.write(crc);
        buffer.write(crc >>> 8);

        for (int i = 0; i < 27; i++) {
          buffer.write(0xFF);
        }

        sector = disk.findSectorAfter(sector);
      }

      for (int i = 0; i < 247; i++) {
        buffer.write(0xFF);
      }

      this.trackReadBuffer = buffer.toByteArray();
      this.trackTotalBytes = this.trackReadBuffer.length;
    }

  }

  private static class MFMTrackHelper extends TrackHelper {

    MFMTrackHelper(final TrDosDisk disk) {
      super(disk, true);
    }

    @Override
    void prepareTrackForRead(final int headIndex, final int trackIndex) {
      Sector sector = this.disk.findFirstSector(headIndex, trackIndex);

      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

      for (int i = 0; i < 80; i++) {
        buffer.write(0x4E);
      }

      for (int i = 0; i < 12; i++) {
        buffer.write(0x00);
      }

      for (int i = 0; i < 3; i++) {
        buffer.write(0xF6);
      }

      buffer.write(0xFC);

      for (int i = 0; i < 50; i++) {
        buffer.write(0x4E);
      }

      while (sector != null) {
        for (int i = 0; i < 12; i++) {
          buffer.write(0x00);
        }

        for (int i = 0; i < 3; i++) {
          buffer.write(0xF5);
        }

        final int crc = writeCrc(sector, buffer);

        for (int i = 0; i < 22; i++) {
          buffer.write(0x4E);
        }

        for (int i = 0; i < 12; i++) {
          buffer.write(0x00);
        }

        for (int i = 0; i < 3; i++) {
          buffer.write(0xF5);
        }

        buffer.write(0xFB);

        for (int i = 0; i < sector.size(); i++) {
          buffer.write(sector.readByte(i));
        }

        buffer.write(crc);
        buffer.write(crc >>> 8);

        for (int i = 0; i < 54; i++) {
          buffer.write(0x4E);
        }

        sector = disk.findSectorAfter(sector);
      }

      for (int i = 0; i < 598; i++) {
        buffer.write(0x4E);
      }

      this.trackReadBuffer = buffer.toByteArray();
      this.trackTotalBytes = this.trackReadBuffer.length;
    }

  }

}
