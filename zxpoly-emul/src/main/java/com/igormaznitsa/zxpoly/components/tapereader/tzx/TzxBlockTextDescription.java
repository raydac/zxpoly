package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TzxBlockTextDescription extends AbstractTzxInformationBlock {

  private final String text;

  public TzxBlockTextDescription(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.TEXT_DESCRIPTION.getId());
    final int chars = inputStream.readByte();
    this.text = new String(inputStream.readByteArray(chars), StandardCharsets.ISO_8859_1);
  }

  public String getText() {
    return text;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    final byte[] chars = this.text.getBytes(StandardCharsets.ISO_8859_1);
    outputStream.write(chars.length);
    outputStream.write(chars);
  }
}
