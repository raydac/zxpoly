package com.igormaznitsa.zxpoly.components.video;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.Properties;

public final class VirtualKeyboardDecoration {
  private static final String PATH = "/com/igormaznitsa/zxpoly/keyboard/";
  private final BufferedImage keyBoardImage;
  private final Rectangle[] keyCoords;

  VirtualKeyboardDecoration(final String id) throws IOException {
    final Properties properties = new Properties();
    final String propertiesResource = PATH + id + ".properties";
    try (final InputStream stream = Objects.requireNonNull(VirtualKeyboardDecoration.class.getResourceAsStream(propertiesResource), "Can't find " + propertiesResource)) {
      properties.load(stream);
    }
    final String imageResource = PATH + Objects.requireNonNull(properties.getProperty("image"), "Can't find image property in " + propertiesResource);

    try (final InputStream imageIn = Objects.requireNonNull(VirtualKeyboardRender.class.getResourceAsStream(imageResource), "Can't find " + imageResource)) {
      this.keyBoardImage = ImageIO.read(imageIn);
    }

    this.keyCoords = new Rectangle[64];
    // CS,Z,X,C,V
    this.keyCoords[0] = find("key.CS", properties);
    this.keyCoords[1] = find("key.Z", properties);
    this.keyCoords[2] = find("key.X", properties);
    this.keyCoords[3] = find("key.C", properties);
    this.keyCoords[4] = find("key.V", properties);

    // A,S,D,F,G
    this.keyCoords[8] = find("key.A", properties);
    this.keyCoords[9] = find("key.S", properties);
    this.keyCoords[10] = find("key.D", properties);
    this.keyCoords[11] = find("key.F", properties);
    this.keyCoords[12] = find("key.G", properties);

    // Q,W,E,R,T
    this.keyCoords[16] = find("key.Q", properties);
    this.keyCoords[17] = find("key.W", properties);
    this.keyCoords[18] = find("key.E", properties);
    this.keyCoords[19] = find("key.R", properties);
    this.keyCoords[20] = find("key.T", properties);

    // 1,2,3,4,5
    this.keyCoords[24] = find("key.1", properties);
    this.keyCoords[25] = find("key.2", properties);
    this.keyCoords[26] = find("key.3", properties);
    this.keyCoords[27] = find("key.4", properties);
    this.keyCoords[28] = find("key.5", properties);

    // 0,9,8,7,6
    this.keyCoords[32] = find("key.0", properties);
    this.keyCoords[33] = find("key.9", properties);
    this.keyCoords[34] = find("key.8", properties);
    this.keyCoords[35] = find("key.7", properties);
    this.keyCoords[36] = find("key.6", properties);

    // P,O,I,U,Y
    this.keyCoords[40] = find("key.P", properties);
    this.keyCoords[41] = find("key.O", properties);
    this.keyCoords[42] = find("key.I", properties);
    this.keyCoords[43] = find("key.U", properties);
    this.keyCoords[44] = find("key.Y", properties);

    // EN,L,K,J,H
    this.keyCoords[48] = find("key.EN", properties);
    this.keyCoords[49] = find("key.L", properties);
    this.keyCoords[50] = find("key.K", properties);
    this.keyCoords[51] = find("key.J", properties);
    this.keyCoords[52] = find("key.H", properties);

    // SP,SS,M.N.B
    this.keyCoords[56] = find("key.SP", properties);
    this.keyCoords[57] = find("key.SS", properties);
    this.keyCoords[58] = find("key.M", properties);
    this.keyCoords[59] = find("key.N", properties);
    this.keyCoords[60] = find("key.B", properties);

    final Rectangle zero = new Rectangle(0, 0, 0, 0);
    for (int i = 0; i < this.keyCoords.length; i++) {
      if (this.keyCoords[i] == null) this.keyCoords[i] = zero;
    }
  }

  private static Rectangle find(final String key, final Properties properties) {
    final String coords = Objects.requireNonNull(properties.getProperty(key), "Can't find key: " + key);
    final String[] splitted = coords.split("\\,");
    if (splitted.length != 4) throw new IllegalArgumentException("Wrong '" + key + "' key coordinates: " + coords);
    try {
      return new Rectangle(Integer.parseInt(splitted[0].trim()),
              Integer.parseInt(splitted[1].trim()),
              Integer.parseInt(splitted[2].trim()),
              Integer.parseInt(splitted[3].trim()));
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Wrong number in key '" + key + "' coordinates: " + coords);
    }
  }

  public int findBitPosition(final Point point) {
    for (int i = 0; i < this.keyCoords.length; i++) {
      if (this.keyCoords[i].contains(point)) return i;
    }
    return -1;
  }

  public int getWidth() {
    return this.keyBoardImage.getWidth();
  }

  public int getHeight() {
    return this.keyBoardImage.getHeight();
  }

  public BufferedImage getImage() {
    return this.keyBoardImage;
  }

  public Rectangle getKeyRectangleForBitIndex(final int bitIndex) {
    return this.keyCoords[bitIndex];
  }
}
