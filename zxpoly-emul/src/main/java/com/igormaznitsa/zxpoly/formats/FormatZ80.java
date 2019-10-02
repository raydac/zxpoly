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

package com.igormaznitsa.zxpoly.formats;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.VideoController;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FormatZ80 extends Snapshot {

  private static final int VERSION_1 = 0;
  private static final int VERSION_2 = 1;
  private static final int VERSION_3A = 2;
  private static final int VERSION_3B = 3;

  private static boolean is48k(final int version, final AbstractZ80Snapshot snapshot) {
    switch (version) {
      case VERSION_1:
        return true;
      case VERSION_2: {
        return snapshot.getMODE() == 0 || snapshot.getMODE() == 1;
      }
      case VERSION_3A:
      case VERSION_3B: {
        return snapshot.getMODE() == 0 || snapshot.getMODE() == 1 || snapshot.getMODE() == 3;
      }
      default:
        return false;
    }
  }

  @Override
  public byte[] saveToArray(Motherboard board, VideoController vc) throws IOException {
    final Z80V3AParser parser = new Z80V3AParser();
    final Z80 cpu = board.getCPU0();

    parser.setREG_A((byte) cpu.getRegister(Z80.REG_A));
    parser.setREG_F((byte) cpu.getRegister(Z80.REG_F));

    parser.setREG_A_ALT((byte) cpu.getRegister(Z80.REG_A, true));
    parser.setREG_F_ALT((byte) cpu.getRegister(Z80.REG_F, true));

    parser.setREG_BC((short) cpu.getRegisterPair(Z80.REGPAIR_BC));
    parser.setREG_BC_ALT((short) cpu.getRegisterPair(Z80.REGPAIR_BC, true));

    parser.setREG_DE((short) cpu.getRegisterPair(Z80.REGPAIR_DE));
    parser.setREG_DE_ALT((short) cpu.getRegisterPair(Z80.REGPAIR_DE, true));

    parser.setREG_HL((short) cpu.getRegisterPair(Z80.REGPAIR_HL));
    parser.setREG_HL_ALT((short) cpu.getRegisterPair(Z80.REGPAIR_HL, true));

    parser.setREG_IX((short) cpu.getRegister(Z80.REG_IX));
    parser.setREG_IY((short) cpu.getRegister(Z80.REG_IY));

    parser.setREG_R((byte) cpu.getRegister(Z80.REG_R));
    parser.setREG_SP((short) cpu.getRegister(Z80.REG_SP));

    parser.setREG_PC((short) 0);
    parser.setREG_PC2((short) cpu.getRegister(Z80.REG_PC));

    parser.setIFF((byte) (cpu.isIFF1() ? 1 : 0));
    parser.setIFF2((byte) (cpu.isIFF2() ? 1 : 0));

    parser.setREG_IR((byte) cpu.getRegister(Z80.REG_I));

    final Z80V3AParser.EMULFLAGS emulflags = parser.makeEMULFLAGS();

    emulflags.setINTERRUPTMODE((byte) cpu.getIM());
    emulflags.setISSUE2EMULATION((byte) 0);
    emulflags.setDOUBLEINTFREQ((byte) 0);
    emulflags.setVIDEOSYNC((byte) 0);
    emulflags.setINPUTDEVICE((byte) 0);

    final ZxPolyModule module = board.getModules()[0];

    final Z80V3AParser.FLAGS flags = parser.makeFLAGS();

    flags.setREG_R_BIT7((byte) (cpu.getRegister(Z80.REG_R) >>> 7));
    flags.setBORDERCOLOR((byte) vc.getPortFE());
    flags.setBASIC_SAMROM((byte) 0);
    flags.setCOMPRESSED((byte) 0);
    flags.setNOMEANING((byte) 0);

    parser.setPORT7FFD((char) module.read7FFD());
    parser.setPORTFF((char) module.readIo(module, 0xFF));

    parser.setHEADERLEN((char) 54);
    parser.setMODE((char) 4);
    parser.setMISCNONZX(new byte[49]);

    // save non compressed data blocks
    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    for (int i = 0; i < 8; i++) {
      int addr = i * 0x4000;
      final byte[] data = new byte[0x4000];
      for (int x = 0; x < 0x4000; x++) {
        data[x] = (byte) module.readHeap(addr++);
      }
      new Bank(i + 3, data).writeNonCompressed(bos);
    }
    bos.flush();
    bos.close();
    parser.setDATA(bos.toByteArray());

    bos = new ByteArrayOutputStream();
    try (JBBPBitOutputStream bitOut = new JBBPBitOutputStream(bos)) {
      parser.write(bitOut);
    }

    return bos.toByteArray();
  }

  @Override
  public String getExtension() {
    return "z80";
  }

  @Override
  public void loadFromArray(final File srcFile, final Motherboard board, final VideoController vc, final byte[] array) throws IOException {
    if (array.length < 30) {
      throw new IOException("File is too short to be Z80 snapshot");
    }

    final AbstractZ80Snapshot snapshot;

    final int version;

    if ((array[6] | array[7]) == 0) {
      switch (((array[31] & 0xFF) << 8 | (array[30] & 0xFF))) {
        case 23: { // Verison 2
          snapshot = new Z80V2Parser();
          version = VERSION_2;
        }
        break;
        case 54: { // Version 3a
          snapshot = new Z80V3AParser();
          version = VERSION_3A;
        }
        break;
        case 55: { // Version 3b
          snapshot = new Z80V3BParser();
          version = VERSION_3B;
        }
        break;
        default:
          throw new IOException("Detected unknown Z80 snapshot version");
      }
    } else {
      snapshot = new Z80V1Parser();
      version = VERSION_1;
    }

    snapshot.fillFromArray(array);

    // check hardware mode
    switch (version) {
      case VERSION_1: {
      }
      break;
      case VERSION_2: {
        switch (snapshot.getMODE()) {
          case 0:
          case 1:
          case 3:
          case 4:
            break;
          default:
            throw new IOException("Unsupported Z80 hardware mode [" + snapshot.getMODE() + ']');
        }
      }
      break;
      case VERSION_3A:
      case VERSION_3B: {
        switch (snapshot.getMODE()) {
          case 0:
          case 1:
          case 3:
          case 4:
          case 5:
          case 6:
            break;
          default:
            throw new IOException("Unsupported Z80 hardware mode [" + snapshot.getMODE() + ']');
        }
      }
      break;
    }

    final boolean mode48 = is48k(version, snapshot);

    if (mode48) {
      doMode48(board);
    } else {
      doMode128(board);
    }

    final Z80 cpu = board.getCPU0();

    cpu.setRegister(Z80.REG_A, snapshot.getREG_A());
    cpu.setRegister(Z80.REG_F, snapshot.getREG_F());
    cpu.setRegister(Z80.REG_A, snapshot.getREG_A_ALT(), true);
    cpu.setRegister(Z80.REG_F, snapshot.getREG_F_ALT(), true);
    cpu.setRegisterPair(Z80.REGPAIR_BC, snapshot.getREG_BC());
    cpu.setRegisterPair(Z80.REGPAIR_BC, snapshot.getREG_BC_ALT(), true);
    cpu.setRegisterPair(Z80.REGPAIR_DE, snapshot.getREG_DE());
    cpu.setRegisterPair(Z80.REGPAIR_DE, snapshot.getREG_DE_ALT(), true);
    cpu.setRegisterPair(Z80.REGPAIR_HL, snapshot.getREG_HL());
    cpu.setRegisterPair(Z80.REGPAIR_HL, snapshot.getREG_HL_ALT(), true);

    cpu.setRegister(Z80.REG_IX, snapshot.getREG_IX());
    cpu.setRegister(Z80.REG_IY, snapshot.getREG_IY());

    cpu.setRegister(Z80.REG_R, snapshot.getREG_R());
    cpu.setRegister(Z80.REG_SP, snapshot.getREG_SP());

    if (version == VERSION_1) {
      cpu.setRegister(Z80.REG_PC, snapshot.getREG_PC());
    } else {
      cpu.setRegister(Z80.REG_PC, snapshot.getREG_PC2());
    }

    cpu.setIFF(snapshot.getIFF() != 0, snapshot.getIFF2() != 0);

    cpu.setRegister(Z80.REG_I, snapshot.getREG_IR());
    cpu.setIM(snapshot.getEMULFLAGS().getINTERRUPTMODE());

    final ZxPolyModule module = board.getModules()[0];

    switch (version) {
      case VERSION_1: {
        ((Z80V1Parser) snapshot).setDATA(Bank.decodeRLE(snapshot.getDATA()));
        for (int i = 0; i < snapshot.getDATA().length; i++) {
          module.writeMemory(cpu, i + 16384, snapshot.getDATA()[i]);
        }
      }
      break;
      default: {
        final Bank[] banks = Bank.toBanks(snapshot.getDATA());

        if (mode48) {
          for (final Bank b : banks) {
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
        } else {
          module.write7FFD(snapshot.getPORT7FFD(), true);
          for (final Bank b : banks) {
            if (b.page >= 3 && b.page < 10) {
              final int offset = (b.page - 3) * 0x4000;
              for (int i = 0; i < 16384; i++) {
                module.writeHeap(offset + i, b.data[i]);
              }
            }
          }
        }
      }
      break;
    }

    vc.setBorderColor(snapshot.getFLAGS().getBORDERCOLOR());
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

  public interface Z80EmulFlags {

    byte getINTERRUPTMODE();

    byte getISSUE2EMULATION();

    byte getDOUBLEINTFREQ();

    byte getVIDEOSYNC();

    byte getINPUTDEVICE();
  }

  public interface Z80Flags {

    byte getREG_R_BIT7();

    byte getBORDERCOLOR();

    byte getBASIC_SAMROM();

    byte getCOMPRESSED();

    byte getNOMEANING();
  }

  public static abstract class AbstractZ80Snapshot {

    public abstract byte getREG_A();

    public abstract byte getREG_F();

    public abstract short getREG_BC();

    public abstract short getREG_HL();

    public abstract short getREG_PC();

    public abstract short getREG_SP();

    public abstract byte getREG_IR();

    public abstract byte getREG_R();

    public abstract Z80Flags getFLAGS();

    public abstract short getREG_DE();

    public abstract short getREG_BC_ALT();

    public abstract short getREG_DE_ALT();

    public abstract short getREG_HL_ALT();

    public abstract byte getREG_A_ALT();

    public abstract byte getREG_F_ALT();

    public abstract short getREG_IY();

    public abstract short getREG_IX();

    public abstract byte getIFF();

    public abstract byte getIFF2();

    public abstract Z80EmulFlags getEMULFLAGS();

    public abstract byte[] getDATA();

    public short getREG_PC2() {
      throw new Error("Must not be called directly");
    }

    public char getMODE() {
      throw new Error("Must not be called directly");
    }

    public char getPORT7FFD() {
      throw new Error("Must not be called directly");
    }

    public char getPORTFF() {
      throw new Error("Must not be called directly");
    }

    public abstract AbstractZ80Snapshot read(JBBPBitInputStream in) throws IOException;

    public void fillFromArray(final byte[] array) throws IOException {
      this.read(new JBBPBitInputStream(new ByteArrayInputStream(array)));
    }
  }

  private static class Bank {

    final int page;
    final byte[] data;

    Bank(final int page, final byte[] data) {
      this.page = page;
      this.data = data;
    }

    static byte[] decodeRLE(final byte[] data) {
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
          } else {
            baos.write(a);
            baos.write(b);
          }
        } else {
          baos.write(a);
        }
      }
      return baos.toByteArray();
    }

    static byte[] unpackBank(final byte[] src, int srcoffset, int srclen) {
      final ByteArrayOutputStream result = new ByteArrayOutputStream(16384);
      if (srclen == 0xFFFF) {
        // non packed
        int len = 0x4000;
        while (len > 0) {
          result.write(src[srcoffset++]);
          len--;
        }
      } else {
        while (srclen > 0) {
          if (srclen >= 4 && src[srcoffset] == (byte) 0xED && src[srcoffset + 1] == (byte) 0xED) {
            srcoffset += 2;
            final int len = src[srcoffset++] & 0xFF;
            final int value = src[srcoffset++] & 0xFF;
            for (int i = len; i > 0; i--) {
              result.write(value);
            }
            srclen -= 4;
          } else {
            result.write(src[srcoffset++]);
            srclen--;
          }
        }
      }
      return result.toByteArray();
    }

    static Bank[] toBanks(final byte[] data) {
      int pos = 0;
      int len = data.length;
      final List<Bank> banks = new ArrayList<>();
      while (len > 0) {
        final int blocklength = ((data[pos++] & 0xFF)) | ((data[pos++] & 0xFF) << 8);
        final int page = data[pos++] & 0xFF;
        len -= 3 + (blocklength == 0xFFFF ? 0x4000 : blocklength);
        final byte[] uncompressed = unpackBank(data, pos, blocklength);
        pos += (blocklength == 0xFFFF ? 0x4000 : blocklength);
        banks.add(new Bank(page, uncompressed));
      }
      return banks.toArray(new Bank[0]);
    }

    void writeNonCompressed(final OutputStream out) throws IOException {
      out.write(0xFF);
      out.write(0xFF);
      out.write(this.page);
      out.write(this.data);
    }
  }

}
