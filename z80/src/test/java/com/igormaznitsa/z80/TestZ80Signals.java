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

import static org.junit.Assert.*;
import org.junit.Test;

public class TestZ80Signals extends AbstractZ80Test {
  @Test
  public void testInt_Disabled(){
    final TestBus tb = new TestBus(0, 0, 0x03);
    final Z80 cpu = new Z80(tb);
    
    cpu.step(~Z80.SIGNAL_IN_nINT);
    
    assertEquals(1, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFF, cpu.getRegister(Z80.REG_SP));
  }
  
  @Test
  public void testInt_Enabled_IM0(){
    final TestBus tb = new TestBus(0xFF, 0, 0x03);
    
    final Z80 cpu = new Z80(tb);
    cpu.setIM(0);
    cpu.setIFF(true, true);
    
    cpu.step(~Z80.SIGNAL_IN_nINT);
    
    assertFalse(cpu.isIFF1());
    assertEquals(0x38, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFD, cpu.getRegister(Z80.REG_SP));
    
    assertTacts(cpu, 13);
  }

  @Test
  public void testInt_Enabled_IM1(){
    final TestBus tb = new TestBus(0xCF, 0, 0x03);
    
    final Z80 cpu = new Z80(tb);
    cpu.setIM(1);
    cpu.setIFF(true, true);
    
    cpu.step(~Z80.SIGNAL_IN_nINT);
    
    assertFalse(cpu.isIFF1());
    assertEquals(0x38, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFD, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x00, tb.readMemory(cpu, cpu.getRegister(Z80.REG_SP), false));
    assertEquals(0x00, tb.readMemory(cpu, cpu.getRegister(Z80.REG_SP)+1, false));

    assertTacts(cpu, 13);
  }

  @Test
  public void testInt_Enabled_IM2(){
    final TestBus tb = new TestBus(0xCF, 0, 0x03);
    
    final Z80 cpu = new Z80(tb);
    cpu.setIM(2);
    cpu.setRegister(Z80.REG_I, 0x86);
    cpu.setIFF(true, true);
    
    cpu.step(~Z80.SIGNAL_IN_nINT);
    
    assertFalse(cpu.isIFF1());
    assertEquals(0x86CF, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFD, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x00, tb.readMemory(cpu, cpu.getRegister(Z80.REG_SP), false));
    assertEquals(0x00, tb.readMemory(cpu, cpu.getRegister(Z80.REG_SP)+1, false));

    assertTacts(cpu, 19);
  }

  @Test
  public void testNmiWithInt_Enabled() {
    final TestBus tb = new TestBus(0xFF, 0, 0x03);

    final Z80 cpu = new Z80(tb);
    cpu.setIM(0);
    cpu.setIFF(true, true);

    cpu.step(~(Z80.SIGNAL_IN_nINT | Z80.SIGNAL_IN_nNMI));

    assertFalse(cpu.isIFF1());
    assertEquals(0x66, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFD, cpu.getRegister(Z80.REG_SP));

    assertTacts(cpu, 11);
  }

  @Test
  public void testInterruptionDisabledAfterEI_INT() {
    final TestBus tb = new TestBus(0xFF, 0, 0xFB, 0x3C);

    final Z80 cpu = new Z80(tb);
    cpu.setRegister(Z80.REG_A, 0);
    cpu.setIM(0);
    cpu.setIFF(false, false);

    cpu.nextInstruction(false, false, false);
    assertEquals(0x0001, cpu.getRegister(Z80.REG_PC));
    
    cpu.nextInstruction(false, false, true);
    assertEquals(0x0002, cpu.getRegister(Z80.REG_PC));

    cpu.nextInstruction(false, false, false);

    assertEquals(0x38, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFD, cpu.getRegister(Z80.REG_SP));
  }

  @Test
  public void testInterruptionDisabledAfterEI_NMI() {
    final TestBus tb = new TestBus(0xFF, 0, 0xFB, 0x3C);

    final Z80 cpu = new Z80(tb);
    cpu.setRegister(Z80.REG_A, 0);
    cpu.setIM(0);
    cpu.setIFF(false, false);

    cpu.nextInstruction(false, false, false);
    assertEquals(0x0001, cpu.getRegister(Z80.REG_PC));
    
    cpu.nextInstruction(false, true, false);
    assertEquals(0x0002, cpu.getRegister(Z80.REG_PC));

    cpu.nextInstruction(false, false, false);

    assertEquals(0x66, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFD, cpu.getRegister(Z80.REG_SP));
  }

  @Test
  public void testNONIForMultiplePrefixes() {
    final TestBus tb = new TestBus(0xFF, 0, 0xED, 0xED, 0xED, 0xED, 0xED, 0x4A);

    final Z80 cpu = new Z80(tb);
    
    cpu.setRegisterPair(Z80.REGPAIR_HL, 0x1111);
    cpu.setRegisterPair(Z80.REGPAIR_BC, 0x2222);
    cpu.setRegister(Z80.REG_F, 0xFF);
    
    cpu.setIM(0);
    cpu.setIFF(true, true);

    assertTrue(cpu.step(0xFFFFFFFF));
    assertEquals(0x0001, cpu.getRegister(Z80.REG_PC));
    
    assertFalse(cpu.step(~Z80.SIGNAL_IN_nINT));
    assertEquals(0x0002, cpu.getRegister(Z80.REG_PC));

    assertTrue(cpu.step(0xFFFFFFFF));
    assertEquals(0x0003, cpu.getRegister(Z80.REG_PC));

    assertFalse(cpu.step(0xFFFFFFFF));
    assertEquals(0x0004, cpu.getRegister(Z80.REG_PC));

    assertTrue(cpu.step(0xFFFFFFFF));
    assertEquals(0x0005, cpu.getRegister(Z80.REG_PC));

    assertFalse(cpu.step(0xFFFFFFFF));
    assertEquals(0x0006, cpu.getRegister(Z80.REG_PC));

    assertFalse(cpu.step(0xFFFFFFFF));

    assertEquals(0x38, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFD, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x06, tb.readMemory(cpu, cpu.getRegister(Z80.REG_SP), false)&0xFF);
    assertEquals(0x00, tb.readMemory(cpu, cpu.getRegister(Z80.REG_SP)+1, false)&0xFF);
  }

  @Test
  public void testHALT_INT_RETI() {
    final TestBus tb = new TestBus(0xFF, 0, 0x76);
    tb.writeMemory(null, 0x38, (byte)0xED);
    tb.writeMemory(null, 0x39, (byte)0x4D);
    
    final Z80 cpu = new Z80(tb);
    cpu.setIM(0);
    cpu.setIFF(true, true);

    // meet HALT
    assertTrue((cpu.getState() & Z80.SIGNAL_OUT_nHALT)!=0);
    assertFalse(cpu.step(0xFFFFFFFF));
    assertEquals(0x0000, cpu.getRegister(Z80.REG_PC));
    assertTrue((cpu.getState() & Z80.SIGNAL_OUT_nHALT) == 0);

    assertFalse(cpu.step(0xFFFFFFFF));
    assertEquals(0x0000, cpu.getRegister(Z80.REG_PC));
    assertTrue((cpu.getState() & Z80.SIGNAL_OUT_nHALT) == 0);

    // send INT
    assertFalse(cpu.step(~Z80.SIGNAL_IN_nINT));
    assertTrue((cpu.getState() & Z80.SIGNAL_OUT_nHALT) != 0);
    assertEquals(0x38, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFD, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x01, tb.readMemory(cpu, cpu.getRegister(Z80.REG_SP), false));
    assertEquals(0x00, tb.readMemory(cpu, cpu.getRegister(Z80.REG_SP)+1, false));
    assertFalse(tb.isRETI());
    
    // do RETI
    assertTrue(cpu.step(0xFFFFFFFF));
    assertEquals(0x39, cpu.getRegister(Z80.REG_PC));
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());
    assertFalse(tb.isRETI());
    
    assertFalse(cpu.step(0xFFFFFFFF));
    assertEquals(0x0001, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFF, cpu.getRegister(Z80.REG_SP));
    assertTrue(tb.isRETI());
    assertFalse(cpu.isIFF1());
    assertFalse(cpu.isIFF2());
  }

  @Test
  public void testHALT_NMI_RETN() {
    final TestBus tb = new TestBus(0xFF, 0, 0x76);
    tb.writeMemory(null, 0x66, (byte) 0xED);
    tb.writeMemory(null, 0x67, (byte) 0x55);

    final Z80 cpu = new Z80(tb);
    cpu.setIM(0);
    cpu.setIFF(true, true);

    // meet HALT
    assertTrue((cpu.getState() & Z80.SIGNAL_OUT_nHALT) != 0);
    assertFalse(cpu.step(0xFFFFFFFF));
    assertEquals(0x0000, cpu.getRegister(Z80.REG_PC));
    assertTrue((cpu.getState() & Z80.SIGNAL_OUT_nHALT) == 0);

    assertFalse(cpu.step(0xFFFFFFFF));
    assertEquals(0x0000, cpu.getRegister(Z80.REG_PC));
    assertTrue((cpu.getState() & Z80.SIGNAL_OUT_nHALT) == 0);

    // send NMI
    assertFalse(cpu.step(~Z80.SIGNAL_IN_nNMI));
    assertTrue((cpu.getState() & Z80.SIGNAL_OUT_nHALT) != 0);
    assertEquals(0x66, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFD, cpu.getRegister(Z80.REG_SP));
    assertEquals(0x01, tb.readMemory(cpu, cpu.getRegister(Z80.REG_SP), false));
    assertEquals(0x00, tb.readMemory(cpu, cpu.getRegister(Z80.REG_SP) + 1, false));
    assertFalse(tb.isRETI());

    // do RETN
    assertTrue(cpu.step(0xFFFFFFFF));
    assertEquals(0x67, cpu.getRegister(Z80.REG_PC));
    assertFalse(cpu.isIFF1());
    assertTrue(cpu.isIFF2());
    assertFalse(tb.isRETI());

    assertFalse(cpu.step(0xFFFFFFFF));
    assertEquals(0x0001, cpu.getRegister(Z80.REG_PC));
    assertEquals(0xFFFF, cpu.getRegister(Z80.REG_SP));
    assertFalse(tb.isRETI());
    assertTrue(cpu.isIFF1());
    assertTrue(cpu.isIFF2());
  }
  
  @Test
  public void testWAIT() {
    final TestBus tb = new TestBus(0xFF, 0, 0xED, 0x4A);
    final Z80 cpu = new Z80(tb);

    assertTrue(cpu.step(0xFFFFFFFF));
    assertEquals(0x0001, cpu.getRegister(Z80.REG_PC));
    
    assertTrue(cpu.step(~Z80.SIGNAL_IN_nWAIT));
    assertEquals(0x0001, cpu.getRegister(Z80.REG_PC));

    assertTrue(cpu.step(~Z80.SIGNAL_IN_nWAIT));
    assertEquals(0x0001, cpu.getRegister(Z80.REG_PC));
    
    assertFalse(cpu.step(0xFFFFFFFF));
    assertEquals(0x0002, cpu.getRegister(Z80.REG_PC));
  }
  
}
