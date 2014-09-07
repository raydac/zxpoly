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

import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import java.io.*;
import java.util.*;
import org.apache.commons.io.*;
import org.picocontainer.PicoContainer;
import org.picocontainer.annotations.Inject;

public class SZEPlugin extends AbstractFilePlugin {
  @Inject
  private PicoContainer context;
  
  @Override
  public int getUID() {
    return ((int) 'S' << 24) | ((int) 'Z' << 16) | ((int) 'E' << 8) | (int) ' ';
  }
  
  public SZEPlugin(){
    super();
  }

  @Override
  public String getName() {
    return "ZX-Poly Sprite corrector files";
  }

  @Override
  public String getToolTip() {
    return "A ZX-Poly Sprite corrector data file format";
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
  public String getFileInfo(final File file) {
    try {
      FileInputStream in = null;
      try {
        in = new FileInputStream(file);
        final ZXPolyData zxpoly = new ZXPolyData(in,this.context.getComponents(AbstractFilePlugin.class));
        
        final StringBuilder result = new StringBuilder();
        result.append("Name : "+result);
        
        return result.toString();
      }
      finally {
        JBBPUtils.closeQuietly(in);
      }
    }
    catch (Exception ex) {
      return null;
    }
  }
  
  @Override
  public ZXPolyData readFrom(final File file, final int index) throws IOException {
    FileInputStream inStream = null;
    try{
      inStream = new FileInputStream(file);
      return new ZXPolyData(inStream, this.context.getComponents(AbstractFilePlugin.class));
    }finally{
      IOUtils.closeQuietly(inStream);
    }
  }

  @Override
  public void writeTo(final File file, final ZXPolyData data) throws IOException {
    FileUtils.writeByteArrayToFile(file, data.getAsArray());
  }

  @Override
  public boolean accept(final File pathname) {
    return pathname!= null && pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".sze");
  }

  @Override
  public String getDescription() {
    return getToolTip()+" (*.SZE)";
  }

}
