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
package com.igormaznitsa.zxpoly.formats;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

public class ZXEMLSnapshotFormatTest {
  
  @Test
  public void testSaveLoad() throws Exception {
    final ZXEMLSnapshotFormat data = new ZXEMLSnapshotFormat();

    data.setRegIR(0, 0x0101);
    data.setRegIR(1, 0x0212);
    data.setRegIR(2, 0x0323);
    data.setRegIR(3, 0x0434);
    
    final byte [] saved = data.save();
    
    final ZXEMLSnapshotFormat unpacked = new ZXEMLSnapshotFormat(saved);

    assertEquals(0x0101, unpacked.getRegIR(0));
    assertEquals(0x0212, unpacked.getRegIR(1));
    assertEquals(0x0323, unpacked.getRegIR(2));
    assertEquals(0x0434, unpacked.getRegIR(3));
    
  }
}
