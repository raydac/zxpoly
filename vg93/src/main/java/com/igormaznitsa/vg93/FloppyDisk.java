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
package com.igormaznitsa.vg93;

public final class FloppyDisk {

  public static final class Track {

    private final Sector[] sectors;
    private final int side;
    private final int sectorSize;
    private final int index;

    Track(final int side, final int indexOfTrack, final int numberOfSectors, final int sectorSize) {
      this.side = side;
      this.index = indexOfTrack;
      this.sectors = new Sector[numberOfSectors];
      this.sectorSize = sectorSize;
      final byte[] data = new byte[sectorSize];
      final int crc = VG93.calculateCrc16(data);
      for (int i = 0; i < numberOfSectors; i++) {
        sectors[i] = new Sector(side, indexOfTrack, i + 1, crc, data);
      }
    }

    public int getIndex() {
      return this.index;
    }

    public int getSide() {
      return this.side;
    }

    public int getSectorSize() {
      return this.sectorSize;
    }

    public int size() {
      return this.sectors.length;
    }

    public Sector sectorAt(final int sectorIndex) {
      final Sector result;
      if (sectorIndex >= 0 && sectorIndex < this.sectors.length) {
        result = this.sectors[sectorIndex];
      }
      else {
        result = null;
      }
      return result;
    }

  }

  public static final class Sector {

    private final int track;
    private final int side;
    private final int number;
    private final byte[] data;
    private int crc;

    private Sector(final int side, final int track, final int number, final int crc, final byte[] data) {
      this.side = side;
      this.track = track;
      this.number = number;
      this.data = data.clone();
      this.crc = crc & 0xFFFF;
    }

    public int getTrack() {
      return this.track;
    }

    public int getSide() {
      return this.side;
    }

    public int getNumber() {
      return this.number;
    }

    public boolean isCrcOk(){
      return true;
    }
    
    public void write(final byte[] data, final int offset, final int length, final boolean updateCrc) {
      if (this.data.length != length) {
        throw new IllegalArgumentException("Incompatible length of data block " + this.data.length + "!=" + length);
      }
      System.arraycopy(data, offset, this.data, 0, length);
      if (updateCrc) {
        this.crc = VG93.calculateCrc16(data);
      }
    }

    public void write(final byte[] data, final boolean updateCrc) {
      if (this.data.length != data.length) {
        throw new IllegalArgumentException("Incompatible length of data block " + this.data.length + "!=" + data.length);
      }
      System.arraycopy(data, 0, this.data, 0, data.length);
      if (updateCrc) {
        this.crc = VG93.calculateCrc16(data);
      }
    }

    public void write(final int offset, final int data, final boolean updateCrc) {
      this.data[offset] = (byte) data;
      if (updateCrc) {
        this.crc = VG93.calculateCrc16(this.data);
      }
    }

    public int read(final int offset) {
      return this.data[offset] & 0xFF;
    }

    public int getCrc() {
      return this.crc;
    }

    public void setCrc(final int crc) {
      this.crc = crc & 0xFFFF;
    }
  }

  private final boolean writeProtect;
  private final int sectorSize;
  private final int sectorsPerTrack;
  private final Track[] tracks;

  public FloppyDisk(final byte[] trdData, final boolean writeProtect) {
    this(2, 80, 16, 256, writeProtect);
    final int size = Math.min(trdData.length, 655360);

    int track = 0;
    int sector = 0;

    for (int i = 0; i < size; i += 0x100) {
      final Sector s = getTrack(track).sectorAt(sector);

      sector++;
      if (sector == this.sectorsPerTrack) {
        sector = 0;
        track++;

      }
      else {
        sector++;
      }
      s.write(trdData, i, 0x100, true);
    }

  }

  public FloppyDisk(final int sides, final int tracksPerSide, final int sectorsPerTrack, final int sectorSize, final boolean writeProtect) {
    this.writeProtect = writeProtect;
    this.sectorSize = sectorSize;
    this.tracks = new Track[tracksPerSide*sides];
    this.sectorsPerTrack = sectorsPerTrack;
    int trackIndex = 0;
    for (int side = 0; side < sides; side++) {
      for (int track = 0; track < tracksPerSide; track++) {
        this.tracks[trackIndex++] = new Track(side, track, this.sectorsPerTrack, sectorSize);
      }
    }
  }

  public Track getTrack(final int index) {
    final Track result;
    if (index < 0 || index >= this.tracks.length) {
      result = null;
    }
    else {
      result = this.tracks[index];
    }
    return result;
  }

  public int getSectorsPerTrack() {
    return this.sectorsPerTrack;
  }

  public boolean isWriteProtect() {
    return this.writeProtect;
  }

  public int getSectorSize() {
    return this.sectorSize;
  }

}
