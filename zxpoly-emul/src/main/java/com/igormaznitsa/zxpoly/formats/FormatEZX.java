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
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.VideoController;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class FormatEZX extends Snapshot {

  public FormatEZX() {
  }

  @Override
  public String getExtension() {
    return "ezx";
  }

  @Override
  public void loadFromArray(final File srcFile, final Motherboard board, final VideoController vc, final byte[] array) throws IOException {
    if (array[0] == 'E' && array[1] == 'Z' && array[2] == 'X') {
      throw new IOException("Unsupported packed version of EZX format!");
    }
    final EZXParser parser = new EZXParser().read(new JBBPBitInputStream(new ByteArrayInputStream(array)));

    doMode128(board);

    final ZxPolyModule module = board.getModules()[0];
    final Z80 cpu = board.getMasterCpu();

    cpu.setRegisterPair(Z80.REGPAIR_AF, parser.getSPECSTATE().getAF());
    cpu.setRegisterPair(Z80.REGPAIR_BC, parser.getSPECSTATE().getBC());
    cpu.setRegisterPair(Z80.REGPAIR_DE, parser.getSPECSTATE().getDE());
    cpu.setRegisterPair(Z80.REGPAIR_HL, parser.getSPECSTATE().getHL());

    cpu.setRegisterPair(Z80.REGPAIR_AF, parser.getSPECSTATE().getAFALT(), true);
    cpu.setRegisterPair(Z80.REGPAIR_BC, parser.getSPECSTATE().getBCALT(), true);
    cpu.setRegisterPair(Z80.REGPAIR_DE, parser.getSPECSTATE().getDEALT(), true);
    cpu.setRegisterPair(Z80.REGPAIR_HL, parser.getSPECSTATE().getHLALT(), true);

    cpu.setRegister(Z80.REG_IX, parser.getSPECSTATE().getIX());
    cpu.setRegister(Z80.REG_IY, parser.getSPECSTATE().getIY());

    cpu.setRegister(Z80.REG_I, parser.getSPECSTATE().getI());
    cpu.setRegister(Z80.REG_R, parser.getSPECSTATE().getR());

    cpu.setIM(parser.getSPECSTATE().getIMMODE());
    cpu.setIFF(parser.getSPECSTATE().getIFF1(), parser.getSPECSTATE().getIFF2());

    vc.writeIo(module, 0xFE, parser.getSPECSTATE().getBORDERCOLOR());
    vc.setBorderColor(parser.getSPECSTATE().getBORDERCOLOR());

    module.write7FFD((parser.getSPECSTATE().getBANKRAMC000() & 0b111)
        | (parser.getSPECSTATE().getBANKVIDEO() == 0 ? 0 : 0b1000)
        | (parser.getSPECSTATE().getBANKROM0000() == 0 ? 0 : 0b10000)
        | (parser.getLOCK7FFD() ? 0b100000 : 0), true);

    for (int pageIndex = 0; pageIndex < parser.ram.length; pageIndex++) {
      final EZXParser.RAM page = parser.ram[pageIndex];
      for (int offst = 0; offst < 0x4000; offst++) {
        module.writeHeap(pageIndex * 0x4000 + offst, page.data[offst]);
      }
    }
  }

  @Override
  public byte[] saveToArray(Motherboard board, VideoController vc) throws IOException {
    throw new IOException("Unsupported operation");
  }

  @Override
  public boolean accept(final File f) {
    return f != null && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".ezx"));
  }

  @Override
  public String getDescription() {
    return "EZX Snapshot (*.ezx)";
  }

  @Override
  public String getName() {
    return "EZX snapshot";
  }
}
