package com.igormaznitsa.zxpoly.components.video;

public enum BorderSize {
  NONE(0, 0, 0, 0),
  SMALL(20, 20, 40, 40),
  FULL(-1,
          -1,
          -1,
          -1);

  public final int leftPixels;
  public final int rightPixels;
  public final int topPixels;
  public final int bottomPixels;

  BorderSize(final int leftPixels, final int rightPixels, final int topPixels, final int bottomPixels) {
    this.leftPixels = leftPixels;
    this.rightPixels = rightPixels;
    this.topPixels = topPixels;
    this.bottomPixels = bottomPixels;
  }

}
