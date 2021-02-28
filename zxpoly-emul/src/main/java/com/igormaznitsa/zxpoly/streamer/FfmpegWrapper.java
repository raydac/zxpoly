package com.igormaznitsa.zxpoly.streamer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

import static com.igormaznitsa.zxpoly.components.snd.Beeper.AUDIO_FORMAT;
import static java.lang.String.join;

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
      try {
        LOGGER.info("FFmpeg exit value: " + process.waitFor());
      } catch (Exception ex) {
        LOGGER.warning("error during ffmpeg stop: " + ex.getMessage());
      }
    }
  }

  public synchronized void start() throws IOException {
    stop();

    final List<String> args = new ArrayList<>();

    args.add(this.ffmpegPath);

    args.add("-loglevel");
    args.add("warning");
    args.add("-nostats");
    args.add("-hide_banner");

    args.add("-fflags");
    args.add("+flush_packets+genpts");
    args.add("-use_wallclock_as_timestamps");
    args.add("1");

    args.add("-thread_queue_size");
    args.add("1024");

    args.add("-f");
    args.add("rawvideo");

    args.add("-re");

    args.add("-video_size");
    args.add("512X384");
    args.add("-pixel_format");
    args.add("rgb24");

    args.add("-i");
    args.add(this.srcVideo);

    final String beeperSampleRate = Integer.toString(Math.round(AUDIO_FORMAT.getSampleRate()));

    if (this.srcAudio != null) {
      args.add("-thread_queue_size");
      args.add("1024");

      args.add("-use_wallclock_as_timestamps");
      args.add("1");

      args.add("-re");

      args.add("-ar");
      args.add(beeperSampleRate);

      args.add("-ac");
      args.add("2");

      args.add("-f");
      args.add("s16le");

      args.add("-i");
      args.add(this.srcAudio);

      args.add("-c:a");
      args.add("mp2");
      args.add("-b:a");
      args.add("192k");

      args.add("-af");
      args.add("aresample=async=" + beeperSampleRate);
    }

    args.add("-preset:v");
    args.add("ultrafast");
    args.add("-tune");
    args.add("zerolatency");

    args.add("-c:v");
    args.add("mpeg1video");

    args.add("-vf");
    args.add("format=yuv420p,scale=pal:flags=fast_bilinear,fps=fps=30");

    args.add("-movflags");
    args.add("+faststart");

    args.add("-map");
    args.add("0:v");

    if (this.srcAudio != null) {
      args.add("-map");
      args.add("1:a");
    }

    args.add("-vsync");
    args.add("cfr");

    args.add("-mpegts_flags");
    args.add("resend_headers");

    args.add("-muxpreload");
    args.add("0");
    args.add("-muxdelay");
    args.add("0");

    args.add("-avoid_negative_ts");
    args.add("disabled");

    args.add("-pcr_period");
    args.add("50");

    args.add("-f");
    args.add("mpegts");
    args.add(this.dstResult);

    LOGGER.info("Starting FFmpeg: " + join(" ", args));

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
