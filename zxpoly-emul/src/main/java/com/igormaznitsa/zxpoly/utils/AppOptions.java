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

import com.igormaznitsa.zxpoly.components.BoardMode;
import com.igormaznitsa.zxpoly.components.video.VirtualKeyboardLook;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

import java.awt.event.KeyEvent;
import java.io.File;
import java.net.InetAddress;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

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

  public synchronized boolean getAutoCsForCursorKeys() {
    return preferences.getBoolean(Option.AUTOCS_FOR_CURSOR_KEYS.name(), true);
  }

  public synchronized void setAutoCsForCursorKeys(final boolean flag) {
    preferences.putBoolean(Option.AUTOCS_FOR_CURSOR_KEYS.name(), flag);
  }

  public synchronized VirtualKeyboardLook getKeyboardLook() {
    VirtualKeyboardLook result = VirtualKeyboardLook.DEFAULT;
    final String name = preferences.get(Option.KEYBOARD_LOOK.name(), result.name());
    try {
      result = VirtualKeyboardLook.valueOf(name);
    } catch (IllegalArgumentException ex) {
      result = VirtualKeyboardLook.DEFAULT;
    }
    return result;
  }

  public synchronized String getCustomRomPath() {
    return preferences.get(Option.CUSTOM_ROM_PATH.name(), null);
  }

  public synchronized void setCustomRomPath(final String path) {
    if (path == null || path.trim().length() == 0) {
      preferences.remove(Option.CUSTOM_ROM_PATH.name());
    } else {
      preferences.put(Option.CUSTOM_ROM_PATH.name(), path);
    }
  }

  public synchronized void setKeyboardLook(final VirtualKeyboardLook look) {
    preferences.put(Option.KEYBOARD_LOOK.name(), look.name());
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

  public synchronized void setTurboSound(final boolean value) {
    preferences.putBoolean(Option.TURBOSOUND.name(), value);
  }

  public synchronized boolean isSoundTurnedOn() {
    return preferences.getBoolean(Option.SOUND_TURNED_ON.name(), false);
  }

  public synchronized void setSoundTurnedOn(final boolean value) {
    preferences.putBoolean(Option.SOUND_TURNED_ON.name(), value);
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

  public synchronized BoardMode getDefaultBoardMode() {
    final String mode = preferences.get(Option.DEFAULT_MODE.name(), BoardMode.ZXPOLY.name());
    try {
      return BoardMode.valueOf(mode);
    } catch (NoSuchElementException ex) {
      return BoardMode.ZXPOLY;
    }
  }

  public synchronized void setDefaultBoardMode(final BoardMode value) {
    preferences.put(Option.DEFAULT_MODE.name(), value.name());
  }

  public synchronized boolean isVkbdApart() {
    return preferences.getBoolean(Option.VKBD_APART.name(), false);
  }

  public synchronized void setVkbdApart(final boolean value) {
    preferences.putBoolean(Option.VKBD_APART.name(), value);
  }

  public synchronized boolean isCovoxFb() {
    return preferences.getBoolean(Option.COVOXFB.name(), false);
  }

  public synchronized void setCovoxFb(final boolean value) {
    preferences.putBoolean(Option.COVOXFB.name(), value);
  }

  public synchronized boolean isKempstonMouseAllowed() {
    return preferences.getBoolean(Option.KEMPSTON_MOUSE_ALLOWED.name(), true);
  }

  public synchronized void setKempstonMouseAllowed(final boolean value) {
    preferences.putBoolean(Option.KEMPSTON_MOUSE_ALLOWED.name(), value);
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

  public synchronized int getCursorJoystickDown() {
    return preferences.getInt(Option.PROTEK_JOYSTICK_VK_DOWN.name(), KeyEvent.VK_DOWN);
  }

  public synchronized void setProtekJoystickDown(final int keyCode) {
    preferences.putInt(Option.PROTEK_JOYSTICK_VK_DOWN.name(), keyCode);
  }

  public synchronized int getProtekJoystickUp() {
    return preferences.getInt(Option.PROTEK_JOYSTICK_VK_UP.name(), KeyEvent.VK_UP);
  }

  public synchronized void setProtekJoystickUp(final int keyCode) {
    preferences.putInt(Option.PROTEK_JOYSTICK_VK_UP.name(), keyCode);
  }

  public synchronized int getProtekJoystickLeft() {
    return preferences.getInt(Option.PROTEK_JOYSTICK_VK_LEFT.name(), KeyEvent.VK_LEFT);
  }

  public synchronized void setProtekJoystickLeft(final int keyCode) {
    preferences.putInt(Option.PROTEK_JOYSTICK_VK_LEFT.name(), keyCode);
  }

  public synchronized int getProtekJoystickRight() {
    return preferences.getInt(Option.PROTEK_JOYSTICK_VK_RIGHT.name(), KeyEvent.VK_RIGHT);
  }

  public synchronized void setProtekJoystickRight(final int keyCode) {
    preferences.putInt(Option.PROTEK_JOYSTICK_VK_RIGHT.name(), keyCode);
  }

  public synchronized int getProtekJoystickFire() {
    return preferences.getInt(Option.PROTEK_JOYSTICK_VK_FIRE.name(), KeyEvent.VK_TAB);
  }

  public synchronized void setProtekJoystickFire(final int keyCode) {
    preferences.putInt(Option.PROTEK_JOYSTICK_VK_FIRE.name(), keyCode);
  }

  public synchronized int getKempstonVkLeft() {
    return preferences.getInt(Option.KEMPSTON_VK_LEFT.name(), KeyEvent.VK_NUMPAD4);
  }

  public synchronized void setKempstonVkLeft(final int keyCode) {
    preferences.putInt(Option.KEMPSTON_VK_LEFT.name(), keyCode);
  }

  public synchronized int getKempstonVkRight() {
    return preferences.getInt(Option.KEMPSTON_VK_RIGHT.name(), KeyEvent.VK_NUMPAD6);
  }

  public synchronized void setKempstonVkRight(final int keyCode) {
    preferences.putInt(Option.KEMPSTON_VK_RIGHT.name(), keyCode);
  }

  public synchronized int getKempstonVkUp() {
    return preferences.getInt(Option.KEMPSTON_VK_UP.name(), KeyEvent.VK_NUMPAD8);
  }

  public synchronized void setKempstonVkUp(final int keyCode) {
    preferences.putInt(Option.KEMPSTON_VK_UP.name(), keyCode);
  }

  public synchronized int getKempstonVkDown() {
    return preferences.getInt(Option.KEMPSTON_VK_DOWN.name(), KeyEvent.VK_NUMPAD2);
  }

  public synchronized void setKempstonVkDown(final int keyCode) {
    preferences.putInt(Option.KEMPSTON_VK_DOWN.name(), keyCode);
  }

  public synchronized int getKempstonVkFire() {
    return preferences.getInt(Option.KEMPSTON_VK_FIRE.name(), KeyEvent.VK_NUMPAD5);
  }

  public synchronized void setKempstonVkFire(final int keyCode) {
    preferences.putInt(Option.KEMPSTON_VK_FIRE.name(), keyCode);
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

  public enum Option {
    AUTOCS_FOR_CURSOR_KEYS,
    CUSTOM_ROM_PATH,
    STREAM_FFMPEGPATH,
    STREAM_GRABSOUND,
    STREAM_ADDR,
    STREAM_PORT,
    STREAM_FRAMERATE,
    DEFAULT_MODE,
    ROMPATH,
    COVOXFB,
    TURBOSOUND,
    INTBETWEENFRAMES,
    LAST_SELECTED_AUDIO_DEVICE,
    SOUND_TURNED_ON,
    VKBD_APART,
    KEYBOARD_LOOK,
    KEMPSTON_MOUSE_ALLOWED,
    KEMPSTON_VK_LEFT,
    KEMPSTON_VK_RIGHT,
    KEMPSTON_VK_UP,
    KEMPSTON_VK_DOWN,
    KEMPSTON_VK_FIRE,
    PROTEK_JOYSTICK_VK_LEFT,
    PROTEK_JOYSTICK_VK_RIGHT,
    PROTEK_JOYSTICK_VK_UP,
    PROTEK_JOYSTICK_VK_DOWN,
    PROTEK_JOYSTICK_VK_FIRE
  }

}
