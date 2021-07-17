package com.igormaznitsa.zxpoly.components.tapereader;

import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.Locale;

public final class TapeSourceFactory {

  private TapeSourceFactory() {

  }

  public static TapeSource makeSource(final TimingProfile timingProfile, File selectedTapFile) throws IOException {
    final String extension = FilenameUtils.getExtension(selectedTapFile.getName()).toLowerCase(
            Locale.ENGLISH);
    if (extension.equals("tap")) {
      try (final InputStream inputStream = new BufferedInputStream(
              new FileInputStream(selectedTapFile))) {
        return new ReaderTap(selectedTapFile.getName(), inputStream);
      }
    } else if (extension.equals("wav")) {
      return new ReaderWav(timingProfile, selectedTapFile.getName(), selectedTapFile);
    } else {
      throw new IllegalArgumentException("Unsupported tape container: " + extension);
    }
  }

}
