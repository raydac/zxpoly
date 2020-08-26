package com.igormaznitsa.zxpoly.components.snd;

import com.igormaznitsa.zxpoly.components.Beeper;
import com.igormaznitsa.zxpoly.components.IoDevice;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;

public final class TurboSoundNedoPc implements IoDevice, AySounder {

  private final Motherboard motherboard;
  private final Beeper beeper;

  private final Ay8910 chipAy0;
  private final Ay8910 chipAy1;

  private Ay8910 selectedChip;

  private int lastA0;
  private int lastB0;
  private int lastC0;
  private int lastA1;
  private int lastB1;
  private int lastC1;

  public TurboSoundNedoPc(final Motherboard motherboard) {
    this.motherboard = motherboard;
    this.beeper = this.motherboard.getBeeper();
    this.chipAy0 = new Ay8910(this::onLevels0);
    this.chipAy1 = new Ay8910(this::onLevels1);

    this.doReset();
  }

  private void onLevels0(final Ay8910 ay, final int levelA, final int levelB, final int levelC) {
    final int a = levelA > 4 ? (levelA + this.lastA0) / 2 : levelA;
    final int b = levelB > 4 ? (levelB + this.lastB0) / 2 : levelB;
    final int c = levelC > 4 ? (levelC + this.lastC0) / 2 : levelC;
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_A, AMPLITUDE_VALUES[a]);
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_B, AMPLITUDE_VALUES[b]);
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_C, AMPLITUDE_VALUES[c]);
    this.lastA0 = a;
    this.lastB0 = b;
    this.lastC0 = c;
  }

  private void onLevels1(final Ay8910 ay, final int levelA, final int levelB, final int levelC) {
    final int a = levelA > 4 ? (levelA + this.lastA1) / 2 : levelA;
    final int b = levelB > 4 ? (levelB + this.lastB1) / 2 : levelB;
    final int c = levelC > 4 ? (levelC + this.lastC1) / 2 : levelC;
    this.beeper.setChannelValue(Beeper.CHANNEL_RESERV_0, AMPLITUDE_VALUES[a]);
    this.beeper.setChannelValue(Beeper.CHANNEL_RESERV_1, AMPLITUDE_VALUES[b]);
    this.beeper.setChannelValue(Beeper.CHANNEL_RESERV_2, AMPLITUDE_VALUES[c]);
    this.lastA1 = a;
    this.lastB1 = b;
    this.lastC1 = c;
  }

  @Override
  public Motherboard getMotherboard() {
    return this.motherboard;
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    if ((port & 2) == 0 && (port & 0x8000) == 0x8000) {
      return this.selectedChip.readData();
    }
    return -1;
  }

  @Override
  public void writeIo(final ZxPolyModule module, final int port, final int value) {
    if ((port & 2) == 0) {
      if ((port & 0xC000) == 0xC000) {
        if (value == 0xFF) {
          this.selectedChip = this.chipAy0;
        } else if (value == 0xFE) {
          this.selectedChip = this.chipAy1;
        }
        this.selectedChip.writeAddress(value);
      } else if ((port & 0xC000) == 0x8000) {
        this.selectedChip.writeData(value);
      }
    }
  }

  @Override
  public void preStep(
      final boolean signalReset,
      final boolean virtualIntTick,
      final boolean wallclockInt
  ) {
    if (signalReset) {
      this.doReset();
    }
  }

  @Override
  public void postStep(final long spentMachineCyclesForStep) {
    this.chipAy0.step(spentMachineCyclesForStep);
    this.chipAy1.step(spentMachineCyclesForStep);
  }

  @Override
  public String getName() {
    return "TurboSound-NedoPc";
  }

  @Override
  public void doReset() {
    this.selectedChip = this.chipAy0;
    this.lastA0 = 0;
    this.lastB0 = 0;
    this.lastC0 = 0;
    this.lastA1 = 0;
    this.lastB1 = 0;
    this.lastC1 = 0;
    this.chipAy0.reset();
    this.chipAy1.reset();
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_POSTSTEP | NOTIFICATION_PRESTEP;
  }
}
