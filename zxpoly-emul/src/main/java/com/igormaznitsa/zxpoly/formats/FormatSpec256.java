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
import com.igormaznitsa.zxpoly.components.VideoController;
import com.igormaznitsa.zxpoly.components.ZxPolyConstants;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;

public class FormatSpec256 extends Snapshot {

  private static final Map<String, BaseItem> APP_BASE = new HashMap<>();

  static {
    try (InputStream in = FormatSpec256.class.getResourceAsStream("/spec256appbase.txt")) {
      for (final String str : IOUtils.readLines(in, StandardCharsets.UTF_8)) {
        final String trimmed = str.trim();
        if (trimmed.isEmpty() || trimmed.startsWith("//")) {
          continue;
        }
        final Matcher matcher = BaseItem.PATTERN.matcher(str);
        if (matcher.find()) {
          APP_BASE.put(matcher.group(2).toLowerCase(Locale.ENGLISH), new BaseItem(matcher.group(1).toLowerCase(Locale.ENGLISH), matcher.group(2).toLowerCase(Locale.ENGLISH), matcher.group(3)));
        } else {
          throw new Error("Can't parse line: " + str);
        }
      }
    } catch (IOException ex) {
      throw new Error("Can't load app base", ex);
    }
  }

  @Override
  public void loadFromArray(final File srcFile, final Motherboard board, final VideoController vc, final byte[] array) throws IOException {
    final Spec256Arch archive = new Spec256Arch(array);
    LOGGER.info("Archive: " + archive);
    final SNAParser parser = archive.getParsedSna();

    final boolean sna128 = parser.extendeddata != null;

    if (sna128) {
      doModeSpec256_128(board);
    } else {
      doModeSpec256_48(board);
    }

    final ZxPolyModule module = board.getModules()[0];
    final Z80 cpu = module.getCpu();

    module.write7FFD(sna128 ? parser.getEXTENDEDDATA().port7ffd : 0b00_1_1_0_000, true);

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

    final int offsetpage2 = 0x8000;
    final int offsetpage5 = 0x14000;
    final int topPageIndex = sna128 ? parser.getEXTENDEDDATA().getPORT7FFD() & 7 : 0;
    final int offsetpageTop = topPageIndex * 0x4000;

    int[] extraBankPages = new int[0];
    if (sna128) {
      extraBankPages = new int[] {0, 1, 2, 3, 4, 5, 6, 7};
      extraBankPages[2] = -1;
      extraBankPages[5] = -1;
      extraBankPages[parser.getEXTENDEDDATA().getPORT7FFD() & 7] = -1;
    }

    for (int i = 0; i < 0x4000; i++) {
      module.writeHeap(offsetpage5 + i, parser.getRAMDUMP()[i]);
      module.writeHeap(offsetpage2 + i, parser.getRAMDUMP()[i + 0x4000]);
      module.writeHeap(offsetpageTop + i, parser.getRAMDUMP()[i + 0x8000]);
    }

    if (sna128) {
      cpu.setRegister(Z80.REG_PC, parser.getEXTENDEDDATA().getREGPC());
      cpu.setRegister(Z80.REG_SP, parser.getREGSP());
      module.write7FFD(parser.getEXTENDEDDATA().getPORT7FFD(), true);
      module.setTrdosActive(parser.getEXTENDEDDATA().getONTRDOS() != 0);

      int extraBankIndex = 0;
      for (int i = 0; i < 8 && extraBankIndex < parser.getEXTENDEDDATA().getEXTRABANK().length; i++) {
        if (extraBankPages[i] < 0) {
          continue;
        }
        final byte[] data = parser.getEXTENDEDDATA().getEXTRABANK()[extraBankIndex++].getDATA();
        final int heapoffset = extraBankPages[i] * 0x4000;
        for (int a = 0; a < data.length; a++) {
          module.writeHeap(heapoffset + a, data[a]);
        }
      }
    } else {
      int spValue = parser.getREGSP();
      final int lowPc;
      final int highPc;
      if (spValue < 0x4000) {
        // ROM area
        final byte [] romData = board.getRomData().getAsArray();
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


      cpu.setRegister(Z80.REG_SP, spValue);
      cpu.setRegister(Z80.REG_PC, (highPc << 8) | lowPc);
    }

    archive.getGfxRamPages().forEach(x -> {
      module.writeGfxRamPage(x.getPageIndex(), gfx2gfxInternalBank(x.getData()));
    });

    module.makeCopyOfRomToGfxRom();
    if (!archive.getGfxRoms().isEmpty()) {
      LOGGER.info("Snapshot provides adapted ROM");
      archive.getGfxRoms().forEach(x -> {
        module.writeGfxRomPage(x.getPageIndex(), gfx2gfxInternalBank(x.getData()));
      });
    }

    board.set3D00(0b1_00_000_0_1, true);
    vc.setBorderColor(parser.getBORDERCOLOR() & 7);
    vc.setVideoMode(ZxPolyConstants.VIDEOMODE_SPEC256);

    final Optional<Spec256Arch.Spec256Bkg> bkg = archive.getBackgrounds().stream()
        .min(Comparator.comparingInt(Spec256Arch.Spec256Bkg::getIndex));
    if (bkg.isPresent()) {
      LOGGER.info("Detected GFX background image");
      VideoController.setGfxBack(bkg.get());
    } else {
      LOGGER.info("No any GFX background");
      VideoController.setGfxBack(null);
    }

    String alignRegVector = archive.getProperties().getProperty("zxpAlignRegs");
    if (alignRegVector == null) {
      BaseItem itemInBase = APP_BASE.get(archive.getSha256().toLowerCase(Locale.ENGLISH));
      if (itemInBase == null) {
        LOGGER.info("Application not found in Spec256 app base");
      } else {
        if (archive.getSha256().equalsIgnoreCase(itemInBase.sha256)) {
          LOGGER.info("Detected item in Spec256 app base, sha256 is OK");
          alignRegVector = itemInBase.regVector;
        } else {
          LOGGER.warning("Detected item in Spec256 app base, sha256 failed");
        }
      }
    } else {
      LOGGER.info("Config file contains register vector: " + alignRegVector);
    }
    final String alignRegisters = archive.getProperties()
        .getProperty("zxpAlignRegs", alignRegVector == null ? "1PSsT" : alignRegVector);
    board.setGfxAlignParams(alignRegisters);

    VideoController
        .setGfxBackOverFF(!"0".equals(archive.getProperties().getProperty("BkOverFF", "0")));
    VideoController
        .setGfxPaper00InkFF(!"0".equals(archive.getProperties().getProperty("Paper00InkFF", "0")));
    VideoController.setGfxHideSameInkPaper(
        !"0".equals(archive.getProperties().getProperty("HideSameInkPaper", "1")));
    VideoController.setGfxDownColorsMixed(
        safeParseInt(archive.getProperties().getProperty("DownColorsMixed", "0"), 0));
    VideoController.setGfxUpColorsMixed(
        safeParseInt(archive.getProperties().getProperty("UpColorsMixed", "64"), 64));

    board.syncSpec256GpuStates();
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

  private static byte[] gfx2gfxInternalBank(final byte[] bankData) {
    final byte[] result = new byte[bankData.length];

    for (int offst = 0; offst < bankData.length; offst += 8) {
      for (int ctx = 0; ctx < 8; ctx++) {
        final int bitMask = 1 << ctx;
        int acc = 0;
        for (int i = 0; i < 8; i++) {
          if ((bankData[offst + i] & bitMask) != 0) {
            acc |= 1 << i;
          }
        }
        result[offst + ctx] = (byte) acc;
      }
    }

    return result;
  }

  private static class BaseItem {
    private static final Pattern PATTERN = Pattern.compile("^\\s*(\\S+)\\s*,\\s*([0-9a-f]+)\\s*,\\s*([AFBCDEHLTXxYy10PSsafbcdehl]+)\\s*$");
    private final String name;
    private final String sha256;
    private final String regVector;

    private BaseItem(final String name, final String sha256, final String regs) {
      this.name = name;
      this.sha256 = sha256;
      this.regVector = regs;
    }
  }

  @Override
  public String getName() {
    return "Spec256 snapshot";
  }

  @Override
  public boolean accept(final File f) {
    return f != null && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".zip"));
  }

  @Override
  public String getDescription() {
    return "Spec256 archive (*.zip)";
  }

}
