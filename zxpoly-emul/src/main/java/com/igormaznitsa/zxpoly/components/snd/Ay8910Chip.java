package com.igormaznitsa.zxpoly.components.snd;

import java.util.Objects;

public final class Ay8910Chip {

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

  private static final int ENV_FLAG_HOLD = 0b0001;
  private static final int ENV_FLAG_ALTR = 0b0010;
  private static final int ENV_FLAG_ATTACK = 0b0100;
  private static final int ENV_FLAG_CONT = 0b1000;
  private static final int[] REG_DATA_MASK = new int[] {
      0xFF, 0x0F, 0xFF, 0x0F, 0xFF, 0x0F, 0x1F, 0xFF,
      0x1F, 0x1F, 0x1F, 0xFF, 0xFF, 0x0F, 0xFF, 0xFF
  };
  private final Ay8910SignalConsumer signalConsumer;
  private boolean enfAttack = false;
  private boolean enfAlter = false;
  private boolean enfCont = false;
  private boolean enfHold = false;
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
  private long machineCycleCounter;
  private int counterE;
  private int envIndexCounter;
  private int envelopeVolume;
  private int rngReg = 1;

  public Ay8910Chip(final Ay8910SignalConsumer signalConsumer) {
    this.signalConsumer = Objects.requireNonNull(signalConsumer);
  }

  public int readAddress() {
    return this.addressLatch;
  }

  public void writeAddress(final int address) {
    this.addressLatch = address & 0xF;
  }

  public int readData() {
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
        return -1;
      }
    }
  }

  public void writeData(final int value) {
    this.writeData(this.addressLatch, value);
  }

  public void writeData(final int address, int value) {
    value &= REG_DATA_MASK[address & 0xF];

    switch (address) {
      case REG_TONE_PERIOD_A_FINE: {
        this.tonePeriodA = (this.tonePeriodA & 0xF00) | value;
      }
      break;
      case REG_TONE_PERIOD_A_ROUGH: {
        this.tonePeriodA = (this.tonePeriodA & 0xFF) | (value << 8);
      }
      break;
      case REG_TONE_PERIOD_B_FINE: {
        this.tonePeriodB = (this.tonePeriodB & 0x0F00) | value;
      }
      break;
      case REG_TONE_PERIOD_B_ROUGH: {
        this.tonePeriodB = (this.tonePeriodB & 0xFF) | (value << 8);
      }
      break;
      case REG_TONE_PERIOD_C_FINE: {
        this.tonePeriodC = (this.tonePeriodC & 0x0F00) | value;
      }
      break;
      case REG_TONE_PERIOD_C_ROUGH: {
        this.tonePeriodC = (this.tonePeriodC & 0xFF) | (value << 8);
      }
      break;
      case REG_NOISE_PERIOD: {
        this.noisePeriod = value;
      }
      break;
      case REG_MIXER_CTRL: {
        this.mixerControl = value;
      }
      break;
      case REG_AMPL_A: {
        this.amplitudeA = value;
      }
      break;
      case REG_AMPL_B: {
        this.amplitudeB = value;
      }
      break;
      case REG_AMPL_C: {
        this.amplitudeC = value;
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
        this.enfAlter = (value & ENV_FLAG_ALTR) != 0;
        this.enfAttack = (value & ENV_FLAG_ATTACK) != 0;
        this.enfHold = (value & ENV_FLAG_HOLD) != 0;
        this.enfCont = (value & ENV_FLAG_CONT) != 0;
        this.envIndexCounter = 0;
        this.envelopeVolume = this.enfAttack ? ENV_MIN : ENV_MAX;
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

  private void doRndNoise() {
    rngReg ^= (((rngReg & 1) ^ ((rngReg >> 3) & 1)) << 17);
    rngReg >>= 1;
    this.signalNcba =
        (rngReg & 1) == 0 ? this.signalNcba & ~SIGNAL_N : this.signalNcba | SIGNAL_N;
  }

  private void processNoiseGen(final int audioTicks) {
    this.counterN += audioTicks;
    if (this.counterN >= (this.noisePeriod == 0 ? 1 : this.noisePeriod)) {
      this.counterN = 0;
      this.doRndNoise();
    }
  }

  private void updateEnvelopeVolume(final int audioTicks) {
    this.counterE += audioTicks;

    if (this.counterE >= (this.envelopePeriod == 0 ? 2 : this.envelopePeriod << 1)) {
      this.counterE = 0;

      if (this.envIndexCounter >= 0) {
        int count = this.envIndexCounter;

        count++;
        count &= 31;

        final int envIndex;

        if (count < 16) {
          envIndex = enfAttack ? count : (ENV_MAX - count);
        } else if (count == 16 && (!enfCont || enfHold)) {
          envIndex = enfCont && (enfAttack ^ enfAlter) ? ENV_MAX : ENV_MIN;
          count = -1;
        } else if (count == 16 && !enfAlter) {
          count = 0;
          envIndex = enfAttack ? ENV_MIN : ENV_MAX;
        } else {
          envIndex = enfAttack ? ENV_MAX - count : count;
        }

        this.envIndexCounter = count;

        this.envelopeVolume = envIndex & 15;
      }
    }
  }

  private void processPeriods(final int audioTicks) {
    this.processNoiseGen(audioTicks);

    this.counterA += audioTicks;
    if (this.counterA >= (this.tonePeriodA == 0 ? 1 : this.tonePeriodA)) {
      this.counterA = 0;
      this.signalNcba ^= SIGNAL_A;
    }

    this.counterB += audioTicks;
    if (this.counterB >= (this.tonePeriodB == 0 ? 1 : this.tonePeriodB)) {
      this.counterB = 0;
      this.signalNcba ^= SIGNAL_B;
    }

    this.counterC += audioTicks;
    if (this.counterC >= (this.tonePeriodC == 0 ? 1 : this.tonePeriodC)) {
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

    final int va = a == 0 ? 0 :
        (this.amplitudeA & 0x10) == 0 ? this.amplitudeA : this.envelopeVolume;
    final int vb = b == 0 ? 0 :
        (this.amplitudeB & 0x10) == 0 ? this.amplitudeB : this.envelopeVolume;
    final int vc = c == 0 ? 0 :
        (this.amplitudeC & 0x10) == 0 ? this.amplitudeC : this.envelopeVolume;

    this.signalConsumer.onAy8910Levels(this, va, vb, vc);
  }

  public void step(final long spentMachineCyclesForStep) {
    this.machineCycleCounter += spentMachineCyclesForStep;

    if (this.machineCycleCounter >= MACHINE_CYCLES_PER_ATICK) {
      final int audioTicks = (int) (this.machineCycleCounter / MACHINE_CYCLES_PER_ATICK);
      this.machineCycleCounter %= MACHINE_CYCLES_PER_ATICK;
      processPeriods(audioTicks);
      updateEnvelopeVolume(audioTicks);
    }
    this.mixOutputSignals();
  }

  public void reset() {
    this.addressLatch = 0;

    this.rngReg = 1;

    final int ctrOffset = (int) (System.nanoTime() & 0x0F);

    this.counterA = ctrOffset;
    this.counterB = ctrOffset;
    this.counterC = ctrOffset;
    this.counterE = ctrOffset;
    this.counterN = ctrOffset;

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
    this.envIndexCounter = 0;
    this.enfAlter = false;
    this.enfAttack = false;
    this.enfCont = false;
    this.enfHold = false;
    this.envelopeVolume = 0;

    this.mixerControl = 0;
  }

  @FunctionalInterface
  public interface Ay8910SignalConsumer {
    void onAy8910Levels(Ay8910Chip ay, int levelA, int levelB, int levelC);
  }

}
