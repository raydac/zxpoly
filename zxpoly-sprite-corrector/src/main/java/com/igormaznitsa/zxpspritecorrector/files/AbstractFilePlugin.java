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

import com.igormaznitsa.zxpspritecorrector.MainFrame;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import java.io.*;
import java.util.List;
import javax.swing.filechooser.FileFilter;
import org.picocontainer.annotations.Inject;

public abstract class AbstractFilePlugin extends FileFilter {
  @Inject
  protected MainFrame mainFrame;
  
  public AbstractFilePlugin(){
    super();
  }

  public abstract String getName();
  public abstract String getToolTip();
  public abstract boolean hasInsideFileList();
  
  public String getFileInfo(File file){
    return "";
  }
  
  public abstract int getUID();
  
  public abstract List<Info> getInsideFileList(File file);

  public abstract ZXPolyData readFrom(File file, int index) throws IOException;
  public abstract void writeTo(File file, ZXPolyData data) throws IOException;
}
