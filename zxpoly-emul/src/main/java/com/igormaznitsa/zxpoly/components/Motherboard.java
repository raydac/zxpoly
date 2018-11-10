/*
 * Copyright (C) 2014 Raydac Research Group Ltd.
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
package com.igormaznitsa.zxpoly.components;

import com.igormaznitsa.zxpoly.components.betadisk.BetaDiscInterface;
import com.igormaznitsa.z80.Utils;
import com.igormaznitsa.z80.Z80;
import java.util.*;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Motherboard implements ZXPoly {

  private static final int NUMBER_OF_INT_BETWEEN_STATISTIC_UPDATE = 4;
  public static final long CPU_FREQ = 3540000L;

  public static final int TRIGGER_NONE = 0;
  public static final int TRIGGER_DIFF_MODULESTATES = 1;
  public static final int TRIGGER_DIFF_MEM_ADDR = 2;
  public static final int TRIGGER_DIFF_EXE_CODE = 4;

  private static final Logger LOG = Logger.getLogger("MB");

  private final ZXPolyModule[] modules;
  private final IODevice[] ioDevices;
  private final AtomicIntegerArray ram = new AtomicIntegerArray(512 * 1024);
  private final VideoController video;
  private final KeyboardKempstonAndTapeIn keyboard;
  private final BetaDiscInterface betaDisk;
  private final RomData rom;

  private volatile int port3D00 = (int) System.nanoTime() & 0xFF; // simulate noise after turning on

  private volatile boolean totalReset;
  private volatile int resetCounter;
  private int triggerMemAddress = 0;

  private int triggers = TRIGGER_NONE;

  private int intCounter;
  private volatile boolean videoFlashState;

  private boolean localResetForAllModules;

  private volatile boolean modeZXPoly = true;

  private int statisticCounter = NUMBER_OF_INT_BETWEEN_STATISTIC_UPDATE;

  private final float[] cpuLoad = new float[4];

  public Motherboard(final RomData rom) {
    if (rom == null) {
      throw new NullPointerException("ROM must not be null");
    }
    this.rom = rom;
    this.modules = new ZXPolyModule[4];
    final List<IODevice> iodevices = new ArrayList<>();
    for (int i = 0; i < this.modules.length; i++) {
      this.modules[i] = new ZXPolyModule(this, i);
      iodevices.add(this.modules[i]);
    }

    this.betaDisk = new BetaDiscInterface(this);

    iodevices.add(this.betaDisk);

    this.keyboard = new KeyboardKempstonAndTapeIn(this);
    iodevices.add(keyboard);
    this.video = new VideoController(this);
    iodevices.add(video);
    iodevices.add(new KempstonMouse(this));
    this.ioDevices = iodevices.toArray(new IODevice[iodevices.size()]);

    // simulation of garbage in memory after power on
    final Random rnd = new Random();
    for (int i = 0; i < this.ram.length(); i++) {
      this.ram.set(i, rnd.nextInt());
    }
  }

  public synchronized void forceResetCPUs() {
    for (final ZXPolyModule p : this.modules) {
      p.getCPU().doReset();
    }
  }

  public void reset() {
    LOG.info("Full system reset");
    this.totalReset = true;
    this.resetCounter = 3;
  }

  public boolean set3D00(final int value, final boolean force) {
    if (is3D00NotLocked() || force) {
      this.port3D00 = value;
      LOG.log(Level.INFO, "set #3D00 to {0}", Utils.toHex(value));

      if ((value & PORTw_ZXPOLY_RESET) != 0) {
        for (final ZXPolyModule m : this.modules) {
          m.prepareLocalReset();
        }
        this.localResetForAllModules = true;
      }

      this.video.setVideoMode((this.port3D00 >> 2) & 0x7);
      return true;
    } else {
      LOG.info("Rejected new value for #3D00 because it is locked");
      return false;
    }
  }

  public int findFirstModuleMemoryDiffOffset() {
    for (int i = 0; i < 0x20000; i++) {
      final int m0 = this.modules[0].readHeapModuleMemory(i);
      final int m1 = this.modules[1].readHeapModuleMemory(i);
      final int m2 = this.modules[2].readHeapModuleMemory(i);
      final int m3 = this.modules[3].readHeapModuleMemory(i);
      if (!(m0 == m1 && m0 == m2 && m0 == m3)) {
        return i;
      }
    }
    return -1;
  }

  public float getActivityCPU0() {
    return this.cpuLoad[0];
  }

  public float getActivityCPU1() {
    return this.cpuLoad[1];
  }

  public float getActivityCPU2() {
    return this.cpuLoad[2];
  }

  public float getActivityCPU3() {
    return this.cpuLoad[3];
  }

  public BetaDiscInterface getBetaDiskInterface() {
    return this.betaDisk;
  }

  public int get3D00() {
    return this.port3D00;
  }

  public int getMappedCPUIndex() {
    return (this.port3D00 >>> 5) & 3;
  }

  public boolean is3D00NotLocked() {
    return (this.port3D00 & PORTw_ZXPOLY_BLOCK) == 0;
  }

  public boolean isCPUModules123InWaitMode() {
    return (this.port3D00 & PORTw_ZXPOLY_nWAIT) == 0;
  }

  public boolean isFlashActive() {
    return this.videoFlashState;
  }

  public int readROM(final int address) {
    return rom.readAdress(address);
  }

  public Z80 getCPU0() {
    return this.modules[0].getCPU();
  }

  public int getTriggers() {
    return this.triggers;
  }

  public void setTrigger(final int flag) {
    this.triggers |= flag;
  }

  public void resetTrigger(final int flag) {
    this.triggers &= ~flag;
  }

  public void setMemTriggerAddress(final int address) {
    this.triggerMemAddress = address;
  }

  public int getMemTriggerAddress() {
    return this.triggerMemAddress;
  }

  public int step(final boolean signalInt, final boolean processStep) {
    this.localResetForAllModules = false;

    final boolean resetStatisticsAtModules;

    int result = TRIGGER_NONE;

    if (signalInt) {
      this.statisticCounter--;
      if (this.statisticCounter <= 0) {
        for (int i = 0; i < 4; i++) {
          this.cpuLoad[i] = Math.min(1.0f, (float) (this.modules[i].getActiveMCyclesBetweeInt() / NUMBER_OF_INT_BETWEEN_STATISTIC_UPDATE) / (float) (VideoController.CYCLES_BETWEEN_INT));
        }
        this.statisticCounter = NUMBER_OF_INT_BETWEEN_STATISTIC_UPDATE;
        resetStatisticsAtModules = true;
      } else {
        resetStatisticsAtModules = false;
      }

      this.intCounter++;
      if (this.intCounter >= 25) {
        this.intCounter = 0;
        this.videoFlashState = !this.videoFlashState;
      }
    } else {
      resetStatisticsAtModules = false;
    }

    if (processStep) {
      final boolean signalReset = this.totalReset;
      this.totalReset = false;

      if (this.resetCounter > 0) {
        this.resetCounter--;
        if (this.resetCounter == 0) {
          this.set3D00(0, true);
        }
      }

      for (final IODevice d : this.ioDevices) {
        d.preStep(signalReset, signalInt);
      }

      final long initialMachineCycleCounter = this.modules[0].getCPU().getMachineCycles();

      if (isZXPolyMode()) {

        final boolean zx0halt;
        final boolean zx1halt;
        final boolean zx2halt;
        final boolean zx3halt;

        // process modules in pseudo-random order to simulate some parallelism and racing
        switch ((int) System.nanoTime() & 0x3) {
          case 0: {
            zx0halt = this.modules[0].step(signalReset, signalInt, resetStatisticsAtModules);
            if (localResetForAllModules) {
              return result;
            }
            zx3halt = this.modules[3].step(signalReset, signalInt, resetStatisticsAtModules);
            zx2halt = this.modules[2].step(signalReset, signalInt, resetStatisticsAtModules);
            zx1halt = this.modules[1].step(signalReset, signalInt, resetStatisticsAtModules);
          }
          break;
          case 1: {
            zx1halt = this.modules[1].step(signalReset, signalInt, resetStatisticsAtModules);
            zx2halt = this.modules[2].step(signalReset, signalInt, resetStatisticsAtModules);
            zx0halt = this.modules[0].step(signalReset, signalInt, resetStatisticsAtModules);
            if (this.localResetForAllModules) {
              return result;
            }
            zx3halt = this.modules[3].step(signalReset, signalInt, resetStatisticsAtModules);
          }
          break;
          case 2: {
            zx3halt = this.modules[3].step(signalReset, signalInt, resetStatisticsAtModules);
            zx0halt = this.modules[0].step(signalReset, signalInt, resetStatisticsAtModules);
            if (this.localResetForAllModules) {
              return result;
            }
            zx1halt = this.modules[1].step(signalReset, signalInt, resetStatisticsAtModules);
            zx2halt = this.modules[2].step(signalReset, signalInt, resetStatisticsAtModules);
          }
          break;
          case 3: {
            zx2halt = this.modules[2].step(signalReset, signalInt, resetStatisticsAtModules);
            zx3halt = this.modules[3].step(signalReset, signalInt, resetStatisticsAtModules);
            zx1halt = this.modules[1].step(signalReset, signalInt, resetStatisticsAtModules);
            zx0halt = this.modules[0].step(signalReset, signalInt, resetStatisticsAtModules);
            if (this.localResetForAllModules) {
              return result;
            }
          }
          break;
          default:
            throw new Error("Unexpected combination number");
        }

        if (is3D00NotLocked() && (zx0halt || zx1halt || zx2halt || zx3halt)) {
          // a cpu has met halt and we need process notification
          if (zx0halt) {
            processHaltNotificationForModule(0);
          }
          if (zx1halt) {
            processHaltNotificationForModule(1);
          }
          if (zx2halt) {
            processHaltNotificationForModule(2);
          }
          if (zx3halt) {
            processHaltNotificationForModule(3);
          }
        }
      } else {
        // ZX 128 mode
        this.modules[0].step(signalReset, signalInt, resetStatisticsAtModules);
      }

      final long spentMachineCycles = this.modules[0].getCPU().getMachineCycles() - initialMachineCycleCounter;

      for (final IODevice d : this.ioDevices) {
        d.postStep(spentMachineCycles);
      }

      final int cur_triggers = this.triggers;

      if (cur_triggers != TRIGGER_NONE) {
        if ((cur_triggers & TRIGGER_DIFF_MODULESTATES) != 0 && !areZXPolyModuleStateEquals()) {
          this.triggers = this.triggers & ~TRIGGER_DIFF_MODULESTATES;
          result |= TRIGGER_DIFF_MODULESTATES;
        }
        if ((cur_triggers & TRIGGER_DIFF_MEM_ADDR) != 0) {
          int val = this.modules[0].readAddress(this.triggerMemAddress);
          for (int i = 1; i < 4; i++) {
            if (val != this.modules[i].readAddress(this.triggerMemAddress)) {
              result |= TRIGGER_DIFF_MEM_ADDR;
              this.triggers = this.triggers & ~TRIGGER_DIFF_MEM_ADDR;
              break;
            }
          }
        }
        
        if ((cur_triggers & TRIGGER_DIFF_EXE_CODE) != 0) {
          final int m1ExeByte = this.modules[0].getCPU().getLastM1InstructionByte();
          final int exeByte = this.modules[0].getCPU().getLastInstructionByte();
          
          for (int i = 1; i < 4; i++) {
            if (m1ExeByte != this.modules[i].getCPU().getLastM1InstructionByte() || exeByte != this.modules[i].getCPU().getLastInstructionByte()) {
              result |= TRIGGER_DIFF_EXE_CODE;
              this.triggers = this.triggers & ~TRIGGER_DIFF_EXE_CODE;
              break;
            }
          }
        }
      }
    }
    return result;
  }

  private boolean areZXPolyModuleStateEquals() {
    final int pc = this.modules[0].getCPU().getRegister(Z80.REG_PC);
    final int sp = this.modules[0].getCPU().getRegister(Z80.REG_SP);
    final int im = this.modules[0].getCPU().getIM();
    final boolean iff1 = this.modules[0].getCPU().isIFF1();
    final boolean iff2 = this.modules[0].getCPU().isIFF2();

    boolean result = true;

    for (int i = 1; result && i < 4; i++) {
      result = pc == this.modules[i].getCPU().getRegister(Z80.REG_PC)
          && sp == this.modules[i].getCPU().getRegister(Z80.REG_SP)
          && im == this.modules[i].getCPU().getIM()
          && iff1 == this.modules[0].getCPU().isIFF1()
          && iff2 == this.modules[0].getCPU().isIFF2();

    }
    return result;
  }

  private void processHaltNotificationForModule(final int index) {
    final ZXPolyModule module = this.modules[index];
    final int reg1 = module.getReg1WrittenData();
    final boolean sendInt = (reg1 & ZXPOLY_wREG1_HALT_NOTIFY_INT) != 0;
    final boolean sendNmi = (reg1 & ZXPOLY_wREG1_HALT_NOTIFY_NMI) != 0;
    if (sendInt) {
      if ((reg1 & ZXPOLY_wREG1_CPU0) != 0) {
        this.modules[0].prepareLocalInt();
      }
      if ((reg1 & ZXPOLY_wREG1_CPU1) != 0) {
        this.modules[1].prepareLocalInt();
      }
      if ((reg1 & ZXPOLY_wREG1_CPU2) != 0) {
        this.modules[2].prepareLocalInt();
      }
      if ((reg1 & ZXPOLY_wREG1_CPU3) != 0) {
        this.modules[3].prepareLocalInt();
      }
    }

    if (sendNmi) {
      if ((reg1 & ZXPOLY_wREG1_CPU0) != 0) {
        this.modules[0].prepareLocalNMI();
      }
      if ((reg1 & ZXPOLY_wREG1_CPU1) != 0) {
        this.modules[1].prepareLocalNMI();
      }
      if ((reg1 & ZXPOLY_wREG1_CPU2) != 0) {
        this.modules[2].prepareLocalNMI();
      }
      if ((reg1 & ZXPOLY_wREG1_CPU3) != 0) {
        this.modules[3].prepareLocalNMI();
      }
    }
  }

  public void setZXPolyMode(final boolean flag) {
    if (this.modeZXPoly != flag) {
      LOG.log(Level.INFO, "Changed motherboard mode to {0}", flag ? "ZX-POLY" : "ZX128");
      this.modeZXPoly = flag;
      this.reset();
    }
  }

  public boolean isZXPolyMode() {
    return this.modeZXPoly;
  }

  public ZXPolyModule[] getZXPolyModules() {
    return this.modules;
  }

  public VideoController getVideoController() {
    return this.video;
  }

  public int readRAM(final ZXPolyModule module, final int address) {
    return this.ram.get(address);
  }

  public void writeRAM(final ZXPolyModule module, final int heapAddress, final int value) {
    this.ram.set(heapAddress, value);
  }

  public int readBusIO(final ZXPolyModule module, final int port) {
    final int mappedCPU = getMappedCPUIndex();
    int result = 0;

    if (isZXPolyMode() && (module.getModuleIndex() == 0 && mappedCPU > 0)) {
      final ZXPolyModule destmodule = modules[mappedCPU];
      result = this.ram.get(destmodule.ramOffset2HeapAddress(port));
      destmodule.prepareLocalInt();
    } else {
      IODevice firstDetected = null;

      for (final IODevice d : this.ioDevices) {
        final int prevResult = result;
        result |= d.readIO(module, port);
        if (prevResult != result) {
          // changed
          if (prevResult == 0) {
            firstDetected = d;
          } else {
            LOG.log(Level.WARNING, "Detected collision during IO reading: {0}, {1} port #{2}", new Object[]{firstDetected, d.getName(), Integer.toHexString(port).toUpperCase(Locale.ENGLISH)});
          }
        }
      }
    }
    return result;
  }

  public void writeBusIO(final ZXPolyModule module, final int port, final int value) {
    final int mappedCPU = getMappedCPUIndex();
    final int moduleIndex = module.getModuleIndex();

    if (isZXPolyMode()) {
      if (moduleIndex == 0) {
        if (port == PORTrw_ZXPOLY) {
          set3D00(value, false);
        } else {
          if (mappedCPU > 0) {
            final ZXPolyModule destmodule = modules[mappedCPU];
            this.ram.set(destmodule.ramOffset2HeapAddress(port), value);
            destmodule.prepareLocalNMI();
          } else {
            for (final IODevice d : this.ioDevices) {
              d.writeIO(module, port, value);
            }
          }
        }
      } else {
        for (final IODevice d : this.ioDevices) {
          d.writeIO(module, port, value);
        }
      }
    } else {
      for (final IODevice d : this.ioDevices) {
        d.writeIO(module, port, value);
      }
    }
  }

  public <T> T findIODevice(final Class<T> klazz) {
    T result = null;
    for (final IODevice d : this.ioDevices) {
      if (klazz.isInstance(d)) {
        result = klazz.cast(d);
      }
    }
    return result;
  }

  public synchronized void resetIODevices() {
    for (final IODevice d : this.ioDevices) {
      d.doReset();
    }
  }
}
