package com.igormaznitsa.zxpoly.components;

import java.util.Arrays;

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

  private static final int[] AMPLITUDE_VALUES;

  static {
    final double maxAmplitude = 240.0d;
    AMPLITUDE_VALUES = Arrays.stream(new double[] {
        0.0000d, 0.0137d, 0.0205d, 0.0291d, 0.0423d, 0.0618d, 0.0847d, 0.1369d,
        0.1691d, 0.2647d, 0.3527d, 0.4499d, 0.5704d, 0.6873d, 0.8482d, 1.0000d
    }).map(x -> Math.min(255, Math.round(x * maxAmplitude))).mapToInt(x -> (int) x).toArray();
  }

  private int counterA;
  private int counterB;
  private int counterC;
  private boolean hiA;
  private boolean hiB;
  private boolean hiC;

  public ZxAy8910(final Motherboard motherboard) {
    this.motherboard = motherboard;
  }

  @Override
  public Motherboard getMotherboard() {
    return this.motherboard;
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    if (!module.isTrdosActive()) {
      if (port == 0xBFFD) {
        switch (this.addressLatch & 0xF) {
          case 0: {
            return this.tonePeriodA & 0xFF;
          }
          case 1: {
            return (this.tonePeriodA >> 8) & 0xF;
          }
          case 2: {
            return this.tonePeriodB & 0xFF;
          }
          case 3: {
            return (this.tonePeriodB >> 8) & 0xF;
          }
          case 4: {
            return this.tonePeriodC & 0xFF;
          }
          case 5: {
            return (this.tonePeriodC >> 8) & 0xF;
          }
          case 6: {
            return this.noisePeriod;
          }
          case 7: {
            return this.mixerReg;
          }
          case 8: {
            return this.amplitudeA;
          }
          case 9: {
            return this.amplitudeB;
          }
          case 10: {
            return this.amplitudeC;
          }
          case 11: {
            return this.envelopePeriod & 0xFF;
          }
          case 12: {
            return (this.envelopePeriod >> 8) & 0xFF;
          }
          case 13: {
            return this.envelopeMode;
          }
          case 14: {
            return this.ioPortA;
          }
          case 15: {
            return this.ioPortB;
          }
        }
      }
    }
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
            this.amplitudeA = value & 0x1F;
          }
          break;
          case 9: {
            this.amplitudeB = value & 0x1F;
          }
          break;
          case 10: {
            this.amplitudeC = value & 0x1F;
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

  private void processPeriods(final int diffTicks) {
    this.counterA -= diffTicks;
    this.counterB -= diffTicks;
    this.counterC -= diffTicks;

    if (this.tonePeriodA == 0) {
      this.counterA = 0;
      this.hiA = false;
    } else {
      if (this.counterA == 0) {
        this.counterA = this.tonePeriodA;
        this.hiA = !this.hiA;
      } else if (this.counterA < 0) {
        do {
          this.hiA = !this.hiA;
          this.counterA += this.tonePeriodA;
        } while (this.counterA <= 0);
      }
    }

    if (this.tonePeriodB == 0) {
      this.counterB = 0;
      this.hiB = false;
    } else {
      if (this.counterB == 0) {
        this.counterB = this.tonePeriodB;
        this.hiB = !this.hiB;
      } else if (this.counterB < 0) {
        do {
          this.hiB = !this.hiB;
          this.counterB += this.tonePeriodB;
        } while (this.counterB <= 0);
      }
    }

    if (this.tonePeriodC == 0) {
      this.counterC = 0;
      this.hiC = false;
    } else {
      if (this.counterC == 0) {
        this.counterC = this.tonePeriodC;
        this.hiC = !this.hiC;
      } else if (this.counterC < 0) {
        do {
          this.hiB = !this.hiC;
          this.counterC += this.tonePeriodC;
        } while (this.counterC <= 0);
      }
    }
  }

  private void doOutput() {
    this.motherboard.getBeeper().setChannelValue(Beeper.CHANNEL_AY_A,
        this.hiA ? AMPLITUDE_VALUES[this.amplitudeA & 0xF] : AMPLITUDE_VALUES[0]);
    this.motherboard.getBeeper().setChannelValue(Beeper.CHANNEL_AY_B,
        this.hiB ? AMPLITUDE_VALUES[this.amplitudeB & 0xF] : AMPLITUDE_VALUES[0]);
    this.motherboard.getBeeper().setChannelValue(Beeper.CHANNEL_AY_C,
        this.hiC ? AMPLITUDE_VALUES[this.amplitudeC & 0xF] : AMPLITUDE_VALUES[0]);
  }

  @Override
  public void postStep(final long spentMachineCyclesForStep) {
    final int periodTicks = Math.max(1, (int) (spentMachineCyclesForStep >>> 3));
    processPeriods(periodTicks);
    this.doOutput();
  }

  @Override
  public String getName() {
    return "AY-8910";
  }

  @Override
  public void doReset() {
    this.counterA = 0;
    this.counterB = 0;
    this.counterC = 0;

    this.hiA = false;
    this.hiB = false;
    this.hiC = false;

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
