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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


import org.junit.Test;

public class Z80Test extends AbstractZ80Test {

  @Test
  public void testReset_Nop() {
    final Z80 cpu = new Z80(new Z80CPUBus() {

      @Override
      public int readRegPortAddr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
      }

      @Override
      public byte readMemory(Z80 cpu, int ctx, int address, boolean m1, boolean cmdOrPrefix) {
        return 0;
      }

      @Override
      public void writeMemory(Z80 cpu, int ctx, int address, byte data) {
      }

      @Override
      public int readPtr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
      }

      @Override
      public int readSpecRegValue(Z80 cpu, int ctx, int reg, int origValue) {
        return origValue;
      }

      @Override
      public int readSpecRegPairValue(Z80 cpu, int ctx, int regPair, int origValue) {
        return origValue;
      }

      @Override
      public byte readPort(Z80 cpu, int ctx, int port) {
        return 0;
      }

      @Override
      public void writePort(Z80 cpu, int ctx, int port, byte data) {
      }

      @Override
      public byte onCPURequestDataLines(Z80 cpu, int ctx) {
        return (byte) 0xFF;
      }

      @Override
      public void onRETI(Z80 cpu, int ctx) {
      }
    });

    for (int m = 0; m < 0x0020; m++) {
      cpu.nextInstruction(111, false, false, false);
    }
    assertEquals(0x0020, cpu.getRegister(Z80.REG_PC));

    cpu.setRegister(Z80.REG_SP, 0x1234);

    assertEquals(0x0020, cpu.getRegister(Z80.REG_PC));
    cpu.step(111, ~Z80.SIGNAL_IN_nRESET);
    cpu.step(111, ~Z80.SIGNAL_IN_nRESET);
    cpu.step(111, ~Z80.SIGNAL_IN_nRESET);
    assertEquals(0x0000, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x0000, cpu.getRegister(Z80.REG_SP));

    cpu.step(111, Z80.SIGNAL_IN_ALL_INACTIVE);
    assertEquals(0x0001, cpu.getRegister(Z80.REG_PC));

    cpu.step(111, ~Z80.SIGNAL_IN_nRESET);
    cpu.step(111, ~Z80.SIGNAL_IN_nRESET);
    cpu.step(111, ~Z80.SIGNAL_IN_nRESET);
    assertEquals(0x0000, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x0000, cpu.getRegister(Z80.REG_SP));
  }

  @Test
  public void testCompareStateWithClonedCPU() {
    final Z80State state = new Z80State();
    state.A = 12;
    state.C = 15;
    final Z80 cpu = executeCommand(state, 0x79);
    cpu.setIM(2);
    cpu.setIFF(false, true);
    assertTrue(cpu.compareState(new Z80(cpu), false));
  }

  @Test
  public void testCommand_LD_A_C() {
    final Z80State state = new Z80State();
    state.A = 12;
    state.C = 15;
    final Z80 cpu = executeCommand(state, 0x79);
    assertEquals(15, cpu.getRegister(Z80.REG_A));
    assertEquals(15, cpu.getRegister(Z80.REG_C));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_LD_A_H() {
    final Z80State state = new Z80State();
    state.A = 12;
    state.H = 15;
    final Z80 cpu = executeCommand(state, 0x7C);
    assertEquals(15, cpu.getRegister(Z80.REG_A));
    assertEquals(15, cpu.getRegister(Z80.REG_H));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_LD_A_L() {
    final Z80State state = new Z80State();
    state.A = 12;
    state.L = 15;
    final Z80 cpu = executeCommand(state, 0x7D);
    assertEquals(15, cpu.getRegister(Z80.REG_A));
    assertEquals(15, cpu.getRegister(Z80.REG_L));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_LD_A_IXh() {
    final Z80State state = new Z80State();
    state.A = 0xFF;
    state.IX = 0x1234;
    final Z80 cpu = executeCommand(state, 0xDD, 0x7C);
    assertEquals(0x12, cpu.getRegister(Z80.REG_A));
    assertEquals(0x1234, cpu.getRegister(Z80.REG_IX));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_A_IXl() {
    final Z80State state = new Z80State();
    state.A = 0xFF;
    state.IX = 0x1234;
    final Z80 cpu = executeCommand(state, 0xDD, 0x7D);
    assertEquals(0x34, cpu.getRegister(Z80.REG_A));
    assertEquals(0x1234, cpu.getRegister(Z80.REG_IX));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_A_IYh() {
    final Z80State state = new Z80State();
    state.A = 0xFF;
    state.IY = 0x1234;
    final Z80 cpu = executeCommand(state, 0xFD, 0x7C);
    assertEquals(0x12, cpu.getRegister(Z80.REG_A));
    assertEquals(0x1234, cpu.getRegister(Z80.REG_IY));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_A_IYl() {
    final Z80State state = new Z80State();
    state.A = 0xFF;
    state.IY = 0x1234;
    final Z80 cpu = executeCommand(state, 0xFD, 0x7D);
    assertEquals(0x34, cpu.getRegister(Z80.REG_A));
    assertEquals(0x1234, cpu.getRegister(Z80.REG_IY));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_A_n() {
    final Z80State state = new Z80State();
    state.A = 12;
    final Z80 cpu = executeCommand(state, 0x3E, 0x67);
    assertEquals(0x67, cpu.getRegister(Z80.REG_A));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_LD_C_D() {
    final Z80State state = new Z80State();
    state.A = 12;
    state.C = 15;
    final Z80 cpu = executeCommand(state, 0x4f);
    assertEquals(12, cpu.getRegister(Z80.REG_A));
    assertEquals(12, cpu.getRegister(Z80.REG_C));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_LD_A_mHL() {
    final Z80State state = new Z80State();
    state.A = 12;
    state.H = 0x12;
    state.L = 0x34;
    this.memory[0x1234] = 0x73;
    final Z80 cpu = executeCommand(state, 0x7E);
    assertEquals(0x73, cpu.getRegister(Z80.REG_A));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_LD_mHL_A() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.H = 0x12;
    state.L = 0x34;
    this.memory[0x1234] = 0x33;
    final Z80 cpu = executeCommand(state, 0x77);
    assertEquals(0x48, cpu.getRegister(Z80.REG_A));
    assertEquals(0x48, this.memory[0x1234]);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_LD_A_mIXd() {
    final Z80State state = new Z80State();
    state.A = 12;
    state.IX = 0x1234;
    this.memory[0x1232] = 0x73;
    final Z80 cpu = executeCommand(state, 0xDD, 0x7E, 0xFE);
    assertEquals(0x73, cpu.getRegister(Z80.REG_A));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_LD_mIXd_A() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.IX = 0x1234;
    this.memory[0x1239] = 0x33;
    final Z80 cpu = executeCommand(state, 0xDD, 0x77, 5);
    assertEquals(0x48, cpu.getRegister(Z80.REG_A));
    assertEquals(0x48, this.memory[0x1239]);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_LD_A_mIYd() {
    final Z80State state = new Z80State();
    state.A = 12;
    state.IY = 0x1234;
    this.memory[0x1239] = 0x73;
    final Z80 cpu = executeCommand(state, 0xFD, 0x7E, 5);
    assertEquals(0x73, cpu.getRegister(Z80.REG_A));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_LD_mIYd_A() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.IY = 0x1234;
    this.memory[0x1232] = 0x33;
    final Z80 cpu = executeCommand(state, 0xFD, 0x77, -2);
    assertEquals(0x48, cpu.getRegister(Z80.REG_A));
    assertEquals(0x48, this.memory[0x1232]);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_LD_mIYd_n() {
    final Z80State state = new Z80State();
    state.IY = 0x5C3A;
    this.memory[0x5C80] = 0x00;
    final Z80 cpu = executeCommand(state, 0xFD, 0x36, 0x46, 0xFF);
    assertEquals(-1, this.memory[0x5C80]);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_LD_mIXd_n() {
    final Z80State state = new Z80State();
    state.IX = 0x5C3A;
    this.memory[0x5C80] = 0x00;
    final Z80 cpu = executeCommand(state, 0xDD, 0x36, 0x46, 0xFF);
    assertEquals(-1, this.memory[0x5C80]);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_LD_A_mBC() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.B = 0x12;
    state.C = 0x34;
    this.memory[0x1234] = 0x33;
    final Z80 cpu = executeCommand(state, 0x0A);
    assertEquals(0x33, cpu.getRegister(Z80.REG_A));
    assertEquals(0x33, this.memory[0x1234]);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_LD_A_mDE() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.D = 0x12;
    state.E = 0x34;
    this.memory[0x1234] = 0x33;
    final Z80 cpu = executeCommand(state, 0x1A);
    assertEquals(0x33, cpu.getRegister(Z80.REG_A));
    assertEquals(0x33, this.memory[0x1234]);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_LD_A_mNN() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    this.memory[0x1234] = 0x33;
    final Z80 cpu = executeCommand(state, 0x3A, 0x34, 0x12);
    assertEquals(0x33, cpu.getRegister(Z80.REG_A));
    assertEquals(0x33, this.memory[0x1234]);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 13);
  }

  @Test
  public void testCommand_LD_mBC_A() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.B = 0x12;
    state.C = 0x34;
    this.memory[0x1234] = 0x33;
    final Z80 cpu = executeCommand(state, 0x02);
    assertEquals(0x48, cpu.getRegister(Z80.REG_A));
    assertEquals(0x48, this.memory[0x1234]);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_LD_mDE_A() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.D = 0x12;
    state.E = 0x34;
    this.memory[0x1234] = 0x33;
    final Z80 cpu = executeCommand(state, 0x12);
    assertEquals(0x48, cpu.getRegister(Z80.REG_A));
    assertEquals(0x48, this.memory[0x1234]);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_LD_mNN_A() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.D = 0x12;
    state.E = 0x34;
    this.memory[0x1234] = 0x33;
    final Z80 cpu = executeCommand(state, 0x32, 0x34, 0x12);
    assertEquals(0x48, cpu.getRegister(Z80.REG_A));
    assertEquals(0x48, this.memory[0x1234]);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 13);
  }

  @Test
  public void testCommand_LD_A_I_noZeroAndIFF2False() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.I = 0x12;
    state.F = 0xFF;
    state.iff1 = true;
    state.iff2 = false;
    final Z80 cpu = executeCommand(state, 0xED, 0x57);
    assertEquals(0x12, cpu.getRegister(Z80.REG_A));
    assertEquals(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 9);
  }

  @Test
  public void testCommand_LD_A_I_ZeroAndIFF2True() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.I = 0;
    state.F = 0xFF;
    state.iff1 = false;
    state.iff2 = true;
    final Z80 cpu = executeCommand(state, 0xED, 0x57);
    assertEquals(0, cpu.getRegister(Z80.REG_A));
    assertEquals(Z80.FLAG_C | Z80.FLAG_Z | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 9);
  }

  @Test
  public void testCommand_LD_A_I_NonZeroAndNegativeIFF2True() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.I = -4;
    state.F = 0xFF;
    state.iff1 = false;
    state.iff2 = true;
    final Z80 cpu = executeCommand(state, 0xED, 0x57);
    assertEquals(-4 & 0xFF, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_S | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 9);
  }

  @Test
  public void testCommand_LD_A_R_noZeroAndIFF2False() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.R = 0x12;
    state.F = 0xFF;
    state.iff1 = true;
    state.iff2 = false;
    final Z80 cpu = executeCommand(state, 0xED, 0x5F);
    assertEquals(0x13, cpu.getRegister(Z80.REG_A));
    assertEquals(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 9);
  }

  @Test
  public void testCommand_LD_A_R_ZeroAndIFF2True() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.R = 0x7F;
    state.F = 0xFF;
    state.iff1 = false;
    state.iff2 = true;
    final Z80 cpu = executeCommand(state, 0xED, 0x5F);
    assertEquals(0, cpu.getRegister(Z80.REG_A));
    assertEquals(Z80.FLAG_C | Z80.FLAG_Z | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 9);
  }

  @Test
  public void testCommand_LD_A_R_NonZeroAndNegativeIFF2True() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.R = -4;
    state.F = 0xFF;
    state.iff1 = false;
    state.iff2 = true;
    final Z80 cpu = executeCommand(state, 0xED, 0x5F);
    assertEquals(-3 & 0xFF, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_S | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 9);
  }

  @Test
  public void testCommand_LD_I_A() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.I = 0;
    final Z80 cpu = executeCommand(state, 0xED, 0x47);
    assertEquals(0x48, cpu.getRegister(Z80.REG_I));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 9);
  }

  @Test
  public void testCommand_LD_R_A() {
    final Z80State state = new Z80State();
    state.A = 0x48;
    state.R = 0;
    final Z80 cpu = executeCommand(state, 0xED, 0x4F);
    assertEquals(0x48, cpu.getRegister(Z80.REG_R));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 9);
  }

  @Test
  public void testCommand_LD_BC_nn() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, 0x01, 0x34, 0x12);
    assertEquals(0x1234, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_LD_DE_nn() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, 0x11, 0x34, 0x12);
    assertEquals(0x1234, cpu.getRegisterPair(Z80.REGPAIR_DE));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_LD_HL_nn() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, 0x21, 0x34, 0x12);
    assertEquals(0x1234, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_LD_IX_nn() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, 0xDD, 0x21, 0x34, 0x12);
    assertEquals(0x1234, cpu.getRegister(Z80.REG_IX));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 14);
  }

  @Test
  public void testCommand_LD_IY_nn() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, 0xFD, 0x21, 0x34, 0x12);
    assertEquals(0x1234, cpu.getRegister(Z80.REG_IY));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 14);
  }

  @Test
  public void testCommand_LD_mNN_BC() {
    final Z80State state = new Z80State();
    state.B = 0xAA;
    state.C = 0xBB;
    final Z80 cpu = executeCommand(state, 0xED, 0x43, 0x34, 0x12);
    assertEquals(0xBB, this.memory[0x1234] & 0xFF);
    assertEquals(0xAA, this.memory[0x1235] & 0xFF);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_LD_mNN_DE() {
    final Z80State state = new Z80State();
    state.D = 0xAA;
    state.E = 0xBB;
    final Z80 cpu = executeCommand(state, 0xED, 0x53, 0x34, 0x12);
    assertEquals(0xBB, this.memory[0x1234] & 0xFF);
    assertEquals(0xAA, this.memory[0x1235] & 0xFF);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_LD_mNN_HL() {
    final Z80State state = new Z80State();
    state.H = 0xAA;
    state.L = 0xBB;
    final Z80 cpu = executeCommand(state, 0xED, 0x63, 0x34, 0x12);
    assertEquals(0xBB, this.memory[0x1234] & 0xFF);
    assertEquals(0xAA, this.memory[0x1235] & 0xFF);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_LD_mNN_SP() {
    final Z80State state = new Z80State();
    state.SP = 0xAABB;
    final Z80 cpu = executeCommand(state, 0xED, 0x73, 0x34, 0x12);
    assertEquals(0xBB, this.memory[0x1234] & 0xFF);
    assertEquals(0xAA, this.memory[0x1235] & 0xFF);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_LD_mNN_IX() {
    final Z80State state = new Z80State();
    state.IX = 0xAABB;
    final Z80 cpu = executeCommand(state, 0xDD, 0x22, 0x34, 0x12);
    assertEquals(0xBB, this.memory[0x1234] & 0xFF);
    assertEquals(0xAA, this.memory[0x1235] & 0xFF);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_LD_mNN_IY() {
    final Z80State state = new Z80State();
    state.IY = 0xAABB;
    final Z80 cpu = executeCommand(state, 0xFD, 0x22, 0x34, 0x12);
    assertEquals(0xBB, this.memory[0x1234] & 0xFF);
    assertEquals(0xAA, this.memory[0x1235] & 0xFF);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_LD_BC_mNN() {
    final Z80State state = new Z80State();
    this.memory[0x1234] = (byte) 0xAA;
    this.memory[0x1235] = (byte) 0xBB;
    final Z80 cpu = executeCommand(state, 0xED, 0x4B, 0x34, 0x12);
    assertEquals(0xBBAA, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_LD_DE_mNN() {
    final Z80State state = new Z80State();
    this.memory[0x1234] = (byte) 0xAA;
    this.memory[0x1235] = (byte) 0xBB;
    final Z80 cpu = executeCommand(state, 0xED, 0x5B, 0x34, 0x12);
    assertEquals(0xBBAA, cpu.getRegisterPair(Z80.REGPAIR_DE));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_LD_HL_mNN() {
    final Z80State state = new Z80State();
    this.memory[0x1234] = (byte) 0xAA;
    this.memory[0x1235] = (byte) 0xBB;
    final Z80 cpu = executeCommand(state, 0xED, 0x6B, 0x34, 0x12);
    assertEquals(0xBBAA, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_LD_IX_mNN() {
    final Z80State state = new Z80State();
    this.memory[0x1234] = (byte) 0xAA;
    this.memory[0x1235] = (byte) 0xBB;
    final Z80 cpu = executeCommand(state, 0xDD, 0x2A, 0x34, 0x12);
    assertEquals(0xBBAA, cpu.getRegister(Z80.REG_IX));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_LD_IY_mNN() {
    final Z80State state = new Z80State();
    this.memory[0x1234] = (byte) 0xAA;
    this.memory[0x1235] = (byte) 0xBB;
    final Z80 cpu = executeCommand(state, 0xFD, 0x2A, 0x34, 0x12);
    assertEquals(0xBBAA, cpu.getRegister(Z80.REG_IY));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_PUSH_BC() {
    final Z80State state = new Z80State();
    state.B = 0x22;
    state.C = 0x33;
    state.SP = 0x1007;
    final Z80 cpu = executeCommand(state, 0xC5);
    assertEquals(0x1005, cpu.getRegister(Z80.REG_SP));
    assertMemory(0x1006, 0x22);
    assertMemory(0x1005, 0x33);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_PUSH_DE() {
    final Z80State state = new Z80State();
    state.D = 0x22;
    state.E = 0x33;
    state.SP = 0x1007;
    final Z80 cpu = executeCommand(state, 0xD5);
    assertEquals(0x1005, cpu.getRegister(Z80.REG_SP));
    assertMemory(0x1006, 0x22);
    assertMemory(0x1005, 0x33);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_PUSH_HL() {
    final Z80State state = new Z80State();
    state.H = 0x22;
    state.L = 0x33;
    state.SP = 0x1007;
    final Z80 cpu = executeCommand(state, 0xE5);
    assertEquals(0x1005, cpu.getRegister(Z80.REG_SP));
    assertMemory(0x1006, 0x22);
    assertMemory(0x1005, 0x33);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_PUSH_AF() {
    final Z80State state = new Z80State();
    state.A = 0x22;
    state.F = 0x33;
    state.SP = 0x1007;
    final Z80 cpu = executeCommand(state, 0xF5);
    assertEquals(0x1005, cpu.getRegister(Z80.REG_SP));
    assertMemory(0x1006, 0x22);
    assertMemory(0x1005, 0x33);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_PUSH_IX() {
    final Z80State state = new Z80State();
    state.IX = 0x2233;
    state.SP = 0x1007;
    final Z80 cpu = executeCommand(state, 0xDD, 0xE5);
    assertEquals(0x1005, cpu.getRegister(Z80.REG_SP));
    assertMemory(0x1006, 0x22);
    assertMemory(0x1005, 0x33);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_PUSH_IY() {
    final Z80State state = new Z80State();
    state.IY = 0x2233;
    state.SP = 0x1007;
    final Z80 cpu = executeCommand(state, 0xFD, 0xE5);
    assertEquals(0x1005, cpu.getRegister(Z80.REG_SP));
    assertMemory(0x1006, 0x22);
    assertMemory(0x1005, 0x33);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_POP_DE() {
    final Z80State state = new Z80State();
    state.SP = 0x1000;
    memory[0x1000] = 0x55;
    memory[0x1001] = 0x33;
    final Z80 cpu = executeCommand(state, 0xD1);
    assertEquals(0x3355, cpu.getRegisterPair(Z80.REGPAIR_DE));
    assertEquals(0x1002, cpu.getRegister(Z80.REG_SP));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_POP_HL() {
    final Z80State state = new Z80State();
    state.SP = 0x1000;
    memory[0x1000] = 0x55;
    memory[0x1001] = 0x33;
    final Z80 cpu = executeCommand(state, 0xE1);
    assertEquals(0x3355, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x1002, cpu.getRegister(Z80.REG_SP));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_POP_AF() {
    final Z80State state = new Z80State();
    state.SP = 0x1000;
    memory[0x1000] = 0x55;
    memory[0x1001] = 0x33;
    final Z80 cpu = executeCommand(state, 0xF1);
    assertEquals(0x3355, cpu.getRegisterPair(Z80.REGPAIR_AF));
    assertEquals(0x1002, cpu.getRegister(Z80.REG_SP));
    state.F = 0x55;
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_POP_IX() {
    final Z80State state = new Z80State();
    state.SP = 0x1000;
    memory[0x1000] = 0x55;
    memory[0x1001] = 0x33;
    final Z80 cpu = executeCommand(state, 0xDD, 0xE1);
    assertEquals(0x3355, cpu.getRegister(Z80.REG_IX));
    assertEquals(0x1002, cpu.getRegister(Z80.REG_SP));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 14);
  }

  @Test
  public void testCommand_POP_IY() {
    final Z80State state = new Z80State();
    state.SP = 0x1000;
    memory[0x1000] = 0x55;
    memory[0x1001] = 0x33;
    final Z80 cpu = executeCommand(state, 0xFD, 0xE1);
    assertEquals(0x3355, cpu.getRegister(Z80.REG_IY));
    assertEquals(0x1002, cpu.getRegister(Z80.REG_SP));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 14);
  }

  @Test
  public void testCommand_EX_DE_HL() {
    final Z80State state = new Z80State();
    state.D = 0x28;
    state.E = 0x22;
    state.H = 0x49;
    state.L = 0x9A;
    final Z80 cpu = executeCommand(state, 0xEB);
    assertEquals(0x2822, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x499A, cpu.getRegisterPair(Z80.REGPAIR_DE));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_EX_mSP_HL() {
    final Z80State state = new Z80State();
    state.SP = 0x8856;
    state.H = 0x70;
    state.L = 0x12;
    memory[0x8856] = 0x11;
    memory[0x8857] = 0x22;
    final Z80 cpu = executeCommand(state, 0xE3);
    assertEquals(0x2211, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertMemory(0x8856, 0x12);
    assertMemory(0x8857, 0x70);
    assertEquals(0x8856, cpu.getRegister(Z80.REG_SP));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_EX_mSP_IX() {
    final Z80State state = new Z80State();
    state.SP = 0x8856;
    state.IX = 0x7012;
    memory[0x8856] = 0x11;
    memory[0x8857] = 0x22;
    final Z80 cpu = executeCommand(state, 0xDD, 0xE3);
    assertEquals(0x2211, cpu.getRegister(Z80.REG_IX));
    assertMemory(0x8856, 0x12);
    assertMemory(0x8857, 0x70);
    assertEquals(0x8856, cpu.getRegister(Z80.REG_SP));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_EX_mSP_IY() {
    final Z80State state = new Z80State();
    state.SP = 0x8856;
    state.IY = 0x7012;
    memory[0x8856] = 0x11;
    memory[0x8857] = 0x22;
    final Z80 cpu = executeCommand(state, 0xFD, 0xE3);
    assertEquals(0x2211, cpu.getRegister(Z80.REG_IY));
    assertMemory(0x8856, 0x12);
    assertMemory(0x8857, 0x70);
    assertEquals(0x8856, cpu.getRegister(Z80.REG_SP));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_EX_AF_AF() {
    final Z80State state = new Z80State();
    state.A = 0x10;
    state.F = 0x20;
    state.altA = 0x30;
    state.altF = 0x40;
    final Z80 cpu = executeCommand(state, 0x08);
    assertEquals(0x30, cpu.getRegister(Z80.REG_A));
    assertEquals(0x40, cpu.getRegister(Z80.REG_F));
    assertEquals(0x10, cpu.getRegister(Z80.REG_A, true));
    assertEquals(0x20, cpu.getRegister(Z80.REG_F, true));
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_EXX() {
    final Z80State state = new Z80State();
    state.A = 1;
    state.F = 3;
    state.B = 0x10;
    state.C = 0x20;
    state.D = 0x30;
    state.E = 0x40;
    state.H = 0x50;
    state.L = 0x60;
    state.IX = 0x61;
    state.IY = 0x62;
    state.WZ = 0x1234;

    state.altA = 2;
    state.altF = 5;
    state.altB = 0x70;
    state.altC = 0x80;
    state.altD = 0x90;
    state.altE = 0xA0;
    state.altH = 0xB0;
    state.altL = 0xC0;
    state.altWZ = 0x4321;

    final Z80 cpu = executeCommand(state, 0xD9);
    assertEquals(0x70, cpu.getRegister(Z80.REG_B));
    assertEquals(0x80, cpu.getRegister(Z80.REG_C));
    assertEquals(0x90, cpu.getRegister(Z80.REG_D));
    assertEquals(0xA0, cpu.getRegister(Z80.REG_E));
    assertEquals(0xB0, cpu.getRegister(Z80.REG_H));
    assertEquals(0xC0, cpu.getRegister(Z80.REG_L));
    assertEquals(0x61, cpu.getRegister(Z80.REG_IX));
    assertEquals(0x62, cpu.getRegister(Z80.REG_IY));
    assertEquals(1, cpu.getRegister(Z80.REG_A));
    assertEquals(3, cpu.getRegister(Z80.REG_F));
    assertEquals(0x10, cpu.getRegister(Z80.REG_B, true));
    assertEquals(0x20, cpu.getRegister(Z80.REG_C, true));
    assertEquals(0x30, cpu.getRegister(Z80.REG_D, true));
    assertEquals(0x40, cpu.getRegister(Z80.REG_E, true));
    assertEquals(0x50, cpu.getRegister(Z80.REG_H, true));
    assertEquals(0x60, cpu.getRegister(Z80.REG_L, true));
    assertEquals(2, cpu.getRegister(Z80.REG_A, true));
    assertEquals(5, cpu.getRegister(Z80.REG_F, true));
    assertEquals(0x1234, cpu.getWZ(true));
    assertEquals(0x4321, cpu.getWZ(false));
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_LDI_BCPostive() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x11;
    memory[0x1111] = (byte) 0x88;
    state.D = 0x22;
    state.E = 0x22;
    memory[0x2222] = (byte) 0x66;
    state.B = 0x00;
    state.C = 0x07;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xED, 0xA0);
    assertEquals(0x1112, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertMemory(0x1111, 0x88);
    assertEquals(0x2223, cpu.getRegisterPair(Z80.REGPAIR_DE));
    assertMemory(0x2222, 0x88);
    assertEquals(0x06, cpu.getRegisterPair(Z80.REGPAIR_BC));

    assertFlagsExcludeReserved(Z80.FLAG_S | Z80.FLAG_Z | Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_LDI_BCZero() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x11;
    memory[0x1111] = (byte) 0x88;
    state.D = 0x22;
    state.E = 0x22;
    memory[0x2222] = (byte) 0x66;
    state.B = 0x00;
    state.C = 0x01;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xED, 0xA0);
    assertEquals(0x1112, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertMemory(0x1111, 0x88);
    assertEquals(0x2223, cpu.getRegisterPair(Z80.REGPAIR_DE));
    assertMemory(0x2222, 0x88);
    assertEquals(0x00, cpu.getRegisterPair(Z80.REGPAIR_BC));

    assertFlagsExcludeReserved(Z80.FLAG_S | Z80.FLAG_Z | Z80.FLAG_C, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_LDIR() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x11;
    state.D = 0x22;
    state.E = 0x22;
    state.B = 0x00;
    state.C = 0x03;

    state.F = 0xFF;

    memory[0x1111] = (byte) 0x88;
    memory[0x1112] = (byte) 0x36;
    memory[0x1113] = (byte) 0xA5;

    memory[0x2222] = (byte) 0x66;
    memory[0x2223] = (byte) 0x59;
    memory[0x2224] = (byte) 0xC5;

    final Z80 cpu = executeRepeatingBlockCommand(state, 0xED, 0xB0);

    assertEquals(0x1114, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x2225, cpu.getRegisterPair(Z80.REGPAIR_DE));
    assertEquals(0x0000, cpu.getRegisterPair(Z80.REGPAIR_BC));

    assertMemory(0x1110, 0x00);
    assertMemory(0x1111, 0x88);
    assertMemory(0x1112, 0x36);
    assertMemory(0x1113, 0xA5);
    assertMemory(0x1114, 0x00);

    assertMemory(0x2221, 0x00);
    assertMemory(0x2222, 0x88);
    assertMemory(0x2223, 0x36);
    assertMemory(0x2224, 0xA5);
    assertMemory(0x2225, 0x00);

    assertFlagsExcludeReserved(Z80.FLAG_S | Z80.FLAG_Z | Z80.FLAG_C, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 58);
  }

  @Test
  public void testCommand_LDD_BCPostive() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x11;
    memory[0x1111] = (byte) 0x88;
    state.D = 0x22;
    state.E = 0x22;
    memory[0x2222] = (byte) 0x66;
    state.B = 0x00;
    state.C = 0x07;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xED, 0xA8);
    assertEquals(0x1110, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertMemory(0x1111, 0x88);
    assertEquals(0x2221, cpu.getRegisterPair(Z80.REGPAIR_DE));
    assertMemory(0x2222, 0x88);
    assertEquals(0x06, cpu.getRegisterPair(Z80.REGPAIR_BC));

    assertFlagsExcludeReserved(Z80.FLAG_S | Z80.FLAG_Z | Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_LDD_BCZero() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x11;
    memory[0x1111] = (byte) 0x88;
    state.D = 0x22;
    state.E = 0x22;
    memory[0x2222] = (byte) 0x66;
    state.B = 0x00;
    state.C = 0x01;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xED, 0xA8);
    assertEquals(0x1110, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertMemory(0x1111, 0x88);
    assertEquals(0x2221, cpu.getRegisterPair(Z80.REGPAIR_DE));
    assertMemory(0x2222, 0x88);
    assertEquals(0x00, cpu.getRegisterPair(Z80.REGPAIR_BC));

    assertFlagsExcludeReserved(Z80.FLAG_S | Z80.FLAG_Z | Z80.FLAG_C, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_LDDR() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x14;
    state.D = 0x22;
    state.E = 0x25;
    state.B = 0x00;
    state.C = 0x03;

    state.F = 0xFF;

    memory[0x1112] = (byte) 0x88;
    memory[0x1113] = (byte) 0x36;
    memory[0x1114] = (byte) 0xA5;

    memory[0x2223] = (byte) 0x66;
    memory[0x2224] = (byte) 0x59;
    memory[0x2225] = (byte) 0xC5;

    final Z80 cpu = executeRepeatingBlockCommand(state, 0xED, 0xB8);

    assertEquals(0x1111, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x2222, cpu.getRegisterPair(Z80.REGPAIR_DE));
    assertEquals(0x0000, cpu.getRegisterPair(Z80.REGPAIR_BC));

    assertMemory(0x1114, 0xA5);
    assertMemory(0x1113, 0x36);
    assertMemory(0x1112, 0x88);
    assertMemory(0x1111, 0x00);

    assertMemory(0x2225, 0xA5);
    assertMemory(0x2224, 0x36);
    assertMemory(0x2223, 0x88);
    assertMemory(0x2222, 0x00);

    assertFlagsExcludeReserved(Z80.FLAG_S | Z80.FLAG_Z | Z80.FLAG_C, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 58);
  }

  @Test
  public void testCommand_CPI_Equal() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x11;
    memory[0x1111] = (byte) 0x3B;
    state.A = 0x3B;
    state.B = 0x00;
    state.C = 0x01;

    state.F = 0xFF;

    final Z80 cpu = executeRepeatingBlockCommand(state, 0xED, 0xA1);

    assertEquals(0x1112, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0000, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertEquals(Z80.FLAG_Z | Z80.FLAG_C | Z80.FLAG_N, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_CPI_NotEqual() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x11;
    memory[0x1111] = (byte) 0x30;
    state.A = 0x3B;
    state.B = 0x00;
    state.C = 0x01;

    state.F = 0xFF;

    final Z80 cpu = executeRepeatingBlockCommand(state, 0xED, 0xA1);

    assertEquals(0x1112, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0000, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_N, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_CPI_BCNotZeroAndNoEqual() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x11;
    memory[0x1111] = (byte) 0x30;
    state.A = 0x3B;
    state.B = 0x00;
    state.C = 0x02;

    state.F = 0xFF;

    final Z80 cpu = executeRepeatingBlockCommand(state, 0xED, 0xA1);

    assertEquals(0x1112, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0001, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_PV | Z80.FLAG_N, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_CPIR_EqualsPresented() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x11;
    state.A = 0xF3;
    state.B = 0x00;
    state.C = 0x07;

    state.F = 0xFF;

    memory[0x1111] = (byte) 0x52;
    memory[0x1112] = (byte) 0x00;
    memory[0x1113] = (byte) 0xF3;

    final Z80 cpu = executeRepeatingBlockCommand(state, 0xED, 0xB1);

    assertEquals(0x1114, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0004, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertEquals(Z80.FLAG_Z | Z80.FLAG_C | Z80.FLAG_PV | Z80.FLAG_N, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 58);
  }

  @Test
  public void testCommand_CPIR_EqualsNotPresented() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x11;
    state.A = 0xF3;
    state.B = 0x00;
    state.C = 0x07;

    state.F = 0xFF;

    memory[0x1111] = (byte) 0x52;
    memory[0x1112] = (byte) 0x00;
    memory[0x1113] = (byte) 0xF4;
    memory[0x1114] = (byte) 0xF5;
    memory[0x1115] = (byte) 0xF6;
    memory[0x1116] = (byte) 0xF9;
    memory[0x1117] = (byte) 0xA9;

    final Z80 cpu = executeRepeatingBlockCommand(state, 0xED, 0xB1);

    assertEquals(0x1118, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0000, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_N | Z80.FLAG_H, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 142);
  }

  @Test
  public void testCommand_CPD_EqualsPresented() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x11;
    memory[0x1111] = (byte) 0x3B;
    state.A = 0x3B;
    state.B = 0x00;
    state.C = 0x01;

    state.F = 0xFF;

    final Z80 cpu = executeRepeatingBlockCommand(state, 0xED, 0xA9);

    assertEquals(0x1110, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0000, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertEquals(Z80.FLAG_Z | Z80.FLAG_C | Z80.FLAG_N, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_CPDR_EqualsPresented() {
    final Z80State state = new Z80State();
    state.H = 0x11;
    state.L = 0x18;
    state.A = 0xF3;
    state.B = 0x00;
    state.C = 0x07;

    memory[0x1118] = (byte) 0x52;
    memory[0x1117] = (byte) 0x00;
    memory[0x1116] = (byte) 0xF3;

    state.F = 0xFF;

    final Z80 cpu = executeRepeatingBlockCommand(state, 0xED, 0xB9);

    assertEquals(0x1115, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0004, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertEquals(Z80.FLAG_Z | Z80.FLAG_PV | Z80.FLAG_C | Z80.FLAG_N, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 58);
  }

  @Test
  public void testCommand_ADD_C_notFlagSet() {
    final Z80State state = new Z80State();
    state.A = 0x44;
    state.C = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x81);

    assertEquals(0x11, cpu.getRegister(Z80.REG_C));
    assertEquals(0x55, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(0, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_ADD_C_flagV() {
    final Z80State state = new Z80State();
    state.A = 0x44;
    state.C = 0x7F;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x81);

    assertEquals(0x7F, cpu.getRegister(Z80.REG_C));
    assertEquals(0xC3, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV | Z80.FLAG_S | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_ADD_N_flagV() {
    final Z80State state = new Z80State();
    state.A = 0x44;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xC6, 0x7F);

    assertEquals(0xC3, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV | Z80.FLAG_S | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_ADD_mHL_flagV() {
    final Z80State state = new Z80State();
    state.A = 0x44;

    state.F = 0xFF;
    state.H = 0x12;
    state.L = 0x34;

    memory[0x1234] = (byte) 0x7F;

    final Z80 cpu = executeCommand(state, 0x86);

    assertEquals(0xC3, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV | Z80.FLAG_S | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_ADD_mIXd_flagV() {
    final Z80State state = new Z80State();
    state.A = 0x44;

    state.F = 0xFF;
    state.IX = 0x1234;

    memory[0x1237] = (byte) 0x7F;

    final Z80 cpu = executeCommand(state, 0xDD, 0x86, 3);

    assertEquals(0xC3, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV | Z80.FLAG_S | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_ADD_mIYd_flagV() {
    final Z80State state = new Z80State();
    state.A = 0x44;

    state.F = 0xFF;
    state.IY = 0x1234;

    memory[0x1237] = (byte) 0x7F;

    final Z80 cpu = executeCommand(state, 0xFD, 0x86, 3);

    assertEquals(0xC3, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV | Z80.FLAG_S | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_ADD_C_flagsZCH() {
    final Z80State state = new Z80State();
    state.A = 0xFF;
    state.C = 0x01;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x81);

    assertEquals(0x01, cpu.getRegister(Z80.REG_C));
    assertEquals(0x00, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_Z | Z80.FLAG_C | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_ADC_C_notFlagSet() {
    final Z80State state = new Z80State();
    state.A = 0x44;
    state.C = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x89);

    assertEquals(0x11, cpu.getRegister(Z80.REG_C));
    assertEquals(0x56, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(0, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_ADC_C_flagV() {
    final Z80State state = new Z80State();
    state.A = 0x44;
    state.C = 0x7F;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x89);

    assertEquals(0x7F, cpu.getRegister(Z80.REG_C));
    assertEquals(0xC4, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV | Z80.FLAG_S | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_ADC_N_flagV() {
    final Z80State state = new Z80State();
    state.A = 0x44;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xCE, 0x7F);

    assertEquals(0xC4, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV | Z80.FLAG_S | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_ADC_mHL_flagV() {
    final Z80State state = new Z80State();
    state.A = 0x44;

    state.L = 0x34;
    state.H = 0x12;
    state.F = 0xFF;

    memory[0x1234] = (byte) 0x7F;

    final Z80 cpu = executeCommand(state, 0x8E);

    assertEquals(0xC4, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV | Z80.FLAG_S | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_ADC_mIXd_flagV() {
    final Z80State state = new Z80State();
    state.A = 0x44;

    state.IX = 0x1234;
    state.F = 0xFF;

    memory[0x1238] = (byte) 0x7F;

    final Z80 cpu = executeCommand(state, 0xDD, 0x8E, 0x04);

    assertEquals(0xC4, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV | Z80.FLAG_S | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_ADC_mIYd_flagV() {
    final Z80State state = new Z80State();
    state.A = 0x44;

    state.IY = 0x1234;
    state.F = 0xFF;

    memory[0x1238] = (byte) 0x7F;

    final Z80 cpu = executeCommand(state, 0xFD, 0x8E, 0x04);

    assertEquals(0xC4, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV | Z80.FLAG_S | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_ADC_C_flagsCH() {
    final Z80State state = new Z80State();
    state.A = 0xFF;
    state.C = 0x01;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x89);

    assertEquals(0x01, cpu.getRegister(Z80.REG_C));
    assertEquals(0x01, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_SUB_C() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.C = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x91);

    assertEquals(0x11, cpu.getRegister(Z80.REG_C));
    assertEquals(0x18, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_SUB_n() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xD6, 0x11);

    assertEquals(0x18, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_SUB_mHL() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.L = 0x34;
    state.H = 0x12;

    memory[0x1234] = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x96);

    assertEquals(0x18, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_SUB_mIX() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.IX = 0x1234;

    memory[0x1239] = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xDD, 0x96, 0x05);

    assertEquals(0x18, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_SUB_mIY() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.IY = 0x1234;

    memory[0x1239] = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFD, 0x96, 0x05);

    assertEquals(0x18, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_SBC_C() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.C = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x99);

    assertEquals(0x11, cpu.getRegister(Z80.REG_C));
    assertEquals(0x17, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_SBC_N() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xDE, 0x11);

    assertEquals(0x17, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_SBC_mHL() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.F = 0xFF;

    state.L = 0x34;
    state.H = 0x12;

    memory[0x1234] = 0x11;

    final Z80 cpu = executeCommand(state, 0x9E);

    assertEquals(0x17, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_SBC_mIXd() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.F = 0xFF;

    state.IX = 0x1234;

    memory[0x1237] = 0x11;

    final Z80 cpu = executeCommand(state, 0xDD, 0x9E, 0x03);

    assertEquals(0x17, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_SBC_mIYd() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.F = 0xFF;

    state.IY = 0x1234;

    memory[0x1237] = 0x11;

    final Z80 cpu = executeCommand(state, 0xFD, 0x9E, 0x03);

    assertEquals(0x17, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_AND_C() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.C = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xA1);

    assertEquals(0x11, cpu.getRegister(Z80.REG_C));
    assertEquals(0x01, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_AND_C_ZeroResult() {
    final Z80State state = new Z80State();
    state.A = 0xF0;
    state.C = 0x0F;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xA1);

    assertEquals(0x0F, cpu.getRegister(Z80.REG_C));
    assertEquals(0x00, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_Z | Z80.FLAG_H | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_AND_C_ParityFalse() {
    final Z80State state = new Z80State();
    state.A = 0x07;
    state.C = 0x09;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xA1);

    assertEquals(0x09, cpu.getRegister(Z80.REG_C));
    assertEquals(0x01, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_AND_N() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xE6, 0x11);

    assertEquals(0x01, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_AND_mHL() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.L = 0x34;
    state.H = 0x12;
    state.F = 0xFF;

    memory[0x1234] = 0x11;

    final Z80 cpu = executeCommand(state, 0xA6);

    assertEquals(0x01, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_AND_mIXd() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.IX = 0x1234;
    state.F = 0xFF;

    memory[0x1239] = 0x11;

    final Z80 cpu = executeCommand(state, 0xDD, 0xA6, 0x05);

    assertEquals(0x01, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_AND_mIYd() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.IY = 0x1234;
    state.F = 0xFF;

    memory[0x1239] = 0x11;

    final Z80 cpu = executeCommand(state, 0xFD, 0xA6, 0x05);

    assertEquals(0x01, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_OR_C() {
    final Z80State state = new Z80State();
    state.A = 0x07;
    state.C = 0x09;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xB1);

    assertEquals(0x09, cpu.getRegister(Z80.REG_C));
    assertEquals(0x0F, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_OR_n() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xF6, 0x11);

    assertEquals(0x39, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_OR_mHL() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.L = 0x34;
    state.H = 0x12;
    state.F = 0xFF;

    memory[0x1234] = 0x11;

    final Z80 cpu = executeCommand(state, 0xB6);

    assertEquals(0x39, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_OR_mIXd() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.IX = 0x1234;
    state.F = 0xFF;

    memory[0x1239] = 0x11;

    final Z80 cpu = executeCommand(state, 0xDD, 0xB6, 0x05);

    assertEquals(0x39, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_OR_mIYd() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.IY = 0x1234;
    state.F = 0xFF;

    memory[0x1239] = 0x11;

    final Z80 cpu = executeCommand(state, 0xFD, 0xB6, 0x05);

    assertEquals(0x39, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_PV, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_XOR_C() {
    final Z80State state = new Z80State();
    state.A = 0x07;
    state.C = 0x09;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xA9);

    assertEquals(0x09, cpu.getRegister(Z80.REG_C));
    assertEquals(0x0E, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(0, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_XOR_n() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xEE, 0x11);

    assertEquals(0x38, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(0, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_XOR_mHL() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.L = 0x34;
    state.H = 0x12;
    state.F = 0xFF;

    memory[0x1234] = 0x11;

    final Z80 cpu = executeCommand(state, 0xAE);

    assertEquals(0x38, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(0, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_XOR_mIXd() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.IX = 0x1234;
    state.F = 0xFF;

    memory[0x1239] = 0x11;

    final Z80 cpu = executeCommand(state, 0xDD, 0xAE, 0x05);

    assertEquals(0x38, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(0, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_XOR_mIYd() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.IY = 0x1234;
    state.F = 0xFF;

    memory[0x1239] = 0x11;

    final Z80 cpu = executeCommand(state, 0xFD, 0xAE, 0x05);

    assertEquals(0x38, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(0, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_CP_C_Equals() {
    final Z80State state = new Z80State();
    state.A = 0xF7;
    state.C = 0xF7;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xB9);

    assertEquals(0xF7, cpu.getRegister(Z80.REG_C));
    assertEquals(0xF7, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_Z | Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_CP_C_More() {
    final Z80State state = new Z80State();
    state.A = 0xF7;
    state.C = 0xF8;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xB9);

    assertEquals(0xF8, cpu.getRegister(Z80.REG_C));
    assertEquals(0xF7, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N | Z80.FLAG_C | Z80.FLAG_S | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_CP_C_Less() {
    final Z80State state = new Z80State();
    state.A = 0xF7;
    state.C = 0xF6;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xB9);

    assertEquals(0xF6, cpu.getRegister(Z80.REG_C));
    assertEquals(0xF7, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_CP_n() {
    final Z80State state = new Z80State();
    state.A = 0x29;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFE, 0x11);

    assertEquals(0x29, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_CP_n_A_less_Value() {
    final Z80State state = new Z80State();
    state.A = 69;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFE, 254);

    assertEquals(69, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_N | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_CP_n_A_greater_Value() {
    final Z80State state = new Z80State();
    state.A = 0xFE;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFE, 69);

    assertEquals(0xFE, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_S | Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_CP_n_A_equ_Value() {
    final Z80State state = new Z80State();
    state.A = 0xFE;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFE, 0xFE);

    assertEquals(0xFE, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_Z | Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_CP_mHL() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.L = 0x34;
    state.H = 0x12;
    memory[0x1234] = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xBE);

    assertEquals(0x29, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_CP_mIXd() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.IX = 0x1234;
    memory[0x1239] = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xDD, 0xBE, 0x05);

    assertEquals(0x29, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_CP_mIYd() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.IY = 0x1234;
    memory[0x1239] = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFD, 0xBE, 0x05);

    assertEquals(0x29, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_ADD_HL_DE() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.L = 0x42;
    state.H = 0x42;
    state.E = 0x11;
    state.D = 0x11;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x19);

    assertEquals(0x5353, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertFlagsExcludeReserved(Z80.FLAG_Z | Z80.FLAG_S | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_ADD_IX_DE() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.IX = 0x3333;
    state.E = 0x55;
    state.D = 0x55;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xDD, 0x19);

    assertEquals(0x8888, cpu.getRegister(Z80.REG_IX));
    assertFlagsExcludeReserved(Z80.FLAG_X | Z80.FLAG_Y | Z80.FLAG_Z | Z80.FLAG_S | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_ADD_IY_DE() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.IY = 0x3333;
    state.E = 0x55;
    state.D = 0x55;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFD, 0x19);

    assertEquals(0x8888, cpu.getRegister(Z80.REG_IY));
    assertFlagsExcludeReserved(Z80.FLAG_X | Z80.FLAG_Y | Z80.FLAG_Z | Z80.FLAG_S | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_ADC_HL_BC() {
    final Z80State state = new Z80State();
    state.A = 0x29;

    state.H = 0x54;
    state.L = 0x37;
    state.B = 0x22;
    state.C = 0x22;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xED, 0x4A);

    assertEquals(0x765A, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertFlagsExcludeReserved(0, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_CPL() {
    final Z80State state = new Z80State();
    state.A = 0xB4;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x2F);

    assertEquals(0x4B, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(0xFF, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_DAA() {
    final Z80State state = new Z80State();
    state.A = 0x3C;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0x27);

    assertEquals(0x42, cpu.getRegister(Z80.REG_A));
    assertEquals(Z80.FLAG_H | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_NEG() {
    final Z80State state = new Z80State();
    state.A = 0x98;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xED, 0x44);

    assertEquals(0x68, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_N | Z80.FLAG_H, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_CCF_Set() {
    final Z80State state = new Z80State();
    state.A = 0x98;

    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x3F);

    assertEquals(0x98, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_X | Z80.FLAG_Y | Z80.FLAG_H | Z80.FLAG_PV | Z80.FLAG_Z | Z80.FLAG_S, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_CCF_Reset() {
    final Z80State state = new Z80State();
    state.A = 0x98;

    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0x3F);

    assertEquals(0x98, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_SCF() {
    final Z80State state = new Z80State();
    state.A = 0x98;

    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0x37);

    assertEquals(0x98, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_NOP() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, 0x00);
    state.PC++;
    state.R++;

    assertEquals(state, new Z80State(cpu));
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_INC_A() {
    final Z80State state = new Z80State();
    state.A = 0x28;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x3C);

    assertEquals(0x29, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_INC_mHL() {
    final Z80State state = new Z80State();
    state.L = 0x34;
    state.H = 0x34;
    state.F = 0xFF;

    this.memory[0x3434] = 0x34;

    final Z80 cpu = executeCommand(state, 0x34);

    assertEquals(0x3434, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertMemory(0x3434, 0x35);
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_INC_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x2020;
    state.F = 0xFF;

    this.memory[0x2030] = 0x34;

    final Z80 cpu = executeCommand(state, 0xDD, 0x34, 0x10);

    assertEquals(0x2020, cpu.getRegister(Z80.REG_IX));
    assertMemory(0x2030, 0x35);
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_INC_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x2020;
    state.F = 0xFF;

    this.memory[0x2030] = 0x34;

    final Z80 cpu = executeCommand(state, 0xFD, 0x34, 0x10);

    assertEquals(0x2020, cpu.getRegister(Z80.REG_IY));
    assertMemory(0x2030, 0x35);
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F) & 0xFF);

    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_INC_BC() {
    final Z80State state = new Z80State();
    state.B = 0x10;
    state.C = 0x00;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x03);

    assertEquals(0x1001, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 6);
  }

  @Test
  public void testCommand_INC_SP() {
    final Z80State state = new Z80State();
    state.SP = 0x1000;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x33);

    assertEquals(0x1001, cpu.getRegister(Z80.REG_SP));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 6);
  }

  @Test
  public void testCommand_INC_HL() {
    final Z80State state = new Z80State();
    state.H = 0x10;
    state.L = 0x00;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x23);

    assertEquals(0x1001, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 6);
  }

  @Test
  public void testCommand_INC_IX() {
    final Z80State state = new Z80State();
    state.IX = 0x1000;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xDD, 0x23);

    assertEquals(0x1001, cpu.getRegister(Z80.REG_IX));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_INC_IXh() {
    final Z80State state = new Z80State();
    state.IX = 0x1000;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xDD, 0x24);

    assertEquals(0x1100, cpu.getRegister(Z80.REG_IX));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_INC_IXl() {
    final Z80State state = new Z80State();
    state.IX = 0xFFFF;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xDD, 0x2C);

    assertEquals(0xFF00, cpu.getRegister(Z80.REG_IX));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_INC_IYh() {
    final Z80State state = new Z80State();
    state.IY = 0x1000;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFD, 0x24);

    assertEquals(0x1100, cpu.getRegister(Z80.REG_IY));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_INC_IYl() {
    final Z80State state = new Z80State();
    state.IY = 0xFFFF;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFD, 0x2C);

    assertEquals(0xFF00, cpu.getRegister(Z80.REG_IY));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_INC_IY() {
    final Z80State state = new Z80State();
    state.IY = 0x1000;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFD, 0x23);

    assertEquals(0x1001, cpu.getRegister(Z80.REG_IY));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_DEC_D() {
    final Z80State state = new Z80State();
    state.D = 0x2A;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x15);

    assertEquals(0x29, cpu.getRegister(Z80.REG_D));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_DEC_mHL() {
    final Z80State state = new Z80State();
    state.H = 0x12;
    state.L = 0x34;
    state.F = 0xFF;

    memory[0x1234] = 0x2A;

    final Z80 cpu = executeCommand(state, 0x35);

    assertMemory(0x1234, 0x29);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);
    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_DEC_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x1234;
    state.F = 0xFF;

    memory[0x1239] = 0x2A;

    final Z80 cpu = executeCommand(state, 0xDD, 0x35, 0x05);

    assertMemory(0x1239, 0x29);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_DEC_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x1234;
    state.F = 0xFF;

    memory[0x1239] = 0x2A;

    final Z80 cpu = executeCommand(state, 0xFD, 0x35, 0x05);

    assertMemory(0x1239, 0x29);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_N, cpu.getRegister(Z80.REG_F) & 0xFF);
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_DEC_BC() {
    final Z80State state = new Z80State();
    state.B = 0x10;
    state.C = 0x00;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x0B);

    assertEquals(0x0FFF, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 6);
  }

  @Test
  public void testCommand_DEC_SP() {
    final Z80State state = new Z80State();
    state.SP = 0x1000;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x3B);

    assertEquals(0x0FFF, cpu.getRegister(Z80.REG_SP));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 6);
  }

  @Test
  public void testCommand_DEC_HL() {
    final Z80State state = new Z80State();
    state.H = 0x10;
    state.L = 0x00;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0x2B);

    assertEquals(0x0FFF, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 6);
  }

  @Test
  public void testCommand_DEC_IX() {
    final Z80State state = new Z80State();
    state.IX = 0x1000;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xDD, 0x2B);

    assertEquals(0x0FFF, cpu.getRegister(Z80.REG_IX));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_DEC_IY() {
    final Z80State state = new Z80State();
    state.IY = 0x1000;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFD, 0x2B);

    assertEquals(0x0FFF, cpu.getRegister(Z80.REG_IY));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_RLCA() {
    final Z80State state = new Z80State();
    state.A = 0x88;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0x07);

    assertEquals(0x11, cpu.getRegister(Z80.REG_A));
    assertEquals(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_RLA() {
    final Z80State state = new Z80State();
    state.A = 0x76;
    state.F = Z80.FLAG_C;

    final Z80 cpu = executeCommand(state, 0x17);

    assertEquals(0xED, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(0, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_RRCA() {
    final Z80State state = new Z80State();
    state.A = 0x11;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0x0F);

    assertEquals(0x88, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_RRA() {
    final Z80State state = new Z80State();
    state.A = 0xE1;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0x1F);

    assertEquals(0x70, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_RLC_r() {
    final int[] codes = new int[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x07};
    final int[] regs = new int[] {Z80.REG_B, Z80.REG_C, Z80.REG_D, Z80.REG_E, Z80.REG_H, Z80.REG_L, Z80.REG_A};
    for (int i = 0; i < codes.length; i++) {
      final Z80State state = new Z80State();
      state.A = 0x88;
      state.B = 0x88;
      state.C = 0x88;
      state.D = 0x88;
      state.E = 0x88;
      state.H = 0x88;
      state.L = 0x88;
      state.F = 0x00;

      final Z80 cpu = executeCommand(state, 0xCB, codes[i]);

      assertEquals(0x11, cpu.getRegister(regs[i]));
      assertEquals(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
      assertTacts(cpu, 8);
    }
  }

  @Test
  public void testCommand_RLC_mHL() {
    final Z80State state = new Z80State();
    state.H = 0x12;
    state.L = 0x34;
    state.F = 0x00;

    memory[0x1234] = (byte) 0x88;

    final Z80 cpu = executeCommand(state, 0xCB, 0x06);

    assertMemory(0x1234, 0x11);
    assertEquals(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_RLC_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0x88;

    final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x05, 0x06);

    assertMemory(0x1239, 0x11);
    assertEquals(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_RLC_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0x88;

    final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x05, 0x06);

    assertMemory(0x1239, 0x11);
    assertEquals(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_RL_D() {
    final Z80State state = new Z80State();
    state.D = 0x8F;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0xCB, 0x12);

    assertEquals(0x1E, cpu.getRegister(Z80.REG_D));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_RL_mHL() {
    final Z80State state = new Z80State();
    state.H = 0x12;
    state.L = 0x34;
    state.F = 0x00;

    memory[0x1234] = (byte) 0x8F;

    final Z80 cpu = executeCommand(state, 0xCB, 0x16);

    assertMemory(0x1234, 0x1E);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_RL_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0x8F;

    final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x05, 0x16);

    assertMemory(0x1239, 0x1E);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_RL_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0x8F;

    final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x05, 0x16);

    assertMemory(0x1239, 0x1E);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_RRC_D() {
    final Z80State state = new Z80State();
    state.D = 0x31;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0xCB, 0x0A);

    assertEquals(0x98, cpu.getRegister(Z80.REG_D));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_S, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_RRC_mHL() {
    final Z80State state = new Z80State();
    state.H = 0x12;
    state.L = 0x34;
    state.F = 0x00;

    memory[0x1234] = (byte) 0x31;

    final Z80 cpu = executeCommand(state, 0xCB, 0x0E);

    assertMemory(0x1234, 0x98);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_S, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_RRC_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0x31;

    final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x05, 0x0E);

    assertMemory(0x1239, 0x98);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_S, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_RRC_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0x31;

    final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x05, 0x0E);

    assertMemory(0x1239, 0x98);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_S, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_RR_D() {
    final Z80State state = new Z80State();
    state.D = 0xDD;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0xCB, 0x1A);

    assertEquals(0x6E, cpu.getRegister(Z80.REG_D));
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_RR_mHL() {
    final Z80State state = new Z80State();
    state.H = 0x12;
    state.L = 0x34;
    state.F = 0x00;

    memory[0x1234] = (byte) 0xDD;

    final Z80 cpu = executeCommand(state, 0xCB, 0x1E);

    assertMemory(0x1234, 0x6E);
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_RR_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0xDD;

    final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x05, 0x1E);

    assertMemory(0x1239, 0x6E);
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_RR_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0xDD;

    final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x05, 0x1E);

    assertMemory(0x1239, 0x6E);
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_SLA_D() {
    final Z80State state = new Z80State();
    state.D = 0xB1;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0xCB, 0x22);

    assertEquals(0x62, cpu.getRegister(Z80.REG_D));
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_SLA_A() {
    final Z80State state = new Z80State();
    state.A = 0xB1;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0xCB, 0x27);

    assertEquals(0x62, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_SLA_mHL() {
    final Z80State state = new Z80State();
    state.H = 0x12;
    state.L = 0x34;
    state.F = 0x00;

    memory[0x1234] = (byte) 0xB1;

    final Z80 cpu = executeCommand(state, 0xCB, 0x26);

    assertMemory(0x1234, 0x62);
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_SLA_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0xB1;

    final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x05, 0x26);

    assertMemory(0x1239, 0x62);
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_SLA_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0xB1;

    final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x05, 0x26);

    assertMemory(0x1239, 0x62);
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_SLL_D() {
    final Z80State state = new Z80State();
    state.D = 0xB1;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0xCB, 0x32);

    assertEquals(0x63, cpu.getRegister(Z80.REG_D));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_SLL_mHL() {
    final Z80State state = new Z80State();
    state.H = 0x12;
    state.L = 0x34;
    state.F = 0x00;

    memory[0x1234] = (byte) 0xB1;

    final Z80 cpu = executeCommand(state, 0xCB, 0x36);

    assertMemory(0x1234, 0x63);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_SLL_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0xB1;

    final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x05, 0x36);

    assertMemory(0x1239, 0x63);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_SLL_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x1234;
    state.F = 0x00;

    memory[0x1239] = (byte) 0xB1;

    final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x05, 0x36);

    assertMemory(0x1239, 0x63);
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_SRA_D() {
    final Z80State state = new Z80State();
    state.D = 0xB8;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0xCB, 0x2A);

    assertEquals(0xDC, cpu.getRegister(Z80.REG_D));
    assertFlagsExcludeReserved(Z80.FLAG_S, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_SRA_mHL() {
    final Z80State state = new Z80State();
    state.H = 0x12;
    state.L = 0x34;
    state.F = 0x00;

    memory[0x1234] = (byte) 0xB8;

    final Z80 cpu = executeCommand(state, 0xCB, 0x2E);

    assertMemory(0x1234, 0xDC);
    assertFlagsExcludeReserved(Z80.FLAG_S, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_SRA_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x1234;
    state.F = 0x00;

    memory[0x1232] = (byte) 0xB8;

    final Z80 cpu = executeCommand(state, 0xDD, 0xCB, -2, 0x2E);

    assertMemory(0x1232, 0xDC);
    assertFlagsExcludeReserved(Z80.FLAG_S, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_SRA_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x1234;
    state.F = 0x00;

    memory[0x1232] = (byte) 0xB8;

    final Z80 cpu = executeCommand(state, 0xFD, 0xCB, -2, 0x2E);

    assertMemory(0x1232, 0xDC);
    assertFlagsExcludeReserved(Z80.FLAG_S, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_SRL_D() {
    final Z80State state = new Z80State();
    state.D = 0x8F;
    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0xCB, 0x3A);

    assertEquals(0x47, cpu.getRegister(Z80.REG_D));
    assertEquals(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_SRL_HL() {
    final Z80State state = new Z80State();
    state.H = 0x12;
    state.L = 0x34;
    state.F = 0x00;

    memory[0x1234] = (byte) 0x8F;

    final Z80 cpu = executeCommand(state, 0xCB, 0x3E);

    assertMemory(0x1234, 0x47);
    assertEquals(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_SRL_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x1234;
    state.F = 0x00;

    memory[0x1230] = (byte) 0x8F;

    final Z80 cpu = executeCommand(state, 0xDD, 0xCB, -4, 0x3E);

    assertMemory(0x1230, 0x47);
    assertEquals(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_SRL_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x1234;
    state.F = 0x00;

    memory[0x1230] = (byte) 0x8F;

    final Z80 cpu = executeCommand(state, 0xFD, 0xCB, -4, 0x3E);

    assertMemory(0x1230, 0x47);
    assertEquals(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_RLD() {
    final Z80State state = new Z80State();
    state.A = 0x7A;
    state.H = 0x50;
    state.L = 0x00;

    state.F = 0x00;

    memory[0x5000] = 0x31;

    final Z80 cpu = executeCommand(state, 0xED, 0x6F);

    assertEquals(0x73, cpu.getRegister(Z80.REG_A));
    assertMemory(0x5000, 0x1A);
    assertFlagsExcludeReserved(0, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 18);
  }

  @Test
  public void testCommand_RRD() {
    final Z80State state = new Z80State();
    state.A = 0x84;
    state.H = 0x50;
    state.L = 0x00;

    state.F = 0x00;

    memory[0x5000] = 0x20;

    final Z80 cpu = executeCommand(state, 0xED, 0x67);

    assertEquals(0x80, cpu.getRegister(Z80.REG_A));
    assertMemory(0x5000, 0x42);
    assertEquals(Z80.FLAG_S, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 18);
  }

  @Test
  public void testCommand_BIT_2_reg() {
    final int[] codes = new int[] {0x50, 0x51, 0x52, 0x53, 0x54, 0x55, 0x57};
    final int[] regs = new int[] {Z80.REG_B, Z80.REG_C, Z80.REG_D, Z80.REG_E, Z80.REG_H, Z80.REG_L, Z80.REG_A};
    for (int i = 0; i < codes.length; i++) {
      final Z80State state = new Z80State();
      state.B = (~4) & 0xFF;
      state.C = (~4) & 0xFF;
      state.D = (~4) & 0xFF;
      state.E = (~4) & 0xFF;
      state.H = (~4) & 0xFF;
      state.L = (~4) & 0xFF;
      state.A = (~4) & 0xFF;

      state.F = 0x00;

      final Z80 cpu = executeCommand(state, 0xCB, codes[i]);

      assertEquals((~4) & 0xFF, cpu.getRegister(regs[i]));
      assertFlagsExcludeReserved(Z80.FLAG_Z | Z80.FLAG_PV | Z80.FLAG_H, cpu.getRegister(Z80.REG_F));
      assertTacts(cpu, 8);
    }
  }

  @Test
  public void testCommand_BIT_7_regIX() {
    for (int i = 0; i < 0xFF; i++) {
      final Z80State state = new Z80State();
      state.IX = 0x1010;
      this.memory[0x1011] = (byte) i;

      final int origf = i ^ 0xFF;
      state.F = origf;

      final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x01, 0x7E);

      assertEquals(0x1010, cpu.getRegister(Z80.REG_IX));
      assertMemory(0x1011, i);

      final int flag = (origf & Z80.FLAG_C) | ((i & 0x80) == 0 ? (Z80.FLAG_PV | Z80.FLAG_Z) : 0) | (i & Z80.FLAG_S) | (state.WZ & (Z80.FLAG_X | Z80.FLAG_Y)) | Z80.FLAG_H;

      assertEquals("Value " + i, flag, cpu.getRegister(Z80.REG_F));

      assertTacts(cpu, 20);
    }
  }

  @Test
  public void testCommand_BIT_2_mHL() {
    final Z80State state = new Z80State();
    state.H = 0x50;
    state.L = 0x00;

    this.memory[0x5000] = (byte) 4;

    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0xCB, 0x56);

    assertEquals(Z80.FLAG_H, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 12);
  }

  @Test
  public void testCommand_BIT_2_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x5000;

    this.memory[0x5010] = (byte) 4;

    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x10, 0x56);

    assertEquals(Z80.FLAG_H, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_BIT_2_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x5000;

    this.memory[0x5010] = (byte) 4;

    state.F = 0x00;

    final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x10, 0x56);

    assertEquals(Z80.FLAG_H, cpu.getRegister(Z80.REG_F));
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_SET_4_B() {
    final Z80State state = new Z80State();
    state.B = 0;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xCB, 0xE0);

    assertEquals(0x10, cpu.getRegister(Z80.REG_B));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_SET_4_mHL() {
    final Z80State state = new Z80State();
    state.H = 0x50;
    state.L = 0x00;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xCB, 0xE6);

    assertMemory(0x5000, 0x10);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_SET_4_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x5000;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x10, 0xE6);

    assertMemory(0x5010, 0x10);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_SET_4_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x5000;
    state.F = 0xFF;

    final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x10, 0xE6);

    assertMemory(0x5010, 0x10);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_RES_4_B() {
    final Z80State state = new Z80State();
    state.B = 0xFF;
    state.F = 0x18;

    final Z80 cpu = executeCommand(state, 0xCB, 0xA0);

    assertEquals(0xEF, cpu.getRegister(Z80.REG_B));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_RES_4_mHL() {
    final Z80State state = new Z80State();
    state.H = 0x50;
    state.L = 0x00;
    state.F = 0xFF;

    memory[0x5000] = (byte) 0xFF;

    final Z80 cpu = executeCommand(state, 0xCB, 0xA6);

    assertMemory(0x5000, 0xEF);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 15);
  }

  @Test
  public void testCommand_RES_4_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x5000;
    state.F = 0xFF;

    memory[0x5010] = (byte) 0xFF;

    final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x10, 0xA6);

    assertMemory(0x5010, 0xEF);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_RES_4_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x5000;
    state.F = 0xFF;

    memory[0x5010] = (byte) 0xFF;

    final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x10, 0xA6);

    assertMemory(0x5010, 0xEF);
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 23);
  }

  @Test
  public void testCommand_HALT() {
    final TestBus tb = new TestBus(0, 0, 0x76, 0xFB);
    tb.block(0x38, 0xED, 0x45); //RETN

    final Z80 cpu = new Z80(tb);
    cpu.setRegister(Z80.REG_SP, 0xFFFF);
    cpu.setIM(1);
    cpu.setIFF(true, true);

    assertEquals(Z80.SIGNAL_OUT_ALL_INACTIVE, cpu.getState());

    cpu.step(111, Z80.SIGNAL_IN_ALL_INACTIVE);
    assertTrue((cpu.getState() & Z80.SIGNAL_OUT_nHALT) == 0);
    assertEquals(0, cpu.getRegister(Z80.REG_PC));
    assertTacts(cpu, 4);

    cpu.step(111, Z80.SIGNAL_IN_ALL_INACTIVE);
    assertTrue((cpu.getState() & Z80.SIGNAL_OUT_nHALT) == 0);
    assertEquals(0, cpu.getRegister(Z80.REG_PC));
    assertTacts(cpu, 8);

    cpu.step(111, Z80.SIGNAL_IN_ALL_INACTIVE);
    assertTrue((cpu.getState() & Z80.SIGNAL_OUT_nHALT) == 0);
    assertEquals(0, cpu.getRegister(Z80.REG_PC));
    assertTacts(cpu, 12);

    cpu.step(111, ~Z80.SIGNAL_IN_nINT);
    assertEquals(0x38, cpu.getPC());
    assertEquals(0xFFFD, cpu.getSP());
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());

    // RETN
    cpu.step(111, Z80.SIGNAL_IN_ALL_INACTIVE);
    assertEquals(0x39, cpu.getPC());

    cpu.step(111, Z80.SIGNAL_IN_ALL_INACTIVE);
    assertEquals(0x01, cpu.getPC());
    assertEquals(0xFFFF, cpu.getSP());
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());

    // EI
    cpu.step(111, Z80.SIGNAL_IN_ALL_INACTIVE);
    assertEquals(0x02, cpu.getPC());
    assertTrue(cpu.isIFF1());
    assertTrue(cpu.isIFF2());
  }

  @Test
  public void testCommand_DI() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, 0xF3);

    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());

    assertTacts(cpu, 4);
    assertFlagsNotChanged(state, cpu);
  }

  @Test
  public void testCommand_EI() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, 0xFB);

    assertTrue(cpu.isIFF1());
    assertTrue(cpu.isIFF2());

    assertTacts(cpu, 4);
    assertFlagsNotChanged(state, cpu);
  }

  @Test
  public void testCommand_IM0() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, 0xED, 0x46);

    assertEquals(0, cpu.getIM());

    assertTacts(cpu, 8);
    assertFlagsNotChanged(state, cpu);
  }

  @Test
  public void testCommand_IM1() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, 0xED, 0x56);

    assertEquals(1, cpu.getIM());

    assertTacts(cpu, 8);
    assertFlagsNotChanged(state, cpu);
  }

  @Test
  public void testCommand_IM2() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, 0xED, 0x5E);

    assertEquals(2, cpu.getIM());

    assertTacts(cpu, 8);
    assertFlagsNotChanged(state, cpu);
  }

  @Test
  public void testCommand_CALL_nn() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xCD, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, 0x88);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A, testbus.readMemory(cpu, 111, 0x3001, false, false));
    assertEquals(0x4A, testbus.readMemory(cpu, 111, 0x3000, false, false));
    assertEquals(0x3000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x2135, cpu.getRegister(Z80.REG_PC));

    assertTacts(cpu, 17);
    assertEquals(0x88, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testM1andLastInstructionByte() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xCD, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, 0x88);

    cpu.nextInstruction(111, false, false, false);

    assertEquals("M1 byte", 0xCD, cpu.getLastM1InstructionByte());
    assertEquals("Last command byte", 0x21, cpu.getLastInstructionByte());
  }

  @Test
  public void testCommand_CALL_NZ_nn_false() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xC4, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_Z;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A4A, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x3002, cpu.getRegister(Z80.REG_SP));

    assertTacts(cpu, 10);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_NZ_nn_true() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xC4, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_Z;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A, testbus.readMemory(cpu, 111, 0x3001, false, false));
    assertEquals(0x4A, testbus.readMemory(cpu, 111, 0x3000, false, false));
    assertEquals(0x3000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x2135, cpu.getRegister(Z80.REG_PC));

    assertTacts(cpu, 17);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_Z_nn_false() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xCC, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_Z;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A4A, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x3002, cpu.getRegister(Z80.REG_SP));

    assertTacts(cpu, 10);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_Z_nn_true() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xCC, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_Z;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A, testbus.readMemory(cpu, 111, 0x3001, false, false));
    assertEquals(0x4A, testbus.readMemory(cpu, 111, 0x3000, false, false));
    assertEquals(0x3000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x2135, cpu.getRegister(Z80.REG_PC));

    assertTacts(cpu, 17);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_NC_nn_false() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xD4, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_C;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A4A, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x3002, cpu.getRegister(Z80.REG_SP));

    assertTacts(cpu, 10);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_NC_nn_true() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xD4, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_C;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A, testbus.readMemory(cpu, 111, 0x3001, false, false));
    assertEquals(0x4A, testbus.readMemory(cpu, 111, 0x3000, false, false));
    assertEquals(0x3000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x2135, cpu.getRegister(Z80.REG_PC));

    assertTacts(cpu, 17);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_C_nn_false() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xDC, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_C;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A4A, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x3002, cpu.getRegister(Z80.REG_SP));

    assertTacts(cpu, 10);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_C_nn_true() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xDC, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_C;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A, testbus.readMemory(cpu, 111, 0x3001, false, false));
    assertEquals(0x4A, testbus.readMemory(cpu, 111, 0x3000, false, false));
    assertEquals(0x3000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x2135, cpu.getRegister(Z80.REG_PC));

    assertTacts(cpu, 17);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_PO_nn_false() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xE4, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_PV;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A4A, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x3002, cpu.getRegister(Z80.REG_SP));

    assertTacts(cpu, 10);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_PO_nn_true() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xE4, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_PV;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A, testbus.readMemory(cpu, 111, 0x3001, false, false));
    assertEquals(0x4A, testbus.readMemory(cpu, 111, 0x3000, false, false));
    assertEquals(0x3000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x2135, cpu.getRegister(Z80.REG_PC));

    assertTacts(cpu, 17);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_PE_nn_false() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xEC, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_PV;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A4A, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x3002, cpu.getRegister(Z80.REG_SP));

    assertTacts(cpu, 10);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_PE_nn_true() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xEC, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_PV;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A, testbus.readMemory(cpu, 111, 0x3001, false, false));
    assertEquals(0x4A, testbus.readMemory(cpu, 111, 0x3000, false, false));
    assertEquals(0x3000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x2135, cpu.getRegister(Z80.REG_PC));

    assertTacts(cpu, 17);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_P_nn_false() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xF4, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_S;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A4A, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x3002, cpu.getRegister(Z80.REG_SP));

    assertTacts(cpu, 10);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_P_nn_true() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xF4, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_S;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A, testbus.readMemory(cpu, 111, 0x3001, false, false));
    assertEquals(0x4A, testbus.readMemory(cpu, 111, 0x3000, false, false));
    assertEquals(0x3000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x2135, cpu.getRegister(Z80.REG_PC));

    assertTacts(cpu, 17);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_M_nn_false() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xFC, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_S;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A4A, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x3002, cpu.getRegister(Z80.REG_SP));

    assertTacts(cpu, 10);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_CALL_M_nn_true() {
    final TestBus testbus = new TestBus(0, 0x1A47, 0xFC, 0x35, 0x21);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_S;

    cpu.setRegister(Z80.REG_PC, 0x1A47);
    cpu.setRegister(Z80.REG_SP, 0x3002);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x1A, testbus.readMemory(cpu, 111, 0x3001, false, false));
    assertEquals(0x4A, testbus.readMemory(cpu, 111, 0x3000, false, false));
    assertEquals(0x3000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x2135, cpu.getRegister(Z80.REG_PC));

    assertTacts(cpu, 17);
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));
  }

  @Test
  public void testCommand_RET() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xC9);
    final Z80 cpu = new Z80(testbus);

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, 0x88);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2002, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x18B5, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x88, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_RET_Z_false() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xC8);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_Z;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x3536, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 5);
  }

  @Test
  public void testCommand_RET_Z_true() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xC8);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_Z;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2002, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x18B5, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_RET_NC_false() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xD0);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_C;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x3536, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 5);
  }

  @Test
  public void testCommand_RET_NC_true() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xD0);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_C;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2002, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x18B5, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_RET_C_false() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xD8);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_C;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x3536, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 5);
  }

  @Test
  public void testCommand_RET_C_true() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xD8);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_C;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2002, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x18B5, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_RET_P_false() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xF0);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_S;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x3536, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 5);
  }

  @Test
  public void testCommand_RET_P_true() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xF0);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_S;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2002, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x18B5, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_RET_M_false() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xF8);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_S;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x3536, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 5);
  }

  @Test
  public void testCommand_RET_M_true() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xF8);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_S;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2002, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x18B5, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_RET_PO_false() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xE0);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_PV;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x3536, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 5);
  }

  @Test
  public void testCommand_RET_PO_true() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xE0);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_PV;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2002, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x18B5, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_RET_PE_false() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xE8);
    final Z80 cpu = new Z80(testbus);

    final int flags = ~Z80.FLAG_PV;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2000, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x3536, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 5);
  }

  @Test
  public void testCommand_RET_PE_true() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xE8);
    final Z80 cpu = new Z80(testbus);

    final int flags = Z80.FLAG_PV;

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, flags);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x2002, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x18B5, cpu.getRegister(Z80.REG_PC));
    assertEquals(flags & 0xFF, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_RETI() {
    final TestBus testbus = new TestBus(0, 0x3535, 0xED, 0x4D);
    final Z80 cpu = new Z80(testbus);

    cpu.setIFF(false, true);

    testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
    testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

    cpu.setRegister(Z80.REG_PC, 0x3535);
    cpu.setRegister(Z80.REG_SP, 0x2000);
    cpu.setRegister(Z80.REG_F, 0x88);

    testbus.resetRETIFlag();

    cpu.nextInstruction(111, false, false, false);

    assertTrue(cpu.isIFF1());
    assertTrue(cpu.isIFF2());
    assertEquals(0x2002, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x18B5, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x88, cpu.getRegister(Z80.REG_F));
    assertTrue(testbus.isRETI());

    assertTacts(cpu, 14);
  }

  @Test
  public void testCommand_RETN() {
    final int[][] RETN = new int[][] {
        {0xED, 0x45},
        {0xED, 0x55},
        {0xED, 0x65},
        {0xED, 0x75},
        {0xED, 0x5D},
        {0xED, 0x6D},
        {0xED, 0x7D}
    };

    for (final int[] cmd : RETN) {
      final TestBus testbus = new TestBus(0, 0x3535, cmd);
      final Z80 cpu = new Z80(testbus);

      cpu.setIFF(false, true);

      testbus.writeMemory(cpu, 111, 0x2000, (byte) 0xB5);
      testbus.writeMemory(cpu, 111, 0x2001, (byte) 0x18);

      cpu.setRegister(Z80.REG_PC, 0x3535);
      cpu.setRegister(Z80.REG_SP, 0x2000);
      cpu.setRegister(Z80.REG_F, 0x88);

      testbus.resetRETIFlag();

      cpu.nextInstruction(111, false, false, false);

      assertTrue(cpu.isIFF1());
      assertTrue(cpu.isIFF2());
      assertEquals(0x2002, cpu.getRegister(Z80.REG_SP));
      assertEquals(0x18B5, cpu.getRegister(Z80.REG_PC));
      assertEquals(0x88, cpu.getRegister(Z80.REG_F));
      assertFalse(testbus.isRETI());

      assertTacts(cpu, 14);
    }
  }

  @Test
  public void testCommand_RST_nn() {
    final int[] codes = new int[] {0xC7, 0xCF, 0xD7, 0xDF, 0xE7, 0xEF, 0xF7, 0xFF};
    final int[] address = new int[] {0x0000, 0x0008, 0x0010, 0x0018, 0x0020, 0x0028, 0x0030, 0x0038};
    for (int i = 0; i < codes.length; i++) {
      final TestBus testbus = new TestBus(0, 0x1A47, codes[i]);
      final Z80 cpu = new Z80(testbus);

      cpu.setRegister(Z80.REG_PC, 0x1A47);
      cpu.setRegister(Z80.REG_SP, 0x3002);
      cpu.setRegister(Z80.REG_F, 0x88);

      cpu.nextInstruction(111, false, false, false);

      assertEquals(0x1A, testbus.readMemory(cpu, 111, 0x3001, false, false));
      assertEquals(0x48, testbus.readMemory(cpu, 111, 0x3000, false, false));
      assertEquals(0x3000, cpu.getRegister(Z80.REG_SP));
      assertEquals(address[i], cpu.getRegister(Z80.REG_PC));

      assertTacts(cpu, 11);
      assertEquals(0x88, cpu.getRegister(Z80.REG_F));
    }
  }

  @Test
  public void testCommand_JP_nn() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, false, 0xC3, 0x34, 0x12);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_NC_nn_false() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_C;

    final Z80 cpu = executeCommand(state, false, 0xD2, 0x34, 0x12);

    assertEquals(0x0003, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_NC_nn_true() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_C) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0xD2, 0x34, 0x12);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_C_nn_false() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_C) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0xDA, 0x34, 0x12);

    assertEquals(0x0003, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_C_nn_true() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_C;

    final Z80 cpu = executeCommand(state, false, 0xDA, 0x34, 0x12);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_NZ_nn_false() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_Z;

    final Z80 cpu = executeCommand(state, false, 0xC2, 0x34, 0x12);

    assertEquals(0x0003, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_NZ_nn_true() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_Z) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0xC2, 0x34, 0x12);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_Z_nn_false() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_Z) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0xCA, 0x34, 0x12);

    assertEquals(0x0003, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_Z_nn_true() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_Z;

    final Z80 cpu = executeCommand(state, false, 0xCA, 0x34, 0x12);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_PO_nn_false() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_PV;

    final Z80 cpu = executeCommand(state, false, 0xE2, 0x34, 0x12);

    assertEquals(0x0003, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_PO_nn_true() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_PV) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0xE2, 0x34, 0x12);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_PE_nn_false() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_PV) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0xEA, 0x34, 0x12);

    assertEquals(0x0003, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_PE_nn_true() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_PV;

    final Z80 cpu = executeCommand(state, false, 0xEA, 0x34, 0x12);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_P_nn_false() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_S;

    final Z80 cpu = executeCommand(state, false, 0xF2, 0x34, 0x12);

    assertEquals(0x0003, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_P_nn_true() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_S) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0xF2, 0x34, 0x12);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_M_nn_false() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_S) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0xFA, 0x34, 0x12);

    assertEquals(0x0003, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JP_M_nn_true() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_S;

    final Z80 cpu = executeCommand(state, false, 0xFA, 0x34, 0x12);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_JR_nn() {
    final Z80State state = new Z80State();
    final Z80 cpu = executeCommand(state, false, 0x18, 0x03);

    assertEquals(0x0005, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 12);
  }

  @Test
  public void testCommand_JR_NZ_nn_false() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_Z;

    final Z80 cpu = executeCommand(state, false, 0x20, 0x03);

    assertEquals(0x0002, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_JR_NZ_nn_true() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_Z) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0x20, 0x03);

    assertEquals(0x0005, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 12);
  }

  @Test
  public void testCommand_JR_Z_nn_false() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_Z) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0x28, 0x03);

    assertEquals(0x0002, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_JR_Z_nn_true() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_Z;

    final Z80 cpu = executeCommand(state, false, 0x28, 0x03);

    assertEquals(0x0005, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 12);
  }

  @Test
  public void testCommand_JR_NC_nn_false() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_C;

    final Z80 cpu = executeCommand(state, false, 0x30, 0x03);

    assertEquals(0x0002, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_JR_NC_nn_true() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_C) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0x30, 0x03);

    assertEquals(0x0005, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 12);
  }

  @Test
  public void testCommand_JR_C_nn_false() {
    final Z80State state = new Z80State();
    state.F = (~Z80.FLAG_C) & 0xFF;

    final Z80 cpu = executeCommand(state, false, 0x38, 0x03);

    assertEquals(0x0002, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 7);
  }

  @Test
  public void testCommand_JR_C_nn_true() {
    final Z80State state = new Z80State();
    state.F = Z80.FLAG_C;

    final Z80 cpu = executeCommand(state, false, 0x38, 0x03);

    assertEquals(0x0005, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 12);
  }

  @Test
  public void testCommand_JP_rHL() {
    final Z80State state = new Z80State();

    state.H = 0x12;
    state.L = 0x34;

    final Z80 cpu = executeCommand(state, false, 0xE9);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 4);
  }

  @Test
  public void testCommand_JP_rIX() {
    final Z80State state = new Z80State();

    state.IX = 0x1234;

    final Z80 cpu = executeCommand(state, false, 0xDD, 0xE9);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_JP_rIY() {
    final Z80State state = new Z80State();

    state.IY = 0x1234;

    final Z80 cpu = executeCommand(state, false, 0xFD, 0xE9);

    assertEquals(0x1234, cpu.getRegister(Z80.REG_PC));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_DJNZ_false() {
    final Z80State state = new Z80State();

    state.B = 5;

    final Z80 cpu = executeCommand(state, false, 0x10, 0x03);

    assertEquals(0x0005, cpu.getRegister(Z80.REG_PC));
    assertEquals(4, cpu.getRegister(Z80.REG_B));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 13);
  }

  @Test
  public void testCommand_DJNZ_true() {
    final Z80State state = new Z80State();

    state.B = 1;

    final Z80 cpu = executeCommand(state, false, 0x10, 0x03);

    assertEquals(0x0002, cpu.getRegister(Z80.REG_PC));
    assertEquals(0, cpu.getRegister(Z80.REG_B));
    assertFlagsNotChanged(state, cpu);

    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_IN_A_n() {
    final TestBus tb = new TestBus(0, 0, 0xDB, 0x01);
    tb.writePort(null, 111, 0x2301, (byte) 0x7B);

    final Z80 cpu = new Z80(tb);
    cpu.setRegister(Z80.REG_A, 0x23);
    cpu.setRegister(Z80.REG_F, 0x00);
    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x7B, cpu.getRegister(Z80.REG_A));
    assertEquals(0x00, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_IN_r_mC() {
    final int[] codes = new int[] {0x78, 0x40, 0x48, 0x50, 0x58, 0x60, 0x68};
    final int[] regs = new int[] {Z80.REG_A, Z80.REG_B, Z80.REG_C, Z80.REG_D, Z80.REG_E, Z80.REG_H, Z80.REG_L};

    for (int i = 0; i < codes.length; i++) {
      final TestBus tb = new TestBus(0, 0, 0xED, codes[i]);
      tb.writePort(null, 111, 0x1007, (byte) 0x7B);

      final Z80 cpu = new Z80(tb);
      cpu.setRegister(regs[i], 0x88);
      cpu.setRegister(Z80.REG_C, 0x07);
      cpu.setRegister(Z80.REG_B, 0x10);
      cpu.setRegister(Z80.REG_F, 0xFF);
      cpu.nextInstruction(111, false, false, false);

      assertEquals(0x0002, cpu.getRegister(Z80.REG_PC));
      assertEquals(0x7B, cpu.getRegister(regs[i]));
      assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));

      assertTacts(cpu, 12);
    }
  }

  @Test
  public void testCommand_IN_C() {
    final TestBus tb = new TestBus(0, 0, 0xED, 0x70);
    tb.writePort(null, 111, 0x1007, (byte) 0x7B);

    final Z80 cpu = new Z80(tb);
    cpu.setRegister(Z80.REG_A, 0x88);
    cpu.setRegister(Z80.REG_C, 0x07);
    cpu.setRegister(Z80.REG_B, 0x10);
    cpu.setRegister(Z80.REG_F, 0xFF);
    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x88, cpu.getRegister(Z80.REG_A));
    assertFlagsExcludeReserved(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 12);
  }

  @Test
  public void testCommand_INI() {
    final TestBus tb = new TestBus(0, 0, 0xED, 0xA2);
    tb.writePort(null, 111, 0x1007, (byte) 0x7B);

    final Z80 cpu = new Z80(tb);
    cpu.setRegister(Z80.REG_A, 0x88);
    cpu.setRegister(Z80.REG_C, 0x07);
    cpu.setRegister(Z80.REG_B, 0x10);
    cpu.setRegister(Z80.REG_H, 0x10);
    cpu.setRegister(Z80.REG_L, 0x00);

    cpu.setRegister(Z80.REG_F, 0x00);
    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x7B, tb.readMemory(cpu, 111, 0x1000, false, false) & 0xFF);
    assertEquals(0x1001, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0F, cpu.getRegister(Z80.REG_B));
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommandINIR() {
    final TestBus tb = new TestBus(0, 0x0000);

    tb.writePort(null, 111, 0x0307, (byte) 0x51);
    tb.writePort(null, 111, 0x0207, (byte) 0xA9);
    tb.writePort(null, 111, 0x0107, (byte) 0x03);

    final Z80State state = new Z80State();
    state.C = 0x07;
    state.B = 0x03;
    state.H = 0x10;
    state.L = 0x00;
    state.F = 0x00;

    final Z80 cpu = executeRepeatingBlockCommand(state, tb, 0xED, 0xB2);

    assertEquals(0x51, tb.readMemory(cpu, 111, 0x1000, false, false) & 0xFF);
    assertEquals(0xA9, tb.readMemory(cpu, 111, 0x1001, false, false) & 0xFF);
    assertEquals(0x03, tb.readMemory(cpu, 111, 0x1002, false, false) & 0xFF);

    assertEquals(0x1003, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0007, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertFlagsExcludeReserved(Z80.FLAG_Z, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 58);
  }

  @Test
  public void testCommand_IND() {
    final TestBus tb = new TestBus(0, 0, 0xED, 0xAA);
    tb.writePort(null, 111, 0x1007, (byte) 0x7B);

    final Z80 cpu = new Z80(tb);
    cpu.setRegister(Z80.REG_A, 0x88);
    cpu.setRegister(Z80.REG_C, 0x07);
    cpu.setRegister(Z80.REG_B, 0x10);
    cpu.setRegister(Z80.REG_H, 0x10);
    cpu.setRegister(Z80.REG_L, 0x00);

    cpu.setRegister(Z80.REG_F, 0x00);
    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x7B, tb.readMemory(cpu, 111, 0x1000, false, false) & 0xFF);
    assertEquals(0x0FFF, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0F, cpu.getRegister(Z80.REG_B));
    assertFlagsExcludeReserved(Z80.FLAG_H | Z80.FLAG_C, cpu.getRegister(Z80.REG_F));
    assertEquals(2, cpu.getRegister(Z80.REG_PC));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_INDR() {
    final TestBus tb = new TestBus(0, 0x0000);

    tb.writePort(null, 111, 0x0307, (byte) 0x51);
    tb.writePort(null, 111, 0x0207, (byte) 0xA9);
    tb.writePort(null, 111, 0x0107, (byte) 0x03);

    final Z80State state = new Z80State();
    state.C = 0x07;
    state.B = 0x03;
    state.H = 0x10;
    state.L = 0x00;
    state.F = 0x00;

    final Z80 cpu = executeRepeatingBlockCommand(state, tb, 0xED, 0xBA);

    assertEquals(0x51, tb.readMemory(cpu, 111, 0x1000, false, false) & 0xFF);
    assertEquals(0xA9, tb.readMemory(cpu, 111, 0x0FFF, false, false) & 0xFF);
    assertEquals(0x03, tb.readMemory(cpu, 111, 0x0FFE, false, false) & 0xFF);

    assertEquals(0x0FFD, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0007, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertFlagsExcludeReserved(Z80.FLAG_Z | Z80.FLAG_C | Z80.FLAG_H, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 58);
  }

  @Test
  public void testCommand_OUT_mn_A() {
    final TestBus tb = new TestBus(0, 0, 0xD3, 0x01);

    final Z80 cpu = new Z80(tb);
    cpu.setRegister(Z80.REG_A, 0x23);
    cpu.setRegister(Z80.REG_F, 0x88);
    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x02, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x23, tb.readPort(cpu, 111, 0x2301));
    assertEquals(0x23, cpu.getRegister(Z80.REG_A));
    assertEquals(0x88, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_UndocumentedOUT_mC_0() {
    final TestBus tb = new TestBus(0, 0, 0xED, 0x71);

    final Z80 cpu = new Z80(tb);
    cpu.setRegister(Z80.REG_C, 0x23);
    tb.writePort(cpu, 111, 0x23, (byte) 0xFF);

    assertEquals(0xFF, tb.readPort(cpu, 111, 0x23) & 0xFF);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x02, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x00, tb.readPort(cpu, 111, 0x23));

    assertTacts(cpu, 12);
  }

  @Test
  public void testCommand_OUT_mC_r() {
    final int[] codes = new int[] {0x79, 0x51, 0x59, 0x61, 0x69};
    final int[] regs = new int[] {Z80.REG_A, Z80.REG_D, Z80.REG_E, Z80.REG_H, Z80.REG_L};

    for (int i = 0; i < codes.length; i++) {
      final TestBus tb = new TestBus(0, 0, 0xED, codes[i]);

      final Z80 cpu = new Z80(tb);
      cpu.setRegister(regs[i], 0x15);
      cpu.setRegister(Z80.REG_PC, 0);
      cpu.setRegister(Z80.REG_C, 0x07);
      cpu.setRegister(Z80.REG_B, 0x10);
      cpu.setRegister(Z80.REG_F, 0x88);
      cpu.nextInstruction(111, false, false, false);

      assertEquals("index " + i, 0x0002, cpu.getRegister(Z80.REG_PC));
      assertEquals("index " + i, 0x15, tb.readPort(cpu, 111, 0x1007) & 0xFF);
      assertEquals("index " + i, 0x15, cpu.getRegister(regs[i]));
      assertEquals("index " + i, 0x1007, cpu.getRegisterPair(Z80.REGPAIR_BC));
      assertEquals("index " + i, 0x88, cpu.getRegister(Z80.REG_F));

      assertTacts(cpu, 12);
    }
  }

  @Test
  public void testCommand_OUT_mC_0() {
    final TestBus tb = new TestBus(0, 0, 0xED, 0x71);

    final Z80 cpu = new Z80(tb);
    tb.writePort(cpu, 111, 0x1007, (byte) 0x67);
    cpu.setRegister(Z80.REG_PC, 0);
    cpu.setRegister(Z80.REG_C, 0x07);
    cpu.setRegister(Z80.REG_B, 0x10);
    cpu.setRegister(Z80.REG_F, 0x88);
    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x0002, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x00, tb.readPort(cpu, 111, 0x1007) & 0xFF);
    assertEquals(0x1007, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertEquals(0x88, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 12);
  }

  @Test
  public void testCommand_OUTI() {
    final TestBus tb = new TestBus(0, 0, 0xED, 0xA3);

    final Z80 cpu = new Z80(tb);
    cpu.setRegister(Z80.REG_C, 0x07);
    cpu.setRegister(Z80.REG_B, 0x10);
    cpu.setRegister(Z80.REG_H, 0x10);
    cpu.setRegister(Z80.REG_L, 0x00);
    cpu.setRegister(Z80.REG_F, 0x00);
    tb.writeMemory(cpu, 111, 0x1000, (byte) 0x59);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x02, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x59, tb.readPort(cpu, 111, 0x1007) & 0xFF);
    assertEquals(0x0F, cpu.getRegister(Z80.REG_B));
    assertEquals(0x07, cpu.getRegister(Z80.REG_C));
    assertEquals(0x1001, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertFlagsExcludeReserved(Z80.FLAG_C, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_OUTIR() {
    final TestBus tb = new TestBus(0, 0x0000);

    tb.writeMemory(null, 111, 0x1000, (byte) 0x51);
    tb.writeMemory(null, 111, 0x1001, (byte) 0xA9);
    tb.writeMemory(null, 111, 0x1002, (byte) 0x03);

    final Z80State state = new Z80State();
    state.C = 0x07;
    state.B = 0x03;
    state.H = 0x10;
    state.L = 0x00;
    state.F = 0x00;

    final Z80 cpu = executeRepeatingBlockCommand(state, tb, 0xED, 0xB3);

    assertEquals(0x51, tb.readPort(cpu, 111, 0x0307) & 0xFF);
    assertEquals(0xA9, tb.readPort(cpu, 111, 0x0207) & 0xFF);
    assertEquals(0x03, tb.readPort(cpu, 111, 0x0107) & 0xFF);

    assertEquals(0x1003, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0007, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertFlagsExcludeReserved(Z80.FLAG_Z | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 58);
  }

  @Test
  public void testCommand_OUTD() {
    final TestBus tb = new TestBus(0, 0, 0xED, 0xAB);

    final Z80 cpu = new Z80(tb);
    cpu.setRegister(Z80.REG_C, 0x07);
    cpu.setRegister(Z80.REG_B, 0x10);
    cpu.setRegister(Z80.REG_H, 0x10);
    cpu.setRegister(Z80.REG_L, 0x00);
    cpu.setRegister(Z80.REG_F, 0x00);
    tb.writeMemory(cpu, 111, 0x1000, (byte) 0x59);

    cpu.nextInstruction(111, false, false, false);

    assertEquals(0x02, cpu.getRegister(Z80.REG_PC));
    assertEquals(0x59, tb.readPort(cpu, 111, 0x1007) & 0xFF);
    assertEquals(0x0F, cpu.getRegister(Z80.REG_B));
    assertEquals(0x07, cpu.getRegister(Z80.REG_C));
    assertEquals(0x0FFF, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertFlagsExcludeReserved(Z80.FLAG_H | Z80.FLAG_PV | Z80.FLAG_C, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_OUTDR() {
    final TestBus tb = new TestBus(0, 0x0000);

    tb.writeMemory(null, 111, 0x1000, (byte) 0x51);
    tb.writeMemory(null, 111, 0x0FFF, (byte) 0xA9);
    tb.writeMemory(null, 111, 0x0FFE, (byte) 0x03);

    final Z80State state = new Z80State();
    state.C = 0x07;
    state.B = 0x03;
    state.H = 0x10;
    state.L = 0x00;
    state.F = 0x00;

    final Z80 cpu = executeRepeatingBlockCommand(state, tb, 0xED, 0xBB);

    assertEquals(0x51, tb.readPort(cpu, 111, 0x0307) & 0xFF);
    assertEquals(0xA9, tb.readPort(cpu, 111, 0x0207) & 0xFF);
    assertEquals(0x03, tb.readPort(cpu, 111, 0x0107) & 0xFF);

    assertEquals(0x0FFD, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x0007, cpu.getRegisterPair(Z80.REGPAIR_BC));
    assertFlagsExcludeReserved(Z80.FLAG_Z | Z80.FLAG_PV | Z80.FLAG_H | Z80.FLAG_C, cpu.getRegister(Z80.REG_F));

    assertTacts(cpu, 58);
  }

  @Test
  public void testCommand_RLC_mIXd_r() {
    final int[] codes = new int[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x07};
    final int[] regs = new int[] {Z80.REG_B, Z80.REG_C, Z80.REG_D, Z80.REG_E, Z80.REG_H, Z80.REG_L, Z80.REG_A};

    for (int i = 0; i < codes.length; i++) {
      final Z80State state = new Z80State();
      state.A = 0x00;
      state.B = 0x00;
      state.C = 0x00;
      state.D = 0x00;
      state.E = 0x00;
      state.H = 0x00;
      state.L = 0x00;
      state.F = 0x00;

      state.IX = 0x1000;

      this.memory[0x1005] = (byte) 0x88;

      final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x05, codes[i]);

      assertEquals(0x11, cpu.getRegister(regs[i]));
      assertMemory(0x1005, 0x11);
      assertEquals(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
      assertTacts(cpu, 23);
    }
  }

  @Test
  public void testCommand_RLC_mIYd_r() {
    final int[] codes = new int[] {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x07};
    final int[] regs = new int[] {Z80.REG_B, Z80.REG_C, Z80.REG_D, Z80.REG_E, Z80.REG_H, Z80.REG_L, Z80.REG_A};

    for (int i = 0; i < codes.length; i++) {
      final Z80State state = new Z80State();
      state.A = 0x00;
      state.B = 0x00;
      state.C = 0x00;
      state.D = 0x00;
      state.E = 0x00;
      state.H = 0x00;
      state.L = 0x00;
      state.F = 0x00;

      state.IY = 0x1000;

      this.memory[0x1005] = (byte) 0x88;

      final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x05, codes[i]);

      assertEquals(0x11, cpu.getRegister(regs[i]));
      assertMemory(0x1005, 0x11);
      assertEquals(Z80.FLAG_C | Z80.FLAG_PV, cpu.getRegister(Z80.REG_F));
      assertTacts(cpu, 23);
    }
  }

  @Test
  public void testCommand_RES_4_mIXd_r() {
    final int[] codes = new int[] {0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA7};
    final int[] regs = new int[] {Z80.REG_B, Z80.REG_C, Z80.REG_D, Z80.REG_E, Z80.REG_H, Z80.REG_L, Z80.REG_A};

    for (int i = 0; i < codes.length; i++) {
      final Z80State state = new Z80State();
      state.A = 0x00;
      state.B = 0x00;
      state.C = 0x00;
      state.D = 0x00;
      state.E = 0x00;
      state.H = 0x00;
      state.L = 0x00;
      state.F = 0x00;

      state.IX = 0x1000;

      this.memory[0x1005] = (byte) 0xFF;

      final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x05, codes[i]);

      assertEquals(0xEF, cpu.getRegister(regs[i]));
      assertMemory(0x1005, 0xEF);
      assertFlagsNotChanged(state, cpu);
      assertTacts(cpu, 23);
    }
  }

  @Test
  public void testCommand_RES_4_mIYd_r() {
    final int[] codes = new int[] {0xA0, 0xA1, 0xA2, 0xA3, 0xA4, 0xA5, 0xA7};
    final int[] regs = new int[] {Z80.REG_B, Z80.REG_C, Z80.REG_D, Z80.REG_E, Z80.REG_H, Z80.REG_L, Z80.REG_A};

    for (int i = 0; i < codes.length; i++) {
      final Z80State state = new Z80State();
      state.A = 0x00;
      state.B = 0x00;
      state.C = 0x00;
      state.D = 0x00;
      state.E = 0x00;
      state.H = 0x00;
      state.L = 0x00;
      state.F = 0x00;

      state.IY = 0x1000;

      this.memory[0x1005] = (byte) 0xFF;

      final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x05, codes[i]);

      assertEquals(0xEF, cpu.getRegister(regs[i]));
      assertMemory(0x1005, 0xEF);
      assertFlagsNotChanged(state, cpu);
      assertTacts(cpu, 23);
    }
  }

  @Test
  public void testCommand_SET_4_mIXd_r() {
    final int[] codes = new int[] {0xE0, 0xE1, 0xE2, 0xE3, 0xE4, 0xE5, 0xE7};
    final int[] regs = new int[] {Z80.REG_B, Z80.REG_C, Z80.REG_D, Z80.REG_E, Z80.REG_H, Z80.REG_L, Z80.REG_A};

    for (int i = 0; i < codes.length; i++) {
      final Z80State state = new Z80State();
      state.A = 0x00;
      state.B = 0x00;
      state.C = 0x00;
      state.D = 0x00;
      state.E = 0x00;
      state.H = 0x00;
      state.L = 0x00;
      state.F = 0x00;

      state.IX = 0x1000;

      this.memory[0x1005] = (byte) 0x00;

      final Z80 cpu = executeCommand(state, 0xDD, 0xCB, 0x05, codes[i]);

      assertEquals(0x10, cpu.getRegister(regs[i]));
      assertMemory(0x1005, 0x10);
      assertFlagsNotChanged(state, cpu);
      assertTacts(cpu, 23);
    }
  }

  @Test
  public void testCommand_SET_4_mIYd_r() {
    final int[] codes = new int[] {0xE0, 0xE1, 0xE2, 0xE3, 0xE4, 0xE5, 0xE7};
    final int[] regs = new int[] {Z80.REG_B, Z80.REG_C, Z80.REG_D, Z80.REG_E, Z80.REG_H, Z80.REG_L, Z80.REG_A};

    for (int i = 0; i < codes.length; i++) {
      final Z80State state = new Z80State();
      state.A = 0x00;
      state.B = 0x00;
      state.C = 0x00;
      state.D = 0x00;
      state.E = 0x00;
      state.H = 0x00;
      state.L = 0x00;
      state.F = 0x00;

      state.IY = 0x1000;

      this.memory[0x1005] = (byte) 0x00;

      final Z80 cpu = executeCommand(state, 0xFD, 0xCB, 0x05, codes[i]);

      assertEquals(0x10, cpu.getRegister(regs[i]));
      assertMemory(0x1005, 0x10);
      assertFlagsNotChanged(state, cpu);
      assertTacts(cpu, 23);
    }
  }

  @Test
  public void testCommand_LD_IYh_D() {
    final Z80State state = new Z80State();
    state.D = 0xDD;
    final Z80 cpu = executeCommand(state, 0xFD, 0x62);
    assertEquals(0xDD00, cpu.getRegister(Z80.REG_IY));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_IYl_D() {
    final Z80State state = new Z80State();
    state.D = 0xDD;
    final Z80 cpu = executeCommand(state, 0xFD, 0x6A);
    assertEquals(0x00DD, cpu.getRegister(Z80.REG_IY));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_D_IYh() {
    final Z80State state = new Z80State();
    state.D = 0xEE;
    state.IY = 0x1234;
    final Z80 cpu = executeCommand(state, 0xFD, 0x54);
    assertEquals(0x12, cpu.getRegister(Z80.REG_D));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_D_IYl() {
    final Z80State state = new Z80State();
    state.D = 0xEE;
    state.IY = 0x1234;
    final Z80 cpu = executeCommand(state, 0xFD, 0x55);
    assertEquals(0x34, cpu.getRegister(Z80.REG_D));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_IXh_D() {
    final Z80State state = new Z80State();
    state.D = 0xDD;
    final Z80 cpu = executeCommand(state, 0xDD, 0x62);
    assertEquals(0xDD00, cpu.getRegister(Z80.REG_IX));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_IXh_nn() {
    final Z80State state = new Z80State();
    state.IX = 0xFFFF;
    final Z80 cpu = executeCommand(state, 0xDD, 0x26, 0x12);
    assertEquals(0x12FF, cpu.getRegister(Z80.REG_IX));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_LD_IXl_nn() {
    final Z80State state = new Z80State();
    state.IX = 0xFFFF;
    final Z80 cpu = executeCommand(state, 0xDD, 0x2E, 0x12);
    assertEquals(0xFF12, cpu.getRegister(Z80.REG_IX));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_LD_IYh_nn() {
    final Z80State state = new Z80State();
    state.IY = 0xFFFF;
    final Z80 cpu = executeCommand(state, 0xFD, 0x26, 0x12);
    assertEquals(0x12FF, cpu.getRegister(Z80.REG_IY));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_LD_IYl_nn() {
    final Z80State state = new Z80State();
    state.IY = 0xFFFF;
    final Z80 cpu = executeCommand(state, 0xFD, 0x2E, 0x12);
    assertEquals(0xFF12, cpu.getRegister(Z80.REG_IY));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 11);
  }

  @Test
  public void testCommand_LD_IXl_D() {
    final Z80State state = new Z80State();
    state.D = 0xDD;
    final Z80 cpu = executeCommand(state, 0xDD, 0x6A);
    assertEquals(0x00DD, cpu.getRegister(Z80.REG_IX));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_D_IXh() {
    final Z80State state = new Z80State();
    state.D = 0xEE;
    state.IX = 0x1234;
    final Z80 cpu = executeCommand(state, 0xDD, 0x54);
    assertEquals(0x12, cpu.getRegister(Z80.REG_D));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_D_IXl() {
    final Z80State state = new Z80State();
    state.D = 0xEE;
    state.IX = 0x1234;
    final Z80 cpu = executeCommand(state, 0xDD, 0x55);
    assertEquals(0x34, cpu.getRegister(Z80.REG_D));
    assertFlagsNotChanged(state, cpu);
    assertTacts(cpu, 8);
  }

  @Test
  public void testCommand_LD_m_HL() {
    final Z80State state = new Z80State();
    state.H = 0xCA;
    state.L = 0xFE;
    final Z80 cpu = executeCommand(state, 0xED, 0x63, 0x34, 0x12);
    assertFlagsNotChanged(state, cpu);
    assertMemory(0x1234, 0xFE);
    assertMemory(0x1235, 0xCA);
    assertTacts(cpu, 20);
  }

  @Test
  public void testCommand_LD_m_HL_2() {
    final Z80State state = new Z80State();
    state.H = 0xCA;
    state.L = 0xFE;
    final Z80 cpu = executeCommand(state, 0x22, 0x34, 0x12);

    assertEquals("M1 byte", 0x22, cpu.getLastM1InstructionByte());
    assertEquals("Last command byte", 0x12, cpu.getLastInstructionByte());

    assertFlagsNotChanged(state, cpu);
    assertMemory(0x1234, 0xFE);
    assertMemory(0x1235, 0xCA);
    assertTacts(cpu, 16);
  }

  @Test
  public void testCommand_LD_L_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x25AF;
    this.memory[0x25C8] = (byte) 0xFE;
    final Z80 cpu = executeCommand(state, 0xFD, 0x6E, 0x19);
    assertFlagsNotChanged(state, cpu);
    assertEquals(0x00, cpu.getRegister(Z80.REG_H));
    assertEquals(0xFE, cpu.getRegister(Z80.REG_L));
    assertEquals(0x25AF, cpu.getRegister(Z80.REG_IY));
    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_LD_H_mIYd() {
    final Z80State state = new Z80State();
    state.IY = 0x25AF;
    this.memory[0x25C8] = (byte) 0xFE;
    final Z80 cpu = executeCommand(state, 0xFD, 0x66, 0x19);
    assertFlagsNotChanged(state, cpu);
    assertEquals(0xFE, cpu.getRegister(Z80.REG_H));
    assertEquals(0x00, cpu.getRegister(Z80.REG_L));
    assertEquals(0x25AF, cpu.getRegister(Z80.REG_IY));
    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_LD_L_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x25AF;
    this.memory[0x25C8] = (byte) 0xFE;
    final Z80 cpu = executeCommand(state, 0xDD, 0x6E, 0x19);
    assertFlagsNotChanged(state, cpu);
    assertEquals(0x00, cpu.getRegister(Z80.REG_H));
    assertEquals(0xFE, cpu.getRegister(Z80.REG_L));
    assertEquals(0x25AF, cpu.getRegister(Z80.REG_IX));
    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_LD_H_mIXd() {
    final Z80State state = new Z80State();
    state.IX = 0x25AF;
    this.memory[0x25C8] = (byte) 0xFE;
    final Z80 cpu = executeCommand(state, 0xDD, 0x66, 0x19);
    assertFlagsNotChanged(state, cpu);
    assertEquals(0xFE, cpu.getRegister(Z80.REG_H));
    assertEquals(0x00, cpu.getRegister(Z80.REG_L));
    assertEquals(0x25AF, cpu.getRegister(Z80.REG_IX));
    assertTacts(cpu, 19);
  }

  @Test
  public void testCommand_LD_SP_HL() {
    final Z80State state = new Z80State();
    state.L = 0xAF;
    state.H = 0x25;
    final Z80 cpu = executeCommand(state, 0xF9);
    assertFlagsNotChanged(state, cpu);
    assertEquals(0x25AF, cpu.getRegisterPair(Z80.REGPAIR_HL));
    assertEquals(0x25AF, cpu.getRegister(Z80.REG_SP));
    assertTacts(cpu, 6);
  }

  @Test
  public void testCommand_LD_SP_IX() {
    final Z80State state = new Z80State();
    state.IX = 0x25AF;
    final Z80 cpu = executeCommand(state, 0xDD, 0xF9);
    assertFlagsNotChanged(state, cpu);
    assertEquals(0x25AF, cpu.getRegister(Z80.REG_IX));
    assertEquals(0x25AF, cpu.getRegister(Z80.REG_SP));
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_LD_SP_IY() {
    final Z80State state = new Z80State();
    state.IY = 0x25AF;
    final Z80 cpu = executeCommand(state, 0xFD, 0xF9);
    assertFlagsNotChanged(state, cpu);
    assertEquals(0x25AF, cpu.getRegister(Z80.REG_IY));
    assertEquals(0x25AF, cpu.getRegister(Z80.REG_SP));
    assertTacts(cpu, 10);
  }

  @Test
  public void testCommand_LD_mIYd_H() {
    final Z80State state = new Z80State();
    state.IY = 0x1000;
    state.H = 0x34;
    final Z80 cpu = executeCommand(state, 0xFD, 0x74, 0x57);
    assertFlagsNotChanged(state, cpu);
    assertMemory(0x1057, 0x34);
    assertTacts(cpu, 19);
  }

  @Test
  public void testIntProcessing_IM0_DefaultAfterReset() {
    final Z80 cpu = new Z80(new Z80CPUBus() {

      @Override
      public int readRegPortAddr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
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
      public int readPtr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
      }

      @Override
      public int readSpecRegValue(Z80 cpu, int ctx, int reg, int origValue) {
        return origValue;
      }

      @Override
      public int readSpecRegPairValue(Z80 cpu, int ctx, int regPair, int origValue) {
        return origValue;
      }

      @Override
      public byte readPort(Z80 cpu, int ctx, int port) {
        return 0;
      }

      @Override
      public void writePort(Z80 cpu, int ctx, int port, byte data) {

      }

      @Override
      public byte onCPURequestDataLines(Z80 cpu, int ctx) {
        return (byte) 0x08;
      }

      @Override
      public void onRETI(Z80 cpu, int ctx) {
      }
    });

    cpu.doReset();
    cpu.setRegister(Z80.REG_SP, 0xFFFF);

    fillMemory(0, 0xFB, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0);

    final int INT = ~Z80.SIGNAL_IN_nINT;

    cpu.setRegister(Z80.REG_A, 0xAA);
    cpu.setRegister(Z80.REG_A, 0xBB, true);

    assertEquals(0, cpu.getIM());
    assertEquals(0, cpu.getRegister(Z80.REG_R));
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());

    cpu.step(111, INT);
    assertEquals(1, cpu.getPC());
    assertEquals(0xFFFF, cpu.getSP());
    assertTrue(cpu.isIFF1());
    assertTrue(cpu.isIFF2());

    cpu.step(111, INT);
    assertEquals(2, cpu.getPC());
    assertEquals(0xFFFF, cpu.getSP());
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());
    assertEquals(0xBB, cpu.getRegister(Z80.REG_A));
  }

  @Test
  public void testIntProcessing_IM1() {
    final Z80 cpu = new Z80(new Z80CPUBus() {

      @Override
      public int readRegPortAddr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
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
      public int readPtr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
      }

      @Override
      public int readSpecRegValue(Z80 cpu, int ctx, int reg, int origValue) {
        return origValue;
      }

      @Override
      public int readSpecRegPairValue(Z80 cpu, int ctx, int regPair, int origValue) {
        return origValue;
      }

      @Override
      public byte readPort(Z80 cpu, int ctx, int port) {
        return 0;
      }

      @Override
      public void writePort(Z80 cpu, int ctx, int port, byte data) {

      }

      @Override
      public byte onCPURequestDataLines(Z80 cpu, int ctx) {
        return (byte) 0x08;
      }

      @Override
      public void onRETI(Z80 cpu, int ctx) {
      }
    });

    cpu.doReset();
    cpu.setRegister(Z80.REG_SP, 0xFFFF);

    final int INT = ~Z80.SIGNAL_IN_nINT;

    fillMemory(0x00, 0xFB); // EI at 0x00
    fillMemory(0x38, 0xED, 0x4D);//RETI at 0x38

    cpu.setRegister(Z80.REG_A, 0xAA);
    cpu.setRegister(Z80.REG_A, 0xBB, true);
    cpu.setIM(1);

    assertEquals(1, cpu.getIM());
    assertEquals(0, cpu.getRegister(Z80.REG_R));
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());

    cpu.step(111, INT);
    assertEquals(1, cpu.getPC());
    assertEquals(0xFFFF, cpu.getSP());
    assertTrue(cpu.isIFF1());
    assertTrue(cpu.isIFF2());

    cpu.step(111, INT);
    assertEquals(0x38, cpu.getPC());
    assertEquals(0xFFFD, cpu.getSP());
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());

    //RETI
    cpu.step(111, INT);
    assertEquals(0x39, cpu.getPC());

    cpu.step(111, Z80.SIGNAL_IN_ALL_INACTIVE);
    assertEquals(0x02, cpu.getPC());
    assertEquals(0xFFFF, cpu.getSP());
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());
  }

  @Test
  public void testIntProcessing_IM2() {
    final Z80 cpu = new Z80(new Z80CPUBus() {

      @Override
      public int readRegPortAddr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
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
      public int readPtr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
      }

      @Override
      public int readSpecRegValue(Z80 cpu, int ctx, int reg, int origValue) {
        return origValue;
      }

      @Override
      public int readSpecRegPairValue(Z80 cpu, int ctx, int regPair, int origValue) {
        return origValue;
      }

      @Override
      public byte readPort(Z80 cpu, int ctx, int port) {
        return 0;
      }

      @Override
      public void writePort(Z80 cpu, int ctx, int port, byte data) {

      }

      @Override
      public byte onCPURequestDataLines(Z80 cpu, int ctx) {
        return (byte) 0x08;
      }

      @Override
      public void onRETI(Z80 cpu, int ctx) {
      }
    });

    cpu.doReset();
    cpu.setRegister(Z80.REG_SP, 0xFFFF);
    cpu.setRegister(Z80.REG_I, 0x10);

    final int INT = ~Z80.SIGNAL_IN_nINT;

    fillMemory(0x00, 0xFB); // EI at 0x00
    fillMemory(0x1008, 0xFF, 0x3F);//address for interruption at 0x1008
    fillMemory(0x3FFF, 0x00, 0x00, 0xED, 0x4D);//RETI at 0x3FFF

    cpu.setRegister(Z80.REG_A, 0xAA);
    cpu.setRegister(Z80.REG_A, 0xBB, true);
    cpu.setIM(2);

    assertEquals(2, cpu.getIM());
    assertEquals(0, cpu.getRegister(Z80.REG_R));
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());

    cpu.step(111, Z80.SIGNAL_IN_ALL_INACTIVE);
    assertEquals(1, cpu.getPC());
    assertEquals(0xFFFF, cpu.getSP());
    assertTrue(cpu.isIFF1());
    assertTrue(cpu.isIFF2());

    // interrupt subroutine
    cpu.step(111, INT);
    assertEquals(0x3FFF, cpu.getPC());
    assertEquals(0xFFFD, cpu.getSP());
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());

    cpu.step(111, INT);
    assertEquals(0x4000, cpu.getPC());
    assertEquals(0xFFFD, cpu.getSP());
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());

    cpu.step(111, INT);
    assertEquals(0x4001, cpu.getPC());
    assertEquals(0xFFFD, cpu.getSP());
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());

    //RETI
    cpu.step(111, INT);
    assertEquals(0x4002, cpu.getPC());

    cpu.step(111, Z80.SIGNAL_IN_ALL_INACTIVE);
    assertEquals(0x02, cpu.getPC());
    assertEquals(0xFFFF, cpu.getSP());
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());
  }

  @Test
  public void testIntProcessing_EI_RET_RST38() {
    final Z80 cpu = new Z80(new Z80CPUBus() {

      @Override
      public int readRegPortAddr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
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
      public int readPtr(Z80 cpu, int ctx, int reg, int valueInReg) {
        return valueInReg;
      }

      @Override
      public int readSpecRegValue(Z80 cpu, int ctx, int reg, int origValue) {
        return origValue;
      }

      @Override
      public int readSpecRegPairValue(Z80 cpu, int ctx, int regPair, int origValue) {
        return origValue;
      }

      @Override
      public byte readPort(Z80 cpu, int ctx, int port) {
        return 0;
      }

      @Override
      public void writePort(Z80 cpu, int ctx, int port, byte data) {

      }

      @Override
      public byte onCPURequestDataLines(Z80 cpu, int ctx) {
        return (byte) 0x08;
      }

      @Override
      public void onRETI(Z80 cpu, int ctx) {
      }
    });

    cpu.doReset();
    cpu.setRegister(Z80.REG_SP, 0xFFFF);
    cpu.setIM(1);

    cpu.setRegisterPair(Z80.REGPAIR_HL, 0x1234);

    fillMemory(0x00, 0xE5, 0xFB, 0xC9); // PUSH HL, EI, RET

    final int INT = ~Z80.SIGNAL_IN_nINT;

    cpu.step(111, INT); // push
    assertFalse(cpu.isIFF1());
    assertEquals(0x01, cpu.getPC());
    assertEquals(0xFFFD, cpu.getSP());

    cpu.step(111, INT); // ei
    assertEquals(0x02, cpu.getPC());
    assertTrue(cpu.isIFF1());
    assertEquals(0xFFFD, cpu.getSP());

    cpu.step(111, INT); // ret and interruption
    assertEquals(0x38, cpu.getPC());
    assertFalse(cpu.isIFF1());
    assertEquals(0xFFFD, cpu.getSP());
  }

}
