package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;

public class TzxBlockJumpTo extends AbstractTzxBlock implements FlowManagementBlock {

  private final short[] offsets;

  public TzxBlockJumpTo(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.JUMP_TO_BLOCK.getId());
    this.offsets = new short[]{(short) readWord(inputStream)};
  }

  @Override
  public short[] getOffsets() {
    return this.offsets;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.offsets[0]);
  }
}
