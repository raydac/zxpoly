/* 
 * Copyright (C) 2014 Igor Maznitsa (http://www.igormaznitsa.com)
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
package com.igormaznitsa.z80;

import static com.igormaznitsa.z80.Z80.FLAG_C;
import static com.igormaznitsa.z80.Z80.REG_F;
import java.util.Arrays;
import java.util.Locale;

/**
 * Tables and some flag set algorithms were copied and adapted from
 * https://github.com/anotherlin/z80emu project, opcode decoding is based on
 * http://www.z80.info/decoding.htm
 */
public final class Z80 {

  private static final byte[] FTABLE_SZYX;
  private static final byte[] FTABLE_SZYXP;

  private static final int FLAG_S_SHIFT = 7;
  public static final int FLAG_S = 1 << FLAG_S_SHIFT;
  private static final int FLAG_Z_SHIFT = 6;
  public static final int FLAG_Z = 1 << FLAG_Z_SHIFT;
  private static final int FLAG_Y_SHIFT = 5;
  public static final int FLAG_Y = 1 << FLAG_Y_SHIFT;
  private static final int FLAG_H_SHIFT = 4;
  public static final int FLAG_H = 1 << FLAG_H_SHIFT;
  private static final int FLAG_X_SHIFT = 3;
  public static final int FLAG_X = 1 << FLAG_X_SHIFT;
  private static final int FLAG_PV_SHIFT = 2;
  public static final int FLAG_PV = 1 << FLAG_PV_SHIFT;
  private static final int FLAG_N_SHIFT = 1;
  public static final int FLAG_N = 1 << FLAG_N_SHIFT;
  private static final int FLAG_C_SHIFT = 0;
  public static final int FLAG_C = 1 << FLAG_C_SHIFT;

  private static final int FLAG_XY = FLAG_X | FLAG_Y;
  private static final int FLAG_SZPV = FLAG_S | FLAG_Z | FLAG_PV;
  private static final int FLAG_SYX = FLAG_S | FLAG_X | FLAG_Y;
  private static final int FLAG_SZ = FLAG_S | FLAG_Z;
  private static final int FLAG_SZC = FLAG_SZ | FLAG_C;
  private static final int FLAG_HC = FLAG_H | FLAG_C;

  private static final byte[] FTABLE_OVERFLOW = new byte[]{
    0, (byte) FLAG_PV, (byte) FLAG_PV, 0
  };

  static {
    // fill tables SZYX and SZYXP
    FTABLE_SZYX = new byte[256];
    FTABLE_SZYXP = new byte[256];
    for (int i = 0; i < 256; i++) {
      FTABLE_SZYX[i] = (byte) ((i & (FLAG_S | FLAG_XY)) | (i == 0 ? FLAG_Z : 0));

      boolean p = true;
      int j = i;
      while (j != 0) {
        if ((j & 1) != 0) {
          p = !p;
        }
        j >>= 1;
      }
      FTABLE_SZYXP[i] = (byte) ((i & (FLAG_S | FLAG_XY)) | (i == 0 ? FLAG_Z : 0) | (p ? FLAG_PV : 0));
    }
  }

  public static final int REG_A = 0;
  public static final int REG_F = 1;
  public static final int REG_B = 2;
  public static final int REG_C = 3;
  public static final int REG_D = 4;
  public static final int REG_E = 5;
  public static final int REG_H = 6;
  public static final int REG_L = 7;
  public static final int REG_IX = 8;
  public static final int REG_IY = 9;
  public static final int REG_SP = 10;
  public static final int REG_PC = 11;
  public static final int REG_I = 12;
  public static final int REG_R = 13;

  public static final int REGPAIR_AF = REG_A;
  public static final int REGPAIR_BC = REG_B;
  public static final int REGPAIR_DE = REG_D;
  public static final int REGPAIR_HL = REG_H;

  public static final int SIGNAL_IN_nINT = 1;
  public static final int SIGNAL_IN_nNMI = 2;
  public static final int SIGNAL_IN_nRESET = 4;
  public static final int SIGNAL_IN_nWAIT = 8;
  public static final int SIGNAL_IN_ALL_INACTIVE = SIGNAL_IN_nINT | SIGNAL_IN_nNMI | SIGNAL_IN_nRESET | SIGNAL_IN_nWAIT;

  public static final int SIGNAL_OUT_nM1 = 1;
  public static final int SIGNAL_OUT_nHALT = 2;
  public static final int SIGNAL_OUT_ALL_INACTIVE = SIGNAL_OUT_nHALT | SIGNAL_OUT_nM1;

  private boolean iff1, iff2;
  private int im;

  private final Z80CPUBus bus;

  private int regIX;
  private int regIY;
  private int regSP;
  private int regPC;
  private int regI;
  private int regR;
  private int regW;
  private int regZ;
  private int regWalt;
  private int regZalt;

  private long machineCycles;

  private final byte[] regSet = new byte[8];
  private final byte[] altRegSet = new byte[8];

  private int lastM1InstructionByte = -1;
  private int lastInstructionByte = -1;

  private int cbDisplacementByte = -1;

  private int prefix;

  private int outSignals = 0xFFFFFFFF;
  private int prevINSignals = 0xFFFFFFFF;

  private boolean interruptAllowedForStep;
  private boolean detectedNMI;
  private boolean detectedINT;
  private boolean insideBlockInstructionPrev;
  private boolean insideBlockInstruction;

  private int resetCycle = 0;

  /**
   * Make full copy of state of the source CPU. NB! pointer to bus will be
   * copied!
   *
   * @param cpu source CPU which state should be copied, must not be null
   */
  public Z80(final Z80 cpu) {
    this.iff1 = cpu.iff1;
    this.iff2 = cpu.iff2;
    this.im = cpu.im;
    this.regI = cpu.regI;
    this.regIX = cpu.regIX;
    this.regIY = cpu.regIY;
    this.regPC = cpu.regPC;
    this.regR = cpu.regR;
    this.regSP = cpu.regSP;
    this.regW = cpu.regW;
    this.regZ = cpu.regZ;
    this.regWalt = cpu.regWalt;
    this.regZalt = cpu.regZalt;
    System.arraycopy(cpu.regSet, 0, this.regSet, 0, cpu.regSet.length);
    System.arraycopy(cpu.altRegSet, 0, this.altRegSet, 0, cpu.altRegSet.length);
    this.lastM1InstructionByte = cpu.lastM1InstructionByte;
    this.lastInstructionByte = cpu.lastInstructionByte;
    this.machineCycles = cpu.machineCycles;
    this.cbDisplacementByte = cpu.cbDisplacementByte;
    this.outSignals = cpu.outSignals;
    this.prevINSignals = cpu.prevINSignals;
    this.interruptAllowedForStep = cpu.interruptAllowedForStep;
    this.detectedINT = cpu.detectedINT;
    this.detectedNMI = cpu.detectedNMI;
    this.insideBlockInstruction = cpu.insideBlockInstruction;
    this.insideBlockInstructionPrev = cpu.insideBlockInstructionPrev;
    this.bus = cpu.bus;
  }

  public Z80(final Z80CPUBus bus) {
    if (bus == null) {
      throw new NullPointerException("The CPU BUS must not be null");
    }
    this.bus = bus;
    _reset(0);
    _reset(1);
    _reset(2);
    this.machineCycles = 3L;
  }

  public int getWZ(final boolean alt) {
    return alt ? (this.regWalt << 8) | this.regZalt : (this.regW << 8) | this.regZ;
  }

  public void setWZ(final int value, final boolean alt) {
    if (alt) {
      this.regWalt = (value >> 8) & 0xFF;
      this.regZalt = value & 0xFF;
    } else {
      this.regW = (value >> 8) & 0xFF;
      this.regZ = value & 0xFF;
    }
  }

  public int getIM() {
    return this.im;
  }

  public void setIM(final int im) {
    this.im = im & 3;
  }

  public boolean isIFF1() {
    return this.iff1;
  }

  public boolean isIFF2() {
    return this.iff2;
  }

  public int getPrefixInProcessing() {
    return this.prefix;
  }

  public int getPrevINSignals() {
    return this.prevINSignals;
  }

  public int getPC() {
    return this.getRegister(REG_PC);
  }

  public int getSP() {
    return this.getRegister(REG_SP);
  }

  public void setIFF(final boolean iff1, final boolean iff2) {
    this.iff1 = iff1;
    this.iff2 = iff2;
  }

  public void setRegisterPair(final int regPair, final int value) {
    this.setRegisterPair(regPair, value, false);
  }

  public void setRegisterPair(final int regPair, final int value, final boolean alt) {
    if (alt) {
      this.altRegSet[regPair] = (byte) (value >>> 8);
      this.altRegSet[regPair + 1] = (byte) value;
    } else {
      this.regSet[regPair] = (byte) (value >>> 8);
      this.regSet[regPair + 1] = (byte) value;
    }
  }

  public int getRegisterPair(final int regPair) {
    return this.getRegisterPair(regPair, false);
  }

  public int getRegisterPair(final int regPair, final boolean alt) {
    final byte[] regset = alt ? altRegSet : regSet;
    return ((regset[regPair] & 0xFF) << 8) | (regset[regPair + 1] & 0xFF);
  }

  public void setRegister(final int reg, final int value) {
    this.setRegister(reg, value, false);
  }

  public void setRegister(final int reg, final int value, final boolean alt) {
    switch (reg) {
      case REG_IX:
        this.regIX = value & 0xFFFF;
        break;
      case REG_IY:
        this.regIY = value & 0xFFFF;
        break;
      case REG_PC:
        this.regPC = value & 0xFFFF;
        break;
      case REG_SP:
        this.regSP = value & 0xFFFF;
        break;
      case REG_I:
        this.regI = value & 0xFF;
        break;
      case REG_R:
        this.regR = value & 0xFF;
        break;
      default: {
        if (alt) {
          this.altRegSet[reg] = (byte) value;
        } else {
          this.regSet[reg] = (byte) value;
        }
      }
    }
  }

  public int getRegister(final int reg) {
    return this.getRegister(reg, false);
  }

  public int getRegister(final int reg, final boolean alt) {
    final byte[] regset = alt ? altRegSet : regSet;
    final int result;
    switch (reg) {
      case REG_IX:
        result = this.regIX;
        break;
      case REG_IY:
        result = this.regIY;
        break;
      case REG_PC:
        result = this.regPC;
        break;
      case REG_SP:
        result = this.regSP;
        break;
      case REG_I:
        result = this.regI;
        break;
      case REG_R:
        result = this.regR;
        break;
      default: {
        result = regset[reg] & 0xFF;
      }
    }
    return result;
  }

  public int getState() {
    return this.outSignals;
  }

  public Z80CPUBus getBus() {
    return this.bus;
  }

  public long getMachineCycles() {
    return this.machineCycles;
  }

  public void resetMCycleCounter() {
    this.machineCycles = 0L;
  }

  public boolean isInsideBlockLoop() {
    return this.insideBlockInstruction;
  }

  private void _reset(final int cycle) {
    switch (cycle % 3) {
      case 0: {
        this.iff1 = false;
        this.iff2 = false;
        this.regI = 0;
        this.regR = 0;
      }
      break;
      case 1: {
        this.regPC = 0;
        this.regSP = 0;
      }
      break;
      case 2: {
        // set AF and AF' by 0xFFFF
        this.regSet[REG_A] = (byte) 0xFF;
        this.regSet[REG_F] = (byte) 0xFF;
        this.altRegSet[REG_A] = (byte) 0xFF;
        this.altRegSet[REG_F] = (byte) 0xFF;
      }
      break;
      default: {
        throw new Error("Unexpected call");
      }
    }

    this.im = 0;
    this.cbDisplacementByte = -1;

    this.insideBlockInstruction = false;
    this.insideBlockInstructionPrev = false;

    this.interruptAllowedForStep = false;

    this.prefix = 0;
    this.outSignals = SIGNAL_OUT_ALL_INACTIVE;

    this.machineCycles += 3;
  }

  private void _resetHalt() {
    if ((this.outSignals & SIGNAL_OUT_nHALT) == 0) {
      this.outSignals |= SIGNAL_OUT_nHALT;
      this.regPC = (this.regPC + 1) & 0xFFFF;
    }
  }

  private void _int() {
    _resetHalt();

    this.iff1 = false;
    this.iff2 = false;

    this.insideBlockInstructionPrev = this.insideBlockInstruction;

    this.detectedINT = false;

    switch (this.im) {
      case 0: {
        _step(this.bus.onCPURequestDataLines(this) & 0xFF);
      }
      break;
      case 1: {
        _step(0xFF);
      }
      break;
      case 2: {
        final int address = _readmem16(((this.regI & 0xFF) << 8) | (this.bus.onCPURequestDataLines(this) & 0xFF));
        _call(address);
        this.machineCycles++;
      }
      break;
      default:
        throw new Error("Unexpected IM mode [" + this.im + ']');
    }

    this.machineCycles += 6;
  }

  private void _rfsh() {
    final int rreg = this.getRegister(REG_R);
    this.setRegister(REG_R, (rreg & 0x80) | ((rreg + 1) & 0x7F));
  }

  private void _call(final int address) {
    final int sp = (this.regSP - 2) & 0xFFFF;
    _writemem8(sp, (byte) this.regPC);
    _writemem8(sp + 1, (byte) (this.regPC >> 8));
    this.regPC = address;
    this.regSP = sp;
  }

  private void _nmi() {
    _resetHalt();
    this.insideBlockInstructionPrev = this.insideBlockInstruction;
    this.iff1 = false;
    this.detectedNMI = false;
    this.detectedINT = false;
    _call(0x66);
    this.machineCycles += 5;
  }

  private void _writemem8(final int address, final byte value) {
    this.bus.writeMemory(this, address & 0xFFFF, value);
    this.machineCycles += 3;
  }

  private int _read16_for_pc() {
    return readInstructionByte(false) | (readInstructionByte(false) << 8);
  }

  private void _writemem16(final int address, final int value) {
    this._writemem8(address, (byte) value);
    this._writemem8(address + 1, (byte) (value >> 8));
  }

  private int _readport(final int port) {
    this.machineCycles += 4;
    return this.bus.readPort(this, port & 0xFFFF) & 0xFF;
  }

  private void _writeport(final int port, final int value) {
    this.bus.writePort(this, port & 0xFFFF, (byte) value);
    this.machineCycles += 4;
  }

  private int _readmem8(final int address) {
    this.machineCycles += 3;
    return this.bus.readMemory(this, address & 0xFFFF, false) & 0xFF;
  }

  private int _readmem16(final int address) {
    return _readmem8(address) | (_readmem8(address + 1) << 8);
  }

  private int _read_ixiy_d() {
    if (this.cbDisplacementByte < 0) {
      return readInstructionByte(false);
    } else {
      this.machineCycles -= 5;
      return this.cbDisplacementByte;
    }
  }

  private int readInstructionByte(final boolean m1) {
    final int pc = this.regPC;
    this.regPC = (this.regPC + 1) & 0xFFFF;
    this.outSignals = (m1 ? this.outSignals & (~SIGNAL_OUT_nM1) : this.outSignals | SIGNAL_OUT_nM1) & 0xFF;
    final int result = this.bus.readMemory(this, pc, m1) & 0xFF;
    this.outSignals = this.outSignals | SIGNAL_OUT_nM1;

    this.machineCycles += m1 ? 4 : 3;

    if (m1) {
      this.lastM1InstructionByte = result;
    }
    
    this.lastInstructionByte = result;
    
    return result;
  }

  private static int extractX(final int cmndByte) {
    return cmndByte >>> 6;
  }

  private static int extractY(final int cmndByte) {
    return (cmndByte >>> 3) & 7;
  }

  private static int extractZ(final int cmndByte) {
    return cmndByte & 7;
  }

  private static int extractP(final int cmndByte) {
    return (cmndByte >>> 4) & 3;
  }

  private static int extractQ(final int cmndByte) {
    return (cmndByte >>> 3) & 1;
  }

  private int normalizedPrefix() {
    return (this.prefix & 0xFF) == 0xCB ? this.prefix >>> 8 : this.prefix;
  }

  private boolean checkCondition(final int cc) {
    final boolean result;
    final int flags = this.regSet[REG_F];
    switch (cc) {
      case 0:
        result = (flags & FLAG_Z) == 0;
        break;
      case 1:
        result = (flags & FLAG_Z) != 0;
        break;
      case 2:
        result = (flags & FLAG_C) == 0;
        break;
      case 3:
        result = (flags & FLAG_C) != 0;
        break;
      case 4:
        result = (flags & FLAG_PV) == 0;
        break;
      case 5:
        result = (flags & FLAG_PV) != 0;
        break;
      case 6:
        result = (flags & FLAG_S) == 0;
        break;
      case 7:
        result = (flags & FLAG_S) != 0;
        break;
      default:
        throw new Error("Unexpected condition");
    }
    return result;
  }

  private int readReg8(final int r) {
    switch (r) {
      case 0:
        return getRegister(REG_B);
      case 1:
        return getRegister(REG_C);
      case 2:
        return getRegister(REG_D);
      case 3:
        return getRegister(REG_E);
      case 4: { // H
        switch (normalizedPrefix()) {
          case 0x00:
            return getRegister(REG_H);
          case 0xDD:
            return (this.regIX >> 8) & 0xFF;
          case 0xFD:
            return (this.regIY >> 8) & 0xFF;
        }
      }
      break;
      case 5: { // L
        switch (normalizedPrefix()) {
          case 0x00:
            return getRegister(REG_L);
          case 0xDD:
            return this.regIX & 0xFF;
          case 0xFD:
            return this.regIY & 0xFF;
        }
      }
      break;
      case 6: { // (HL)
        switch (normalizedPrefix()) {
          case 0x00: {
            return _readmem8(getRegisterPair(REGPAIR_HL));
          }
          case 0xDD: {
            this.machineCycles += 5;
            final int address = this.regIX + (byte) _read_ixiy_d();
            this.setWZ(address, false);
            return _readmem8(address);
          }
          case 0xFD: {
            this.machineCycles += 5;
            final int address = this.regIY + (byte) _read_ixiy_d();
            this.setWZ(address, false);
            return _readmem8(address);
          }
        }
      }
      break;
      case 7:
        return getRegister(REG_A);
    }
    throw new Error("Unexpected prefix or R index [" + this.prefix + ':' + r + ']');
  }

  private void writeReg16(final int p, final int value) {
    switch (p) {
      case 0:
        setRegisterPair(REGPAIR_BC, value);
        return;
      case 1:
        setRegisterPair(REGPAIR_DE, value);
        return;
      case 2: {
        switch (normalizedPrefix()) {
          case 0x00:
            setRegisterPair(REGPAIR_HL, value);
            return;
          case 0xDD:
            setRegister(REG_IX, value);
            return;
          case 0xFD:
            setRegister(REG_IY, value);
            return;
        }
      }
      break;
      case 3:
        setRegister(REG_SP, value);
        return;
    }
    throw new Error("unexpected P index or prefix [" + this.prefix + ':' + p + ']');
  }

  private void writeReg16_2(final int p, final int value) {
    switch (p) {
      case 0:
        setRegisterPair(REGPAIR_BC, value);
        return;
      case 1:
        setRegisterPair(REGPAIR_DE, value);
        return;
      case 2: {
        switch (normalizedPrefix()) {
          case 0x00:
            setRegisterPair(REGPAIR_HL, value);
            return;
          case 0xDD:
            setRegister(REG_IX, value);
            return;
          case 0xFD:
            setRegister(REG_IY, value);
            return;
        }
      }
      break;
      case 3:
        setRegisterPair(REGPAIR_AF, value);
        return;
    }
    throw new Error("unexpected P index or prefix [" + this.prefix + ':' + p + ']');
  }

  private int readReg16(final int p) {
    switch (p) {
      case 0:
        return getRegisterPair(REGPAIR_BC);
      case 1:
        return getRegisterPair(REGPAIR_DE);
      case 2: {
        switch (normalizedPrefix()) {
          case 0x00:
            return getRegisterPair(REGPAIR_HL);
          case 0xDD:
            return getRegister(REG_IX);
          case 0xFD:
            return getRegister(REG_IY);
        }
      }
      break;
      case 3:
        return getRegister(REG_SP);
    }
    throw new Error("Unexpected P index or prefix [" + this.prefix + ':' + p + ']');
  }

  private int readReg16_2(final int p) {
    switch (p) {
      case 0:
        return getRegisterPair(REGPAIR_BC);
      case 1:
        return getRegisterPair(REGPAIR_DE);
      case 2: {
        switch (normalizedPrefix()) {
          case 0x00:
            return getRegisterPair(REGPAIR_HL);
          case 0xDD:
            return getRegister(REG_IX);
          case 0xFD:
            return getRegister(REG_IY);
        }
      }
      break;
      case 3:
        return getRegisterPair(REGPAIR_AF);
    }
    throw new Error("Unexpected P index or prefix [" + this.prefix + ':' + p + ']');
  }

  private void writeReg8(final int r, final int value) {
    switch (r) {
      case 0:
        setRegister(REG_B, value);
        return;
      case 1:
        setRegister(REG_C, value);
        return;
      case 2:
        setRegister(REG_D, value);
        return;
      case 3:
        setRegister(REG_E, value);
        return;
      case 4:
        if (this.cbDisplacementByte < 0) {
          switch (normalizedPrefix()) {
            case 0x00:
              setRegister(REG_H, value);
              break;
            case 0xDD: {
              this.regIX = (this.regIX & 0xFF) | ((value & 0xFF) << 8);
            }
            break;
            case 0xFD: {
              this.regIY = (this.regIY & 0xFF) | ((value & 0xFF) << 8);
            }
            break;
          }
        } else {
          setRegister(REG_H, value);
        }
        return;
      case 5:
        if (this.cbDisplacementByte < 0) {
          switch (normalizedPrefix()) {
            case 0x00:
              setRegister(REG_L, value);
              break;
            case 0xDD: {
              this.regIX = (this.regIX & 0xFF00) | (value & 0xFF);
            }
            break;
            case 0xFD: {
              this.regIY = (this.regIY & 0xFF00) | (value & 0xFF);
            }
            break;
          }
        } else {
          setRegister(REG_L, value);
        }
        return;
      case 6: { // (HL)
        switch (normalizedPrefix()) {
          case 0x00: {
            _writemem8(getRegisterPair(REGPAIR_HL), (byte) value);
            return;
          }
          case 0xDD: {
            final int address = this.regIX + (byte) value;
            _writemem8(address, (byte) readInstructionByte(false));
            this.setWZ(address, false);
            this.machineCycles += 2;
            return;
          }
          case 0xFD: {
            final int address = this.regIY + (byte) value;
            _writemem8(address, (byte) readInstructionByte(false));
            this.setWZ(address, false);
            this.machineCycles += 2;
            return;
          }
        }
      }
      break;
      case 7:
        setRegister(REG_A, value);
        return;
    }
    throw new Error("unexpected P index or prefix [" + this.prefix + ':' + r + ']');
  }

  private void writeReg8_forLdReg8Instruction(final int r, final int value) {
    switch (r) {
      case 0:
        setRegister(REG_B, value);
        return;
      case 1:
        setRegister(REG_C, value);
        return;
      case 2:
        setRegister(REG_D, value);
        return;
      case 3:
        setRegister(REG_E, value);
        return;
      case 4:
        switch (normalizedPrefix()) {
          case 0x00:
            setRegister(REG_H, value);
            return;
          case 0xDD:
            setRegister(REG_IX, (value << 8) | (getRegister(REG_IX) & 0x00FF));
            return;
          case 0xFD:
            setRegister(REG_IY, (value << 8) | (getRegister(REG_IY) & 0x00FF));
            return;
        }
        break;
      case 5:
        switch (normalizedPrefix()) {
          case 0x00:
            setRegister(REG_L, value);
            return;
          case 0xDD:
            setRegister(REG_IX, (getRegister(REG_IX) & 0xFF00) | (value & 0xFF));
            return;
          case 0xFD:
            setRegister(REG_IY, (getRegister(REG_IY) & 0xFF00) | (value & 0xFF));
            return;
        }
        break;
      case 6: { // (HL)
        switch (normalizedPrefix()) {
          case 0x00:
            _writemem8(getRegisterPair(REGPAIR_HL), (byte) value);
            return;
          case 0xDD:
            _writemem8(this.regIX + (byte) readInstructionByte(false), (byte) value);
            this.machineCycles += 5;
            return;
          case 0xFD:
            _writemem8(this.regIY + (byte) readInstructionByte(false), (byte) value);
            this.machineCycles += 5;
            return;
        }
      }
      break;
      case 7:
        setRegister(REG_A, value);
        return;
    }
    throw new Error("unexpected P index or prefix [" + this.prefix + ':' + r + ']');
  }

  private void writeReg8_UseCachedInstructionByte(final int r, final int value) {
    switch (r) {
      case 0:
        setRegister(REG_B, value);
        return;
      case 1:
        setRegister(REG_C, value);
        return;
      case 2:
        setRegister(REG_D, value);
        return;
      case 3:
        setRegister(REG_E, value);
        return;
      case 4:
        if (this.cbDisplacementByte < 0) {
          switch (normalizedPrefix()) {
            case 0x00:
              setRegister(REG_H, value);
              break;
            case 0xDD: {
              this.regIX = (this.regIX & 0xFF) | ((value & 0xFF) << 8);
            }
            break;
            case 0xFD: {
              this.regIY = (this.regIY & 0xFF) | ((value & 0xFF) << 8);
            }
            break;
          }
        } else {
          setRegister(REG_H, value);
        }
        return;
      case 5:
        if (this.cbDisplacementByte < 0) {
          switch (normalizedPrefix()) {
            case 0x00:
              setRegister(REG_L, value);
              break;
            case 0xDD: {
              this.regIX = (this.regIX & 0xFF00) | (value & 0xFF);
            }
            break;
            case 0xFD: {
              this.regIY = (this.regIY & 0xFF00) | (value & 0xFF);
            }
            break;
          }
        } else {
          setRegister(REG_L, value);
        }
        return;
      case 6: { // (HL)
        this.machineCycles += 1;
        switch (normalizedPrefix()) {
          case 0x00:
            _writemem8(getRegisterPair(REGPAIR_HL), (byte) value);
            return;
          case 0xDD:
            _writemem8(this.regIX + (byte) (this.cbDisplacementByte < 0 ? this.lastInstructionByte : this.cbDisplacementByte), (byte) value);
            return;
          case 0xFD:
            _writemem8(this.regIY + (byte) (this.cbDisplacementByte < 0 ? this.lastInstructionByte : this.cbDisplacementByte), (byte) value);
            return;
        }
      }
      break;
      case 7:
        setRegister(REG_A, value);
        return;
    }
    throw new Error("unexpected P index or prefix [" + this.prefix + ':' + r + ']');
  }

  public int getLastM1InstructionByte() {
    return this.lastM1InstructionByte;
  }

  public int getLastInstructionByte() {
    return this.lastInstructionByte;
  }

  private boolean isHiLoFront(final int signal, final int signals) {
    return (((this.prevINSignals & signal) ^ (signals & signal)) & (signal & this.prevINSignals)) != 0;
  }

  /**
   * Process whole instruction or send signals but only step of a block
   * instruction will be processed.
   *
   * @param signalRESET true sends the RESET signal to the CPU
   * @param signalNMI true sends the NMI signal to the CPU
   * @param signalNT true sends the INT signal to the CPU
   */
  public void nextInstruction(final boolean signalRESET, final boolean signalNMI, final boolean signalNT) {
    int flag = (signalNT ? 0 : SIGNAL_IN_nINT) | (signalNMI ? 0 : SIGNAL_IN_nNMI) | (signalRESET ? 0 : SIGNAL_IN_nRESET) | SIGNAL_IN_nWAIT;
    while (step(flag)) {
      flag = SIGNAL_IN_ALL_INACTIVE;
    }
  }

  /**
   * Process whole instruction or send signals and block operations will be
   * processed entirely.
   *
   * @param signalRESET true sends the RESET signal to the CPU
   * @param signalNMI true sends the NMI signal to the CPU
   * @param signalNT true sends the INT signal to the CPU
   */
  public void nextInstruction_SkipBlockInstructions(final boolean signalRESET, final boolean signalNMI, final boolean signalNT) {
    int flag = (signalNT ? 0 : SIGNAL_IN_nINT) | (signalNMI ? 0 : SIGNAL_IN_nNMI) | (signalRESET ? 0 : SIGNAL_IN_nRESET) | SIGNAL_IN_nWAIT;
    while (step(flag) || this.insideBlockInstruction) {
      flag = SIGNAL_IN_ALL_INACTIVE;
    }
  }

  /**
   * Process one step.
   *
   * @param inSignals external signal states to be processes during the step.
   * @return false if there is not any instruction under processing, true
   * otherwise
   */
  public boolean step(final int inSignals) {
    try {
      final boolean result;
      this.interruptAllowedForStep = true;

      if ((inSignals & SIGNAL_IN_nWAIT) == 0) {
        // PROCESS nWAIT
        this.machineCycles++;
        result = this.prefix != 0;
      } else if ((inSignals & SIGNAL_IN_nRESET) == 0) {
        // START RESET
        _reset(this.resetCycle++);
        result = false;
      } else {
        // Process command
        if (_step(readInstructionByte(true))) {
          // Command completed
          this.prefix = 0;
          result = false;

          if (this.interruptAllowedForStep) {
            // Check interruptions
            if ((inSignals & SIGNAL_IN_nNMI) == 0) {
              // NMI
              _nmi();
            } else if (this.iff1 && (inSignals & SIGNAL_IN_nINT) == 0) {
              // INT
              _int();
            }
          }
        } else {
          result = true;
        }
      }

      return result;
    }
    finally {
      this.prevINSignals = inSignals;
    }
  }

  private boolean _step(final int commandByte) {
    this.lastInstructionByte = commandByte;

    boolean commandCompleted = true;
    this.insideBlockInstruction = false;

    switch (this.prefix) {
      case 0xDD:
      case 0xFD:
      case 0x00: {
        _rfsh();

        switch (extractX(commandByte)) {
          case 0: {
            final int z = extractZ(commandByte);
            switch (z) {
              case 0: {
                final int y = extractY(commandByte);
                switch (y) {
                  case 0:
                    doNOP();
                    break;
                  case 1:
                    doEX_AF_AF();
                    break;
                  case 2:
                    doDJNZ();
                    break;
                  case 3:
                    doJR();
                    break;
                  default:
                    doJR(y - 4);
                    break;
                }
              }
              break;
              case 1: {
                final int p = extractP(commandByte);
                if (extractQ(commandByte) == 0) {
                  doLDRegPairByNextWord(p);
                } else {
                  doADD_HL_RegPair(p);
                }
              }
              break;
              case 2: {
                if (extractQ(commandByte) == 0) {
                  switch (extractP(commandByte)) {
                    case 0:
                      doLD_mBC_A();
                      break;
                    case 1:
                      doLD_mDE_A();
                      break;
                    case 2:
                      doLD_mNN_HL();
                      break;
                    default:
                      doLD_mNN_A();
                      break;
                  }
                } else {
                  switch (extractP(commandByte)) {
                    case 0:
                      doLD_A_mBC();
                      break;
                    case 1:
                      doLD_A_mDE();
                      break;
                    case 2:
                      doLD_HL_mem();
                      break;
                    default:
                      doLD_A_mem();
                      break;
                  }
                }
              }
              break;
              case 3: {
                final int p = extractP(commandByte);
                if (extractQ(commandByte) == 0) {
                  doINCRegPair(p);
                } else {
                  doDECRegPair(p);
                }
              }
              break;
              case 4:
                doINCReg(extractY(commandByte));
                break;
              case 5:
                doDECReg(extractY(commandByte));
                break;
              case 6:
                doLD_Reg_ByValue(extractY(commandByte));
                break;
              case 7:
                switch (extractY(commandByte)) {
                  case 0:
                    doRLCA();
                    break;
                  case 1:
                    doRRCA();
                    break;
                  case 2:
                    doRLA();
                    break;
                  case 3:
                    doRRA();
                    break;
                  case 4:
                    doDAA();
                    break;
                  case 5:
                    doCPL();
                    break;
                  case 6:
                    doSCF();
                    break;
                  default:
                    doCCF();
                    break;
                }
                break;
            }
          }
          break;
          case 1: {
            final int z = extractZ(commandByte);
            final int y = extractY(commandByte);
            if (z == 6 && y == 6) {
              doHalt();
            } else {
              doLDRegByReg(y, z);
            }
          }
          break;
          case 2: {
            doALU_A_Reg(extractY(commandByte), extractZ(commandByte));
          }
          break;
          case 3: {
            switch (extractZ(commandByte)) {
              case 0:
                doRETByFlag(extractY(commandByte));
                break;
              case 1: {
                final int p = extractP(commandByte);
                if (extractQ(commandByte) == 0) {
                  doPOPRegPair(p);
                } else {
                  switch (p) {
                    case 0:
                      doRET();
                      break;
                    case 1:
                      doEXX();
                      break;
                    case 2:
                      doJP_HL();
                      break;
                    default:
                      doLD_SP_HL();
                      break;
                  }
                }
              }
              break;
              case 2:
                doJP_cc(extractY(commandByte));
                break;
              case 3: {
                switch (extractY(commandByte)) {
                  case 0:
                    doJP();
                    break;
                  case 1:
                    this.prefix = (this.prefix << 8) | 0xCB;
                    this.cbDisplacementByte = -1;
                    commandCompleted = false;
                    break;
                  case 2:
                    doOUTnA();
                    break;
                  case 3:
                    doIN_A_n();
                    break;
                  case 4:
                    doEX_mSP_HL();
                    break;
                  case 5:
                    doEX_DE_HL();
                    break;
                  case 6:
                    doDI();
                    break;
                  default:
                    doEI();
                    break;
                }
              }
              break;
              case 4: {
                doCALL(extractY(commandByte));
              }
              break;
              case 5: {
                if (extractQ(commandByte) == 0) {
                  doPUSH(extractP(commandByte));
                } else {
                  switch (extractP(commandByte)) {
                    case 0:
                      doCALL();
                      break;
                    case 1: {
                      this.prefix = 0xDD;
                      commandCompleted = false;
                    }
                    break;
                    case 2: {
                      this.prefix = 0xED;
                      commandCompleted = false;
                    }
                    break;
                    default: {
                      this.prefix = 0xFD;
                      commandCompleted = false;
                    }
                    break;
                  }
                }
              }
              break;
              case 6:
                doALU_A_n(extractY(commandByte));
                break;
              case 7:
                doRST(extractY(commandByte) << 3);
                break;
              default:
                throw new Error("Unexpected X");
            }
          }
          break;
        }
      }
      break;
      case 0xFDCB:
      case 0xDDCB: {
        if (this.cbDisplacementByte < 0) {
          this.cbDisplacementByte = commandByte;
          commandCompleted = false;
        } else {

          final int z = extractZ(commandByte);
          final int y = extractY(commandByte);

          switch (extractX(commandByte)) {
            case 0: {
              if (z == 6) {
                doRollShift(y, z);
              } else {
                doROTmem_LDreg(z, y);
              }
            }
            break;
            case 1: {
              doBIT(y, z);
            }
            break;
            case 2: {
              if (z == 6) {
                doRES(y, z);
              } else {
                doRESmem_LDreg(z, y);
              }
            }
            break;
            default: {
              if (z == 6) {
                doSET(y, z);
              } else {
                doSETmem_LDreg(z, y);
              }
            }
            break;
          }
          this.prefix = 0;
          this.cbDisplacementByte = -1;
        }
      }
      break;
      case 0xCB: {
        final int y = extractY(commandByte);
        final int z = extractZ(commandByte);
        switch (extractX(commandByte)) {
          case 0:
            doRollShift(y, z);
            break;
          case 1:
            doBIT(y, z);
            break;
          case 2:
            doRES(y, z);
            break;
          default:
            doSET(y, z);
            break;
        }
        this.prefix = 0;
      }
      break;
      case 0xED: {
        if (commandByte == 0xCB) {
          this.prefix = 0xEDCB;
        } else {
          this.prefix = 0;
          switch (extractX(commandByte)) {
            case 0:
            case 3:
              doNONI();
              break;
            case 1: {
              switch (extractZ(commandByte)) {
                case 0: {
                  final int y = extractY(commandByte);
                  if (y == 6) {
                    doIN_C();
                  } else {
                    doIN_C(y);
                  }
                }
                break;
                case 1: {
                  final int y = extractY(commandByte);
                  if (y == 6) {
                    doOUT_C();
                  } else {
                    doOUT_C(y);
                  }
                }
                break;
                case 2: {
                  final int p = extractP(commandByte);
                  if (extractQ(commandByte) == 0) {
                    doSBC_HL_RegPair(p);
                  } else {
                    doADC_HL_RegPair(p);
                  }
                }
                break;
                case 3: {
                  final int p = extractP(commandByte);
                  if (extractQ(commandByte) == 0) {
                    doLD_mNN_RegP(p);
                  } else {
                    doLD_RegP_mNN(p);
                  }
                }
                break;
                case 4:
                  doNEG();
                  break;
                case 5: {
                  if (extractY(commandByte) == 1) {
                    doRETI();
                  } else {
                    doRETN();
                  }
                }
                break;
                case 6:
                  doIM(extractY(commandByte));
                  break;
                case 7: {
                  switch (extractY(commandByte)) {
                    case 0:
                      doLD_I_A();
                      break;
                    case 1:
                      doLD_R_A();
                      break;
                    case 2:
                      doLD_A_I();
                      break;
                    case 3:
                      doLD_A_R();
                      break;
                    case 4:
                      doRRD();
                      break;
                    case 5:
                      doRLD();
                      break;
                    default:
                      doNOP();
                      break;
                  }
                }
                break;
                default:
                  throw new Error("Unexpected Z");
              }
            }
            break;
            case 2: {
              final int z = extractZ(commandByte);
              final int y = extractY(commandByte);
              if (z <= 3 && y >= 4) {
                this.insideBlockInstruction = doBLI(y, z);
              } else {
                doNONI();
              }
            }
            break;
            default:
              throw new Error("Unexpected X");
          }
        }
      }
      break;
      default:
        throw new Error("Illegal prefix state [0x" + Integer.toHexString(this.prefix).toUpperCase(Locale.ENGLISH) + ']');
    }

    return commandCompleted;
  }

  private void doNONI() {
    this.prefix = 0;
    this.interruptAllowedForStep = false;
  }

  private void doHalt() {
    this.outSignals &= (~SIGNAL_OUT_nHALT & 0xFF);
    this.regPC--;
  }

  private void doNOP() {
  }

  private void doEX_AF_AF() {
    byte temp = this.regSet[REG_A];
    this.regSet[REG_A] = this.altRegSet[REG_A];
    this.altRegSet[REG_A] = temp;
    temp = this.regSet[REG_F];
    this.regSet[REG_F] = this.altRegSet[REG_F];
    this.altRegSet[REG_F] = temp;
  }

  private void doDJNZ() {
    final int offset = (byte) readInstructionByte(false);
    this.machineCycles++;
    int b = this.regSet[REG_B] & 0xFF;
    if (--b != 0) {
      this.regPC = (this.regPC + offset) & 0xFFFF;
      this.machineCycles += 5;
    }
    this.regSet[REG_B] = (byte) b;
  }

  private void doJR(final int cc) {
    final int offset = (byte) readInstructionByte(false);
    if (checkCondition(cc)) {
      this.regPC = (this.regPC + offset) & 0xFFFF;
      this.machineCycles += 5;
    }
  }

  private void doJR() {
    final int offset = (byte) readInstructionByte(false);
    this.regPC = (this.regPC + offset) & 0xFFFF;
    this.setWZ(this.regPC, false);
    this.machineCycles += 5;
  }

  private void doLDRegPairByNextWord(final int p) {
    final int value = _read16_for_pc();
    writeReg16(p, value);
  }

  private void doADD_HL_RegPair(final int p) {
    final int reg = readReg16(2);
    final int value = readReg16(p);
    final int result = reg + value;
    this.setWZ(reg + 1, iff1);
    writeReg16(2, result);

    final int c = reg ^ value ^ result;
    this.regSet[REG_F] = (byte) ((this.regSet[REG_F] & FLAG_SZPV) | ((result >>> 8) & FLAG_XY) | ((c >>> 8) & FLAG_H) | ((c >>> (16 - FLAG_C_SHIFT))));

    this.machineCycles += 7;
  }

  private void doADC_HL_RegPair(final int p) {
    final int x = readReg16(2);
    final int y = readReg16(p);

    final int z = x + y + (this.regSet[REG_F] & FLAG_C);
    this.setWZ(x + 1, false);

    int c = x ^ y ^ z;
    int f = (z & 0xffff) != 0 ? (z >> 8) & FLAG_SYX : FLAG_Z;

    f |= (c >>> 8) & FLAG_H;
    f |= FTABLE_OVERFLOW[c >>> 15];
    f |= z >>> (16 - FLAG_C_SHIFT);

    this.regSet[REG_F] = (byte) f;
    writeReg16(2, z);

    this.machineCycles += 7;
  }

  private void doSBC_HL_RegPair(final int p) {
    final int x = readReg16(2);
    final int y = readReg16(p);

    final int z = x - y - (this.regSet[REG_F] & FLAG_C);
    int c = x ^ y ^ z;

    this.setWZ(x + 1, iff1);

    int f = FLAG_N;
    f |= (z & 0xffff) != 0 ? (z >>> 8) & FLAG_SYX : FLAG_Z;

    f |= (c >>> 8) & FLAG_H;
    c &= 0x018000;
    f |= FTABLE_OVERFLOW[c >>> 15];
    f |= c >>> (16 - FLAG_C_SHIFT);

    writeReg16(2, z);
    this.regSet[REG_F] = (byte) f;

    this.machineCycles += 7;
  }

  private void doLD_mBC_A() {
    final int address = getRegisterPair(REGPAIR_BC);
    final byte a = this.regSet[REG_A];
    this.regW = a & 0xFF;
    this.regZ = (address + 1) & 0xFF;
    this._writemem8(address, a);
  }

  private void doLD_mDE_A() {
    final int address = getRegisterPair(REGPAIR_DE);
    final byte a = this.regSet[REG_A];
    this.regW = a & 0xFF;
    this.regZ = (address + 1) & 0xFF;
    this._writemem8(address, a);
  }

  private void doLD_mNN_HL() {
    final int addr = _read16_for_pc();
    _writemem16(addr, readReg16(2));
    this.setWZ(addr + 1, false);
  }

  private void doLD_mNN_A() {
    final int address = _read16_for_pc();
    final byte a = this.regSet[REG_A];
    this.regW = a & 0xFF;
    this.regZ = (address + 1) & 0xFF;
    this._writemem8(address, a);
  }

  private void doLD_A_mBC() {
    final int addr = getRegisterPair(REGPAIR_BC);
    setRegister(REG_A, _readmem8(addr));
    this.setWZ(addr + 1, false);
  }

  private void doLD_A_mDE() {
    final int addr = getRegisterPair(REGPAIR_DE);
    setRegister(REG_A, _readmem8(getRegisterPair(REGPAIR_DE)));
    this.setWZ(addr + 1, false);
  }

  private void doLD_HL_mem() {
    final int addr = _readmem16(_read16_for_pc());
    writeReg16(2, addr);
    this.setWZ(addr + 1, false);
  }

  private void doLD_A_mem() {
    final int address = _read16_for_pc();
    setRegister(REG_A, _readmem8(address));
    this.setWZ(address + 1, false);
  }

  private void doINCRegPair(final int p) {
    writeReg16(p, readReg16(p) + 1);
    this.machineCycles += 2;
  }

  private void doDECRegPair(final int p) {
    writeReg16(p, readReg16(p) - 1);
    this.machineCycles += 2;
  }

  private void doINCReg(final int y) {
    final int x = readReg8(y);

    int z = x + 1;
    int c = x ^ z;

    int f = this.regSet[REG_F] & FLAG_C;
    f |= (c & FLAG_H);
    f |= FTABLE_SZYX[z & 0xff];
    f |= FTABLE_OVERFLOW[(c >>> 7) & 0x03];

    writeReg8_UseCachedInstructionByte(y, z);
    this.regSet[REG_F] = (byte) f;
  }

  private void doDECReg(final int y) {
    final int x = readReg8(y);

    final int z = x - 1;
    final int c = x ^ z;

    writeReg8_UseCachedInstructionByte(y, z);

    int f = FLAG_N | (this.regSet[REG_F] & FLAG_C);
    f |= (c & FLAG_H);
    f |= FTABLE_SZYX[z & 0xff];
    f |= FTABLE_OVERFLOW[(c >>> 7) & 0x03];

    this.regSet[REG_F] = (byte) f;
  }

  private void doLD_Reg_ByValue(final int y) {
    writeReg8(y, readInstructionByte(false));
  }

  private void doRLCA() {
    int a = this.regSet[REG_A] & 0xFF;
    a = (a << 1) | (a >>> 7);
    this.regSet[REG_F] = (byte) ((this.regSet[REG_F] & FLAG_SZPV) | (a & (FLAG_XY | FLAG_C)));
    this.regSet[REG_A] = (byte) a;
  }

  private void doRRCA() {
    int a = this.regSet[REG_A] & 0xFF;
    int c = a & FLAG_C;
    a = (a >>> 1) | (a << 7);
    this.regSet[REG_F] = (byte) ((this.regSet[REG_F] & FLAG_SZPV) | (a & FLAG_XY) | c);
    this.regSet[REG_A] = (byte) a;
  }

  private void doRLA() {
    final int A = this.regSet[REG_A] & 0xFF;
    final int a = A << 1;
    int f = (this.regSet[REG_F] & FLAG_SZPV) | (a & FLAG_XY) | (A >>> 7);
    this.regSet[REG_A] = (byte) (a | (this.regSet[REG_F] & FLAG_C));
    this.regSet[REG_F] = (byte) f;
  }

  private void doRRA() {
    int A = this.regSet[REG_A] & 0xFF;
    int c;
    c = A & 0x01;
    A = (A >> 1) | ((regSet[REG_F] & FLAG_C) << 7);
    this.regSet[REG_F] = (byte) ((this.regSet[REG_F] & FLAG_SZPV) | (A & FLAG_XY) | c);
    this.regSet[REG_A] = (byte) A;
  }

  private void doDAA() {
    int a = this.regSet[REG_A] & 0xFF;
    int flags = this.regSet[REG_F];
    final int c;
    int d;
    if (a > 0x99 || (flags & FLAG_C) != 0) {
      c = FLAG_C;
      d = 0x60;
    } else {
      c = d = 0;
    }
    if ((a & 0x0f) > 0x09 || (flags & FLAG_H) != 0) {
      d += 0x06;
    }
    final int newa = (a + ((flags & FLAG_N) == 0 ? +d : -d)) & 0xFF;
    this.regSet[REG_A] = (byte) newa;
    this.regSet[REG_F] = (byte) (FTABLE_SZYXP[newa] | ((newa ^ a) & FLAG_H) | (this.regSet[REG_F] & FLAG_N) | c);
  }

  private void doCPL() {
    int A = this.regSet[REG_A] & 0xFF;
    A = ~A;
    this.regSet[REG_A] = (byte) A;
    this.regSet[REG_F] = (byte) ((this.regSet[REG_F] & (FLAG_SZPV | FLAG_C)) | (A & FLAG_XY) | FLAG_H | FLAG_N);
  }

  private void doSCF() {
    int A = this.regSet[REG_A] & 0xFF;
    this.regSet[REG_F] = (byte) ((this.regSet[REG_F] & FLAG_SZPV) | (A & FLAG_XY) | FLAG_C);
  }

  private void doCCF() {
    int A = this.regSet[REG_A] & 0xFF;
    int c = this.regSet[REG_F] & FLAG_C;
    this.regSet[REG_F] = (byte) ((this.regSet[REG_F] & FLAG_SZPV) | (c << FLAG_H_SHIFT) | (A & FLAG_XY) | (c ^ FLAG_C));
  }

  private void doLDRegByReg(final int y, final int z) {
    if (z == 6 || y == 6) {
      // process (HL),(IXd),(IYd)
      if (y == 6) {
        final int oldprefix = this.prefix;
        this.prefix = 0;
        final int value = readReg8(z);
        this.prefix = oldprefix;
        writeReg8_forLdReg8Instruction(y, value);
      } else {
        final int value = readReg8(z);
        this.prefix = 0;
        writeReg8_forLdReg8Instruction(y, value);
      }
    } else {
      writeReg8_forLdReg8Instruction(y, readReg8(z));
    }
  }

  private void doRETByFlag(final int y) {
    if (checkCondition(y)) {
      final int sp = this.regSP;
      int sp1 = sp + 1;
      this.regPC = _readmem8(sp) | (_readmem8(sp1++) << 8);
      this.setWZ(this.regPC, false);
      this.regSP = sp1 & 0xFFFF;
    }
    this.machineCycles++;
  }

  private void doRET() {
    final int sp = this.regSP;
    int sp1 = sp + 1;
    this.regPC = _readmem8(sp) | (_readmem8(sp1++) << 8);
    this.setWZ(this.regPC, false);
    this.regSP = sp1 & 0xFFFF;
  }

  private void doPOPRegPair(final int p) {
    final int address = getRegister(REG_SP);
    writeReg16_2(p, _readmem16(address));
    setRegister(REG_SP, address + 2);
  }

  private void doEXX() {
    for (int i = REG_B; i < REG_IX; i++) {
      final byte b = this.regSet[i];
      this.regSet[i] = this.altRegSet[i];
      this.altRegSet[i] = b;
    }
    int w = this.regW;
    int z = this.regZ;
    this.regW = this.regWalt;
    this.regZ = this.regZalt;
    this.regWalt = w;
    this.regZalt = z;
  }

  private void doJP_HL() {
    this.regPC = readReg16(2);
  }

  private void doLD_SP_HL() {
    final int value;
    switch (normalizedPrefix()) {
      case 0x00:
        value = getRegisterPair(REGPAIR_HL);
        break;
      case 0xDD:
        value = getRegister(REG_IX);
        break;
      case 0xFD:
        value = getRegister(REG_IY);
        break;
      default:
        throw new Error("Unexpected prefix for LD SP,HL [" + normalizedPrefix() + ']');
    }
    this.machineCycles += 2;
    setRegister(REG_SP, value);

  }

  private void doJP_cc(final int cc) {
    final int address = _read16_for_pc();
    if (checkCondition(cc)) {
      this.regPC = address;
    }
    this.setWZ(address, false);
  }

  private void doJP() {
    this.regPC = _read16_for_pc();
    this.setWZ(this.regPC, false);
  }

  private void doOUTnA() {
    final int n = readInstructionByte(false);
    final int a = this.regSet[REG_A] & 0xFF;
    _writeport((a << 8) | n, a);
  }

  private void doIN_A_n() {
    this.regSet[REG_A] = (byte) _readport(((this.regSet[REG_A] & 0xFF) << 8) | readInstructionByte(false));
  }

  private void doEX_mSP_HL() {
    final int stacktop = this.regSP;
    final int hl = readReg16(2);
    final int value = _readmem8(stacktop) | (_readmem8(stacktop + 1) << 8);
    this.setWZ(value, false);
    writeReg16(2, value);
    _writemem8(stacktop, (byte) hl);
    _writemem8(stacktop + 1, (byte) (hl >> 8));

    this.machineCycles += 3;
  }

  private void doEX_DE_HL() {
    byte tmp = this.regSet[REG_D];
    this.regSet[REG_D] = this.regSet[REG_H];
    this.regSet[REG_H] = tmp;
    tmp = this.regSet[REG_E];
    this.regSet[REG_E] = this.regSet[REG_L];
    this.regSet[REG_L] = tmp;
  }

  private void doDI() {
    this.iff1 = false;
    this.iff2 = false;
    this.interruptAllowedForStep = false;
    this.insideBlockInstruction = false;
    this.insideBlockInstructionPrev = false;
    this.detectedINT = false;
  }

  private void doEI() {
    this.iff1 = true;
    this.iff2 = true;
    this.interruptAllowedForStep = false;
    this.insideBlockInstruction = false;
    this.insideBlockInstructionPrev = false;
    this.detectedINT = false;
  }

  private void doCALL(final int y) {
    final int address = _read16_for_pc();
    if (checkCondition(y)) {
      _call(address);
      this.machineCycles++;
    }
    this.setWZ(address, false);
  }

  private void doPUSH(final int p) {
    final int address = getRegister(REG_SP) - 2;
    _writemem16(address, readReg16_2(p));
    setRegister(REG_SP, address);
    this.machineCycles++;
  }

  private void doCALL() {
    final int address = _read16_for_pc();
    this.setWZ(address, false);
    _call(address);
    this.machineCycles++;
  }

  private void doALU_A_Reg(final int op, final int reg) {
    _aluAccumulatorOp(op, readReg8(reg));
  }

  private void doALU_A_n(final int op) {
    _aluAccumulatorOp(op, readInstructionByte(false));
  }

  private void _aluAccumulatorOp(final int op, final int value) {
    final int a = this.regSet[REG_A] & 0xFF;
    final int flagc = this.regSet[REG_F] & FLAG_C;

    final int result;

    int f;

    switch (op) {
      case 0: { // ADD
        int z = a + value;
        int c = a ^ value ^ z;
        f = c & FLAG_H;
        f |= FTABLE_SZYX[z & 0xff];
        f |= FTABLE_OVERFLOW[c >>> 7];
        f |= z >>> (8 - FLAG_C_SHIFT);
        result = z;
      }
      break;
      case 1: { // ADC
        int z = a + value + flagc;
        int c = a ^ value ^ z;
        f = c & FLAG_H;
        f |= FTABLE_SZYX[z & 0xff];
        f |= FTABLE_OVERFLOW[c >>> 7];
        f |= z >>> (8 - FLAG_C_SHIFT);
        result = z;
      }
      break;
      case 2: { // SUB
        int z = a - value;
        int c = a ^ value ^ z;
        f = FLAG_N | (c & FLAG_H);
        f |= FTABLE_SZYX[z & 0xff];
        c &= 0x0180;
        f |= FTABLE_OVERFLOW[c >>> 7];
        f |= c >>> (8 - FLAG_C_SHIFT);
        result = z;
      }
      break;
      case 3: { // SBC
        int z = a - value - flagc;
        int c = a ^ value ^ z;
        f = FLAG_N | (c & FLAG_H);
        f |= FTABLE_SZYX[z & 0xff];
        c &= 0x0180;
        f |= FTABLE_OVERFLOW[c >>> 7];
        f |= c >>> (8 - FLAG_C_SHIFT);
        result = z;
      }
      break;
      case 4: { // AND
        result = a & value;
        f = FTABLE_SZYXP[result] | FLAG_H;
      }
      break;
      case 5: { // XOR
        result = a ^ value;
        f = FTABLE_SZYXP[result];
      }
      break;
      case 6: { // OR
        result = a | value;
        f = FTABLE_SZYXP[result];
      }
      break;
      case 7: { // CP
        int z = a - value;

        int c = a ^ value ^ z;
        f = FLAG_N | (c & FLAG_H);
        f |= FTABLE_SZYX[z & 0xff] & FLAG_SZ;
        f |= value & FLAG_XY;
        c &= 0x0180;
        f |= FTABLE_OVERFLOW[c >>> 7];
        f |= c >>> (8 - FLAG_C_SHIFT);

        result = a;
      }
      break;
      default:
        throw new Error("Detected unexpected ALU operation [" + op + ']');
    }
    this.regSet[REG_A] = (byte) result;
    this.regSet[REG_F] = (byte) f;
  }

  private void doRST(final int address) {
    _call(address);
    this.setWZ(address, false);
    this.machineCycles++;
  }

  private int doRollShift(final int op, final int reg) {
    int x = readReg8(reg);
    final int origc = this.regSet[REG_F] & FLAG_C;
    int c = origc;
    switch (op) {
      case 0: { // RLC
        c = (x >>> 7) & FLAG_C;
        x = (x << 1) | c;
      }
      break;
      case 1: { // RRC
        c = x & 0x01;
        x = (x >>> 1) | (c << 7);
      }
      break;
      case 2: { // RL
        c = x >>> 7;
        x = (x << 1) | origc;
      }
      break;
      case 3: { // RR
        c = x & 0x01;
        x = (x >>> 1) | (origc << 7);
      }
      break;
      case 4: { // SLA
        c = x >>> 7;
        x <<= 1;
      }
      break;
      case 5: { // SRA
        c = x & 0x01;
        x = (x & 0x80) | (x >>> 1);
      }
      break;
      case 6: { // SLL
        c = x >>> 7;
        x = (x << 1) | 0x01;
      }
      break;
      case 7: { // SRL
        c = x & 0x01;
        x >>>= 1;
      }
      break;
      default:
        throw new Error("Unexpected operation index [" + op + ']');
    }
    writeReg8_UseCachedInstructionByte(reg, x);
    this.regSet[REG_F] = (byte) (FTABLE_SZYXP[x & 0xFF] | c);
    return x;
  }

  private void doROTmem_LDreg(final int reg, final int op) {
    writeReg8(reg, doRollShift(op, 6));
  }

  private void doBIT(final int bit, final int reg) {
    final int val = readReg8(reg);
    final int x = val & (1 << bit);

    this.regSet[REG_F] = (byte) ((x == 0 ? (FLAG_Z | FLAG_PV) : 0) | (x & FLAG_S) | (this.regW & FLAG_XY) | FLAG_H | (this.regSet[REG_F] & FLAG_C));

    if (reg == 6) {
      this.machineCycles++;
    }
  }

  private int doRES(final int bit, final int reg) {
    final int value = readReg8(reg) & ~(1 << bit);
    writeReg8_UseCachedInstructionByte(reg, value);
    return value;
  }

  private int doSET(final int bit, final int reg) {
    final int value = readReg8(reg) | (1 << bit);
    writeReg8_UseCachedInstructionByte(reg, value);
    return value;
  }

  private void doRESmem_LDreg(final int reg, final int bit) {
    writeReg8(reg, doRES(bit, 6));
  }

  private void doSETmem_LDreg(final int reg, final int bit) {
    writeReg8(reg, doSET(bit, 6));
  }

  private void doIN_C() {
    final int port = ((this.regSet[REG_B] & 0xFF) << 8) | (this.regSet[REG_C] & 0xFF);
    final int value = _readport(port) & 0xFF;

    this.regSet[REG_F] = (byte) (FTABLE_SZYXP[value] | (this.regSet[REG_F] & FLAG_C));
  }

  private void doIN_C(final int y) {
    final int port = getRegisterPair(REGPAIR_BC);
    final int value = _readport(port) & 0xFF;
    writeReg8(y, value);

    this.regSet[REG_F] = (byte) (FTABLE_SZYXP[value] | (this.regSet[REG_F] & FLAG_C));
  }

  private void doOUT_C() {
    final int port = ((this.regSet[REG_B] & 0xFF) << 8) | (this.regSet[REG_C] & 0xFF);
    this.setWZ(port + 1, false);
    _writeport(port, 0x00);
  }

  private void doOUT_C(final int y) {
    final int port = ((this.regSet[REG_B] & 0xFF) << 8) | (this.regSet[REG_C] & 0xFF);
    _writeport(port, readReg8(y));
    if (y == 7) { // reg A
      this.setWZ(port + 1, false);
    }
  }

  private void doLD_mNN_RegP(final int p) {
    final int addr = _read16_for_pc();
    _writemem16(addr, readReg16(p));
    this.setWZ(addr + 1, false);
  }

  private void doLD_RegP_mNN(final int p) {
    final int addr = _readmem16(_read16_for_pc());
    writeReg16(p, addr);
    this.setWZ(addr + 1, false);
  }

  private void doNEG() {
    final int A = this.regSet[REG_A] & 0xFF;

    int a = A;
    int z = -a;
    int c = a ^ z;
    int f = FLAG_N | (c & FLAG_H);
    z &= 0xFF;
    f |= FTABLE_SZYX[z];
    c &= 0x0180;
    f |= FTABLE_OVERFLOW[c >>> 7];
    f |= c >>> (8 - FLAG_C_SHIFT);
    this.regSet[REG_A] = (byte) z;
    this.regSet[REG_F] = (byte) f;
  }

  private void doRETI() {
    doRET();
    this.iff1 = this.iff2;
    this.insideBlockInstruction = this.insideBlockInstructionPrev;
    this.detectedINT = false;
    this.bus.onRETI(this);
  }

  private void doRETN() {
    doRET();
    this.iff1 = this.iff2;
    this.insideBlockInstruction = this.insideBlockInstructionPrev;
    this.detectedINT = false;
    this.detectedNMI = false;
  }

  private void doIM(final int y) {
    switch (y) {
      case 4:
      case 0:
        this.im = 0;
        return;
      case 5:
      case 1:
        this.im = this.im == 0 ? 1 : 0;
        return;
      case 6:
      case 2:
        this.im = 1;
        return;
      case 7:
      case 3:
        this.im = 2;
        return;
    }
    throw new Error("unexpected IM index [" + y + ']');
  }

  private void doLD_I_A() {
    setRegister(REG_I, getRegister(REG_A));
    this.machineCycles++;
  }

  private void doLD_R_A() {
    setRegister(REG_R, getRegister(REG_A));
    this.machineCycles++;
  }

  private void doLD_A_I() {
    final int value = getRegister(REG_I);
    setRegister(REG_A, value);

    this.regSet[REG_F] = (byte) (FTABLE_SZYX[value] | (this.iff2 && !(this.detectedINT || this.detectedNMI) ? FLAG_PV : 0) | (this.regSet[REG_F] & FLAG_C));

    this.machineCycles++;
  }

  private void doLD_A_R() {
    final int value = getRegister(REG_R);
    setRegister(REG_A, value);

    this.regSet[REG_F] = (byte) (FTABLE_SZYX[value] | (this.iff2 && !(this.detectedINT || this.detectedNMI) ? FLAG_PV : 0) | (this.regSet[REG_F] & FLAG_C));

    this.machineCycles++;
  }

  private void doRRD() {
    final int HL = getRegisterPair(REGPAIR_HL);
    this.setWZ(HL + 1, false);
    final int A = this.regSet[REG_A] & 0xFF;
    int x = _readmem8(HL);
    int y = (A & 0xf0) << 8;
    y |= ((x & 0x0f) << 8) | ((A & 0x0f) << 4) | (x >> 4);
    _writemem8(HL, (byte) y);
    y >>>= 8;
    this.regSet[REG_A] = (byte) y;
    this.regSet[REG_F] = (byte) (FTABLE_SZYXP[y] | (this.regSet[REG_F] & FLAG_C));

    this.machineCycles += 4;
  }

  private void doRLD() {
    final int HL = getRegisterPair(REGPAIR_HL);
    this.setWZ(HL + 1, false);
    final int A = this.regSet[REG_A] & 0xFF;
    int x = _readmem8(HL);
    int y = (A & 0xf0) << 8;
    y |= (x << 4) | (A & 0x0f);
    _writemem8(HL, (byte) y);
    y >>>= 8;
    this.regSet[REG_A] = (byte) y;
    this.regSet[REG_F] = (byte) (FTABLE_SZYXP[y] | (this.regSet[REG_F] & FLAG_C));

    this.machineCycles += 4;
  }

  private boolean doBLI(final int y, final int z) {
    boolean insideLoop = false;
    switch (y) {
      case 4: {
        switch (z) {
          case 0:
            doLDI();
            break;
          case 1:
            doCPI();
            break;
          case 2:
            doINI();
            break;
          case 3:
            doOUTI();
            break;
          default:
            throw new Error("Unexpected Z index [" + z + ']');
        }
      }
      break;
      case 5: {
        switch (z) {
          case 0:
            doLDD();
            break;
          case 1:
            doCPD();
            break;
          case 2:
            doIND();
            break;
          case 3:
            doOUTD();
            break;
          default:
            throw new Error("Unexpected Z index [" + z + ']');
        }
      }
      break;
      case 6: {
        switch (z) {
          case 0:
            insideLoop = doLDIR();
            break;
          case 1:
            insideLoop = doCPIR();
            break;
          case 2:
            insideLoop = doINIR();
            break;
          case 3:
            insideLoop = doOTIR();
            break;
          default:
            throw new Error("Unexpected Z index [" + z + ']');
        }
      }
      break;
      case 7: {
        switch (z) {
          case 0:
            insideLoop = doLDDR();
            break;
          case 1:
            insideLoop = doCPDR();
            break;
          case 2:
            insideLoop = doINDR();
            break;
          case 3:
            insideLoop = doOTDR();
            break;
          default:
            throw new Error("Unexpected Z index [" + z + ']');
        }
      }
      break;
    }
    return insideLoop;
  }

  private void doLDI() {
    int hl = getRegisterPair(REGPAIR_HL);
    int de = getRegisterPair(REGPAIR_DE);

    int value = _readmem8(hl++);

    _writemem8(de++, (byte) value);
    setRegisterPair(REGPAIR_HL, hl);
    setRegisterPair(REGPAIR_DE, de);

    final int bc = (getRegisterPair(REGPAIR_BC) - 1) & 0xFFFF;
    setRegisterPair(REGPAIR_BC, bc);

    int f = this.regSet[REG_F] & FLAG_SZC;
    f |= bc == 0 ? 0 : FLAG_PV;
    value += this.regSet[REG_A] & 0xFF;
    f |= value & FLAG_X;
    f |= (value << (FLAG_Y_SHIFT - 1)) & FLAG_Y;

    this.regSet[REG_F] = (byte) f;

    this.machineCycles += 2;
  }

  private boolean doLDIR() {
    doLDI();
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_PV) != 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.setWZ(this.regPC + 1, false);
      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doCPI() {
    int hl = getRegisterPair(REGPAIR_HL);
    int n = _readmem8(hl++);

    final int a = getRegister(REG_A);
    final int z = a - n;
    setRegisterPair(REGPAIR_HL, hl);
    final int bc = getRegisterPair(REGPAIR_BC) - 1;
    setRegisterPair(REGPAIR_BC, bc);
    this.setWZ(this.getWZ(false) + 1, false);

    int f = (a ^ n ^ z) & FLAG_H;
    n = z - (f >>> FLAG_H_SHIFT);
    f |= (n << (FLAG_Y_SHIFT - 1)) & FLAG_Y;
    f |= n & FLAG_X;
    f |= FTABLE_SZYX[z & 0xff] & FLAG_SZ;
    f |= bc != 0 ? FLAG_PV : 0;
    this.regSet[REG_F] = (byte) (f | FLAG_N | (this.regSet[REG_F] & FLAG_C));

    this.machineCycles += 5;
  }

  private boolean doCPIR() {
    doCPI();
    boolean loopNonCompleted = true;
    final int flags = this.regSet[REG_F];
    if ((flags & (FLAG_Z | FLAG_PV)) == FLAG_PV) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.setWZ(this.regPC + 1, false);
      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doINI() {
    final int bc = getRegisterPair(REGPAIR_BC);
    this.setWZ(bc + 1, false);
    int hl = getRegisterPair(REGPAIR_HL);
    int x = _readport(bc);
    _writemem8(hl++, (byte) x);
    final int b = ((bc >>> 8) - 1) & 0xFF;
    this.regSet[REG_B] = (byte) b;
    setRegisterPair(REGPAIR_HL, hl);

    int f = FTABLE_SZYX[b & 0xff] | (x >> (7 - FLAG_N_SHIFT));
    x += (readReg8(REG_C) + 1) & 0xff;
    f |= (x & 0x0100) != 0 ? FLAG_HC : 0;
    f |= FTABLE_SZYXP[(x & 0x07) ^ b] & FLAG_PV;
    this.regSet[REG_F] = (byte) f;

    this.machineCycles++;
  }

  private boolean doINIR() {
    doINI();
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doIND() {
    final int bc = getRegisterPair(REGPAIR_BC);
    int hl = getRegisterPair(REGPAIR_HL);
    int x = _readport(bc);
    _writemem8(hl--, (byte) x);
    this.setWZ(bc - 1, false);
    final int b = ((bc >>> 8) - 1) & 0xFF;
    this.regSet[REG_B] = (byte) b;
    setRegisterPair(REGPAIR_HL, hl);

    int f = FTABLE_SZYX[b & 0xff] | (x >> (7 - FLAG_N_SHIFT));
    x += (readReg8(REG_C) - 1) & 0xff;
    f |= (x & 0x0100) != 0 ? FLAG_HC : 0;
    f |= FTABLE_SZYXP[(x & 0x07) ^ b] & FLAG_PV;
    this.regSet[REG_F] = (byte) f;

    this.machineCycles++;
  }

  private boolean doINDR() {
    doIND();
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doOUTI() {
    final int bc = getRegisterPair(REGPAIR_BC);
    int hl = getRegisterPair(REGPAIR_HL);
    int x = _readmem8(hl++);
    _writeport(bc, x);
    final int b = ((bc >>> 8) - 1) & 0xFF;
    this.regSet[REG_B] = (byte) b;
    this.setWZ(this.getRegisterPair(REGPAIR_BC) + 1, false);
    setRegisterPair(REGPAIR_HL, hl);

    int f = FTABLE_SZYX[b & 0xff] | (x >> (7 - FLAG_N_SHIFT));
    x += hl & 0xff;
    f |= (x & 0x0100) != 0 ? FLAG_HC : 0;
    f |= FTABLE_SZYXP[(x & 0x07) ^ b] & FLAG_PV;
    this.regSet[REG_F] = (byte) f;

    this.machineCycles++;
  }

  private boolean doOTIR() {
    doOUTI();
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doOUTD() {
    final int bc = getRegisterPair(REGPAIR_BC);
    int hl = getRegisterPair(REGPAIR_HL);
    int x = _readmem8(hl--);
    _writeport(bc, x);
    final int b = ((bc >>> 8) - 1) & 0xFF;
    this.regSet[REG_B] = (byte) b;
    this.setWZ(this.getRegisterPair(REGPAIR_BC) + 1, false);
    setRegisterPair(REGPAIR_HL, hl);

    int f = FTABLE_SZYX[b & 0xff] | (x >> (7 - FLAG_N_SHIFT));
    x += hl & 0xff;
    f |= (x & 0x0100) != 0 ? FLAG_HC : 0;
    f |= FTABLE_SZYXP[(x & 0x07) ^ b] & FLAG_PV;
    this.regSet[REG_F] = (byte) f;

    this.machineCycles++;
  }

  private boolean doOTDR() {
    doOUTD();
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doLDD() {
    int hl = getRegisterPair(REGPAIR_HL);
    int de = getRegisterPair(REGPAIR_DE);

    int x = _readmem8(hl--);

    _writemem8(de--, (byte) x);
    setRegisterPair(REGPAIR_HL, hl);
    setRegisterPair(REGPAIR_DE, de);

    final int bc = (getRegisterPair(REGPAIR_BC) - 1) & 0xFFFF;
    setRegisterPair(REGPAIR_BC, bc);

    int f = this.regSet[REG_F] & FLAG_SZC;
    f |= bc != 0 ? FLAG_PV : 0;
    x += this.regSet[REG_A] & 0xFF;
    f |= x & FLAG_X;
    f |= (x << (FLAG_Y_SHIFT - 1)) & FLAG_Y;

    this.regSet[REG_F] = (byte) f;

    this.machineCycles += 2;
  }

  private boolean doLDDR() {
    doLDD();
    boolean loopNonCompleted = true;
    if (this.getRegisterPair(REGPAIR_BC) != 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.setWZ(this.regPC + 1, false);
      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doCPD() {
    int hl = getRegisterPair(REGPAIR_HL);
    int n = _readmem8(hl--);
    final int a = getRegister(REG_A);
    final int z = a - n;
    setRegisterPair(REGPAIR_HL, hl);
    final int bc = getRegisterPair(REGPAIR_BC) - 1;
    setRegisterPair(REGPAIR_BC, bc);

    this.setWZ(this.getWZ(false) - 1, false);

    int f = (a ^ n ^ z) & FLAG_H;
    n = z - (f >>> FLAG_H_SHIFT);
    f |= (n << (FLAG_Y_SHIFT - 1)) & FLAG_Y;
    f |= n & FLAG_X;
    f |= FTABLE_SZYX[z & 0xff] & FLAG_SZ;
    f |= bc != 0 ? FLAG_PV : 0;
    this.regSet[REG_F] = (byte) (f | FLAG_N | (this.regSet[REG_F] & FLAG_C));

    this.machineCycles += 5;
  }

  private boolean doCPDR() {
    doCPD();
    boolean loopNonCompleted = true;
    final int flags = this.regSet[REG_F];
    if ((flags & (FLAG_Z | FLAG_PV)) == FLAG_PV) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.setWZ(this.regPC + 1, false);

      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  public String getStateAsString() {
    final StringBuilder result = new StringBuilder(512);
    result.append("PC=").append(Utils.toHex(this.getRegister(Z80.REG_PC))).append(',');
    result.append("SP=").append(Utils.toHex(this.getRegister(Z80.REG_SP))).append(',');
    result.append("IX=").append(Utils.toHex(this.getRegister(Z80.REG_IX))).append(',');
    result.append("IY=").append(Utils.toHex(this.getRegister(Z80.REG_IY))).append(',');
    result.append("AF=").append(Utils.toHex(this.getRegisterPair(Z80.REGPAIR_AF))).append(',');
    result.append("BC=").append(Utils.toHex(this.getRegisterPair(Z80.REGPAIR_BC))).append(',');
    result.append("DE=").append(Utils.toHex(this.getRegisterPair(Z80.REGPAIR_DE))).append(',');
    result.append("HL=").append(Utils.toHex(this.getRegisterPair(Z80.REGPAIR_HL))).append(',');
    result.append("AF'=").append(Utils.toHex(this.getRegisterPair(Z80.REGPAIR_AF, true))).append(',');
    result.append("BC'=").append(Utils.toHex(this.getRegisterPair(Z80.REGPAIR_BC, true))).append(',');
    result.append("DE'=").append(Utils.toHex(this.getRegisterPair(Z80.REGPAIR_DE, true))).append(',');
    result.append("HL'=").append(Utils.toHex(this.getRegisterPair(Z80.REGPAIR_HL, true))).append(',');
    result.append("R=").append(Utils.toHex(this.getRegister(Z80.REG_R))).append(',');
    result.append("I=").append(Utils.toHex(this.getRegister(Z80.REG_I))).append(',');
    result.append("IM=").append(this.getIM()).append(',');
    result.append("IFF1=").append(this.iff1).append(',');
    result.append("IFF2=").append(this.iff2).append(',');
    result.append("M1ExeByte=").append(this.lastM1InstructionByte).append(',');
    result.append("lastExeByte=").append(this.lastInstructionByte);
    return result.toString();
  }

  public void doReset() {
    this._reset(0);
    this._reset(1);
    this._reset(2);
  }

  public boolean compareState(final Z80 other, final boolean compareExe) {
    if (!Arrays.equals(this.regSet, other.regSet)) {
      return false;
    }
    if (!Arrays.equals(this.altRegSet, other.altRegSet)) {
      return false;
    }
    if (this.im != other.im) {
      return false;
    }
    if (this.iff1 != other.iff1) {
      return false;
    }
    if (this.iff2 != other.iff2) {
      return false;
    }
    if (this.regI != other.regI) {
      return false;
    }
    if (this.regIX != other.regIX) {
      return false;
    }
    if (this.regIY != other.regIY) {
      return false;
    }
    if (this.regPC != other.regPC) {
      return false;
    }
    if (this.regR != other.regR) {
      return false;
    }
    if (this.regW != other.regW || this.regWalt != other.regWalt) {
      return false;
    }
    if (this.regZ != other.regZ || this.regZalt != other.regZalt) {
      return false;
    }
    if (compareExe && (this.lastM1InstructionByte != other.lastM1InstructionByte || this.lastInstructionByte != other.lastInstructionByte)) {
      return false;
    }
    
    return this.regSP == other.regSP;
  }

}
