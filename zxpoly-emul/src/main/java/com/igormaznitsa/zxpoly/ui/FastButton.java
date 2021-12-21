package com.igormaznitsa.zxpoly.ui;

import com.igormaznitsa.zxpoly.utils.Utils;

import javax.swing.*;

public enum FastButton {

  VIRTUAL_KEYBOARD("Virtual keyboard", "Show virtual keyboard", "vkbd.png", null, JToggleButton.class),
  START_PAUSE("Play/pause emulation", "Play/Pause emulation", "emul_pause.png", "emul_play.png", JToggleButton.class);

  private final String title;
  private final Class<? extends AbstractButton> buttonClass;
  private final ImageIcon icon;
  private final ImageIcon iconSelected;
  private final String toolTip;

  FastButton(final String title, final String toolTip, final String icon, final String iconSelected, final Class<? extends AbstractButton> buttonClass) {
    if (icon != null) {
      this.icon = new ImageIcon(Utils.loadIcon(icon));
    } else {
      this.icon = null;
    }

    if (iconSelected != null) {
      this.iconSelected = new ImageIcon(Utils.loadIcon(iconSelected));
    } else {
      this.iconSelected = null;
    }

    this.toolTip = toolTip;
    this.title = title;
    this.buttonClass = buttonClass;
  }

  public String getComponentName() {
    return "FAST_BUTTON_" + this.name();
  }

  public String getToolTip() {
    return this.toolTip;
  }

  public ImageIcon getIcon() {
    return this.icon == null ? this.iconSelected : this.icon;
  }

  public ImageIcon getIconSelected() {
    return this.iconSelected == null ? this.icon : this.iconSelected;
  }

  public String getTitle() {
    return this.title;
  }

  public Class<? extends AbstractButton> getButtonClass() {
    return this.buttonClass;
  }
}
