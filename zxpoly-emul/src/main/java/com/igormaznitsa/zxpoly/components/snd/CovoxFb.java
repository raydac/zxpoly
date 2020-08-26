package com.igormaznitsa.zxpoly.components.snd;

import static com.igormaznitsa.zxpoly.components.snd.Beeper.CHANNEL_COVOX;


import com.igormaznitsa.zxpoly.components.IoDevice;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;

public final class CovoxFb implements IoDevice {

  private final Motherboard motherboard;
  private final Beeper beeper;

  public CovoxFb(final Motherboard motherboard) {
    this.motherboard = motherboard;
    this.beeper = this.motherboard.getBeeper();
  }

  @Override
  public Motherboard getMotherboard() {
    return this.motherboard;
  }

  @Override
  public int readIo(ZxPolyModule module, int port) {
    return -1;
  }

  @Override
  public void writeIo(ZxPolyModule module, int port, int value) {
    if (!module.isTrdosActive() && (port & 0b100) == 0) {
      this.beeper.setChannelValue(CHANNEL_COVOX, value);
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
    return "Covox FB";
  }

  @Override
  public void doReset() {
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_NONE;
  }
}
