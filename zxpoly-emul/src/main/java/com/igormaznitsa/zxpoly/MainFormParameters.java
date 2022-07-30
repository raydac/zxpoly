package com.igormaznitsa.zxpoly;

import static java.util.Objects.requireNonNull;

import com.igormaznitsa.zxpoly.components.video.BorderWidth;
import com.igormaznitsa.zxpoly.components.video.VirtualKeyboardLook;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import java.io.File;

public final class MainFormParameters {
  private String title;
  private String appIconPath;
  private String romPath;

  private File openSnapshot;

  private TimingProfile timingProfile;

  private VirtualKeyboardLook virtualKeyboardLook;

  private BorderWidth borderWidth;

  private boolean showMainMenu;

  private boolean undecorated;

  private boolean showIndicatorPanel;

  private Bounds bounds;

  private Bounds keyboardBounds;

  private boolean activateSound;

  private boolean forceAcbChannelSound;

  public MainFormParameters() {
  }

  public boolean isForceAcbChannelSound() {
    return this.forceAcbChannelSound;
  }

  public MainFormParameters setForceAcbChannelSound(final boolean forceAcbChannelSound) {
    this.forceAcbChannelSound = forceAcbChannelSound;
    return this;
  }

  public boolean isActivateSound() {
    return this.activateSound;
  }

  public MainFormParameters setActivateSound(final boolean activateSound) {
    this.activateSound = activateSound;
    return this;
  }

  public Bounds getKeyboardBounds() {
    return this.keyboardBounds;
  }

  public MainFormParameters setKeyboardBounds(final Bounds bounds) {
    this.keyboardBounds = bounds;
    return this;
  }

  public Bounds getBounds() {
    return this.bounds;
  }

  public MainFormParameters setBounds(final Bounds bounds) {
    this.bounds = bounds;
    return this;
  }

  public boolean isUndecorated() {
    return this.undecorated;
  }

  public MainFormParameters setUndecorated(final boolean undecorated) {
    this.undecorated = undecorated;
    return this;
  }

  public File getOpenSnapshot() {
    return this.openSnapshot;
  }

  public MainFormParameters setOpenSnapshot(final File file) {
    this.openSnapshot = file;
    return this;
  }

  public boolean isShowMainMenu() {
    return this.showMainMenu;
  }

  public MainFormParameters setShowMainMenu(final boolean showMainMenu) {
    this.showMainMenu = showMainMenu;
    return this;
  }

  public boolean isShowIndicatorPanel() {
    return this.showIndicatorPanel;
  }

  public MainFormParameters setShowIndicatorPanel(final boolean showIndicatorPanel) {
    this.showIndicatorPanel = showIndicatorPanel;
    return this;
  }

  public VirtualKeyboardLook getVirtualKeyboardLook() {
    return this.virtualKeyboardLook;
  }

  public MainFormParameters setVirtualKeyboardLook(final VirtualKeyboardLook virtualKeyboardLook) {
    this.virtualKeyboardLook = requireNonNull(virtualKeyboardLook);
    return this;
  }

  public TimingProfile getTimingProfile() {
    return this.timingProfile;
  }

  public MainFormParameters setTimingProfile(final TimingProfile timingProfile) {
    this.timingProfile = requireNonNull(timingProfile);
    return this;
  }

  public BorderWidth getBorderWidth() {
    return this.borderWidth;
  }

  public MainFormParameters setBorderWidth(final BorderWidth borderWidth) {
    this.borderWidth = requireNonNull(borderWidth);
    return this;
  }

  public String getTitle() {
    return title;
  }

  public MainFormParameters setTitle(final String value) {
    this.title = value;
    return this;
  }

  public String getAppIconPath() {
    return appIconPath;
  }

  public MainFormParameters setAppIconPath(final String value) {
    this.appIconPath = value;
    return this;
  }

  public String getRomPath() {
    return romPath;
  }

  public MainFormParameters setRomPath(final String value) {
    this.romPath = value;
    return this;
  }
}
