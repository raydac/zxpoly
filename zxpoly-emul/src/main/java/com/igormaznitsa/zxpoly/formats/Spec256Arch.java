package com.igormaznitsa.zxpoly.formats;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.zxpoly.components.RomData;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static org.apache.commons.compress.utils.IOUtils.readFully;

public class Spec256Arch {

  private static final int GFX_PAGE_SIZE = 0x4000 * 8;

  static final Pattern GFn_PATTERN = Pattern.compile("^.*\\.gf(x|[0-7])$");
  private static final Pattern ROM_ABC_PATTERN = Pattern.compile("^.*\\.gf([ab])$");
  private static final Pattern ROM_ROMNUM_PATTERN = Pattern.compile("^(?:.*/)?rom([01])\\.gfx$");
  private static final Pattern BKG_PATTERN = Pattern.compile("^.*\\.b([0-9]{2})$");
  private static final Pattern PALETTE_PATTERN = Pattern.compile("^.*\\.p(al|[0-9]{2})$");
  private static final Pattern SNA_NAME_PATTERN =
          Pattern.compile("([^/]*).sna$", Pattern.CASE_INSENSITIVE);
  private final SNAParser parsedSna;
  private final byte[] xorData;
  private final List<Spec256GfxOrigPage> gfxRoms;
  private final List<Spec256GfxOrigPage> gfxRamPages;
  private final List<Spec256Bkg> backgrounds;
  private final List<Spec256Palette> palettes;
  private final Properties properties;
  private final boolean mode128;
  private final String sha256;
  private final String snaName;

  public Spec256Arch() {
    this.parsedSna = null;
    this.xorData = null;
    this.gfxRoms = null;
    this.gfxRamPages = null;
    this.backgrounds = null;
    this.palettes = null;
    this.properties = null;
    this.mode128 = false;
    this.sha256 = null;
    this.snaName = null;
  }

  public Spec256Arch(final RomData romData, final byte[] zipArchive) throws IOException {
    final FoundSna foundSna = findSna(zipArchive);
    if (foundSna == null) {
      throw new IllegalArgumentException("Archive doesn't contain SNA file");
    }

    final SNAParser parsedSna = foundSna.parsed;
    final String parsedSnaName = foundSna.name;

    try (ZipFile zipFile = new ZipFile(new SeekableInMemoryByteChannel(zipArchive))) {
      final Enumeration<ZipArchiveEntry> iterator = zipFile.getEntries();

      final List<Spec256Bkg> listBackgrounds = new ArrayList<>();
      final List<Spec256Palette> listPalettes = new ArrayList<>();
      final List<Spec256GfxOrigPage> romPages = new ArrayList<>();
      final List<Spec256GfxOrigPage> ramPages = new ArrayList<>();
      final Properties properties = new Properties();
      byte[] xorData = null;
      byte[] foundGfxSnapshot = null;

      while (iterator.hasMoreElements()) {
        final ZipArchiveEntry entry = iterator.nextElement();
        if (entry.isDirectory()) {
          continue;
        }
        final String name = entry.getName().replace('\\', '/').toLowerCase(Locale.ENGLISH);
        if (name.endsWith(".xor")) {
          xorData = readData(zipFile, entry);
        } else if (name.endsWith(".cfg")) {
          properties.load(new ByteArrayInputStream(readData(zipFile, entry)));
        } else {
          Matcher matcher = ROM_ABC_PATTERN.matcher(name);
          if (matcher.find()) {
            final int romPageIndex = Character.toLowerCase(matcher.group(1).charAt(0)) - 'a';
            final byte[] read = readData(zipFile, entry);
            if (read.length > 0) {
              romPages.add(
                      new Spec256GfxOrigPage(
                              romPageIndex,
                              romData.makeCopyPage(romPageIndex),
                              copyOf(read, GFX_PAGE_SIZE)));
            }
          } else {
            matcher = ROM_ROMNUM_PATTERN.matcher(name);
            if (matcher.find()) {
              final byte[] read = readData(zipFile, entry);
              if (read.length > 0) {
                final int romPageIndex;
                switch (Integer.parseInt(matcher.group(1))) {
                  case 0:
                    romPageIndex = 1;
                    break;
                  case 1:
                    romPageIndex = 0;
                    break;
                  default:
                    throw new Error("Detected unexpected ROM page index: " + matcher.group(1));
                }
                romPages.add(
                        new Spec256GfxOrigPage(
                                romPageIndex,
                                romData.makeCopyPage(romPageIndex),
                                copyOf(read, GFX_PAGE_SIZE)));
              }
            } else {
              matcher = GFn_PATTERN.matcher(name);
              if (matcher.find()) {
                final String suffix = matcher.group(1);
                final byte[] read = readData(zipFile, entry);
                if ("x".equals(suffix)) {
                  if (read.length != GFX_PAGE_SIZE * 3) {
                    throw new IOException("Unexpected size of GFX block: " + read.length);
                  }
                  foundGfxSnapshot = read;
                } else {
                  if (read.length != GFX_PAGE_SIZE) {
                    throw new IOException("Unexpected size of GFn block: " + read.length);
                  }
                  final int pageIndex = Integer.parseInt(suffix);
                  ramPages.add(
                          new Spec256GfxOrigPage(pageIndex, findRam(pageIndex, parsedSna), read));
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
      }

      if (parsedSna == null) {
        throw new IOException("Can't find SNA file in Spec256 archive");
      }

      if (foundGfxSnapshot == null) {
        throw new IOException(("Can't find GFX file in Spec256 archive"));
      }

      if (foundGfxSnapshot.length != GFX_PAGE_SIZE * 3) {
        throw new IOException("Found GFX file has wrong size: " + foundGfxSnapshot.length);
      }

      this.sha256 = calcSha256(foundSna.fileBody, foundGfxSnapshot);
      this.snaName = parsedSnaName;
      this.parsedSna = parsedSna;
      this.properties = properties;
      this.palettes = Collections.unmodifiableList(listPalettes);
      this.backgrounds = Collections.unmodifiableList(listBackgrounds);
      this.xorData = xorData;
      this.mode128 = this.parsedSna.extendeddata != null;

      final int topRamPageIndex = this.mode128 ? this.parsedSna.extendeddata.port7ffd & 7 : 0;

      final byte[] gfxRam5 = copyOfRange(foundGfxSnapshot, 0, GFX_PAGE_SIZE);
      final byte[] gfxRam2 = copyOfRange(foundGfxSnapshot, GFX_PAGE_SIZE, GFX_PAGE_SIZE * 2);
      final byte[] gfxRamTop =
              copyOfRange(foundGfxSnapshot, GFX_PAGE_SIZE * 2, GFX_PAGE_SIZE * 3);

      if (ramPages.stream().noneMatch(x -> x.getPageIndex() == 5)) {
        ramPages.add(
                new Spec256GfxOrigPage(5, findRam(5, parsedSna), gfxRam5));
      }
      if (ramPages.stream().noneMatch(x -> x.getPageIndex() == 2)) {
        ramPages.add(
                new Spec256GfxOrigPage(2, findRam(2, parsedSna), gfxRam2));
      }
      if (ramPages.stream().noneMatch(x -> x.getPageIndex() == topRamPageIndex)) {
        ramPages.add(new Spec256GfxOrigPage(topRamPageIndex,
                findRam(topRamPageIndex, parsedSna), gfxRamTop));
      }

      this.gfxRamPages = Collections.unmodifiableList(ramPages);
      this.gfxRoms = Collections.unmodifiableList(romPages);
    }
  }

  private static String calcSha256(final byte[] snaBody, final byte[] gfxBody) {
    final byte[] snaPlusGfx = new byte[snaBody.length + gfxBody.length];
    System.arraycopy(snaBody, 0, snaPlusGfx, 0, snaBody.length);
    System.arraycopy(gfxBody, 0, snaPlusGfx, snaBody.length, gfxBody.length);
    return DigestUtils.sha256Hex(snaPlusGfx);
  }

  private static FoundSna findSna(final byte[] zipArchive) throws IOException {
    try (ZipFile zipFile = new ZipFile(new SeekableInMemoryByteChannel(zipArchive))) {
      final Enumeration<ZipArchiveEntry> iterator = zipFile.getEntries();
      while (iterator.hasMoreElements()) {
        final ZipArchiveEntry entry = iterator.nextElement();
        if (entry.isDirectory()) {
          continue;
        }
        final String name = entry.getName().replace('\\', '/').toLowerCase(Locale.ENGLISH);
        if (name.endsWith(".sna")) {
          final Matcher matcher = SNA_NAME_PATTERN.matcher(name);
          if (!matcher.find()) {
            throw new IOException("Unexpected SNA name: " + name);
          }
          String parsedSnaName = matcher.group(1);
          final byte[] snaFileBody = readData(zipFile, entry);
          return new FoundSna(
                  parsedSnaName,
                  snaFileBody,
                  new SNAParser().read(new JBBPBitInputStream(new ByteArrayInputStream(snaFileBody)))
          );
        }
      }
    }
    return null;
  }

  private static byte[] findRam(final int pageIndex, final SNAParser parsedSna) {
    if (pageIndex == 5) {
      return copyOfRange(parsedSna.ramdump, 0x0000, 0x4000);
    } else if (pageIndex == 2) {
      return copyOfRange(parsedSna.ramdump, 0x4000, 0x8000);
    } else {
      if (parsedSna.extendeddata == null) {
        if (pageIndex == 0) {
          return copyOfRange(parsedSna.ramdump, 0x8000, 0xC000);
        } else {
          return new byte[0x4000];
        }
      } else {
        final int topPageIndex = parsedSna.extendeddata.port7ffd & 7;
        if (pageIndex == topPageIndex) {
          return copyOfRange(parsedSna.ramdump, 0x8000, 0xC000);
        } else {
          final int[] pages = new int[]{0, 1, 2, 3, 4, 5, 6, 7};
          pages[5] = -1;
          pages[2] = -1;
          pages[topPageIndex] = -1;
          int extraBankIndex = 0;
          for (int p : pages) {
            if (p < 0) {
              continue;
            }
            if (p == pageIndex) {
              return parsedSna.extendeddata.extrabank[extraBankIndex].getDATA().clone();
            }
            extraBankIndex++;
          }
          throw new IllegalArgumentException("Can't find page for index: " + pageIndex);
        }
      }
    }
  }

  static byte[] readData(final ZipFile file, final ZipArchiveEntry entry)
          throws IOException {
    final byte[] result = new byte[(int) entry.getSize()];
    final int size = readFully(file.getInputStream(entry), result);
    if (size != result.length) {
      throw new IOException("Wrong read size: " + entry);
    }
    return result;
  }

  public String getSnaName() {
    return this.snaName;
  }

  public String getSha256() {
    return this.sha256;
  }

  public SNAParser getParsedSna() {
    return this.parsedSna;
  }

  public byte[] getXorData() {
    return this.xorData;
  }

  public List<Spec256GfxOrigPage> getGfxRoms() {
    return this.gfxRoms;
  }

  public List<Spec256GfxOrigPage> getGfxRamPages() {
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

  public boolean is128() {
    return this.mode128;
  }

  @Override
  public String toString() {
    return String.format("Spec256arch (%s,%s)", this.snaName, this.sha256);
  }

  private static class FoundSna {
    final String name;
    final byte[] fileBody;
    final SNAParser parsed;

    FoundSna(String name, final byte[] fileBody, final SNAParser parsed) {
      this.name = name;
      this.parsed = parsed;
      this.fileBody = fileBody;
    }
  }

  public static class Spec256GfxPage {
    private final int pageIndex;
    private final byte[] gfxData;

    public Spec256GfxPage(final int pageIndex, final byte[] gfxData) {
      this.pageIndex = pageIndex;
      this.gfxData = gfxData;
    }

    public int getPageIndex() {
      return this.pageIndex;
    }

    public byte[] packGfxData() {
      final byte[] result = new byte[this.gfxData.length];

      for (int offset = 0; offset < this.gfxData.length; offset += 8) {
        for (int ctx = 0; ctx < 8; ctx++) {
          final int bitMask = 1 << ctx;
          for (int i = 0; i < 8; i++) {
            result[offset + ctx] |= (byte) ((this.gfxData[offset + i] & bitMask) != 0 ? 1 << i : 0);
          }
        }
      }

      return result;
    }

    public byte[] getGfxData() {
      return this.gfxData;
    }
  }

  public static class Spec256GfxOrigPage extends Spec256GfxPage {
    private final byte[] origData;

    public Spec256GfxOrigPage(final int pageIndex, final byte[] origData, final byte[] gfxData) {
      super(pageIndex, gfxData);
      this.origData = origData;
    }

    public Spec256GfxOrigPage(final Spec256GfxOrigPage orig, final byte[] newGfxData) {
      super(orig.getPageIndex(), newGfxData);
      this.origData = orig.origData;
    }

    public byte[] getOrigData() {
      return this.origData;
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

    public int getIndex() {
      return this.index;
    }

    public byte[] getData() {
      return this.data;
    }

    public int getWidth() {
      return this.width;
    }

    public int getHeight() {
      return this.height;
    }
  }

}
