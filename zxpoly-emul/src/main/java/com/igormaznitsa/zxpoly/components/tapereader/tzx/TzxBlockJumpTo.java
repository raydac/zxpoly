package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockJumpTo extends AbstractTzxBlock implements FlowManagementBlock {

  private final short offset;

  public TzxBlockJumpTo(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.JUMP_TO_BLOCK.getId());
    this.offset = (short) readWord(inputStream);
  }

  public short getOffset() {
    return offset;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeDWord(outputStream, this.offset);
  }
}
