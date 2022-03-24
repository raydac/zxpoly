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

package com.igormaznitsa.zxpoly.formats;

import com.igormaznitsa.zxpoly.components.BoardMode;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.RomData;
import com.igormaznitsa.zxpoly.components.video.VideoController;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class FormatPRom extends ArrayUnsaveable {

  @Override
  public void loadFromArray(final File srcFile, final Motherboard board, final VideoController vc,
                            final byte[] array) throws IOException {
    if (srcFile.length() > 0x40000L) {
      throw new IOException("Too big PROM file");
    }

    final byte[] data;
    try (final InputStream inputStream = new FileInputStream(srcFile)) {
      data = IOUtils.readFully(inputStream, (int) srcFile.length());
    }

    final List<RomData> roms = new ArrayList<>();
    for (int index = 0; index < 4 && index * 0x4000 < data.length; index++) {
      final int pos = index * 0x4000;
      final byte[] portion = new byte[Math.min(0x4000, data.length - pos)];
      System.arraycopy(data, pos, portion, 0, portion.length);
      roms.add(new RomData(srcFile.getName() + '#' + index, portion));
      LOGGER.info("PROM part " + index + " length " + portion.length + " bytes");
    }

    for (int i = 0; i < 4; i++) {
      final RomData romData = roms.get(i % roms.size());
      LOGGER.info("set PROM for module " + i + ": " + romData.getSource());
      board.getModules()[i].setRomData(romData);
    }
    board.setBoardMode(BoardMode.ZXPOLY, true);
  }

  @Override
  public boolean canMakeSnapshotForBoardMode(final BoardMode mode) {
    return false;
  }

  @Override
  public String getExtension() {
    return "prom";
  }

//  @Override
//  public byte[] saveToArray(Motherboard board, VideoController vc) throws IOException {
//    throw new IOException("Unsupported board mode: " + board.getBoardMode());
//  }

  @Override
  public String getName() {
    return "ZX-Poly ROM snapshot";
  }

  @Override
  public boolean accept(final File f) {
    return f != null
            && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".prom"));
  }

  @Override
  public String getDescription() {
    return "ZX-Poly ROM image (*.prom)";
  }

}
