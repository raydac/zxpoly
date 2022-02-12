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
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class K1818VG93 {

  public static final int STATUS_BUSY = 0b0000_0001;
  public static final int STATUS_INDEXMARK_DRQ = 0b0000_0010;
  public static final int STATUS_TR00_DATALOST = 0b0000_0100;
  public static final int STATUS_CRCERROR = 0b0000_1000;
  public static final int STATUS_SEEKERROR_NOTFOUNF = 0b0001_0000;
  public static final int STATUS_HEADLOADED_RECTYPE_WRITEFAULT = 0b0010_0000;
  public static final int STATUS_WRITE_PROTECT = 0b0100_0000;
  public static final int STATUS_NOT_READY = 0b1000_0000;
  static final int ADDR_COMMAND_STATE = 0;
  static final int ADDR_TRACK = 1;
  static final int ADDR_SECTOR = 2;
  static final int ADDR_DATA = 3;
  private static final long DELAY_FDD_MOTOR_ON_MS = 2000L;
  private static final int REG_COMMAND = 0x00;
  private static final int REG_STATUS = 0x01;
  private static final int REG_TRACK = 0x02;
  private static final int REG_SECTOR = 0x03;
  private static final int REG_DATA_WR = 0x04;
  private static final int REG_DATA_RD = 0x05;
  private static final int COMMAND_FLAG_MULTIPLE_RECORDS = 0b0001_0000;
  private final long tstatesDiskTurn;
  private final long[] tstatesPerTrackChange;
  private final long tstatesPerSector;
  private final long tstatesPerSectorBe;
  private final long tstatesIndexMarkLength;
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
  private boolean outwardStepDirection;
  private boolean trackIndexMarkerActive;
  private boolean mfmModulation;
  private long sectorPositioningCycles;
  private long operationTimeOutCycles;
  private volatile long lastBusyOnTime;
  private Object tempAuxiliaryObject;

  private long timeIndexMarkChange = -1L;

  public K1818VG93(final TimingProfile profile, final Logger logger) {
    this.logger = logger;

    this.tstatesDiskTurn = profile.tstatesFrame * 4L;
    this.tstatesPerTrackChange = new long[]{
            profile.tstatesFrame / 4,
            profile.tstatesFrame / 2,
            profile.tstatesFrame,
            profile.tstatesFrame + profile.tstatesFrame / 3,
    };
    this.tstatesPerSector = tstatesDiskTurn / TrDosDisk.SECTORS_PER_TRACK;
    this.tstatesPerSectorBe = profile.tstatesFrame / 640;
    this.tstatesIndexMarkLength = (profile.tstatesFrame * 80L) / 1000L; // 4 ms

    reset();
  }

  public void reset() {
    Arrays.fill(registers, 0);
    this.trackIndexMarkerActive = false;
    this.timeIndexMarkChange = -1L;
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
      this.timeIndexMarkChange = -1L;
      this.trackIndexMarkerActive = false;
      this.sector = disk.findFirstSector(this.registers[REG_TRACK]);
    }
  }

  private TrDosDisk getDisk() {
    return this.trdosDisk.get();
  }

  private void resetStatus(final boolean preventDataLostFlag) {
    registers[REG_STATUS] = preventDataLostFlag ? registers[REG_STATUS] & STATUS_TR00_DATALOST : 0;
  }

  private void setInternalFlag(final int flags) {
    if ((flags & STATUS_BUSY) != 0) {
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

  public boolean isMFMModulation() {
    return this.mfmModulation;
  }

  public void setMFMModulation(final boolean flag) {
    this.mfmModulation = flag;
  }

  private void updateStatusForCommandsTypeI() {
    if (this.registers[REG_TRACK] == 0) {
      setInternalFlag(STATUS_TR00_DATALOST);
    } else {
      resetInternalFlag(STATUS_TR00_DATALOST);
    }

    if (this.trdosDisk.get() == null) {
      setInternalFlag(STATUS_INDEXMARK_DRQ);
    } else {
      if (this.sector != null && !this.sector.isCrcOk()) {
        setInternalFlag(STATUS_CRCERROR);
      }
      if (this.trackIndexMarkerActive) {
        setInternalFlag(STATUS_INDEXMARK_DRQ);
      } else {
        resetInternalFlag(STATUS_INDEXMARK_DRQ);
      }
    }
  }

  public int read(final int address) {
    switch (address & 0b11) {
      case ADDR_COMMAND_STATE: {
        return registers[REG_STATUS] & 0xFF;
      }
      case ADDR_TRACK:
        return registers[REG_TRACK] & 0xFF;
      case ADDR_SECTOR:
        return registers[REG_SECTOR] & 0xFF;
      case ADDR_DATA:
        if (this.flagWaitDataRd) {
          this.flagWaitDataRd = false;
          resetInternalFlag(STATUS_INDEXMARK_DRQ);
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
        if (isFlag(STATUS_BUSY)) {
          if ((normValue >>> 4) == 0b1101) {
            this.registers[REG_COMMAND] = normValue;
            this.firstCommandStep = true;
            this.flagWaitDataRd = false;
            this.flagWaitDataWr = false;
            this.operationTimeOutCycles = 0L;
            setInternalFlag(STATUS_BUSY);
          }
        } else {
          switch (normValue >>> 4) {
            case 0b1000: // RD SECTOR
            case 0b1001:

            case 0b1010:
            case 0b1011: // WR SECTOR

            case 0b1100: // RD ADDRESS
            case 0b1110: // RD TRACK
            case 0b1111: { // WR TRACK
              resetStatus(false);
            }
            break;
          }
          this.operationTimeOutCycles = 0L;
          this.registers[REG_COMMAND] = normValue;
          this.firstCommandStep = true;
          this.flagWaitDataRd = false;
          this.flagWaitDataWr = false;
          setInternalFlag(STATUS_BUSY);
        }

        if (this.firstCommandStep) {
          logger.log(Level.INFO,
                  "FDD cmd (" + toBinByte(normValue) + "): " + commandAsText(normValue));
        }
      }
      break;
      case ADDR_TRACK: { // track
        if (!isFlag(STATUS_BUSY)) {
          registers[REG_TRACK] = normValue;
        }
      }
      break;
      case ADDR_SECTOR: { // sector
        if (!isFlag(STATUS_BUSY)) {
          registers[REG_SECTOR] = normValue;
        }
      }
      break;
      case ADDR_DATA: { // data
        if (this.flagWaitDataWr) {
          this.flagWaitDataWr = false;
          resetInternalFlag(STATUS_INDEXMARK_DRQ);
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
    final int addressTrack = this.registers[REG_TRACK];
    final int addressSector = this.registers[REG_SECTOR];

    final TrDosDisk disk = this.trdosDisk.get();

    final String address = String.format("current(track=%d, head=%d, sector=%d, dataReg=%d)",
            addressTrack,
            (disk == null ? -1 : disk.getHeadIndex()),
            addressSector,
            this.registers[REG_DATA_WR]
    );

    final int high = command >>> 4;
    switch (high) {
      case 0b0000: {
        return "RESTORE (" + address + ')';
      }
      case 0b0001: {
        return "SEEK (" + address + ")";
      }
      case 0b0010:
      case 0b0011: {
        return "STEP (" + address + ')';
      }
      case 0b0100:
      case 0b0101: {
        return "STEP_IN (" + address + ')';
      }
      case 0b0110:
      case 0b0111: {
        return "STEP_OUT (" + address + ')';
      }
      case 0b1000:
      case 0b1001: {
        return "RD.SECTOR" + ((high & 1) == 0 ? "(S)" : "(M)") + ' ' + address;
      }
      case 0b1010:
      case 0b1011: {
        return "WR.SECTOR" + ((high & 1) == 0 ? "(S)" : "(M)") + ' ' + address;
      }
      case 0b1100: {
        return "RD.ADDR";
      }
      case 0b1110: {
        return "RD.TRACK (track=" + addressTrack + ")";
      }
      case 0b1111: {
        return "WR.TRACK (track=" + addressTrack + ")";
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
      setInternalFlag(STATUS_NOT_READY);
    } else {
      resetInternalFlag(STATUS_NOT_READY);
      if (tstatesCounter >= this.timeIndexMarkChange) {
        this.timeIndexMarkChange = tstatesCounter + (this.trackIndexMarkerActive ? tstatesDiskTurn - tstatesIndexMarkLength : tstatesIndexMarkLength);
        this.trackIndexMarkerActive = !this.trackIndexMarkerActive;
      }
    }

    final int command = this.registers[REG_COMMAND];
    if ((command & 0x80) == 0) {
      this.updateStatusForCommandsTypeI();
    }
    if (isFlag(STATUS_BUSY)) {
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

  private void loadSector(final int track, final int sector) {
    final TrDosDisk floppy = this.trdosDisk.get();
    final Sector foundSector;
    if (floppy == null) {
      foundSector = null;
    } else {
      foundSector = floppy.findSector(track, sector);
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
    logger.info("INTERRUPT (counter=" + this.counter + ')');
    this.operationTimeOutCycles = -1L;
    resetInternalFlag(STATUS_BUSY);
  }

  private void cmdRestore(final long tstatesCounter, final int command, final boolean start) {
    final TrDosDisk currentDisk = this.trdosDisk.get();

    resetStatus(false);

    if (start) {
      this.counter = 0;
    }

    if (currentDisk == null) {
      setInternalFlag(STATUS_NOT_READY);
    } else {
      if ((command & 0b00001000) != 0) {
        setInternalFlag(STATUS_HEADLOADED_RECTYPE_WRITEFAULT);
      }
      if (currentDisk.isWriteProtect()) {
        setInternalFlag(STATUS_WRITE_PROTECT);
      }

      if (counter < 0xFF && this.registers[REG_TRACK] > 0) {
        this.registers[REG_TRACK]--;
        this.sector = currentDisk.findFirstSector(this.registers[REG_TRACK]);
        if (this.sector == null) {
          setInternalFlag(STATUS_SEEKERROR_NOTFOUNF);
        } else {
          this.registers[REG_SECTOR] = 1;
          setInternalFlag(STATUS_BUSY);
        }
      } else {
        if (this.registers[REG_TRACK] != 0) {
          this.sector = currentDisk.findFirstSector(this.registers[REG_TRACK]);
          if (this.sector != null)
            this.registers[REG_SECTOR] = 1;
          setInternalFlag(STATUS_SEEKERROR_NOTFOUNF);
        }
      }
    }
  }

  private void cmdSeek(final long tstatesCounter, final int command, final boolean start) {
    resetStatus(false);

    final TrDosDisk currentDisk = this.trdosDisk.get();

    if ((command & 0b0000_1000) != 0) {
      setInternalFlag(STATUS_HEADLOADED_RECTYPE_WRITEFAULT);
    }

    if (currentDisk == null) {
      setInternalFlag(STATUS_NOT_READY);
    } else {
      if (start) {
        this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
      }

      if (currentDisk.isWriteProtect()) {
        setInternalFlag(STATUS_WRITE_PROTECT);
      }

      boolean completed = false;
      if (tstatesCounter >= this.operationTimeOutCycles) {
        if (this.registers[REG_TRACK] != this.registers[REG_DATA_WR]) {
          if (this.registers[REG_TRACK] < this.registers[REG_DATA_WR]) {
            this.registers[REG_TRACK]++;
          } else {
            this.registers[REG_TRACK]--;
          }
          this.registers[REG_SECTOR] = 1;
          loadSector(this.registers[REG_TRACK], this.registers[REG_SECTOR]);
          logger.info("FDD head moved to track " + this.registers[REG_TRACK] + ", target track is "
                  + this.registers[REG_DATA_WR]);
          this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerTrackChange[command & 2]);
        }

        completed = this.registers[REG_TRACK] == this.registers[REG_DATA_WR];

        if (this.sector == null) {
          setInternalFlag(STATUS_SEEKERROR_NOTFOUNF);
          completed = true;
        } else {
          loadSector(this.registers[REG_TRACK], this.registers[REG_SECTOR]);
          if (this.sector != null && !this.sector.isCrcOk()) {
            setInternalFlag(STATUS_CRCERROR);
          }
        }
      }

      if (completed) {
        this.logger.info("SEEK completed on track=" + this.registers[REG_TRACK]);
      } else {
        setInternalFlag(STATUS_BUSY);
      }
    }
  }

  private void cmdReadAddress(final long tstatesCounter, final int command, final boolean start) {
    resetStatus(true);

    final TrDosDisk currentDisk = this.trdosDisk.get();

    if (currentDisk == null) {
      setInternalFlag(STATUS_NOT_READY);
    } else {
      if (start) {
        if (this.sector == null) {
          this.sector = currentDisk.findFirstSector(this.registers[REG_TRACK]);
        }

        this.counter = 6;
        this.sectorPositioningCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);

        this.extraCounter = 0;
      }

      if (this.sector == null) {
        this.sector = currentDisk.findFirstSector(this.registers[REG_TRACK]);
      }

      if (this.sector == null) {
        setInternalFlag(STATUS_SEEKERROR_NOTFOUNF);
        resetDataRegisters();
      } else {
        if (tstatesCounter < this.sectorPositioningCycles) {
          setInternalFlag(STATUS_BUSY);
        } else {
          if (this.sectorPositioningCycles >= 0L) {
            this.sectorPositioningCycles = -1L;
            this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
          }

          if (!this.sector.isCrcOk()) {
            setInternalFlag(STATUS_CRCERROR);
          } else {
            if (!this.flagWaitDataRd) {
              this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
              if (this.counter > 0) {
                switch (this.counter) {
                  case 6: {// track
                    final int trackNum = this.sector.getTrackNumber();
                    provideReadData(trackNum);
                    setInternalFlag(STATUS_INDEXMARK_DRQ);
                    this.extraCounter |= (trackNum << 16);
                  }
                  break;
                  case 5: {// side
                    final int side = this.sector.getSide();
                    provideReadData(side);
                    setInternalFlag(STATUS_INDEXMARK_DRQ);
                    this.extraCounter |= (side << 8);
                  }
                  break;
                  case 4: {// sector
                    final int sectorId = this.sector.getPhysicalIndex();
                    provideReadData(sectorId);
                    setInternalFlag(STATUS_INDEXMARK_DRQ);
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
                    setInternalFlag(STATUS_INDEXMARK_DRQ);
                  }
                  break;
                  case 2: {// crc1
                    final int crc = this.sector.getCrc() >>> 8;
                    provideReadData(crc);
                    setInternalFlag(STATUS_INDEXMARK_DRQ);
                  }
                  break;
                  case 1: {// crc2
                    final int crc = this.sector.getCrc() & 0xFF;
                    provideReadData(crc);
                    setInternalFlag(STATUS_INDEXMARK_DRQ);
                  }
                  break;
                  default:
                    throw new Error("Unexpected counter state");
                }
                this.counter--;
                if (this.counter > 0) {
                  setInternalFlag(STATUS_BUSY);
                } else {
                  final int track = (this.extraCounter >> 16) & 0xFF;
                  final int side = (this.extraCounter >> 8) & 0xFF;
                  final int sector = this.extraCounter & 0xFF;
                  logger.info("FOUND.ADDR t=" + track + ";h=" + side + ":s=" + sector);
                }
              }
            } else {
              if (tstatesCounter > this.operationTimeOutCycles) {
                setInternalFlag(STATUS_TR00_DATALOST);
              } else {
                setInternalFlag(STATUS_INDEXMARK_DRQ | STATUS_BUSY);
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
      setInternalFlag(STATUS_NOT_READY);
    } else {
      if (start) {
        this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
      }

      if ((command & 0b00001000) != 0) {
        setInternalFlag(STATUS_HEADLOADED_RECTYPE_WRITEFAULT);
      }
      if (thedisk.isWriteProtect()) {
        setInternalFlag(STATUS_WRITE_PROTECT);
      }
      if (!this.sector.isCrcOk()) {
        setInternalFlag(STATUS_CRCERROR);
      }

      if (tstatesCounter < this.operationTimeOutCycles) {
        setInternalFlag(STATUS_BUSY);
      } else {
        int currentTrack = this.registers[REG_TRACK];
        if (this.outwardStepDirection) {
          currentTrack = (currentTrack + 1) & 0xFF;
        } else {
          currentTrack = (currentTrack - 1) & 0xFF;
        }
        this.sector = thedisk.findRandomSector(currentTrack);

        if ((command & 0x10) != 0) {
          this.registers[REG_TRACK] = currentTrack;
        }

        if (this.sector == null) {
          setInternalFlag(STATUS_SEEKERROR_NOTFOUNF);
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
      setInternalFlag(STATUS_NOT_READY);
    } else {
      loadSector(this.registers[REG_TRACK], this.registers[REG_SECTOR]);

      if (this.sector != null && doCheckSide && this.sector.getSide() != sideNumber) {
        this.sector = null;
      }

      if (this.sector == null) {
        resetDataRegisters();
        setInternalFlag(STATUS_SEEKERROR_NOTFOUNF);
      } else {
        if (start) {
          this.counter = 0;
          this.sectorPositioningCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
        }

        if (tstatesCounter < this.sectorPositioningCycles) {
          setInternalFlag(STATUS_BUSY);
          resetInternalFlag(STATUS_INDEXMARK_DRQ);
        } else {
          if (this.sectorPositioningCycles >= 0L) {
            // start reading of first byte
            this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
            this.sectorPositioningCycles = -1L;
          }

          if (this.flagWaitDataRd) {
            if (tstatesCounter > this.operationTimeOutCycles) {
              setInternalFlag(STATUS_TR00_DATALOST);
            } else {
              setInternalFlag(STATUS_INDEXMARK_DRQ | STATUS_BUSY);
            }
          } else {
            if (this.counter >= this.sector.size()) {
              // sector reading end
              this.sectorPositioningCycles = Math.abs(tstatesCounter + tstatesPerSector);
              if (multiSectors) {
                if (!this.sector.isLastOnTrack()) {
                  this.logger.info("RD.SECTOR completed, start next sector in multi-sec");
                  this.registers[REG_SECTOR] = (this.registers[REG_SECTOR] + 1) & 0xFF;
                  loadSector(this.registers[REG_TRACK], this.registers[REG_SECTOR]);
                  this.counter = 0;
                  setInternalFlag(STATUS_BUSY);
                  resetInternalFlag(STATUS_INDEXMARK_DRQ);
                }
              } else {
                this.logger.info("RD.SECTOR completed");
              }
            } else {
              final int data = this.sector.readByte(this.counter++);
              this.sectorPositioningCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);

              if (data < 0) {
                setInternalFlag(STATUS_TR00_DATALOST);
                resetInternalFlag(STATUS_INDEXMARK_DRQ);
              } else {
                if (tstatesCounter > this.operationTimeOutCycles) {
                  setInternalFlag(STATUS_TR00_DATALOST);
                } else {
                  this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
                  provideReadData(data);
                  setInternalFlag(STATUS_INDEXMARK_DRQ | STATUS_BUSY);
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
      setInternalFlag(STATUS_NOT_READY);
    } else {
      if (thedisk.isWriteProtect()) {
        setInternalFlag(STATUS_WRITE_PROTECT);
      }
    }

    loadSector(this.registers[REG_TRACK], this.registers[REG_SECTOR]);

    if (this.sector != null && doCheckSide && this.sector.getSide() != softSide) {
      this.sector = null;
    }

    if (this.sector == null) {
      resetDataRegisters();
      setInternalFlag(STATUS_SEEKERROR_NOTFOUNF);
    } else {
      if (start) {
        this.counter = 0;
        this.flagWaitDataWr = true;
        this.sectorPositioningCycles = Math.abs(tstatesCounter + tstatesPerSector);
      }

      if (tstatesCounter < this.sectorPositioningCycles) {
        setInternalFlag(STATUS_BUSY);
      } else {
        if (this.sectorPositioningCycles >= 0L) {
          // start reading of first byte
          this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
          this.sectorPositioningCycles = -1L;
        }

        if (!this.flagWaitDataWr) {
          this.flagWaitDataWr = true;
          if (this.counter >= this.sector.size()) {
            this.registers[REG_SECTOR] = (this.registers[REG_SECTOR] + 1) & 0xFF;
            this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
            if (multiOp) {
              if (!this.sector.isLastOnTrack()) {
                this.counter = 0;
                setInternalFlag(STATUS_BUSY);
              }
            }
          } else {
            if (tstatesCounter > this.operationTimeOutCycles) {
              resetDataRegisters();
              setInternalFlag(STATUS_SEEKERROR_NOTFOUNF);
            } else {
              this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
              if (!this.sector.writeByte(this.counter++, this.registers[REG_DATA_WR])) {
                setInternalFlag(STATUS_HEADLOADED_RECTYPE_WRITEFAULT);
              } else {
                if (this.counter < this.sector.size()) {
                  setInternalFlag(STATUS_INDEXMARK_DRQ | STATUS_BUSY);
                }
              }
            }
          }
        } else {
          if (tstatesCounter > this.operationTimeOutCycles) {
            setInternalFlag(STATUS_TR00_DATALOST);
          }
          setInternalFlag(STATUS_INDEXMARK_DRQ | STATUS_BUSY);
        }
      }
    }
  }

  private void cmdReadTrack(final long tstatesCounter, final int command, final boolean start) {
    resetStatus(true);

    final TrDosDisk currentDisk = this.trdosDisk.get();
    if (currentDisk == null) {
      setInternalFlag(STATUS_NOT_READY);
    } else {
      if (start) {
        this.counter = 0;
        final TrackHelper helper = this.mfmModulation ? new MFMTrackHelper(this.getDisk()) :
                new FMTracHelper(this.getDisk());
        helper.prepareTrackForRead(this.registers[REG_TRACK]);

        this.tempAuxiliaryObject = helper;
        this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
      }

      if (!this.flagWaitDataRd) {
        this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);

        final TrackHelper helper = (TrackHelper) this.tempAuxiliaryObject;

        if (!helper.isCompleted()) {
          final int data = helper.readNextTrackData();

          if (data >= 0) {
            provideReadData(data);
            setInternalFlag(STATUS_BUSY);
          }
        }
      } else {
        if (tstatesCounter > this.operationTimeOutCycles) {
          setInternalFlag(STATUS_TR00_DATALOST);
        }
        setInternalFlag(STATUS_INDEXMARK_DRQ | STATUS_BUSY);
      }
    }
  }

  private void cmdWriteTrack(final long tstatesCounter, final int command, final boolean start) {
    resetStatus(false);

    final TrDosDisk currentDisk = this.trdosDisk.get();

    if (currentDisk == null) {
      setInternalFlag(STATUS_NOT_READY);
    } else {
      if (start) {
        this.counter = 0;
        this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
        this.flagWaitDataWr = true;
        this.tempAuxiliaryObject = this.mfmModulation ? new MFMTrackHelper(this.getDisk()) :
                new FMTracHelper(this.getDisk());
      }

      if (!this.flagWaitDataWr) {
        this.operationTimeOutCycles = Math.abs(tstatesCounter + tstatesPerSectorBe);
        this.flagWaitDataWr = true;

        final int data = this.registers[REG_DATA_WR] & 0xFF;
        final TrackHelper helper = (TrackHelper) this.tempAuxiliaryObject;

        if (!helper.isCompleted()) {
          try {
            helper.writeNextDataByte(data);
            setInternalFlag(STATUS_BUSY | STATUS_INDEXMARK_DRQ);
          } catch (IOException ex) {
            setInternalFlag(STATUS_HEADLOADED_RECTYPE_WRITEFAULT);
          }
        }
      } else {
        if (tstatesCounter > this.operationTimeOutCycles) {
          setInternalFlag(STATUS_TR00_DATALOST);
        }
        setInternalFlag(STATUS_INDEXMARK_DRQ | STATUS_BUSY);
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
                  this.disk.findSector(this.trackIndex, this.sectorIndex);
          if (sector == null) {
            throw new IOException(
                    "Can't find sector: " + this.trackIndex + ':' + this.sectorIndex);
          }
          if (!sector.writeByte(this.dataByteIndex, dataByte)) {
            throw new IOException(
                    "Can't write " + this.dataByteIndex + " byte to sector: " + this.trackIndex + ':' + this.sectorIndex);
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

    abstract void prepareTrackForRead(int trackIndex);

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
    void prepareTrackForRead(final int trackIndex) {
      Sector sector = this.disk.findFirstSector(trackIndex);

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

        sector = disk.findNextSector(sector);
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
    void prepareTrackForRead(final int trackIndex) {
      Sector sector = this.disk.findFirstSector(trackIndex);

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

        sector = disk.findNextSector(sector);
      }

      for (int i = 0; i < 598; i++) {
        buffer.write(0x4E);
      }

      this.trackReadBuffer = buffer.toByteArray();
      this.trackTotalBytes = this.trackReadBuffer.length;
    }

  }

}
