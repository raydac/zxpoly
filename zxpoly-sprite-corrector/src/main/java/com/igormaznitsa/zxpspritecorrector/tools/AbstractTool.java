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
package com.igormaznitsa.zxpspritecorrector.tools;

import com.igormaznitsa.zxpspritecorrector.components.EditorComponent;
import com.igormaznitsa.zxpspritecorrector.utils.GfxUtils;
import java.awt.Rectangle;
import javax.swing.*;

public abstract class AbstractTool extends JToggleButton {

  private static final long serialVersionUID = 5453667415344533432L;

  public static final int BUTTON_NONE = 0;
  public static final int BUTTON_MOUSE_LEFT = 1;
  public static final int BUTTON_MOUSE_MIDDLE = 2;
  public static final int BUTTON_MOUSE_RIGHT = 4;
  public static final int BUTTON_SHIFT = 8;
  public static final int BUTTON_CTRL = 16;
  public static final int BUTTON_ALT = 32;

  protected final BoundedRangeModel model = new DefaultBoundedRangeModel(1, 0, 1, 32);

  public AbstractTool(final String iconName, final String toolTip) {
    super();
    setModel(new ToolButtonModel(this));
    setIcon(new ImageIcon(GfxUtils.loadImage(iconName)));
    setToolTipText(toolTip);
  }

  @Override
  public boolean isFocusable() {
    return false;
  }

  public BoundedRangeModel getScaleModel() {
    return this.model;
  }

  public boolean doesSupport512x384() {
    return true;
  }

  public abstract void process(final EditorComponent editComponent, final Rectangle area, final int buttons);

  public static boolean isCoordValid(final int x, final int y, final boolean mode512) {
    if (mode512) {
      return x >= 0 && y >= 0 && x < 512 && y < 384;
    } else {
      return x >= 0 && y >= 0 && x < 256 && y < 192;
    }
  }
}
