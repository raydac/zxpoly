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

package com.igormaznitsa.zxpoly.components.betadisk;

import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Random;

public class TrDosDisk {

  private static final int MAX_SIDES = 2;
  private static final int MAX_TRACKS_PER_SIDE = 86;
  private static final int SECTORS_PER_TRACK = 16;
  private static final int SECTOR_SIZE = 256;
  private static final Random RND = new Random();
  private final byte[] data;
  private final Sector[] sectors;
  private boolean writeProtect;
  private File srcFile;
  private SourceDataType type;
  public TrDosDisk() {
    this(null, SourceDataType.TRD, new byte[MAX_SIDES * MAX_TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE], false);
  }
  public TrDosDisk(final File srcFile, final SourceDataType type, final byte[] srcData, final boolean writeProtect) {
    this.srcFile = srcFile;
    this.type = type;
    this.sectors = new Sector[SECTORS_PER_TRACK * MAX_TRACKS_PER_SIDE * MAX_SIDES];
    final byte[] diskData;

    switch (type) {
      case SCL: {
        if (srcData.length < 10 || !JBBPUtils.arrayStartsWith(srcData, "SINCLAIR".getBytes(StandardCharsets.US_ASCII)) || srcData.length < (9 + (0x100 + 14) * (srcData[8] & 0xFF))) {
          throw new RuntimeException("Not SCL file");
        }
        diskData = new byte[MAX_SIDES * MAX_TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE];
        int size = 0;
        final int items = srcData[8] & 0xFF;
        for (int i = 0; i < items; i++) {
          size += srcData[9 + 14 * i + 13] & 0xFF;
        }

        int diskPointer = SECTORS_PER_TRACK * SECTOR_SIZE; // track 1, sector 0

        if (size > 2544) {
          throw new RuntimeException("The SCL image has non-standard number of blocks: " + size);
        } else {
          // make catalog area
          int processedSectors = 0;
          int sclPointer = 9 + 14 * items;
          int track00Pointer = 0x0000;
          for (int itemIndex = 0; itemIndex < items; itemIndex++) {
            final int catalogItemOffset = 9 + 14 * itemIndex;
            System.arraycopy(srcData, catalogItemOffset, diskData, track00Pointer, 14);
            final int lengthInSectors = srcData[catalogItemOffset + 13] & 0xFF;
            track00Pointer += 14;

            diskData[track00Pointer++] = (byte) decodeLogicalSectorIndex(diskPointer);
            diskData[track00Pointer++] = (byte) decodeLogicalTrackIndex(diskPointer);

            for (int s = 0; s < lengthInSectors; s++) {
              System.arraycopy(srcData, sclPointer, diskData, diskPointer, SECTOR_SIZE);

              diskPointer += SECTOR_SIZE;
              sclPointer += SECTOR_SIZE;
            }
            processedSectors += lengthInSectors;
          }
          // system sector
          track00Pointer = 8 * SECTOR_SIZE; // logical sector 8
          diskData[track00Pointer++] = 0x00; // must be zero

          track00Pointer += 224; // empty

          diskData[track00Pointer++] = (byte) decodeLogicalSectorIndex(diskPointer); // index of the first free sector
          diskData[track00Pointer++] = (byte) decodeLogicalTrackIndex(diskPointer); // index of the first free track

          diskData[track00Pointer++] = 0x10; // disk type
          diskData[track00Pointer++] = (byte) items; // number of files

          final int freeSectors = (MAX_SIDES * MAX_TRACKS_PER_SIDE * SECTORS_PER_TRACK - SECTORS_PER_TRACK) - processedSectors;
          diskData[track00Pointer++] = (byte) (freeSectors & 0xFF); // number of free sectors
          diskData[track00Pointer++] = (byte) (freeSectors >> 8);

          diskData[track00Pointer++] = 0x10; // ID of TRDOS

          track00Pointer += 2;//not used

          //not used but filled by 32
          for (int e = 0; e < 9; e++) {
            diskData[track00Pointer++] = 32;
          }

          track00Pointer++; // not used

          diskData[track00Pointer++] = 0x00; // number of deleted files

          // name of disk, no more than 11 chars
          for (final char ch : "SCLIMAGE".toCharArray()) {
            diskData[track00Pointer++] = (byte) ch;
          }
          //not used
          for (int e = 0; e < 3; e++) {
            diskData[track00Pointer++] = 0; //
          }
        }
      }
      break;
      case TRD: {
        diskData = srcData.length >= (MAX_SIDES * MAX_TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE) ? srcData : Arrays.copyOf(srcData, MAX_SIDES * MAX_TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE);
      }
      break;
      default:
        throw new Error("Unexpected source [" + type + ']');
    }
    int p = 0;
    this.writeProtect = writeProtect;
    this.data = diskData;
    for (int i = 0; i < diskData.length; i += SECTOR_SIZE) {
      this.sectors[p++] = new Sector(this, decodeSide(i), decodePhysicalTrackIndex(i), decodePhysicalSectorIndex(i), i, diskData);
    }
  }

  public static int decodePhysicalTrackIndex(final int dataOffset) {
    return dataOffset >> 13;
  }

  public static int decodeSide(final int dataOffset) {
    return (dataOffset >> 12) & 1;
  }

  public static int decodeLogicalTrackIndex(final int dataOffset) {
    return dataOffset / (SECTOR_SIZE * SECTORS_PER_TRACK);
  }

  public static int decodePhysicalSectorIndex(final int dataOffset) {
    return ((dataOffset / SECTOR_SIZE) % SECTORS_PER_TRACK) + 1;
  }

  public static int decodeLogicalSectorIndex(final int dataOffset) {
    return (dataOffset / SECTOR_SIZE) % SECTORS_PER_TRACK;
  }

  public File getSrcFile() {
    return this.srcFile;
  }

  public SourceDataType getType() {
    return this.type;
  }

  public void replaceSrcFile(final File newFile, final SourceDataType type, final boolean resetChangeFlag) {
    this.srcFile = newFile;
    this.type = type;

    if (resetChangeFlag) {
      for (final Sector s : this.sectors) {
        synchronized (s) {
          s.written = false;
        }
      }
    }
  }

  public boolean isChanged() {
    boolean result = false;
    for (final Sector s : this.sectors) {
      synchronized (s) {
        if (s.written) {
          result = true;
          break;
        }
      }
    }
    return result;
  }

  public byte[] getDiskData() {
    synchronized (this.data) {
      return this.data.clone();
    }
  }

  public Sector findRandomSector(final int side, final int track) {
    Sector sector = findFirstSector(side, track);
    if (sector != null) {
      int toskip = RND.nextInt(SECTORS_PER_TRACK);
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

  public Sector findSector(final int side, final int track, final int physicalSectorIndex) {
    Sector result = null;

    for (final Sector s : this.sectors) {
      if (s.getSide() == side && s.getTrackNumber() == track && s.getSectorId() == physicalSectorIndex) {
        result = s;
        break;
      }
    }
    return result;
  }

  public int getSides() {
    return MAX_SIDES;
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

  public enum SourceDataType {

    SCL,
    TRD
  }

  public static final class Sector {

    private final TrDosDisk owner;
    private final byte[] data;
    private final int side;
    private final int track;
    private final int sectorId;
    private final int offset;
    private int crc;
    private boolean written;

    private Sector(final TrDosDisk disk, final int side, final int track, final int sector, final int offset, final byte[] data) {
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
      synchronized (this.data) {
        if (offsetAtSector < 0 || offsetAtSector >= SECTOR_SIZE) {
          return -1;
        } else {
          return this.data[getOffset() + offsetAtSector] & 0xFF;
        }
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
      } else {
        if (this.owner.isWriteProtect()) {
          return false;
        }
        synchronized (this.data) {
          this.data[getOffset() + offsetAtSector] = (byte) value;
          this.written = true;
        }
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
}
