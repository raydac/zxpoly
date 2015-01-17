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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.mapper.*;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.VideoController;
import com.igormaznitsa.zxpoly.components.ZXPolyModule;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

public class FormatSNA extends Snapshot {

  private static final JBBPParser PARSER_SNA = JBBPParser.prepare("ubyte regI;"
          + "<ushort altRegHL; <ushort altRegDE; <ushort altRegBC; <ushort altRegAF;"
          + "<ushort regHL; <ushort regDE; <ushort regBC; <ushort regIY; <ushort regIX;"
          + "ubyte interrupt; ubyte regR;"
          + "<ushort regAF; <ushort regSP;"
          + "ubyte intMode;"
          + "ubyte borderColor;"
          + "byte [49152] ramDump;"
          + "<ushort regPC;"
          + "ubyte port7FFD;"
          + "byte onTrDos;"
          + "extrabank [_]{"
          + " byte [16384] data;"
          + "}", JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF);

  @Bin
  private static class ExtraBank {

    private byte[] data;
  }

  @Bin(type = BinType.UBYTE)
  private int regI, intMode, borderColor, interrupt, regR;
  @Bin(type = BinType.USHORT)
  private int altRegHL, altRegDE, altRegBC, altRegAF, regHL, regDE, regBC, regIY, regIX, regAF, regSP;
  @Bin
  private byte[] ramDump;
  @Bin(type = BinType.USHORT)
  private int regPC;
  @Bin(type = BinType.UBYTE)
  private int port7FFD;
  @Bin(type = BinType.BYTE)
  private boolean onTrDos;
  @Bin
  private ExtraBank[] extrabank;

  private boolean sna128;

  public FormatSNA() {
  }

  @Override
  public boolean load(byte[] array) throws IOException {
    PARSER_SNA.parse(array).mapTo(this, JBBPMapper.FLAG_IGNORE_MISSING_VALUES);
    sna128 = array.length > 49179;
    return !sna128;
  }

  @Override
  public void fillModule(final ZXPolyModule module, final VideoController vc) {
    final Z80 cpu = module.getCPU();
    cpu.doReset();

    cpu.setRegisterPair(Z80.REGPAIR_AF, this.regAF);
    cpu.setRegisterPair(Z80.REGPAIR_BC, this.regBC);
    cpu.setRegisterPair(Z80.REGPAIR_DE, this.regDE);
    cpu.setRegisterPair(Z80.REGPAIR_HL, this.regHL);

    cpu.setRegisterPair(Z80.REGPAIR_AF, this.altRegAF, true);
    cpu.setRegisterPair(Z80.REGPAIR_BC, this.altRegBC, true);
    cpu.setRegisterPair(Z80.REGPAIR_DE, this.altRegDE, true);
    cpu.setRegisterPair(Z80.REGPAIR_HL, this.altRegHL, true);

    cpu.setRegister(Z80.REG_IX, this.regIX);
    cpu.setRegister(Z80.REG_IY, this.regIY);

    cpu.setRegister(Z80.REG_I, this.regI);
    cpu.setRegister(Z80.REG_R, this.regR);

    cpu.setIM(this.intMode);
    cpu.setIFF((this.interrupt & 2) != 0, (this.interrupt & 2) != 0);

    vc.writeIO(module, 0xFE, this.borderColor);
    vc.setBorderColor(this.borderColor);

    if (this.sna128) {
      final int offsetpage2 = 0x8000;
      final int offsetpage5 = 0x14000;
      final int offsetpageTop = (this.port7FFD & 7) * 0x4000;

      for (int i = 0; i < 0x4000; i++) {
        module.writeHeapModuleMemory(offsetpage5 + i, this.ramDump[i]);
        module.writeHeapModuleMemory(offsetpage2 + i, this.ramDump[i + 0x4000]);
        module.writeHeapModuleMemory(offsetpageTop + i, this.ramDump[i + 0x8000]);
      }

      cpu.setRegister(Z80.REG_PC, this.regPC);
      cpu.setRegister(Z80.REG_SP, this.regSP+2);
      module.set7FFD(this.port7FFD, true);
      module.setTRDOSActive(this.onTrDos);

      int bankindex = 0;
      final int mapped = 0x24 | (1 << (this.port7FFD & 7));
      for (int i = 0; i < 8 && bankindex < this.extrabank.length; i++) {
        if ((mapped & (1 << i)) == 0) {
          final byte[] data = this.extrabank[bankindex++].data;
          final int heapoffset = i * 0x4000;
          for (int a = 0; a < data.length; a++) {
            module.writeHeapModuleMemory(heapoffset + a, data[a]);
          }
        }
      }

    }
    else {
      for (int i = 0; i < this.ramDump.length; i++) {
        module.writeMemory(cpu, 16384 + i, this.ramDump[i]);
      }

      final int calculatedPC = (module.readMemory(cpu, this.regSP, false) & 0xFF) + 0x100 * (module.readMemory(cpu, this.regSP + 1, false) & 0xFF);
      cpu.setRegister(Z80.REG_SP, this.regSP + 2);
      cpu.setRegister(Z80.REG_PC, calculatedPC);
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
    return "SNA Snapshot";
  }
}
