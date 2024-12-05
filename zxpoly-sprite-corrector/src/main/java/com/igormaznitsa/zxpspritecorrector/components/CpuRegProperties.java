package com.igormaznitsa.zxpspritecorrector.components;

import java.util.Properties;

public class CpuRegProperties extends Properties {

  public static final String IFF1 = "IFF1";
  public static final String IFF2 = "IFF2";
  public static final String REG_ALT_A = "ALT_A";
  public static final String REG_ALT_F = "ALT_F";
  public static final String REG_ALT_BC = "ALT_BC";
  public static final String REG_ALT_DE = "ALT_DE";
  public static final String REG_ALT_HL = "ALT_HL";
  public static final String REG_A = "A";
  public static final String REG_F = "F";
  public static final String REG_BC = "BC";
  public static final String REG_DE = "DE";
  public static final String REG_HL = "HL";
  public static final String REG_IX = "IX";
  public static final String REG_IY = "IY";
  public static final String REG_IR = "IR";
  public static final String REG_IM = "IM";
  public static final String REG_PC = "PC";
  public static final String REG_SP = "SP";
  public static final String PORT_7FFD = "7FFD";

  public CpuRegProperties(Properties defaults) {
    super(defaults);
  }

  public CpuRegProperties() {
    super();
  }

  public int extractInt(final String name, final int defaultValue) {
    final String property = this.getProperty(name, Integer.toString(defaultValue));
    return Integer.parseInt(property.trim());
  }

  public boolean extractBoolean(final String name, final boolean defaultValue) {
    final String property = this.getProperty(name, Boolean.toString(defaultValue));
    return Boolean.parseBoolean(property.trim());
  }

}
