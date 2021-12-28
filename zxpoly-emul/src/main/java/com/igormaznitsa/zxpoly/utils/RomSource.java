package com.igormaznitsa.zxpoly.utils;

import java.util.Set;

public enum RomSource {
  TEST("TEST ROM", AppOptions.TEST_ROM, Set.of(), Set.of(), Set.of()),
  ZX128_WOS("ZX-128 Tr-Dos (WoS)", "http://wos.meulie.net/pub/sinclair/emulators/pc/russian/ukv12f5.zip", Set.of("48.rom"), Set.of("128tr.rom"), Set.of("trdos.rom")),
  ZX128_ARCH("ZX-128 Tr-Dos (Archive.org)", "https://archive.org/download/World_of_Spectrum_June_2017_Mirror/World%20of%20Spectrum%20June%202017%20Mirror.zip/World%20of%20Spectrum%20June%202017%20Mirror/sinclair/emulators/pc/russian/ukv12f5.zip", Set.of("48.rom"), Set.of("128tr.rom"), Set.of("trdos.rom")),
  ZX128_PDP11RU("ZX-128 Tr-Dos (Pdp-11.ru)", "http://mirrors.pdp-11.ru/_zx/vtrdos.ru/emulz/UKV12F5.ZIP", Set.of("48.rom"), Set.of("128tr.rom"), Set.of("trdos.rom")),
  ZX128_VTRDOS("ZX-128 Tr-Dos (VTR-DOS)", "http://trd.speccy.cz/emulz/UKV12F5.ZIP", Set.of("48.rom"), Set.of("128tr.rom"), Set.of("trdos.rom")),
  ZX128_UBUNTU("ZX-128 (ubuntu.com)", "http://es.archive.ubuntu.com/ubuntu/pool/multiverse/s/spectrum-roms/spectrum-roms_20081224-5_all.deb", Set.of("128-1.rom"), Set.of("128-0.rom"), Set.of()),
  AMSTRAD_PLUS2_128_DEBIAN("Amstrad +2 128 (debian.org)", "http://ftp.de.debian.org/debian/pool/non-free/s/spectrum-roms/spectrum-roms_20081224-5_all.deb", Set.of("plus2-1.rom"), Set.of("plus2-0.rom"), Set.of()),
  SPANISH_128_GREENEND("Spanish 128 (greenend.org.uk)", "https://www.chiark.greenend.org.uk/~cjwatson/code/spectrum-roms/spectrum-roms-20081224.tar.gz", Set.of("128-spanish-1.rom"), Set.of("128-spanish-0.rom"), Set.of()),
  UNKNOWN("", "", Set.of(), Set.of(), Set.of());

  private final String title;
  private final String link;
  private final Set<String> rom48names;
  private final Set<String> rom128names;
  private final Set<String> trdosNames;

  RomSource(final String title, final String link, final Set<String> rom48names, final Set<String> rom128names, final Set<String> trdosNames) {
    this.title = title;
    this.link = link;
    this.rom48names = rom48names;
    this.rom128names = rom128names;
    this.trdosNames = trdosNames;
  }

  public static RomSource findForTitle(final String title, final RomSource dflt) {
    for (final RomSource r : RomSource.values()) {
      if (r.title.equalsIgnoreCase(title)) {
        return r;
      }
    }
    return dflt;
  }

  public static RomSource findForLink(final String link, final RomSource dflt) {
    for (final RomSource r : RomSource.values()) {
      if (r.link.equals(link)) {
        return r;
      }
    }
    return dflt;
  }

  public Set<String> getRom48names() {
    return this.rom48names;
  }

  public Set<String> getRom128names() {
    return this.rom128names;
  }

  public Set<String> getTrDosNames() {
    return this.trdosNames;
  }

  @Override
  public String toString() {
    return this.title;
  }

  public String getTitle() {
    return this.title;
  }

  public String getLink() {
    return this.link;
  }
}
