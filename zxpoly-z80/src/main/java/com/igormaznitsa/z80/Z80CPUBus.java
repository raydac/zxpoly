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

package com.igormaznitsa.z80;

public interface Z80CPUBus {
  byte readMemory(Z80 cpu, int ctx, int address, boolean m1, boolean cmdOrPrefix);

  void writeMemory(Z80 cpu, int ctx, int address, byte data);

  int readPtr(Z80 cpu, int ctx, int reg, int valueInReg);

  int readSpecRegValue(Z80 cpu, int ctx, int reg, int origValue);

  int readSpecRegPairValue(Z80 cpu, int ctx, int regPair, int origValue);

  int readRegPortAddr(Z80 cpu, int ctx, int reg, int valueInReg);

  byte readPort(Z80 cpu, int ctx, int port);

  void writePort(Z80 cpu, int ctx, int port, byte data);

  byte onCPURequestDataLines(Z80 cpu, int ctx);

  void onRETI(Z80 cpu, int ctx);

  void onInterrupt(Z80 cpu, int ctx, boolean nmi);
}
