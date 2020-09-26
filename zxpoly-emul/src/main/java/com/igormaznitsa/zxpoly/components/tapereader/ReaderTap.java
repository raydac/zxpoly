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

package com.igormaznitsa.zxpoly.components.tapereader;

import static com.igormaznitsa.jbbp.io.JBBPOut.BeginBin;
import static java.lang.Integer.toHexString;


import com.igormaznitsa.jbbp.io.JBBPBitInputStream;
import com.igormaznitsa.jbbp.io.JBBPByteOrder;
import com.igormaznitsa.jbbp.io.JBBPOut;
import com.igormaznitsa.zxpoly.components.TapFormatParser;
import com.igormaznitsa.zxpoly.components.TapFormatParser.TAPBLOCK;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.ListModel;
import javax.swing.SwingUtilities;
import javax.swing.event.ListDataListener;

final class ReaderTap implements ListModel<ReaderTap.TapBlock>, TapeSource {

  private static final Logger LOGGER = Logger.getLogger("TAP");

  private static final long PULSELEN_PILOT = 2168L;
  private static final long PULSELEN_SYNC1 = 667L;
  private static final long PULSELEN_SYNC2 = 735L;
  private static final long PULSELEN_ZERO = 855L;
  private static final long PULSELEN_SYNC3 = 954L;
  private static final long PULSELEN_ONE = 1710L;
  private static final long IMPULSNUMBER_PILOT_HEADER = 8063L;
  private static final long IMPULSNUMBER_PILOT_DATA = 3223L;
  private static final long PAUSE_BETWEEN = 7000000L; // two sec

  private final List<ActionListener> listeners = new CopyOnWriteArrayList<>();
  private final String name;
  private final List<TapBlock> tapBlockList = new ArrayList<>();
  private volatile TapBlock current;
  private volatile State state = State.STOPPED;
  private volatile boolean signalInState;
  private long counterMain;
  private long counterEx;
  private int mask;
  private int buffered;
  private int controlChecksum;

  public ReaderTap(final String name, final InputStream tap) throws IOException {
    this.name = name;
    final TapFormatParser tapParser = new TapFormatParser().read(new JBBPBitInputStream(tap));
    if (tapParser.getTAPBLOCK().length == 0) {
      this.current = null;
      LOGGER.warning("Can't find blocks in TAP file");
    } else {
      this.current = new TapBlock(tapParser.getTAPBLOCK()[0]);
      this.current.index = 0;
      this.tapBlockList.add(current);
      this.current.prev = null;
      TapBlock item = this.current;
      for (int i = 1; i < tapParser.getTAPBLOCK().length; i++) {
        final TapBlock newitem = new TapBlock(tapParser.getTAPBLOCK()[i]);
        newitem.index = i;
        newitem.prev = item;
        item.next = newitem;
        item = newitem;

        this.tapBlockList.add(item);
      }
      item.next = null;
      LOGGER.log(Level.INFO, "Pointer to " + makeDescription(this.current));
    }
  }

  @Override
  public boolean isNavigable() {
    return true;
  }

  @Override
  public void dispose() {

  }

  public void addActionListener(final ActionListener l) {
    this.listeners.add(l);
  }

  public void removeActionListener(final ActionListener l) {
    this.listeners.remove(l);
  }

  public void fireAction(final int id, final String command) {
    final ActionEvent event = new ActionEvent(this, id, command);
    final Runnable run = () -> this.listeners.forEach(l -> l.actionPerformed(event));
    if (SwingUtilities.isEventDispatchThread()) {
      run.run();
    } else {
      SwingUtilities.invokeLater(run);
    }
  }

  public String getName() {
    return this.name;
  }

  public TapBlock getCurrent() {
    return this.current;
  }

  public void setCurrent(int index) {
    this.rewindToStart();
    while (index > 0) {
      this.rewindToNextBlock();
      index--;
    }
  }

  @Override
  public boolean canGenerateWav() {
    return true;
  }

  @Override
  public synchronized boolean isPlaying() {
    return this.state != State.STOPPED;
  }

  @Override
  public synchronized boolean startPlay() {
    if (this.state != State.STOPPED && this.current == null) {
      this.state = State.STOPPED;
      fireStop();
      return false;
    }

    this.state = State.INBETWEEN;
    this.counterMain = -1L;
    firePlay();
    return true;
  }

  @Override
  public synchronized void stopPlay() {
    this.counterMain = -1L;
    this.state = State.STOPPED;
    fireStop();
  }

  private void fireStop() {
    fireAction(0, "stop");
  }

  private void firePlay() {
    fireAction(1, "play");
  }

  public synchronized void rewindToStart() {
    stopPlay();
    if (this.current != null) {
      while (this.current.prev != null) {
        this.current = this.current.prev;
      }
      LOGGER.log(Level.INFO, "Pointer to " + makeDescription(this.current));
    }
  }

  private boolean toNextBlock() {
    if (this.current == null) {
      return false;
    } else {
      if (this.current.isLast()) {
        return false;
      } else {
        this.current = this.current.next;
        LOGGER.log(Level.INFO, "Pointer to " + makeDescription(this.current));
        return true;
      }
    }
  }

  public synchronized boolean rewindToNextBlock() {
    stopPlay();
    return toNextBlock();
  }

  @Override
  public synchronized boolean rewindToPrevBlock() {
    stopPlay();
    if (this.current == null) {
      return false;
    } else {
      if (!this.current.isFirst()) {
        this.current = this.current.prev;
        LOGGER.log(Level.INFO, "Pointer to " + makeDescription(this.current));
        return true;
      } else {
        return false;
      }
    }
  }

  public synchronized boolean isHi() {
    return this.signalInState;
  }

  public synchronized byte[] getAsWAV() throws IOException {
    final int FREQ = 22050;
    final int CYCLESPERSAMPLE = (int) ((1000000000L / (long) FREQ) / 286L);

    final ByteArrayOutputStream data = new ByteArrayOutputStream(1024 * 1024);

    rewindToStart();
    this.signalInState = false;
    this.counterMain = -1L;
    this.state = State.INBETWEEN;

    while (this.state != State.STOPPED) {
      data.write(this.signalInState ? 0xFF : 0x00);
      updateForSpentMachineCycles(CYCLESPERSAMPLE);
    }

    final JBBPOut out = BeginBin(JBBPByteOrder.LITTLE_ENDIAN);

    return out.
        Byte("RIFF").
        Int(data.size() + 40).
        Byte("WAVE").
        Byte("fmt ").
        Int(16). // Size
        Short(1). // Audio format
        Short(1). // Num channels
        Int(FREQ).// Sample rate
        Int(FREQ). // Byte rate
        Short(1). // Block align
        Short(8). // Bits per sample
        Byte("data").
        Int(data.size()).
        Byte(data.toByteArray()).End().toByteArray();
  }

  private String makeDescription(final TapBlock block) {
    if (block == null) {
      return "No block";
    } else if (block.isHeader() && block.data.length == 17) {
      final StringBuilder buffer = new StringBuilder();
      switch (block.data[0] & 0xFF) {
        case 0:
          buffer.append("BASIC");
          break;
        case 1:
          buffer.append("NUM.ARRAY");
          break;
        case 2:
          buffer.append("CHR.ARRAY");
          break;
        case 3:
          buffer.append("CODE");
          break;
        default:
          buffer.append("UNKNOWN");
          break;
      }

      buffer.append(" \"");

      for (int i = 1; i < 11; i++) {
        buffer.append((char) (block.data[i] & 0xFF));
      }

      buffer.append("\"");

      return buffer.toString();
    } else {
      return "CODE_BLOCK len=#" + toHexString(block.data.length).toUpperCase(Locale.ENGLISH);
    }
  }

  private void loadDataByteToRead(final int data) {
    this.controlChecksum ^= data;
    this.mask = 0x80;
    this.buffered = data;
    this.counterEx = ((data & this.mask) == 0 ? PULSELEN_ZERO : PULSELEN_ONE);
    this.signalInState = !this.signalInState;
  }

  private boolean processDataByte(final long machineCycles) {
    boolean result = false;
    long counter = this.counterEx & 0x8000000000000000L;
    this.counterEx = (this.counterEx & 0x7FFFFFFFFFFFFFFFL) - machineCycles;
    if (this.counterEx <= 0) {
      if (counter != 0) {
        this.mask >>>= 1;
        if (this.mask == 0) {
          result = true;
        } else {
          this.counterEx = (this.buffered & this.mask) == 0 ? PULSELEN_ZERO : PULSELEN_ONE;
          this.signalInState = !this.signalInState;
          counter = 0;
        }
      } else {
        this.counterEx = (this.buffered & this.mask) == 0 ? PULSELEN_ZERO : PULSELEN_ONE;
        counter = 0x8000000000000000L;
        this.signalInState = !this.signalInState;
      }
    }
    this.counterEx |= counter;
    return result;
  }

  @Override
  public int getCurrentBlockIndex() {
    return this.current == null ? -1 : this.current.index;
  }

  @Override
  public ListModel getBlockListModel() {
    return this;
  }

  @Override
  public synchronized void updateForSpentMachineCycles(final long machineCycles) {
    if (this.state != State.STOPPED) {
      final TapBlock block = this.current;

      switch (this.state) {
        case INBETWEEN: {
          if (counterMain < 0) {
            LOGGER.info("PAUSE");
            this.signalInState = false;
            this.counterMain = PAUSE_BETWEEN;
          } else {
            this.counterMain -= machineCycles;
            if (this.counterMain <= 0L) {
              this.state = State.PILOT;
              this.counterMain = -1L;
            }
          }
        }
        break;
        case PILOT: {
          if (this.counterMain < 0L) {
            LOGGER.log(Level.INFO, "PILOT (" + (block.isHeader() ? "header" : "data") + ')');
            this.counterMain =
                block.isHeader() ? IMPULSNUMBER_PILOT_HEADER : IMPULSNUMBER_PILOT_DATA;
            this.signalInState = !this.signalInState;
            this.counterEx = PULSELEN_PILOT;
          } else {
            this.counterEx -= machineCycles;
            if (this.counterEx <= 0) {
              this.counterMain--;
              if (this.counterMain <= 0) {
                this.state = State.SYNC1;
                this.counterMain = -1L;
              } else {
                this.signalInState = !signalInState;
                this.counterEx = PULSELEN_PILOT;
              }
            }
          }
        }
        break;
        case SYNC1: {
          if (this.counterMain < 0L) {
            LOGGER.info("SYNC1");
            this.counterEx = PULSELEN_SYNC1;
            this.signalInState = !signalInState;
            this.counterMain = 0L;
          } else {
            this.counterEx -= machineCycles;
            if (counterEx <= 0) {
              this.counterMain = -1L;
              this.state = State.SYNC2;
            }
          }
        }
        break;
        case SYNC2: {
          if (this.counterMain < 0L) {
            LOGGER.info("SYNC2");
            this.counterEx = PULSELEN_SYNC2;
            this.signalInState = !signalInState;
            this.counterMain = 0L;
          } else {
            this.counterEx -= machineCycles;
            if (counterEx <= 0) {
              this.counterMain = -1L;
              this.state = State.FLAG;
            }
          }
        }
        break;
        case FLAG: {
          if (this.counterMain < 0L) {
            LOGGER.log(Level.INFO,
                "FLAG (#" + toHexString(block.flag & 0xFF).toUpperCase(Locale.ENGLISH) + ')');
            this.controlChecksum = 0;
            this.counterMain = 0L;
            loadDataByteToRead(block.flag & 0xFF);
          } else {
            if (processDataByte(machineCycles)) {
              this.counterMain = -1L;
              this.state = State.DATA;
            }
          }
        }
        break;
        case DATA: {
          if (this.counterMain < 0L) {
            LOGGER.log(Level.INFO, "DATA (len=#"
                + toHexString(block.data.length & 0xFFFF).toUpperCase(Locale.ENGLISH) + ')');
            this.counterMain = 0L;
            loadDataByteToRead(block.data[(int) this.counterMain++]);
          } else {
            if (processDataByte(machineCycles)) {
              if (this.counterMain < block.data.length) {
                loadDataByteToRead(block.data[(int) this.counterMain++]);
              } else {
                this.counterMain = -1;
                this.state = State.CHECKSUM;
              }
            }
          }
        }
        break;
        case CHECKSUM: {
          if (this.counterMain < 0L) {
            LOGGER.log(Level.INFO,
                "CHK (xor=#" + toHexString(block.checksum & 0xFF).toUpperCase(Locale.ENGLISH)
                    + ')');
            if ((block.checksum & 0xFF) != (this.controlChecksum & 0xFF)) {
              LOGGER.log(Level.WARNING, "Different XOR sum : at file #"
                  + toHexString(block.checksum & 0xFF).toUpperCase(Locale.ENGLISH)
                  + ", calculated #"
                  + toHexString(this.controlChecksum & 0xFF).toUpperCase(Locale.ENGLISH));
            }
            this.counterMain = 0L;
            loadDataByteToRead(block.checksum & 0xFF);
          } else {
            if (processDataByte(machineCycles)) {
              this.counterMain = -1L;
              this.state = State.SYNC3;
            }
          }
        }
        break;
        case SYNC3: {
          if (this.counterMain < 0L) {
            LOGGER.info("SYNC3");
            this.counterEx = PULSELEN_SYNC3;
            this.signalInState = !signalInState;
            this.counterMain = 0L;
          } else {
            this.counterEx -= machineCycles;
            if (counterEx <= 0) {
              this.counterMain = -1L;
              if (!this.toNextBlock()) {
                stopPlay();
              } else {
                this.state = State.INBETWEEN;
              }
            }
          }
        }
        break;
        default:
          throw new Error("Unexpected state [" + this.state + ']');
      }
    }
  }


  @Override
  public boolean isThresholdAllowed() {
    return false;
  }

  @Override
  public float getThreshold() {
    return 0.0f;
  }

  @Override
  public void setThreshold(float threshold) {

  }

  @Override
  public int size() {
    return this.tapBlockList.stream().mapToInt(TapBlock::size).sum();
  }

  @Override
  public int getSize() {
    return this.tapBlockList.size();
  }

  @Override
  public TapBlock getElementAt(final int index) {
    return this.tapBlockList.get(index);
  }

  @Override
  public void addListDataListener(final ListDataListener l) {
  }

  @Override
  public void removeListDataListener(ListDataListener l) {
  }

  private enum State {

    STOPPED,
    INBETWEEN,
    PILOT,
    SYNC1,
    SYNC2,
    SYNC3,
    FLAG,
    DATA,
    CHECKSUM
  }

  public static final class TapBlock {

    private static final Charset USASCII = StandardCharsets.US_ASCII;

    byte flag;
    byte[] data;
    byte checksum;

    transient TapBlock prev;
    transient TapBlock next;
    transient int index;
    transient String name;

    public TapBlock(TAPBLOCK block) {
      this.flag = block.getFLAG();
      this.checksum = block.getCHECKSUM();
      this.data = block.getDATA().clone();
    }

    public boolean isFirst() {
      return this.prev == null;
    }

    public boolean isLast() {
      return this.next == null;
    }

    public boolean isHeader() {
      return (this.flag & 0xFF) < 0x80;
    }

    public String getName() {
      if (this.name == null) {
        if (isHeader()) {
          if (this.data.length < 11) {
            this.name = "<NONSTANDARD HEADER LENGTH>";
          } else {
            this.name = new String(this.data, 1, 10, USASCII);
          }
          final String type;
          switch (this.data[0]) {
            case 0:
              type = "BAS";
              break;
            case 1:
              type = "###";
              break;
            case 2:
              type = "TXT";
              break;
            case 3:
              type = "COD";
              break;
            default:
              type = "???";
              break;
          }
          this.name = type + ": " + this.name;
        } else {
          this.name = "===: ..........";
        }
      }
      return this.name;
    }

    public int size() {
      return this.data.length;
    }

    public TapBlock findFirst() {
      TapBlock result = this;
      while (!result.isFirst()) {
        result = result.prev;
      }
      return result;
    }

    public int getIndex() {
      return this.index;
    }

    @Override
    public String toString() {
      return getName() + " (" + this.data.length + " bytes)";
    }
  }

}
