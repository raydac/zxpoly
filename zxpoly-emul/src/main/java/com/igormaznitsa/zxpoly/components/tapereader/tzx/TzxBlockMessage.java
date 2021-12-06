package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TzxBlockMessage extends AbstractTzxBlock implements InformationBlock {

  private final int timeInSeconds;
  private final String text;

  public TzxBlockMessage(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.MESSAGE_BLOCK.getId());
    this.timeInSeconds = inputStream.readByte();
    final int chars = inputStream.readByte();
    this.text = new String(inputStream.readByteArray(chars), StandardCharsets.ISO_8859_1);
  }

  public int getTimeInSeconds() {
    return timeInSeconds;
  }

  public String getText() {
    return text;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    outputStream.write(this.timeInSeconds);
    final byte[] chars = this.text.getBytes(StandardCharsets.ISO_8859_1);
    outputStream.write(chars.length);
    outputStream.write(chars);
  }
}
