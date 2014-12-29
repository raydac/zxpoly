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

import java.util.*;

/**
 *
 * @author Igor Maznitsa (http://www.igormaznitsa.com)
 */
public class TRDOSDisk {

  private static final int SIDES = 2;
  private static final int TRACKS_PER_SIDE = 80;
  private static final int SECTORS_PER_TRACK = 16;
  private static final int SECTOR_SIZE = 256;

  public static class Sector {

    private final TRDOSDisk owner;
    private int crc;
    private final byte[] data;
    private final int side;
    private final int track;
    private final int sector;

    private Sector(final TRDOSDisk disk, final int side, final int track, final int sector, final int crc, final byte[] data) {
      this.side = side;
      this.track = track;
      this.sector = sector;
      this.crc = crc;
      this.data = data;
      this.owner = disk; 
    }

    public boolean isWriteProtect(){
      return this.owner.isWriteProtect();
    }
    
    public boolean isLastOnTrack() {
      return this.sector == SECTORS_PER_TRACK;
    }

    public int getSide() {
      return this.side;
    }

    public int getTrack() {
      return this.track;
    }

    public int getSector() {
      return this.sector;
    }

    public int getCrc() {
      return this.crc;
    }

    public int readByte(final int offsetFromSectorStart) {
      if (offsetFromSectorStart < 0 || offsetFromSectorStart >= SECTOR_SIZE) {
        return -1;
      }
      else {
        return this.data[getOffset() + offsetFromSectorStart] & 0xFF;
      }
    }

    public boolean writeByte(final int offsetFromSectorStart, final int value) {
      if (offsetFromSectorStart < 0 || offsetFromSectorStart >= SECTOR_SIZE) {
        return false;
      }
      else {
        if (this.owner.isWriteProtect()) {
          return false;
        }
        this.data[getOffset() + offsetFromSectorStart] = (byte) value;
        return true;
      }
    }

    public int size() {
      return SECTOR_SIZE;
    }

    private int getOffset() {
      return  (this.track * SIDES + this.side) * SECTOR_SIZE * SECTORS_PER_TRACK + (this.sector - 1) * SECTOR_SIZE;
    }

    public boolean isCrcOk() {
      return true;
    }
  }

  private byte[] data;
  private boolean writeProtect;
  private final Sector[] sectors;

  public TRDOSDisk() {
    this(new byte[SIDES * TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE], false);
  }

  public TRDOSDisk(final byte[] data, final boolean writeProtect) {
    final byte[] workData = data.length >= (SIDES * TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE) ? data : Arrays.copyOf(data, SIDES * TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE);
    this.data = workData;
    this.writeProtect = writeProtect;

    this.sectors = new Sector[SECTORS_PER_TRACK*TRACKS_PER_SIDE*SIDES];

    int p = 0;
    for(int side = 0; side<SIDES;side++){
      for(int track=0;track<TRACKS_PER_SIDE;track++){
        for(int sector=0;sector<SECTORS_PER_TRACK;sector++){
          this.sectors[p++] = new Sector(this, side, track, sector+1, track, workData);
        }
      }
    }
  }

  public Sector findFirstSector(final int side, final int track) {
    Sector result = null;

    for (final Sector s : this.sectors) {
      if (s.getSide() == side && s.getTrack() == track) {
        result = s;
        break;
      }
    }

    return result;
  }
  
  public Sector findSectorAfter(final Sector sector) {
    boolean next = false;
    for(final Sector s : this.sectors){
      if (next){
        return s;
      }
      if (s == sector){
        next = true;
      }
    }
    return null;
  }
  
  public Sector findSector(final int side, final int track, final int sector) {
    Sector result = null;

      for(final Sector s : this.sectors){
        if (s.getSide() == side && s.getTrack() == track && s.getSector() == sector){
          result = s;
          break;
        }
      }

    if (result == null) {
      System.out.println("Can't find side = " + side + " track = " + track + " sector = " + sector);
    }
    return result;
  }

  public void write(final int address, final int value) {
    if (!this.writeProtect) {
      this.data[address] = (byte) value;
    }
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
