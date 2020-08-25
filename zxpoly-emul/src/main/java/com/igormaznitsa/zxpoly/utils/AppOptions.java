/*
 * Copyright (C) 2014-2019 Igor Maznitsa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.igormaznitsa.zxpoly.utils;

import java.io.File;
import java.net.InetAddress;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

public final class AppOptions {

  public static final String TEST_ROM = "zxpolytest.rom";
  private static final Logger LOGGER = Logger.getLogger(AppOptions.class.getName());
  private static final AppOptions INSTANCE = new AppOptions();
  private static final String APP_FOLDER_NAME = ".zxpolyemul";
  private final Preferences preferences = Preferences.userNodeForPackage(AppOptions.class);

  public static AppOptions getInstance() {
    return INSTANCE;
  }

  public synchronized boolean isTestRomActive() {
    return TEST_ROM.equals(this.getActiveRom());
  }

  public synchronized String getLastSelectedAudioDevice() {
    return preferences.get(Option.LAST_SELECTED_AUDIO_DEVICE.name(), null);
  }

  public synchronized void setLastSelectedAudioDevice(final String device) {
    preferences.put(Option.LAST_SELECTED_AUDIO_DEVICE.name(), device);
  }

  public synchronized String getAddress() {
    return preferences
        .get(Option.STREAM_ADDR.name(), InetAddress.getLoopbackAddress().getHostAddress());
  }

  public synchronized void setAddress(final String address) {
    preferences.put(Option.STREAM_ADDR.name(),
        address == null ? InetAddress.getLoopbackAddress().getHostAddress() : address);
  }

  public synchronized int getPort() {
    return preferences.getInt(Option.STREAM_PORT.name(), 0);
  }

  public synchronized void setPort(final int port) {
    preferences.putInt(Option.STREAM_PORT.name(), port & 0xFFFF);
  }

  public synchronized boolean isTurboSound() {
    return preferences.getBoolean(Option.TURBOSOUND.name(), false);
  }

  public synchronized int getFrameRate() {
    return preferences.getInt(Option.STREAM_FRAMERATE.name(), 25);
  }

  public synchronized void setFrameRate(final int value) {
    preferences.putInt(Option.STREAM_FRAMERATE.name(), Math.max(1, Math.min(50, value)));
  }

  public synchronized String getFfmpegPath() {
    return preferences
        .get(Option.STREAM_FFMPEGPATH.name(), SystemUtils.IS_OS_WINDOWS ? "ffmpeg.exe" : "ffmpeg");
  }

  public synchronized void setFfmpegPath(final String path) {
    preferences.put(Option.STREAM_FFMPEGPATH.name(),
        path == null ? (SystemUtils.IS_OS_WINDOWS ? "ffmpeg.exe" : "ffmpeg") : path);
  }

  public synchronized boolean isGrabSound() {
    return preferences.getBoolean(Option.STREAM_GRABSOUND.name(), false);
  }

  public synchronized void setGrabSound(final boolean value) {
    preferences.putBoolean(Option.STREAM_GRABSOUND.name(), value);
  }

  public synchronized boolean isCovoxFb() {
    return preferences.getBoolean(Option.COVOXFB.name(), false);
  }

  public synchronized void setCovoxFb(final boolean value) {
    preferences.putBoolean(Option.COVOXFB.name(), value);
  }

  public synchronized void setTurboSound(final boolean value) {
    preferences.putBoolean(Option.TURBOSOUND.name(), value);
  }

  public enum Rom {
    TEST("ROM TEST", AppOptions.TEST_ROM),
    ZX128WOS("ROM ZX-128 TRDOS (WoS)",
        "http://wos.meulie.net/pub/sinclair/emulators/pc/russian/ukv12f5.zip"),
    ZX128ARCH("ROM ZX-128 TRDOS (Archive.org)",
        "https://archive.org/download/World_of_Spectrum_June_2017_Mirror/World%20of%20Spectrum%20June%202017%20Mirror.zip/World%20of%20Spectrum%20June%202017%20Mirror/sinclair/emulators/pc/russian/ukv12f5.zip"),
    ZX128PDP11RU("ROM ZX-128 TRDOS (Pdp-11.ru)",
        "http://mirrors.pdp-11.ru/_zx/vtrdos.ru/emulz/UKV12F5.ZIP"),
    ZX128VTRDOS("ROM ZX-128 TRDOS (VTR-DOS)", "http://trd.speccy.cz/emulz/UKV12F5.ZIP");

    private final String title;
    private final String link;

    Rom(final String title, final String link) {
      this.title = title;
      this.link = link;
    }

    public static Rom findForTitle(final String title, final Rom dflt) {
      for (final Rom r : Rom.values()) {
        if (r.title.equalsIgnoreCase(title)) {
          return r;
        }
      }
      return dflt;
    }

    public static Rom findForLink(final String link, final Rom dflt) {
      for (final Rom r : Rom.values()) {
        if (r.link.equals(link)) {
          return r;
        }
      }
      return dflt;
    }

    @Override
    public String toString() {
      return this.title;
    }

    public String getTitle() {
      return this.title;
    }

    public String getLink() {
      return this.link;
    }
  }

  public synchronized String getActiveRom() {
    return preferences.get(Option.ROMPATH.name(), AppOptions.TEST_ROM);
  }

  public synchronized void setActiveRom(final String romPath) {
    preferences.put(Option.ROMPATH.name(), romPath);
  }

  public synchronized int getIntBetweenFrames() {
    return preferences.getInt(Option.INTBETWEENFRAMES.name(), 3);
  }

  public synchronized void setIntBetweenFrames(final int value) {
    preferences.putInt(Option.INTBETWEENFRAMES.name(), Math.max(0, value));
  }

  public synchronized void flush() throws BackingStoreException {
    preferences.flush();
  }

  public synchronized File getAppConfigFolder() {
    String folder = System.getenv("APPDATA");
    if (folder == null) {
      folder = System.getProperty("user.home", FileUtils.getTempDirectoryPath());
    }

    final File configFolder = new File(folder, APP_FOLDER_NAME);
    if (!configFolder.exists()) {
      if (configFolder.mkdirs()) {
        LOGGER.info("Created config folder: " + configFolder);
      } else {
        LOGGER.warning("Can't create config folder: " + configFolder);
      }
    }
    return configFolder;
  }

  public enum Option {
    STREAM_FFMPEGPATH,
    STREAM_GRABSOUND,
    STREAM_ADDR,
    STREAM_PORT,
    STREAM_FRAMERATE,
    ROMPATH,
    COVOXFB,
    TURBOSOUND,
    INTBETWEENFRAMES,
    LAST_SELECTED_AUDIO_DEVICE
  }

}
