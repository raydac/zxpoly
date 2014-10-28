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
package com.igormaznitsa.zxpoly;

import java.io.IOException;
import java.util.logging.*;
import javax.swing.SwingUtilities;

public class main {
  public static void main(final String ... args) {
    for(final Handler h : Logger.getLogger("").getHandlers()){
      h.setFormatter(new Formatter() {

        @Override
        public String format(final LogRecord record) {
          return record.getLevel()+" ["+record.getLoggerName()+"] : "+record.getMessage()+'\n';
        }
      });
    }
    
    
    SwingUtilities.invokeLater(new Runnable() {
      @Override
      public void run() {
        MainForm form = null;
        try {
          form = new MainForm();
        }
        catch (IOException ex) {
          ex.printStackTrace();
          System.exit(1);
        }
        form.setVisible(true);
      }
    });
  }
}