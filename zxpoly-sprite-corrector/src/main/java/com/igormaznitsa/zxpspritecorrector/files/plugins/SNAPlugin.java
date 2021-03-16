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

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.mapper.JBBPMapper;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;

import javax.swing.filechooser.FileFilter;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;
import java.util.stream.IntStream;

public final class SNAPlugin extends AbstractFilePlugin {

  private static final Logger LOGGER = Logger.getLogger("SNA-Plugin");

  private static final String DESCRIPTION = "SNA snapshot";

  private final JBBPParser SNA_PARSER = JBBPParser.prepare(
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
          + "ubyte iff2;"
          + "ubyte regR;"
          + "<ushort regAF;"
          + "<ushort regSP;"
          + "ubyte intMode;"
          + "ubyte borderColor;"
          + "byte [49152] ramDump;"
          + "extendedData {"
          + "  <ushort regPC;"
          + "  ubyte port7FFD;"
          + "  byte onTrDos;"
          + "  extrabank [_] {"
          + "    byte [16384] data;"
          + "  }"
          + "}",
      JBBPParser.FLAG_SKIP_REMAINING_FIELDS_IF_EOF
  );

  @Override
  public boolean isExportable() {
    return false;
  }

  @Override
  public String getToolTip(final boolean forExport) {
    return DESCRIPTION;
  }

  @Override
  public boolean doesContainInternalFileItems() {
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
        return f != null &&
            (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".sna"));
      }

      @Override
      public String getDescription() {
        return getToolTip(true) + " (*.SNA)";
      }
    };
  }

  @Override
  public String getPluginDescription(final boolean forExport) {
    return DESCRIPTION;
  }

  @Override
  public String getPluginUID() {
    return "SNAS";
  }

  @Override
  public List<Info> getImportingContainerFileList(final File file) {
    return null;
  }

  @Override
  public String getExtension(final boolean forExport) {
    return "sna";
  }

  @Override
  public ReadResult readFrom(final String name, final byte[] array, final int index) throws IOException {
    final boolean sna128;

    switch (array.length) {
      case 49179:
        sna128 = false;
        break;
      case 131103:
      case 147487:
        sna128 = true;
        break;
      default: {
        throw new IOException("SNA file has inappropriate length: " + array.length);
      }
    }


    SnaFileSnapshot snaFile =
        SNA_PARSER.parse(array).mapTo(new SnaFileSnapshot(), JBBPMapper.FLAG_IGNORE_MISSING_VALUES);

    if (sna128) {
      LOGGER.info("SNA128 DETECTED");

      final byte[] data = new byte[0x4000 * (3 + snaFile.extendedData.extrabank.length)];
      LOGGER.info("SNA128 data length: " + data.length + " bytes");

      final int topPage = snaFile.extendedData.port7FFD & 7;

      // page 5
      System.arraycopy(snaFile.ramDump, 0, data, 5 * 0x4000, 0x4000);
      // page 2
      System.arraycopy(snaFile.ramDump, 0x4000, data, 2 * 0x4000, 0x4000);

      // page from 7FFD
      System.arraycopy(snaFile.ramDump, 0x8000, data, topPage * 0x4000, 0x4000);

      // other pages
      int extraBankIndex = 0;
      for (int i = 0; i < 8; i++) {
        if (extraBankIndex >= snaFile.extendedData.extrabank.length
            || i == 2
            || i == 5
            || i == topPage) {
          continue;
        }

        final byte[] pageData = snaFile.extendedData.extrabank[extraBankIndex].data;
        System.arraycopy(pageData, 0, data, i * 0x4000, 0x4000);

        extraBankIndex++;
      }

      final byte[] extra = JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN)
          // indexes of ram banks
          .Byte(data.length / 0x4000)
          .Byte(IntStream.range(0, data.length / 0x4000).map(x -> x + 3).toArray())

          .Byte(snaFile.regAF >>> 8)
          .Byte(snaFile.regAF)
          .Short(snaFile.regBC)
          .Short(snaFile.regHL)
          .Short(0)
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
          .Byte((snaFile.iff2 & 4) == 0 ? 0 : 0xFF)
          .Byte((snaFile.iff2 & 4) == 0 ? 0 : 0xFF)
          .Bits(JBBPBitNumber.BITS_2, snaFile.intMode)
          .Bit(0)
          .Bit(0)
          .Bits(JBBPBitNumber.BITS_2, 0)
          .Bits(JBBPBitNumber.BITS_2, 0)

          .Short(23)
          .Short(snaFile.extendedData.regPC)
          .Byte(3)
          .Byte(snaFile.extendedData.port7FFD)
          .Byte(0)
          .Byte(0)
          .Byte(0)
          .Byte(0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0)

          .End().toByteArray();
      return new ReadResult(new ZXPolyData(
              new Info(name, 'C', snaFile.extendedData.regPC, data.length, 0, extra),
              new Z80InZXPOutPlugin(), data), null);
    } else {
      LOGGER.info("SNA48 DETECTED");

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
          // No information about RAM banks in ZX48
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
          .Byte((snaFile.iff2 & 4) == 0 ? 0 : 0xFF)
          .Byte((snaFile.iff2 & 4) == 0 ? 0 : 0xFF)
          .Bits(JBBPBitNumber.BITS_2, snaFile.intMode)
          .Bit(0)
          .Bit(0)
          .Bits(JBBPBitNumber.BITS_2, 0)
          .Bits(JBBPBitNumber.BITS_2, 0)
          .End().toByteArray();
      return new ReadResult(
              new ZXPolyData(new Info(name, 'C', startAddress, dataLength, 0, extra),
                      new Z80InZXPOutPlugin(), data), null);
    }
  }

  @Override
  public void writeTo(
      final File file,
      final ZXPolyData data,
      final SessionData sessionData,
      final Object... extraData
  )
      throws IOException {
    throw new IOException("SNA export is unsupported");
  }

  @Override
  public boolean accept(final File f) {
    return f != null &&
        (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".sna"));
  }

  @Override
  public String getDescription() {
    return DESCRIPTION + " (*.SNA)";
  }

  public static class SnaFileSnapshot {

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
    public byte iff2;
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

    @Bin
    public ExtendedData extendedData;

    public Object newInstance(Class<?> klazz) {
      if (klazz == ExtendedData.class) {
        return new ExtendedData();
      }
      return null;
    }

    @Bin
    public class ExtendedData {
      public char regPC;
      @Bin(type = BinType.UBYTE)
      public byte port7FFD;
      public byte onTrDos;
      public Extrabank[] extrabank;

      public Object newInstance(Class<?> klazz) {
        if (klazz == Extrabank.class) {
          return new Extrabank();
        }
        return null;
      }

      @Bin
      public class Extrabank {
        public byte[] data;
      }
    }
  }

}
