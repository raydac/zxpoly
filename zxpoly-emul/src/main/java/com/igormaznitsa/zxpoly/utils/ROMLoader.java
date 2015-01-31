/*
 * Copyright (C) 2015 Raydac Research Group Ltd.
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
package com.igormaznitsa.zxpoly.utils;

import com.igormaznitsa.zxpoly.components.RomData;
import java.io.*;
import org.apache.commons.compress.archivers.zip.*;
import org.apache.commons.io.IOUtils;
import org.apache.commons.net.ftp.*;

public class ROMLoader {

  public static final String FTP_ROM_HOST = "ftp.worldofspectrum.org";
  public static final String FTP_ROM_FILE_PATH = "/pub/sinclair/emulators/pc/russian/ukv12f5.zip";
  private static final String ROM_48 = "48.rom";
  private static final String ROM_128TR = "128tr.rom";
  private static final String ROM_TRDOS = "trdos.rom";

  public ROMLoader() {

  }

  byte[] loadFTPArchive() throws IOException {
    final FTPClient client = new FTPClient();
    client.connect(FTP_ROM_HOST);
    int replyCode = client.getReplyCode();

    if (FTPReply.isPositiveCompletion(replyCode)) {
      try {
        client.login("anonymous", "anonymous");
        client.setFileType(FTP.BINARY_FILE_TYPE);
        client.enterLocalPassiveMode();

        final ByteArrayOutputStream out = new ByteArrayOutputStream(300000);
        if (client.retrieveFile(FTP_ROM_FILE_PATH, out)) {
          return out.toByteArray();
        }
        else {
          throw new IOException("Can't load file 'ftp://" + FTP_ROM_HOST + FTP_ROM_FILE_PATH + '\'');
        }
      }
      finally {
        client.disconnect();
      }
    }
    else {
      client.disconnect();
      throw new IOException("Can't connect to ftp " + FTP_ROM_HOST);
    }
  }

  public RomData getROM() throws IOException {
    final byte[] loaded = loadFTPArchive();
    final ZipArchiveInputStream in = new ZipArchiveInputStream(new ByteArrayInputStream(loaded));

    byte[] rom48 = null;
    byte[] rom128 = null;
    byte[] romTrDos = null;

    while (true) {
      final ZipArchiveEntry entry = in.getNextZipEntry();
      if (entry == null) {
        break;
      }

      if (entry.isDirectory()) {
        continue;
      }

      if (ROM_48.equalsIgnoreCase(entry.getName())) {
        final int size = (int) entry.getSize();
        if (size > 16384) {
          throw new IOException("ROM 48 has too big size");
        }
        rom48 = new byte[16384];
        IOUtils.readFully(in, rom48, 0, size);
      }
      else if (ROM_128TR.equalsIgnoreCase(entry.getName())) {
        final int size = (int) entry.getSize();
        if (size > 16384) {
          throw new IOException("ROM 128TR has too big size");
        }
        rom128 = new byte[16384];
        IOUtils.readFully(in, rom128, 0, size);
      }
      else if (ROM_TRDOS.equalsIgnoreCase(entry.getName())) {
        final int size = (int) entry.getSize();
        if (size > 16384) {
          throw new IOException("ROM TRDOS has too big size");
        }
        romTrDos = new byte[16384];
        IOUtils.readFully(in, romTrDos, 0, size);
      }
    }

    if (rom48 == null) {
      throw new IOException(ROM_48 + " not found");
    }
    if (rom128 == null) {
      throw new IOException(ROM_128TR + " not found");
    }
    if (romTrDos == null) {
      throw new IOException(ROM_TRDOS + " not found");
    }

    return new RomData(rom48,rom128,romTrDos);
  }
}
