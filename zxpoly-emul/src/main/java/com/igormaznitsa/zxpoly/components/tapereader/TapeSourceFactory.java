package com.igormaznitsa.zxpoly.components.tapereader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.apache.commons.io.FilenameUtils;

public final class TapeSourceFactory {

  private TapeSourceFactory() {

  }

  public static TapeSource makeSource(File selectedTapFile) throws IOException {
    final String extension = FilenameUtils.getExtension(selectedTapFile.getName()).toLowerCase(
        Locale.ENGLISH);
    if (extension.equals("tap")) {
      try (final InputStream inputStream = new BufferedInputStream(
          new FileInputStream(selectedTapFile))) {
        return new ReaderTap(selectedTapFile.getName(), inputStream);
      }
    } else if (extension.equals("wav")) {
      return new ReaderWav(selectedTapFile.getName(), selectedTapFile);
    } else {
      throw new IllegalArgumentException("Unsupported tape container: " + extension);
    }
  }

}
