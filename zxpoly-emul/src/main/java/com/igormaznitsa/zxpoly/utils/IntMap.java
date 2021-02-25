package com.igormaznitsa.zxpoly.utils;

import java.util.OptionalInt;

import static java.util.Arrays.copyOf;

public final class IntMap {

  private final long[][] baskets;

  public IntMap(final int capacity) {
    this.baskets = new long[capacity][];
  }

  public void put(int key, int value) {
    final int basketIndex = key % this.baskets.length;

    final long longKey = (long) key & 0xFFFF_FFFFL;

    long[] kvPair = this.baskets[basketIndex];
    if (kvPair == null) {
      kvPair = new long[]{(longKey << 32) | ((long) value & 0xFFFF_FFFFL)};
      this.baskets[basketIndex] = kvPair;
    } else {
      boolean found = false;
      for (int i = 0; i < kvPair.length; i++) {
        if ((kvPair[i] >>> 32) == longKey) {
          found = true;
          kvPair[i] = (longKey << 32) | ((long) value & 0xFFFF_FFFFL);
          break;
        }
      }
      if (!found) {
        kvPair = copyOf(kvPair, kvPair.length + 1);
        kvPair[kvPair.length - 1] = (longKey << 32) | ((long) value & 0xFFFF_FFFFL);
        this.baskets[basketIndex] = kvPair;
      }
    }
  }

  public OptionalInt get(final int key) {
    final int basketIndex = key % this.baskets.length;
    long[] basket = this.baskets[basketIndex];
    if (basket != null) {
      final long longKey = (long) key & 0xFFFF_FFFFL;
      for (final long kvPair : basket) {
        if ((kvPair >>> 32) == longKey) {
          return OptionalInt.of((int) kvPair);
        }
      }
    }
    return OptionalInt.empty();
  }

  public int size() {
    int result = 0;
    for (final long[] b : this.baskets) {
      result += b == null ? 0 : b.length;
    }
    return result;
  }
}
