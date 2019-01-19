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

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.*;

public final class PenWidth extends JPanel {

  private static final long serialVersionUID = -7662850701072499309L;

  private final JSlider widthSlider;
  private final JLabel showLabel;
  private final BufferedImage showImage;

  private static final int ICON_WIDTH = 32;
  private static final int ICON_HEIGHT = 32;

  private final AtomicReference<BoundedRangeModel> currentModel = new AtomicReference<>();

  private final ChangeListener changeListener;

  private final TitledBorder titledBorder = BorderFactory.createTitledBorder("---");

  public PenWidth() {
    super();
    this.changeListener = (ChangeEvent e) -> {
      drawImageForValue(((BoundedRangeModel) e.getSource()).getValue());
    };
    setOpaque(false);
    setLayout(new BorderLayout(0, 0));

    this.showImage = new BufferedImage(ICON_WIDTH, ICON_HEIGHT, BufferedImage.TYPE_INT_RGB);

    this.widthSlider = new JSlider();
    this.widthSlider.setMajorTickSpacing(10);
    this.widthSlider.setMinorTickSpacing(5);
    this.widthSlider.setSnapToTicks(false);

    this.showLabel = new JLabel(new ImageIcon(showImage));

    add(this.showLabel, BorderLayout.CENTER);
    add(this.widthSlider, BorderLayout.SOUTH);

    this.widthSlider.setSize(ICON_WIDTH << 1, 10);
    this.widthSlider.setFocusable(false);

    setBorder(this.titledBorder);

    setSize(getPreferredSize());

    updateLookForCurrentModel();
  }

  @Override
  public boolean isFocusable() {
    return false;
  }

  public int getValue() {
    final BoundedRangeModel model = this.currentModel.get();
    return model == null ? -1 : model.getValue();
  }

  protected void updateLookForCurrentModel() {
    final BoundedRangeModel model = this.currentModel.get();
    if (model == null) {
      this.showLabel.setEnabled(false);
      this.widthSlider.setEnabled(false);
      drawImageForValue(-1);
    } else {
      this.showLabel.setEnabled(true);
      this.widthSlider.setEnabled(true);
      drawImageForValue(model.getValue());
    }
  }

  public void setModel(final BoundedRangeModel model) {
    final BoundedRangeModel prev = this.currentModel.getAndSet(model);
    if (prev != null) {
      prev.removeChangeListener(this.changeListener);
    }
    if (model == null) {
      this.widthSlider.setPaintTicks(false);
    } else {
      this.widthSlider.setModel(model);
      this.widthSlider.setPaintTicks(true);
      model.addChangeListener(this.changeListener);
    }
    updateLookForCurrentModel();
  }

  private void drawImageForValue(final int newValue) {
    final Graphics gfx = showImage.getGraphics();
    gfx.setColor(Color.white);
    gfx.fillRect(0, 0, ICON_WIDTH, ICON_HEIGHT);
    if (newValue > 0) {
      this.titledBorder.setTitle("Width:" + newValue + " px");

      gfx.setColor(Color.black);
      gfx.fillRect((ICON_WIDTH - newValue) / 2, (ICON_HEIGHT - newValue) / 2, newValue, newValue);
    } else {
      this.titledBorder.setTitle("Width:---");
    }
    gfx.dispose();
    repaint();
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(100, 110);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

}
