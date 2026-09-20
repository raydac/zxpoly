package com.igormaznitsa.zxpoly.ui;

import static java.util.Objects.requireNonNull;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.function.IntFunction;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;

public class CustomIntSlider extends JPanel {
  private final JSlider slider;
  private final JLabel labelMinIndicator;
  private final JLabel labelMaxIndicator;
  private final JLabel labelIndicator;
  private final IntFunction<String> valueLabel;

  public CustomIntSlider(
      final int min,
      final int max,
      final String minLabel,
      final String maxLabel,
      final IntFunction<String> valueLabel) {
    super(new GridBagLayout());

    this.valueLabel = requireNonNull(valueLabel);

    this.slider = new JSlider(JSlider.HORIZONTAL, min, max, min);
    this.slider.setPaintTrack(true);
    this.slider.setPaintLabels(false);
    this.slider.setMajorTickSpacing(Math.max(1, (max - min) / 4));
    this.slider.setPaintTicks(true);

    this.labelMinIndicator = new JLabel(requireNonNull(minLabel));
    this.labelMinIndicator.setHorizontalAlignment(JLabel.LEFT);

    this.labelIndicator = new JLabel(this.valueLabel.apply(min));
    this.labelIndicator.setHorizontalAlignment(JLabel.CENTER);

    this.labelMaxIndicator = new JLabel(requireNonNull(maxLabel));
    this.labelMaxIndicator.setHorizontalAlignment(JLabel.RIGHT);

    final GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.weightx = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    this.add(this.labelMinIndicator, gridBagConstraints);

    gridBagConstraints.anchor = GridBagConstraints.CENTER;
    gridBagConstraints.gridx = 1;
    gridBagConstraints.weightx = 1000;
    this.add(this.labelIndicator, gridBagConstraints);

    gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.gridx = 2;
    gridBagConstraints.weightx = 1;
    this.add(this.labelMaxIndicator, gridBagConstraints);

    gridBagConstraints.anchor = GridBagConstraints.CENTER;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.weightx = 1000;
    this.add(this.slider, gridBagConstraints);

    this.slider.addChangeListener(e -> this.labelIndicator.setText(
        "<html><b>" + this.valueLabel.apply(this.slider.getValue()) + "</b></html>"));
  }

  @Override
  public void setEnabled(final boolean value) {
    super.setEnabled(value);

    this.labelMinIndicator.setEnabled(value);
    this.labelMaxIndicator.setEnabled(value);
    this.labelIndicator.setEnabled(value);
    this.slider.setEnabled(value);
  }

  @Override
  public void setToolTipText(final String text) {
    super.setToolTipText(text);
    this.slider.setToolTipText(text);
    this.labelMinIndicator.setToolTipText(text);
    this.labelMaxIndicator.setToolTipText(text);
    this.labelIndicator.setToolTipText(text);
  }

  public int getValue() {
    return this.slider.getValue();
  }

  public void setValue(final int value) {
    this.slider.setValue(value);
  }

}
