package com.igormaznitsa.zxpoly.components.sound;

public interface AyBasedSoundDevice {
  void setAyAddress(int address);

  int getAyAddress();

  void setAyRegister(int address, int value);

  int getAyRegister(int address);
}
