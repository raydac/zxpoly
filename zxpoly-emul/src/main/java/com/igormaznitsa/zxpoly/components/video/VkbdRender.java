package com.igormaznitsa.zxpoly.components.video;

import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Objects;

import static com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn.*;

public final class VkbdRender {

  private static final long VKB_STICKY_KEYS = ZXKEY_CS | ZXKEY_SS;
  private static final long[] KEY_TABLE = new long[]{
          ZXKEY_1, ZXKEY_2, ZXKEY_3, ZXKEY_4, ZXKEY_5, ZXKEY_6, ZXKEY_7, ZXKEY_8, ZXKEY_9, ZXKEY_0,
          ZXKEY_Q, ZXKEY_W, ZXKEY_E, ZXKEY_R, ZXKEY_T, ZXKEY_Y, ZXKEY_U, ZXKEY_I, ZXKEY_O, ZXKEY_P,
          ZXKEY_A, ZXKEY_S, ZXKEY_D, ZXKEY_F, ZXKEY_G, ZXKEY_H, ZXKEY_J, ZXKEY_K, ZXKEY_L, ZXKEY_EN,
          ZXKEY_CS, ZXKEY_Z, ZXKEY_X, ZXKEY_C, ZXKEY_V, ZXKEY_B, ZXKEY_N, ZXKEY_M, ZXKEY_SS, ZXKEY_SP
  };
  private static final int[] BIT2KEY;
  private static final BufferedImage IMAGE_ZXKEYS = Utils.loadIcon("zxkeys.png");

  static {
    BIT2KEY = new int[64];
    for (int keyIndex = 0; keyIndex < KEY_TABLE.length; keyIndex++) {
      long acc = KEY_TABLE[keyIndex];
      int arrayIndex = 0;
      while (acc != 0) {
        acc >>>= 1;
        arrayIndex++;
      }
      BIT2KEY[arrayIndex - 1] = keyIndex;
    }
  }

  private final KeyboardKempstonAndTapeIn mainKeyboard;
  private volatile long vkbKeysState = ZXKEY_NONE;
  private volatile MouseEvent lastMouseClickEvent = null;

  public VkbdRender(final Motherboard motherboard) {
    this.mainKeyboard = Objects.requireNonNull(motherboard.findIoDevice(KeyboardKempstonAndTapeIn.class));
  }

  public void setLastMouseEvent(final MouseEvent clickMouseEvent) {
    this.lastMouseClickEvent = clickMouseEvent;
  }

  public void render(final Component parent, final Graphics2D gfx, final Rectangle renderRectangle, final boolean transparentIfNotFocused) {
    final PointerInfo pointerInfo = MouseInfo.getPointerInfo();
    final Point mousePoint = new Point(pointerInfo.getLocation());
    SwingUtilities.convertPointFromScreen(mousePoint, parent);

    if (transparentIfNotFocused) {
      gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, renderRectangle.contains(mousePoint) ? 1.0f : 0.5f));
    }
    gfx.drawImage(IMAGE_ZXKEYS, renderRectangle.x, renderRectangle.y, renderRectangle.width, renderRectangle.height, null);

    final MouseEvent lastClickEvent = this.lastMouseClickEvent;
    this.lastMouseClickEvent = null;
    processVkbMouseClick(renderRectangle, lastClickEvent);
    drawVkbState(gfx, renderRectangle);
  }

  private void drawVkbState(final Graphics2D gfx, final Rectangle keyboardArea) {
    final int keyAreaW = keyboardArea.width / 10;
    final int keyAreaH = keyboardArea.height / 4;

    long keyBits = ((this.mainKeyboard.getKeyState() & this.vkbKeysState) ^ ZXKEY_NONE) & ZXKEY_NONE;

    gfx.setColor(Color.GREEN);
    gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
    int bitPos = 0;
    while (keyBits != 0) {
      if ((keyBits & 1L) != 0) {
        final int keyIndex = BIT2KEY[bitPos];
        final int px = (keyIndex % 10) * keyAreaW;
        final int py = (keyIndex / 10) * keyAreaH;
        gfx.fillOval(px + keyboardArea.x, py + keyboardArea.y, keyAreaW, keyAreaH);
      }
      bitPos++;
      keyBits >>>= 1;
    }
  }

  public void processVkbMouseClick(final Rectangle keyboardArea, final MouseEvent mouseEvent) {
    final int keyAreaW = keyboardArea.width / 10;
    final int keyAreaH = keyboardArea.height / 4;

    long result = this.vkbKeysState;

    if (mouseEvent != null && keyboardArea.contains(mouseEvent.getPoint())) {
      final Point mousePoint = mouseEvent.getPoint();
      final int kx = (mousePoint.x - keyboardArea.x) / keyAreaW;
      final int ky = (mousePoint.y - keyboardArea.y) / keyAreaH;

      final long keyMask = KEY_TABLE[ky * 10 + kx];

      final boolean pressingEvent = mouseEvent.getID() == MouseEvent.MOUSE_PRESSED;

      if ((keyMask & VKB_STICKY_KEYS) == 0) {
        if (pressingEvent) {
          result &= ~keyMask;
        } else {
          result |= keyMask;
        }
      } else {
        if (pressingEvent) {
          result ^= keyMask;
        }
      }
    }
    this.vkbKeysState = result;
  }

  public void doReset() {
    this.vkbKeysState = ZXKEY_NONE;
    this.lastMouseClickEvent = null;
  }

  public long getKeyState() {
    return this.vkbKeysState;
  }

  public int getImageHeight() {
    return IMAGE_ZXKEYS.getHeight();
  }

  public int getImageWidth() {
    return IMAGE_ZXKEYS.getWidth();
  }
}
