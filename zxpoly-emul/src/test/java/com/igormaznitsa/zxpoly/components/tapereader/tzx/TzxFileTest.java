package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public class TzxFileTest extends AbstractTzxTest {

  private static final String[] TEST_GAMES = new String[]{
          "3descape",
          "720degrees",
          "basil",
          "book",
          "clowns",
          "dd2",
          "pang",
          "poker",
          "ppt",
          "quest",
          "wcc",
          "wizardswarrior",
          "worldgames",
          "silvas",
          "xevious",
          "sII",
          "tlc",
          "test_control_blocks",
          "trailing-pause-block",
          "vallation",
          "dvIII"
  };

  @Test
  public void testReadRenderWrite() throws IOException {
    for (final String game : TEST_GAMES) {
      System.out.println("TZX test: " + game);
      final byte[] data = readTestResource(game);
      final TzxFile parsed = new TzxFile(new ByteArrayInputStream(data));

      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      parsed.write(buffer);
      buffer.close();

      final byte[] savedResult = buffer.toByteArray();
      assertArrayEquals(data, savedResult);

      final TzxWavRenderer.RenderResult renderResult = new TzxWavRenderer(TzxWavRenderer.Freq.FREQ_44100, parsed, null).render();
      assertTrue(renderResult.getWavData().length > 0);
    }
  }

}