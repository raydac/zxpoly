package com.igormaznitsa.zxpoly.streamer;

import com.igormaznitsa.zxpoly.components.Beeper;
import com.igormaznitsa.zxpoly.components.VideoController;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.logging.Logger;

public class ZxVideoStreamer {
  public static final Logger LOGGER = Logger.getLogger("VideoStreamer");

  private final VideoController videoController;
  private final Beeper beeper;

  private final AtomicReference<Thread> currentThread = new AtomicReference<>();
  private final int frameRate;
  private final String ffmpegPath;
  private final InetAddress address;
  private final int port;
  private final TcpWriter videoWriter;
  private final TcpWriter soundWriter;
  private final AtomicReference<FfmpegWrapper> ffmpegWrapper = new AtomicReference<>();
  private final Consumer<ZxVideoStreamer> endWorkConsumer;
  private final ZxSoundPort soudPort;
  private final InternalHttpServer outDataRetranslator;
  private volatile boolean stopped;

  public ZxVideoStreamer(
      final VideoController videoController,
      final Beeper beeper,
      final String ffmpegPath,
      final InetAddress address,
      final int port,
      final int snapsPerSecond,
      final Consumer<ZxVideoStreamer> endWorkConsumer) {
    this.beeper = beeper;
    this.endWorkConsumer = endWorkConsumer;
    this.ffmpegPath = ffmpegPath;
    this.address = address;
    this.port = port;
    this.videoController = videoController;
    this.frameRate = snapsPerSecond;
    this.videoWriter =
        new TcpWriter("tcp-video-writer", 2, InetAddress.getLoopbackAddress(), 0);
    this.soundWriter =
        new TcpWriter("tcp-sound-writer", 16, InetAddress.getLoopbackAddress(), 0);
    this.outDataRetranslator =
        new InternalHttpServer("video/MP2T", InetAddress.getLoopbackAddress(), 0,
            address, port);
    this.soudPort = new ZxSoundPort(this.soundWriter);
  }

  public ZxSoundPort getSoudPort() {
    return this.soudPort;
  }

  public void stop() {
    this.stopped = true;

    this.outDataRetranslator.stop();

    if (this.beeper != null) {
      this.beeper.setSourceSoundPort(null);
    }

    final FfmpegWrapper ffmpeg = this.ffmpegWrapper.getAndSet(null);
    if (ffmpeg != null) {
      ffmpeg.stop();
    }

    this.videoWriter.stop();
    this.soundWriter.stop();

    final Thread thread = this.currentThread.getAndSet(null);
    if (thread != null) {
      thread.interrupt();
      try {
        thread.join();
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public void start() {
    stop();

    this.stopped = false;
    CountDownLatch latch = new CountDownLatch(2);
    final AtomicInteger errorCounter = new AtomicInteger();

    final AbstractTcpSingleThreadServer.TcpServerListener
        listener = new AbstractTcpSingleThreadServer.TcpServerListener() {

      @Override
      public void onConnected(AbstractTcpSingleThreadServer writer, Socket socket) {
        LOGGER.info("Incomming connection " + writer.getId() + ": " + socket);
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
      public void onDone(final AbstractTcpSingleThreadServer writer) {
        ZxVideoStreamer.this.onDone();
      }

    };

    this.videoWriter.addListener(listener);
    this.soundWriter.addListener(listener);

    this.videoWriter.start();
    this.soundWriter.start();

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
      this.outDataRetranslator.start();
    } catch (Exception ex) {
      stop();
      throw new IllegalStateException("Can't start internal tcp-http retranslator", ex);
    }

    final FfmpegWrapper ffmpeg = new FfmpegWrapper(
        this.ffmpegPath,
        this.frameRate,
        "tcp://" + this.videoWriter.getServerAddress(),
        "tcp://" + this.soundWriter.getServerAddress(),
        "tcp://" + this.outDataRetranslator.getTcpAddress()
    );

    if (this.beeper != null) {
      this.beeper.setSourceSoundPort(new ZxSoundPort(this.soundWriter));
    }

    try {
      ffmpeg.start();
    } catch (Exception ex) {
      stop();
      throw new IllegalStateException("Can't start FFmpeg", ex);
    }
    try {
      Thread.sleep(300);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
    }

    if (!ffmpeg.isAlive()) {
      this.stop();
      throw new IllegalStateException("Can't start");
    }

    final Thread newThread = new Thread(
        this::doWork,
        "zx-video-streamer-" + Long.toHexString(System.currentTimeMillis())
    );
    newThread.setDaemon(true);
    if (this.currentThread.compareAndSet(null, newThread)) {
      newThread.start();
    }
  }

  private void onDone() {
    this.stop();
    if (this.endWorkConsumer != null) {
      this.endWorkConsumer.accept(this);
    }
  }

  private void doWork() {
    LOGGER.info("started streamer, http address: " + this.outDataRetranslator.getHttpAddress());

    final long delay = ((10000L / frameRate) + 5L) / 10L;
    while (!Thread.currentThread().isInterrupted()) {
      final long time = System.currentTimeMillis();
      this.videoWriter.write(this.videoController.grabRgb());
      final long restDelay = (time + delay) - System.currentTimeMillis();
      if (restDelay > 0) {
        try {
          Thread.sleep(restDelay);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }
  }
}
