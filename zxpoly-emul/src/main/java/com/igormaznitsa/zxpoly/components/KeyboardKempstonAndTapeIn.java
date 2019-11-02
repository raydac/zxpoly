/*
 * Copyright (C) 2014-2019 Igor Maznitsa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.igormaznitsa.zxpoly.components;

import static java.lang.System.arraycopy;


import java.awt.event.KeyEvent;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class KeyboardKempstonAndTapeIn implements IoDevice {

  private static final int KEMPSTON_RIGHT = 1;
  private static final int KEMPSTON_LEFT = 2;
  private static final int KEMPSTON_DOWN = 4;
  private static final int KEMPSTON_UP = 8;
  private static final int KEMPSTON_FIRE = 16;

  private static final int TAP_BIT = 0b01000000;

  private final Motherboard board;

  private final int[] keyboardLines = new int[8];
  private final int[] bufferKeyboardLines = new int[8];
  private final AtomicInteger kempstonSignals = new AtomicInteger();
  private final AtomicReference<TapeFileReader> tap = new AtomicReference<>();
  private int kempstonBuffer = 0;

  public KeyboardKempstonAndTapeIn(final Motherboard board) {
    this.board = board;
  }

  private int getKbdValueForLines(int hiByte) {
    int result = 0xFF;
    for (int i = 0; i < 8; i++) {
      if ((hiByte & 1) == 0) {
        result &= this.bufferKeyboardLines[i];
      }
      hiByte >>= 1;
    }
    return result;
  }

  private int readKeyboardAndTap(final int port, final TapeFileReader tapeFileReader) {
    final int tapbit = tapeFileReader == null ? 0 : tapeFileReader.getSignal() ? TAP_BIT : 0;
    return (getKbdValueForLines(port >>> 8) & ~TAP_BIT) | tapbit;
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    int result = -1;
    if (!module.isTrdosActive()) {
      final int normalizedPort = port & 0xFF;

      if ((normalizedPort & 1) == 0) {
        if (module.getMotherboard().isZxPolyMode()) {
          if (normalizedPort == 0xFE) {
            result = readKeyboardAndTap(port, this.getTap());
          }
        } else {
          result = readKeyboardAndTap(port, this.getTap());
        }
      } else {
        if (normalizedPort == 0x1F) {// KEMPSTON
          result = this.kempstonBuffer;
        }
      }
    }
    return result;
  }

  @Override
  public void writeIo(final ZxPolyModule module, final int port, final int value) {
  }

  @Override
  public Motherboard getMotherboard() {
    return this.board;
  }

  @Override
  public void doReset() {
    synchronized (this.keyboardLines) {
      Arrays.fill(this.keyboardLines, 0x1F);
    }
    this.kempstonSignals.set(0);
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_PRESTEP | NOTIFICATION_POSTSTEP;
  }

  @Override
  public void preStep(final boolean signalReset, final boolean signalInt) {
    if (signalReset) {
      doReset();
    }
    synchronized (this.keyboardLines) {
      arraycopy(this.keyboardLines, 0, this.bufferKeyboardLines, 0, 8);
    }
    this.kempstonBuffer = this.kempstonSignals.get();
  }

  @Override
  public String getName() {
    return "Keyboard";
  }

  public TapeFileReader getTap() {
    return this.tap.get();
  }

  public void setTap(final TapeFileReader tap) {
    this.tap.set(tap);
  }

  public void onKeyEvent(final KeyEvent evt) {
    final boolean pressed;
    switch (evt.getID()) {
      case KeyEvent.KEY_PRESSED:
        pressed = true;
        break;
      case KeyEvent.KEY_RELEASED:
        pressed = false;
        break;
      default:
        return;
    }

    int line = 0;
    int code = 0;
    int kempston = 0;

    switch (evt.getKeyCode()) {
      case KeyEvent.VK_ESCAPE: {
        if (this.board.getVideoController().isHoldMouse()) {
          this.board.getVideoController().setHoldMouse(false);
        }
      }
      break;
      case KeyEvent.VK_KP_LEFT:
      case KeyEvent.VK_NUMPAD4: {
        kempston = KEMPSTON_LEFT;
      }
      break;
      case KeyEvent.VK_KP_UP:
      case KeyEvent.VK_NUMPAD8: {
        kempston = KEMPSTON_UP;
      }
      break;
      case KeyEvent.VK_KP_RIGHT:
      case KeyEvent.VK_NUMPAD6: {
        kempston = KEMPSTON_RIGHT;
      }
      break;
      case KeyEvent.VK_KP_DOWN:
      case KeyEvent.VK_NUMPAD2: {
        kempston = KEMPSTON_DOWN;
      }
      break;
      case 65368: // NUMPAD 5 
      case KeyEvent.VK_NUMPAD5: {
        kempston = KEMPSTON_FIRE;
      }
      break;
      case KeyEvent.VK_1: {
        line = 3;
        code = 0x1E;
      }
      break;
      case KeyEvent.VK_2: {
        line = 3;
        code = 0x1D;
      }
      break;
      case KeyEvent.VK_3: {
        line = 3;
        code = 0x1B;
      }
      break;
      case KeyEvent.VK_4: {
        line = 3;
        code = 0x17;
      }
      break;
      case KeyEvent.VK_5: {
        line = 3;
        code = 0x0F;
      }
      break;
      case KeyEvent.VK_6: {
        line = 4;
        code = 0x0F;
      }
      break;
      case KeyEvent.VK_7: {
        line = 4;
        code = 0x17;
      }
      break;
      case KeyEvent.VK_8: {
        line = 4;
        code = 0x1B;
      }
      break;
      case KeyEvent.VK_9: {
        line = 4;
        code = 0x1D;
      }
      break;
      case KeyEvent.VK_0: {
        line = 4;
        code = 0x1E;
      }
      break;
      case KeyEvent.VK_Q: {
        line = 2;
        code = 0x1E;
      }
      break;
      case KeyEvent.VK_W: {
        line = 2;
        code = 0x1D;
      }
      break;
      case KeyEvent.VK_E: {
        line = 2;
        code = 0x1B;
      }
      break;
      case KeyEvent.VK_R: {
        line = 2;
        code = 0x17;
      }
      break;
      case KeyEvent.VK_T: {
        line = 2;
        code = 0x0F;
      }
      break;
      case KeyEvent.VK_Y: {
        line = 5;
        code = 0x0F;
      }
      break;
      case KeyEvent.VK_U: {
        line = 5;
        code = 0x17;
      }
      break;
      case KeyEvent.VK_I: {
        line = 5;
        code = 0x1B;
      }
      break;
      case KeyEvent.VK_O: {
        line = 5;
        code = 0x1D;
      }
      break;
      case KeyEvent.VK_P: {
        line = 5;
        code = 0x1E;
      }
      break;
      case KeyEvent.VK_A: {
        line = 1;
        code = 0x1E;
      }
      break;
      case KeyEvent.VK_S: {
        line = 1;
        code = 0x1D;
      }
      break;
      case KeyEvent.VK_D: {
        line = 1;
        code = 0x1B;
      }
      break;
      case KeyEvent.VK_F: {
        line = 1;
        code = 0x17;
      }
      break;
      case KeyEvent.VK_G: {
        line = 1;
        code = 0x0F;
      }
      break;
      case KeyEvent.VK_H: {
        line = 6;
        code = 0x0F;
      }
      break;
      case KeyEvent.VK_J: {
        line = 6;
        code = 0x17;
      }
      break;
      case KeyEvent.VK_K: {
        line = 6;
        code = 0x1B;
      }
      break;
      case KeyEvent.VK_L: {
        line = 6;
        code = 0x1D;
      }
      break;
      case KeyEvent.VK_ENTER: {
        line = 6;
        code = 0x1E;
      }
      break;
      case KeyEvent.VK_Z: {
        line = 0;
        code = 0x1D;
      }
      break;
      case KeyEvent.VK_X: {
        line = 0;
        code = 0x1B;
      }
      break;
      case KeyEvent.VK_C: {
        line = 0;
        code = 0x17;
      }
      break;
      case KeyEvent.VK_V: {
        line = 0;
        code = 0x0F;
      }
      break;
      case KeyEvent.VK_B: {
        line = 7;
        code = 0x0F;
      }
      break;
      case KeyEvent.VK_N: {
        line = 7;
        code = 0x17;
      }
      break;
      case KeyEvent.VK_M: {
        line = 7;
        code = 0x1B;
      }
      break;
      case KeyEvent.VK_SPACE: {
        line = 7;
        code = 0x1E;
      }
      break;
      case KeyEvent.VK_SHIFT: {
        line = 0;
        code = 0x1E;
      }
      break;
      case KeyEvent.VK_ALT: {
        line = 7;
        code = 0x1D;
      }
      break;
      case KeyEvent.VK_BACK_SPACE: {
        line = 0x0004;
        code = 0x1E1E;
      }
      break;
      case KeyEvent.VK_LEFT: {
        line = 0x0003;
        code = 0x1E0F;
      }
      break;
      case KeyEvent.VK_RIGHT: {
        line = 0x0004;
        code = 0x1E1B;
      }
      break;
      case KeyEvent.VK_UP: {
        line = 0x0004;
        code = 0x1E17;
      }
      break;
      case KeyEvent.VK_DOWN: {
        line = 0x0004;
        code = 0x1E0F;
      }
      break;
      case KeyEvent.VK_COMMA: {
        line = 0x0707;
        code = 0x1D17;
      }
      break;
      case KeyEvent.VK_PERIOD: {
        line = 0x0707;
        code = 0x1D1B;
      }
      break;
      case KeyEvent.VK_EQUALS: {
        line = 0x0706;
        code = 0x1D1D;
      }
      break;
    }

    while (code != 0) {
      final int theline = line & 0xFF;
      final int thecode = code & 0xFF;

      line >>>= 8;
      code >>>= 8;

      synchronized (this.keyboardLines) {
        if (pressed) {
          this.keyboardLines[theline] &= thecode;
        } else {
          this.keyboardLines[theline] |= (~thecode & 0x1F);
        }
      }
    }

    if (kempston != 0) {
      if (pressed) {
        this.kempstonSignals.set(kempston | this.kempstonSignals.get());
      } else {
        this.kempstonSignals.set((~kempston & this.kempstonSignals.get()) & 0xFF);
      }
    }
  }

  @Override
  public void postStep(final long spentMachineCyclesForStep) {
    final TapeFileReader currentTap = this.getTap();
    if (currentTap != null) {
      currentTap.updateForSpentMachineCycles(spentMachineCyclesForStep);
    }
  }

  @Override
  public String toString() {
    return this.getName();
  }

}
