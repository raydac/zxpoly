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
import com.igormaznitsa.zxpoly.components.BoardMode;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import com.igormaznitsa.zxpoly.components.video.VideoController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FormatZ80 extends Snapshot {

  private static final int PAGE_SIZE = 0x4000;

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
  public boolean canMakeSnapshotForBoardMode(final BoardMode mode) {
    return mode == BoardMode.ZX128 || mode == BoardMode.SPEC256;
  }

  @Override
  public byte[] saveToArray(Motherboard board, VideoController vc) throws IOException {
    final Z80V3AParser parser = new Z80V3AParser();
    final Z80 cpu = board.getMasterCpu();

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
    final boolean mode48 = isMode48(module);

    final Z80V3AParser.FLAGS flags = parser.makeFLAGS();

    flags.setREG_R_BIT7((byte) (cpu.getRegister(Z80.REG_R) >>> 7));
    flags.setBORDERCOLOR((byte) vc.getPortFE());
    flags.setBASIC_SAMROM((byte) 0);
    flags.setCOMPRESSED((byte) 0);
    flags.setNOMEANING((byte) 0);

    parser.setPORT7FFD((char) module.read7FFD());
    parser.setPORTFF((char) module.getMotherboard().readBusIo(module, 0xFF));

    parser.setHEADERLEN((char) 54);
    parser.setMODE(mode48 ? (char) 0 : (char) 4);
    parser.setMISCNONZX(new byte[49]);

    ByteArrayOutputStream bos = new ByteArrayOutputStream();
    if (mode48) {
      // save non compressed data blocks
      final byte[] page4000 = module.makeCopyOfZxMemPage(1);
      final byte[] page8000 = module.makeCopyOfZxMemPage(2);
      final byte[] pageC000 = module.makeCopyOfZxMemPage(3);
      new Bank(4, page8000).writeNonCompressed(bos);
      new Bank(5, pageC000).writeNonCompressed(bos);
      new Bank(8, page4000).writeNonCompressed(bos);
    } else {
      // save non compressed data blocks
      for (int i = 0; i < 8; i++) {
        final byte[] data = module.makeCopyOfHeapPage(i);
        new Bank(i + 3, data).writeNonCompressed(bos);
      }
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
      throw new IOException("File is too short one to be Z80 snapshot");
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
        LOGGER.info("Z80 snapshot v1 " + (snapshot.getFLAGS().getCOMPRESSED() == 0 ? "" : "(compressed)"));
      }
      break;
      case VERSION_2: {
        LOGGER.info("Z80 snapshot v2" + (snapshot.getFLAGS().getCOMPRESSED() == 0 ? "" : "(compressed)"));
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
        LOGGER.info("Z80 snapshot v3" + (version == VERSION_3A ? "A" : "B") + (snapshot.getFLAGS().getCOMPRESSED() == 0 ? "" : "(compressed)"));
        switch (snapshot.getMODE()) {
          case 0:
            LOGGER.info("Mode 48k");
            break;
          case 1:
            LOGGER.info("Mode 48k+If.1");
            break;
          case 3:
            LOGGER.info("Mode 48k+M.G.T.");
            break;
          case 4:
            LOGGER.info("Mode 128k");
            break;
          case 5:
            LOGGER.info("Mode 128k+If.1");
            break;
          case 6:
            LOGGER.info("Mode 128k+M.G.T.");
            break;
          default:
            throw new IOException("Unsupported Z80 hardware mode [" + snapshot.getMODE() + ']');
        }
      }
      break;
      default:
        throw new IllegalArgumentException("Unexpected Z80 snapshot version: " + version);
    }

    final boolean snapshot48k = is48k(version, snapshot);

    if (snapshot48k) {
      doMode48(board);
    } else {
      doMode128(board);
    }

    final Z80 cpu = board.getMasterCpu();

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

    cpu.setRegister(Z80.REG_R, snapshot.getREG_R() | (snapshot.getFLAGS().getREG_R_BIT7() << 7));
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

    if (version == VERSION_1) {
      ((Z80V1Parser) snapshot).setDATA(snapshot.getFLAGS().getCOMPRESSED() == 0 ? snapshot.getDATA() : Bank.decodeRLE(snapshot.getDATA(), 0, snapshot.getDATA().length));
      for (int i = 0; i < snapshot.getDATA().length; i++) {
        module.writeMemory(cpu, 0, i + PAGE_SIZE, snapshot.getDATA()[i]);
      }
    } else {
      final Bank[] banks = Bank.toBanks(snapshot.getDATA());

      if (snapshot48k) {
        for (final Bank b : banks) {
          final int offset;
          switch (b.page) {
            case 4: {
              offset = PAGE_SIZE * 2;
            }
            break;
            case 5: {
              offset = PAGE_SIZE * 3;
            }
            break;
            case 8: {
              offset = PAGE_SIZE;
            }
            break;
            default:
              throw new IllegalArgumentException("Detected unexpected bank page index: " + b.page);
          }
          for (int i = 0; i < PAGE_SIZE; i++) {
            module.writeMemory(cpu, 0, offset + i, b.data[i]);
          }
        }
      } else {
        module.write7FFD(snapshot.getPORT7FFD(), true);
        for (final Bank b : banks) {
          if (b.page < 3) {
            module.writeHeapPage(b.page, b.data);
          } else if (b.page <= 10) {
            module.writeHeapPage(b.page - 3, b.data);
          } else {
            throw new IOException("Unexpected page index (x>10):" + b.page);
          }
        }
      }
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
      this.read(new JBBPBitInputStream(new ByteArrayInputStream(array), false));
    }
  }

  private static class Bank {

    final int page;
    final byte[] data;

    Bank(final int page, final byte[] data) {
      this.page = page;
      this.data = data;
    }

    static byte[] decodeRLE(final byte[] source, int offset, int length) {
      // RLE packed
      // 0xED 0xED repeat value
      // 0x00 0xED 0xED 0x00 - END marker
      final ByteArrayOutputStream result = new ByteArrayOutputStream(PAGE_SIZE);
      while (length > 0) {
        if (length >= 4) {
          if (source[offset] == (byte) 0x00
                  && source[offset + 1] == (byte) 0xED
                  && source[offset + 2] == (byte) 0xED
                  && source[offset + 3] == (byte) 0x00) {
            length = 0;
          } else if (source[offset] == (byte) 0xED && source[offset + 1] == (byte) 0xED) {
            offset += 2;
            int repeat = source[offset++] & 0xFF;
            final int value = source[offset++] & 0xFF;
            length -= 4;
            while (repeat > 0) {
              result.write(value);
              repeat--;
            }
          } else {
            result.write(source[offset++]);
            length--;
          }
        } else {
          result.write(source[offset++]);
          length--;
        }
      }
      return result.toByteArray();
    }

    static byte[] unpackBank(final byte[] src, int offset, int length) {
      final byte[] result;
      if (length == 0xFFFF) {
        result = new byte[PAGE_SIZE];
        System.arraycopy(src, offset, result, 0, PAGE_SIZE);
      } else {
        result = decodeRLE(src, offset, length);
      }
      return result;
    }

    static Bank[] toBanks(final byte[] data) {
      int pos = 0;
      int len = data.length;
      final List<Bank> banks = new ArrayList<>();
      while (len > 0) {
        final int blocklength = ((data[pos++] & 0xFF)) | ((data[pos++] & 0xFF) << 8);
        final int page = data[pos++] & 0xFF;
        len -= 3 + (blocklength == 0xFFFF ? PAGE_SIZE : blocklength);
        final byte[] uncompressed = unpackBank(data, pos, blocklength);
        pos += (blocklength == 0xFFFF ? PAGE_SIZE : blocklength);
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
