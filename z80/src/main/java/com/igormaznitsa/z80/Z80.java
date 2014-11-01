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
import java.util.Locale;

public final class Z80 {

  private static final int[] FLAGARRAY_PARITY;

  static {
    // Make initialization of the inside packed array contains parity bits for values 0..255
    FLAGARRAY_PARITY = new int[8];

    int mask = 1;
    int accum = 0;
    int index = 0;
    boolean parity;
    for (int i = 0; i < 256; i++) {
      parity = true;
      for (int j = 0; j < 8; j++) {
        if ((i & (1 << j)) != 0) {
          parity = !parity;
        }
      }

      accum |= parity ? mask : 0;
      mask <<= 1;
      if (mask == 0) {
        mask = 1;
        FLAGARRAY_PARITY[index++] = accum;
        accum = 0;
      }
    }
  }

  public static final int FLAG_S = 0x80;
  public static final int FLAG_Z = 0x40;
  public static final int FLAG_RESERVED_5 = 0x20;
  public static final int FLAG_H = 0x10;
  public static final int FLAG_RESERVED_3 = 0x08;
  public static final int FLAG_PV = 0x04;
  public static final int FLAG_N = 0x02;
  public static final int FLAG_C = 0x01;

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

  private long tactCounter;

  private final byte[] regSet = new byte[8];
  private final byte[] altRegSet = new byte[8];

  private int lastReadInstructionByte;

  private int cbDisplacementByte = -1;

  private int prefix;

  private int outSignals = 0xFFFFFFFF;
  private int prevINSignals = 0xFFFFFFFF;

  private boolean tempIgnoreInterruption;
  private boolean pendingNMI;
  private boolean pendingINT;
  private boolean insideBlockInstructionPrev;
  private boolean insideBlockInstruction;
  
  private int resetCycle = 0;

  private static int checkParity(final int value) {
    return (FLAGARRAY_PARITY[value >>> 5] & (1 << (value & 0x1F))) == 0 ? 0 : FLAG_PV;
  }

  private void updateFlags(final int notAffected, final int var, final int set) {
    byte flag = this.regSet[REG_F];
    this.regSet[REG_F] = (byte) ((flag & notAffected) | ((set | var) & ~notAffected));
  }

  public Z80(final Z80CPUBus bus) {
    if (bus == null) {
      throw new NullPointerException("The CPU BUS must not be null");
    }
    this.bus = bus;
    _reset(0);
    _reset(1);
    _reset(2);
    this.tactCounter = 3L;
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
    }
    else {
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
        }
        else {
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

  public long getTacts() {
    return this.tactCounter;
  }

  public boolean isInsideBlockLoop(){
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
        this.regSP = 0xFFFF;
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
    }

    this.im = 0;
    this.cbDisplacementByte = -1;

    this.insideBlockInstruction = false;
    this.insideBlockInstructionPrev = false;
    
    this.tempIgnoreInterruption = false;
    this.pendingINT = false;
    this.pendingNMI = false;

    this.prefix = 0;
    this.outSignals = SIGNAL_OUT_ALL_INACTIVE;

    this.tactCounter += 3;
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
    
    this.pendingINT = false;

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
        _call(((this.regI & 0xFF) << 8) | (this.bus.onCPURequestDataLines(this) & 0xFF));
        this.tactCounter += 7;
      }
      break;
      default:
        throw new Error("Unexpected IM mode [" + this.im + ']');
    }

    this.tactCounter += 6;
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
    this.pendingNMI = false;
    this.pendingINT = false;
    _call(0x66);
    this.tactCounter += 5;
  }

  private void _writemem8(final int address, final byte value) {
    this.bus.writeMemory(this, address & 0xFFFF, value);
    this.tactCounter += 3;
  }

  private int _read16_for_pc() {
    return readInstructionByte(false) | (readInstructionByte(false) << 8);
  }

  private void _writemem16(final int address, final int value) {
    this._writemem8(address, (byte) value);
    this._writemem8(address + 1, (byte) (value >> 8));
  }

  private int _readport(final int port) {
    this.tactCounter += 4;
    return this.bus.readPort(this, port & 0xFFFF) & 0xFF;
  }

  private void _writeport(final int port, final int value) {
    this.bus.writePort(this, port & 0xFFFF, (byte) value);
    this.tactCounter += 4;
  }

  private int _readmem8(final int address) {
    this.tactCounter += 3;
    return this.bus.readMemory(this, address & 0xFFFF, false) & 0xFF;
  }

  private int _readmem16(final int address) {
    return _readmem8(address) | (_readmem8(address + 1) << 8);
  }

  private int _read_ixiy_d() {
    if (this.cbDisplacementByte < 0) {
      return readInstructionByte(false);
    }
    else {
      this.tactCounter -= 5;
      return this.cbDisplacementByte;
    }
  }

  private int readInstructionByte(final boolean m1) {
    final int pc = this.regPC++;
    this.regPC &= 0xFFFF;
    this.outSignals = m1 ? this.outSignals & (~SIGNAL_OUT_nM1) : this.outSignals | SIGNAL_OUT_nM1;
    final int result = this.bus.readMemory(this, pc, m1) & 0xFF;
    this.outSignals = this.outSignals | SIGNAL_OUT_nM1;

    this.tactCounter += m1 ? 4 : 3;

    this.lastReadInstructionByte = result;
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
    final int flags = this.regSet[REG_F];
    switch (cc) {
      case 0:
        return (flags & FLAG_Z) == 0;
      case 1:
        return (flags & FLAG_Z) != 0;
      case 2:
        return (flags & FLAG_C) == 0;
      case 3:
        return (flags & FLAG_C) != 0;
      case 4:
        return (flags & FLAG_PV) == 0;
      case 5:
        return (flags & FLAG_PV) != 0;
      case 6:
        return (flags & FLAG_S) == 0;
      case 7:
        return (flags & FLAG_S) != 0;
    }
    throw new Error("Unexpected condition index [" + cc + ']');
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
          case 0x00:
            return _readmem8(getRegisterPair(REGPAIR_HL));
          case 0xDD:
            this.tactCounter += 5;
            return _readmem8(this.regIX + (byte) _read_ixiy_d());
          case 0xFD:
            this.tactCounter += 5;
            return _readmem8(this.regIY + (byte) _read_ixiy_d());
        }
      }
      break;
      case 7:
        return getRegister(REG_A);
    }
    throw new Error("Unexpected prefix or R index [" + this.prefix + ':' + r + ']');
  }

  private int readReg8WithResetPrefixForIndex(final int r) {
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
          case 0x00:
            return _readmem8(getRegisterPair(REGPAIR_HL));
          case 0xDD:
            this.tactCounter += 5;
            this.prefix = 0;
            return _readmem8(this.regIX + (byte) _read_ixiy_d());
          case 0xFD:
            this.tactCounter += 5;
            this.prefix = 0;
            return _readmem8(this.regIY + (byte) _read_ixiy_d());
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
        setRegister(REG_H, value);
        return;
      case 5:
        setRegister(REG_L, value);
        return;
      case 6: { // (HL)
        switch (normalizedPrefix()) {
          case 0x00:
            _writemem8(getRegisterPair(REGPAIR_HL), (byte) value);
            return;
          case 0xDD:
            _writemem8(this.regIX + (byte)value, (byte) readInstructionByte(false));
            this.tactCounter += 2;
            return;
          case 0xFD:
            _writemem8(this.regIY + (byte)value, (byte) readInstructionByte(false));
            this.tactCounter += 2;
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
            setRegister(REG_IX, (value<<8)|(getRegister(REG_IX) & 0x00FF));
            return;
          case 0xFD:
            setRegister(REG_IY, (value << 8) | (getRegister(REG_IY) & 0x00FF));
            return;
        }break;
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
            this.tactCounter += 5;
            return;
          case 0xFD:
            _writemem8(this.regIY + (byte) readInstructionByte(false), (byte) value);
            this.tactCounter += 5;
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
        setRegister(REG_H, value);
        return;
      case 5:
        setRegister(REG_L, value);
        return;
      case 6: { // (HL)
        this.tactCounter += 1;
        switch (normalizedPrefix()) {
          case 0x00:
            _writemem8(getRegisterPair(REGPAIR_HL), (byte) value);
            return;
          case 0xDD:
            _writemem8(this.regIX + (byte) (this.cbDisplacementByte < 0 ? this.lastReadInstructionByte : this.cbDisplacementByte), (byte) value);
            return;
          case 0xFD:
            _writemem8(this.regIY + (byte) (this.cbDisplacementByte < 0 ? this.lastReadInstructionByte : this.cbDisplacementByte), (byte) value);
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

  private boolean isHiLoFront(final int signal, final int signals) {
    return (((this.prevINSignals & signal) ^ (signals & signal)) & (signal & this.prevINSignals)) != 0;
  }

  /**
   * Process whole instruction or send signals but only step of a block instruction will be processed.
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
   * Process whole instruction or send signals and  block operations will be processed entirely.
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
      if ((inSignals & SIGNAL_IN_nRESET) == 0) {
        // make simulation of reset signal for as minimum 3 cycles
        _reset(this.resetCycle++);
        return false;
      }

      if ((inSignals & SIGNAL_IN_nWAIT) == 0) {
        this.tactCounter++;
        return this.prefix != 0;
      }

      boolean interruptionProcessed = false;

      if (this.pendingNMI || isHiLoFront(SIGNAL_IN_nNMI, inSignals)) {
        if (this.tempIgnoreInterruption || this.prefix != 0) {
          this.pendingNMI = true;
        }
        else {
          _nmi();
          interruptionProcessed = true;
        }
      }
      else if (this.pendingINT || isHiLoFront(SIGNAL_IN_nINT, inSignals)) {
        if (this.tempIgnoreInterruption || this.prefix != 0) {
          this.pendingINT = this.iff1;
        }
        else if (this.iff1) {
          if (this.prefix != 0) {
            this.pendingINT = true;
          }
          else {
            _int();
            interruptionProcessed = true;
          }
        }
      }

      final boolean notcompleted;
      if (interruptionProcessed) {
        notcompleted = false;
      }
      else {
        if (_step(readInstructionByte(true))) {
          this.prefix = 0;
          notcompleted = false;
        }
        else {
          this.prefix &= 0xFFFF;
          notcompleted = true;
        }
      }
      return notcompleted;
    }
    finally {
      this.prevINSignals = inSignals;
    }
  }

  private boolean _step(final int commandByte) {
    boolean commandCompleted = true;

    this.tempIgnoreInterruption = false;
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
                }
                else {
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
                }
                else {
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
                }
                else {
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
            }
            else {
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
                }
                else {
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
                }
                else {
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
        }
        else {

          final int z = extractZ(commandByte);
          final int y = extractY(commandByte);

          switch (extractX(commandByte)) {
            case 0: {
              if (z == 6) {
                doRollShift(y, z);
              }
              else {
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
              }
              else {
                doRESmem_LDreg(z, y);
              }
            }
            break;
            default: {
              if (z == 6) {
                doSET(y, z);
              }
              else {
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
        }
        else {
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
                  }
                  else {
                    doIN_C(y);
                  }
                }
                break;
                case 1: {
                  final int y = extractY(commandByte);
                  if (y == 6) {
                    doOUT_C();
                  }
                  else {
                    doOUT_C(y);
                  }
                }
                break;
                case 2: {
                  final int p = extractP(commandByte);
                  if (extractQ(commandByte) == 0) {
                    doSBC_HL_RegPair(p);
                  }
                  else {
                    doADC_HL_RegPair(p);
                  }
                }
                break;
                case 3: {
                  final int p = extractP(commandByte);
                  if (extractQ(commandByte) == 0) {
                    doLD_mNN_RegP(p);
                  }
                  else {
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
                  }
                  else {
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
              }
              else {
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
    this.tempIgnoreInterruption = true;
  }

  private void doHalt() {
    this.outSignals &= ~SIGNAL_OUT_nHALT;
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
    this.tactCounter++;
    int b = this.regSet[REG_B] & 0xFF;
    if (--b != 0) {
      this.regPC = (this.regPC + offset) & 0xFFFF;
      this.tactCounter += 5;
    }
    this.regSet[REG_B] = (byte) b;
  }

  private void doJR(final int cc) {
    final int offset = (byte) readInstructionByte(false);
    if (checkCondition(cc)) {
      this.regPC = (this.regPC + offset) & 0xFFFF;
      this.tactCounter += 5;
    }
  }

  private void doJR() {
    final int offset = (byte) readInstructionByte(false);
    this.regPC = (this.regPC + offset) & 0xFFFF;
    this.tactCounter += 5;
  }

  private void doLDRegPairByNextWord(final int p) {
    final int value = _read16_for_pc();
    writeReg16(p, value);
  }

  private void doADD_HL_RegPair(final int p) {
    final int reg = readReg16(2);
    final int value = readReg16(p);

    final int result = reg + value;
    
    final int flagH = (((reg & 0x0FFF) + (value & 0x0FFF)) & 0x1000) == 0 ? 0 : FLAG_H;
    final int flagC = (result & 0x10000) == 0 ? 0 : FLAG_C;

    writeReg16(2, result);

    updateFlags(FLAG_RESERVED_3 | FLAG_RESERVED_5 | FLAG_S | FLAG_Z | FLAG_PV, flagC | flagH, 0);

    this.tactCounter += 7;
  }

  private void doADC_HL_RegPair(final int p) {
    final int reg = readReg16(2);
    final int value = readReg16(p);

    final int result = reg + value + (this.regSet[REG_F] & FLAG_C);

    final int flagH = (((reg & 0xF00) + (value & 0xF00)) & 0x100) == 0 ? 0 : FLAG_H;
    final int flagC = (result & 0x10000) == 0 ? 0 : FLAG_C;
    final int flagZ = (result & 0xFFFF) == 0 ? FLAG_Z : 0;
    final int flagS = (result & 0x8000) == 0 ? 0 : FLAG_S;
    final int flagPV = (~(reg ^ value) & (reg ^ result) & 0x8000) == 0 ? 0 : FLAG_PV;

    writeReg16(2, result);

    updateFlags(FLAG_RESERVED_3 | FLAG_RESERVED_5, flagC | flagH | flagZ | flagS | flagPV, 0);

    this.tactCounter += 7;
  }

  private void doSBC_HL_RegPair(final int p) {
    final int reg = readReg16(2);
    final int value = readReg16(p);

    final int valuec = this.regSet[REG_F] & FLAG_C;
    
    final int result = reg - value - valuec;

    final int flagH = (((reg & 0x0FFF) - (value & 0x0FFF) - valuec) & 0x1000) == 0 ? 0 : FLAG_H;
    final int flagC = (result & 0x10000) == 0 ? 0 : FLAG_C;
    final int flagZ = (result & 0xFFFF) == 0 ? FLAG_Z : 0;
    final int flagS = (result & 0x8000) == 0 ? 0 : FLAG_S;
    final int flagPV = (~(reg ^ value) & (reg ^ result) & 0x8000) == 0 ? 0 : FLAG_PV;
    
    final int flag5 = (result & (FLAG_RESERVED_5 << 8)) == 0 ? 0 : FLAG_RESERVED_5;
    final int flag3 = (result & (FLAG_RESERVED_3 << 8)) == 0 ? 0 : FLAG_RESERVED_3;
    
    writeReg16(2, result);

    updateFlags(0, flagC | flagH | flagZ | flagS | flagPV | flag5 | flag3, FLAG_N);

    this.tactCounter += 7;
  }

  private void doLD_mBC_A() {
    final int address = getRegisterPair(REGPAIR_BC);
    this._writemem8(address, this.regSet[REG_A]);
  }

  private void doLD_mDE_A() {
    final int address = getRegisterPair(REGPAIR_DE);
    this._writemem8(address, this.regSet[REG_A]);
  }

  private void doLD_mNN_HL() {
    _writemem16(_read16_for_pc(), readReg16(2));
  }

  private void doLD_mNN_A() {
    final int address = _read16_for_pc();
    this._writemem8(address, this.regSet[REG_A]);
  }

  private void doLD_A_mBC() {
    setRegister(REG_A, _readmem8(getRegisterPair(REGPAIR_BC)));
  }

  private void doLD_A_mDE() {
    setRegister(REG_A, _readmem8(getRegisterPair(REGPAIR_DE)));
  }

  private void doLD_HL_mem() {
    writeReg16(2, _readmem16(_read16_for_pc()));
  }

  private void doLD_A_mem() {
    final int address = _read16_for_pc();
    setRegister(REG_A, _readmem8(address));
  }

  private void doINCRegPair(final int p) {
    writeReg16(p, readReg16(p) + 1);
    this.tactCounter += 2;
  }

  private void doDECRegPair(final int p) {
    writeReg16(p, readReg16(p) - 1);
    this.tactCounter += 2;
  }

  private void doINCReg(final int y) {
    final int a = readReg8(y);
    final int flagPV = a == 0x7F ? FLAG_PV : 0;
    final int result = a + 1;
    writeReg8_UseCachedInstructionByte(y, result);

    final int flagS = (result & 0x80) == 0 ? 0 : FLAG_S;
    final int flagZ = (result & 0xFF) == 0 ? FLAG_Z : 0;
    final int flagH = (result & 0xF) == 0 ? FLAG_H : 0;

    final int flag5 = (result & FLAG_RESERVED_5) ==0 ? 0 : FLAG_RESERVED_5;
    final int flag3 = (result & FLAG_RESERVED_3) ==0 ? 0 : FLAG_RESERVED_3;
    
    updateFlags(FLAG_C, flag5|flag3|flagH | flagPV | flagS | flagZ, 0);
  }

  private void doDECReg(final int y) {
    final int a = readReg8(y);
    final int flagPV = a == 0x80 ? FLAG_PV : 0;
    final int result = a - 1;
    writeReg8_UseCachedInstructionByte(y, result);

    final int flagS = (result & 0x80) == 0 ? 0 : FLAG_S;
    final int flagZ = (result & 0xFF) == 0 ? FLAG_Z : 0;
    final int flagH = (result & 0xF) == 0xF ? FLAG_H : 0;

    final int flag5 = (result & FLAG_RESERVED_5) == 0 ? 0 : FLAG_RESERVED_5;
    final int flag3 = (result & FLAG_RESERVED_3) == 0 ? 0 : FLAG_RESERVED_3;

    updateFlags(FLAG_C, flag5 | flag3 | flagH | flagPV | flagS | flagZ, FLAG_N);
  }

  private void doLD_Reg_ByValue(final int y) {
    writeReg8(y, readInstructionByte(false));
  }

  private void doRLCA() {
    final int value = this.regSet[REG_A] << 1;
    final int flagc = (value & 0x100) == 0 ? 0 : FLAG_C;
    this.regSet[REG_A] = (byte) (value | ((value >>> 8) & 1));
    updateFlags(FLAG_Z | FLAG_RESERVED_3 | FLAG_PV | FLAG_S | FLAG_RESERVED_5, flagc, 0);
  }

  private void doRRCA() {
    final int value = this.regSet[REG_A] & 0xFF;
    final int flagc = value & FLAG_C;
    this.regSet[REG_A] = (byte) ((value >>> 1) | (flagc << 7));
    updateFlags(FLAG_Z | FLAG_RESERVED_3 | FLAG_PV | FLAG_S | FLAG_RESERVED_5, flagc, 0);
  }

  private void doRLA() {
    final int value = this.regSet[REG_A] << 1;
    this.regSet[REG_A] = (byte) (value | (this.regSet[REG_F] & FLAG_C));
    final int flagc = (value & 0x100) == 0 ? 0 : FLAG_C;
    updateFlags(FLAG_Z | FLAG_RESERVED_3 | FLAG_PV | FLAG_S | FLAG_RESERVED_5, flagc, 0);
  }

  private void doRRA() {
    final int value = this.regSet[REG_A] & 0xFF;
    this.regSet[REG_A] = (byte) ((value >>> 1) | (this.regSet[REG_F] & FLAG_C) << 7);
    final int flagc = value & FLAG_C;
    updateFlags(FLAG_Z | FLAG_RESERVED_3 | FLAG_PV | FLAG_S | FLAG_RESERVED_5, flagc, 0);
  }

  private void doDAA() {
    int addVal = 0;
    final int a = this.regSet[REG_A];
    final int low = a & 0xf;
    final int high = (a >> 4) & 0xF;

    final int flags = this.regSet[REG_F];

    final boolean flagH = (flags & FLAG_H) != 0;
    final boolean flagC = (flags & FLAG_C) != 0;

    int fC = flagC ? FLAG_C : 0;

    if ((flags & FLAG_N) == 0) {
      if (!flagC) {
        if ((high <= 0x08) && (!flagH) && (low >= 0x0a)) {
          addVal = 0x06;
        }
        else if ((high <= 0x09) && flagH && (low <= 0x03)) {
          addVal = 0x06;
        }
        else if ((high >= 0x0a) && (!flagH) && (low <= 0x09)) {
          addVal = 0x60;
        }
        else if ((high >= 0x09) && (!flagH) && (low >= 0x0a)) {
          addVal = 0x66;
        }
        else if ((high >= 0x0a) && flagH && (low <= 0x03)) {
          addVal = 0x66;
        }
      }
      else // Carry was set
      {
        if ((high <= 0x02) && (!flagH) && (low <= 0x09)) {
          addVal = 0x60;
        }
        else if ((high <= 0x02) && (!flagH) && (low >= 0x0a)) {
          addVal = 0x66;
        }
        else if ((high <= 0x03) && flagH && (low <= 0x03)) {
          addVal = 0x66;
        }
      }

      if (addVal >= 0x60) {
        fC = FLAG_C;
      }
    }
    else {
      if (!flagC) {
        if (flagH) {
          if ((high <= 0x08) && (low >= 0x06)) {
            addVal = 0xfa;
          }
        }
      }
      else // Carry was set
      {
        if (!flagC) {
          if ((high >= 0x07) && (low <= 0x09)) {
            addVal = 0xa0;
          }
        }
        else // H=1
        {
          if ((high >= 0x06) && (low >= 0x06)) {
            addVal = 0x9a;
          }
        }
      }
    }

    final int result = (a + addVal) & 0xFF;

    this.regSet[REG_A] = (byte) (result);

    final int fS = (result & 0x80) == 0 ? 0 : FLAG_S;
    final int fZ = result == 0 ? FLAG_Z : 0;
    final int fPV = checkParity(result);
    final int fH = (((a & 0xF) + (addVal & 0xF)) & 0x10) == 0 ? 0 : FLAG_H;

    updateFlags(FLAG_RESERVED_3 | FLAG_RESERVED_5 | FLAG_N, fS | fZ | fPV | fH, 0);
  }

  private void doCPL() {
    this.regSet[REG_A] ^= 0xFF;
    updateFlags(FLAG_C | FLAG_PV | FLAG_S | FLAG_RESERVED_3 | FLAG_RESERVED_5 | FLAG_Z, 0, FLAG_N | FLAG_H);
  }

  private void doSCF() {
    updateFlags(FLAG_PV | FLAG_S | FLAG_RESERVED_3 | FLAG_RESERVED_5 | FLAG_Z, 0, FLAG_C);
  }

  private void doCCF() {
    final int flags = this.regSet[REG_F];
    final boolean flagc = (flags & FLAG_C) != 0;
    updateFlags(FLAG_PV | FLAG_S | FLAG_RESERVED_3 | FLAG_RESERVED_5 | FLAG_Z, flagc ? FLAG_H : FLAG_C, 0);
  }

  private void doLDRegByReg(final int y, final int z) {
    writeReg8_forLdReg8Instruction(y, readReg8WithResetPrefixForIndex(z));
  }

  private void doRETByFlag(final int y) {
    if (checkCondition(y)) {
      final int sp = this.regSP;
      int sp1 = sp + 1;
      this.regPC = _readmem8(sp) | (_readmem8(sp1++) << 8);
      this.regSP = sp1 & 0xFFFF;
    }
    this.tactCounter++;
  }

  private void doRET() {
    final int sp = this.regSP;
    int sp1 = sp + 1;
    this.regPC = _readmem8(sp) | (_readmem8(sp1++) << 8);
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
  }

  private void doJP_HL() {
    this.regPC = readReg16(2);
  }

  private void doLD_SP_HL() {
    setRegister(REG_SP, getRegisterPair(REGPAIR_HL));
  }

  private void doJP_cc(final int cc) {
    final int address = _read16_for_pc();
    if (checkCondition(cc)) {
      this.regPC = address;
    }
  }

  private void doJP() {
    this.regPC = _read16_for_pc();
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
    writeReg16(2, _readmem8(stacktop) | (_readmem8(stacktop + 1) << 8));
    _writemem8(stacktop, (byte) hl);
    _writemem8(stacktop + 1, (byte) (hl >> 8));

    this.tactCounter += 3;
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
    this.tempIgnoreInterruption = true;
  }

  private void doEI() {
    this.iff1 = true;
    this.iff2 = true;
    this.tempIgnoreInterruption = true;
  }

  private void doCALL(final int y) {
    final int address = _read16_for_pc();
    if (checkCondition(y)) {
      _call(address);
      this.tactCounter++;
    }
  }

  private void doPUSH(final int p) {
    final int address = getRegister(REG_SP) - 2;
    _writemem16(address, readReg16_2(p));
    setRegister(REG_SP, address);
    this.tactCounter++;
  }

  private void doCALL() {
    final int address = _read16_for_pc();
    _call(address);
    this.tactCounter++;
  }

  private void doALU_A_Reg(final int op, final int reg) {
    _aluAccumulatorOp(op, readReg8(reg));
  }

  private void doALU_A_n(final int op) {
    _aluAccumulatorOp(op, readInstructionByte(false));
  }

  private void _aluAccumulatorOp(final int op, final int value) {
    final int a = this.regSet[REG_A] & 0xFF;

    final int result;

    final int flagN;
    final int flagH;
    final int flagC;
    final int flagPV;

    boolean changing = true;
    switch (op) {
      case 0: { // ADD
        result = a + value;
        flagN = 0;
        flagH = (((a & 0xF) + (value & 0xF)) & 0x10) == 0 ? 0 : FLAG_H;
        flagC = (result & 0x100) == 0 ? 0 : FLAG_C;
        flagPV = (~(a ^ value) & (a ^ result) & 0x80) == 0 ? 0 : FLAG_PV;
      }
      break;
      case 1: { // ADC
        result = a + value + (this.regSet[REG_F] & 1);
        flagN = 0;
        flagH = (((a & 0xF) + (value & 0xF)) & 0x10) == 0 ? 0 : FLAG_H;
        flagC = (result & 0x100) == 0 ? 0 : FLAG_C;
        flagPV = (~(a ^ value) & (a ^ result) & 0x80) == 0 ? 0 : FLAG_PV;
      }
      break;
      case 2: { // SUB
        result = a - value;
        flagN = FLAG_N;
        flagC = (result & 0x100) == 0 ? 0 : FLAG_C;
        flagPV = ((a ^ value) & (a ^ result) & 0x80) == 0 ? 0 : FLAG_PV;
        flagH = ((a ^ value ^ result) & 0x10) == 0 ? 0 : FLAG_H;
      }
      break;
      case 3: { // SBC
        result = a - value - (this.regSet[REG_F] & 1);
        flagN = FLAG_N;
        flagC = (result & 0x100) == 0 ? 0 : FLAG_C;
        flagPV = ((a ^ value) & (a ^ result) & 0x80) == 0 ? 0 : FLAG_PV;
        flagH = ((a ^ value ^ result) & 0x10) == 0 ? 0 : FLAG_H;
      }
      break;
      case 4: { // AND
        result = a & value;
        flagN = 0;
        flagC = 0;
        flagH = FLAG_H;
        flagPV = checkParity(result);
      }
      break;
      case 5: { // XOR
        result = a ^ value;
        flagN = 0;
        flagC = 0;
        flagH = 0;
        flagPV = checkParity(result);
      }
      break;
      case 6: { // OR
        result = a | value;
        flagN = 0;
        flagC = 0;
        flagH = 0;
        flagPV = checkParity(result);
      }
      break;
      case 7: { // CP
        changing = false;
        result = a - value;
        flagN = FLAG_N;
        flagC = (result & 0x100) == 0 ? 0 : FLAG_C;
        flagPV = ((a ^ value) & (a ^ result) & 0x80) == 0 ? 0 : FLAG_PV;
        flagH = ((a ^ value ^ result) & 0x10) == 0 ? 0 : FLAG_H;
      }
      break;
      default:
        throw new Error("Detected unexpected ALU operation [" + op + ']');
    }
    if (changing) {
      this.regSet[REG_A] = (byte) result;
    }

    final int flagZ = (result & 0xFF) == 0 ? FLAG_Z : 0;
    final int flagS = (result & 0x80) == 0 ? 0 : FLAG_S;

    updateFlags(FLAG_RESERVED_3 | FLAG_RESERVED_5, flagZ | flagS | flagC | flagPV | flagH | flagN, 0);
  }

  private void doRST(final int address) {
    _call(address);
    this.tactCounter++;
  }

  private int doRollShift(final int op, final int reg) {
    final int value = readReg8(reg);
    final int result;
    int flagC = this.regSet[REG_F] & FLAG_C;
    switch (op) {
      case 0: { // RLC
        result = (value << 1) | (value >>> 7);
        flagC = (value >>> 7) & FLAG_C;
      }
      break;
      case 1: { // RRC
        flagC = value & FLAG_C;
        result = (value >>> 1) | (flagC << 7);
      }
      break;
      case 2: { // RL
        result = (value << 1) | flagC;
        flagC = (value >>> 7) & FLAG_C;
      }
      break;
      case 3: { // RR
        result = (value >>> 1) | (flagC << 7);
        flagC = value & FLAG_C;
      }
      break;
      case 4: { // SLA
        result = value << 1;
        flagC = (value >>> 7) & FLAG_C;
      }
      break;
      case 5: { // SRA
        flagC = value & FLAG_C;
        result = (value >>> 1) | (value & 0x80);
      }
      break;
      case 6: { // SLL
        result = (value << 1) | 1;
        flagC = (value >>> 7) & FLAG_C;
      }
      break;
      case 7: { // SRL
        result = value >>> 1;
        flagC = value & FLAG_C;
      }
      break;
      default:
        throw new Error("Unexpected operation index [" + op + ']');
    }

    final int flagZ = result == 0 ? FLAG_Z : 0;
    final int flagS = (result & 0x80) == 0 ? 0 : FLAG_S;
    final int flagPV = checkParity(result & 0xFF);

    writeReg8_UseCachedInstructionByte(reg, result);

    updateFlags(FLAG_RESERVED_3 | FLAG_RESERVED_5, flagC | flagPV | flagS | flagZ, 0);

    return result;
  }

  private void doROTmem_LDreg(final int reg, final int op) {
    writeReg8(reg, doRollShift(op, 6));
  }

  private void doBIT(final int bit, final int reg) {
    final int flagZ = (readReg8(reg) & (1 << bit)) == 0 ? FLAG_Z : 0;
    //        lg_flagS = (bit == 0x7) ? !lg_flagZ : false;
    final int flagS = (bit == 0x7) ? (flagZ ^ FLAG_Z)<<1 : 0;
    updateFlags(FLAG_C, flagS | flagZ | (flagZ >>> 4), FLAG_H);
    if (reg == 6) {
      this.tactCounter++;
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
    final int value = _readport(port);

    final int flagZ = value == 0 ? FLAG_Z : 0;
    final int flagS = (value & 0x80) == 0 ? 0 : FLAG_S;
    final int flagPV = checkParity(value);

    updateFlags(FLAG_C | FLAG_RESERVED_3 | FLAG_RESERVED_5, flagZ | flagS | flagPV, 0);
  }

  private void doIN_C(final int y) {
    final int port = getRegisterPair(REGPAIR_BC);
    final int value = _readport(port);
    writeReg8(y, value);

    final int flagZ = value == 0 ? FLAG_Z : 0;
    final int flagS = (value & 0x80) == 0 ? 0 : FLAG_S;
    final int flagPV = checkParity(value);

    updateFlags(FLAG_C | FLAG_RESERVED_3 | FLAG_RESERVED_5, flagZ | flagS | flagPV, 0);
  }

  private void doOUT_C() {
    final int port = ((this.regSet[REG_B] & 0xFF) << 8) | (this.regSet[REG_C] & 0xFF);
    _writeport(port, 0x00);
  }

  private void doOUT_C(final int y) {
    final int port = ((this.regSet[REG_B] & 0xFF) << 8) | (this.regSet[REG_C] & 0xFF);
    _writeport(port, readReg8(y));
  }

  private void doLD_mNN_RegP(final int p) {
    _writemem16(_read16_for_pc(), readReg16(p));
  }

  private void doLD_RegP_mNN(final int p) {
    writeReg16(p, _readmem16(_read16_for_pc()));
  }

  private void doNEG() {
    final int a = this.regSet[REG_A] & 0xFF;
    final int result = 0 - a;
    this.regSet[REG_A] = (byte) result;

    final int flagPV = a == 0x80 ? FLAG_PV : 0;
    final int flagC = a != 0 ? FLAG_C : 0;
    final int flagZ = result == 0 ? FLAG_Z : 0;
    final int flagS = (result & 0x80) == 0 ? 0 : FLAG_S;
    final int flagH = ((a ^ result) & 0x10) == 0 ? 0 : FLAG_H;

    updateFlags(FLAG_RESERVED_3 | FLAG_RESERVED_5, flagPV | flagC | flagH | flagZ | flagS | flagH, FLAG_N);
  }

  private void doRETI() {
    doRET();
    this.bus.onRETI(this);
    this.insideBlockInstruction = this.insideBlockInstructionPrev;
  }

  private void doRETN() {
    doRET();
    this.iff1 = this.iff2;
    this.insideBlockInstruction = this.insideBlockInstructionPrev;
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
    this.tactCounter++;
  }

  private void doLD_R_A() {
    setRegister(REG_R, getRegister(REG_A));
    this.tactCounter++;
  }

  private void doLD_A_I() {
    final int value = getRegister(REG_I);
    setRegister(REG_A, value);
    updateFlags(FLAG_C, (value == 0 ? FLAG_Z : 0) | (this.iff2 ? FLAG_PV : 0) | (value & FLAG_S), 0);
    this.tactCounter++;
  }

  private void doLD_A_R() {
    final int value = getRegister(REG_R);
    setRegister(REG_A, value);
    updateFlags(FLAG_C, (value == 0 ? FLAG_Z : 0) | (this.iff2 ? FLAG_PV : 0) | (value & FLAG_S), 0);
    this.tactCounter++;
  }

  private void doRRD() {
    int a = this.regSet[REG_A] & 0xFF;
    int addr = getRegisterPair(REGPAIR_HL);
    int val = _readmem8(addr);

    final int lowa = a & 0x0F;
    a = (a & 0xF0) | (val & 0x0F);
    val = (val >>> 4) | (lowa << 4);

    this.regSet[REG_A] = (byte) a;
    _writemem8(addr, (byte) val);

    final int flagZ = a == 0 ? FLAG_Z : 0;
    final int flagS = (a & 0x80) == 0 ? 0 : FLAG_S;
    final int flagPV = checkParity(a);

    updateFlags(FLAG_C | FLAG_RESERVED_3 | FLAG_RESERVED_5, flagZ | flagS | flagPV, 0);

    this.tactCounter += 4;
  }

  private void doRLD() {
    int a = this.regSet[REG_A] & 0xFF;
    final int addr = getRegisterPair(REGPAIR_HL);
    final int val = (_readmem8(addr) << 4) | (a & 0xF);
    a = (a & 0xF0) | (val >>> 8);

    this.regSet[REG_A] = (byte) a;
    _writemem8(addr, (byte) val);

    final int flagZ = a == 0 ? FLAG_Z : 0;
    final int flagS = (a & 0x80) == 0 ? 0 : FLAG_S;
    final int flagPV = checkParity(a);

    updateFlags(FLAG_C | FLAG_RESERVED_3 | FLAG_RESERVED_5, flagZ | flagS | flagPV, 0);

    this.tactCounter += 4;
  }

  private boolean doBLI(final int y, final int z) {
    boolean insideLoop = false;
    switch (y) {
      case 4: {
        switch (z) {
          case 0: doLDI(); break;
          case 1: doCPI(); break;
          case 2: doINI(); break;
          case 3: doOUTI(); break;
        }
      }
      break;
      case 5: {
        switch (z) {
          case 0: doLDD();break;
          case 1: doCPD();break;
          case 2: doIND();break;
          case 3: doOUTD();break;
        }
      }
      break;
      case 6: {
        switch (z) {
          case 0: insideLoop = doLDIR();break;
          case 1: insideLoop = doCPIR();break;
          case 2: insideLoop = doINIR();break;
          case 3: insideLoop = doOTIR();break;
        }
      }
      break;
      case 7: {
        switch (z) {
          case 0: insideLoop = doLDDR();break;
          case 1: insideLoop = doCPDR();break;
          case 2: insideLoop = doINDR();break;
          case 3: insideLoop = doOTDR();break;
        }
      }
      break;
    }
    return insideLoop;
  }

  private void doLDI() {
    int hl = getRegisterPair(REGPAIR_HL);
    int de = getRegisterPair(REGPAIR_DE);
    _writemem8(de++, (byte) _readmem8(hl++));
    setRegisterPair(REGPAIR_HL, hl);
    setRegisterPair(REGPAIR_DE, de);

    final int bc = (getRegisterPair(REGPAIR_BC) - 1) & 0xFFFF;
    setRegisterPair(REGPAIR_BC, bc);

    updateFlags(FLAG_C | FLAG_Z | FLAG_S, bc == 0 ? 0 : FLAG_PV, 0);

    this.tactCounter += 2;
  }

  private boolean doLDIR() {
    doLDI();
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_PV) != 0) {
      this.regSet[REG_F] &= (~FLAG_PV & 0xFF);
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.tactCounter += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doCPI() {
    int hl = getRegisterPair(REGPAIR_HL);
    final int m = _readmem8(hl++);
    final int a = getRegister(REG_A);
    final int r = a - m;
    setRegisterPair(REGPAIR_HL, hl);
    final int bc = getRegisterPair(REGPAIR_BC) - 1;
    setRegisterPair(REGPAIR_BC, bc);
    updateFlags(FLAG_C, ((r & 0x80) == 0 ? 0 : FLAG_S) | (r == 0 ? FLAG_Z : 0) | (((a ^ m ^ r) & 0x10) == 0 ? 0 : FLAG_H) | (bc == 0 ? 0 : FLAG_PV), FLAG_N);

    this.tactCounter += 5;
  }

  private boolean doCPIR() {
    doCPI();
    boolean loopNonCompleted = true;
    final int flags = this.regSet[REG_F];
    if ((flags & (FLAG_Z | FLAG_PV)) == FLAG_PV) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.tactCounter += 5;
    }else{
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doINI() {
    final int bc = getRegisterPair(REGPAIR_BC);
    int hl = getRegisterPair(REGPAIR_HL);
    final byte value = (byte) _readport(bc);
    _writemem8(hl++, value);
    final int b = ((bc >>> 8) - 1) & 0xFF;
    this.regSet[REG_B] = (byte) b;
    setRegisterPair(REGPAIR_HL, hl);

    updateFlags(FLAG_C | FLAG_H | FLAG_PV | FLAG_S | FLAG_RESERVED_3 | FLAG_RESERVED_5, (b == 0 ? FLAG_Z : 0), FLAG_N);
    this.tactCounter++;
  }

  private boolean doINIR() {
    doINI();
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.tactCounter += 5;
    }else{
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doIND() {
    final int bc = getRegisterPair(REGPAIR_BC);
    int hl = getRegisterPair(REGPAIR_HL);
    final byte value = (byte) _readport(bc);
    _writemem8(hl--, value);
    final int b = ((bc >>> 8) - 1) & 0xFF;
    this.regSet[REG_B] = (byte) b;
    setRegisterPair(REGPAIR_HL, hl);

    updateFlags(FLAG_C | FLAG_H | FLAG_PV | FLAG_S | FLAG_RESERVED_3 | FLAG_RESERVED_5, (b == 0 ? FLAG_Z : 0), FLAG_N);
    this.tactCounter++;
  }

  private boolean doINDR() {
    doIND();
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.tactCounter += 5;
    }else{
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doOUTI() {
    final int bc = getRegisterPair(REGPAIR_BC);
    int hl = getRegisterPair(REGPAIR_HL);
    final int value = _readmem8(hl++);
    _writeport(bc, value);
    final int b = ((bc >>> 8) - 1) & 0xFF;
    this.regSet[REG_B] = (byte) b;
    setRegisterPair(REGPAIR_HL, hl);
    updateFlags(FLAG_C | FLAG_H | FLAG_PV | FLAG_S | FLAG_RESERVED_3 | FLAG_RESERVED_5, (b == 0 ? FLAG_Z : 0), FLAG_N);
    this.tactCounter++;
  }

  private boolean doOTIR() {
    doOUTI();
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.tactCounter += 5;
    }else{
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doOUTD() {
    final int bc = getRegisterPair(REGPAIR_BC);
    int hl = getRegisterPair(REGPAIR_HL);
    final int value = _readmem8(hl--);
    _writeport(bc, value);
    final int b = ((bc >>> 8) - 1) & 0xFF;
    this.regSet[REG_B] = (byte) b;
    setRegisterPair(REGPAIR_HL, hl);
    updateFlags(FLAG_C | FLAG_H | FLAG_PV | FLAG_S | FLAG_RESERVED_3 | FLAG_RESERVED_5, (b == 0 ? FLAG_Z : 0), FLAG_N);
    this.tactCounter++;
  }

  private boolean doOTDR() {
    doOUTD();
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.tactCounter += 5;
    }else{
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doLDD() {
    int hl = getRegisterPair(REGPAIR_HL);
    int de = getRegisterPair(REGPAIR_DE);
    _writemem8(de--, (byte) _readmem8(hl--));
    setRegisterPair(REGPAIR_HL, hl);
    setRegisterPair(REGPAIR_DE, de);

    final int bc = (getRegisterPair(REGPAIR_BC) - 1) & 0xFFFF;
    setRegisterPair(REGPAIR_BC, bc);

    updateFlags(FLAG_C | FLAG_Z | FLAG_S, bc == 0 ? 0 : FLAG_PV, 0);

    this.tactCounter += 2;
  }

  private boolean doLDDR() {
    doLDD();
    boolean loopNonCompleted = true;
    if ((this.regSet[REG_F] & FLAG_PV) != 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.tactCounter += 5;
    }else{
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doCPD() {
    int hl = getRegisterPair(REGPAIR_HL);
    final int m = _readmem8(hl--);
    final int a = getRegister(REG_A);
    final int r = a - m;
    setRegisterPair(REGPAIR_HL, hl);
    final int bc = getRegisterPair(REGPAIR_BC) - 1;
    setRegisterPair(REGPAIR_BC, bc);
    updateFlags(FLAG_C, ((r & 0x80) == 0 ? 0 : FLAG_S) | (r == 0 ? FLAG_Z : 0) | (((a ^ m ^ r) & 0x10) == 0 ? 0 : FLAG_H) | (bc == 0 ? 0 : FLAG_PV), FLAG_N);

    this.tactCounter += 5;
  }

  private boolean doCPDR() {
    doCPD();
    boolean loopNonCompleted = true;
    final int flags = this.regSet[REG_F];
    if ((flags & (FLAG_Z | FLAG_PV)) == FLAG_PV) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.tactCounter += 5;
    }else{
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

}
