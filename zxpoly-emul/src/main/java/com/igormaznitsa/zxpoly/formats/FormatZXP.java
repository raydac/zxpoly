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

import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.BoardMode;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import com.igormaznitsa.zxpoly.components.video.VideoController;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class FormatZXP extends ArraySaveable {

  @Override
  public String getExtension() {
    return "zxp";
  }

  @Override
  public boolean canMakeSnapshotForBoardMode(final BoardMode mode) {
    return mode == BoardMode.ZXPOLY;
  }

  @Override
  public byte[] saveToArray(Motherboard board, VideoController vc) throws IOException {
    final ZxEmlSnapshotFormat snapshot = new ZxEmlSnapshotFormat();

    for (int cpu = 0; cpu < 4; cpu++) {
      final ZxPolyModule module = board.getModules()[cpu];
      final Z80 z80 = module.getCpu();

      snapshot.getREG_PC()[cpu] = (short) z80.getRegister(Z80.REG_PC);
      snapshot.getREG_SP()[cpu] = (short) z80.getRegister(Z80.REG_SP);

      snapshot.getREG_AF()[cpu] = (short) z80.getRegisterPair(Z80.REGPAIR_AF, false);
      snapshot.getREG_AF_ALT()[cpu] = (short) z80.getRegisterPair(Z80.REGPAIR_AF, true);

      snapshot.getREG_BC()[cpu] = (short) z80.getRegisterPair(Z80.REGPAIR_BC, false);
      snapshot.getREG_BC_ALT()[cpu] = (short) z80.getRegisterPair(Z80.REGPAIR_BC, true);

      snapshot.getREG_DE()[cpu] = (short) z80.getRegisterPair(Z80.REGPAIR_DE, false);
      snapshot.getREG_DE_ALT()[cpu] = (short) z80.getRegisterPair(Z80.REGPAIR_DE, true);

      snapshot.getREG_HL()[cpu] = (short) z80.getRegisterPair(Z80.REGPAIR_HL, false);
      snapshot.getREG_HL_ALT()[cpu] = (short) z80.getRegisterPair(Z80.REGPAIR_HL, true);

      snapshot.getREG_IX()[cpu] = (short) z80.getRegister(Z80.REG_IX);
      snapshot.getREG_IY()[cpu] = (short) z80.getRegister(Z80.REG_IY);

      snapshot.getREG_IR()[cpu] =
              (short) ((z80.getRegister(Z80.REG_I) << 8) | z80.getRegister(Z80.REG_R));

      snapshot.getIFF()[cpu] = z80.isIFF1();
      snapshot.getIFF2()[cpu] = z80.isIFF2();
      snapshot.getREG_IM()[cpu] = (byte) z80.getIM();

      module.fillArrayByPortValues(snapshot.getModulePorts(cpu));

      final ZXPParser.PAGES memory = snapshot.getPAGES()[cpu];

      for (final ZXPParser.PAGES.PAGE p : memory.getPAGE()) {
        final int pageOffset = p.getINDEX() * 0x4000;
        for (int addr = 0; addr < 0x4000; addr++) {
          p.getDATA()[addr] = (byte) module.readHeap(pageOffset + addr);
        }
      }
    }

    snapshot.setPORT3D00((char) board.get3D00());
    snapshot.setPORTFE((char) vc.getPortFE());

    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    try (JBBPBitOutputStream out = new JBBPBitOutputStream(bos)) {
      snapshot.write(out);
    }
    return bos.toByteArray();
  }

  @Override
  public void loadFromArray(final File srcFile, final Motherboard board, final VideoController vc,
                            final byte[] array) throws IOException {
    final ZxEmlSnapshotFormat snapshot = new ZxEmlSnapshotFormat(array);

    this.doZxPoly(board);

    for (int cpu = 0; cpu < 4; cpu++) {
      final ZxPolyModule module = board.getModules()[cpu];
      final Z80 z80 = module.getCpu();

      z80.setRegister(Z80.REG_PC, snapshot.getREG_PC()[cpu]);
      z80.setRegister(Z80.REG_SP, snapshot.getREG_SP()[cpu]);

      z80.setRegisterPair(Z80.REGPAIR_AF, snapshot.getREG_AF()[cpu], false);
      z80.setRegisterPair(Z80.REGPAIR_AF, snapshot.getREG_AF_ALT()[cpu], true);

      z80.setRegisterPair(Z80.REGPAIR_BC, snapshot.getREG_BC()[cpu], false);
      z80.setRegisterPair(Z80.REGPAIR_BC, snapshot.getREG_BC_ALT()[cpu], true);

      z80.setRegisterPair(Z80.REGPAIR_DE, snapshot.getREG_DE()[cpu], false);
      z80.setRegisterPair(Z80.REGPAIR_DE, snapshot.getREG_DE_ALT()[cpu], true);

      z80.setRegisterPair(Z80.REGPAIR_HL, snapshot.getREG_HL()[cpu], false);
      z80.setRegisterPair(Z80.REGPAIR_HL, snapshot.getREG_HL_ALT()[cpu], true);

      z80.setRegister(Z80.REG_IX, snapshot.getREG_IX()[cpu]);
      z80.setRegister(Z80.REG_IY, snapshot.getREG_IY()[cpu]);

      z80.setRegister(Z80.REG_R, snapshot.getREG_IR()[cpu] & 0xFF);
      z80.setRegister(Z80.REG_I, (snapshot.getREG_IR()[cpu] >> 8) & 0xFF);

      z80.setIFF(snapshot.getIFF()[cpu], snapshot.getIFF2()[cpu]);
      z80.setIM(snapshot.getREG_IM()[cpu]);

      module.fillPortByValues(snapshot.getModulePorts(cpu)[0], snapshot.getModulePorts(cpu)[1],
              snapshot.getModulePorts(cpu)[2], snapshot.getModulePorts(cpu)[3],
              snapshot.getModulePorts(cpu)[4]);

      final ZXPParser.PAGES memory = snapshot.getPAGES()[cpu];

      for (final ZXPParser.PAGES.PAGE p : memory.getPAGE()) {
        module.syncWriteHeapPage(p.getINDEX(), p.getDATA());
      }
    }

    board.set3D00(snapshot.getPORT3D00(), true);
    vc.setBorderColor(snapshot.getPORTFE() & 7);
  }

  @Override
  public String getName() {
    return "ZXPoly-Z80 snapshot";
  }

  @Override
  public boolean accept(final File f) {
    return f != null &&
            (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".zxp"));
  }

  @Override
  public String getDescription() {
    return "ZXPoly-Z80 Snapshot (*.zxp)";
  }

}
