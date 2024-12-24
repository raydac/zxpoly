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
package com.igormaznitsa.zxpoly.formats;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import org.junit.Test;

public class ZXEMLSnapshotFormatTest {

  private static byte[] loadResource(final String name) throws Exception {
    final InputStream ins = ZXEMLSnapshotFormatTest.class.getResourceAsStream(name);
    assertNotNull("Can't find resource " + name, ins);
    final JBBPBitInputStream in = new JBBPBitInputStream(ins, false);
    final byte[] result = in.readByteArray(-1);
    in.close();
    return result;
  }

  @Test
  public void testSaveLoad_Snapshot() throws Exception {
    final byte[] array = loadResource("fh.zxp");

    final ZxEmlSnapshotFormat data = new ZxEmlSnapshotFormat();
    data.read(new JBBPBitInputStream(new ByteArrayInputStream(array.clone()), false));
    assertArrayEquals(array, data.save());
  }
}
