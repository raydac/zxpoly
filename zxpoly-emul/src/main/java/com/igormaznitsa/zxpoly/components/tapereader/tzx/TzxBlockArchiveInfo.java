package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class TzxBlockArchiveInfo extends AbstractTzxInformationBlock {

  private final int blockLength;
  private final Text[] texts;

  public TzxBlockArchiveInfo(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.ARCHIVE_INFO.getId());
    this.blockLength = readWord(inputStream);
    final int textItems = inputStream.readByte();
    this.texts = new Text[textItems];
    for (int i = 0; i < textItems; i++) {
      this.texts[i] = new Text(inputStream);
    }
  }

  public int getBlockLength() {
    return blockLength;
  }

  public Text[] getTexts() {
    return texts;
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeWord(outputStream, this.blockLength);
    outputStream.write(this.texts.length);
    for (final Text t : this.texts) {
      t.write(outputStream);
    }
  }

  public static final class Text {

    private final int type;
    private final String text;

    private Text(final JBBPBitInputStream inputStream) throws IOException {
      this.type = inputStream.readByte();
      final int chars = inputStream.readByte();
      this.text = new String(inputStream.readByteArray(chars), StandardCharsets.ISO_8859_1);
    }

    public void write(final JBBPBitOutputStream outputStream) throws IOException {
      outputStream.write(this.type);
      final byte[] chars = this.text.getBytes(StandardCharsets.ISO_8859_1);
      outputStream.write(chars.length);
      outputStream.write(chars);
    }

    public enum Type {
      FULL_TITLE(0x00),
      PUBLISHER(0x01),
      AUTHOR(0x02),
      YEAR_OF_PUBLICATION(0x03),
      LANGUAGE(0x04),
      GAME_TYPE(0x05),
      PRICE(0x06),
      PROTECTION(0x07),
      ORIGIN(0x08),
      COMMENTS(0xFF),
      UNKNOWN(-1);

      private final int id;

      Type(final int id) {
        this.id = id;
      }

      public static Type findForId(final int id) {
        Type result = UNKNOWN;
        for (final Type t : Type.values()) {
          if (t.id == id) {
            result = t;
            break;
          }
        }
        return result;
      }

      public int getId() {
        return this.id;
      }
    }
  }


}
