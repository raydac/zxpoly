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
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Random;

import static java.util.Arrays.copyOf;

public class TrDosDisk {

  public static final int MAX_SIDES = 2;
  public static final int MAX_TRACKS_PER_SIDE = 86;
  public static final int SECTORS_PER_TRACK = 16;
  public static final int SECTOR_SIZE = 256;
  private static final Random RND = new Random();
  private final byte[] data;
  private final Sector[] sectors;
  private volatile boolean writeProtect;
  private volatile File srcFile;
  private volatile SourceDataType type;

  private volatile int headIndex = 0;

  public TrDosDisk(final String diskName) {
    this(null, SourceDataType.TRD, makeEmptyTrDosDisk(diskName), false);
  }

  public TrDosDisk(final File srcFile, final SourceDataType type, final byte[] srcData,
                   final boolean writeProtect) {
    this.srcFile = srcFile;
    this.type = type;
    this.sectors = new Sector[SECTORS_PER_TRACK * MAX_TRACKS_PER_SIDE * MAX_SIDES];
    final byte[] diskData;

    switch (type) {
      case SCL: {
        if (srcData.length < 10 ||
                !JBBPUtils.arrayStartsWith(srcData, "SINCLAIR".getBytes(StandardCharsets.US_ASCII)) ||
                srcData.length < (9 + (0x100 + 14) * (srcData[8] & 0xFF))) {
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
          int totallySectors = 0;
          int sclPointer = 9 + 14 * items;

          int track00Pointer = 0x0000;

          for (int idx = 0; idx < items; idx++) {
            final int catalogOffset = 9 + 14 * idx;
            System.arraycopy(srcData, catalogOffset, diskData, track00Pointer, 14);
            final int sizeInSectors = srcData[catalogOffset + 0xD] & 0xFF;
            track00Pointer += 14;

            diskData[track00Pointer++] = (byte) extractLogicalSectorIndex(diskPointer);
            diskData[track00Pointer++] = (byte) extractLogicalTrackIndex(diskPointer);

            for (int s = 0; s < sizeInSectors; s++) {
              System.arraycopy(srcData, sclPointer, diskData, diskPointer, SECTOR_SIZE);

              diskPointer += SECTOR_SIZE;
              sclPointer += SECTOR_SIZE;
            }
            totallySectors += sizeInSectors;
          }
          // system sector
          track00Pointer = 8 * SECTOR_SIZE; // logical sector 8
          diskData[track00Pointer++] = 0x00; // must be zero

          for (int i = 0; i < 224; i++) { // empty 224 bytes
            diskData[track00Pointer++] = 0;
          }

          diskData[track00Pointer++] =
                  (byte) extractLogicalSectorIndex(diskPointer); // index of the first free sector
          diskData[track00Pointer++] =
                  (byte) extractLogicalTrackIndex(diskPointer); // index of the first free track

          diskData[track00Pointer++] = 0x16; // disk type
          diskData[track00Pointer++] = (byte) items; // number of files

          final int freeSectors =
                  (MAX_SIDES * MAX_TRACKS_PER_SIDE * SECTORS_PER_TRACK - SECTORS_PER_TRACK)
                          - totallySectors;
          diskData[track00Pointer++] = (byte) (freeSectors & 0xFF); // number of free sectors
          diskData[track00Pointer++] = (byte) (freeSectors >> 8);

          diskData[track00Pointer++] = 0x10; // sectors on track

          // two zeros
          diskData[track00Pointer++] = 0;
          diskData[track00Pointer++] = 0;

          // 9 spaces
          for (int e = 0; e < 9; e++) {
            diskData[track00Pointer++] = 0x20;
          }

          // one zero
          diskData[track00Pointer++] = 0;

          // number of deleted files
          diskData[track00Pointer++] = 0;

          // name of disk
          final String imageName =
                  srcFile == null ? "Unknown" : FilenameUtils.getBaseName(srcFile.getName());
          for (int i = 0; i < Math.min(8, imageName.length()); i++) {
            diskData[track00Pointer++] = (byte) imageName.charAt(i);
          }
          if (imageName.length() < 8) {
            for (int i = 0; i < 8 - imageName.length(); i++) {
              diskData[track00Pointer++] = (byte) ' ';
            }
          }

          // three zero byte, end of the sector
          for (int e = 0; e < 3; e++) {
            diskData[track00Pointer++] = 0;
          }
        }
      }
      break;
      case TRD: {
        diskData =
                srcData.length >= (MAX_SIDES * MAX_TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE)
                        ? srcData :
                        copyOf(srcData, MAX_SIDES * MAX_TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE);
      }
      break;
      default:
        throw new Error("Unexpected source [" + type + ']');
    }
    int p = 0;
    this.writeProtect = writeProtect;
    this.data = diskData;
    for (int i = 0; i < diskData.length; i += SECTOR_SIZE) {
      this.sectors[p++] =
              new Sector(this, extractSideNumber(i), extractPhysicalTrackIndex(i),
                      extractPhysicalSectorIndex(i),
                      i, diskData);
    }
  }

  private static byte[] makeEmptyTrDosDisk(final String diskName) {
    final byte[] data = new byte[MAX_SIDES * MAX_TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE];

    int offset = SECTOR_SIZE * 8 + 225;

    data[offset++] = 0; // FR_S_NEXT (0x8E1)
    data[offset++] = 1; // FR_T_NEXT (0x8E2)
    data[offset++] = 0x16; // TYPE DISC (0x8E3)
    data[offset++] = 0; // N_FILES (0x8E4)

    data[offset++] = (byte) 0xF0; // N_FRE_SEC (0x8E5)
    data[offset++] = (byte) 0x09; // (0x8E6)

    data[offset++] = 0x10; // MAIN_BYTE // (0x8E7)

    data[offset++] = 0; // ZERO (0x8E8)
    data[offset++] = 0; // ZERO (0x8E9)

    data[offset++] = 0x20; // BLANK9 (0x8EA)
    data[offset++] = 0x20; // (0x8EB)
    data[offset++] = 0x20; // (0x8EC)
    data[offset++] = 0x20; // (0x8ED)
    data[offset++] = 0x20; // (0x8EE)
    data[offset++] = 0x20; // (0x8EF)
    data[offset++] = 0x20; // (0x8F0)
    data[offset++] = 0x20; // (0x8F1)
    data[offset++] = 0x20; // (0x8F2)

    data[offset++] = 0x0; // ZERO (0x8F3)

    data[offset++] = 0x0; // N_DEL_FIL (0x8F4)

    for (int i = 0; i < 9; i++) { // DISC TITL (0x8F5)
      if (i < diskName.length()) {
        final char chr = diskName.charAt(i);
        if (chr < 127 && (Character.isAlphabetic(chr) || Character.isDigit(chr))) {
          data[offset++] = (byte) chr;
        } else {
          data[offset++] = '_';
        }
      } else {
        data[offset++] = 0x20;
      }
    }

    data[offset++] = 0x0; // ZERO
    data[offset++] = 0x0; // ZERO
    data[offset] = 0x0; // ZERO

    return data;
  }

  public static int extractPhysicalTrackIndex(final int dataOffset) {
    return dataOffset >> 13;
  }

  public static int extractSideNumber(final int dataOffset) {
    return (dataOffset >> 12) & 1;
  }

  public static int extractLogicalTrackIndex(final int dataOffset) {
    return dataOffset / (SECTOR_SIZE * SECTORS_PER_TRACK);
  }

  public static int extractPhysicalSectorIndex(final int dataOffset) {
    return ((dataOffset / SECTOR_SIZE) % SECTORS_PER_TRACK) + 1;
  }

  public static int extractLogicalSectorIndex(final int dataOffset) {
    return (dataOffset / SECTOR_SIZE) % SECTORS_PER_TRACK;
  }

  public int getHeadIndex() {
    return this.headIndex;
  }

  public void setHeadIndex(final int index) {
    this.headIndex = index & 1;
  }

  public File getSrcFile() {
    return this.srcFile;
  }

  public SourceDataType getType() {
    return this.type;
  }

  public void replaceSrcFile(final File newFile, final SourceDataType type,
                             final boolean resetChangeFlag) {
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

  public Sector findRandomSector(final int track) {
    Sector sector = findFirstSector(track);
    if (sector != null) {
      int toskip = RND.nextInt(SECTORS_PER_TRACK);
      Sector found = sector;
      while (toskip-- > 0) {
        found = findNextSector(found);
      }
      if (found != null) {
        sector = found;
      }
    }
    return sector;
  }

  public Sector findFirstSector(final int track) {
    Sector result = null;

    for (final Sector s : this.sectors) {
      if (s.getSide() == this.headIndex && s.getTrackNumber() == track) {
        result = s;
        break;
      }
    }

    return result;
  }

  public Sector findNextSector(final Sector sector) {
    final int head = this.headIndex;
    final int track = sector.track;
    final int sectorIndex = (sector.getPhysicalIndex() + 1) % SECTORS_PER_TRACK;

    for (final Sector s : this.sectors) {
      if (s.getSide() == head && s.getTrackNumber() == track && s.getPhysicalIndex() == sectorIndex)
        return s;
    }
    return null;
  }

  public Sector findSector(final int track, final int physicalSectorIndex) {
    Sector result = null;

    for (final Sector s : this.sectors) {
      if (s.getSide() == this.headIndex && s.getTrackNumber() == track &&
              s.getPhysicalIndex() == physicalSectorIndex) {
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
    private final int physicalIndex;
    private final int offset;
    private int crc;
    private boolean written;

    private Sector(final TrDosDisk disk, final int side, final int track, final int physicalIndex,
                   final int offset, final byte[] data) {
      this.side = side;
      this.track = track;
      this.physicalIndex = physicalIndex;
      this.data = data;
      this.owner = disk;
      this.offset = offset;
      updateCrc();
    }

    @Override
    public String toString() {
      return String.format("Sector(side=%d,track=%d,phIndex=%d", this.side, this.track, this.physicalIndex);
    }

    public boolean isWriteProtect() {
      return this.owner.isWriteProtect();
    }

    public boolean isLastOnTrack() {
      return this.physicalIndex == SECTORS_PER_TRACK;
    }

    public int getSide() {
      return this.side;
    }

    public int getTrackNumber() {
      return this.track;
    }

    public int getPhysicalIndex() {
      return this.physicalIndex;
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
