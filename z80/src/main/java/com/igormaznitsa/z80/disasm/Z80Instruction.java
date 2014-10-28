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
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Z80Instruction {

  public static final int SPEC_INDEX = 0x100;
  public static final int SPEC_OFFSET = 0x101;
  public static final int SPEC_UNSIGNED_BYTE = 0x102;
  public static final int SPEC_UNSIGNED_WORD = 0x103;

  private final int[] codes;
  private final String textRepresentation;

  private static final Pattern CODE_PART_CHECKING = Pattern.compile("([0-9A-F]{2})+(\\s+(d|e|nn|n))*(\\s*[0-9A-F]{2}+)?");
  private static final Pattern CODE_PART_PARSING = Pattern.compile("[0-9A-F]{2}|\\s+(?:d|e|nn|n)");

  private final int length;
  private final int fixedPartLength;

  private final boolean has_index;
  private final boolean has_offset;
  private final boolean has_byte;
  private final boolean has_word;

  public Z80Instruction(final String def) {
    final String codePart = def.substring(0, 11).trim();
    final String asmPart = def.substring(11).trim();

    this.codes = parseCode(codePart);
    this.textRepresentation = checkAsmPart(asmPart);

    int len = 0;
    int fixLength = 0;
    boolean calcFixLength = true;

    boolean hasIndex = false;
    boolean hasOffset = false;
    boolean hasByte = false;
    boolean hasWord = false;

    for (final int c : this.codes) {
      len++;
      switch (c) {
        case SPEC_INDEX:
          hasIndex = true;
          break;
        case SPEC_OFFSET:
          hasOffset = true;
          break;
        case SPEC_UNSIGNED_BYTE:
          hasByte = true;
          break;
        case SPEC_UNSIGNED_WORD:
          hasWord = true;
          len++;
          break;
      }

      if (c < SPEC_INDEX && calcFixLength) {
        fixLength++;
      }
      else {
        calcFixLength = false;
      }
    }
    this.length = len;
    this.fixedPartLength = fixLength;
    this.has_byte = hasByte;
    this.has_index = hasIndex;
    this.has_offset = hasOffset;
    this.has_word = hasWord;
  }

  public boolean hasIndex() {
    return this.has_index;
  }

  public boolean hasOffset() {
    return this.has_offset;
  }

  public boolean hasByte() {
    return this.has_byte;
  }

  public boolean hasWord() {
    return this.has_word;
  }

  public int getFixedPartLength() {
    return this.fixedPartLength;
  }

  public int getLength() {
    return this.length;
  }

  int[] getInstructionCodes() {
    return this.codes;
  }

  public boolean matches(final byte[] array, int offset) {
    for (int i = 0; i < this.codes.length; i++) {
      if (offset >= array.length) {
        return false;
      }
      switch (this.codes[i]) {
        case SPEC_INDEX:
        case SPEC_OFFSET:
        case SPEC_UNSIGNED_BYTE:
          offset++;
          break;
        case SPEC_UNSIGNED_WORD:
          offset += 2;
          if (offset >= array.length) {
            return false;
          }
          break;
        default: {
          if ((array[offset++] & 0xFF) != this.codes[i]) {
            return false;
          }
        }
        break;
      }
    }
    return true;
  }

  private String checkAsmPart(final String asmPart) {
    final String replace = asmPart.replace("+d", "%").replace("e", "%").replace("nn", "%").replace("n", "%");
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

  private static String indexToHex(final byte index) {
    String num = Integer.toHexString(Math.abs(index)).toUpperCase(Locale.ENGLISH);
    if (num.length() < 2) {
      num = '0' + num;
    }

    if (index < 0) {
      return "-#" + num;
    }
    else {
      return "+#" + num;
    }
  }

  private static String offsetToHex(final byte offset, final int fixPartLenghtOfCommand, final int programCounter) {
    if (programCounter < 0) {
      String num = Integer.toHexString(Math.abs(offset)).toUpperCase(Locale.ENGLISH);
      if (num.length() < 2) {
        num = '0' + num;
      }

      if (offset < 0) {
        return "PC-#" + num;
      }
      else {
        return "PC+#" + num;
      }
    }
    else {
      final int address = programCounter + offset + (offset<0 ? fixPartLenghtOfCommand : 0);
      String addressAsHex = Integer.toHexString(Math.abs(address)).toUpperCase(Locale.ENGLISH);
      return '#' + (addressAsHex.length() < 4 ? "0000".substring(0, 4 - addressAsHex.length()) + addressAsHex : addressAsHex);
    }
  }

  private static String unsignedByteToHex(final byte value) {
    String str = Integer.toHexString(value & 0xFF).toUpperCase(Locale.ENGLISH);
    return '#' + (str.length() < 2 ? '0' + str : str);
  }

  private static String unsignedWordToHex(final byte low, final byte hi) {
    String str = Integer.toHexString((hi << 8 | (low & 0xFF)) & 0xFFFF).toUpperCase(Locale.ENGLISH);
    return '#' + (str.length() < 4 ? "0000".substring(0, 4 - str.length()) + str : str);
  }

  /**
   * Decode instruction placed in byte array for its offset.
   *
   * @param array a byte array contains the instruction, must not be null
   * @param offset the offset to the instruction in the byte array, must be 0 or
   * greater
   * @param pcCounter the current PC counter, it can be negative if its value is
   * not known
   * @return the string representation of instruction or null if it was
   * impossible to decode the instruction
   */
  public String decode(final byte[] array, int offset, final int pcCounter) {
    String sindex = null;
    String soffset = null;
    String sbyte = null;
    String sword = null;

    for (int i = 0; i < this.codes.length; i++) {
      if (offset >= array.length) {
        return null;
      }
      switch (this.codes[i]) {
        case SPEC_INDEX: {
          sindex = indexToHex(array[offset++]);
        }
        break;
        case SPEC_OFFSET: {
          soffset = offsetToHex(array[offset++], this.fixedPartLength, pcCounter);
        }
        break;
        case SPEC_UNSIGNED_BYTE: {
          sbyte = unsignedByteToHex(array[offset++]);
        }
        break;
        case SPEC_UNSIGNED_WORD: {
          if (offset > (array.length - 2)) {
            return null;
          }
          sword = unsignedWordToHex(array[offset++], array[offset++]);
        }
        break;
        default: {
          if ((array[offset++] & 0xFF) != this.codes[i]) {
            return null;
          }
        }break;
      }
    }

    String result = this.textRepresentation;
    if (sindex != null) {
      result = result.replace("+d", sindex);
    }
    if (soffset != null) {
      result = result.replace("e", soffset);
    }
    if (sbyte != null) {
      result = result.replace("n", sbyte);
    }
    if (sword != null) {
      result = result.replace("nn", sword);
    }

    return result;
  }

  @Override
  public String toString() {
    return this.textRepresentation;
  }

}
