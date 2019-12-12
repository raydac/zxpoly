/*
 * Copyright (C) 2019 Igor Maznitsa
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


import java.util.Arrays;
import org.junit.Before;

public abstract class AbstractZ80Test {

  protected final byte[] memory = new byte[0x10000];

  @Before
  public void beforeTest() {
    Arrays.fill(this.memory, (byte) 0);
  }

  public void fillMemory(int startAddress, final int... bytes) {
    for (int i : bytes) {
      this.memory[startAddress++] = (byte) i;
    }
  }

  public void assertTacts(final Z80 cpu, final long tacts) {
    assertEquals("Tacts must be " + tacts, tacts, cpu.getMachineCycles() - 3);
  }

  public void assertMemoryEmpty(final int since) {
    for (int i = since; i < 0x10000; i++) {
      if (this.memory[i] != 0) {
        fail("Memory is not empty at 0x" + Integer.toHexString(i).toUpperCase());
      }
    }
  }

  public void assertMemoryWord(final int address, final int value) {
    final int low = this.memory[address & 0xFFFF] & 0xFF;
    final int high = this.memory[(address + 1) & 0xFFFF] & 0xFF;
    assertEquals("Word must be " + value + " at " + address, value & 0xFFFF, (high << 8) | low);
  }

  public void assertMemory(final int address, final int value) {
    assertEquals("The Memory value at " + address + " must be " + value, value, this.memory[address] & 0xFF);
  }

  public void assertMemory(final int address, final int... value) {
    for (int i = 0; i < value.length; i++) {
      assertMemory(address + i, value[i]);
    }
  }

  public void assertFlagsNotChanged(final Z80State state, final Z80 cpu) {
    assertEquals("F must be the same", state.F, cpu.getRegister(Z80.REG_F, false));
    assertEquals("F' must be the same", state.altF, cpu.getRegister(Z80.REG_F, true));
  }

  public Z80 executeCommand(final int... code) {
    return this.executeCommand(null, code);
  }

  public Z80 executeRepeatingBlockCommand(final Z80State state, final TestBus testBus, final int... code) {
    for (int i = 0; i < code.length; i++) {
      testBus.writeMemory(null, 111, i, (byte) code[i]);
    }

    final Z80 cpu = new Z80(testBus);
    if (state != null) {
      state.set(cpu);
    }

    final int pc = cpu.getRegister(Z80.REG_PC);

    do {
      cpu.nextInstruction(111, false, false, false);
    }
    while (cpu.getRegister(Z80.REG_PC) == pc);

    assertEquals("PC must be at " + code.length, code.length, cpu.getRegister(Z80.REG_PC));

    return cpu;
  }

  public Z80 executeRepeatingBlockCommand(final Z80State state, final int... code) {
    for (int i = 0; i < code.length; i++) {
      this.memory[i] = (byte) code[i];
    }

    final Z80CPUBus bus = new Z80CPUBus() {

      @Override
      public byte readMemoryForReg(Z80 z80, int ctx, int reg, int address) {
        return this.readMemory(z80, ctx, address, false, false);
      }

      @Override
      public byte readMemory(Z80 cpu, int ctx, int address, boolean m1, boolean cmdOrPrefix) {
        return memory[address];
      }

      @Override
      public void writeMemory(Z80 cpu, int ctx, int address, byte data) {
        memory[address] = data;
      }

      @Override
      public byte readPort(Z80 cpu, int ctx, int port) {
        fail("Unexpected port reading");
        return -1;
      }

      @Override
      public void writePort(Z80 cpu, int ctx, int port, byte data) {
        fail("Unexpected port writing");
      }

      @Override
      public byte onCPURequestDataLines(Z80 cpu, int ctx) {
        fail("Unsupported here");
        return 0;
      }

      @Override
      public void onRETI(Z80 cpu, int ctx) {
        fail("Unsupported here");
      }
    };

    final Z80 cpu = new Z80(bus);
    if (state != null) {
      state.set(cpu);
    }

    final int pc = cpu.getRegister(Z80.REG_PC);

    do {
      cpu.nextInstruction(111, false, false, false);
    }
    while (cpu.getRegister(Z80.REG_PC) == pc);

    assertEquals("PC must be at " + code.length, code.length, cpu.getRegister(Z80.REG_PC));

    return cpu;
  }

  public Z80 executeCommand(final Z80State state, final int... code) {
    return this.executeCommand(state, true, code);
  }

  public Z80 executeCommand(final Z80State state, final boolean checkPC, final int... code) {

    for (int i = 0; i < code.length; i++) {
      this.memory[i] = (byte) code[i];
    }

    final Z80CPUBus bus = new Z80CPUBus() {

      @Override
      public byte readMemoryForReg(Z80 z80, int ctx, int reg, int address) {
        return this.readMemory(z80, ctx, address, false, false);
      }

      @Override
      public byte readMemory(Z80 cpu, int ctx, int address, boolean m1, boolean cmdOrPrefix) {
        return memory[address];
      }

      @Override
      public void writeMemory(Z80 cpu, int ctx, int address, byte data) {
        memory[address] = data;
      }

      @Override
      public byte readPort(Z80 cpu, int ctx, int port) {
        fail("Unexpected port reading");
        return -1;
      }

      @Override
      public void writePort(Z80 cpu, int ctx, int port, byte data) {
        fail("Unexpected port writing");
      }

      @Override
      public byte onCPURequestDataLines(Z80 cpu, int ctx) {
        fail("Unsupported here");
        return 0;
      }

      @Override
      public void onRETI(Z80 cpu, int ctx) {
        fail("Unsupported here");
      }

    };

    final Z80 cpu = new Z80(bus);
    if (state != null) {
      state.set(cpu);
    }

    cpu.nextInstruction(111, false, false, false);

    if (checkPC) {
      assertEquals("PC must be at " + code.length, code.length, cpu.getRegister(Z80.REG_PC));
    }

    return cpu;
  }

  public void assertFlagsExcludeReserved(final int etalon, final int valueToCheck) {
    final int mask = (~(Z80.FLAG_X | Z80.FLAG_Y)) & 0xFF;
    assertEquals("Flags must be equals", etalon & mask, valueToCheck & mask);
  }

  public static class Z80State {

    public int A, F, B, C, D, E, H, L;
    public int WZ;
    public int altWZ;
    public int altA, altF, altB, altC, altD, altE, altH, altL;
    public int IX, IY, SP, PC;
    public int I, R;
    public boolean iff1, iff2;
    public int lastM1InstructionByte, lastInstructionByte;

    public Z80State() {
      this.PC = 0;
      this.SP = 0xFFFF;
    }

    public Z80State(final Z80 cpu) {
      this.iff1 = cpu.isIFF1();
      this.iff2 = cpu.isIFF2();

      this.lastInstructionByte = cpu.getLastInstructionByte();
      this.lastM1InstructionByte = cpu.getLastM1InstructionByte();

      this.A = cpu.getRegister(Z80.REG_A, false);
      this.F = cpu.getRegister(Z80.REG_F, false);
      this.B = cpu.getRegister(Z80.REG_B, false);
      this.C = cpu.getRegister(Z80.REG_C, false);
      this.D = cpu.getRegister(Z80.REG_D, false);
      this.E = cpu.getRegister(Z80.REG_E, false);
      this.H = cpu.getRegister(Z80.REG_H, false);
      this.L = cpu.getRegister(Z80.REG_L, false);

      this.WZ = cpu.getWZ(false);
      this.altWZ = cpu.getWZ(true);

      this.altA = cpu.getRegister(Z80.REG_A, true);
      this.altF = cpu.getRegister(Z80.REG_F, true);
      this.altB = cpu.getRegister(Z80.REG_B, true);
      this.altC = cpu.getRegister(Z80.REG_C, true);
      this.altD = cpu.getRegister(Z80.REG_D, true);
      this.altE = cpu.getRegister(Z80.REG_E, true);
      this.altH = cpu.getRegister(Z80.REG_H, true);
      this.altL = cpu.getRegister(Z80.REG_L, true);

      this.IX = cpu.getRegister(Z80.REG_IX);
      this.IY = cpu.getRegister(Z80.REG_IY);
      this.SP = cpu.getRegister(Z80.REG_SP);
      this.PC = cpu.getRegister(Z80.REG_PC);
      this.I = cpu.getRegister(Z80.REG_I);
      this.R = cpu.getRegister(Z80.REG_R);
    }

    public void set(final Z80 cpu) {
      cpu.setIFF(this.iff1, this.iff2);

      cpu.setRegister(Z80.REG_A, this.A, false);
      cpu.setRegister(Z80.REG_F, this.F, false);
      cpu.setRegister(Z80.REG_B, this.B, false);
      cpu.setRegister(Z80.REG_C, this.C, false);
      cpu.setRegister(Z80.REG_D, this.D, false);
      cpu.setRegister(Z80.REG_E, this.E, false);
      cpu.setRegister(Z80.REG_H, this.H, false);
      cpu.setRegister(Z80.REG_L, this.L, false);

      cpu.setWZ(this.WZ, false);
      cpu.setWZ(this.altWZ, true);

      cpu.setRegister(Z80.REG_A, this.altA, true);
      cpu.setRegister(Z80.REG_F, this.altF, true);
      cpu.setRegister(Z80.REG_B, this.altB, true);
      cpu.setRegister(Z80.REG_C, this.altC, true);
      cpu.setRegister(Z80.REG_D, this.altD, true);
      cpu.setRegister(Z80.REG_E, this.altE, true);
      cpu.setRegister(Z80.REG_H, this.altH, true);
      cpu.setRegister(Z80.REG_L, this.altL, true);

      cpu.setRegister(Z80.REG_IX, this.IX);
      cpu.setRegister(Z80.REG_IY, this.IY);
      cpu.setRegister(Z80.REG_SP, this.SP);
      cpu.setRegister(Z80.REG_PC, this.PC);
      cpu.setRegister(Z80.REG_I, this.I);
      cpu.setRegister(Z80.REG_R, this.R);
    }

    @Override
    public int hashCode() {
      return super.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      if (obj instanceof Z80State) {
        final Z80State that = (Z80State) obj;

        assertEquals("Trigger IFF1", this.iff1, that.iff1);
        assertEquals("Trigger IFF2", this.iff2, that.iff2);

        assertEquals("Register A", this.A, that.A);
        assertEquals("Register F", this.F, that.F);
        assertEquals("Register B", this.B, that.B);
        assertEquals("Register C", this.C, that.C);
        assertEquals("Register D", this.D, that.D);
        assertEquals("Register E", this.E, that.E);
        assertEquals("Register H", this.H, that.H);
        assertEquals("Register L", this.L, that.L);
        assertEquals("Register WZ", this.WZ, that.WZ);

        assertEquals("Register A'", this.altA, that.altA);
        assertEquals("Register F'", this.altF, that.altF);
        assertEquals("Register B'", this.altB, that.altB);
        assertEquals("Register C'", this.altC, that.altC);
        assertEquals("Register D'", this.altD, that.altD);
        assertEquals("Register E'", this.altE, that.altE);
        assertEquals("Register H'", this.altH, that.altH);
        assertEquals("Register L'", this.altL, that.altL);
        assertEquals("Register WZ'", this.altWZ, that.altWZ);

        assertEquals("Register IX", this.IX, that.IX);
        assertEquals("Register IY", this.IY, that.IY);
        assertEquals("Register SP", this.SP, that.SP);
        assertEquals("Register PC", this.PC, that.PC);
        assertEquals("Register I", this.I, that.I);
        assertEquals("Register R", this.R, that.R);

        return true;
      }
      return false;
    }
  }

}
