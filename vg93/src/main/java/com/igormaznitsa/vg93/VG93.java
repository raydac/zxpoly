package com.igormaznitsa.vg93;

import com.igormaznitsa.vg93.FloppyDisk.Sector;
import java.util.Arrays;

public class VG93 {

  protected NGMDInterface ngmdInterface;

  public static final int COMMAND_TYPE_NONE = 0;
  public static final int COMMAND_TYPE_1 = 1;
  public static final int COMMAND_TYPE_2 = 2;
  public static final int COMMAND_TYPE_3 = 3;
  public static final int COMMAND_TYPE_4 = 4;

  private static final int REG_COMMAND_STATE = 0;
  private static final int REG_TRACK = 1;
  private static final int REG_SECTOR = 2;
  private static final int REG_DATA = 2;

  private final int[] wRegs = new int[4];
  private final int[] rRegs = new int[4];

  private Sector currentSector;
  private boolean firstOperationStep;
  private int operationCommonValue;
  private long timeoutForOperation;

  private static final int STATUS_FDD_READY = 0b10000000;
  private static final int STATUS_WRITE_PROTECT = 0b01000000;
  private static final int STATUS_HEAD_LOAD_OR_WRITE_ERROR = 0b00100000;
  private static final int STATUS_SEEK_ERROR = 0b00010000;
  private static final int STATUS_CRC_ERROR = 0b00001000;
  private static final int STATUS_TR00_OR_DATA_LOST = 0b00000100;
  private static final int STATUS_MARKER_OR_DATA_REQ = 0b00000010;
  private static final int STATUS_BUSY = 0b00000001;

  public VG93(final long millisecondsPerStep) {
    reset();
  }

  public static int calculateCrc16(final byte[] array) {
    int crc = 0xCDB4;
    for (int i = 0; i < array.length; i++) {
      crc ^= ((array[i] & 0xFF) << 8);
      for (int b = 0; b < 8; b++) {
        if (((crc << 1) & 0x10000) != 0) {
          crc ^= 0x1021;
        }
      }
    }
    return crc & 0xFFFF;
  }

  public void step() {
    if ((this.rRegs[REG_COMMAND_STATE] & 1) != 0) {
      // presented non completed command
      try {
        final int command = this.wRegs[REG_COMMAND_STATE];
        switch ((command >> 4) & 0x0F) {
          case 0b0000: {
            cmdRestore(command);
          }
          break;
          case 0b0001: {
            cmdSeek(command);
          }
          break;
          case 0b0010:
          case 0b0011: {
            cmdStep(command);
          }
          break;
          case 0b0100:
          case 0b0101: {
            cmdStepIn(command);
          }
          break;
          case 0b0110:
          case 0b0111: {
            cmdStepOut(command);
          }
          break;
          case 0b1000:
          case 0b1001: {
            cmdReadSector(command);
          }
          break;
          case 0b1010:
          case 0b1011: {
            cmdWriteSector(command);
          }
          break;
          case 0b1100: {
            cmdReadAddress(command);
          }
          break;
          case 0b1101: {
            cmdForceInterrupt(command);
          }
          break;
          case 0b1110: {
            cmdReadTrack(command);
          }
          break;
          case 0b1111: {
            cmdWriteTrack(command);
          }
          break;
        }
      }
      finally {
        firstOperationStep = false;
      }
    }
  }

  private void prepareStatusForSeekAndAux() {
    final Sector sector = this.ngmdInterface.getCurrentSector();
    int result = 0;
    result |= this.ngmdInterface.isFDDReady() ? STATUS_FDD_READY : 0;
    result |= this.ngmdInterface.isWriteProtect() ? STATUS_WRITE_PROTECT : 0;
    result |= this.ngmdInterface.isHeadLoaded() ? STATUS_HEAD_LOAD_OR_WRITE_ERROR : 0;
    result |= this.ngmdInterface.isTR00() ? STATUS_TR00_OR_DATA_LOST : 0;
    result |= this.ngmdInterface.isIndex() ? STATUS_MARKER_OR_DATA_REQ : 0;
    result |= sector == null ? STATUS_CRC_ERROR : sector.isCrcOk() ? 0 : STATUS_CRC_ERROR;
    this.rRegs[REG_COMMAND_STATE] = result;
  }

  private void prepareStatusForSectorWrite() {
    int result = 0;
    result |= this.ngmdInterface.isFDDReady() ? STATUS_FDD_READY : 0;
    result |= this.ngmdInterface.isWriteProtect() ? STATUS_WRITE_PROTECT : 0;
    result |= this.ngmdInterface.isWriteProtect() ? STATUS_HEAD_LOAD_OR_WRITE_ERROR : 0;
    result |= this.currentSector == null ? STATUS_SEEK_ERROR : 0;
    result |= this.currentSector == null ? STATUS_CRC_ERROR : this.currentSector.isCrcOk() ? 0 : STATUS_CRC_ERROR;
    result |= !this.ngmdInterface.isFDDReady() || this.currentSector == null ? STATUS_TR00_OR_DATA_LOST : 0;

    this.rRegs[REG_COMMAND_STATE] = result;
  }

  private void prepareStatusForSectorReading() {
    final int side = (this.wRegs[REG_COMMAND_STATE] & 0b1000) == 0 ? 0 : 1;
    final boolean checkSide = (this.wRegs[REG_COMMAND_STATE] & 0b0001) != 0;
    
    int result = 0;
    result |= this.ngmdInterface.isFDDReady() ? STATUS_FDD_READY : 0;

    result |= (this.wRegs[REG_COMMAND_STATE] & 1) == 0 ? 0 : STATUS_HEAD_LOAD_OR_WRITE_ERROR;
    result |= checkSide ? this.currentSector == null || this.currentSector.getSide()!= side ? STATUS_SEEK_ERROR : 0 : 0;
    result |= this.currentSector == null ? STATUS_CRC_ERROR : this.currentSector.isCrcOk() ? 0 : STATUS_CRC_ERROR;
    result |= !this.ngmdInterface.isFDDReady() || this.currentSector == null ? STATUS_TR00_OR_DATA_LOST : 0;

    this.rRegs[REG_COMMAND_STATE] = result;
  }

  private void prepareStatusForAddressReading() {
    int result = 0;
    result |= this.ngmdInterface.isFDDReady() ? STATUS_FDD_READY : 0;

    result |= this.currentSector == null ? STATUS_SEEK_ERROR : 0;
    result |= this.currentSector == null ? STATUS_CRC_ERROR : this.currentSector.isCrcOk() ? 0 : STATUS_CRC_ERROR;
    result |= !this.ngmdInterface.isFDDReady() || this.currentSector == null ? STATUS_TR00_OR_DATA_LOST : 0;

    this.rRegs[REG_COMMAND_STATE] = result;
  }

  private void prepareStatusForTrackWriting() {
    int result = 0;
    result |= this.ngmdInterface.isFDDReady() ? STATUS_FDD_READY : 0;
    result |= this.ngmdInterface.isWriteProtect() ? STATUS_WRITE_PROTECT : 0;
    result |= this.ngmdInterface.isWriteProtect() ? STATUS_HEAD_LOAD_OR_WRITE_ERROR : 0;
    result |= !this.ngmdInterface.isFDDReady() || this.currentSector == null ? STATUS_TR00_OR_DATA_LOST : 0;

    this.rRegs[REG_COMMAND_STATE] = result;
  }

  private void prepareStatusForTrackReading() {
    int result = 0;
    result |= this.ngmdInterface.isFDDReady() ? STATUS_FDD_READY : 0;
    result |= !this.ngmdInterface.isFDDReady() || this.currentSector == null ? STATUS_TR00_OR_DATA_LOST : 0;

    this.rRegs[REG_COMMAND_STATE] = result;
  }

  private void cmdReadSector(final int data) {
    prepareStatusForSectorReading();
  }

  private void cmdWriteSector(final int data) {
    prepareStatusForSectorWrite();

  }

  private void cmdReadAddress(final int data) {
    prepareStatusForAddressReading();

  }

  private void cmdForceInterrupt(final int data) {
    prepareStatusForSeekAndAux();
    this.operationCommonValue = 0;
    this.timeoutForOperation = Long.MIN_VALUE;
  }

  private void cmdReadTrack(final int data) {
    prepareStatusForTrackReading();

  }

  private void cmdWriteTrack(final int data) {
    prepareStatusForTrackWriting();

  }

  private void cmdStepIn(final int data) {
    prepareStatusForSeekAndAux();

  }

  private void cmdStepOut(final int data) {
    prepareStatusForSeekAndAux();

  }

  private void cmdStep(final int data) {
    prepareStatusForSeekAndAux();

  }

  private void cmdSeek(final int data) {
    prepareStatusForSeekAndAux();

  }

  private void cmdRestore(final int data) {
    if (this.firstOperationStep){
      this.operationCommonValue = 255;
    }
    
    final long delay;
    switch (data & 3) {
      case 0:
        delay = 6L;
        break;
      case 1:
        delay = 12L;
        break;
      case 2:
        delay = 20L;
        break;
      default:
        delay = 30L;
        break;
    }

    this.ngmdInterface.doLoadHead((data & 0b1000) != 0);

    final Sector sector = this.ngmdInterface.getCurrentSector();

    prepareStatusForSeekAndAux();
    if (!this.ngmdInterface.isTR00()){
      if (System.currentTimeMillis() >= this.timeoutForOperation) {
        this.timeoutForOperation = System.currentTimeMillis()+delay;
        this.operationCommonValue--;
        if (this.operationCommonValue>0){
          this.ngmdInterface.doStep(false);
          this.rRegs[REG_COMMAND_STATE] |= STATUS_BUSY;
        }else{
          this.rRegs[REG_TRACK] = 0;
          this.rRegs[REG_COMMAND_STATE] |= sector != null && sector.getTrack() == this.rRegs[REG_TRACK] ? 0 : STATUS_SEEK_ERROR;
        }
      }
    }else{
      this.rRegs[REG_TRACK] = 0;
      this.rRegs[REG_COMMAND_STATE] |= sector!=null && sector.getTrack() == this.rRegs[REG_TRACK] ? 0 : STATUS_SEEK_ERROR;
    }
    
  }

  private void setStatusFlag(final int flag){
    this.rRegs[REG_COMMAND_STATE] |= flag;
  }
  
  private void resetStatusFlag(final int flag){
    final int value = this.rRegs[REG_COMMAND_STATE];
    final int mask = (flag ^ 0xFF) & 0xFF;
    this.rRegs[REG_COMMAND_STATE] = this.rRegs[REG_COMMAND_STATE] & mask;
  }
  
  public void write(final int register, int data) {
    data &= 0xFF;
    switch (register & 3) {
      case REG_COMMAND_STATE: { // command
        if ((this.rRegs[REG_COMMAND_STATE] & 1) == 0) {
          this.firstOperationStep = true;
          this.timeoutForOperation = Long.MIN_VALUE;
          this.wRegs[REG_COMMAND_STATE] = data;
        }
        else {
          if ((data >> 4) == 0b1101) {
            // force interrupt
            this.wRegs[REG_COMMAND_STATE] = data;
          }
        }
      }
      break;
      default: {
        this.wRegs[register] = data & 0xFF;
      }
      break;
    }
  }

  public int read(final int register) {
    return rRegs[register & 3] & 0xFF;
  }

  public void reset() {
    this.firstOperationStep = false;
    this.operationCommonValue = 0;
    this.timeoutForOperation = Long.MIN_VALUE;
    Arrays.fill(wRegs, 0);
    Arrays.fill(rRegs, 0);
  }

}
