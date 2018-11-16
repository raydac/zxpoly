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
package com.igormaznitsa.zxpspritecorrector.files.plugins;

import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.FileNameDialog;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;
import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;

public class SCRPlugin extends AbstractFilePlugin {

  public SCRPlugin() {
    super();
  }

  @Override
  public String getPluginUID() {
    return "SCRP";
  }

  @Override
  public String getToolTip(final boolean forExport) {
    return "A ZX-Spectrum Screen file";
  }

  @Override
  public boolean doesImportContainInsideFileList() {
    return false;
  }

  @Override
  public List<Info> getImportingContainerFileList(final File file) {
    return null;
  }

  @Override
  public String getImportingFileInfo(File file) {
    return "  A ZX-Spectrum scrren  \n  256x192  ";
  }

  @Override
  public ReadResult readFrom(final File file, final int index) throws IOException {
    final byte[] wholeFile = FileUtils.readFileToByteArray(file);
    return new ReadResult(new ZXPolyData(new Info(file.getName(), '$', 16384, wholeFile.length, 0), this, wholeFile), null);
  }

  @Override
  public void writeTo(final File file, final ZXPolyData data, final SessionData sessionData) throws IOException {
    final FileNameDialog dialog = new FileNameDialog(this.mainFrame, "Base file name is " + file.getName(), FileNameDialog.makeFileNames(file.getName()), null, null);
    dialog.setVisible(true);
    if (dialog.approved()) {
      final String[] fileNames = dialog.getFileName();
      for (int i = 0; i < fileNames.length; i++) {
        final String fileName = fileNames[i];
        if (fileName == null) {
          continue;
        }
        final File newfile = new File(file.getParentFile(), fileName);
        if (!saveDataToFile(newfile, data.getDataForCPU(i))) {
          break;
        }
      }
    }
  }

  @Override
  public boolean accept(final File pathname) {
    return pathname != null && (pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".scr"));
  }

  @Override
  public javax.swing.filechooser.FileFilter getImportFileFilter() {
    return this;
  }

  @Override
  public javax.swing.filechooser.FileFilter getExportFileFilter() {
    return this;
  }

  @Override
  public String getExtension(final boolean forExport) {
    return "scr";
  }

  @Override
  public String getDescription() {
    return getToolTip(false) + " (*.SCR)";
  }

  @Override
  public String getPluginDescription(final boolean forExport) {
    return "SCR file";
  }

}
