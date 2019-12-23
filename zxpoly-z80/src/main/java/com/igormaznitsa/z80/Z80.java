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

package com.igormaznitsa.z80;

import java.util.Arrays;
import java.util.Locale;

/**
 * Tables and some flag set algorithms were copied and adapted from
 * https://github.com/anotherlin/z80emu project, opcode decoding is based on
 * http://www.z80.info/decoding.htm
 */
public final class Z80 {

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
  private static final byte[] FTABLE_OVERFLOW = new byte[] {
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

  private final Z80CPUBus bus;
  private final byte[] regSet = new byte[8];
  private final byte[] altRegSet = new byte[8];
  private boolean iff1;
  private boolean iff2;
  private int im;
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

  public Z80 fillByState(final Z80 src) {
    this.prefix = src.prefix;
    this.resetCycle = src.resetCycle;
    this.iff1 = src.iff1;
    this.iff2 = src.iff2;
    this.im = src.im;
    this.regI = src.regI;
    this.regIX = src.regIX;
    this.regIY = src.regIY;
    this.regPC = src.regPC;
    this.regR = src.regR;
    this.regSP = src.regSP;
    this.regW = src.regW;
    this.regZ = src.regZ;
    this.regWalt = src.regWalt;
    this.regZalt = src.regZalt;
    System.arraycopy(src.regSet, 0, this.regSet, 0, src.regSet.length);
    System.arraycopy(src.altRegSet, 0, this.altRegSet, 0, src.altRegSet.length);
    this.lastM1InstructionByte = src.lastM1InstructionByte;
    this.lastInstructionByte = src.lastInstructionByte;
    this.machineCycles = src.machineCycles;
    this.cbDisplacementByte = src.cbDisplacementByte;
    this.outSignals = src.outSignals;
    this.prevINSignals = src.prevINSignals;
    this.interruptAllowedForStep = src.interruptAllowedForStep;
    this.detectedINT = src.detectedINT;
    this.detectedNMI = src.detectedNMI;
    this.insideBlockInstruction = src.insideBlockInstruction;
    this.insideBlockInstructionPrev = src.insideBlockInstructionPrev;
    return this;
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

  private void _int(final int ctx) {
    _resetHalt();

    this.iff1 = false;
    this.iff2 = false;

    this.insideBlockInstructionPrev = this.insideBlockInstruction;

    this.detectedINT = false;

    switch (this.im) {
      case 0: {
        _step(ctx, this.bus.onCPURequestDataLines(this, ctx) & 0xFF);
      }
      break;
      case 1: {
        _step(ctx, 0xFF);
      }
      break;
      case 2: {
        final int address = _readmem16(ctx, ((this.regI & 0xFF) << 8) | (this.bus.onCPURequestDataLines(this, ctx) & 0xFF));
        _call(ctx, address);
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

  private void _call(final int ctx, final int address) {
    final int sp = (this.regSP - 2) & 0xFFFF;
    _writemem8(ctx, sp, (byte) this.regPC);
    _writemem8(ctx, sp + 1, (byte) (this.regPC >> 8));
    this.regPC = address;
    this.regSP = sp;
  }

  private void _nmi(final int ctx) {
    _resetHalt();
    this.insideBlockInstructionPrev = this.insideBlockInstruction;
    this.iff1 = false;
    this.detectedNMI = false;
    this.detectedINT = false;
    _call(ctx, 0x66);
    this.machineCycles += 5;
  }

  private void _writemem8(final int ctx, final int address, final byte value) {
    this.bus.writeMemory(this, ctx, address & 0xFFFF, value);
    this.machineCycles += 3;
  }

  private int _readNextPcAddressedWord(final int ctx) {
    return readInstrOrPrefix(ctx, false) | (readInstrOrPrefix(ctx, false) << 8);
  }

  private void _writemem16(final int ctx, final int address, final int value) {
    this._writemem8(ctx, address, (byte) value);
    this._writemem8(ctx, address + 1, (byte) (value >> 8));
  }

  private int _portAddrFromReg(final int ctx, final int reg, final int origValue) {
    return this.bus.readRegPortAddr(this, ctx, reg, origValue);
  }

  private int _readport(final int ctx, final int port) {
    this.machineCycles += 4;
    return this.bus.readPort(this, ctx, port & 0xFFFF) & 0xFF;
  }

  private void _writeport(final int ctx, final int port, final int value) {
    this.bus.writePort(this, ctx, port & 0xFFFF, (byte) value);
    this.machineCycles += 4;
  }

  private int _readmem8(final int ctx, final int address) {
    this.machineCycles += 3;
    return this.bus.readMemory(this, ctx, address & 0xFFFF, false, false) & 0xFF;
  }

  private int _readmem16(final int ctx, final int address) {
    return _readmem8(ctx, address) | (_readmem8(ctx, address + 1) << 8);
  }

  private int _read_ixiy_d(final int ctx) {
    if (this.cbDisplacementByte < 0) {
      return readInstrOrPrefix(ctx, false);
    } else {
      this.machineCycles -= 5;
      return this.cbDisplacementByte;
    }
  }

  private int readInstrOrPrefix(final int ctx, final boolean m1) {
    final int pc = this.regPC;
    this.regPC = (this.regPC + 1) & 0xFFFF;
    this.outSignals = (m1 ? this.outSignals & (~SIGNAL_OUT_nM1) : this.outSignals | SIGNAL_OUT_nM1) & 0xFF;
    final int result = this.bus.readMemory(this, ctx, pc, m1, true) & 0xFF;
    this.outSignals = this.outSignals | SIGNAL_OUT_nM1;

    this.machineCycles += m1 ? 4 : 3;

    if (m1) {
      this.lastM1InstructionByte = result;
    }

    this.lastInstructionByte = result;

    return result;
  }

  private int normalizedPrefix() {
    return (this.prefix & 0xFF) == 0xCB ? this.prefix >>> 8 : this.prefix;
  }

  private boolean checkCondition(final int cc) {
    final boolean result;
    final int flags = this.regSet[REG_F];
    switch (cc) {
      case 0: // NZ
        result = (flags & FLAG_Z) == 0;
        break;
      case 1: // Z
        result = (flags & FLAG_Z) != 0;
        break;
      case 2: // NC
        result = (flags & FLAG_C) == 0;
        break;
      case 3: // C
        result = (flags & FLAG_C) != 0;
        break;
      case 4: // PO
        result = (flags & FLAG_PV) == 0;
        break;
      case 5: // PE
        result = (flags & FLAG_PV) != 0;
        break;
      case 6: // P
        result = (flags & FLAG_S) == 0;
        break;
      case 7: // M
        result = (flags & FLAG_S) != 0;
        break;
      default:
        throw new Error("Unexpected condition");
    }
    return result;
  }

  /**
   * Make full copy of state of the source CPU. NB! pointer to bus will be
   * copied!
   *
   * @param cpu source CPU which state should be copied, must not be null
   */
  public Z80(final Z80 cpu) {
    this.prefix = cpu.prefix;
    this.resetCycle = cpu.resetCycle;
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

  /**
   * Parse string with id of registers and prepare bit vector for it.
   * main set: <b>A,F,B,C,D,E,H,L,1(F without C)</b>
   * alt.set: <b>sa,f,b,c,d,e,h,l,0(F' without C)</b>
   * index: <b>X(high byte IX), x(lower byte IX),Y(high byte IY), y(lower byte IY)</b>
   * spec: <b>P(PC),S(high byte SP),s(lower byte SP)</b>
   *
   * @param regs string where each char means register or its part
   * @return formed bit vector
   * @see #alignRegisterValuesWith(Z80, int)
   * @since 2.0.1
   */
  public static int parseAndPackRegAlignValue(final String regs) {
    final String allowedPositions = "AFBCDEHLXxYy10PSsafbcdehl";
    final String trimmed = regs.trim();
    int result = 0;
    for (final char c : trimmed.toCharArray()) {
      final int index = allowedPositions.indexOf(c);
      if (index < 0) {
        throw new IllegalArgumentException("Unexpected char: " + c + " expected one from '" + allowedPositions + "'");
      } else {
        result |= 1 << index;
      }
    }
    return result;
  }

  /**
   * Set value of some registers from source CPU.
   *
   * @param src                 source CPU must not be null
   * @param packedRegisterFlags bit flags describe needed registers
   * @return the instance
   * @see #parseAndPackRegAlignValue(String)
   * @since 2.0.1
   */
  public Z80 alignRegisterValuesWith(final Z80 src, int packedRegisterFlags) {
    this.cbDisplacementByte = src.cbDisplacementByte;
    this.prefix = src.prefix;
    this.iff1 = src.iff1;
    this.iff2 = src.iff2;
    this.im = src.im;
    this.regI = src.regI;
    this.regR = src.regR;
    this.insideBlockInstruction = src.insideBlockInstruction;
    this.insideBlockInstructionPrev = src.insideBlockInstructionPrev;
    this.prevINSignals = src.prevINSignals;
    this.interruptAllowedForStep = src.interruptAllowedForStep;
    this.detectedINT = src.detectedINT;
    this.detectedNMI = src.detectedNMI;

    if (packedRegisterFlags != 0) {
      //"AFBCDEHL XxYy10PSs afbcdehl"
      int pos = 0;
      while (packedRegisterFlags != 0) {
        if ((packedRegisterFlags & 1) != 0) {
          if (pos < 8) {
            this.regSet[pos] = src.regSet[pos];
          } else if (pos < 17) {
            switch (pos - 8) {
              case 0:
                this.regIX = (this.regIX & 0xFF) | (src.regIX & 0xFF00);
                break;
              case 1:
                this.regIX = (this.regIX & 0xFF00) | (src.regIX & 0xFF);
                break;
              case 2:
                this.regIY = (this.regIY & 0xFF) | (src.regIY & 0xFF00);
                break;
              case 3:
                this.regIY = (this.regIY & 0xFF00) | (src.regIY & 0xFF);
                break;
              case 4:
                this.regSet[REG_F] = (byte) ((this.regSet[REG_F] & FLAG_C) | (src.regSet[REG_F] & ~FLAG_C));
                break;
              case 5:
                this.altRegSet[REG_F] = (byte) ((this.altRegSet[REG_F] & FLAG_C) | (src.altRegSet[REG_F] & ~FLAG_C));
                break;
              case 6:
                this.regPC = src.regPC;
                break;
              case 7:
                this.regSP = (this.regSP & 0xFF) | (src.regSP & 0xFF00);
                break;
              case 8:
                this.regSP = (this.regSP & 0xFF00) | (src.regSP & 0xFF);
                break;
              default:
                throw new Error("Unexpected state");
            }
          } else {
            final int reg = pos - 17;
            this.altRegSet[reg] = src.altRegSet[reg];
          }
        }
        packedRegisterFlags >>>= 1;
        pos++;
      }
    }
    return this;
  }

  private int readReg8(final int ctx, final int r) {
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
            return _readmem8(ctx, this.getRegisterPair(REGPAIR_HL));
          }
          case 0xDD: {
            this.machineCycles += 5;
            final int address = this.regIX + (byte) _read_ixiy_d(ctx);
            this.setWZ(address, false);
            return _readmem8(ctx, address);
          }
          case 0xFD: {
            this.machineCycles += 5;
            final int address = this.regIY + (byte) _read_ixiy_d(ctx);
            this.setWZ(address, false);
            return _readmem8(ctx, address);
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

  private void writeReg8(final int ctx, final int r, final int value) {
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
            _writemem8(ctx, this.getRegisterPair(REGPAIR_HL), (byte) value);
            return;
          }
          case 0xDD: {
            final int address = this.regIX + (byte) value;
            _writemem8(ctx, address, (byte) readInstrOrPrefix(ctx, false));
            this.setWZ(address, false);
            this.machineCycles += 2;
            return;
          }
          case 0xFD: {
            final int address = this.regIY + (byte) value;
            _writemem8(ctx, address, (byte) readInstrOrPrefix(ctx, false));
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
   * @param ctx         context of method call, will be propagated to all sub-calls
   * @param signalRESET true sends the RESET signal to the CPU
   * @param signalNMI   true sends the NMI signal to the CPU
   * @param signalNT    true sends the INT signal to the CPU
   */
  public void nextInstruction(final int ctx, final boolean signalRESET, final boolean signalNMI, final boolean signalNT) {
    int flag = (signalNT ? 0 : SIGNAL_IN_nINT) | (signalNMI ? 0 : SIGNAL_IN_nNMI) | (signalRESET ? 0 : SIGNAL_IN_nRESET) | SIGNAL_IN_nWAIT;
    while (step(ctx, flag)) {
      flag = SIGNAL_IN_ALL_INACTIVE;
    }
  }

  /**
   * Process whole instruction or send signals and block operations will be
   * processed entirely.
   *
   * @param ctx         context of method call, will be propagated to all sub-calls
   * @param signalRESET true sends the RESET signal to the CPU
   * @param signalNMI   true sends the NMI signal to the CPU
   * @param signalNT    true sends the INT signal to the CPU
   */
  public void nextInstruction_SkipBlockInstructions(final int ctx, final boolean signalRESET, final boolean signalNMI, final boolean signalNT) {
    int flag = (signalNT ? 0 : SIGNAL_IN_nINT)
        | (signalNMI ? 0 : SIGNAL_IN_nNMI)
        | (signalRESET ? 0 : SIGNAL_IN_nRESET)
        | SIGNAL_IN_nWAIT;
    while (step(ctx, flag) || this.insideBlockInstruction) {
      flag = SIGNAL_IN_ALL_INACTIVE;
    }
  }

  /**
   * Process one step.
   *
   * @param ctx       context of method call, will be propagated to all sub-calls
   * @param inSignals external signal states to be processes during the step.
   * @return false if there is not any instruction under processing, true
   * otherwise
   */
  public boolean step(final int ctx, final int inSignals) {
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
        if (_step(ctx, readInstrOrPrefix(ctx, true))) {
          // Command completed
          this.prefix = 0;
          result = false;

          if (this.interruptAllowedForStep) {
            // Check interruptions
            if ((inSignals & SIGNAL_IN_nNMI) == 0) {
              // NMI
              _nmi(ctx);
            } else if (this.iff1 && (inSignals & SIGNAL_IN_nINT) == 0) {
              // INT
              _int(ctx);
            }
          }
        } else {
          result = true;
        }
      }

      return result;
    } finally {
      this.prevINSignals = inSignals;
    }
  }

  private boolean _step(final int ctx, final int commandByte) {
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
                    doDJNZ(ctx);
                    break;
                  case 3:
                    doJR(ctx);
                    break;
                  default:
                    doJR(ctx, y - 4);
                    break;
                }
              }
              break;
              case 1: {
                final int p = extractP(commandByte);
                if (extractQ(commandByte) == 0) {
                  doLDRegPairByNextWord(ctx, p);
                } else {
                  doADD_HL_RegPair(p);
                }
              }
              break;
              case 2: {
                if (extractQ(commandByte) == 0) {
                  switch (extractP(commandByte)) {
                    case 0:
                      doLD_mBC_A(ctx);
                      break;
                    case 1:
                      doLD_mDE_A(ctx);
                      break;
                    case 2:
                      doLD_mNN_HL(ctx);
                      break;
                    default:
                      doLD_mNN_A(ctx);
                      break;
                  }
                } else {
                  switch (extractP(commandByte)) {
                    case 0:
                      doLD_A_mBC(ctx);
                      break;
                    case 1:
                      doLD_A_mDE(ctx);
                      break;
                    case 2:
                      doLD_HL_mem(ctx);
                      break;
                    default:
                      doLD_A_mem(ctx);
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
                doINCReg(ctx, extractY(commandByte));
                break;
              case 5:
                doDECReg(ctx, extractY(commandByte));
                break;
              case 6:
                doLD_Reg_ByValue(ctx, extractY(commandByte));
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
              doLDRegByReg(ctx, y, z);
            }
          }
          break;
          case 2: {
            doALU_A_Reg(ctx, extractY(commandByte), extractZ(commandByte));
          }
          break;
          case 3: {
            switch (extractZ(commandByte)) {
              case 0:
                doRETByFlag(ctx, extractY(commandByte));
                break;
              case 1: {
                final int p = extractP(commandByte);
                if (extractQ(commandByte) == 0) {
                  doPOPRegPair(ctx, p);
                } else {
                  switch (p) {
                    case 0:
                      doRET(ctx);
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
                doJP_cc(ctx, extractY(commandByte));
                break;
              case 3: {
                switch (extractY(commandByte)) {
                  case 0:
                    doJP(ctx);
                    break;
                  case 1:
                    this.prefix = (this.prefix << 8) | 0xCB;
                    this.cbDisplacementByte = -1;
                    commandCompleted = false;
                    break;
                  case 2:
                    doOUTnA(ctx);
                    break;
                  case 3:
                    doIN_A_n(ctx);
                    break;
                  case 4:
                    doEX_mSP_HL(ctx);
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
                doCALL(ctx, extractY(commandByte));
              }
              break;
              case 5: {
                if (extractQ(commandByte) == 0) {
                  doPUSH(ctx, extractP(commandByte));
                } else {
                  switch (extractP(commandByte)) {
                    case 0:
                      doCALL(ctx);
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
                doALU_A_n(ctx, extractY(commandByte));
                break;
              case 7:
                doRST(ctx, extractY(commandByte) << 3);
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
                doRollShift(ctx, y, z);
              } else {
                doROTmem_LDreg(ctx, z, y);
              }
            }
            break;
            case 1: {
              doBIT(ctx, y, z);
            }
            break;
            case 2: {
              if (z == 6) {
                doRES(ctx, y, z);
              } else {
                doRESmem_LDreg(ctx, z, y);
              }
            }
            break;
            default: {
              if (z == 6) {
                doSET(ctx, y, z);
              } else {
                doSETmem_LDreg(ctx, z, y);
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
            doRollShift(ctx, y, z);
            break;
          case 1:
            doBIT(ctx, y, z);
            break;
          case 2:
            doRES(ctx, y, z);
            break;
          default:
            doSET(ctx, y, z);
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
                    doIN_C(ctx);
                  } else {
                    doIN_C(ctx, y);
                  }
                }
                break;
                case 1: {
                  final int y = extractY(commandByte);
                  if (y == 6) {
                    doOUT_C(ctx);
                  } else {
                    doOUT_C(ctx, y);
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
                    doLD_mNN_RegP(ctx, p);
                  } else {
                    doLD_RegP_mNN(ctx, p);
                  }
                }
                break;
                case 4:
                  doNEG();
                  break;
                case 5: {
                  if (extractY(commandByte) == 1) {
                    doRETI(ctx);
                  } else {
                    doRETN(ctx);
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
                      doRRD(ctx);
                      break;
                    case 5:
                      doRLD(ctx);
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
                this.insideBlockInstruction = doBLI(ctx, y, z);
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

  private void doDJNZ(final int ctx) {
    final int offset = (byte) readInstrOrPrefix(ctx, false);
    this.machineCycles++;
    int b = this.regSet[REG_B] & 0xFF;
    if (--b != 0) {
      this.regPC = (this.regPC + offset) & 0xFFFF;
      this.machineCycles += 5;
    }
    this.regSet[REG_B] = (byte) b;
  }

  private void doJR(final int ctx, final int cc) {
    final int offset = (byte) readInstrOrPrefix(ctx, false);
    if (checkCondition(cc)) {
      this.regPC = (this.regPC + offset) & 0xFFFF;
      this.machineCycles += 5;
    }
  }

  private void doJR(final int ctx) {
    final int offset = (byte) readInstrOrPrefix(ctx, false);
    this.regPC = (this.regPC + offset) & 0xFFFF;
    this.setWZ(this.regPC, false);
    this.machineCycles += 5;
  }

  private void doLDRegPairByNextWord(final int ctx, final int p) {
    final int value = _readNextPcAddressedWord(ctx);
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

  private void writeReg8_forLdReg8Instruction(final int ctx, final int r, final int value) {
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
            _writemem8(ctx, this.getRegisterPair(REGPAIR_HL), (byte) value);
            return;
          case 0xDD:
            _writemem8(ctx, this.regIX + (byte) readInstrOrPrefix(ctx, false), (byte) value);
            this.machineCycles += 5;
            return;
          case 0xFD:
            _writemem8(ctx, this.regIY + (byte) readInstrOrPrefix(ctx, false), (byte) value);
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

  private void writeReg8_UseCachedInstructionByte(final int ctx, final int r, final int value) {
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
            _writemem8(ctx, this.getRegisterPair(REGPAIR_HL), (byte) value);
            return;
          case 0xDD:
            _writemem8(ctx, this.regIX + (byte) (this.cbDisplacementByte < 0 ? this.lastInstructionByte : this.cbDisplacementByte), (byte) value);
            return;
          case 0xFD:
            _writemem8(ctx, this.regIY + (byte) (this.cbDisplacementByte < 0 ? this.lastInstructionByte : this.cbDisplacementByte), (byte) value);
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

  private void doLD_mNN_HL(final int ctx) {
    final int addr = _readNextPcAddressedWord(ctx);
    _writemem16(ctx, addr, readReg16(2));
    this.setWZ(addr + 1, false);
  }

  private void doLD_mNN_A(final int ctx) {
    final int address = _readNextPcAddressedWord(ctx);
    final byte a = this.regSet[REG_A];
    this.regW = a & 0xFF;
    this.regZ = (address + 1) & 0xFF;
    this._writemem8(ctx, address, a);
  }

  private void doLD_mBC_A(final int ctx) {
    final int address = this.getRegisterPair(REGPAIR_BC);
    final byte a = this.regSet[REG_A];
    this.regW = a & 0xFF;
    this.regZ = (address + 1) & 0xFF;
    this._writemem8(ctx, address, a);
  }

  private void doLD_mDE_A(final int ctx) {
    final int address = this.getRegisterPair(REGPAIR_DE);
    final byte a = this.regSet[REG_A];
    this.regW = a & 0xFF;
    this.regZ = (address + 1) & 0xFF;
    this._writemem8(ctx, address, a);
  }

  private void doLD_HL_mem(final int ctx) {
    final int addr = _readmem16(ctx, _readNextPcAddressedWord(ctx));
    writeReg16(2, addr);
    this.setWZ(addr + 1, false);
  }

  private void doLD_A_mem(final int ctx) {
    final int address = _readNextPcAddressedWord(ctx);
    setRegister(REG_A, _readmem8(ctx, address));
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

  private void doINCReg(final int ctx, final int y) {
    final int x = readReg8(ctx, y);

    int z = x + 1;
    int c = x ^ z;

    int f = this.regSet[REG_F] & FLAG_C;
    f |= (c & FLAG_H);
    f |= FTABLE_SZYX[z & 0xff];
    f |= FTABLE_OVERFLOW[(c >>> 7) & 0x03];

    writeReg8_UseCachedInstructionByte(ctx, y, z);
    this.regSet[REG_F] = (byte) f;
  }

  private void doDECReg(final int ctx, final int y) {
    final int x = readReg8(ctx, y);

    final int z = x - 1;
    final int c = x ^ z;

    writeReg8_UseCachedInstructionByte(ctx, y, z);

    int f = FLAG_N | (this.regSet[REG_F] & FLAG_C);
    f |= (c & FLAG_H);
    f |= FTABLE_SZYX[z & 0xff];
    f |= FTABLE_OVERFLOW[(c >>> 7) & 0x03];

    this.regSet[REG_F] = (byte) f;
  }

  private void doLD_Reg_ByValue(final int ctx, final int y) {
    writeReg8(ctx, y, readInstrOrPrefix(ctx, false));
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

  private void doLDRegByReg(final int ctx, final int y, final int z) {
    if (z == 6 || y == 6) {
      // process (HL),(IXd),(IYd)
      if (y == 6) {
        final int oldprefix = this.prefix;
        this.prefix = 0;
        final int value = readReg8(ctx, z);
        this.prefix = oldprefix;
        writeReg8_forLdReg8Instruction(ctx, y, value);
      } else {
        final int value = readReg8(ctx, z);
        this.prefix = 0;
        writeReg8_forLdReg8Instruction(ctx, y, value);
      }
    } else {
      writeReg8_forLdReg8Instruction(ctx, y, readReg8(ctx, z));
    }
  }

  private void doRETByFlag(final int ctx, final int y) {
    if (checkCondition(y)) {
      final int sp = this.regSP;
      int sp1 = sp + 1;
      this.regPC = _readmem8(ctx, sp) | (_readmem8(ctx, sp1++) << 8);
      this.setWZ(this.regPC, false);
      this.regSP = sp1 & 0xFFFF;
    }
    this.machineCycles++;
  }

  private void doRET(final int ctx) {
    final int sp = this.regSP;
    int sp1 = sp + 1;
    this.regPC = _readmem8(ctx, sp) | (_readmem8(ctx, sp1++) << 8);
    this.setWZ(this.regPC, false);
    this.regSP = sp1 & 0xFFFF;
  }

  private void doPOPRegPair(final int ctx, final int p) {
    final int address = getRegister(REG_SP);
    writeReg16_2(p, _readmem16(ctx, address));
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

  private void doJP_cc(final int ctx, final int cc) {
    final int address = _readNextPcAddressedWord(ctx);
    if (checkCondition(cc)) {
      this.regPC = address;
    }
    this.setWZ(address, false);
  }

  private void doJP(final int ctx) {
    this.regPC = _readNextPcAddressedWord(ctx);
    this.setWZ(this.regPC, false);
  }

  private void doOUTnA(final int ctx) {
    final int n = readInstrOrPrefix(ctx, false);
    final int a = _portAddrFromReg(ctx, REG_A, this.regSet[REG_A]) & 0xFF;
    _writeport(ctx, (a << 8) | n, a);
  }

  private void doIN_A_n(final int ctx) {
    this.regSet[REG_A] = (byte) _readport(ctx, ((_portAddrFromReg(ctx, REG_A, this.regSet[REG_A]) & 0xFF) << 8) | readInstrOrPrefix(ctx, false));
  }

  private void doEX_mSP_HL(final int ctx) {
    final int stacktop = this.regSP;
    final int hl = readReg16(2);
    final int value = _readmem8(ctx, stacktop) | (_readmem8(ctx, stacktop + 1) << 8);
    this.setWZ(value, false);
    writeReg16(2, value);
    _writemem8(ctx, stacktop, (byte) hl);
    _writemem8(ctx, stacktop + 1, (byte) (hl >> 8));

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

  private void doCALL(final int ctx, final int y) {
    final int address = _readNextPcAddressedWord(ctx);
    if (checkCondition(y)) {
      _call(ctx, address);
      this.machineCycles++;
    }
    this.setWZ(address, false);
  }

  private void doCALL(final int ctx) {
    final int address = _readNextPcAddressedWord(ctx);
    this.setWZ(address, false);
    _call(ctx, address);
    this.machineCycles++;
  }

  private void doPUSH(final int ctx, final int p) {
    final int address = getRegister(REG_SP) - 2;
    _writemem16(ctx, address, readReg16_2(p));
    setRegister(REG_SP, address);
    this.machineCycles++;
  }

  private void doALU_A_Reg(final int ctx, final int op, final int reg) {
    _aluAccumulatorOp(op, readReg8(ctx, reg));
  }

  private void doALU_A_n(final int ctx, final int op) {
    _aluAccumulatorOp(op, readInstrOrPrefix(ctx, false));
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

  private void doRST(final int ctx, final int address) {
    _call(ctx, address);
    this.setWZ(address, false);
    this.machineCycles++;
  }

  private int doRollShift(final int ctx, final int op, final int reg) {
    int x = readReg8(ctx, reg);
    final int prevC = this.regSet[REG_F] & FLAG_C;
    final int c;
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
        x = (x << 1) | prevC;
      }
      break;
      case 3: { // RR
        c = x & 0x01;
        x = (x >>> 1) | (prevC << 7);
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
    writeReg8_UseCachedInstructionByte(ctx, reg, x);
    this.regSet[REG_F] = (byte) (FTABLE_SZYXP[x & 0xFF] | c);
    return x;
  }

  private void doROTmem_LDreg(final int ctx, final int reg, final int op) {
    writeReg8(ctx, reg, doRollShift(ctx, op, 6));
  }

  private void doBIT(final int ctx, final int bit, final int reg) {
    final int val = readReg8(ctx, reg);
    final int x = val & (1 << bit);

    this.regSet[REG_F] = (byte) ((x == 0 ? (FLAG_Z | FLAG_PV) : 0) | (x & FLAG_S) | (this.regW & FLAG_XY) | FLAG_H | (this.regSet[REG_F] & FLAG_C));

    if (reg == 6) {
      this.machineCycles++;
    }
  }

  private int doRES(final int ctx, final int bit, final int reg) {
    final int value = readReg8(ctx, reg) & ~(1 << bit);
    writeReg8_UseCachedInstructionByte(ctx, reg, value);
    return value;
  }

  private int doSET(final int ctx, final int bit, final int reg) {
    final int value = readReg8(ctx, reg) | (1 << bit);
    writeReg8_UseCachedInstructionByte(ctx, reg, value);
    return value;
  }

  private void doRESmem_LDreg(final int ctx, final int reg, final int bit) {
    writeReg8(ctx, reg, doRES(ctx, bit, 6));
  }

  private void doSETmem_LDreg(final int ctx, final int reg, final int bit) {
    writeReg8(ctx, reg, doSET(ctx, bit, 6));
  }

  private void doIN_C(final int ctx) {
    final int value = _readport(ctx, _portAddrFromReg(ctx, REGPAIR_BC, this.getRegisterPair(REGPAIR_BC))) & 0xFF;
    this.regSet[REG_F] = (byte) (FTABLE_SZYXP[value] | (this.regSet[REG_F] & FLAG_C));
  }

  private void doIN_C(final int ctx, final int y) {
    final int value = _readport(ctx, _portAddrFromReg(ctx, REGPAIR_BC, getRegisterPair(REGPAIR_BC))) & 0xFF;
    writeReg8(ctx, y, value);

    this.regSet[REG_F] = (byte) (FTABLE_SZYXP[value] | (this.regSet[REG_F] & FLAG_C));
  }

  private void doOUT_C(final int ctx) {
    final int port = _portAddrFromReg(ctx, REGPAIR_BC, this.getRegisterPair(REGPAIR_BC));
    this.setWZ(port + 1, false);
    _writeport(ctx, port, 0x00);
  }

  private void doOUT_C(final int ctx, final int y) {
    final int port = _portAddrFromReg(ctx, REGPAIR_BC, this.getRegisterPair(REGPAIR_BC));
    _writeport(ctx, port, readReg8(ctx, y));
    if (y == 7) { // reg A
      this.setWZ(port + 1, false);
    }
  }

  private void doLD_mNN_RegP(final int ctx, final int p) {
    final int addr = _readNextPcAddressedWord(ctx);
    _writemem16(ctx, addr, readReg16(p));
    this.setWZ(addr + 1, false);
  }

  private void doLD_RegP_mNN(final int ctx, final int p) {
    final int addr = _readmem16(ctx, _readNextPcAddressedWord(ctx));
    writeReg16(p, addr);
    this.setWZ(addr + 1, false);
  }

  private void doNEG() {
    int a = this.regSet[REG_A] & 0xFF;
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

  private void doRETI(final int ctx) {
    doRET(ctx);
    this.iff1 = this.iff2;
    this.insideBlockInstruction = this.insideBlockInstructionPrev;
    this.detectedINT = false;
    this.bus.onRETI(this, ctx);
  }

  private void doRETN(final int ctx) {
    doRET(ctx);
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

  private void doRRD(final int ctx) {
    int hl = this.getRegisterPair(REGPAIR_HL);
    this.setWZ(hl + 1, false);
    final int A = this.regSet[REG_A] & 0xFF;
    int x = _readmem8(ctx, hl);
    int y = (A & 0xf0) << 8;
    y |= ((x & 0x0f) << 8) | ((A & 0x0f) << 4) | (x >> 4);
    _writemem8(ctx, hl, (byte) y);
    y >>>= 8;
    this.regSet[REG_A] = (byte) y;
    this.regSet[REG_F] = (byte) (FTABLE_SZYXP[y] | (this.regSet[REG_F] & FLAG_C));

    this.machineCycles += 4;
  }

  private void doRLD(final int ctx) {
    int hl = this.getRegisterPair(REGPAIR_HL);
    this.setWZ(hl + 1, false);
    final int A = this.regSet[REG_A] & 0xFF;
    int x = _readmem8(ctx, hl);
    int y = (A & 0xf0) << 8;
    y |= (x << 4) | (A & 0x0f);
    _writemem8(ctx, hl, (byte) y);
    y >>>= 8;
    this.regSet[REG_A] = (byte) y;
    this.regSet[REG_F] = (byte) (FTABLE_SZYXP[y] | (this.regSet[REG_F] & FLAG_C));

    this.machineCycles += 4;
  }

  private boolean doBLI(final int ctx, final int y, final int z) {
    boolean insideLoop = false;
    switch (y) {
      case 4: {
        switch (z) {
          case 0:
            doLDI(ctx);
            break;
          case 1:
            doCPI(ctx);
            break;
          case 2:
            doINI(ctx);
            break;
          case 3:
            doOUTI(ctx);
            break;
          default:
            throw new Error("Unexpected Z index [" + z + ']');
        }
      }
      break;
      case 5: {
        switch (z) {
          case 0:
            doLDD(ctx);
            break;
          case 1:
            doCPD(ctx);
            break;
          case 2:
            doIND(ctx);
            break;
          case 3:
            doOUTD(ctx);
            break;
          default:
            throw new Error("Unexpected Z index [" + z + ']');
        }
      }
      break;
      case 6: {
        switch (z) {
          case 0:
            insideLoop = doLDIR(ctx);
            break;
          case 1:
            insideLoop = doCPIR(ctx);
            break;
          case 2:
            insideLoop = doINIR(ctx);
            break;
          case 3:
            insideLoop = doOTIR(ctx);
            break;
          default:
            throw new Error("Unexpected Z index [" + z + ']');
        }
      }
      break;
      case 7: {
        switch (z) {
          case 0:
            insideLoop = doLDDR(ctx);
            break;
          case 1:
            insideLoop = doCPDR(ctx);
            break;
          case 2:
            insideLoop = doINDR(ctx);
            break;
          case 3:
            insideLoop = doOTDR(ctx);
            break;
          default:
            throw new Error("Unexpected Z index [" + z + ']');
        }
      }
      break;
    }
    return insideLoop;
  }

  private void doLD_A_mBC(final int ctx) {
    final int addr = this.getRegisterPair(REGPAIR_BC);
    setRegister(REG_A, _readmem8(ctx, addr));
    this.setWZ(addr + 1, false);
  }

  private boolean doLDIR(final int ctx) {
    doLDI(ctx);
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

  private void doLD_A_mDE(final int ctx) {
    final int addr = this.getRegisterPair(REGPAIR_DE);
    setRegister(REG_A, _readmem8(ctx, addr));
    this.setWZ(addr + 1, false);
  }

  private boolean doCPIR(final int ctx) {
    doCPI(ctx);
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

  private void doINI(final int ctx) {
    final int bc = _portAddrFromReg(ctx, REGPAIR_BC, getRegisterPair(REGPAIR_BC));
    this.setWZ(bc + 1, false);
    int hl = this.getRegisterPair(REGPAIR_HL);
    int x = _readport(ctx, bc);
    _writemem8(ctx, hl++, (byte) x);
    final int b = ((bc >>> 8) - 1) & 0xFF;
    this.regSet[REG_B] = (byte) b;
    setRegisterPair(REGPAIR_HL, hl);

    int f = FTABLE_SZYX[b & 0xff] | (x >> (7 - FLAG_N_SHIFT));
    x += (readReg8(ctx, REG_C) + 1) & 0xff;
    f |= (x & 0x0100) != 0 ? FLAG_HC : 0;
    f |= FTABLE_SZYXP[(x & 0x07) ^ b] & FLAG_PV;
    this.regSet[REG_F] = (byte) f;

    this.machineCycles++;
  }

  private boolean doINIR(final int ctx) {
    doINI(ctx);
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doIND(final int ctx) {
    final int bc = _portAddrFromReg(ctx, REGPAIR_BC, getRegisterPair(REGPAIR_BC));
    int hl = this.getRegisterPair(REGPAIR_HL);
    int x = _readport(ctx, bc);
    _writemem8(ctx, hl--, (byte) x);
    this.setWZ(bc - 1, false);
    final int b = ((bc >>> 8) - 1) & 0xFF;
    this.regSet[REG_B] = (byte) b;
    setRegisterPair(REGPAIR_HL, hl);

    int f = FTABLE_SZYX[b & 0xff] | (x >> (7 - FLAG_N_SHIFT));
    x += (readReg8(ctx, REG_C) - 1) & 0xff;
    f |= (x & 0x0100) != 0 ? FLAG_HC : 0;
    f |= FTABLE_SZYXP[(x & 0x07) ^ b] & FLAG_PV;
    this.regSet[REG_F] = (byte) f;

    this.machineCycles++;
  }

  private boolean doINDR(final int ctx) {
    doIND(ctx);
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doLDI(final int ctx) {
    int hl = this.getRegisterPair(REGPAIR_HL);
    int de = this.getRegisterPair(REGPAIR_DE);

    int value = _readmem8(ctx, hl++);

    _writemem8(ctx, de++, (byte) value);
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

  private boolean doOTIR(final int ctx) {
    doOUTI(ctx);
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doCPI(final int ctx) {
    int hl = this.getRegisterPair(REGPAIR_HL);
    int n = _readmem8(ctx, hl++);

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

  private boolean doOTDR(final int ctx) {
    doOUTD(ctx);
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.machineCycles += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doOUTI(final int ctx) {
    final int bc = _portAddrFromReg(ctx, REGPAIR_BC, this.getRegisterPair(REGPAIR_BC));
    int hl = this.getRegisterPair(REGPAIR_HL);
    int x = _readmem8(ctx, hl++);
    _writeport(ctx, bc, x);
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

  private boolean doLDDR(final int ctx) {
    doLDD(ctx);
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

  private void doOUTD(final int ctx) {
    final int bc = _portAddrFromReg(ctx, REGPAIR_BC, this.getRegisterPair(REGPAIR_BC));
    int hl = this.getRegisterPair(REGPAIR_HL);
    int x = _readmem8(ctx, hl--);
    _writeport(ctx, bc, x);
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

  private boolean doCPDR(final int ctx) {
    doCPD(ctx);
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

  private void doLDD(final int ctx) {
    int hl = this.getRegisterPair(REGPAIR_HL);
    int de = this.getRegisterPair(REGPAIR_DE);

    int x = _readmem8(ctx, hl--);

    _writemem8(ctx, de--, (byte) x);
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

  private void doCPD(final int ctx) {
    int hl = this.getRegisterPair(REGPAIR_HL);
    int n = _readmem8(ctx, hl--);
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

}