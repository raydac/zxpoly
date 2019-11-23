package com.igormaznitsa.zxpoly.formats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;


import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

public class Spec256ArchTest {

  private byte[] readSnapshot(final String name) throws IOException {
    return IOUtils.resourceToByteArray("/snapshots/" + name);
  }

  @Test
  public void testParseSpec256_48kb() throws Exception {
    final Spec256Arch arch = new Spec256Arch(readSnapshot("Renegade_spec256.zip"));
    assertNull(arch.getParsedSna().extendeddata);
    assertEquals(16, arch.getProperties().size());
    assertEquals(8, arch.getGfxRoms().size());
    assertEquals(24, arch.getGfxRamPages().size());
  }

  @Test
  public void testParseSpec256_128kb() throws Exception {
    final Spec256Arch arch = new Spec256Arch(readSnapshot("TreeWeeks128k_spec256.zip"));
    assertNotNull(arch.getParsedSna().extendeddata);
    assertTrue(arch.getGfxRoms().isEmpty());
    assertEquals(15, arch.getProperties().size());
    assertEquals(72, arch.getGfxRamPages().size());
  }
}