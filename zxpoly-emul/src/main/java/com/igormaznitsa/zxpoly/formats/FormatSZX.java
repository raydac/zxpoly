package com.igormaznitsa.zxpoly.formats;

import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import com.igormaznitsa.zxpoly.components.snd.AyBasedSoundDevice;
import com.igormaznitsa.zxpoly.components.video.VideoController;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.apache.commons.compress.compressors.deflate.DeflateCompressorInputStream;
import org.apache.commons.io.IOUtils;

public class FormatSZX extends Snapshot {

  @Override
  public String getExtension() {
    return "szx";
  }

  @Override
  public void loadFromArray(
      final File srcFile,
      final Motherboard board,
      final VideoController vc,
      final byte[] array) throws IOException {

    final SzxContainer container = new SzxContainer(new ByteArrayInputStream(array));
    LOGGER.log(Level.INFO, container.toString());

    final ZxPolyModule module = board.getModules()[0];
    final Z80 cpu = module.getCpu();

    final boolean spec128;

    switch (container.getMachineId()) {
      case SzxContainer.ZXSTMID_16K:
      case SzxContainer.ZXSTMID_48K:
      case SzxContainer.ZXSTMID_NTSC48K: {
        spec128 = false;
        doMode48(board);
      }
      break;
      default: {
        spec128 = true;
        doMode128(board);
      }
      break;
    }

    container.getBlocks().stream()
        .filter(x -> x.getId() == SzxContainer.SzxBlock.ID_ZXSTZ80REGS)
        .forEach(x -> {
          x.consume((block, in) -> {
            cpu.setRegisterPair(Z80.REGPAIR_AF, in.readWord());
            cpu.setRegisterPair(Z80.REGPAIR_BC, in.readWord());
            cpu.setRegisterPair(Z80.REGPAIR_DE, in.readWord());
            cpu.setRegisterPair(Z80.REGPAIR_HL, in.readWord());

            cpu.setRegisterPair(Z80.REGPAIR_AF, in.readWord(), true);
            cpu.setRegisterPair(Z80.REGPAIR_BC, in.readWord(), true);
            cpu.setRegisterPair(Z80.REGPAIR_DE, in.readWord(), true);
            cpu.setRegisterPair(Z80.REGPAIR_HL, in.readWord(), true);

            cpu.setRegister(Z80.REG_IX, in.readWord());
            cpu.setRegister(Z80.REG_IY, in.readWord());
            cpu.setRegister(Z80.REG_SP, in.readWord());
            cpu.setRegister(Z80.REG_PC, in.readWord());

            cpu.setRegister(Z80.REG_I, in.readByte());
            cpu.setRegister(Z80.REG_R, in.readByte());
            final boolean iff1 = in.readByte() != 0;
            final boolean iff2 = in.readByte() != 0;
            cpu.setIFF(iff1, iff2);
            cpu.setIM(in.readByte());

            final int dwCyclesStart = in.readDWord();
            final int chHoldIntReqCycles = in.readByte();
            final int chFlags = in.readByte();
            final int wMemPtr = in.readWord();

            cpu.setTstates(dwCyclesStart);

            in.assertNoMoreData();
          });
        });

    container.getBlocks().stream()
        .filter(x -> x.getId() == SzxContainer.SzxBlock.ID_ZXSTAYBLOCK)
        .forEach(x -> x.consume((block, in) -> {
          var flags = in.readByte();
          var currentRegister = in.readByte();
          var registerData = in.readFully(16);
          in.assertNoMoreData();
          var foundAyDevice = board.findIoDevice(AyBasedSoundDevice.class);
          if (foundAyDevice != null) {
            foundAyDevice.setAyAddress(currentRegister);
            for (int i = 0; i < registerData.length; i++) {
              foundAyDevice.setAyRegister(i, registerData[i] & 0xFF);
            }
          }
        }));

    container.getBlocks().stream()
        .filter(x -> x.getId() == SzxContainer.SzxBlock.ID_ZXSTSPECREGS)
        .forEach(x -> x.consume((block, in) -> {
          final int border = in.readByte();
          final int port7FFD = in.readByte();

          final int port1FFDorEFF7 = in.readByte();
          final int portFE = in.readByte();
          final int reserved = in.readDWord();

          if (spec128) {
            module.write7FFD(port7FFD, true);
          }
          board.getVideoController().setBorderColor(border & 7);

          in.assertNoMoreData();
        }));

    container.getBlocks().stream()
        .filter(x -> x.getId() == SzxContainer.SzxBlock.ID_ZXSTRAMPAGE)
        .forEach(x -> x.consume((block, in) -> {
          final int flags = in.readWord();
          final int pageNum = in.readByte();
          byte[] data = in.rest();

          if ((flags & 1) != 0) {
            data = SzxContainer.SzxBlock.decompress(data);
          }
          module.syncWriteHeapPage(pageNum, data);

          in.assertNoMoreData();
        }));
  }

  @Override
  public byte[] saveToArray(final Motherboard board, final VideoController vc) throws IOException {
    throw new IOException("Not-implemented");
  }

  @Override
  public String getName() {
    return "Spectaculator";
  }

  @Override
  public boolean accept(final File f) {
    return f != null &&
        (f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".szx"));
  }

  @Override
  public String getDescription() {
    return "Spectaculator (*.szx)";
  }

  public static class SzxContainer {
    public static final int FLAG_ZXSTMF_ALTERNATETIMINGS = 1;
    public static final int ZXSTMID_16K = 0;
    public static final int ZXSTMID_48K = 1;
    public static final int ZXSTMID_128K = 2;
    public static final int ZXSTMID_PLUS2 = 3;
    public static final int ZXSTMID_PLUS2A = 4;
    public static final int ZXSTMID_PLUS3 = 5;
    public static final int ZXSTMID_PLUS3E = 6;
    public static final int ZXSTMID_PENTAGON128 = 7;
    public static final int ZXSTMID_TC2048 = 8;
    public static final int ZXSTMID_TC2068 = 9;
    public static final int ZXSTMID_SCORPION = 10;
    public static final int ZXSTMID_SE = 11;
    public static final int ZXSTMID_TS2068 = 12;
    public static final int ZXSTMID_PENTAGON512 = 13;
    public static final int ZXSTMID_PENTAGON1024 = 14;
    public static final int ZXSTMID_NTSC48K = 15;
    public static final int ZXSTMID_128KE = 16;
    private static final int MAGIC = makeDword('Z', 'X', 'S', 'T');
    private final List<SzxBlock> blocks;
    private final int magic;
    private final int majorVersion;
    private final int minorVersion;
    private final int machineId;
    private final int flags;

    SzxContainer(final InputStream inputStream) throws IOException {
      final SzxStream in = new SzxStream(inputStream);

      this.magic = in.readDWord();
      if (this.magic != MAGIC) {
        throw new IOException("Stream is not SZF, can't detect magic");
      }
      this.majorVersion = in.readByte();
      this.minorVersion = in.readByte();
      this.machineId = in.readByte();
      this.flags = in.readByte();

      final List<SzxBlock> foundBlocks = new ArrayList<>();
      while (in.available() > 0) {
        foundBlocks.add(new SzxBlock(in));
      }
      this.blocks = Collections.unmodifiableList(foundBlocks);
    }

    private static String asString(final int dword) {
      char d = (char) ((dword >>> 24) & 0xFF);
      char c = (char) ((dword >>> 16) & 0xFF);
      char b = (char) ((dword >>> 8) & 0xFF);
      char a = (char) (dword & 0xFF);

      return "" + (Character.isISOControl(a) ? ' ' : a)
          + (Character.isISOControl(b) ? ' ' : b)
          + (Character.isISOControl(c) ? ' ' : c)
          + (Character.isISOControl(d) ? ' ' : d);
    }

    private static int makeDword(final char a, final char b, final char c, final char d) {
      return ((d & 0xFF) << 24) | ((c & 0xFF) << 16) | ((b & 0xFF) << 8) | (a & 0xFF);
    }

    @Override
    public String toString() {
      return String.format("SzxContainer(major=%d,minor=%d,machine=%d,flags=%d,blocks=%s",
          this.majorVersion,
          this.minorVersion,
          this.machineId,
          this.flags,
          this.blocks.stream().map(SzxBlock::toString)
              .collect(Collectors.joining(",", "[", "]"))
      );
    }

    public List<SzxBlock> getBlocks() {
      return this.blocks;
    }

    public int getMagic() {
      return this.magic;
    }

    public int getMajorVersion() {
      return this.majorVersion;
    }

    public int getMinorVersion() {
      return this.minorVersion;
    }

    public int getMachineId() {
      return this.machineId;
    }

    public int getFlags() {
      return this.flags;
    }

    public static class SzxBlock {
      public static final int ID_ZXSTATASP = makeDword('Z', 'X', 'A', 'T');
      public static final int ID_ZXSTATASPRAM = makeDword('A', 'T', 'R', 'P');
      public static final int ID_ZXSTAYBLOCK = makeDword('A', 'Y', (char) 0, (char) 0);
      public static final int ID_ZXSTCF = makeDword('Z', 'X', 'C', 'F');
      public static final int ID_ZXSTCFRAM = makeDword('C', 'F', 'R', 'P');
      public static final int ID_ZXSTCOVOX = makeDword('C', 'O', 'V', 'X');
      public static final int ID_ZXSTBETA128 = makeDword('B', '1', '2', '8');
      public static final int ID_ZXSTBETADISK = makeDword('B', 'D', 'S', 'K');
      public static final int ID_ZXSTCREATOR = makeDword('C', 'R', 'T', 'R');
      public static final int ID_ZXSTDOCK = makeDword('D', 'O', 'C', 'K');
      public static final int ID_ZXSTDSKFILE = makeDword('D', 'S', 'K', (char) 0);
      public static final int ID_ZXSTGS = makeDword('G', 'S', (char) 0, (char) 0);
      public static final int ID_ZXSTGSRAMPAGE = makeDword('G', 'S', 'R', 'P');
      public static final int ID_ZXSTKEYBOARD = makeDword('K', 'E', 'Y', 'B');
      public static final int ID_ZXSTIF1 = makeDword('I', 'F', '1', (char) 0);
      public static final int ID_ZXSTIF2ROM = makeDword('I', 'F', '2', 'R');
      public static final int ID_ZXSTJOYSTICK = makeDword('J', 'O', 'Y', (char) 0);
      public static final int ID_ZXSTMCART = makeDword('M', 'D', 'R', 'V');
      public static final int ID_ZXSTMOUSE = makeDword('A', 'M', 'X', 'M');
      public static final int ID_ZXSTMULTIFACE = makeDword('M', 'F', 'C', 'E');
      public static final int ID_ZXSTOPUS = makeDword('O', 'P', 'U', 'S');
      public static final int ID_ZXSTOPUSDISK = makeDword('O', 'D', 'S', 'K');
      public static final int ID_ZXSTPLUS3 = makeDword('+', '3', (char) 0, (char) 0);
      public static final int ID_ZXSTPLUSD = makeDword('P', 'L', 'S', 'D');
      public static final int ID_ZXSTPLUSDDISK = makeDword('P', 'D', 'S', 'K');
      public static final int ID_ZXSTRAMPAGE = makeDword('R', 'A', 'M', 'P');
      public static final int ID_ZXSTROM = makeDword('R', 'O', 'M', (char) 0);
      public static final int ID_ZXSTSCLDREGS = makeDword('S', 'C', 'L', 'D');
      public static final int ID_ZXSTSIDE = makeDword('S', 'I', 'D', 'E');
      public static final int ID_ZXSTSPECDRUM = makeDword('D', 'R', 'U', 'M');
      public static final int ID_ZXSTSPECREGS = makeDword('S', 'P', 'C', 'R');
      public static final int ID_ZXSTTAPE = makeDword('T', 'A', 'P', 'E');
      public static final int ID_ZXSTUSPEECH = makeDword('U', 'S', 'P', 'E');
      public static final int ID_ZXSTZXPRINTER = makeDword('Z', 'X', 'P', 'R');
      public static final int ID_ZXSTZ80REGS = makeDword('Z', '8', '0', 'R');

      private final int id;
      private final byte[] data;

      private SzxBlock(final SzxStream in) {
        this.id = in.readDWord();
        final int size = in.readDWord();
        LOGGER.info("Read " + asString(this.id) + " size " + size);
        this.data = in.readFully(size);
      }

      public static byte[] decompress(final byte[] data) {
        try (final DeflateCompressorInputStream in = new DeflateCompressorInputStream(
            new ByteArrayInputStream(data))) {
          final ByteArrayOutputStream out = new ByteArrayOutputStream(data.length << 1);
          final byte[] buffer = new byte[1024];
          int n;
          while (-1 != (n = in.read(buffer))) {
            out.write(buffer, 0, n);
          }
          return out.toByteArray();
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }

      public int getId() {
        return this.id;
      }

      public void consume(final BiConsumer<SzxBlock, SzxStream> consumer) {
        consumer.accept(this, new SzxStream(new ByteArrayInputStream(this.data)));
      }

      @Override
      public String toString() {
        return "SzxBlock(id=" + asString(this.id) + ",size=" + this.data.length + ')';
      }
    }

    public static class SzxStream {
      private final InputStream inputStream;

      public SzxStream(final InputStream inputStream) {
        this.inputStream = Objects.requireNonNull(inputStream);
      }

      public byte[] rest() {
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
          while (this.inputStream.available() > 0) {
            final int data = this.inputStream.read();
            if (data < 0) {
              break;
            }
            baos.write(data);
          }
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
        return baos.toByteArray();
      }

      public void assertNoMoreData() {
        if (this.available() > 0) {
          throw new RuntimeException("Detected unexpected data");
        }
      }

      public int readByte() {
        final int result;
        try {
          result = this.inputStream.read();
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        if (result < 0) {
          throw new RuntimeException(new EOFException("End of file"));
        }
        return result;
      }

      public int readChar() {
        return readByte();
      }

      public int readWord() {
        final int a = readByte();
        final int b = readByte();
        return (b << 8) | a;
      }

      private int readDWord() {
        final int a = readByte();
        final int b = readByte();
        final int c = readByte();
        final int d = readByte();
        return (d << 24) | (c << 16) | (b << 8) | a;
      }

      public int available() {
        try {
          return this.inputStream.available();
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
      }

      public byte[] readFully(final int size) {
        final byte[] result = new byte[size];
        try {
          IOUtils.readFully(this.inputStream, result);
        } catch (IOException ex) {
          throw new RuntimeException(ex);
        }
        return result;
      }
    }
  }
}
