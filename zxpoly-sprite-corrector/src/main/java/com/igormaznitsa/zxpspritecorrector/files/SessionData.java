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
package com.igormaznitsa.zxpspritecorrector.files;

import com.igormaznitsa.jbbp.io.*;
import com.igormaznitsa.zxpspritecorrector.components.EditorComponent;
import java.io.IOException;

public class SessionData {
  private final int baseAddress;
  private final boolean showGrid;
  private final boolean showColumns;
  private final int columnNumber;
  private final boolean zxAddressing;
  private final boolean invertBase;
  private final boolean mode512x384;
  private final int zoom;
  private final EditorComponent.AttributeMode attributeMode;
  
  public int getBaseAddress(){
    return this.baseAddress;
  }
  
  public int getZoom(){
    return this.zoom;
  }
  
  public boolean isInvertBaseShow(){
    return this.invertBase;
  }
  
  public boolean is512Mode(){
    return this.mode512x384;
  }
  
  public boolean isShowGrid(){
    return this.showGrid;
  }
  
  public boolean isShowColumns(){
    return this.showColumns;
  }
  
  public int getColumnNumber(){
    return this.columnNumber;
  }
  
  public boolean isZXAddressing(){
    return this.zxAddressing;
  }
  
  public EditorComponent.AttributeMode getAttributeMode(){
    return this.attributeMode;
  }
  
  public SessionData(final JBBPBitInputStream inStream) throws IOException {
    baseAddress = inStream.readInt(JBBPByteOrder.BIG_ENDIAN);
    showGrid = inStream.readBoolean();
    showColumns = inStream.readBoolean();
    zxAddressing = inStream.readBoolean();
    invertBase = inStream.readBoolean();
    mode512x384 = inStream.readBoolean();
    columnNumber = inStream.readInt(JBBPByteOrder.BIG_ENDIAN);
    attributeMode = EditorComponent.AttributeMode.values()[inStream.readInt(JBBPByteOrder.BIG_ENDIAN)];
    zoom = inStream.readInt(JBBPByteOrder.BIG_ENDIAN);
  }
          
  public SessionData(final EditorComponent editor){
    this.baseAddress = editor.getAddress();
    this.showGrid = editor.isShowGrid();
    this.showColumns = editor.isShowColumnBorders();
    this.columnNumber = editor.getColumns();
    this.zxAddressing = editor.isZXScreenMode();
    this.attributeMode = editor.getShowAttributes();
    this.invertBase = editor.isInvertShowBaseData();
    this.mode512x384 = editor.isMode512();
    this.zoom = editor.getZoom();
  }

  public void fill(final EditorComponent editor){
    editor.setAddress(this.baseAddress);
    editor.setShowGrid(this.showGrid);
    editor.setShowColumnBorders(this.showColumns);
    editor.setColumns(this.columnNumber);
    editor.setZXScreenMode(this.zxAddressing);
    editor.setShowAttributes(this.attributeMode);
    editor.setZoom(this.zoom);
  }
  
  public byte [] makeArray() throws IOException {
    return JBBPOut.BeginBin().
            Int(this.baseAddress).
            Bool(this.showGrid, this.showColumns,this.zxAddressing, this.invertBase, this.mode512x384).
            Int(this.columnNumber, this.attributeMode.ordinal(), this.zoom).End().toByteArray();
  }
  
}
