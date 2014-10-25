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
package com.igormaznitsa.z80.disasm;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class Z80Disasm {
  static {
    final InputStream in = Z80Disasm.class.getClassLoader().getResourceAsStream("z80opcodes.lst");
    final List<Z80Instruction> list = new ArrayList<Z80Instruction>(1500);
    try{
      final BufferedReader reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
      while(true){
        final String line = reader.readLine();
        if (line == null) break;
        final String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
        list.add(new Z80Instruction(trimmed));
      }
      in.close();
    }catch(Exception ex){
      throw new Error("Can't load Z80 opcode list", ex);
    }

    // make prefix tree
    
  }
  
  public Z80Instruction decodeInstruction(final byte [] array,final int offset){
    return null;
  }
}
