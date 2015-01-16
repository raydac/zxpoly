/*
 * Copyright (C) 2014 Raydac Research Group Ltd.
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

import com.igormaznitsa.zxpoly.components.VideoController;
import com.igormaznitsa.zxpoly.components.ZXPolyModule;
import java.io.IOException;
import javax.swing.filechooser.FileFilter;

public abstract class Snapshot extends FileFilter {
  /**
   * Fille a cpu module by data. 
   * @param module the module, must not be null.
   * @param vc the video controller, must not be null
   */
  public abstract void fillModule(final ZXPolyModule module, final VideoController vc);
  /**
   * Parse array.
   * @param array the array to be parsed, must not be null
   * @return true if the system must be locked in mode 48, false for 128 mode
   * @throws IOException it will be thrown for parsing problems
   */
  public abstract boolean load(final byte [] array) throws IOException; 
  public abstract String getName();
}
