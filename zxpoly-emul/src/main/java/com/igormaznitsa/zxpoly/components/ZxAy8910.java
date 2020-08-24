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

  private static final int SIGNAL_N = 0b1000;
  private static final int SIGNAL_C = 0b0100;
  private static final int SIGNAL_B = 0b0010;
  private static final int SIGNAL_A = 0b0001;

  private static final int ENV_MIN = 0;
  private static final int ENV_MAX = 15;

  private static final int ENV_FLAG_ATTACK = 0b0100;

  private int lastVa;
  private int lastVb;
  private int lastVc;

  static {
    AMPLITUDE_VALUES = Arrays.stream(new double[] {
        0.0000d, 0.0137d, 0.0205d, 0.0291d, 0.0423d, 0.0618d, 0.0847d, 0.1369d,
        0.1691d, 0.2647d, 0.3527d, 0.4499d, 0.5704d, 0.6873d, 0.8482d, 1.0000d
    }).map(x -> Math.min(Beeper.MAX_AMPLITUDE, Math.round(x * Beeper.MAX_AMPLITUDE)))
        .mapToInt(x -> (int) x)
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
  private int mixerControl;
  private int envelopePeriod;
  private int envelopeMode;
  private int ioPortA;
  private int ioPortB;
  private int counterA;
  private int counterB;
  private int counterC;
  private int counterN;
  private int signalNcba;
  private int rng;
  private int curEnv;
  private boolean envFirstHalf;

  private long machineCycleCounter;
  private int counterE;

  public ZxAy8910(final Motherboard motherboard) {
    this.motherboard = motherboard;
  }

  @Override
  public Motherboard getMotherboard() {
    return this.motherboard;
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    if ((port & 2) == 0) {
      if ((port & 0xC000) == 0x8000) {
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
            return this.mixerControl;
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

  @Override
  public void writeIo(final ZxPolyModule module, final int port, final int value) {
    if ((port & 2) == 0) {
      if ((port & 0xC0FF) == 0xC0FD) {
        this.addressLatch = value;
      } else if ((port & 0xC000) == 0x8000) {
        switch (this.addressLatch & 0xF) {
          case REG_TONE_PERIOD_A_FINE: {
            this.tonePeriodA = (this.tonePeriodA & 0xF00) | value;
          }
          break;
          case REG_TONE_PERIOD_A_ROUGH: {
            this.tonePeriodA = (this.tonePeriodA & 0xFF) | ((value & 0xF) << 8);
          }
          break;
          case REG_TONE_PERIOD_B_FINE: {
            this.tonePeriodB = (this.tonePeriodB & 0x0F00) | value;
          }
          break;
          case REG_TONE_PERIOD_B_ROUGH: {
            this.tonePeriodB = (this.tonePeriodB & 0xFF) | ((value & 0xF) << 8);
          }
          break;
          case REG_TONE_PERIOD_C_FINE: {
            this.tonePeriodC = (this.tonePeriodC & 0x0F00) | value;
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
            this.mixerControl = value;
          }
          break;
          case REG_AMPL_A: {
            this.amplitudeA = value & 0x1F;
          }
          break;
          case REG_AMPL_B: {
            this.amplitudeB = value & 0x1F;
          }
          break;
          case REG_AMPL_C: {
            this.amplitudeC = value & 0x1F;
          }
          break;
          case REG_ENV_PERIOD_FINE: {
            this.envelopePeriod = (this.envelopePeriod & 0xFF00) | value;
          }
          break;
          case REG_ENV_PERIOD_ROUGH: {
            this.envelopePeriod = (this.envelopePeriod & 0x00FF) | (value << 8);
          }
          break;
          case REG_ENV_SHAPE: {
            this.envelopeMode = value & 0xF;
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

  private void doRndNoise() {
    if (((rng + 1) & 0x02) != 0) {
      this.signalNcba ^= SIGNAL_N;
    }
    if ((this.rng & 0x01) != 0) {
      this.rng ^= 0x24000;
    }
    this.rng >>>= 1;
  }

  private void processNoiseGen(final int audioTicks) {
    this.counterN += audioTicks;
    if (this.counterN >= this.noisePeriod) {
      this.counterN = 0;
      this.doRndNoise();
    }
  }

  private void processEnvelope(final int audioTicks) {
    this.counterE += audioTicks;
    if (this.counterE >= (this.envelopePeriod == 0 ? 2 : this.envelopePeriod << 1)) {
      this.counterE = 0;
      switch (this.envelopeMode) {
        case 0b0000:
        case 0b0001:
        case 0b0010:
        case 0b0011:
        case 0b1001: { // \____
          if (this.envFirstHalf) {
            this.curEnv = Math.max(this.curEnv - 1, ENV_MIN);
            this.envFirstHalf = this.curEnv > ENV_MIN;
          } else {
            this.curEnv = ENV_MIN;
          }
        }
        break;
        case 0b1111:
        case 0b0100:
        case 0b0101:
        case 0b0110:
        case 0b0111: { // /|____
          if (this.envFirstHalf) {
            this.curEnv = Math.min(ENV_MAX, this.curEnv + 1);
            this.envFirstHalf = this.curEnv < ENV_MAX;
          } else {
            this.curEnv = ENV_MIN;
          }
        }
        break;
        case 0b1000: { // \|\|\|\|\|
          if (this.envFirstHalf) {
            this.curEnv = Math.max(ENV_MIN, this.curEnv - 1);
            this.envFirstHalf = this.curEnv > ENV_MIN;
          } else {
            this.curEnv = ENV_MAX;
            this.envFirstHalf = true;
          }
        }
        break;
        case 0b1010: { // \/\/\/\/\/
          if (this.envFirstHalf) {
            this.curEnv = Math.max(ENV_MIN, this.curEnv - 1);
            this.envFirstHalf = this.curEnv > ENV_MIN;
          } else {
            this.curEnv = Math.min(ENV_MAX, this.curEnv + 1);
            this.envFirstHalf = this.curEnv == ENV_MAX;
          }
        }
        break;
        case 0b1011: { // \|--------
          if (this.envFirstHalf) {
            this.curEnv = Math.max(ENV_MIN, this.curEnv - 1);
            this.envFirstHalf = this.curEnv > ENV_MIN;
          } else {
            this.curEnv = ENV_MAX;
          }
        }
        break;
        case 0b1100: { // /|/|/|/|/|/|
          if (this.envFirstHalf) {
            this.curEnv = Math.min(ENV_MAX, this.curEnv + 1);
            this.envFirstHalf = this.curEnv < ENV_MAX;
          } else {
            this.curEnv = ENV_MIN;
            this.envFirstHalf = true;
          }
        }
        break;
        case 0b1101: { // /----------
          if (this.envFirstHalf) {
            this.curEnv = Math.min(ENV_MAX, this.curEnv + 1);
            this.envFirstHalf = this.curEnv < ENV_MAX;
          } else {
            this.curEnv = ENV_MAX;
          }
        }
        break;
        case 0b1110: { // /\/\/\/\/\/\
          if (this.envFirstHalf) {
            this.curEnv = Math.min(ENV_MAX, this.curEnv + 1);
            this.envFirstHalf = this.curEnv < ENV_MAX;
          } else {
            this.curEnv = Math.max(ENV_MIN, this.curEnv - 1);
            this.envFirstHalf = this.curEnv == ENV_MIN;
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

    this.counterA += audioTicks;
    if (this.counterA >= this.tonePeriodA) {
      this.counterA = 0;
      this.signalNcba ^= SIGNAL_A;
    }

    this.counterB += audioTicks;
    if (this.counterB >= this.tonePeriodB) {
      this.counterB = 0;
      this.signalNcba ^= SIGNAL_B;
    }

    this.counterC += audioTicks;
    if (this.counterC >= this.tonePeriodC) {
      this.counterC = 0;
      this.signalNcba ^= SIGNAL_C;
    }
  }

  private void mixOutputSignals() {
    final int mixer = this.mixerControl;
    final int ncba = this.signalNcba;
    final int n = ncba >> 3;
    final int nmask = mixer >> 3;

    final int mixedCba = ncba | mixer;

    final int a = mixedCba & (n | nmask) & 1;
    final int b = (mixedCba >> 1) & (n | (nmask >> 1)) & 1;
    final int c = (mixedCba >> 2) & (n | (nmask >> 2)) & 1;

    final int envAmpl = this.curEnv;

    final int va = (AMPLITUDE_VALUES[a == 0 ? 0 :
        this.amplitudeA > 0xF ? envAmpl : this.amplitudeA] + this.lastVa) / 2;
    final int vb = (AMPLITUDE_VALUES[b == 0 ? 0 :
        this.amplitudeB > 0xF ? envAmpl : this.amplitudeB] + this.lastVb) / 2;
    final int vc = (AMPLITUDE_VALUES[c == 0 ? 0 :
        this.amplitudeC > 0xF ? envAmpl : this.amplitudeC] + this.lastVc) / 2;

    this.motherboard.getBeeper()
        .setChannelValue(Beeper.CHANNEL_AY_A, va);
    this.motherboard.getBeeper()
        .setChannelValue(Beeper.CHANNEL_AY_B, vb);
    this.motherboard.getBeeper()
        .setChannelValue(Beeper.CHANNEL_AY_C, vc);

    this.lastVa = va;
    this.lastVb = vb;
    this.lastVc = vc;
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

    this.signalNcba = 0;

    this.tonePeriodA = 0;
    this.tonePeriodB = 0;
    this.tonePeriodC = 0;

    this.envelopePeriod = 0;

    this.amplitudeA = 0;
    this.amplitudeB = 0;
    this.amplitudeC = 0;

    this.ioPortA = 0;
    this.ioPortB = 0;

    this.noisePeriod = 0;
    this.envelopeMode = 0;
    this.mixerControl = 0;

    this.rng = 1;

    this.lastVa = AMPLITUDE_VALUES[0];
    this.lastVb = AMPLITUDE_VALUES[0];
    this.lastVc = AMPLITUDE_VALUES[0];
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_POSTSTEP | NOTIFICATION_PRESTEP;
  }
}
