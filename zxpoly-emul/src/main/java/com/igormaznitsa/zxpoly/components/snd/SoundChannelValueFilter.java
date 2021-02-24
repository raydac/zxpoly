package com.igormaznitsa.zxpoly.components.snd;

final class SoundChannelValueFilter {

  SoundChannelValueFilter() {
    this.reset();
  }

  void reset() {
  }

  int update(final int spentTstates, final int nextLevel) {
    return nextLevel;
  }

}
