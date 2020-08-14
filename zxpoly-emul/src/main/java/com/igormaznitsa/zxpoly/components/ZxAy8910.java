package com.igormaznitsa.zxpoly.components;

import java.util.Arrays;

public class ZxAy8910 implements IoDevice {

  private static final int[] AMPLITUDE_VALUES;
  private static final int MACHINE_CYCLES_PER_ATICK = 16;

  private static final int REG_TONE_PERIOD_A_FINE = 0x00;
  private static final int REG_TONE_PERIOD_A_ROUGH = 0x01;
  private static final int REG_TONE_PERIOD_B_FINE = 0x02;
  private static final int REG_TONE_PERIOD_B_ROUGH = 0x03;
  private static final int REG_TONE_PERIOD_C_FINE = 0x04;
  private static final int REG_TONE_PERIOD_C_ROUGH = 0x05;
  private static final int REG_NOISE_PERIOD = 0x06;
  private static final int REG_MIXER_CTRL = 0x07;
  private static final int REG_AMPL_A = 0x08;
  private static final int REG_AMPL_B = 0x09;
  private static final int REG_AMPL_C = 0x0A;
  private static final int REG_ENV_PERIOD_FINE = 0x0B;
  private static final int REG_ENV_PERIOD_ROUGH = 0x0C;
  private static final int REG_ENV_SHAPE = 0x0D;
  private static final int REG_IO_A = 0x0E;
  private static final int REG_IO_B = 0x0F;

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
    if (!module.isTrdosActive() && (port & 2) == 0) {
      if ((port & 0xC0FF) == 0xC0FD) {
        switch (this.addressLatch) {
          case REG_TONE_PERIOD_A_FINE: {
            return this.tonePeriodA & 0xFF;
          }
          case REG_TONE_PERIOD_A_ROUGH: {
            return (this.tonePeriodA >> 8) & 0xF;
          }
          case REG_TONE_PERIOD_B_FINE: {
            return this.tonePeriodB & 0xFF;
          }
          case REG_TONE_PERIOD_B_ROUGH: {
            return (this.tonePeriodB >> 8) & 0xF;
          }
          case REG_TONE_PERIOD_C_FINE: {
            return this.tonePeriodC & 0xFF;
          }
          case REG_TONE_PERIOD_C_ROUGH: {
            return (this.tonePeriodC >> 8) & 0xF;
          }
          case REG_NOISE_PERIOD: {
            return this.noisePeriod;
          }
          case REG_MIXER_CTRL: {
            return this.mixerReg;
          }
          case REG_AMPL_A: {
            return this.amplitudeA;
          }
          case REG_AMPL_B: {
            return this.amplitudeB;
          }
          case REG_AMPL_C: {
            return this.amplitudeC;
          }
          case REG_ENV_PERIOD_FINE: {
            return this.envelopePeriod & 0xFF;
          }
          case REG_ENV_PERIOD_ROUGH: {
            return (this.envelopePeriod >> 8) & 0xFF;
          }
          case REG_ENV_SHAPE: {
            return this.envelopeMode;
          }
          case REG_IO_A: {
            return this.ioPortA;
          }
          case REG_IO_B: {
            return this.ioPortB;
          }
          default: {
            // IGNORE
          }
          break;
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
    this.counterB = this.tonePeriodB;
    this.hiB = true;
  }

  private void initCounterC() {
    this.counterC = this.tonePeriodC;
    this.hiC = true;
  }

  @Override
  public void writeIo(final ZxPolyModule module, final int port, final int value) {
    if (!module.isTrdosActive() & (port & 2) == 0) {
      if ((port & 0xC0FF) == 0xC0FD) {
        this.addressLatch = value;
      } else if ((port & 0xC000) == 0x8000) {
        switch (this.addressLatch) {
          case REG_TONE_PERIOD_A_FINE: {
            this.tonePeriodA = (this.tonePeriodA & 0xF00) | value;
          }
          break;
          case REG_TONE_PERIOD_A_ROUGH: {
            this.tonePeriodA = (this.tonePeriodA & 0xFF) | ((value & 0xF) << 8);
          }
          break;
          case REG_TONE_PERIOD_B_FINE: {
            this.tonePeriodB = (this.tonePeriodB & 0xF00) | value;
          }
          break;
          case REG_TONE_PERIOD_B_ROUGH: {
            this.tonePeriodB = (this.tonePeriodB & 0xFF) | ((value & 0xF) << 8);
          }
          break;
          case REG_TONE_PERIOD_C_FINE: {
            this.tonePeriodC = (this.tonePeriodC & 0xF00) | value;
          }
          break;
          case REG_TONE_PERIOD_C_ROUGH: {
            this.tonePeriodC = (this.tonePeriodC & 0xFF) | ((value & 0xF) << 8);
          }
          break;
          case REG_NOISE_PERIOD: {
            this.noisePeriod = value & 0x1F;
          }
          break;
          case REG_MIXER_CTRL: {
            this.mixerReg = value;
          }
          break;
          case REG_AMPL_A: {
            this.amplitudeA = value & 0x1F;
            if (this.amplitudeA != 0) {
              this.initCounterA();
            }
          }
          break;
          case REG_AMPL_B: {
            this.amplitudeB = value & 0x1F;
            if (this.amplitudeB != 0) {
              this.initCounterB();
            }
          }
          break;
          case REG_AMPL_C: {
            this.amplitudeC = value & 0x1F;
            if (this.amplitudeC != 0) {
              this.initCounterC();
            }
          }
          break;
          case REG_ENV_PERIOD_FINE: {
            this.envelopePeriod = (this.envelopePeriod & 0xFF00) | value;
            initEnvelope();
          }
          break;
          case REG_ENV_PERIOD_ROUGH: {
            this.envelopePeriod = (this.envelopePeriod & 0x00FF) | (value << 8);
            initEnvelope();
          }
          break;
          case REG_ENV_SHAPE: {
            this.envelopeMode = value & 0xF;
            this.initEnvelope();
          }
          break;
          case REG_IO_A: {
            this.ioPortA = value;
          }
          break;
          case REG_IO_B: {
            this.ioPortB = value;
          }
          break;
          default: {
            // IGNORE
          }
          ;
          break;
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
    final int realNoisePeriod = this.noisePeriod == 0 ? 1 : this.noisePeriod;

    this.counterN -= audioTicks;
    if (this.counterN == 0) {
      this.counterN = realNoisePeriod;
      this.doRndNoise(1);
    } else if (this.counterN < 0) {
      int changes = Math.abs(this.counterN + realNoisePeriod - 1) / realNoisePeriod;
      this.doRndNoise(changes);
      this.counterN += changes * realNoisePeriod;
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
        int periods = (Math.abs(this.counterA) + this.tonePeriodA - 1) / this.tonePeriodA;
        this.hiA = ((periods & 1) == 0) == this.hiA;
        this.counterA += periods * this.tonePeriodA;
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
        int periods = (Math.abs(this.counterB) + this.tonePeriodB - 1) / this.tonePeriodB;
        this.hiB = ((periods & 1) == 0) == this.hiB;
        this.counterB += periods * this.tonePeriodB;
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
        int periods = (Math.abs(this.counterC) + this.tonePeriodC - 1) / this.tonePeriodC;
        this.hiC = ((periods & 1) == 0) == this.hiC;
        this.counterC += periods * this.tonePeriodC;
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

    this.noisePeriod = 0;
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
