package com.igormaznitsa.zxpoly.animeencoders;


import com.igormaznitsa.zxpoly.utils.IntMap;

import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;
import java.util.Objects;
import java.util.OptionalInt;

/**
 * Class AnimatedGifEncoder - Encodes a GIF file consisting of one or more
 * frames.
 *
 * <pre>
 *  Example:
 *     AnimatedGifEncoder e = new AnimatedGifEncoder();
 *     e.start(outputFileName);
 *     e.setDelay(1000);   // 1 frame per sec
 *     e.addFrame(image1);
 *     e.addFrame(image2);
 *     e.finish();
 * </pre>
 * <p>
 * No copyright asserted on the source code of this class. May be used for any
 * purpose, however, refer to the Unisys LZW patent for restrictions on use of
 * the associated LZWEncoder class. Please forward any corrections to
 * kweiner@fmsware.com.
 *
 * @author Kevin Weiner, FM Software
 * @version 1.03 November 2003
 */

public final class AnimatedGlobalPaletteGifEncoder {

  private final int width; // image width
  private final int height; // image height
  private final byte[] newFrameIndexes; // converted frame indexed to palette
  private final byte[] preparedFrameIndexes;
  private final int[] rgbPalette;
  private final IntMap mapRgb2index;
  private int repeat = -1; // no repeat
  private int delay = 0; // frame delay (hundredths)
  private boolean started = false; // ready to output frames
  private OutputStream out;
  private int preparedFrameDelay;
  private boolean firstFrame;

  public AnimatedGlobalPaletteGifEncoder(final int imageWidth, final int imageHeight, final int[] palette) {
    if (palette.length == 0 || palette.length > 256) throw new IllegalArgumentException("Wrong size of palette");
    this.width = imageWidth;
    this.height = imageHeight;
    this.rgbPalette = new int[256];
    this.mapRgb2index = new IntMap(1024);
    for (int i = 0; i < palette.length; i++) {
      final int rgb = palette[i] & 0xFF_FF_FF;
      this.rgbPalette[i] = rgb;
      this.mapRgb2index.put(rgb, i);
    }
    this.newFrameIndexes = new byte[imageWidth * imageHeight];
    this.preparedFrameIndexes = new byte[imageWidth * imageHeight];
  }

  /**
   * Sets the delay time between each frame, or changes it for subsequent frames
   * (applies to last frame added).
   *
   * @param time delay time
   */
  public void setDelay(final Duration time) {
    this.delay = Math.round(time.toMillis() / 10.0f);
    this.preparedFrameDelay = this.delay;
  }

  /**
   * Sets the number of times the set of GIF frames should be played. Default is
   * 1; 0 means play indefinitely. Must be invoked before the first image is
   * added.
   *
   * @param iter int number of iterations.
   */
  public void setRepeat(int iter) {
    if (iter >= 0) {
      this.repeat = iter;
    }
  }

  /**
   * Adds next GIF frame. The frame is not written immediately, but is actually
   * deferred until the next frame is received so that timing data can be
   * inserted. Invoking <code>finish()</code> flushes all frames. If
   * <code>setSize</code> was not invoked, the size of the first image is used
   * for all subsequent frames.
   *
   * @param argb argb frame to write.
   */
  public void addFrame(int[] argb) throws IOException {
    if (!this.started) {
      throw new IllegalStateException("Not started yet");
    }

    final boolean foundDifference = findNewFrameIndexes(argb);

    if (this.firstFrame) {
      System.arraycopy(this.newFrameIndexes, 0, this.preparedFrameIndexes, 0, this.preparedFrameIndexes.length);
      this.preparedFrameDelay = this.delay;
      this.firstFrame = false;
    } else {
      if (foundDifference) {
        dropPreparedFrame();
        System.arraycopy(this.newFrameIndexes, 0, this.preparedFrameIndexes, 0, this.preparedFrameIndexes.length);
      } else {
        final int nextDelayValue = this.preparedFrameDelay + this.delay;
        if ((nextDelayValue & 0xFFFF_0000) != 0) {
          dropPreparedFrame();
        } else {
          this.preparedFrameDelay += this.delay;
        }
      }
    }
  }

  private void dropPreparedFrame() throws IOException {
    writeGraphicCtrlExt(this.preparedFrameDelay); // write graphic control extension
    writeImageDesc(); // image descriptor
    writePreparedIndexes(); // encode and write pixel data
    this.preparedFrameDelay = this.delay;
  }

  private boolean findNewFrameIndexes(final int[] argb) {
    boolean changed = false;
    for (int i = 0; i < this.newFrameIndexes.length; i++) {
      final byte prev = this.newFrameIndexes[i];
      final int rgb = argb[i] & 0xFF_FF_FF;
      final OptionalInt index = this.mapRgb2index.get(rgb);
      final byte newValue;
      if (index.isPresent()) {
        newValue = (byte) index.getAsInt();
      } else {
        int found = 0;
        double distance = Double.MAX_VALUE;

        final int br = (rgb >> 16) & 0xFF;
        final int bg = (rgb >> 8) & 0xFF;
        final int bb = rgb & 0xFF;

        for (int j = 0; j < this.rgbPalette.length; j++) {
          final int prgb = this.rgbPalette[j];
          final int dr = br - ((prgb >> 16) & 0xFF);
          final int dg = bg - ((prgb >> 8) & 0xFF);
          final int db = bb - (prgb & 0xFF);

          final double thisDist = Math.sqrt(dr * dr + dg * dg + db * db);
          if (thisDist < distance) {
            found = j;
            distance = thisDist;
          }
        }
        this.mapRgb2index.put(rgb, found);
        newValue = (byte) found;
      }
      if (prev != newValue) {
        this.newFrameIndexes[i] = newValue;
        changed = true;
      }
    }
    return changed;
  }

  /**
   * Flushes any pending data and closes output file. If writing to an
   * OutputStream, the stream is not closed.
   */
  public void finish() throws IOException {
    if (!this.started) throw new IllegalStateException("Not started yet");
    this.started = false;

    this.dropPreparedFrame();

    this.out.write(0x3b); // gif trailer
    this.out.flush();

    // reset for subsequent use
    out = null;
  }

  /**
   * Initiates GIF file creation on the given stream. The stream is not closed
   * automatically.
   *
   * @param os OutputStream on which GIF images are written.
   * @return false if initial write failed.
   */
  public void start(OutputStream os) throws IOException {
    if (this.started) throw new IllegalStateException("Already started");
    this.started = true;
    this.out = Objects.requireNonNull(os);
    writeString("GIF89a"); // header
    writeLSD(); // logical screen descriptior

    // global palette
    for (int rgb : this.rgbPalette) {
      this.out.write(rgb >> 16);
      this.out.write(rgb >> 8);
      this.out.write(rgb);
    }

    if (this.repeat >= 0) {
      // use NS app extension to indicate reps
      writeNetscapeExt();
    }

    this.firstFrame = true;
  }

  /**
   * Writes Graphic Control Extension
   */
  protected void writeGraphicCtrlExt(final int delayCounter) throws IOException {
    out.write(0x21); // extension introducer
    out.write(0xf9); // GCE label
    out.write(4); // data block size

    // packed fields
    out.write(4);

    writeShort(delayCounter); // delay x 1/100 sec
    out.write(0); // transparent color index
    out.write(0); // block terminator
  }

  /**
   * Writes Image Descriptor
   */
  protected void writeImageDesc() throws IOException {
    out.write(0x2c); // image separator
    writeShort(0); // image position x,y = 0,0
    writeShort(0);
    writeShort(width); // image size
    writeShort(height);
    // GCT in use
    out.write(0);
  }

  /**
   * Writes Logical Screen Descriptor
   */
  protected void writeLSD() throws IOException {
    // logical screen size
    writeShort(width);
    writeShort(height);

    // packed fields
    out.write(0xF7);

    out.write(0); // background color index
    out.write(0); // pixel aspect ratio - assume 1:1
  }

  /**
   * Writes Netscape application extension to define repeat count.
   */
  protected void writeNetscapeExt() throws IOException {
    out.write(0x21); // extension introducer
    out.write(0xff); // app extension label
    out.write(11); // block size
    writeString("NETSCAPE" + "2.0"); // app id + auth code
    out.write(3); // sub-block size
    out.write(1); // loop sub-block id
    writeShort(repeat); // loop count (extra iterations, 0=repeat forever)
    out.write(0); // block terminator
  }

  /**
   * Encodes and writes pixel data
   */
  private void writePreparedIndexes() throws IOException {
    LZWEncoder encoder = new LZWEncoder(this.width, this.height, this.preparedFrameIndexes);
    encoder.encode(out);
  }

  /**
   * Write 16-bit value to output stream, LSB first
   */
  private void writeShort(int value) throws IOException {
    out.write(value);
    out.write(value >> 8);
  }

  /**
   * Writes string to output stream
   */
  private void writeString(String s) throws IOException {
    for (int i = 0; i < s.length(); i++) {
      out.write((byte) s.charAt(i));
    }
  }

// ==============================================================================
// Adapted from Jef Poskanzer's Java port by way of J. M. G. Elliott.
// K Weiner 12/00

  private static final class LZWEncoder {

    static final int BITS = 12;
    static final int HSIZE = 5003; // 80% occupancy
    // Algorithm: use open addressing double hashing (no chaining) on the
    // prefix code / next character combination. We do a variant of Knuth's
    // algorithm D (vol. 3, sec. 6.4) along with G. Knott's relatively-prime
    // secondary probe. Here, the modular division first probe is gives way
    // to a faster exclusive-or manipulation. Also do block compression with
    // an adaptive reset, whereby the code table is cleared when the compression
    // ratio decreases, but after the table fills. The variable-length output
    // codes are re-sized at this point, and a special CLEAR code is generated
    // for the decompressor. Late addition: construct the table according to
    // file size for noticeable speed improvement on small files. Please direct
    // questions about this implementation to ames!jaw.
    static final int[] masks = {0x0000, 0x0001, 0x0003, 0x0007, 0x000F, 0x001F, 0x003F, 0x007F, 0x00FF, 0x01FF,
            0x03FF, 0x07FF, 0x0FFF, 0x1FFF, 0x3FFF, 0x7FFF, 0xFFFF};
    private static final int EOF = -1;
    final int maxbits = BITS; // user settable max # bits/code
    // General DEFINEs
    final int maxmaxcode = 1 << BITS; // should NEVER generate this code

    // GIFCOMPR.C - GIF Image compression routines
    //
    // Lempel-Ziv compression based on 'compress'. GIF modifications by
    // David Rowley (mgardi@watdcsu.waterloo.edu)
    final int[] htab = new int[HSIZE];
    // GIF Image compression - modified 'compress'
    //
    // Based on: compress.c - File compression ala IEEE Computer, June 1984.
    //
    // By Authors: Spencer W. Thomas (decvax!harpo!utah-cs!utah-gr!thomas)
    // Jim McKie (decvax!mcvax!jim)
    // Steve Davies (decvax!vax135!petsd!peora!srd)
    // Ken Turkowski (decvax!decwrl!turtlevax!ken)
    // James A. Woods (decvax!ihnp4!ames!jaw)
    // Joe Orost (decvax!vax135!petsd!joe)
    final int[] codetab = new int[HSIZE];
    final int hsize = HSIZE; // for dynamic table sizing
    // Define the storage for the packet accumulator
    final byte[] accum = new byte[256];
    // output
    //
    // Output the given code.
    // Inputs:
    // code: A n_bits-bit integer. If == -1, then EOF. This assumes
    // that n_bits =< wordsize - 1.
    // Outputs:
    // Outputs code to the file.
    // Assumptions:
    // Chars are 8 bits long.
    // Algorithm:
    // Maintain a BITS character long buffer (so that 8 codes will
    // fit in it exactly). Use the VAX insv instruction to insert each
    // code in turn. When the buffer fills up empty it and start over.
    private final int imgW, imgH;
    private final byte[] pixAry;
    private final int initCodeSize;
    int n_bits; // number of bits/code
    int maxcode; // maximum code, given n_bits
    int free_ent = 0; // first unused entry
    // block compression parameters -- after all codes are used up,
    // and compression rate changes, start over.
    boolean clear_flg = false;
    int g_init_bits;
    int ClearCode;
    int EOFCode;
    int cur_accum = 0;
    int cur_bits = 0;
    // Number of characters so far in this 'packet'
    int a_count;
    private int remaining;
    private int curPixel;

    // ----------------------------------------------------------------------------
    LZWEncoder(int width, int height, byte[] pixels) {
      imgW = width;
      imgH = height;
      pixAry = pixels;
      initCodeSize = 8;
    }

    // Add a character to the end of the current packet, and if it is 254
    // characters, flush the packet to disk.
    void char_out(byte c, OutputStream outs) throws IOException {
      accum[a_count++] = c;
      if (a_count >= 254)
        flush_char(outs);
    }

    // Clear out the hash table

    // table clear for block compress
    void cl_block(OutputStream outs) throws IOException {
      cl_hash(hsize);
      free_ent = ClearCode + 2;
      clear_flg = true;

      output(ClearCode, outs);
    }

    // reset code table
    void cl_hash(int hsize) {
      for (int i = 0; i < hsize; ++i)
        htab[i] = -1;
    }

    void compress(int init_bits, OutputStream outs) throws IOException {
      int fcode;
      int i /* = 0 */;
      int c;
      int ent;
      int disp;
      int hsize_reg;
      int hshift;

      // Set up the globals: g_init_bits - initial number of bits
      g_init_bits = init_bits;

      // Set up the necessary values
      clear_flg = false;
      n_bits = g_init_bits;
      maxcode = MAXCODE(n_bits);

      ClearCode = 1 << (init_bits - 1);
      EOFCode = ClearCode + 1;
      free_ent = ClearCode + 2;

      a_count = 0; // clear packet

      ent = nextPixel();

      hshift = 0;
      for (fcode = hsize; fcode < 65536; fcode *= 2)
        ++hshift;
      hshift = 8 - hshift; // set hash code range bound

      hsize_reg = hsize;
      cl_hash(hsize_reg); // clear hash table

      output(ClearCode, outs);

      outer_loop:
      while ((c = nextPixel()) != EOF) {
        fcode = (c << maxbits) + ent;
        i = (c << hshift) ^ ent; // xor hashing

        if (htab[i] == fcode) {
          ent = codetab[i];
          continue;
        } else if (htab[i] >= 0) // non-empty slot
        {
          disp = hsize_reg - i; // secondary hash (after G. Knott)
          if (i == 0)
            disp = 1;
          do {
            if ((i -= disp) < 0)
              i += hsize_reg;

            if (htab[i] == fcode) {
              ent = codetab[i];
              continue outer_loop;
            }
          } while (htab[i] >= 0);
        }
        output(ent, outs);
        ent = c;
        if (free_ent < maxmaxcode) {
          codetab[i] = free_ent++; // code -> hashtable
          htab[i] = fcode;
        } else
          cl_block(outs);
      }
      // Put out the final code.
      output(ent, outs);
      output(EOFCode, outs);
    }

    // ----------------------------------------------------------------------------
    void encode(OutputStream os) throws IOException {
      os.write(initCodeSize); // write "initial code size" byte

      remaining = imgW * imgH; // reset navigation variables
      curPixel = 0;

      compress(initCodeSize + 1, os); // compress and write the pixel data

      os.write(0); // write block terminator
    }

    // Flush the packet to disk, and reset the accumulator
    void flush_char(OutputStream outs) throws IOException {
      if (a_count > 0) {
        outs.write(a_count);
        outs.write(accum, 0, a_count);
        a_count = 0;
      }
    }

    final int MAXCODE(int n_bits) {
      return (1 << n_bits) - 1;
    }

    // ----------------------------------------------------------------------------
    // Return the next pixel from the image
    // ----------------------------------------------------------------------------
    private int nextPixel() {
      if (remaining == 0)
        return EOF;

      --remaining;

      byte pix = pixAry[curPixel++];

      return pix & 0xff;
    }

    void output(int code, OutputStream outs) throws IOException {
      cur_accum &= masks[cur_bits];

      if (cur_bits > 0)
        cur_accum |= (code << cur_bits);
      else
        cur_accum = code;

      cur_bits += n_bits;

      while (cur_bits >= 8) {
        char_out((byte) (cur_accum & 0xff), outs);
        cur_accum >>= 8;
        cur_bits -= 8;
      }

      // If the next entry is going to be too big for the code size,
      // then increase it, if possible.
      if (free_ent > maxcode || clear_flg) {
        if (clear_flg) {
          maxcode = MAXCODE(n_bits = g_init_bits);
          clear_flg = false;
        } else {
          ++n_bits;
          if (n_bits == maxbits)
            maxcode = maxmaxcode;
          else
            maxcode = MAXCODE(n_bits);
        }
      }

      if (code == EOFCode) {
        // At EOF, write the rest of the buffer.
        while (cur_bits > 0) {
          char_out((byte) (cur_accum & 0xff), outs);
          cur_accum >>= 8;
          cur_bits -= 8;
        }

        flush_char(outs);
      }
    }
  }
}

