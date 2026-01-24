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

package com.igormaznitsa.zxpoly.components.sound;

import static java.lang.Long.toHexString;
import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import com.igormaznitsa.zxpoly.components.tapereader.WriterWav;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.IntStream;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.Line;
import javax.sound.sampled.SourceDataLine;

public final class Beeper {

  public static final int CHANNEL_BEEPER = 0;
  public static final int CHANNEL_COVOX = 1;
  public static final int CHANNEL_AY_A = 2;
  public static final int CHANNEL_AY_B = 3;
  public static final int CHANNEL_AY_C = 4;
  public static final int CHANNEL_TS_A = 5;
  public static final int CHANNEL_TS_B = 6;
  public static final int CHANNEL_TS_C = 7;
  public static final AudioFormat AUDIO_FORMAT = SndBufferContainer.AUDIO_FORMAT;
  private static final Logger LOGGER = Logger.getLogger(Beeper.class.getName());
  private static final IWavWriter NULL_WAV = new IWavWriter() {
    @Override
    public void updateState(boolean tiStatesInt, boolean wallClockInt, int spentTiStates,
                            int levelLeft, int levelRight) {

    }

    @Override
    public void dispose() {

    }
  };
  private static final IBeeper NULL_BEEPER = new IBeeper() {
    @Override
    public void updateState(boolean tiStatesInt, boolean wallClockInt, int spentTiStates,
                            int levelLeft, int levelRight) {
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
  private final boolean tryConsumeLessSystemResources;
  private final AtomicReference<IBeeper> activeInternalBeeper = new AtomicReference<>(NULL_BEEPER);
  private final SoundChannelLowPassFilter[] soundChannelLowPassFilters;
  private final int[] channels = new int[8];
  private final MixerFunction mixerLeft;
  private final MixerFunction mixerRight;
  private final TimingProfile timingProfile;
  private final AtomicReference<IWavWriter> activeWavWriter = new AtomicReference<>(NULL_WAV);
  private final AtomicReference<IWavWriter> suspendedWavWriter = new AtomicReference<>();

  public Beeper(
      final TimingProfile timingProfile,
      final float lowPassFilterValue,
      final boolean useAcbSoundScheme,
      final boolean covoxPresented,
      final boolean turboSoundPresented,
      final boolean tryConsumeLessSystemResources) {
    this.soundChannelLowPassFilters =
        IntStream.range(0, 8).mapToObj(i -> new SoundChannelLowPassFilter(i, lowPassFilterValue))
            .toArray(SoundChannelLowPassFilter[]::new);
    this.tryConsumeLessSystemResources = tryConsumeLessSystemResources;
    this.timingProfile = timingProfile;
    if (useAcbSoundScheme) {
      if (turboSoundPresented && covoxPresented) {
        this.mixerLeft = MixerUtilsACB::mixLeft_TS_CVX;
        this.mixerRight = MixerUtilsACB::mixRight_TS_CVX;
      } else if (covoxPresented) {
        this.mixerLeft = MixerUtilsACB::mixLeft_CVX;
        this.mixerRight = MixerUtilsACB::mixRight_CVX;
      } else if (turboSoundPresented) {
        this.mixerLeft = MixerUtilsACB::mixLeft_TS;
        this.mixerRight = MixerUtilsACB::mixRight_TS;
      } else {
        this.mixerLeft = MixerUtilsACB::mixLeft;
        this.mixerRight = MixerUtilsACB::mixRight;
      }
    } else {
      if (turboSoundPresented && covoxPresented) {
        this.mixerLeft = MixerUtilsABC::mixLeft_TS_CVX;
        this.mixerRight = MixerUtilsABC::mixRight_TS_CVX;
      } else if (covoxPresented) {
        this.mixerLeft = MixerUtilsABC::mixLeft_CVX;
        this.mixerRight = MixerUtilsABC::mixRight_CVX;
      } else if (turboSoundPresented) {
        this.mixerLeft = MixerUtilsABC::mixLeft_TS;
        this.mixerRight = MixerUtilsABC::mixRight_TS;
      } else {
        this.mixerLeft = MixerUtilsABC::mixLeft;
        this.mixerRight = MixerUtilsABC::mixRight;
      }
    }
  }

  public void setChannelValue(final int channel, final int level256) {
    this.channels[channel] = level256 & 0xFF;
  }

  public boolean hasActiveWaFile() {
    return this.activeWavWriter.get() != NULL_WAV;
  }

  public void replaceSuspendedWriter(final IWavWriter replacement) {
    if (this.suspendedWavWriter.get() == null) {
      throw new IllegalStateException("There is no any suspended writer to replace");
    }
    final IWavWriter wavWriter = this.suspendedWavWriter.getAndSet(requireNonNull(replacement));
    if (wavWriter != replacement) {
      wavWriter.dispose();
    }
  }

  public void suspendWavWriter() {
    this.suspendedWavWriter.set(this.activeWavWriter.getAndSet(NULL_WAV));
  }

  public void resumeWavWriter() {
    final IWavWriter suspendedWriter = this.suspendedWavWriter.getAndSet(null);
    if (suspendedWriter == null) {
      throw new IllegalStateException("Suspended writer is null");
    } else {
      this.activeWavWriter.set(suspendedWriter);
    }
  }

  public IWavWriter makeTargetWavWriter(final File file) throws IOException {
    final IWavWriter prev = this.activeWavWriter.getAndSet(NULL_WAV);
    if (prev != null) {
      prev.dispose();
    }

    final IWavWriter newWavWriter;
    if (file == null) {
      newWavWriter = NULL_WAV;
    } else {
      newWavWriter = new WavWriterImpl(this.timingProfile, file);
    }

    return newWavWriter;
  }

  public Optional<SourceSoundPort> setSourceSoundPort(final SourceSoundPort soundPort) {
    final IBeeper prevBeeper = this.activeInternalBeeper.get();
    if (soundPort == null) {
      this.activeInternalBeeper.getAndSet(NULL_BEEPER).dispose();
    } else {
      try {
        final IBeeper newInternalBeeper =
            new InternalBeeper(this.timingProfile, soundPort, this.tryConsumeLessSystemResources);
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

  public void updateState(final boolean tiStatesInt, final boolean wallClockInt,
                          final int spentTiStates) {
    final int leftChannel =
        this.mixerLeft.mix(this.channels, this.soundChannelLowPassFilters, spentTiStates);
    final int rightChannel =
        this.mixerRight.mix(this.channels, this.soundChannelLowPassFilters, spentTiStates);

    this.activeInternalBeeper.get()
        .updateState(tiStatesInt,
            wallClockInt,
            spentTiStates,
            leftChannel,
            rightChannel
        );

    this.activeWavWriter.get()
        .updateState(tiStatesInt,
            wallClockInt,
            spentTiStates,
            leftChannel,
            rightChannel
        );
  }

  public void reset() {
    this.clearChannels();
    this.activeInternalBeeper.get().reset();
  }

  public void clearChannels() {
    Arrays.fill(this.channels, 0);
    for (final SoundChannelLowPassFilter f : this.soundChannelLowPassFilters) {
      f.reset();
    }
  }

  public boolean isActive() {
    return this.activeInternalBeeper.get() != NULL_BEEPER;
  }

  public void dispose() {
    this.activeInternalBeeper.getAndSet(NULL_BEEPER).dispose();
    this.activeWavWriter.getAndSet(NULL_WAV).dispose();
  }

  public AudioFormat getAudioFormat() {
    return SndBufferContainer.AUDIO_FORMAT;
  }

  @FunctionalInterface
  private interface MixerFunction {
    int mix(int[] values, SoundChannelLowPassFilter[] filters, int spentTiStates);
  }

  public interface IWavWriter {
    void updateState(boolean tiStatesInt, boolean wallClockInt, int spentTiStates, int levelLeft,
                     int levelRight);

    void dispose();
  }

  private interface IBeeper {

    Optional<SourceSoundPort> getSoundPort();

    void start();

    void updateState(boolean tiStatesInt, boolean wallClockInt, int spentTiStates, int levelLeft,
                     int levelRight);

    void dispose();

    void reset();
  }

  private static final class WavWriterImpl implements IWavWriter {

    private final WriterWav.WavFile targetFile;

    private final double framesPerTick;
    private int lastLeftChannel = 0;
    private int lastRightChannel = 0;
    private double frameCounter = 0.0d;

    private WavWriterImpl(final TimingProfile timingProfile, final File wavFile)
        throws IOException {
      LOGGER.info("Creating WAV file: " + wavFile);
      this.targetFile = new WriterWav.WavFile(wavFile);

      this.framesPerTick = 44100.0d / timingProfile.clockFreq;

      final int encoding;
      if (AUDIO_FORMAT.getEncoding() == AudioFormat.Encoding.PCM_SIGNED ||
          AUDIO_FORMAT.getEncoding() == AudioFormat.Encoding.PCM_UNSIGNED) {
        encoding = 1;
      } else if (AUDIO_FORMAT.getEncoding() == AudioFormat.Encoding.ALAW) {
        encoding = 6;
      } else if (AUDIO_FORMAT.getEncoding() == AudioFormat.Encoding.ULAW) {
        encoding = 7;
      } else {
        throw new IllegalArgumentException("Unsupported WAV encode: " + AUDIO_FORMAT.getEncoding());
      }

      this.targetFile.header(
          encoding,
          AUDIO_FORMAT.getChannels(),
          44100,
          176400,
          4,
          16
      );
    }

    @Override
    public void updateState(
        final boolean tiStatesInt,
        final boolean wallClockInt,
        final int spentTiStates,
        final int levelLeft,
        final int levelRight
    ) {
      final double frameOffset = spentTiStates * this.framesPerTick;

      long currentFrame = (long) this.frameCounter;
      this.frameCounter += frameOffset;
      long endFrame = (long) this.frameCounter;

      final byte leftHigh = (byte) (this.lastLeftChannel >> 8);
      final byte leftLow = (byte) this.lastLeftChannel;

      final byte rightHigh = (byte) (this.lastRightChannel >> 8);
      final byte rightLow = (byte) this.lastRightChannel;

      try {
        for (; currentFrame < endFrame; currentFrame++) {
          this.targetFile.data(leftLow, leftHigh, rightLow, rightHigh);
        }
      } catch (final IOException ex) {
        LOGGER.log(Level.SEVERE, "Can;t write WAV data into file", ex);
      } finally {
        this.lastRightChannel = levelRight;
        this.lastLeftChannel = levelLeft;
      }
    }

    @Override
    public void dispose() {
      try {
        LOGGER.info("Closing wav file");
        this.targetFile.close();
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, "Error during WAV file close", ex);
      }
    }
  }

  private static final class InternalBeeper implements IBeeper {

    private final BlockingQueue<byte[]> soundDataQueue =
        new ArrayBlockingQueue<>(SndBufferContainer.BUFFERS_NUMBER);
    private final SourceDataLine sourceDataLine;
    private final Thread thread;
    private final SndBufferContainer sndBuffer;
    private final Optional<SourceSoundPort> optionalSourceSoundPort;
    private volatile boolean working = true;

    private InternalBeeper(
        final TimingProfile timingProfile,
        final SourceSoundPort optionalSourceSoundPort,
        final boolean tryConsumeLessSystemResources) {
      this.sndBuffer = new SndBufferContainer(timingProfile);
      this.optionalSourceSoundPort = Optional.of(optionalSourceSoundPort);
      this.sourceDataLine = optionalSourceSoundPort.asSourceDataLine();
      final Line.Info lineInfo = this.sourceDataLine.getLineInfo();
      LOGGER.info("Got sound data line: " + lineInfo.toString());

      this.thread = tryConsumeLessSystemResources ?
          Thread.ofVirtual().name("zxp-beeper-thread-virtual-" + toHexString(System.nanoTime()))
              .unstarted(this::mainLoop) :
          Thread.ofPlatform().name("zxp-beeper-thread-" + toHexString(System.nanoTime()))
              .unstarted(this::mainLoop);
      this.thread.setPriority(Thread.MAX_PRIORITY);
      this.thread.setDaemon(true);
      this.thread.setUncaughtExceptionHandler(
          (t, e) -> {
            LOGGER.log(Level.SEVERE, "Unexpected error: " + e.getMessage());
            e.printStackTrace(System.err);
          });
    }

    @Override
    public Optional<SourceSoundPort> getSoundPort() {
      return this.optionalSourceSoundPort;
    }

    @Override
    public void start() {
      if (this.thread != null && this.working) {
        this.thread.start();
      }
    }

    @Override
    public void updateState(
        boolean tiStatesInt,
        boolean wallClockInt,
        int spentTiStates,
        final int levelLeft,
        final int levelRight
    ) {
      if (this.working) {
        if (wallClockInt) {
          this.soundDataQueue.offer(sndBuffer.nextBuffer(levelLeft, levelRight));
          sndBuffer.resetPosition();
        } else {
          sndBuffer.setValue(spentTiStates, levelLeft, levelRight);
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

    private void mainLoop() {
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
