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

package com.igormaznitsa.zxpspritecorrector.utils;

import java.awt.Image;
import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import javax.imageio.ImageIO;

public final class GfxUtils {

  private GfxUtils() {
  }

  public static Image loadImage(final String iconFile) {
    try {
      return ImageIO.read(ClassLoader
          .getSystemResourceAsStream("com/igormaznitsa/zxpspritecorrector/icons/" + iconFile));
    } catch (Exception ex) {
      ex.printStackTrace();
    }
    return null;
  }

  public static boolean doesClipboardHasImage() {
    final Transferable transferable =
        Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
    return transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor);
  }

  public static Image getImageFromClipboard() {
    final Transferable transferable =
        Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);
    if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.imageFlavor)) {
      try {
        return (Image) transferable.getTransferData(DataFlavor.imageFlavor);
      } catch (UnsupportedFlavorException | IOException e) {
        e.printStackTrace();
      }
    }
    return null;
  }
}
