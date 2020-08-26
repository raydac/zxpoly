package com.igormaznitsa.zxpoly.components.snd;

import com.igormaznitsa.zxpoly.components.Beeper;
import com.igormaznitsa.zxpoly.components.IoDevice;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;

public final class Zx128Ay8910 implements IoDevice, AySounder {

  private final Motherboard motherboard;
  private final Ay8910 ay8910;
  private final Beeper beeper;

  private int lastA;
  private int lastB;
  private int lastC;

  public Zx128Ay8910(final Motherboard motherboard) {
    this.motherboard = motherboard;
    this.ay8910 = new Ay8910(this::onAyLevels);
    this.beeper = this.motherboard.getBeeper();
  }

  private void onAyLevels(final Ay8910 ay, final int levelA, final int levelB, final int levelC) {
    final int a = (levelA + lastA) / 2;
    final int b = (levelB + lastB) / 2;
    final int c = (levelC + lastC) / 2;
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_A, AMPLITUDE_VALUES[a]);
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_B, AMPLITUDE_VALUES[b]);
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_C, AMPLITUDE_VALUES[c]);
    this.lastA = a;
    this.lastB = b;
    this.lastC = c;
  }

  @Override
  public Motherboard getMotherboard() {
    return this.motherboard;
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    if ((port & 2) == 0 && (port & 0x8000) == 0x8000) {
      return this.ay8910.readData();
    }
    return -1;
  }

  @Override
  public void writeIo(final ZxPolyModule module, final int port, final int value) {
    if ((port & 2) == 0) {
      if ((port & 0xC000) == 0xC000) {
        this.ay8910.writeAddress(value);
      } else if ((port & 0xC000) == 0x8000) {
        this.ay8910.writeData(value);
      }
    }
  }

  @Override
  public void preStep(boolean signalReset, boolean virtualIntTick, boolean wallClockInt) {
    if (signalReset) {
      this.doReset();
    }
  }

  @Override
  public void postStep(final long spentMachineCyclesForStep) {
    this.ay8910.step(spentMachineCyclesForStep);
  }

  @Override
  public String getName() {
    return "Zx128AY-8910";
  }

  @Override
  public void doReset() {
    this.lastA = 0;
    this.lastB = 0;
    this.lastC = 0;
    this.ay8910.reset();
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_POSTSTEP | NOTIFICATION_PRESTEP;
  }
}
