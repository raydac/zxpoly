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

  @Test
  public void testNanosToParkLeavesTheLeadUnparked() {
    assertEquals(14_000_000L, Timer.nanosToPark(15_000_000L, 1_000_000L));
  }

  @Test
  public void testNanosToParkStaysAwakeInsideTheLead() {
    assertEquals(0L, Timer.nanosToPark(500_000L, 1_000_000L));
    assertEquals(0L, Timer.nanosToPark(1_000_000L, 1_000_000L));
  }

  @Test
  public void testNanosToParkIsDisabledWithoutLead() {
    assertEquals(0L, Timer.nanosToPark(15_000_000L, -1L));
    assertEquals(0L, Timer.nanosToPark(15_000_000L, 0L));
    assertEquals(0L, Timer.nanosToPark(-1L, 1_000_000L));
  }
}
