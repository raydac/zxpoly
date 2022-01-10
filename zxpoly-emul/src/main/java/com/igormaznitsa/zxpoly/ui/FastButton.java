package com.igormaznitsa.zxpoly.ui;

import com.igormaznitsa.zxpoly.utils.Utils;

import javax.swing.*;

public enum FastButton {
  RESET(true, "Reset", "Reset device", "fb_reset.png", null, JButton.class),
  MAGIC(true, "Magic", "Make NMI signal for CPU", "fb_magic.png", null, JButton.class),
  SOUND_ON_OFF(true, "Sound On/Off", "Activate/mute sound engine", "fb_sound_off.png", "fb_sound_on.png", JToggleButton.class),
  TAPE_PLAY_STOP(true, "Tape PLAY/STOP", "Tape Play/Stop", "fb_tape.png", null, JToggleButton.class),
  TURBO_MODE(true, "Turbo mode", "Stop delay emulation for CPU", "fb_turbomode.png", null, JToggleButton.class),
  ZX_KEYBOARD_OFF(true, "ZX-Keyboard Off", "Stop emulation of ZX keyboard events with PC keyboard", "fb_zxkbd_disable.png", null, JToggleButton.class),
  VIRTUAL_KEYBOARD(false, "Virtual keyboard", "Show virtual keyboard", "vkbd.png", null, JToggleButton.class),
  START_PAUSE(false, "Play/pause emulation", "Play/Pause emulation", "emul_pause.png", "emul_play.png", JToggleButton.class);

  private final String title;
  private final Class<? extends AbstractButton> buttonClass;
  private final ImageIcon icon;
  private final ImageIcon iconSelected;
  private final String toolTip;
  private final boolean optional;

  FastButton(final boolean optional, final String title, final String toolTip, final String icon, final String iconSelected, final Class<? extends AbstractButton> buttonClass) {
    this.optional = optional;
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

  public static FastButton findForComponentName(final String componentName) {
    if (componentName == null) return null;
    for (final FastButton b : values()) {
      if (b.getComponentName().equals(componentName)) return b;
    }
    return null;
  }

  public boolean isOptional() {
    return this.optional;
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
