package com.igormaznitsa.zxpoly.utils;

import java.time.Duration;

public final class Timer {

  private final long delay;
  private long timeout = -1L;

  public Timer(final Duration delay) {
    this.delay = delay.toNanos();
  }

  public void next(final Duration delay) {
    this.timeout = System.nanoTime() + delay.toNanos();
  }

  public void next() {
    this.timeout = System.nanoTime() + this.delay;
  }

  public boolean completed() {
    return System.nanoTime() >= this.timeout;
  }
}
