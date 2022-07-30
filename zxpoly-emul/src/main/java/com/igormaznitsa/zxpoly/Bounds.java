package com.igormaznitsa.zxpoly;

import static java.lang.Integer.parseInt;

import java.awt.Rectangle;
import picocli.CommandLine;

public class Bounds implements CommandLine.ITypeConverter<Bounds> {
  private final Integer x;
  private final Integer y;
  private final int width;
  private final int height;

  public Bounds() {
    this.x = null;
    this.y = null;
    this.width = -1;
    this.height = -1;
  }

  public Bounds(final Integer x, final Integer y, final int width, final int height) {
    this.x = x;
    this.y = y;
    this.width = width;
    this.height = height;
  }

  public int getX() {
    return this.x == null ? 0 : this.x;
  }

  public int getY() {
    return this.y == null ? 0 : this.y;
  }

  public int getWidth() {
    return this.width;
  }

  public int getHeight() {
    return this.height;
  }

  public boolean hasCoordinates() {
    return this.x != null && this.y != null;
  }

  @Override
  public Bounds convert(final String value) throws Exception {
    final String[] split = value.split(",");

    Integer x = null;
    Integer y = null;
    final int width;
    final int height;

    try {
      if (split.length == 4) {
        x = parseInt(split[0].trim());
        y = parseInt(split[1].trim());
        width = parseInt(split[2].trim());
        height = parseInt(split[3].trim());
      } else if (split.length == 2) {
        width = parseInt(split[0].trim());
        height = parseInt(split[1].trim());
      } else {
        throw new IllegalArgumentException(
            "Can't recognize as bounds, must have 2 or 4 numeric comma separated values: " + value);
      }
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Can't recognize a number at " + value);
    }

    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Width and height must be positive ones: " + value);
    }

    return new Bounds(x, y, width, height);
  }

  public Bounds withPositionIfNot(final int x, final int y) {
    if (this.x == null || this.y == null) {
      return new Bounds(x, y, this.width, this.height);
    } else {
      return this;
    }
  }

  public Rectangle asRectangle() {
    return new Rectangle(this.x == null ? 0 : this.x, this.y == null ? 0 : this.y, this.width,
        this.height);
  }
}
