package com.igormaznitsa.zxpoly.utils;

import java.time.Duration;

public final class Timer {

  private final long delay;
  private long start = 0L;
  private long timeout = -1L;

  public Timer(final Duration delay) {
    this.delay = delay.toNanos();
  }

  public void next(final Duration delay) {
    this.start = System.nanoTime();
    this.timeout = this.start + delay.toNanos();
  }

  public void next() {
    this.start = System.nanoTime();
    this.timeout = this.start + this.delay;
  }

  public boolean completed() {
    final long current = System.nanoTime();
    return current <= this.start || current >= this.timeout;
  }
}
