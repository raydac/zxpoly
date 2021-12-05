package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public abstract class AbstractTzxTest {

  protected static byte[] readTestResource(final String name) throws IOException {
    final String resourcePath = "/tzx/" + name + ".tzx";
    return IOUtils.resourceToByteArray(resourcePath);
  }

  protected static byte[] asByteArray(final AbstractTzxBlock block) throws IOException {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    final JBBPBitOutputStream jbbpBitOutputStream = new JBBPBitOutputStream(outputStream);
    block.write(jbbpBitOutputStream);
    jbbpBitOutputStream.flush();
    jbbpBitOutputStream.close();

    byte[] saved = outputStream.toByteArray();
    byte[] withoutId = new byte[saved.length - 1];
    System.arraycopy(saved, 1, withoutId, 0, withoutId.length);

    return withoutId;
  }


}
