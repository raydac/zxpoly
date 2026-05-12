package com.igormaznitsa.zxpspritecorrector;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Version {

  public static final String APPLICATION_VERSION;
  private static final String BUILD_PROPERTIES =
      "com/igormaznitsa/zxpspritecorrector/build.properties";

  static {
    APPLICATION_VERSION = readRawVersion();
  }

  private Version() {
  }

  private static String readRawVersion() {
    try (InputStream in = Version.class.getClassLoader().getResourceAsStream(BUILD_PROPERTIES)) {
      if (in != null) {
        final Properties properties = new Properties();
        properties.load(in);
        final String fromFile = properties.getProperty("application.version", "").trim();
        if (!fromFile.isEmpty() && !fromFile.contains("${")) {
          return fromFile;
        }
      }
    } catch (final IOException ignored) {
      // fall through
    }
    final Package pkg = Version.class.getPackage();
    if (pkg != null) {
      final String fromManifest = pkg.getImplementationVersion();
      if (fromManifest != null && !fromManifest.isBlank()) {
        return fromManifest.trim();
      }
    }
    return "0.0.0-DEV";
  }
}
