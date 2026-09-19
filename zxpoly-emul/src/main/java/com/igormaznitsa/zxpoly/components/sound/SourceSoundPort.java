package com.igormaznitsa.zxpoly.components.sound;

import static java.util.Arrays.stream;
import static java.util.Objects.requireNonNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.Line;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.Mixer;
import javax.sound.sampled.SourceDataLine;

public class SourceSoundPort implements Comparable<SourceSoundPort> {
  private final UUID uuid = UUID.randomUUID();
  private final String name;
  private final Mixer.Info mixerInfo;
  private final Line.Info lineInfo;
  private final Mixer mixer;
  private final Line line;

  public SourceSoundPort(
      final Mixer mixer,
      final String name,
      final Line line
  ) {
    this.mixer = mixer;
    this.name = name;
    this.line = line;
    this.mixerInfo = mixer == null ? null : mixer.getMixerInfo();
    this.lineInfo = line == null ? null : line.getLineInfo();
  }

  private SourceSoundPort(
      final Mixer.Info mixerInfo,
      final Line.Info lineInfo,
      final String name
  ) {
    this.mixerInfo = requireNonNull(mixerInfo, "mixerInfo");
    this.lineInfo = requireNonNull(lineInfo, "lineInfo");
    this.name = requireNonNull(name, "name");
    this.mixer = null;
    this.line = null;
  }

  public static List<SourceSoundPort> findForFormat(final AudioFormat format) {
    requireNonNull(format, "format");
    return stream(AudioSystem.getMixerInfo())
        .map(SourceSoundPort::portsForMixer)
        .flatMap(List::stream)
        .filter(port -> port.doesSupport(format))
        .sorted()
        .toList();
  }

  private static List<SourceSoundPort> portsForMixer(final Mixer.Info mixerInfo) {
    try {
      final Mixer mixer = AudioSystem.getMixer(mixerInfo);
      return stream(mixer.getSourceLineInfo())
          .filter(SourceSoundPort::isSourceDataLineInfo)
          .map(lineInfo -> new SourceSoundPort(
              mixerInfo,
              lineInfo,
              mixerInfo.getName() + ':' + lineInfo))
          .toList();
    } catch (final Exception ex) {
      return List.of();
    }
  }

  private static boolean isSourceDataLineInfo(final Line.Info lineInfo) {
    return SourceDataLine.class.isAssignableFrom(lineInfo.getLineClass());
  }

  public boolean doesSupport(final AudioFormat format) {
    final Line.Info info = this.lineInfo != null
        ? this.lineInfo
        : (this.line == null ? null : this.line.getLineInfo());
    if (info instanceof DataLine.Info dataLineInfo) {
      return dataLineInfo.isFormatSupported(format)
          || Arrays.stream(dataLineInfo.getFormats()).anyMatch(format::matches);
    }
    return false;
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public String getName() {
    return this.name;
  }

  public Mixer getMixer() {
    if (this.mixer != null) {
      return this.mixer;
    }
    return this.mixerInfo == null ? null : AudioSystem.getMixer(this.mixerInfo);
  }

  public Line getLine() {
    return this.line;
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public int compareTo(final SourceSoundPort that) {
    return this.name.compareTo(that.name);
  }

  public SourceDataLine asSourceDataLine() {
    if (this.line instanceof SourceDataLine sourceDataLine) {
      return sourceDataLine;
    }
    return this.obtainSourceDataLine();
  }

  private SourceDataLine obtainSourceDataLine() {
    if (this.mixerInfo == null || this.lineInfo == null) {
      throw new IllegalArgumentException("Sound port has no mixer line: " + this.name);
    }
    try {
      final Mixer obtainedMixer = AudioSystem.getMixer(this.mixerInfo);
      final Line obtainedLine = obtainedMixer.getLine(this.lineInfo);
      if (obtainedLine instanceof SourceDataLine sourceDataLine) {
        return sourceDataLine;
      }
      throw new IllegalArgumentException("Mixer line is not a SourceDataLine: " + this.name);
    } catch (final LineUnavailableException ex) {
      throw new IllegalArgumentException("Can't obtain source data line: " + ex.getMessage(), ex);
    }
  }
}
