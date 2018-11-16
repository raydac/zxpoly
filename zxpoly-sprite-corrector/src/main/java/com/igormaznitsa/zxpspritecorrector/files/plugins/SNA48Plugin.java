/*
 * Copyright (C) 2017 Raydac Research Group Ltd.
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

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FileUtils;
import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;

public class SNA48Plugin extends AbstractFilePlugin {

  private final JBBPParser PARSER = JBBPParser.prepare(
          "ubyte regI;"
          + "<ushort altRegHL;"
          + "<ushort altRegDE;"
          + "<ushort altRegBC;"
          + "<ushort altRegAF;"
          + "<ushort regHL;"
          + "<ushort regDE;"
          + "<ushort regBC;"
          + "<ushort regIY;"
          + "<ushort regIX;"
          + "ubyte interrupt;"
          + "ubyte regR;"
          + "<ushort regAF;"
          + "<ushort regSP;"
          + "ubyte intMode;"
          + "ubyte borderColor;"
          + "byte [49152] ramDump;");

  public class SNAFileV1 {

    @Bin(type = BinType.UBYTE)
    public byte regI;
    @Bin(type = BinType.USHORT)
    public char altRegHL;
    @Bin(type = BinType.USHORT)
    public char altRegDE;
    @Bin(type = BinType.USHORT)
    public char altRegBC;
    @Bin(type = BinType.USHORT)
    public char altRegAF;
    @Bin(type = BinType.USHORT)
    public char regHL;
    @Bin(type = BinType.USHORT)
    public char regDE;
    @Bin(type = BinType.USHORT)
    public char regBC;
    @Bin(type = BinType.USHORT)
    public char regIY;
    @Bin(type = BinType.USHORT)
    public char regIX;
    @Bin(type = BinType.UBYTE)
    public byte interrupt;
    @Bin(type = BinType.UBYTE)
    public byte regR;
    @Bin(type = BinType.USHORT)
    public char regAF;
    @Bin(type = BinType.USHORT)
    public char regSP;
    @Bin(type = BinType.UBYTE)
    public byte intMode;
    @Bin(type = BinType.UBYTE)
    public byte borderColor;
    @Bin(type = BinType.BYTE_ARRAY)
    public byte[] ramDump;
  }

  @Override
  public boolean allowsExport() {
    return false;
  }

  @Override
  public String getToolTip(final boolean forExport) {
    return forExport ? "ZXPZ80 snapshot" : "SNA ZX48 snapshot";
  }

  @Override
  public boolean doesImportContainInsideFileList() {
    return false;
  }

  @Override
  public FileFilter getImportFileFilter() {
    return this;
  }

  @Override
  public FileFilter getExportFileFilter() {
    return new javax.swing.filechooser.FileFilter() {
      @Override
      public boolean accept(final File f) {
        return f != null && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".zxp"));
      }

      @Override
      public String getDescription() {
        return "ZXP Snapshot (*.ZXP)";
      }
    };
  }

  @Override
  public String getPluginDescription(final boolean forExport) {
    return forExport ? "SNA ZX48 file" : "SNA ZX48 file";
  }

  @Override
  public String getPluginUID() {
    return "SNAZX48";
  }

  @Override
  public List<Info> getImportingContainerFileList(final File file) {
    return null;
  }

  @Override
  public String getExtension(final boolean forExport) {
    return forExport ? "zxp" : "sna";
  }

  @Override
  public ReadResult readFrom(final File file, final int index) throws IOException {
    final byte[] array = FileUtils.readFileToByteArray(file);

    if (array.length != 49179) {
      throw new IOException("It is not SNA 48 file, size must be 49179 bytes");
    }

    SNAFileV1 snaFile = PARSER.parse(array).mapTo(SNAFileV1.class);

    int regsp = snaFile.regSP;

    if (regsp < 0x4000) {
      throw new IOException("Can't import SNA because its stack in ROM area!");
    }

    final int lowaddr = snaFile.ramDump[regsp - 0x4000] & 0xFF;
    regsp = (regsp + 1) & 0xFFFF;
    final int highaddr = snaFile.ramDump[regsp - 0x4000] & 0xFF;
    regsp = (regsp + 1) & 0xFFFF;
    snaFile.regSP = (char) regsp;

    final int startAddress = (highaddr << 8) | lowaddr;

    final int dataLength = snaFile.ramDump.length;
    final byte[] data = snaFile.ramDump;

    final byte[] extra = JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN)
            .Byte(0)
            .Byte(snaFile.regAF >>> 8)
            .Byte(snaFile.regAF)
            .Short(snaFile.regBC)
            .Short(snaFile.regHL)
            .Short(startAddress)
            .Short(snaFile.regSP)
            .Byte(snaFile.regI)
            .Byte(snaFile.regR)
            .Bit((snaFile.regR & 0x80) != 0)
            .Bits(JBBPBitNumber.BITS_3, snaFile.borderColor)
            .Bit(0, 0)
            .Bits(JBBPBitNumber.BITS_2, 0)
            .Short(snaFile.regDE)
            .Short(snaFile.altRegBC)
            .Short(snaFile.altRegDE)
            .Short(snaFile.altRegHL)
            .Byte(snaFile.altRegAF >>> 8)
            .Byte(snaFile.altRegAF)
            .Short(snaFile.regIY)
            .Short(snaFile.regIX)
            .Byte(0xFF)
            .Byte(snaFile.interrupt)
            .Bits(JBBPBitNumber.BITS_2, snaFile.intMode)
            .Bit(0)
            .Bit(0)
            .Bits(JBBPBitNumber.BITS_2, 0)
            .Bits(JBBPBitNumber.BITS_2, 0)
            .End().toByteArray();

    System.out.println("HEADER LEN : " + extra.length);

    return new ReadResult(new ZXPolyData(new Info(file.getName(), 'C', startAddress, dataLength, 0, extra), new Z80Plugin(), data), null);
  }

  @Override
  public void writeTo(final File file, final ZXPolyData data, final SessionData sessionData) throws IOException {
    if (!(data.getPlugin() instanceof SNA48Plugin)) {
      throw new IOException("Only imported SNA ZX48 snapshot can be exported");
    }
  }

  @Override
  public boolean accept(final File f) {
    return f != null && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".sna"));
  }

  @Override
  public String getDescription() {
    return "SNA ZX48 Snapshot (*.SNA)";
  }

}
