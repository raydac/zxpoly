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
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.mapper.BinType;
import com.igormaznitsa.jbbp.mapper.JBBPMapper;
import com.igormaznitsa.jbbp.mapper.JBBPMapperCustomFieldProcessor;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayByte;
import com.igormaznitsa.jbbp.model.JBBPFieldBit;
import com.igormaznitsa.jbbp.model.JBBPFieldStruct;
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
import org.apache.commons.io.FileUtils;

public class Z80Plugin extends AbstractFilePlugin {

  private static final int PAGE_SIZE = 0x4000;

  private static final int VERSION_1 = 0;
  private static final int VERSION_2 = 1;
  private static final int VERSION_3A = 2;
  private static final int VERSION_3B = 3;
  private static final JBBPParser Z80_MAINPART = JBBPParser.prepare(
      "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
  );
  private static final JBBPParser Z80_VERSION1 = JBBPParser.prepare(
      "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "byte [_] data;"
  );
  private static final JBBPParser Z80_VERSION2 = JBBPParser.prepare(
      "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "<ushort extrahdrlen;" // header length
          + "<ushort reg_pc2;"
          + "ubyte mode;"
          + "ubyte port7FFD;"
          + "ubyte portFF;"
          + "byte [18] extra;"// misc non zx or not supported stuff
          + "byte [_] data;"
  );
  private static final JBBPParser Z80_VERSION3A = JBBPParser.prepare(
      "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "<ushort extrahdrlen;" // header length
          + "<ushort reg_pc2;"
          + "ubyte mode;"
          + "ubyte port7FFD;"
          + "ubyte portFF;"
          + "byte [49] extra;" // misc non zx or not supported stuff
          + "byte [_] data;"
  );
  private static final JBBPParser Z80_VERSION3B = JBBPParser.prepare(
      "byte reg_a; byte reg_f; <short reg_bc; <short reg_hl; <short reg_pc; <short reg_sp; byte reg_ir; byte reg_r; "
          + "flags{ bit:1 reg_r_bit7; bit:3 bordercolor; bit:1 basic_samrom; bit:1 compressed; bit:2 nomeaning;}"
          + "<short reg_de; <short reg_bc_alt; <short reg_de_alt; <short reg_hl_alt; byte reg_a_alt; byte reg_f_alt; <short reg_iy; <short reg_ix; byte iff; byte iff2;"
          + "emulFlags{bit:2 interruptmode; bit:1 issue2emulation; bit:1 doubleintfreq; bit:2 videosync; bit:2 inputdevice;}"
          + "<ushort extrahdrlen;" // header length
          + "<ushort reg_pc2;"
          + "ubyte mode;"
          + "ubyte port7FFD;"
          + "ubyte portFF;"
          + "byte [50] extra;" // misc non zx or not supported stuff
          + "byte [_] data;"
  );

  private static boolean is48k(final int version, final Z80Snapshot snapshot) {
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

  private static boolean is48k(final int version, final byte[] header) {
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

  private static int getVersion(final byte[] data) {
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

  private static int makePair(final byte a, final byte b) {
    return ((a & 0xFF) << 8) | (b & 0xFF);
  }

  private static byte[] convertZ80BankIndexesToPages(final byte[] bankIndexes, final boolean mode48, final int version) {
    final byte[] result;
    switch (version) {
      case VERSION_1: {
        result = new byte[] {5, 2, 0};
      }
      break;
      default: {
        if (mode48) {
          result = new byte[bankIndexes.length];
          for (int i = 0; i < bankIndexes.length; i++) {
            final int pageIndex;
            switch (bankIndexes[i] & 0xFF) {
              case 8:
                pageIndex = 5;
                break;
              case 4:
                pageIndex = 2;
                break;
              case 5:
                pageIndex = 0;
                break;
              default:
                throw new IllegalArgumentException("Unexpected bank index for Z80 48K mode: " + bankIndexes[i]);
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
    }
    return result;
  }

  private static Page makePage(final int cpu, final int page, final ZXPolyData data, final int offset) throws IOException {
    final byte[] bankData = new byte[PAGE_SIZE];
    System.arraycopy(data.getDataForCPU(cpu), offset, bankData, 0, PAGE_SIZE);
    return new Page(page, bankData);
  }

  @Override
  public String getPluginDescription(final boolean forExport) {
    return forExport ? "ZXP file" : "Z80 file";
  }

  @Override
  public boolean accept(final File f) {
    return f != null && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".z80"));
  }

  @Override
  public String getDescription() {
    return "Z80 Snapshot (*.Z80)";
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
    return forExport ? "ZXPZ80 snapshot" : "Z80 snapshot file";
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
        return f != null && (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".zxp"));
      }

      @Override
      public String getDescription() {
        return "ZXP Snapshot (*.ZXP)";
      }
    };
  }

  @Override
  public String getExtension(boolean forExport) {
    return forExport ? "zxp" : "z80";
  }

  @Override
  public ReadResult readFrom(final File file, int index) throws IOException {
    final byte[] array = FileUtils.readFileToByteArray(file);

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

    final Z80Snapshot current = parser.parse(array).mapTo(new Z80Snapshot(), new DataProcessor(version), JBBPMapper.FLAG_IGNORE_MISSING_VALUES);

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

    switch (version) {
      case VERSION_1: {
        headerLength = 30;
        data = current.data;
        startAddress = current.reg_pc & 0xFFFF;
      }
      break;
      default: {
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
              for (int i = 0; i < PAGE_SIZE; i++) {
                data[offset + i] = b.data[i];
              }
            }
          }
        } else {
          data = new byte[PAGE_SIZE * 8];
          for (final Bank b : current.banks) {
            if (b.page >= 3 && b.page <= 10) {
              final int offset = (b.page - 3) * PAGE_SIZE;
              for (int i = 0; i < PAGE_SIZE; i++) {
                data[offset + i] = b.data[i];
              }
            }
          }
        }
      }
      break;
    }

    final byte[] extra = new byte[(version == VERSION_1 ? 1 : 1 + current.banks.length) + headerLength];
    extra[0] = current.banks == null ? (byte) 0 : (byte) current.banks.length;
    int bankIndex = 1;
    if (version != VERSION_1) {
      for (final Bank b : current.banks) {
        extra[bankIndex++] = (byte) b.page;
      }
    }
    System.arraycopy(array, 0, extra, bankIndex, headerLength);
    return new ReadResult(new ZXPolyData(new Info(file.getName(), 'C', startAddress, data.length, PAGE_SIZE, extra), this, data), null);
  }

  @Override
  public void writeTo(final File file, final ZXPolyData data, final SessionData sessionData) throws IOException {
    if (!(data.getPlugin() instanceof Z80Plugin)) {
      throw new IOException("Only imported Z80 snapshot can be exported");
    }

    final Z80ExportDialog dialog = new Z80ExportDialog(this.mainFrame);
    dialog.setVisible(true);
    if (!dialog.isAccepted()) {
      return;
    }

    final int videoMode = dialog.getVideoMode();

    final byte[] extra = data.getInfo().getExtra();

    final byte[] bankIndexes;

    final int banksInExtra;
    if (extra[0] == 0) {
      banksInExtra = 0;
      bankIndexes = new byte[] {8, 4, 5};
    } else {
      bankIndexes = new byte[extra[0] & 0xFF];
      banksInExtra = bankIndexes.length;
      for (int i = 0; i < bankIndexes.length; i++) {
        bankIndexes[i] = extra[i + 1];
      }
    }
    final byte[] z80header = new byte[extra.length - (banksInExtra + 1)];
    System.arraycopy(extra, banksInExtra + 1, z80header, 0, z80header.length);
    final int version = getVersion(z80header);
    final Z80MainHeader mheader = Z80_MAINPART.parse(z80header).mapTo(new Z80MainHeader());
    final int regpc = version == VERSION_1 ? mheader.reg_pc : ((z80header[33] & 0xFF) << 8) | (z80header[32] & 0xFF);

    final ZXEMLSnapshotFormat block = new ZXEMLSnapshotFormat();

    for (int cpuIndex = 0; cpuIndex < 4; cpuIndex++) {
      block.setAF(cpuIndex, makePair(mheader.reg_a, mheader.reg_f), false);
      block.setAF(cpuIndex, makePair(mheader.reg_a_alt, mheader.reg_f_alt), true);
      block.setBC(cpuIndex, mheader.reg_bc, false);
      block.setBC(cpuIndex, mheader.reg_bc_alt, true);
      block.setDE(cpuIndex, mheader.reg_de, false);
      block.setDE(cpuIndex, mheader.reg_de_alt, true);
      block.setHL(cpuIndex, mheader.reg_hl, false);
      block.setHL(cpuIndex, mheader.reg_hl_alt, true);
      block.setRegIX(cpuIndex, mheader.reg_ix);
      block.setRegIY(cpuIndex, mheader.reg_iy);
      block.setRegIR(cpuIndex, makePair(mheader.reg_ir, mheader.reg_r));
      block.setRegIM(cpuIndex, mheader.emulFlags.interruptmode);
      block.setRegPC(cpuIndex, regpc);
      block.setRegSP(cpuIndex, mheader.reg_sp);
      block.setIFF(cpuIndex, mheader.iff != 0);
      block.setIFF2(cpuIndex, mheader.iff2 != 0);
    }

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

    block.setPort3D00((videoMode << 2) | 0x80 | 1);

    block.setModulePorts(0, port7ffd, 0, 0, 0, 0);
    block.setModulePorts(1, port7ffd, 0x10 | (1 << 1), 0, 0, 0);
    block.setModulePorts(2, port7ffd, 0x10 | (2 << 1), 0, 0, 0);
    block.setModulePorts(3, port7ffd, 0x10 | (3 << 1), 0, 0, 0);

    block.setPortFE(mheader.flags.bordercolor & 7);

    final byte[] pageIndexes = convertZ80BankIndexesToPages(bankIndexes, is48k(version, z80header), version);

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
    public Object prepareObjectForMapping(JBBPFieldStruct parsedBlock, Bin annotation, Field field) {
      if (this.version == VERSION_1) {
        if (field.getName().equals("data")) {
          final byte[] data = parsedBlock.findFieldForNameAndType("data", JBBPFieldArrayByte.class).getArray();

          if (parsedBlock.findFieldForPathAndType("flags.compressed", JBBPFieldBit.class).getAsBool()) {
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
          final byte[] rawdata = parsedBlock.findFieldForNameAndType("data", JBBPFieldArrayByte.class).getArray();
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

    @Bin(outOrder = 1, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    public byte interruptmode;
    @Bin(outOrder = 2, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    public byte issue2emulation;
    @Bin(outOrder = 3, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    public byte doubleintfreq;
    @Bin(outOrder = 4, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    public byte videosync;
    @Bin(outOrder = 5, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    public byte inputdevice;
  }

  public static class Flags {

    @Bin(outOrder = 1, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    public byte reg_r_bit7;
    @Bin(outOrder = 2, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_3)
    public byte bordercolor;
    @Bin(outOrder = 3, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    public byte basic_samrom;
    @Bin(outOrder = 4, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_1)
    public byte compressed;
    @Bin(outOrder = 5, type = BinType.BIT, outBitNumber = JBBPBitNumber.BITS_2)
    public byte nomeaning;
  }

  public static class Z80Snapshot {

    @Bin(outOrder = 1)
    public byte reg_a;
    @Bin(outOrder = 2)
    public byte reg_f;
    @Bin(outOrder = 3)
    public short reg_bc;
    @Bin(outOrder = 4)
    public short reg_hl;
    @Bin(outOrder = 5)
    public short reg_pc;
    @Bin(outOrder = 6)
    public short reg_sp;
    @Bin(outOrder = 7)
    public byte reg_ir;
    @Bin(outOrder = 8)
    public byte reg_r;

    @Bin(outOrder = 9)
    Flags flags;

    @Bin(outOrder = 10)
    public short reg_de;
    @Bin(outOrder = 11)
    public short reg_bc_alt;
    @Bin(outOrder = 12)
    public short reg_de_alt;
    @Bin(outOrder = 13)
    public short reg_hl_alt;
    @Bin(outOrder = 14)
    public byte reg_a_alt;
    @Bin(outOrder = 15)
    public byte reg_f_alt;
    @Bin(outOrder = 16)
    public short reg_iy;
    @Bin(outOrder = 17)
    public short reg_ix;
    @Bin(outOrder = 18)
    public byte iff;
    @Bin(outOrder = 19)
    public byte iff2;

    @Bin(outOrder = 20)
    public EmulFlags emulFlags;

    @Bin(outOrder = 21, custom = true)
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

    @Bin(outOrder = 1)
    public byte reg_a;
    @Bin(outOrder = 2)
    public byte reg_f;
    @Bin(outOrder = 3)
    public short reg_bc;
    @Bin(outOrder = 4)
    public short reg_hl;
    @Bin(outOrder = 5)
    public short reg_pc;
    @Bin(outOrder = 6)
    public short reg_sp;
    @Bin(outOrder = 7)
    public byte reg_ir;
    @Bin(outOrder = 8)
    public byte reg_r;

    @Bin(outOrder = 9)
    public Flags flags;

    @Bin(outOrder = 10)
    public short reg_de;
    @Bin(outOrder = 11)
    public short reg_bc_alt;
    @Bin(outOrder = 12)
    public short reg_de_alt;
    @Bin(outOrder = 13)
    public short reg_hl_alt;
    @Bin(outOrder = 14)
    public byte reg_a_alt;
    @Bin(outOrder = 15)
    public byte reg_f_alt;
    @Bin(outOrder = 16)
    public short reg_iy;
    @Bin(outOrder = 17)
    public short reg_ix;
    @Bin(outOrder = 18)
    public byte iff;
    @Bin(outOrder = 19)
    public byte iff2;

    @Bin(outOrder = 20)
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
