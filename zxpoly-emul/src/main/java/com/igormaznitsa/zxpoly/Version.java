package com.igormaznitsa.zxpoly;

public interface Version {
  int VERSION_MAJOR = 2;
  int VERSION_MINOR = 3;
  int VERSION_BUILD = 4;

  String APP_TITLE = "ZX-Poly emulator";
  String APP_VERSION =
      String.format("v %d.%d.%d", VERSION_MAJOR, VERSION_MINOR, VERSION_BUILD);

}
