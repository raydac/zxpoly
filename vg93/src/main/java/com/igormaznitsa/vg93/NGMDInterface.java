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
package com.igormaznitsa.vg93;

import com.igormaznitsa.vg93.FloppyDisk.Sector;

public interface NGMDInterface {

  /**
   * Do step to next track
   *
   * @param outward true if step to outward, false otherwise
   */
  void doStep(boolean outward);

  /**
   * Load head on FDD.
   * @param loadHead
   */
  void doLoadHead(boolean loadHead);

  /**
   * Get status of FDD head.
   *
   * @return true if the head is loaded, false otherwise
   */
  boolean isHeadLoaded();

  /**
   * Get status of FDD.
   *
   * @return true if FDD is ready, false otherwise
   */
  boolean isFDDReady();

  /**
   * Check the track 00.
   *
   * @return true if the FDD head over track 00, false otherwise
   */
  boolean isTR00();

  /**
   * Check that the index hole detected.
   *
   * @return true if the index hole detected, false otherwise
   */
  boolean isIndex();

  /**
   * Check that the disk is write protect.
   *
   * @return true if the disk in FDD is write protect, false otherwise
   */
  boolean isWriteProtect();


  Sector getCurrentSector();
}
