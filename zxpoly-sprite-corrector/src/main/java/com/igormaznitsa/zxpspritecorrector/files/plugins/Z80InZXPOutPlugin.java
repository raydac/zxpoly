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

import static com.igormaznitsa.zxpspritecorrector.SpriteCorrectorMainFrame.EXTRA_PROPERTY_OVERRIDE_CPU_DATA;
import static com.igormaznitsa.zxpspritecorrector.SpriteCorrectorMainFrame.deserializeProperties;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.JBBPBitNumber;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.mapper.JBBPMapper;
import com.igormaznitsa.jbbp.mapper.JBBPMapperCustomFieldProcessor;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldBit;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
import com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;
import com.igormaznitsa.zxpspritecorrector.files.Z80ExportDialog;
import com.igormaznitsa.zxpspritecorrector.files.ZXEMLSnapshotFormat;
import com.igormaznitsa.zxpspritecorrector.files.ZXEMLSnapshotFormat.Page;
import com.igormaznitsa.zxpspritecorrector.files.ZXEMLSnapshotFormat.Pages;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class Z80InZXPOutPlugin extends AbstractFilePlugin {

  public static final int PAGE_SIZE = 0x4000;
  public static final int VERSION_1 = 0;
  public static final int VERSION_2 = 1;
  public static final int VERSION_3A = 2;
  public static final int VERSION_3B = 3;
  public static final JBBPParser Z80_MAINPART = JBBPParser.prepare(
      "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          +
          "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          +
          "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          +
          "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
  );
  public static final JBBPParser Z80_VERSION1 = JBBPParser.prepare(
      "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          +
          "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          +
          "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          +
          "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "byte [_] data;"
  );
  public static final JBBPParser Z80_VERSION2 = JBBPParser.prepare(
      "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          +
          "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          +
          "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          +
          "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "<ushort extrahdrlen;" // header length
          + "<ushort reg_pc2;"
          + "ubyte mode;"
          + "ubyte port7FFD;"
          + "ubyte portFF;"
          + "byte [18] extra;"// misc non zx or not supported stuff
          + "byte [_] data;"
  );
  public static final JBBPParser Z80_VERSION3A = JBBPParser.prepare(
      "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          +
          "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          +
          "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          +
          "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "<ushort extrahdrlen;" // header length
          + "<ushort reg_pc2;"
          + "ubyte mode;"
          + "ubyte port7FFD;"
          + "ubyte portFF;"
          + "byte [49] extra;" // misc non zx or not supported stuff
          + "byte [_] data;"
  );
  public static final JBBPParser Z80_VERSION3B = JBBPParser.prepare(
      "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          +
          "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          +
          "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          +
          "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "<ushort extrahdrlen;" // header length
          + "<ushort reg_pc2;"
          + "ubyte mode;"
          + "ubyte port7FFD;"
          + "ubyte portFF;"
          + "byte [50] extra;" // misc non zx or not supported stuff
          + "byte [_] data;"
  );
  private static final String DESCRIPTION_IN = "Z80 snapshot";
  private static final String DESCRIPTION_OUT = "ZX-Poly snapshot";

  public static boolean is48k(final int version, final Z80Snapshot snapshot) {
    switch (version) {
      case VERSION_1:
        return true;
      case VERSION_2: {
        return snapshot.mode == 0 || snapshot.mode == 1;
      }
      case VERSION_3A:
      case VERSION_3B: {
        return snapshot.mode == 0 || snapshot.mode == 1 || snapshot.mode == 3;
      }
      default:
        return false;
    }
  }

  public static boolean is48k(final int version, final byte[] header) {
    switch (version) {
      case VERSION_1:
        return true;
      case VERSION_2: {
        return header[34] == 0 || header[34] == 1;
      }
      case VERSION_3A:
      case VERSION_3B: {
        return header[34] == 0 || header[34] == 1 || header[34] == 3;
      }
      default:
        return false;
    }
  }

  public static int getVersion(final byte[] data) {
    final int version;
    if ((data[6] | data[7]) == 0) {
      switch (((data[31] & 0xFF) << 8 | (data[30] & 0xFF))) {
        case 23: { // Verison 2
          version = VERSION_2;
        }
        break;
        case 54: { // Version 3a
          version = VERSION_3A;
        }
        break;
        case 55: { // Version 3b
          version = VERSION_3B;
        }
        break;
        default:
          version = -1;
      }
    } else {
      version = VERSION_1;
    }
    return version;
  }

  public static int makePair(final byte a, final byte b) {
    return ((a & 0xFF) << 8) | (b & 0xFF);
  }

  public static byte[] convertZ80BankIndexesToPages(final byte[] bankIndexes, final boolean mode48,
                                                    final int version) {
    final byte[] result;
    if (version == VERSION_1) {
      result = new byte[] {5, 2, 0};
    } else {
      if (mode48) {
        result = new byte[bankIndexes.length];
        for (int i = 0; i < bankIndexes.length; i++) {
          final int pageIndex;
          switch (bankIndexes[i] & 0xFF) {
            case 8:
              pageIndex = 0;
              break;
            case 4:
              pageIndex = 5;
              break;
            case 5:
              pageIndex = 2;
              break;
            default:
              throw new IllegalArgumentException(
                  "Unexpected bank index for Z80 48K mode: " + bankIndexes[i]);
          }
          result[i] = (byte) pageIndex;
        }
      } else {
        result = new byte[bankIndexes.length];
        for (int i = 0; i < bankIndexes.length; i++) {
          final int page = (bankIndexes[i] & 0xFF) - 3;
          if (page >= 0 && page < 8) {
            result[i] = (byte) page;
          }
        }
      }
    }
    return result;
  }

  public static Page makePage(final int cpu, final int page, final ZXPolyData data,
                              final int offset) {
    final byte[] bankData = new byte[PAGE_SIZE];
    System.arraycopy(data.getDataForCPU(cpu), offset, bankData, 0, PAGE_SIZE);
    return new Page(page, bankData);
  }

  public static int getPcReg(final Z80MainHeader mainHeader, final byte[] header) {
    final int version = getVersion(header);
    return version == VERSION_1 ? mainHeader.reg_pc :
        ((header[33] & 0xFF) << 8) | (header[32] & 0xFF);
  }

  public static byte[] extractHeader(final byte[] extra) {
    final int banksInExtra;
    if (extra[0] == 0) {
      banksInExtra = 0;
    } else {
      banksInExtra = extra[0] & 0xFF;
    }

    final byte[] z80SnapshotHeader = new byte[extra.length - (banksInExtra + 1)];
    System.arraycopy(extra, banksInExtra + 1, z80SnapshotHeader, 0, z80SnapshotHeader.length);

    return z80SnapshotHeader;
  }

  public static Z80MainHeader extractZ80SnapshotHeader(final byte[] snapshotHeader)
      throws IOException {
    return Z80_MAINPART.parse(snapshotHeader).mapTo(new Z80MainHeader());
  }

  public static int getPort7ffd(final int version, final byte[] snapshotHeader) {
    final int port7ffd;
    if (version == VERSION_1) {
      port7ffd = 0x30;
    } else {
      final int hwMode = snapshotHeader[34];
      switch (version) {
        case VERSION_2: {
          if (hwMode == 3 || hwMode == 4) {
            port7ffd = snapshotHeader[35] & 0xFF;
          } else {
            port7ffd = 0x30;
          }
        }
        break;
        case VERSION_3A:
        case VERSION_3B: {
          if (hwMode == 4 || hwMode == 5 || hwMode == 6) {
            port7ffd = snapshotHeader[35] & 0xFF;
          } else {
            port7ffd = 0x30;
          }
        }
        break;
        default:
          port7ffd = 0x30;
      }
    }
    return port7ffd;
  }

  @Override
  public String getPluginDescription(final boolean forExport) {
    return forExport ? DESCRIPTION_OUT : DESCRIPTION_IN;
  }

  @Override
  public boolean accept(final File f) {
    return f != null &&
        (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".z80"));
  }

  @Override
  public String getDescription() {
    return DESCRIPTION_IN + " (*.Z80)";
  }

  @Override
  public boolean doesContainInternalFileItems() {
    return false;
  }

  @Override
  public String getPluginUID() {
    return "Z80S";
  }

  @Override
  public List<Info> getImportingContainerFileList(File file) {
    return null;
  }

  @Override
  public String getToolTip(final boolean forExport) {
    return forExport ? DESCRIPTION_OUT : DESCRIPTION_IN;
  }

  @Override
  public javax.swing.filechooser.FileFilter getImportFileFilter() {
    return this;
  }

  @Override
  public javax.swing.filechooser.FileFilter getExportFileFilter() {
    return new javax.swing.filechooser.FileFilter() {
      @Override
      public boolean accept(final File f) {
        return f != null &&
            (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".zxp"));
      }

      @Override
      public String getDescription() {
        return DESCRIPTION_OUT + " (*.ZXP)";
      }
    };
  }

  @Override
  public String getExtension(boolean forExport) {
    return forExport ? "zxp" : "z80";
  }

  @Override
  public ReadResult readFrom(String name, byte[] array, int index) throws IOException {
    if (array.length < 30) {
      throw new IOException("File is too short to be Z80 snapshot");
    }

    final int version = getVersion(array);

    final JBBPParser parser;
    switch (version) {
      case VERSION_1:
        parser = Z80_VERSION1;
        break;
      case VERSION_2:
        parser = Z80_VERSION2;
        break;
      case VERSION_3A:
        parser = Z80_VERSION3A;
        break;
      case VERSION_3B:
        parser = Z80_VERSION3B;
        break;
      default:
        throw new IOException("Detected unsupported version of Z80 snapshot");
    }

    final Z80Snapshot current = parser.parse(array)
        .mapTo(new Z80Snapshot(), new DataProcessor(version),
            JBBPMapper.FLAG_IGNORE_MISSING_VALUES);

    // check hardware mode
    switch (version) {
      case VERSION_1: {
      }
      break;
      case VERSION_2: {
        switch (current.mode) {
          case 0:
          case 1:
          case 3:
          case 4:
            break;
          default:
            throw new IOException("Unsupported Z80 hardware mode [" + current.mode + ']');
        }
      }
      break;
      case VERSION_3A:
      case VERSION_3B: {
        switch (current.mode) {
          case 0:
          case 1:
          case 3:
          case 4:
          case 5:
          case 6:
            break;
          default:
            throw new IOException("Unsupported Z80 hardware mode [" + current.mode + ']');
        }
      }
      break;
    }

    final boolean mode48 = is48k(version, current);

    final int headerLength;
    final byte[] data;
    final int startAddress;

    if (version == VERSION_1) {
      headerLength = 30;
      data = current.data;
      startAddress = current.reg_pc & 0xFFFF;
    } else {
      headerLength = 30 + current.extrahdrlen;
      startAddress = current.reg_pc2;
      if (mode48) {
        data = new byte[PAGE_SIZE * 3];
        for (final Bank b : current.banks) {
          int offset = -1;
          switch (b.page) {
            case 4: {
              offset = PAGE_SIZE;
            }
            break;
            case 5: {
              offset = PAGE_SIZE * 2;
            }
            break;
            case 8: {
              offset = 0x0000;
            }
            break;
          }
          if (offset >= 0) {
            System.arraycopy(b.data, 0, data, offset, PAGE_SIZE);
          }
        }
      } else {
        data = new byte[PAGE_SIZE * 8];
        for (final Bank b : current.banks) {
          if (b.page < 3) {
            final int offset = b.page * PAGE_SIZE;
            System.arraycopy(b.data, 0, data, offset, PAGE_SIZE);
          } else if (b.page <= 10) {
            final int offset = (b.page - 3) * PAGE_SIZE;
            System.arraycopy(b.data, 0, data, offset, PAGE_SIZE);
          }
        }
      }
    }

    final byte[] extra =
        new byte[(version == VERSION_1 ? 1 : 1 + current.banks.length) + headerLength];
    extra[0] = current.banks == null ? (byte) 0 : (byte) current.banks.length;
    int bankIndex = 1;
    if (version != VERSION_1) {
      for (final Bank b : current.banks) {
        extra[bankIndex++] = (byte) b.page;
      }
    }
    System.arraycopy(array, 0, extra, bankIndex, headerLength);
    return new ReadResult(
        new ZXPolyData(new Info(name, 'C', startAddress, data.length, PAGE_SIZE, true, extra),
            this, data), null);
  }

  private CpuRegProperties findCpu(final SessionData sessionData) {
    final String data = sessionData.getExtraProperty(EXTRA_PROPERTY_OVERRIDE_CPU_DATA);
    if (data == null) {
      return new CpuRegProperties();
    }
    return new CpuRegProperties(deserializeProperties(data));
  }

  @Override
  public void writeTo(
      final File file,
      final ZXPolyData data,
      final SessionData sessionData,
      final Object... extraData
  ) throws IOException {
    if (!(data.getPlugin() instanceof Z80InZXPOutPlugin)) {
      throw new IOException("Only imported Z80 snapshot can be exported");
    }
    final CpuRegProperties cpuRegProperties = findCpu(sessionData);

    final Z80ExportDialog dialog = new Z80ExportDialog(this.spriteCorrectorMainFrame);
    dialog.setVisible(true);
    if (!dialog.isAccepted()) {
      return;
    }

    final int videoMode = dialog.getVideoMode();

    final byte[] extra = data.getInfo().getExtra();

    final byte[] bankIndexes;
    if (extra[0] == 0) {
      bankIndexes = new byte[] {8, 4, 5};
    } else {
      bankIndexes = new byte[extra[0] & 0xFF];
      System.arraycopy(extra, 1, bankIndexes, 0, bankIndexes.length);
    }
    final byte[] snapshotHeader = extractHeader(extra);
    final int version = getVersion(snapshotHeader);
    final Z80MainHeader mainSnapshotHeader = extractZ80SnapshotHeader(snapshotHeader);
    final ZXEMLSnapshotFormat block = new ZXEMLSnapshotFormat();

    final int reg_a = cpuRegProperties.extractInt(CpuRegProperties.REG_A, mainSnapshotHeader.reg_a);
    final int reg_f = cpuRegProperties.extractInt(CpuRegProperties.REG_F, mainSnapshotHeader.reg_f);
    final int reg_bc =
        cpuRegProperties.extractInt(CpuRegProperties.REG_BC, mainSnapshotHeader.reg_bc);
    final int reg_de =
        cpuRegProperties.extractInt(CpuRegProperties.REG_DE, mainSnapshotHeader.reg_de);
    final int reg_hl =
        cpuRegProperties.extractInt(CpuRegProperties.REG_HL, mainSnapshotHeader.reg_hl);

    final int reg_a_alt =
        cpuRegProperties.extractInt(CpuRegProperties.REG_ALT_A, mainSnapshotHeader.reg_a_alt);
    final int reg_f_alt =
        cpuRegProperties.extractInt(CpuRegProperties.REG_ALT_F, mainSnapshotHeader.reg_f_alt);
    final int reg_bc_alt =
        cpuRegProperties.extractInt(CpuRegProperties.REG_ALT_BC, mainSnapshotHeader.reg_bc_alt);
    final int reg_de_alt =
        cpuRegProperties.extractInt(CpuRegProperties.REG_ALT_DE, mainSnapshotHeader.reg_de_alt);
    final int reg_hl_alt =
        cpuRegProperties.extractInt(CpuRegProperties.REG_ALT_HL, mainSnapshotHeader.reg_hl_alt);

    final int reg_ix =
        cpuRegProperties.extractInt(CpuRegProperties.REG_IX, mainSnapshotHeader.reg_ix);
    final int reg_iy =
        cpuRegProperties.extractInt(CpuRegProperties.REG_IY, mainSnapshotHeader.reg_iy);

    final int reg_im = cpuRegProperties.extractInt(CpuRegProperties.REG_IM,
        mainSnapshotHeader.emulFlags.interruptmode);
    final int reg_ir = cpuRegProperties.extractInt(CpuRegProperties.REG_IR,
        makePair(mainSnapshotHeader.reg_ir, mainSnapshotHeader.reg_r));

    final int reg_sp =
        cpuRegProperties.extractInt(CpuRegProperties.REG_SP, mainSnapshotHeader.reg_sp);
    final int reg_pc = cpuRegProperties.extractInt(CpuRegProperties.REG_PC,
        getPcReg(mainSnapshotHeader, snapshotHeader));

    final boolean reg_iff =
        cpuRegProperties.extractBoolean(CpuRegProperties.IFF1, mainSnapshotHeader.iff != 0);
    final boolean reg_iff2 =
        cpuRegProperties.extractBoolean(CpuRegProperties.IFF2, mainSnapshotHeader.iff2 != 0);

    final int port_7FFD = cpuRegProperties.extractInt(CpuRegProperties.PORT_7FFD,
        getPort7ffd(version, snapshotHeader));

    for (int cpuIndex = 0; cpuIndex < 4; cpuIndex++) {
      block.setAF(cpuIndex, makePair((byte) reg_a, (byte) reg_f), false);
      block.setAF(cpuIndex, makePair((byte) reg_a_alt, (byte) reg_f_alt), true);
      block.setBC(cpuIndex, reg_bc, false);
      block.setBC(cpuIndex, reg_bc_alt, true);
      block.setDE(cpuIndex, reg_de, false);
      block.setDE(cpuIndex, reg_de_alt, true);
      block.setHL(cpuIndex, reg_hl, false);
      block.setHL(cpuIndex, reg_hl_alt, true);
      block.setRegIX(cpuIndex, reg_ix);
      block.setRegIY(cpuIndex, reg_iy);
      block.setRegIR(cpuIndex, reg_ir);
      block.setRegIM(cpuIndex, reg_im);
      block.setRegPC(cpuIndex, reg_pc);
      block.setRegSP(cpuIndex, reg_sp);
      block.setIFF(cpuIndex, reg_iff);
      block.setIFF2(cpuIndex, reg_iff2);
    }

    block.setPort3D00((videoMode << 2) | 0x80 | 1);

    block.setModulePorts(0, port_7FFD, 0, 0, 0, 0);
    block.setModulePorts(1, port_7FFD, 0x10 | (1 << 1), 0, 0, 0);
    block.setModulePorts(2, port_7FFD, 0x10 | (2 << 1), 0, 0, 0);
    block.setModulePorts(3, port_7FFD, 0x10 | (3 << 1), 0, 0, 0);

    block.setPortFE(mainSnapshotHeader.flags.bordercolor & 7);

    final byte[] pageIndexes =
        convertZ80BankIndexesToPages(bankIndexes, is48k(version, snapshotHeader), version);

    for (int cpu = 0; cpu < 4; cpu++) {
      final List<Page> pages = new ArrayList<>();
      int offsetIndex = 0;
      for (final byte page : pageIndexes) {
        pages.add(makePage(cpu, page & 0xFF, data, offsetIndex * PAGE_SIZE));
        offsetIndex++;
      }
      block.setPages(cpu, new Pages(pages.toArray(new Page[0])));
    }
    saveDataToFile(file, block.save());
  }

  static class Bank {

    final int page;
    final byte[] data;

    Bank(final int page, final byte[] data) {
      this.page = page;
      this.data = data;
    }
  }

  private static class DataProcessor implements JBBPMapperCustomFieldProcessor {

    private final int version;

    private DataProcessor(final int version) {
      this.version = version;
    }

    static byte[] decodeRLE(final byte[] source, int offset, int length) {
      // RLE packed
      // 0xED 0xED repeat value
      // 0x00 0xED 0xED 0x00 - END marker
      final ByteArrayOutputStream result = new ByteArrayOutputStream(PAGE_SIZE);
      while (length > 0) {
        if (length >= 4) {
          if (source[offset] == (byte) 0x00
              && source[offset + 1] == (byte) 0xED
              && source[offset + 2] == (byte) 0xED
              && source[offset + 3] == (byte) 0x00) {
            break;
          }

          if (source[offset] == (byte) 0xED && source[offset + 1] == (byte) 0xED) {
            offset += 2;
            int repeat = source[offset++] & 0xFF;
            final int value = source[offset++] & 0xFF;
            length -= 4;
            while (repeat > 0) {
              result.write(value);
              repeat--;
            }
          } else {
            result.write(source[offset++]);
            length--;
          }
        } else {
          result.write(source[offset++]);
          length--;
        }
      }
      return result.toByteArray();
    }

    static byte[] unpackBank(final byte[] src, int offset, int length) {
      final byte[] result;
      if (length == 0xFFFF) {
        result = new byte[PAGE_SIZE];
        System.arraycopy(src, offset, result, 0, PAGE_SIZE);
      } else {
        result = decodeRLE(src, offset, length);
      }
      return result;
    }

    @Override
    public Object prepareObjectForMapping(JBBPFieldStruct parsedBlock, Bin annotation,
                                          Field field) {
      if (this.version == VERSION_1) {
        if (field.getName().equals("data")) {
          final byte[] data =
              parsedBlock.findFieldForNameAndType("data", JBBPFieldArrayByte.class).getArray();

          if (parsedBlock.findFieldForPathAndType("flags.compressed", JBBPFieldBit.class)
              .getAsBool()) {
            return decodeRLE(data, 0, data.length);
          } else {
            // uncompressed
            return data;
          }
        } else {
          return null;
        }
      } else {
        if (field.getName().equalsIgnoreCase("data")) {
          return parsedBlock.findFieldForNameAndType("data", JBBPFieldArrayByte.class).getArray();
        } else if (field.getName().equalsIgnoreCase("banks")) {
          final byte[] rawdata =
              parsedBlock.findFieldForNameAndType("data", JBBPFieldArrayByte.class).getArray();
          int pos = 0;
          int len = rawdata.length;
          final List<Bank> banks = new ArrayList<>();
          while (len > 0) {
            final int blocklength = ((rawdata[pos++] & 0xFF)) | ((rawdata[pos++] & 0xFF) << 8);
            final int page = rawdata[pos++] & 0xFF;
            len -= 3 + (blocklength == 0xFFFF ? PAGE_SIZE : blocklength);
            final byte[] uncompressed = unpackBank(rawdata, pos, blocklength);
            pos += blocklength == 0xFFFF ? PAGE_SIZE : blocklength;
            banks.add(new Bank(page, uncompressed));
          }
          return banks.toArray(new Bank[0]);
        } else {
          return null;
        }
      }
    }

  }

  public static class EmulFlags {

    @Bin(order = 1, type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_2)
    public byte interruptmode;
    @Bin(order = 2, type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_1)
    public byte issue2emulation;
    @Bin(order = 3, type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_1)
    public byte doubleintfreq;
    @Bin(order = 4, type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_2)
    public byte videosync;
    @Bin(order = 5, type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_2)
    public byte inputdevice;
  }

  public static class Flags {

    @Bin(order = 1, type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_1)
    public byte reg_r_bit7;
    @Bin(order = 2, type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_3)
    public byte bordercolor;
    @Bin(order = 3, type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_1)
    public byte basic_samrom;
    @Bin(order = 4, type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_1)
    public byte compressed;
    @Bin(order = 5, type = BinType.BIT, bitNumber = JBBPBitNumber.BITS_2)
    public byte nomeaning;
  }

  public static class Z80Snapshot {

    @Bin(order = 1)
    public byte reg_a;
    @Bin(order = 2)
    public byte reg_f;
    @Bin(order = 3)
    public short reg_bc;
    @Bin(order = 4)
    public short reg_hl;
    @Bin(order = 5)
    public short reg_pc;
    @Bin(order = 6)
    public short reg_sp;
    @Bin(order = 7)
    public byte reg_ir;
    @Bin(order = 8)
    public byte reg_r;
    @Bin(order = 10)
    public short reg_de;
    @Bin(order = 11)
    public short reg_bc_alt;
    @Bin(order = 12)
    public short reg_de_alt;
    @Bin(order = 13)
    public short reg_hl_alt;
    @Bin(order = 14)
    public byte reg_a_alt;
    @Bin(order = 15)
    public byte reg_f_alt;
    @Bin(order = 16)
    public short reg_iy;
    @Bin(order = 17)
    public short reg_ix;
    @Bin(order = 18)
    public byte iff;
    @Bin(order = 19)
    public byte iff2;
    @Bin(order = 20)
    public EmulFlags emulFlags;
    @Bin(order = 21, custom = true)
    public byte[] data;
    // version 2,3A
    @Bin(type = BinType.USHORT)
    public int extrahdrlen;
    @Bin(type = BinType.USHORT)
    public int reg_pc2;
    @Bin(type = BinType.UBYTE)
    public int mode;
    @Bin(type = BinType.UBYTE)
    public int port7FFD;
    @Bin(type = BinType.UBYTE)
    public int portFF;
    @Bin(type = BinType.BYTE_ARRAY)
    public byte[] extra;
    @Bin(custom = true)
    public Bank[] banks;
    @Bin(order = 9)
    Flags flags;

    public Object newInstance(Class<?> klazz) {
      if (klazz == Flags.class) {
        return new Flags();
      } else if (klazz == EmulFlags.class) {
        return new EmulFlags();
      } else {
        return null;
      }
    }
  }

  public static class Z80MainHeader {

    @Bin(order = 1)
    public byte reg_a;
    @Bin(order = 2)
    public byte reg_f;
    @Bin(order = 3)
    public short reg_bc;
    @Bin(order = 4)
    public short reg_hl;
    @Bin(order = 5)
    public short reg_pc;
    @Bin(order = 6)
    public short reg_sp;
    @Bin(order = 7)
    public byte reg_ir;
    @Bin(order = 8)
    public byte reg_r;

    @Bin(order = 9)
    public Flags flags;

    @Bin(order = 10)
    public short reg_de;
    @Bin(order = 11)
    public short reg_bc_alt;
    @Bin(order = 12)
    public short reg_de_alt;
    @Bin(order = 13)
    public short reg_hl_alt;
    @Bin(order = 14)
    public byte reg_a_alt;
    @Bin(order = 15)
    public byte reg_f_alt;
    @Bin(order = 16)
    public short reg_iy;
    @Bin(order = 17)
    public short reg_ix;
    @Bin(order = 18)
    public byte iff;
    @Bin(order = 19)
    public byte iff2;

    @Bin(order = 20)
    public EmulFlags emulFlags;

    public static Object newInstance(Class<?> klazz) {
      if (klazz == EmulFlags.class) {
        return new EmulFlags();
      } else {
        return null;
      }
    }
  }
}
