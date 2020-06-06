/*
 * Copyright (C) 2014-2020 Igor Maznitsa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.igormaznitsa.zxpoly.components;

import static com.igormaznitsa.zxpoly.components.VideoController.MCYCLES_PER_INT;
import static java.lang.Long.toHexString;
import static java.lang.String.format;
import static java.util.Arrays.fill;
import static javax.sound.sampled.AudioFormat.Encoding.PCM_SIGNED;


import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;

public class Beeper {

  private static final Logger LOGGER = Logger.getLogger("Beeper");
  private static final boolean LOG_RAW_SOUND = false;
  private static final int SND_FREQ = 44100;

  public static final AudioFormat AUDIO_FORMAT = new AudioFormat(
      PCM_SIGNED,
      SND_FREQ,
      16,
      2,
      4,
      SND_FREQ,
      false
  );


  private static final IBeeper NULL_BEEPER = new IBeeper() {
    @Override
    public void updateState(boolean intSignal, long machineCycleInInt, int level) {
    }

    @Override
    public float getMasterGain() {
      return -1.0f;
    }

    @Override
    public void setMasterGain(float valueInDb) {

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
  };

  private final AtomicReference<IBeeper> activeInternalBeeper = new AtomicReference<>(NULL_BEEPER);

  public Beeper() {
  }

  public void reset() {
    this.activeInternalBeeper.get().reset();
  }

  public void setSourceSoundPort(final SourceSoundPort soundPort) {
    if (soundPort == null) {
      this.activeInternalBeeper.getAndSet(NULL_BEEPER).dispose();
    } else {
      try {
        final IBeeper newInternalBeeper = new InternalBeeper(soundPort);
        if (this.activeInternalBeeper.compareAndSet(NULL_BEEPER, newInternalBeeper)) {
          newInternalBeeper.start();
        }
      } catch (IllegalArgumentException ex) {
        LOGGER.severe("Can't create beeper: " + ex.getMessage());
        this.activeInternalBeeper.getAndSet(NULL_BEEPER).dispose();
      }
    }
  }

  public boolean isNullBeeper() {
    return this.activeInternalBeeper.get() == NULL_BEEPER;
  }

  public void updateState(boolean intSignal, long machineCycleInInt, int portFeD4D3) {
    this.activeInternalBeeper.get().updateState(intSignal, machineCycleInInt, portFeD4D3);
  }

  public boolean isActive() {
    return this.activeInternalBeeper.get() != NULL_BEEPER;
  }

  public void dispose() {
    this.activeInternalBeeper.get().dispose();
  }

  public AudioFormat getAudioFormat() {
    return AUDIO_FORMAT;
  }

  private interface IBeeper {

    void start();

    float getMasterGain();

    void setMasterGain(float valueInDb);

    void updateState(boolean intSignal, long machineCycleInInt, int level);

    void dispose();

    void reset();
  }

  private static final class InternalBeeper implements IBeeper, Runnable {

    private static final int NUMBER_OF_LEVELS = 8;
    private static final int SAMPLES_PER_INT = SND_FREQ / 50;
    private static final int SND_BUFFER_LENGTH =
        SAMPLES_PER_INT * AUDIO_FORMAT.getChannels() * AUDIO_FORMAT.getSampleSizeInBits() / 8;
    private final byte[] soundBuffer = new byte[SND_BUFFER_LENGTH];
    private final BlockingQueue<byte[]> soundDataQueue = new ArrayBlockingQueue<>(2);
    private final SourceDataLine sourceDataLine;
    private final Thread thread;
    private final byte[] LEVELS = new byte[NUMBER_OF_LEVELS];
    private volatile boolean working = true;
    private final AtomicReference<FloatControl> gainControl = new AtomicReference<>();

    @Override
    public void start() {
      if (this.thread != null && this.working) {
        this.thread.start();
      }
    }

    private void initMasterGain() {
      final FloatControl gainControl = this.gainControl.get();
      if (gainControl != null) {
        gainControl.setValue(-20.0f); // 50%
      }
    }

    @Override
    public float getMasterGain() {
      final FloatControl gainControl = this.gainControl.get();
      if (gainControl == null || !this.working) {
        return -1.0f;
      } else {
        return gainControl.getValue();
      }
    }

    @Override
    public void setMasterGain(final float valueInDb) {
      final FloatControl gainControl = this.gainControl.get();
      if (gainControl != null && this.working) {
        gainControl.setValue(
            Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), valueInDb)));
      }
    }

    private InternalBeeper(final SourceSoundPort sourceSoundPort) {
      //----- init sound level table
      LEVELS[0b000] = Byte.MIN_VALUE;
      LEVELS[0b001] = Byte.MIN_VALUE + 17; // 6.5%
      LEVELS[0b010] = Byte.MIN_VALUE + 46; // 18%
      LEVELS[0b011] = Byte.MIN_VALUE + 65; // 25.4%
      LEVELS[0b100] = Byte.MIN_VALUE + 204; // 80%
      LEVELS[0b101] = Byte.MIN_VALUE + 223; // 87%
      LEVELS[0b110] = Byte.MIN_VALUE + 238; // 93%
      LEVELS[0b111] = Byte.MAX_VALUE;
      //-------------------------------

      this.sourceDataLine = sourceSoundPort.asSourceDataLine();
      final Line.Info lineInfo = this.sourceDataLine.getLineInfo();
      LOGGER.info("Got sound data line: " + lineInfo.toString());

      this.thread = new Thread(this, "beeper-thread-" + toHexString(System.nanoTime()));
      this.thread
          .setPriority(Thread.NORM_PRIORITY + 1);
      this.thread.setDaemon(true);
    }

    private void blink(final byte fillByte) {
      if (this.working) {
        this.soundDataQueue.offer(this.soundBuffer.clone());
        fill(this.soundBuffer, fillByte);
      }
    }

    @Override
    public void updateState(
        boolean wallclockIntSignal,
        long machineCyclesInInt,
        final int level
    ) {
      if (this.working) {
        final byte value = LEVELS[level];
        int position = ((int) ((machineCyclesInInt * SAMPLES_PER_INT + MCYCLES_PER_INT / 2)
            / MCYCLES_PER_INT)) * 4;

        if (wallclockIntSignal) {
          blink(value);
        }

        if (position <= SND_BUFFER_LENGTH) {
          fill(this.soundBuffer,
              position,
              SND_BUFFER_LENGTH,
              value);
        }

        if (wallclockIntSignal) {
          blink(value);
          fill(this.soundBuffer,
              0,
              SND_BUFFER_LENGTH,
              value);
        }
      }
    }

    @Override
    public void reset() {
      if (this.working) {
        LOGGER.info("Reseting");
        fill(this.soundBuffer, LEVELS[0]);
      }
    }

    private String makeFileNameForFormat(final AudioFormat format) {
      final DateFormat dataFormat = new SimpleDateFormat("hhmmss");

      return format("bpr_hz%d_ch%d_%s_%s_%s.raw",
          (int) format.getSampleRate(),
          format.getChannels(),
          format.getEncoding() == PCM_SIGNED ? "SGN" : "USGN",
          format.isBigEndian() ? "bge" : "lte",
          dataFormat.format(new Date()));
    }

    private OutputStream makeLogStream(final File folder) {
      if (!LOG_RAW_SOUND) {
        return null;
      }
      try {
        final File logFile = new File(folder, makeFileNameForFormat(AUDIO_FORMAT));
        LOGGER.info("Log beeper raw data into " + logFile);
        return new BufferedOutputStream(new FileOutputStream(logFile));
      } catch (Exception ex) {
        LOGGER.warning("Can't create log file:" + ex.getMessage());
        return null;
      }
    }

    private synchronized void flushDataIntoLine(final byte[] data) {
      final int len = Math.min(this.sourceDataLine.available(), data.length);
      this.sourceDataLine.write(data, data.length - len, len);
    }

    @Override
    public void run() {
      LOGGER.info("Starting thread");
      final OutputStream logStream = makeLogStream(new File("./"));
      try {
        this.sourceDataLine.open(AUDIO_FORMAT, SND_BUFFER_LENGTH * 5);
        if (this.sourceDataLine.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
          final FloatControl gainControl =
              (FloatControl) this.sourceDataLine.getControl(FloatControl.Type.MASTER_GAIN);
          LOGGER.info(format("Got master gain control %f..%f", gainControl.getMinimum(),
              gainControl.getMaximum()));
          this.gainControl.set(gainControl);
        } else {
          LOGGER.warning("Master gain control is not supported");
        }
        this.initMasterGain();

        LOGGER.info(format(
            "Sound line opened, buffer size is %d byte(s)",
            this.sourceDataLine.getBufferSize())
        );

        this.sourceDataLine.start();

        LOGGER.info("Sound line started");

        while (this.working && !Thread.currentThread().isInterrupted()) {
          final byte[] dataBlock = soundDataQueue.take();
          if (LOG_RAW_SOUND && logStream != null) {
            logStream.write(dataBlock);
          }
          this.flushDataIntoLine(dataBlock);
        }
        LOGGER.info("Main loop completed");
      } catch (InterruptedException ex) {
        LOGGER.info("Interruption");
        Thread.currentThread().interrupt();
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Error in sound line work: " + ex);
      } finally {
        JBBPUtils.closeQuietly(logStream);
        try {
          this.sourceDataLine.stop();
          LOGGER.info("Line stopped");
        } catch (Exception ex) {
          LOGGER.warning("Exception in source line stop: " + ex.getMessage());
        } finally {
          try {
            this.sourceDataLine.flush();
            this.sourceDataLine.close();
            LOGGER.info("Line closed");
          } catch (Exception ex) {
            LOGGER.warning("Exception in source line close: " + ex.getMessage());
          }
        }
        LOGGER.info("Thread stopped");
      }
    }

    @Override
    public void dispose() {
      if (this.thread != null && this.working) {
        this.working = false;
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
}
