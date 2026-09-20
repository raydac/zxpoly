package com.igormaznitsa.zxpoly.components.sound;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import org.junit.Test;

public class BeeperWavWriterTest {

  @Test
  public void testStartKeepsRecordingAssignedWhileFileDialogWouldBeOpen() throws IOException {
    final Beeper beeper = this.newBeeper();
    final File wavFile = this.newTempWavFile();

    try {
      beeper.suspendWavWriter();
      beeper.replaceSuspendedWriter(beeper.makeTargetWavWriter(wavFile));

      assertTrue(beeper.hasActiveWavFile());

      beeper.resumeWavWriter();

      assertTrue(beeper.hasActiveWavFile());
      beeper.updateState(false, false, TimingProfile.SPECTRUM48.tstatesFrame);
    } finally {
      beeper.dispose();
    }

    assertTrue(wavFile.length() > 0L);
  }

  @Test
  public void testStopClearsRecordingAfterResume() throws IOException {
    final Beeper beeper = this.newBeeper();
    final File wavFile = this.newTempWavFile();

    try {
      beeper.suspendWavWriter();
      beeper.replaceSuspendedWriter(beeper.makeTargetWavWriter(wavFile));
      beeper.resumeWavWriter();

      beeper.suspendWavWriter();
      beeper.stopSuspendedWavWriter();

      assertFalse(beeper.hasActiveWavFile());

      beeper.resumeWavWriter();

      assertFalse(beeper.hasActiveWavFile());
    } finally {
      beeper.dispose();
    }
  }

  @Test
  public void testCancelLeavesRecordingOff() {
    final Beeper beeper = this.newBeeper();

    try {
      beeper.suspendWavWriter();
      beeper.resumeWavWriter();

      assertFalse(beeper.hasActiveWavFile());
    } finally {
      beeper.dispose();
    }
  }

  private Beeper newBeeper() {
    return new Beeper(
        TimingProfile.SPECTRUM48,
        SoundChannelLowPassFilter.OFF,
        false,
        false,
        false,
        false);
  }

  private File newTempWavFile() throws IOException {
    final File wavFile = Files.createTempFile("zxpoly-beeper-", ".wav").toFile();
    wavFile.deleteOnExit();
    return wavFile;
  }
}
