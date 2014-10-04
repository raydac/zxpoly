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
package com.igormaznitsa.zxpspritecorrector.files;

import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;

public class SCRPlugin extends AbstractFilePlugin {

  public SCRPlugin() {
    super();
  }

  @Override
  public int getUID() {
    return ((int) 'S' << 24) | ((int) 'C' << 16) | ((int) 'R' << 8) | (int) ' ';
  }
  
  @Override
  public String getName() {
    return "SCR files";
  }

  @Override
  public String getToolTip() {
    return "A ZX-Spectrum Screen file";
  }

  @Override
  public boolean hasInsideFileList() {
    return false;
  }

  @Override
  public List<Info> getInsideFileList(final File file) {
    return null;
  }

  @Override
  public String getFileInfo(File file) {
    return "  A ZX-Spectrum scrren  \n  256x192  ";
  }

  @Override
  public ZXPolyData readFrom(final File file, final int index) throws IOException {
    final byte[] wholeFile = FileUtils.readFileToByteArray(file);
    return new ZXPolyData(new Info(file.getName(), 'C', 16384, wholeFile.length), this, wholeFile);
  }

  @Override
  public void writeTo(final File file, final ZXPolyData data) throws IOException {
  }

  @Override
  public boolean accept(final File pathname) {
    return pathname != null && pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".scr");
  }

  @Override
  public String getDescription() {
    return getToolTip() + " (*.SCR)";
  }

}
