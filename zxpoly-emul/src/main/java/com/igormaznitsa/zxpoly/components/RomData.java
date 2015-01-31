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

import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public final class RomData {

  private final byte[] data;
  private final int addressMask;

  public RomData(final byte [] ... args){
    int size = 0;
    for(final byte [] a : args){
      size += a.length;
    }
    
    final byte [] result = new byte [size];
    int pos = 0;
    for(final byte [] a : args){
      System.arraycopy(a, 0, result, pos, a.length);
      pos += a.length;
    }
    this.data = result;
    this.addressMask = JBBPUtils.makeMask(((size / 0x4000)*0x4000) == size ? size -1 : size);
  }
  
  public RomData(final byte[] array) {
    if (array.length > 0x10000) {
      throw new IllegalArgumentException("Rom data must not be greater than 64k");
    }

    final int size  = ((array.length+0x3FFF) / 0x4000) * 0x4000;
    this.data = new byte[size];
    this.addressMask = JBBPUtils.makeMask(size-1);
    System.arraycopy(array, 0, this.data, 0, array.length);
  }
  
  public int getMask(){
    return this.addressMask;
  }

  public byte[] getAsArray() {
    return this.data;
  }

  public static RomData read(final File file) throws IOException {
    return new RomData(FileUtils.readFileToByteArray(file));
  }

  public static RomData read(final InputStream in) throws IOException {
    try {
      return new RomData(IOUtils.toByteArray(in));
    }
    finally {
      IOUtils.closeQuietly(in);
    }
  }

  public int readAdress(final int address) {
    return this.data[address & this.addressMask] & 0xFF;
  }

}
