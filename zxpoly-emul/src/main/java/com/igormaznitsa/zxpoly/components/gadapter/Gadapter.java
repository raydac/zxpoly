package com.igormaznitsa.zxpoly.components.gadapter;

import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import java.util.concurrent.atomic.AtomicReference;
import net.java.games.input.Component;
import net.java.games.input.Controller;

public abstract class Gadapter implements Runnable {
  protected final KeyboardKempstonAndTapeIn parent;
  private final Controller controller;
  private final GadapterType destination;
  private final AtomicReference<Thread> controllerThread = new AtomicReference<>();

  Gadapter(final KeyboardKempstonAndTapeIn keyboardModule, final Controller controller, final GadapterType destination) {
    this.parent = keyboardModule;
    this.controller = controller;
    this.destination = destination;
  }

  public Controller getController() {
    return this.controller;
  }

  public GadapterType getType() {
    return this.destination;
  }

  public void dispose() {
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

  public void start() {
    final Thread thread = new Thread(this, "zxp.gcontroller." + this.controller.getName());
    thread.setPriority(Thread.NORM_PRIORITY);
    thread.setDaemon(true);
    if (!this.controllerThread.compareAndSet(null, thread)) {
      throw new Error("Detected attempt to restart already started controller!");
    }
    thread.start();
  }

  protected abstract void doLeft();

  protected abstract void doRight();

  protected abstract void doCenterX();

  protected abstract void doUp();

  protected abstract void doDown();

  protected abstract void doCenterY();

  protected abstract void doFire(boolean pressed);

  @Override
  public void run() {
    try {
      while (!Thread.currentThread().isInterrupted() && this.controllerThread.get() != null) {
        if (!this.controller.poll()) {
          break;
        }

        for (final Component c : this.controller.getComponents()) {
          final Component.Identifier identifier = c.getIdentifier();
          final float pollData = c.getPollData();

          final boolean minusOne = Float.compare(pollData, -1.0f) == 0;
          final boolean plusOne = Float.compare(pollData, 1.0f) == 0;

          if (identifier == Component.Identifier.Axis.X) {
            if (minusOne) {
              doLeft();
            } else if (plusOne) {
              doRight();
            } else {
              doCenterX();
            }
          } else if (identifier == Component.Identifier.Axis.Y) {
            if (minusOne) {
              this.doUp();
            } else if (plusOne) {
              this.doDown();
            } else {
              this.doCenterY();
            }
          } else if (identifier == Component.Identifier.Button.THUMB2
              || identifier == Component.Identifier.Button.A
              || identifier == Component.Identifier.Button._2
          ) {
            this.doFire(pollData != 0.0f);
          }
        }

        try {
          Thread.sleep(10);
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
        }
      }
    } finally {
      this.parent.notifyUnregisterGadapter(this);
    }
  }

  @Override
  public String toString() {
    return this.controller.getName();
  }
}
