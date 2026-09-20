package com.igormaznitsa.zxpoly.utils;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimerTest {

  private static final long DELAY = 20_000_000L;

  @Test
  public void testNextTimeoutKeepsCadenceWhenSlightlyLate() {
    final long timeout = 100_000_000L;
    final long now = timeout + 5_000_000L;

    assertEquals(timeout + DELAY, Timer.nextTimeout(now, timeout, DELAY));
  }

  @Test
  public void testNextTimeoutKeepsCadenceWhenOnTime() {
    final long timeout = 100_000_000L;

    assertEquals(timeout + DELAY, Timer.nextTimeout(timeout, timeout, DELAY));
  }

  @Test
  public void testNextTimeoutResetsWhenMoreThanOnePeriodLate() {
    final long timeout = 100_000_000L;
    final long now = timeout + DELAY + 1L;

    assertEquals(now + DELAY, Timer.nextTimeout(now, timeout, DELAY));
  }

  @Test
  public void testNextTimeoutArmsFromNowWhenUninitialized() {
    final long now = 50_000_000L;

    assertEquals(now + DELAY, Timer.nextTimeout(now, -1L, DELAY));
  }
}
