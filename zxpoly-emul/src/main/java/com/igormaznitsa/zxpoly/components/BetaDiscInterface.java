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

import java.util.logging.Logger;

public class BetaDiscInterface implements IODevice {
  private static final Logger log = Logger.getLogger("BD");
  
  private final Motherboard board;
  
  public BetaDiscInterface(final Motherboard board){
    this.board = board;
  }

  @Override
  public int readIO(final ZXPolyModule module, final int port) {
    return 0;
  }

  @Override
  public void writeIO(final ZXPolyModule module, final int port, final int value) {
  }

  @Override
  public Motherboard getMotherboard() {
    return this.board;
  }

  @Override
  public void preStep(final boolean signalReset, final boolean signalInt) {
  }

  @Override
  public String getName() {
    return "BetaDiscInterface";
  }

}
