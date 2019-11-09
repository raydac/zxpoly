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

public interface ZxPolyConstants {

  int PORTw_ZX128 = 0x7FFD;
  int PORTw_ZX128_INTCPU0 = 0x80;
  int PORTw_ZX128_ROMRAM = 0x40;
  int PORTw_ZX128_LOCK = 0x20;
  int PORTw_ZX128_ROM = 0x10;
  int PORTw_ZX128_SCREEN = 0x08;
  int PORTw_ZX128_RAMPAGE_MASK = 0x7;

  int PORTrw_ZXPOLY = 0x3D00;
  int PORTw_ZXPOLY_BLOCK = 0x80;
  int PORTw_ZXPOLY_RESET = 0x02;
  int PORTw_ZXPOLY_nWAIT = 0x01;

  int PORTr_ZXPOLY_IOFORCPU0 = 0x10;
  int PORTr_ZXPOLY_MEMDISABLED = 0x08;
  int PORTr_ZXPOLY_IODISABLED = 0x04;

  int REG0w_MEMORY_WRITING_DISABLED = 0x08;
  int REG0w_IO_WRITING_DISABLED = 0x10;
  int REG0w_LOCAL_RESET = 0x20;
  int REG0w_LOCAL_NMI = 0x40;
  int REG0w_LOCAL_INT = 0x80;

  int REG0r_HALT_STATUS = 0x01;
  int REG0r_WAIT_STATUS = 0x02;

  int VIDEOMODE_ZX48_CPU0 = 0;
  int VIDEOMODE_ZX48_CPU1 = 1;
  int VIDEOMODE_ZX48_CPU2 = 2;
  int VIDEOMODE_ZX48_CPU3 = 3;
  int VIDEOMODE_ZXPOLY_256x192 = 4;
  int VIDEOMODE_ZXPOLY_512x384 = 5;
  int VIDEOMODE_ZXPOLY_256x192_INKPAPER_MASK = 6;
  int VIDEOMODE_ZXPOLY_256x192_FLASH_MASK = 7;

  int ZXPOLY_wREG0_INT = 0x80;
  int ZXPOLY_wREG0_NMI = 0x40;
  int ZXPOLY_wREG0_RESET = 0x20;
  int ZXPOLY_wREG0_OUT_DISABLED = 0x10;
  int ZXPOLY_wREG0_MEMWR_DISABLED = 0x08;

  int ZXPOLY_rREG0_HALTMODE = 0x01;
  int ZXPOLY_rREG0_WAITMODE = 0x02;

  int ZXPOLY_wREG1_HALT_NOTIFY_NMI = 0x80;
  int ZXPOLY_wREG1_HALT_NOTIFY_INT = 0x40;
  int ZXPOLY_wREG1_WRITE_MAPPED_IO_7FFD = 0x20;
  int ZXPOLY_wREG1_DISABLE_NMI = 0x10;
  int ZXPOLY_wREG1_CPU3 = 0x08;
  int ZXPOLY_wREG1_CPU2 = 0x04;
  int ZXPOLY_wREG1_CPU1 = 0x02;
  int ZXPOLY_wREG1_CPU0 = 0x01;

  int PORTw_TRDOS_CMD = 0x1f;
  int PORTr_TRDOS_STATE = 0x1f;
  int PORTrw_TRDOS_TRACK = 0x3f;
  int PORTrw_TRDOS_SECTOR = 0x5f;
  int PORTrw_TRDOS_DATA = 0x7f;

  int PORTr_KMOUSE_BUTTONS = 0xfadf;
  int PORTr_KMOUSE_XCOORD = 0xfbdf;
  int PORTr_KMOUSE_YCOORD = 0xffdf;

  int PORTr_KJOYSTICK = 0x001F;

  int PORTr_ZX48_BUS = 0x00FF;

  int PORTrw_ZX48_KTB = 0x00FE;
  int PORTr_ZX48_KTB_TAPE = 0x40;
  int PORTw_ZX48_KTB_TAPE = 0x08;
  int PORTw_ZX48_KTB_BEAPER = 0x10;

  int PORTrw_ZXPRINTER = 0x00FB;
}
