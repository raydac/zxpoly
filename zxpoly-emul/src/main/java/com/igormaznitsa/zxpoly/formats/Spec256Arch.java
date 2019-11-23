package com.igormaznitsa.zxpoly.formats;

import static java.lang.System.arraycopy;
import static org.apache.commons.compress.utils.IOUtils.readFully;


import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.FileUtils;

public class Spec256Arch {

  private static final Pattern GFn_PATTERN = Pattern.compile("^.*\\.gf(x|[0-7])$");
  private static final Pattern ROM_PATTERN = Pattern.compile("^.*\\.gf([ab])$");
  private static final Pattern BKG_PATTERN = Pattern.compile("^.*\\.b([0-9]{2})$");
  private static final Pattern PALETTE_PATTERN = Pattern.compile("^.*\\.p(al|[0-9]{2})$");
  private static final int PAGE_SIZE = 0x4000;
  private final SNAParser parsedSna;
  private final byte[] xorData;
  private final List<Spec256RomBank> gfxRoms;
  private final List<Spec256RamPage> gfxRamPages;
  private final List<Spec256Bkg> backgrounds;
  private final List<Spec256Palette> palettes;
  private final Properties properties;
  private final boolean mode128;

  public Spec256Arch(final File file) throws IOException {
    this(FileUtils.readFileToByteArray(file));
  }

  public Spec256Arch(final byte[] zipArchive) throws IOException {
    final ZipFile zipFile = new ZipFile(new SeekableInMemoryByteChannel(zipArchive));
    final Enumeration<ZipArchiveEntry> iterator = zipFile.getEntries();

    final List<Spec256Bkg> listBackgrounds = new ArrayList<>();
    final List<Spec256Palette> listPalettes = new ArrayList<>();
    final Map<String, byte[]> packedGfxRoms = new HashMap<>();
    final Map<Integer, byte[]> packedGfxRamPages = new HashMap<>();
    final Properties properties = new Properties();
    SNAParser parsedSna = null;
    byte[] xorData = null;

    while (iterator.hasMoreElements()) {
      final ZipArchiveEntry entry = iterator.nextElement();
      if (entry.isDirectory()) {
        continue;
      }
      final String name = entry.getName().replace('\\', '/').toLowerCase(Locale.ENGLISH);
      if (name.endsWith(".sna")) {
        parsedSna = new SNAParser().read(new JBBPBitInputStream(new ByteArrayInputStream(readData(zipFile, entry))));
      } else if (name.endsWith(".xor")) {
        xorData = readData(zipFile, entry);
      } else if (name.endsWith(".cfg")) {
        properties.load(new ByteArrayInputStream(readData(zipFile, entry)));
      } else {
        Matcher matcher = GFn_PATTERN.matcher(name);
        if (matcher.find()) {
          final String suffix = matcher.group(1);
          final byte[] read = readData(zipFile, entry);
          if ("x".equals(suffix)) {
            packedGfxRamPages.put(-1, read);
          } else {
            packedGfxRamPages.put(Integer.parseInt(suffix), read);
          }
        } else {
          matcher = ROM_PATTERN.matcher(name);
          if (matcher.find()) {
            final String romBank = matcher.group(1);
            final byte[] read = readData(zipFile, entry);
            if (read.length > 0) {
              packedGfxRoms.put(romBank, read);
            }
          } else {
            matcher = BKG_PATTERN.matcher(name);
            if (matcher.find()) {
              final int index = Integer.parseInt(matcher.group(1));
              final byte[] data = readData(zipFile, entry);
              listBackgrounds.add(new Spec256Bkg(index, data, 320, 200));
            } else {
              matcher = PALETTE_PATTERN.matcher(name);
              if (matcher.find()) {
                final String suffix = matcher.group(1);
                final byte[] data = readData(zipFile, entry);
                if ("al".equals(suffix)) {
                  listPalettes.add(-1, new Spec256Palette(-1, data));
                } else {
                  final int index = Integer.parseInt(suffix);
                  listPalettes.add(index, new Spec256Palette(index, data));
                }
              }
            }
          }
        }
      }
    }

    if (parsedSna == null) {
      throw new IOException("Can't find SNA file in archive");
    }

    this.parsedSna = parsedSna;
    this.properties = properties;
    this.palettes = Collections.unmodifiableList(listPalettes);
    this.backgrounds = Collections.unmodifiableList(listBackgrounds);
    this.xorData = xorData;
    this.mode128 = this.parsedSna.extendeddata != null;

    final int topRamPageIndex;
    if (this.parsedSna.extendeddata != null) {
      topRamPageIndex = this.parsedSna.extendeddata.port7ffd & 7;
    } else {
      topRamPageIndex = 0;
    }

    final List<Spec256RamPage> gfxRamPages = new ArrayList<>();
    for (final Map.Entry<Integer, byte[]> e : packedGfxRamPages.entrySet()) {
      if (e.getKey() == -1) {
        final byte[] page5 = new byte[0x4000];
        final byte[] page2 = new byte[0x4000];
        final byte[] pageTop = new byte[0x4000];

        final byte[] theData = e.getValue();

        for (int i = 0; i < 8; i++) {
          final int offset = i * PAGE_SIZE * 3;

          arraycopy(theData, offset, page5, 0, PAGE_SIZE);
          arraycopy(theData, offset + 0x4000, page2, 0, PAGE_SIZE);
          arraycopy(theData, offset + 0x8000, pageTop, 0, PAGE_SIZE);

          gfxRamPages.add(new Spec256RamPage(5, i, page5));
          gfxRamPages.add(new Spec256RamPage(2, i, page2));
          gfxRamPages.add(new Spec256RamPage(topRamPageIndex, i, pageTop));
        }
      } else {
        gfxRamPages.addAll(parseRamBanks(e.getKey(), e.getValue()));
      }
    }

    final List<Spec256RomBank> gfxRomBanks = new ArrayList<>();
    for (final Map.Entry<String, byte[]> e : packedGfxRoms.entrySet()) {
      gfxRomBanks.addAll(parseRomBanks(e.getKey().charAt(0), e.getValue()));
    }

    this.gfxRoms = Collections.unmodifiableList(gfxRomBanks);
    this.gfxRamPages = Collections.unmodifiableList(gfxRamPages);
  }

  private static byte[] readData(final ZipFile file, final ZipArchiveEntry entry) throws IOException {
    final byte[] result = new byte[(int) entry.getSize()];
    final int size = readFully(file.getInputStream(entry), result);
    if (size != result.length) {
      throw new IOException("Wrong read size: " + entry);
    }
    return result;
  }

  private List<Spec256RomBank> parseRomBanks(final char bankId, final byte[] data) {
    final List<Spec256RomBank> result = new ArrayList<>();

    final int pages = data.length / 0x4000;
    for (int i = 0; i < pages; i++) {
      final byte[] pageData = new byte[0x4000];
      arraycopy(data, i * 0x4000, pageData, 0, 0x4000);
      result.add(new Spec256RomBank(bankId, i, pageData));
    }

    return result;
  }

  private List<Spec256RamPage> parseRamBanks(final int ramPageIndex, final byte[] data) {
    final List<Spec256RamPage> result = new ArrayList<>();

    final int pages = data.length / 0x4000;
    for (int i = 0; i < pages; i++) {
      final byte[] pageData = new byte[0x4000];
      arraycopy(data, i * 0x4000, pageData, 0, 0x4000);
      result.add(new Spec256RamPage(ramPageIndex, i, pageData));
    }

    return result;
  }

  public SNAParser getParsedSna() {
    return this.parsedSna;
  }

  public byte[] getXorData() {
    return this.xorData;
  }

  public List<Spec256RomBank> getGfxRoms() {
    return this.gfxRoms;
  }

  public List<Spec256RamPage> getGfxRamPages() {
    return this.gfxRamPages;
  }

  public List<Spec256Bkg> getBackgrounds() {
    return this.backgrounds;
  }

  public List<Spec256Palette> getPalettes() {
    return this.palettes;
  }

  public Properties getProperties() {
    return this.properties;
  }

  public boolean isMode128() {
    return this.mode128;
  }

  public static class Spec256RomBank {
    final char bankId;
    final int cpuIndex;
    final byte[] data;

    private Spec256RomBank(final char bankId, final int cpuIndex, final byte[] data) {
      this.bankId = bankId;
      this.cpuIndex = cpuIndex;
      this.data = data;
    }

    public char getBankId() {
      return this.bankId;
    }

    public int getCpuIndex() {
      return this.cpuIndex;
    }

    public byte[] getData() {
      return this.data;
    }
  }

  public static class Spec256RamPage {
    private final int ramPageIndex;
    private final int cpuIndex;
    private final byte[] data;

    private Spec256RamPage(final int ramPageIndex, final int cpuIndex, final byte[] data) {
      this.ramPageIndex = ramPageIndex;
      this.cpuIndex = cpuIndex;
      this.data = data;
    }

    public byte[] getData() {
      return this.data;
    }

    public int getCpuIndex() {
      return this.cpuIndex;
    }

    public int getRamPageIndex() {
      return this.ramPageIndex;
    }
  }

  public static class Spec256Palette {
    private final int index;
    private final byte[] data;

    private Spec256Palette(final int index, final byte[] data) {
      this.index = index;
      this.data = data;
    }

    public int getIndex() {
      return this.index;
    }

    public byte[] getData() {
      return this.data;
    }

    public int size() {
      return this.data.length / 3;
    }

  }

  public static class Spec256Bkg {
    private final int index;
    private final byte[] data;
    private final int width;
    private final int height;

    private Spec256Bkg(final int index, final byte[] data, final int width, final int height) {
      this.index = index;
      this.data = data;
      this.width = width;
      this.height = height;
    }

    public byte[] getData() {
      return this.data;
    }

    public int getWidth() {
      return this.width;
    }

    public int getHeight() {
      return this.width;
    }
  }


}
