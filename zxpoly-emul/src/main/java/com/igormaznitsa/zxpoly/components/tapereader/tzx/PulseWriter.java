package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import java.io.IOException;

@FunctionalInterface
public interface PulseWriter {
  void writePulse(int pulseLength, boolean hi) throws IOException;
}
