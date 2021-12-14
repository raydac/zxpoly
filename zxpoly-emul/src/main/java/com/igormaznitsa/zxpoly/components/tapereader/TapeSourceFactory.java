package com.igormaznitsa.zxpoly.components.tapereader;

import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import org.apache.commons.io.FilenameUtils;

import java.io.*;
import java.util.Locale;

public final class TapeSourceFactory {

  private TapeSourceFactory() {

  }

  public static TapeSource makeSource(final TapeContext tapeContext, final TimingProfile timingProfile, File selectedTapFile) throws IOException {
    final String extension = FilenameUtils.getExtension(selectedTapFile.getName()).toLowerCase(
            Locale.ENGLISH);

    final TapeSource result;

    switch (extension) {
      case "tap": {
        try (final InputStream inputStream = new BufferedInputStream(
                new FileInputStream(selectedTapFile))) {
          result = new ReaderTap(tapeContext, selectedTapFile.getName(), inputStream);
        }
      }
      break;
      case "wav": {
        result = new ReaderWav(tapeContext, timingProfile, selectedTapFile.getName(), selectedTapFile);
      }
      break;
      case "tzx": {
        try (final InputStream inputStream = new BufferedInputStream(
                new FileInputStream(selectedTapFile))) {
          result = new ReaderTzx(tapeContext, timingProfile, selectedTapFile.getName(), inputStream);
        }
      }
      break;
      default:
        throw new IllegalArgumentException("Unsupported tape container: " + extension);
    }
    return result;
  }

}
