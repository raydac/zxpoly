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
import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.FileNameDialog;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SCLPlugin extends AbstractFilePlugin {

  private static final String DESCRIPTION = "TR-Dos SCL disk image";

  public static final JBBPParser CATALOG_PARSER = JBBPParser
      .prepare("byte [8] name; ubyte type; <ushort start; <ushort length; ubyte sectors;");

  public SCLPlugin() {
    super();
  }

  @Override
  public String getPluginUID() {
    return "SCLP";
  }

  @Override
  public String getToolTip(final boolean forExport) {
    return DESCRIPTION;
  }

  @Override
  public boolean doesContainInternalFileItems() {
    return true;
  }

  @Override
  public String getPluginDescription(final boolean forExport) {
    return DESCRIPTION;
  }

  @Override
  public List<Info> getImportingContainerFileList(final File file) {
    try {
      final List<Info> result = new ArrayList<>();

      JBBPBitInputStream in = null;
      try {
        in = new JBBPBitInputStream(new FileInputStream(file), false);
        final long id = in.readLong(JBBPByteOrder.BIG_ENDIAN);
        if (id == 0x53494E434C414952L) {
          // it's scl
          final int fileNumber = in.readByte();
          for (int i = 0; i < fileNumber; i++) {
            final SCLCatalogItem item = CATALOG_PARSER.parse(in).mapTo(new SCLCatalogItem());
            result.add(new Info(item.name, item.type, item.start, item.length, -1, true));
          }
        } else {
          // it's not scl
          return null;
        }
      } finally {
        JBBPUtils.closeQuietly(in);
      }

      return result;
    } catch (Exception ex) {
      return null;
    }
  }

  @Override
  public ReadResult readFrom(final String name, final byte[] dataArray, final int index) throws IOException {
    final List<SCLCatalogItem> list = new ArrayList<>();
    final JBBPBitInputStream in =
        new JBBPBitInputStream(new ByteArrayInputStream(dataArray), false);
    try {
      final long id = in.readLong(JBBPByteOrder.BIG_ENDIAN);
      if (id == 0x53494E434C414952L) {
        // it's scl
        final int fileNumber = in.readByte();
        for (int i = 0; i < fileNumber; i++) {
          final SCLCatalogItem item = CATALOG_PARSER.parse(in).mapTo(new SCLCatalogItem());
          list.add(item);
        }

        final SCLCatalogItem itemToRead = list.get(index);

        for (int i = 0; i < index; i++) {
          final int len = list.get(i).sectors * 256;
          if (len != in.skip(len)) {
            throw new IllegalStateException("Can't skip bytes:" + list.get(i).length);
          }
        }
        final long offset = in.getCounter();
        return new ReadResult(new ZXPolyData(
                new Info(itemToRead.name, itemToRead.type, itemToRead.start, itemToRead.length,
                        (int) offset, true), this, in.readByteArray(itemToRead.sectors * 256)), null);

      } else {
        throw new IllegalArgumentException("It's not a SCl file");
      }
    } finally {
      JBBPUtils.closeQuietly(in);
    }
  }

  @Override
  public void writeTo(
      final File file,
      final ZXPolyData data,
      final SessionData sessionData,
      final Object... extraData
  ) throws IOException {

    final String zxname = data.getInfo().getName();
    final String[] zxFileName =
        new String[] {prepareNameForTRD(zxname, 0), prepareNameForTRD(zxname, 1),
            prepareNameForTRD(zxname, 2), prepareNameForTRD(zxname, 3)};

    final char type = data.getInfo().getType();

    final FileNameDialog fileNameDialog =
            new FileNameDialog(this.spriteCorrectorMainFrame, "SCL file " + file.getName(), null, zxFileName,
                    new char[]{type, type, type, type});
    fileNameDialog.setVisible(true);
    if (fileNameDialog.approved()) {
      final JBBPOut out = JBBPOut.BeginBin();
      out.Long(0x53494E434C414952L).ByteOrder(JBBPByteOrder.LITTLE_ENDIAN).Byte(4);

      final String[] fnames = fileNameDialog.getZxName();
      final Character[] fchars = fileNameDialog.getZxType();

      final int sectors = (data.length() >>> 8) + ((data.length() & 0xFF) == 0 ? 0 : 1);

      for (int i = 0; i < 4; i++) {
        out.Byte(fnames[i]).Byte(fchars[i])
            .Short(data.getInfo().getStartAddress(), data.getInfo().getLength()).Byte(sectors);
      }

      out.ResetCounter();
      for (int i = 0; i < 4; i++) {
        final byte[] arr = data.getDataForCPU(i);
        out.Byte(arr).Align(256);
      }
      saveDataToFile(file, out.End().toByteArray());
    }
  }

  @Override
  public boolean accept(final File pathname) {
    return pathname != null
        && (pathname.isDirectory() || pathname.getName().toLowerCase(Locale.ENGLISH).endsWith(".scl"));
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
    return "scl";
  }

  @Override
  public String getDescription() {
    return getToolTip(false) + " (*.SCL)";
  }

  public static final class SCLCatalogItem {

    @Bin(type = BinType.BYTE_ARRAY)
    public String name;
    @Bin(type = BinType.UBYTE)
    public char type;
    @Bin(type = BinType.USHORT)
    public int start;
    @Bin(type = BinType.USHORT)
    public int length;
    @Bin(type = BinType.UBYTE)
    public int sectors;
  }

}
