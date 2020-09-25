package com.igormaznitsa.zxpoly.components.tapereader;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;
import org.apache.commons.io.FilenameUtils;

public class TapeSourceFactory {

  public static TapeSource getSource(File selectedTapFile) throws IOException {
    final String extension = FilenameUtils.getExtension(selectedTapFile.getName()).toLowerCase(
        Locale.ENGLISH);
    if (extension.equals("tap")) {
      try (final InputStream inputStream = new BufferedInputStream(
          new FileInputStream(selectedTapFile))) {
        return new TapeFileReader(selectedTapFile.getName(), inputStream);
      }
    } else {
      throw new IllegalArgumentException("Unsupported tape container: " + extension);
    }
  }

}
