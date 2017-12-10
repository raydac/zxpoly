/*
 * The class is absolutely free for reusage and modifications.
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

  private static class LzwTable {

    private final static int RES_CODES = 2;
    private final static short HASH_FREE = (short) 0xFFFF;
    private final static short NEXT_FIRST = (short) 0xFFFF;
    private final static int MAXBITS = 12;
    private final static int MAXSTR = (1 << MAXBITS);
    private final static short HASHSIZE = 9973;
    private final static short HASHSTEP = 2039;

    private final byte strChr[];
    private final short strNxt[];
    private final short stringHashes[];
    private short numStrings;

    private LzwTable() {
      this.strChr = new byte[MAXSTR];
      this.strNxt = new short[MAXSTR];
      this.stringHashes = new short[HASHSIZE];
    }

    private int addCharString(final short index, final byte b) {
      int hshidx;
      if (this.numStrings >= MAXSTR) {
        return 0xFFFF;
      }

      hshidx = hash(index, b);
      while (this.stringHashes[hshidx] != HASH_FREE) {
        hshidx = (hshidx + HASHSTEP) % HASHSIZE;
      }

      this.stringHashes[hshidx] = this.numStrings;
      this.strChr[this.numStrings] = b;
      this.strNxt[this.numStrings] = (index != HASH_FREE) ? index : NEXT_FIRST;

      return this.numStrings++;
    }

    private short findCharString(final short index, final byte b) {
      int hshidx, nxtidx;

      if (index == HASH_FREE) {
        return b;
      }

      hshidx = hash(index, b);
      while ((nxtidx = this.stringHashes[hshidx]) != HASH_FREE) {
        if (this.strNxt[nxtidx] == index && this.strChr[nxtidx] == b) {
          return (short) nxtidx;
        }
        hshidx = (hshidx + HASHSTEP) % HASHSIZE;
      }

      return (short) 0xFFFF;
    }

    private void clearTable(final int codesize) {
      this.numStrings = 0;

      for (int q = 0; q < HASHSIZE; q++) {
        this.stringHashes[q] = HASH_FREE;
      }

      int w = (1 << codesize) + RES_CODES;
      for (int q = 0; q < w; q++) {
        this.addCharString((short) 0xFFFF, (byte) q);
      }
    }

    private int hash(final short index, final byte lastbyte) {
      return ((int) ((short) (lastbyte << 8) ^ index) & 0xFFFF) % HASHSIZE;
    }
  }

  private static final class BitBuffer256 {

    private final OutputStream targetStream;
    private final byte[] buffer;
    private int streamIndex;
    private int bitsLeft;

    private BitBuffer256(final OutputStream stream, final byte[] dataBuffer256) {
      this.targetStream = stream;
      this.streamIndex = 0;
      this.bitsLeft = 8;
      this.buffer = dataBuffer256;
    }

    private void flush() throws IOException {
      int nBytes = this.streamIndex + ((this.bitsLeft == 8) ? 0 : 1);

      if (nBytes > 0) {
        this.targetStream.write(nBytes);
        this.targetStream.write(buffer, 0, nBytes);

        this.buffer[0] = 0;
        this.streamIndex = 0;
        this.bitsLeft = 8;
      }
    }

    private void writeBits(int bits, int nBits) throws IOException {
      int nBytes = 255;

      do {
        if ((this.streamIndex == 254 && this.bitsLeft == 0) || this.streamIndex > 254) {
          this.targetStream.write(nBytes);
          this.targetStream.write(this.buffer, 0, nBytes);

          this.buffer[0] = 0;
          this.streamIndex = 0;
          this.bitsLeft = 8;
        }

        if (nBits <= this.bitsLeft) {
          this.buffer[this.streamIndex] |= (bits & ((1 << nBits) - 1)) << (8 - this.bitsLeft);

          this.bitsLeft -= nBits;
          nBits = 0;
        } else {
          this.buffer[this.streamIndex] |= (bits & ((1 << this.bitsLeft) - 1)) << (8 - this.bitsLeft);
          bits >>= this.bitsLeft;
          nBits -= this.bitsLeft;
          this.buffer[++this.streamIndex] = 0;
          this.bitsLeft = 8;
        }

      }
      while (nBits != 0);
    }
  }

  private final JBBPBitOutputStream stream;
  private final int intsBetweenFrames;
  private final int frameDelay;
  private final byte[] dataBuffer256 = new byte[256];
  private final byte[] indexBuffer = new byte[VideoController.SCREEN_WIDTH * VideoController.SCREEN_HEIGHT];

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
    for (int i = 0; i < VideoController.SCREEN_WIDTH * VideoController.SCREEN_HEIGHT; i++) {
      final int ci = VideoController.rgbColorToIndex(rgbPixels[i]);
      if (ci < 0) {
        throw new IOException("Detected unsupported color in buffer [" + Integer.toHexString(rgbPixels[i]) + ']');
      }
      this.indexBuffer[i] = (byte) ci;
    }
    this.stream.write(4);
    ZXPolyAGifEncoder.compress(this.stream, 4, this.indexBuffer, this.dataBuffer256);
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

  private static void compress(final OutputStream stream, final int codeSize, final byte data[], final byte[] dataBuffer256) throws IOException {
    byte c;
    short index;
    int clearcode, endofinfo, numbits, limit;
    short prefix = (short) 0xFFFF;

    final BitBuffer256 bitFile = new BitBuffer256(stream, dataBuffer256);
    final LzwTable strings = new LzwTable();

    clearcode = 1 << codeSize;
    endofinfo = clearcode + 1;

    numbits = codeSize + 1;
    limit = (1 << numbits) - 1;

    strings.clearTable(codeSize);
    bitFile.writeBits(clearcode, numbits);

    for (int loop = 0; loop < data.length; loop++) {
      c = data[loop];
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
