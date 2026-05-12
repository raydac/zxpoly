package com.igormaznitsa.zxpoly;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public final class Version {

  public static final String APP_TITLE = "ZX-Poly emulator";
  public static final String APPLICATION_VERSION;
  public static final int VERSION_MAJOR;
  public static final int VERSION_MINOR;
  public static final int VERSION_BUILD;
  public static final String APP_VERSION;
  private static final String BUILD_PROPERTIES = "com/igormaznitsa/zxpoly/build.properties";

  static {
    final String raw = readRawVersion();
    APPLICATION_VERSION = raw;
    final int[] triplet = parseSemverTriplet(raw);
    VERSION_MAJOR = triplet[0];
    VERSION_MINOR = triplet[1];
    VERSION_BUILD = triplet[2];
    APP_VERSION = "v " + APPLICATION_VERSION;
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

  /**
   * Parses leading numeric {@code major.minor.patch} from a Maven-style version (e.g. {@code 2.3.5-SNAPSHOT}).
   */
  private static int[] parseSemverTriplet(final String raw) {
    int end = 0;
    while (end < raw.length()) {
      final char c = raw.charAt(end);
      if (Character.isDigit(c) || c == '.') {
        end++;
      } else {
        break;
      }
    }
    final String prefix = end == 0 ? "" : raw.substring(0, end);
    if (prefix.isEmpty()) {
      return new int[] {0, 0, 0};
    }
    final String[] parts = prefix.split("\\.");
    final int major = parts.length > 0 ? parseUnsignedOrZero(parts[0]) : 0;
    final int minor = parts.length > 1 ? parseUnsignedOrZero(parts[1]) : 0;
    final int build = parts.length > 2 ? parseUnsignedOrZero(parts[2]) : 0;
    return new int[] {major, minor, build};
  }

  private static int parseUnsignedOrZero(final String segment) {
    if (segment == null || segment.isEmpty()) {
      return 0;
    }
    try {
      return Integer.parseUnsignedInt(segment);
    } catch (final NumberFormatException ex) {
      return 0;
    }
  }
}
