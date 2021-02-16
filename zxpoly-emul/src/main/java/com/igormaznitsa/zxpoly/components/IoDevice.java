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

public interface IoDevice extends ZxPolyConstants {

  int NOTIFICATION_NONE = 0;
  int NOTIFICATION_PRESTEP = 1;
  int NOTIFICATION_POSTSTEP = 2;

  Motherboard getMotherboard();

  default void init() {
  }

  /**
   * Read IO port byte
   *
   * @param module module index
   * @param port   port address
   * @return value of its port, or -1 if device has not data for such port
   */
  int readIo(ZxPolyModule module, int port);

  void writeIo(ZxPolyModule module, int port, int value);

  void preStep(boolean signalReset, boolean tstatesIntReached, boolean wallclockInt);

  void postStep(int spentTstates);

  String getName();

  void doReset();

  int getNotificationFlags();
}
