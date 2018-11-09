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
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.FileNameDialog;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;
import java.io.*;
import java.util.*;
import javax.swing.JOptionPane;
import org.apache.commons.io.FilenameUtils;

public class TAPPlugin extends AbstractFilePlugin {

  public static final JBBPParser TAP_FILE_PARSER = JBBPParser.prepare("tapblocks [_]{ <ushort len; byte flag; byte [len-2] data; byte checksum;}");
  public static final JBBPParser HEADER_PARSER = JBBPParser.prepare("byte type; byte [10] name; <ushort length; <ushort param1; <ushort param2;");
  
  public TAPPlugin(){
    super();
  }

  @Override
  public String getPluginDescription(final boolean forExport) {
    return "TAP file";
  }

  @Override
  public String getToolTip(final boolean forExport) {
    return "A Tape image format";
  }

  @Override
  public boolean doesImportContainInsideFileList() {
    return true;
  }

  private static String extractHeaderName(final byte [] headerData){
    final StringBuilder result = new StringBuilder(10);
    
    for(int i=0;i<10;i++){
      final int value = headerData[i] & 0xFF;
      if (value<32 || value>0x7E) {
        result.append(' ');
      }else{
        result.append((char)value);
      }
    }
    
    return result.toString();
  }
  
  private static int extractStartAddressField(final byte[] headerData) {
    return (headerData[12] & 0xFF) | ((headerData[13] & 0xFF) << 8);
  }

  private static int extractDataLengthField(final byte [] headerData){
    return (headerData[10] & 0xFF) | ((headerData[11] & 0xFF)<<8);
  }
  
  @Override
  public List<Info> getImportingContainerFileList(final File file) {
    try{
      final List<Info> result = new ArrayList<Info>();
      
      JBBPBitInputStream in = null;
      try{
        in = new JBBPBitInputStream(new FileInputStream(file));
        
        while(in.hasAvailableData()){
          final int length = in.readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN);
          final int flag = in.readByte();
          
          if (flag == 0){
            // standard rom
            final int standardflag = in.readByte();
            final byte [] data = in.readByteArray(length-2);
            final int datalen = extractDataLengthField(data);
            final int address = extractStartAddressField(data);
            switch(standardflag){
              case 0 : {
                // program header
                result.add(new Info(extractHeaderName(data), 'B', address, datalen, -1));
              } break;
              case 1 : {
                // numeric data array header
                result.add(new Info(extractHeaderName(data), 'N', address,datalen, -1));
              }break; 
              case 2 : {
                // alphanumeric data array header
                result.add(new Info(extractHeaderName(data), 'S', address, datalen, -1));
              }break;
              case 3 : {
                // code block
                result.add(new Info(extractHeaderName(data), 'C', address, datalen, -1));
              }break;
              default : {
                // unknown
                result.add(new Info("<Unknown>",'U', address, length, -1));
              }break;
            }
          } else {
            if (flag == 0xFF){
              // data block
              result.add(new Info("<Code>", 'D', -1, length-2, -1));
            }else{
              // custom
              result.add(new Info("<Unknown>", 'U', -1, length, -1));
            }
            in.skip(length-1);
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
    JBBPBitInputStream in = new JBBPBitInputStream(new FileInputStream(file));
    try {
      int curindex = 0;
      
      while (in.hasAvailableData()) {
        final int length = in.readUnsignedShort(JBBPByteOrder.LITTLE_ENDIAN);
        final int flag = in.readByte();

        final int offset = (int)in.getCounter();
        final Info info;
        if (flag == 0) {
          // standard rom
          final int standardflag = in.readByte();
          final byte[] data = in.readByteArray(length - 2);
          final int datalen = extractDataLengthField(data);
          final int address = extractStartAddressField(data);
          
          switch (standardflag) {
            case 0: {
              // program header
              info = new Info(extractHeaderName(data), 'B', address, datalen, offset);
            }
            break;
            case 1: {
              // numeric data array header
              info = new Info(extractHeaderName(data), 'N', address, datalen, offset);
            }
            break;
            case 2: {
              // alphanumeric data array header
              info = new Info(extractHeaderName(data), 'S', address, datalen, offset);
            }
            break;
            case 3: {
              // code block
              info = new Info(extractHeaderName(data), 'C', address, datalen, offset);
            }
            break;
            default: {
              // unknown
              info = new Info("<Unknown>", 'U', address, length, offset);
            }
            break;
          }
          
          if (curindex<index){
            curindex++;
          }else{
            throw new IllegalArgumentException("Selected item is not a data block but a header");
          }
        }
        else {
          if (flag == 0xFF) {
            // data block
            info = new Info("<Code>", 'D', -1, length - 2, offset);
          }
          else {
            // custom
            info = new Info("<Unknown>", 'U', -1, length, offset);
          }
          
          if (curindex<index){
            curindex ++;
            in.skip(length - 1);
          }else{
            return new ReadResult(new ZXPolyData(info, this, in.readByteArray(length-1)), null);
          }
        }
      }
      throw new IllegalArgumentException("Can't find file for index "+index);

    }
    finally {
      JBBPUtils.closeQuietly(in);
    }
  }

  @Override
  public void writeTo(final File file, final ZXPolyData data, final SessionData session) throws IOException {
    final int saveAsSeparateFiles = JOptionPane.showConfirmDialog(this.mainFrame, "Save each block as a separated file?","Separate files",JOptionPane.YES_NO_CANCEL_OPTION);
    if (saveAsSeparateFiles == JOptionPane.CANCEL_OPTION) return;
    
    
      final String baseName = file.getName();
      final String baseZXName =  FilenameUtils.getBaseName(baseName);
    if (saveAsSeparateFiles == JOptionPane.YES_OPTION){
      
      final FileNameDialog fileNameDialog = new FileNameDialog(this.mainFrame, "Saving as separated files", new String[]{addNumberToFileName(baseName, 0),
        addNumberToFileName(baseName, 1),addNumberToFileName(baseName, 2),addNumberToFileName(baseName, 3)},
              new String[]{prepareNameForTAP(baseZXName, 0),prepareNameForTAP(baseZXName, 1),prepareNameForTAP(baseZXName, 2),prepareNameForTAP(baseZXName, 3)},
        null);
      fileNameDialog.setVisible(true);
      if (fileNameDialog.approved()) {
        final String [] fileNames = fileNameDialog.getFileName();
        final String [] zxNames = fileNameDialog.getZxName();
        for(int i=0;i<4;i++){
          final byte [] headerblock = makeHeaderBlock(zxNames[i], data.getInfo().getStartAddress(), data.length());
          final byte [] datablock = makeDataBlock(data.getDataForCPU(i));
          final byte [] dataToSave = JBBPOut.BeginBin().Byte(wellTapBlock(headerblock)).Byte(wellTapBlock(datablock)).End().toByteArray();
          
          final File fileToSave = new File(file.getParent(),fileNames[i]);
          if (!saveDataToFile(fileToSave, dataToSave)) return;
        }
      }
    }else{
      final FileNameDialog fileNameDialog = new FileNameDialog(this.mainFrame, "Save as "+baseName, null,
              new String[]{prepareNameForTAP(baseZXName, 0), prepareNameForTAP(baseZXName, 1), prepareNameForTAP(baseZXName, 2), prepareNameForTAP(baseZXName, 3)},
              null);
      fileNameDialog.setVisible(true);
      if (fileNameDialog.approved()){
        final String[] zxNames = fileNameDialog.getZxName();
        final JBBPOut out = JBBPOut.BeginBin();
        for (int i = 0; i < 4; i++) {
          final byte[] headerblock = makeHeaderBlock(zxNames[i], data.getInfo().getStartAddress(), data.length());
          final byte[] datablock = makeDataBlock(data.getDataForCPU(i));
          out.Byte(wellTapBlock(headerblock)).Byte(wellTapBlock(datablock));
        }
        saveDataToFile(file, out.End().toByteArray());
      }
    }
    
  }

  private byte [] wellTapBlock(final byte [] data) throws IOException {
    return JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN).Short(data.length+1).Byte(data).Byte(doTapCRC(data)).End().toByteArray();
  }
  
  private byte doTapCRC(byte [] array){
    byte result = 0;
    for(byte b : array){
      result ^= b;
    }
    return result;
  }
  
  private byte [] makeHeaderBlock(final String name, final int startAddress, final int dataLength) throws IOException {
    final JBBPOut out = JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN);
    if (name.length()!=10) throw new IllegalArgumentException("Name must have 10 length");
    return out.Byte(0,3).Byte(name).Short(dataLength,startAddress,32768).End().toByteArray();
  }
  
  private byte [] makeDataBlock(final byte [] data) throws IOException {
    final JBBPOut out = JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN);
    return out.Byte(0xFF).Byte(data).End().toByteArray();
  }
  
  @Override
  public boolean accept(final File pathname) {
    return pathname!= null && (pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".tap"));
  }

  @Override
  public String getDescription() {
    return getToolTip(false)+" (*.TAP)";
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
    return "tap";
  }

  @Override
  public String getPluginUID() {
    return "TAPP";
  }
  
}
