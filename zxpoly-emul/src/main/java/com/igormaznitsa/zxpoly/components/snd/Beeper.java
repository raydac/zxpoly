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

package com.igormaznitsa.zxpoly.components.snd;

import static com.igormaznitsa.zxpoly.components.video.VideoController.MCYCLES_PER_INT;
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
import java.util.Arrays;
import java.util.Date;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;

public final class Beeper {

  private static final Logger LOGGER = Logger.getLogger("Beeper");
  private static final boolean LOG_RAW_SOUND = false;
  private static final int SND_FREQ = 44100;

  public static final int AMPLITUDE_MAX = 255;
  public static final int AMPLITUDE_MIN = 0;

  public static final int CHANNEL_BEEPER = 0;
  public static final int CHANNEL_COVOX = 1;
  public static final int CHANNEL_AY_A = 2;
  public static final int CHANNEL_AY_B = 3;
  public static final int CHANNEL_AY_C = 4;
  public static final int CHANNEL_RESERV_0 = 5;
  public static final int CHANNEL_RESERV_1 = 6;
  public static final int CHANNEL_RESERV_2 = 7;

  public static final int[] BEEPER_LEVELS;

  static {
    BEEPER_LEVELS =
        Arrays.stream(new double[] {0.0d, 0.065d, 0.18d, 0.254d, 0.80d, 0.87d, 0.93d, 1.0d})
            .mapToInt(d -> Math.min(255, (int) Math.round(d * AMPLITUDE_MAX))).toArray();
  }

  private final AtomicLong channels = new AtomicLong(0L);

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
    this.clearChannels();
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

  public void setChannelValue(final int channel, final int level256) {
    long newValue;
    long oldValue;
    do {
      oldValue = this.channels.get();
      newValue =
          (oldValue & ~(0xFFL << (8 * channel))) | ((long) (level256 & 0XFF) << (8 * channel));
    } while (!this.channels.compareAndSet(oldValue, newValue));
  }

  private int mixChannelsAsSignedByte() {
    long value = this.channels.get();
    int mixed = 0;
    while (value != 0) {
      mixed += (int) value & 0xFF;
      value >>>= 8;
    }
    return (mixed << 5) - 32768;
  }

  public void updateState(boolean intSignal, long machineCycleInInt) {
    this.activeInternalBeeper.get()
        .updateState(intSignal, machineCycleInInt, this.mixChannelsAsSignedByte());
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

  public void clearChannels() {
    this.channels.set(0);
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

    private static final int SAMPLES_PER_INT = SND_FREQ / 50;
    private static final int SND_BUFFER_LENGTH =
        SAMPLES_PER_INT * AUDIO_FORMAT.getChannels() * AUDIO_FORMAT.getSampleSizeInBits() / 8;
    private final byte[] soundBuffer = new byte[SND_BUFFER_LENGTH];
    private final BlockingQueue<byte[]> soundDataQueue = new ArrayBlockingQueue<>(5);
    private final SourceDataLine sourceDataLine;
    private final Thread thread;
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
        gainControl.setValue(-10.0f);
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
      this.sourceDataLine = sourceSoundPort.asSourceDataLine();
      final Line.Info lineInfo = this.sourceDataLine.getLineInfo();
      LOGGER.info("Got sound data line: " + lineInfo.toString());

      this.thread = new Thread(this, "zxp-beeper-thread-" + toHexString(System.nanoTime()));
      this.thread.setDaemon(true);
    }

    private static void fillSndBuffer(final byte[] sndBuffer, int fromIndex,
                                      final int value) {
      if (value == 0) {
        fill(sndBuffer, fromIndex, SND_BUFFER_LENGTH, (byte) 0);
      } else {
        final byte low = (byte) value;
        final byte high = (byte) (value >> 8);

        while (fromIndex < SND_BUFFER_LENGTH) {
          sndBuffer[fromIndex++] = low;
          sndBuffer[fromIndex++] = high;
        }
      }

    }

    private static void fillSndBuffer(final byte[] array, final int value) {
      if (value == 0) {
        fill(array, (byte) 0);
      } else {
        final byte low = (byte) value;
        final byte high = (byte) (value >> 8);
        int index = array.length;
        while (index > 0) {
          array[--index] = high;
          array[--index] = low;
        }
      }
    }

    private void blink(final int value) {
      if (this.working) {
        this.soundDataQueue.offer(this.soundBuffer.clone());
        fillSndBuffer(this.soundBuffer, value);
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

    @Override
    public void updateState(
        boolean wallclockIntSignal,
        long machineCyclesInInt,
        final int level
    ) {
      if (this.working) {
        int position = ((int) ((machineCyclesInInt * SAMPLES_PER_INT + MCYCLES_PER_INT / 2)
            / MCYCLES_PER_INT)) * 4;

        if (wallclockIntSignal) {
          blink(level);
          fillSndBuffer(this.soundBuffer, level);
        }

        if (position <= SND_BUFFER_LENGTH) {
          fillSndBuffer(this.soundBuffer,
              position,
              level);
        }
      }
    }

    @Override
    public void reset() {
      if (this.working) {
        LOGGER.info("Reset");
        fill(this.soundBuffer, (byte) AMPLITUDE_MIN);
      }
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
