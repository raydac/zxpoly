package com.igormaznitsa.z80;

public final class Pair<L, R> {

  private final L left;
  private final R right;

  private Pair(L left, R right) {
    this.left = left;
    this.right = right;
  }

  public static <L, R> Pair<L, R> pairOf(L left, R right) {
    return new Pair<>(left, right);
  }

  public L getLeft() {
    return this.left;
  }

  public R getRight() {
    return this.right;
  }
}
