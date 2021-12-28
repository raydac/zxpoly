package com.igormaznitsa.zxpoly.components.tapereader;

import com.igormaznitsa.zxpoly.components.tapereader.tzx.TzxFile;
import com.igormaznitsa.zxpoly.components.tapereader.tzx.TzxWavRenderer;
import com.igormaznitsa.zxpoly.components.tapereader.wave.ByteArraySeekableContainer;
import com.igormaznitsa.zxpoly.components.tapereader.wave.InMemoryWavFile;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;

import javax.swing.*;
import javax.swing.event.ListDataListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ReaderTzx implements TapeSource, ListModel<TzxWavRenderer.RenderResult.NamedOffsets> {

  private static final Logger LOGGER = Logger.getLogger("TZX");

  private final List<ActionListener> actionListeners = new CopyOnWriteArrayList<>();
  private final AtomicLong tStateCounter = new AtomicLong(0L);
  private final TzxFile tzxFile;
  private final String name;
  private final TimingProfile timingProfile;
  private final InMemoryWavFile inMemoryWavFile;
  private final TzxWavRenderer.RenderResult renderedWav;
  private final AtomicInteger blockIndex = new AtomicInteger(0);
  private final TapeContext tapeContext;
  private final TzxWavRenderer.DataType SIGNAL_STOP_TAPE = TzxWavRenderer.DataType.STOP_TAPE;
  private final TzxWavRenderer.DataType SIGNAL_STOP_TAPE_IF_ZX48 = TzxWavRenderer.DataType.STOP_TAPE_IF_ZX48;
  private volatile boolean playing;
  private volatile float bias = 0.01f;

  public ReaderTzx(final TapeContext context, final TimingProfile timingProfile, final String name, final InputStream tap) throws IOException {
    this.tapeContext = context;
    this.timingProfile = timingProfile;
    this.name = name;
    this.tzxFile = new TzxFile(tap);

    this.tzxFile.getBlockList()
            .forEach(x -> LOGGER.info("Found block: " + x.getClass().getSimpleName()));

    final long startTime = System.currentTimeMillis();
    this.renderedWav = this.renderAsWav();

    LOGGER.info(String.format("TZX to WAV conversion took %d ms, size %d bytes", (System.currentTimeMillis() - startTime), this.renderedWav.getWavData().length));
    this.inMemoryWavFile = new InMemoryWavFile(new ByteArraySeekableContainer(this.renderedWav.getWavData()), timingProfile.ulaFrameTact * 50L);
  }

  @Override
  public void dispose() {
    this.stopPlay();
    this.actionListeners.clear();
  }

  @Override
  public boolean canGenerateWav() {
    return true;
  }

  @Override
  public int getSize() {
    return this.renderedWav.getNamedOffsets().size();
  }

  @Override
  public TzxWavRenderer.RenderResult.NamedOffsets getElementAt(final int index) {
    return this.renderedWav.getNamedOffsets().get(index);
  }

  @Override
  public void addListDataListener(final ListDataListener l) {

  }

  @Override
  public void removeListDataListener(final ListDataListener l) {

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
    return this.inMemoryWavFile.size();
  }

  private void fireActionListeners(final int id, final String command) {
    SwingUtilities.invokeLater(() -> this.actionListeners.forEach(x -> x.actionPerformed(new ActionEvent(this, id, command))));
  }

  @Override
  public void updateForSpentMachineCycles(final long spentTstates) {
    if (this.playing) {
      this.tStateCounter.addAndGet(spentTstates);
      final int wavFilePosition = (int) this.inMemoryWavFile.calcPositionInWavFile(this.tStateCounter.get());
      if (wavFilePosition < this.renderedWav.getControlData().length) {
        final int controlCode = this.renderedWav.getControlData()[wavFilePosition];
        boolean rewindUntilControlChange = false;

        if (controlCode == SIGNAL_STOP_TAPE.ordinal()) {
          LOGGER.info("Sending signal 'stop tape'");
          this.tapeContext.onTapeSignal(this, TapeContext.ControlSignal.STOP_TAPE);
          rewindUntilControlChange = true;
        } else if (controlCode == SIGNAL_STOP_TAPE_IF_ZX48.ordinal()) {
          LOGGER.info("Sending signal 'stop tape if zx48'");
          this.tapeContext.onTapeSignal(this, TapeContext.ControlSignal.STOP_TAPE_IF_ZX48);
          rewindUntilControlChange = true;
        }
        if (rewindUntilControlChange) {
          for (; ; ) {
            final int position = (int) this.inMemoryWavFile.calcPositionInWavFile(this.tStateCounter.addAndGet(4L));
            if (position < this.renderedWav.getControlData().length) {
              if (controlCode != this.renderedWav.getControlData()[position]) {
                break;
              }
            } else {
              this.playing = false;
              break;
            }
          }
        }
      }
    }
  }

  @Override
  public boolean isPlaying() {
    return this.playing;
  }

  @Override
  public void rewindToStart() {
    this.stopPlay();
    this.tStateCounter.set(0L);
  }

  @Override
  public boolean isHi() {
    if (this.playing) {
      try {
        final float value = this.inMemoryWavFile.readAtPosition(this.tStateCounter.get());
        return value > this.bias;
      } catch (final ArrayIndexOutOfBoundsException ex) {
        this.stopPlay();
        return false;
      }
    } else {
      return false;
    }
  }

  private boolean toNextBlock() {
    if (this.blockIndex.get() == this.renderedWav.getNamedOffsets().size() - 1) {
      return false;
    } else {
      this.blockIndex.incrementAndGet();
      LOGGER.log(Level.INFO, "Pointer to " + this.getElementAt(this.blockIndex.get()));
      return true;
    }
  }

  @Override
  public synchronized boolean rewindToNextBlock() {
    this.stopPlay();
    return toNextBlock();
  }

  @Override
  public synchronized boolean rewindToPrevBlock() {
    this.stopPlay();
    if (this.blockIndex.get() > 0) {
      this.blockIndex.decrementAndGet();
      LOGGER.log(Level.INFO, "Pointer to " + this.getElementAt(this.blockIndex.get()));
      return true;
    } else {
      return false;
    }
  }


  @Override
  public boolean isNavigable() {
    return true;
  }

  @Override
  public void stopPlay() {
    this.playing = false;
    this.fireActionListeners(0, "stop");
  }

  @Override
  public boolean startPlay() {
    final long offset = this.inMemoryWavFile.calcPositionInWavData(this.tStateCounter.get());
    LOGGER.info("Starting play from offset in WAV data: " + offset + " bytes");

    this.playing = true;
    this.fireActionListeners(1, "play");

    return this.playing;
  }

  private TzxWavRenderer.RenderResult renderAsWav() throws IOException {
    return new TzxWavRenderer(TzxWavRenderer.Freq.FREQ_44100, this.tzxFile, LOGGER).render();
  }

  @Override
  public byte[] getAsWAV() throws IOException {
    return this.renderedWav.getWavData();
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
  public synchronized void setCurrent(final int index) {
    this.stopPlay();
    if (index < 0) {
      this.tStateCounter.set(0L);
    } else {
      final TzxWavRenderer.RenderResult.NamedOffsets offset = this.renderedWav.getNamedOffsets().get(index);
      LOGGER.info("Tape pointer to " + offset);
      this.tStateCounter.set(this.inMemoryWavFile.findTstatesForWavDataPosition(offset.getOffsetInWav()));
    }
  }

  @Override
  public int getCurrentBlockIndex() {
    final long wavFilePosition = TzxWavRenderer.WAV_HEADER_LENGTH + this.inMemoryWavFile.calcPositionInWavData(this.tStateCounter.get());
    return (int) this.renderedWav.getNamedOffsets().stream().filter(x -> x.getOffsetInWav() < wavFilePosition).count();
  }

  @Override
  public String getName() {
    return this.name;
  }

  @Override
  public ListModel getBlockListModel() {
    return this;
  }

}
