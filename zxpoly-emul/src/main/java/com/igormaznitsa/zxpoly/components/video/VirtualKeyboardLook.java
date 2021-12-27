package com.igormaznitsa.zxpoly.components.video;

import java.io.IOException;

public enum VirtualKeyboardLook {
  DEFAULT("default"),
  ZX48("zx48"),
  AMBRO_ZXPLUS("ambrozxplus"),
  AMBRO_TIMEX("ambrotimex"),
  AMBRO_TIMEX2048PAL("ambrotimex2048pal");

  private final String baseName;

  VirtualKeyboardLook(final String baseName) {
    this.baseName = baseName;
  }

  public VirtualKeyboardDecoration load() throws IOException {
    return new VirtualKeyboardDecoration(this.baseName);
  }

  @Override
  public String toString() {
    return this.name();
  }
}
