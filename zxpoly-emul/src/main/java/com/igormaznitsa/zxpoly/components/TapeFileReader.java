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

  private static final long PERIOD_PILOT = 2168L;
  private static final long PERIOD_SYNCH1 = 667L;
  private static final long PERIOD_SYNCH2 = 735L;
  private static final long PERIOD_RES = 855L;
  private static final long PERIOD_SET = 1710L;
  private static final long REPEAT_PILOT_HEADER = 8063L;
  private static final long REPEAT_DATA_HEADER = 3223L;
  private static final int REPEAT_BIT = 2;

  private static final JBBPParser TAP_FILE_PARSER = JBBPParser.prepare("tapblocks [_]{ <ushort len; byte flag; byte [len-2] data; byte checksum;}");

  private enum State {

    STOPPED,
    INBETWEEN,
    PILOT,
    SYNCHRO1,
    SYNCHRO2,
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

    private static int calculateMCyclesForByte(int data) {
      int result = 0;
      for (int i = 0; i < 8; i++) {
        result += ((data & 0x80) == 0 ? PERIOD_RES : PERIOD_SET) * REPEAT_BIT;
        data <<= 1;
      }
      return result;
    }

    private void generateImpulses(final OutputStream out, final int cyclesPerSample, final long periodInCycles, final long repeat) throws IOException {
      final long samples = (periodInCycles+(cyclesPerSample>>1))/cyclesPerSample;
      final long middle = samples >> 1;

      for (int i = 0; i < repeat; i++) {
        for (long j = 0; j < samples; j++) {
          if (j < middle) {
            out.write(0x00);
          }else{
            out.write(0xFF);
          }
        } 
      }
    }

    public void writeAsUnsigned8BitMonoPCMData(final OutputStream out, final int cyclesPerSample) throws IOException {
      // header
      generateImpulses(out, cyclesPerSample, PERIOD_PILOT, isHeader() ? REPEAT_PILOT_HEADER : REPEAT_DATA_HEADER);
      // sync1
      generateImpulses(out, cyclesPerSample, PERIOD_SYNCH1, 1);
      // sync2
      generateImpulses(out, cyclesPerSample, PERIOD_SYNCH2, 1);
      // flag
      int thedata = this.flag;
      for (int j = 0; j < 8; j++) {
        if ((thedata & 0x80) == 0) {
          generateImpulses(out, cyclesPerSample, PERIOD_RES, REPEAT_BIT);
        }
        else {
          generateImpulses(out, cyclesPerSample, PERIOD_SET, REPEAT_BIT);
        }
        thedata <<= 1;
      }
      // data
      for (final byte b : this.data) {
        thedata = b;
        for (int j = 0; j < 8; j++) {
          if ((thedata & 0x80) == 0) {
            generateImpulses(out, cyclesPerSample, PERIOD_RES, REPEAT_BIT);
          }
          else {
            generateImpulses(out, cyclesPerSample, PERIOD_SET, REPEAT_BIT);
          }
          thedata <<= 1;
        }
      }
      // flag
      thedata = this.checksum;
      for (int j = 0; j < 8; j++) {
        if ((thedata & 0x80) == 0) {
          generateImpulses(out, cyclesPerSample, PERIOD_RES, REPEAT_BIT);
        }
        else {
          generateImpulses(out, cyclesPerSample, PERIOD_SET, REPEAT_BIT);
        }
        thedata <<= 1;
      }
    }

    public long calculateBlockLengthInCycles() {
      long counter = 0L;

      if (isHeader()) {
        counter += REPEAT_PILOT_HEADER * PERIOD_PILOT;
      }
      else {
        counter += REPEAT_DATA_HEADER * PERIOD_PILOT;
      }

      counter += PERIOD_SYNCH1 + PERIOD_SYNCH2;

      counter += calculateMCyclesForByte(this.flag);
      for (final byte b : this.data) {
        counter += calculateMCyclesForByte(b);
      }
      counter += calculateMCyclesForByte(this.checksum);

      return counter;
    }
  }

  private class DataBuffer {
    private int currentdata;
    private int mask;
    private long counter;
    private long middlePoint;
    private int repeat;
    
    public void set(final int data){
      this.currentdata = data;
      this.mask = 0x80;
      initForMask(true);
    }
    
    private void initForMask(final boolean setRepeat){
      this.counter = (this.currentdata & this.mask) == 0 ? PERIOD_RES : PERIOD_SET;
      this.middlePoint = this.counter >>> 1;
      if (setRepeat) this.repeat = REPEAT_BIT;
      inState = true;
    }
    
    public boolean process(final long machineCycles){
      boolean result = false;
      this.counter -= machineCycles;
      if (this.counter<=0){
        this.repeat --;
        if (this.repeat<=0){
          this.mask >>>= 1;
          if (this.mask == 0){
            result = true;
          }
          initForMask(true);
        }else{
          initForMask(false);
        }
      }else if (counter<=this.middlePoint){
        inState = false;
      }else{
        inState = true;
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
      if (this.current == null){
        this.state = State.STOPPED;
        return false;
      }
    }
    this.state = State.PILOT;
    this.counterMain = -1L;
    return true;
  }

  public synchronized void stopPlay() {
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
    final ByteArrayOutputStream data = new ByteArrayOutputStream(1000000);

    TapBlock block = this.current.findFirst();
    do {
      for (int i = 0; i < 30000; i++) {
        data.write(0);
      }
      block.writeAsUnsigned8BitMonoPCMData(data, 158);
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
            Int(22050).// Sample rate
            Int(22050). // Byte rate 
            Short(1). // Block align
            Short(8). // Bits per sample
            Byte("data").
            Int(data.size()).
            Byte(data.toByteArray()).End().toByteArray();
  }

  public synchronized void updateForSpentMachineCycles(final long machineCycles) {
    if (this.state != State.STOPPED){
      final TapBlock block = this.current;
              
      switch(this.state){
        case INBETWEEN : {
          this.inState = false;
          if (counterMain<0){
          System.out.println("INBETWEEN");
            this.counterMain = (REPEAT_DATA_HEADER>>1)*PERIOD_PILOT;
          }else{
            this.counterMain-=machineCycles;
            if (this.counterMain <= 0L){
              this.state = State.PILOT;
              this.counterMain = -1L;
            }
          }
        }break;
        case PILOT : {
          if (this.counterMain<0L){
          System.out.println("PILOT");
            if (block.isHeader()){
              this.counterMain = REPEAT_PILOT_HEADER;
            }else{
              this.counterMain = REPEAT_DATA_HEADER;
            }
            this.counterEx = PERIOD_PILOT;
            this.inState = true;
          }else{
            this.counterEx -= machineCycles;
            if (this.counterEx<=0){
              this.counterMain--;
              if (this.counterMain>0){
                this.counterEx = PERIOD_PILOT;
                this.inState = true;
              }else{
                this.state = State.SYNCHRO1;
                this.counterMain = -1L;
              }
            }else if (this.counterEx<(PERIOD_PILOT>>1)){
              this.inState = false;
            }else{
              this.inState = true;
            }
          }
        }break;
        case SYNCHRO1 : {
          if (this.counterMain<0L){
          System.out.println("SYNCHRO1");
            this.counterMain = PERIOD_SYNCH1;
            this.inState = true;
          }else{
            this.counterMain -= machineCycles;
            if (this.counterMain<(PERIOD_SYNCH1>>1)){
              this.inState = false;
            }
            if (this.counterMain<=0L){
              this.counterMain = -1L;
              this.state = State.SYNCHRO2;
            }
          }
        }break;
        case SYNCHRO2 : {
          if (this.counterMain < 0L) {
          System.out.println("SYNCHRO2");
            this.counterMain = PERIOD_SYNCH2;
            this.inState = true;
          }
          else {
            this.counterMain -= machineCycles;
            if (this.counterMain < (PERIOD_SYNCH2 >> 1)) {
              this.inState = false;
            }
            if (this.counterMain <= 0L) {
              this.counterMain = -1L;
              this.state = State.FLAG;
            }
          }
        }break;
        case FLAG : {
          if (this.counterMain < 0L) {
            System.out.println("FLAG");
            this.counterMain = 0L;
            this.buffer.set(block.flag);
          }
          else {
            if (this.buffer.process(machineCycles)){
              this.counterMain = -1L;
              this.state = State.DATA;
            }
          }
        }break;
        case DATA : {
          if (this.counterMain<0L){
            System.out.println("DATA");
            this.counterMain = 0L;
            this.buffer.set(block.data[(int) this.counterMain]);
          }else{
            if (this.buffer.process(machineCycles)) {
              this.counterMain++;
              if (this.counterMain<block.data.length){
                this.buffer.set(block.data[(int)this.counterMain]);
              }else{
                this.counterMain = -1;
                this.state = State.CHECKSUM;
              }
            }
          }
        }break;
        case CHECKSUM: {
          if (this.counterMain < 0L) {
            System.out.println("CHECKSUM");
            this.counterMain = 0L;
            this.buffer.set(block.checksum);
          }
          else {
            if (this.buffer.process(machineCycles)) {
              this.counterMain = -1L;
              this.state = State.INBETWEEN;
            }
          }
        }
        break;
        default: throw new Error("Unexpected state ["+this.state+']');
      }
    }
  }
}
