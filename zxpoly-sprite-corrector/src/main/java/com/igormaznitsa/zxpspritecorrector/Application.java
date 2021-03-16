/*
 * Copyright (C) 2019 Igor Maznitsa
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

package com.igormaznitsa.zxpspritecorrector;

import com.igormaznitsa.zxpspritecorrector.cmdline.SliceImageCmd;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import java.awt.*;
import java.util.Arrays;
import java.util.Locale;

public class Application {

  public static void main(final String[] args) {
    if (args.length > 0) {
      if ("sliceImage".equals(args[0])) {
        System.exit(new SliceImageCmd().process(Arrays.copyOfRange(args, 1, args.length)));
      } else {
        System.err.println("Unexpected command : " + args[0]);
        System.exit(1);
      }
    }

    java.util.Locale.setDefault(Locale.ROOT);

    try {
      for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    SwingUtilities.invokeLater(() -> {
      new SpriteCorrectorMainFrame(GraphicsEnvironment.getLocalGraphicsEnvironment().getDefaultScreenDevice().getDefaultConfiguration(),
              true).setVisible(true);
    });

  }
}
