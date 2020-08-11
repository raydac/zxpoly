package com.igormaznitsa.zxpoly.components;

public class ZxAy8910 implements IoDevice {

  private final Motherboard motherboard;

  private int addressLatch;

  private int tonePeriodA;
  private int amplitudeA;
  private int tonePeriodB;
  private int amplitudeB;
  private int tonePeriodC;
  private int amplitudeC;
  private int noisePeriod;
  private int mixerReg;
  private int envelopePeriod;
  private int envelopeMode;
  private int ioPortA;
  private int ioPortB;

  public ZxAy8910(final Motherboard motherboard) {
    this.motherboard = motherboard;
  }

  @Override
  public Motherboard getMotherboard() {
    return this.motherboard;
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    return -1;
  }

  @Override
  public void writeIo(final ZxPolyModule module, final int port, final int value) {
    if (!module.isTrdosActive()) {
      if (port == 0xFFFD) {
        this.addressLatch = value;
      } else if (port == 0xBFFD) {
        switch (this.addressLatch & 0xF) {
          case 0: {
            this.tonePeriodA = (this.tonePeriodA & 0xF00) | value;
          }
          break;
          case 1: {
            this.tonePeriodA = (this.tonePeriodA & 0xFF) | (value << 8);
          }
          break;
          case 2: {
            this.tonePeriodB = (this.tonePeriodB & 0xF00) | value;
          }
          break;
          case 3: {
            this.tonePeriodB = (this.tonePeriodB & 0xFF) | (value << 8);
          }
          break;
          case 4: {
            this.tonePeriodC = (this.tonePeriodC & 0xF00) | value;
          }
          break;
          case 5: {
            this.tonePeriodC = (this.tonePeriodC & 0xFF) | (value << 8);
          }
          break;
          case 6: {
            this.noisePeriod = value & 0b11111;
          }
          break;
          case 7: {
            this.mixerReg = value;
          }
          break;
          case 8: {
            this.amplitudeA = value & 0b11111;
          }
          break;
          case 9: {
            this.amplitudeB = value & 0b11111;
          }
          break;
          case 10: {
            this.amplitudeC = value & 0b11111;
          }
          break;
          case 11: {
            this.envelopePeriod = (this.envelopePeriod & 0xFF00) | value;
          }
          break;
          case 12: {
            this.envelopePeriod = (this.envelopePeriod & 0x00FF) | (value << 8);
          }
          break;
          case 13: {
            this.envelopeMode = value & 0xF;
          }
          break;
          case 14: {
            this.ioPortA = value;
          }
          break;
          case 15: {
            this.ioPortB = value;
          }
          break;
        }
      }
    }
  }

  @Override
  public void preStep(boolean signalReset, boolean virtualIntTick, boolean wallclockInt) {

  }

  @Override
  public void postStep(long spentMachineCyclesForStep) {

  }

  @Override
  public String getName() {
    return "AY-8910";
  }

  @Override
  public void doReset() {
    this.tonePeriodA = 1;
    this.tonePeriodB = 1;
    this.tonePeriodC = 1;

    this.envelopePeriod = 0;

    this.amplitudeA = 0;
    this.amplitudeB = 0;
    this.amplitudeC = 0;

    this.ioPortA = 0;
    this.ioPortB = 0;

    this.noisePeriod = 0;
    this.envelopeMode = 0;
    this.mixerReg = 0xFF;
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_POSTSTEP;
  }
}
