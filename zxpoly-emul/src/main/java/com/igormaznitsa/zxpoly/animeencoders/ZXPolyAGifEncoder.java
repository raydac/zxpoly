/*
 * Copyright (C) 2017 Raydac Research Group Ltd.
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
package com.igormaznitsa.zxpoly.animeencoders;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.zxpoly.MainForm;
import com.igormaznitsa.zxpoly.components.VideoController;

public final class ZXPolyAGifEncoder implements AnimationEncoder {

  private static class LzwStringTable {

    private final static int RES_CODES = 2;
    private final static short HASH_FREE = (short) 0xFFFF;
    private final static short NEXT_FIRST = (short) 0xFFFF;
    private final static int MAXBITS = 12;
    private final static int MAXSTR = (1 << MAXBITS);
    private final static short HASHSIZE = 9973;
    private final static short HASHSTEP = 2039;

    private byte strChr_[];
    private short strNxt_[];
    private short strHsh_[];
    private short nStrings_;

    LzwStringTable() {
      strChr_ = new byte[MAXSTR];
      strNxt_ = new short[MAXSTR];
      strHsh_ = new short[HASHSIZE];
    }

    int addCharString(final short index, final byte b) {
      int hshidx;
      if (nStrings_ >= MAXSTR) {
        return 0xFFFF;
      }

      hshidx = hash(index, b);
      while (strHsh_[hshidx] != HASH_FREE) {
        hshidx = (hshidx + HASHSTEP) % HASHSIZE;
      }

      strHsh_[hshidx] = nStrings_;
      strChr_[nStrings_] = b;
      strNxt_[nStrings_] = (index != HASH_FREE) ? index : NEXT_FIRST;

      return nStrings_++;
    }

    short findCharString(short index, byte b) {
      int hshidx, nxtidx;

      if (index == HASH_FREE) {
        return b;
      }

      hshidx = hash(index, b);
      while ((nxtidx = strHsh_[hshidx]) != HASH_FREE) {
        if (strNxt_[nxtidx] == index && strChr_[nxtidx] == b) {
          return (short) nxtidx;
        }
        hshidx = (hshidx + HASHSTEP) % HASHSIZE;
      }

      return (short) 0xFFFF;
    }

    void clearTable(int codesize) {
      nStrings_ = 0;

      for (int q = 0; q < HASHSIZE; q++) {
        strHsh_[q] = HASH_FREE;
      }

      int w = (1 << codesize) + RES_CODES;
      for (int q = 0; q < w; q++) {
        this.addCharString((short) 0xFFFF, (byte) q);
      }
    }

    int hash(short index, byte lastbyte) {
      return ((int) ((short) (lastbyte << 8) ^ index) & 0xFFFF) % HASHSIZE;
    }
  }

  private static class BitFile {

    private OutputStream stream_ = null;
    private final byte[] buffer_;
    private int streamIndex_, bitsLeft_;

    BitFile(OutputStream stream) {
      stream_ = stream;
      buffer_ = new byte[256];
      streamIndex_ = 0;
      bitsLeft_ = 8;
    }

    void flush()
        throws IOException {
      int nBytes = streamIndex_ + ((bitsLeft_ == 8) ? 0 : 1);

      if (nBytes > 0) {
        stream_.write(nBytes);
        stream_.write(buffer_, 0, nBytes);

        buffer_[0] = 0;
        streamIndex_ = 0;
        bitsLeft_ = 8;
      }
    }

    void writeBits(int bits, int nBits) throws IOException {
      int nBitsWritten = 0;
      int nBytes = 255;

      do {
        if ((streamIndex_ == 254 && bitsLeft_ == 0) || streamIndex_ > 254) {
          stream_.write(nBytes);
          stream_.write(buffer_, 0, nBytes);

          buffer_[0] = 0;
          streamIndex_ = 0;
          bitsLeft_ = 8;
        }

        if (nBits <= bitsLeft_) {
          buffer_[streamIndex_] |= (bits & ((1 << nBits) - 1)) << (8 - bitsLeft_);

          nBitsWritten += nBits;
          bitsLeft_ -= nBits;
          nBits = 0;
        } else {
          buffer_[streamIndex_] |= (bits & ((1 << bitsLeft_) - 1))
              << (8 - bitsLeft_);

          nBitsWritten += bitsLeft_;
          bits >>= bitsLeft_;
          nBits -= bitsLeft_;
          buffer_[++streamIndex_] = 0;
          bitsLeft_ = 8;
        }

      }
      while (nBits != 0);
    }
  }

  private final JBBPBitOutputStream stream;
  private final int intsBetweenFrames;
  private final int frameDelay;

  @Override
  public int getIntsBetweenFrames() {
    return this.intsBetweenFrames;
  }

  public ZXPolyAGifEncoder(final File file, final int frameRate, final boolean loop) throws IOException {
    this.stream = new JBBPBitOutputStream(new BufferedOutputStream(new FileOutputStream(file), 0xFFFF));
    this.intsBetweenFrames = (int) (1000 / MainForm.TIMER_INT_DELAY_MILLISECONDS) / frameRate;
    this.frameDelay = (int) (this.intsBetweenFrames * MainForm.TIMER_INT_DELAY_MILLISECONDS) / 10;

    // magic
    this.stream.write(new byte[]{0x47, 0x49, 0x46, 0x38, 0x39, 0x61});
    // logic screen size
    this.stream.writeShort(VideoController.SCREEN_WIDTH, JBBPByteOrder.LITTLE_ENDIAN);
    this.stream.writeShort(VideoController.SCREEN_HEIGHT, JBBPByteOrder.LITTLE_ENDIAN);

    // palette size 16 colors
    this.stream.writeBits(3, JBBPBitNumber.BITS_3);

    // palette sorting
    this.stream.writeBits(0, JBBPBitNumber.BITS_1);

    // color resolution
    this.stream.writeBits(7, JBBPBitNumber.BITS_3);

    // global palette
    this.stream.writeBits(1, JBBPBitNumber.BITS_1);

    this.stream.write(0); // background color
    this.stream.write(0); // default pixel aspect ratio

    // global palette
    for (final int i : VideoController.ZXPALETTE) {
      final int r = (i >>> 16) & 0xFF;
      final int g = (i >>> 8) & 0xFF;
      final int b = i & 0xFF;
      this.stream.write(r);
      this.stream.write(g);
      this.stream.write(b);
    }

    if (loop) {
      // netscape extension
      this.stream.write('!');
      this.stream.write(0xFF);
      this.stream.write(0x0B);
      this.stream.write(new byte[]{0x4E, 0x45, 0x54, 0x53, 0x43, 0x41, 0x50, 0x45, 0x32, 0x2E, 0x30});
      this.stream.write(3);
      this.stream.write(1);
      this.stream.writeShort(0, JBBPByteOrder.LITTLE_ENDIAN);
      this.stream.write(0);
    }
  }

  @Override
  public void saveFrame(final int[] rgbPixels) throws IOException {
    // Write delay
    this.stream.write('!');
    this.stream.write(0xF9);
    this.stream.write(0x04);
    this.stream.write(0);
    this.stream.writeShort(this.frameDelay, JBBPByteOrder.LITTLE_ENDIAN);
    this.stream.write(0);
    this.stream.write(0);

    // frame parameters
    this.stream.write(',');
    this.stream.writeShort(0, JBBPByteOrder.LITTLE_ENDIAN); // L
    this.stream.writeShort(0, JBBPByteOrder.LITTLE_ENDIAN); // T
    this.stream.writeShort(VideoController.SCREEN_WIDTH, JBBPByteOrder.LITTLE_ENDIAN); // W
    this.stream.writeShort(VideoController.SCREEN_HEIGHT, JBBPByteOrder.LITTLE_ENDIAN); // H
    this.stream.write(0); // flag

    // pack frame
    final byte[] b = new byte[VideoController.SCREEN_WIDTH * VideoController.SCREEN_HEIGHT];
    for (int i = 0; i < VideoController.SCREEN_WIDTH * VideoController.SCREEN_HEIGHT; i++) {
      final int ci = VideoController.rgbColorToIndex(rgbPixels[i]);
      if (ci < 0) {
        throw new IOException("Detected unsupported color in buffer [" + Integer.toHexString(rgbPixels[i]) + ']');
      }
      b[i] = (byte) ci;
    }
    this.stream.write(4);
    ZXPolyAGifEncoder.writeLzwCompressed(this.stream, 4, b);
    this.stream.write(0);

  }

  @Override
  public void close() throws IOException {
    try {
      this.stream.write(';');
      this.stream.flush();
      this.stream.close();
    }
    finally {
      JBBPUtils.closeQuietly(this.stream);
    }
  }

  private static void writeLzwCompressed(OutputStream stream, int codeSize, byte toCompress[]) throws IOException {
    byte c;
    short index;
    int clearcode, endofinfo, numbits, limit;
    short prefix = (short) 0xFFFF;

    BitFile bitFile = new BitFile(stream);
    LzwStringTable strings = new LzwStringTable();

    clearcode = 1 << codeSize;
    endofinfo = clearcode + 1;

    numbits = codeSize + 1;
    limit = (1 << numbits) - 1;

    strings.clearTable(codeSize);
    bitFile.writeBits(clearcode, numbits);

    for (int loop = 0; loop < toCompress.length; loop++) {
      c = toCompress[loop];
      if ((index = strings.findCharString(prefix, c)) != -1) {
        prefix = index;
      } else {
        bitFile.writeBits(prefix, numbits);
        if (strings.addCharString(prefix, c) > limit) {
          if (++numbits > 12) {
            bitFile.writeBits(clearcode, numbits - 1);
            strings.clearTable(codeSize);
            numbits = codeSize + 1;
          }

          limit = (1 << numbits) - 1;
        }

        prefix = (short) ((short) c & 0xFF);
      }
    }

    if (prefix != -1) {
      bitFile.writeBits(prefix, numbits);
    }

    bitFile.writeBits(endofinfo, numbits);
    bitFile.flush();
  }

}
