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

import com.igormaznitsa.vg93.FloppyDisk.Sector;
import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;

public class FloppyDrive_GOST28273_89 {

  /**
   * The IN Signal to select the DRIVE 0.
   */
  public static final int SIGNAL_IN_SELECT_DRIVE0 = 0x0100;
  /**
   * The IN Signal to select the DRIVE 1.
   */
  public static final int SIGNAL_IN_SELECT_DRIVE1 = 0x0200;
  /**
   * The IN Signal to select the DRIVE 2.
   */
  public static final int SIGNAL_IN_SELECT_DRIVE2 = 0x0400;
  /**
   * The IN Signal to select the DRIVE 3.
   */
  public static final int SIGNAL_IN_SELECT_DRIVE3 = 0x0800;
  /**
   * The IN Signal to turn on the motor.
   */
  public static final int SIGNAL_IN_MOTOR = 0x1000;
  /**
   * The IN Signal to make one step and to change the current track.
   */
  public static final int SIGNAL_IN_STEP = 0x2000;
  /**
   * The IN Signal shows that the direction of change the current track for
   * steps is outward.
   */
  public static final int SIGNAL_IN_STEP_DIRECTION_OUTWARD = 0x4000;
  /**
   * The IN Signal to select the disk side 0.
   */
  public static final int SIGNAL_IN_SELECT_SIDE_0 = 0x8000;
  /**
   * The IN Signal to write data on the disk.
   */
  public static final int SIGNAL_IN_WRITE_ENABLE = 0x10000;
  /**
   * The IN Signal to load the head.
   */
  public static final int SIGNAL_IN_LOAD_HEAD = 0x20000;
  /**
   * The IN Signal to change the write electricity current in the head.
   */
  public static final int SIGNAL_IN_CHANGE_WRITE_CURRENT = 0x40000;

  /**
   * The OUT Signal shows that the head on the track 0.
   */
  public static final int SIGNAL_OUT_TR00 = 0x0100;
  /**
   * The OUT Signal shows that the disk is write protect.
   */
  public static final int SIGNAL_OUT_WRITE_PROTECT = 0x0200;
  /**
   * The OUT Signal shows that the index hole is met.
   */
  public static final int SIGNAL_OUT_INDEX_SECTOR = 0x0400;
  /**
   * The OUT Signal shows that the drive is ready for work.
   */
  public static final int SIGNAL_OUT_READY = 0x0800;

  //------------------------------------------------------------
  private int currentTrack;
  private int currentSector;
  private final int driveIndexMask;
  private int prevSignals;
  private boolean indexHole;
  
  private FloppyDisk disk;

  private long timeOfMotorReady;
  private long timeOfHeadChangeTrackReady;
  private long lastIndexSectorTime;

  private final ReentrantLock diskLocker = new ReentrantLock();

  public final void reset() {
    final Random rnd = new Random();

    this.currentTrack = rnd.nextInt(getMaxTrackNumber());

    this.timeOfMotorReady = Long.MAX_VALUE;
    this.timeOfHeadChangeTrackReady = Long.MAX_VALUE;
    this.lastIndexSectorTime = Long.MIN_VALUE;
    this.prevSignals = 0xFFFF;
    this.disk = null;

    this.onReset();
  }

  public FloppyDrive_GOST28273_89(final int driveIndex) {
    this.driveIndexMask = 1 << (driveIndex & 3);
    this.prevSignals = 0;

    reset();
  }

  public void insertDisk(final FloppyDisk disk) {
    diskLocker.lock();
    try {
      this.disk = disk;
      this.currentSector = 0;
      this.indexHole = true;
    }
    finally {
      diskLocker.unlock();
    }
  }

  public FloppyDisk getDisk() {
    diskLocker.lock();
    try {
      return this.disk;
    }
    finally {
      diskLocker.unlock();
    }
  }

  private static boolean is(final int signals, final int mask) {
    return (signals & mask) == mask;
  }

  private boolean isFront(final int signals, final int mask) {
    final int prev = this.prevSignals & mask;
    final boolean result;
    if (prev == 0) {
      result = ((this.prevSignals ^ signals) & mask) == mask;
    }
    else {
      result = false;
    }
    return result;
  }

  private boolean isEnd(final int signals, final int mask) {
    final int prev = this.prevSignals & mask;
    final boolean result;
    if (prev == mask) {
      result = ((this.prevSignals ^ signals) & mask) == mask;
    }
    else {
      result = false;
    }
    return result;
  }

  private boolean notChanged(final int signals, final int mask) {
    return ((this.prevSignals ^ signals) & mask) == 0;
  }

  public int process(final int signals) {
    final int thesignals = onSignals(signals);

    diskLocker.lock();
    try {
      if (this.disk==null){
        this.indexHole = true;
      }else{
        if (this.indexHole){
          this.indexHole = false;
        }else{
          this.currentSector ++;
          if (this.currentSector>=this.disk.getSectorsPerTrack()){
            this.currentSector = 0;
            this.indexHole = true;
          }
        }
      }
      // invert signals because active level in GOST is the low one
      final int invertedSignals = (thesignals ^ 0xFFFF) & 0xFFFF;

      final boolean driveSelected = (invertedSignals & this.driveIndexMask) == this.driveIndexMask;

      int status;

      if (driveSelected) {
        if (notChanged(invertedSignals, SIGNAL_IN_MOTOR)) {
          processMotorSignal(is(invertedSignals, SIGNAL_IN_MOTOR));
        }

        if (isFront(invertedSignals, SIGNAL_IN_STEP)) {
          stepHeadToNextTrack(is(invertedSignals, SIGNAL_IN_STEP_DIRECTION_OUTWARD));
        }

        final boolean driveReady = isDriveReady();
        status = (driveReady ? SIGNAL_OUT_READY : 0) | (isDiskWriteProtect() ? SIGNAL_OUT_WRITE_PROTECT : 0) | (isHeadOverTrack00() ? SIGNAL_OUT_TR00 : 0) | (this.indexHole ? SIGNAL_OUT_INDEX_SECTOR : 0);
      }
      else {
        processMotorSignal(false);
        status = 0;
      }

      return status ^ 0xFFFF;
    }
    finally {
      this.prevSignals = thesignals;
      diskLocker.unlock();
    }
  }

  public Sector getCurrentSector(){
    Sector result = null;
    if (this.disk!=null){
      final FloppyDisk.Track track = this.disk.getTrack(this.currentTrack);
      if (track!=null){
        result = track.sectorAt(this.currentSector);
      }
    }
    return result;
  }
  
  protected void stepHeadToNextTrack(final boolean outward) {
    if (isHeadInPosition()) {
      if (outward) {
        if (this.currentTrack < this.getMaxTrackNumber() - 1) {
          this.currentTrack++;
          this.timeOfHeadChangeTrackReady = System.currentTimeMillis() + getTrackChangeDelay();
        }
      }
      else {
        if (this.currentTrack > 0) {
          this.currentTrack--;
          this.timeOfHeadChangeTrackReady = System.currentTimeMillis() + getTrackChangeDelay();
        }
      }
    }
  }

  public boolean isIndexSector() {
    final boolean indexSector = (System.currentTimeMillis() - this.lastIndexSectorTime) >= getIndexSectorIntervalInMilliseconds();
    if (indexSector) {
      this.lastIndexSectorTime = System.currentTimeMillis();
    }
    return indexSector;
  }

  public boolean isDriveReady() {
    return isDiskPresented() && isMotorReady() && isHeadInPosition();
  }

  public boolean isDiskPresented() {
    return this.disk != null;
  }

  public boolean isHeadInPosition() {
    return System.currentTimeMillis() >= this.timeOfHeadChangeTrackReady;
  }

  public boolean isHeadOverTrack00() {
    return this.currentTrack == 0;
  }

  public int getTrack(){
    return this.currentTrack;
  }
  
  public boolean isDiskWriteProtect() {
    final FloppyDisk thedisk = this.disk;
    return thedisk == null ? false : thedisk.isWriteProtect();
  }

  protected int onSignals(final int signals) {
    return signals;
  }

  protected void onReset() {

  }

  public boolean isMotorReady() {
    return this.timeOfMotorReady <= System.currentTimeMillis();
  }

  private void processMotorSignal(final boolean on) {
    if (on) {
      if (this.timeOfMotorReady == Long.MAX_VALUE) {
        this.timeOfMotorReady = System.currentTimeMillis() + getMotorStartDelayInMilliseconds();
      }
    }
    else {
      this.timeOfMotorReady = Long.MAX_VALUE;
    }
  }

  protected long getMotorStartDelayInMilliseconds() {
    return 800L;
  }

  protected long getIndexSectorIntervalInMilliseconds() {
    return 200L;
  }

  protected long getTrackChangeDelay() {
    return 16L;
  }

  protected int getMaxTrackNumber() {
    return 80;
  }

  protected long getSectorReadTime() {
    return 12L;
  }
  
  public int getDriveIndex() {
    switch (this.driveIndexMask) {
      case 1:
        return 0;
      case 2:
        return 1;
      case 4:
        return 2;
      case 8:
        return 3;
      default:
        return -1;
    }
  }

  public int getTrackIndex() {
    return this.currentTrack;
  }
}
