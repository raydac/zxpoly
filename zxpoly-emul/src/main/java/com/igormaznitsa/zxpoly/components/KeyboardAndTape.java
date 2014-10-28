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

import java.awt.event.KeyEvent;

public class KeyboardAndTape implements IODevice {
  private final Motherboard board;
  
  private final int [] keyboardLines = new int [8];
  
  public KeyboardAndTape(final Motherboard board){
    this.board = board;
  }

  private int getKbdValueForLines(int hiByte){
    int result = 0xFF;
    for(int i=0;i<8;i++){
      if ((hiByte & 1)==0){
        result &= this.keyboardLines[i];
      }
    }
    return result;
  }
  
  @Override
  public int readIO(final ZXPolyModule module, final int port) {
    if ((port & 0xFF)==0xFE){
      return getKbdValueForLines((port >> 8) & 0xFF);
    }else{
      return 0;
    }
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
    if (signalReset){
      for(int i=0;i<this.keyboardLines.length; i++){
        this.keyboardLines[i] |= 0x1F;
      }
    }
  }
 
  @Override
  public String getName() {
    return "Keyboard";
  }

  public void onKeyEvent(final KeyEvent evt){
    final boolean pressed;
    switch(evt.getID()){
      case KeyEvent.KEY_PRESSED : pressed = true;break;
      case KeyEvent.KEY_RELEASED : pressed = false;break;
      default: return;
    }
    
    final int line;
    final int code;
    
    switch (evt.getKeyCode()) {
      case KeyEvent.VK_1:{line = 3; code = 0x1E;}break;
      case KeyEvent.VK_2:{line = 3; code = 0x1D;}break;
      case KeyEvent.VK_3:{line = 3; code = 0x1B;}break;
      case KeyEvent.VK_4:{line = 3; code = 0x17;}break;
      case KeyEvent.VK_5:{line = 3; code = 0x0F;}break;
      case KeyEvent.VK_6:{line = 4; code = 0x0F;}break;
      case KeyEvent.VK_7:{line = 4; code = 0x17;}break;
      case KeyEvent.VK_8:{line = 4; code = 0x1B;}break;
      case KeyEvent.VK_9:{line = 4; code = 0x1D;}break;
      case KeyEvent.VK_0:{line = 4; code = 0x1E;}break;
      case KeyEvent.VK_Q:{line = 2; code = 0x1E;}break;
      case KeyEvent.VK_W:{line = 2; code = 0x1D;}break;
      case KeyEvent.VK_E:{line = 2; code = 0x1B;}break;
      case KeyEvent.VK_R:{line = 2; code = 0x17;}break;
      case KeyEvent.VK_T:{line = 2; code = 0x0F;}break;
      case KeyEvent.VK_Y:{line = 5; code = 0x0F;}break;
      case KeyEvent.VK_U:{line = 5; code = 0x17;}break;
      case KeyEvent.VK_I:{line = 5; code = 0x1B;}break;
      case KeyEvent.VK_O:{line = 5; code = 0x1D;}break;
      case KeyEvent.VK_P:{line = 5; code = 0x1E;}break;
      case KeyEvent.VK_A:{line = 1; code = 0x1E;}break;
      case KeyEvent.VK_S:{line = 1; code = 0x1D;}break;
      case KeyEvent.VK_D:{line = 1; code = 0x1B;}break;
      case KeyEvent.VK_F:{line = 1; code = 0x17;}break;
      case KeyEvent.VK_G:{line = 1; code = 0x0F;}break;
      case KeyEvent.VK_H:{line = 6; code = 0x0F;}break;
      case KeyEvent.VK_J:{line = 6; code = 0x17;}break;
      case KeyEvent.VK_K:{line = 6; code = 0x1B;}break;
      case KeyEvent.VK_L:{line = 6; code = 0x1D;}break;
      case KeyEvent.VK_ENTER:{line = 6; code = 0x1E;}break;
      case KeyEvent.VK_Z:{line = 0; code = 0x1D;}break;
      case KeyEvent.VK_X:{line = 0; code = 0x1B;}break;
      case KeyEvent.VK_C:{line = 0; code = 0x17;}break;
      case KeyEvent.VK_V:{line = 0; code = 0x0F;}break;
      case KeyEvent.VK_B:{line = 7; code = 0x0F;}break;
      case KeyEvent.VK_N:{line = 7; code = 0x17;}break;
      case KeyEvent.VK_M: {line = 7; code = 0x1B;}break;
      case KeyEvent.VK_SPACE: {line = 7; code = 0x1E; } break;
      case KeyEvent.VK_SHIFT:{ line = 0; code = 0x1E;}break;
      case KeyEvent.VK_ALT:{line = 7; code = 0x1D;}break;
      default: return;
    }

    if (pressed){
      this.keyboardLines[line] &= code;
    }else{
      this.keyboardLines[line] |= (~code & 0x1F);
    }
  }
}
