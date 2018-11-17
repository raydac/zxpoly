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
package com.igormaznitsa.zxpoly.ui;

import com.igormaznitsa.jbbp.utils.JBBPUtils;
import java.awt.*;
import java.awt.image.BufferedImage;
import javax.swing.*;

public final class CPULoadIndicator extends JPanel {

  private static final long serialVersionUID = -8360307819233085278L;

  private static final Stroke STROKE_INDICATOR = new BasicStroke(1.0f);
  private static final Stroke STROKE_GRID = new BasicStroke(0.3f);

  private Color gridColor;
  private final int gridStep;

  private final Dimension thesize;

  private final BufferedImage buffer;

  private int lastY;

  public CPULoadIndicator(final int width, final int height, final int gridStep, final String text, final Color foreground, final Color background, final Color grid) {
    super();
    this.gridStep = Math.max(gridStep, 6);

    JBBPUtils.assertNotNull(background, "Background must not be null");
    JBBPUtils.assertNotNull(foreground, "Foreground must not be null");

    super.setBackground(background);
    super.setForeground(foreground);
    this.gridColor = grid;

    this.setOpaque(true);
    this.setDoubleBuffered(true);

    this.setToolTipText(text);

    this.thesize = new Dimension(width, height);
    super.setSize(width, height);

    this.buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);

    updateForState(0);

    setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

    clear();
  }

  @Override
  public void setBackground(final Color color) {
    super.setBackground(color);
    if (this.buffer != null) {
      synchronized (this.buffer) {
        final Graphics2D gfx = this.buffer.createGraphics();
        gfx.setColor(color);
        gfx.fillRect(0, 0, this.buffer.getWidth(), this.buffer.getHeight());
        gfx.dispose();
      }
    }
  }

  public Color getGridColor() {
    return this.gridColor;
  }

  public void setGridColor(final Color color) {
    this.gridColor = color;
  }

  public void clear() {
    synchronized (this.buffer) {
      this.lastY = getHeight();
      final Graphics2D gfx = this.buffer.createGraphics();
      gfx.setColor(this.getBackground());
      gfx.fillRect(0, 0, this.buffer.getWidth(), this.buffer.getHeight());
      gfx.dispose();
    }
  }

  public void updateForState(final float loading) {
    final int step = this.gridStep >> 1;
    synchronized (this.buffer) {
      final int w = this.buffer.getWidth();
      final int h = this.buffer.getHeight();

      final Graphics2D gfx = this.buffer.createGraphics();
      gfx.drawImage(this.buffer, -step, 0, null);
      gfx.setColor(this.getBackground());
      gfx.fillRect(w - step, 0, step, h);

      gfx.setColor(this.getForeground());

      final int startx = this.buffer.getWidth() - step;
      final int curx = this.buffer.getWidth();
      final int level = h - Math.round(loading * (h - 4)) - 2;
      gfx.setStroke(STROKE_INDICATOR);

      gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

      gfx.drawLine(startx, this.lastY, curx, level);
      this.lastY = level;
      gfx.dispose();
    }
    repaint();
  }

  @Override
  public int getWidth() {
    return this.thesize.width;
  }

  @Override
  public int getHeight() {
    return this.thesize.height;
  }

  @Override
  public Dimension getSize() {
    return this.thesize;
  }

  @Override
  public Dimension getMinimumSize() {
    return this.thesize;
  }

  @Override
  public Dimension getMaximumSize() {
    return this.thesize;
  }

  @Override
  public Dimension getPreferredSize() {
    return this.thesize;
  }

  @Override
  public void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g;

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    synchronized (this.buffer) {
      g2.drawImage(this.buffer, 0, 0, null);
    }

    final int w = this.getWidth();
    final int h = this.getHeight();

    if (this.gridColor != null) {
      g2.setStroke(STROKE_GRID);
      g2.setColor(this.gridColor);
      for (int y = this.getHeight() - this.gridStep; y > 0; y -= this.gridStep) {
        g2.drawLine(0, y, w, y);
      }

      for (int x = 0; x < w; x += this.gridStep) {
        g2.drawLine(x, 0, x, h);
      }
    }
  }

}
