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
package com.igormaznitsa.vg93;

import com.igormaznitsa.vg93.FloppyDisk.Sector;
import com.igormaznitsa.vg93.FloppyDisk.Track;
import org.junit.Test;
import static org.junit.Assert.*;

public class FloppyDiskTest {
  
  @Test
  public void testCreateFormatted() {
    final FloppyDisk floppy = new FloppyDisk(2, 80, 16, 256, true);
    for(int side=0;side<2;side++){
      for(int track=0;track<80;track++){
        final Track thetrack = floppy.getTrack(side*80+track);
        for(int sector=0;sector<16;sector++){
          final Sector sec = thetrack.sectorAt(sector);
          assertEquals("side "+side+" track "+track+" sector "+sector, side,sec.getSide());
          assertEquals("side " + side + " track " + track + " sector " + sector,track,sec.getTrack());
          assertEquals("side " + side + " track " + track + " sector " + sector,sector+1,sec.getNumber());
          assertTrue(sec.getCrc()!=0);
        }
      }
    }
  }
  
}
