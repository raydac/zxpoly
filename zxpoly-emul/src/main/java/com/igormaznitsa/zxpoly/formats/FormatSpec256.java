/*
 * Copyright (C) 2014-2019 Igor Maznitsa
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

package com.igormaznitsa.zxpoly.formats;

import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZxPolyConstants;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import com.igormaznitsa.zxpoly.components.video.VideoController;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

public class FormatSpec256 extends Snapshot {

  private static final Map<String, BaseItem> APP_BASE = new HashMap<>();

  static {
    try (InputStream in = FormatSpec256.class.getResourceAsStream("/spec256appbase.txt")) {
      for (final String str : IOUtils.readLines(in, StandardCharsets.UTF_8)) {
        final String trimmed = str.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("//") || trimmed.startsWith("#")) {
          continue;
        }
        final Matcher matcher = BaseItem.PATTERN.matcher(str);
        if (matcher.find()) {
          APP_BASE.put
              (matcher.group(2).toLowerCase(Locale.ENGLISH),
                  new BaseItem(matcher.group(1).toLowerCase(Locale.ENGLISH),
                      matcher.group(2).toLowerCase(Locale.ENGLISH),
                      matcher.group(3)));
        } else {
          throw new Error("Can't parse line: " + str);
        }
      }
    } catch (IOException ex) {
      throw new Error("Can't load app bData()ase", ex);
    }
  }

  private static Spec256Arch.Spec256GfxPage decodeGfx(Spec256Arch.Spec256GfxPage from) {
    final int dataLen = from.getGfxData().length;

    final byte[] result = new byte[dataLen];

    for (int offst = 0; offst < dataLen; offst += 8) {
      for (int ctx = 0; ctx < 8; ctx++) {
        final int bitMask = 1 << ctx;
        int acc = 0;
        for (int i = 0; i < 8; i++) {
          if ((from.getGfxData()[offst + i] & bitMask) != 0) {
            acc |= 1 << i;
          }
        }
        result[offst + ctx] = (byte) acc;
      }
    }

    return new Spec256Arch.Spec256GfxPage(from, result);
  }

  private static Spec256Arch.Spec256GfxPage adaptPageForColor16(
      final Spec256Arch.Spec256GfxPage page) {
    final byte[] origData = page.getOrigData();
    final byte[] gfxData = page.getGfxData();
    final byte[] newGfx = new byte[gfxData.length];

    final Map<Byte, Byte> cache = new HashMap<>();

    for (int i = 0; i < origData.length; i++) {
      final int gfxOffset = i * 8;
      for (int b = 0; b < 8; b++) {
        final int gfxAddr = gfxOffset + b;
        final boolean origSet = (origData[i] & (1 << b)) != 0;
        final byte oldGfx = gfxData[gfxAddr];
        if (oldGfx != 0 && oldGfx != (byte) 0xFF) {
          newGfx[gfxAddr] = cache
              .computeIfAbsent(oldGfx, spindex -> {
                byte zxpIndex = (byte) VideoController.toZxPolyIndex(spindex);
                if (zxpIndex == 0) {
                  zxpIndex = 8;
                }
                return zxpIndex;
              });
        } else {
          newGfx[gfxAddr] = oldGfx;
        }
      }
    }
    return new Spec256Arch.Spec256GfxPage(page, newGfx);
  }

  @Override
  public void loadFromArray(final File srcFile, final Motherboard board, final VideoController vc,
                            final byte[] array) throws IOException {
    final Spec256Arch archive = new Spec256Arch(board.getRomData(), array);
    final BaseItem dbItem = APP_BASE.get(archive.getSha256().toLowerCase(Locale.ENGLISH));
    if (dbItem == null) {
      LOGGER.info("Application not found in Spec256 app base");
    }

    final boolean modeSpec256colors16 =
        !"0".equals(findProperty(archive, "GFXColors16", dbItem, "0"));

    LOGGER.info("Archive: " + archive);
    final SNAParser parser = archive.getParsedSna();

    if (archive.is128()) {
      doModeSpec256_128(board, modeSpec256colors16);
    } else {
      doModeSpec256_48(board, modeSpec256colors16);
    }

    final ZxPolyModule module = board.getModules()[0];
    final Z80 cpu = module.getCpu();

    module.write7FFD(archive.is128() ? parser.getEXTENDEDDATA().port7ffd : 0b00_1_1_0_000, true);

    cpu.setRegisterPair(Z80.REGPAIR_AF, parser.getREGAF());
    cpu.setRegisterPair(Z80.REGPAIR_BC, parser.getREGBC());
    cpu.setRegisterPair(Z80.REGPAIR_DE, parser.getREGDE());
    cpu.setRegisterPair(Z80.REGPAIR_HL, parser.getREGHL());

    cpu.setRegisterPair(Z80.REGPAIR_AF, parser.getALTREGAF(), true);
    cpu.setRegisterPair(Z80.REGPAIR_BC, parser.getALTREGBC(), true);
    cpu.setRegisterPair(Z80.REGPAIR_DE, parser.getALTREGDE(), true);
    cpu.setRegisterPair(Z80.REGPAIR_HL, parser.getALTREGHL(), true);

    cpu.setRegister(Z80.REG_IX, parser.getREGIX());
    cpu.setRegister(Z80.REG_IY, parser.getREGIY());

    cpu.setRegister(Z80.REG_I, parser.getREGI());
    cpu.setRegister(Z80.REG_R, parser.getREGR());

    cpu.setIM(parser.getINTMODE());
    final boolean iff = (parser.getIFF2() & 4) != 0;
    cpu.setIFF(iff, iff);

    final int[] pages =
        archive.is128() ? new int[] {0, 1, 2, 3, 4, 5, 6, 7} : new int[] {5, 2, 0};

    for (final int pageIndex : pages) {
      archive.getGfxRamPages().stream()
          .filter(x -> x.getPageIndex() == pageIndex)
          .findFirst()
          .ifPresent(p -> {
            LOGGER.info("Detected RAM page: " + p.getPageIndex());
            module.syncWriteHeapPage(p.getPageIndex(), p.getOrigData());
            module.writeGfxRamPage(decodeGfx(modeSpec256colors16 ? adaptPageForColor16(p) : p));
          });
    }

    module.makeCopyOfRomToGfxRom();
    if (!archive.getGfxRoms().isEmpty()) {
      LOGGER.info("provided adapted ROM");
      archive.getGfxRoms().stream()
          .map(x -> modeSpec256colors16 ? adaptPageForColor16(x) : x)
          .forEach(x -> {
            module.writeGfxRomPage(decodeGfx(x));
          });
    }

    if (archive.is128()) {
      LOGGER.info("Detected SNA 128");
      LOGGER.info("#" + Integer.toHexString(parser.getEXTENDEDDATA().getREGPC()) + " => PC");
      cpu.setRegister(Z80.REG_PC, parser.getEXTENDEDDATA().getREGPC());
      LOGGER.info("#" + Integer.toHexString(parser.getREGSP()) + " => SP");
      cpu.setRegister(Z80.REG_SP, parser.getREGSP());
      LOGGER.info("#" + Integer.toHexString(parser.getEXTENDEDDATA().getPORT7FFD()) + " => #7FFD");
      module.write7FFD(parser.getEXTENDEDDATA().getPORT7FFD(), true);
      module.setTrdosActive(parser.getEXTENDEDDATA().getONTRDOS() != 0);
    } else {
      LOGGER.info("Detected SNA 48");
      int spValue = parser.getREGSP();
      final int lowPc;
      final int highPc;
      if (spValue < 0x4000) {
        // ROM area
        final byte[] romData = board.getRomData().getAsArray();
        lowPc = romData[spValue] & 0xFF;
        spValue = (spValue + 1) & 0xFFFF;
        highPc = romData[spValue] & 0xFF;
        spValue = (spValue + 1) & 0xFFFF;
      } else {
        lowPc = parser.getRAMDUMP()[spValue - 0x4000] & 0xFF;
        spValue = (spValue + 1) & 0xFFFF;
        highPc = parser.getRAMDUMP()[spValue - 0x4000] & 0xFF;
        spValue = (spValue + 1) & 0xFFFF;
      }
      final int pcAddr = (highPc << 8) | lowPc;
      LOGGER.info("#" + Integer.toHexString(pcAddr) + " => PC");
      LOGGER.info("#" + Integer.toHexString(spValue) + " => SP");
      cpu.setRegister(Z80.REG_SP, spValue);
      cpu.setRegister(Z80.REG_PC, pcAddr);
    }
    LOGGER.info("Interrupt mode: " + parser.getINTMODE());


    board.set3D00(0b1_00_000_0_1, true);
    vc.setBorderColor(parser.getBORDERCOLOR() & 7);

    if (modeSpec256colors16) {
      vc.setVideoMode(ZxPolyConstants.VIDEOMODE_SPEC256_16);
    } else {
      vc.setVideoMode(ZxPolyConstants.VIDEOMODE_SPEC256);
    }

    final Optional<Spec256Arch.Spec256Bkg> bkg = archive.getBackgrounds().stream()
        .min(Comparator.comparingInt(Spec256Arch.Spec256Bkg::getIndex));
    if (bkg.isPresent()) {
      LOGGER.info("Detected GFX background image");
      VideoController.setGfxBack(bkg.get());
    } else {
      LOGGER.info("No any GFX background");
      VideoController.setGfxBack(null);
    }

    board.setGfxAlignParams(findProperty(archive, "zxpAlignRegs", dbItem, "1PSsT"));

    VideoController
        .setGfxBackOverFF(!"0".equals(findProperty(archive, "BkOverFF", dbItem, "0")));

    VideoController
        .setGfxHideSameInkPaper(
            !"0".equals(findProperty(archive, "HideSameInkPaper", dbItem, "1")));

    VideoController.setGfxDownColorsMixed(
        safeParseInt(findProperty(archive, "DownColorsMixed", dbItem, "0"), 0));

    VideoController.setGfxUpColorsMixed(
        safeParseInt(findProperty(archive, "UpColorsMixed", dbItem, "64"), 64));

    VideoController
        .setGfxPaper00InkFF(!"0".equals(
            findProperty(archive, "Paper00InkFF", dbItem, bkg.isPresent() ? "0" : "1")));

    final boolean leveledXor = !"0".equals(
        findProperty(archive, "GFXLeveledXOR", dbItem, "0"));

    final boolean leveledAnd = !"0".equals(
        findProperty(archive, "GFXLeveledAND", dbItem, "0"));

    final boolean leveledOr = !"0".equals(
        findProperty(archive, "GFXLeveledOR", dbItem, "0"));

    board.setGfxLeveledLogicalOps(leveledXor, leveledAnd, leveledOr);

    board.syncSpec256GpuStates();
  }

  private String findProperty(final Spec256Arch arch, final String name, final BaseItem baseItem,
                              final String dflt) {
    return arch.getProperties()
        .getProperty(name,
            baseItem == null ? dflt : baseItem.findProperty(name, dflt));
  }

  private int safeParseInt(final String str, final int dflt) {
    try {
      return Integer.parseInt(str.trim());
    } catch (NumberFormatException ex) {
      return dflt;
    }
  }

  @Override
  public String getExtension() {
    return "zip";
  }

  @Override
  public byte[] saveToArray(Motherboard board, VideoController vc) throws IOException {
    throw new IOException("Save is unsupported");
  }

  @Override
  public String getName() {
    return "Spec256 snapshot";
  }

  @Override
  public boolean accept(final File f) {
    return f != null
        && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".zip"));
  }

  @Override
  public String getDescription() {
    return "Spec256 archive (*.zip)";
  }

  private static class BaseItem {
    private static final Pattern PATTERN = Pattern.compile(
        "^\\s*(\\S+)\\s*,\\s*([0-9a-f]+)\\s*(?:,\\s*([\\S]+)\\s*)?$");
    private final String name;
    private final String sha256;
    private final Properties defaultProperties;

    private BaseItem(
        final String name,
        final String sha256,
        final String defaultPropertiesList
    ) {
      this.name = name;
      this.sha256 = sha256;
      this.defaultProperties = new Properties();
      if (defaultPropertiesList != null && !defaultPropertiesList.trim().isEmpty()) {
        for (final String line : defaultPropertiesList.split(";")) {
          final String trimmed = line.trim();
          if (!trimmed.isEmpty()) {
            final String[] pair = trimmed.split("=");
            if (pair.length != 2) {
              throw new Error("Can't find name-value: " + trimmed);
            } else {
              this.defaultProperties.setProperty(pair[0].trim(), pair[1].trim());
            }
          }
        }
      }
    }

    private String findProperty(final String name, final String dflt) {
      return this.defaultProperties.getProperty(name, dflt);
    }
  }

}
