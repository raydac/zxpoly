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

package com.igormaznitsa.zxpoly.components;

import com.igormaznitsa.z80.MemoryAccessProvider;
import com.igormaznitsa.z80.Utils;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.z80.Z80CPUBus;
import com.igormaznitsa.z80.Z80Instruction;
import com.igormaznitsa.z80.disasm.Z80Disasm;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.logging.Logger;

@SuppressWarnings("WeakerAccess")
public final class ZxPolyModule implements IoDevice, Z80CPUBus, MemoryAccessProvider {

  private static final int INTERRUPTION_LENGTH_CYCLES = 5;

  private final Logger logger;
  private final Motherboard board;
  private final int moduleIndex;
  private final Z80 cpu;
  private final int PORT_REG0;
  private final int PORT_REG1;
  private final int PORT_REG2;
  private final int PORT_REG3;
  private final AtomicIntegerArray zxPolyRegsWritten = new AtomicIntegerArray(4);
  private final AtomicInteger port7FFD = new AtomicInteger();
  private int intCounter;
  private int nmiCounter;
  private int lastM1Address;

  private boolean activeRegisterReading;
  private int registerReadingCounter = 0;

  private boolean localInt;
  private boolean localNmi;
  private boolean waitSignal;

  private boolean stopAddressWait;
  private int localResetCounter;

  private long mcyclesOfActivityBetweenInt;

  private volatile boolean trdosRomActive;

  public ZxPolyModule(final Motherboard board, final int index) {
    this.board = board;
    this.moduleIndex = index;

    this.PORT_REG0 = calcPortForRegister(index, 0);
    this.PORT_REG1 = calcPortForRegister(index, 1);
    this.PORT_REG2 = calcPortForRegister(index, 2);
    this.PORT_REG3 = calcPortForRegister(index, 3);

    this.cpu = new Z80(this);

    this.logger = Logger.getLogger("ZX#" + index);

    logger.info("Inited");
  }

  private static int calcPortForRegister(final int moduleIndex, final int registerIndex) {
    return (moduleIndex << 12) | (registerIndex << 8) | 0xFF;
  }

  public int getHeapOffset() {
    return (this.zxPolyRegsWritten.get(0) & 7) * 0x10000;
  }

  private int getHeapRamOffset() {
    return (this.zxPolyRegsWritten.get(0) & 7) * 0x10000;
  }

  public Z80 getCpu() {
    return this.cpu;
  }

  private static int packAddress(final int address) {
    return ((address >> 1) & 0x1)
        | ((address >> 1) & 0x2)
        | ((address >> 5) & 0x4)
        | ((address >> 8) & 0x8)
        | ((address >> 10) & 0x10)
        | ((address >> 9) & 0x20);
  }

  public void fillArrayByPortValues(final byte[] fiveElementArray) {
    fiveElementArray[0] = (byte) this.port7FFD.get();
    fiveElementArray[1] = (byte) this.zxPolyRegsWritten.get(0);
    fiveElementArray[2] = (byte) this.zxPolyRegsWritten.get(1);
    fiveElementArray[3] = (byte) this.zxPolyRegsWritten.get(2);
    fiveElementArray[4] = (byte) this.zxPolyRegsWritten.get(3);
  }

  public void fillPortByValues(final int port7ffd, final int reg0, final int reg1, final int reg2, final int reg3) {
    this.port7FFD.set(port7ffd & 0xFF);
    this.zxPolyRegsWritten.set(0, reg0 & 0xFF);
    this.zxPolyRegsWritten.set(1, reg1 & 0xFF);
    this.zxPolyRegsWritten.set(2, reg2 & 0xFF);
    this.zxPolyRegsWritten.set(3, reg3 & 0xFF);
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    final int result;
    if (this.board.isZxPolyMode()) {
      final int mappedModuleIndex = this.board.getMappedCpuIndex();

      if (module.isTrdosActive()) {
        result = -1;
      } else {
        if (module != this && this.moduleIndex > 0 && this.moduleIndex == mappedModuleIndex) {
          // reading memory for IO offset and make notification through INT
          result = this.board.readRam(module, getHeapRamOffset() + port);
          prepareLocalInt();
        } else {
          if (!isTrdosActive() && port == PORT_REG0) {
            final int cpuState = this.cpu.getState();
            final int addr = packAddress(this.lastM1Address);

            result = ((cpuState & Z80.SIGNAL_OUT_nHALT) == 0 ? ZXPOLY_rREG0_HALTMODE : 0)
                | (this.waitSignal ? ZXPOLY_rREG0_WAITMODE : 0) | (addr << 2);
          } else {
            result = -1;
          }
        }
      }
    } else {
      result = -1;
    }
    return result;
  }

  public boolean isActiveRegistersAsMemorySource() {
    return this.activeRegisterReading;
  }

  public boolean isTrdosActive() {
    return this.trdosRomActive;
  }

  public void setTrdosActive(final boolean active) {
    this.trdosRomActive = active;
  }

  @Override
  public void writeIo(final ZxPolyModule module, final int port, final int value) {
    if (this.board.isZxPolyMode()) {
      if (this.board.is3D00NotLocked()
          && module.moduleIndex <= this.moduleIndex
          && !module.isTrdosActive()
      ) {
        if (port == PORT_REG0) {
          this.zxPolyRegsWritten.set(0, value);
          if ((value & ZXPOLY_wREG0_RESET) != 0) {
            prepareLocalReset();
          }
          if ((value & ZXPOLY_wREG0_NMI) != 0) {
            prepareLocalNmi();
          }
          if ((value & ZXPOLY_wREG0_INT) != 0) {
            prepareLocalInt();
          }
        } else {
          if (port == PORT_REG1) {
            this.zxPolyRegsWritten.set(1, value);
          } else if (port == PORT_REG2) {
            this.zxPolyRegsWritten.set(2, value);
          } else if (port == PORT_REG3) {
            this.zxPolyRegsWritten.set(3, value);
          }
        }
      }
    }
  }

  public long getActiveMCyclesBetweenInt() {
    return Math.max(0L, this.mcyclesOfActivityBetweenInt);
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

  public void prepareLocalNmi() {
    this.localNmi = true;
  }

  public void prepareLocalInt() {
    this.localInt = true;
  }

  public boolean step(final boolean signalReset, final boolean commonInt, final boolean resetStatistic) {
    final boolean doInt;
    final boolean doNmi;

    if (resetStatistic) {
      this.mcyclesOfActivityBetweenInt = 0L;
    }

    if (this.moduleIndex == 0) {
      if (this.board.is3D00NotLocked() && !is7FFDLocked()) {
        doInt = (this.port7FFD.get() & PORTw_ZX128_INTCPU0) == 0 && (commonInt || this.localInt);
      } else {
        doInt = commonInt || this.localInt;
      }
    } else {
      doInt = (!this.activeRegisterReading && this.registerReadingCounter <= 0) && ((!this.board.is3D00NotLocked() && commonInt) || this.localInt);
    }
    this.localInt = false;

    doNmi = (this.zxPolyRegsWritten.get(1) & ZXPOLY_wREG1_DISABLE_NMI) == 0 && this.localNmi;
    this.localNmi = false;

    if (doInt) {
      if (this.intCounter == 0) {
        this.intCounter = INTERRUPTION_LENGTH_CYCLES;
      }
    } else {
      if (this.intCounter > 0) {
        this.intCounter--;
      }
    }

    if (doNmi) {
      if (this.nmiCounter == 0) {
        this.nmiCounter = INTERRUPTION_LENGTH_CYCLES;
      }
    } else {
      if (this.nmiCounter > 0) {
        this.nmiCounter--;
      }
    }

    final int sigReset = signalReset || (this.localResetCounter > 0) ? Z80.SIGNAL_IN_nRESET : 0;
    if (this.localResetCounter > 0) {
      this.localResetCounter--;
    }

    final int sigWait = this.waitSignal ? Z80.SIGNAL_IN_nWAIT : 0;

    final long curMcyclesNumber = this.cpu.getMachineCycles();

    final int oldCpuState = this.cpu.getState();
    this.cpu.step(Z80.SIGNAL_IN_ALL_INACTIVE ^ sigReset ^ (this.intCounter > 0 ? Z80.SIGNAL_IN_nINT : 0) ^ sigWait ^ (this.nmiCounter > 0 ? Z80.SIGNAL_IN_nNMI : 0));
    final int newCpuState = this.cpu.getState();

    final boolean isHaltDetected = (newCpuState & Z80.SIGNAL_OUT_nHALT) == 0 && (oldCpuState & Z80.SIGNAL_OUT_nHALT) != 0;

    final boolean cpuIsActive = (sigWait | sigReset) == 0 && !(isHaltDetected || doInt);

    this.mcyclesOfActivityBetweenInt += cpuIsActive ? Math.max(0L, this.cpu.getMachineCycles() - curMcyclesNumber) : -15000L;

    return isHaltDetected;
  }

  public boolean is7FFDLocked() {
    return (this.port7FFD.get() & PORTw_ZX128_LOCK) != 0;
  }

  public int readVideo(final int videoOffset) {
    final int moduleRamOffsetInHeap = getHeapOffset();

    final int result;
    if ((this.port7FFD.get() & PORTw_ZX128_SCREEN) == 0) {
      // RAM 5
      result = this.board.readRam(this, 0x14000 + moduleRamOffsetInHeap + videoOffset);
    } else {
      // RAM 7
      result = this.board.readRam(this, 0x1C000 + moduleRamOffsetInHeap + videoOffset);
    }
    return result;
  }

  public void writeHeap(final int offset, final int data) {
    if (offset < 0 || offset > 0x1FFFF) {
      throw new IllegalArgumentException("Outbound memory offset [" + offset + ']');
    }
    this.board.writeRam(this, getHeapOffset() + offset, data);
  }

  public int readHeap(final int offset) {
    if (offset < 0 || offset > 0x1FFFF) {
      throw new IllegalArgumentException("Outbound memory offset [" + offset + ']');
    }
    return this.board.readRam(this, getHeapOffset() + offset);
  }

  @Override
  public byte readAddress(final int address) {
    final int value7FFD = this.port7FFD.get();
    return memoryByteForAddress(value7FFD, address);
  }

  @Override
  public byte readMemory(final Z80 cpu, final int address, final boolean m1) {

    final int value7FFD = this.port7FFD.get();

    final boolean activeRom128 = (value7FFD & PORTw_ZX128_ROM) == 0;

    if (m1) {
      this.lastM1Address = address;

      final int address_h = address >>> 8;

      if (address_h == 0x3D && !activeRom128) {
        // turn on the TR-DOS ROM Section
        this.trdosRomActive = true;
      } else if (this.trdosRomActive && address_h >= 0x40) {
        // turn off the TR-DOS ROM Section
        this.trdosRomActive = false;
      }
    }

    final byte result;
    if (this.board.isZxPolyMode()) {
      if (m1 && this.board.is3D00NotLocked() && this.registerReadingCounter == 0) {
        final int moduleStopAddress = this.zxPolyRegsWritten.get(2) | (this.zxPolyRegsWritten.get(3) << 8);
        this.stopAddressWait = address != 0 && address == moduleStopAddress;
      }

      if (this.registerReadingCounter > 0 && !this.activeRegisterReading && m1 && address == 0) {
        this.activeRegisterReading = true;
      }

      if (this.activeRegisterReading) {
        // read local registers R1-R3 instead of memory
        result = (byte) this.zxPolyRegsWritten.get(4 - this.registerReadingCounter);
        this.registerReadingCounter--;
        if (this.registerReadingCounter == 0) {
          this.zxPolyRegsWritten.set(1, 0);
          this.zxPolyRegsWritten.set(2, 0);
          this.zxPolyRegsWritten.set(3, 0);
          this.activeRegisterReading = false;
        }
      } else {
        result = memoryByteForAddress(value7FFD, address);
      }
    } else {
      result = memoryByteForAddress(value7FFD, address);
    }
    return result;
  }

  private byte memoryByteForAddress(final int valueAt7FFD, final int address) {
    final int ramAddress = ramOffset2HeapAddress(address);

    final byte result;
    if (address < 0x4000) {
      if ((valueAt7FFD & PORTw_ZX128_ROMRAM) != 0) {
        //RAM0
        result = (byte) this.board.readRam(this, ramAddress);
      } else {
        if (this.trdosRomActive) {
          result = (byte) this.board.readRom(address + 0x8000);
        } else {
          final boolean activeRom128 = (valueAt7FFD & PORTw_ZX128_ROM) == 0;
          result = (byte) this.board.readRom(address + (activeRom128 ? 0x4000 : 0));
        }
      }
    } else {
      result = (byte) this.board.readRam(this, ramAddress);
    }
    return result;
  }

  public int ramOffset2HeapAddress(final int address) {
    final int page = (address >>> 14) & 3;
    final int offset = address & 0x3FFF;
    final int result;
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
        result = 0x4000 * (this.port7FFD.get() & 0x7);
      }
      break;
      default: {
        throw new IllegalArgumentException("Unexpected page index:" + page);
      }
    }
    return getHeapOffset() + (result | offset);
  }

  public String toHexStringSinceAddress(final int address, final int length) {
    final StringBuilder hex = new StringBuilder();
    final StringBuilder chars = new StringBuilder();

    for (int i = 0; i < length; i++) {
      final int b = readAddress(address + i) & 0xFF;
      final String h = Integer.toHexString(b).toUpperCase(Locale.ENGLISH);
      if (hex.length() > 0) {
        hex.append(' ');
      }
      if (h.length() == 1) {
        hex.append('0');
      }
      hex.append(h);

      if (!Character.isISOControl(b) && b <= 0x80) {
        chars.append((char) b);
      } else {
        chars.append('.');
      }
    }

    return Utils.toHex(address) + ' ' + hex + "  " + chars;
  }

  public List<DisasmLine> disasmSinceAddress(final int address, final int itemsToDecode) {
    final List<DisasmLine> result = new ArrayList<>();

    int addr = address;
    for (final Z80Instruction i : Z80Disasm.decodeList(this, null, address, itemsToDecode)) {
      result.add(new DisasmLine(addr, i == null ? null : i.decode(this, addr, addr)));
      addr += i == null ? 1 : i.getLength();
    }

    return result;
  }

  @Override
  public void writeMemory(final Z80 cpu, final int address, final byte data) {
    final int val = data & 0xFF;
    if (this.board.isZxPolyMode()) {
      final int reg0 = this.zxPolyRegsWritten.get(0);

      if ((reg0 & REG0w_MEMORY_WRITING_DISABLED) == 0) {
        final int ramOffsetInHeap = ramOffset2HeapAddress(address);

        if (address < 0x4000) {
          if (this.board.is3D00NotLocked() && (this.port7FFD.get() & PORTw_ZX128_ROMRAM) != 0) {
            //RAM0
            this.board.writeRam(this, ramOffsetInHeap, val);
          }
        } else {
          this.board.writeRam(this, ramOffsetInHeap, val);
        }
      }
    } else {
      final int ramOffsetInHeap = ramOffset2HeapAddress(address);
      if (address >= 0x4000) {
        // RAM AREA
        this.board.writeRam(this, ramOffsetInHeap, val);
      }
    }
  }

  @Override
  public byte readPort(final Z80 cpu, final int port) {
    final byte result;
    if (this.board.isZxPolyMode()) {
      if (port == PORTrw_ZXPOLY) {
        if (this.moduleIndex == 0 && this.board.getMappedCpuIndex() > 0) {
          result = (byte) this.board.readBusIo(this, port);
        } else {
          final int reg0 = zxPolyRegsWritten.get(0);
          final int outForCPU0 = this.moduleIndex == this.board.getMappedCpuIndex() ? PORTr_ZXPOLY_IOFORCPU0 : 0;
          final int memDisabled = (reg0 & ZXPOLY_wREG0_MEMWR_DISABLED) == 0 ? 0 : PORTr_ZXPOLY_MEMDISABLED;
          final int ioDisabled = (reg0 & ZXPOLY_wREG0_OUT_DISABLED) == 0 ? 0 : PORTr_ZXPOLY_IODISABLED;
          result = (byte) (this.moduleIndex | ((reg0 & 7) << 5) | outForCPU0 | memDisabled | ioDisabled);
        }
      } else {
        result = (byte) this.board.readBusIo(this, port);
      }
    } else {
      result = (byte) this.board.readBusIo(this, port);
    }
    return result;
  }

  public int read7FFD() {
    return this.port7FFD.get();
  }

  public void write7FFD(final int value, final boolean writeEvenIfLocked) {
    if (((this.port7FFD.get() & PORTw_ZX128_LOCK) == 0) || writeEvenIfLocked) {
      this.port7FFD.set(value);
    }
  }

  @Override
  public void writePort(final Z80 cpu, final int port, final byte data) {
    final int val = data & 0xFF;

    if (this.board.isZxPolyMode()) {
      final int reg0 = this.zxPolyRegsWritten.get(0);
      if ((reg0 & ZXPOLY_wREG0_OUT_DISABLED) == 0 || port == PORTw_ZX128) {
        if (port == PORTw_ZX128) {
          if (this.moduleIndex == 0) {
            if (this.board.getMappedCpuIndex() > 0 && (this.zxPolyRegsWritten.get(1) & ZXPOLY_wREG1_WRITE_MAPPED_IO_7FFD) != 0) {
              this.board.writeBusIo(this, port, val);
            } else {
              write7FFD(val, false);
            }
          } else {
            write7FFD(val, false);
          }
        } else {
          this.board.writeBusIo(this, port, val);
        }
      }
    } else {
      if (port == PORTw_ZX128) {
        write7FFD(val, false);
      } else {
        this.board.writeBusIo(this, port, val);
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
  public int getNotificationFlags() {
    return NOTIFICATION_PRESTEP;
  }

  @Override
  public void preStep(final boolean signalReset, final boolean signalInt) {
    if (signalReset) {
      setStateForSystemReset();
    }
    prepareWaitSignal();
  }

  @Override
  public void postStep(long spentMachineCyclesForStep) {
  }

  private void prepareWaitSignal() {
    if (this.board.isZxPolyMode()) {
      this.waitSignal = this.stopAddressWait || (this.moduleIndex > 0 && this.board.isSlaveModulesInWaitMode());
    } else {
      this.waitSignal = false;
    }
  }

  @Override
  public String getName() {
    return "ZXM#" + moduleIndex;
  }

  public int getReg1WrittenData() {
    return this.zxPolyRegsWritten.get(1);
  }

  private void setStateForSystemReset() {
    logger.info("Reset");
    this.intCounter = 0;
    this.nmiCounter = 0;
    this.port7FFD.set(0);
    this.trdosRomActive = false;
    this.registerReadingCounter = 0;
    this.activeRegisterReading = false;
    this.localResetCounter = 3;
    this.lastM1Address = 0;
    this.localInt = false;
    this.localNmi = false;

    for (int i = 0; i < this.zxPolyRegsWritten.length(); i++) {
      this.zxPolyRegsWritten.set(i, 0);
    }

    // set the intitial module memory offset in the heap
    this.zxPolyRegsWritten.set(0, this.moduleIndex << 1);
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

  public boolean isMaster() {
    return this.moduleIndex == 0;
  }

  public void lockZx48Mode() {
    this.port7FFD.set(0b00110000);
    this.trdosRomActive = false;
  }

  @Override
  public void doReset() {
    setStateForSystemReset();
    this.localResetCounter = 0;
    this.cpu.doReset();
  }
}
