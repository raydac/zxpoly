package com.igormaznitsa.zxpoly.components.snd;

import com.igormaznitsa.zxpoly.components.IoDevice;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;

public final class Zx128Ay8910 implements IoDevice {

  private final Motherboard motherboard;
  private final Ay8910Chip ay8910;
  private final Beeper beeper;
  private final int[] audioLevels;

  public Zx128Ay8910(final Motherboard motherboard) {
    this.audioLevels = motherboard.getSoundLevels().getLevels();
    this.motherboard = motherboard;
    this.ay8910 = new Ay8910Chip(this::onAyLevels);
    this.beeper = this.motherboard.getBeeper();
  }

  private void onAyLevels(final Ay8910Chip ay, final int levelA, final int levelB,
                          final int levelC) {
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_A, this.audioLevels[levelA]);
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_B, this.audioLevels[levelB]);
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_C, this.audioLevels[levelC]);
  }

  @Override
  public Motherboard getMotherboard() {
    return this.motherboard;
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    if (!module.isTrdosActive() && (port & 2) == 0 && (port & 0x8000) == 0x8000) {
      return this.ay8910.readData();
    }
    return -1;
  }

  @Override
  public void writeIo(final ZxPolyModule module, final int port, final int value) {
    if (!module.isTrdosActive() && (port & 2) == 0) {
      if ((port & 0xC000) == 0xC000) {
        this.ay8910.writeAddress(value);
      } else if ((port & 0xC000) == 0x8000) {
        this.ay8910.writeData(value);
      }
    }
  }

  @Override
  public void preStep(int frameTiStates, boolean signalReset, boolean tstatesIntReached, boolean wallClockInt) {
    if (signalReset) {
      this.doReset();
    }
  }

  @Override
  public void postStep(final int spentTstates) {
    this.ay8910.step(spentTstates);
  }

  @Override
  public String getName() {
    return "Zx128AY-8910";
  }

  @Override
  public void doReset() {
    this.ay8910.reset();
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_POSTSTEP | NOTIFICATION_PRESTEP;
  }
}
