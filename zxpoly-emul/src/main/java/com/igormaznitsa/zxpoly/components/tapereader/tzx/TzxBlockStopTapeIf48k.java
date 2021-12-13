package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockStopTapeIf48k extends AbstractTzxBlock implements SoundDataBlock {

  public TzxBlockStopTapeIf48k(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.STOP_TAPE_IF_48K.getId());
    if (readDWord(inputStream) != 0L) throw new IOException("Unexpected length of stop tape block");
  }

  @Override
  public int getDataLength() {
    return 0;
  }

  @Override
  public byte[] extractData() throws IOException {
    return new byte[0];
  }

  @Override
  public void write(JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeDWord(outputStream, 0L);
  }
}
