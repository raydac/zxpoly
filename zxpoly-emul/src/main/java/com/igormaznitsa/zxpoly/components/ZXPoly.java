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

public interface ZXPoly {

  public static final int PORTw_ZX128 = 0x7FFD;
  public static final int PORTw_ZX128_INTCPU0 = 0x80;
  public static final int PORTw_ZX128_ROMRAM = 0x40;
  public static final int PORTw_ZX128_LOCK = 0x20;
  public static final int PORTw_ZX128_ROM = 0x10;
  public static final int PORTw_ZX128_SCREEN = 0x08;
  public static final int PORTw_ZX128_RAMPAGE_MASK = 0x7;

  public static final int PORTrw_ZXPOLY = 0x3D00;
  public static final int PORTw_ZXPOLY_BLOCK = 0x80;
  public static final int PORTw_ZXPOLY_RESET = 0x02;
  public static final int PORTw_ZXPOLY_nWAIT = 0x01;

  public static final int PORTr_ZXPOLY_IOFORCPU0 = 0x10;
  public static final int PORTr_ZXPOLY_MEMDISABLED = 0x08;
  public static final int PORTr_ZXPOLY_IODISABLED = 0x04;

  public static final int REG0w_MEMORY_WRITING_DISABLED = 0x08;
  public static final int REG0w_IO_WRITING_DISABLED = 0x10;
  public static final int REG0w_LOCAL_RESET = 0x20;
  public static final int REG0w_LOCAL_NMI = 0x40;
  public static final int REG0w_LOCAL_INT = 0x80;
 
  public static final int REG0r_HALT_STATUS = 0x01;
  public static final int REG0r_WAIT_STATUS = 0x02;
  
  public static final int VIDEOMODE_ZX48_CPU0 = 0;
  public static final int VIDEOMODE_ZX48_CPU1 = 1;
  public static final int VIDEOMODE_ZX48_CPU2 = 2;
  public static final int VIDEOMODE_ZX48_CPU3 = 3;
  public static final int VIDEOMODE_ZXPOLY_256x192 = 4;
  public static final int VIDEOMODE_ZXPOLY_512x384 = 5;
  public static final int VIDEOMODE_ZXPOLY_256x192_A0 = 6;
  public static final int VIDEOMODE_RESERVED2 = 7;

  public static final int ZXPOLY_wREG0_INT = 0x80;
  public static final int ZXPOLY_wREG0_NMI = 0x40;
  public static final int ZXPOLY_wREG0_RESET = 0x20;
  public static final int ZXPOLY_wREG0_OUT_DISABLED = 0x10;
  public static final int ZXPOLY_wREG0_MEMWR_DISABLED = 0x08;

  public static final int ZXPOLY_rREG0_HALTMODE = 0x01;
  public static final int ZXPOLY_rREG0_WAITMODE = 0x02;

  public static final int ZXPOLY_wREG1_HALT_NOTIFY_NMI = 0x80;
  public static final int ZXPOLY_wREG1_HALT_NOTIFY_INT = 0x40;
  public static final int ZXPOLY_wREG1_WRITE_MAPPED_IO_7FFD = 0x20;
  public static final int ZXPOLY_wREG1_DISABLE_NMI = 0x10;
  public static final int ZXPOLY_wREG1_CPU3 = 0x08;
  public static final int ZXPOLY_wREG1_CPU2 = 0x04;
  public static final int ZXPOLY_wREG1_CPU1 = 0x02;
  public static final int ZXPOLY_wREG1_CPU0 = 0x01;

  public static final int PORTw_TRDOS_CMD = 0x1f;
  public static final int PORTr_TRDOS_STATE = 0x1f;
  public static final int PORTrw_TRDOS_TRACK = 0x3f;
  public static final int PORTrw_TRDOS_SECTOR = 0x5f;
  public static final int PORTrw_TRDOS_DATA = 0x7f;

  public static final int PORTr_KMOUSE_BUTTONS = 0xfadf;
  public static final int PORTr_KMOUSE_XCOORD = 0xfbdf;
  public static final int PORTr_KMOUSE_YCOORD = 0xffdf;

  public static final int PORTr_KJOYSTICK = 0x001F;
  
  public static final int PORTr_ZX48_BUS = 0x00FF;
  
  public static final int PORTrw_ZX48_KTB = 0x00FE;
  public static final int PORTr_ZX48_KTB_TAPE = 0x40;
  public static final int PORTw_ZX48_KTB_TAPE = 0x08;
  public static final int PORTw_ZX48_KTB_BEAPER = 0x10;
  
  public static final int PORTrw_ZXPRINTER = 0x00FB;
}
