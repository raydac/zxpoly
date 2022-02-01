/*
 * Copyright (C) 2014-2022 Igor Maznitsa
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

package com.igormaznitsa.zxpoly.components.tapereader;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

public class TapFormatParser {

  private TapBlock[] blocks;

  public TapFormatParser() {

  }

  public TapBlock[] getTapBlocks() {
    return this.blocks;
  }

  public TapFormatParser read(final JBBPBitInputStream stream) throws IOException {
    final List<TapBlock> blocks = new ArrayList<>();
    while (stream.hasAvailableData()) {
      final TapBlock next = new TapBlock(stream);
      if (next.getData() != null) {
        blocks.add(next);
      }
    }
    this.blocks = blocks.toArray(TapBlock[]::new);
    return this;
  }

  public static final class TapBlock {

    private final byte flag;
    private final byte[] data;
    private final byte checksum;

    private TapBlock(final JBBPBitInputStream inputStream) throws IOException {
      final int length = inputStream.readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN);
      if (length == 0) {
        this.flag = 0;
        this.data = null;
        this.checksum = 0;
      } else if (length < 2) {
        throw new IOException("Detected block length " + length + ", may be wrong format");
      } else {
        this.flag = (byte) inputStream.readByte();
        this.data = inputStream.readByteArray(length - 2);
        this.checksum = (byte) inputStream.readByte();
      }
    }

    public byte getChecksum() {
      return this.checksum;
    }

    public byte[] getData() {
      return requireNonNull(this.data);
    }

    public byte getFlag() {
      return this.flag;
    }
  }
}
