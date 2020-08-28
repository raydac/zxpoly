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

package com.igormaznitsa.z80.disasm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;


import com.igormaznitsa.z80.ByteArrayMemoryAccessProvider;
import com.igormaznitsa.z80.Z80Instruction;
import java.util.List;
import org.junit.Test;

public class Z80DisasmTest {

  @Test
  public void testDecodeList() {
    final ByteArrayMemoryAccessProvider memoryProvider =
        new ByteArrayMemoryAccessProvider(new byte[] {
            (byte) 0xBB, (byte) 0x6D, (byte) 0xEF, (byte) 0x40, (byte) 0x45, (byte) 0x21,
            (byte) 0x00, (byte) 0x00, (byte) 0x22,
            (byte) 0x4B, (byte) 0x84, (byte) 0x21, (byte) 0xA8, (byte) 0x9D, (byte) 0xEF,
            (byte) 0x0A, (byte) 0x45, (byte) 0xEF,
            (byte) 0x2E, (byte) 0x45, (byte) 0xC9, (byte) 0x48, (byte) 0x65, (byte) 0x6C,
            (byte) 0x6C, (byte) 0x6F, (byte) 0x20,
            (byte) 0x77, (byte) 0x6F, (byte) 0x72, (byte) 0x6C, (byte) 0x64, (byte) 0x21,
            (byte) 0x00});
    final List<Z80Instruction> list = Z80Disasm.decodeList(memoryProvider, null, 0, 26);

    assertEquals(26, list.size());
    assertNotNull(list.get(24));

    final StringBuilder builder = new StringBuilder();
    int off = 0;
    for (final Z80Instruction i : list) {
      if (i == null) {
        builder.append("<UNKNOWN>\n");
        off++;
      } else {
        builder.append(i.decode(memoryProvider, off, off + 0x4000)).append('\n');
        off += i.getLength();
      }
    }

    assertEquals("CP E\n"
            + "LD L,L\n"
            + "RST #28\n"
            + "LD B,B\n"
            + "LD B,L\n"
            + "LD HL,#0000\n"
            + "LD (#844B),HL\n"
            + "LD HL,#9DA8\n"
            + "RST #28\n"
            + "LD A,(BC)\n"
            + "LD B,L\n"
            + "RST #28\n"
            + "LD L,#45\n"
            + "RET\n"
            + "LD C,B\n"
            + "LD H,L\n"
            + "LD L,H\n"
            + "LD L,H\n"
            + "LD L,A\n"
            + "JR NZ,#4093\n"
            + "LD L,A\n"
            + "LD (HL),D\n"
            + "LD L,H\n"
            + "LD H,H\n"
            + "LD HL,#BB00\n"
            + "LD L,L\n"
        , builder.toString());
  }

}
