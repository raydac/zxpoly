package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;

public class TzxFileTest extends AbstractTzxTest {

  private static final String[] TEST_GAMES = new String[]{"3descape", "720degrees", "pang", "wizardswarrior", "worldgames", "ppt"};

  @Test
  public void testReadWrite() throws IOException {
    for (final String game : TEST_GAMES) {
      final byte[] data = readTestResource(game);
      final TzxFile parsed = new TzxFile(new ByteArrayInputStream(data));

      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      parsed.write(buffer);
      buffer.close();

      final byte[] savedResult = buffer.toByteArray();
      assertArrayEquals(data, savedResult);
    }
  }

}