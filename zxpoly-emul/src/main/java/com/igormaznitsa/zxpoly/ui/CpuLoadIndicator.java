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

package com.igormaznitsa.zxpoly.ui;

import static com.igormaznitsa.jbbp.utils.JBBPUtils.assertNotNull;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Stroke;
import java.awt.image.BufferedImage;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.JPanel;

public final class CpuLoadIndicator extends JPanel {

  private static final long serialVersionUID = -8360307819233085278L;

  private static final Stroke STROKE_INDICATOR = new BasicStroke(1.0f);
  private static final Stroke STROKE_GRID = new BasicStroke(0.3f);
  private final int gridStep;
  private final Dimension indicatorSize;
  private final BufferedImage buffer;
  private final Graphics2D bufferGraphics;
  private Color gridColor;
  private int lastY;

  public CpuLoadIndicator(final int width, final int height, final int gridStep, final String text, final Color foreground, final Color background, final Color grid) {
    super();
    this.gridStep = Math.max(gridStep, 6);

    assertNotNull(background, "Background must not be null");
    assertNotNull(foreground, "Foreground must not be null");

    super.setBackground(background);
    super.setForeground(foreground);
    this.gridColor = grid;

    this.setOpaque(true);
    this.setDoubleBuffered(true);

    this.setToolTipText(text);

    this.indicatorSize = new Dimension(width, height);
    super.setSize(width, height);

    this.buffer = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    this.bufferGraphics = buffer.createGraphics();
    this.bufferGraphics.setStroke(STROKE_INDICATOR);
    this.bufferGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

    updateForState(0);

    clear();
  }

  private final Lock lockBufgerGraphics = new ReentrantLock();

  @Override
  public void setBackground(final Color color) {
    super.setBackground(color);
    if (this.buffer != null) {
      this.lockBufgerGraphics.lock();
      try {
        this.bufferGraphics.setColor(color);
        this.bufferGraphics.fillRect(0, 0, this.buffer.getWidth(), this.buffer.getHeight());
      } finally {
        this.lockBufgerGraphics.unlock();
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
    this.lockBufgerGraphics.lock();
    try {
      this.lastY = getHeight();
      this.bufferGraphics.setColor(this.getBackground());
      this.bufferGraphics.fillRect(0, 0, this.buffer.getWidth(), this.buffer.getHeight());
    } finally {
      this.lockBufgerGraphics.unlock();
    }
  }

  public void updateForState(final float loading) {
    final int step = this.gridStep >> 1;
    this.lockBufgerGraphics.lock();
    try {
      final int w = this.buffer.getWidth();
      final int h = this.buffer.getHeight();

      this.bufferGraphics.drawImage(this.buffer, -step, 0, null);
      this.bufferGraphics.setColor(this.getBackground());
      this.bufferGraphics.fillRect(w - step, 0, step, h);

      this.bufferGraphics.setColor(this.getForeground());

      final int startx = this.buffer.getWidth() - step;
      final int curx = this.buffer.getWidth();
      final int level = h - Math.round(loading * (h - 4)) - 2;

      this.bufferGraphics.drawLine(startx, this.lastY, curx, level);
      this.lastY = level;
    } finally {
      this.lockBufgerGraphics.unlock();
    }
    repaint();
  }

  @Override
  public int getWidth() {
    return this.indicatorSize.width;
  }

  @Override
  public int getHeight() {
    return this.indicatorSize.height;
  }

  @Override
  public Dimension getSize() {
    return this.indicatorSize;
  }

  @Override
  public Dimension getMinimumSize() {
    return this.indicatorSize;
  }

  @Override
  public Dimension getMaximumSize() {
    return this.indicatorSize;
  }

  @Override
  public Dimension getPreferredSize() {
    return this.indicatorSize;
  }

  @Override
  public void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g;

    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

    this.lockBufgerGraphics.lock();
    try {
      g2.drawImage(this.buffer, 0, 0, null);
    } finally {
      this.lockBufgerGraphics.unlock();
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
