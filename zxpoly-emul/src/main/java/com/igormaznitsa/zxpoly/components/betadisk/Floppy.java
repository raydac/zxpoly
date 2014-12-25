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
public class Floppy {
  
  public static class Sector {
    private int side;
    private int track;
    private int index;
    private int crc;
    private byte [] data;
    private int offset;
    private int length;
    private final Track parent;
    
    private Sector(final Track parent, final int side, final int track, final int index, final int crc, final byte [] data, final int offset, final int length){
      this.parent = parent;
      this.side = side;
      this.track = track;
      this.index = index;
      this.crc = crc;
      this.data = data;
      this.offset = offset;
      this.length = length;
    }
    
    public int getSide(){
      return this.side;
    }
    
    public int getTrack(){
      return this.track;
    }
    
    public int getIndex(){
      return this.index;
    }
    
    public int getCrc(){
      return this.crc;
    }
    
    public int readByte(final int offsetFromSectorStart){
      if (offsetFromSectorStart<0 || offsetFromSectorStart>=this.length){
        return -1;
      }else{
        return this.data[offsetFromSectorStart+this.offset] & 0xFF;
      }
    }

    public boolean writeByte(final int offsetFromSectorStart, final int value){
      if (offsetFromSectorStart<0 || offsetFromSectorStart>=this.length){
        return false;
      }else{
        if (this.parent.parent.isWriteProtect()) return false;
        this.data[offsetFromSectorStart+this.offset] = (byte)value;
        return true;
      }
    }
  }
  
  public static class Track {
    private final Sector [] sectors;
    private final int side;
    private final int index;
    private final byte [] data;
    private final int offset;
    private final int length;
    private final Floppy parent;
    
    private Track(final Floppy parent, final int side, final int index, final int sectors, final int sectorLength, final byte [] data, final int offset){
      this.parent = parent;
      this.side = side;
      this.index = index;
      this.data = data;
      this.offset = offset;
      this.sectors = new Sector[sectors];
      int curoffset = offset;
      for(int i=0;i<sectors;i++){
        this.sectors[i] = new Sector(this, side, index, i, 0, data, curoffset, sectorLength);
        curoffset += sectorLength;
      }
      this.length = curoffset-offset;
    }
  
    public int getSide(){
      return this.side;
    }
    
    public int getIndex(){
      return this.index;
    }
    
    public int getOffset(){
      return this.offset;
    }
    
    public Sector getSector(final int index){
      if (index<0 || index>=this.sectors.length){
        return null;
      }else{
        return this.sectors[index];
      }
    }
    
    public int size(){
      return this.length;
    }
  }
  private static final int SIDES = 2;
  private static final int TRACKS_PER_SIDE = 80;
  private static final int SECTORS_PER_TRACK = 16;
  private static final int SECTOR_SIZE = 256;
  
  private byte[] data;
  private boolean writeProtect;
  int i_Track;
  private final int sides;
  private int currentTrackIndex;
  private final Track [] tracks;
  
  public Floppy() {
    this(new byte[SIDES*TRACKS_PER_SIDE*SECTORS_PER_TRACK*SECTOR_SIZE],false);
  }

  public int getCurrentTrackIndex(){
    return this.currentTrackIndex;
  }
  
  public void setCurrentTrackIndex(final int index){
    this.currentTrackIndex = Math.max(0, Math.min(this.tracks.length-1, index));
  }
  
  public Floppy(final byte[] data, final boolean writeProtect) {
    final byte [] normaldata = data.length>=(SIDES * TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE) ? data : Arrays.copyOf(data, SIDES * TRACKS_PER_SIDE * SECTORS_PER_TRACK * SECTOR_SIZE);
    this.data = normaldata;
    this.writeProtect = writeProtect;
    
    final List<Track> tracklist = new ArrayList<>();
    
    int side = 0;
    int trackIndex = 0;
    int offset = 0;
    
    while(offset<data.length){
      final Track newtrack = new Track(this, side, trackIndex, SECTORS_PER_TRACK, SECTOR_SIZE, normaldata, offset);
      tracklist.add(newtrack);
      offset += newtrack.size();
      if (offset < data.length){
        trackIndex++;
        if (trackIndex>=TRACKS_PER_SIDE){
          trackIndex = 0;
          side++;
        }
      }
    }
    
    this.tracks = tracklist.toArray(new Track[tracklist.size()]);
    this.sides = side+1;
  }

  public Sector getSector(final int side, final int track, final int sector){
    if (side<0 || side>1 || track<0 || track>=TRACKS_PER_SIDE || sector<0 || sector>=SECTORS_PER_TRACK) return null;
    return this.tracks[side*TRACKS_PER_SIDE + track].getSector(sector);
  }
  
  public void write(final int address, final int value){
    if (!this.writeProtect){
      this.data[address] = (byte)value;
    }
  }
  
  public int getSides(){
    return this.sides;
  }
  
  public int read(final int address){
    System.out.println("Read from disk "+address);
    return this.data[address] & 0xFF;
  }
  
  public boolean isWriteProtect(){
    return this.writeProtect;
  }
  
  public int size(){
    return this.data.length;
  }

  @Override
  public String toString(){
    return "Floppy(sides="+this.sides+", tracks="+this.tracks.length+", size="+this.data.length+')';
  }
}
