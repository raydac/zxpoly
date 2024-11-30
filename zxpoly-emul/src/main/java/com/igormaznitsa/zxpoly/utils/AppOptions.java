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
import com.igormaznitsa.zxpoly.components.sound.VolumeProfile;
import com.igormaznitsa.zxpoly.components.video.BorderWidth;
import com.igormaznitsa.zxpoly.components.video.VirtualKeyboardLook;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import com.igormaznitsa.zxpoly.ui.FastButton;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.swing.UIManager;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.SystemUtils;

public final class AppOptions {

  public static final String TEST_ROM = "zxpolytest.prom";
  private static final Logger LOGGER = Logger.getLogger(AppOptions.class.getName());
  private static final AtomicReference<AppOptions> INSTANCE = new AtomicReference<>();
  private static final String APP_FOLDER_NAME = ".zxpolyemul";
  private volatile static File forceFile;
  private final Preferences preferences;
  private final Lock locker = new ReentrantLock();

  private AppOptions(final File forceFile) {
    if (forceFile == null) {
      LOGGER.log(Level.CONFIG, "Creating options for system provided store");
      this.preferences = Preferences.userNodeForPackage(AppOptions.class);
    } else {
      try {
        LOGGER.log(Level.CONFIG, "Creating options for force file: " + forceFile);
        this.preferences = new FilePlainPreferences("zxpoly-emulator", forceFile, true);
      } catch (IOException ex) {
        throw new Error("Can't create file preferences", ex);
      }
    }
  }

  public static void setForceFile(final File file) {
    forceFile = file;
  }

  public static AppOptions getInstance() {
    AppOptions result = INSTANCE.get();
    if (result == null) {
      AppOptions newInstance = new AppOptions(forceFile);
      if (INSTANCE.compareAndSet(null, newInstance)) {
        result = newInstance;
      }
    }
    return result;
  }

  public String getActiveRom() {
    this.locker.lock();
    try {
      return preferences.get(Option.ROMPATH.name(), AppOptions.TEST_ROM);
    } finally {
      this.locker.unlock();
    }
  }

  public void setActiveRom(final String romPath) {
    this.locker.lock();
    try {
      preferences.put(Option.ROMPATH.name(), romPath);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isTestRomActive() {
    this.locker.lock();
    try {
      return TEST_ROM.equals(this.getActiveRom());
    } finally {
      this.locker.unlock();
    }
  }

  public String getLastSelectedAudioDevice() {
    this.locker.lock();
    try {
      return preferences.get(Option.LAST_SELECTED_AUDIO_DEVICE.name(), null);
    } finally {
      this.locker.unlock();
    }
  }

  public void setLastSelectedAudioDevice(final String device) {
    this.locker.lock();
    try {
      preferences.put(Option.LAST_SELECTED_AUDIO_DEVICE.name(), device);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean getAutoCsForCursorKeys() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.AUTOCS_FOR_CURSOR_KEYS.name(), true);
    } finally {
      this.locker.unlock();
    }
  }

  public void setAutoCsForCursorKeys(final boolean flag) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.AUTOCS_FOR_CURSOR_KEYS.name(), flag);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isUlaPlus() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.ULAPLUS.name(), false);
    } finally {
      this.locker.unlock();
    }
  }

  public void setUlaPlus(final boolean flag) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.ULAPLUS.name(), flag);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isOldColorTvOnStart() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.OLD_COLOR_TV_ON_START.name(), false);
    } finally {
      this.locker.unlock();
    }
  }

  public void setOldColorTvOnStart(final boolean flag) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.OLD_COLOR_TV_ON_START.name(), flag);
    } finally {
      this.locker.unlock();
    }
  }

  public VirtualKeyboardLook getKeyboardLook() {
    this.locker.lock();
    try {
      VirtualKeyboardLook result = VirtualKeyboardLook.DEFAULT;
      final String name = preferences.get(Option.KEYBOARD_LOOK.name(), result.name());
      try {
        result = VirtualKeyboardLook.valueOf(name);
      } catch (IllegalArgumentException ex) {
        // do nothing
      }
      return result;
    } finally {
      this.locker.unlock();
    }
  }

  public void setKeyboardLook(final VirtualKeyboardLook look) {
    this.locker.lock();
    try {
      preferences.put(Option.KEYBOARD_LOOK.name(), look.name());
    } finally {
      this.locker.unlock();
    }
  }

  public VolumeProfile getVolumeProfile() {
    this.locker.lock();
    try {
      VolumeProfile result = VolumeProfile.EXPONENTIAL;
      final String name = preferences.get(Option.VOLUME_PROFILE.name(), result.name());
      try {
        result = VolumeProfile.valueOf(name);
      } catch (IllegalArgumentException ex) {
        // do noting
      }
      return result;
    } finally {
      this.locker.unlock();
    }
  }

  public void setVolumeProfile(final VolumeProfile volumeProfile) {
    this.locker.lock();
    try {
      preferences.put(Option.VOLUME_PROFILE.name(), volumeProfile.name());
    } finally {
      this.locker.unlock();
    }
  }

  public BorderWidth getBorderWidth() {
    this.locker.lock();
    try {
      BorderWidth result = BorderWidth.FULL;
      final String name = preferences.get(Option.BORDER_WIDTH.name(), result.name());
      try {
        result = BorderWidth.valueOf(name);
      } catch (IllegalArgumentException ex) {
        // do noting
      }
      return result;
    } finally {
      this.locker.unlock();
    }
  }

  public void setBorderWidth(final BorderWidth value) {
    this.locker.lock();
    try {
      preferences.put(Option.BORDER_WIDTH.name(), value.name());
    } finally {
      this.locker.unlock();
    }
  }

  public String getCustomRomPath() {
    this.locker.lock();
    try {
      return preferences.get(Option.CUSTOM_ROM_PATH.name(), null);
    } finally {
      this.locker.unlock();
    }
  }

  public void setCustomRomPath(final String path) {
    this.locker.lock();
    try {
      if (path == null || path.trim().isEmpty()) {
        preferences.remove(Option.CUSTOM_ROM_PATH.name());
      } else {
        preferences.put(Option.CUSTOM_ROM_PATH.name(), path);
      }
    } finally {
      this.locker.unlock();
    }
  }

  public String getAddress() {
    this.locker.lock();
    try {
      return preferences
          .get(Option.STREAM_ADDR.name(), InetAddress.getLoopbackAddress().getHostAddress());
    } finally {
      this.locker.unlock();
    }
  }

  public void setAddress(final String address) {
    this.locker.lock();
    try {
      preferences.put(Option.STREAM_ADDR.name(),
          address == null ? InetAddress.getLoopbackAddress().getHostAddress() : address);
    } finally {
      this.locker.unlock();
    }
  }

  public int getPort() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.STREAM_PORT.name(), 0);
    } finally {
      this.locker.unlock();
    }
  }

  public void setPort(final int port) {
    this.locker.lock();
    try {
      preferences.putInt(Option.STREAM_PORT.name(), port & 0xFFFF);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isTurboSound() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.TURBOSOUND.name(), false);
    } finally {
      this.locker.unlock();
    }
  }

  public void setTurboSound(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.TURBOSOUND.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isSoundTurnedOn() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.SOUND_TURNED_ON.name(), false);
    } finally {
      this.locker.unlock();
    }
  }

  public void setSoundTurnedOn(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.SOUND_TURNED_ON.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isInterlacedScan() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.INTERLACED_SCAN.name(), true);
    } finally {
      this.locker.unlock();
    }
  }

  public void setInterlacedScan(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.INTERLACED_SCAN.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isTryLessResources() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.TRY_LESS_RESOURCES.name(), false);
    } finally {
      this.locker.unlock();
    }
  }

  public void setTryLessResources(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.TRY_LESS_RESOURCES.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public int getFrameRate() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.STREAM_FRAMERATE.name(), 25);
    } finally {
      this.locker.unlock();
    }
  }

  public void setFrameRate(final int value) {
    this.locker.lock();
    try {
      preferences.putInt(Option.STREAM_FRAMERATE.name(), Math.max(1, Math.min(50, value)));
    } finally {
      this.locker.unlock();
    }
  }

  public String getFfmpegPath() {
    this.locker.lock();
    try {
      return preferences
          .get(Option.STREAM_FFMPEGPATH.name(),
              SystemUtils.IS_OS_WINDOWS ? "ffmpeg.exe" : "ffmpeg");
    } finally {
      this.locker.unlock();
    }
  }

  public void setFfmpegPath(final String path) {
    this.locker.lock();
    try {
      preferences.put(Option.STREAM_FFMPEGPATH.name(),
          path == null ? (SystemUtils.IS_OS_WINDOWS ? "ffmpeg.exe" : "ffmpeg") : path);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isGrabSound() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.STREAM_GRABSOUND.name(), false);
    } finally {
      this.locker.unlock();
    }
  }

  public void setGrabSound(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.STREAM_GRABSOUND.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public TimingProfile getTimingProfile() {
    this.locker.lock();
    try {
      final String timing =
          preferences.get(Option.TIMING_PROFILE.name(), TimingProfile.PENTAGON128.name());
      try {
        return TimingProfile.valueOf(timing);
      } catch (IllegalArgumentException ex) {
        return TimingProfile.PENTAGON128;
      }
    } finally {
      this.locker.unlock();
    }
  }

  public void setTimingProfile(final TimingProfile value) {
    this.locker.lock();
    try {
      preferences.put(Option.TIMING_PROFILE.name(), value.name());
    } finally {
      this.locker.unlock();
    }
  }

  public BoardMode getDefaultBoardMode() {
    this.locker.lock();
    try {
      final String mode = preferences.get(Option.DEFAULT_MODE.name(), BoardMode.ZXPOLY.name());
      try {
        return BoardMode.valueOf(mode);
      } catch (IllegalArgumentException ex) {
        return BoardMode.ZXPOLY;
      }
    } finally {
      this.locker.unlock();
    }
  }

  public void setDefaultBoardMode(final BoardMode value) {
    this.locker.lock();
    try {
      preferences.put(Option.DEFAULT_MODE.name(), value.name());
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isVkbdApart() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.VKBD_APART.name(), false);
    } finally {
      this.locker.unlock();
    }
  }

  public void setVkbdApart(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.VKBD_APART.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isAttributePortFf() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.ATTRIBUTE_PORT_FF.name(), true);
    } finally {
      this.locker.unlock();
    }
  }

  public void setAttributePortFf(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.ATTRIBUTE_PORT_FF.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public String getUiLfClass() {
    this.locker.lock();
    try {
      return preferences.get(Option.UI_LF_CLASS.name(), UIManager.getSystemLookAndFeelClassName());
    } finally {
      this.locker.unlock();
    }
  }

  public void setUiLfClass(final String className) {
    this.locker.lock();
    try {
      if (className == null) {
        this.preferences.remove(Option.UI_LF_CLASS.name());
      } else {
        this.preferences.put(Option.UI_LF_CLASS.name(), className);
      }
    } finally {
      this.locker.unlock();
    }
  }

  public String getUiScale() {
    this.locker.lock();
    try {
      return preferences.get(Option.UI_SCALE.name(), null);
    } finally {
      this.locker.unlock();
    }
  }

  public void setUiScale(final String uiScale) {
    this.locker.lock();
    try {
      if (uiScale == null) {
        this.preferences.remove(Option.UI_SCALE.name());
      } else {
        this.preferences.put(Option.UI_SCALE.name(), uiScale);
      }
    } finally {
      this.locker.unlock();
    }
  }

  public List<FastButton> getFastButtons() {
    this.locker.lock();
    try {
      final String buttons = preferences.get(Option.FAST_BUTTONS.name(), null);
      if (buttons == null) {
        return List.of();
      } else {
        return Arrays.stream(buttons.split(","))
            .flatMap(x -> {
              try {
                return Stream.of(FastButton.valueOf(x.trim().toUpperCase(Locale.ENGLISH)));
              } catch (IllegalArgumentException ex) {
                return Stream.empty();
              }
            })
            .filter(FastButton::isOptional)
            .distinct()
            .collect(Collectors.toList());
      }
    } finally {
      this.locker.unlock();
    }
  }

  public void setFastButtons(final List<FastButton> fastButtons) {
    this.locker.lock();
    try {
      final String packetValue = fastButtons.stream()
          .filter(FastButton::isOptional)
          .distinct()
          .map(FastButton::name)
          .collect(Collectors.joining(","));
      this.preferences.put(Option.FAST_BUTTONS.name(), packetValue);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isCovoxFb() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.COVOXFB.name(), false);
    } finally {
      this.locker.unlock();
    }
  }

  public void setCovoxFb(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.COVOXFB.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isSyncPaint() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.SYNC_PAINT.name(), false);
    } finally {
      this.locker.unlock();
    }
  }

  public void setSyncPaint(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.SYNC_PAINT.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isShowIndicatorPanel() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.SHOW_INDICATOR_PANEL.name(), true);
    } finally {
      this.locker.unlock();
    }
  }

  public void setShowIndicatorPanel(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.SHOW_INDICATOR_PANEL.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isSoundChannelsACB() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.SOUND_CHANNELS_ACB.name(), false);
    } finally {
      this.locker.unlock();
    }
  }

  public void setSoundChannelsACB(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.SOUND_CHANNELS_ACB.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public boolean isKempstonMouseAllowed() {
    this.locker.lock();
    try {
      return preferences.getBoolean(Option.KEMPSTON_MOUSE_ALLOWED.name(), true);
    } finally {
      this.locker.unlock();
    }
  }

  public void setKempstonMouseAllowed(final boolean value) {
    this.locker.lock();
    try {
      preferences.putBoolean(Option.KEMPSTON_MOUSE_ALLOWED.name(), value);
    } finally {
      this.locker.unlock();
    }
  }

  public int getIntBetweenFrames() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.INTBETWEENFRAMES.name(), 2);
    } finally {
      this.locker.unlock();
    }
  }

  public void setIntBetweenFrames(final int value) {
    this.locker.lock();
    try {
      preferences.putInt(Option.INTBETWEENFRAMES.name(), Math.max(0, value));
    } finally {
      this.locker.unlock();
    }
  }

  public int getCursorJoystickDown() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.PROTEK_JOYSTICK_VK_DOWN.name(), KeyEvent.VK_DOWN);
    } finally {
      this.locker.unlock();
    }
  }

  public void setProtekJoystickDown(final int keyCode) {
    this.locker.lock();
    try {
      preferences.putInt(Option.PROTEK_JOYSTICK_VK_DOWN.name(), keyCode);
    } finally {
      this.locker.unlock();
    }
  }

  public int getProtekJoystickUp() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.PROTEK_JOYSTICK_VK_UP.name(), KeyEvent.VK_UP);
    } finally {
      this.locker.unlock();
    }
  }

  public void setProtekJoystickUp(final int keyCode) {
    this.locker.lock();
    try {
      preferences.putInt(Option.PROTEK_JOYSTICK_VK_UP.name(), keyCode);
    } finally {
      this.locker.unlock();
    }
  }

  public int getProtekJoystickLeft() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.PROTEK_JOYSTICK_VK_LEFT.name(), KeyEvent.VK_LEFT);
    } finally {
      this.locker.unlock();
    }
  }

  public void setProtekJoystickLeft(final int keyCode) {
    this.locker.lock();
    try {
      preferences.putInt(Option.PROTEK_JOYSTICK_VK_LEFT.name(), keyCode);
    } finally {
      this.locker.unlock();
    }
  }

  public int getProtekJoystickRight() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.PROTEK_JOYSTICK_VK_RIGHT.name(), KeyEvent.VK_RIGHT);
    } finally {
      this.locker.unlock();
    }
  }

  public void setProtekJoystickRight(final int keyCode) {
    this.locker.lock();
    try {
      preferences.putInt(Option.PROTEK_JOYSTICK_VK_RIGHT.name(), keyCode);
    } finally {
      this.locker.unlock();
    }
  }

  public int getProtekJoystickFire() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.PROTEK_JOYSTICK_VK_FIRE.name(), KeyEvent.VK_TAB);
    } finally {
      this.locker.unlock();
    }
  }

  public void setProtekJoystickFire(final int keyCode) {
    this.locker.lock();
    try {
      preferences.putInt(Option.PROTEK_JOYSTICK_VK_FIRE.name(), keyCode);
    } finally {
      this.locker.unlock();
    }
  }

  public int getKempstonVkLeft() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.KEMPSTON_VK_LEFT.name(), KeyEvent.VK_NUMPAD4);
    } finally {
      this.locker.unlock();
    }
  }

  public void setKempstonVkLeft(final int keyCode) {
    this.locker.lock();
    try {
      preferences.putInt(Option.KEMPSTON_VK_LEFT.name(), keyCode);
    } finally {
      this.locker.unlock();
    }
  }

  public int getKempstonVkRight() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.KEMPSTON_VK_RIGHT.name(), KeyEvent.VK_NUMPAD6);
    } finally {
      this.locker.unlock();
    }
  }

  public void setKempstonVkRight(final int keyCode) {
    this.locker.lock();
    try {
      preferences.putInt(Option.KEMPSTON_VK_RIGHT.name(), keyCode);
    } finally {
      this.locker.unlock();
    }
  }

  public int getKempstonVkUp() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.KEMPSTON_VK_UP.name(), KeyEvent.VK_NUMPAD8);
    } finally {
      this.locker.unlock();
    }
  }

  public void setKempstonVkUp(final int keyCode) {
    this.locker.lock();
    try {
      preferences.putInt(Option.KEMPSTON_VK_UP.name(), keyCode);
    } finally {
      this.locker.unlock();
    }
  }

  public int getKempstonVkDown() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.KEMPSTON_VK_DOWN.name(), KeyEvent.VK_NUMPAD2);
    } finally {
      this.locker.unlock();
    }
  }

  public void setKempstonVkDown(final int keyCode) {
    this.locker.lock();
    try {
      preferences.putInt(Option.KEMPSTON_VK_DOWN.name(), keyCode);
    } finally {
      this.locker.unlock();
    }
  }

  public int getKempstonVkFire() {
    this.locker.lock();
    try {
      return preferences.getInt(Option.KEMPSTON_VK_FIRE.name(), KeyEvent.VK_NUMPAD5);
    } finally {
      this.locker.unlock();
    }
  }

  public void setKempstonVkFire(final int keyCode) {
    this.locker.lock();
    try {
      preferences.putInt(Option.KEMPSTON_VK_FIRE.name(), keyCode);
    } finally {
      this.locker.unlock();
    }
  }

  public void flush() throws BackingStoreException {
    this.locker.lock();
    try {
      preferences.flush();
    } finally {
      this.locker.unlock();
    }
  }

  public File getAppConfigFolder() {
    this.locker.lock();
    try {
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
    } finally {
      this.locker.unlock();
    }
  }

  public File getRomCacheFolder() {
    return new File(this.getAppConfigFolder(), "cache");
  }

  public enum Option {
    BORDER_WIDTH,
    TIMING_PROFILE,
    SYNC_PAINT,
    SHOW_INDICATOR_PANEL,
    FAST_BUTTONS,
    VOLUME_PROFILE,
    UI_LF_CLASS,
    UI_SCALE,
    OLD_COLOR_TV_ON_START,
    INTERLACED_SCAN,
    AUTOCS_FOR_CURSOR_KEYS,
    CUSTOM_ROM_PATH,
    STREAM_FFMPEGPATH,
    STREAM_GRABSOUND,
    STREAM_ADDR,
    STREAM_PORT,
    STREAM_FRAMERATE,
    DEFAULT_MODE,
    SOUND_CHANNELS_ACB,
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
    PROTEK_JOYSTICK_VK_FIRE,
    ATTRIBUTE_PORT_FF,
    ULAPLUS,
    TRY_LESS_RESOURCES
  }

}
