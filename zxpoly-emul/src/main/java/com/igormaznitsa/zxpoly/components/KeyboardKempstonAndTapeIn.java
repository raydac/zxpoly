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

import com.igormaznitsa.zxpoly.components.gadapter.GameControllerAdapter;
import com.igormaznitsa.zxpoly.components.gadapter.GameControllerAdapterInterface2;
import com.igormaznitsa.zxpoly.components.gadapter.GameControllerAdapterKempston;
import com.igormaznitsa.zxpoly.components.gadapter.GameControllerAdapterType;
import com.igormaznitsa.zxpoly.components.tapereader.TapeSource;
import com.igormaznitsa.zxpoly.utils.AppOptions;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEvent;
import net.java.games.input.ControllerListener;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static net.java.games.input.ControllerEnvironment.getDefaultEnvironment;

public final class KeyboardKempstonAndTapeIn implements IoDevice {

  private static final Logger LOGGER = Logger.getLogger("InController");

  public static final long ZXKEY_CS =
          0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001L;
  public static final long ZXKEY_Z =
          0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000010L;
  public static final long ZXKEY_X =
          0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000100L;
  public static final long ZXKEY_C =
          0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00001000L;
  public static final long ZXKEY_V =
          0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00010000L;

  public static final long ZXKEY_A =
          0b00000000_00000000_00000000_00000000_00000000_00000000_00000001_00000000L;
  public static final long ZXKEY_S =
          0b00000000_00000000_00000000_00000000_00000000_00000000_00000010_00000000L;
  public static final long ZXKEY_D =
          0b00000000_00000000_00000000_00000000_00000000_00000000_00000100_00000000L;
  public static final long ZXKEY_F =
          0b00000000_00000000_00000000_00000000_00000000_00000000_00001000_00000000L;
  public static final long ZXKEY_G =
          0b00000000_00000000_00000000_00000000_00000000_00000000_00010000_00000000L;

  public static final long ZXKEY_Q =
          0b00000000_00000000_00000000_00000000_00000000_00000001_00000000_00000000L;
  public static final long ZXKEY_W =
          0b00000000_00000000_00000000_00000000_00000000_00000010_00000000_00000000L;
  public static final long ZXKEY_E =
          0b00000000_00000000_00000000_00000000_00000000_00000100_00000000_00000000L;
  public static final long ZXKEY_R =
          0b00000000_00000000_00000000_00000000_00000000_00001000_00000000_00000000L;
  public static final long ZXKEY_T =
          0b00000000_00000000_00000000_00000000_00000000_00010000_00000000_00000000L;

  public static final long ZXKEY_1 =
          0b00000000_00000000_00000000_00000000_00000001_00000000_00000000_00000000L;
  public static final long ZXKEY_2 =
          0b00000000_00000000_00000000_00000000_00000010_00000000_00000000_00000000L;
  public static final long ZXKEY_3 =
          0b00000000_00000000_00000000_00000000_00000100_00000000_00000000_00000000L;
  public static final long ZXKEY_4 =
          0b00000000_00000000_00000000_00000000_00001000_00000000_00000000_00000000L;
  public static final long ZXKEY_5 =
          0b00000000_00000000_00000000_00000000_00010000_00000000_00000000_00000000L;

  public static final long ZXKEY_0 =
          0b00000000_00000000_00000000_00000001_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_9 =
          0b00000000_00000000_00000000_00000010_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_8 =
          0b00000000_00000000_00000000_00000100_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_7 =
          0b00000000_00000000_00000000_00001000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_6 =
          0b00000000_00000000_00000000_00010000_00000000_00000000_00000000_00000000L;

  public static final long ZXKEY_P =
          0b00000000_00000000_00000001_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_O =
          0b00000000_00000000_00000010_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_I =
          0b00000000_00000000_00000100_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_U =
          0b00000000_00000000_00001000_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_Y =
          0b00000000_00000000_00010000_00000000_00000000_00000000_00000000_00000000L;

  public static final long ZXKEY_EN =
          0b00000000_00000001_00000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_L =
          0b00000000_00000010_00000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_K =
          0b00000000_00000100_00000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_J =
          0b00000000_00001000_00000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_H =
          0b00000000_00010000_00000000_00000000_00000000_00000000_00000000_00000000L;

  public static final long ZXKEY_SP =
          0b00000001_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_SS =
          0b00000010_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_M =
          0b00000100_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_N =
          0b00001000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  public static final long ZXKEY_B =
          0b00010000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;

  public static final long ZXKEY_NONE =
          0b00011111_00011111_00011111_00011111_00011111_00011111_00011111_00011111L;

  private static final int KEMPSTON_RIGHT = 1;
  private static final int KEMPSTON_LEFT = 2;
  private static final int KEMPSTON_DOWN = 4;
  private static final int KEMPSTON_UP = 8;
  private static final int KEMPSTON_FIRE = 16;

  private static final int MIC_BIT = 0b0100_0000;

  private final Motherboard board;
  private final AtomicReference<TapeSource> tap = new AtomicReference<>();
  private final List<Controller> detectedControllers;
  private final List<GameControllerAdapter> activeGameControllerAdapters =
          new CopyOnWriteArrayList<>();
  private volatile long keyboardLines = ZXKEY_NONE;
  private volatile long bufferKeyboardLines = 0L;
  private volatile int kempstonSignals = 0;
  private volatile int kempstonBuffer = 0;
  private final int cursorJoystickVkLeft;

  private final boolean kempstonMouseAllowed;

  private final int kempstonVkLeft;
  private final int kempstonVkRight;
  private final int kempstonVkUp;
  private final int kempstonVkDown;
  private final int kempstonVkFire;
  private final int cursorJoystickVkRight;
  private final int cursorJoystickVkUp;
  private final int cursorJoystickVkDown;
  private final int cursorJoystickVkFire;
  private volatile boolean onlyJoystickEvents = false;

  private final long cursorCsMask = AppOptions.getInstance().getAutoCsForCursorKeys() ? ZXKEY_CS : 0L;
  private volatile boolean activatedKempstonJoystick = true;

  public KeyboardKempstonAndTapeIn(final Motherboard board, final boolean kempstonMouseAllowed) {
    this.board = board;
    this.kempstonMouseAllowed = kempstonMouseAllowed;

    this.kempstonVkLeft = AppOptions.getInstance().getKempstonVkLeft();
    this.kempstonVkRight = AppOptions.getInstance().getKempstonVkRight();
    this.kempstonVkUp = AppOptions.getInstance().getKempstonVkUp();
    this.kempstonVkDown = AppOptions.getInstance().getKempstonVkDown();
    this.kempstonVkFire = AppOptions.getInstance().getKempstonVkFire();

    this.cursorJoystickVkDown = AppOptions.getInstance().getCursorJoystickDown();
    this.cursorJoystickVkFire = AppOptions.getInstance().getProtekJoystickFire();
    this.cursorJoystickVkLeft = AppOptions.getInstance().getProtekJoystickLeft();
    this.cursorJoystickVkRight = AppOptions.getInstance().getProtekJoystickRight();
    this.cursorJoystickVkUp = AppOptions.getInstance().getProtekJoystickUp();

    if (getDefaultEnvironment().isSupported()) {
      this.detectedControllers =
              Arrays.stream(getDefaultEnvironment().getControllers())
                      .filter(x -> isControllerTypeAllowed(x.getType())).collect(Collectors.toCollection(CopyOnWriteArrayList::new));
      getDefaultEnvironment().addControllerListener(new ControllerListener() {
        @Override
        public void controllerRemoved(ControllerEvent controllerEvent) {
          if (isControllerTypeAllowed(controllerEvent.getController().getType())) {
            LOGGER.info("Removed controller: " + controllerEvent.getController().getName());
            detectedControllers.remove(controllerEvent.getController());
          }
        }

        @Override
        public void controllerAdded(ControllerEvent controllerEvent) {
          if (isControllerTypeAllowed(controllerEvent.getController().getType())) {
            LOGGER.info("Added controller: " + controllerEvent.getController().getName());
            detectedControllers.add(controllerEvent.getController());
          }
        }
      });
    } else {
      this.detectedControllers = null;
    }
  }

  public boolean isKempstonJoystickActivated() {
    return this.activatedKempstonJoystick;
  }

  public void setKempstonJoystickActivated(final boolean activated) {
    LOGGER.info("Activated joystick: " + (activated ? "KEMPSTON" : "CURSOR"));
    this.activatedKempstonJoystick = activated;
    this.kempstonSignals = 0;
  }

  public boolean isOnlyJoystickEvents() {
    return this.onlyJoystickEvents;
  }

  public void setOnlyJoystickEvents(final boolean flag) {
    this.onlyJoystickEvents = flag;
    this.keyboardLines = ZXKEY_NONE;
  }

  public void disposeAllActiveGameControllerAdapters() {
    this.activeGameControllerAdapters.forEach(GameControllerAdapter::dispose);
    this.activeGameControllerAdapters.clear();
  }

  public GameControllerAdapter makeGameControllerAdapter(final Controller controller,
                                                         final GameControllerAdapterType type) {
    switch (type) {
      case KEMPSTON:
        return new GameControllerAdapterKempston(this, controller);
      case INTERFACEII_PLAYER1:
        return new GameControllerAdapterInterface2(0, this, controller);
      case INTERFACEII_PLAYER2:
        return new GameControllerAdapterInterface2(1, this, controller);
      default:
        throw new Error("Unexpected destination: " + type);
    }
  }

  public List<GameControllerAdapter> getActiveGadapters() {
    synchronized (this.activeGameControllerAdapters) {
      return new ArrayList<>(this.activeGameControllerAdapters);
    }
  }

  public void setActiveGameControllerAdapters(final List<GameControllerAdapter> adapters) {
    this.activeGameControllerAdapters.forEach(GameControllerAdapter::dispose);
    if (!this.activeGameControllerAdapters.isEmpty()) {
      throw new Error("Detected non-disposed controller");
    }
    adapters.forEach(x -> {
      LOGGER.info("Registering adapter: " + x);
      this.activeGameControllerAdapters.addAll(adapters);
    });
    this.activeGameControllerAdapters.forEach(GameControllerAdapter::start);
  }

  private boolean isControllerTypeAllowed(final Controller.Type type) {
    return type == Controller.Type.FINGERSTICK
            || type == Controller.Type.STICK
            || type == Controller.Type.RUDDER
            || type == Controller.Type.GAMEPAD;
  }

  public List<Controller> getDetectedControllers() {
    return this.detectedControllers == null ? Collections.emptyList() : this.detectedControllers;
  }

  public boolean isControllerEngineAllowed() {
    return this.detectedControllers != null;
  }

  private int getKbdValueForLines(int scanLinePort) {
    final long vkbKeyState = this.board.getVideoController().getVkbState();
    final long state = vkbKeyState & this.bufferKeyboardLines;

    scanLinePort ^= 0xFF;

    int result = 0x1F;
    int shift = 0;
    while (scanLinePort != 0) {
      if ((scanLinePort & 1) != 0) {
        result &= (int) (state >>> shift);
      }
      shift += 8;
      scanLinePort >>= 1;
    }
    return result & 0x1F;
  }

  private int readKeyboardAndTap(final int scanLinePort, final TapeSource tapeFileReader) {
    int result = 0xFF;
    result &= getKbdValueForLines((scanLinePort >>> 8) & 0xFF);
    if (this.isTapeIn()) result ^= MIC_BIT;
    return result | 0b101_00000;
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    int result = -1;
    if (!module.isTrdosActive()) {
      final boolean inZxPolyMode = module.getMotherboard().getBoardMode() == BoardMode.ZXPOLY;

      final int lowPortAddress = port & 0xFF;

      if ((lowPortAddress & 1) == 0) {
        if (!inZxPolyMode || lowPortAddress == 0xFE) {
          result = readKeyboardAndTap(port, this.getTap());
        }
      } else {
        // KEMPSTON JOYSTICK
        if (inZxPolyMode) {
          if (lowPortAddress == 0x1F) { // full decode
            result = this.kempstonBuffer;
          }
        } else {
          if (this.kempstonMouseAllowed) {
            if (lowPortAddress == 0x1F) { // full decode
              result = this.kempstonBuffer;
            }
          } else {
            if ((lowPortAddress & 0b100000) == 0) { // partial decode
              result = this.kempstonBuffer;
            }
          }
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
    this.keyboardLines = ZXKEY_NONE;
    this.kempstonSignals = 0;
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_PRESTEP | NOTIFICATION_POSTSTEP;
  }

  @Override
  public void preStep(final int frameTiStates, final boolean signalReset, final boolean tstatesIntReached,
                      boolean wallclockInt) {
    if (signalReset) {
      doReset();
    }
    this.bufferKeyboardLines = this.keyboardLines;
    this.kempstonBuffer = this.kempstonSignals;
  }

  @Override
  public String getName() {
    return "Keyboard";
  }

  public TapeSource getTap() {
    return this.tap.get();
  }

  public void setTap(final TapeSource tap) {
    this.tap.set(tap);
  }

  public boolean onKeyEvent(final KeyEvent evt) {
    if (evt.isControlDown() && evt.getKeyCode() != KeyEvent.VK_SPACE && !this.onlyJoystickEvents) {
      return false;
    }

    final boolean pressed;
    switch (evt.getID()) {
      case KeyEvent.KEY_PRESSED:
        pressed = true;
        break;
      case KeyEvent.KEY_RELEASED:
        pressed = false;
        break;
      default:
        return false;
    }

    long zxKeyCode = 0L;
    int kempstonCode = 0;

    final int keyCode = evt.getKeyCode();

    final boolean kempsonJoystickActivated = this.activatedKempstonJoystick;

    if (kempsonJoystickActivated) {
      if (keyCode == this.kempstonVkLeft) {
        kempstonCode = KEMPSTON_LEFT;
      } else if (keyCode == this.kempstonVkRight) {
        kempstonCode = KEMPSTON_RIGHT;
      } else if (keyCode == this.kempstonVkUp) {
        kempstonCode = KEMPSTON_UP;
      } else if (keyCode == this.kempstonVkDown) {
        kempstonCode = KEMPSTON_DOWN;
      } else if (keyCode == this.kempstonVkFire) {
        kempstonCode = KEMPSTON_FIRE;
      }
    } else {
      if (keyCode == this.cursorJoystickVkLeft) {
        zxKeyCode = ZXKEY_5;
      } else if (keyCode == this.cursorJoystickVkRight) {
        zxKeyCode = ZXKEY_8;
      } else if (keyCode == this.cursorJoystickVkUp) {
        zxKeyCode = ZXKEY_7;
      } else if (keyCode == this.cursorJoystickVkDown) {
        zxKeyCode = ZXKEY_6;
      } else if (keyCode == this.cursorJoystickVkFire) {
        zxKeyCode = ZXKEY_0;
      }
    }

    if (!this.onlyJoystickEvents || (evt.getModifiersEx() & KeyEvent.CTRL_DOWN_MASK) != 0) {
      switch (keyCode) {
        case KeyEvent.VK_ESCAPE: {
          if (this.board.getVideoController().isMouseTrapActive()) {
            this.board.getVideoController().setTrapMouseActive(false);
          }
        }
        break;
        case KeyEvent.VK_1: {
          zxKeyCode = ZXKEY_1;
        }
        break;
        case KeyEvent.VK_2: {
          zxKeyCode = ZXKEY_2;
        }
        break;
        case KeyEvent.VK_3: {
          zxKeyCode = ZXKEY_3;
        }
        break;
        case KeyEvent.VK_4: {
          zxKeyCode = ZXKEY_4;
        }
        break;
        case KeyEvent.VK_5: {
          zxKeyCode = ZXKEY_5;
        }
        break;
        case KeyEvent.VK_6: {
          zxKeyCode = ZXKEY_6;
        }
        break;
        case KeyEvent.VK_7: {
          zxKeyCode = ZXKEY_7;
        }
        break;
        case KeyEvent.VK_8: {
          zxKeyCode = ZXKEY_8;
        }
        break;
        case KeyEvent.VK_9: {
          zxKeyCode = ZXKEY_9;
        }
        break;
        case KeyEvent.VK_0: {
          zxKeyCode = ZXKEY_0;
        }
        break;
        case KeyEvent.VK_Q: {
          zxKeyCode = ZXKEY_Q;
        }
        break;
        case KeyEvent.VK_W: {
          zxKeyCode = ZXKEY_W;
        }
        break;
        case KeyEvent.VK_E: {
          zxKeyCode = ZXKEY_E;
        }
        break;
        case KeyEvent.VK_R: {
          zxKeyCode = ZXKEY_R;
        }
        break;
        case KeyEvent.VK_T: {
          zxKeyCode = ZXKEY_T;
        }
        break;
        case KeyEvent.VK_Y: {
          zxKeyCode = ZXKEY_Y;
        }
        break;
        case KeyEvent.VK_U: {
          zxKeyCode = ZXKEY_U;
        }
        break;
        case KeyEvent.VK_I: {
          zxKeyCode = ZXKEY_I;
        }
        break;
        case KeyEvent.VK_O: {
          zxKeyCode = ZXKEY_O;
        }
        break;
        case KeyEvent.VK_P: {
          zxKeyCode = ZXKEY_P;
        }
        break;
        case KeyEvent.VK_A: {
          zxKeyCode = ZXKEY_A;
        }
        break;
        case KeyEvent.VK_S: {
          zxKeyCode = ZXKEY_S;
        }
        break;
        case KeyEvent.VK_D: {
          zxKeyCode = ZXKEY_D;
        }
        break;
        case KeyEvent.VK_F: {
          zxKeyCode = ZXKEY_F;
        }
        break;
        case KeyEvent.VK_G: {
          zxKeyCode = ZXKEY_G;
        }
        break;
        case KeyEvent.VK_H: {
          zxKeyCode = ZXKEY_H;
        }
        break;
        case KeyEvent.VK_J: {
          zxKeyCode = ZXKEY_J;
        }
        break;
        case KeyEvent.VK_K: {
          zxKeyCode = ZXKEY_K;
        }
        break;
        case KeyEvent.VK_L: {
          zxKeyCode = ZXKEY_L;
        }
        break;
        case KeyEvent.VK_ENTER: {
          zxKeyCode = ZXKEY_EN;
        }
        break;
        case KeyEvent.VK_Z: {
          zxKeyCode = ZXKEY_Z;
        }
        break;
        case KeyEvent.VK_X: {
          zxKeyCode = ZXKEY_X;
        }
        break;
        case KeyEvent.VK_C: {
          zxKeyCode = ZXKEY_C;
        }
        break;
        case KeyEvent.VK_V: {
          zxKeyCode = ZXKEY_V;
        }
        break;
        case KeyEvent.VK_B: {
          zxKeyCode = ZXKEY_B;
        }
        break;
        case KeyEvent.VK_N: {
          zxKeyCode = ZXKEY_N;
        }
        break;
        case KeyEvent.VK_M: {
          zxKeyCode = ZXKEY_M;
        }
        break;
        case KeyEvent.VK_SPACE: {
          zxKeyCode = ZXKEY_SP;
        }
        break;
        case KeyEvent.VK_SHIFT: {
          zxKeyCode = ZXKEY_CS;
        }
        break;
        case KeyEvent.VK_ALT: {
          zxKeyCode = ZXKEY_SS;
        }
        break;
        case KeyEvent.VK_BACK_SPACE: {
          zxKeyCode = ZXKEY_CS | ZXKEY_0;
        }
        break;
        case KeyEvent.VK_LEFT: {
          if (kempsonJoystickActivated) {
            zxKeyCode = this.cursorCsMask | ZXKEY_5;
          }
        }
        break;
        case KeyEvent.VK_RIGHT: {
          if (kempsonJoystickActivated) {
            zxKeyCode = this.cursorCsMask | ZXKEY_8;
          }
        }
        break;
        case KeyEvent.VK_UP: {
          if (kempsonJoystickActivated) {
            zxKeyCode = this.cursorCsMask | ZXKEY_7;
          }
        }
        break;
        case KeyEvent.VK_DOWN: {
          if (kempsonJoystickActivated) {
            zxKeyCode = this.cursorCsMask | ZXKEY_6;
          }
        }
        break;
        case KeyEvent.VK_COMMA: {
          zxKeyCode = ZXKEY_SS | ZXKEY_N;
        }
        break;
        case KeyEvent.VK_PERIOD: {
          zxKeyCode = ZXKEY_SS | ZXKEY_M;
        }
        break;
        case KeyEvent.VK_EQUALS: {
          zxKeyCode = ZXKEY_SS | ZXKEY_L;
        }
        break;
        case KeyEvent.VK_SLASH: {
          zxKeyCode = ZXKEY_SS | ZXKEY_V;
        }
        break;
        case KeyEvent.VK_QUOTE: {
          zxKeyCode = ZXKEY_SS | ZXKEY_7;
        }
        break;
        case KeyEvent.VK_MINUS: {
          zxKeyCode = ZXKEY_SS | ZXKEY_J;
        }
        break;
      }
    }

    boolean consumed = false;

    if (zxKeyCode != 0L) {
      long theLinesState = this.keyboardLines;
      if (pressed) {
        theLinesState &= ZXKEY_NONE ^ zxKeyCode;
      } else {
        theLinesState |= ZXKEY_NONE & zxKeyCode;
      }
      this.keyboardLines = theLinesState;
      consumed = true;
    }

    if (kempstonCode != 0) {
      int theSignal = this.kempstonSignals;
      if (pressed) {
        theSignal |= kempstonCode;
      } else {
        theSignal = (~kempstonCode & theSignal) & 0xFF;
      }
      this.kempstonSignals = theSignal;
      consumed = true;
    }
    return consumed;
  }

  @Override
  public void postStep(final int spentTstates) {
    final TapeSource currentTap = this.getTap();
    if (currentTap != null) {
      currentTap.updateForSpentMachineCycles(spentTstates);
    }
  }

  @Override
  public String toString() {
    return this.getName();
  }

  public boolean isTapeIn() {
    final TapeSource reader = this.tap.get();
    if (reader == null) {
      return false;
    } else {
      return reader.isHi();
    }
  }

  public void doKempstonCenterX() {
    int state = this.kempstonSignals;
    state = state
            & ~(KeyboardKempstonAndTapeIn.KEMPSTON_RIGHT | KeyboardKempstonAndTapeIn.KEMPSTON_LEFT);
    this.kempstonSignals = state;
  }

  public void doKempstonCenterY() {
    int state = this.kempstonSignals;
    state =
            state & ~(KeyboardKempstonAndTapeIn.KEMPSTON_UP | KeyboardKempstonAndTapeIn.KEMPSTON_DOWN);
    this.kempstonSignals = state;
  }

  public void doKempstonLeft() {
    int state = this.kempstonSignals;
    state = KeyboardKempstonAndTapeIn.KEMPSTON_LEFT |
            (state & ~KeyboardKempstonAndTapeIn.KEMPSTON_RIGHT);
    this.kempstonSignals = state;
  }

  public void doKempstonUp() {
    int state = this.kempstonSignals;
    state =
            KeyboardKempstonAndTapeIn.KEMPSTON_UP | (state & ~KeyboardKempstonAndTapeIn.KEMPSTON_DOWN);
    this.kempstonSignals = state;
  }

  public void doKempstonRight() {
    int state = this.kempstonSignals;
    state = KeyboardKempstonAndTapeIn.KEMPSTON_RIGHT |
            (state & ~KeyboardKempstonAndTapeIn.KEMPSTON_LEFT);
    this.kempstonSignals = state;
  }

  public void doKempstonDown() {
    int state = this.kempstonSignals;
    state =
            KeyboardKempstonAndTapeIn.KEMPSTON_DOWN | (state & ~KeyboardKempstonAndTapeIn.KEMPSTON_UP);
    this.kempstonSignals = state;
  }

  public void doKempstonFire(final boolean pressed) {
    int state = this.kempstonSignals;
    if (pressed) {
      state |= KeyboardKempstonAndTapeIn.KEMPSTON_FIRE;
    } else {
      state = state & ~KeyboardKempstonAndTapeIn.KEMPSTON_FIRE;
    }
    this.kempstonSignals = state;
  }

  public void doInterface2Fire(final int player, final boolean pressed) {
    long state = this.keyboardLines;
    if (player == 0) {
      if (pressed) {
        state &= ZXKEY_NONE ^ ZXKEY_5;
      } else {
        state |= ZXKEY_NONE & ZXKEY_5;
      }
    } else {
      if (pressed) {
        state &= ZXKEY_NONE ^ ZXKEY_0;
      } else {
        state |= ZXKEY_NONE & ZXKEY_0;
      }
    }
    this.keyboardLines = state;
  }

  public void doInterface2Down(final int player) {
    long state = this.keyboardLines;
    if (player == 0) {
      state = (state | (ZXKEY_NONE & ZXKEY_4)) & (ZXKEY_NONE ^ ZXKEY_3);
    } else {
      state = (state | (ZXKEY_NONE & ZXKEY_9)) & (ZXKEY_NONE ^ ZXKEY_8);
    }
    this.keyboardLines = state;
  }

  public void doInterface2Up(final int player) {
    long state = this.keyboardLines;
    if (player == 0) {
      state = (state | (ZXKEY_NONE & ZXKEY_3)) & (ZXKEY_NONE ^ ZXKEY_4);
    } else {
      state = (state | (ZXKEY_NONE & ZXKEY_8)) & (ZXKEY_NONE ^ ZXKEY_9);
    }
    this.keyboardLines = state;
  }

  public void doInterface2Left(final int player) {
    long state = this.keyboardLines;
    if (player == 0) {
      state = (state | (ZXKEY_NONE & ZXKEY_2)) & (ZXKEY_NONE ^ ZXKEY_1);
    } else {
      state = (state | (ZXKEY_NONE & ZXKEY_7)) & (ZXKEY_NONE ^ ZXKEY_6);
    }
    this.keyboardLines = state;
  }

  public void doInterface2Right(final int player) {
    long state = this.keyboardLines;
    if (player == 0) {
      state = (state | (ZXKEY_NONE & ZXKEY_1)) & (ZXKEY_NONE ^ ZXKEY_2);
    } else {
      state = (state | (ZXKEY_NONE & ZXKEY_6)) & (ZXKEY_NONE ^ ZXKEY_7);
    }
    this.keyboardLines = state;
  }

  public void doInterface2CenterX(final int player) {
    long state = this.keyboardLines;
    if (player == 0) {
      state |= ZXKEY_NONE & (ZXKEY_1 | ZXKEY_2);
    } else {
      state |= ZXKEY_NONE & (ZXKEY_6 | ZXKEY_7);
    }
    this.keyboardLines = state;
  }

  public void doInterface2CenterY(final int player) {
    long state = this.keyboardLines;
    if (player == 0) {
      state |= ZXKEY_NONE & (ZXKEY_3 | ZXKEY_4);
    } else {
      state |= ZXKEY_NONE & (ZXKEY_8 | ZXKEY_9);
    }
    this.keyboardLines = state;
  }

  public void notifyUnregisterGadapter(final GameControllerAdapter adapter) {
    LOGGER.info("Unregistering adapter: " + adapter);
    this.activeGameControllerAdapters.remove(adapter);
  }

  public long getKeyState() {
    return this.keyboardLines;
  }
}
