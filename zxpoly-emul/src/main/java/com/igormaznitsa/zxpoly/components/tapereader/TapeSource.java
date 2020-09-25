package com.igormaznitsa.zxpoly.components.tapereader;

import java.awt.event.ActionListener;
import java.io.IOException;
import javax.swing.ListModel;

public interface TapeSource {
  boolean getSignal();

  void updateForSpentMachineCycles(long spentTstates);

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

  String getName();

  ListModel getBlockListModel();
}
