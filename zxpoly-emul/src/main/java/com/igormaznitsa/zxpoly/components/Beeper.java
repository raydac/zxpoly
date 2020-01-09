package com.igormaznitsa.zxpoly.components;

import static com.igormaznitsa.zxpoly.components.VideoController.CYCLES_BETWEEN_INT;


import java.util.Arrays;
import java.util.concurrent.Exchanger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;

public class Beeper {

  private static final Logger LOGGER = Logger.getLogger("Beeper");

  private static final byte SND_LEVEL0 = 0; // 00
  private static final byte SND_LEVEL1 = 32; // 01
  private static final byte SND_LEVEL2 = 100; // 10
  private static final byte SND_LEVEL3 = 127; // 11
  private final AtomicReference<InternalBeeper> activeInternalBeeper = new AtomicReference<>();

  public Beeper() {

  }

  public void reset() {
    final InternalBeeper activeBeeper = this.activeInternalBeeper.get();
    if (activeBeeper != null) {
      activeBeeper.reset();
    }
  }

  public void sendSoundBuffer() {
    InternalBeeper beeper = this.activeInternalBeeper.get();
    if (beeper != null) {
      beeper.blink();
    }
  }

  public void doLevel(final long machineCycles, final int d4d3) {
    InternalBeeper beeper = this.activeInternalBeeper.get();
    if (beeper != null) {
      switch (d4d3) {
        case 0:
          beeper.set(machineCycles, SND_LEVEL0);
          break;
        case 1:
          beeper.set(machineCycles, SND_LEVEL1);
          break;
        case 2:
          beeper.set(machineCycles, SND_LEVEL2);
          break;
        default:
          beeper.set(machineCycles, SND_LEVEL3);
          break;
      }
    }
  }

  public void setEnable(final boolean flag) {
    if (flag) {
      final InternalBeeper newInternalBeeper = new InternalBeeper();
      if (this.activeInternalBeeper.compareAndSet(null, newInternalBeeper)) {
        final Thread newSoundThread = new Thread(newInternalBeeper, "zxpoly-beeper-thread");
        newSoundThread.setDaemon(true);
        newSoundThread.start();
      }
    } else {
      final InternalBeeper activeBeeper = this.activeInternalBeeper.getAndSet(null);
      if (activeBeeper != null) {
        activeBeeper.dispose();
      }
    }
  }

  private static final class InternalBeeper implements Runnable {
    private static final int SND_FREQ = 22050;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(SND_FREQ, 8, 1, false, true);
    private static final int SAMPLES_IN_INT = SND_FREQ / 50;
    private final byte[] SND_BUFFER1 = new byte[SAMPLES_IN_INT];
    private final byte[] SND_BUFFER2 = new byte[SAMPLES_IN_INT];
    private final Exchanger<byte[]> exchanger = new Exchanger<>();
    private volatile byte[] currentSndBuffer = SND_BUFFER1;
    private volatile boolean active = true;
    private int lastDataPosition;
    private byte lastLevel;

    private InternalBeeper() {
      Arrays.fill(SND_BUFFER1, SND_LEVEL0);
      Arrays.fill(SND_BUFFER2, SND_LEVEL0);
    }

    private SourceDataLine findDataLine() throws LineUnavailableException {
      DataLine.Info infoDataLine = new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT);
      return (SourceDataLine) AudioSystem.getLine(infoDataLine);
    }

    private void blink() {
      final byte[] buffer = this.currentSndBuffer;
      if (buffer == SND_BUFFER1) {
        this.currentSndBuffer = SND_BUFFER2;
      } else {
        this.currentSndBuffer = SND_BUFFER1;
      }
      Arrays.fill(this.currentSndBuffer, this.lastLevel);
      this.lastDataPosition = 0;
      try {
        exchanger.exchange(buffer);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }

    private void set(long machineCycleInInt, final byte level) {
      if (machineCycleInInt > CYCLES_BETWEEN_INT) {
        blink();
        machineCycleInInt -= CYCLES_BETWEEN_INT;
      }
      final byte[] data = this.currentSndBuffer;

      final int position = (int) ((machineCycleInInt * (SAMPLES_IN_INT - 1) + (SAMPLES_IN_INT >> 1)) / CYCLES_BETWEEN_INT);
      int lastPos = this.lastDataPosition;

      if (position < lastPos) {
        lastPos = 0;
      }
      Arrays.fill(data, lastPos, position, this.lastLevel);
      Arrays.fill(data, position, SAMPLES_IN_INT - 1, level);
      this.lastDataPosition = position;
      this.lastLevel = level;
    }

    private void reset() {
      Arrays.fill(SND_BUFFER1, SND_LEVEL0);
      Arrays.fill(SND_BUFFER2, SND_LEVEL1);
      this.currentSndBuffer = SND_BUFFER1;
      this.lastLevel = SND_LEVEL0;
      this.lastDataPosition = 0;
    }

    @Override
    public void run() {
      LOGGER.info("Starting");
      try (SourceDataLine soundLine = findDataLine()) {
        soundLine.open(AUDIO_FORMAT, SAMPLES_IN_INT * 50);
        soundLine.start();

        LOGGER.info("Sound line started");
        while (active && !Thread.currentThread().isInterrupted()) {
          final byte[] buffer = exchanger.exchange(null);
          soundLine.write(buffer, 0, buffer.length);
        }
        LOGGER.info("Main loop completed");
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Error in sound line work: " + ex.getMessage());
      } finally {
        LOGGER.info("Stopping");
      }
    }

    void dispose() {
      this.active = false;
      LOGGER.info("Disposing");
    }
  }
}
