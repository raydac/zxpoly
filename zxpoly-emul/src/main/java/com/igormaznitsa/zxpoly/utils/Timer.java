package com.igormaznitsa.zxpoly.utils;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

public final class Timer {

  private final long delay;
  private long start = 0L;
  private long timeout = -1L;

  private final long sleepDelay;

  public Timer(final Duration delay) {
    this(delay, null);
  }

  public Timer(final Duration delay, final Duration sleepDelay) {
    this.delay = delay.toNanos();
    this.sleepDelay = sleepDelay == null ? -1L : sleepDelay.toNanos();
  }

  public void next(final Duration delay) {
    this.start = System.nanoTime();
    this.timeout = this.start + delay.toNanos();
  }

  public void sleep() {
    if (this.sleepDelay > 0L) {
      final long nanos = this.timeout - System.nanoTime();
      if (nanos > this.sleepDelay) {
        LockSupport.parkNanos(this.sleepDelay);
      }
    }
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
