package com.igormaznitsa.zxpoly.components.gadapter;

import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import net.java.games.input.Controller;

public class GadapterKempston extends Gadapter {

  public GadapterKempston(KeyboardKempstonAndTapeIn keyboardModule, Controller controller) {
    super(keyboardModule, controller, GadapterType.KEMPSTON);
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
  protected void doCenterY() {
    this.parent.doKempstonCenterY();
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
  protected void doFire(final boolean pressed) {
    this.parent.doKempstonFire(pressed);
  }
}
