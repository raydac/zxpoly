package com.igormaznitsa.zxpoly.components;

import java.util.Arrays;

public class ZxAy8910 implements IoDevice {

  private static final int[] AMPLITUDE_VALUES;
  private static final int MACHINE_CYCLES_PER_ATICK = 8;

  static {
    final double maxAmplitude = 240.0d;
    AMPLITUDE_VALUES = Arrays.stream(new double[] {
        0.0000d, 0.0137d, 0.0205d, 0.0291d, 0.0423d, 0.0618d, 0.0847d, 0.1369d,
        0.1691d, 0.2647d, 0.3527d, 0.4499d, 0.5704d, 0.6873d, 0.8482d, 1.0000d
    }).map(x -> Math.min(255, Math.round(x * maxAmplitude))).mapToInt(x -> (int) x).toArray();
  }

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
  private int counterA;
  private int counterB;
  private int counterC;
  private int counterN;
  private boolean hiA;
  private boolean hiB;
  private boolean hiC;
  private boolean hiN;
  private int rng;

  private long machineCycleCounter;

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
          default:
            throw new Error("Unexpected");
        }
      }
    }
    return -1;
  }

  private void initCounterA() {
    this.counterA = this.tonePeriodA;
    this.hiA = true;
  }

  private void initCounterB() {
    this.counterA = this.tonePeriodA;
    this.hiA = true;
  }

  private void initCounterC() {
    this.counterA = this.tonePeriodA;
    this.hiA = true;
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
            this.tonePeriodA = (this.tonePeriodA & 0xFF) | ((value & 0xF) << 8);
          }
          break;
          case 2: {
            this.tonePeriodB = (this.tonePeriodB & 0xF00) | value;
          }
          break;
          case 3: {
            this.tonePeriodB = (this.tonePeriodB & 0xFF) | ((value & 0xF) << 8);
          }
          break;
          case 4: {
            this.tonePeriodC = (this.tonePeriodC & 0xF00) | value;
          }
          break;
          case 5: {
            this.tonePeriodC = (this.tonePeriodC & 0xFF) | ((value & 0xF) << 8);
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
            if (this.amplitudeA != 0) {
              this.initCounterA();
            }
          }
          break;
          case 9: {
            this.amplitudeB = value & 0x1F;
            if (this.amplitudeB != 0) {
              this.initCounterB();
            }
          }
          break;
          case 10: {
            this.amplitudeC = value & 0x1F;
            if (this.amplitudeC != 0) {
              this.initCounterC();
            }
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
          default:
            throw new Error("Unexpected");
        }
      }
    }
  }

  @Override
  public void preStep(boolean signalReset, boolean virtualIntTick, boolean wallClockInt) {
    if (signalReset) {
      this.doReset();
    }
  }

  private boolean isActiveA() {
    return this.tonePeriodA != 0;
  }

  private boolean isActiveB() {
    return this.tonePeriodB != 0;
  }

  private boolean isActiveC() {
    return this.tonePeriodC != 0;
  }

  private void doRndNoise(int counts) {
    do {
      if (((rng + 1) & 0x02) != 0) {
        this.hiN = !this.hiN;
      }
      if ((this.rng & 0x01) != 0) {
        this.rng ^= 0x24000;
      }
      this.rng >>>= 1;
    } while (--counts > 0);
  }

  private void processNoiseGen(final int audioTicks) {
    this.counterN -= audioTicks;
    if (this.counterN == 0) {
      this.counterN = this.noisePeriod;
      this.doRndNoise(1);
    } else if (this.counterN < 0) {
      int changes = Math.abs(this.counterN + this.noisePeriod - 1) / this.noisePeriod;
      this.doRndNoise(changes);
      this.counterN += changes * this.noisePeriod;
    }
  }

  private void processPeriods(final int audioTicks) {
    if (this.noisePeriod == 0) {
      this.hiN = true;
    } else {
      this.processNoiseGen(audioTicks);
    }

    if (this.isActiveA()) {
      this.counterA -= audioTicks;
      if (this.counterA == 0) {
        this.counterA = this.tonePeriodA;
        this.hiA = !this.hiA;
      } else if (this.counterA < 0) {
        int changes = Math.abs(this.counterA + this.tonePeriodA - 1) / this.tonePeriodA;
        this.hiA = ((changes & 1) == 0) == this.hiA;
        this.counterA += changes * this.tonePeriodA;
      }
    } else {
      this.counterA = 0;
      this.hiA = false;
    }

    if (this.isActiveB()) {
      this.counterB -= audioTicks;
      if (this.counterB == 0) {
        this.counterB = this.tonePeriodB;
        this.hiB = !this.hiB;
      } else if (this.counterB < 0) {
        int changes = Math.abs(this.counterB + this.tonePeriodB - 1) / this.tonePeriodB;
        this.hiB = ((changes & 1) == 0) == this.hiB;
        this.counterB += changes * this.tonePeriodB;
      }
    } else {
      this.counterB = 0;
      this.hiB = false;
    }

    if (this.isActiveC()) {
      this.counterC -= audioTicks;
      if (this.counterC == 0) {
        this.counterC = this.tonePeriodC;
        this.hiC = !this.hiC;
      } else if (this.counterC < 0) {
        int changes = Math.abs(this.counterC + this.tonePeriodC - 1) / this.tonePeriodC;
        this.hiC = ((changes & 1) == 0) == this.hiC;
        this.counterC += changes * this.tonePeriodC;
      }
    } else {
      this.counterC = 0;
      this.hiC = false;
    }
  }

  private void mixOutputSignals() {
    final int mixer = this.mixerReg;

    final boolean noiseA = (mixer & 0b001000) == 0;
    final boolean noiseB = (mixer & 0b010000) == 0;
    final boolean noiseC = (mixer & 0b100000) == 0;

    if ((mixer & 0b000001) == 0 || noiseA) {
      final boolean level = this.hiA ^ (noiseA && this.hiN);
      this.motherboard.getBeeper().setChannelValue(
          Beeper.CHANNEL_AY_A,
          AMPLITUDE_VALUES[level ? this.amplitudeA & 0xF : 0]
      );
    }

    if ((mixer & 0b000010) == 0 || noiseB) {
      final boolean level = this.hiB ^ (noiseB && this.hiN);
      this.motherboard.getBeeper().setChannelValue(
          Beeper.CHANNEL_AY_B,
          AMPLITUDE_VALUES[level ? this.amplitudeB & 0xF : 0]
      );

    }

    if ((mixer & 0b000100) == 0 || noiseC) {
      final boolean level = this.hiC ^ (noiseC && this.hiN);
      this.motherboard.getBeeper().setChannelValue(
          Beeper.CHANNEL_AY_C,
          AMPLITUDE_VALUES[level ? this.amplitudeC & 0xF : 0]
      );
    }
  }

  @Override
  public void postStep(final long spentMachineCyclesForStep) {
    this.machineCycleCounter += spentMachineCyclesForStep;

    if (this.machineCycleCounter >= MACHINE_CYCLES_PER_ATICK) {
      final int audioTicks = (int) (this.machineCycleCounter / MACHINE_CYCLES_PER_ATICK);
      this.machineCycleCounter %= MACHINE_CYCLES_PER_ATICK;
      processPeriods(audioTicks);
    }

    this.mixOutputSignals();
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
    this.counterN = 0;

    this.hiA = false;
    this.hiB = false;
    this.hiC = false;
    this.hiN = false;

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

    this.rng = 1;
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_POSTSTEP | NOTIFICATION_PRESTEP;
  }
}
