/*
 * Copyright (C) 2020 igorm
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

import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.PAGE_SIZE;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.VERSION_1;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.VERSION_2;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.VERSION_3A;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.VERSION_3B;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.Z80_MAINPART;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.convertZ80BankIndexesToPages;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.getVersion;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.is48k;
import static com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin.makePair;
import static java.lang.Math.sqrt;


import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class Spec256ZipPlugin extends AbstractFilePlugin {

  private static final String DESCRIPTION = "Spec256 container";

  public static final int[] ARGB_PALETTE_ZXPOLY = new int[] {
      0xFF000000,
      0xFF0000A0,
      0xFFA00000,
      0xFFA000A0,
      0xFF00A000,
      0xFF00A0A0,
      0xFFA0A000,
      0xFFA0A0A0,
      0xFF000000,
      0xFF0000FF,
      0xFFFF0000,
      0xFFFF00FF,
      0xFF00FF00,
      0xFF00FFFF,
      0xFFFFFF00,
      0xFFFFFFFF};

  private static final int[] PALETTE_ARGB_SPEC256 = readSpec256RawPalette();
  private static final byte[] MAP_ZXPOLY2SPEC256INDEX = makePaletteMap(PALETTE_ARGB_SPEC256);

  private static byte[] getPhysicalCpuPage(final int cpu, final int page, final ZXPolyData data) {
    final byte[] bankData = new byte[PAGE_SIZE];
    System.arraycopy(data.getDataForCPU(cpu), page * PAGE_SIZE, bankData, 0, PAGE_SIZE);
    return bankData;
  }

  private static byte[] getPhysicalMaskPage(final int page, final ZXPolyData data) {
    final byte[] maskData = new byte[PAGE_SIZE];
    System.arraycopy(data.getMask(), page * PAGE_SIZE, maskData, 0, PAGE_SIZE);
    return maskData;
  }

  private static byte[] getPhysicalBasePage(final int page, final ZXPolyData data) {
    final byte[] baseData = new byte[PAGE_SIZE];
    System.arraycopy(data.getBaseData(), page * PAGE_SIZE, baseData, 0, PAGE_SIZE);
    return baseData;
  }

  private static byte findClosestSpec256PaletteIndex(
      final int argbColor,
      final int[] argbPalette,
      final int minIndexIncl,
      final int maxIndexExcl
  ) {
    final int r = (argbColor >>> 16) & 0xFF;
    final int g = (argbColor >>> 8) & 0xFF;
    final int b = argbColor & 0xFF;

    double curDistance = Double.MAX_VALUE;
    int lastIndex = minIndexIncl;

    for (int i = minIndexIncl; i < maxIndexExcl; i++) {
        
      final int ir = (argbPalette[i] >>> 16) & 0xFF;
      final int ig = (argbPalette[i] >>> 8) & 0xFF;
      final int ib = argbPalette[i] & 0xFF;

      final double dr = r - ir;
      final double dg = g - ig;
      final double db = b - ib;

      final double distance = sqrt((dr * dr) + (double)(dg * dg) + (double)(db * db));
      if (distance < curDistance) {
        lastIndex = i;
        curDistance = distance;
      }
    }
    return (byte) lastIndex;
  }
  
  private static byte[] makePaletteMap(final int[] argbSpec256Palette) {
    final byte[] result = new byte[ARGB_PALETTE_ZXPOLY.length];

    for (int i = 0; i < result.length; i++) {
      result[i] = findClosestSpec256PaletteIndex(
          ARGB_PALETTE_ZXPOLY[i],
          argbSpec256Palette,
          1,
          254);
    }

    return result;
  }

  private static String colorToHtml(final int argb) {
      final String r = Integer.toHexString((argb >>> 16) & 0xFF).toUpperCase(Locale.ENGLISH);
      final String g = Integer.toHexString((argb >>> 8) & 0xFF).toUpperCase(Locale.ENGLISH);
      final String b = Integer.toHexString(argb & 0xFF).toUpperCase(Locale.ENGLISH);
      
      return "#"+(r.length() < 2 ? "0" : "") + r
              + (g.length() < 2 ? "0" : "") + g
              + (b.length() < 2 ? "0" : "") + b;
  }
  
  private static String paletteAsHtml(final int [] argbPalette) {
      final StringBuilder result = new StringBuilder();
      result.append("<html><body><table>");
      for(int i=0;i<argbPalette.length;i++){
          result.append("<tr>");
          result.append("<th><b>  ").append(i).append("  </b></th>");
          result.append("<th>").append(colorToHtml(argbPalette[i])).append("</th>");
          result.append("<th style=\"width:50%;background-color:").append(colorToHtml(argbPalette[i])).append("\">").append("                ").append("</th>");
          result.append("</tr>");
      }
      result.append("</table></body></html>");
      return result.toString();
  }
  
  private static int[] readSpec256RawPalette() {
    try (InputStream in = Spec256ZipPlugin.class.getResourceAsStream("/spec256.pal")) {
      final int[] result = new int[256];

      for (int i = 0; i < 256; i++) {
        final int red = in.read();
        final int green = in.read();
        final int blue = in.read();
        if (red < 0 || green < 0 || blue < 0) {
          throw new EOFException();
        }
        result[i] = 0xFF000000 | (red << 16) | (green << 8) | blue;
      }
      // FileUtils.write(new File("spec256pal.html"),paletteAsHtml(result),StandardCharsets.UTF_8);
      return result;
    } catch (IOException ex) {
      throw new Error(ex);
    }
  }

  private static byte[] makeSpec256Data(
      byte cpu0,
      byte cpu1,
      byte cpu2,
      byte cpu3,
      byte baseData,
      byte maskData
  ) {
    final byte[] bankBytes = new byte[8];

    for (int i = 0; i < 8; i++) {
      final int bitMask = 1 << i;
      if ((maskData & bitMask) == 0) {
        bankBytes[i] = (baseData & bitMask) == 0 ? 0 : (byte) 0xFF;
      } else {
        final int zxPolyColorIndex = ((cpu3 & bitMask) == 0 ? 0 : 0x08)
            | ((cpu0 & bitMask) == 0 ? 0 : 0x04)
            | ((cpu1 & bitMask) == 0 ? 0 : 0x02)
            | ((cpu2 & bitMask) == 0 ? 0 : 0x01);

        bankBytes[i] = MAP_ZXPOLY2SPEC256INDEX[zxPolyColorIndex];
      }
    }
    return bankBytes;
  }

  @Override
  public boolean isImportable() {
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
    return this;
  }

  @Override
  public String getPluginDescription(final boolean forExport) {
    return DESCRIPTION;
  }

  @Override
  public String getPluginUID() {
    return "S2ZP";
  }

  @Override
  public List<Info> getImportingContainerFileList(final File file) {
    return Collections.emptyList();
  }

  @Override
  public String getExtension(final boolean forExport) {
    return "zip";
  }

  @Override
  public ReadResult readFrom(final File file, final int index) throws IOException {
    throw new IOException("Reading is unsupported");
  }

  @Override
  public void writeTo(
      final File file,
      final ZXPolyData data,
      final SessionData sessionData,
      final Object... extraObjects) throws IOException {
    if (!(data.getPlugin() instanceof Z80InZXPOutPlugin)) {
      throw new IOException("Only imported Z80 snapshot can be exported");
    }

    final byte[] extraData = data.getInfo().getExtra();
    final byte[] bankIndexes;

    final int banksInExtra;
    if (extraData[0] == 0) {
      banksInExtra = 0;
      bankIndexes = new byte[] {8, 4, 5};
    } else {
      bankIndexes = new byte[extraData[0] & 0xFF];
      banksInExtra = bankIndexes.length;
      System.arraycopy(extraData, 1, bankIndexes, 0, bankIndexes.length);
    }
    final byte[] z80header = Arrays.copyOfRange(extraData, banksInExtra + 1, extraData.length);
    final int version = getVersion(z80header);
    final Z80InZXPOutPlugin.Z80MainHeader mheader =
        Z80_MAINPART.parse(z80header).mapTo(new Z80InZXPOutPlugin.Z80MainHeader());
    final int regPc = version == VERSION_1 ? mheader.reg_pc :
        ((z80header[32] & 0xFF) << 8) | (z80header[33] & 0xFF);
    final byte[] pageIndexes =
        convertZ80BankIndexesToPages(bankIndexes, is48k(version, z80header), version);

    final boolean orig48 = is48k(version, z80header);
    final boolean snaIn48 = orig48 && mheader.reg_sp > 0x4000;

    final int port7ffd;
    if (version == VERSION_1) {
      port7ffd = 0x30;
    } else {
      final int hwmode = z80header[34];
      switch (version) {
        case VERSION_2: {
          if (hwmode == 3 || hwmode == 4) {
            port7ffd = z80header[35] & 0xFF;
          } else {
            port7ffd = 0x30;
          }
        }
        break;
        case VERSION_3A:
        case VERSION_3B: {
          if (hwmode == 4 || hwmode == 5 || hwmode == 6) {
            port7ffd = z80header[35] & 0xFF;
          } else {
            port7ffd = 0x30;
          }
        }
        break;
        default:
          port7ffd = 0x30;
      }
    }

    final List<GfxPage> extraGfxPages = new ArrayList<>();
    final byte[] mainGfxData;

    final JBBPOut mainSnapshotOut = JBBPOut.BeginBin()
        .Byte(makeSnaHeaderFromZ80Header(mheader, snaIn48));

    if (snaIn48) {
      JBBPOut ram = JBBPOut.BeginBin();

      for (int page = 0; page < 3; page++) {
        ram.Byte(getPhysicalBasePage(page, data));
      }
      final byte[] ramField = ram.End().toByteArray();

      int spAddr = mheader.reg_sp;

      spAddr--;
      ramField[spAddr - 0x4000] = (byte) regPc;
      spAddr--;
      ramField[spAddr - 0x4000] = (byte) (regPc >>> 8);

      mainSnapshotOut.Byte(ramField);
      mainGfxData = makeGfx(data, 0, 1, 2);
    } else {
      if (orig48) {
        for (int page = 0; page < 3; page++) {
          mainSnapshotOut.Byte(getPhysicalBasePage(page, data));
        }
        mainGfxData = makeGfx(data, 0, 1, 2);
      } else {
        mainSnapshotOut.Byte(getPhysicalBasePage(5, data));
        mainSnapshotOut.Byte(getPhysicalBasePage(2, data));
        mainSnapshotOut.Byte(getPhysicalBasePage(port7ffd & 7, data));
        mainGfxData = makeGfx(data, 5, 2, port7ffd & 7);
      }

      mainSnapshotOut
          .Short(regPc)
          .Byte(port7ffd)
          .Byte(0);

      if (orig48) {
        final byte[] fakePage = new byte[0x4000];
        for (int i = 0; i < 5; i++) {
          mainSnapshotOut.Byte(fakePage);
        }
      } else {
        for (int i : pageIndexes) {
          if (i == 2 || i == 5 || i == (port7ffd & 7)) {
            continue;
          }
          mainSnapshotOut.Byte(getPhysicalBasePage(i, data));
          extraGfxPages.add(new GfxPage(i, makeGfx(data, i)));
        }
      }
      if (!orig48 && extraGfxPages.stream().noneMatch(x -> x.index == 0)) {
        extraGfxPages.add(new GfxPage(0, makeGfx(data, 0)));
      }
    }
    saveSpec256Zip(file, mainSnapshotOut.End().toByteArray(), mainGfxData,
        (Properties) extraObjects[0], extraGfxPages);
  }

  private byte[] makeGfx(final ZXPolyData data, final int... pageIndexes) throws IOException {
    final JBBPOut result = JBBPOut.BeginBin();

    for (final int pindex : pageIndexes) {
      final byte[] pageMask = getPhysicalMaskPage(pindex, data);
      final byte[] baseData = getPhysicalBasePage(pindex, data);

      final byte[] cpuData0 = getPhysicalCpuPage(0, pindex, data);
      final byte[] cpuData1 = getPhysicalCpuPage(1, pindex, data);
      final byte[] cpuData2 = getPhysicalCpuPage(2, pindex, data);
      final byte[] cpuData3 = getPhysicalCpuPage(3, pindex, data);

      for (int offst = 0; offst < PAGE_SIZE; offst++) {
        result.Byte(makeSpec256Data(
            cpuData0[offst],
            cpuData1[offst],
            cpuData2[offst],
            cpuData3[offst],
            baseData[offst],
            pageMask[offst]));
      }
    }

    return result.End().toByteArray();
  }

  private void saveSpec256Zip(
      final File file,
      final byte[] snaData,
      final byte[] gfxData,
      final Properties configProperties,
      final List<GfxPage> gfxPages
  ) throws IOException {
    final String name = FilenameUtils.getBaseName(file.getName()).toUpperCase(Locale.ENGLISH);
    try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
      final ZipEntry snaEntry = new ZipEntry(name + ".SNA");
      zos.putNextEntry(snaEntry);
      zos.write(snaData);
      zos.closeEntry();

      final ZipEntry gfxEntry = new ZipEntry(name + ".GFX");
      zos.putNextEntry(gfxEntry);
      zos.write(gfxData);
      zos.closeEntry();

      final ZipEntry cfgEntry = new ZipEntry(name + ".CFG");
      zos.putNextEntry(cfgEntry);

      final StringWriter writer = new StringWriter();
      configProperties.stringPropertyNames().forEach(propName -> {
        writer.append(propName + '=' + configProperties.getProperty(propName) + '\n');
      });

      zos.write(writer.toString().getBytes(StandardCharsets.UTF_8));
      zos.closeEntry();

      for (final GfxPage page : gfxPages) {
        final ZipEntry pageEntry = new ZipEntry(name + ".GF" + page.index);
        zos.putNextEntry(pageEntry);
        zos.write(page.data);
        zos.closeEntry();
      }
      zos.finish();
    }
  }

  @Override
  public boolean accept(File f) {
    return f.isDirectory()
        || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".zip");
  }

  @Override
  public String getDescription() {
    return this.getToolTip(true) + " (*.ZIP)";
  }

  private byte[] makeSnaHeaderFromZ80Header(
      final Z80InZXPOutPlugin.Z80MainHeader z80header,
      final boolean pcOnStack
  ) throws IOException {
    return JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN)
        .Byte(z80header.reg_ir)
        .Short(z80header.reg_hl_alt)
        .Short(z80header.reg_de_alt)
        .Short(z80header.reg_bc_alt)
        .Short(makePair(z80header.reg_a_alt, z80header.reg_f_alt))
        .Short(z80header.reg_hl)
        .Short(z80header.reg_de)
        .Short(z80header.reg_bc)
        .Short(z80header.reg_iy)
        .Short(z80header.reg_ix)
        .Byte(z80header.iff2 == 0 ? 0 : 4)
        .Byte(z80header.reg_r)
        .Short(makePair(z80header.reg_a, z80header.reg_f))
        .Short(z80header.reg_sp - (pcOnStack ? 2 : 0))
        .Byte(z80header.emulFlags.interruptmode)
        .Byte(z80header.flags.bordercolor)
        .End()
        .toByteArray();
  }

  private static final class GfxPage {

    private final int index;
    private final byte[] data;

    public GfxPage(final int index, final byte[] data) {
      this.index = index;
      this.data = data;
    }
  }
}
