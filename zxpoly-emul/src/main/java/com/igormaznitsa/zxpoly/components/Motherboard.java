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

import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.z80.disasm.Z80Disasm;
import com.igormaznitsa.z80.disasm.Z80Instruction;
import java.io.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

public class Motherboard implements ZXPoly {

  private static final Logger LOG = Logger.getLogger("MB");

  private final ZXPolyModule[] modules;
  private final IODevice[] ioDevices;
  private final byte[] ram = new byte[512 * 1024];
  private final VideoController video;
  private final KeyboardAndTape keyboard;
  private final AtomicReference<ZXRom> rom = new AtomicReference<ZXRom>();

  private int port3D00 = (int) System.nanoTime() & 0xFF;
  private boolean totalReset = true;

  private int intCounter;
  private volatile boolean videoFlashState;

  private static FileWriter logFile;
  
  private String toHex(final int val){
    final String hex = Integer.toHexString(val).toUpperCase(Locale.ENGLISH);
    return '#'+(hex.length()<4 ? "0000".substring(0,4-hex.length()) + hex: hex);
  }
  
  private void logCPU(final ZXPolyModule module){
    try{
    if (logFile == null){
      logFile = new FileWriter("cpulog.log",false);
    }
    
    final String sp = toHex(module.getCPU().getRegister(Z80.REG_SP));
    final String pc = toHex(module.getCPU().getRegister(Z80.REG_PC));
    final String f = toHex(module.getCPU().getRegister(Z80.REG_F));
    
    final int pcreg = module.getCPU().getRegister(Z80.REG_PC);
    
    final Z80Instruction in = Z80Disasm.decodeInstruction(rom.get().getArray(), pcreg);
    
      logFile.write(pc+"\t"+in.decode(rom.get().getArray(), pcreg, pcreg)+"\t\t SP:"+sp+"\tF:"+f+'\n');
      logFile.flush();
    }catch(Exception ex){
      ex.printStackTrace();
    }
  }
  
  
  public Motherboard(final ZXRom rom) {
    this.rom.set(rom);
    this.modules = new ZXPolyModule[4];
    final List<IODevice> iodevices = new ArrayList<IODevice>();
    for (int i = 0; i < this.modules.length; i++) {
      this.modules[i] = new ZXPolyModule(this, i);
      iodevices.add(this.modules[i]);
    }
    iodevices.add(new BetaDiscInterface(this));
    
    this.keyboard = new KeyboardAndTape(this);
    iodevices.add(keyboard);
    this.video = new VideoController(this);
    iodevices.add(video);
    this.ioDevices = iodevices.toArray(new IODevice[iodevices.size()]);

    // simulation of garbage in memory after power on
    final Random rnd = new Random();
    rnd.nextBytes(this.ram);
  }

  public void reset() {
    LOG.info("Full system reset");
    this.port3D00 = 0;
    this.totalReset = true;
    set3D00(0);
  }

  public boolean set3D00(final int value) {
    if (is3D00NotLocked()) {
      this.port3D00 = value;
      this.video.setVideoMode((this.port3D00 >> 2) & 0x7);
      LOG.info("#3D00 has changed to "+value);
      return true;
    }else{
      LOG.info("Rejected new value for #3D00 because it is locked");
      return false;
    }
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
    final ZXRom therom = this.rom.get();
    return therom.readAddress(address);
  }

  public void setROM(final ZXRom newRom) {
    this.rom.set(newRom);
  }

  private boolean ttt;

  public void step(final boolean signalInt) {
    if (signalInt) {
      this.intCounter++;
      if (this.intCounter >= 50) {
        this.intCounter = 0;
        this.videoFlashState = !this.videoFlashState;
      }
    }

    final boolean signalReset = this.totalReset;
    this.totalReset = false;

    for (final IODevice d : this.ioDevices) {
      d.preStep(signalReset, signalInt);
    }

    // simpulation of possible racing
    final boolean zx0halt;
    final boolean zx1halt;
    final boolean zx2halt;
    final boolean zx3halt;

    switch ((int) System.nanoTime() & 0x3) {
      case 0: {
        zx0halt = this.modules[0].step(signalReset, signalInt);
        zx3halt = this.modules[3].step(signalReset, signalInt);
        zx2halt = this.modules[2].step(signalReset, signalInt);
        zx1halt = this.modules[1].step(signalReset, signalInt);
      }
      break;
      case 1: {
        zx1halt = this.modules[1].step(signalReset, signalInt);
        zx2halt = this.modules[2].step(signalReset, signalInt);
        zx0halt = this.modules[0].step(signalReset, signalInt);
        zx3halt = this.modules[3].step(signalReset, signalInt);
      }
      break;
      case 2: {
        zx3halt = this.modules[3].step(signalReset, signalInt);
        zx0halt = this.modules[0].step(signalReset, signalInt);
        zx1halt = this.modules[1].step(signalReset, signalInt);
        zx2halt = this.modules[2].step(signalReset, signalInt);
      }
      break;
      case 3: {
        zx2halt = this.modules[2].step(signalReset, signalInt);
        zx3halt = this.modules[3].step(signalReset, signalInt);
        zx1halt = this.modules[1].step(signalReset, signalInt);
        zx0halt = this.modules[0].step(signalReset, signalInt);
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

  public ZXPolyModule[] getZXPolyModules() {
    return this.modules;
  }

  public VideoController getVideoController() {
    return this.video;
  }

  public int readRAM(final ZXPolyModule module, final int address) {
    return this.ram[address];
  }

  public void writeRAM(final ZXPolyModule module, final int heapAddress, final int value) {
    this.ram[heapAddress] = (byte) value;
  }

  public int readBusIO(final ZXPolyModule module, final int port) {
    final int mappedCPU = getMappedCPUIndex();
    int result = 0;

    if (module.getModuleIndex() == 0 && mappedCPU > 0) {
      final ZXPolyModule destmodule = modules[mappedCPU];
      result = this.ram[destmodule.getHeapOffset() + port];
      destmodule.prepareLocalInt();
    }
    else {
      IODevice firstDetected = null;

      for (final IODevice d : this.ioDevices) {
        final int prevResult = result;
        result |= d.readIO(module, port);
        if (prevResult != result) {
          // changed
          if (prevResult == 0) {
            firstDetected = d;
          }
          else {
            LOG.warning("Detected collision during IO reading: " + firstDetected.getName() + ", " + d.getName() + " port #" + Integer.toHexString(port).toUpperCase(Locale.ENGLISH));
          }
        }
      }
    }
    return result;
  }

  public void writeBusIO(final ZXPolyModule module, final int port, final int value) {
    final int mappedCPU = getMappedCPUIndex();
    final int moduleIndex = module.getModuleIndex();

    if (moduleIndex == 0) {
      if (port == PORTrw_ZXPOLY) {
          set3D00(value);
      }
      else {
        if (mappedCPU > 0) {
          final ZXPolyModule destmodule = modules[mappedCPU];
          this.ram[destmodule.getHeapOffset() + port] = (byte) value;
          destmodule.prepareLocalNMI();
        }
        else {
          for (final IODevice d : this.ioDevices) {
            d.writeIO(module, port, value);
          }
        }
      }
    }
    else {
      for (final IODevice d : this.ioDevices) {
        d.writeIO(module, port, value);
      }
    }
  }

  public KeyboardAndTape getKeyboard() {
    return this.keyboard;
  }
}
