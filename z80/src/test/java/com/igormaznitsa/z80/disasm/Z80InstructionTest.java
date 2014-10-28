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
package com.igormaznitsa.z80.disasm;

import org.junit.Test;
import static org.junit.Assert.*;

public class Z80InstructionTest {
  
  @Test
  public void testParse_LD_IY_d_n() {
    final Z80Instruction ins = new Z80Instruction("FD36 d n   LD (IY+d),n");
    assertEquals(4, ins.getLength());
    assertEquals(2, ins.getFixedPartLength());
    assertArrayEquals(new int []{0xFD,0x36,Z80Instruction.SPEC_INDEX,Z80Instruction.SPEC_UNSIGNED_BYTE}, ins.getInstructionCodes());
  
    assertEquals("LD (IY-#0F),#08",ins.decode(new byte []{(byte)0xFD,(byte)0x36,(byte)-15,8}, 0, -1));
    assertNull(ins.decode(new byte[]{(byte) 0xFD, (byte) 0x37, (byte) -15, 8}, 0, -1));
  }
  
  @Test
  public void testParse_DJNZ() {
    final Z80Instruction ins = new Z80Instruction("10 e       DJNZ e");
    assertEquals(2, ins.getLength());
    assertEquals(1, ins.getFixedPartLength());
    assertArrayEquals(new int []{0x10,Z80Instruction.SPEC_OFFSET}, ins.getInstructionCodes());
  
    assertEquals("DJNZ PC-#08",ins.decode(new byte []{(byte)0x10,(byte)-8}, 0, -1));
    assertEquals("DJNZ PC+#7F",ins.decode(new byte []{(byte)0x10,(byte)0x7F}, 0, -1));
    assertEquals("DJNZ #407F",ins.decode(new byte []{(byte)0x10,(byte)0x7F}, 0, 0x4000));
    assertEquals("DJNZ #0001",ins.decode(new byte []{(byte)0x10,(byte)0x01}, 0, 0x0000));
  }
  
  @Test
  public void testParse_CALL() {
    final Z80Instruction ins = new Z80Instruction("E4 nn      CALL PO,nn");
    assertEquals(3, ins.getLength());
    assertEquals(1, ins.getFixedPartLength());
    assertArrayEquals(new int []{0xE4,Z80Instruction.SPEC_UNSIGNED_WORD}, ins.getInstructionCodes());
  
    assertEquals("CALL PO,#1234",ins.decode(new byte []{(byte)0xE4,(byte)0x34,(byte)0x12}, 0, -1));
    assertEquals("CALL PO,#0000",ins.decode(new byte []{(byte)0xE4,(byte)0x00,(byte)0x00}, 0, -1));
    assertEquals("CALL PO,#0001",ins.decode(new byte []{(byte)0xE4,(byte)0x01,(byte)0x00}, 0, -1));
    assertNull(ins.decode(new byte []{(byte)0xE4,(byte)0x34}, 0, -1));
    assertNull(ins.decode(new byte []{(byte)0xE4}, 0, -1));
  }
  
}
