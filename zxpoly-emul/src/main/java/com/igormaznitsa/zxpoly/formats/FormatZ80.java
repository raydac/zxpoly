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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.mapper.*;
import com.igormaznitsa.jbbp.model.*;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.VideoController;
import com.igormaznitsa.zxpoly.components.ZXPolyModule;
import java.io.*;
import java.lang.reflect.Field;

public class FormatZ80 extends Snapshot {

  class EmulFlags {

    @Bin(outOrder = 1, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    byte interruptmode;
    @Bin(outOrder = 2, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    byte issue2emulation;
    @Bin(outOrder = 3, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    byte doubleintfreq;
    @Bin(outOrder = 4, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    byte videosync;
    @Bin(outOrder = 5, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    byte inputdevice;
  }

  class Flags {

    @Bin(outOrder = 1, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    byte reg_r_bit7;
    @Bin(outOrder = 2, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_3)
    byte bordercolor;
    @Bin(outOrder = 3, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    byte basic_samrom;
    @Bin(outOrder = 4, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    byte compressed;
    @Bin(outOrder = 5, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    byte nomeaning;
  }

  class Z80Snapshot {

    @Bin(outOrder = 1)
    byte reg_a;
    @Bin(outOrder = 2)
    byte reg_f;
    @Bin(outOrder = 3)
    short reg_bc;
    @Bin(outOrder = 4)
    short reg_hl;
    @Bin(outOrder = 5)
    short reg_pc;
    @Bin(outOrder = 6)
    short reg_sp;
    @Bin(outOrder = 7)
    byte reg_ir;
    @Bin(outOrder = 8)
    byte reg_r;

    @Bin(outOrder = 9)
    Flags flags;

    @Bin(outOrder = 10)
    short reg_de;
    @Bin(outOrder = 11)
    short reg_bc_alt;
    @Bin(outOrder = 12)
    short reg_de_alt;
    @Bin(outOrder = 13)
    short reg_hl_alt;
    @Bin(outOrder = 14)
    byte reg_a_alt;
    @Bin(outOrder = 15)
    byte reg_f_alt;
    @Bin(outOrder = 16)
    short reg_iy;
    @Bin(outOrder = 17)
    short reg_ix;
    @Bin(outOrder = 18)
    byte iff;
    @Bin(outOrder = 19)
    byte iff2;

    @Bin(outOrder = 20)
    EmulFlags emulFlags;

    @Bin(outOrder = 21, custom = true)
    byte[] data;
  }

  private static final JBBPParser z80Parser = JBBPParser.prepare(
          "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "byte [_] data;"
  );
  
  private Z80Snapshot current;
  
  private static class DataProcessor implements JBBPMapperCustomFieldProcessor {

    @Override
    public Object prepareObjectForMapping(JBBPFieldStruct parsedBlock, Bin annotation, Field field) {
      if (field.getName().equals("data")) {
        final byte[] data = parsedBlock.findFieldForNameAndType("data", JBBPFieldArrayByte.class).getArray();

        if (parsedBlock.findFieldForPathAndType("flags.compressed", JBBPFieldBit.class).getAsBool()) {
          // RLE compressed
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
              }
              else {
                baos.write(a);
                baos.write(b);
              }
            }
            else {
              baos.write(a);
            }
          }
          return baos.toByteArray();
        }
        else {
          // uncompressed
          return data;
        }
      }
      else {
        return null;
      }
    }
  }

  @Override
  public void fillModule(ZXPolyModule module, VideoController vc) {
    final Z80 cpu = module.getCPU();
    cpu.doReset();
    
    cpu.setRegister(Z80.REG_A, current.reg_a);
    cpu.setRegister(Z80.REG_F, current.reg_f);
    cpu.setRegister(Z80.REG_A, current.reg_a_alt, true);
    cpu.setRegister(Z80.REG_F, current.reg_f_alt, true);
    cpu.setRegisterPair(Z80.REGPAIR_BC, current.reg_bc);
    cpu.setRegisterPair(Z80.REGPAIR_BC, current.reg_bc_alt, true);
    cpu.setRegisterPair(Z80.REGPAIR_DE, current.reg_de);
    cpu.setRegisterPair(Z80.REGPAIR_DE, current.reg_de_alt, true);
    cpu.setRegisterPair(Z80.REGPAIR_HL, current.reg_hl);
    cpu.setRegisterPair(Z80.REGPAIR_HL, current.reg_hl_alt, true);
    
    cpu.setRegister(Z80.REG_IX, current.reg_ix);
    cpu.setRegister(Z80.REG_IY, current.reg_iy);
    
    cpu.setRegister(Z80.REG_R, current.reg_r);
    cpu.setRegister(Z80.REG_SP, current.reg_sp);
    cpu.setRegister(Z80.REG_PC, current.reg_pc);
 
    
    cpu.setIFF(current.iff!=0, current.iff2 != 0);
    
    cpu.setRegister(Z80.REG_I, current.reg_ir >>> 8);
    cpu.setIM(current.emulFlags.interruptmode);
    
    for(int i=0; i<current.data.length; i++){
      module.writeMemory(cpu, i+16384, current.data[i]);
    }
        
    vc.setBorderColor(current.flags.bordercolor);
  }

  @Override
  public void load(byte[] array) throws IOException {
    current = z80Parser.parse(array).mapTo(Z80Snapshot.class, new DataProcessor());
    if (current.reg_pc == 0){
      throw new IOException("Detected snapshot Z80 of 2 or 3 version, only 1st version allowed!");
    }
  }

  @Override
  public String getName() {
    return "Z80 snapshot";
  }

  @Override
  public boolean accept(final File f) {
    return f != null && (f.isDirectory() || f.getName().toString().endsWith(".z80"));
  }

  @Override
  public String getDescription() {
    return "Z80 Snapshot (*.z80)";
  }

}
