package com.igormaznitsa.zxpoly.components.video.tvfilters;

import java.awt.*;

import static java.util.stream.Stream.of;

public enum TvFilterChain {
  NONE("None", new TvFilter[0]),
  GRAYSCALE("Grayscale", new TvFilter[]{TvFilterGrayscale.getInstance()}),
  BLACKWHITE("Black & White", new TvFilter[]{TvOrderedBayer.getInstance()}),
  OLDTV("Old TV Color", new TvFilter[]{TvFilterOldTv.getInstance()}),
  AMBERCRT("Old TV Amber", new TvFilter[]{TvFilterAmberCrt.getInstance()}),
  GREENCRT("Old TV Green", new TvFilter[]{TvFilterGreenCrt.getInstance()}),
  GAUSSIAN_BLUR("Gaussian blur", new TvFilter[]{TvFilterGaussian.getInstance()});

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

  public boolean isGifCompatible() {
    return of(this.filterChain).allMatch(TvFilter::isGifCompatible);
  }

  public TvFilter[] getFilterChain() {
    return this.filterChain;
  }
}
