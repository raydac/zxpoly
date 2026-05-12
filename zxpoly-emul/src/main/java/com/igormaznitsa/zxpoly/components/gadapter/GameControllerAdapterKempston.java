package com.igormaznitsa.zxpoly.components.gadapter;

import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import de.gurkenlabs.input4j.InputDevice;

public class GameControllerAdapterKempston extends GameControllerAdapter {

  public GameControllerAdapterKempston(final KeyboardKempstonAndTapeIn keyboardModule,
                                       final InputDevice inputDevice) {
    super(keyboardModule, inputDevice, GameControllerAdapterType.KEMPSTON);
  }

  @Override
  protected void doLeft() {
    this.parent.doKempstonLeft();
  }

  @Override
  protected void doRight() {
    this.parent.doKempstonRight();
  }

  @Override
  protected void doCenterX() {
    this.parent.doKempstonCenterX();
  }

  @Override
  protected void doUp() {
    this.parent.doKempstonUp();
  }

  @Override
  protected void doDown() {
    this.parent.doKempstonDown();
  }

  @Override
  protected void doCenterY() {
    this.parent.doKempstonCenterY();
  }

  @Override
  protected void doFire(final boolean pressed) {
    this.parent.doKempstonFire(pressed);
  }
}
