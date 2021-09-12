package com.igormaznitsa.zxpoly.ui;

import javax.swing.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class JIndicatorLabel extends JLabel {
  private final Icon active;
  private final Icon inactive;
  private String tooltipActive;
  private String tooltipInactive;

  private final AtomicBoolean status = new AtomicBoolean(false);

  public JIndicatorLabel(final Icon active, final Icon inactive, final String tooltipActive,
                         final String tooltipInactive) {
    super();
    this.active = active;
    this.inactive = inactive;
    this.tooltipActive = tooltipActive;
    this.tooltipInactive = tooltipInactive;
    updateForState();
  }

  public void setTooltips(final String active, final String inactive) {
    this.tooltipActive = active;
    this.tooltipInactive = inactive;
    updateForState();
  }

  private void updateForState() {
    final boolean stateActive = this.status.get();
    this.setIcon(stateActive ? this.active : this.inactive);
    this.setToolTipText(stateActive ? this.tooltipActive : this.tooltipInactive);
  }

  public boolean getStatus() {
    return this.status.get();
  }

  public void setStatus(final boolean active) {
    if (this.status.compareAndSet(!active, active)) {
      SwingUtilities.invokeLater(() -> updateForState());
    }
  }
}
