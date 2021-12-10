package com.igormaznitsa.zxpoly.components.tapereader.tzx;

public interface FlowManagementBlock {
  short[] ZERO = new short[]{0};
  short[] NEXT = new short[]{1};

  short[] getOffsets();
}
