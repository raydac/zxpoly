package com.igormaznitsa.zxpoly.components.video;

import static org.junit.Assert.assertArrayEquals;

import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import org.junit.Test;

public class VideoControllerBorderFillTest {

  @Test
  public void testFillRotatedMatchesPerTactBorderWrites() {
    for (final TimingProfile profile : TimingProfile.values()) {
      this.assertSpanMatchesPerTact(profile, 0, profile.tstatesFrame, 0xFF112233);
      this.assertSpanMatchesPerTact(profile, 1_000, 1_000 + profile.tstatesPerLine, 0xFF00FF00);
      this.assertSpanMatchesPerTact(profile, profile.tstatesFrame - 80, profile.tstatesFrame,
          0xFFFF0000);
    }
  }

  @Test
  public void testFillRotatedIgnoresEmptySpan() {
    final int[] data = new int[] {7, 7, 7};

    VideoController.fillRotatedBorder(data, data.length, 1, 0, 1);

    assertArrayEquals(new int[] {7, 7, 7}, data);
  }

  private void assertSpanMatchesPerTact(
      final TimingProfile profile,
      final int from,
      final int to,
      final int color) {
    final int frame = profile.tstatesFrame;
    final int[] expected = new int[frame];
    final int[] actual = new int[frame];

    for (int cpuT = from; cpuT < to; cpuT++) {
      expected[profile.toBorderRasterTstate(cpuT)] = color;
    }

    VideoController.fillRotatedBorder(
        actual,
        frame,
        profile.toBorderRasterTstate(from),
        to - from,
        color);

    assertArrayEquals(profile.name(), expected, actual);
  }
}
