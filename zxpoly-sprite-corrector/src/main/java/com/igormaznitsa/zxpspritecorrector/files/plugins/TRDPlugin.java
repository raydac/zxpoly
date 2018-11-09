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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.FileNameDialog;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;
import java.io.*;
import java.util.*;

public class TRDPlugin extends AbstractFilePlugin {

  public static final JBBPParser CATALOG_PARSER = JBBPParser.prepare("byte [8] name; ubyte type; <ushort start; <ushort length; ubyte sectors; ubyte firstSector; ubyte track;");
  
  private static final class TRDosCatalogItem {
    @Bin(type = BinType.BYTE_ARRAY) String name;
    @Bin(type = BinType.UBYTE) char type;
    @Bin(type = BinType.USHORT) int start;
    @Bin(type = BinType.USHORT) int length;
    @Bin(type = BinType.UBYTE) int sectors;
    @Bin(type = BinType.UBYTE) int firstSector;
    @Bin(type = BinType.UBYTE) int track;
  }
  
  public TRDPlugin(){
    super();
  }

  @Override
  public String getPluginDescription(final boolean forExport) {
    return "TRD file";
  }


  @Override
  public String getToolTip(final boolean forExport) {
    return "A TR-DOS disk image format";
  }

  @Override
  public boolean doesImportContainInsideFileList() {
    return true;
  }

  @Override
  public List<Info> getImportingContainerFileList(final File file) {
    try{
      final List<Info> result = new ArrayList<Info>();
      
      JBBPBitInputStream in = null;
      try{
        in = new JBBPBitInputStream(new FileInputStream(file));
        
        for(int i=0;i<128;i++){
          final TRDosCatalogItem item = CATALOG_PARSER.parse(in).mapTo(TRDosCatalogItem.class);
          if (item.name.charAt(0)>1){
            result.add(new Info(item.name, item.type, item.start, item.length, -1));
          }
        }
        
      }finally{
        JBBPUtils.closeQuietly(in);
      }
      
      return result;
    }catch(Exception ex){
      return null;
    }
  }

  @Override
  public ReadResult readFrom(final File file, final int index) throws IOException {
    final JBBPBitInputStream inStream = new JBBPBitInputStream(new FileInputStream(file),JBBPBitOrder.LSB0);
    try{
      final List<TRDosCatalogItem> list = new ArrayList<TRDosCatalogItem>();
      for (int i = 0; i < 128; i++) {
        final TRDosCatalogItem item = CATALOG_PARSER.parse(inStream).mapTo(TRDosCatalogItem.class);
        if (item.name.charAt(0) > 1) {
          list.add(item);
        }
      }

      final TRDosCatalogItem info = list.get(index);
      
      final int offsetToFile = ((info.track<<4)+info.firstSector)*256;
      final long toskip = offsetToFile - inStream.getCounter();
      final long skept = inStream.skip(toskip);
      if (skept != toskip){
        throw new IllegalStateException("Can't skip needed byte number ["+toskip+']');
      }
      return new ReadResult(new ZXPolyData(new Info(info.name, info.type, info.start, info.length, offsetToFile), this, inStream.readByteArray(info.sectors<<8)), null);
        
    }finally{
      JBBPUtils.closeQuietly(inStream);
    }
  }

  @Override
  public void writeTo(final File file, final ZXPolyData data, final SessionData session) throws IOException {

    final String zxname = data.getInfo().getName();
    final String[] zxFileName = new String[]{prepareNameForTRD(zxname, 0), prepareNameForTRD(zxname, 1), prepareNameForTRD(zxname, 2), prepareNameForTRD(zxname, 3)};

    final char type = data.getInfo().getType();

    final FileNameDialog fileNameDialog = new FileNameDialog(this.mainFrame, "TRD file " + file.getName(), null, zxFileName, new char[]{type, type, type, type});
    fileNameDialog.setVisible(true);
    if (fileNameDialog.approved()) {
      final JBBPOut out = JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN);

      final String[] fnames = fileNameDialog.getZxName();
      final Character[] fchars = fileNameDialog.getZxType();

      final int sectorslen = (data.length() >>> 8) + ((data.length() & 0xFF) == 0 ? 0 : 1);

      int csector = 16;
      
      for (int i = 0; i < 4; i++) {
        out.Byte(fnames[i]).Byte(fchars[i].charValue()).Short(data.getInfo().getStartAddress(), data.getInfo().getLength()).Byte(sectorslen).Byte(csector & 0xF).Byte(csector>>>4);
        csector += sectorslen;
      }

      out.Align(2048).ResetCounter();
      out.Byte(0).Skip(224).Byte(csector & 0xF,csector>>>4,22,4).Short(2560-csector).Byte(16,0,0).Byte("         ").Byte(0,0).Byte("ZXPOLY D").Byte(0,0,0);
      out.Skip(1792);
      
      out.ResetCounter();
      for (int i = 0; i < 4; i++) {
        final byte[] arr = data.getDataForCPU(i);
        out.Byte(arr).Align(256);
      }
      out.Align(2544*256);
      
      saveDataToFile(file, out.End().toByteArray());
    }
  }

  @Override
  public boolean accept(final File pathname) {
    return pathname!= null && (pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".trd"));
  }

  @Override
  public String getDescription() {
    return getToolTip(false)+" (*.TRD)";
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
    return "trd";
  }

  @Override
  public String getPluginUID() {
    return "TRDP";
  }
  
}
