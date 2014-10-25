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

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Z80Instruction {

  public static final int SPEC_INDEX = 0x100;
  public static final int SPEC_OFFSET = 0x101;
  public static final int SPEC_UNSIGNED_BYTE = 0x102;
  public static final int SPEC_UNSIGNED_WORD = 0x103;

  private final int[] codes;
  private final String template;

  private static final Pattern CODE_PART_CHECKING = Pattern.compile("([0-9A-F]{2})+(\\s+(d|e|n|nn))*(\\s*[0-9A-F]{2}+)?");
  private static final Pattern CODE_PART_PARSING = Pattern.compile("[0-9A-F]{2}|\\s+(?:d|e|nn|n)");

  private final int length;

  public Z80Instruction(final String def) {
    final String codePart = def.substring(0, 11).trim();
    final String asmPart = def.substring(11).trim();

    this.codes = parseCode(codePart);
    this.template = checkAsmPart(asmPart);

    int len = 0;
    for (final int c : this.codes) {
      len++;
      if (c == SPEC_UNSIGNED_WORD) {
        len++;
      }
    }
    this.length = len;
  }

  public int getLength() {
    return this.length;
  }

  private String checkAsmPart(final String asmPart) {
    final String replace = asmPart.replace("+d", "%").replace("+e", "%").replace("nn", "%").replace("n", "%");
    for (final char c : replace.toCharArray()) {
      switch (c) {
        case 'd':
        case 'e':
        case 'n':
          throw new IllegalArgumentException("Wrong pattern format detected [" + c + "] in '" + asmPart + '\'');
      }
    }
    return asmPart;
  }

  private static int[] parseCode(final String codePart) {
    if (CODE_PART_CHECKING.matcher(codePart).matches()) {
      final int[] lst = new int[16];
      int index = 0;

      final Matcher m = CODE_PART_PARSING.matcher(codePart);

      while (m.find()) {
        final String str = m.group().trim();
        final int value;
        if (str.equals("d")) {
          value = SPEC_INDEX;
        }
        else if (str.equals("e")) {
          value = SPEC_OFFSET;
        }
        else if (str.equals("nn")) {
          value = SPEC_UNSIGNED_WORD;
        }
        else if (str.equals("n")) {
          value = SPEC_UNSIGNED_BYTE;
        }
        else {
          value = Integer.parseInt(str, 16);
        }
        lst[index++] = value;
      }
      return Arrays.copyOf(lst, index);
    }
    else {
      throw new IllegalArgumentException("Can't recognize byte command description [" + codePart + ']');
    }
  }

}
