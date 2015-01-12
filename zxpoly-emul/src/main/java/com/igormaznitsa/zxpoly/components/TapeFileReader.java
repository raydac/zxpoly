/*
 * Copyright (C) 2015 Raydac Research Group Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed getSignal the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.igormaznitsa.zxpoly.components;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayStruct;
import java.io.*;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class TapeFileReader {

  private static final Logger log = Logger.getLogger("TAP");

  private static final long PULSELEN_PILOT = 2168L;
  private static final long PULSELEN_SYNC1 = 667L;
  private static final long PULSELEN_SYNC2 = 735L;
  private static final long PULSELEN_ZERO = 855L;
  private static final long PULSELEN_ONE = 1710L;
  private static final long IMPULSNUMBER_PILOT_HEADER = 8063L;
  private static final long IMPULSNUMBER_PILOT_DATA = 3223L;
  private static final long PAUSE_BETWEEN = 7000000L; // two sec

  private static final JBBPParser TAP_FILE_PARSER = JBBPParser.prepare("tapblocks [_]{ <ushort len; byte flag; byte [len-2] data; byte checksum;}");

  private enum State {

    STOPPED,
    INBETWEEN,
    PILOT,
    SYNC1,
    SYNC2,
    FLAG,
    DATA,
    CHECKSUM
  }

  @Bin
  private static final class TapBlock {

    @Bin
    byte flag;
    @Bin
    byte[] data;
    @Bin
    byte checksum;
    transient TapBlock prev;
    transient TapBlock next;

    public boolean isFirst() {
      return this.prev == null;
    }

    public boolean isLast() {
      return this.next == null;
    }

    public boolean isHeader() {
      return (this.flag & 0xFF) < 0x80;
    }

    public TapBlock findFirst() {
      TapBlock result = this;
      while (!result.isFirst()) {
        result = result.prev;
      }
      return result;
    }

    private void generateImpulses(final OutputStream out, final int cyclesPerSample, final long cyclesOn, final long cyclesOff, final long repeat) throws IOException {
      final long samplesOn = Math.round((double) cyclesOn / (double) cyclesPerSample);
      final long samplesOff = Math.round((double) cyclesOff / (double) cyclesPerSample);

      for (int i = 0; i < repeat; i++) {
        for (int j = 0; j < samplesOn; j++) {
          out.write(0xFF);
        }
        for (int j = 0; j < samplesOff; j++) {
          out.write(0x00);
        }
      }
    }
  }

  private TapBlock current;
  private State state = State.STOPPED;
  private long counterMain;
  private long counterEx;
  private int mask;
  private int buffered;
  private int controlChecksum;
  private boolean inState;

  public TapeFileReader(final InputStream tap) throws IOException {
    final JBBPFieldArrayStruct parsed = TAP_FILE_PARSER.parse(tap).findFirstFieldForType(JBBPFieldArrayStruct.class);
    if (parsed.size() == 0) {
      this.current = null;
      log.warning("Can't find blocks in TAP file");
    }
    else {
      this.current = parsed.getElementAt(0).mapTo(TapBlock.class);
      this.current.prev = null;
      TapBlock item = this.current;
      for (int i = 1; i < parsed.size(); i++) {
        final TapBlock newitem = parsed.getElementAt(i).mapTo(TapBlock.class);
        newitem.prev = item;
        item.next = newitem;
        item = newitem;
      }
      item.next = null;
      log.info("Pointer to " + makeDescription(this.current));
    }
  }

  public synchronized String getNameOfCurrentBlock() {
    return null;
  }

  public synchronized boolean isPlaying() {
    return this.state != State.STOPPED;
  }

  public synchronized boolean startPlay() {
    if (this.state != State.STOPPED) {
      if (this.current == null) {
        this.state = State.STOPPED;
        return false;
      }
    }
    this.state = State.INBETWEEN;
    this.counterMain = -1L;
    return true;
  }

  public synchronized void stopPlay() {
    this.counterMain = -1L;
    this.state = State.STOPPED;
  }

  public synchronized void rewindToStart() {
    stopPlay();
    if (this.current != null) {
      while (this.current.prev != null) {
        this.current = this.current.prev;
      }
      log.info("Pointer to " + makeDescription(this.current));
    }
  }

  public synchronized boolean rewindToNextBlock() {
    stopPlay();
    if (this.current == null) {
      return false;
    }
    else {
      if (this.current.isLast()) {
        return false;
      }
      else {
        this.current = this.current.next;
        log.info("Pointer to " + makeDescription(this.current));
        return true;
      }
    }
  }

  public synchronized boolean rewindToPrevBlock() {
    stopPlay();
    if (this.current == null) {
      return false;
    }
    else {
      if (!this.current.isFirst()) {
        this.current = this.current.prev;
        log.info("Pointer to " + makeDescription(this.current));
        return true;
      }
      else {
        return false;
      }
    }
  }

  public synchronized boolean getSignal() {
    return this.inState;
  }

  public synchronized byte[] getAsWAV() throws IOException {
    final int FREQ = 22050;
    final int CYCLESPERSAMPLE = (int) ((1000000000L / (long) FREQ) / 286L);

    final ByteArrayOutputStream data = new ByteArrayOutputStream(1000000);

    rewindToStart();
    this.inState = false;
    this.counterMain = -1L;
    this.state = State.INBETWEEN;
    while(this.state!=State.STOPPED){
      data.write(this.inState ? 0xFF : 0x00);
      updateForSpentMachineCycles(CYCLESPERSAMPLE);
    }

    final JBBPOut out = JBBPOut.BeginBin(JBBPByteOrder.LITTLE_ENDIAN);

    return out.
            Byte("RIFF").
            Int(data.size() + 40).
            Byte("WAVE").
            Byte("fmt ").
            Int(16). // Size
            Short(1). // Audio format
            Short(1). // Num channels
            Int(FREQ).// Sample rate
            Int(FREQ). // Byte rate 
            Short(1). // Block align
            Short(8). // Bits per sample
            Byte("data").
            Int(data.size()).
            Byte(data.toByteArray()).End().toByteArray();
  }

  private String makeDescription(final TapBlock block) {
    if (block == null) {
      return "No block";
    }
    else if (block.isHeader() && block.data.length == 17) {
      final StringBuilder name = new StringBuilder();
      switch (block.data[0] & 0xFF) {
        case 0:
          name.append("BASIC");
          break;
        case 1:
          name.append("NUM.ARRAY");
          break;
        case 2:
          name.append("CHR.ARRAY");
          break;
        case 3:
          name.append("CODE");
          break;
        default:
          name.append("UNKNOWN");
          break;
      }

      name.append(" \"");

      for (int i = 1; i < 11; i++) {
        name.append((char) (block.data[i] & 0xFF));
      }

      name.append("\"");

      return name.toString();
    }
    else {
      return "CODE_BLOCK len=#" + Integer.toHexString(block.data.length).toUpperCase(Locale.ENGLISH);
    }
  }

  private void loadDataByteToRead(final int data, final long machineCycles){
    this.controlChecksum ^= data;
    this.mask = 0x80;
    this.buffered = data;
    this.counterEx = ((data & this.mask) == 0 ? PULSELEN_ZERO : PULSELEN_ONE);
    this.inState = !this.inState;
  }
  
  private boolean processDataByte(final long machineCycles){
    boolean result = false;
    long counter = this.counterEx & 0x8000000000000000L;
    this.counterEx = (this.counterEx & 0x7FFFFFFFFFFFFFFFL)-machineCycles;
    if (this.counterEx<=0){
      if (counter!=0){
        this.mask >>>= 1;
        if (this.mask == 0){
          result = true;
        }else{
          this.counterEx = (this.buffered & this.mask) == 0 ? PULSELEN_ZERO : PULSELEN_ONE;
          this.inState = !this.inState;
          counter = 0;
        }
      }else{
        this.counterEx = (this.buffered & this.mask) == 0 ? PULSELEN_ZERO : PULSELEN_ONE;
        counter = 0x8000000000000000L;
        this.inState = !this.inState;
      }
    }
    this.counterEx |= counter;
    return result;
  }
  
  public synchronized void updateForSpentMachineCycles(final long machineCycles) {
    if (this.state != State.STOPPED) {
      final TapBlock block = this.current;

      switch (this.state) {
        case INBETWEEN: {
          if (counterMain < 0) {
            log.info("PAUSE");
            this.inState = false;
            this.counterMain = PAUSE_BETWEEN;
          }
          else {
            this.counterMain -= machineCycles;
            if (this.counterMain <= 0L) {
              this.state = State.PILOT;
              this.counterMain = -1L;
            }
          }
        }
        break;
        case PILOT: {
          if (this.counterMain < 0L) {
            log.log(Level.INFO, "PILOT ("+(block.isHeader() ? "header" : "data")+')');
            this.counterMain = block.isHeader() ? IMPULSNUMBER_PILOT_HEADER : IMPULSNUMBER_PILOT_DATA;
            this.inState = !this.inState;
            this.counterEx = PULSELEN_PILOT;
          }
          else {
            this.counterEx -= machineCycles;
            if (this.counterEx<=0){
              this.counterMain--;
              if (this.counterMain<=0){
                this.state = State.SYNC1;
                this.counterMain = -1L;
              }else{
                this.inState = !inState;
                this.counterEx = PULSELEN_PILOT;
              }
            }
          }
        }
        break;
        case SYNC1: {
          if (this.counterMain < 0L) {
            log.info("SYNC1");
            this.counterEx = PULSELEN_SYNC1;
            this.inState = !inState;
            this.counterMain = 0L;
          }
          else {
            this.counterEx -= machineCycles;
            if (counterEx<=0){
              this.counterMain = -1L;
              this.state = State.SYNC2;
            }
          }
        }
        break;
        case SYNC2: {
          if (this.counterMain < 0L) {
            log.info("SYNC1");
            this.counterEx = PULSELEN_SYNC2;
            this.inState = !inState;
            this.counterMain = 0L;
          }
          else {
            this.counterEx -= machineCycles;
            if (counterEx <= 0) {
              this.counterMain = -1L;
              this.state = State.FLAG;
            }
          }
        }
        break;
        case FLAG: {
          if (this.counterMain < 0L) {
            log.log(Level.INFO, "FLAG (#"+Integer.toHexString(block.flag & 0xFF).toUpperCase(Locale.ENGLISH)+')');
            this.controlChecksum = 0;
            this.counterMain = 0L;
            loadDataByteToRead(block.flag & 0xFF, machineCycles);
          }
          else {
            if (processDataByte(machineCycles)) {
              this.counterMain = -1L;
              this.state = State.DATA;
            }
          }
        }
        break;
        case DATA: {
          if (this.counterMain < 0L) {
            log.log(Level.INFO, "DATA (len=#"+Integer.toHexString(block.data.length & 0xFFFF).toUpperCase(Locale.ENGLISH)+')');
            this.counterMain = 0L;
            loadDataByteToRead(block.data[(int) this.counterMain],machineCycles);
          }
          else {
            if (processDataByte(machineCycles)) {
              this.counterMain++;
              if (this.counterMain < block.data.length) {
                loadDataByteToRead(block.data[(int) this.counterMain],machineCycles);
              }
              else {
                this.counterMain = -1;
                this.state = State.CHECKSUM;
              }
            }
          }
        }
        break;
        case CHECKSUM: {
          if (this.counterMain < 0L) {
            log.log(Level.INFO, "CHK (xor=#{0})", Integer.toHexString(block.checksum & 0xFF).toUpperCase(Locale.ENGLISH));
            if ((block.checksum & 0xFF) != (this.controlChecksum & 0xFF)) {
              log.warning("Different XOR sum : at file #"+Integer.toHexString(block.checksum & 0xFF).toUpperCase(Locale.ENGLISH)+", calculated #"+Integer.toHexString(this.controlChecksum & 0xFF).toUpperCase(Locale.ENGLISH));
            }
            this.counterMain = 0L;
            loadDataByteToRead(block.checksum & 0xFF, machineCycles);
          }
          else {
            if (processDataByte(machineCycles)) {
              this.counterMain = -1L;
              if (!this.rewindToNextBlock()) {
                this.state = State.STOPPED;
              }
              else {
                this.state = State.INBETWEEN;
              }
            }
          }
        }
        break;
        default:
          throw new Error("Unexpected state [" + this.state + ']');
      }
    }
  }
}
