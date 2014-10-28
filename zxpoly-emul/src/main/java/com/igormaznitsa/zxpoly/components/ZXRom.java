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
package com.igormaznitsa.zxpoly.components;

import java.io.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public final class ZXRom {

  private final byte[] data;

  public ZXRom(final byte[] array) {
    if (array.length > 0x8000) {
      throw new IllegalArgumentException("Too big data to be a ZX ROM (max 32768 bytes)");
    }
    this.data = new byte[0x8000];
    System.arraycopy(array, 0, this.data, 0, array.length);
  }

  public byte [] getArray(){
    return this.data;
  }
  
  public int readAddress(final int address) {
    return this.data[address & 0x7FFF] & 0xFF;
  }

  public static ZXRom read(final File file) throws IOException {
    return new ZXRom(FileUtils.readFileToByteArray(file));
  }

  public static ZXRom read(final InputStream in) throws IOException {
    try {
      return new ZXRom(IOUtils.toByteArray(in));
    }
    finally {
      IOUtils.closeQuietly(in);
    }
  }

}
