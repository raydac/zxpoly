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

package com.igormaznitsa.zxpspritecorrector.files.plugins;

import com.igormaznitsa.zxpspritecorrector.MainFrame;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;
import java.io.File;
import java.io.IOException;
import java.util.List;
import javax.swing.JOptionPane;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.picocontainer.annotations.Inject;

public abstract class AbstractFilePlugin extends FileFilter {

  @Inject
  protected MainFrame mainFrame;

  public AbstractFilePlugin() {
    super();
  }

  protected static String prepareNameForTRD(final String name, final int index) {
    String prepared;
    if (name.length() >= 8) {
      prepared = name.substring(0, 7);
    } else {
      prepared = name + "_______".substring(0, 7 - name.length());
    }
    return prepared + index;
  }

  protected static String addNumberToFileName(final String name, final int number) {
    String base = FilenameUtils.getBaseName(name);
    final String ext = FilenameUtils.getExtension(name);
    return base + number + '.' + ext;
  }

  protected static String prepareNameForTAP(final String name, final int index) {
    String prepared;
    if (name.length() >= 10) {
      prepared = name.substring(0, 9);
    } else {
      prepared = name + "_________".substring(0, 9 - name.length());
    }
    return prepared + index;
  }

  public boolean allowsExport() {
    return true;
  }

  public abstract String getToolTip(boolean forExport);

  public abstract boolean doesImportContainInsideFileList();

  public abstract FileFilter getImportFileFilter();

  public abstract FileFilter getExportFileFilter();

  public abstract String getPluginDescription(boolean forExport);

  public String getImportingFileInfo(File file) {
    return "";
  }

  public abstract String getPluginUID();

  public abstract List<Info> getImportingContainerFileList(File file);

  public abstract String getExtension(boolean forExport);

  public abstract ReadResult readFrom(File file, int index) throws IOException;

  public abstract void writeTo(File file, ZXPolyData data, SessionData sessionData) throws IOException;

  protected boolean saveDataToFile(final File file, final byte[] data) throws IOException {
    if (file.isFile()) {
      switch (JOptionPane.showConfirmDialog(this.mainFrame, "Overwrite file '" + file.getAbsolutePath() + "'?", "Overwrite file", JOptionPane.YES_NO_CANCEL_OPTION)) {
        case JOptionPane.NO_OPTION:
          return true;
        case JOptionPane.CANCEL_OPTION:
          return false;
      }
    }
    FileUtils.writeByteArrayToFile(file, data);
    return true;
  }

  public final static class ReadResult {

    private final ZXPolyData data;
    private final SessionData session;

    public ReadResult(final ZXPolyData data, final SessionData session) {
      this.data = data;
      this.session = session;
    }

    public ZXPolyData getData() {
      return this.data;
    }

    public SessionData getSessionData() {
      return this.session;
    }

  }
}
