package com.igormaznitsa.zxpoly.components.tapereader;

import com.igormaznitsa.zxpoly.components.tapereader.tzx.AbstractTzxBlock;
import com.igormaznitsa.zxpoly.components.tapereader.tzx.DataBlock;
import com.igormaznitsa.zxpoly.components.tapereader.tzx.TzxFile;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ReaderTzx implements ListModel<ReaderTzx.TzxBlockRef>, TapeSource {

  private final List<TzxBlockRef> navigableBlocks;
  private final TzxFile tzxFile;
  private final String name;
  private final TimingProfile timingProfile;
  private volatile TapeContext tapeContext;

  public ReaderTzx(final TimingProfile timingProfile, final String name, final InputStream tap) throws IOException {
    this.timingProfile = timingProfile;
    this.name = name;
    this.tzxFile = new TzxFile(tap);

    final List<TzxBlockRef> navigableList = new ArrayList<>();
    for (int i = 0; i < this.tzxFile.getBlockList().size(); i++) {
      final AbstractTzxBlock block = this.tzxFile.getBlockList().get(i);
      if (block instanceof DataBlock) {
        navigableList.add(new TzxBlockRef(block, i));
      }
    }

    this.navigableBlocks = Collections.unmodifiableList(navigableList);
  }

  @Override
  public void setTapeContext(final TapeContext context) {
    this.tapeContext = context;
  }

  @Override
  public boolean isHi() {
    return false;
  }

  @Override
  public void updateForSpentMachineCycles(final long spentTstates) {

  }

  @Override
  public void dispose() {

  }

  @Override
  public boolean isPlaying() {
    return false;
  }

  @Override
  public void rewindToStart() {

  }

  @Override
  public boolean rewindToNextBlock() {
    return false;
  }

  @Override
  public boolean rewindToPrevBlock() {
    return false;
  }

  @Override
  public boolean isNavigable() {
    return true;
  }

  @Override
  public void stopPlay() {

  }

  @Override
  public boolean startPlay() {
    return false;
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
  public void setCurrent(final int selected) {

  }

  @Override
  public int getCurrentBlockIndex() {
    return 0;
  }

  @Override
  public boolean canGenerateWav() {
    return false;
  }

  @Override
  public boolean isThresholdAllowed() {
    return false;
  }

  @Override
  public float getThreshold() {
    return 0;
  }

  @Override
  public void setThreshold(float threshold) {

  }

  @Override
  public int size() {
    return this.tzxFile.stream()
            .filter(x -> x instanceof DataBlock).mapToInt(b -> ((DataBlock) b).getDataLength()).sum();
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public ListModel<?> getBlockListModel() {
    return this;
  }

  @Override
  public int getSize() {
    return this.navigableBlocks.size();
  }

  @Override
  public TzxBlockRef getElementAt(final int index) {
    return this.navigableBlocks.get(index);
  }

  @Override
  public void addListDataListener(ListDataListener l) {

  }

  @Override
  public void removeListDataListener(ListDataListener l) {

  }

  public static final class TzxBlockRef {
    private final AbstractTzxBlock tzxBlock;
    private final int blockIndex;

    private TzxBlockRef(final AbstractTzxBlock tzxBlock, final int blockIndex) {
      this.blockIndex = blockIndex;
      this.tzxBlock = tzxBlock;
    }

    @Override
    public String toString() {
      return "Block #" + blockIndex;
    }
  }
}
