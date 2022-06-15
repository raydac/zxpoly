package com.igormaznitsa.zxpoly;

import com.igormaznitsa.zxpoly.components.video.BorderWidth;
import com.igormaznitsa.zxpoly.components.video.VirtualKeyboardLook;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;

import java.io.File;

import static java.util.Objects.requireNonNull;

public final class MainFormParameters {
  private String title;
  private String appIconPath;
  private String romPath;

  private File openSnapshot;

  private TimingProfile timingProfile;

  private VirtualKeyboardLook virtualKeyboardLook;

  private BorderWidth borderWidth;

  public MainFormParameters() {
  }

  public File getOpenSnapshot() {
    return this.openSnapshot;
  }

  public MainFormParameters setOpenSnapshot(final File file) {
    this.openSnapshot = file;
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
