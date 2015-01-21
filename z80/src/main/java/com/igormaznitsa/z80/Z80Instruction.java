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
package com.igormaznitsa.z80;

import com.igormaznitsa.z80.asm.Z80AsmCompileException;
import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class Z80Instruction {

  public interface ExpressionProcessor {

    int evalExpression(String expression);
  }

  private static final Pattern CODE_PART_CHECKING = Pattern.compile("([0-9A-F]{2})+(\\s+(d|e|nn|n))*(\\s*[0-9A-F]{2}+)?");
  private static final Pattern CODE_PART_PARSING = Pattern.compile("[0-9A-F]{2}|\\s+(?:d|e|nn|n)");

  private final static List<Z80Instruction> INSTRUCTIONS;

  static {
    final List<Z80Instruction> list = new ArrayList<>(1500);
    final InputStream in = Z80Instruction.class.getClassLoader().getResourceAsStream("z80opcodes.lst");
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
      INSTRUCTIONS = Collections.unmodifiableList(list);
    }
    catch (IOException ex) {
      throw new Error("Can't load Z80 instruction list", ex);
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
  }

  public static final int SPEC_INDEX = 0x100;
  public static final int SPEC_OFFSET = 0x101;
  public static final int SPEC_UNSIGNED_BYTE = 0x102;
  public static final int SPEC_UNSIGNED_WORD = 0x103;

  private final Pattern asmPattern;
  private final int[] asmGroupValues;
  private final int[] codes;
  private final String textRepresentation;

  private final int length;
  private final int fixedPartLength;

  private final boolean has_index;
  private final boolean has_offset;
  private final boolean has_byte;
  private final boolean has_word;

  public static List<Z80Instruction> getInstructions() {
    return INSTRUCTIONS;
  }

  Z80Instruction(final String def) {
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
        default:
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

    final String preprocessed = asmPart.replace("+d", "<").replace("e", ">").replace("nn", "%").replace("n", "&");
    final StringBuilder builder = new StringBuilder();

    int asmGroupIndex = 0;
    final int[] asmGroups = new int[10];

    final StringBuilder hexNum = new StringBuilder();

    final String group = "(\\S.*)";

    builder.append('^');
    int hexNumCntr = 0;
    for (final char c : preprocessed.toCharArray()) {
      switch (c) {
        case '#': {
          hexNumCntr = 2;
        }
        break;
        case '<': {
          builder.append(group);
          asmGroups[asmGroupIndex++] = SPEC_INDEX;
        }
        break;
        case '>': {
          builder.append(group);
          asmGroups[asmGroupIndex++] = SPEC_OFFSET;
        }
        break;
        case '%': {
          builder.append(group);
          asmGroups[asmGroupIndex++] = SPEC_UNSIGNED_WORD;
        }
        break;
        case '&': {
          builder.append(group);
          asmGroups[asmGroupIndex++] = SPEC_UNSIGNED_BYTE;
        }
        break;
        case ',':
        case ')':
        case '(': {
          builder.append("\\s*\\").append(c).append("\\s*");
        }
        break;
        default: {
          if (hexNumCntr > 0) {
            hexNum.append(c);
            hexNumCntr--;
            if (hexNumCntr == 0) {
              asmGroups[asmGroupIndex++] = Integer.parseInt(hexNum.toString(), 16);
              hexNum.setLength(0);
              builder.append(group);
            }
          }
          else if (Character.isDigit(c)) {
            asmGroups[asmGroupIndex++] = c - '0';
            builder.append(group);
          }
          else {
            builder.append(c);
          }
        }
        break;
      }
    }
    builder.append('$');

    this.asmPattern = Pattern.compile(builder.toString(), Pattern.CASE_INSENSITIVE);
    this.asmGroupValues = Arrays.copyOf(asmGroups, asmGroupIndex);
  }

  public boolean matches(final String asm) {
    return this.asmPattern.matcher(asm.trim()).matches();
  }

  public byte[] compile(final String asm, final ExpressionProcessor expressionCalc) {

    final Matcher m = this.asmPattern.matcher(asm.trim());
    if (m.find()) {
      // check constants
      for (int i = 0; i < this.asmGroupValues.length; i++) {
        final int value = this.asmGroupValues[i];
        if (value < SPEC_INDEX) {
          final int result = expressionCalc.evalExpression(m.group(i + 1));
          if (result != value) {
            return null;
          }
        }
      }

      // fill values
      final byte[] resultBuff = new byte[16];

      int resultIndex = 0;

      for (int i = 0; i < this.codes.length; i++) {
        final int type = this.codes[i];
        if (type < SPEC_INDEX) {
          resultBuff[resultIndex++] = (byte) type;
        }
        else {
          int groupIndex = -1;
          for (int j = 0; j < this.asmGroupValues.length; j++) {
            if (this.asmGroupValues[j] == type) {
              groupIndex = j + 1;
              break;
            }
          }
          if (groupIndex < 0) {
            throw new Error("Unexpected state, group nt found!");
          }
          final int result = expressionCalc.evalExpression(m.group(groupIndex));
          switch (type) {
            case SPEC_INDEX: {
              if (result < -128 || result > 127) {
                throw new Z80AsmCompileException("Wrong index value [" + result + ']');
              }
              resultBuff[resultIndex++] = (byte) result;
            }
            break;
            case SPEC_OFFSET: {
              if (result < -128 || result > 127) {
                throw new Z80AsmCompileException("Wrong offset value [" + result + ']');
              }
              resultBuff[resultIndex++] = (byte) result;
            }
            break;
            case SPEC_UNSIGNED_BYTE: {
              resultBuff[resultIndex++] = (byte) result;
            }
            break;
            case SPEC_UNSIGNED_WORD: {
              resultBuff[resultIndex++] = (byte) result;
              resultBuff[resultIndex++] = (byte) (result >>> 8);
            }
            break;
            default: {
              throw new Error("Unexpected type [" + type + ']');
            }
          }
        }
      }
      return Arrays.copyOf(resultBuff, resultIndex);
    }
    else {
      return null;
    }
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

  public int[] getInstructionCodes() {
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
      final int theoffset = fixPartLenghtOfCommand + 1 + offset;
      String num = Integer.toHexString(Math.abs(theoffset)).toUpperCase(Locale.ENGLISH);
      if (num.length() < 2) {
        num = '0' + num;
      }

      if (theoffset < 0) {
        return "PC-#" + num;
      }
      else {
        return "PC+#" + num;
      }
    }
    else {
      final int address = programCounter + offset + fixPartLenghtOfCommand + 1;
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
        }
        break;
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
