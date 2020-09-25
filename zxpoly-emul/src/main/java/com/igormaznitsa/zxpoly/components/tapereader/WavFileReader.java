package com.igormaznitsa.zxpoly.components.tapereader;

import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import javax.swing.ListModel;

public class WavFileReader implements TapeSource {

  private volatile boolean playing;

  public WavFileReader(final String name, final File file) throws IOException {

  }

  @Override
  public boolean canGenerateWav() {
    return false;
  }

  @Override
  public boolean getSignal() {
    return false;
  }

  @Override
  public void updateForSpentMachineCycles(long spentTstates) {

  }

  @Override
  public boolean isPlaying() {
    return this.playing;
  }

  @Override
  public void rewindToStart() {

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
  }

  @Override
  public boolean startPlay() {
    this.playing = true;
    return this.playing;
  }

  @Override
  public byte[] getAsWAV() throws IOException {
    return new byte[0];
  }

  @Override
  public void removeActionListener(ActionListener listener) {

  }

  @Override
  public void addActionListener(ActionListener listener) {

  }

  @Override
  public void setCurrent(int selected) {

  }

  @Override
  public int getCurrentBlockIndex() {
    return 0;
  }

  @Override
  public String getName() {
    return null;
  }

  @Override
  public ListModel getBlockListModel() {
    return null;
  }
}
