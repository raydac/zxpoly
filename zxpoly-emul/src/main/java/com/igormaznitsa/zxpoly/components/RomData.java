/*
 * Copyright (C) 2014-2019 Igor Maznitsa
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

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import static com.igormaznitsa.jbbp.utils.JBBPUtils.makeMask;
import static java.lang.System.arraycopy;
import static java.util.stream.Stream.of;

public final class RomData {

  private final String source;
  private final byte[] data;
  private final int addressMask;
  private final boolean trdosPresented;


  public RomData(final String source, final byte[]... args) {
    this.source = source;
    final int size = of(args).mapToInt(x -> x.length).sum();

    final byte[] result = new byte[size];
    int pos = 0;
    for (final byte[] a : args) {
      arraycopy(a, 0, result, pos, a.length);
      pos += a.length;
    }
    this.data = result;
    this.addressMask = makeMask(((size / 0x4000) * 0x4000) == size ? size - 1 : size);
    this.trdosPresented = args.length > 2;
  }

  public RomData(final String source, final byte[] array) {
    this.source = source;

    if (array.length > 0x10000) {
      throw new IllegalArgumentException("Rom data must not be greater than 64k");
    }

    this.trdosPresented = array.length > 0x8000;

    final int size = ((array.length + 0x3FFF) / 0x4000) * 0x4000;
    this.data = new byte[size];
    this.addressMask = makeMask(size - 1);
    arraycopy(array, 0, this.data, 0, array.length);
  }

  public boolean isTrdosPresented() {
    return this.trdosPresented;
  }

  public static RomData read(final File file) throws IOException {
    return new RomData(file.getName(), FileUtils.readFileToByteArray(file));
  }

  public static RomData read(final String sourceName, final InputStream in) throws IOException {
    return new RomData(sourceName, IOUtils.toByteArray(in));
  }

  public String getSource() {
    return this.source;
  }

  public int getMask() {
    return this.addressMask;
  }

  public byte[] getAsArray() {
    return this.data;
  }

  public int readAdress(final int address) {
    return this.data[address & this.addressMask] & 0xFF;
  }

  public byte[] makeCopyPage(final int page) {
    final byte[] result = new byte[0x4000];
    final int offset = page * 0x4000;
    System.arraycopy(this.data, offset + 0, result, 0, 0x4000);
    return result;
  }
}
