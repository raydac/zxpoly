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
import com.igormaznitsa.zxpoly.formats.Spec256Arch;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.atomic.AtomicReference;
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
  private volatile boolean gfxPtrFromMainCpu = false;

  private final AtomicReference<RomData> romData = new AtomicReference<>();
  private boolean activeRegisterReading;
  private int registerReadingCounter = 0;

  private boolean localInt;
  private boolean localNmi;
  private boolean waitSignal;

  private boolean stopAddressWait;
  private int localResetCounter;

  private long mcyclesOfActivityBetweenInt;

  private volatile boolean trdosRomActive;

  private final byte[] gfxRam;
  private final byte[] gfxRom;

  private int gfxPort7FFD;
  private boolean gfxWaitSignal;
  private int gfxIntCounter;
  private int gfxNmiCounter;
  private boolean gfxTrDosRomActive;

  private static final int GFX_PAGE_SIZE = 0x4000 * 8;

  public ZxPolyModule(final Motherboard board, final RomData romData, final int index) {
    this.romData.set(Objects.requireNonNull(romData));
    this.board = Objects.requireNonNull(board);
    this.moduleIndex = index;

    this.PORT_REG0 = calcPortForRegister(index, 0);
    this.PORT_REG1 = calcPortForRegister(index, 1);
    this.PORT_REG2 = calcPortForRegister(index, 2);
    this.PORT_REG3 = calcPortForRegister(index, 3);

    this.cpu = new Z80(this);

    this.logger = Logger.getLogger("ZX#" + index);

    if (index == 0) {
      this.gfxRam = new byte[128 * 8 * 1024];
      this.gfxRom = new byte[32 * 8 * 1024];
    } else {
      this.gfxRam = null;
      this.gfxRom = null;
    }

    logger.info("Inited");
  }

  void saveInternalCopyForGfx() {
    this.gfxPort7FFD = this.port7FFD.get();
    this.gfxWaitSignal = this.waitSignal;
    this.gfxIntCounter = this.intCounter;
    this.gfxNmiCounter = this.nmiCounter;
    this.gfxTrDosRomActive = this.trdosRomActive;
  }

  public boolean isGfxPtrFromMainCpu() {
    return this.gfxPtrFromMainCpu;
  }

  public void setGfxPtrFromMainCpu(final boolean value) {
    this.gfxPtrFromMainCpu = value;
  }

  public RomData getRomData() {
    return this.romData.get();
  }

  public void setRomData(final RomData romData) {
    this.romData.set(Objects.requireNonNull(romData));
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

  public void fillPortByValues(final int port7ffd, final int reg0, final int reg1, final int reg2,
                               final int reg3) {
    this.port7FFD.set(port7ffd & 0xFF);
    this.zxPolyRegsWritten.set(0, reg0 & 0xFF);
    this.zxPolyRegsWritten.set(1, reg1 & 0xFF);
    this.zxPolyRegsWritten.set(2, reg2 & 0xFF);
    this.zxPolyRegsWritten.set(3, reg3 & 0xFF);
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    final int result;
    if (this.board.getBoardMode() == BoardMode.ZXPOLY) {
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
    if (this.board.getBoardMode() == BoardMode.ZXPOLY) {
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

  public boolean step(final boolean signalReset, final boolean commonInt,
                      final boolean resetStatistic) {
    final boolean doInt;
    final boolean doNmi;

    final int sigWait;
    final long curMcyclesNumber;
    final int sigReset;

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
      doInt = (!this.activeRegisterReading && this.registerReadingCounter <= 0) &&
          ((!this.board.is3D00NotLocked() && commonInt) || this.localInt);
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

    sigReset = signalReset || (this.localResetCounter > 0) ? Z80.SIGNAL_IN_nRESET : 0;
    if (this.localResetCounter > 0) {
      this.localResetCounter--;
    }

    sigWait = this.waitSignal ? Z80.SIGNAL_IN_nWAIT : 0;
    curMcyclesNumber = this.cpu.getStepTstates();

    final int oldCpuState = this.cpu.getState();
    this.cpu.step(0,
        Z80.SIGNAL_IN_ALL_INACTIVE ^ sigReset ^ (this.intCounter > 0 ? Z80.SIGNAL_IN_nINT : 0)
            ^ sigWait ^ (this.nmiCounter > 0 ? Z80.SIGNAL_IN_nNMI : 0));
    final int newCpuState = this.cpu.getState();

    final boolean isHaltDetected =
        (newCpuState & Z80.SIGNAL_OUT_nHALT) == 0 && (oldCpuState & Z80.SIGNAL_OUT_nHALT) != 0;

    final boolean cpuIsActive = (sigWait | sigReset) == 0 && !(isHaltDetected || doInt);

    this.mcyclesOfActivityBetweenInt +=
        cpuIsActive ? this.cpu.getStepTstates() : -15000L;
    return isHaltDetected;
  }

  public void stepWithGfxCpu(final int ctx, final Z80 gfxCpu, final boolean signalReset,
                             final boolean commonInt) {
    this.localInt = false;
    int sigReset = signalReset ? Z80.SIGNAL_IN_nRESET : 0;
    int sigWait = this.gfxWaitSignal ? Z80.SIGNAL_IN_nWAIT : 0;
    gfxCpu.step(ctx,
        Z80.SIGNAL_IN_ALL_INACTIVE ^ sigReset ^ (this.gfxIntCounter > 0 ? Z80.SIGNAL_IN_nINT : 0)
            ^ sigWait ^ (this.gfxNmiCounter > 0 ? Z80.SIGNAL_IN_nNMI : 0));
  }

  public boolean is7FFDLocked() {
    return (this.port7FFD.get() & PORTw_ZX128_LOCK) != 0;
  }

  public long readGfxVideo(final int videoOffset) {
    int offset;
    if ((this.gfxPort7FFD & PORTw_ZX128_SCREEN) == 0) {
      // RAM 5
      offset = 5 * GFX_PAGE_SIZE + (videoOffset << 3);
    } else {
      // RAM 7
      offset = 7 * GFX_PAGE_SIZE + (videoOffset << 3);
    }

    long result = 0L;
    for (int pixIndex = 0; pixIndex < 8; pixIndex++) {
      result <<= 8;
      int acc = 0;
      final int msk = 1 << (7 - pixIndex);
      for (int bitIndex = 0; bitIndex < 8; bitIndex++) {
        if ((this.gfxRam[offset + bitIndex] & msk) != 0) {
          acc |= (1 << bitIndex);
        }
      }
      result |= acc;
    }
    return result;
  }

  public long readGfxVideo16(final int videoOffset) {
    int offset;
    if ((this.gfxPort7FFD & PORTw_ZX128_SCREEN) == 0) {
      // RAM 5
      offset = 5 * GFX_PAGE_SIZE + (videoOffset << 3);
    } else {
      // RAM 7
      offset = 7 * GFX_PAGE_SIZE + (videoOffset << 3);
    }

    long result = 0L;
    for (int pixIndex = 0; pixIndex < 8; pixIndex++) {
      result <<= 5;
      int acc = 0;
      final int msk = 1 << (7 - pixIndex);
      for (int bitIndex = 0; bitIndex < 5; bitIndex++) {
        if ((this.gfxRam[offset + bitIndex] & msk) != 0) {
          acc |= (1 << bitIndex);
        }
      }
      result |= acc;
    }
    return result;
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

  public synchronized void syncWriteHeapPage(final int pageIndex, final byte[] data) {
    if (data.length != 0x4000) {
      throw new IllegalArgumentException("Page size must be 0x4000:" + data.length);
    }
    final int pageOffset = pageIndex * 0x4000;
    for (int i = 0; i < data.length; i++) {
      this.board.writeRam(this, this.getHeapOffset() + pageOffset + i, data[i]);
    }
  }

  public byte[] makeCopyOfRomPage(final int page) {
    return this.romData.get().makeCopyPage(page);
  }

  public byte[] makeCopyOfHeapPage(final int page) {
    final byte[] result = new byte[0x4000];
    final int offset = 0x4000 * page;
    for (int i = 0; i < 0x4000; i++) {
      result[i] = (byte) this.readHeap(i + offset);
    }
    return result;
  }

  public byte[] makeCopyOfZxMemPage(final int page) {
    final byte[] result = new byte[0x4000];
    final int offset = 0x4000 * page;
    for (int i = 0; i < 0x4000; i++) {
      result[i] = this.readAddress(i + offset);
    }
    return result;
  }

  public int readHeap(final int offset) {
    if (offset < 0 || offset > 0x1FFFF) {
      throw new IllegalArgumentException("Outbound memory offset [" + offset + ']');
    }
    return this.board.readRam(this, getHeapOffset() + offset);
  }

  @Override
  public byte readAddress(final int address) {
    return memoryByteForAddress(this.port7FFD.get(), this.trdosRomActive, address);
  }

  @Override
  public byte readMemory(final Z80 cpu, final int ctx, final int address, final boolean m1,
                         final boolean cmdOrPrefix) {
    final byte result;

    if (ctx == 0) {
      final int value7FFD = this.port7FFD.get();

      if (m1) {
        this.lastM1Address = address;
        final int address_h = address >>> 8;

        final boolean trdosEnabled = (value7FFD & PORTw_ZX128_ROM) != 0;

        if (this.trdosRomActive) {
          this.trdosRomActive = trdosEnabled && address_h < 0x40;
        } else {
          this.trdosRomActive = trdosEnabled && address_h == 0x3D;
        }
      }


      if (this.board.getBoardMode() == BoardMode.ZXPOLY) {
        if (m1 && this.board.is3D00NotLocked() && this.registerReadingCounter == 0) {
          final int moduleStopAddress =
              this.zxPolyRegsWritten.get(2) | (this.zxPolyRegsWritten.get(3) << 8);
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
          result = memoryByteForAddress(value7FFD, this.trdosRomActive, address);
        }
      } else {
        result = memoryByteForAddress(value7FFD, this.trdosRomActive, address);
      }
    } else {
      if (address < 0x4000) {
        // ROM area
        result = readGfxMemory(ctx - 1, this.gfxPort7FFD, this.gfxTrDosRomActive, address);
      } else if (cmdOrPrefix) {
        // read command from non-changed memory area
        result = memoryByteForAddress(this.gfxPort7FFD, this.gfxTrDosRomActive, address);
      } else {
        result = readGfxMemory(ctx - 1, this.gfxPort7FFD, this.gfxTrDosRomActive, address);
      }
    }
    return result;
  }

  public byte readGfxMemory(final int gfxCoreIndex,
                            final int valueAt7FFD,
                            final boolean trdosRomActive,
                            final int address) {
    final byte result;
    if (address < 0x4000) {
      if (trdosRomActive) {
        result = (byte) this.romData.get().readAdress(address + 0x8000);
      } else {
        result =
            this.gfxRom[((valueAt7FFD >> 4) & 1) * GFX_PAGE_SIZE + (address << 3) + gfxCoreIndex];
      }
    } else {
      final int page;
      final int offsetInPage;
      if (address < 0x8000) {
        page = 5;
        offsetInPage = address - 0x4000;
      } else if (address < 0xC000) {
        page = 2;
        offsetInPage = address - 0x8000;
      } else {
        page = valueAt7FFD & 7;
        offsetInPage = address - 0xC000;
      }
      result = this.gfxRam[page * GFX_PAGE_SIZE + (offsetInPage << 3) + gfxCoreIndex];
    }
    return result;
  }

  public void makeCopyOfRomToGfxRom() {
    final byte[] data = this.romData.get().getAsArray();
    int offst = 0;
    for (int i = 0; i < 0x8000 && i < data.length; i++) {
      final int value = data[i] & 0xFF;
      for (int j = 0; j < 8; j++) {
        this.gfxRom[offst++] = (byte) value;
      }
    }
  }

  public void writeGfxRomPage(final Spec256Arch.Spec256GfxPage page) {
    synchronized (this.gfxRom) {
      int startOffset = page.getPageIndex() * GFX_PAGE_SIZE;
      final byte[] data = page.getGfxData();
      for (int i = 0; i < data.length; i++) {
        this.gfxRom[startOffset + i] = data[i];
      }
    }
  }

  public void writeGfxRamPage(final Spec256Arch.Spec256GfxPage page) {
    synchronized (this.gfxRam) {
      int startOffset = page.getPageIndex() * GFX_PAGE_SIZE;
      for (byte gfxPageDatum : page.getGfxData()) {
        this.gfxRam[startOffset++] = (byte) gfxPageDatum;
      }
    }
  }

  public void writeGfxMemory(final int gfxCoreIndex,
                             final int valueAt7FFD,
                             final int address,
                             final int value) {
    if (address >= 0x4000) {
      final int page;
      final int offsetInPage;
      if (address < 0x8000) {
        page = 5;
        offsetInPage = address - 0x4000;
      } else if (address < 0xC000) {
        page = 2;
        offsetInPage = address - 0x8000;
      } else {
        page = valueAt7FFD & 7;
        offsetInPage = address - 0xC000;
      }
      final int ramHeapAddr = page * GFX_PAGE_SIZE + (offsetInPage << 3) + gfxCoreIndex;
      this.gfxRam[ramHeapAddr] = (byte) value;
    }
  }

  private byte memoryByteForAddress(
      final int valueAt7FFD,
      final boolean trDosActive,
      final int address) {
    final int ramAddress = ramOffset2HeapAddress(valueAt7FFD, address);

    final byte result;
    if (address < 0x4000) {
      if ((valueAt7FFD & PORTw_ZX128_ROMRAM) != 0) {
        //RAM0
        result = (byte) this.board.readRam(this, ramAddress);
      } else {
        if (trDosActive) {
          result = (byte) this.romData.get().readAdress(address + 0x8000);
        } else {
          result =
              (byte) this.romData.get().readAdress(address + ((valueAt7FFD >> 4) & 1) * 0x4000);
        }
      }
    } else {
      result = (byte) this.board.readRam(this, ramAddress);
    }
    return result;
  }

  public int ramOffset2HeapAddress(final int value7FFD, final int address) {
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
        result = 0x4000 * (value7FFD & 0x7);
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

  public void poke(final int page, final int offset, final int value) {
    final int addr = this.getHeapOffset() + page * 0x4000 + (offset & 0x3FFF);
    this.board.writeRam(this, addr, value);
  }

  @Override
  public void writeMemory(final Z80 cpu, final int ctx, final int address, final byte data) {
    final int val = data & 0xFF;
    if (ctx == 0) {
      final int value7FFD = this.port7FFD.get();
      if (this.board.getBoardMode() == BoardMode.ZXPOLY) {
        final int reg0 = this.zxPolyRegsWritten.get(0);

        if ((reg0 & REG0w_MEMORY_WRITING_DISABLED) == 0) {
          final int ramOffsetInHeap = ramOffset2HeapAddress(value7FFD, address);

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
        final int ramOffsetInHeap = ramOffset2HeapAddress(value7FFD, address);
        if (address >= 0x4000) {
          // RAM AREA
          this.board.writeRam(this, ramOffsetInHeap, val);
        }
      }
    } else {
      this.writeGfxMemory(ctx - 1, this.gfxPort7FFD, address, val);
    }
  }

  @Override
  public int readPtr(Z80 cpu, int ctx, int reg, int valueInReg) {
    if (ctx != 0 && this.gfxPtrFromMainCpu) {
      final Z80 mainCpu = this.cpu;
      switch (reg) {
        case Z80.REG_SP:
          return mainCpu.getSP();
        case Z80.REG_IX:
        case Z80.REG_IY:
          return mainCpu.getRegister(reg, false);
        case Z80.REGPAIR_BC:
        case Z80.REGPAIR_DE:
        case Z80.REGPAIR_HL:
          return mainCpu.getRegisterPair(reg, false);
        default:
          return valueInReg;
      }
    } else {
      return valueInReg;
    }
  }

  @Override
  public int readSpecRegValue(Z80 cpu, int ctx, int reg, int origValue) {
    if (ctx != 0 && this.gfxPtrFromMainCpu) {
      final Z80 mainCpu = this.cpu;
      return mainCpu.getRegister(reg, false);
    } else {
      return origValue;
    }
  }

  @Override
  public int readSpecRegPairValue(Z80 cpu, int ctx, int regPair, int origValue) {
    if (ctx != 0 && this.gfxPtrFromMainCpu) {
      final Z80 mainCpu = this.cpu;
      return mainCpu.getRegisterPair(regPair, false);
    } else {
      return origValue;
    }
  }

  @Override
  public int readRegPortAddr(Z80 cpu, int ctx, int reg, int valueInReg) {
    if (ctx == 0) {
      return valueInReg;
    } else {
      switch (reg) {
        case Z80.REG_A:
          return this.cpu.getRegister(Z80.REG_A);
        case Z80.REGPAIR_BC:
          return this.cpu.getRegisterPair(Z80.REGPAIR_BC);
        default:
          return valueInReg;
      }
    }
  }

  @Override
  public byte readPort(final Z80 cpu, final int ctx, final int port) {
    final byte result;
    if (ctx == 0) {
      if (this.board.getBoardMode() == BoardMode.ZXPOLY) {
        if (port == PORTrw_ZXPOLY) {
          if (this.moduleIndex == 0 && this.board.getMappedCpuIndex() > 0) {
            result = (byte) this.board.readBusIo(this, port);
          } else {
            final int reg0 = zxPolyRegsWritten.get(0);
            final int outForCPU0 =
                this.moduleIndex == this.board.getMappedCpuIndex() ? PORTr_ZXPOLY_IOFORCPU0 : 0;
            final int memDisabled =
                (reg0 & ZXPOLY_wREG0_MEMWR_DISABLED) == 0 ? 0 : PORTr_ZXPOLY_MEMDISABLED;
            final int ioDisabled =
                (reg0 & ZXPOLY_wREG0_OUT_DISABLED) == 0 ? 0 : PORTr_ZXPOLY_IODISABLED;
            result = (byte) (this.moduleIndex | ((reg0 & 7) << 5) | outForCPU0 | memDisabled |
                ioDisabled);
          }
        } else {
          result = (byte) this.board.readBusIo(this, port);
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
  public void writePort(final Z80 cpu, final int ctx, final int port, final byte data) {
    if (ctx == 0) {
      final int val = data & 0xFF;
      if (this.board.getBoardMode() == BoardMode.ZXPOLY) {
        final int reg0 = this.zxPolyRegsWritten.get(0);
        if ((reg0 & ZXPOLY_wREG0_OUT_DISABLED) == 0 || port == PORTw_ZX128) {
          if (port == PORTw_ZX128) {
            if (this.moduleIndex == 0) {
              if (this.board.getMappedCpuIndex() > 0 &&
                  (this.zxPolyRegsWritten.get(1) & ZXPOLY_wREG1_WRITE_MAPPED_IO_7FFD) != 0) {
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
        if ((port & 0x8002) == 0) { // only A15 and A1 in use to detect #7FFD in non ZX-Poly mode
          write7FFD(val, false);
        } else {
          this.board.writeBusIo(this, port, val);
        }
      }
    }
  }

  @Override
  public byte onCPURequestDataLines(final Z80 cpu, final int ctx) {
    return (byte) 0xFF;
  }

  @Override
  public void onRETI(final Z80 cpu, final int ctx) {
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_PRESTEP;
  }

  @Override
  public void preStep(final boolean signalReset, final boolean tstatesIntReached,
                      boolean wallclockInt) {
    if (signalReset) {
      setStateForSystemReset();
    }
    prepareWaitSignal();
  }

  @Override
  public void postStep(int spentTstates) {
  }

  private void prepareWaitSignal() {
    if (this.board.getBoardMode() == BoardMode.ZXPOLY) {
      this.waitSignal =
          this.stopAddressWait || (this.moduleIndex > 0 && this.board.isSlaveModulesInWaitMode());
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
