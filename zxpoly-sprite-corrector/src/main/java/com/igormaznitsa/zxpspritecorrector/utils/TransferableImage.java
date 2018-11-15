/*
 * Copyright (C) 2018 Raydac Research Group Ltd.
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
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.image.RenderedImage;
import java.io.IOException;

public final class TransferableImage implements Transferable {
  
  final Image image;

  public TransferableImage(final Image image) {
    this.image = image;
  }

  @Override
  public Object getTransferData(DataFlavor flavor)
          throws UnsupportedFlavorException, IOException {
    if (flavor.equals(DataFlavor.imageFlavor) && image != null) {
      return image;
    } else {
      throw new UnsupportedFlavorException(flavor);
    }
  }

  @Override
  public DataFlavor[] getTransferDataFlavors() {
    DataFlavor[] flavors = new DataFlavor[1];
    flavors[0] = DataFlavor.imageFlavor;
    return flavors;
  }

  @Override
  public boolean isDataFlavorSupported(final DataFlavor flavor) {
    DataFlavor[] flavors = getTransferDataFlavors();
    for (int i = 0; i < flavors.length; i++) {
      if (flavor.equals(flavors[i])) {
        return true;
      }
    }

    return false;
  }
  
  public void toClipboard(){
    Clipboard c = Toolkit.getDefaultToolkit().getSystemClipboard();
    c.setContents(this, null);

  }
}
