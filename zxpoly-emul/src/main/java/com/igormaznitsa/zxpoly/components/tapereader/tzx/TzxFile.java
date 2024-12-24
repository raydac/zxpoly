package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

/**
 * Reader fpr TZX 1.20
 * based on http://k1.spdns.de/Develop/Projects/zasm/Info/TZX%20format.html#STDSPEED
 */
public class TzxFile {

  private final int revisionMajor;
  private final int revisionMinor;

  private final List<AbstractTzxBlock> blockList;

  public TzxFile(final InputStream inputStream) throws IOException {
    final JBBPBitInputStream jbbpBitInputStream = new JBBPBitInputStream(inputStream, false);
    final String signature = new String(jbbpBitInputStream.readByteArray(7), StandardCharsets.ISO_8859_1);
    if (!signature.equals("ZXTape!")) throw new IOException("TZX signature error: " + signature);
    if (jbbpBitInputStream.readByte() != 0x1A) throw new IOException("TZX error end of text file marker");
    this.revisionMajor = jbbpBitInputStream.readByte();
    this.revisionMinor = jbbpBitInputStream.readByte();

    final List<AbstractTzxBlock> blocks = new ArrayList<>();
    while (jbbpBitInputStream.hasAvailableData()) {
      blocks.add(TzxBlockId.readNextBlock(jbbpBitInputStream));
    }
    this.blockList = Collections.unmodifiableList(blocks);
  }

  public Stream<AbstractTzxBlock> stream() {
    return this.blockList.stream();
  }

  public List<AbstractTzxBlock> getBlockList() {
    return this.blockList;
  }

  public int getRevisionMajor() {
    return this.revisionMajor;
  }

  public int getRevisionMinor() {
    return this.revisionMinor;
  }

  public void write(final OutputStream outputStream) throws IOException {
    final JBBPBitOutputStream jbbpBitOutputStream = new JBBPBitOutputStream(outputStream);

    jbbpBitOutputStream.write("ZXTape!".getBytes(StandardCharsets.ISO_8859_1));
    jbbpBitOutputStream.write(0x1A);
    jbbpBitOutputStream.write(this.revisionMajor);
    jbbpBitOutputStream.write(this.revisionMinor);

    for (final AbstractTzxBlock block : this.blockList) {
      block.write(jbbpBitOutputStream);
    }
    jbbpBitOutputStream.flush();
  }
}
