package com.igormaznitsa.zxpoly.utils;

import java.time.Duration;
import java.util.concurrent.locks.LockSupport;

public final class Timer {

  private final long delay;
  private long start = 0L;
  private long timeout = -1L;

  private final long leadNanos;

  public Timer(final Duration delay) {
    this(delay, null);
  }

  public Timer(final Duration delay, final Duration lead) {
    this.delay = delay.toNanos();
    this.leadNanos = lead == null ? -1L : lead.toNanos();
  }

  static long nanosToPark(final long remainingNanos, final long leadNanos) {
    if (leadNanos <= 0L || remainingNanos <= leadNanos) {
      return 0L;
    }
    return remainingNanos - leadNanos;
  }

  static long nextTimeout(final long now, final long currentTimeout, final long delay) {
    if (currentTimeout > 0L) {
      final long overdue = now - currentTimeout;
      if (overdue >= 0L && overdue < delay) {
        return currentTimeout + delay;
      }
    }
    return now + delay;
  }

  public void next(final Duration delay) {
    this.start = System.nanoTime();
    this.timeout = this.start + delay.toNanos();
  }

  public void sleep() {
    final long parkNanos = nanosToPark(this.timeout - System.nanoTime(), this.leadNanos);
    if (parkNanos > 0L) {
      LockSupport.parkNanos(parkNanos);
    }
  }

  public void next() {
    final long now = System.nanoTime();
    final long armedTimeout = nextTimeout(now, this.timeout, this.delay);
    this.start = armedTimeout - this.delay;
    this.timeout = armedTimeout;
  }

  public boolean completed() {
    final long current = System.nanoTime();
    return current <= this.start || current >= this.timeout;
  }
}
