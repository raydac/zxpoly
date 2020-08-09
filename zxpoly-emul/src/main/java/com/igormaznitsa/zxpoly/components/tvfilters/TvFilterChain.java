package com.igormaznitsa.zxpoly.components.tvfilters;

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

  public TvFilter[] getFilterChain() {
    return this.filterChain;
  }
}
