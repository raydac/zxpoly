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

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import static java.lang.Long.toHexString;
import static java.lang.String.format;

public final class Beeper {

  public static final int AMPLITUDE_MAX = 255;
  public static final int CHANNEL_BEEPER = 0;
  public static final int CHANNEL_COVOX = 1;
  public static final int CHANNEL_AY_A = 2;
  public static final int CHANNEL_AY_B = 3;
  public static final int CHANNEL_AY_C = 4;
  public static final int CHANNEL_RESERV_0 = 5;
  public static final int CHANNEL_RESERV_1 = 6;
  public static final int CHANNEL_RESERV_2 = 7;
  public static final int[] BEEPER_LEVELS;
  public static final AudioFormat AUDIO_FORMAT = SndBufferContainer.AUDIO_FORMAT;
  private static final Logger LOGGER = Logger.getLogger("Beeper");
  private static final IBeeper NULL_BEEPER = new IBeeper() {
    @Override
    public void updateState(boolean tstatesInt, boolean wallclockInt, int spentTstates, int level) {
    }

    @Override
    public Optional<SourceSoundPort> getSoundPort() {
      return Optional.empty();
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

  static {
    BEEPER_LEVELS =
            Arrays.stream(new double[]{0.0d, 0.065d, 0.18d, 0.254d, 0.80d, 0.87d, 0.93d, 1.0d})
                    .mapToInt(d -> Math.min(AMPLITUDE_MAX, (int) Math.round(d * AMPLITUDE_MAX))).toArray();
  }

  private final AtomicReference<IBeeper> activeInternalBeeper = new AtomicReference<>(NULL_BEEPER);
  private final SoundChannelValueFilter[] soundChannelFilters = IntStream.range(0, 8).mapToObj(i -> new SoundChannelValueFilter()).toArray(SoundChannelValueFilter[]::new);
  private long channels = 0L;

  public Beeper() {
  }

  public Optional<SourceSoundPort> setSourceSoundPort(final SourceSoundPort soundPort) {
    final IBeeper prevBeeper = this.activeInternalBeeper.get();
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
    return prevBeeper.getSoundPort();
  }

  public boolean isNullBeeper() {
    return this.activeInternalBeeper.get() == NULL_BEEPER;
  }

  public void setChannelValue(final int channel, final int level256) {
    this.channels =
            (this.channels & ~(0xFFL << (8 * channel))) | ((long) (level256 & 0XFF) << (8 * channel));
  }

  public void reset() {
    this.clearChannels();
    this.activeInternalBeeper.get().reset();
  }

  private int mixChannelsAsSignedShort(final int spentTstates) {
    long value = this.channels;
    int mixed = 0;
    int channelIndex = 0;
    while (value != 0L) {
      final int channelValue = this.soundChannelFilters[channelIndex++].update(spentTstates, (((int) value) & 0xFF) << 5);
      value >>>= 8;
      mixed += channelValue;
    }
    return mixed - 32768;
  }

  public void updateState(final boolean tstatesInt, final boolean wallclockInt,
                          final int spentTstates) {
    this.activeInternalBeeper.get()
            .updateState(tstatesInt, wallclockInt, spentTstates, this.mixChannelsAsSignedShort(spentTstates));
  }

  public boolean isActive() {
    return this.activeInternalBeeper.get() != NULL_BEEPER;
  }

  public void dispose() {
    this.activeInternalBeeper.get().dispose();
  }

  public AudioFormat getAudioFormat() {
    return SndBufferContainer.AUDIO_FORMAT;
  }

  public void clearChannels() {
    this.channels = 0L;
    for (final SoundChannelValueFilter f : this.soundChannelFilters) f.reset();
  }


  private interface IBeeper {

    Optional<SourceSoundPort> getSoundPort();

    void start();

    void updateState(boolean tstatesInt, boolean wallclockInt, int spentTstates, int level);

    void dispose();

    void reset();
  }

  private static final class InternalBeeper implements IBeeper, Runnable {

    private final BlockingQueue<byte[]> soundDataQueue =
            new ArrayBlockingQueue<>(SndBufferContainer.BUFFERS_NUMBER);
    private final SourceDataLine sourceDataLine;
    private final Thread thread;
    private final SndBufferContainer sndBuffer = new SndBufferContainer();
    private final Optional<SourceSoundPort> sourceSoundPort;
    private volatile boolean working = true;

    private InternalBeeper(final SourceSoundPort sourceSoundPort) {
      this.sourceSoundPort = Optional.of(sourceSoundPort);
      this.sourceDataLine = sourceSoundPort.asSourceDataLine();
      final Line.Info lineInfo = this.sourceDataLine.getLineInfo();
      LOGGER.info("Got sound data line: " + lineInfo.toString());

      this.thread = new Thread(this, "zxp-beeper-thread-" + toHexString(System.nanoTime()));
      this.thread.setPriority(Thread.NORM_PRIORITY + 2);
      this.thread.setDaemon(true);
    }

    @Override
    public Optional<SourceSoundPort> getSoundPort() {
      return this.sourceSoundPort;
    }

    @Override
    public void start() {
      if (this.thread != null && this.working) {
        this.thread.start();
      }
    }

    @Override
    public void updateState(
            boolean tstatesIntReached,
            boolean wallclockInt,
            int spentTstates,
            final int level
    ) {
      if (this.working) {
        if (wallclockInt) {
          this.soundDataQueue.offer(sndBuffer.nextBuffer(level));
          sndBuffer.resetPosition();
        } else {
          sndBuffer.setValue(spentTstates, level);
        }
      }
    }

    private void writeToLine(final byte[] data) {
      this.sourceDataLine.write(data, 0, Math.min(this.sourceDataLine.available(), data.length));
    }

    @Override
    public void reset() {
      if (this.working) {
        LOGGER.info("Reset");
        this.soundDataQueue.clear();
        this.sndBuffer.reset();
      }
    }

    @Override
    public void run() {
      LOGGER.info("Starting thread");
      try {
        this.sourceDataLine
                .open(SndBufferContainer.AUDIO_FORMAT,
                        SndBufferContainer.SND_BUFFER_SIZE * SndBufferContainer.BUFFERS_NUMBER);

        LOGGER.info(format(
                "Sound line opened, buffer size is %d byte(s)",
                this.sourceDataLine.getBufferSize())
        );

        byte[] empty =
                new byte[SndBufferContainer.BUFFERS_NUMBER * SndBufferContainer.SND_BUFFER_SIZE];
        Arrays.fill(empty, (byte) 0xFF);
        this.sourceDataLine.write(empty, 0, empty.length);

        this.sourceDataLine.start();

        LOGGER.info("Sound line started");

        while (this.working && !Thread.currentThread().isInterrupted()) {
          final byte[] dataBlock = soundDataQueue.poll();
          if (dataBlock != null) {
            this.writeToLine(dataBlock);
          }
        }
        LOGGER.info("Main loop completed");
      } catch (Exception ex) {
        LOGGER.log(Level.WARNING, "Error in sound line work: " + ex);
      } finally {
        try {
          this.sourceDataLine.drain();
        } finally {
          try {
            this.sourceDataLine.stop();
            LOGGER.info("Line stopped");
          } catch (Exception ex) {
            LOGGER.warning("Exception in source line stop: " + ex.getMessage());
          } finally {
            try {
              this.sourceDataLine.close();
              LOGGER.info("Line closed");
            } catch (Exception ex) {
              LOGGER.warning("Exception in source line close: " + ex.getMessage());
            }
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
