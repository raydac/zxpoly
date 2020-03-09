package com.igormaznitsa.zxpoly.components.gadapter;

import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import net.java.games.input.Controller;

public class GadapterInterface2 extends Gadapter {

  private final int playerIndex;

  public GadapterInterface2(final int index, KeyboardKempstonAndTapeIn keyboardModule, Controller controller) {
    super(keyboardModule, controller, GadapterType.INTERFACEII_PLAYER1);
    this.playerIndex = index;
  }

  @Override
  protected void doLeft() {
    this.parent.doInterface2Left(this.playerIndex);
  }

  @Override
  protected void doRight() {
    this.parent.doInterface2Right(this.playerIndex);
  }

  @Override
  protected void doCenterX() {
    this.parent.doInterface2CenterX(this.playerIndex);
  }

  @Override
  protected void doUp() {
    this.parent.doInterface2Up(this.playerIndex);
  }

  @Override
  protected void doDown() {
    this.parent.doInterface2Down(this.playerIndex);
  }

  @Override
  protected void doCenterY() {
    this.parent.doInterface2CenterY(this.playerIndex);
  }

  @Override
  protected void doFire(final boolean pressed) {
    this.parent.doInterface2Fire(this.playerIndex, pressed);
  }
}
