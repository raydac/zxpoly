package com.igormaznitsa.zxpoly.components.video;

import java.io.IOException;

public enum VirtualKeyboardLook {
  ZX48("zx48"),
  DEFAULT("default");

  private final String baseName;

  VirtualKeyboardLook(final String baseName) {
    this.baseName = baseName;
  }

  public VirtualKeyboardDecoration load() throws IOException {
    return new VirtualKeyboardDecoration(this.baseName);
  }
}
