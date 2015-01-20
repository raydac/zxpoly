/*
 * Copyright (C) 2014 Raydac Research Group Ltd.
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
package com.igormaznitsa.zxpoly.components.betadisk;

import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.nio.charset.Charset;
import java.util.*;

public class TRDOSDisk {

  public enum Source {

    SCL,
    TRD
  }

  private static final int SIDES = 2;
  private static final int TRACKS_PER_SIDE = 80;
  private static final int SECTORS_PER_TRACK = 16;
  private static final int SECTOR_SIZE = 256;

  private static final Random rnd = new Random();

  public static class Sector {

    private final TRDOSDisk owner;
    private int crc;
    private final byte[] data;
    private final int side;
    private final int track;
    private final int sectorId;
    private final int offset;

    private Sector(final TRDOSDisk disk, final int side, final int track, final int sector, final int offset, final byte[] data) {
      this.side = side;
      this.track = track;
      this.sectorId = sector;
      this.data = data;
      this.owner = disk;
      this.offset = offset;
      updateCrc();
    }

    public boolean isWriteProtect() {
      return this.owner.isWriteProtect();
    }

    public boolean isLastOnTrack() {
      return this.sectorId == SECTORS_PER_TRACK;
    }

    public int getSide() {
      return this.side;
    }

    public int getTrackNumber() {
      return this.track;
    }

    public int getSectorId() {
      return this.sectorId;
    }

    public int getCrc() {
      return this.crc;
    }

    public int readByte(final int offsetAtSector) {
      if (offsetAtSector < 0 || offsetAtSector >= SECTOR_SIZE) {
        return -1;
      }
      else {
        return this.data[getOffset() + offsetAtSector] & 0xFF;
      }
    }

    public void updateCrc() {
      int lcrc = 0xcdb4;
      for (int off = 0; off < SECTOR_SIZE; off++) {
        lcrc ^= (this.readByte(off) << 8);
        for (int i = 0; i < 8; i++) {
          lcrc <<= 1;
          if ((lcrc & 0x10000) != 0) {
            lcrc ^= 0x1021;
          }
        }
      }
      this.crc = lcrc & 0xFFFF;
    }

    public boolean writeByte(final int offsetAtSector, final int value) {
      if (offsetAtSector < 0 || offsetAtSector >= SECTOR_SIZE) {
        return false;
      }
      else {
        if (this.owner.isWriteProtect()) {
          return false;
        }
        this.data[getOffset() + offsetAtSector] = (byte) value;
        this.updateCrc();
        return true;
      }
    }

    public int size() {
      return SECTOR_SIZE;
    }

    private int getOffset() {
      return this.offset;
    }

    public boolean isCrcOk() {
      return true;
    }
  }

  private byte[] data;
  private boolean writeProtect;
  private final Sector[] sectors;

  public TRDOSDisk() {
    this(Source.TRD, new byte[SIDES * TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE], false);
  }

  public TRDOSDisk(final Source src, final byte[] data, final boolean writeProtect) {
    this.sectors = new Sector[SECTORS_PER_TRACK * TRACKS_PER_SIDE * SIDES];
    final byte[] workData;

    switch (src) {
      case SCL: {
        if (data.length < 10 || !JBBPUtils.arrayStartsWith(data, "SINCLAIR".getBytes(Charset.forName("US-ASCII"))) || data.length < (9 + (0x100 + 14) * (data[8] & 0xFF))) {
          throw new RuntimeException("Not SCL file");
        }
        workData = new byte[SIDES * TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE];
        int size = 0;
        final int items = data[8] & 0xFF;
        for (int i = 0; i < items; i++) {
          size += data[9 + 14 * i + 13] & 0xFF;
        }
        if (size > 2544) {
          throw new RuntimeException("The SCL image needs non-standard disk size [" + size + " blocks]");
        }
        else {
          // make catalog area
          int dataOffset = 0x0000;
          int sector = 0;
          int track = 1;
          int processedSectors = 0;
          for (int i = 0; i < items; i++) {
            final int itemOffset = 9 + 14 * i;
            System.arraycopy(data, itemOffset, workData, dataOffset, 14);
            final int sectorsForFile = data[itemOffset + 13] & 0xFF;
            dataOffset += 14;

            workData[dataOffset++] = (byte) sector;
            workData[dataOffset++] = (byte) track;

            final int srcFileOffset = 9 + 14 * items + processedSectors * SECTOR_SIZE;
            final int dstFileOffset = track * SECTORS_PER_TRACK * SECTOR_SIZE + sector * SECTOR_SIZE;
            System.arraycopy(data, srcFileOffset, workData, dstFileOffset, sectorsForFile * SECTOR_SIZE);

            processedSectors += sectorsForFile;
            for (int s = 0; s < sectorsForFile; s++) {
              sector++;
              if (sector == 0x10) {
                track++;
                sector = 0;
              }
            }
          }
          // system sector
          dataOffset = 8 * SECTOR_SIZE; // sector 9
          workData[dataOffset++] = 0x00; // must be zero

          dataOffset += 224; // empty

          workData[dataOffset++] = (byte) sector; // number of the first free sector
          workData[dataOffset++] = (byte) track; // number of the first free track

          workData[dataOffset++] = 0x10; // disk type
          workData[dataOffset++] = (byte) items; // number of files

          final int freeSectors = 2544 - processedSectors;
          workData[dataOffset++] = (byte) (freeSectors & 0xFF); // number of free sectors
          workData[dataOffset++] = (byte) (freeSectors >> 8);

          workData[dataOffset++] = 0x10; // ID of TRDOS

          //not used
          for (int e = 0; e < 2; e++) {
            workData[dataOffset++] = 0;
          }
          //not used
          for (int e = 0; e < 9; e++) {
            workData[dataOffset++] = 32;
          }
          workData[dataOffset++] = 0; // not used

          workData[dataOffset++] = 0x00; // number of deleted files

          // name of disk, no more than 11 chars
          for (final char ch : "SCLIMAGE".toCharArray()) {
            workData[dataOffset++] = (byte) ch;
          }
          //not used
          for (int e = 0; e < 3; e++) {
            workData[dataOffset++] = 0; // 
          }
        }
      }
      break;
      case TRD: {
        workData = data.length >= (SIDES * TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE) ? data : Arrays.copyOf(data, SIDES * TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE);
      }
      break;
      default:
        throw new Error("Unexpected source [" + src + ']');
    }
    int p = 0;
    this.writeProtect = writeProtect;
    this.data = workData;
    for (int i = 0; i < workData.length; i += SECTOR_SIZE) {
      this.sectors[p++] = new Sector(this, (i >> 12) & 1, i >> 13, ((i >> 8) & 0x0f) + 1, i, workData);
    }
  }

  public Sector findRandomSector(final int side, final int track) {
    Sector sector = findFirstSector(side, track);
    if (sector != null) {
      int toskip = rnd.nextInt(SECTORS_PER_TRACK);
      Sector found = sector;
      while (toskip-- > 0) {
        found = findSectorAfter(found);
      }
      if (found != null) {
        sector = found;
      }
    }
    return sector;
  }

  public Sector findFirstSector(final int side, final int track) {
    Sector result = null;

    for (final Sector s : this.sectors) {
      if (s.getSide() == side && s.getTrackNumber() == track) {
        result = s;
        break;
      }
    }

    return result;
  }

  public Sector findSectorAfter(final Sector sector) {
    boolean next = false;
    for (final Sector s : this.sectors) {
      if (next) {
        return s;
      }
      if (s == sector) {
        next = true;
      }
    }
    return null;
  }

  public Sector findSector(final int side, final int track, final int sector) {
    Sector result = null;

    for (final Sector s : this.sectors) {
      if (s.getSide() == side && s.getTrackNumber() == track && s.getSectorId() == sector) {
        result = s;
        break;
      }
    }

    if (result == null) {
      System.out.println("Can't find side = " + side + " track = " + track + " sector = " + sector);
    }
    return result;
  }

  public int getSides() {
    return SIDES;
  }

  public int read(final int address) {
    return this.data[address] & 0xFF;
  }

  public boolean isWriteProtect() {
    return this.writeProtect;
  }

  public int size() {
    return this.data.length;
  }
}
