package com.igormaznitsa.zxpoly.streamer;

import com.igormaznitsa.jbbp.io.JBBPBitOutputStream;
import junit.framework.TestCase;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.OptionalInt;

@SuppressWarnings("XorPower")
public class WebSocketStreamWrapperTest extends TestCase {
  @Test
  public void testWrite_Small_NoMask() throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream stream = new JBBPBitOutputStream(buffer);

    HttpProcessor.WebSocketStreamWrapper.writeWebSocketFrame(stream, 2, OptionalInt.empty(), new byte[]{1, 2, 3});
    stream.close();
    final byte[] frame = buffer.toByteArray();

    Assert.assertEquals(5, frame.length);

    Assert.assertEquals(0b1_000_0010, frame[0] & 0xFF);
    Assert.assertEquals(0b0_0000011, frame[1] & 0xFF);
    Assert.assertEquals(1, frame[2] & 0xFF);
    Assert.assertEquals(2, frame[3] & 0xFF);
    Assert.assertEquals(3, frame[4] & 0xFF);
  }

  @Test
  public void testWrite_Small_Mask() throws IOException {
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    final JBBPBitOutputStream stream = new JBBPBitOutputStream(buffer);

    HttpProcessor.WebSocketStreamWrapper.writeWebSocketFrame(stream, 2, OptionalInt.of(0x06070809), new byte[]{1, 2, 3});
    stream.close();
    final byte[] frame = buffer.toByteArray();

    Assert.assertEquals(9, frame.length);

    Assert.assertEquals(0b1_000_0010, frame[0] & 0xFF);
    Assert.assertEquals(0b1_0000011, frame[1] & 0xFF);

    Assert.assertEquals(6, frame[2] & 0xFF);
    Assert.assertEquals(7, frame[3] & 0xFF);
    Assert.assertEquals(8, frame[4] & 0xFF);
    Assert.assertEquals(9, frame[5] & 0xFF);

    Assert.assertEquals(1 ^ 6, frame[6] & 0xFF);
    Assert.assertEquals(2 ^ 7, frame[7] & 0xFF);
    Assert.assertEquals(3 ^ 8, frame[8] & 0xFF);
  }
}