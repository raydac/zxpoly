package com.igormaznitsa.zxpoly.components.tapereader;

import com.igormaznitsa.zxpoly.components.tapereader.wave.FileSeekableContainer;
import com.igormaznitsa.zxpoly.components.tapereader.wave.InMemoryWavFile;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

public class ReaderWav implements TapeSource {

  private final String name;
  private final InMemoryWavFile wavFile;
  private final AtomicLong tstateCounter = new AtomicLong(0L);
  private final List<ActionListener> actionListeners = new CopyOnWriteArrayList<>();
  private final TimingProfile timingProfile;
  private final TapeContext tapeContext;
  private volatile boolean playing;
  private volatile float bias = 0.01f;

  public ReaderWav(final TapeContext tapeContext, final TimingProfile timingProfile, final String name, final File file) throws IOException {
    this.tapeContext = tapeContext;
    this.timingProfile = timingProfile;
    this.name = name;
    try (final FileSeekableContainer container = new FileSeekableContainer(new RandomAccessFile(file, "r"))) {
      this.wavFile = new InMemoryWavFile(container, timingProfile.ulaFrameTact * 50L);
    }
  }

  @Override
  public void dispose() {
    this.stopPlay();
    this.wavFile.dispose();
    this.actionListeners.clear();
  }

  @Override
  public boolean canGenerateWav() {
    return false;
  }

  @Override
  public boolean isHi() {
    if (this.playing) {
      try {
        final float value = this.wavFile.readAtPosition(this.tstateCounter.get());
        return value > this.bias;
      } catch (ArrayIndexOutOfBoundsException ex) {
        this.stopPlay();
        return false;
      }
    } else {
      return false;
    }
  }

  @Override
  public boolean isThresholdAllowed() {
    return true;
  }

  @Override
  public float getThreshold() {
    return this.bias;
  }

  @Override
  public void setThreshold(float threshold) {
    this.bias = Math.max(0.0f, Math.min(threshold, 1.0f));
  }

  @Override
  public int size() {
    return this.wavFile.size();
  }

  private void fireActionListeners(final int id, final String command) {
    SwingUtilities.invokeLater(() -> this.actionListeners.forEach(x -> x.actionPerformed(new ActionEvent(this, id, command))));
  }

  @Override
  public void updateForSpentMachineCycles(final long spentTstates) {
    if (this.playing) {
      this.tstateCounter.addAndGet(spentTstates);
    }
  }

  @Override
  public boolean isPlaying() {
    return this.playing;
  }

  @Override
  public void rewindToStart() {
    this.stopPlay();
    this.tstateCounter.set(0L);
  }

  @Override
  public boolean rewindToNextBlock() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean rewindToPrevBlock() {
    throw new UnsupportedOperationException();
  }

  @Override
  public boolean isNavigable() {
    return false;
  }

  @Override
  public void stopPlay() {
    this.playing = false;
    this.fireActionListeners(0, "stop");
  }

  @Override
  public boolean startPlay() {
    this.playing = true;
    this.fireActionListeners(1, "play");
    return this.playing;
  }

  @Override
  public byte[] getAsWAV() throws IOException {
    throw new UnsupportedOperationException();
  }

  @Override
  public void removeActionListener(final ActionListener listener) {
    this.actionListeners.remove(listener);
  }

  @Override
  public void addActionListener(final ActionListener listener) {
    this.actionListeners.add(listener);
  }

  @Override
  public void setCurrent(int index) {
    throw new UnsupportedOperationException();
  }

  @Override
  public int getCurrentBlockIndex() {
    return -1;
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public ListModel getBlockListModel() {
    throw new UnsupportedOperationException();
  }
}
