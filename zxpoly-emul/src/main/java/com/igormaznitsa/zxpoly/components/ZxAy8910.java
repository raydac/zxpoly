package com.igormaznitsa.zxpoly.components;

import java.util.Arrays;

public class ZxAy8910 implements IoDevice {

  private static final int[] AMPLITUDE_VALUES;
  private static final int MACHINE_CYCLES_PER_ATICK = 16;
  private static final int ATICKS_IN_ENVELOPE_STEP = 16;

  static {
    AMPLITUDE_VALUES = Arrays.stream(new double[] {
        0.0000d, 0.0137d, 0.0205d, 0.0291d, 0.0423d, 0.0618d, 0.0847d, 0.1369d,
        0.1691d, 0.2647d, 0.3527d, 0.4499d, 0.5704d, 0.6873d, 0.8482d, 1.0000d
    }).map(x -> Math.min(255, Math.round(x * Beeper.MAX_AMPLITUDE))).mapToInt(x -> (int) x)
        .toArray();
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
  private int envelopeValue;
  private boolean envelopeFirstPart;

  private long machineCycleCounter;
  private int envelopeStepCounter;

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
            final int bits = value & 0x1F;
            this.noisePeriod = bits == 0 ? 1 : bits;
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
            initEnvelope();
          }
          break;
          case 12: {
            this.envelopePeriod = (this.envelopePeriod & 0x00FF) | (value << 8);
            initEnvelope();
          }
          break;
          case 13: {
            this.envelopeMode = value & 0xF;
            this.initEnvelope();
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
      this.counterN = this.noisePeriod == 0 ? 1 : this.noisePeriod;
      this.doRndNoise(1);
    } else if (this.counterN < 0) {
      int changes = Math.abs(this.counterN + this.noisePeriod - 1) / this.noisePeriod;
      this.doRndNoise(changes);
      this.counterN += changes * this.noisePeriod;
      if (this.counterN == 0) {
        this.counterN = 1;
      }
    }
  }

  private void initEnvelope() {
    if ((this.envelopeMode & 0b0100) == 0) {
      this.envelopeValue = 15;
    } else {
      this.envelopeValue = 0;
    }
    this.envelopeFirstPart = true;
    this.envelopeStepCounter = this.envelopePeriod >> 4;
  }

  private void processEnvelope(final int audioTicks) {
    if (this.envelopePeriod == 0) {
      this.envelopeValue = 15;
      return;
    }

    this.envelopeStepCounter -= audioTicks;
    final int steps;
    if (this.envelopeStepCounter > 0) {
      steps = 0;
    } else {
      if (this.envelopeStepCounter == 0) {
        steps = 1;
        this.envelopeStepCounter = this.envelopePeriod >> 4;
      } else {
        final int perStep = this.envelopePeriod >> 4;
        if (perStep == 0) {
          steps = Math.abs(this.envelopeStepCounter);
        } else {
          steps = Math.max(1, (this.envelopePeriod + perStep + 1) / perStep);
        }
      }
      this.envelopeStepCounter = this.envelopePeriod >> 4;
    }

    if (steps > 0) {
      switch (this.envelopeMode & 0xF) {
        case 0b0001:
        case 0b0010:
        case 0b0011:
        case 0b1001:
        case 0b0000: { // \____
          if (this.envelopeValue > 0) {
            this.envelopeValue = Math.max(this.envelopeValue - steps, 0);
            this.envelopeFirstPart = this.envelopeValue == 0;
          }
        }
        break;
        case 0b0100:
        case 0b0101:
        case 0b0110:
        case 0b0111: { // /|____
          if (this.envelopeFirstPart && this.envelopeValue < 15) {
            this.envelopeValue = Math.min(15, this.envelopeValue + steps);
            this.envelopeFirstPart = this.envelopeValue < 15;
          }
        }
        break;
        case 0b1000: { // \|\|\|\|\|
          if (this.envelopeFirstPart) {
            this.envelopeValue = Math.max(0, this.envelopeValue - steps);
            this.envelopeFirstPart = this.envelopeValue > 0;
          } else {
            this.envelopeValue = 15;
            this.envelopeFirstPart = true;
          }
        }
        break;
        case 0b1010: { // \/\/\/\/\/
          if (this.envelopeFirstPart) {
            this.envelopeValue = Math.max(0, this.envelopeValue - steps);
            this.envelopeFirstPart = this.envelopeValue > 0;
          } else {
            this.envelopeValue = Math.min(15, this.envelopeValue + steps);
            this.envelopeFirstPart = this.envelopeValue == 15;
          }
        }
        break;
        case 0b1011: { // \|--------
          if (this.envelopeFirstPart) {
            this.envelopeValue = Math.max(0, this.envelopeValue - steps);
            this.envelopeFirstPart = this.envelopeValue > 0;
          } else {
            this.envelopeValue = 15;
          }
        }
        break;
        case 0b1100: { // /|/|/|/|/|/|
          if (this.envelopeFirstPart) {
            this.envelopeValue = Math.min(15, this.envelopeValue + steps);
            this.envelopeFirstPart = this.envelopeValue < 15;
          } else {
            this.envelopeValue = 0;
            this.envelopeFirstPart = true;
          }
        }
        break;
        case 0b1101: { // /----------
          if (this.envelopeFirstPart) {
            this.envelopeValue = Math.min(15, this.envelopeValue + steps);
            this.envelopeFirstPart = this.envelopeValue < 15;
          } else {
            this.envelopeValue = 15;
          }
        }
        break;
        case 0b1110: { // /\/\/\/\/\/\
          if (this.envelopeFirstPart) {
            this.envelopeValue = Math.min(15, this.envelopeValue + steps);
            this.envelopeFirstPart = this.envelopeValue < 15;
          } else {
            this.envelopeValue = Math.max(0, this.envelopeValue - steps);
            this.envelopeFirstPart = this.envelopeValue == 0;
          }
        }
        break;
        case 0b1111: { // /|__________
          if (this.envelopeFirstPart) {
            this.envelopeValue = Math.min(15, this.envelopeValue + steps);
            this.envelopeFirstPart = this.envelopeValue < 15;
          } else {
            this.envelopeValue = 0;
          }
        }
        break;
        default:
          throw new Error("Unexpected");
      }
    }
  }

  private void processPeriods(final int audioTicks) {
    this.processNoiseGen(audioTicks);

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

    final boolean toneA = (mixer & 0b000001) == 0;
    final boolean toneB = (mixer & 0b000010) == 0;
    final boolean toneC = (mixer & 0b000100) == 0;

    if (noiseA || toneA) {
      final boolean level = (!toneA || this.hiA) && (!noiseA || hiN);
      int amplitude = level ? AMPLITUDE_VALUES[this.amplitudeA & 0xF] : AMPLITUDE_VALUES[0];
      amplitude = (this.amplitudeA & 0x10) == 0 ? amplitude : (amplitude * this.envelopeValue) / 15;
      this.motherboard.getBeeper().setChannelValue(Beeper.CHANNEL_AY_A, amplitude);
    } else {
      this.motherboard.getBeeper()
          .setChannelValue(Beeper.CHANNEL_AY_A, AMPLITUDE_VALUES[this.amplitudeA & 0xF]);
    }

    if (noiseB || toneB) {
      final boolean level = (!toneB || this.hiB) && (!noiseB || hiN);
      int amplitude = level ? AMPLITUDE_VALUES[this.amplitudeB & 0xF] : AMPLITUDE_VALUES[0];
      amplitude = (this.amplitudeB & 0x10) == 0 ? amplitude : (amplitude * this.envelopeValue) / 15;
      this.motherboard.getBeeper().setChannelValue(Beeper.CHANNEL_AY_B, amplitude);
    } else {
      this.motherboard.getBeeper()
          .setChannelValue(Beeper.CHANNEL_AY_B, AMPLITUDE_VALUES[this.amplitudeB & 0xF]);
    }

    if (noiseC || toneC) {
      final boolean level = (!toneC || this.hiC) && (!noiseC || hiN);
      int amplitude = level ? AMPLITUDE_VALUES[this.amplitudeC & 0xF] : AMPLITUDE_VALUES[0];
      amplitude = (this.amplitudeC & 0x10) == 0 ? amplitude : (amplitude * this.envelopeValue) / 15;
      this.motherboard.getBeeper().setChannelValue(Beeper.CHANNEL_AY_C, amplitude);
    } else {
      this.motherboard.getBeeper()
          .setChannelValue(Beeper.CHANNEL_AY_C, AMPLITUDE_VALUES[this.amplitudeC & 0xF]);
    }
  }

  @Override
  public void postStep(final long spentMachineCyclesForStep) {
    this.machineCycleCounter += spentMachineCyclesForStep;

    if (this.machineCycleCounter >= MACHINE_CYCLES_PER_ATICK) {
      final int audioTicks = (int) (this.machineCycleCounter / MACHINE_CYCLES_PER_ATICK);
      this.machineCycleCounter %= MACHINE_CYCLES_PER_ATICK;
      processPeriods(audioTicks);
      processEnvelope(audioTicks);
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
    this.counterN = 1;

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

    this.noisePeriod = 1;
    this.envelopeMode = 0;
    this.mixerReg = 0xFF;

    this.rng = 1;

    this.initEnvelope();
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_POSTSTEP | NOTIFICATION_PRESTEP;
  }
}