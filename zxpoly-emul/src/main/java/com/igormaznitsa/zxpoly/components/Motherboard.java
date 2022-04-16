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

package com.igormaznitsa.zxpoly.components;

import com.igormaznitsa.z80.Utils;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.components.betadisk.BetaDiscInterface;
import com.igormaznitsa.zxpoly.components.snd.*;
import com.igormaznitsa.zxpoly.components.video.VideoController;
import com.igormaznitsa.zxpoly.components.video.VirtualKeyboardDecoration;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.igormaznitsa.zxpoly.components.snd.Beeper.CHANNEL_BEEPER;
import static com.igormaznitsa.zxpoly.components.video.timings.TimingProfile.UlaTact.*;
import static java.lang.Math.min;

@SuppressWarnings({"unused", "NonAtomicOperationOnVolatileField"})
public final class Motherboard implements ZxPolyConstants {

  public static final int TRIGGER_NONE = 0;
  public static final int TRIGGER_DIFF_MODULESTATES = 1;
  public static final int TRIGGER_DIFF_MEM_ADDR = 2;
  public static final int TRIGGER_DIFF_EXE_CODE = 4;
  private static final int NUMBER_OF_INT_BETWEEN_STATISTIC_UPDATE = 4;
  private static final Logger LOGGER = Logger.getLogger("MB");

  private static final int SPEC256_GFX_CORES = 8;
  private static final int NUMBER_OF_MODULES = 4;
  private final ZxPolyModule[] modules;
  private final Z80[] spec256GfxCores;
  private final IoDevice[] ioDevices;
  private final IoDevice[] ioDevicesPreStep;
  private final IoDevice[] ioDevicesPostStep;
  private final byte[] ram = new byte[512 * 1024];
  private final VideoController video;
  private final KeyboardKempstonAndTapeIn keyboard;
  private final BetaDiscInterface betaDisk;
  private final float[] cpuLoad = new float[4];
  private final Beeper beeper;
  private final TimingProfile timingProfile;
  private final VolumeProfile soundLevels;
  private final int[] audioLevels;
  private final TimingProfile.UlaTact[] memoryTimings;
  private final boolean attributePortFf;
  private volatile int port3D00 = (int) System.nanoTime() & 0xFF; // simulate noise after turning on
  private volatile boolean totalReset;
  private volatile int resetCounter;
  private int triggerMemAddress = 0;
  private int triggers = TRIGGER_NONE;
  private int intCounter;
  private volatile boolean videoFlashState;
  private boolean localResetForAllModules;
  private volatile BoardMode boardMode;
  private int statisticCounter = NUMBER_OF_INT_BETWEEN_STATISTIC_UPDATE;
  private volatile int gfxSyncRegsRecord = 0;
  private volatile boolean gfxLeveledXor = false;
  private volatile boolean gfxLeveledOr = false;
  private volatile boolean gfxLeveledAnd = false;
  private int frameTiStatesCounter = 0;
  private boolean frameIntTriggered;

  public Motherboard(
          final VolumeProfile soundLevels,
          final TimingProfile timingProfile,
          final RomData rom,
          final BoardMode boardMode,
          final boolean syncRepaint,
          final boolean useAcbSoundScheme,
          final boolean enableCovoxFb,
          final boolean useTurboSound,
          final boolean allowKempstonMouse,
          final boolean attributePortFf,
          final VirtualKeyboardDecoration vkbdContainer
  ) {
    this.attributePortFf = attributePortFf;
    this.soundLevels = soundLevels;
    this.audioLevels = this.soundLevels.getLevels();
    this.timingProfile = timingProfile;
    this.modules = new ZxPolyModule[NUMBER_OF_MODULES];
    final List<IoDevice> ioDevices = new ArrayList<>();
    for (int i = 0; i < NUMBER_OF_MODULES; i++) {
      this.modules[i] = new ZxPolyModule(timingProfile, this, Objects.requireNonNull(rom, "ROM must not be null"), i);
      ioDevices.add(this.modules[i]);
    }

    this.memoryTimings = timingProfile.makeUlaFrame();

    this.boardMode = boardMode;
    this.beeper = new Beeper(timingProfile, useAcbSoundScheme, enableCovoxFb, useTurboSound);
    if (rom.isTrdosPresented()) {
      LOGGER.info("TR-DOS presented in ROM, creating BetaDiskInterface");
      this.betaDisk = new BetaDiscInterface(this.timingProfile, this);
      ioDevices.add(this.betaDisk);
    } else {
      LOGGER.warning("TR-DOS is not presented in ROM, BetaDiskInterface disabled");
      this.betaDisk = null;
    }

    this.keyboard = new KeyboardKempstonAndTapeIn(timingProfile, this, allowKempstonMouse);
    ioDevices.add(keyboard);
    this.video = new VideoController(timingProfile, syncRepaint, this, vkbdContainer);
    ioDevices.add(video);
    ioDevices.add(new KempstonMouse(this));

    if (useTurboSound) {
      LOGGER.info("TurboSound activated as AY");
      ioDevices.add(new TurboSoundNedoPc(this));
    } else {
      ioDevices.add(new Zx128Ay8910(this));
    }

    if (enableCovoxFb) {
      LOGGER.info("Covox #FB is enabled and added among IO devices");
      ioDevices.add(new CovoxFb(this));
    }

    this.ioDevices = ioDevices.toArray(new IoDevice[0]);
    this.ioDevicesPreStep = Arrays.stream(this.ioDevices)
            .filter(x -> (x.getNotificationFlags() & IoDevice.NOTIFICATION_PRESTEP) != 0)
            .toArray(IoDevice[]::new);
    this.ioDevicesPostStep = Arrays.stream(this.ioDevices)
            .filter(x -> (x.getNotificationFlags() & IoDevice.NOTIFICATION_POSTSTEP) != 0)
            .toArray(IoDevice[]::new);

    // simulation of garbage in memory after power on
    for (int i = 0; i < this.ram.length; i++) {
      Random rnd = new Random();
      this._writeRam(i, rnd.nextInt());
    }

    this.spec256GfxCores = new Z80[SPEC256_GFX_CORES];
    for (int i = 0; i < SPEC256_GFX_CORES; i++) {
      this.spec256GfxCores[i] = new Z80(this.modules[0].getCpu());
    }
  }

  private static boolean isContended(final int address, final int port7FFD) {
    final int pageStart = address & 0xC000;
    return pageStart == 0x4000 || (pageStart == 0xC000 && (port7FFD & 1) != 0);
  }

  private static boolean isUlaPort(final int port) {
    return (port & 1) == 0;
  }

  public boolean isBetaDiskPresented() {
    return this.betaDisk != null;
  }

  public VolumeProfile getSoundLevels() {
    return this.soundLevels;
  }

  public boolean isGfxLeveledXor() {
    return this.gfxLeveledXor;
  }

  public boolean isGfxLeveledOr() {
    return this.gfxLeveledOr;
  }

  public boolean isGfxLeveledAnd() {
    return this.gfxLeveledAnd;
  }

  public void setGfxLeveledLogicalOps(final boolean xor, final boolean or, final boolean and) {
    LOGGER.info(String.format("Set GFX leveled logic ops XOR=%b, OR=%b, AND=%b", xor, or, and));
    this.gfxLeveledXor = xor;
    this.gfxLeveledOr = or;
    this.gfxLeveledAnd = and;
  }

  public Beeper getBeeper() {
    return this.beeper;
  }

  public synchronized void forceResetAllCpu() {
    for (final ZxPolyModule p : this.modules) {
      p.getCpu().doReset();
    }
  }

  private void _writeRam(final int address, final int value) {
    this.ram[address] = (byte) value;
  }

  private int _readRam(final int address) {
    return this.ram[address] & 0xFF;
  }

  public void reset() {
    LOGGER.info("Full system reset");
    this.beeper.reset();
    this.totalReset = true;
    this.resetCounter = 3;
    this.setGfxLeveledLogicalOps(false, false, false);
  }

  public void resetAndRestoreRom(final RomData rom) {
    LOGGER.info("Restoring ROM");
    for (final ZxPolyModule module : this.modules) {
      module.setRomData(rom);
    }
    LOGGER.info("Full system reset");
    this.totalReset = true;
    this.resetCounter = 3;
  }

  public void set3D00(final int value, final boolean force) {
    if (isNotLockedPort3D00() || force) {
      this.port3D00 = value;
      LOGGER.log(Level.INFO, "set #3D00 to " + Utils.toHex(value));

      if ((value & PORTw_ZXPOLY_RESET) != 0) {
        for (final ZxPolyModule m : this.modules) {
          m.prepareLocalReset();
        }
        this.localResetForAllModules = true;
      }

      this.video.setVideoMode((this.port3D00 >> 2) & 0x7);
    } else {
      LOGGER.info("Rejected new value for #3D00 because it is locked");
    }
  }

  public int findFirstDiffAddrInModuleMemory() {
    int addr = -1;
    for (int i = 0; i < 0x20000; i++) {
      final int m0 = this.modules[0].readHeap(i);
      final int m1 = this.modules[1].readHeap(i);
      final int m2 = this.modules[2].readHeap(i);
      final int m3 = this.modules[3].readHeap(i);

      final int summ = m0 + m1 + m2 + m3;
      if ((m0 << 2) != summ) {
        addr = i;
        break;
      }
    }
    return addr;
  }

  public float getCpuActivity(final int cpuIndex) {
    return this.cpuLoad[cpuIndex];
  }

  public BetaDiscInterface getBetaDiskInterface() {
    return this.betaDisk;
  }

  public int get3D00() {
    return this.port3D00;
  }

  public int getMappedCpuIndex() {
    return (this.port3D00 >>> 5) & 3;
  }

  public boolean isNotLockedPort3D00() {
    return (this.port3D00 & PORTw_ZXPOLY_BLOCK) == 0;
  }

  public boolean isSlaveModulesInWaitMode() {
    return (this.port3D00 & PORTw_ZXPOLY_nWAIT) == 0;
  }

  public boolean isFlashActive() {
    return this.videoFlashState;
  }

  public Z80 getMasterCpu() {
    return this.modules[0].getCpu();
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

  public int getMemTriggerAddress() {
    return this.triggerMemAddress;
  }

  public void setMemTriggerAddress(final int address) {
    this.triggerMemAddress = address;
  }

  public void setGfxAlignParams(final String registersToAlignOnStep) {
    this.gfxSyncRegsRecord = Z80.parseAndPackRegAlignValue(registersToAlignOnStep);
    LOGGER.info("Set GFX register list for aligning, '" + registersToAlignOnStep + "' = "
            + Integer.toBinaryString(this.gfxSyncRegsRecord));
    this.modules[0].setGfxPtrFromMainCpu(registersToAlignOnStep.contains("T"));
  }

  public int getFrameTiStates() {
    return this.frameTiStatesCounter;
  }

  public void dryIntTickOnWallClockTime(final boolean tstatesIntReached, final boolean wallclockInt,
                                        final int tstates) {
    this.beeper.clearChannels();
    this.beeper.updateState(tstatesIntReached, wallclockInt, tstates);
  }

  public void syncGfxCpuState(final Z80 sourceCpu) {
    for (final Z80 spec256GfxCore : this.spec256GfxCores) {
      spec256GfxCore.fillByState(sourceCpu);
    }
  }

  public int step(final boolean tiStatesIntReached,
                  final boolean wallClockIntReached,
                  final boolean commonNmi,
                  final boolean startNewFrame,
                  final boolean executionEnabled) {

    if (startNewFrame) {
      this.startNewFrame();
    }

    this.localResetForAllModules = false;

    final BoardMode currentMode = this.boardMode;

    final boolean resetStatisticsAtModules;

    int result = TRIGGER_NONE;

    if (commonNmi) {
      LOGGER.info("Incoming common NNI signal");
    }

    final boolean intTriggered;
    if (this.frameIntTriggered) {
      intTriggered = false;
    } else {
      intTriggered = tiStatesIntReached && wallClockIntReached;
      this.frameIntTriggered = intTriggered;
    }

    if (wallClockIntReached) {
      this.statisticCounter--;
      if (this.statisticCounter <= 0) {
        for (int i = 0; i < 4; i++) {
          this.cpuLoad[i] = min(1.0f, (float) (this.modules[i].getActiveMCyclesBetweenInt()
                  / NUMBER_OF_INT_BETWEEN_STATISTIC_UPDATE) / (float) (this.timingProfile.tstatesFrame));
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

    if (executionEnabled) {
      final ZxPolyModule[] modules = this.modules;

      final boolean signalReset = this.totalReset;
      this.totalReset = false;

      if (this.resetCounter > 0) {
        this.resetCounter--;
        if (this.resetCounter == 0) {
          this.set3D00(this.boardMode == BoardMode.ZXPOLY ? 0 : PORTw_ZXPOLY_BLOCK, true);
        }
      }

      for (final IoDevice device : this.ioDevicesPreStep) {
        device.preStep(this.frameTiStatesCounter, signalReset, tiStatesIntReached, wallClockIntReached);
      }

      final BoardMode mode = this.getBoardMode();

      switch (this.boardMode) {
        case ZXPOLY: {
          final boolean zx0halt;
          final boolean zx1halt;
          final boolean zx2halt;
          final boolean zx3halt;

          switch ((int) System.nanoTime() & 0x3) {
            case 0: {
              zx0halt = modules[0].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              if (localResetForAllModules) {
                return result;
              }
              zx3halt = modules[3].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              zx2halt = modules[2].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              zx1halt = modules[1].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
            }
            break;
            case 1: {
              zx1halt = modules[1].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              zx2halt = modules[2].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              zx0halt = modules[0].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              if (this.localResetForAllModules) {
                return result;
              }
              zx3halt = modules[3].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
            }
            break;
            case 2: {
              zx3halt = modules[3].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              zx0halt = modules[0].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              if (this.localResetForAllModules) {
                return result;
              }
              zx1halt = modules[1].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              zx2halt = modules[2].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
            }
            break;
            case 3: {
              zx2halt = modules[2].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              zx3halt = modules[3].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              zx1halt = modules[1].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              zx0halt = modules[0].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
              if (this.localResetForAllModules) {
                return result;
              }
            }
            break;
            default:
              throw new Error("Unexpected value");
          }

          if (isNotLockedPort3D00() && (zx0halt || zx1halt || zx2halt || zx3halt)) {
            // a cpu has met halt and we need process notification
            if (zx0halt) {
              doModuleHaltNotification(0);
            }
            if (zx1halt) {
              doModuleHaltNotification(1);
            }
            if (zx2halt) {
              doModuleHaltNotification(2);
            }
            if (zx3halt) {
              doModuleHaltNotification(3);
            }
          }
        }
        break;
        case ZX128: {
          modules[0].step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
        }
        break;
        case SPEC256: {
          final ZxPolyModule masterModule = modules[0];
          final Z80 mainCpu = masterModule.getCpu();
          masterModule.saveInternalCopyForGfx();
          final int syncRegRecord = this.gfxSyncRegsRecord;
          for (int i = 0; i < SPEC256_GFX_CORES; i++) {
            final Z80 gfxCore = this.spec256GfxCores[i];
            gfxCore.alignRegisterValuesWith(mainCpu, syncRegRecord);
            masterModule.gfxGpuStep(i + 1, gfxCore);
          }
          masterModule.step(currentMode, signalReset, intTriggered, commonNmi, resetStatisticsAtModules);
        }
        break;
        default:
          throw new Error("Unexpected board mode: " + this.boardMode);
      }

      final int spentTiStates = this.modules[0].getCpu().getStepTstates();
      this.frameTiStatesCounter += spentTiStates;

      final int feValue = this.video.getPortFE();
      final int levelTapeOut = this.audioLevels[((feValue >> 3) & 1) == 0 ? 0 : 14];
      final int levelSpeaker = this.audioLevels[((feValue >> 4) & 1) == 0 ? 0 : 15];
      final int levelTapeIn = this.audioLevels[this.keyboard.isTapeIn() ? 6 : 0];

      final int mixedLevels = Math.min(this.audioLevels[15], levelSpeaker + levelTapeIn + levelTapeOut);

      this.beeper.setChannelValue(CHANNEL_BEEPER, mixedLevels);

      for (final IoDevice device : this.ioDevicesPostStep) {
        device.postStep(spentTiStates);
      }

      this.beeper.updateState(tiStatesIntReached, wallClockIntReached, spentTiStates);

      final int curTriggers = this.triggers;

      if (curTriggers != TRIGGER_NONE) {
        if ((curTriggers & TRIGGER_DIFF_MODULESTATES) != 0 && !haveModulesSamePositionAndMode()) {
          this.triggers = this.triggers & ~TRIGGER_DIFF_MODULESTATES;
          result |= TRIGGER_DIFF_MODULESTATES;
        }
        if ((curTriggers & TRIGGER_DIFF_MEM_ADDR) != 0) {
          int val = modules[0].readAddress(this.triggerMemAddress);
          for (int i = 1; i < 4; i++) {
            if (val != modules[i].readAddress(this.triggerMemAddress)) {
              result |= TRIGGER_DIFF_MEM_ADDR;
              this.triggers = this.triggers & ~TRIGGER_DIFF_MEM_ADDR;
              break;
            }
          }
        }

        if ((curTriggers & TRIGGER_DIFF_EXE_CODE) != 0) {
          final int m1ExeByte = modules[0].getCpu().getLastM1InstructionByte();
          final int exeByte = modules[0].getCpu().getLastInstructionByte();

          for (int i = 1; i < 4; i++) {
            if (m1ExeByte != modules[i].getCpu().getLastM1InstructionByte() ||
                    exeByte != modules[i].getCpu().getLastInstructionByte()) {
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

  private void doModuleHaltNotification(final int moduleIndex) {
    final ZxPolyModule module = this.modules[moduleIndex];
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
        this.modules[0].prepareLocalNmi();
      }
      if ((reg1 & ZXPOLY_wREG1_CPU1) != 0) {
        this.modules[1].prepareLocalNmi();
      }
      if ((reg1 & ZXPOLY_wREG1_CPU2) != 0) {
        this.modules[2].prepareLocalNmi();
      }
      if ((reg1 & ZXPOLY_wREG1_CPU3) != 0) {
        this.modules[3].prepareLocalNmi();
      }
    }
  }

  public BoardMode getBoardMode() {
    return this.boardMode;
  }

  public void setBoardMode(final BoardMode newMode, final boolean doReset) {
    if (this.boardMode != newMode) {
      LOGGER.log(Level.INFO, "Motherboard mode changed to " + newMode);
      this.boardMode = newMode;
      if (doReset) {
        this.reset();
      }
    }
  }

  public ZxPolyModule[] getModules() {
    return this.modules;
  }

  public VideoController getVideoController() {
    return this.video;
  }

  public int readRam(final ZxPolyModule module, final int address) {
    return this._readRam(address);
  }

  public void writeRam(final ZxPolyModule module, final int heapAddress, final int value) {
    this._writeRam(heapAddress, value);
  }

  private boolean haveModulesSamePositionAndMode() {
    final Z80 cpu0 = this.modules[0].getCpu();
    final int pc = cpu0.getRegister(Z80.REG_PC);
    final int sp = cpu0.getRegister(Z80.REG_SP);
    final int im = cpu0.getIM();
    final boolean iff1 = cpu0.isIFF1();
    final boolean iff2 = cpu0.isIFF2();

    boolean result = true;

    for (int i = 1; result && i < 4; i++) {
      result = pc == this.modules[i].getCpu().getRegister(Z80.REG_PC)
              && sp == this.modules[i].getCpu().getRegister(Z80.REG_SP)
              && im == this.modules[i].getCpu().getIM()
              && iff1 == this.modules[i].getCpu().isIFF1()
              && iff2 == this.modules[i].getCpu().isIFF2();

    }
    return result;
  }

  public void writeBusIo(final ZxPolyModule module, final int port, final int value) {
    final int mappedCpu = getMappedCpuIndex();

    if (this.getBoardMode() == BoardMode.ZXPOLY) {
      if (module.isMaster()) {
        if (port == PORTrw_ZXPOLY) {
          set3D00(value, false);
        } else {
          if (mappedCpu == 0) {
            for (final IoDevice d : this.ioDevices) {
              d.writeIo(module, port, value);
            }
          } else {
            final ZxPolyModule targetModule = this.modules[mappedCpu];
            this._writeRam(targetModule.ramOffset2HeapAddress(targetModule.read7FFD(), port), value);
            targetModule.prepareLocalNmi();
          }
        }
      } else {
        for (final IoDevice d : this.ioDevices) {
          d.writeIo(module, port, value);
        }
      }
    } else {
      for (final IoDevice d : this.ioDevices) {
        d.writeIo(module, port, value);
      }
    }
  }

  public <T> T findIoDevice(final Class<T> klazz) {
    T result = null;
    for (final IoDevice d : this.ioDevices) {
      if (klazz.isInstance(d)) {
        result = klazz.cast(d);
        break;
      }
    }
    return result;
  }

  public int readBusIo(final ZxPolyModule module, final int port) {
    final int mappedCPU = getMappedCpuIndex();
    int result = -1;

    if (this.getBoardMode() == BoardMode.ZXPOLY && module.isMaster() && mappedCPU != 0) {
      final ZxPolyModule destinationModule = this.modules[mappedCPU];
      result = this._readRam(destinationModule.ramOffset2HeapAddress(destinationModule.read7FFD(), port));
      destinationModule.prepareLocalInt();
    } else {
      IoDevice firstDetectedActiveDevice = null;
      for (final IoDevice device : this.ioDevices) {
        final int data = device.readIo(module, port);
        if (data < 0) {
          continue;
        }

        if (result < 0) {
          result = data;
          firstDetectedActiveDevice = device;
        } else {
          final int prevResult = result;
          result |= data;
          if (prevResult != result) {
            LOGGER.log(Level.WARNING,
                    "Detected IO collision during read: " + firstDetectedActiveDevice + ", "
                            + device.getName() + " port #"
                            + Integer.toHexString(port).toUpperCase(Locale.ENGLISH));
          }
        }
      }

      if (result < 0) {
        if (this.attributePortFf && (port & 1) != 0) {
          if (this.frameTiStatesCounter < this.timingProfile.tstatesFrame) {
            final TimingProfile.UlaTact ramState = this.memoryTimings[this.frameTiStatesCounter];
            switch (ramState.type) {
              case TYPE_BORDER_FETCH_B1:
              case TYPE_SHIFT1_AND_FETCH_B2:
              case TYPE_SHIFT2_AND_FETCH_B1: {
                result = this.modules[0].readAddress(ramState.addressPixel);
              }
              break;
              case TYPE_BORDER_FETCH_A1:
              case TYPE_SHIFT1_AND_FETCH_A2:
              case TYPE_SHIFT2_AND_FETCH_A1: {
                result = this.modules[0].readAddress(ramState.addressAttribute);
              }
              break;
            }
          }
        }
      }
    }
    return result;
  }

  int contendPort(final int port7FFD, final int port) {
    int cpuTact = this.frameTiStatesCounter;
    int result = 0;
    if (isContended(port, port7FFD)) {
      cpuTact += this.frameTiStatesCounter < this.timingProfile.tstatesFrame ? this.memoryTimings[this.frameTiStatesCounter].contention : 0;
    }

    int shift = 1;
    int ft = (cpuTact + shift) % this.timingProfile.tstatesFrame;

    if (isUlaPort(port)) {
      cpuTact += this.memoryTimings[ft].contention;
    } else if (isContended(port, port7FFD)) {
      cpuTact += this.memoryTimings[ft].contention;
      ft += this.memoryTimings[ft].contention;
      ft++;
      ft %= this.timingProfile.tstatesFrame;
      cpuTact += this.memoryTimings[ft].contention;
      ft += this.memoryTimings[ft].contention;
      ft++;
      ft %= this.timingProfile.tstatesFrame;
      cpuTact += this.memoryTimings[ft].contention;
    }
    return cpuTact - this.frameTiStatesCounter;
  }

  int getContendedDelay(final int port7FFD, final int address) {
    int result = 0;
    if (isContended(address, port7FFD)) {
      result = this.frameTiStatesCounter < this.timingProfile.tstatesFrame ? this.memoryTimings[this.frameTiStatesCounter].contention : 0;
    }
    return result;
  }

  public void resetIoDevices() {
    for (final IoDevice device : this.ioDevices) {
      device.doReset();
    }
  }

  public void dispose() {
    this.beeper.dispose();
  }

  public byte[] getHeapRam() {
    return this.ram;
  }

  public List<IoDevice> findIoDevices() {
    return Arrays.asList(this.ioDevices);
  }

  public void startNewFrame() {
    this.frameTiStatesCounter = 0;
    this.frameIntTriggered = false;
  }

  public void doNop() {
    this.frameTiStatesCounter += 4;
  }

  public boolean isMode48k() {
    return this.modules[0].is48mode();
  }
}
