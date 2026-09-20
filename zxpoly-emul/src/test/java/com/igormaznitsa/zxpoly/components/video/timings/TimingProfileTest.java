package com.igormaznitsa.zxpoly.components.video.timings;

import static com.igormaznitsa.zxpoly.components.video.timings.TimingProfile.UlaTact.TYPE_BORDER_FETCH_A1;
import static com.igormaznitsa.zxpoly.components.video.timings.TimingProfile.UlaTact.TYPE_BORDER_FETCH_B1;
import static com.igormaznitsa.zxpoly.components.video.timings.TimingProfile.UlaTact.TYPE_SHIFT1_AND_FETCH_B2;

import junit.framework.TestCase;
import org.junit.Test;

public class TimingProfileTest extends TestCase {

  @Test
  public void testSpectrum48IntToPaperAndContention() {
    final TimingProfile profile = TimingProfile.SPECTRUM48;
    assertEquals(69888, profile.tstatesFrame);
    assertEquals(224, profile.tstatesPerLine);
    assertEquals(14336, profile.tstatesStartScreen);
    assertEquals(32, profile.tstatesInt);

    final TimingProfile.UlaTact[] frame = profile.makeUlaFrame();
    assertEquals(6, frame[14335].contention);
    assertEquals(5, frame[14336].contention);
    assertEquals(TYPE_BORDER_FETCH_B1, frame[14334].type);
    assertEquals(TYPE_BORDER_FETCH_A1, frame[14335].type);
    assertEquals(TYPE_SHIFT1_AND_FETCH_B2, frame[14336].type);
    assertEquals(profile.tstatesStartScreen,
        this.cpuTstateOfRaster(profile, this.ulaFirstPaper(profile)));
    assertFalse(profile.evenM1);
  }

  @Test
  public void testSpectrum128IntToPaperAndContention() {
    final TimingProfile profile = TimingProfile.SPECTRUM128;
    assertEquals(70908, profile.tstatesFrame);
    assertEquals(228, profile.tstatesPerLine);
    assertEquals(24, profile.tstatesPerBorderLeft);
    assertEquals(24, profile.tstatesPerBorderRight);
    assertEquals(14362, profile.tstatesStartScreen);
    assertEquals(36, profile.tstatesInt);

    final TimingProfile.UlaTact[] frame = profile.makeUlaFrame();
    assertEquals(6, frame[14361].contention);
    assertEquals(5, frame[14362].contention);
    assertEquals(TYPE_BORDER_FETCH_B1, frame[14360].type);
    assertEquals(TYPE_BORDER_FETCH_A1, frame[14361].type);
    assertEquals(TYPE_SHIFT1_AND_FETCH_B2, frame[14362].type);
    assertEquals(profile.tstatesStartScreen,
        this.cpuTstateOfRaster(profile, this.ulaFirstPaper(profile)));
    assertTrue(profile.evenM1);
  }

  @Test
  public void testPentagonRasterStaysAlignedWithLineStart() {
    final TimingProfile profile = TimingProfile.PENTAGON128;
    assertEquals(71680, profile.tstatesFrame);
    assertEquals(0, profile.tstatesUlaPhase);
    assertEquals(17988, profile.tstatesStartScreen);
    assertEquals(TYPE_SHIFT1_AND_FETCH_B2, profile.makeUlaFrame()[17988].type);
    assertFalse(profile.evenM1);
  }

  @Test
  public void testSpectrum48BorderStaysPinnedToDoubledPaper() {
    final TimingProfile profile = TimingProfile.SPECTRUM48;
    final TimingProfile.BorderBlit blit = profile.alignBorderToPaper(96, 112, 512, 384);

    assertEquals(0, blit.visibleX());
    assertEquals(0, blit.visibleY());
    assertEquals(704, blit.visibleWidth());
    assertEquals(608, blit.visibleHeight());
    assertEquals(96, blit.imageX() + profile.tstatesFirstPaperTact * 4);
    assertEquals(112, blit.imageY() + profile.tstatesFirstPaperLine * 2);
  }

  @Test
  public void testSpectrum128BorderMatches48KVisibleWidth() {
    final TimingProfile profile = TimingProfile.SPECTRUM128;
    final TimingProfile.BorderBlit blit = profile.alignBorderToPaper(96, 110, 512, 384);

    assertEquals(0, blit.visibleX());
    assertEquals(0, blit.visibleY());
    assertEquals(704, blit.visibleWidth());
    assertEquals(606, blit.visibleHeight());
    assertEquals(96, blit.imageX() + profile.tstatesFirstPaperTact * 4);
    assertEquals(110, blit.imageY() + profile.tstatesFirstPaperLine * 2);
  }

  @Test
  public void testPentagonBorderUsesAsymmetricInsets() {
    final TimingProfile profile = TimingProfile.PENTAGON128;
    final TimingProfile.BorderBlit blit = profile.alignBorderToPaper(144, 128, 512, 384);

    assertEquals(0, blit.visibleX());
    assertEquals(0, blit.visibleY());
    assertEquals(768, blit.visibleWidth());
    assertEquals(608, blit.visibleHeight());
    assertEquals(144, blit.imageX() + profile.tstatesFirstPaperTact * 4);
    assertEquals(128, blit.imageY() + profile.tstatesFirstPaperLine * 2);
  }

  @Test
  public void testSpectrum48BorderRasterLagsPaperByFourLines() {
    final TimingProfile profile = TimingProfile.SPECTRUM48;
    assertEquals(4, profile.linesBorderPaintDelay);
    assertEquals(0, profile.tstatesBorderRasterShift);
    assertEquals(0, TimingProfile.SPECTRUM128.linesBorderPaintDelay);
    assertEquals(-2, TimingProfile.SPECTRUM128.tstatesBorderRasterShift);
    assertEquals(0, TimingProfile.PENTAGON128.linesBorderPaintDelay);
    assertEquals(0, TimingProfile.PENTAGON128.tstatesBorderRasterShift);
    assertEquals(
        Math.floorMod(
            profile.toRasterTstate(profile.tstatesStartScreen) + 4 * profile.tstatesPerLine,
            profile.tstatesFrame),
        profile.toBorderRasterTstate(profile.tstatesStartScreen));
  }

  @Test
  public void testSpectrum128BorderRasterLeadsPaperByTwoTstates() {
    final TimingProfile profile = TimingProfile.SPECTRUM128;
    assertEquals(
        Math.floorMod(profile.toRasterTstate(profile.tstatesStartScreen) - 2, profile.tstatesFrame),
        profile.toBorderRasterTstate(profile.tstatesStartScreen));
  }

  @Test
  public void testSpectrum48PaperContentionPattern() {
    this.assertPaperContentionPattern(TimingProfile.SPECTRUM48, 14335);
  }

  @Test
  public void testSpectrum128PaperContentionPattern() {
    this.assertPaperContentionPattern(TimingProfile.SPECTRUM128, 14361);
    final TimingProfile.UlaTact[] frame = TimingProfile.SPECTRUM128.makeUlaFrame();
    assertEquals(6, frame[14361 + 228].contention);
    assertEquals(5, frame[14362 + 228].contention);
  }

  @Test
  public void testPentagonHasNoMemoryContention() {
    final TimingProfile profile = TimingProfile.PENTAGON128;
    final TimingProfile.UlaTact[] frame = profile.makeUlaFrame();

    assertEquals(0, frame[profile.tstatesStartScreen].contention);
    assertEquals(0, profile.memoryContentionDelay(0, 0x4000, profile.tstatesStartScreen));
    assertEquals(0, profile.contendPort(0, 0x00FE, profile.tstatesStartScreen));
    assertEquals(0, profile.contendPort(1, 0x7FFE, profile.tstatesStartScreen));
  }

  @Test
  public void testContendedAddressesFollow128kPaging() {
    assertTrue(TimingProfile.isContendedAddress(0x4000, 0));
    assertTrue(TimingProfile.isContendedAddress(0x7FFF, 0));
    assertFalse(TimingProfile.isContendedAddress(0x3FFF, 1));
    assertFalse(TimingProfile.isContendedAddress(0x8000, 1));
    assertFalse(TimingProfile.isContendedAddress(0xC000, 0));
    assertTrue(TimingProfile.isContendedAddress(0xC000, 1));
    assertTrue(TimingProfile.isContendedAddress(0xFFFF, 7));
  }

  @Test
  public void testUlaPortContentionMatchesFuseEarlyLateSplit() {
    final TimingProfile profile = TimingProfile.SPECTRUM48;

    assertEquals(0, profile.memoryContentionDelay(0, 0x0000, 14335));
    assertEquals(6, profile.memoryContentionDelay(0, 0x4000, 14335));
    assertEquals(0, profile.memoryContentionDelay(0, 0xC000, 14335));
    assertEquals(6, profile.memoryContentionDelay(1, 0xC000, 14335));

    assertEquals(5, profile.contendPort(0, 0x00FE, 14335));
    assertEquals(6, profile.contendPort(0, 0x7FFE, 14335));
    assertEquals(0, profile.contendPort(0, 0x00FE, 0));
    assertTrue(profile.contendPort(0, 0x7FFD, 14335) > 0);
    assertFalse(TimingProfile.isUlaPort(0x7FFD));
  }

  private void assertPaperContentionPattern(final TimingProfile profile,
                                            final int firstContendedT) {
    final TimingProfile.UlaTact[] frame = profile.makeUlaFrame();
    final int[] expected = {6, 5, 4, 3, 2, 1, 0, 0};

    for (int i = 0; i < 128; i++) {
      assertEquals(expected[i % 8], frame[firstContendedT + i].contention);
    }

    assertEquals(0, frame[firstContendedT - 1].contention);
    assertEquals(0, frame[firstContendedT + 128].contention);
  }

  private int ulaFirstPaper(final TimingProfile profile) {
    return profile.tstatesPerLine * profile.tstatesFirstPaperLine + profile.tstatesFirstPaperTact;
  }

  private int cpuTstateOfRaster(final TimingProfile profile, final int rasterT) {
    return Math.floorMod(rasterT - profile.tstatesUlaPhase, profile.tstatesFrame);
  }
}
