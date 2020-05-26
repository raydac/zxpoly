package com.igormaznitsa.zxpoly.streamer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class FfmpegWrapper {

  private final String ffmpegPath;
  private final int frameRate;
  private final String srcVideo;
  private final String srcAudio;
  private final String dstResult;

  private final AtomicReference<Process> process = new AtomicReference<>();

  public static final Logger LOGGER = Logger.getLogger("VideoStreamer");


  public FfmpegWrapper(
      final String ffmpegPath,
      final int frameRate,
      final String srcVideo,
      final String srcAudio,
      final String dstResult
  ) {
    this.ffmpegPath = ffmpegPath;
    this.frameRate = frameRate;
    this.srcVideo = srcVideo;
    this.srcAudio = srcAudio;
    this.dstResult = dstResult;
  }

  public boolean isAlive() {
    final Process process = this.process.get();
    return process != null && process.isAlive();
  }

  public synchronized void stop() {
    final Process process = this.process.getAndSet(null);
    if (process != null) {
      process.destroyForcibly();
    }
  }

  public synchronized void start() throws IOException {
    stop();

    final List<String> args = new ArrayList<>();

    args.add(this.ffmpegPath);

    args.add("-y");
    args.add("-loglevel");
    args.add("warning");
    args.add("-nostats");
    args.add("-hide_banner");

    args.add("-fflags");
    args.add("+flush_packets+genpts");
    args.add("-use_wallclock_as_timestamps");
    args.add("1");

    args.add("-f");
    args.add("rawvideo");

    args.add("-framerate");
    args.add(Integer.toString(frameRate));

    args.add("-video_size");
    args.add("512X384");
    args.add("-pixel_format");
    args.add("rgb24");

    args.add("-i");
    args.add(this.srcVideo);

    // Sound-----
    args.add("-ac");
    args.add("2");
    args.add("-ar");
    args.add("44100");
    args.add("-f");
    args.add("s16be");
    args.add("-i");
    args.add(this.srcAudio);
    args.add("-c:a");
    args.add("ac3_fixed");
    args.add("-b:a");
    args.add("192k");
    //-------

    args.add("-preset:v");
    args.add("faster");
    args.add("-tune");
    args.add("zerolatency");

    args.add("-c:v");
    args.add("libx264");
    args.add("-pix_fmt");
    args.add("yuv420p");

    args.add("-blocksize");
    args.add("2048");
    args.add("-flush_packets");
    args.add("1");
    args.add("-movflags");
    args.add("+faststart+genpts");
    args.add("-r");
    args.add("25");

    args.add("-f");
    args.add("mpegts");
    args.add(this.dstResult);

    LOGGER.info("Starting FFmpeg: "+args.stream().collect(Collectors.joining(" ")));

    final Process process = new ProcessBuilder(args)
        .redirectError(ProcessBuilder.Redirect.INHERIT)
        .redirectOutput(ProcessBuilder.Redirect.INHERIT)
        .redirectInput(ProcessBuilder.Redirect.PIPE)
        .start();

    if (!this.process.compareAndSet(null, process)) {
      throw new Error("Unexpected state, detected already started process");
    }
  }
}
