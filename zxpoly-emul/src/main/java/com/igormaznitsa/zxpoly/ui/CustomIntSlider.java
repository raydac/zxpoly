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
  private final IntFunction<Double> convertingFunction;
  private double value;

  public CustomIntSlider(
      final int min,
      final int max,
      final IntFunction<Double> convertingFunction) {
    super(new GridBagLayout());

    this.convertingFunction = requireNonNull(convertingFunction);

    this.slider = new JSlider(JSlider.HORIZONTAL, min, max, min);
    this.slider.setPaintTrack(true);
    this.slider.setPaintLabels(false);
    this.slider.setPaintTicks(false);

    this.labelIndicator = new JLabel(this.valueAsText(min));

    final GridBagConstraints gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.weightx = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;

    this.labelMinIndicator = new JLabel(valueAsText(min));
    this.labelMinIndicator.setHorizontalAlignment(JLabel.LEFT);
    this.add(this.labelMinIndicator, gridBagConstraints);

    gridBagConstraints.anchor = GridBagConstraints.CENTER;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridx = 1;
    gridBagConstraints.weightx = 1000;

    this.labelIndicator.setHorizontalAlignment(JLabel.CENTER);
    this.add(this.labelIndicator, gridBagConstraints);

    gridBagConstraints.anchor = GridBagConstraints.EAST;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridx = 2;
    gridBagConstraints.weightx = 1;

    this.labelMaxIndicator = new JLabel(valueAsText(max));
    this.labelMinIndicator.setHorizontalAlignment(JLabel.RIGHT);
    this.add(this.labelMaxIndicator, gridBagConstraints);

    gridBagConstraints.anchor = GridBagConstraints.CENTER;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.weightx = 1000;

    this.add(this.slider, gridBagConstraints);

    this.slider.addChangeListener(e -> {
      this.labelIndicator.setText(
          "<html><b>" + this.valueAsText(this.slider.getValue()) + "</b></html>");
    });
  }

  @Override
  public void setEnabled(final boolean value) {
    super.setEnabled(value);

    this.labelMinIndicator.setEnabled(value);
    this.labelMaxIndicator.setEnabled(value);
    this.labelIndicator.setEnabled(value);
    this.slider.setEnabled(value);
  }

  public int getValue() {
    return this.slider.getValue();
  }

  public void setValue(final int value) {
    this.slider.setValue(value);
  }

  private String valueAsText(final int value) {
    final double doubleValue = this.convertingFunction.apply(value);
    return String.format("%.3f", doubleValue);
  }

}
