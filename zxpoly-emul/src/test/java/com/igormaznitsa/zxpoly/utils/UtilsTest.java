package com.igormaznitsa.zxpoly.utils;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UtilsTest {

  @Test
  public void test() {
    assertEquals(1, Utils.minimalRequiredBitsFor(0));
    assertEquals(2, Utils.minimalRequiredBitsFor(3));
    assertEquals(5, Utils.minimalRequiredBitsFor(31));
    assertEquals(6, Utils.minimalRequiredBitsFor(32));
    assertEquals(6, Utils.minimalRequiredBitsFor(37));
    assertEquals(32, Utils.minimalRequiredBitsFor(-128));
    assertEquals(8, Utils.minimalRequiredBitsFor(128));
  }

}