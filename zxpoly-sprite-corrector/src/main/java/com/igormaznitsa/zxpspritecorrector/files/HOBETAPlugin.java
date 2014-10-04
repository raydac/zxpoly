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
import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import java.io.*;
import java.util.*;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class HOBETAPlugin extends AbstractFilePlugin {

  public static final JBBPParser HOBETA_FILE_PARSER = JBBPParser.prepare("byte [8] name; byte type; <ushort start; <ushort length; skip; ubyte sectors; <ushort checksum; byte [_] data;");

  private final class Hobeta {

    @Bin(type = BinType.BYTE_ARRAY)
    String name;
    @Bin(type = BinType.BYTE)
    byte type;
    @Bin(type = BinType.USHORT)
    int start;
    @Bin(type = BinType.USHORT)
    int length;
    @Bin(type = BinType.UBYTE)
    int sectors;
    @Bin(type = BinType.USHORT)
    int checksum;
    @Bin
    byte[] data;
  }

  public HOBETAPlugin() {
    super();
  }

  @Override
  public int getUID() {
    return ((int) 'H' << 24) | ((int) 'B' << 16) | ((int) 'T' << 8) | (int) 'A';
  }

  @Override
  public String getName() {
    return "Hobeta files";
  }

  @Override
  public String getToolTip() {
    return "A Hobeta file format";
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
      JBBPBitInputStream in = null;
      try {
        final StringBuilder result = new StringBuilder();

        final Hobeta hobeta = HOBETA_FILE_PARSER.parse(new FileInputStream(file)).mapTo(Hobeta.class);
        result.append("     Name:").append(hobeta.name).append("  ").append('\n');
        result.append("     Type:").append((char) hobeta.type).append("  ").append('\n');
        result.append("    Start:").append(hobeta.start).append("  ").append('\n');
        result.append("   Length:").append(hobeta.length).append(" bytes").append("  ").append('\n');
        result.append("  Sectors:").append(hobeta.sectors).append(" sectors").append("  ");

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
    final byte[] wholeFile = FileUtils.readFileToByteArray(file);
    final Hobeta parsed = HOBETA_FILE_PARSER.parse(wholeFile).mapTo(Hobeta.class);
    return new ZXPolyData(new Info(parsed.name, (char) (parsed.type & 0xFF), parsed.start, parsed.length), this, parsed.data);
  }

  @Override
  public void writeTo(final File file, final ZXPolyData data) throws IOException {
    final File dir = file.getParentFile();
    final String name = file.getName();
    final char zxType = data.getInfo().getType();
    final String zxName = data.getInfo().getName();

    final FileNameDialog nameDialog = new FileNameDialog(
            this.mainFrame,
            new String[]{addNumberToName(name, 0), addNumberToName(name, 1), addNumberToName(name, 2), addNumberToName(name, 3)},
            new String[]{addNumberToZXFileName(zxName, 0), addNumberToZXFileName(zxName, 1), addNumberToZXFileName(zxName, 2), addNumberToZXFileName(zxName, 3)},
            new char[]{zxType, zxType, zxType, zxType}
    );
    nameDialog.setVisible(true);

    if (nameDialog.approved()) {
    }

  }

  private String addNumberToName(final String name, final int number) {
    String base = FilenameUtils.getBaseName(name);
    final String ext = FilenameUtils.getExtension(name);
    return base + Integer.toString(number) + '.' + ext;
  }

  private String addNumberToZXFileName(final String baseName, final int number) {
    final String normalized = normalizeName(baseName);
    final String numberStr = Integer.toString(number);
    return normalized.substring(0, Math.max(0, 8 - numberStr.length())) + numberStr;
  }

  private int makeCRC(final byte[] array) {
    int crc = 0;
    for (int i = 0; i < array.length; crc = crc + (array[i] * 257) + i, i++);
    return crc;
  }

  private static String normalizeName(final String name) {
    return name.length() < 8 ? name + "         ".substring(0, 8 - name.length()) : name.substring(0, 8);
  }

  private void writeDataBlockAsHobeta(final File file, final String name, final byte type, final int start, final byte[] data) throws IOException {
    final byte[] header = JBBPOut.BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Byte(normalizeName(name)).Byte(name).Short(start, data.length).Byte((data.length >>> 8) + 1, 0).End().toByteArray();
    final byte[] full = JBBPOut.BeginBin().ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Byte(header).Short(makeCRC(header)).Byte(data).End().toByteArray();
    FileUtils.writeByteArrayToFile(file, full);
  }

  @Override
  public boolean accept(final File pathname) {
    return pathname != null && pathname.isDirectory() || pathname.getName().lastIndexOf(".$") == (pathname.getName().length() - 3);
  }

  @Override
  public String getDescription() {
    return getToolTip() + " (*.$?)";
  }

}
