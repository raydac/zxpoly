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
      this.crc = crc;
      this.data = data;
      this.owner = disk; 
      this.offset = offset;
      updateCrc();
    }

    public boolean isWriteProtect(){
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

    public void updateCrc(){
      int  lcrc = 0xcdb4;
      for(int off=0;off<SECTOR_SIZE;off++){
        lcrc^=(this.readByte(off) << 8);
        for(int i=0;i<8;i++){
         lcrc<<=1;
         if ((lcrc & 0x10000)!=0){
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
      return  this.offset;
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
    for(int i=0; i<workData.length; i+=SECTOR_SIZE){
      this.sectors[p++] = new Sector(this, (i>>12) & 1, i>>13, ((i >> 8) & 0x0f) + 1,i, workData);
    }
  }

  public Sector findRandomSector(final int side, final int track) {
    Sector sector = findFirstSector(side, track);
    if (sector!=null){
      int toskip = rnd.nextInt(SECTORS_PER_TRACK);
      Sector found = sector;
      while(toskip-->0){
        found = findSectorAfter(found);
      }
      if (found!=null){
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
        if (s.getSide() == side && s.getTrackNumber() == track && s.getSectorId() == sector){
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
