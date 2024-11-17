package com.igormaznitsa.zxpoly.components.tapereader;

public interface TapeContext {
  void onTapeSignal(TapeSource tapeSource, ControlSignal controlSignal);

  enum ControlSignal {
    STOP_TAPE,
    STOP_TAPE_IF_ZX48
  }
}
