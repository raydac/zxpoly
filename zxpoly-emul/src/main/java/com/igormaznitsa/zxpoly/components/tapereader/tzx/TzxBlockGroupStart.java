package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

import static com.igormaznitsa.zxpoly.components.tapereader.tzx.TzxWavRenderer.WAV_HEADER_LENGTH;

public class TzxBlockGroupStart extends AbstractTzxInformationBlock implements ITzxBlock {

  private final String groupName;

  public TzxBlockGroupStart(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.GROUP_START.getId());
    final int length = inputStream.readByte();
    this.groupName = new String(inputStream.readByteArray(length), StandardCharsets.ISO_8859_1);
  }

  public String getGroupName() {
    return groupName;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    final byte[] chars = this.groupName.getBytes(StandardCharsets.ISO_8859_1);
    outputStream.write(chars.length);
    outputStream.write(chars);
  }

  @Override
  public TzxWavRenderer.RenderResult.NamedOffsets renderBlockProcess(Logger logger, Long dataStreamCounter) {
    return new TzxWavRenderer.RenderResult.NamedOffsets("GROUP>> " + this.getGroupName(), WAV_HEADER_LENGTH + dataStreamCounter);
  }
}
