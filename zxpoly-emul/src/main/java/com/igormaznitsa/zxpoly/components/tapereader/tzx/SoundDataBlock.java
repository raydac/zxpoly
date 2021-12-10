package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import java.io.IOException;

public interface SoundDataBlock {

  int getDataLength();

  byte[] extractData() throws IOException;
}
