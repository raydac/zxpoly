package com.igormaznitsa.zxpspritecorrector.components;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

public final class PenWidth extends JPanel implements BoundedRangeModel {

  private static final long serialVersionUID = -7662850701072499309L;
  
  private final JSlider widthSlider;
  private final JLabel showLabel;
  private final BufferedImage showImage;

  private static final int ICON_WIDTH = 32;
  private static final int ICON_HEIGHT = 32;

  private final List<ChangeListener> changeListeners = new ArrayList<ChangeListener>();

  private int selectedPenWidth;

  public PenWidth() {
    super();
    setOpaque(false);
    setLayout(new BorderLayout(0, 0));

    showImage = new BufferedImage(ICON_WIDTH, ICON_HEIGHT, BufferedImage.TYPE_INT_RGB);

    widthSlider = new JSlider();
    showLabel = new JLabel(new ImageIcon(showImage));

    add(showLabel, BorderLayout.CENTER);
    add(widthSlider, BorderLayout.SOUTH);

    widthSlider.setModel(this);

    widthSlider.setSize(ICON_WIDTH << 1, 10);

    widthSlider.setFocusable(false);

    setBorder(BorderFactory.createTitledBorder("Tool size"));

    setValue(1);

    setSize(getPreferredSize());
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(96, 86);
  }

  @Override
  public Dimension getMinimumSize() {
    return getPreferredSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  @Override
  public int getMinimum() {
    return 1;
  }

  @Override
  public void setMinimum(int newMinimum) {

  }

  @Override
  public int getMaximum() {
    return 24;
  }

  @Override
  public void setMaximum(int newMaximum) {

  }

  @Override
  public int getValue() {
    return selectedPenWidth;
  }

  @Override
  public void setValue(final int newValue) {
    final int value = Math.min(getMaximum(), Math.max(getMinimum(), newValue));

    selectedPenWidth = value;
    final Graphics gfx = showImage.getGraphics();
    gfx.setColor(Color.white);
    gfx.fillRect(0, 0, ICON_WIDTH, ICON_HEIGHT);
    gfx.setColor(Color.black);
    gfx.fillRect((ICON_WIDTH - selectedPenWidth) / 2, (ICON_HEIGHT - selectedPenWidth) / 2, selectedPenWidth, selectedPenWidth);
    gfx.dispose();

    for (final ChangeListener l : changeListeners) {
      l.stateChanged(new ChangeEvent(this));
    }
    
    repaint();
  }

  @Override
  public void setValueIsAdjusting(boolean b) {

  }

  @Override
  public boolean getValueIsAdjusting() {
    return true;
  }

  @Override
  public int getExtent() {
    return 0;
  }

  @Override
  public void setExtent(int newExtent) {

  }

  @Override
  public void setRangeProperties(int value, int extent, int min, int max, boolean adjusting) {

  }

  @Override
  public void addChangeListener(final ChangeListener l) {
    if (l!=null) {
      this.changeListeners.add(l);
    }
  }

  @Override
  public void removeChangeListener(final ChangeListener l) {
    this.changeListeners.remove(l);
  }
}
