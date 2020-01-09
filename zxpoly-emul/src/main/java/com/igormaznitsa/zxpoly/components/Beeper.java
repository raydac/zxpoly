package com.igormaznitsa.zxpoly.components;

import static javax.sound.sampled.AudioFormat.Encoding.PCM_UNSIGNED;


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

  private static final IBeeper NULL_BEEPER = new IBeeper() {
    @Override
    public void updateState(boolean intSignal, long machineCycleInInt, int portFed4d3) {
    }

    @Override
    public void dispose() {
    }

    @Override
    public void reset() {
    }

    @Override
    public void start() {

    }

    @Override
    public void pause() {

    }

    @Override
    public void resume() {

    }
  };

  private final AtomicReference<IBeeper> activeInternalBeeper = new AtomicReference<>(NULL_BEEPER);

  public Beeper() {

  }

  public void reset() {
    this.activeInternalBeeper.get().reset();
  }

  public void setEnable(final boolean flag) {
    if (flag && this.activeInternalBeeper.get() == NULL_BEEPER) {
      try {
        final IBeeper newInternalBeeper = new InternalBeeper();
        if (this.activeInternalBeeper.compareAndSet(NULL_BEEPER, newInternalBeeper)) {
          newInternalBeeper.start();
        }
      } catch (LineUnavailableException ex) {
        LOGGER.severe("Can't create beeper: " + ex.getMessage());
      }
    } else {
      this.activeInternalBeeper.getAndSet(NULL_BEEPER).dispose();
    }
  }

  public void resume() {
    this.activeInternalBeeper.get().resume();
  }

  public void pause() {
    this.activeInternalBeeper.get().pause();
  }

  public void updateState(boolean intSignal, long machineCycleInInt, int portFeD4D3) {
    this.activeInternalBeeper.get().updateState(intSignal, machineCycleInInt, portFeD4D3);
  }

  public boolean isActive() {
    return this.activeInternalBeeper.get() != NULL_BEEPER;
  }

  private interface IBeeper {
    void start();

    void pause();

    void resume();

    void updateState(boolean intSignal, long machineCycleInInt, int portFeD4D3);

    void dispose();

    void reset();
  }

  private static final class InternalBeeper implements IBeeper, Runnable {
    private static final int SND_FREQ = 44100;
    private static final int NUM_OF_BUFFERS = 5;
    private static final AudioFormat AUDIO_FORMAT = new AudioFormat(
        PCM_UNSIGNED,
        SND_FREQ,
        8,
        1,
        1,
        SND_FREQ,
        true
    );
    private static final int SAMPLES_IN_INT = SND_FREQ / 50;
    private final byte[][] soundBuffers = new byte[NUM_OF_BUFFERS][SAMPLES_IN_INT];
    private final Exchanger<byte[]> exchanger = new Exchanger<>();
    private final SourceDataLine sourceDataLine;
    private final Thread thread;
    private int activeBufferIndex = 0;
    private volatile boolean active = true;

    private InternalBeeper() throws LineUnavailableException {
      for (byte[] b : this.soundBuffers) {
        Arrays.fill(b, SND_LEVEL0);
      }
      this.sourceDataLine = (SourceDataLine) AudioSystem.getLine(new DataLine.Info(SourceDataLine.class, AUDIO_FORMAT));
      this.thread = new Thread(this, "beeper-thread-" + System.nanoTime());
      this.thread.setDaemon(true);
    }

    @Override
    public void start() {
      this.thread.start();
    }

    @Override
    public void pause() {
      LOGGER.info("Paused");
    }

    @Override
    public void resume() {
      LOGGER.info("Resumed");
    }

    void blink() {
      final byte[] currentBuffer = this.soundBuffers[this.activeBufferIndex++];
      if (this.activeBufferIndex > NUM_OF_BUFFERS) {
        this.activeBufferIndex = 0;
      }
      try {
        exchanger.exchange(currentBuffer);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }

    @Override
    public void updateState(
        final boolean intSignal,
        final long machineCycleInInt,
        final int portFeD4D3
    ) {
    }

    @Override
    public void reset() {
      LOGGER.info("Reseting");
      for (byte[] b : this.soundBuffers) {
        Arrays.fill(b, SND_LEVEL0);
      }
    }

    @Override
    public void run() {
      LOGGER.info("Starting thread");
      try {
        this.sourceDataLine.start();
        LOGGER.info("Sound line started");
        while (this.active && !Thread.currentThread().isInterrupted()) {
          final byte[] buffer = exchanger.exchange(null);
          if (buffer != null) {
            this.sourceDataLine.write(buffer, 0, buffer.length);
          }
        }
        LOGGER.info("Main loop completed");
      } catch (InterruptedException ex) {
        LOGGER.info("Interruption");
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Error in sound line work: " + ex);
      } finally {
        try {
          this.sourceDataLine.stop();
        } catch (Exception ex) {
          LOGGER.warning("Exception in source line stop: " + ex.getMessage());
        } finally {
          try {
            this.sourceDataLine.close();
          } catch (Exception ex) {
            LOGGER.warning("Exception in source line close: " + ex.getMessage());
          }
        }
        LOGGER.info("Thread stopped");
      }
    }

    @Override
    public void dispose() {
      LOGGER.info("Disposing");
      this.thread.interrupt();
      try {
        this.thread.join();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }
}
