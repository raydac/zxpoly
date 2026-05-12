package com.igormaznitsa.zxpoly.components.gadapter;

import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import de.gurkenlabs.input4j.InputDevice;

public class GameControllerAdapterInterface2 extends GameControllerAdapter {

  public GameControllerAdapterInterface2(final KeyboardKempstonAndTapeIn keyboardModule,
                                         final InputDevice inputDevice,
                                         final GameControllerAdapterType destination) {
    super(keyboardModule, inputDevice, destination);
    if (destination != GameControllerAdapterType.INTERFACEII_PLAYER1
        && destination != GameControllerAdapterType.INTERFACEII_PLAYER2) {
      throw new IllegalArgumentException(destination.name());
    }
  }

  private int playerIndex() {
    return this.getType() == GameControllerAdapterType.INTERFACEII_PLAYER2 ? 1 : 0;
  }

  @Override
  protected void doLeft() {
    this.parent.doInterface2Left(this.playerIndex());
  }

  @Override
  protected void doRight() {
    this.parent.doInterface2Right(this.playerIndex());
  }

  @Override
  protected void doCenterX() {
    this.parent.doInterface2CenterX(this.playerIndex());
  }

  @Override
  protected void doUp() {
    this.parent.doInterface2Up(this.playerIndex());
  }

  @Override
  protected void doDown() {
    this.parent.doInterface2Down(this.playerIndex());
  }

  @Override
  protected void doCenterY() {
    this.parent.doInterface2CenterY(this.playerIndex());
  }

  @Override
  protected void doFire(final boolean pressed) {
    this.parent.doInterface2Fire(this.playerIndex(), pressed);
  }
}
