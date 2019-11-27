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

import static com.igormaznitsa.zxpoly.components.ZxPolyConstants.PORTw_ZXPOLY_BLOCK;
import static java.util.Arrays.stream;


import com.igormaznitsa.zxpoly.components.BoardMode;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.VideoController;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;
import javax.swing.filechooser.FileFilter;

public abstract class Snapshot extends FileFilter {

  protected static final Logger LOGGER = Logger.getLogger("SNPSHT");

  public void doMode48(final Motherboard board) {
    LOGGER.info("Mode ZX-48");
    board.set3D00(PORTw_ZXPOLY_BLOCK, true);
    stream(board.getModules()).forEach(ZxPolyModule::lockZx48Mode);
    board.setBoardMode(BoardMode.ZX128, false);
    board.getMasterCpu().doReset();
  }

  public void doMode128(final Motherboard board) {
    LOGGER.info("Mode ZX-128");
    board.set3D00(PORTw_ZXPOLY_BLOCK, true);
    board.setBoardMode(BoardMode.ZX128, false);
    board.getMasterCpu().doReset();
  }

  public void doModeSpec256_128(final Motherboard board) {
    LOGGER.info("Mode Spec256.128");
    board.set3D00(PORTw_ZXPOLY_BLOCK, true);
    board.setBoardMode(BoardMode.SPEC256, false);
    board.getMasterCpu().doReset();
  }

  public void doModeSpec256_48(final Motherboard board) {
    LOGGER.info("Mode Spec256.48");
    board.set3D00(PORTw_ZXPOLY_BLOCK, true);
    stream(board.getModules()).forEach(ZxPolyModule::lockZx48Mode);
    board.setBoardMode(BoardMode.SPEC256, false);
    board.getMasterCpu().doReset();
  }

  public void doZxPoly(final Motherboard board) {
    LOGGER.info("Mode ZX-Poly");
    board.set3D00(0, true);
    board.setBoardMode(BoardMode.ZXPOLY, false);
    board.getMasterCpu().doReset();
  }

  public abstract String getExtension();

  public abstract void loadFromArray(File srcFile, Motherboard board, VideoController vc, byte[] array) throws IOException;

  public abstract byte[] saveToArray(Motherboard board, VideoController vc) throws IOException;

  public abstract String getName();
}
