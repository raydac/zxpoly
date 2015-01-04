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
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.VideoController;
import com.igormaznitsa.zxpoly.components.ZXPolyModule;
import java.io.File;
import java.io.IOException;

public class FormatSNA extends Snapshot {

  private static final JBBPParser PARSER_SNA_48 = JBBPParser.prepare(
          "ubyte regI;"
          + "<ushort altRegHL; <ushort altRegDE; <ushort altRegBC; <ushort altRegAF;"
          + "<ushort regHL; <ushort regDE; <ushort regBC; <ushort regIY; <ushort regIX;"
          + "ubyte interrupt; ubyte regR;"
          + "<ushort regAF; <ushort regSP;"
          + "ubyte intMode;"
          + "ubyte borderColor;"
          + "byte [49152] ramDump;");

  @Bin(type = BinType.UBYTE)
  private int regI, intMode, borderColor, interrupt, regR;
  @Bin(type = BinType.USHORT)
  private int altRegHL, altRegDE, altRegBC, altRegAF, regHL, regDE, regBC, regIY, regIX, regAF, regSP;
  @Bin
  private byte[] ramDump;

  public FormatSNA() {
  }

  @Override
  public void load(byte[] array) throws IOException {
    PARSER_SNA_48.parse(array).mapTo(this);
  }


  @Override
  public void fillModule(final ZXPolyModule module, final VideoController vc) {
    final Z80 cpu = module.getCPU();
    cpu.doReset();
    
    cpu.setRegisterPair(Z80.REGPAIR_AF, regAF);
    cpu.setRegisterPair(Z80.REGPAIR_BC, regBC);
    cpu.setRegisterPair(Z80.REGPAIR_DE, regDE);
    cpu.setRegisterPair(Z80.REGPAIR_HL, regHL);

    cpu.setRegisterPair(Z80.REGPAIR_AF, altRegAF, true);
    cpu.setRegisterPair(Z80.REGPAIR_BC, altRegBC, true);
    cpu.setRegisterPair(Z80.REGPAIR_DE, altRegDE, true);
    cpu.setRegisterPair(Z80.REGPAIR_HL, altRegHL, true);
  
    cpu.setRegister(Z80.REG_IX, regIX);
    cpu.setRegister(Z80.REG_IY, regIY);
    
    cpu.setRegister(Z80.REG_I, regI);
    cpu.setRegister(Z80.REG_R, regR);
    
    cpu.setIM(intMode);
    
    vc.writeIO(module, 0xFE, borderColor);

    for (int i = 0; i < ramDump.length; i++) {
      module.writeMemory(cpu, 16384 + i, ramDump[i]);
    }
    
    final int calculatedPC = (module.readMemory(cpu, regSP, false) & 0xFF)+0x100*(module.readMemory(cpu, regSP+1, false) & 0xFF);

    cpu.setRegister(Z80.REG_SP, regSP+2);
    cpu.setRegister(Z80.REG_PC, calculatedPC);
    
    cpu.setIFF((interrupt & 2)!=0, (interrupt & 2) != 0);
    
    vc.setBorderColor(this.borderColor);
  }

  @Override
  public boolean accept(final File f) {
    return f!=null && (f.isDirectory() || f.getName().toString().endsWith(".sna"));
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
