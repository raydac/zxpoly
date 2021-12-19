package com.igormaznitsa.zxpoly.components.tapereader.tzx;

public abstract class AbstractTzxFlowManagementBlock extends AbstractTzxBlock {
  public static final short[] ZERO = new short[]{0};
  public static final short[] NEXT = new short[]{1};

  public AbstractTzxFlowManagementBlock(final int id) {
    super(id);
  }

  abstract short[] getOffsets();
}
