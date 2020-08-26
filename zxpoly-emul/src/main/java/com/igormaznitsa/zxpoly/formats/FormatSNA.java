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
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import com.igormaznitsa.zxpoly.components.video.VideoController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.stream.IntStream;

public class FormatSNA extends Snapshot {

  public FormatSNA() {
  }

  @Override
  public String getExtension() {
    return "sna";
  }

  @Override
  public void loadFromArray(final File srcFile, final Motherboard board, final VideoController vc, final byte[] array) throws IOException {
    final SNAParser parser = new SNAParser().read(new JBBPBitInputStream(new ByteArrayInputStream(array)));
    final boolean sna128 = array.length > 49179;

    if (sna128) {
      doMode128(board);
    } else {
      doMode48(board);
    }

    final ZxPolyModule module = board.getModules()[0];
    final Z80 cpu = board.getMasterCpu();

    cpu.setRegisterPair(Z80.REGPAIR_AF, parser.getREGAF());
    cpu.setRegisterPair(Z80.REGPAIR_BC, parser.getREGBC());
    cpu.setRegisterPair(Z80.REGPAIR_DE, parser.getREGDE());
    cpu.setRegisterPair(Z80.REGPAIR_HL, parser.getREGHL());

    cpu.setRegisterPair(Z80.REGPAIR_AF, parser.getALTREGAF(), true);
    cpu.setRegisterPair(Z80.REGPAIR_BC, parser.getALTREGBC(), true);
    cpu.setRegisterPair(Z80.REGPAIR_DE, parser.getALTREGDE(), true);
    cpu.setRegisterPair(Z80.REGPAIR_HL, parser.getALTREGHL(), true);

    cpu.setRegister(Z80.REG_IX, parser.getREGIX());
    cpu.setRegister(Z80.REG_IY, parser.getREGIY());

    cpu.setRegister(Z80.REG_I, parser.getREGI());
    cpu.setRegister(Z80.REG_R, parser.getREGR());

    cpu.setIM(parser.getINTMODE());
    final boolean iff = (parser.getIFF2() & 4) != 0;
    cpu.setIFF(iff, iff);

    vc.writeIo(module, 0xFE, parser.getBORDERCOLOR());
    vc.setBorderColor(parser.getBORDERCOLOR());

    if (sna128) {
      final int offsetpage2 = 0x8000;
      final int offsetpage5 = 0x14000;
      final int offsetpageTop = (parser.getEXTENDEDDATA().getPORT7FFD() & 7) * 0x4000;

      final int[] bankIndex = new int[] {0, 1, 2, 3, 4, 5, 6, 7};
      bankIndex[2] = -1;
      bankIndex[5] = -1;
      bankIndex[parser.getEXTENDEDDATA().getPORT7FFD() & 7] = -1;

      module.syncWriteHeapPage(5, Arrays.copyOfRange(parser.ramdump, 0, 0x4000));
      module.syncWriteHeapPage(2, Arrays.copyOfRange(parser.ramdump, 0x4000, 0x8000));
      module.syncWriteHeapPage(parser.getEXTENDEDDATA().getPORT7FFD() & 7,
          Arrays.copyOfRange(parser.ramdump, 0x8000, 0xC000));

      cpu.setRegister(Z80.REG_PC, parser.getEXTENDEDDATA().getREGPC());
      cpu.setRegister(Z80.REG_SP, parser.getREGSP());
      module.write7FFD(parser.getEXTENDEDDATA().getPORT7FFD(), true);
      module.setTrdosActive(parser.getEXTENDEDDATA().getONTRDOS() != 0);

      int extraBankIndex = 0;
      for (int i = 0; i < 8 && extraBankIndex < parser.getEXTENDEDDATA().getEXTRABANK().length;
           i++) {
        if (bankIndex[i] < 0) {
          continue;
        }
        module.syncWriteHeapPage(bankIndex[i],
            parser.getEXTENDEDDATA().getEXTRABANK()[extraBankIndex++].getDATA());
      }

    } else {
      for (int i = 0; i < parser.getRAMDUMP().length; i++) {
        module.writeMemory(cpu, 0, 0x4000 + i, parser.getRAMDUMP()[i]);
      }

      int regsp = parser.getREGSP();
      final int lowaddr = parser.getRAMDUMP()[regsp - 0x4000] & 0xFF;
      regsp = (regsp + 1) & 0xFFFF;
      final int highaddr = parser.getRAMDUMP()[regsp - 0x4000] & 0xFF;
      regsp = (regsp + 1) & 0xFFFF;
      parser.setREGSP((char) regsp);
      final int startAddress = (highaddr << 8) | lowaddr;

      cpu.setRegister(Z80.REG_SP, parser.getREGSP());
      cpu.setRegister(Z80.REG_PC, startAddress);
    }
  }

  @Override
  public byte[] saveToArray(Motherboard board, VideoController vc) throws IOException {
    final SNAParser parser = new SNAParser();

    final ZxPolyModule module = board.getModules()[0];
    final Z80 cpu = board.getMasterCpu();

    parser.setREGAF((char) cpu.getRegisterPair(Z80.REGPAIR_AF));
    parser.setREGBC((char) cpu.getRegisterPair(Z80.REGPAIR_BC));
    parser.setREGDE((char) cpu.getRegisterPair(Z80.REGPAIR_DE));
    parser.setREGHL((char) cpu.getRegisterPair(Z80.REGPAIR_HL));

    parser.setALTREGAF((char) cpu.getRegisterPair(Z80.REGPAIR_AF, true));
    parser.setALTREGBC((char) cpu.getRegisterPair(Z80.REGPAIR_BC, true));
    parser.setALTREGDE((char) cpu.getRegisterPair(Z80.REGPAIR_DE, true));
    parser.setALTREGHL((char) cpu.getRegisterPair(Z80.REGPAIR_HL, true));

    parser.setREGIX((char) cpu.getRegister(Z80.REG_IX));
    parser.setREGIY((char) cpu.getRegister(Z80.REG_IY));

    parser.setREGI((char) cpu.getRegister(Z80.REG_I));
    parser.setREGR((char) cpu.getRegister(Z80.REG_R));

    parser.setINTMODE((char) cpu.getIM());
    parser.setIFF2((char) (cpu.isIFF1() ? 4 : 0));

    parser.setBORDERCOLOR((char) vc.getPortFE());

    final int topPageIndex = module.read7FFD() & 7;

    final int offsetpage2 = 0x8000;
    final int offsetpage5 = 0x14000;
    final int offsetpageTop = topPageIndex * 0x4000;

    final byte[] lowram = new byte[49179];

    final int[] bankIndex = new int[] {0, 1, 2, 3, 4, 5, 6, 7};
    bankIndex[2] = -1;
    bankIndex[5] = -1;
    bankIndex[topPageIndex] = -1;

    for (int i = 0; i < 0x4000; i++) {
      lowram[i] = (byte) module.readHeap(offsetpage5 + i);
      lowram[i + 0x4000] = (byte) module.readHeap(offsetpage2 + i);
      lowram[i + 0x8000] = (byte) module.readHeap(offsetpageTop + i);
    }

    parser.setREGSP((char) cpu.getRegister(Z80.REG_SP));
    parser.setRAMDUMP(lowram);

    SNAParser.EXTENDEDDATA extData = parser.makeEXTENDEDDATA();

    extData.setREGPC((char) cpu.getRegister(Z80.REG_PC));
    extData.setPORT7FFD((char) module.read7FFD());
    extData.setONTRDOS((byte) (module.isTrdosActive() ? 1 : 0));

    final int totalExtraBanks = (int) IntStream.of(bankIndex).filter(x -> x >= 0).count();

    final SNAParser.EXTENDEDDATA.EXTRABANK[] extraBank = parser.getEXTENDEDDATA().makeEXTRABANK(totalExtraBanks);

    for (int i = 0; i < extraBank.length; i++) {
      extraBank[i] = new SNAParser.EXTENDEDDATA.EXTRABANK(parser);
      extraBank[i].setDATA(new byte[0x4000]);
    }

    int extraBankIndex = 0;
    for (int i = 0; i < 8; i++) {
      if (bankIndex[i] < 0) {
        continue;
      }
      final byte[] data = parser.getEXTENDEDDATA().getEXTRABANK()[extraBankIndex++].getDATA();
      final int heapoffset = bankIndex[i] * 0x4000;
      for (int a = 0; a < data.length; a++) {
        data[a] = (byte) module.readHeap(heapoffset + a);
      }
    }

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (JBBPBitOutputStream bitOut = new JBBPBitOutputStream(bos)) {
      parser.write(bitOut);
    }
    return bos.toByteArray();
  }

  @Override
  public boolean accept(final File f) {
    return f != null && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".sna"));
  }

  @Override
  public String getDescription() {
    return "SNA Snapshot (*.sna)";
  }

  @Override
  public String getName() {
    return "SNA snapshot";
  }
}
