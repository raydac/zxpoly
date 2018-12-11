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
import java.nio.charset.StandardCharsets;
import java.util.*;

public class TRDOSDisk {

  public enum Source {

    SCL,
    TRD
  }

  private static final int MAX_SIDES = 2;
  private static final int MAX_TRACKS_PER_SIDE = 86;
  private static final int SECTORS_PER_TRACK = 16;
  private static final int SECTOR_SIZE = 256;

  private static final Random RND = new Random();

  public static final class Sector {

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
      } else {
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
      } else {
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
    this(Source.TRD, new byte[MAX_SIDES * MAX_TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE], false);
  }

  public TRDOSDisk(final Source src, final byte[] srcData, final boolean writeProtect) {
    this.sectors = new Sector[SECTORS_PER_TRACK * MAX_TRACKS_PER_SIDE * MAX_SIDES];
    final byte[] diskData;

    switch (src) {
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
          throw new RuntimeException("The SCL image needs non-standard disk size [" + size + " blocks]");
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
        throw new Error("Unexpected source [" + src + ']');
    }
    int p = 0;
    this.writeProtect = writeProtect;
    this.data = diskData;
    for (int i = 0; i < diskData.length; i += SECTOR_SIZE) {
      this.sectors[p++] = new Sector(this, decodeSide(i), decodePhysicalTrackIndex(i), decodePhysicalSectorIndex(i), i, diskData);
    }
  }

  public byte[] getDiskData() {
    return this.data;
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

//    if (result == null) {
//      System.out.println("Can't find side = " + side + " ph.track = " + track + " ph.sector = " + physicalSectorIndex);
//    }
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
}
