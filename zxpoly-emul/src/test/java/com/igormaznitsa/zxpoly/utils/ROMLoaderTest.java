/*
 * Copyright (C) 2015 Raydac Research Group Ltd.
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
package com.igormaznitsa.zxpoly.utils;

import com.igormaznitsa.zxpoly.components.RomData;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Ignore;

@Ignore
public class ROMLoaderTest {
  
  @Test
  public void testLoadFromWOS() throws Exception {
    final byte [] data = new ROMLoader().loadFTPArchive();
    assertNotNull(data);
    assertTrue(data.length>100000);
  }
  
  @Test
  public void testLoadAndExtractROMFromArchive() throws Exception {
    final RomData data = new ROMLoader().getROM();
    assertEquals(0x4000*3,data.getAsArray().length);
    assertEquals("48.rom",0xAF,data.getAsArray()[0x01] & 0xFF);
    assertEquals("128tr.rom",0x01,data.getAsArray()[0x4001] & 0xFF);
    assertEquals("trdos.rom",0x11,data.getAsArray()[0x8001] & 0xFF);
    assertEquals(0xFFFF, data.getMask());
  }
  
}
