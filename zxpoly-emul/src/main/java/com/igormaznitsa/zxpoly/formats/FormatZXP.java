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

import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.*;
import java.io.*;
import java.util.Locale;

public class FormatZXP extends Snapshot {

  @Override
  public void loadFromArray(final Motherboard board, final VideoController vc, final byte[] array) throws IOException {
    final ZXEMLSnapshotFormat snapshot = new ZXEMLSnapshotFormat(array);

    for(int cpu = 0 ; cpu<4; cpu++){
      final ZXPolyModule module = board.getZXPolyModules()[cpu];
      final Z80 z80 = module.getCPU();
      
      z80.setRegister(Z80.REG_PC, snapshot.getREG_PC()[cpu]);
      z80.setRegister(Z80.REG_SP, snapshot.getREG_SP()[cpu]);

      z80.setRegisterPair(Z80.REGPAIR_AF, snapshot.getREG_AF()[cpu],false);
      z80.setRegisterPair(Z80.REGPAIR_AF, snapshot.getREG_AF_ALT()[cpu],true);
      
      z80.setRegisterPair(Z80.REGPAIR_BC, snapshot.getREG_BC()[cpu],false);
      z80.setRegisterPair(Z80.REGPAIR_BC, snapshot.getREG_BC_ALT()[cpu],true);
      
      z80.setRegisterPair(Z80.REGPAIR_DE, snapshot.getREG_DE()[cpu],false);
      z80.setRegisterPair(Z80.REGPAIR_DE, snapshot.getREG_DE_ALT()[cpu],true);
      
      z80.setRegisterPair(Z80.REGPAIR_HL, snapshot.getREG_HL()[cpu],false);
      z80.setRegisterPair(Z80.REGPAIR_HL, snapshot.getREG_HL_ALT()[cpu],true);
      
      z80.setRegister(Z80.REG_IX, snapshot.getREG_IX()[cpu]);
      z80.setRegister(Z80.REG_IY, snapshot.getREG_IY()[cpu]);

      z80.setRegister(Z80.REG_R, snapshot.getREG_IR()[cpu] & 0xFF);
      z80.setRegister(Z80.REG_I, (snapshot.getREG_IR()[cpu]>>8) & 0xFF);
      
      z80.setIFF(snapshot.getIFF()[cpu],snapshot.getIFF2()[cpu]);
      z80.setIM(snapshot.getREG_IM()[cpu]);

      module.loadModuleLocalPortsByValues(snapshot.getModulePorts(cpu)[0], snapshot.getModulePorts(cpu)[1], snapshot.getModulePorts(cpu)[2], snapshot.getModulePorts(cpu)[3], snapshot.getModulePorts(cpu)[4]);
    
      final ZXPParser.PAGES memory = snapshot.getPAGES()[cpu];
      
      for(final ZXPParser.PAGES.PAGE p : memory.getPAGE()){
        final int pageOffset = p.getINDEX()* 0x4000;
        for(int addr=0; addr<0x4000; addr++){
          module.writeHeapModuleMemory(pageOffset + addr, p.getDATA()[addr] & 0xFF);
        }
      }
    }
 
    board.set3D00(snapshot.getPORT3D00(), true);
    vc.setBorderColor(snapshot.getPORTFE() &  7);
  }

  @Override
  public String getName() {
    return "ZXPoly-Z80 snapshot";
  }

  @Override
  public boolean accept(final File f) {
    return f != null && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".zxp"));
  }

  @Override
  public String getDescription() {
    return "ZXPoly-Z80 Snapshot (*.zxp)";
  }
  
}
