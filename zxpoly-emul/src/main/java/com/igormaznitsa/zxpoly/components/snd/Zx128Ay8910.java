package com.igormaznitsa.zxpoly.components.snd;

import com.igormaznitsa.zxpoly.components.Beeper;
import com.igormaznitsa.zxpoly.components.IoDevice;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import java.util.Arrays;

public final class Zx128Ay8910 implements IoDevice {

  private static final int[] AMPLITUDE_VALUES;

  static {
    AMPLITUDE_VALUES = Arrays.stream(new double[] {
        0.0000d, 0.0137d, 0.0205d, 0.0291d, 0.0423d, 0.0618d, 0.0847d, 0.1369d,
        0.1691d, 0.2647d, 0.3527d, 0.4499d, 0.5704d, 0.6873d, 0.8482d, 1.0000d
    }).map(x -> Math.min(Beeper.MAX_AMPLITUDE, Math.round(x * Beeper.MAX_AMPLITUDE)))
        .mapToInt(x -> (int) x)
        .toArray();
  }

  private final Motherboard motherboard;
  private final Ay8910 ay8910;
  private final Beeper beeper;

  public Zx128Ay8910(final Motherboard motherboard) {
    this.motherboard = motherboard;
    this.ay8910 = new Ay8910(this::onAyLevels);
    this.beeper = this.motherboard.getBeeper();
  }

  private void onAyLevels(final Ay8910 ay, final int levelA, final int levelB, final int levelC) {
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_A, AMPLITUDE_VALUES[levelA]);
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_B, AMPLITUDE_VALUES[levelB]);
    this.beeper.setChannelValue(Beeper.CHANNEL_AY_C, AMPLITUDE_VALUES[levelC]);
  }

  @Override
  public Motherboard getMotherboard() {
    return this.motherboard;
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    if ((port & 0xFF) == 0xFD) {
      return this.ay8910.readData();
    }
    return -1;
  }

  @Override
  public void writeIo(final ZxPolyModule module, final int port, final int value) {
    if ((port & 0xFF) == 0xFD) {
      if ((port & 0x4000) == 0x4000) {
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
    this.ay8910.reset();
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_POSTSTEP | NOTIFICATION_PRESTEP;
  }
}
