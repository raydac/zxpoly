package com.igormaznitsa.zxpoly.components.snd;

public interface AyBasedSoundDevice {
  void setAyAddress(int address);

  void setAyRegister(int address, int value);
}
