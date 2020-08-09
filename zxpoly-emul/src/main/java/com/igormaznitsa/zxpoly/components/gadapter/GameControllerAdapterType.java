package com.igormaznitsa.zxpoly.components.gadapter;

public enum GameControllerAdapterType {
  NONE(""),
  KEMPSTON("Kempston"),
  INTERFACEII_PLAYER1("InterfaceII Player1"),
  INTERFACEII_PLAYER2("InterfaceII Player2");

  private final String description;

  GameControllerAdapterType(final String description) {
    this.description = description;
  }

  public String getDescription() {
    return this.description;
  }

  @Override
  public String toString() {
    return this.description;
  }
}
