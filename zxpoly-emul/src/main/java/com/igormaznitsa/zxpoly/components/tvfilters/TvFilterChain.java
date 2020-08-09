package com.igormaznitsa.zxpoly.components.tvfilters;

import java.awt.Color;

public enum TvFilterChain {
  NONE("None", new TvFilter[0]),
  GRAYSCALE("Grayscale", new TvFilter[] {TvFilterGrayscale.getInstance()}),
  SCANLINES("Old TV", new TvFilter[] {TvFilterOldTv.getInstance()});

  private final String text;
  private final TvFilter[] filterChain;

  TvFilterChain(final String text, final TvFilter[] chain) {
    this.text = text;
    this.filterChain = chain;
  }

  public String getText() {
    return this.text;
  }

  public boolean isEmpty() {
    return this.filterChain.length == 0;
  }

  public Color applyBorderColor(final Color borderColor) {
    if (this.filterChain.length == 0) {
      return borderColor;
    }
    Color result = borderColor;
    for (final TvFilter f : this.filterChain) {
      result = f.applyBorderColor(result);
    }
    return result;
  }

  public TvFilter[] getFilterChain() {
    return this.filterChain;
  }
}
