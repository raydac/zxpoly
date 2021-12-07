package com.igormaznitsa.zxpoly.components.tapereader.tzx;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.zip.DataFormatException;
import java.util.zip.Inflater;

public class TzxBlockCSWRecording extends AbstractTzxBlock implements DataBlock {

  public static final int COMPRESSION_RLE = 1;
  public static final int COMPRESSION_ZRLE = 2;
  private final long blockLength;
  private final int pauseAfterBlockMs;
  private final int samplingRate;
  private final int compressionType;
  private final long numberStoredPulses;
  private final byte[] data;

  public TzxBlockCSWRecording(final JBBPBitInputStream inputStream) throws IOException {
    super(TzxBlockId.CSW_RECORDING_BLOCK.getId());

    this.blockLength = readDWord(inputStream);
    this.pauseAfterBlockMs = readWord(inputStream);
    this.samplingRate = readThreeByteValue(inputStream);
    this.compressionType = inputStream.readByte();
    this.numberStoredPulses = readDWord(inputStream);
    this.data = inputStream.readByteArray((int) (blockLength - 10));
  }

  public byte[] decompressData() throws IOException {
    switch (this.compressionType) {
      case COMPRESSION_RLE:
        return this.decompressRle();
      case COMPRESSION_ZRLE:
        return this.decompressZRle();
      default:
        throw new IllegalStateException("Unsupported compression: " + this.compressionType);
    }
  }

  private byte[] decompressRle() {
    final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
    int pos = 0;
    while (pos < this.data.length) {
      final int count = this.data[pos++] & 0xFF;
      if (count == 0xC1) {
        outputStream.write(count);
      } else if (count <= 0xC0) {
        outputStream.write(count);
      } else {
        final int next = this.data[pos++] & 0xFF;
        for (int i = 0; i < (count - 0xC0); i++) {
          outputStream.write(next);
        }
      }
    }
    return outputStream.toByteArray();
  }

  private byte[] decompressZRle() throws IOException {
    final Inflater decompressor = new Inflater(false);
    decompressor.setInput(this.data);
    final byte[] buffer = new byte[(int) this.numberStoredPulses * 2];
    try {
      final int length = decompressor.inflate(buffer);
      decompressor.end();
      return Arrays.copyOf(buffer, length);
    } catch (DataFormatException ex) {
      throw new IOException("Data format exception", ex);
    }
  }

  @Override
  public void write(final JBBPBitOutputStream outputStream) throws IOException {
    super.write(outputStream);
    writeDWord(outputStream, this.blockLength);
    writeWord(outputStream, this.pauseAfterBlockMs);
    writeThreeByteValue(outputStream, this.samplingRate);
    outputStream.write(this.compressionType);
    writeDWord(outputStream, this.numberStoredPulses);
    outputStream.write(this.data);
  }

  public long getBlockLength() {
    return blockLength;
  }

  public int getPauseAfterBlockMs() {
    return pauseAfterBlockMs;
  }

  public int getSamplingRate() {
    return samplingRate;
  }

  public int getCompressionType() {
    return compressionType;
  }

  public long getNumberStoredPulses() {
    return numberStoredPulses;
  }

  @Override
  public int getDataLength() {
    return this.data.length;
  }
}
