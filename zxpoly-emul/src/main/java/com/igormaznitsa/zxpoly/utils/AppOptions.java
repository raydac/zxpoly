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
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import org.apache.commons.io.FileUtils;

public final class AppOptions {

  public static final String TEST_ROM = "zxpolytest.rom";
  private static final String ROMPATH = "ROM_PATH";
  private static final String INT_BETWEEN_FRAMES = "INT_BETWEEN_FRAMES";
  private static final AppOptions INSTANCE = new AppOptions();
  private static final String APP_FOLDER_NAME = ".zxpolyemul";
  private final Preferences preferences = Preferences.userNodeForPackage(AppOptions.class);

  public static AppOptions getInstance() {
    return INSTANCE;
  }

  public synchronized boolean isTestRomActive() {
    return TEST_ROM.equals(this.getActiveRom());
  }

  public synchronized String getActiveRom() {
    return preferences.get(ROMPATH, AppOptions.TEST_ROM);
  }

  public synchronized void setActiveRom(final String romPath) {
    preferences.put(ROMPATH, romPath);
  }

  public synchronized int getIntBetweenFrames() {
    return preferences.getInt(INT_BETWEEN_FRAMES, 3);
  }

  public synchronized void setIntBetweenFrames(final int value) {
    preferences.putInt(INT_BETWEEN_FRAMES, Math.max(0, value));
  }

  public synchronized void flush() throws BackingStoreException {
    preferences.flush();
  }

  public synchronized File getAppConfigFolder() {
    String folder = System.getenv("APPDATA");
    if (folder == null) {
      folder = System.getProperty("user.home", FileUtils.getTempDirectoryPath());
    }

    final File cfgfolder = new File(folder, APP_FOLDER_NAME);
    if (!cfgfolder.exists()) {
      cfgfolder.mkdirs();
    }
    return cfgfolder;
  }

}
