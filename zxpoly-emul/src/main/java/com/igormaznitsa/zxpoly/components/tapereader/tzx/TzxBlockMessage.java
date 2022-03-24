package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static com.igormaznitsa.zxpoly.components.tapereader.tzx.TzxWavRenderer.WAV_HEADER_LENGTH;

public class TzxBlockMessage extends AbstractTzxInformationBlock implements ITzxBlock {

  private final int timeInSeconds;
  private final String text;

  public TzxBlockMessage(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.MESSAGE_BLOCK.getId());
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

  @Override
  public TzxWavRenderer.RenderResult.NamedOffsets renderBlockProcess(Logger logger, Long dataStreamCounter) {
    if (logger != null) {
      final String messageText = this.getText().replace('\r', ' ').replace('\t', ' ').replace('\n', ' ');
      logger.info("TzxMessage: " + messageText);
      return new TzxWavRenderer.RenderResult.NamedOffsets("MESSAGE: " + messageText, WAV_HEADER_LENGTH + dataStreamCounter);
    }
    return null;
  }
}
