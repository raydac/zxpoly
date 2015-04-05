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
import com.igormaznitsa.zxpoly.formats.ZXEMLSnapshotFormat.Page;
import com.igormaznitsa.zxpoly.formats.ZXEMLSnapshotFormat.Pages;
import java.io.*;
import java.util.Locale;

public class FormatZXPZ80 extends Snapshot {

  @Override
  public void loadFromArray(final Motherboard board, final VideoController vc, final byte[] array) throws IOException {
    final ZXEMLSnapshotFormat snapshot = new ZXEMLSnapshotFormat(array);

    for(int cpu = 0 ; cpu<4; cpu++){
      final ZXPolyModule module = board.getZXPolyModules()[cpu];
      final Z80 z80 = module.getCPU();
      
      z80.setRegister(Z80.REG_PC, snapshot.getRegPC(cpu));
      z80.setRegister(Z80.REG_SP, snapshot.getRegSP(cpu));

      z80.setRegisterPair(Z80.REGPAIR_AF, snapshot.getAF(cpu,false),false);
      z80.setRegisterPair(Z80.REGPAIR_AF, snapshot.getAF(cpu,true),true);
      
      z80.setRegisterPair(Z80.REGPAIR_BC, snapshot.getBC(cpu,false),false);
      z80.setRegisterPair(Z80.REGPAIR_BC, snapshot.getBC(cpu,true),true);
      
      z80.setRegisterPair(Z80.REGPAIR_DE, snapshot.getDE(cpu,false),false);
      z80.setRegisterPair(Z80.REGPAIR_DE, snapshot.getDE(cpu,true),true);
      
      z80.setRegisterPair(Z80.REGPAIR_HL, snapshot.getHL(cpu,false),false);
      z80.setRegisterPair(Z80.REGPAIR_HL, snapshot.getHL(cpu,true),true);
      
      z80.setRegister(Z80.REG_IX, snapshot.getRegIX(cpu));
      z80.setRegister(Z80.REG_IY, snapshot.getRegIY(cpu));

      z80.setRegister(Z80.REG_R, snapshot.getRegIR(cpu) & 0xFF);
      z80.setRegister(Z80.REG_I, (snapshot.getRegIR(cpu)>>8) & 0xFF);
      
      z80.setIFF(snapshot.isIFF(cpu),snapshot.isIFF2(cpu));
      z80.setIM(snapshot.getRegIM(cpu));

      module.loadModuleLocalPortsByValues(snapshot.getModulePorts(cpu)[0], snapshot.getModulePorts(cpu)[1], snapshot.getModulePorts(cpu)[2], snapshot.getModulePorts(cpu)[3], snapshot.getModulePorts(cpu)[4]);
    
      final Pages memory = snapshot.getPages(cpu);
      
      for(final Page p : memory.getPages()){
        final int pageOffset = p.getIndex() * 0x4000;
        for(int addr=0; addr<0x4000; addr++){
          module.writeHeapModuleMemory(pageOffset + addr, p.getData()[addr] & 0xFF);
        }
      }
    }
 
    board.set3D00(snapshot.getPort3D00(), true);
    vc.setBorderColor(snapshot.getPortFE() &  7);
  }

  @Override
  public String getName() {
    return "ZXPoly-Z80 snapshot";
  }

  @Override
  public boolean accept(final File f) {
    return f != null && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".zxpz80"));
  }

  @Override
  public String getDescription() {
    return "ZXPoly-Z80 Snapshot (*.zxpz80)";
  }
  
}
