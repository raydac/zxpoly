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

import static net.java.games.input.ControllerEnvironment.getDefaultEnvironment;


import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.java.games.input.Component;
import net.java.games.input.Controller;
import net.java.games.input.ControllerEvent;
import net.java.games.input.ControllerListener;

public final class KeyboardKempstonAndTapeIn implements IoDevice {

  private static final Logger LOGGER = Logger.getLogger("InController");

  private static final long ZXKEY_CS = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000001L;
  private static final long ZXKEY_Z = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000010L;
  private static final long ZXKEY_X = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00000100L;
  private static final long ZXKEY_C = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00001000L;
  private static final long ZXKEY_V = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000000_00010000L;

  private static final long ZXKEY_A = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000001_00000000L;
  private static final long ZXKEY_S = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000010_00000000L;
  private static final long ZXKEY_D = 0b00000000_00000000_00000000_00000000_00000000_00000000_00000100_00000000L;
  private static final long ZXKEY_F = 0b00000000_00000000_00000000_00000000_00000000_00000000_00001000_00000000L;
  private static final long ZXKEY_G = 0b00000000_00000000_00000000_00000000_00000000_00000000_00010000_00000000L;

  private static final long ZXKEY_Q = 0b00000000_00000000_00000000_00000000_00000000_00000001_00000000_00000000L;
  private static final long ZXKEY_W = 0b00000000_00000000_00000000_00000000_00000000_00000010_00000000_00000000L;
  private static final long ZXKEY_E = 0b00000000_00000000_00000000_00000000_00000000_00000100_00000000_00000000L;
  private static final long ZXKEY_R = 0b00000000_00000000_00000000_00000000_00000000_00001000_00000000_00000000L;
  private static final long ZXKEY_T = 0b00000000_00000000_00000000_00000000_00000000_00010000_00000000_00000000L;

  private static final long ZXKEY_1 = 0b00000000_00000000_00000000_00000000_00000001_00000000_00000000_00000000L;
  private static final long ZXKEY_2 = 0b00000000_00000000_00000000_00000000_00000010_00000000_00000000_00000000L;
  private static final long ZXKEY_3 = 0b00000000_00000000_00000000_00000000_00000100_00000000_00000000_00000000L;
  private static final long ZXKEY_4 = 0b00000000_00000000_00000000_00000000_00001000_00000000_00000000_00000000L;
  private static final long ZXKEY_5 = 0b00000000_00000000_00000000_00000000_00010000_00000000_00000000_00000000L;

  private static final long ZXKEY_0 = 0b00000000_00000000_00000000_00000001_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_9 = 0b00000000_00000000_00000000_00000010_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_8 = 0b00000000_00000000_00000000_00000100_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_7 = 0b00000000_00000000_00000000_00001000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_6 = 0b00000000_00000000_00000000_00010000_00000000_00000000_00000000_00000000L;

  private static final long ZXKEY_P = 0b00000000_00000000_00000001_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_O = 0b00000000_00000000_00000010_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_I = 0b00000000_00000000_00000100_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_U = 0b00000000_00000000_00001000_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_Y = 0b00000000_00000000_00010000_00000000_00000000_00000000_00000000_00000000L;

  private static final long ZXKEY_EN = 0b00000000_00000001_00000000_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_L = 0b00000000_00000010_00000000_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_K = 0b00000000_00000100_00000000_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_J = 0b00000000_00001000_00000000_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_H = 0b00000000_00010000_00000000_00000000_00000000_00000000_00000000_00000000L;

  private static final long ZXKEY_SP = 0b00000001_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_SS = 0b00000010_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_M = 0b00000100_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_N = 0b00001000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;
  private static final long ZXKEY_B = 0b00010000_00000000_00000000_00000000_00000000_00000000_00000000_00000000L;

  private static final long ZXKEY_NONE = 0b00011111_00011111_00011111_00011111_00011111_00011111_00011111_00011111L;

  private static final int KEMPSTON_RIGHT = 1;
  private static final int KEMPSTON_LEFT = 2;
  private static final int KEMPSTON_DOWN = 4;
  private static final int KEMPSTON_UP = 8;
  private static final int KEMPSTON_FIRE = 16;

  private static final int TAP_BIT = 0b01000000;

  private final Motherboard board;

  private volatile long keyboardLines = 0L;
  private volatile long bufferKeyboardLines = 0L;
  private volatile int kempstonSignals = 0;
  private volatile int kempstonBuffer = 0;
  private final AtomicReference<TapeFileReader> tap = new AtomicReference<>();

  private final List<Controller> controllers;
  private final List<ControllerProcessor> activeControllerProcessors = new CopyOnWriteArrayList<>();

  public KeyboardKempstonAndTapeIn(final Motherboard board) {
    this.board = board;

    if (getDefaultEnvironment().isSupported()) {
      this.controllers = new CopyOnWriteArrayList<>(Arrays.stream(getDefaultEnvironment().getControllers())
          .filter(x -> isControllerTypeAllowed(x.getType()))
          .collect(Collectors.toList()));
      getDefaultEnvironment().addControllerListener(new ControllerListener() {
        @Override
        public void controllerRemoved(ControllerEvent controllerEvent) {
          if (isControllerTypeAllowed(controllerEvent.getController().getType())) {
            LOGGER.info("Removed controller: " + controllerEvent.getController().getName());
            controllers.remove(controllerEvent.getController());
          }
        }

        @Override
        public void controllerAdded(ControllerEvent controllerEvent) {
          if (isControllerTypeAllowed(controllerEvent.getController().getType())) {
            LOGGER.info("Added controller: " + controllerEvent.getController().getName());
            controllers.add(controllerEvent.getController());
          }
        }
      });
    } else {
      this.controllers = null;
    }
  }

  public void disposeAllControllerProcessors() {
    synchronized (this.activeControllerProcessors) {
      this.activeControllerProcessors.forEach(ControllerProcessor::dispose);
      this.activeControllerProcessors.clear();
    }
  }

  public ControllerProcessor makeControllerProcessor(final Controller controller, final ControllerDestination destination) {
    return new ControllerProcessor(this, controller, destination);
  }

  public List<ControllerProcessor> getActiveControllerProcessors() {
    synchronized (this.activeControllerProcessors) {
      return new ArrayList<>(this.activeControllerProcessors);
    }
  }

  public void setActiveControllerProcessors(final List<ControllerProcessor> activeControllerProcessors) {
    synchronized (this.activeControllerProcessors) {
      this.activeControllerProcessors.forEach(ControllerProcessor::dispose);
      this.activeControllerProcessors.clear();
      this.activeControllerProcessors.addAll(activeControllerProcessors);
      this.activeControllerProcessors.forEach(ControllerProcessor::start);
    }
  }

  private boolean isControllerTypeAllowed(final Controller.Type type) {
    return type == Controller.Type.FINGERSTICK
        || type == Controller.Type.STICK
        || type == Controller.Type.RUDDER
        || type == Controller.Type.GAMEPAD;
  }

  public List<Controller> getControllers() {
    return this.controllers == null ? Collections.emptyList() : this.controllers;
  }

  public boolean isControllerEngineAllowed() {
    return this.controllers != null;
  }

  public enum ControllerDestination {
    NONE(""),
    KEMPSTON("Kempston"),
    INTERFACEII_PLAYER1("InterfaceII Player1"),
    INTERFACEII_PLAYER2("InterfaceII Player2");

    private final String description;

    ControllerDestination(final String description) {
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

  public class ControllerProcessor implements Runnable {
    private final KeyboardKempstonAndTapeIn parent;
    private final Controller controller;
    private final ControllerDestination destination;
    private final AtomicReference<Thread> controllerThread = new AtomicReference<>();

    private ControllerProcessor(final KeyboardKempstonAndTapeIn keyboardModule, final Controller controller, final ControllerDestination destination) {
      this.parent = keyboardModule;
      this.controller = controller;
      this.destination = destination;
    }

    public Controller getController() {
      return this.controller;
    }

    public ControllerDestination getDestination() {
      return this.destination;
    }

    void dispose() {
      final Thread thread = this.controllerThread.getAndSet(null);
      if (thread != null) {
        thread.interrupt();
        try {
          thread.join();
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    }

    void start() {
      final Thread thread = new Thread(this, "zxpoly-controller-" + this.controller.getName());
      thread.setDaemon(true);
      thread.start();
    }

    @Override
    public void run() {
      while (!Thread.currentThread().isInterrupted()) {
        if (!this.controller.poll()) {
          LOGGER.warning("Can't poll data from controller: " + this);
          break;
        }

        for (final Component c : this.controller.getComponents()) {
          final Component.Identifier identifier = c.getIdentifier();
          if (identifier == Component.Identifier.Axis.X) {
            final float value = c.getPollData();
            if (value < 0.0f) {
              kempstonSignals = KEMPSTON_LEFT | (kempstonSignals & ~KEMPSTON_RIGHT);
            } else if (value > 0.0f) {
              kempstonSignals = KEMPSTON_RIGHT | (kempstonSignals & ~KEMPSTON_LEFT);
            } else {
              kempstonSignals = kempstonSignals & ~(KEMPSTON_LEFT | KEMPSTON_RIGHT);
            }
          } else if (identifier == Component.Identifier.Axis.Y) {
            final float value = c.getPollData();
            if (value < 0.0f) {
              kempstonSignals = KEMPSTON_UP | (kempstonSignals & ~KEMPSTON_DOWN);
            } else if (value > 0.0f) {
              kempstonSignals = KEMPSTON_DOWN | (kempstonSignals & ~KEMPSTON_UP);
            } else {
              kempstonSignals = kempstonSignals & ~(KEMPSTON_DOWN | KEMPSTON_UP);
            }
          } else if (identifier == Component.Identifier.Button.THUMB2) {
            final float value = c.getPollData();
            if (value == 0.0f) {
              kempstonSignals = kempstonSignals & ~KEMPSTON_FIRE;
            } else {
              kempstonSignals |= KEMPSTON_FIRE;
            }
          }
        }
        Thread.yield();
      }
      activeControllerProcessors.remove(this);
    }

    @Override
    public String toString() {
      return this.controller.getName();
    }
  }

  private int getKbdValueForLines(int highPortByte) {
    final long state = this.bufferKeyboardLines;
    int result = 0xFF;
    for (int i = 0; i < 8; i++) {
      if ((highPortByte & 1) == 0) {
        result &= (int) (state >>> (i * 8));
      }
      highPortByte >>= 1;
    }
    return result;
  }

  private int readKeyboardAndTap(final int port, final TapeFileReader tapeFileReader) {
    final int tapbit = tapeFileReader == null ? 0 : tapeFileReader.getSignal() ? TAP_BIT : 0;
    return getKbdValueForLines(port >>> 8) | tapbit;
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    int result = -1;
    if (!module.isTrdosActive()) {
      final boolean zxPolyMode = module.getMotherboard().getBoardMode() == BoardMode.ZXPOLY;

      final int lowerPortByte = port & 0xFF;

      if ((lowerPortByte & 1) == 0) {
        if (zxPolyMode) {
          if (lowerPortByte == 0xFE) {
            result = readKeyboardAndTap(port, this.getTap());
          }
        } else {
          result = readKeyboardAndTap(port, this.getTap());
        }
      } else {
        if ((zxPolyMode && lowerPortByte == 0x1F)
            || (!zxPolyMode && (lowerPortByte & 0b100000) == 0)) {// KEMPSTON
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
    this.keyboardLines = ZXKEY_NONE;
    this.kempstonSignals = 0;
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
    this.bufferKeyboardLines = this.keyboardLines;
    this.kempstonBuffer = this.kempstonSignals;
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

  public boolean onKeyEvent(final KeyEvent evt) {
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

    switch (evt.getKeyCode()) {
      case KeyEvent.VK_ESCAPE: {
        if (this.board.getVideoController().isHoldMouse()) {
          this.board.getVideoController().setHoldMouse(false);
        }
      }
      break;
      case KeyEvent.VK_KP_LEFT:
      case KeyEvent.VK_NUMPAD4: {
        kempstonCode = KEMPSTON_LEFT;
      }
      break;
      case KeyEvent.VK_KP_UP:
      case KeyEvent.VK_NUMPAD8: {
        kempstonCode = KEMPSTON_UP;
      }
      break;
      case KeyEvent.VK_KP_RIGHT:
      case KeyEvent.VK_NUMPAD6: {
        kempstonCode = KEMPSTON_RIGHT;
      }
      break;
      case KeyEvent.VK_KP_DOWN:
      case KeyEvent.VK_NUMPAD2: {
        kempstonCode = KEMPSTON_DOWN;
      }
      break;
      case 65368: // NUMPAD 5 
      case KeyEvent.VK_NUMPAD5: {
        kempstonCode = KEMPSTON_FIRE;
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
        zxKeyCode = ZXKEY_CS | ZXKEY_5;
      }
      break;
      case KeyEvent.VK_RIGHT: {
        zxKeyCode = ZXKEY_CS | ZXKEY_8;
      }
      break;
      case KeyEvent.VK_UP: {
        zxKeyCode = ZXKEY_CS | ZXKEY_7;
      }
      break;
      case KeyEvent.VK_DOWN: {
        zxKeyCode = ZXKEY_CS | ZXKEY_6;
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

  public boolean isTapeIn() {
    final TapeFileReader reader = this.tap.get();
    return reader != null && reader.getSignal();
  }
}
