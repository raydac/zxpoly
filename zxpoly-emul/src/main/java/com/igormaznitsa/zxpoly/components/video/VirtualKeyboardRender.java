package com.igormaznitsa.zxpoly.components.video;

import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import com.igormaznitsa.zxpoly.components.Motherboard;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.util.Objects;

import static com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn.*;

public final class VirtualKeyboardRender {

  private static final long VKB_STICKY_KEYS = ZXKEY_CS | ZXKEY_SS;
  private static final long[] KEY_TABLE = new long[]{
          ZXKEY_1, ZXKEY_2, ZXKEY_3, ZXKEY_4, ZXKEY_5, ZXKEY_6, ZXKEY_7, ZXKEY_8, ZXKEY_9, ZXKEY_0,
          ZXKEY_Q, ZXKEY_W, ZXKEY_E, ZXKEY_R, ZXKEY_T, ZXKEY_Y, ZXKEY_U, ZXKEY_I, ZXKEY_O, ZXKEY_P,
          ZXKEY_A, ZXKEY_S, ZXKEY_D, ZXKEY_F, ZXKEY_G, ZXKEY_H, ZXKEY_J, ZXKEY_K, ZXKEY_L, ZXKEY_EN,
          ZXKEY_CS, ZXKEY_Z, ZXKEY_X, ZXKEY_C, ZXKEY_V, ZXKEY_B, ZXKEY_N, ZXKEY_M, ZXKEY_SS, ZXKEY_SP
  };
  private static final int[] BIT2KEY;
  private static final int TICKS_BEFORE_RESET_STICKY = 10;

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
  private final VirtualKeyboardDecoration vkbdDecoration;
  private volatile int ticksTillResetStyickyKeys = 0;
  private volatile long vkbKeysState = ZXKEY_NONE;
  private volatile MouseEvent lastMouseClickEvent = null;

  public VirtualKeyboardRender(final Motherboard motherboard, final VirtualKeyboardDecoration vkbdDecoration) {
    this.mainKeyboard = Objects.requireNonNull(motherboard.findIoDevice(KeyboardKempstonAndTapeIn.class));
    this.vkbdDecoration = Objects.requireNonNull(vkbdDecoration);
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
    gfx.drawImage(this.vkbdDecoration.getImage(), renderRectangle.x, renderRectangle.y, renderRectangle.width, renderRectangle.height, null);

    final MouseEvent lastClickEvent = this.lastMouseClickEvent;
    this.lastMouseClickEvent = null;
    processVkbMouseClick(renderRectangle, lastClickEvent);
    drawVkbState(gfx, renderRectangle);
  }

  private void drawVkbState(final Graphics2D gfx, final Rectangle keyboardArea) {
    long totalKeyboardState = ((this.mainKeyboard.getKeyState() & this.vkbKeysState) ^ ZXKEY_NONE) & ZXKEY_NONE;

    final double scaleX = (double) keyboardArea.width / this.vkbdDecoration.getWidth();
    final double scaleY = (double) keyboardArea.height / this.vkbdDecoration.getHeight();

    gfx.setColor(Color.GREEN);
    gfx.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.5f));
    int bitPos = 0;
    while (totalKeyboardState != 0) {
      if ((totalKeyboardState & 1L) != 0) {
        final Rectangle rectangle = this.vkbdDecoration.getKeyRectangleForBitIndex(bitPos);
        gfx.fillOval(keyboardArea.x + (int) Math.round(scaleX * rectangle.x), keyboardArea.y + (int) Math.round(scaleY * rectangle.y), (int) Math.round(scaleX * rectangle.width), (int) Math.round(scaleY * rectangle.height));
      }
      bitPos++;
      totalKeyboardState >>>= 1;
    }
  }

  public void preState(final boolean signalReset, final boolean tstatesIntReached,
                       boolean wallclockInt) {
    if (wallclockInt && this.ticksTillResetStyickyKeys > 0) {
      this.ticksTillResetStyickyKeys--;
      if (this.ticksTillResetStyickyKeys == 0) {
        this.vkbKeysState |= VKB_STICKY_KEYS;
      }
    }
  }

  public void processVkbMouseClick(final Rectangle keyboardArea, final MouseEvent mouseEvent) {
    long result = this.vkbKeysState;

    final double scaleX = (double) keyboardArea.width / this.vkbdDecoration.getWidth();
    final double scaleY = (double) keyboardArea.height / this.vkbdDecoration.getHeight();

    if (mouseEvent != null && keyboardArea.contains(mouseEvent.getPoint())) {
      final Point normalized = new Point((int) Math.round((mouseEvent.getPoint().x - keyboardArea.x) / scaleX), (int) Math.round((mouseEvent.getPoint().y - keyboardArea.y) / scaleY));
      final int pressedKeyBit = this.vkbdDecoration.findBitPosition(normalized);
      if (pressedKeyBit < 0) return;

      final long keyCode = KEY_TABLE[BIT2KEY[pressedKeyBit]];

      final boolean pressingEvent = mouseEvent.getID() == MouseEvent.MOUSE_PRESSED;

      if ((keyCode & VKB_STICKY_KEYS) == 0) {
        if (pressingEvent) {
          result &= ~keyCode;
        } else {
          result |= keyCode;
        }
      } else {
        if (pressingEvent) {
          result ^= keyCode;
          if ((result & VKB_STICKY_KEYS) == 0) {
            this.ticksTillResetStyickyKeys = TICKS_BEFORE_RESET_STICKY;
          }
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
    return this.vkbdDecoration.getHeight();
  }

  public int getImageWidth() {
    return this.vkbdDecoration.getWidth();
  }
}
