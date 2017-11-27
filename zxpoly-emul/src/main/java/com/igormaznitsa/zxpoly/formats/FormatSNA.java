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
package com.igormaznitsa.zxpoly.formats;

import java.io.ByteArrayInputStream;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.*;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;

public class FormatSNA extends Snapshot {

  public FormatSNA() {
  }

  @Override
  public void loadFromArray(final File srcFile, final Motherboard board, final VideoController vc, final byte[] array) throws IOException {
    final SNAParser parser = new SNAParser().read(new JBBPBitInputStream(new ByteArrayInputStream(array)));
    final boolean sna128 = array.length > 49179;
    if (sna128) {
      doMode128(board);
    }
    else{
      doMode48(board);
    }

    final ZXPolyModule module = board.getZXPolyModules()[0];
    final Z80 cpu = board.getCPU0();

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
    cpu.setIFF(true, (parser.getINTERRUPT() & 2) != 0);

    vc.writeIO(module, 0xFE, parser.getBORDERCOLOR());
    vc.setBorderColor(parser.getBORDERCOLOR());

    if (sna128) {
      final int offsetpage2 = 0x8000;
      final int offsetpage5 = 0x14000;
      final int offsetpageTop = (parser.getEXTENDEDDATA().getPORT7FFD() & 7) * 0x4000;

      for (int i = 0; i < 0x4000; i++) {
        module.writeHeapModuleMemory(offsetpage5 + i, parser.getRAMDUMP()[i]);
        module.writeHeapModuleMemory(offsetpage2 + i, parser.getRAMDUMP()[i + 0x4000]);
        module.writeHeapModuleMemory(offsetpageTop + i, parser.getRAMDUMP()[i + 0x8000]);
      }

      cpu.setRegister(Z80.REG_PC, parser.getEXTENDEDDATA().getREGPC());
      cpu.setRegister(Z80.REG_SP, parser.getREGSP() + 2);
      module.set7FFD(parser.getEXTENDEDDATA().getPORT7FFD(), true);
      module.setTRDOSActive(parser.getEXTENDEDDATA().getONTRDOS() != 0);

      int bankindex = 0;
      final int mapped = 0x24 | (1 << (parser.getEXTENDEDDATA().getPORT7FFD() & 7));
      for (int i = 0; i < 8 && bankindex < parser.getEXTENDEDDATA().getEXTRABANK().length; i++) {
        if ((mapped & (1 << i)) == 0) {
          final byte[] data = parser.getEXTENDEDDATA().getEXTRABANK()[bankindex++].getDATA();
          final int heapoffset = i * 0x4000;
          for (int a = 0; a < data.length; a++) {
            module.writeHeapModuleMemory(heapoffset + a, data[a]);
          }
        }
      }

    }
    else {
      for (int i = 0; i < parser.getRAMDUMP().length; i++) {
        module.writeMemory(cpu, 0x4000 + i, parser.getRAMDUMP()[i]);
      }

      int regsp = parser.getREGSP();
      final int lowaddr = parser.getRAMDUMP()[regsp-0x4000] & 0xFF;
      regsp = (regsp + 1) & 0xFFFF;
      final int highaddr = parser.getRAMDUMP()[regsp-0x4000] & 0xFF;
      regsp = (regsp + 1) & 0xFFFF;
      parser.setREGSP((char)regsp);
      final int startAddress = (highaddr<<8)|lowaddr;      
      
      cpu.setRegister(Z80.REG_SP, parser.getREGSP());
      cpu.setRegister(Z80.REG_PC, startAddress);
    }
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
