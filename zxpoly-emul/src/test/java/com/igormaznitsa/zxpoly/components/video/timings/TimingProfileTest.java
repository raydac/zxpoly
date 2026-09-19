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
  }

  @Test
  public void testSpectrum128IntToPaper() {
    final TimingProfile profile = TimingProfile.SPECTRUM128;
    assertEquals(70908, profile.tstatesFrame);
    assertEquals(228, profile.tstatesPerLine);
    assertEquals(14361, profile.tstatesStartScreen);
    assertEquals(TYPE_SHIFT1_AND_FETCH_B2, profile.makeUlaFrame()[14361].type);
    assertEquals(profile.tstatesStartScreen,
        this.cpuTstateOfRaster(profile, this.ulaFirstPaper(profile)));
  }

  @Test
  public void testPentagonRasterStaysAlignedWithLineStart() {
    final TimingProfile profile = TimingProfile.PENTAGON128;
    assertEquals(71680, profile.tstatesFrame);
    assertEquals(0, profile.tstatesUlaPhase);
    assertEquals(17988, profile.tstatesStartScreen);
    assertEquals(TYPE_SHIFT1_AND_FETCH_B2, profile.makeUlaFrame()[17988].type);
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

  private int ulaFirstPaper(final TimingProfile profile) {
    return profile.tstatesPerLine * profile.tstatesFirstPaperLine + profile.tstatesFirstPaperTact;
  }

  private int cpuTstateOfRaster(final TimingProfile profile, final int rasterT) {
    return Math.floorMod(rasterT - profile.tstatesUlaPhase, profile.tstatesFrame);
  }
}
