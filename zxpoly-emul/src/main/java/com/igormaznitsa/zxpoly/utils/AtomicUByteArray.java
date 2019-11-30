package com.igormaznitsa.zxpoly.utils;

import java.util.concurrent.atomic.AtomicIntegerArray;

public final class AtomicUByteArray {
  private final AtomicIntegerArray array;
  private final int length;

  public AtomicUByteArray(final int size) {
    if ((size & 3) != 0) {
      throw new IllegalArgumentException("Wrong size: " + size);
    }
    this.length = size;
    this.array = new AtomicIntegerArray(size / 4);
  }

  public int length() {
    return this.length;
  }

  public int get(final int addr) {
    final int lineAddr = addr >> 2;
    final int shft = (addr & 0b11) << 3;
    final int value = this.array.get(lineAddr);
    return value >>> shft;
  }

  public void set(final int addr, final int data) {
    final int lineAddr = addr >> 2;
    final int shft = (addr & 0b11) << 3;
    int oldvalue;
    int newvalue;
    do {
      oldvalue = this.array.get(lineAddr);
      newvalue = (oldvalue & ~(0xFF << shft)) | ((data & 0xFF) << shft);
    } while (!this.array.compareAndSet(lineAddr, oldvalue, newvalue));
  }
}
