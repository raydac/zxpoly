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

import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;
import java.io.*;
import java.util.*;
import org.apache.commons.io.*;
import org.picocontainer.PicoContainer;
import org.picocontainer.annotations.Inject;

public class SZEPlugin extends AbstractFilePlugin {
  @Inject
  private PicoContainer context;
  
  @Override
  public String getPluginUID() {
    return "SZEP";
  }
  
  public SZEPlugin(){
    super();
  }

  @Override
  public boolean allowsExport() {
    return false;
  }

  @Override
  public String getToolTip(final boolean forExport) {
    return "ZX-Poly Sprite corrector session";
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
  public String getImportingFileInfo(final File file) {
    try {
      FileInputStream in = null;
      try {
        in = new FileInputStream(file);
        final ZXPolyData zxpoly = new ZXPolyData(in,this.context.getComponents(AbstractFilePlugin.class));
        
        final StringBuilder result = new StringBuilder();
        result.append("  Name : ").append(zxpoly.getInfo().getName()).append('\n');
        result.append("  Type : ").append(zxpoly.getInfo().getType()).append('\n');
        result.append("Length : ").append(zxpoly.getInfo().getLength()).append('\n');
        result.append("Plugin : ").append(zxpoly.getPlugin().getPluginDescription(false));
        
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
  public String getPluginDescription(final boolean forExport) {
    return "SZE file";
  }
  
  @Override
  public ReadResult readFrom(final File file, final int index) throws IOException {
    FileInputStream inStream = null;
    try{
      inStream = new FileInputStream(file);
      final ZXPolyData zxpolyData = new ZXPolyData(inStream, this.context.getComponents(AbstractFilePlugin.class));
      final JBBPBitInputStream in = new JBBPBitInputStream(inStream);
      final int length = in.readInt(JBBPByteOrder.BIG_ENDIAN);
      return new ReadResult(zxpolyData, new SessionData(in));
    }finally{
      IOUtils.closeQuietly(inStream);
    }
  }

  @Override
  public void writeTo(final File file, final ZXPolyData data, final SessionData sessionData) throws IOException {
    final byte [] dataarray = data.getAsArray();
    final byte [] sessionarray = sessionData.makeArray();
    
    final byte [] result = JBBPOut.BeginBin().Byte(dataarray).Int(sessionarray.length).Byte(sessionarray).End().toByteArray();
    
    saveDataToFile(file, result);
  }

  @Override
  public boolean accept(final File pathname) {
    return pathname!= null && pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".sze");
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
  public String getExtension(boolean forExport) {
    return "sze";
  }

  @Override
  public String getDescription() {
    return getToolTip(false)+" (*.SZE)";
  }

}
