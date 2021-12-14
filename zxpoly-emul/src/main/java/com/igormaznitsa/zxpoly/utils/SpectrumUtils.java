package com.igormaznitsa.zxpoly.utils;

public class SpectrumUtils {
  private static final String[] UPPER = new String[]{
          " ", "▝", "▘", "▀", "▗", "▐", "▚", "▜", "▖", "▞", "▌", "▛", "▄", "▟",
          "▙", "█", "Ⓐ", "Ⓑ", "Ⓒ", "Ⓓ", "Ⓔ", "Ⓕ", "Ⓖ", "Ⓗ", "Ⓘ", "Ⓙ", "Ⓚ",
          "Ⓛ", "Ⓜ", "Ⓝ", "Ⓞ", "Ⓟ", "Ⓠ", "Ⓡ", "Ⓢ", "Ⓣ", "Ⓤ", "RND", "INKEY$",
          "PI", "FN ", "POINT ", "SCREEN$ ", "ATTR ", "AT ", "TAB ", "VAL$ ",
          "CODE ", "VAL ", "LEN ", "SIN ", "COS ", "TAN ", "ASN ", "ACS ", "ATN ",
          "LN ", "EXP ", "INT ", "SQR ", "SGN ", "ABS ", "PEEK ", "IN ", "USR ",
          "STR$ ", "CHR$ ", "NOT ", "BIN ", " OR ", " AND ", "<=", ">=", "<>",
          " LINE ", " THEN ", " TO ", " STEP ", " DEF FN ", " CAT ", " FORMAT ",
          " MOVE ", " ERASE ", " OPEN #", " CLOSE #", " MERGE ", " VERIFY ",
          " BEEP ", " CIRCLE ", " INK ", " PAPER ", " FLASH ", " BRIGHT ",
          " INVERSE ", " OVER ", " OUT ", " LPRINT ", " LLIST ", " STOP ", " READ ",
          " DATA ", " RESTORE ", " NEW ", " BORDER ", " CONTINUE ", " DIM ", " REM ",
          " FOR ", " GO TO ", " GO SUB ", " INPUT ", " LOAD ", " LIST ", " LET ",
          " PAUSE ", " NEXT ", " POKE ", " PRINT ", " PLOT ", " RUN ", " SAVE ",
          " RANDOMIZE ", " IF ", " CLS ", " DRAW ", " CLEAR ", " RETURN ", " COPY "
  };

  public static String fromZxString(final String str) {
    final StringBuilder result = new StringBuilder();
    for (final char c : str.toCharArray()) {
      result.append(fromZxChar(c));
    }
    return result.toString();
  }

  public static String fromZxChar(final char chr) {
    final int code = chr & 0xFF;
    if (chr > 0x80) {
      return UPPER[code - 0x80];
    } else
      switch (code) {
        case 0x0D:
          return "\n";
        case 0x5E:
          return "↑";
        case 0x60:
          return "£";
        case 0x7F:
          return "©";
        default:
          return String.valueOf(chr);
      }
  }

}
