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

package com.igormaznitsa.zxpoly.tracer;

import java.awt.Color;
import java.awt.Font;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.util.Objects;
import javax.swing.JFormattedTextField;

public abstract class AbstractHexValueField extends JFormattedTextField {

  protected static final String ALLOWED_CHARS = "0123456789abcdefABCDEF";
  protected int intValue;
  private Color lastForeground;

  private Color changeColor = Color.RED;

  public AbstractHexValueField() {
    super();

    this.lastForeground = this.getForeground();
    this.setFont(new Font(Font.MONOSPACED, Font.PLAIN, this.getFont().getSize()));

    this.addKeyListener(new KeyAdapter() {

      @Override
      public void keyTyped(final KeyEvent e) {
        if (e.getKeyCode() == KeyEvent.VK_DELETE || e.getKeyCode() == KeyEvent.VK_BACK_SPACE ||
            ALLOWED_CHARS.indexOf(e.getKeyChar()) < 0) {
          e.consume();
        }
      }

    });


  }

  public Color getChangeColor() {
    return this.changeColor;
  }

  public void setChangeColor(Color color) {
    this.changeColor = Objects.requireNonNull(color);
  }

  public void setValue(int value) {
    value = processValue(value);

    if (this.intValue != value) {
      this.intValue = value;
      this.lastForeground = this.getForeground();
      this.setForeground(this.changeColor);
      refreshTextValue();
    } else {
      this.setForeground(this.lastForeground);
    }
  }

  protected abstract void refreshTextValue();

  protected abstract int processValue(int value);

  public int getIntValue() {
    String text = this.getText();
    text = text.isEmpty() ? "0" : text.trim();
    return Integer.parseInt(text, 16);
  }
}
