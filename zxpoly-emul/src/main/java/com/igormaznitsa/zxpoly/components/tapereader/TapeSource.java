package com.igormaznitsa.zxpoly.components.tapereader;

import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.ListModel;

public interface TapeSource<T> {

  boolean isHi();

  void updateForSpentMachineCycles(long spentTstates);

  void dispose();

  boolean isPlaying();

  void rewindToStart();

  boolean rewindToNextBlock();

  boolean rewindToPrevBlock();

  boolean isNavigable();

  void stopPlay();

  boolean startPlay();

  byte[] getAsWAV() throws IOException;

  void removeActionListener(ActionListener listener);

  void addActionListener(ActionListener listener);

  void setCurrent(int selected);

  int getCurrentBlockIndex();

  boolean canGenerateWav();

  boolean isThresholdAllowed();

  float getThreshold();

  void setThreshold(float threshold);

  int size();

  String getName();

  ListModel<T> getBlockListModel();
}
