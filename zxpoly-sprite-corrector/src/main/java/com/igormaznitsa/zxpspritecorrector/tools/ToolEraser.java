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
import java.awt.Rectangle;

public class ToolEraser extends AbstractTool {

  private static final long serialVersionUID = -7746198807169766129L;

  public ToolEraser() {
    super("draw_eraser.png", "Eraser erases pixels");
  }

  @Override
  public void process(EditorComponent editComponent, Rectangle area, int modifiers) {
    final EditorComponent.ZXGraphics gfx = editComponent.getZXGraphics();

    final boolean mode512 = editComponent.isMode512();
    
    for (int x = 0; x < area.width; x++) {
      for (int y = 0; y < area.height; y++) {
        final int dx = x + area.x;
        final int dy = y + area.y;

        if (!isCoordValid(dx, dy, mode512)) {
          continue;
        }
        gfx.resetPoint(dx, dy);
      }
    }

    gfx.flush();
  }

}
