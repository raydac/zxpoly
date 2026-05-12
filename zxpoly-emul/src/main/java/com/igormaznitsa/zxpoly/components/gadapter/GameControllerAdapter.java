package com.igormaznitsa.zxpoly.components.gadapter;

import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import de.gurkenlabs.input4j.InputComponent;
import de.gurkenlabs.input4j.InputDevice;
import de.gurkenlabs.input4j.components.Axis;
import de.gurkenlabs.input4j.components.XInput;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

public abstract class GameControllerAdapter implements Runnable {

  private static final float STICK_THRESHOLD = 0.4f;
  private static final float DPAD_THRESHOLD = 0.5f;

  protected final KeyboardKempstonAndTapeIn parent;
  private final InputDevice inputDevice;
  private final GameControllerAdapterType destination;
  private final AtomicReference<Thread> controllerThread = new AtomicReference<>();

  GameControllerAdapter(final KeyboardKempstonAndTapeIn keyboardModule,
                        final InputDevice inputDevice,
                        final GameControllerAdapterType destination) {
    this.parent = keyboardModule;
    this.inputDevice = inputDevice;
    this.destination = destination;
  }

  private static float readPrimaryX(final InputDevice device) {
    return device.getComponent(Axis.AXIS_X)
        .map(InputComponent::getData)
        .orElseGet(
            () -> device.getComponent(XInput.LEFT_THUMB_X).map(InputComponent::getData).orElse(0f));
  }

  private static float readPrimaryY(final InputDevice device) {
    return device.getComponent(Axis.AXIS_Y)
        .map(InputComponent::getData)
        .orElseGet(
            () -> device.getComponent(XInput.LEFT_THUMB_Y).map(InputComponent::getData).orElse(0f));
  }

  public final InputDevice getInputDevice() {
    return this.inputDevice;
  }

  public final GameControllerAdapterType getType() {
    return this.destination;
  }

  protected abstract void doLeft();

  protected abstract void doRight();

  protected abstract void doCenterX();

  protected abstract void doUp();

  protected abstract void doDown();

  protected abstract void doCenterY();

  protected abstract void doFire(boolean pressed);

  public final void dispose() {
    final Thread thread = this.controllerThread.getAndSet(null);
    if (thread != null) {
      thread.interrupt();
      try {
        thread.join();
      } catch (final InterruptedException ex) {
        Thread.currentThread().interrupt();
      }
    }
  }

  public final void start() {
    final Thread thread =
        Thread.ofVirtual()
            .name("zxp-gamepad-" + Integer.toHexString(System.identityHashCode(this.inputDevice)))
            .unstarted(this);
    if (!this.controllerThread.compareAndSet(null, thread)) {
      throw new Error("Detected attempt to restart already started controller!");
    }
    thread.start();
  }

  /**
   * Linux evdev device ids are paths such as {@code /dev/input/eventN}. input4j's Linux plugin does not yet refresh
   * on hot-unplug, so polling keeps hitting a dead fd and logs SEVERE on every {@code read} until this thread stops.
   */
  private static boolean isLinuxEvdevDevicePath(final String deviceId) {
    return deviceId != null && deviceId.startsWith("/dev/input/event");
  }

  private void clearEmulatedOutputs() {
    this.doCenterX();
    this.doCenterY();
    this.doFire(false);
  }

  private void applyStickXY(float x, float y) {
    if (x < -STICK_THRESHOLD) {
      this.doLeft();
    } else if (x > STICK_THRESHOLD) {
      this.doRight();
    } else {
      this.doCenterX();
    }
    if (y < -STICK_THRESHOLD) {
      this.doUp();
    } else if (y > STICK_THRESHOLD) {
      this.doDown();
    } else {
      this.doCenterY();
    }
  }

  private void applyDpad(final InputDevice device) {
    final boolean up =
        device.getComponent(XInput.DPAD_UP).map(c -> c.getData() > DPAD_THRESHOLD).orElse(false);
    final boolean down =
        device.getComponent(XInput.DPAD_DOWN).map(c -> c.getData() > DPAD_THRESHOLD).orElse(false);
    final boolean left =
        device.getComponent(XInput.DPAD_LEFT).map(c -> c.getData() > DPAD_THRESHOLD).orElse(false);
    final boolean right =
        device.getComponent(XInput.DPAD_RIGHT).map(c -> c.getData() > DPAD_THRESHOLD).orElse(false);
    if (left) {
      this.doLeft();
    } else if (right) {
      this.doRight();
    } else {
      this.doCenterX();
    }
    if (up) {
      this.doUp();
    } else if (down) {
      this.doDown();
    } else {
      this.doCenterY();
    }
  }

  @Override
  public final void run() {
    try {
      while (!Thread.currentThread().isInterrupted() && this.controllerThread.get() != null) {
        if (isLinuxEvdevDevicePath(this.inputDevice.getID())
            && !Files.exists(Path.of(this.inputDevice.getID()))) {
          this.clearEmulatedOutputs();
          break;
        }
        try {
          this.inputDevice.poll();
        } catch (final RuntimeException ex) {
          this.clearEmulatedOutputs();
          break;
        }
        final float x = readPrimaryX(this.inputDevice);
        final float y = readPrimaryY(this.inputDevice);
        if (Math.abs(x) < STICK_THRESHOLD && Math.abs(y) < STICK_THRESHOLD) {
          this.applyDpad(this.inputDevice);
        } else {
          this.applyStickXY(x, y);
        }
        float buttonAccum = 0.0f;
        boolean buttonDetected = false;
        for (final InputComponent c : this.inputDevice.getComponents()) {
          if (c.isButton()) {
            buttonAccum += Math.abs(c.getData());
            buttonDetected = true;
          }
        }
        if (buttonDetected) {
          this.doFire(buttonAccum != 0.0f);
        } else {
          this.doFire(false);
        }
        try {
          Thread.sleep(10);
        } catch (final InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    } finally {
      this.parent.notifyUnregisterGadapter(this);
    }
  }

  @Override
  public String toString() {
    return this.inputDevice.getDisplayName();
  }
}
