package com.igormaznitsa.zxpoly.utils;

import junit.framework.TestCase;
import org.junit.Test;

public class IntMapTest extends TestCase {
  @Test
  public void testPutGet() {
    final IntMap map = new IntMap(0x1000);

    final int size = 0xFFFFF;

    for (int i = 0; i < size; i++) {
      map.put(i, 10000 + i);
    }

    assertEquals(size, map.size());

    for (int i = 0; i < size; i++) {
      assertEquals(10000 + i, map.get(i).getAsInt());
    }
  }
}