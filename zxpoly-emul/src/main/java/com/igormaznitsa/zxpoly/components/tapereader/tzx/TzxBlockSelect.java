package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TzxBlockSelect extends AbstractTzxBlock {

  private final int blockLength;
  private final Selection[] selections;

  public TzxBlockSelect(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlock.SELECT_BLOCK.getId());
    this.blockLength = readWord(inputStream);
    final int numberPfSelections = inputStream.readByte();
    this.selections = new Selection[numberPfSelections];
    for (int i = 0; i < this.selections.length; i++) {
      this.selections[i] = new Selection(inputStream);
    }
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.blockLength);
    outputStream.write(this.selections.length);
    for (final Selection s : this.selections) {
      s.write(outputStream);
    }
  }

  public Selection[] getSelections() {
    return selections;
  }

  public static final class Selection {
    private final short offset;
    private final String description;

    private Selection(final JBBPBitInputStream inputStream) throws IOException {
      this.offset = (short) readWord(inputStream);
      final int length = inputStream.readByte();
      this.description = new String(inputStream.readByteArray(length), StandardCharsets.ISO_8859_1);
    }

    public void write(final JBBPBitOutputStream outputStream) throws IOException {
      writeWord(outputStream, this.offset);
      final byte[] chars = description.getBytes(StandardCharsets.ISO_8859_1);
      outputStream.write(chars.length);
      outputStream.write(chars);
    }
  }


}
