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

import java.util.Random;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class Z80RandomCodeBlockTest {

  @Test
  public void testRandomCodeBlock() throws Exception {
    final TestBus testbus = new TestBus(0, 0);
    final Random rnd = new Random(8962L);
    for (int i = 0; i < 0x10000; i++) {
      byte code = (byte) rnd.nextInt(0xFFFFFF);
      while (code == (byte) 0x76) {
        code = (byte) rnd.nextInt(0xFFFFFF);
      }
      testbus.writeMemory(null, i, code);
    }

    final Z80 cpu = new Z80(testbus);
    int steps = 0;
    final long starttime = System.currentTimeMillis();
    for (int i = 0; i < 400000000; i++) {
      cpu.step(0xFFFFFFFF);
      steps++;
    }
    final long milliseconds = System.currentTimeMillis() - starttime;
    final long tactsWouldBeAtStandard = 4000 * milliseconds;
    final long speed = Math.round(((double) cpu.getTacts() / (double) tactsWouldBeAtStandard) * 100d);

    assertEquals(0xFFFF, cpu.getState() & 0xFFFF);

    System.out.println("Executed " + steps + " step(s), PC=" + cpu.getRegister(Z80.REG_PC) + ", SP=" + cpu.getRegister(Z80.REG_SP) + ", tacts=" + cpu.getTacts() + ", speed=" + speed + "%");
  }

}
