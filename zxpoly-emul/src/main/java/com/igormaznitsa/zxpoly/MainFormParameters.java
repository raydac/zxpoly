package com.igormaznitsa.zxpoly;

import com.igormaznitsa.zxpoly.components.BoardMode;
import com.igormaznitsa.zxpoly.components.sound.VolumeProfile;
import com.igormaznitsa.zxpoly.components.video.BorderWidth;
import com.igormaznitsa.zxpoly.components.video.VirtualKeyboardLook;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import java.io.File;

public final class MainFormParameters {
  private String title;
  private String appIconPath;
  private String romPath;

  private File openSnapshot;

  private File preferencesFile;

  private TimingProfile timingProfile;

  private VirtualKeyboardLook virtualKeyboardLook;

  private BorderWidth borderWidth;

  private Boolean showMainMenu;

  private Boolean undecorated;

  private Boolean showIndicatorPanel;

  private Bounds bounds;

  private Bounds keyboardBounds;

  private Boolean activateSound;
  private Boolean syncRepaint;

  private Boolean forceAcbChannelSound;

  private Boolean tryUseLessSystemResources;

  private Boolean turboSound;
  private Boolean covoxFb;
  private Boolean allowKempstonMouse;
  private Boolean attributePortFf;
  private Boolean ulaPlus;
  private Boolean interlaceScan;

  private BoardMode boardMode;
  private VolumeProfile volumeProfile;

  public MainFormParameters() {
  }

  public static <T> T requireNonNullElseEvenNull(T obj, T defaultObj) {
    return (obj != null) ? obj : defaultObj;
  }

  public Boolean isInterlaceScan(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.interlaceScan, defaultValue);
  }

  public MainFormParameters setInterlaceScan(final Boolean value) {
    this.interlaceScan = value;
    return this;
  }

  public VolumeProfile getVolumeProfile(final VolumeProfile defaultValue) {
    return requireNonNullElseEvenNull(this.volumeProfile, defaultValue);
  }

  public MainFormParameters setVolumeProfile(final VolumeProfile value) {
    this.volumeProfile = value;
    return this;
  }

  public Boolean isTurboSound(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.turboSound, defaultValue);
  }

  public MainFormParameters setTurboSound(final Boolean value) {
    this.turboSound = value;
    return this;
  }

  public boolean isCovoxFb(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.covoxFb, defaultValue);
  }

  public MainFormParameters setCovoxFb(final Boolean value) {
    this.covoxFb = value;
    return this;
  }

  public boolean isAllowKempstonMouse(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.allowKempstonMouse, defaultValue);
  }

  public MainFormParameters setAllowKempstonMouse(final Boolean value) {
    this.allowKempstonMouse = value;
    return this;
  }

  public BoardMode getBoardMode(final BoardMode defaultValue) {
    return requireNonNullElseEvenNull(this.boardMode, defaultValue);
  }

  public MainFormParameters setBoardMode(final BoardMode value) {
    this.boardMode = value;
    return this;
  }

  public boolean isUlaPlus(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.ulaPlus, defaultValue);
  }

  public MainFormParameters setUlaPlus(final Boolean value) {
    this.ulaPlus = value;
    return this;
  }

  public boolean isAttributePortFf(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.attributePortFf, defaultValue);
  }

  public MainFormParameters setAttributePortFf(final Boolean value) {
    this.attributePortFf = value;
    return this;
  }

  public Boolean isSyncRepaint(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.syncRepaint, defaultValue);
  }

  public MainFormParameters setSyncRepaint(final Boolean value) {
    this.syncRepaint = value;
    return this;
  }

  public File getPreferencesFile(final File defaultValue) {
    return requireNonNullElseEvenNull(this.preferencesFile, defaultValue);
  }

  public MainFormParameters setPreferencesFile(final File value) {
    this.preferencesFile = value;
    return this;
  }

  public Boolean isTryUseLessSystemResources(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.tryUseLessSystemResources, defaultValue);
  }

  public MainFormParameters setTryUseLessSystemResources(final Boolean value) {
    this.tryUseLessSystemResources = value;
    return this;
  }

  public Boolean isForceAcbChannelSound(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.forceAcbChannelSound, defaultValue);
  }

  public MainFormParameters setForceAcbChannelSound(final Boolean forceAcbChannelSound) {
    this.forceAcbChannelSound = forceAcbChannelSound;
    return this;
  }

  public boolean isActivateSound(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.activateSound, defaultValue);
  }

  public MainFormParameters setActivateSound(final Boolean activateSound) {
    this.activateSound = activateSound;
    return this;
  }

  public Bounds getKeyboardBounds(final Bounds defaultValue) {
    return requireNonNullElseEvenNull(this.keyboardBounds, defaultValue);
  }

  public MainFormParameters setKeyboardBounds(final Bounds bounds) {
    this.keyboardBounds = bounds;
    return this;
  }

  public Bounds getBounds(final Bounds defaultValue) {
    return requireNonNullElseEvenNull(this.bounds, defaultValue);
  }

  public MainFormParameters setBounds(final Bounds bounds) {
    this.bounds = bounds;
    return this;
  }

  public Boolean isUndecorated(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.undecorated, defaultValue);
  }

  public MainFormParameters setUndecorated(final Boolean undecorated) {
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

  public boolean isShowMainMenu(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.showMainMenu, defaultValue);
  }

  public MainFormParameters setShowMainMenu(final Boolean showMainMenu) {
    this.showMainMenu = showMainMenu;
    return this;
  }

  public boolean isShowIndicatorPanel(final boolean defaultValue) {
    return requireNonNullElseEvenNull(this.showIndicatorPanel, defaultValue);
  }

  public MainFormParameters setShowIndicatorPanel(final Boolean showIndicatorPanel) {
    this.showIndicatorPanel = showIndicatorPanel;
    return this;
  }

  public VirtualKeyboardLook getVirtualKeyboardLook(final VirtualKeyboardLook defaultValue) {
    return requireNonNullElseEvenNull(this.virtualKeyboardLook, defaultValue);
  }

  public MainFormParameters setVirtualKeyboardLook(final VirtualKeyboardLook value) {
    this.virtualKeyboardLook = value;
    return this;
  }

  public TimingProfile getTimingProfile(final TimingProfile defaultValue) {
    return requireNonNullElseEvenNull(this.timingProfile, defaultValue);
  }

  public MainFormParameters setTimingProfile(final TimingProfile value) {
    this.timingProfile = value;
    return this;
  }

  public BorderWidth getBorderWidth(final BorderWidth defaultValue) {
    return requireNonNullElseEvenNull(this.borderWidth, defaultValue);
  }

  public MainFormParameters setBorderWidth(final BorderWidth value) {
    this.borderWidth = value;
    return this;
  }

  public String getTitle(final String defaultValue) {
    return requireNonNullElseEvenNull(this.title, defaultValue);
  }

  public MainFormParameters setTitle(final String value) {
    this.title = value;
    return this;
  }

  public String getAppIconPath(final String defaultValue) {
    return requireNonNullElseEvenNull(this.appIconPath, defaultValue);
  }

  public MainFormParameters setAppIconPath(final String value) {
    this.appIconPath = value;
    return this;
  }

  public String getRomPath(final String defaultValue) {
    return requireNonNullElseEvenNull(this.romPath, defaultValue);
  }

  public MainFormParameters setRomPath(final String value) {
    this.romPath = value;
    return this;
  }
}
