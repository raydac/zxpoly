package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class TzxBlockCustomInfo extends AbstractTzxBlock implements InformationBlock {

  private final String idString;
  private final byte[] customInfo;

  public TzxBlockCustomInfo(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.CUSTOM_INFO.getId());
    this.idString = new String(inputStream.readByteArray(0x10), StandardCharsets.ISO_8859_1);
    final long length = readDWord(inputStream);
    this.customInfo = inputStream.readByteArray((int) length);
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    outputStream.write(Arrays.copyOf(this.idString.getBytes(StandardCharsets.ISO_8859_1), 0x10));
    writeDWord(outputStream, this.customInfo.length);
    outputStream.write(this.customInfo);
  }

  public String getIdString() {
    return idString;
  }

  public byte[] getCustomInfo() {
    return customInfo;
  }
}
