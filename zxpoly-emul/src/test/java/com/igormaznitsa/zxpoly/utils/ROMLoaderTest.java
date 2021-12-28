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
package com.igormaznitsa.zxpoly.utils;

import com.igormaznitsa.zxpoly.components.RomData;
import org.junit.Ignore;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;

@Ignore
public class ROMLoaderTest {
  
  @Test
  public void testLoadAndExtractROMFromArchiveVTRD() throws Exception {
    final RomData data = RomLoader.getROMFrom("http://trd.speccy.cz/emulz/UKV12F5.ZIP", Set.of(), Set.of(), Set.of());
    assertEquals(0x4000*3,data.getAsArray().length);
    assertEquals("48.rom",0xAF,data.getAsArray()[0x01] & 0xFF);
    assertEquals("128tr.rom",0x01,data.getAsArray()[0x4001] & 0xFF);
    assertEquals("trdos.rom",0x11,data.getAsArray()[0x8001] & 0xFF);
    assertEquals(0xFFFF, data.getMask());
  }
  
  @Test
  public void testLoadAndExtractROMFromArchiveWOS() throws Exception {
    final RomData data = RomLoader.getROMFrom("ftp://anonymous:anonymous@ftp.worldofspectrum.org/pub/sinclair/emulators/pc/russian/ukv12f5.zip", Set.of(), Set.of(), Set.of());
    assertEquals(0x4000*3,data.getAsArray().length);
    assertEquals("48.rom",0xAF,data.getAsArray()[0x01] & 0xFF);
    assertEquals("128tr.rom",0x01,data.getAsArray()[0x4001] & 0xFF);
    assertEquals("trdos.rom",0x11,data.getAsArray()[0x8001] & 0xFF);
    assertEquals(0xFFFF, data.getMask());
  }
  
}
