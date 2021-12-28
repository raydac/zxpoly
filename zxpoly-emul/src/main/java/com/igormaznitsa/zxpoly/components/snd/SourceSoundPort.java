package com.igormaznitsa.zxpoly.components.snd;

import javax.sound.sampled.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class SourceSoundPort implements Comparable<SourceSoundPort> {
  private final UUID uuid = UUID.randomUUID();
  private final String name;
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
  }

  public static List<SourceSoundPort> findForFormat(final AudioFormat format) {
    final List<SourceSoundPort> result = new ArrayList<>();
    final Mixer.Info[] mixers = AudioSystem.getMixerInfo();
    for (final Mixer.Info mixerInfo : mixers) {
      try (Mixer mixer = AudioSystem.getMixer(mixerInfo)) {
        try {
          mixer.open();
        } catch (LineUnavailableException ex) {
          continue;
        }
        final Line.Info[] sourceLineInfo = mixer.getSourceLineInfo();
        for (final Line.Info lineInfo : sourceLineInfo) {
          try {
            final Line line = mixer.getLine(lineInfo);
            if (line instanceof SourceDataLine) {
              result.add(new SourceSoundPort(mixer,
                      mixerInfo.getName() + ':' + line.getLineInfo().toString(), line));
            }
          } catch (LineUnavailableException ex) {
            // DO NOTHING
          }
        }
      }
    }

    return result.stream()
            .sorted()
            .filter(x -> x.doesSupport(format))
            .collect(Collectors.toList());
  }

  public boolean doesSupport(final AudioFormat format) {
    final Line.Info info = this.line.getLineInfo();
    boolean result = false;
    if (info instanceof DataLine.Info) {
      result = Stream.of(((DataLine.Info) info).getFormats())
              .anyMatch(format::matches);
    }
    return result;
  }

  public UUID getUuid() {
    return this.uuid;
  }

  public String getName() {
    return this.name;
  }

  public Mixer getMixer() {
    return this.mixer;
  }

  public Line getLine() {
    return this.line;
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public int compareTo(SourceSoundPort o) {
    return this.name.compareTo(o.name);
  }

  public SourceDataLine asSourceDataLine() {
    return (SourceDataLine) this.line;
  }
}
