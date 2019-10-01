/*
 * Copyright (C) 2019 Igor Maznitsa
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

package com.igormaznitsa.zxpspritecorrector.components;

import com.igormaznitsa.zxpspritecorrector.utils.GfxUtils;
import com.igormaznitsa.zxpspritecorrector.utils.ZXPalette;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JComponent;

public final class ZXColorSelector extends JComponent implements MouseListener {

  private static final long serialVersionUID = 4513035804817773772L;
  private final Image iconINK;
  private final Image iconPAINT;
  private final List<ActionListener> actionListeners = new ArrayList<>();
  private final Dimension minSize;
  private int selectedInk = 0;
  private int selectedPaint = 15;

  public ZXColorSelector() {
    super();

    iconINK = GfxUtils.loadImage("ink.gif");
    iconPAINT = GfxUtils.loadImage("paint.gif");

    selectedInk = 0;
    selectedPaint = 15;

    if (iconINK != null && iconPAINT != null) {
      this.minSize = new Dimension((Math.max(iconINK.getWidth(null), iconPAINT.getWidth(null)) + 5) * 8, iconINK.getHeight(null) + 5 + iconPAINT.getHeight(null));
    } else {
      this.minSize = new Dimension(320, 40);
    }

    addMouseListener(this);
  }

  public int getSelectedInk() {
    return selectedInk;
  }

  public int getSelectedPaint() {
    return selectedPaint;
  }

  @Override
  public Dimension getMinimumSize() {
    return this.minSize;
  }

  @Override
  public boolean isFocusable() {
    return false;
  }

  @Override
  public void paintComponent(final Graphics gfx) {
    final int fullwidth = getWidth();
    final int fullheight = getHeight();

    final int cellwidth = fullwidth / 8;
    final int cellheight = fullheight / 2;

    if (cellwidth == 0 || cellheight == 0) {
      return;
    }

    for (int i = 0; i < 8; i++) {
      int indx = i;
      gfx.setColor(ZXPalette.COLORS[i]);

      final int cellx = i * cellwidth;

      gfx.fillRect(cellx, 0, cellwidth, cellheight);

      if (indx == selectedInk) {
        if (iconINK != null) {
          gfx.drawImage(iconINK, cellx, 0, null);
        }
      }

      if (indx == selectedPaint && iconPAINT != null) {
        gfx.drawImage(iconPAINT, cellx + (cellwidth - iconPAINT.getWidth(null)), cellheight - iconPAINT.getHeight(null), null);
      }

      indx += 8;
      gfx.setColor(ZXPalette.COLORS[indx]);

      gfx.fillRect(cellx, cellheight, cellwidth, cellheight);
      if (indx == selectedInk && iconINK != null) {
        gfx.drawImage(iconINK, cellx, cellheight, null);
      }

      if (indx == selectedPaint) {
        if (iconPAINT != null) {
          gfx.drawImage(iconPAINT, cellx + (cellwidth - iconPAINT.getWidth(null)), cellheight + (cellheight - iconPAINT.getHeight(null)), null);
        }
      }

    }
  }

  @Override
  public void mouseClicked(final MouseEvent e) {
    final int x = e.getX();
    final int y = e.getY();

    final int width = getWidth();
    final int height = getHeight();

    final int cellwidth = width / 8;
    final int cellheight = height / 2;

    final int column = x / cellwidth;
    final int row = y / cellheight;

    if (row < 0 || row > 1 || column < 0 || column > 7) {
      return;
    }

    switch (e.getButton()) {
      case MouseEvent.BUTTON1: {
        this.selectedInk = row * 8 + column;
        repaint();

        actionListeners.forEach((p_listener) -> p_listener.actionPerformed(new ActionEvent(this, 0, null)));

      }
      break;
      case MouseEvent.BUTTON3: {
        this.selectedPaint = row * 8 + column;
        repaint();

        this.actionListeners.forEach((l) -> l.actionPerformed(new ActionEvent(this, 0, null)));
      }
      break;
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {

  }

  @Override
  public void mouseReleased(MouseEvent e) {

  }

  @Override
  public void mouseEntered(MouseEvent e) {

  }

  @Override
  public void mouseExited(MouseEvent e) {

  }

  public void addActionListener(final ActionListener l) {
    if (l != null) {
      actionListeners.add(l);
    }
  }

  public void removeActionListener(final ActionListener l) {
    actionListeners.remove(l);
  }

  public void setInkIndex(int colorIndex) {
    selectedInk = colorIndex & 0xF;
    repaint();
  }

  public void setPaintIndex(int colorIndex) {
    selectedPaint = colorIndex & 0xF;
    repaint();
  }

}
