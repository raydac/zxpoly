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

import java.util.Locale;

/**
 * Tables and some flag set algorithms were copied and adapted from
 * https://github.com/anotherlin/z80emu project, opcode decoding is based on
 * http://www.z80.info/decoding.htm
 */
public final class Z80 {

  public static final int REG_UNKNOWN = -1;
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
  public static final int SIGNAL_IN_ALL_INACTIVE =
      SIGNAL_IN_nINT | SIGNAL_IN_nNMI | SIGNAL_IN_nRESET | SIGNAL_IN_nWAIT;
  public static final int SIGNAL_OUT_nM1 = 1;
  public static final int SIGNAL_OUT_nHALT = 2;
  public static final int SIGNAL_OUT_ALL_INACTIVE = SIGNAL_OUT_nHALT | SIGNAL_OUT_nM1;

  // if the flag is true then it makes green z80bltst.tap v5.0 2022-01-11 by Ped7g
  // but in the same time FUSE Z80 tests are red for block commands
  private static final byte[] FTABLE_SZYX;
  private static final byte[] FTABLE_SZYXP;
  private static final int FLAG_S_SHIFT = 7;
  public static final int FLAG_S = 1 << FLAG_S_SHIFT;
  private static final int FLAG_Z_SHIFT = 6;
  public static final int FLAG_Z = 1 << FLAG_Z_SHIFT;
  private static final int FLAG_SZ = FLAG_S | FLAG_Z;
  private static final int FLAG_Y_SHIFT = 5;
  public static final int FLAG_Y = 1 << FLAG_Y_SHIFT;
  private static final int FLAG_H_SHIFT = 4;
  public static final int FLAG_H = 1 << FLAG_H_SHIFT;
  private static final int FLAG_X_SHIFT = 3;
  public static final int FLAG_X = 1 << FLAG_X_SHIFT;
  private static final int FLAG_XY = FLAG_X | FLAG_Y;
  private static final int FLAG_SYX = FLAG_S | FLAG_X | FLAG_Y;
  private static final int FLAG_PV_SHIFT = 2;
  public static final int FLAG_PV = 1 << FLAG_PV_SHIFT;
  private static final int FLAG_SZPV = FLAG_S | FLAG_Z | FLAG_PV;
  private static final byte[] FTABLE_OVERFLOW = new byte[] {
      0, (byte) FLAG_PV, (byte) FLAG_PV, 0
  };
  private static final int FLAG_N_SHIFT = 1;
  public static final int FLAG_N = 1 << FLAG_N_SHIFT;
  private static final int FLAG_C_SHIFT = 0;
  public static final int FLAG_C = 1 << FLAG_C_SHIFT;
  private static final int FLAG_SZC = FLAG_SZ | FLAG_C;
  private static final int FLAG_HC = FLAG_H | FLAG_C;

  static {
    // fill tables SZYX and SZYXP
    FTABLE_SZYX = new byte[0x100];
    FTABLE_SZYXP = new byte[0x100];
    for (int i = 0; i < 256; i++) {
      final int szyx = i & (FLAG_X | FLAG_Y | FLAG_S);

      FTABLE_SZYX[i] = (byte) szyx;

      int j = i;
      int parity = 0;
      for (int k = 0; k < 8; k++) {
        parity ^= (j & 1);
        j >>>= 1;
      }

      FTABLE_SZYXP[i] = (byte) (szyx | (parity != 0 ? 0 : FLAG_PV));
    }
    FTABLE_SZYX[0] |= FLAG_Z;
    FTABLE_SZYXP[0] |= FLAG_Z;
  }

  private final Z80CPUBus bus;
  private int regA;
  private int regF;
  private int regB;
  private int regC;
  private int regD;
  private int regE;
  private int regH;
  private int regL;
  private int altA;
  private int altF;
  private int altB;
  private int altC;
  private int altD;
  private int altE;
  private int altH;
  private int altL;
  private int memptr;
  private boolean iff1;
  private boolean iff2;
  private int im;
  private int regIX;
  private int regIY;
  private int regSP;
  private int regPC;
  private int regI;
  private int regR;
  private int tiStates;
  private int lastM1InstructionByte = -1;
  private int lastInstructionByte = -1;
  private int cbDisplacementByte = -1;
  private int prefix;
  private int outSignals = 0xFFFFFFFF;
  private int prevInSignals = 0xFFFFFFFF;
  private boolean stepAllowsInterruption;
  private boolean nmiTrigger;
  private int resetCycle = 0;

  private int internalRegQ;
  private int internalRegLastQ;

  public Z80(final Z80CPUBus bus) {
    if (bus == null) {
      throw new NullPointerException("The CPU BUS must not be null");
    }
    this.bus = bus;
    _reset(0);
    _reset(1);
    _reset(2);
    this.tiStates = 0;
  }

  /**
   * Make full copy of state of the source CPU. NB! pointer to bus will be
   * copied!
   *
   * @param cpu source CPU which state should be copied, must not be null
   */
  public Z80(final Z80 cpu) {
    this.memptr = cpu.memptr;
    this.prefix = cpu.prefix;
    this.internalRegQ = cpu.internalRegQ;
    this.internalRegLastQ = cpu.internalRegLastQ;
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
    this.copyGpRegistersFrom(cpu);
    this.lastM1InstructionByte = cpu.lastM1InstructionByte;
    this.lastInstructionByte = cpu.lastInstructionByte;
    this.tiStates = cpu.tiStates;
    this.cbDisplacementByte = cpu.cbDisplacementByte;
    this.outSignals = cpu.outSignals;
    this.prevInSignals = cpu.prevInSignals;
    this.stepAllowsInterruption = cpu.stepAllowsInterruption;
    this.nmiTrigger = cpu.nmiTrigger;
    this.bus = cpu.bus;
  }

  /**
   * Parse string with id of registers and prepare bit vector for it.
   * main set: <b>A,F,B,C,D,E,H,L,1(F without C)</b>
   * alt.set: <b>sa,f,b,c,d,e,h,l,0(F' without C)</b>
   * special: <b>T(use PTR reg values from main CPU)</b>
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
      if (c == 'T') {
        continue;
      }
      final int index = allowedPositions.indexOf(c);
      if (index < 0) {
        throw new IllegalArgumentException(
            "Unexpected char: " + c + " expected one from '" + allowedPositions + "'");
      } else {
        result |= 1 << index;
      }
    }
    return result;
  }

  private static boolean isLoHiFront(final int oldValue, final int newValue, final int mask) {
    final int xored = oldValue ^ newValue;
    return (xored & mask) == mask && (newValue & mask) == mask;
  }

  private static boolean isHiLoFront(final int oldValue, final int newValue, final int mask) {
    final int xored = oldValue ^ newValue;
    return (xored & mask) == mask && (oldValue & mask) == mask;
  }

  public Z80 fillByState(final Z80 sourceCpu) {
    this.memptr = sourceCpu.memptr;
    this.prefix = sourceCpu.prefix;
    this.resetCycle = sourceCpu.resetCycle;
    this.iff1 = sourceCpu.iff1;
    this.iff2 = sourceCpu.iff2;
    this.im = sourceCpu.im;
    this.regI = sourceCpu.regI;
    this.regIX = sourceCpu.regIX;
    this.regIY = sourceCpu.regIY;
    this.regPC = sourceCpu.regPC;
    this.regR = sourceCpu.regR;
    this.regSP = sourceCpu.regSP;
    this.copyGpRegistersFrom(sourceCpu);
    this.lastM1InstructionByte = sourceCpu.lastM1InstructionByte;
    this.lastInstructionByte = sourceCpu.lastInstructionByte;
    this.tiStates = sourceCpu.tiStates;
    this.cbDisplacementByte = sourceCpu.cbDisplacementByte;
    this.outSignals = sourceCpu.outSignals;
    this.prevInSignals = sourceCpu.prevInSignals;
    this.stepAllowsInterruption = sourceCpu.stepAllowsInterruption;
    this.nmiTrigger = sourceCpu.nmiTrigger;
    return this;
  }

  public int getMemPtr() {
    return this.memptr;
  }

  public void setMemPtr(final int value) {
    this.memptr = value & 0xFFFF;
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

  public int getPrevInSignals() {
    return this.prevInSignals;
  }

  public int getPC() {
    return this.regPC;
  }

  public void setIFF(final boolean iff1, final boolean iff2) {
    this.iff1 = iff1;
    this.iff2 = iff2;
  }

  public void setRegisterPair(final int regPair, final int value) {
    this.setRegisterPair(regPair, value, false);
  }

  public void setRegisterPair(final int regPair, final int value, final boolean alt) {
    final int hi = (value >>> 8) & 0xFF;
    final int lo = value & 0xFF;
    if (alt) {
      switch (regPair) {
        case REGPAIR_AF:
          this.altA = hi;
          this.altF = lo;
          return;
        case REGPAIR_BC:
          this.altB = hi;
          this.altC = lo;
          return;
        case REGPAIR_DE:
          this.altD = hi;
          this.altE = lo;
          return;
        case REGPAIR_HL:
          this.altH = hi;
          this.altL = lo;
          return;
        default:
          throw new Error("Unexpected register pair [" + regPair + ']');
      }
    }
    switch (regPair) {
      case REGPAIR_AF:
        this.regA = hi;
        this.regF = lo;
        return;
      case REGPAIR_BC:
        this.regB = hi;
        this.regC = lo;
        return;
      case REGPAIR_DE:
        this.regD = hi;
        this.regE = lo;
        return;
      case REGPAIR_HL:
        this.regH = hi;
        this.regL = lo;
        return;
      default:
        throw new Error("Unexpected register pair [" + regPair + ']');
    }
  }

  public int getRegisterPair(final int regPair) {
    return this.getRegisterPair(regPair, false);
  }

  public int getRegisterPair(final int regPair, final boolean alt) {
    if (alt) {
      return switch (regPair) {
        case REGPAIR_AF -> (this.altA << 8) | this.altF;
        case REGPAIR_BC -> (this.altB << 8) | this.altC;
        case REGPAIR_DE -> (this.altD << 8) | this.altE;
        case REGPAIR_HL -> (this.altH << 8) | this.altL;
        default -> throw new Error("Unexpected register pair [" + regPair + ']');
      };
    }
    return switch (regPair) {
      case REGPAIR_AF -> (this.regA << 8) | this.regF;
      case REGPAIR_BC -> (this.regB << 8) | this.regC;
      case REGPAIR_DE -> (this.regD << 8) | this.regE;
      case REGPAIR_HL -> (this.regH << 8) | this.regL;
      default -> throw new Error("Unexpected register pair [" + regPair + ']');
    };
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
      default:
        this.writeGp8(alt, reg, value);
        break;
    }
  }

  public int getRegister(final int reg) {
    return this.getRegister(reg, false);
  }

  public int getRegister(final int reg, final boolean alt) {
    return switch (reg) {
      case REG_IX -> this.regIX;
      case REG_IY -> this.regIY;
      case REG_PC -> this.regPC;
      case REG_SP -> this.regSP;
      case REG_I -> this.regI;
      case REG_R -> this.regR;
      default -> this.readGp8(alt, reg);
    };
  }


  private void copyGpRegistersFrom(final Z80 src) {
    this.regA = src.regA;
    this.regF = src.regF;
    this.regB = src.regB;
    this.regC = src.regC;
    this.regD = src.regD;
    this.regE = src.regE;
    this.regH = src.regH;
    this.regL = src.regL;
    this.altA = src.altA;
    this.altF = src.altF;
    this.altB = src.altB;
    this.altC = src.altC;
    this.altD = src.altD;
    this.altE = src.altE;
    this.altH = src.altH;
    this.altL = src.altL;
  }

  private int readGp8(final boolean alt, final int reg) {
    if (alt) {
      return switch (reg) {
        case REG_A -> this.altA;
        case REG_F -> this.altF;
        case REG_B -> this.altB;
        case REG_C -> this.altC;
        case REG_D -> this.altD;
        case REG_E -> this.altE;
        case REG_H -> this.altH;
        case REG_L -> this.altL;
        default -> throw new Error("Unexpected register [" + reg + ']');
      };
    }
    return switch (reg) {
      case REG_A -> this.regA;
      case REG_F -> this.regF;
      case REG_B -> this.regB;
      case REG_C -> this.regC;
      case REG_D -> this.regD;
      case REG_E -> this.regE;
      case REG_H -> this.regH;
      case REG_L -> this.regL;
      default -> throw new Error("Unexpected register [" + reg + ']');
    };
  }

  private void writeGp8(final boolean alt, final int reg, final int value) {
    final int masked = value & 0xFF;
    if (alt) {
      switch (reg) {
        case REG_A:
          this.altA = masked;
          return;
        case REG_F:
          this.altF = masked;
          return;
        case REG_B:
          this.altB = masked;
          return;
        case REG_C:
          this.altC = masked;
          return;
        case REG_D:
          this.altD = masked;
          return;
        case REG_E:
          this.altE = masked;
          return;
        case REG_H:
          this.altH = masked;
          return;
        case REG_L:
          this.altL = masked;
          return;
        default:
          throw new Error("Unexpected register [" + reg + ']');
      }
    }
    switch (reg) {
      case REG_A:
        this.regA = masked;
        return;
      case REG_F:
        this.regF = masked;
        return;
      case REG_B:
        this.regB = masked;
        return;
      case REG_C:
        this.regC = masked;
        return;
      case REG_D:
        this.regD = masked;
        return;
      case REG_E:
        this.regE = masked;
        return;
      case REG_H:
        this.regH = masked;
        return;
      case REG_L:
        this.regL = masked;
        return;
      default:
        throw new Error("Unexpected register [" + reg + ']');
    }
  }

  public int getState() {
    return this.outSignals;
  }

  public Z80CPUBus getBus() {
    return this.bus;
  }

  public void setTstates(final int tiStates) {
    this.tiStates = Math.max(0, tiStates);
  }

  public void addTstates(final int tiStates) {
    this.tiStates += tiStates;
  }

  public int getStepTstates() {
    return this.tiStates;
  }

  private void _reset(final int cycle) {
    switch (cycle % 3) {
      case 0: {
        this.internalRegQ = 0;
        this.internalRegLastQ = 0;
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
        this.regA = 0xFF;
        this.regF = 0xFF;
        this.altA = 0xFF;
        this.altF = 0xFF;
      }
      break;
      default: {
        throw new Error("Unexpected call");
      }
    }

    this.im = 0;
    this.cbDisplacementByte = -1;

    this.stepAllowsInterruption = false;

    this.prefix = 0;
    this.outSignals = SIGNAL_OUT_ALL_INACTIVE;

    this.tiStates += 3;
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

    this.bus.onInterrupt(this, ctx, false);

    switch (this.im) {
      case 0: {
        _step(ctx, this.bus.onCPURequestDataLines(this, ctx) & 0xFF, true);
      }
      break;
      case 1: {
        _step(ctx, 0xFF, true);
      }
      break;
      case 2: {
        final int vector = ((_readSpecRegValue(ctx, REG_I, this.regI) & 0xFF) << 8)
            | (this.bus.onCPURequestDataLines(this, ctx) & 0xFF);
        final int address = _readmem16(ctx, vector);
        this.setMemPtr(address);
        _call(ctx, address);
        this.tiStates++;
      }
      break;
      default:
        throw new Error("Unexpected IM mode [" + this.im + ']');
    }

    this.tiStates += 6;
  }

  private void _incR() {
    this.regR = (this.regR & 0x80) | ((this.regR + 1) & 0x7F);
  }

  public int getSP() {
    return this.regSP;
  }

  private void _nmi(final int ctx) {
    this.bus.onInterrupt(this, ctx, true);

    _resetHalt();
    this.iff1 = false;
    this.nmiTrigger = false;
    _call(ctx, 0x66);
    this.tiStates += 5;
  }

  private void _writemem8(final int ctx, final int address, final byte value) {
    this.bus.writeMemory(this, ctx, address & 0xFFFF, value);
    this.tiStates += 3;
  }

  private int _readNextPcAddressedWord(final int ctx) {
    return readInstrOrPrefix(ctx, false) | (readInstrOrPrefix(ctx, false) << 8);
  }

  private void _writemem16(final int ctx, final int address, final int value) {
    this._writemem8(ctx, address, (byte) value);
    this._writemem8(ctx, address + 1, (byte) (value >> 8));
  }

  private void _call(final int ctx, final int address) {
    final int sp = (_readPtr(ctx, REG_SP, this.regSP) - 2) & 0xFFFF;
    _writemem8(ctx, sp, (byte) this.regPC);
    _writemem8(ctx, sp + 1, (byte) (this.regPC >> 8));
    this.regPC = address;
    this.regSP = sp;
  }

  private int _readSpecRegValue(final int ctx, final int reg, final int origValue) {
    return this.bus.readSpecRegValue(this, ctx, reg, origValue);
  }

  private int _readSpecRegPairValue(final int ctx, final int regPair, final int origValue) {
    return this.bus.readSpecRegPairValue(this, ctx, regPair, origValue);
  }

  private int _portAddrFromReg(final int ctx, final int reg, final int origValue) {
    return this.bus.readRegPortAddr(this, ctx, reg, origValue);
  }

  private int _readport(final int ctx, final int port) {
    this.tiStates += 4;
    return this.bus.readPort(this, ctx, port & 0xFFFF) & 0xFF;
  }

  private void _writeport(final int ctx, final int port, final int value) {
    this.bus.writePort(this, ctx, port & 0xFFFF, (byte) value);
    this.tiStates += 4;
  }

  private int _readmem8(final int ctx, final int address) {
    this.tiStates += 3;
    return this.bus.readMemory(this, ctx, address & 0xFFFF, false, false) & 0xFF;
  }

  private int _readmem8withM1(final int ctx, final int address) {
    this.tiStates += 3;
    return this.bus.readMemory(this, ctx, address & 0xFFFF, true, false) & 0xFF;
  }

  private int _readmem16(final int ctx, final int address) {
    return _readmem8(ctx, address) | (_readmem8(ctx, address + 1) << 8);
  }

  private int _read_ixiy_d(final int ctx) {
    if (this.cbDisplacementByte < 0) {
      return readInstrOrPrefix(ctx, false);
    } else {
      this.tiStates -= 5;
      return this.cbDisplacementByte;
    }
  }

  private int readInstrOrPrefix(final int ctx, final boolean m1) {
    final boolean nonDisplacementByte = (this.prefix & 0xFF00) == 0;

    final int pc = this.regPC;
    this.regPC = (this.regPC + 1) & 0xFFFF;
    this.outSignals =
        (m1 ? this.outSignals & (~SIGNAL_OUT_nM1) : this.outSignals | SIGNAL_OUT_nM1) & 0xFF;
    final int result = this.bus.readMemory(this, ctx, pc, m1 && nonDisplacementByte, true) & 0xFF;
    this.outSignals = this.outSignals | SIGNAL_OUT_nM1;

    this.tiStates += m1 ? 4 : 3;

    if (m1) {
      this.lastM1InstructionByte = result;
      if (nonDisplacementByte) {
        _incR();
      }
    }

    this.lastInstructionByte = result;

    return result;
  }

  private int normalizedPrefix() {
    return (this.prefix & 0xFF) == 0xCB ? this.prefix >>> 8 : this.prefix;
  }

  private boolean checkCondition(final int cc) {
    final boolean result;
    final int flags = this.regF;
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

  private int _readPtr(final int ctx, final int reg, final int origValue) {
    return this.bus.readPtr(this, ctx, reg, origValue);
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
    this.lastInstructionByte = src.lastInstructionByte;
    this.lastM1InstructionByte = src.lastM1InstructionByte;
    this.prevInSignals = src.prevInSignals;
    this.stepAllowsInterruption = src.stepAllowsInterruption;
    this.nmiTrigger = src.nmiTrigger;

    if (packedRegisterFlags == 0) {
      this.regPC = src.regPC;
      this.regSP = src.regSP;
    } else {
      //"AFBCDEHL XxYy10PSs afbcdehl"
      int pos = 0;
      while (packedRegisterFlags != 0) {
        if ((packedRegisterFlags & 1) != 0) {
          if (pos < 8) {
            this.writeGp8(false, pos, src.readGp8(false, pos));
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
                this.regF = (this.regF & FLAG_C) | (src.regF & ~FLAG_C);
                break;
              case 5:
                this.altF = (this.altF & FLAG_C) | (src.altF & ~FLAG_C);
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
            this.writeGp8(true, reg, src.readGp8(true, reg));
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
        return this.regB;
      case 1:
        return this.regC;
      case 2:
        return this.regD;
      case 3:
        return this.regE;
      case 4: {
        switch (this.normalizedPrefix()) {
          case 0x00:
            return this.regH;
          case 0xDD:
            return (this.regIX >> 8) & 0xFF;
          case 0xFD:
            return (this.regIY >> 8) & 0xFF;
        }
      }
      break;
      case 5: {
        switch (this.normalizedPrefix()) {
          case 0x00:
            return this.regL;
          case 0xDD:
            return this.regIX & 0xFF;
          case 0xFD:
            return this.regIY & 0xFF;
        }
      }
      break;
      case 6: {
        switch (this.normalizedPrefix()) {
          case 0x00: {
            final int address = this._readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL));
            return this._readmem8(ctx, address);
          }
          case 0xDD: {
            this.tiStates += 5;
            final int address =
                this._readPtr(ctx, REG_IX, this.regIX) + (byte) this._read_ixiy_d(ctx);
            this.setMemPtr(address);
            return this._readmem8(ctx, address);
          }
          case 0xFD: {
            this.tiStates += 5;
            final int address =
                this._readPtr(ctx, REG_IY, this.regIY) + (byte) this._read_ixiy_d(ctx);
            this.setMemPtr(address);
            return this._readmem8(ctx, address);
          }
        }
      }
      break;
      case 7:
        return this.regA;
    }
    throw new Error("Unexpected prefix or R index [" + this.prefix + ':' + r + ']');
  }

  private void writeReg16(final int p, final int value) {
    switch (p) {
      case 0:
        this.regB = (value >>> 8) & 0xFF;
        this.regC = value & 0xFF;
        return;
      case 1:
        this.regD = (value >>> 8) & 0xFF;
        this.regE = value & 0xFF;
        return;
      case 2: {
        switch (this.normalizedPrefix()) {
          case 0x00:
            this.regH = (value >>> 8) & 0xFF;
            this.regL = value & 0xFF;
            return;
          case 0xDD:
            this.regIX = value & 0xFFFF;
            return;
          case 0xFD:
            this.regIY = value & 0xFFFF;
            return;
        }
      }
      break;
      case 3:
        this.regSP = value & 0xFFFF;
        return;
    }
    throw new Error("unexpected P index or prefix [" + this.prefix + ':' + p + ']');
  }

  private void writeReg16_2(final int p, final int value) {
    switch (p) {
      case 0:
        this.regB = (value >>> 8) & 0xFF;
        this.regC = value & 0xFF;
        return;
      case 1:
        this.regD = (value >>> 8) & 0xFF;
        this.regE = value & 0xFF;
        return;
      case 2: {
        switch (this.normalizedPrefix()) {
          case 0x00:
            this.regH = (value >>> 8) & 0xFF;
            this.regL = value & 0xFF;
            return;
          case 0xDD:
            this.regIX = value & 0xFFFF;
            return;
          case 0xFD:
            this.regIY = value & 0xFFFF;
            return;
        }
      }
      break;
      case 3:
        this.regA = (value >>> 8) & 0xFF;
        this.regF = value & 0xFF;
        return;
    }
    throw new Error("unexpected P index or prefix [" + this.prefix + ':' + p + ']');
  }

  private int readHlPtr(final int ctx) {
    switch (this.normalizedPrefix()) {
      case 0x00:
        return this._readPtr(ctx, REGPAIR_HL, (this.regH << 8) | this.regL);
      case 0xDD:
        return this._readPtr(ctx, REG_IX, this.regIX);
      case 0xFD:
        return this._readPtr(ctx, REG_IY, this.regIY);
    }
    throw new Error("Unexpected prefix:" + this.prefix);
  }

  private int readReg16(final int p) {
    switch (p) {
      case 0:
        return (this.regB << 8) | this.regC;
      case 1:
        return (this.regD << 8) | this.regE;
      case 2: {
        switch (this.normalizedPrefix()) {
          case 0x00:
            return (this.regH << 8) | this.regL;
          case 0xDD:
            return this.regIX;
          case 0xFD:
            return this.regIY;
        }
      }
      break;
      case 3:
        return this.regSP;
    }
    throw new Error("Unexpected P index or prefix [" + this.prefix + ':' + p + ']');
  }

  private int readReg16_2(final int p) {
    switch (p) {
      case 0:
        return (this.regB << 8) | this.regC;
      case 1:
        return (this.regD << 8) | this.regE;
      case 2: {
        switch (this.normalizedPrefix()) {
          case 0x00:
            return (this.regH << 8) | this.regL;
          case 0xDD:
            return this.regIX;
          case 0xFD:
            return this.regIY;
        }
      }
      break;
      case 3:
        return (this.regA << 8) | this.regF;
    }
    throw new Error("Unexpected P index or prefix [" + this.prefix + ':' + p + ']');
  }

  private void writeReg8(final int ctx, final int r, final int value) {
    switch (r) {
      case 0:
        this.regB = value & 0xFF;
        return;
      case 1:
        this.regC = value & 0xFF;
        return;
      case 2:
        this.regD = value & 0xFF;
        return;
      case 3:
        this.regE = value & 0xFF;
        return;
      case 4:
        if (this.cbDisplacementByte < 0) {
          switch (normalizedPrefix()) {
            case 0x00:
              this.regH = value & 0xFF;
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
          this.regH = value & 0xFF;
        }
        return;
      case 5:
        if (this.cbDisplacementByte < 0) {
          switch (normalizedPrefix()) {
            case 0x00:
              this.regL = value & 0xFF;
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
          this.regL = value & 0xFF;
        }
        return;
      case 6: { // (HL)
        switch (normalizedPrefix()) {
          case 0x00: {
            final int address = _readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL));
            _writemem8(ctx, address,
                (byte) value);
            return;
          }
          case 0xDD: {
            final int address = _readPtr(ctx, REG_IX, this.regIX) + (byte) value;
            _writemem8(ctx, address, (byte) readInstrOrPrefix(ctx, false));
            this.setMemPtr(address);
            this.tiStates += 2;
            return;
          }
          case 0xFD: {
            final int address = _readPtr(ctx, REG_IY, this.regIY) + (byte) value;
            _writemem8(ctx, address, (byte) readInstrOrPrefix(ctx, false));
            this.setMemPtr(address);
            this.tiStates += 2;
            return;
          }
        }
      }
      break;
      case 7:
        this.regA = value & 0xFF;
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

  /**
   * Process whole instruction or send signals but only step of a block
   * instruction will be processed.
   *
   * @param ctx         context of method call, will be propagated to all sub-calls
   * @param signalRESET true sends the RESET signal to the CPU
   * @param signalNMI   true sends the NMI signal to the CPU
   * @param signalNT    true sends the INT signal to the CPU
   * @return spent machine cycles during execution
   */
  public int nextInstruction(final int ctx, final boolean signalRESET, final boolean signalNMI,
                             final boolean signalNT) {
    int flag = (signalNT ? 0 : SIGNAL_IN_nINT) | (signalNMI ? 0 : SIGNAL_IN_nNMI)
        | (signalRESET ? 0 : SIGNAL_IN_nRESET) | SIGNAL_IN_nWAIT;

    int spentTstates = 0;
    while (step(ctx, flag)) {
      flag = SIGNAL_IN_ALL_INACTIVE;
      spentTstates += this.getStepTstates();
    }
    spentTstates += this.getStepTstates();
    return spentTstates;
  }

  /**
   * Process one step.
   *
   * @param ctx             context of method call, will be propagated to all sub-calls
   * @param incomingSignals external signal states to be processes during the step.
   * @return false if there is not any instruction under processing, true
   * otherwise
   */
  public boolean step(final int ctx, final int incomingSignals) {
    this.nmiTrigger =
        this.nmiTrigger || isHiLoFront(this.prevInSignals, incomingSignals, SIGNAL_IN_nNMI);

    this.tiStates = 0;
    try {
      final boolean result;
      this.stepAllowsInterruption = true;

      if ((incomingSignals & SIGNAL_IN_nWAIT) == 0) {
        // PROCESS nWAIT
        this.tiStates++;
        result = this.prefix != 0;
      } else if ((incomingSignals & SIGNAL_IN_nRESET) == 0) {
        // START RESET
        _reset(this.resetCycle++);
        result = false;
      } else {
        // Process command
        this.internalRegLastQ = this.internalRegQ;
        this.internalRegQ = 0;


        final boolean incomingInterrupt =
            this.nmiTrigger || (this.iff1 && (incomingSignals & SIGNAL_IN_nINT) == 0);

        if (_step(ctx, readInstrOrPrefix(ctx, true), incomingInterrupt)) {
          // Command completed
          this.prefix = 0;
          result = false;

          if (this.stepAllowsInterruption) {
            // Check interruptions
            if (this.nmiTrigger) {
              // NMI
              this.nmiTrigger = false;
              _nmi(ctx);
            } else if (this.iff1 && (incomingSignals & SIGNAL_IN_nINT) == 0) {
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
      this.prevInSignals = incomingSignals;
    }
  }

  private boolean _step(final int ctx, final int commandByte, final boolean incommingInterrupt) {
    this.lastInstructionByte = commandByte;

    switch (this.prefix) {
      case 0xDD:
      case 0xFD:
      case 0x00:
        return this.decodeUnprefixed(ctx, commandByte);
      case 0xFDCB:
      case 0xDDCB:
        return this.decodeIndexedCB(ctx, commandByte);
      case 0xCB:
        this.decodeCB(ctx, commandByte);
        return true;
      case 0xED:
        return this.decodeED(ctx, commandByte, incommingInterrupt);
      default:
        throw new Error("Illegal prefix state [0x"
            + Integer.toHexString(this.prefix).toUpperCase(Locale.ENGLISH) + ']');
    }
  }

  private boolean decodeUnprefixed(final int ctx, final int op) {
    switch (op) {
      case 0x00:
        this.doNOP();
        return true;
      case 0x01:
        this.doLDRegPairByNextWord(ctx, 0);
        return true;
      case 0x02:
        this.doLD_mBC_A(ctx);
        return true;
      case 0x03:
        this.doINCRegPair(0);
        return true;
      case 0x04:
        this.doINCReg(ctx, 0);
        return true;
      case 0x05:
        this.doDECReg(ctx, 0);
        return true;
      case 0x06:
        this.doLD_Reg_ByValue(ctx, 0);
        return true;
      case 0x07:
        this.doRLCA();
        return true;
      case 0x08:
        this.doEX_AF_AF();
        return true;
      case 0x09:
        this.doADD_HL_RegPair(0);
        return true;
      case 0x0A:
        this.doLD_A_mBC(ctx);
        return true;
      case 0x0B:
        this.doDECRegPair(0);
        return true;
      case 0x0C:
        this.doINCReg(ctx, 1);
        return true;
      case 0x0D:
        this.doDECReg(ctx, 1);
        return true;
      case 0x0E:
        this.doLD_Reg_ByValue(ctx, 1);
        return true;
      case 0x0F:
        this.doRRCA();
        return true;
      case 0x10:
        this.doDJNZ(ctx);
        return true;
      case 0x11:
        this.doLDRegPairByNextWord(ctx, 1);
        return true;
      case 0x12:
        this.doLD_mDE_A(ctx);
        return true;
      case 0x13:
        this.doINCRegPair(1);
        return true;
      case 0x14:
        this.doINCReg(ctx, 2);
        return true;
      case 0x15:
        this.doDECReg(ctx, 2);
        return true;
      case 0x16:
        this.doLD_Reg_ByValue(ctx, 2);
        return true;
      case 0x17:
        this.doRLA();
        return true;
      case 0x18:
        this.doJR(ctx);
        return true;
      case 0x19:
        this.doADD_HL_RegPair(1);
        return true;
      case 0x1A:
        this.doLD_A_mDE(ctx);
        return true;
      case 0x1B:
        this.doDECRegPair(1);
        return true;
      case 0x1C:
        this.doINCReg(ctx, 3);
        return true;
      case 0x1D:
        this.doDECReg(ctx, 3);
        return true;
      case 0x1E:
        this.doLD_Reg_ByValue(ctx, 3);
        return true;
      case 0x1F:
        this.doRRA();
        return true;
      case 0x20:
        this.doJR(ctx, 0);
        return true;
      case 0x21:
        this.doLDRegPairByNextWord(ctx, 2);
        return true;
      case 0x22:
        this.doLD_mNN_HL(ctx);
        return true;
      case 0x23:
        this.doINCRegPair(2);
        return true;
      case 0x24:
        this.doINCReg(ctx, 4);
        return true;
      case 0x25:
        this.doDECReg(ctx, 4);
        return true;
      case 0x26:
        this.doLD_Reg_ByValue(ctx, 4);
        return true;
      case 0x27:
        this.doDAA();
        return true;
      case 0x28:
        this.doJR(ctx, 1);
        return true;
      case 0x29:
        this.doADD_HL_RegPair(2);
        return true;
      case 0x2A:
        this.doLD_HL_mem(ctx);
        return true;
      case 0x2B:
        this.doDECRegPair(2);
        return true;
      case 0x2C:
        this.doINCReg(ctx, 5);
        return true;
      case 0x2D:
        this.doDECReg(ctx, 5);
        return true;
      case 0x2E:
        this.doLD_Reg_ByValue(ctx, 5);
        return true;
      case 0x2F:
        this.doCPL();
        return true;
      case 0x30:
        this.doJR(ctx, 2);
        return true;
      case 0x31:
        this.doLDRegPairByNextWord(ctx, 3);
        return true;
      case 0x32:
        this.doLD_mNN_A(ctx);
        return true;
      case 0x33:
        this.doINCRegPair(3);
        return true;
      case 0x34:
        this.doINCReg(ctx, 6);
        return true;
      case 0x35:
        this.doDECReg(ctx, 6);
        return true;
      case 0x36:
        this.doLD_Reg_ByValue(ctx, 6);
        return true;
      case 0x37:
        this.doSCF();
        return true;
      case 0x38:
        this.doJR(ctx, 3);
        return true;
      case 0x39:
        this.doADD_HL_RegPair(3);
        return true;
      case 0x3A:
        this.doLD_A_mem(ctx);
        return true;
      case 0x3B:
        this.doDECRegPair(3);
        return true;
      case 0x3C:
        this.doINCReg(ctx, 7);
        return true;
      case 0x3D:
        this.doDECReg(ctx, 7);
        return true;
      case 0x3E:
        this.doLD_Reg_ByValue(ctx, 7);
        return true;
      case 0x3F:
        this.doCCF();
        return true;
      case 0x40:
        this.doLDRegByReg(ctx, 0, 0);
        return true;
      case 0x41:
        this.doLDRegByReg(ctx, 0, 1);
        return true;
      case 0x42:
        this.doLDRegByReg(ctx, 0, 2);
        return true;
      case 0x43:
        this.doLDRegByReg(ctx, 0, 3);
        return true;
      case 0x44:
        this.doLDRegByReg(ctx, 0, 4);
        return true;
      case 0x45:
        this.doLDRegByReg(ctx, 0, 5);
        return true;
      case 0x46:
        this.doLDRegByReg(ctx, 0, 6);
        return true;
      case 0x47:
        this.doLDRegByReg(ctx, 0, 7);
        return true;
      case 0x48:
        this.doLDRegByReg(ctx, 1, 0);
        return true;
      case 0x49:
        this.doLDRegByReg(ctx, 1, 1);
        return true;
      case 0x4A:
        this.doLDRegByReg(ctx, 1, 2);
        return true;
      case 0x4B:
        this.doLDRegByReg(ctx, 1, 3);
        return true;
      case 0x4C:
        this.doLDRegByReg(ctx, 1, 4);
        return true;
      case 0x4D:
        this.doLDRegByReg(ctx, 1, 5);
        return true;
      case 0x4E:
        this.doLDRegByReg(ctx, 1, 6);
        return true;
      case 0x4F:
        this.doLDRegByReg(ctx, 1, 7);
        return true;
      case 0x50:
        this.doLDRegByReg(ctx, 2, 0);
        return true;
      case 0x51:
        this.doLDRegByReg(ctx, 2, 1);
        return true;
      case 0x52:
        this.doLDRegByReg(ctx, 2, 2);
        return true;
      case 0x53:
        this.doLDRegByReg(ctx, 2, 3);
        return true;
      case 0x54:
        this.doLDRegByReg(ctx, 2, 4);
        return true;
      case 0x55:
        this.doLDRegByReg(ctx, 2, 5);
        return true;
      case 0x56:
        this.doLDRegByReg(ctx, 2, 6);
        return true;
      case 0x57:
        this.doLDRegByReg(ctx, 2, 7);
        return true;
      case 0x58:
        this.doLDRegByReg(ctx, 3, 0);
        return true;
      case 0x59:
        this.doLDRegByReg(ctx, 3, 1);
        return true;
      case 0x5A:
        this.doLDRegByReg(ctx, 3, 2);
        return true;
      case 0x5B:
        this.doLDRegByReg(ctx, 3, 3);
        return true;
      case 0x5C:
        this.doLDRegByReg(ctx, 3, 4);
        return true;
      case 0x5D:
        this.doLDRegByReg(ctx, 3, 5);
        return true;
      case 0x5E:
        this.doLDRegByReg(ctx, 3, 6);
        return true;
      case 0x5F:
        this.doLDRegByReg(ctx, 3, 7);
        return true;
      case 0x60:
        this.doLDRegByReg(ctx, 4, 0);
        return true;
      case 0x61:
        this.doLDRegByReg(ctx, 4, 1);
        return true;
      case 0x62:
        this.doLDRegByReg(ctx, 4, 2);
        return true;
      case 0x63:
        this.doLDRegByReg(ctx, 4, 3);
        return true;
      case 0x64:
        this.doLDRegByReg(ctx, 4, 4);
        return true;
      case 0x65:
        this.doLDRegByReg(ctx, 4, 5);
        return true;
      case 0x66:
        this.doLDRegByReg(ctx, 4, 6);
        return true;
      case 0x67:
        this.doLDRegByReg(ctx, 4, 7);
        return true;
      case 0x68:
        this.doLDRegByReg(ctx, 5, 0);
        return true;
      case 0x69:
        this.doLDRegByReg(ctx, 5, 1);
        return true;
      case 0x6A:
        this.doLDRegByReg(ctx, 5, 2);
        return true;
      case 0x6B:
        this.doLDRegByReg(ctx, 5, 3);
        return true;
      case 0x6C:
        this.doLDRegByReg(ctx, 5, 4);
        return true;
      case 0x6D:
        this.doLDRegByReg(ctx, 5, 5);
        return true;
      case 0x6E:
        this.doLDRegByReg(ctx, 5, 6);
        return true;
      case 0x6F:
        this.doLDRegByReg(ctx, 5, 7);
        return true;
      case 0x70:
        this.doLDRegByReg(ctx, 6, 0);
        return true;
      case 0x71:
        this.doLDRegByReg(ctx, 6, 1);
        return true;
      case 0x72:
        this.doLDRegByReg(ctx, 6, 2);
        return true;
      case 0x73:
        this.doLDRegByReg(ctx, 6, 3);
        return true;
      case 0x74:
        this.doLDRegByReg(ctx, 6, 4);
        return true;
      case 0x75:
        this.doLDRegByReg(ctx, 6, 5);
        return true;
      case 0x76:
        this.doHalt();
        return true;
      case 0x77:
        this.doLDRegByReg(ctx, 6, 7);
        return true;
      case 0x78:
        this.doLDRegByReg(ctx, 7, 0);
        return true;
      case 0x79:
        this.doLDRegByReg(ctx, 7, 1);
        return true;
      case 0x7A:
        this.doLDRegByReg(ctx, 7, 2);
        return true;
      case 0x7B:
        this.doLDRegByReg(ctx, 7, 3);
        return true;
      case 0x7C:
        this.doLDRegByReg(ctx, 7, 4);
        return true;
      case 0x7D:
        this.doLDRegByReg(ctx, 7, 5);
        return true;
      case 0x7E:
        this.doLDRegByReg(ctx, 7, 6);
        return true;
      case 0x7F:
        this.doLDRegByReg(ctx, 7, 7);
        return true;
      case 0x80:
        this.doALU_A_Reg(ctx, 0, 0);
        return true;
      case 0x81:
        this.doALU_A_Reg(ctx, 0, 1);
        return true;
      case 0x82:
        this.doALU_A_Reg(ctx, 0, 2);
        return true;
      case 0x83:
        this.doALU_A_Reg(ctx, 0, 3);
        return true;
      case 0x84:
        this.doALU_A_Reg(ctx, 0, 4);
        return true;
      case 0x85:
        this.doALU_A_Reg(ctx, 0, 5);
        return true;
      case 0x86:
        this.doALU_A_Reg(ctx, 0, 6);
        return true;
      case 0x87:
        this.doALU_A_Reg(ctx, 0, 7);
        return true;
      case 0x88:
        this.doALU_A_Reg(ctx, 1, 0);
        return true;
      case 0x89:
        this.doALU_A_Reg(ctx, 1, 1);
        return true;
      case 0x8A:
        this.doALU_A_Reg(ctx, 1, 2);
        return true;
      case 0x8B:
        this.doALU_A_Reg(ctx, 1, 3);
        return true;
      case 0x8C:
        this.doALU_A_Reg(ctx, 1, 4);
        return true;
      case 0x8D:
        this.doALU_A_Reg(ctx, 1, 5);
        return true;
      case 0x8E:
        this.doALU_A_Reg(ctx, 1, 6);
        return true;
      case 0x8F:
        this.doALU_A_Reg(ctx, 1, 7);
        return true;
      case 0x90:
        this.doALU_A_Reg(ctx, 2, 0);
        return true;
      case 0x91:
        this.doALU_A_Reg(ctx, 2, 1);
        return true;
      case 0x92:
        this.doALU_A_Reg(ctx, 2, 2);
        return true;
      case 0x93:
        this.doALU_A_Reg(ctx, 2, 3);
        return true;
      case 0x94:
        this.doALU_A_Reg(ctx, 2, 4);
        return true;
      case 0x95:
        this.doALU_A_Reg(ctx, 2, 5);
        return true;
      case 0x96:
        this.doALU_A_Reg(ctx, 2, 6);
        return true;
      case 0x97:
        this.doALU_A_Reg(ctx, 2, 7);
        return true;
      case 0x98:
        this.doALU_A_Reg(ctx, 3, 0);
        return true;
      case 0x99:
        this.doALU_A_Reg(ctx, 3, 1);
        return true;
      case 0x9A:
        this.doALU_A_Reg(ctx, 3, 2);
        return true;
      case 0x9B:
        this.doALU_A_Reg(ctx, 3, 3);
        return true;
      case 0x9C:
        this.doALU_A_Reg(ctx, 3, 4);
        return true;
      case 0x9D:
        this.doALU_A_Reg(ctx, 3, 5);
        return true;
      case 0x9E:
        this.doALU_A_Reg(ctx, 3, 6);
        return true;
      case 0x9F:
        this.doALU_A_Reg(ctx, 3, 7);
        return true;
      case 0xA0:
        this.doALU_A_Reg(ctx, 4, 0);
        return true;
      case 0xA1:
        this.doALU_A_Reg(ctx, 4, 1);
        return true;
      case 0xA2:
        this.doALU_A_Reg(ctx, 4, 2);
        return true;
      case 0xA3:
        this.doALU_A_Reg(ctx, 4, 3);
        return true;
      case 0xA4:
        this.doALU_A_Reg(ctx, 4, 4);
        return true;
      case 0xA5:
        this.doALU_A_Reg(ctx, 4, 5);
        return true;
      case 0xA6:
        this.doALU_A_Reg(ctx, 4, 6);
        return true;
      case 0xA7:
        this.doALU_A_Reg(ctx, 4, 7);
        return true;
      case 0xA8:
        this.doALU_A_Reg(ctx, 5, 0);
        return true;
      case 0xA9:
        this.doALU_A_Reg(ctx, 5, 1);
        return true;
      case 0xAA:
        this.doALU_A_Reg(ctx, 5, 2);
        return true;
      case 0xAB:
        this.doALU_A_Reg(ctx, 5, 3);
        return true;
      case 0xAC:
        this.doALU_A_Reg(ctx, 5, 4);
        return true;
      case 0xAD:
        this.doALU_A_Reg(ctx, 5, 5);
        return true;
      case 0xAE:
        this.doALU_A_Reg(ctx, 5, 6);
        return true;
      case 0xAF:
        this.doALU_A_Reg(ctx, 5, 7);
        return true;
      case 0xB0:
        this.doALU_A_Reg(ctx, 6, 0);
        return true;
      case 0xB1:
        this.doALU_A_Reg(ctx, 6, 1);
        return true;
      case 0xB2:
        this.doALU_A_Reg(ctx, 6, 2);
        return true;
      case 0xB3:
        this.doALU_A_Reg(ctx, 6, 3);
        return true;
      case 0xB4:
        this.doALU_A_Reg(ctx, 6, 4);
        return true;
      case 0xB5:
        this.doALU_A_Reg(ctx, 6, 5);
        return true;
      case 0xB6:
        this.doALU_A_Reg(ctx, 6, 6);
        return true;
      case 0xB7:
        this.doALU_A_Reg(ctx, 6, 7);
        return true;
      case 0xB8:
        this.doALU_A_Reg(ctx, 7, 0);
        return true;
      case 0xB9:
        this.doALU_A_Reg(ctx, 7, 1);
        return true;
      case 0xBA:
        this.doALU_A_Reg(ctx, 7, 2);
        return true;
      case 0xBB:
        this.doALU_A_Reg(ctx, 7, 3);
        return true;
      case 0xBC:
        this.doALU_A_Reg(ctx, 7, 4);
        return true;
      case 0xBD:
        this.doALU_A_Reg(ctx, 7, 5);
        return true;
      case 0xBE:
        this.doALU_A_Reg(ctx, 7, 6);
        return true;
      case 0xBF:
        this.doALU_A_Reg(ctx, 7, 7);
        return true;
      case 0xC0:
        this.doRETByFlag(ctx, 0);
        return true;
      case 0xC1:
        this.doPOPRegPair(ctx, 0);
        return true;
      case 0xC2:
        this.doJP_cc(ctx, 0);
        return true;
      case 0xC3:
        this.doJP(ctx);
        return true;
      case 0xC4:
        this.doCALL(ctx, 0);
        return true;
      case 0xC5:
        this.doPUSH(ctx, 0);
        return true;
      case 0xC6:
        this.doALU_A_n(ctx, 0);
        return true;
      case 0xC7:
        this.doRST(ctx, 0);
        return true;
      case 0xC8:
        this.doRETByFlag(ctx, 1);
        return true;
      case 0xC9:
        this.doRET(ctx);
        return true;
      case 0xCA:
        this.doJP_cc(ctx, 1);
        return true;
      case 0xCB:
        this.prefix = (this.prefix << 8) | 0xCB;
        this.cbDisplacementByte = -1;
        return false;
      case 0xCC:
        this.doCALL(ctx, 1);
        return true;
      case 0xCD:
        this.doCALL(ctx);
        return true;
      case 0xCE:
        this.doALU_A_n(ctx, 1);
        return true;
      case 0xCF:
        this.doRST(ctx, 8);
        return true;
      case 0xD0:
        this.doRETByFlag(ctx, 2);
        return true;
      case 0xD1:
        this.doPOPRegPair(ctx, 1);
        return true;
      case 0xD2:
        this.doJP_cc(ctx, 2);
        return true;
      case 0xD3:
        this.doOUTnA(ctx);
        return true;
      case 0xD4:
        this.doCALL(ctx, 2);
        return true;
      case 0xD5:
        this.doPUSH(ctx, 1);
        return true;
      case 0xD6:
        this.doALU_A_n(ctx, 2);
        return true;
      case 0xD7:
        this.doRST(ctx, 16);
        return true;
      case 0xD8:
        this.doRETByFlag(ctx, 3);
        return true;
      case 0xD9:
        this.doEXX();
        return true;
      case 0xDA:
        this.doJP_cc(ctx, 3);
        return true;
      case 0xDB:
        this.doIN_A_n(ctx);
        return true;
      case 0xDC:
        this.doCALL(ctx, 3);
        return true;
      case 0xDD:
        this.prefix = 0xDD;
        return false;
      case 0xDE:
        this.doALU_A_n(ctx, 3);
        return true;
      case 0xDF:
        this.doRST(ctx, 24);
        return true;
      case 0xE0:
        this.doRETByFlag(ctx, 4);
        return true;
      case 0xE1:
        this.doPOPRegPair(ctx, 2);
        return true;
      case 0xE2:
        this.doJP_cc(ctx, 4);
        return true;
      case 0xE3:
        this.doEX_mSP_HL(ctx);
        return true;
      case 0xE4:
        this.doCALL(ctx, 4);
        return true;
      case 0xE5:
        this.doPUSH(ctx, 2);
        return true;
      case 0xE6:
        this.doALU_A_n(ctx, 4);
        return true;
      case 0xE7:
        this.doRST(ctx, 32);
        return true;
      case 0xE8:
        this.doRETByFlag(ctx, 5);
        return true;
      case 0xE9:
        this.doJP_HL(ctx);
        return true;
      case 0xEA:
        this.doJP_cc(ctx, 5);
        return true;
      case 0xEB:
        this.doEX_DE_HL();
        return true;
      case 0xEC:
        this.doCALL(ctx, 5);
        return true;
      case 0xED:
        this.prefix = 0xED;
        return false;
      case 0xEE:
        this.doALU_A_n(ctx, 5);
        return true;
      case 0xEF:
        this.doRST(ctx, 40);
        return true;
      case 0xF0:
        this.doRETByFlag(ctx, 6);
        return true;
      case 0xF1:
        this.doPOPRegPair(ctx, 3);
        return true;
      case 0xF2:
        this.doJP_cc(ctx, 6);
        return true;
      case 0xF3:
        this.doDI();
        return true;
      case 0xF4:
        this.doCALL(ctx, 6);
        return true;
      case 0xF5:
        this.doPUSH(ctx, 3);
        return true;
      case 0xF6:
        this.doALU_A_n(ctx, 6);
        return true;
      case 0xF7:
        this.doRST(ctx, 48);
        return true;
      case 0xF8:
        this.doRETByFlag(ctx, 7);
        return true;
      case 0xF9:
        this.doLD_SP_HL(ctx);
        return true;
      case 0xFA:
        this.doJP_cc(ctx, 7);
        return true;
      case 0xFB:
        this.doEI();
        return true;
      case 0xFC:
        this.doCALL(ctx, 7);
        return true;
      case 0xFD:
        this.prefix = 0xFD;
        return false;
      case 0xFE:
        this.doALU_A_n(ctx, 7);
        return true;
      case 0xFF:
        this.doRST(ctx, 56);
        return true;
      default:
        throw new Error("Unexpected opcode");
    }
  }

  private void decodeCB(final int ctx, final int op) {
    switch (op) {
      case 0x00:
        this.doRollShift(ctx, 0, 0);
        break;
      case 0x01:
        this.doRollShift(ctx, 0, 1);
        break;
      case 0x02:
        this.doRollShift(ctx, 0, 2);
        break;
      case 0x03:
        this.doRollShift(ctx, 0, 3);
        break;
      case 0x04:
        this.doRollShift(ctx, 0, 4);
        break;
      case 0x05:
        this.doRollShift(ctx, 0, 5);
        break;
      case 0x06:
        this.doRollShift(ctx, 0, 6);
        break;
      case 0x07:
        this.doRollShift(ctx, 0, 7);
        break;
      case 0x08:
        this.doRollShift(ctx, 1, 0);
        break;
      case 0x09:
        this.doRollShift(ctx, 1, 1);
        break;
      case 0x0A:
        this.doRollShift(ctx, 1, 2);
        break;
      case 0x0B:
        this.doRollShift(ctx, 1, 3);
        break;
      case 0x0C:
        this.doRollShift(ctx, 1, 4);
        break;
      case 0x0D:
        this.doRollShift(ctx, 1, 5);
        break;
      case 0x0E:
        this.doRollShift(ctx, 1, 6);
        break;
      case 0x0F:
        this.doRollShift(ctx, 1, 7);
        break;
      case 0x10:
        this.doRollShift(ctx, 2, 0);
        break;
      case 0x11:
        this.doRollShift(ctx, 2, 1);
        break;
      case 0x12:
        this.doRollShift(ctx, 2, 2);
        break;
      case 0x13:
        this.doRollShift(ctx, 2, 3);
        break;
      case 0x14:
        this.doRollShift(ctx, 2, 4);
        break;
      case 0x15:
        this.doRollShift(ctx, 2, 5);
        break;
      case 0x16:
        this.doRollShift(ctx, 2, 6);
        break;
      case 0x17:
        this.doRollShift(ctx, 2, 7);
        break;
      case 0x18:
        this.doRollShift(ctx, 3, 0);
        break;
      case 0x19:
        this.doRollShift(ctx, 3, 1);
        break;
      case 0x1A:
        this.doRollShift(ctx, 3, 2);
        break;
      case 0x1B:
        this.doRollShift(ctx, 3, 3);
        break;
      case 0x1C:
        this.doRollShift(ctx, 3, 4);
        break;
      case 0x1D:
        this.doRollShift(ctx, 3, 5);
        break;
      case 0x1E:
        this.doRollShift(ctx, 3, 6);
        break;
      case 0x1F:
        this.doRollShift(ctx, 3, 7);
        break;
      case 0x20:
        this.doRollShift(ctx, 4, 0);
        break;
      case 0x21:
        this.doRollShift(ctx, 4, 1);
        break;
      case 0x22:
        this.doRollShift(ctx, 4, 2);
        break;
      case 0x23:
        this.doRollShift(ctx, 4, 3);
        break;
      case 0x24:
        this.doRollShift(ctx, 4, 4);
        break;
      case 0x25:
        this.doRollShift(ctx, 4, 5);
        break;
      case 0x26:
        this.doRollShift(ctx, 4, 6);
        break;
      case 0x27:
        this.doRollShift(ctx, 4, 7);
        break;
      case 0x28:
        this.doRollShift(ctx, 5, 0);
        break;
      case 0x29:
        this.doRollShift(ctx, 5, 1);
        break;
      case 0x2A:
        this.doRollShift(ctx, 5, 2);
        break;
      case 0x2B:
        this.doRollShift(ctx, 5, 3);
        break;
      case 0x2C:
        this.doRollShift(ctx, 5, 4);
        break;
      case 0x2D:
        this.doRollShift(ctx, 5, 5);
        break;
      case 0x2E:
        this.doRollShift(ctx, 5, 6);
        break;
      case 0x2F:
        this.doRollShift(ctx, 5, 7);
        break;
      case 0x30:
        this.doRollShift(ctx, 6, 0);
        break;
      case 0x31:
        this.doRollShift(ctx, 6, 1);
        break;
      case 0x32:
        this.doRollShift(ctx, 6, 2);
        break;
      case 0x33:
        this.doRollShift(ctx, 6, 3);
        break;
      case 0x34:
        this.doRollShift(ctx, 6, 4);
        break;
      case 0x35:
        this.doRollShift(ctx, 6, 5);
        break;
      case 0x36:
        this.doRollShift(ctx, 6, 6);
        break;
      case 0x37:
        this.doRollShift(ctx, 6, 7);
        break;
      case 0x38:
        this.doRollShift(ctx, 7, 0);
        break;
      case 0x39:
        this.doRollShift(ctx, 7, 1);
        break;
      case 0x3A:
        this.doRollShift(ctx, 7, 2);
        break;
      case 0x3B:
        this.doRollShift(ctx, 7, 3);
        break;
      case 0x3C:
        this.doRollShift(ctx, 7, 4);
        break;
      case 0x3D:
        this.doRollShift(ctx, 7, 5);
        break;
      case 0x3E:
        this.doRollShift(ctx, 7, 6);
        break;
      case 0x3F:
        this.doRollShift(ctx, 7, 7);
        break;
      case 0x40:
        this.doBIT(ctx, 0, 0);
        break;
      case 0x41:
        this.doBIT(ctx, 0, 1);
        break;
      case 0x42:
        this.doBIT(ctx, 0, 2);
        break;
      case 0x43:
        this.doBIT(ctx, 0, 3);
        break;
      case 0x44:
        this.doBIT(ctx, 0, 4);
        break;
      case 0x45:
        this.doBIT(ctx, 0, 5);
        break;
      case 0x46:
        this.doBIT(ctx, 0, 6);
        break;
      case 0x47:
        this.doBIT(ctx, 0, 7);
        break;
      case 0x48:
        this.doBIT(ctx, 1, 0);
        break;
      case 0x49:
        this.doBIT(ctx, 1, 1);
        break;
      case 0x4A:
        this.doBIT(ctx, 1, 2);
        break;
      case 0x4B:
        this.doBIT(ctx, 1, 3);
        break;
      case 0x4C:
        this.doBIT(ctx, 1, 4);
        break;
      case 0x4D:
        this.doBIT(ctx, 1, 5);
        break;
      case 0x4E:
        this.doBIT(ctx, 1, 6);
        break;
      case 0x4F:
        this.doBIT(ctx, 1, 7);
        break;
      case 0x50:
        this.doBIT(ctx, 2, 0);
        break;
      case 0x51:
        this.doBIT(ctx, 2, 1);
        break;
      case 0x52:
        this.doBIT(ctx, 2, 2);
        break;
      case 0x53:
        this.doBIT(ctx, 2, 3);
        break;
      case 0x54:
        this.doBIT(ctx, 2, 4);
        break;
      case 0x55:
        this.doBIT(ctx, 2, 5);
        break;
      case 0x56:
        this.doBIT(ctx, 2, 6);
        break;
      case 0x57:
        this.doBIT(ctx, 2, 7);
        break;
      case 0x58:
        this.doBIT(ctx, 3, 0);
        break;
      case 0x59:
        this.doBIT(ctx, 3, 1);
        break;
      case 0x5A:
        this.doBIT(ctx, 3, 2);
        break;
      case 0x5B:
        this.doBIT(ctx, 3, 3);
        break;
      case 0x5C:
        this.doBIT(ctx, 3, 4);
        break;
      case 0x5D:
        this.doBIT(ctx, 3, 5);
        break;
      case 0x5E:
        this.doBIT(ctx, 3, 6);
        break;
      case 0x5F:
        this.doBIT(ctx, 3, 7);
        break;
      case 0x60:
        this.doBIT(ctx, 4, 0);
        break;
      case 0x61:
        this.doBIT(ctx, 4, 1);
        break;
      case 0x62:
        this.doBIT(ctx, 4, 2);
        break;
      case 0x63:
        this.doBIT(ctx, 4, 3);
        break;
      case 0x64:
        this.doBIT(ctx, 4, 4);
        break;
      case 0x65:
        this.doBIT(ctx, 4, 5);
        break;
      case 0x66:
        this.doBIT(ctx, 4, 6);
        break;
      case 0x67:
        this.doBIT(ctx, 4, 7);
        break;
      case 0x68:
        this.doBIT(ctx, 5, 0);
        break;
      case 0x69:
        this.doBIT(ctx, 5, 1);
        break;
      case 0x6A:
        this.doBIT(ctx, 5, 2);
        break;
      case 0x6B:
        this.doBIT(ctx, 5, 3);
        break;
      case 0x6C:
        this.doBIT(ctx, 5, 4);
        break;
      case 0x6D:
        this.doBIT(ctx, 5, 5);
        break;
      case 0x6E:
        this.doBIT(ctx, 5, 6);
        break;
      case 0x6F:
        this.doBIT(ctx, 5, 7);
        break;
      case 0x70:
        this.doBIT(ctx, 6, 0);
        break;
      case 0x71:
        this.doBIT(ctx, 6, 1);
        break;
      case 0x72:
        this.doBIT(ctx, 6, 2);
        break;
      case 0x73:
        this.doBIT(ctx, 6, 3);
        break;
      case 0x74:
        this.doBIT(ctx, 6, 4);
        break;
      case 0x75:
        this.doBIT(ctx, 6, 5);
        break;
      case 0x76:
        this.doBIT(ctx, 6, 6);
        break;
      case 0x77:
        this.doBIT(ctx, 6, 7);
        break;
      case 0x78:
        this.doBIT(ctx, 7, 0);
        break;
      case 0x79:
        this.doBIT(ctx, 7, 1);
        break;
      case 0x7A:
        this.doBIT(ctx, 7, 2);
        break;
      case 0x7B:
        this.doBIT(ctx, 7, 3);
        break;
      case 0x7C:
        this.doBIT(ctx, 7, 4);
        break;
      case 0x7D:
        this.doBIT(ctx, 7, 5);
        break;
      case 0x7E:
        this.doBIT(ctx, 7, 6);
        break;
      case 0x7F:
        this.doBIT(ctx, 7, 7);
        break;
      case 0x80:
        this.doRES(ctx, 0, 0);
        break;
      case 0x81:
        this.doRES(ctx, 0, 1);
        break;
      case 0x82:
        this.doRES(ctx, 0, 2);
        break;
      case 0x83:
        this.doRES(ctx, 0, 3);
        break;
      case 0x84:
        this.doRES(ctx, 0, 4);
        break;
      case 0x85:
        this.doRES(ctx, 0, 5);
        break;
      case 0x86:
        this.doRES(ctx, 0, 6);
        break;
      case 0x87:
        this.doRES(ctx, 0, 7);
        break;
      case 0x88:
        this.doRES(ctx, 1, 0);
        break;
      case 0x89:
        this.doRES(ctx, 1, 1);
        break;
      case 0x8A:
        this.doRES(ctx, 1, 2);
        break;
      case 0x8B:
        this.doRES(ctx, 1, 3);
        break;
      case 0x8C:
        this.doRES(ctx, 1, 4);
        break;
      case 0x8D:
        this.doRES(ctx, 1, 5);
        break;
      case 0x8E:
        this.doRES(ctx, 1, 6);
        break;
      case 0x8F:
        this.doRES(ctx, 1, 7);
        break;
      case 0x90:
        this.doRES(ctx, 2, 0);
        break;
      case 0x91:
        this.doRES(ctx, 2, 1);
        break;
      case 0x92:
        this.doRES(ctx, 2, 2);
        break;
      case 0x93:
        this.doRES(ctx, 2, 3);
        break;
      case 0x94:
        this.doRES(ctx, 2, 4);
        break;
      case 0x95:
        this.doRES(ctx, 2, 5);
        break;
      case 0x96:
        this.doRES(ctx, 2, 6);
        break;
      case 0x97:
        this.doRES(ctx, 2, 7);
        break;
      case 0x98:
        this.doRES(ctx, 3, 0);
        break;
      case 0x99:
        this.doRES(ctx, 3, 1);
        break;
      case 0x9A:
        this.doRES(ctx, 3, 2);
        break;
      case 0x9B:
        this.doRES(ctx, 3, 3);
        break;
      case 0x9C:
        this.doRES(ctx, 3, 4);
        break;
      case 0x9D:
        this.doRES(ctx, 3, 5);
        break;
      case 0x9E:
        this.doRES(ctx, 3, 6);
        break;
      case 0x9F:
        this.doRES(ctx, 3, 7);
        break;
      case 0xA0:
        this.doRES(ctx, 4, 0);
        break;
      case 0xA1:
        this.doRES(ctx, 4, 1);
        break;
      case 0xA2:
        this.doRES(ctx, 4, 2);
        break;
      case 0xA3:
        this.doRES(ctx, 4, 3);
        break;
      case 0xA4:
        this.doRES(ctx, 4, 4);
        break;
      case 0xA5:
        this.doRES(ctx, 4, 5);
        break;
      case 0xA6:
        this.doRES(ctx, 4, 6);
        break;
      case 0xA7:
        this.doRES(ctx, 4, 7);
        break;
      case 0xA8:
        this.doRES(ctx, 5, 0);
        break;
      case 0xA9:
        this.doRES(ctx, 5, 1);
        break;
      case 0xAA:
        this.doRES(ctx, 5, 2);
        break;
      case 0xAB:
        this.doRES(ctx, 5, 3);
        break;
      case 0xAC:
        this.doRES(ctx, 5, 4);
        break;
      case 0xAD:
        this.doRES(ctx, 5, 5);
        break;
      case 0xAE:
        this.doRES(ctx, 5, 6);
        break;
      case 0xAF:
        this.doRES(ctx, 5, 7);
        break;
      case 0xB0:
        this.doRES(ctx, 6, 0);
        break;
      case 0xB1:
        this.doRES(ctx, 6, 1);
        break;
      case 0xB2:
        this.doRES(ctx, 6, 2);
        break;
      case 0xB3:
        this.doRES(ctx, 6, 3);
        break;
      case 0xB4:
        this.doRES(ctx, 6, 4);
        break;
      case 0xB5:
        this.doRES(ctx, 6, 5);
        break;
      case 0xB6:
        this.doRES(ctx, 6, 6);
        break;
      case 0xB7:
        this.doRES(ctx, 6, 7);
        break;
      case 0xB8:
        this.doRES(ctx, 7, 0);
        break;
      case 0xB9:
        this.doRES(ctx, 7, 1);
        break;
      case 0xBA:
        this.doRES(ctx, 7, 2);
        break;
      case 0xBB:
        this.doRES(ctx, 7, 3);
        break;
      case 0xBC:
        this.doRES(ctx, 7, 4);
        break;
      case 0xBD:
        this.doRES(ctx, 7, 5);
        break;
      case 0xBE:
        this.doRES(ctx, 7, 6);
        break;
      case 0xBF:
        this.doRES(ctx, 7, 7);
        break;
      case 0xC0:
        this.doSET(ctx, 0, 0);
        break;
      case 0xC1:
        this.doSET(ctx, 0, 1);
        break;
      case 0xC2:
        this.doSET(ctx, 0, 2);
        break;
      case 0xC3:
        this.doSET(ctx, 0, 3);
        break;
      case 0xC4:
        this.doSET(ctx, 0, 4);
        break;
      case 0xC5:
        this.doSET(ctx, 0, 5);
        break;
      case 0xC6:
        this.doSET(ctx, 0, 6);
        break;
      case 0xC7:
        this.doSET(ctx, 0, 7);
        break;
      case 0xC8:
        this.doSET(ctx, 1, 0);
        break;
      case 0xC9:
        this.doSET(ctx, 1, 1);
        break;
      case 0xCA:
        this.doSET(ctx, 1, 2);
        break;
      case 0xCB:
        this.doSET(ctx, 1, 3);
        break;
      case 0xCC:
        this.doSET(ctx, 1, 4);
        break;
      case 0xCD:
        this.doSET(ctx, 1, 5);
        break;
      case 0xCE:
        this.doSET(ctx, 1, 6);
        break;
      case 0xCF:
        this.doSET(ctx, 1, 7);
        break;
      case 0xD0:
        this.doSET(ctx, 2, 0);
        break;
      case 0xD1:
        this.doSET(ctx, 2, 1);
        break;
      case 0xD2:
        this.doSET(ctx, 2, 2);
        break;
      case 0xD3:
        this.doSET(ctx, 2, 3);
        break;
      case 0xD4:
        this.doSET(ctx, 2, 4);
        break;
      case 0xD5:
        this.doSET(ctx, 2, 5);
        break;
      case 0xD6:
        this.doSET(ctx, 2, 6);
        break;
      case 0xD7:
        this.doSET(ctx, 2, 7);
        break;
      case 0xD8:
        this.doSET(ctx, 3, 0);
        break;
      case 0xD9:
        this.doSET(ctx, 3, 1);
        break;
      case 0xDA:
        this.doSET(ctx, 3, 2);
        break;
      case 0xDB:
        this.doSET(ctx, 3, 3);
        break;
      case 0xDC:
        this.doSET(ctx, 3, 4);
        break;
      case 0xDD:
        this.doSET(ctx, 3, 5);
        break;
      case 0xDE:
        this.doSET(ctx, 3, 6);
        break;
      case 0xDF:
        this.doSET(ctx, 3, 7);
        break;
      case 0xE0:
        this.doSET(ctx, 4, 0);
        break;
      case 0xE1:
        this.doSET(ctx, 4, 1);
        break;
      case 0xE2:
        this.doSET(ctx, 4, 2);
        break;
      case 0xE3:
        this.doSET(ctx, 4, 3);
        break;
      case 0xE4:
        this.doSET(ctx, 4, 4);
        break;
      case 0xE5:
        this.doSET(ctx, 4, 5);
        break;
      case 0xE6:
        this.doSET(ctx, 4, 6);
        break;
      case 0xE7:
        this.doSET(ctx, 4, 7);
        break;
      case 0xE8:
        this.doSET(ctx, 5, 0);
        break;
      case 0xE9:
        this.doSET(ctx, 5, 1);
        break;
      case 0xEA:
        this.doSET(ctx, 5, 2);
        break;
      case 0xEB:
        this.doSET(ctx, 5, 3);
        break;
      case 0xEC:
        this.doSET(ctx, 5, 4);
        break;
      case 0xED:
        this.doSET(ctx, 5, 5);
        break;
      case 0xEE:
        this.doSET(ctx, 5, 6);
        break;
      case 0xEF:
        this.doSET(ctx, 5, 7);
        break;
      case 0xF0:
        this.doSET(ctx, 6, 0);
        break;
      case 0xF1:
        this.doSET(ctx, 6, 1);
        break;
      case 0xF2:
        this.doSET(ctx, 6, 2);
        break;
      case 0xF3:
        this.doSET(ctx, 6, 3);
        break;
      case 0xF4:
        this.doSET(ctx, 6, 4);
        break;
      case 0xF5:
        this.doSET(ctx, 6, 5);
        break;
      case 0xF6:
        this.doSET(ctx, 6, 6);
        break;
      case 0xF7:
        this.doSET(ctx, 6, 7);
        break;
      case 0xF8:
        this.doSET(ctx, 7, 0);
        break;
      case 0xF9:
        this.doSET(ctx, 7, 1);
        break;
      case 0xFA:
        this.doSET(ctx, 7, 2);
        break;
      case 0xFB:
        this.doSET(ctx, 7, 3);
        break;
      case 0xFC:
        this.doSET(ctx, 7, 4);
        break;
      case 0xFD:
        this.doSET(ctx, 7, 5);
        break;
      case 0xFE:
        this.doSET(ctx, 7, 6);
        break;
      case 0xFF:
        this.doSET(ctx, 7, 7);
        break;
      default:
        throw new Error("Unexpected CB opcode");
    }
    this.prefix = 0;
  }

  private boolean decodeED(final int ctx, final int op, final boolean incomingInterrupt) {
    switch (op) {
      case 0xCB:
        this.prefix = 0xEDCB;
        return true;
      case 0x00:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x01:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x02:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x03:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x04:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x05:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x06:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x07:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x08:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x09:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x0A:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x0B:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x0C:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x0D:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x0E:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x0F:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x10:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x11:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x12:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x13:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x14:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x15:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x16:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x17:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x18:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x19:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x1A:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x1B:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x1C:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x1D:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x1E:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x1F:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x20:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x21:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x22:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x23:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x24:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x25:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x26:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x27:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x28:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x29:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x2A:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x2B:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x2C:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x2D:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x2E:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x2F:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x30:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x31:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x32:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x33:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x34:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x35:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x36:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x37:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x38:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x39:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x3A:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x3B:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x3C:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x3D:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x3E:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x3F:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x40:
        this.prefix = 0;
        this.doIN_C(ctx, 0);
        return true;
      case 0x41:
        this.prefix = 0;
        this.doOUT_C(ctx, 0);
        return true;
      case 0x42:
        this.prefix = 0;
        this.doSBC_HL_RegPair(0);
        return true;
      case 0x43:
        this.prefix = 0;
        this.doLD_mNN_RegP(ctx, 0);
        return true;
      case 0x44:
        this.prefix = 0;
        this.doNEG();
        return true;
      case 0x45:
        this.prefix = 0;
        this.doRETN(ctx);
        return true;
      case 0x46:
        this.prefix = 0;
        this.doIM(0);
        return true;
      case 0x47:
        this.prefix = 0;
        this.doLD_I_A();
        return true;
      case 0x48:
        this.prefix = 0;
        this.doIN_C(ctx, 1);
        return true;
      case 0x49:
        this.prefix = 0;
        this.doOUT_C(ctx, 1);
        return true;
      case 0x4A:
        this.prefix = 0;
        this.doADC_HL_RegPair(0);
        return true;
      case 0x4B:
        this.prefix = 0;
        this.doLD_RegP_mNN(ctx, 0);
        return true;
      case 0x4C:
        this.prefix = 0;
        this.doNEG();
        return true;
      case 0x4D:
        this.prefix = 0;
        this.doRETI(ctx);
        return true;
      case 0x4E:
        this.prefix = 0;
        this.doIM(1);
        return true;
      case 0x4F:
        this.prefix = 0;
        this.doLD_R_A();
        return true;
      case 0x50:
        this.prefix = 0;
        this.doIN_C(ctx, 2);
        return true;
      case 0x51:
        this.prefix = 0;
        this.doOUT_C(ctx, 2);
        return true;
      case 0x52:
        this.prefix = 0;
        this.doSBC_HL_RegPair(1);
        return true;
      case 0x53:
        this.prefix = 0;
        this.doLD_mNN_RegP(ctx, 1);
        return true;
      case 0x54:
        this.prefix = 0;
        this.doNEG();
        return true;
      case 0x55:
        this.prefix = 0;
        this.doRETN(ctx);
        return true;
      case 0x56:
        this.prefix = 0;
        this.doIM(2);
        return true;
      case 0x57:
        this.prefix = 0;
        this.doLD_A_I(incomingInterrupt);
        return true;
      case 0x58:
        this.prefix = 0;
        this.doIN_C(ctx, 3);
        return true;
      case 0x59:
        this.prefix = 0;
        this.doOUT_C(ctx, 3);
        return true;
      case 0x5A:
        this.prefix = 0;
        this.doADC_HL_RegPair(1);
        return true;
      case 0x5B:
        this.prefix = 0;
        this.doLD_RegP_mNN(ctx, 1);
        return true;
      case 0x5C:
        this.prefix = 0;
        this.doNEG();
        return true;
      case 0x5D:
        this.prefix = 0;
        this.doRETN(ctx);
        return true;
      case 0x5E:
        this.prefix = 0;
        this.doIM(3);
        return true;
      case 0x5F:
        this.prefix = 0;
        this.doLD_A_R(incomingInterrupt);
        return true;
      case 0x60:
        this.prefix = 0;
        this.doIN_C(ctx, 4);
        return true;
      case 0x61:
        this.prefix = 0;
        this.doOUT_C(ctx, 4);
        return true;
      case 0x62:
        this.prefix = 0;
        this.doSBC_HL_RegPair(2);
        return true;
      case 0x63:
        this.prefix = 0;
        this.doLD_mNN_RegP(ctx, 2);
        return true;
      case 0x64:
        this.prefix = 0;
        this.doNEG();
        return true;
      case 0x65:
        this.prefix = 0;
        this.doRETN(ctx);
        return true;
      case 0x66:
        this.prefix = 0;
        this.doIM(4);
        return true;
      case 0x67:
        this.prefix = 0;
        this.doRRD(ctx);
        return true;
      case 0x68:
        this.prefix = 0;
        this.doIN_C(ctx, 5);
        return true;
      case 0x69:
        this.prefix = 0;
        this.doOUT_C(ctx, 5);
        return true;
      case 0x6A:
        this.prefix = 0;
        this.doADC_HL_RegPair(2);
        return true;
      case 0x6B:
        this.prefix = 0;
        this.doLD_RegP_mNN(ctx, 2);
        return true;
      case 0x6C:
        this.prefix = 0;
        this.doNEG();
        return true;
      case 0x6D:
        this.prefix = 0;
        this.doRETN(ctx);
        return true;
      case 0x6E:
        this.prefix = 0;
        this.doIM(5);
        return true;
      case 0x6F:
        this.prefix = 0;
        this.doRLD(ctx);
        return true;
      case 0x70:
        this.prefix = 0;
        this.doIN_C(ctx);
        return true;
      case 0x71:
        this.prefix = 0;
        this.doOUT_C(ctx);
        return true;
      case 0x72:
        this.prefix = 0;
        this.doSBC_HL_RegPair(3);
        return true;
      case 0x73:
        this.prefix = 0;
        this.doLD_mNN_RegP(ctx, 3);
        return true;
      case 0x74:
        this.prefix = 0;
        this.doNEG();
        return true;
      case 0x75:
        this.prefix = 0;
        this.doRETN(ctx);
        return true;
      case 0x76:
        this.prefix = 0;
        this.doIM(6);
        return true;
      case 0x77:
        this.prefix = 0;
        this.doNOP();
        return true;
      case 0x78:
        this.prefix = 0;
        this.doIN_C(ctx, 7);
        return true;
      case 0x79:
        this.prefix = 0;
        this.doOUT_C(ctx, 7);
        return true;
      case 0x7A:
        this.prefix = 0;
        this.doADC_HL_RegPair(3);
        return true;
      case 0x7B:
        this.prefix = 0;
        this.doLD_RegP_mNN(ctx, 3);
        return true;
      case 0x7C:
        this.prefix = 0;
        this.doNEG();
        return true;
      case 0x7D:
        this.prefix = 0;
        this.doRETN(ctx);
        return true;
      case 0x7E:
        this.prefix = 0;
        this.doIM(7);
        return true;
      case 0x7F:
        this.prefix = 0;
        this.doNOP();
        return true;
      case 0x80:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x81:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x82:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x83:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x84:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x85:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x86:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x87:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x88:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x89:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x8A:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x8B:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x8C:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x8D:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x8E:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x8F:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x90:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x91:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x92:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x93:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x94:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x95:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x96:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x97:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x98:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x99:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x9A:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x9B:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x9C:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x9D:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x9E:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0x9F:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xA0:
        this.prefix = 0;
        this.doBLI(ctx, 4, 0, incomingInterrupt);
        return true;
      case 0xA1:
        this.prefix = 0;
        this.doBLI(ctx, 4, 1, incomingInterrupt);
        return true;
      case 0xA2:
        this.prefix = 0;
        this.doBLI(ctx, 4, 2, incomingInterrupt);
        return true;
      case 0xA3:
        this.prefix = 0;
        this.doBLI(ctx, 4, 3, incomingInterrupt);
        return true;
      case 0xA4:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xA5:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xA6:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xA7:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xA8:
        this.prefix = 0;
        this.doBLI(ctx, 5, 0, incomingInterrupt);
        return true;
      case 0xA9:
        this.prefix = 0;
        this.doBLI(ctx, 5, 1, incomingInterrupt);
        return true;
      case 0xAA:
        this.prefix = 0;
        this.doBLI(ctx, 5, 2, incomingInterrupt);
        return true;
      case 0xAB:
        this.prefix = 0;
        this.doBLI(ctx, 5, 3, incomingInterrupt);
        return true;
      case 0xAC:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xAD:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xAE:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xAF:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xB0:
        this.prefix = 0;
        this.doBLI(ctx, 6, 0, incomingInterrupt);
        return true;
      case 0xB1:
        this.prefix = 0;
        this.doBLI(ctx, 6, 1, incomingInterrupt);
        return true;
      case 0xB2:
        this.prefix = 0;
        this.doBLI(ctx, 6, 2, incomingInterrupt);
        return true;
      case 0xB3:
        this.prefix = 0;
        this.doBLI(ctx, 6, 3, incomingInterrupt);
        return true;
      case 0xB4:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xB5:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xB6:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xB7:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xB8:
        this.prefix = 0;
        this.doBLI(ctx, 7, 0, incomingInterrupt);
        return true;
      case 0xB9:
        this.prefix = 0;
        this.doBLI(ctx, 7, 1, incomingInterrupt);
        return true;
      case 0xBA:
        this.prefix = 0;
        this.doBLI(ctx, 7, 2, incomingInterrupt);
        return true;
      case 0xBB:
        this.prefix = 0;
        this.doBLI(ctx, 7, 3, incomingInterrupt);
        return true;
      case 0xBC:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xBD:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xBE:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xBF:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xC0:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xC1:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xC2:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xC3:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xC4:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xC5:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xC6:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xC7:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xC8:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xC9:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xCA:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xCC:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xCD:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xCE:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xCF:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xD0:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xD1:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xD2:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xD3:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xD4:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xD5:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xD6:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xD7:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xD8:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xD9:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xDA:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xDB:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xDC:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xDD:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xDE:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xDF:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xE0:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xE1:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xE2:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xE3:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xE4:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xE5:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xE6:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xE7:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xE8:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xE9:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xEA:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xEB:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xEC:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xED:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xEE:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xEF:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xF0:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xF1:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xF2:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xF3:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xF4:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xF5:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xF6:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xF7:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xF8:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xF9:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xFA:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xFB:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xFC:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xFD:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xFE:
        this.prefix = 0;
        this.doNONI();
        return true;
      case 0xFF:
        this.prefix = 0;
        this.doNONI();
        return true;
      default:
        throw new Error("Unexpected ED opcode");
    }
  }

  private boolean decodeIndexedCB(final int ctx, final int op) {
    if (this.cbDisplacementByte < 0) {
      this.cbDisplacementByte = op;
      return false;
    }
    switch (op) {
      case 0x00:
        this.doROTmem_LDreg(ctx, 0, 0);
        break;
      case 0x01:
        this.doROTmem_LDreg(ctx, 1, 0);
        break;
      case 0x02:
        this.doROTmem_LDreg(ctx, 2, 0);
        break;
      case 0x03:
        this.doROTmem_LDreg(ctx, 3, 0);
        break;
      case 0x04:
        this.doROTmem_LDreg(ctx, 4, 0);
        break;
      case 0x05:
        this.doROTmem_LDreg(ctx, 5, 0);
        break;
      case 0x06:
        this.doRollShift(ctx, 0, 6);
        break;
      case 0x07:
        this.doROTmem_LDreg(ctx, 7, 0);
        break;
      case 0x08:
        this.doROTmem_LDreg(ctx, 0, 1);
        break;
      case 0x09:
        this.doROTmem_LDreg(ctx, 1, 1);
        break;
      case 0x0A:
        this.doROTmem_LDreg(ctx, 2, 1);
        break;
      case 0x0B:
        this.doROTmem_LDreg(ctx, 3, 1);
        break;
      case 0x0C:
        this.doROTmem_LDreg(ctx, 4, 1);
        break;
      case 0x0D:
        this.doROTmem_LDreg(ctx, 5, 1);
        break;
      case 0x0E:
        this.doRollShift(ctx, 1, 6);
        break;
      case 0x0F:
        this.doROTmem_LDreg(ctx, 7, 1);
        break;
      case 0x10:
        this.doROTmem_LDreg(ctx, 0, 2);
        break;
      case 0x11:
        this.doROTmem_LDreg(ctx, 1, 2);
        break;
      case 0x12:
        this.doROTmem_LDreg(ctx, 2, 2);
        break;
      case 0x13:
        this.doROTmem_LDreg(ctx, 3, 2);
        break;
      case 0x14:
        this.doROTmem_LDreg(ctx, 4, 2);
        break;
      case 0x15:
        this.doROTmem_LDreg(ctx, 5, 2);
        break;
      case 0x16:
        this.doRollShift(ctx, 2, 6);
        break;
      case 0x17:
        this.doROTmem_LDreg(ctx, 7, 2);
        break;
      case 0x18:
        this.doROTmem_LDreg(ctx, 0, 3);
        break;
      case 0x19:
        this.doROTmem_LDreg(ctx, 1, 3);
        break;
      case 0x1A:
        this.doROTmem_LDreg(ctx, 2, 3);
        break;
      case 0x1B:
        this.doROTmem_LDreg(ctx, 3, 3);
        break;
      case 0x1C:
        this.doROTmem_LDreg(ctx, 4, 3);
        break;
      case 0x1D:
        this.doROTmem_LDreg(ctx, 5, 3);
        break;
      case 0x1E:
        this.doRollShift(ctx, 3, 6);
        break;
      case 0x1F:
        this.doROTmem_LDreg(ctx, 7, 3);
        break;
      case 0x20:
        this.doROTmem_LDreg(ctx, 0, 4);
        break;
      case 0x21:
        this.doROTmem_LDreg(ctx, 1, 4);
        break;
      case 0x22:
        this.doROTmem_LDreg(ctx, 2, 4);
        break;
      case 0x23:
        this.doROTmem_LDreg(ctx, 3, 4);
        break;
      case 0x24:
        this.doROTmem_LDreg(ctx, 4, 4);
        break;
      case 0x25:
        this.doROTmem_LDreg(ctx, 5, 4);
        break;
      case 0x26:
        this.doRollShift(ctx, 4, 6);
        break;
      case 0x27:
        this.doROTmem_LDreg(ctx, 7, 4);
        break;
      case 0x28:
        this.doROTmem_LDreg(ctx, 0, 5);
        break;
      case 0x29:
        this.doROTmem_LDreg(ctx, 1, 5);
        break;
      case 0x2A:
        this.doROTmem_LDreg(ctx, 2, 5);
        break;
      case 0x2B:
        this.doROTmem_LDreg(ctx, 3, 5);
        break;
      case 0x2C:
        this.doROTmem_LDreg(ctx, 4, 5);
        break;
      case 0x2D:
        this.doROTmem_LDreg(ctx, 5, 5);
        break;
      case 0x2E:
        this.doRollShift(ctx, 5, 6);
        break;
      case 0x2F:
        this.doROTmem_LDreg(ctx, 7, 5);
        break;
      case 0x30:
        this.doROTmem_LDreg(ctx, 0, 6);
        break;
      case 0x31:
        this.doROTmem_LDreg(ctx, 1, 6);
        break;
      case 0x32:
        this.doROTmem_LDreg(ctx, 2, 6);
        break;
      case 0x33:
        this.doROTmem_LDreg(ctx, 3, 6);
        break;
      case 0x34:
        this.doROTmem_LDreg(ctx, 4, 6);
        break;
      case 0x35:
        this.doROTmem_LDreg(ctx, 5, 6);
        break;
      case 0x36:
        this.doRollShift(ctx, 6, 6);
        break;
      case 0x37:
        this.doROTmem_LDreg(ctx, 7, 6);
        break;
      case 0x38:
        this.doROTmem_LDreg(ctx, 0, 7);
        break;
      case 0x39:
        this.doROTmem_LDreg(ctx, 1, 7);
        break;
      case 0x3A:
        this.doROTmem_LDreg(ctx, 2, 7);
        break;
      case 0x3B:
        this.doROTmem_LDreg(ctx, 3, 7);
        break;
      case 0x3C:
        this.doROTmem_LDreg(ctx, 4, 7);
        break;
      case 0x3D:
        this.doROTmem_LDreg(ctx, 5, 7);
        break;
      case 0x3E:
        this.doRollShift(ctx, 7, 6);
        break;
      case 0x3F:
        this.doROTmem_LDreg(ctx, 7, 7);
        break;
      case 0x40:
        this.doBIT(ctx, 0, 6);
        break;
      case 0x41:
        this.doBIT(ctx, 0, 6);
        break;
      case 0x42:
        this.doBIT(ctx, 0, 6);
        break;
      case 0x43:
        this.doBIT(ctx, 0, 6);
        break;
      case 0x44:
        this.doBIT(ctx, 0, 6);
        break;
      case 0x45:
        this.doBIT(ctx, 0, 6);
        break;
      case 0x46:
        this.doBIT(ctx, 0, 6);
        break;
      case 0x47:
        this.doBIT(ctx, 0, 6);
        break;
      case 0x48:
        this.doBIT(ctx, 1, 6);
        break;
      case 0x49:
        this.doBIT(ctx, 1, 6);
        break;
      case 0x4A:
        this.doBIT(ctx, 1, 6);
        break;
      case 0x4B:
        this.doBIT(ctx, 1, 6);
        break;
      case 0x4C:
        this.doBIT(ctx, 1, 6);
        break;
      case 0x4D:
        this.doBIT(ctx, 1, 6);
        break;
      case 0x4E:
        this.doBIT(ctx, 1, 6);
        break;
      case 0x4F:
        this.doBIT(ctx, 1, 6);
        break;
      case 0x50:
        this.doBIT(ctx, 2, 6);
        break;
      case 0x51:
        this.doBIT(ctx, 2, 6);
        break;
      case 0x52:
        this.doBIT(ctx, 2, 6);
        break;
      case 0x53:
        this.doBIT(ctx, 2, 6);
        break;
      case 0x54:
        this.doBIT(ctx, 2, 6);
        break;
      case 0x55:
        this.doBIT(ctx, 2, 6);
        break;
      case 0x56:
        this.doBIT(ctx, 2, 6);
        break;
      case 0x57:
        this.doBIT(ctx, 2, 6);
        break;
      case 0x58:
        this.doBIT(ctx, 3, 6);
        break;
      case 0x59:
        this.doBIT(ctx, 3, 6);
        break;
      case 0x5A:
        this.doBIT(ctx, 3, 6);
        break;
      case 0x5B:
        this.doBIT(ctx, 3, 6);
        break;
      case 0x5C:
        this.doBIT(ctx, 3, 6);
        break;
      case 0x5D:
        this.doBIT(ctx, 3, 6);
        break;
      case 0x5E:
        this.doBIT(ctx, 3, 6);
        break;
      case 0x5F:
        this.doBIT(ctx, 3, 6);
        break;
      case 0x60:
        this.doBIT(ctx, 4, 6);
        break;
      case 0x61:
        this.doBIT(ctx, 4, 6);
        break;
      case 0x62:
        this.doBIT(ctx, 4, 6);
        break;
      case 0x63:
        this.doBIT(ctx, 4, 6);
        break;
      case 0x64:
        this.doBIT(ctx, 4, 6);
        break;
      case 0x65:
        this.doBIT(ctx, 4, 6);
        break;
      case 0x66:
        this.doBIT(ctx, 4, 6);
        break;
      case 0x67:
        this.doBIT(ctx, 4, 6);
        break;
      case 0x68:
        this.doBIT(ctx, 5, 6);
        break;
      case 0x69:
        this.doBIT(ctx, 5, 6);
        break;
      case 0x6A:
        this.doBIT(ctx, 5, 6);
        break;
      case 0x6B:
        this.doBIT(ctx, 5, 6);
        break;
      case 0x6C:
        this.doBIT(ctx, 5, 6);
        break;
      case 0x6D:
        this.doBIT(ctx, 5, 6);
        break;
      case 0x6E:
        this.doBIT(ctx, 5, 6);
        break;
      case 0x6F:
        this.doBIT(ctx, 5, 6);
        break;
      case 0x70:
        this.doBIT(ctx, 6, 6);
        break;
      case 0x71:
        this.doBIT(ctx, 6, 6);
        break;
      case 0x72:
        this.doBIT(ctx, 6, 6);
        break;
      case 0x73:
        this.doBIT(ctx, 6, 6);
        break;
      case 0x74:
        this.doBIT(ctx, 6, 6);
        break;
      case 0x75:
        this.doBIT(ctx, 6, 6);
        break;
      case 0x76:
        this.doBIT(ctx, 6, 6);
        break;
      case 0x77:
        this.doBIT(ctx, 6, 6);
        break;
      case 0x78:
        this.doBIT(ctx, 7, 6);
        break;
      case 0x79:
        this.doBIT(ctx, 7, 6);
        break;
      case 0x7A:
        this.doBIT(ctx, 7, 6);
        break;
      case 0x7B:
        this.doBIT(ctx, 7, 6);
        break;
      case 0x7C:
        this.doBIT(ctx, 7, 6);
        break;
      case 0x7D:
        this.doBIT(ctx, 7, 6);
        break;
      case 0x7E:
        this.doBIT(ctx, 7, 6);
        break;
      case 0x7F:
        this.doBIT(ctx, 7, 6);
        break;
      case 0x80:
        this.doRESmem_LDreg(ctx, 0, 0);
        break;
      case 0x81:
        this.doRESmem_LDreg(ctx, 1, 0);
        break;
      case 0x82:
        this.doRESmem_LDreg(ctx, 2, 0);
        break;
      case 0x83:
        this.doRESmem_LDreg(ctx, 3, 0);
        break;
      case 0x84:
        this.doRESmem_LDreg(ctx, 4, 0);
        break;
      case 0x85:
        this.doRESmem_LDreg(ctx, 5, 0);
        break;
      case 0x86:
        this.doRES(ctx, 0, 6);
        break;
      case 0x87:
        this.doRESmem_LDreg(ctx, 7, 0);
        break;
      case 0x88:
        this.doRESmem_LDreg(ctx, 0, 1);
        break;
      case 0x89:
        this.doRESmem_LDreg(ctx, 1, 1);
        break;
      case 0x8A:
        this.doRESmem_LDreg(ctx, 2, 1);
        break;
      case 0x8B:
        this.doRESmem_LDreg(ctx, 3, 1);
        break;
      case 0x8C:
        this.doRESmem_LDreg(ctx, 4, 1);
        break;
      case 0x8D:
        this.doRESmem_LDreg(ctx, 5, 1);
        break;
      case 0x8E:
        this.doRES(ctx, 1, 6);
        break;
      case 0x8F:
        this.doRESmem_LDreg(ctx, 7, 1);
        break;
      case 0x90:
        this.doRESmem_LDreg(ctx, 0, 2);
        break;
      case 0x91:
        this.doRESmem_LDreg(ctx, 1, 2);
        break;
      case 0x92:
        this.doRESmem_LDreg(ctx, 2, 2);
        break;
      case 0x93:
        this.doRESmem_LDreg(ctx, 3, 2);
        break;
      case 0x94:
        this.doRESmem_LDreg(ctx, 4, 2);
        break;
      case 0x95:
        this.doRESmem_LDreg(ctx, 5, 2);
        break;
      case 0x96:
        this.doRES(ctx, 2, 6);
        break;
      case 0x97:
        this.doRESmem_LDreg(ctx, 7, 2);
        break;
      case 0x98:
        this.doRESmem_LDreg(ctx, 0, 3);
        break;
      case 0x99:
        this.doRESmem_LDreg(ctx, 1, 3);
        break;
      case 0x9A:
        this.doRESmem_LDreg(ctx, 2, 3);
        break;
      case 0x9B:
        this.doRESmem_LDreg(ctx, 3, 3);
        break;
      case 0x9C:
        this.doRESmem_LDreg(ctx, 4, 3);
        break;
      case 0x9D:
        this.doRESmem_LDreg(ctx, 5, 3);
        break;
      case 0x9E:
        this.doRES(ctx, 3, 6);
        break;
      case 0x9F:
        this.doRESmem_LDreg(ctx, 7, 3);
        break;
      case 0xA0:
        this.doRESmem_LDreg(ctx, 0, 4);
        break;
      case 0xA1:
        this.doRESmem_LDreg(ctx, 1, 4);
        break;
      case 0xA2:
        this.doRESmem_LDreg(ctx, 2, 4);
        break;
      case 0xA3:
        this.doRESmem_LDreg(ctx, 3, 4);
        break;
      case 0xA4:
        this.doRESmem_LDreg(ctx, 4, 4);
        break;
      case 0xA5:
        this.doRESmem_LDreg(ctx, 5, 4);
        break;
      case 0xA6:
        this.doRES(ctx, 4, 6);
        break;
      case 0xA7:
        this.doRESmem_LDreg(ctx, 7, 4);
        break;
      case 0xA8:
        this.doRESmem_LDreg(ctx, 0, 5);
        break;
      case 0xA9:
        this.doRESmem_LDreg(ctx, 1, 5);
        break;
      case 0xAA:
        this.doRESmem_LDreg(ctx, 2, 5);
        break;
      case 0xAB:
        this.doRESmem_LDreg(ctx, 3, 5);
        break;
      case 0xAC:
        this.doRESmem_LDreg(ctx, 4, 5);
        break;
      case 0xAD:
        this.doRESmem_LDreg(ctx, 5, 5);
        break;
      case 0xAE:
        this.doRES(ctx, 5, 6);
        break;
      case 0xAF:
        this.doRESmem_LDreg(ctx, 7, 5);
        break;
      case 0xB0:
        this.doRESmem_LDreg(ctx, 0, 6);
        break;
      case 0xB1:
        this.doRESmem_LDreg(ctx, 1, 6);
        break;
      case 0xB2:
        this.doRESmem_LDreg(ctx, 2, 6);
        break;
      case 0xB3:
        this.doRESmem_LDreg(ctx, 3, 6);
        break;
      case 0xB4:
        this.doRESmem_LDreg(ctx, 4, 6);
        break;
      case 0xB5:
        this.doRESmem_LDreg(ctx, 5, 6);
        break;
      case 0xB6:
        this.doRES(ctx, 6, 6);
        break;
      case 0xB7:
        this.doRESmem_LDreg(ctx, 7, 6);
        break;
      case 0xB8:
        this.doRESmem_LDreg(ctx, 0, 7);
        break;
      case 0xB9:
        this.doRESmem_LDreg(ctx, 1, 7);
        break;
      case 0xBA:
        this.doRESmem_LDreg(ctx, 2, 7);
        break;
      case 0xBB:
        this.doRESmem_LDreg(ctx, 3, 7);
        break;
      case 0xBC:
        this.doRESmem_LDreg(ctx, 4, 7);
        break;
      case 0xBD:
        this.doRESmem_LDreg(ctx, 5, 7);
        break;
      case 0xBE:
        this.doRES(ctx, 7, 6);
        break;
      case 0xBF:
        this.doRESmem_LDreg(ctx, 7, 7);
        break;
      case 0xC0:
        this.doSETmem_LDreg(ctx, 0, 0);
        break;
      case 0xC1:
        this.doSETmem_LDreg(ctx, 1, 0);
        break;
      case 0xC2:
        this.doSETmem_LDreg(ctx, 2, 0);
        break;
      case 0xC3:
        this.doSETmem_LDreg(ctx, 3, 0);
        break;
      case 0xC4:
        this.doSETmem_LDreg(ctx, 4, 0);
        break;
      case 0xC5:
        this.doSETmem_LDreg(ctx, 5, 0);
        break;
      case 0xC6:
        this.doSET(ctx, 0, 6);
        break;
      case 0xC7:
        this.doSETmem_LDreg(ctx, 7, 0);
        break;
      case 0xC8:
        this.doSETmem_LDreg(ctx, 0, 1);
        break;
      case 0xC9:
        this.doSETmem_LDreg(ctx, 1, 1);
        break;
      case 0xCA:
        this.doSETmem_LDreg(ctx, 2, 1);
        break;
      case 0xCB:
        this.doSETmem_LDreg(ctx, 3, 1);
        break;
      case 0xCC:
        this.doSETmem_LDreg(ctx, 4, 1);
        break;
      case 0xCD:
        this.doSETmem_LDreg(ctx, 5, 1);
        break;
      case 0xCE:
        this.doSET(ctx, 1, 6);
        break;
      case 0xCF:
        this.doSETmem_LDreg(ctx, 7, 1);
        break;
      case 0xD0:
        this.doSETmem_LDreg(ctx, 0, 2);
        break;
      case 0xD1:
        this.doSETmem_LDreg(ctx, 1, 2);
        break;
      case 0xD2:
        this.doSETmem_LDreg(ctx, 2, 2);
        break;
      case 0xD3:
        this.doSETmem_LDreg(ctx, 3, 2);
        break;
      case 0xD4:
        this.doSETmem_LDreg(ctx, 4, 2);
        break;
      case 0xD5:
        this.doSETmem_LDreg(ctx, 5, 2);
        break;
      case 0xD6:
        this.doSET(ctx, 2, 6);
        break;
      case 0xD7:
        this.doSETmem_LDreg(ctx, 7, 2);
        break;
      case 0xD8:
        this.doSETmem_LDreg(ctx, 0, 3);
        break;
      case 0xD9:
        this.doSETmem_LDreg(ctx, 1, 3);
        break;
      case 0xDA:
        this.doSETmem_LDreg(ctx, 2, 3);
        break;
      case 0xDB:
        this.doSETmem_LDreg(ctx, 3, 3);
        break;
      case 0xDC:
        this.doSETmem_LDreg(ctx, 4, 3);
        break;
      case 0xDD:
        this.doSETmem_LDreg(ctx, 5, 3);
        break;
      case 0xDE:
        this.doSET(ctx, 3, 6);
        break;
      case 0xDF:
        this.doSETmem_LDreg(ctx, 7, 3);
        break;
      case 0xE0:
        this.doSETmem_LDreg(ctx, 0, 4);
        break;
      case 0xE1:
        this.doSETmem_LDreg(ctx, 1, 4);
        break;
      case 0xE2:
        this.doSETmem_LDreg(ctx, 2, 4);
        break;
      case 0xE3:
        this.doSETmem_LDreg(ctx, 3, 4);
        break;
      case 0xE4:
        this.doSETmem_LDreg(ctx, 4, 4);
        break;
      case 0xE5:
        this.doSETmem_LDreg(ctx, 5, 4);
        break;
      case 0xE6:
        this.doSET(ctx, 4, 6);
        break;
      case 0xE7:
        this.doSETmem_LDreg(ctx, 7, 4);
        break;
      case 0xE8:
        this.doSETmem_LDreg(ctx, 0, 5);
        break;
      case 0xE9:
        this.doSETmem_LDreg(ctx, 1, 5);
        break;
      case 0xEA:
        this.doSETmem_LDreg(ctx, 2, 5);
        break;
      case 0xEB:
        this.doSETmem_LDreg(ctx, 3, 5);
        break;
      case 0xEC:
        this.doSETmem_LDreg(ctx, 4, 5);
        break;
      case 0xED:
        this.doSETmem_LDreg(ctx, 5, 5);
        break;
      case 0xEE:
        this.doSET(ctx, 5, 6);
        break;
      case 0xEF:
        this.doSETmem_LDreg(ctx, 7, 5);
        break;
      case 0xF0:
        this.doSETmem_LDreg(ctx, 0, 6);
        break;
      case 0xF1:
        this.doSETmem_LDreg(ctx, 1, 6);
        break;
      case 0xF2:
        this.doSETmem_LDreg(ctx, 2, 6);
        break;
      case 0xF3:
        this.doSETmem_LDreg(ctx, 3, 6);
        break;
      case 0xF4:
        this.doSETmem_LDreg(ctx, 4, 6);
        break;
      case 0xF5:
        this.doSETmem_LDreg(ctx, 5, 6);
        break;
      case 0xF6:
        this.doSET(ctx, 6, 6);
        break;
      case 0xF7:
        this.doSETmem_LDreg(ctx, 7, 6);
        break;
      case 0xF8:
        this.doSETmem_LDreg(ctx, 0, 7);
        break;
      case 0xF9:
        this.doSETmem_LDreg(ctx, 1, 7);
        break;
      case 0xFA:
        this.doSETmem_LDreg(ctx, 2, 7);
        break;
      case 0xFB:
        this.doSETmem_LDreg(ctx, 3, 7);
        break;
      case 0xFC:
        this.doSETmem_LDreg(ctx, 4, 7);
        break;
      case 0xFD:
        this.doSETmem_LDreg(ctx, 5, 7);
        break;
      case 0xFE:
        this.doSET(ctx, 7, 6);
        break;
      case 0xFF:
        this.doSETmem_LDreg(ctx, 7, 7);
        break;
      default:
        throw new Error("Unexpected DDCB opcode");
    }
    this.prefix = 0;
    this.cbDisplacementByte = -1;
    return true;
  }

  private void doNONI() {
    this.prefix = 0;
    this.stepAllowsInterruption = false;
  }

  private void doHalt() {
    this.outSignals &= (~SIGNAL_OUT_nHALT & 0xFF);
    this.regPC--;
  }

  private void doNOP() {
  }

  private void doEX_AF_AF() {
    int temp = this.regA;
    this.regA = this.altA;
    this.altA = temp;
    temp = this.regF;
    this.regF = this.altF;
    this.altF = temp;
  }

  private void doDJNZ(final int ctx) {
    final int offset = (byte) readInstrOrPrefix(ctx, false);
    this.tiStates++;
    int b = _readSpecRegValue(ctx, REG_B, this.regB) & 0xFF;
    final int address = (this.regPC + offset) & 0xFFFF;
    if (--b != 0) {
      this.regPC = address;
      this.setMemPtr(address);
      this.tiStates += 5;
    }
    this.regB = b & 0xFF;
  }

  private void doJR(final int ctx, final int cc) {
    final int offset = (byte) readInstrOrPrefix(ctx, false);
    if (checkCondition(cc)) {
      final int address = (this.regPC + offset) & 0xFFFF;
      this.setMemPtr(address);
      this.regPC = address;
      this.tiStates += 5;
    }
  }

  private void doJR(final int ctx) {
    final int offset = (byte) readInstrOrPrefix(ctx, false);
    final int address = (this.regPC + offset) & 0xFFFF;
    this.regPC = address;
    this.setMemPtr(address);
    this.tiStates += 5;
  }

  private void doLDRegPairByNextWord(final int ctx, final int p) {
    final int value = _readNextPcAddressedWord(ctx);
    writeReg16(p, value);
  }

  private void doADD_HL_RegPair(final int p) {
    final int reg = readReg16(2);
    final int value = readReg16(p);
    final int result = reg + value;
    writeReg16(2, result);

    final int c = reg ^ value ^ result;
    final int f = (byte) ((this.regF & FLAG_SZPV)
        | ((result >>> 8) & FLAG_XY)
        | ((c >>> 8) & FLAG_H) | ((c >>> (16 - FLAG_C_SHIFT))));
    this.internalRegQ = f;
    this.regF = f & 0xFF;

    this.setMemPtr(reg + 1);

    this.tiStates += 7;
  }

  private void doADC_HL_RegPair(final int p) {
    final int x = readReg16(2);
    final int y = readReg16(p);

    final int z = x + y + (this.regF & FLAG_C);

    int c = x ^ y ^ z;
    int f = (z & 0xffff) != 0 ? (z >> 8) & FLAG_SYX : FLAG_Z;

    f |= (c >>> 8) & FLAG_H;
    f |= FTABLE_OVERFLOW[c >>> 15];
    f |= z >>> (16 - FLAG_C_SHIFT);

    this.internalRegQ = f;
    this.regF = f & 0xFF;
    writeReg16(2, z);

    this.setMemPtr(x + 1);

    this.tiStates += 7;
  }

  private void doSBC_HL_RegPair(final int p) {
    final int x = readReg16(2);
    final int y = readReg16(p);

    final int z = x - y - (this.regF & FLAG_C);
    int c = x ^ y ^ z;

    int f = FLAG_N;
    f |= (z & 0xffff) != 0 ? (z >>> 8) & FLAG_SYX : FLAG_Z;

    f |= (c >>> 8) & FLAG_H;
    c &= 0x018000;
    f |= FTABLE_OVERFLOW[c >>> 15];
    f |= c >>> (16 - FLAG_C_SHIFT);

    writeReg16(2, z);
    this.internalRegQ = f;
    this.regF = f & 0xFF;

    this.setMemPtr(x + 1);

    this.tiStates += 7;
  }

  private void writeReg8_forLdReg8Instruction(final int ctx, final int r, final int value) {
    switch (r) {
      case 0:
        this.regB = value & 0xFF;
        return;
      case 1:
        this.regC = value & 0xFF;
        return;
      case 2:
        this.regD = value & 0xFF;
        return;
      case 3:
        this.regE = value & 0xFF;
        return;
      case 4:
        switch (normalizedPrefix()) {
          case 0x00:
            this.regH = value & 0xFF;
            return;
          case 0xDD:
            this.regIX = ((value & 0xFF) << 8) | (this.regIX & 0x00FF);
            return;
          case 0xFD:
            this.regIY = ((value & 0xFF) << 8) | (this.regIY & 0x00FF);
            return;
        }
        break;
      case 5:
        switch (normalizedPrefix()) {
          case 0x00:
            this.regL = value & 0xFF;
            return;
          case 0xDD:
            this.regIX = (this.regIX & 0xFF00) | (value & 0xFF);
            return;
          case 0xFD:
            this.regIY = (this.regIY & 0xFF00) | (value & 0xFF);
            return;
        }
        break;
      case 6: { // (HL)
        switch (normalizedPrefix()) {
          case 0x00:
            _writemem8(ctx, _readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL)),
                (byte) value);
            return;
          case 0xDD: {
            final int address =
                _readPtr(ctx, REG_IX, this.regIX) + (byte) readInstrOrPrefix(ctx, false);
            _writemem8(ctx, address, (byte) value);
            this.setMemPtr(address);
            this.tiStates += 5;
            return;
          }
          case 0xFD: {
            final int address =
                _readPtr(ctx, REG_IY, this.regIY) + (byte) readInstrOrPrefix(ctx, false);
            _writemem8(ctx, address, (byte) value);
            this.setMemPtr(address);
            this.tiStates += 5;
            return;
          }
        }
      }
      break;
      case 7:
        this.regA = value & 0xFF;
        return;
    }
    throw new Error("unexpected P index or prefix [" + this.prefix + ':' + r + ']');
  }

  private void writeReg8_UseCachedInstructionByte(final int ctx, final int r, final int value) {
    switch (r) {
      case 0:
        this.regB = value & 0xFF;
        return;
      case 1:
        this.regC = value & 0xFF;
        return;
      case 2:
        this.regD = value & 0xFF;
        return;
      case 3:
        this.regE = value & 0xFF;
        return;
      case 4:
        if (this.cbDisplacementByte < 0) {
          switch (normalizedPrefix()) {
            case 0x00:
              this.regH = value & 0xFF;
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
          this.regH = value & 0xFF;
        }
        return;
      case 5:
        if (this.cbDisplacementByte < 0) {
          switch (normalizedPrefix()) {
            case 0x00:
              this.regL = value & 0xFF;
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
          this.regL = value & 0xFF;
        }
        return;
      case 6: { // (HL)
        this.tiStates += 1;
        switch (normalizedPrefix()) {
          case 0x00:
            _writemem8(ctx, _readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL)),
                (byte) value);
            return;
          case 0xDD:
            _writemem8(ctx, _readPtr(ctx, REG_IX, this.regIX) +
                (byte) (this.cbDisplacementByte < 0 ? this.lastInstructionByte :
                    this.cbDisplacementByte), (byte) value);
            return;
          case 0xFD:
            _writemem8(ctx, _readPtr(ctx, REG_IY, this.regIY) +
                (byte) (this.cbDisplacementByte < 0 ? this.lastInstructionByte :
                    this.cbDisplacementByte), (byte) value);
            return;
        }
      }
      break;
      case 7:
        this.regA = value & 0xFF;
        return;
    }
    throw new Error("unexpected P index or prefix [" + this.prefix + ':' + r + ']');
  }

  private void doLD_mNN_HL(final int ctx) {
    final int address = _readNextPcAddressedWord(ctx);
    _writemem16(ctx, address, readReg16(2));
    this.setMemPtr(address + 1);
  }

  private void doLD_mNN_A(final int ctx) {
    final int address = _readNextPcAddressedWord(ctx);
    final int a = this.regA;
    this.setMemPtr((a << 8) | ((address + 1) & 0xFF));
    this._writemem8(ctx, address, (byte) a);
  }

  private void doLD_mBC_A(final int ctx) {
    final int regValue = this.getRegisterPair(REGPAIR_BC);
    final int address = _readPtr(ctx, REGPAIR_BC, regValue);
    final int a = this.regA;
    this._writemem8(ctx, address, (byte) a);
    this.setMemPtr((a << 8) | ((address + 1) & 0xFF));
  }

  private void doLD_mDE_A(final int ctx) {
    final int regValue = this.getRegisterPair(REGPAIR_DE);
    final int address = _readPtr(ctx, REGPAIR_DE, regValue);
    final int a = this.regA;
    this._writemem8(ctx, address, (byte) a);
    this.setMemPtr((a << 8) | ((address + 1) & 0xFF));
  }

  private void doLD_HL_mem(final int ctx) {
    final int nextAddress = _readNextPcAddressedWord(ctx);
    final int value = _readmem16(ctx, nextAddress);
    this.setMemPtr(nextAddress + 1);
    writeReg16(2, value);
  }

  private void doLD_A_mem(final int ctx) {
    final int address = _readNextPcAddressedWord(ctx);
    this.regA = this._readmem8(ctx, address);
    this.setMemPtr(address + 1);
  }

  private void doINCRegPair(final int p) {
    writeReg16(p, readReg16(p) + 1);
    this.tiStates += 2;
  }

  private void doDECRegPair(final int p) {
    writeReg16(p, readReg16(p) - 1);
    this.tiStates += 2;
  }

  private void doINCReg(final int ctx, final int y) {
    final int x = readReg8(ctx, y);

    int z = x + 1;
    int c = x ^ z;

    int f = this.regF & FLAG_C;
    f |= (c & FLAG_H);
    f |= FTABLE_SZYX[z & 0xff];
    f |= FTABLE_OVERFLOW[(c >>> 7) & 0x03];

    writeReg8_UseCachedInstructionByte(ctx, y, z);
    this.internalRegQ = f;
    this.regF = f & 0xFF;
  }

  private void doDECReg(final int ctx, final int y) {
    final int x = readReg8(ctx, y);

    final int z = x - 1;
    final int c = x ^ z;

    writeReg8_UseCachedInstructionByte(ctx, y, z);

    int f = FLAG_N | (this.regF & FLAG_C);
    f |= (c & FLAG_H);
    f |= FTABLE_SZYX[z & 0xff];
    f |= FTABLE_OVERFLOW[(c >>> 7) & 0x03];

    this.internalRegQ = f;
    this.regF = f & 0xFF;
  }

  private void doLD_Reg_ByValue(final int ctx, final int y) {
    writeReg8(ctx, y, readInstrOrPrefix(ctx, false));
  }

  private void doRLCA() {
    int a = this.regA & 0xFF;
    a = (a << 1) | (a >>> 7);
    final int f = ((this.regF & FLAG_SZPV) | (a & (FLAG_XY | FLAG_C)));
    this.internalRegQ = f;
    this.regF = f & 0xFF;
    this.regA = a & 0xFF;
  }

  private void doRRCA() {
    int a = this.regA & 0xFF;
    int f = (this.regF & (FLAG_SZPV)) | (a & FLAG_C);
    a = (a >>> 1) | (a << 7);
    f |= (a & FLAG_XY);
    this.internalRegQ = f;
    this.regF = f & 0xFF;
    this.regA = a & 0xFF;
  }

  private void doRLA() {
    final int A = this.regA & 0xFF;
    final int a = A << 1;
    int f = (this.regF & FLAG_SZPV) | (a & FLAG_XY) | (A >>> 7);
    this.internalRegQ = f;
    this.regA = (a | (this.regF & FLAG_C)) & 0xFF;
    this.regF = f & 0xFF;
  }

  private void doRRA() {
    int A = this.regA & 0xFF;
    int c;
    c = A & 0x01;
    A = (A >> 1) | ((this.regF & FLAG_C) << 7);
    final int f = ((this.regF & FLAG_SZPV) | (A & FLAG_XY) | c);
    this.internalRegQ = f;
    this.regF = f & 0xFF;
    this.regA = A & 0xFF;
  }

  private void doDAA() {
    int a = this.regA & 0xFF;
    int flags = this.regF;
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
    this.regA = newa & 0xFF;
    final int f = (FTABLE_SZYXP[newa] | ((newa ^ a) & FLAG_H) | (this.regF & FLAG_N) | c);
    this.internalRegQ = f;
    this.regF = f & 0xFF;
  }

  private void doCPL() {
    int A = this.regA & 0xFF;
    A = ~A;
    this.regA = A & 0xFF;
    final int f = ((this.regF & (FLAG_SZPV | FLAG_C)) | (A & FLAG_XY) | FLAG_H | FLAG_N);
    this.regF = f & 0xFF;
    this.internalRegQ = f;
  }

  private void doSCF() {
    int a = this.regA;
    int f = this.regF;
    f = (f & FLAG_SZPV) | (((this.internalRegLastQ ^ f) | a) & FLAG_XY) | FLAG_C;
    this.regF = f & 0xFF;
    this.internalRegQ = f;
  }

  private void doCCF() {
    int a = this.regA;
    int f = this.regF & 0xFF;
    f = (f & FLAG_SZPV)
        | ((f & FLAG_C) == 0 ? FLAG_C : FLAG_H)
        | (((this.internalRegLastQ ^ f) | a) & FLAG_XY);
    this.regF = f & 0xFF;
    this.internalRegQ = f;
  }

  private void doLDRegByReg(final int ctx, final int y, final int z) {
    if (z == 6 || y == 6) {
      // process (HL),(IXd),(IYd)
      if (y == 6) {
        final int oldPrefix = this.prefix;
        this.prefix = 0;
        final int value = readReg8(ctx, z);
        this.prefix = oldPrefix;
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
      final int sp = _readPtr(ctx, REG_SP, this.getSP());
      int sp1 = sp + 1;
      final int address = _readmem8(ctx, sp) | (_readmem8(ctx, sp1++) << 8);
      this.setMemPtr(address);
      this.regPC = address;
      this.regSP = sp1 & 0xFFFF;
    }
    this.tiStates++;
  }

  private void doRET(final int ctx) {
    final int sp = _readPtr(ctx, REG_SP, this.getSP());
    int sp1 = sp + 1;
    final int address = _readmem8(ctx, sp) | (_readmem8(ctx, sp1++) << 8);
    this.regPC = address;
    this.setMemPtr(address);
    this.regSP = sp1 & 0xFFFF;
  }

  private void doPOPRegPair(final int ctx, final int p) {
    final int address = _readPtr(ctx, REG_SP, this.getSP());
    writeReg16_2(p, _readmem16(ctx, address));
    setRegister(REG_SP, address + 2);
  }

  private void doEXX() {
    int temp = this.regB;
    this.regB = this.altB;
    this.altB = temp;
    temp = this.regC;
    this.regC = this.altC;
    this.altC = temp;
    temp = this.regD;
    this.regD = this.altD;
    this.altD = temp;
    temp = this.regE;
    this.regE = this.altE;
    this.altE = temp;
    temp = this.regH;
    this.regH = this.altH;
    this.altH = temp;
    temp = this.regL;
    this.regL = this.altL;
    this.altL = temp;
  }

  private void doJP_HL(final int ctx) {
    this.regPC = readHlPtr(ctx);
  }

  private void doLD_SP_HL(final int ctx) {
    this.tiStates += 2;
    setRegister(REG_SP, readHlPtr(ctx));
  }

  private void doJP_cc(final int ctx, final int cc) {
    final int address = _readNextPcAddressedWord(ctx);
    this.setMemPtr(address);
    if (checkCondition(cc)) {
      this.regPC = address;
    }
  }

  private void doJP(final int ctx) {
    final int address = _readNextPcAddressedWord(ctx);
    this.regPC = address;
    this.setMemPtr(address);
  }

  private void doOUTnA(final int ctx) {
    final int n = readInstrOrPrefix(ctx, false);
    final int a = _portAddrFromReg(ctx, REG_A, this.regA) & 0xFF;
    final int port = (a << 8) | n;
    this.setMemPtr((a << 8) | ((port + 1) & 0xFF));
    _writeport(ctx, port, a);
  }

  private void doIN_A_n(final int ctx) {
    final int address = ((_portAddrFromReg(ctx, REG_A, this.regA) & 0xFF) << 8)
        | readInstrOrPrefix(ctx, false);
    this.setMemPtr(address + 1);
    this.regA = _readport(ctx, address) & 0xFF;
  }

  private void doEX_mSP_HL(final int ctx) {
    final int stackTop = _readPtr(ctx, REG_SP, this.getSP());
    final int hl = readReg16(2);
    final int value = _readmem8(ctx, stackTop) | (_readmem8(ctx, stackTop + 1) << 8);
    writeReg16(2, value);
    _writemem8(ctx, stackTop, (byte) hl);
    _writemem8(ctx, stackTop + 1, (byte) (hl >> 8));

    this.setMemPtr(value);

    this.tiStates += 3;
  }

  private void doEX_DE_HL() {
    int tmp = this.regD;
    this.regD = this.regH;
    this.regH = tmp;
    tmp = this.regE;
    this.regE = this.regL;
    this.regL = tmp;
  }

  private void doDI() {
    this.iff1 = false;
    this.iff2 = false;
    this.stepAllowsInterruption = false;
  }

  private void doEI() {
    this.iff1 = true;
    this.iff2 = true;
    this.stepAllowsInterruption = false;
  }

  private void doCALL(final int ctx, final int y) {
    final int address = _readNextPcAddressedWord(ctx);
    this.setMemPtr(address);
    if (checkCondition(y)) {
      _call(ctx, address);
      this.tiStates++;
    }
  }

  private void doCALL(final int ctx) {
    final int address = _readNextPcAddressedWord(ctx);
    this.setMemPtr(address);
    _call(ctx, address);
    this.tiStates++;
  }

  private void doPUSH(final int ctx, final int p) {
    final int address = _readPtr(ctx, REG_SP, this.getSP()) - 2;
    _writemem16(ctx, address, readReg16_2(p));
    setRegister(REG_SP, address);
    this.tiStates++;
  }

  private void doALU_A_Reg(final int ctx, final int op, final int reg) {
    _aluAccumulatorOp(ctx, op, reg, readReg8(ctx, reg));
  }

  private void doALU_A_n(final int ctx, final int op) {
    _aluAccumulatorOp(ctx, op, REG_UNKNOWN, readInstrOrPrefix(ctx, false));
  }

  private void _aluAccumulatorOp(final int ctx, final int op, final int regIndex, final int value) {
    final int a = this.regA & 0xFF;
    final int flagC = this.regF & FLAG_C;

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
        int z = a + value + flagC;
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
        int z = a - value - flagC;
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
        result = this.bus.postProcessAnd(this, ctx, regIndex, a, value, a & value);
        f = FTABLE_SZYXP[result] | FLAG_H;
      }
      break;
      case 5: { // XOR
        result = this.bus.postProcessXor(this, ctx, regIndex, a, value, a ^ value);
        f = FTABLE_SZYXP[result];
      }
      break;
      case 6: { // OR
        result = this.bus.postProcessOr(this, ctx, regIndex, a, value, a | value);
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
    this.regA = result & 0xFF;
    this.internalRegQ = f;
    this.regF = f & 0xFF;
  }

  private void doRST(final int ctx, final int address) {
    _call(ctx, address & 0xFF);
    this.setMemPtr(address);
    this.tiStates++;
  }

  private int doRollShift(final int ctx, final int op, final int reg) {
    int x = readReg8(ctx, reg);
    final int prevC = this.regF & FLAG_C;
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
    final int f = (FTABLE_SZYXP[x & 0xFF] | c);
    this.internalRegQ = f;
    this.regF = f & 0xFF;
    return x;
  }

  private void doROTmem_LDreg(final int ctx, final int reg, final int op) {
    writeReg8(ctx, reg, doRollShift(ctx, op, 6));
  }

  private void doBIT(final int ctx, final int bit, final int reg) {
    final int val = readReg8(ctx, reg);
    final int result = val & (1 << bit);

    final int h;
    if (reg == 6) {
      this.tiStates++;
      // (HL),(IX),(IY)
      h = this.memptr >> 8;
    } else {
      h = val;
    }

    int f = this.regF;
    f = (f & FLAG_C) | FLAG_H | (h & FLAG_XY);

    // NB! Flag P/V is UNKNOWN in Z80 manual!
    if (result == 0) {
      f |= FLAG_PV | FLAG_Z;
    }

    // NB! in Z80 manual written that S flag in UNKNOWN
    if (bit == 7 && (val & 0x80) != 0) {
      f |= FLAG_S;
    }
    this.internalRegQ = f;
    this.regF = f & 0xFF;
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
    final int port = _portAddrFromReg(ctx, REGPAIR_BC, this.getRegisterPair(REGPAIR_BC));
    this.setMemPtr(port + 1);
    final int value =
        _readport(ctx, port);
    final int f = (FTABLE_SZYXP[value] | (this.regF & FLAG_C));
    this.internalRegQ = f;
    this.regF = f & 0xFF;
  }

  private void doIN_C(final int ctx, final int y) {
    final int port = _portAddrFromReg(ctx, REGPAIR_BC, getRegisterPair(REGPAIR_BC));
    this.setMemPtr(port + 1);
    final int value =
        _readport(ctx, port) & 0xFF;
    writeReg8(ctx, y, value);

    final int f = (FTABLE_SZYXP[value] | (this.regF & FLAG_C));
    this.internalRegQ = f;
    this.regF = f & 0xFF;
  }

  private void doOUT_C(final int ctx) {
    final int port = _portAddrFromReg(ctx, REGPAIR_BC, this.getRegisterPair(REGPAIR_BC));
    this.setMemPtr(port + 1);
    _writeport(ctx, port, 0);
  }

  private void doOUT_C(final int ctx, final int y) {
    final int port = _portAddrFromReg(ctx, REGPAIR_BC, this.getRegisterPair(REGPAIR_BC));
    _writeport(ctx, port, readReg8(ctx, y));
    this.setMemPtr(port + 1);
  }

  private void doLD_mNN_RegP(final int ctx, final int p) {
    final int address = _readNextPcAddressedWord(ctx);
    _writemem16(ctx, address, readReg16(p));
    this.setMemPtr(address + 1);
  }

  private void doLD_RegP_mNN(final int ctx, final int p) {
    final int addressSource = _readNextPcAddressedWord(ctx);
    final int value = _readmem16(ctx, addressSource);
    writeReg16(p, value);
    this.setMemPtr(addressSource + 1);
  }

  private void doNEG() {
    int a = this.regA & 0xFF;
    int z = -a;
    int c = a ^ z;
    int f = FLAG_N | (c & FLAG_H);
    z &= 0xFF;
    f |= FTABLE_SZYX[z];
    c &= 0x0180;
    f |= FTABLE_OVERFLOW[c >>> 7];
    f |= c >>> (8 - FLAG_C_SHIFT);
    this.internalRegQ = f;
    this.regA = z & 0xFF;
    this.regF = f & 0xFF;
  }

  private void doRETI(final int ctx) {
    doRET(ctx);
    this.bus.onRETI(this, ctx);
  }

  private void doRETN(final int ctx) {
    this.iff1 = this.iff2;
    this.nmiTrigger = false;
    doRET(ctx);
  }

  private void doIM(final int y) {
    switch (y) {
      case 4:
      case 0:
      case 5:
      case 1:
        this.im = 0;
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
    this.regI = this.regA;
    this.tiStates++;
  }

  private void doLD_R_A() {
    this.regR = this.regA;
    this.tiStates++;
  }

  private void doLD_A_I(final boolean signalIntActive) {
    final int value = this.regI;
    this.regA = value;

    final int f = (FTABLE_SZYX[value]
        | (this.iff2 && !(signalIntActive || this.nmiTrigger) ? FLAG_PV : 0)
        | (this.regF & FLAG_C));
    this.internalRegQ = f;
    this.regF = f & 0xFF;

    this.tiStates++;
  }

  private void doLD_A_R(final boolean signalIntActive) {
    final int value = this.regR;
    this.regA = value;

    final int f = (FTABLE_SZYX[value]
        | (this.iff2 && !(signalIntActive || this.nmiTrigger) ? FLAG_PV : 0)
        | (this.regF & FLAG_C));
    this.internalRegQ = f;
    this.regF = f & 0xFF;

    this.tiStates++;
  }

  private void doRRD(final int ctx) {
    int hl = _readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL));
    final int a = this.regA & 0xFF;
    int x = _readmem8(ctx, hl);
    int y = (a & 0xf0) << 8;
    y |= ((x & 0x0f) << 8) | ((a & 0x0f) << 4) | (x >> 4);
    _writemem8(ctx, hl, (byte) y);
    y >>>= 8;
    this.regA = y & 0xFF;
    final int f = (FTABLE_SZYXP[y] | (this.regF & FLAG_C));
    this.internalRegQ = f;
    this.regF = f & 0xFF;

    this.setMemPtr(hl + 1);

    this.tiStates += 4;
  }

  private void doRLD(final int ctx) {
    int hl = _readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL));
    final int A = this.regA & 0xFF;
    int x = _readmem8(ctx, hl);
    int y = (A & 0xf0) << 8;
    y |= (x << 4) | (A & 0x0f);
    _writemem8(ctx, hl, (byte) y);
    y >>>= 8;
    this.regA = y & 0xFF;
    final int f = (FTABLE_SZYXP[y] | (this.regF & FLAG_C));
    this.regF = f & 0xFF;
    this.internalRegQ = f;

    this.setMemPtr(hl + 1);

    this.tiStates += 4;
  }

  private boolean doBLI(final int ctx, final int y, final int z, final boolean incomingInterrupt) {
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
            doINI_IND(ctx, true);
            break;
          case 3:
            doOUTI_OUTD(ctx, true);
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
            doINI_IND(ctx, false);
            break;
          case 3:
            doOUTI_OUTD(ctx, false);
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
    final int regValue = this.getRegisterPair(REGPAIR_BC);
    final int address = _readPtr(ctx, REGPAIR_BC, regValue);
    this.regA = this._readmem8(ctx, address);
    this.setMemPtr(regValue + 1);
  }

  private boolean doLDIR(final int ctx) {
    doLDI(ctx);
    boolean loopNonCompleted = true;
    if ((this.regF & FLAG_PV) != 0) {
      final int address = (this.regPC - 2) & 0xFFFF;
      this.setMemPtr(address + 1);
      this.regPC = address;
      this.tiStates += 5;

      updateBlockOperationFlagXY();
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void updateBlockOperationFlagXY() {
    this.regF = ((this.regF & ~FLAG_XY) | ((this.regPC >> 8) & FLAG_XY)) & 0xFF;
  }

  private void doLD_A_mDE(final int ctx) {
    final int regValue = this.getRegisterPair(REGPAIR_DE);
    final int address = _readPtr(ctx, REGPAIR_DE, regValue);
    this.regA = this._readmem8(ctx, address);
    this.setMemPtr(regValue + 1);
  }

  private boolean doCPIR(final int ctx) {
    doCPI(ctx);
    boolean loopNonCompleted = true;
    final int flags = this.regF;

    this.internalRegQ = flags;

    if ((flags & (FLAG_Z | FLAG_PV)) == FLAG_PV) {
      final int address = (this.regPC - 2) & 0xFFFF;
      this.setMemPtr(address + 1);
      this.regPC = address;
      this.tiStates += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private int doINI_IND(final int ctx, final boolean ini) {
    final int delta = ini ? 1 : -1;

    int hl = _readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL));
    final int bc = _portAddrFromReg(ctx, REGPAIR_BC, getRegisterPair(REGPAIR_BC));
    final int data = _readport(ctx, bc);
    _writemem8(ctx, hl, (byte) data);
    hl += delta;
    final int b = ((bc >>> 8) - 1) & 0xFF;
    this.regB = b & 0xFF;
    setRegisterPair(REGPAIR_HL, hl);

    final int initemp2 = (data + (bc & 0xFF) + delta) & 0xff;
    final int f = ((data & 0x80) == 0 ? 0 : FLAG_N)
        | (initemp2 < data ? FLAG_HC : 0)
        | (FTABLE_SZYXP[(initemp2 & 0x07) ^ b] & FLAG_PV)
        | FTABLE_SZYX[b];
    this.regF = f & 0xFF;
    this.internalRegQ = f;

    this.setMemPtr(bc + delta);

    this.tiStates++;

    return data;
  }

  private void updateFlags_INxR_OTxR(final int data) {
    final int regB = this.regB & 0xFF;
    int flagP = this.regF & FLAG_PV;
    int flagH = this.regF & FLAG_H;

    final int regF = this.regF & 0xFF;

    if ((regF & FLAG_C) == 0) {
      flagP = flagP ^ (FTABLE_SZYXP[regB & 0x07] & FLAG_PV) ^ FLAG_PV;
    } else {
      if ((data & 0x80) == 0) {
        flagP = flagP ^ (FTABLE_SZYXP[(regB + 1) & 0x07] & FLAG_PV) ^ FLAG_PV;
        flagH = (regB & 0x0F) == 0x0F ? FLAG_H : 0;
      } else {
        flagP = flagP ^ (FTABLE_SZYXP[(regB - 1) & 0x07] & FLAG_PV) ^ FLAG_PV;
        flagH = (regB & 0x0F) == 0x00 ? FLAG_H : 0;
      }
    }

    this.regF = ((regF & ~(FLAG_PV | FLAG_H)) | flagP | flagH) & 0xFF;
  }

  private boolean doINIR(final int ctx) {
    final int data = doINI_IND(ctx, true);
    boolean loopNonCompleted = true;
    if ((this.regF & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.setMemPtr((0x100 + (this.regC & 0xFF) + 1) & 0xFFFF);
      this.tiStates += 5;
      updateBlockOperationFlagXY();
      updateFlags_INxR_OTxR(data);
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private boolean doINDR(final int ctx) {
    final int data = doINI_IND(ctx, false);
    boolean loopNonCompleted = true;
    if ((this.regF & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.setMemPtr((0x100 + (this.regC & 0xFF) - 1) & 0xFFFF);
      this.tiStates += 5;
      updateBlockOperationFlagXY();
      updateFlags_INxR_OTxR(data);
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doLDI(final int ctx) {
    int hl = _readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL));
    int de = _readPtr(ctx, REGPAIR_DE, this.getRegisterPair(REGPAIR_DE));

    int value = _readmem8(ctx, hl++);

    _writemem8(ctx, de++, (byte) value);
    setRegisterPair(REGPAIR_HL, hl);
    setRegisterPair(REGPAIR_DE, de);

    final int bc =
        (_readSpecRegPairValue(ctx, REGPAIR_BC, getRegisterPair(REGPAIR_BC)) - 1) & 0xFFFF;
    setRegisterPair(REGPAIR_BC, bc);

    int f = this.regF & FLAG_SZC;
    f |= bc == 0 ? 0 : FLAG_PV;
    value += this.regA & 0xFF;
    f |= value & FLAG_X;
    f |= (value << (FLAG_Y_SHIFT - 1)) & FLAG_Y;
    this.regF = f & 0xFF;
    this.internalRegQ = f;

    this.tiStates += 2;
  }

  private boolean doOTIR(final int ctx) {
    final int data = doOUTI_OUTD(ctx, true);
    boolean loopNonCompleted = true;
    if ((this.regF & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.tiStates += 5;
      updateBlockOperationFlagXY();
      updateFlags_INxR_OTxR(data);
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private void doCPI(final int ctx) {
    int hl = _readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL));
    int n = _readmem8(ctx, hl++);

    final int a = this.regA;
    final int z = a - n;
    setRegisterPair(REGPAIR_HL, hl);
    final int bc = _readSpecRegPairValue(ctx, REGPAIR_BC, getRegisterPair(REGPAIR_BC)) - 1;
    setRegisterPair(REGPAIR_BC, bc);

    int f = (a ^ n ^ z) & FLAG_H;
    n = z - (f >>> FLAG_H_SHIFT);
    f |= (n << (FLAG_Y_SHIFT - 1)) & FLAG_Y;
    f |= n & FLAG_X;
    f |= FTABLE_SZYX[z & 0xff] & FLAG_SZ;
    f |= bc != 0 ? FLAG_PV : 0;
    f |= (f | FLAG_N | (this.regF & FLAG_C));
    this.regF = f & 0xFF;

    this.internalRegQ = f;

    this.setMemPtr(this.getMemPtr() + 1);

    this.tiStates += 5;
  }

  private boolean doOTDR(final int ctx) {
    final int data = doOUTI_OUTD(ctx, false);

    boolean loopNonCompleted = true;
    if ((this.regF & FLAG_Z) == 0) {
      this.regPC = (this.regPC - 2) & 0xFFFF;
      this.tiStates += 5;
      updateBlockOperationFlagXY();
      updateFlags_INxR_OTxR(data);
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private boolean doLDDR(final int ctx) {
    doLDD(ctx);
    boolean loopNonCompleted = true;
    if (this.getRegisterPair(REGPAIR_BC) != 0) {
      final int address = (this.regPC - 2) & 0xFFFF;
      this.regPC = address;
      this.setMemPtr(address + 1);
      this.tiStates += 5;

      updateBlockOperationFlagXY();
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  private int doOUTI_OUTD(final int ctx, final boolean inc) {
    final int delta = inc ? 1 : -1;

    final int bc = _portAddrFromReg(ctx, REGPAIR_BC, this.getRegisterPair(REGPAIR_BC));
    int hl = _readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL));
    final int data = _readmem8(ctx, hl);
    final int b = ((bc >>> 8) - 1) & 0xFF;
    _writeport(ctx, (b << 8) | (bc & 0xFF), data);
    this.regB = b & 0xFF;

    hl += delta;
    setRegisterPair(REGPAIR_HL, hl);

    final int outitemp2 = (data + (hl & 0xFF)) & 0xFF;
    final int f = ((data & 0x80) == 0 ? 0 : FLAG_N)
        | (outitemp2 < data ? FLAG_HC : 0)
        | (FTABLE_SZYXP[(outitemp2 & 0x07) ^ b] & FLAG_PV)
        | FTABLE_SZYX[b];
    this.internalRegQ = f;
    this.regF = f & 0xFF;

    this.setMemPtr(((b << 8) | (bc & 0xFF)) + delta);

    this.tiStates++;

    return data;
  }

  private boolean doCPDR(final int ctx) {
    doCPD(ctx);
    boolean loopNonCompleted = true;
    final int flags = this.regF;

    this.internalRegQ = flags;

    if ((flags & (FLAG_Z | FLAG_PV)) == FLAG_PV) {
      final int address = (this.regPC - 2) & 0xFFFF;
      this.regPC = address;
      this.setMemPtr(address + 1);
      this.tiStates += 5;
    } else {
      loopNonCompleted = false;
    }
    return loopNonCompleted;
  }

  public String getStateAsString() {
    String result = "PC=" + Utils.toHex(this.getRegister(Z80.REG_PC)) + ',' +
        "SP=" + Utils.toHex(this.getRegister(Z80.REG_SP)) + ',' +
        "IX=" + Utils.toHex(this.getRegister(Z80.REG_IX)) + ',' +
        "IY=" + Utils.toHex(this.getRegister(Z80.REG_IY)) + ',' +
        "AF=" + Utils.toHex(this.getRegisterPair(Z80.REGPAIR_AF)) + ',' +
        "BC=" + Utils.toHex(this.getRegisterPair(Z80.REGPAIR_BC)) + ',' +
        "DE=" + Utils.toHex(this.getRegisterPair(Z80.REGPAIR_DE)) + ',' +
        "HL=" + Utils.toHex(this.getRegisterPair(Z80.REGPAIR_HL)) + ',' +
        "AF'=" + Utils.toHex(this.getRegisterPair(Z80.REGPAIR_AF, true)) +
        ',' +
        "BC'=" + Utils.toHex(this.getRegisterPair(Z80.REGPAIR_BC, true)) +
        ',' +
        "DE'=" + Utils.toHex(this.getRegisterPair(Z80.REGPAIR_DE, true)) +
        ',' +
        "HL'=" + Utils.toHex(this.getRegisterPair(Z80.REGPAIR_HL, true)) +
        ',' +
        "R=" + Utils.toHex(this.getRegister(Z80.REG_R)) + ',' +
        "I=" + Utils.toHex(this.getRegister(Z80.REG_I)) + ',' +
        "IM=" + this.getIM() + ',' +
        "IFF1=" + this.iff1 + ',' +
        "IFF2=" + this.iff2 + ',' +
        "M1ExeByte=" + this.lastM1InstructionByte + ',' +
        "lastExeByte=" + this.lastInstructionByte;
    return result;
  }

  public void doReset() {
    this._reset(0);
    this._reset(1);
    this._reset(2);
  }

  public boolean compareState(final Z80 other, final boolean compareExe) {
    if (this.regA != other.regA || this.regF != other.regF
        || this.regB != other.regB || this.regC != other.regC
        || this.regD != other.regD || this.regE != other.regE
        || this.regH != other.regH || this.regL != other.regL) {
      return false;
    }
    if (this.altA != other.altA || this.altF != other.altF
        || this.altB != other.altB || this.altC != other.altC
        || this.altD != other.altD || this.altE != other.altE
        || this.altH != other.altH || this.altL != other.altL) {
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
    if (compareExe && (this.lastM1InstructionByte != other.lastM1InstructionByte ||
        this.lastInstructionByte != other.lastInstructionByte)) {
      return false;
    }

    return this.regSP == other.regSP;
  }

  private void doLDD(final int ctx) {
    int hl = _readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL));
    int de = _readPtr(ctx, REGPAIR_DE, this.getRegisterPair(REGPAIR_DE));

    int x = _readmem8(ctx, hl--);

    _writemem8(ctx, de--, (byte) x);
    setRegisterPair(REGPAIR_HL, hl);
    setRegisterPair(REGPAIR_DE, de);

    final int bc =
        (_readSpecRegPairValue(ctx, REGPAIR_BC, getRegisterPair(REGPAIR_BC)) - 1) & 0xFFFF;
    setRegisterPair(REGPAIR_BC, bc);

    int f = this.regF & FLAG_SZC;
    f |= bc != 0 ? FLAG_PV : 0;
    x += this.regA & 0xFF;
    f |= x & FLAG_X;
    f |= (x << (FLAG_Y_SHIFT - 1)) & FLAG_Y;
    this.regF = f & 0xFF;
    this.internalRegQ = f;

    this.tiStates += 2;
  }

  private void doCPD(final int ctx) {
    int hl = _readPtr(ctx, REGPAIR_HL, this.getRegisterPair(REGPAIR_HL));
    int n = _readmem8(ctx, hl--);
    final int a = this.regA;
    final int z = a - n;
    setRegisterPair(REGPAIR_HL, hl);
    final int bc = _readSpecRegPairValue(ctx, REGPAIR_BC, getRegisterPair(REGPAIR_BC)) - 1;
    setRegisterPair(REGPAIR_BC, bc);

    int f = (a ^ n ^ z) & FLAG_H;
    n = z - (f >>> FLAG_H_SHIFT);
    f |= (n << (FLAG_Y_SHIFT - 1)) & FLAG_Y;
    f |= n & FLAG_X;
    f |= FTABLE_SZYX[z & 0xff] & FLAG_SZ;
    f |= bc != 0 ? FLAG_PV : 0;
    f |= FLAG_N | (this.regF & FLAG_C);
    this.internalRegQ = f;
    this.regF = f & 0xFF;

    this.setMemPtr(this.getMemPtr() - 1);

    this.tiStates += 5;
  }

}