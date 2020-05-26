package com.igormaznitsa.zxpoly.streamer;

import java.util.ArrayList;
import java.util.List;

public final class PreemptiveBuffer {

  private final List<byte[]> list;
  private final int max;
  private volatile boolean started = false;

  public PreemptiveBuffer(final int max) {
    this.max = max;
    this.list = new ArrayList<>(max);
  }

  public boolean isStarted() {
    return this.started;
  }

  public void start() {
    this.started = true;
  }

  public void suspend() {
    this.started = false;
  }

  public int size() {
    synchronized (this.list) {
      return this.list.size();
    }
  }

  public void clear() {
    synchronized (this.list) {
      this.list.clear();
    }
  }

  public byte[] next() {
    synchronized (this.list) {
      return this.list.isEmpty() ? null : this.list.remove(0);
    }
  }

  public void put(final byte[] data) {
    synchronized (this.list) {
      if (this.started) {
        if (this.list.size() == this.max) {
          this.list.set(this.max - 1, data);
        } else {
          this.list.add(data);
        }
      } else {
        if (this.list.size() == this.max) {
          this.list.remove(0);
        }
        this.list.add(data);
      }
    }
  }
}
