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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import java.io.*;
import java.util.*;

public class TAPPlugin extends AbstractFilePlugin {

  public static final JBBPParser TAP_FILE_PARSER = JBBPParser.prepare("tapblocks [_]{ <ushort len; byte flag; byte [len-2] data; byte checksum;}");
  public static final JBBPParser HEADER_PARSER = JBBPParser.prepare("byte type; byte [10] name; <ushort length; <ushort param1; <ushort param2;");
  
  public TAPPlugin(){
    super();
  }

  @Override
  public String getName() {
    return "TAP files";
  }

  @Override
  public String getToolTip() {
    return "A Tape image format";
  }

  @Override
  public boolean hasInsideFileList() {
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
  public List<Info> getInsideFileList(final File file) {
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
    return null;
  }

  @Override
  public void writeTo(final File file, final ZXPolyData data, final SessionData session) throws IOException {
  }

  @Override
  public boolean accept(final File pathname) {
    return pathname!= null && pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".tap");
  }

  @Override
  public String getDescription() {
    return getToolTip()+" (*.TAP)";
  }

  @Override
  public String getExtension() {
    return "tap";
  }

  @Override
  public String getUID() {
    return "TAPP";
  }
  
}
