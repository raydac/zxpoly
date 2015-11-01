/*
 * Copyright (C) 2015 Raydac Research Group Ltd.
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

import java.text.ParseException;
import java.util.Locale;
import javax.swing.JFormattedTextField;
import javax.swing.text.MaskFormatter;

public class HexValue2Field extends AbstractHexValueField {

  public HexValue2Field () {
    super();
    final JFormattedTextField.AbstractFormatter FORMAT;
    try {
      final MaskFormatter formatter = new MaskFormatter("HH");
      formatter.setValidCharacters(ALLOWED_CHARS);
      formatter.setPlaceholderCharacter('0');
      formatter.setAllowsInvalid(false);
      FORMAT = formatter;
    }
    catch (ParseException ex) {
      throw new Error("Can't prepare formatter", ex);
    }

    setFormatter(FORMAT);
    refreshTextValue();
  }

  @Override
  protected int processValue (int value) {
    return value & 0xFF;
  }

  @Override
  protected final void refreshTextValue () {
    String hex = Integer.toHexString(this.intValue).toUpperCase(Locale.ENGLISH);
    hex = hex.length() < 2 ? "0" + hex : hex.substring(0, 2);
    this.setText(hex);
  }

}
