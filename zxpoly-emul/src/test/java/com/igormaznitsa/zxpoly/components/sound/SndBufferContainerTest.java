package com.igormaznitsa.zxpoly.components.sound;

import static org.junit.Assert.assertEquals;

import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import org.junit.Test;

public class SndBufferContainerTest {

  @Test
  public void testSetValueIgnoresFrameSizedDeltaWithoutOverflow() {
    final SndBufferContainer buffer = new SndBufferContainer(TimingProfile.SPECTRUM128);

    buffer.setValue(TimingProfile.SPECTRUM128.tstatesFrame, 100, 100);
    buffer.setValue(TimingProfile.SPECTRUM128.tstatesFrame, 100, 100);
    buffer.setValue(Integer.MAX_VALUE / 2, 100, 100);

    final byte[] frame = buffer.nextBuffer(0, 0);
    assertEquals(SndBufferContainer.SND_BUFFER_SIZE, frame.length);
  }
}
