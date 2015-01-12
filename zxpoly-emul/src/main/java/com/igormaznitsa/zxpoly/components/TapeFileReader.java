/*
 * Copyright (C) 2015 Raydac Research Group Ltd.
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
package com.igormaznitsa.zxpoly.components;

import com.igormaznitsa.jbbp.JBBPParser;
import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.jbbp.mapper.Bin;
import com.igormaznitsa.jbbp.model.JBBPFieldArrayStruct;
import java.io.*;

public class TapeFileReader {

  private static final long PULSELEN_PILOT = 2168L;
  private static final long PULSELEN_SYNC_ON = 667L;
  private static final long PULSELEN_SYNC_OFF = 735L;
  private static final long PULSELEN_ZERO = 855L;
  private static final long PULSELEN_ONE = 1710L;
  private static final long LENGTH_PILOT_HEADER = 8063L;
  private static final long LENGTH_PILOT_DATA = 3223L;

  private static final JBBPParser TAP_FILE_PARSER = JBBPParser.prepare("tapblocks [_]{ <ushort len; byte flag; byte [len-2] data; byte checksum;}");

  private enum State {

    STOPPED,
    INBETWEEN,
    PILOT,
    SYNCHRO,
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
      return this.flag < 0x80;
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

    public void writeAsUnsigned8BitMonoPCMData(final OutputStream out, final int cyclesPerSample) throws IOException {
      // header
      generateImpulses(out, cyclesPerSample, PULSELEN_PILOT, PULSELEN_PILOT, isHeader() ? LENGTH_PILOT_HEADER : LENGTH_PILOT_DATA);
      // sync
      generateImpulses(out, cyclesPerSample, PULSELEN_SYNC_ON, PULSELEN_SYNC_OFF, 1);
      // flag
      int thedata = this.flag;
      int mask = 0x80;
      while (mask != 0) {
        if ((thedata & mask) == 0) {
          generateImpulses(out, cyclesPerSample, PULSELEN_ZERO, PULSELEN_ZERO, 1);
        }
        else {
          generateImpulses(out, cyclesPerSample, PULSELEN_ONE, PULSELEN_ONE, 1);
        }
        mask >>>= 1;
      }
      // data
      for (final byte b : this.data) {
        thedata = b;
        mask = 0x80;
        while (mask != 0) {
          if ((thedata & mask) == 0) {
            generateImpulses(out, cyclesPerSample, PULSELEN_ZERO, PULSELEN_ZERO, 1);
          }
          else {
            generateImpulses(out, cyclesPerSample, PULSELEN_ONE, PULSELEN_ONE, 1);
          }
          mask >>>= 1;
        }
      }
      // checksum
      thedata = this.checksum;
      mask = 0x80;
      while (mask != 0) {
        if ((thedata & mask) == 0) {
          generateImpulses(out, cyclesPerSample, PULSELEN_ZERO, PULSELEN_ZERO, 1);
        }
        else {
          generateImpulses(out, cyclesPerSample, PULSELEN_ONE, PULSELEN_ONE, 1);
        }
        mask >>>= 1;
      }
    }
  }

  private final class DataBuffer {

    private int databuffer;
    private int mask;
    private long counter;

    public void load(final int data) {
      this.databuffer = data;
      this.mask = 0x80;
      timingForBit();
      inState = true;
    }

    private void timingForBit() {
      this.counter = (this.databuffer & this.mask) == 0 ? PULSELEN_ZERO : PULSELEN_ONE;
    }

    public boolean process(final long machineCycles) {
      boolean result = false;
      this.counter -= machineCycles;
      if (this.counter <= 0) {
        if (inState) {
          timingForBit();
          inState = false;
        }
        else {
          this.mask >>>= 1;
          if (this.mask == 0) {
            result = true;
          }
          else {
            timingForBit();
            inState = true;
          }
        }
      }
      return result;
    }
  }

  private TapBlock current;
  private State state = State.STOPPED;
  private long counterMain;
  private long counterEx;
  private boolean inState;
  private final DataBuffer buffer = new DataBuffer();

  public TapeFileReader(final InputStream tap) throws IOException {
    final JBBPFieldArrayStruct parsed = TAP_FILE_PARSER.parse(tap).findFirstFieldForType(JBBPFieldArrayStruct.class);
    if (parsed.size() == 0) {
      this.current = null;
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
    this.inState = false;
    this.state = State.PILOT;
    this.counterMain = -1L;
    return true;
  }

  public synchronized void stopPlay() {
    this.inState = false;
    this.counterMain = -1L;
    this.state = State.STOPPED;
  }

  public synchronized void rewindToStart() {
    if (this.current != null) {
      while (this.current.prev != null) {
        this.current = this.current.prev;
      }
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
        return true;
      }
      else {
        return false;
      }
    }
  }

  public synchronized boolean in() {
    boolean result = false;
    if (this.state != State.STOPPED) {
      result = this.inState;
    }
    return result;
  }

  public synchronized byte[] getAsWAV() throws IOException {
    final int FREQ = 22050;
    final int CYCLESPERSAMPLE = (int) ((1000000000L / (long) FREQ) / 286L);

    final ByteArrayOutputStream data = new ByteArrayOutputStream(1000000);

    TapBlock block = this.current.findFirst();
    do {
      for (int i = 0; i < 30000; i++) {
        data.write(0);
      }
      block.writeAsUnsigned8BitMonoPCMData(data, CYCLESPERSAMPLE);
      block = block.next;
    }
    while (block != null);

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

  private void initSignalSimulation(final long lengthOn, final long number) {
    this.counterEx = lengthOn;
    this.counterMain = number;
    this.inState = true;
  }

  private boolean processSignalSimulation(final long lengthOn, final long lengthOff, final long spentMachineCycles) {
    boolean result = false;
    this.counterEx -= spentMachineCycles;
    if (this.counterEx <= 0) {
      if (this.inState) {
        this.inState = false;
        this.counterEx = lengthOff;
      }
      else {
        this.counterMain--;
        if (this.counterMain <= 0) {
          this.inState = false;
          this.counterMain = -1L;
          result = true;
        }
        else {
          this.counterEx = lengthOn;
          this.inState = true;
        }
      }
    }
    return result;
  }

  public synchronized void updateForSpentMachineCycles(final long machineCycles) {
    if (this.state != State.STOPPED) {
      final TapBlock block = this.current;

      switch (this.state) {
        case INBETWEEN: {
          this.inState = false;
          if (counterMain < 0) {
            System.out.println("INBETWEEN");
            this.counterMain = LENGTH_PILOT_DATA * PULSELEN_PILOT;
            this.inState = false;
          }
          else {
            this.counterMain -= machineCycles;
            if (this.counterMain <= 0L) {
              this.state = State.PILOT;
              this.counterMain = -1L;
            }
            this.inState = true;
          }
        }
        break;
        case PILOT: {
          if (this.counterMain < 0L) {
            System.out.println("PILOT");
            initSignalSimulation(PULSELEN_PILOT, block.isHeader() ? LENGTH_PILOT_HEADER : LENGTH_PILOT_DATA);
          }
          else {
            if (processSignalSimulation(PULSELEN_PILOT, PULSELEN_PILOT, machineCycles)) {
              this.state = State.SYNCHRO;
            }
          }
        }
        break;
        case SYNCHRO: {
          if (this.counterMain < 0L) {
            System.out.println("SYNCHRO");
            initSignalSimulation(PULSELEN_SYNC_ON, 1);
          }
          else {
            if (processSignalSimulation(PULSELEN_SYNC_ON, PULSELEN_SYNC_OFF, machineCycles)) {
              this.state = State.FLAG;
            }
          }
        }
        break;
        case FLAG: {
          if (this.counterMain < 0L) {
            System.out.println("FLAG");
            this.counterMain = 0L;
            this.buffer.load(block.flag);
          }
          else {
            if (this.buffer.process(machineCycles)) {
              this.counterMain = -1L;
              this.state = State.DATA;
            }
          }
        }
        break;
        case DATA: {
          if (this.counterMain < 0L) {
            System.out.println("DATA");
            this.counterMain = 0L;
            this.buffer.load(block.data[(int) this.counterMain]);
          }
          else {
            if (this.buffer.process(machineCycles)) {
              this.counterMain++;
              if (this.counterMain < block.data.length) {
                this.buffer.load(block.data[(int) this.counterMain]);
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
            System.out.println("CHECKSUM");
            this.counterMain = 0L;
            this.buffer.load(block.checksum);
          }
          else {
            if (this.buffer.process(machineCycles)) {
              this.counterMain = -1L;
              if (!this.rewindToNextBlock()){
                this.state = State.STOPPED;
              }else{
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
