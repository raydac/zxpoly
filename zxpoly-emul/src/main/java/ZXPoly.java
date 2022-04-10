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

import com.igormaznitsa.zxpoly.MainForm;
import com.igormaznitsa.zxpoly.utils.AppOptions;
import org.apache.commons.lang3.SystemUtils;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class ZXPoly {

  public static final String APP_TITLE = "ZX-Poly emulator";
  public static final String APP_VERSION = "v 2.3.0";

  private static void setGnomeAppTitle() {
    try {
      final Toolkit toolkit = Toolkit.getDefaultToolkit();
      final Field awtAppClassNameField = toolkit.getClass().getDeclaredField("awtAppClassName");
      awtAppClassNameField.setAccessible(true);
      awtAppClassNameField.set(toolkit, ZXPoly.APP_TITLE);
    } catch (Exception ex) {
      //Do nothing
    }
  }

  public static void main(final String... args) {
    final String uiScale = AppOptions.getInstance().getUiScale();
    if (uiScale != null) {
      if (System.getProperty("sun.java2d.uiScale", null) == null) {
        System.out.println("Detected scale UI: " + uiScale);
        System.setProperty("sun.java2d.uiScale", uiScale.trim() + 'x');
        System.setProperty("sun.java2d.uiScale.enabled", "true");
      } else {
        System.out.println("Detected provided sun.java2d.uiScale property: " + System.getProperty("sun.java2d.uiScale", null));
      }
    }

    for (final Handler h : Logger.getLogger("").getHandlers()) {
      h.setFormatter(new Formatter() {

        @Override
        public String format(final LogRecord record) {
          return record.getLevel() + " [" + record.getLoggerName() + "] : " + record.getMessage() + '\n';
        }
      });
    }

    if (SystemUtils.IS_OS_LINUX) {
      setGnomeAppTitle();
    }

    SwingUtilities.invokeLater(() -> {
      final MainForm form;

      final String uiLfClass = AppOptions.getInstance().getUiLfClass();
      try {
        UIManager.setLookAndFeel(uiLfClass);
      } catch (Exception ex) {
        System.err.println("Can't select L&F: " + uiLfClass);
      }

      try {
        String romPath = AppOptions.getInstance().getCustomRomPath();
        if (romPath == null) {
          romPath = System.getProperty("zxpoly.rom.path", AppOptions.getInstance().getActiveRom());
        } else {
          System.out.println("Custom ROM path in use: " + romPath);
        }
        form = new MainForm(APP_TITLE + ' ' + APP_VERSION, romPath);
      } catch (Exception ex) {
        ex.printStackTrace();
        System.exit(1);
        return;
      }
      form.setVisible(true);
    });
  }
}
