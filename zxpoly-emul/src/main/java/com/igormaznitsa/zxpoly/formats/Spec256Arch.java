package com.igormaznitsa.zxpoly.formats;

import static org.apache.commons.compress.utils.IOUtils.readFully;


import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry;
import org.apache.commons.compress.archivers.zip.ZipFile;
import org.apache.commons.compress.utils.SeekableInMemoryByteChannel;
import org.apache.commons.io.FileUtils;

public class Spec256Arch {

  private static final int GFX_PAGE_SIZE = 0x4000 * 8;

  private static final Pattern GFn_PATTERN = Pattern.compile("^.*\\.gf(x|[0-7])$");
  private static final Pattern ROM_ABC_PATTERN = Pattern.compile("^.*\\.gf([ab])$");
  private static final Pattern ROM_ROMNUM_PATTERN = Pattern.compile("^(?:.*/)?rom([01])\\.gfx$");
  private static final Pattern BKG_PATTERN = Pattern.compile("^.*\\.b([0-9]{2})$");
  private static final Pattern PALETTE_PATTERN = Pattern.compile("^.*\\.p(al|[0-9]{2})$");
  private static final Pattern SNA_NAME_PATTERN = Pattern.compile("([^/]*).sna$", Pattern.CASE_INSENSITIVE);
  private final SNAParser parsedSna;
  private final byte[] xorData;
  private final List<Spec256GfxPage> gfxRoms;
  private final List<Spec256GfxPage> gfxRamPages;
  private final List<Spec256Bkg> backgrounds;
  private final List<Spec256Palette> palettes;
  private final Properties properties;
  private final boolean mode128;
  private final String sha256;
  private final String snaName;

  public Spec256Arch(final File file) throws IOException {
    this(FileUtils.readFileToByteArray(file));
  }

  public Spec256Arch(final byte[] zipArchive) throws IOException {
    try (ZipFile zipFile = new ZipFile(new SeekableInMemoryByteChannel(zipArchive))) {
      final Enumeration<ZipArchiveEntry> iterator = zipFile.getEntries();

      final List<Spec256Bkg> listBackgrounds = new ArrayList<>();
      final List<Spec256Palette> listPalettes = new ArrayList<>();
      final List<Spec256GfxPage> foundGfxRoms = new ArrayList<>();
      final List<Spec256GfxPage> foundGfxRams = new ArrayList<>();
      final Properties properties = new Properties();
      SNAParser parsedSna = null;
      byte[] xorData = null;
      byte[] foundGfxSnapshot = null;
      byte[] snaFileBody = null;

      String parsedSnaName = null;

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
          parsedSnaName = matcher.group(1);
          snaFileBody = readData(zipFile, entry);
          parsedSna = new SNAParser().read(new JBBPBitInputStream(new ByteArrayInputStream(snaFileBody)));
        } else if (name.endsWith(".xor")) {
          xorData = readData(zipFile, entry);
        } else if (name.endsWith(".cfg")) {
          properties.load(new ByteArrayInputStream(readData(zipFile, entry)));
        } else {
          Matcher matcher = ROM_ABC_PATTERN.matcher(name);
          if (matcher.find()) {
            final int romBankId = matcher.group(1).charAt(0) - 'a';
            final byte[] read = readData(zipFile, entry);
            if (read.length > 0) {
              foundGfxRoms.add(new Spec256GfxPage(romBankId, Arrays.copyOf(read, GFX_PAGE_SIZE)));
            }
          } else {
            matcher = ROM_ROMNUM_PATTERN.matcher(name);
            if (matcher.find()) {
              final byte[] read = readData(zipFile, entry);
              if (read.length > 0) {
                foundGfxRoms.add(new Spec256GfxPage(Integer.parseInt(matcher.group(1)), Arrays.copyOf(read, GFX_PAGE_SIZE)));
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
                  foundGfxRams.add(new Spec256GfxPage(Integer.parseInt(suffix), read));
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

      final byte[] snagfx = new byte[snaFileBody.length + foundGfxSnapshot.length];
      System.arraycopy(snaFileBody, 0, snagfx, 0, snaFileBody.length);
      System.arraycopy(foundGfxSnapshot, 0, snagfx, snaFileBody.length, foundGfxSnapshot.length);
      this.sha256 = DigestUtils.sha256Hex(snagfx);

      this.snaName = parsedSnaName;
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

      final byte[] ram5 = new byte[GFX_PAGE_SIZE];
      final byte[] ram2 = new byte[GFX_PAGE_SIZE];
      final byte[] ramTop = new byte[GFX_PAGE_SIZE];

      System.arraycopy(foundGfxSnapshot, 0, ram5, 0, ram5.length);
      System.arraycopy(foundGfxSnapshot, ram5.length, ram2, 0, ram2.length);
      System.arraycopy(foundGfxSnapshot, ram5.length + ram2.length, ramTop, 0, ramTop.length);

      if (foundGfxRams.stream().noneMatch(x -> x.pageIndex == 5)) {
        foundGfxRams.add(new Spec256GfxPage(5, ram5));
      }
      if (foundGfxRams.stream().noneMatch(x -> x.pageIndex == 2)) {
        foundGfxRams.add(new Spec256GfxPage(2, ram2));
      }
      if (foundGfxRams.stream().noneMatch(x -> x.pageIndex == topRamPageIndex)) {
        foundGfxRams.add(new Spec256GfxPage(topRamPageIndex, ramTop));
      }

      this.gfxRamPages = Collections.unmodifiableList(foundGfxRams);
      this.gfxRoms = Collections.unmodifiableList(foundGfxRoms);

    }
  }

  public String getSnaName() {
    return this.snaName;
  }

  public String getSha256() {
    return this.sha256;
  }

  private static byte[] readData(final ZipFile file, final ZipArchiveEntry entry) throws IOException {
    final byte[] result = new byte[(int) entry.getSize()];
    final int size = readFully(file.getInputStream(entry), result);
    if (size != result.length) {
      throw new IOException("Wrong read size: " + entry);
    }
    return result;
  }

  public SNAParser getParsedSna() {
    return this.parsedSna;
  }

  public byte[] getXorData() {
    return this.xorData;
  }

  public List<Spec256GfxPage> getGfxRoms() {
    return this.gfxRoms;
  }

  public List<Spec256GfxPage> getGfxRamPages() {
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

  public static class Spec256GfxPage {
    private final int pageIndex;
    private final byte[] data;

    private Spec256GfxPage(final int pageIndex, final byte[] data) {
      this.pageIndex = pageIndex;
      this.data = data.clone();
    }

    public byte[] getData() {
      return this.data;
    }

    public int getPageIndex() {
      return this.pageIndex;
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

  @Override
  public String toString() {
    return String.format("Spec256arch (%s,%s)", this.snaName, this.sha256);
  }

}
