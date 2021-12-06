package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockStopTapeIf48k extends AbstractTzxBlock implements FlowManagementBlock {

  public TzxBlockStopTapeIf48k(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.STOP_TAPE_IF_48K.getId());
    if (readDWord(inputStream) != 0L) throw new IOException("Unexpected length of stop tape block");
  }

  @Override
  public void write(JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeDWord(outputStream, 0L);
  }
}
