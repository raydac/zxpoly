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
package com.igormaznitsa.zxpoly.components;

import com.igormaznitsa.z80.*;
import java.util.Arrays;
import java.util.Locale;
import java.util.logging.Logger;

public final class ZXPolyModule implements IODevice, Z80CPUBus {

  private final Logger LOGGER;

  private final Motherboard board;
  private final int moduleIndex;

  private final Z80 cpu;

  private final int PORT_REG0;
  private final int PORT_REG1;
  private final int PORT_REG2;
  private final int PORT_REG3;

  private final int[] zxPolyRegsWritten = new int[4];

  private int port7FFD;
  private int lastM1Address;

  private boolean activeRegisterReading;
  private int registerReadingCounter = 0;

  private boolean localInt;
  private boolean localNmi;
  private boolean waitSignal;

  private boolean stopAddressWait;
  private int localResetCounter;

  private boolean trdosROM;

  private static int calcRegAddress(final int moduleIndex, final int reg) {
    return (moduleIndex << 12) | (reg << 8) | 0xFF;
  }

  public int getHeapOffset() {
    return (this.zxPolyRegsWritten[0] & 7) * 0x10000;
  }

  public ZXPolyModule(final Motherboard board, final int index) {
    this.board = board;
    this.moduleIndex = index;

    this.PORT_REG0 = calcRegAddress(index, 0);
    this.PORT_REG1 = calcRegAddress(index, 1);
    this.PORT_REG2 = calcRegAddress(index, 2);
    this.PORT_REG3 = calcRegAddress(index, 3);

    this.cpu = new Z80(this);

    this.LOGGER = Logger.getLogger("ZX#" + index);

    LOGGER.info("Inited");
  }

  private int getRAMOffsetInHeap() {
    return (this.zxPolyRegsWritten[0] & 7) * 0x10000;
  }

  public Z80 getCPU() {
    return this.cpu;
  }

  @Override
  public int readIO(final ZXPolyModule module, final int port) {
    final int result;
    final int mappedModule = this.board.getMappedCPUIndex();

    if (module != this && this.moduleIndex > 0 && this.moduleIndex == mappedModule) {
      // reading memory for IO offset and make notification through INT
      result = this.board.readRAM(module, getRAMOffsetInHeap() + port);
      prepareLocalInt();
    }
    else {
      if (!isTRDOSActive() && port == PORT_REG0) {
        final int cpuState = this.cpu.getState();
        final int addr = ((this.lastM1Address >> 1) & 0x1)
                | ((this.lastM1Address >> 1) & 0x2)
                | ((this.lastM1Address >> 5) & 0x4)
                | ((this.lastM1Address >> 8) & 0x8)
                | ((this.lastM1Address >> 10) & 0x10)
                | ((this.lastM1Address >> 9) & 0x20);

        result = ((cpuState & Z80.SIGNAL_OUT_nHALT) == 0 ? ZXPOLY_rREG0_HALTMODE : 0)
                | (this.waitSignal ? ZXPOLY_rREG0_WAITMODE : 0) | (addr << 2);
      }
      else {
        result = 0;
      }
    }
    return result;
  }

  public boolean isTRDOSActive() {
    return this.trdosROM;
  }

  @Override
  public void writeIO(final ZXPolyModule module, final int port, final int value) {
    if (this.board.is3D00NotLocked() && module.moduleIndex <= this.moduleIndex) {
      if (port == PORT_REG0) {
        this.zxPolyRegsWritten[0] = value;
        if ((value & ZXPOLY_wREG0_RESET) != 0) {
          prepareLocalReset();
        }
        if ((value & ZXPOLY_wREG0_NMI) != 0) {
          prepareLocalNMI();
        }
        if ((value & ZXPOLY_wREG0_INT) != 0) {
          prepareLocalInt();
        }
      }
      else if (!module.isTRDOSActive()) {
        if (port == PORT_REG1) {
          this.zxPolyRegsWritten[1] = value;
        }
        else if (port == PORT_REG2) {
          this.zxPolyRegsWritten[2] = value;
        }
        else if (port == PORT_REG3) {
          this.zxPolyRegsWritten[3] = value;
        }
      }
    }
  }

  @Override
  public Motherboard getMotherboard() {
    return this.board;
  }

  public void prepareLocalReset() {
    this.localResetCounter = 3;
    this.registerReadingCounter = 3;
    this.activeRegisterReading = false;
  }

  public void prepareLocalNMI() {
    this.localNmi = true;
  }

  public void prepareLocalInt() {
    this.localInt = true;
  }

  public boolean step(final boolean signalReset, final boolean commonInt) {
    final int sigReset = signalReset || (this.localResetCounter > 0) ? Z80.SIGNAL_IN_nRESET : 0;
    if (this.localResetCounter > 0) {
      this.localResetCounter--;
    }

    final int sigInt;
    if (this.moduleIndex == 0) {
      sigInt = commonInt || this.localInt ? Z80.SIGNAL_IN_nINT : 0;
    }
    else {
      sigInt = (this.board.is3D00NotLocked() ? false : commonInt) || this.localInt ? Z80.SIGNAL_IN_nINT : 0;
    }
    this.localInt = false;

    final int sigNmi = (this.zxPolyRegsWritten[1] & ZXPOLY_wREG1_DISABLE_NMI) == 0 ? (this.localNmi ? Z80.SIGNAL_IN_nNMI : 0) : 0;
    this.localNmi = false;

    final int sigWait = this.waitSignal ? Z80.SIGNAL_IN_nWAIT : 0;

    final int oldCpuState = this.cpu.getState();
    this.cpu.step(Z80.SIGNAL_IN_ALL_INACTIVE ^ sigReset ^ sigInt ^ sigWait ^ sigNmi);
    final int newCpuState = this.cpu.getState();

    final boolean metHalt = (newCpuState & Z80.SIGNAL_OUT_nHALT) == 0 && (oldCpuState & Z80.SIGNAL_OUT_nHALT) != 0;
    return metHalt;
  }

  public int readVideoMemory(final int videoOffset) {
    final int moduleRamOffsetInHeap = getHeapOffset();

    final int result;
    if ((this.port7FFD & PORTw_ZX128_SCREEN) == 0) {
      // RAM 5
      result = this.board.readRAM(this, 0x14000 + moduleRamOffsetInHeap + videoOffset);
    }
    else {
      // RAM 7
      result = this.board.readRAM(this, 0x1C000 + moduleRamOffsetInHeap + videoOffset);
    }
    return result;
  }

  public String getMemoryValuesAsString(final int address, final int number) {
    final StringBuilder result = new StringBuilder(128);

    result.append(Utils.toHex(address)).append(": ");

    int theaddress = address;
    for (int i = 0; i < number; i++) {
      if (i > 0) {
        result.append(',');
      }

      final int value;
      if (theaddress < 0x4000) {
        if ((this.port7FFD & PORTw_ZX128_ROMRAM) == 0) {
          value = this.board.readROM(theaddress);
        }
        else {
          value = this.board.readRAM(this, ramOffset2HeapAddress(theaddress));
        }
      }
      else {
        value = this.board.readRAM(this, ramOffset2HeapAddress(theaddress));
      }

      theaddress++;

      final String hex = Integer.toHexString(value).toUpperCase(Locale.ENGLISH);

      if (hex.length() < 2) {
        result.append('0');
      }
      result.append(hex);
    }

    return result.toString();
  }

  @Override
  public byte readMemory(final Z80 cpu, final int address, final boolean m1) {

    final boolean activeRom128 = (this.port7FFD & PORTw_ZX128_ROM) == 0;

    if (m1) {
      this.lastM1Address = address;

      final int address_h = address >>> 8;

      if (address_h == 0x3D && !activeRom128) {
        // turn on the TR-DOS ROM Section
        this.trdosROM = true;
      }
      else if (this.trdosROM && address_h >= 0x40) {
        // turn off the TR-DOS ROM Section
        this.trdosROM = false;
      }
    }

    if (m1 && this.board.is3D00NotLocked() && this.registerReadingCounter == 0) {
      this.stopAddressWait = address != 0 && address == (this.zxPolyRegsWritten[2] | (this.zxPolyRegsWritten[3] << 8));
    }

    final byte result;

    if (this.registerReadingCounter > 0 && !this.activeRegisterReading && m1 && address == 0) {
      this.activeRegisterReading = true;
    }

    if (this.activeRegisterReading) {
      // read local registers R1-R3 instead of memory
      result = (byte) this.zxPolyRegsWritten[4 - this.registerReadingCounter];
      this.registerReadingCounter--;
      if (this.registerReadingCounter == 0) {
        this.zxPolyRegsWritten[1] = 0;
        this.zxPolyRegsWritten[2] = 0;
        this.zxPolyRegsWritten[3] = 0;
        this.activeRegisterReading = false;
      }
    }
    else {
      final int ramAddress = ramOffset2HeapAddress(address);
      if (address < 0x4000) {
        if ((this.port7FFD & PORTw_ZX128_ROMRAM) != 0) {
          //RAM0
          result = (byte) this.board.readRAM(this, ramAddress);
        }
        else {
          if (this.trdosROM) {
            result = (byte) this.board.readROM(address + 0x8000);
          }
          else {
            result = (byte) this.board.readROM(address + (activeRom128 ? 0x4000 : 0));
          }
        }
      }
      else {
        result = (byte) this.board.readRAM(this, ramAddress);
      }
    }
    return result;
  }

  public int ramOffset2HeapAddress(final int address) {
    final int page = (address >>> 14) & 3;
    final int offset = address & 0x3FFF;
    int result = 0;
    switch (page) {
      case 0: {
        //RAM0
        result = 0;
      }
      break;
      case 1: {
        // CPU1, RAM5
        result = 0x14000;
      }
      break;
      case 2: {
        // CPU2, RAM2
        result = 0x8000;
      }
      break;
      case 3: {
        //CPU3, top page
        result = 0x4000 * (this.port7FFD & 0x7);
      }
      break;
    }
    return getHeapOffset() + (result | offset);
  }

  @Override
  public void writeMemory(final Z80 cpu, final int address, final byte data) {
    final int reg0 = this.zxPolyRegsWritten[0];
    final int val = data & 0xFF;

    if ((reg0 & REG0w_MEMORY_WRITING_DISABLED) == 0) {
      final int ramAddress = ramOffset2HeapAddress(address);

      if (address < 0x4000) {
        if ((this.port7FFD & PORTw_ZX128_ROMRAM) != 0) {
          //RAM0
          this.board.writeRAM(this, ramAddress, val);
        }
      }
      else {
        this.board.writeRAM(this, ramAddress, val);
      }
    }
  }

  @Override
  public byte readPort(final Z80 cpu, final int port) {
    final byte result;
    if (port == PORTrw_ZXPOLY) {
      if (this.moduleIndex == 0 && this.board.getMappedCPUIndex() > 0) {
        result = (byte) this.board.readBusIO(this, port);
      }
      else {
        final int reg0 = zxPolyRegsWritten[0];
        final int outForCPU0 = this.moduleIndex == this.board.getMappedCPUIndex() ? PORTr_ZXPOLY_IOFORCPU0 : 0;
        final int memDisabled = (reg0 & ZXPOLY_wREG0_MEMWR_DISABLED) == 0 ? 0 : PORTr_ZXPOLY_MEMDISABLED;
        final int ioDisabled = (reg0 & ZXPOLY_wREG0_OUT_DISABLED) == 0 ? 0 : PORTr_ZXPOLY_IODISABLED;
        result = (byte) (this.moduleIndex | ((this.zxPolyRegsWritten[0] & 7) << 5) | outForCPU0 | memDisabled | ioDisabled);

      }
    }
    else {
      result = (byte) this.board.readBusIO(this, port);
    }
    return result;
  }

  private void set7FFD(final int value) {
    if ((this.port7FFD & PORTw_ZX128_LOCK) == 0) {
      this.port7FFD = value;
    }
  }

  @Override
  public void writePort(final Z80 cpu, final int port, final byte data) {
    final int val = data & 0xFF;
    final int reg0 = this.zxPolyRegsWritten[0];

    if ((reg0 & ZXPOLY_wREG0_OUT_DISABLED) == 0) {
      if (port == PORTw_ZX128) {
        if (this.moduleIndex == 0) {
          if (this.board.getMappedCPUIndex() > 0 && (this.zxPolyRegsWritten[1] & ZXPOLY_wREG1_WRITE_MAPPED_IO_7FFD) != 0) {
            this.board.writeBusIO(this, port, val);
          }
          else {
            set7FFD(val);
          }
        }
        else {
          set7FFD(val);
        }
      }
      else {
        this.board.writeBusIO(this, port, val);
      }
    }
  }

  @Override
  public byte onCPURequestDataLines(final Z80 cpu) {
    return (byte) 0xFF;
  }

  @Override
  public void onRETI(final Z80 cpu) {
  }

  @Override
  public void preStep(final boolean signalReset, final boolean signalInt) {
    if (signalReset) {
      setStateForSystemReset();
    }
    prepareWaitSignal();
  }

  private void prepareWaitSignal() {
    this.waitSignal = this.stopAddressWait || (this.moduleIndex > 0 && this.board.isCPUModules123InWaitMode());
  }

  @Override
  public String getName() {
    return "ZXM#" + moduleIndex;
  }

  public int getReg1WrittenData() {
    return this.zxPolyRegsWritten[1];
  }

  private void setStateForSystemReset() {
    LOGGER.info("Reset");
    this.port7FFD = 0;
    this.trdosROM = false;
    this.registerReadingCounter = 0;
    this.activeRegisterReading = false;
    this.localResetCounter = 3;
    this.lastM1Address = 0;
    this.localInt = false;
    this.localNmi = false;
    Arrays.fill(this.zxPolyRegsWritten, 0);

    // set the intitial module memory offset in the heap
    this.zxPolyRegsWritten[0] = this.moduleIndex << 1;
  }

  public int getLastM1Address() {
    return this.lastM1Address;
  }

  public int getModuleIndex() {
    return this.moduleIndex;
  }

  @Override
  public String toString() {
    return "ZXM#" + this.moduleIndex;
  }

}
