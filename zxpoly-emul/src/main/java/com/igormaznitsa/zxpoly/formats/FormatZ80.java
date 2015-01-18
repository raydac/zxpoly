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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.mapper.*;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.VideoController;
import com.igormaznitsa.zxpoly.components.ZXPolyModule;
import java.io.*;
import java.lang.reflect.Field;
import java.util.*;

public class FormatZ80 extends Snapshot {

  private static final int VERSION_1 = 0;
  private static final int VERSION_2 = 1;
  private static final int VERSION_3A = 2;
  private static final int VERSION_3B = 3;

  class EmulFlags {

    @Bin(outOrder = 1, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    byte interruptmode;
    @Bin(outOrder = 2, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    byte issue2emulation;
    @Bin(outOrder = 3, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    byte doubleintfreq;
    @Bin(outOrder = 4, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    byte videosync;
    @Bin(outOrder = 5, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    byte inputdevice;
  }

  class Flags {

    @Bin(outOrder = 1, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    byte reg_r_bit7;
    @Bin(outOrder = 2, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_3)
    byte bordercolor;
    @Bin(outOrder = 3, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    byte basic_samrom;
    @Bin(outOrder = 4, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    byte compressed;
    @Bin(outOrder = 5, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    byte nomeaning;
  }

  static class Bank {

    final int page;
    final byte[] data;

    Bank(final int page, final byte[] data) {
      this.page = page;
      this.data = data;
    }
  }

  class Z80Snapshot {

    @Bin(outOrder = 1)
    byte reg_a;
    @Bin(outOrder = 2)
    byte reg_f;
    @Bin(outOrder = 3)
    short reg_bc;
    @Bin(outOrder = 4)
    short reg_hl;
    @Bin(outOrder = 5)
    short reg_pc;
    @Bin(outOrder = 6)
    short reg_sp;
    @Bin(outOrder = 7)
    byte reg_ir;
    @Bin(outOrder = 8)
    byte reg_r;

    @Bin(outOrder = 9)
    Flags flags;

    @Bin(outOrder = 10)
    short reg_de;
    @Bin(outOrder = 11)
    short reg_bc_alt;
    @Bin(outOrder = 12)
    short reg_de_alt;
    @Bin(outOrder = 13)
    short reg_hl_alt;
    @Bin(outOrder = 14)
    byte reg_a_alt;
    @Bin(outOrder = 15)
    byte reg_f_alt;
    @Bin(outOrder = 16)
    short reg_iy;
    @Bin(outOrder = 17)
    short reg_ix;
    @Bin(outOrder = 18)
    byte iff;
    @Bin(outOrder = 19)
    byte iff2;

    @Bin(outOrder = 20)
    EmulFlags emulFlags;

    @Bin(outOrder = 21, custom = true)
    byte[] data;

    // version 2,3A
    @Bin(type = BinType.USHORT)
    int reg_pc2;

    @Bin(type = BinType.UBYTE)
    int mode;

    @Bin(type = BinType.UBYTE)
    int port7FFD;

    @Bin(type = BinType.UBYTE)
    int portFF;

    @Bin(custom = true)
    Bank[] banks;
  }

  private int version;

  private static final JBBPParser Z80_VERSION1 = JBBPParser.prepare(
          "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "byte [_] data;"
  );

  private static final JBBPParser Z80_VERSION2 = JBBPParser.prepare(
          "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "skip:2;" // header length
          + "<ushort reg_pc2;"
          + "ubyte mode;"
          + "ubyte port7FFD;"
          + "ubyte portFF;"
          + "skip:18;"// misc non zx or not supported stuff 
          + "byte [_] data;"
  );

  private static final JBBPParser Z80_VERSION3A = JBBPParser.prepare(
          "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "skip:2;" // header length
          + "<ushort reg_pc2;"
          + "ubyte mode;"
          + "ubyte port7FFD;"
          + "ubyte portFF;"
          + "skip:49;" // misc non zx or not supported stuff 
          + "byte [_] data;"
  );

  private static final JBBPParser Z80_VERSION3B = JBBPParser.prepare(
          "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "skip:2;" // header length
          + "<ushort reg_pc2;"
          + "ubyte mode;"
          + "ubyte port7FFD;"
          + "ubyte portFF;"
          + "skip:50;" // misc non zx or not supported stuff 
          + "byte [_] data;"
  );

  private Z80Snapshot current;

  private static class DataProcessor implements JBBPMapperCustomFieldProcessor {

    private final int version;

    private DataProcessor(final int version) {
      this.version = version;
    }

    @Override
    public Object prepareObjectForMapping(JBBPFieldStruct parsedBlock, Bin annotation, Field field) {
      if (this.version == VERSION_1) {
        if (field.getName().equals("data")) {
          final byte[] data = parsedBlock.findFieldForNameAndType("data", JBBPFieldArrayByte.class).getArray();

          if (parsedBlock.findFieldForPathAndType("flags.compressed", JBBPFieldBit.class).getAsBool()) {
            // RLE compressed
            final ByteArrayOutputStream baos = new ByteArrayOutputStream(data.length << 1);
            int i = 0;

            final int len = data.length - 4;

            while (i < len) {
              final int a = data[i++] & 0xFF;
              if (a == 0xED) {
                final int b = data[i++] & 0xFF;
                if (b == 0xED) {
                  int num = data[i++] & 0xFF;
                  final int val = data[i++] & 0xFF;
                  while (num > 0) {
                    baos.write(val);
                    num--;
                  }
                }
                else {
                  baos.write(a);
                  baos.write(b);
                }
              }
              else {
                baos.write(a);
              }
            }
            return baos.toByteArray();
          }
          else {
            // uncompressed
            return data;
          }
        }
        else {
          return null;
        }
      }
      else {
        if (field.getName().equalsIgnoreCase("data")) {
          return parsedBlock.findFieldForNameAndType("data", JBBPFieldArrayByte.class).getArray();
        }
        else if (field.getName().equalsIgnoreCase("banks")) {
          final byte[] rawdata = parsedBlock.findFieldForNameAndType("data", JBBPFieldArrayByte.class).getArray();
          int pos = 0;
          int len = rawdata.length;
          final List<Bank> banks = new ArrayList<Bank>();
          while (len > 0) {
            final int blocklength = ((rawdata[pos++] & 0xFF)) | ((rawdata[pos++] & 0xFF) << 8);
            final int page = rawdata[pos++] & 0xFF;
            len -= 3 + (blocklength == 0xFFFF ? 16384 : blocklength);
            final byte[] uncompressed = blocklength == 0xFFFF ? Arrays.copyOfRange(rawdata, pos, 16384) : unpack(rawdata, pos, blocklength);
            pos += blocklength;
            banks.add(new Bank(page, uncompressed));
          }
          return banks.toArray(new Bank[banks.size()]);
        }
        else {
          return null;
        }
      }
    }

    private byte[] unpack(final byte[] src, int srcoffset, int srclen) {
      final ByteArrayOutputStream result = new ByteArrayOutputStream(16384);
      while (srclen > 0) {
        if (srclen >= 4 && src[srcoffset] == (byte) 0xED && src[srcoffset + 1] == (byte) 0xED) {
          srcoffset += 2;
          final int len = src[srcoffset++] & 0xFF;
          final int value = src[srcoffset++] & 0xFF;
          for (int i = len; i > 0; i--) {
            result.write(value);
          }
          srclen -= 4;
        }
        else {
          result.write(src[srcoffset++]);
          srclen--;
        }
      }
      return result.toByteArray();
    }
  }

  @Override
  public void fillModule(final ZXPolyModule module, final VideoController vc) {
    final Z80 cpu = module.getCPU();
    cpu.doReset();

    cpu.setRegister(Z80.REG_A, current.reg_a);
    cpu.setRegister(Z80.REG_F, current.reg_f);
    cpu.setRegister(Z80.REG_A, current.reg_a_alt, true);
    cpu.setRegister(Z80.REG_F, current.reg_f_alt, true);
    cpu.setRegisterPair(Z80.REGPAIR_BC, current.reg_bc);
    cpu.setRegisterPair(Z80.REGPAIR_BC, current.reg_bc_alt, true);
    cpu.setRegisterPair(Z80.REGPAIR_DE, current.reg_de);
    cpu.setRegisterPair(Z80.REGPAIR_DE, current.reg_de_alt, true);
    cpu.setRegisterPair(Z80.REGPAIR_HL, current.reg_hl);
    cpu.setRegisterPair(Z80.REGPAIR_HL, current.reg_hl_alt, true);

    cpu.setRegister(Z80.REG_IX, current.reg_ix);
    cpu.setRegister(Z80.REG_IY, current.reg_iy);

    cpu.setRegister(Z80.REG_R, current.reg_r);
    cpu.setRegister(Z80.REG_SP, current.reg_sp);

    if (this.version == VERSION_1) {
      cpu.setRegister(Z80.REG_PC, current.reg_pc);
    }
    else {
      cpu.setRegister(Z80.REG_PC, current.reg_pc2);
    }

    cpu.setIFF(current.iff != 0, current.iff2 != 0);

    cpu.setRegister(Z80.REG_I, current.reg_ir);
    cpu.setIM(current.emulFlags.interruptmode);

    switch (this.version) {
      case VERSION_1: {
        for (int i = 0; i < current.data.length; i++) {
          module.writeMemory(cpu, i + 16384, current.data[i]);
        }
      }
      break;
      default: {
        if (is48k()) {
          for (final Bank b : this.current.banks) {
            int offset = -1;
            switch (b.page) {
              case 4: {
                offset = 0x8000;
              }
              break;
              case 5: {
                offset = 0xC000;
              }
              break;
              case 8: {
                offset = 0x4000;
              }
              break;
            }
            if (offset >= 0) {
              for (int i = 0; i < 16384; i++) {
                module.writeMemory(cpu, offset + i, b.data[i]);
              }
            }
          }
        }
        else {
          module.set7FFD(this.current.port7FFD, true);
          for (final Bank b : this.current.banks) {
            if (b.page >= 3 && b.page < 10) {
              final int offset = (b.page - 3) * 0x4000;
              for (int i = 0; i < 16384; i++) {
                module.writeHeapModuleMemory(offset + i, b.data[i]);
              }
            }
          }
        }
      }
      break;
    }

    vc.setBorderColor(current.flags.bordercolor);
  }

  @Override
  public boolean load(final byte[] array) throws IOException {
    final JBBPParser parser;

    if ((array[6] | array[7]) == 0) {
      switch (((array[31] & 0xFF) << 8 | (array[30] & 0xFF))) {
        case 23: { // Verison 2
          parser = Z80_VERSION2;
          version = VERSION_2;
        }
        break;
        case 54: { // Version 3a
          parser = Z80_VERSION3A;
          version = VERSION_3A;
        }
        break;
        case 55: { // Version 3b
          parser = Z80_VERSION3B;
          version = VERSION_3B;
        }
        break;
        default:
          throw new IOException("Detected unknown Z80 snapshot version");
      }
    }
    else {
      parser = Z80_VERSION1;
      version = VERSION_1;
    }

    current = parser.parse(array).mapTo(Z80Snapshot.class, new DataProcessor(this.version), JBBPMapper.FLAG_IGNORE_MISSING_VALUES);

    return is48k();
  }

  @Override
  public String getName() {
    return "Z80 snapshot";
  }

  @Override
  public boolean accept(final File f) {
    return f != null && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".z80"));
  }

  @Override
  public String getDescription() {
    return "Z80 Snapshot (*.z80)";
  }

  public boolean is48k() {
    switch (version) {
      case VERSION_1:
        return true;
      case VERSION_2: {
        return this.current.mode == 0 || this.current.mode == 1;
      }
      case VERSION_3A:
      case VERSION_3B: {
        return this.current.mode == 0 || this.current.mode == 1 || this.current.mode == 3;
      }
      default:
        return false;
    }
  }

}
