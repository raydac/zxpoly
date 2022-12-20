package com.igormaznitsa.zxpoly.streamer;

import static com.igormaznitsa.zxpoly.components.video.tvfilters.TvFilter.RASTER_HEIGHT;
import static com.igormaznitsa.zxpoly.components.video.tvfilters.TvFilter.RASTER_WIDTH_ARGB_INT;

import com.igormaznitsa.zxpoly.components.snd.Beeper;
import com.igormaznitsa.zxpoly.components.video.VideoController;
import com.igormaznitsa.zxpoly.utils.Timer;
import com.igormaznitsa.zxpoly.utils.Utils;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URL;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.logging.Logger;

public final class ZxVideoStreamer {
  public static final Logger LOGGER = Logger.getLogger(ZxVideoStreamer.class.getName());

  private final VideoController videoController;

  private final AtomicBoolean started = new AtomicBoolean();
  private final Consumer<ZxVideoStreamer> endWorkConsumer;
  private final Timer wallClock = new Timer(Duration.ofMillis(20));
  private final byte[] rgbArray = new byte[RASTER_WIDTH_ARGB_INT * RASTER_HEIGHT * 3];
  private volatile TcpWriter videoWriter;
  private volatile TcpWriter soundWriter;
  private volatile FfmpegWrapper ffmpegWrapper;
  private volatile ZxStreamingSoundPort soudPort;
  private volatile HttpProcessor httpProcessor;
  private volatile Beeper beeper;
  private volatile Duration delayBetweenFrameGrab;
  private volatile boolean internalEntitiesStarted;

  public ZxVideoStreamer(
          final VideoController videoController,
          final Consumer<ZxVideoStreamer> endWorkConsumer) {
    this.endWorkConsumer = endWorkConsumer;
    this.videoController = videoController;
  }

  private synchronized void stopAllInternalEntities() {
    if (this.httpProcessor != null) {
      this.httpProcessor.stop();
      this.httpProcessor = null;
    }

    if (this.beeper != null) {
      this.beeper.setSourceSoundPort(null);
    }

    if (this.ffmpegWrapper != null) {
      this.ffmpegWrapper.stop();
      this.ffmpegWrapper = null;
    }

    if (this.videoWriter != null) {
      this.videoWriter.stop();
      this.videoWriter = null;
    }
    if (this.soundWriter != null) {
      this.soundWriter.stop();
      this.soundWriter = null;
    }
  }

  public boolean isStarted() {
    return this.started.get();
  }

  public void stop() {
    if (this.started.compareAndSet(true, false)) {
      LOGGER.info("Stopping");
      this.internalEntitiesStarted = false;
      stopAllInternalEntities();
    }
  }

  private synchronized void startInternalEntities(
          final InetAddress address,
          final int port,
          final String ffmpegPath,
          final int frameRate) {
    this.videoWriter =
            new TcpWriter("tcp-video-writer", 2, InetAddress.getLoopbackAddress(), 0);

    this.delayBetweenFrameGrab = Duration.ofMillis((1000L + frameRate / 2) / frameRate);
    this.wallClock.next(this.delayBetweenFrameGrab);

    if (this.beeper == null) {
      this.soundWriter = null;
      this.soudPort = null;
    } else {
      this.soundWriter =
              new TcpWriter("tcp-sound-writer", 2, InetAddress.getLoopbackAddress(), 0);
      this.soudPort = new ZxStreamingSoundPort(this.soundWriter);
    }

    CountDownLatch latch = new CountDownLatch(this.soundWriter == null ? 1 : 2);
    final AtomicInteger errorCounter = new AtomicInteger();

    final AbstractTcpSingleThreadServer.TcpServerListener
            listener = new AbstractTcpSingleThreadServer.TcpServerListener() {

      @Override
      public void onConnected(AbstractTcpSingleThreadServer writer, Socket socket) {
        LOGGER.info(String.format("Incoming connection %s:%s", writer.getId(), socket));
      }

      @Override
      public void onEstablishing(AbstractTcpSingleThreadServer writer, ServerSocket socket,
                                 Throwable error) {
        if (error != null) {
          errorCounter.incrementAndGet();
        }
        latch.countDown();
      }

      @Override
      public void onDone(final AbstractTcpSingleThreadServer source) {
        internalEntitiesStarted = false;
        ZxVideoStreamer.this.onDone();
      }

      @Override
      public void onConnectionDone(final AbstractTcpSingleThreadServer source,
                                   final Socket socket) {
        internalEntitiesStarted = false;
        ZxVideoStreamer.this.onDone();
      }
    };

    this.videoWriter.addListener(listener);
    if (this.soundWriter != null) {
      this.soundWriter.addListener(listener);
      this.soundWriter.start();
    }
    this.videoWriter.start();

    try {
      latch.await();
    } catch (InterruptedException ex) {
      stop();
      return;
    }

    if (errorCounter.get() != 0) {
      stop();
      throw new IllegalStateException("Can't start internal server");
    }

    try {
      this.httpProcessor =
              new HttpProcessor(
                      "video/MP2T",
                      InetAddress.getLoopbackAddress(),
                      0,
                      address,
                      port,
                      server -> {
                        LOGGER.info("Internal HTTP server has been stopped");
                        this.stop();
                      });
      this.httpProcessor.start();
    } catch (Exception ex) {
      stop();
      throw new IllegalStateException("Can't start internal tcp-http retranslator", ex);
    }

    final FfmpegWrapper ffmpeg = new FfmpegWrapper(
            ffmpegPath,
            frameRate,
            "tcp://" + this.videoWriter.getServerAddress(),
            this.soundWriter == null ? null :
                    "tcp://" + this.soundWriter.getServerAddress(),
            "tcp://" + this.httpProcessor.getTcpAddress()
    );

    if (this.beeper != null) {
      this.beeper.setSourceSoundPort(new ZxStreamingSoundPort(this.soundWriter));
    }

    try {
      ffmpeg.start();
    } catch (Exception ex) {
      stop();
      LOGGER.warning("Can't start ffmpeg: " + ex.getMessage());
      throw new IllegalStateException(ex.getMessage(), ex);
    }
    try {
      Thread.sleep(300);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }

    if (!ffmpeg.isAlive()) {
      this.stop();
      throw new IllegalStateException("ffmpeg can't start");
    }
  }

  public void start(
          final Beeper beeper,
          final String ffmpegPath,
          final InetAddress address,
          final int port,
          final int frameRate
  ) {
    if (this.started.compareAndSet(false, true)) {
      this.beeper = beeper;
      this.startInternalEntities(address, port, ffmpegPath, frameRate);
      internalEntitiesStarted = true;
      final String link = "http://" + this.httpProcessor.getHttpAddress() + '/';
      try {
        Utils.browseLink(new URL(link));
      } catch (MalformedURLException ex) {
        LOGGER.warning("Can't make URL: " + link);
      }
    }
  }

  private void onDone() {
    this.stop();
    if (this.endWorkConsumer != null) {
      this.endWorkConsumer.accept(this);
    }
  }

  public void onWallclockInt() {
    if (this.internalEntitiesStarted) {
      if (this.wallClock.completed()) {
        this.wallClock.next(this.delayBetweenFrameGrab);
        this.videoWriter.write(this.videoController.grabRgb(this.rgbArray));
        this.wallClock.next();
      }
    }
  }
}
