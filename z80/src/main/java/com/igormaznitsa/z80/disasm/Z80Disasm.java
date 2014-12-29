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

public enum Z80Disasm {

  ;

  private static final Z80Instruction[] NO_PREFIXED;
  private static final Z80Instruction[] CB_PREFIXED;
  private static final Z80Instruction[] DD_PREFIXED;
  private static final Z80Instruction[] FD_PREFIXED;
  private static final Z80Instruction[] DDCB_PREFIXED;
  private static final Z80Instruction[] FDCB_PREFIXED;

  static {
    final List<Z80Instruction> list = new ArrayList<>(1500);
    final InputStream in = Z80Disasm.class.getClassLoader().getResourceAsStream("z80opcodes.lst");
    BufferedReader reader = null;
    try {
      reader = new BufferedReader(new InputStreamReader(in, "UTF-8"));
      while (true) {
        final String line = reader.readLine();
        if (line == null) {
          break;
        }
        final String trimmed = line.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("#")) {
          continue;
        }
        list.add(new Z80Instruction(trimmed));
      }
      in.close();
    }
    catch (IOException ex) {
      throw new Error("Can't load Z80 opcode list", ex);
    }
    finally {
      try {
        if (reader != null) {
          reader.close();
        }
      }
      catch (IOException ex) {
        ex.printStackTrace();
      }
    }

    final List<Z80Instruction> noPrefixed = new ArrayList<Z80Instruction>();
    final List<Z80Instruction> cbPrefixed = new ArrayList<Z80Instruction>();
    final List<Z80Instruction> ddPrefixed = new ArrayList<Z80Instruction>();
    final List<Z80Instruction> fdPrefixed = new ArrayList<Z80Instruction>();
    final List<Z80Instruction> ddcbPrefixed = new ArrayList<Z80Instruction>();
    final List<Z80Instruction> fdcbPrefixed = new ArrayList<Z80Instruction>();

    for (final Z80Instruction i : list) {
      final int[] codes = i.getInstructionCodes();
      switch (i.getLength()) {
        case 1:
          noPrefixed.add(i);
          break;
        case 2: {
          switch (codes[0]) {
            case 0xCB:
              cbPrefixed.add(i);
              break;
            case 0xDD:
              ddPrefixed.add(i);
              break;
            case 0xFD:
              fdPrefixed.add(i);
              break;
            default:
              noPrefixed.add(i);
              break;
          }
        }
        break;
        default: {
          switch (codes[0]) {
            case 0xCB:
              cbPrefixed.add(i);
              break;
            case 0xDD:
              if (codes[1] == 0xCB) {
                ddcbPrefixed.add(i);
              }
              else {
                ddPrefixed.add(i);
              }
              break;
            case 0xFD:
              if (codes[1] == 0xCB) {
                fdcbPrefixed.add(i);
              }
              else {
                fdPrefixed.add(i);
              }
              break;
            default:
              noPrefixed.add(i);
              break;
          }
        }
      }
    }

    NO_PREFIXED = noPrefixed.toArray(new Z80Instruction[noPrefixed.size()]);
    CB_PREFIXED = cbPrefixed.toArray(new Z80Instruction[cbPrefixed.size()]);
    DD_PREFIXED = ddPrefixed.toArray(new Z80Instruction[ddPrefixed.size()]);
    FD_PREFIXED = fdPrefixed.toArray(new Z80Instruction[fdPrefixed.size()]);
    DDCB_PREFIXED = ddcbPrefixed.toArray(new Z80Instruction[ddcbPrefixed.size()]);
    FDCB_PREFIXED = fdcbPrefixed.toArray(new Z80Instruction[fdcbPrefixed.size()]);
  }

  public static List<Z80Instruction> decodeList(final byte[] array, final int offset, final int max) {
    final List<Z80Instruction> result = new ArrayList<Z80Instruction>();

    int off = offset;

    int size = max;

    while (off < array.length && size != 0) {
      final Z80Instruction i = decodeInstruction(array, off);
      result.add(i);
      if (i == null) {
        off++;
      }
      else {
        off += i.getLength();
      }
      size--;
    }

    return result;
  }

  public static Z80Instruction decodeInstruction(final byte[] array, final int offset) {
    final Z80Instruction[] arraytofind;

    int off = offset;

    if (off >= array.length) {
      return null;
    }

    switch (array[off++] & 0xFF) {
      case 0xCB:
        arraytofind = CB_PREFIXED;
        break;
      case 0xDD: {
        if (off >= array.length) {
          return null;
        }
        arraytofind = (array[off] & 0xFF) == 0xCB ? DDCB_PREFIXED : DD_PREFIXED;
      }
      break;
      case 0xFD: {
        if (off >= array.length) {
          return null;
        }
        arraytofind = (array[off] & 0xFF) == 0xCB ? FDCB_PREFIXED : FD_PREFIXED;
      }
      break;
      default:
        arraytofind = NO_PREFIXED;
        break;
    }

    Z80Instruction result = null;

    for (final Z80Instruction i : arraytofind) {
      if (i.matches(array, offset)) {
        result = i;
        break;
      }
    }
    return result;
  }
}
