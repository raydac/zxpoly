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

public class CompileInstructionException extends RuntimeException {
  private static final long serialVersionUID = 1861814764760862649L;
  
  private final Z80Instruction source;
  
  public CompileInstructionException(final Z80Instruction source, final String text){
    super(text);
    this.source = source;
  }
  
  public Z80Instruction getSource(){
    return this.source;
  }
}
