package com.igormaznitsa.zxpoly.components.betadisk;

import static java.util.Collections.unmodifiableList;

import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class DskContainer {

  private final boolean extended;
  private final List<Track> tracks;

  private final int trackSize;

  private final int sides;
  private final int tracksPerSide;

  private final int maxSectorSize;

  public DskContainer(final byte[] data) throws IOException {
    final boolean standardDsk = JBBPUtils.arrayStartsWith(data, "MV - CPCEMU ".getBytes(StandardCharsets.US_ASCII));
    final boolean extendedDsk = JBBPUtils.arrayStartsWith(data, "EXTENDED CPC DSK ".getBytes(StandardCharsets.US_ASCII));
    if (data.length < 0x100 || !(standardDsk || extendedDsk)) {
      throw new RuntimeException("Not SCL file");
    }

    this.tracksPerSide = data[0x30] & 0xFF;
    this.sides = data[0x31] & 0xFF;
    final int[] trackSizeTable = new int[this.tracksPerSide * this.sides];

    if (extendedDsk) {
      this.trackSize = -1;
      for (int i = 0; i < this.tracksPerSide * this.sides; i++) {
        trackSizeTable[i] = (data[0x34 + i] & 0xFF) << 8;
      }
    } else {
      this.trackSize = (data[0x32] & 0xFF) | ((data[0x33] & 0xFF) << 8);
      Arrays.fill(trackSizeTable, this.trackSize);
    }

    final List<Track> foundTracks = new ArrayList<>();

    final JBBPBitInputStream inputStream =
        new JBBPBitInputStream(new ByteArrayInputStream(data), false);
    inputStream.readByteArray(0x100);

    for (int i = 0; i < this.tracksPerSide * this.sides && inputStream.hasAvailableData(); i++) {
      foundTracks.add(new Track(extendedDsk, trackSizeTable[i], inputStream));
    }

    this.extended = extendedDsk;
    this.tracks = unmodifiableList(foundTracks);

    this.maxSectorSize = this.tracks.stream()
            .flatMap(x -> x.sectors.stream())
            .mapToInt(x -> x.data.length)
            .max().orElse(0);
  }

  private static int getSectorSizeIndexAsByteLength(final int sectorSizeIndex) {
    switch (sectorSizeIndex) {
      case 0:
        return 128;
      case 1:
        return 256;
      case 2:
        return 512;
      case 3:
        return 1024;
      case 4:
        return 2048;
      case 5:
        return 4096;
      default:
        return -1;
    }
  }

  public int getMaxSectorSize() {
    return this.maxSectorSize;
  }

  public int getSides() {
    return this.sides;
  }

  public int getTracksPerSide() {
    return this.tracksPerSide;
  }

  public int getTrackSize() {
    return this.trackSize;
  }

  public boolean isExtended() {
    return this.extended;
  }

  public List<Track> getTracks() {
    return this.tracks;
  }

  @Override
  public String toString() {
    return String.format("DskContainer[extended=%s,sides=%d,tracksPerSide=%d,tracks=%d,maxSector=%d]",
            this.extended,
            this.sides,
            this.tracksPerSide,
            this.tracks.size(),
            this.maxSectorSize
    );
  }

  public static final class Sector {
    private final int track;
    private final int side;
    private final int sectorId;
    private final int sectorSize;
    private final int fdcStatusReg1;
    private final int fdcStatusReg2;
    private final int actualDataLengthInBytes;

    private final byte[] data;

    private Sector(final boolean extended, final JBBPBitInputStream in) throws IOException {
      this.track = in.readByte();
      this.side = in.readByte();
      this.sectorId = in.readByte();
      this.sectorSize = in.readByte();
      this.fdcStatusReg1 = in.readByte();
      this.fdcStatusReg2 = in.readByte();

      int dataLength = in.readByte() | (in.readByte() << 8);

      if (!extended) {
        dataLength = getSectorSizeIndexAsByteLength(this.sectorSize);
      }
      this.actualDataLengthInBytes = dataLength;

      this.data = new byte[this.actualDataLengthInBytes];
    }

    public int getTrack() {
      return this.track;
    }

    public int getSide() {
      return this.side;
    }

    public int getSectorId() {
      return this.sectorId;
    }

    public int getSectorSize() {
      return this.sectorSize;
    }

    public int getFdcStatusReg1() {
      return this.fdcStatusReg1;
    }

    public int getFdcStatusReg2() {
      return this.fdcStatusReg2;
    }

    public int getActualDataLengthInBytes() {
      return this.actualDataLengthInBytes;
    }

    public byte[] getData() {
      return this.data;
    }

    private void readSectorData(final JBBPBitInputStream inputStream) throws IOException {
      final byte[] dataArray = inputStream.readByteArray(this.data.length);
      System.arraycopy(dataArray, 0, this.data, 0, data.length);
    }
  }

  public static final class Track {
    private final int trackNumber;
    private final int sideNumber;
    private final int sectorSize;
    private final int gap3length;
    private final int fillerByte;

    private final List<Sector> sectors;

    private Track(final boolean extended, final int trackSize, final JBBPBitInputStream in) throws IOException {
      final long trackStartOffset = in.getCounter();
      if (!JBBPUtils.arrayStartsWith(in.readByteArray(12), "Track-Info\r\n".getBytes(StandardCharsets.US_ASCII))) {
        throw new IOException("Can't find expected header of track at #" + Long.toHexString(trackStartOffset).toLowerCase(Locale.ENGLISH));
      }
      if (in.skip(4) != 4) throw new IOException("Can't skip 4 bytes");
      this.trackNumber = in.readByte();
      this.sideNumber = in.readByte();
      if (in.skip(2) != 2) throw new IOException("Can't skip 2 bytes");
      this.sectorSize = in.readByte();

      final int numberOfSectors = in.readByte();
      this.gap3length = in.readByte();
      this.fillerByte = in.readByte();

      final List<Sector> sectorList = new ArrayList<>();
      for (int i = 0; i < numberOfSectors; i++) {
        sectorList.add(new Sector(extended, in));
      }
      this.sectors = unmodifiableList(sectorList);

      for (final Sector s : this.sectors) {
        s.readSectorData(in);
      }

      if (trackSize > 0) {
        final long skip = trackSize - (in.getCounter() - trackStartOffset);
        if (skip < 0L) {
          throw new IOException("Unexpected track size");
        }
        if (in.skip(skip) != skip) throw new IOException("Can't skip bytes till end of track");
      }
    }

    public int getTrackNumber() {
      return this.trackNumber;
    }

    public int getSideNumber() {
      return this.sideNumber;
    }

    public int getSectorSize() {
      return this.sectorSize;
    }

    public int getGap3length() {
      return this.gap3length;
    }

    public int getFillerByte() {
      return this.fillerByte;
    }

    public List<Sector> getSectors() {
      return this.sectors;
    }
  }
}
