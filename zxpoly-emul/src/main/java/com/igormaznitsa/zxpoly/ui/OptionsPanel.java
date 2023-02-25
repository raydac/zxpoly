/*
 * Copyright (C) 2020 Igor Maznitsa
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

package com.igormaznitsa.zxpoly.ui;

import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createTitledBorder;

import com.igormaznitsa.zxpoly.components.BoardMode;
import com.igormaznitsa.zxpoly.components.snd.VolumeProfile;
import com.igormaznitsa.zxpoly.components.video.BorderWidth;
import com.igormaznitsa.zxpoly.components.video.VirtualKeyboardLook;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import com.igormaznitsa.zxpoly.utils.AppOptions;
import com.igormaznitsa.zxpoly.utils.RomSource;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

public class OptionsPanel extends JTabbedPane {

  private static final Logger LOGGER = Logger.getLogger(OptionsPanel.class.getName());

  private JCheckBox checkCovoxFb;
  private JCheckBox checkUlaPlus;
  private JCheckBox checkTurboSound;
  private JCheckBox checkZx128ByDefault;
  private JCheckBox checkKempstonMouseAllowed;
  private JCheckBox checkOldTvFilter;
  private JLabel labelInterlacedScan;
  private JLabel labelSyncPaint;
  private JLabel labelOldTvFilter;
  private JLabel labelCovoxFb;
  private JLabel labelUlaPlus;
  private JLabel labelVolumeProfile;
  private JLabel labelSoundSchemeACB;
  private JLabel labelTurboSound;
  private JLabel labelZx128ByDefault;
  private JLabel labelKempstonMouseAllowed;
  private JLabel labelVirtualKbdApart;
  private JLabel labelVirtualKbdLook;
  private JLabel labelCustomRomPath;
  private JLabel labelMacroCursorKeys;
  private JLabel labelTimingProfile;
  private JLabel labelTryLessResources;
  private JLabel labelBorderWidth;
  private JLabel labelEmulateFFport;
  private JCheckBox checkGrabSound;
  private JCheckBox checkTryLessResources;
  private JCheckBox checkInterlacedScan;
  private JCheckBox checkSoundSchemeACB;
  private JCheckBox checkSyncPaint;
  private JCheckBox checkVkbdApart;
  private JCheckBox checkAutoiCsForCursorKeys;
  private JCheckBox checkEmulateFFport;
  private JComboBox<String> comboNetAdddr;
  private JComboBox<String> comboRomSource;
  private JComboBox<VirtualKeyboardLook> comboKeyboardLook;
  private JComboBox<VolumeProfile> comboVolumeProfile;
  private JComboBox<TimingProfile> comboTimingProfile;
  private JComboBox<BorderWidth> comboBorderWidth;
  private JLabel labelFfMpegPath;
  private JLabel labelNetInterface;
  private JLabel labelPort;
  private JLabel labelSound;
  private JLabel labelRomSource;
  private JLabel labelIntFrame;
  private JLabel labelFrameRate;
  private JSpinner spinnerFramesPerSec;
  private JSpinner spinnerIntFrame;
  private JSpinner spinnerPort;
  private JTextField textFfmpegPath;
  private JFilePathTextField textCustomRomPath;

  private KeyCodeChooser keySelectorKempstonLeft;
  private KeyCodeChooser keySelectorKempstonRight;
  private KeyCodeChooser keySelectorKempstonUp;
  private KeyCodeChooser keySelectorKempstonDown;
  private KeyCodeChooser keySelectorKempstonFire;

  private KeyCodeChooser keySelectorProtekJoystickLeft;
  private KeyCodeChooser keySelectorProtekJoystickRight;
  private KeyCodeChooser keySelectorProtekJoystickUp;
  private KeyCodeChooser keySelectorProtekJoystickDown;
  private KeyCodeChooser keySelectorProtekJoystickFire;

  public OptionsPanel(final DataContainer dataContainer) {
    super();

    initComponents();
    this.comboRomSource.removeAllItems();
    Arrays.stream(RomSource.values()).filter(x -> x != RomSource.UNKNOWN).forEach(x -> this.comboRomSource.addItem(x.getTitle()));

    final List<String> addressList = new ArrayList<>();

    try {
      final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
      while (interfaces.hasMoreElements()) {
        NetworkInterface next = interfaces.nextElement();
        final Enumeration<InetAddress> addresses = next.getInetAddresses();
        while (addresses.hasMoreElements()) {
          final InetAddress addr = addresses.nextElement();
          addressList.add(addr.getHostAddress());
        }
      }
    } catch (Exception ex) {
      LOGGER.warning("Can't form network adapter address list for error: " + ex.getMessage());
    }

    Collections.sort(addressList);

    final String loopBackAddress = InetAddress.getLoopbackAddress().getHostAddress();
    if (!addressList.contains(loopBackAddress)) {
      addressList.add(0, loopBackAddress);
    }
    this.comboNetAdddr.removeAllItems();
    addressList.forEach(x -> this.comboNetAdddr.addItem(x));

    this.fillByDataContainer(dataContainer == null ? new DataContainer() : dataContainer);

    this.comboRomSource.addActionListener(e -> {
      if (RomSource.TEST.getTitle().equals(this.comboRomSource.getSelectedItem())) {
        this.checkZx128ByDefault.setSelected(false);
      }
    });
  }

  private void fillByDataContainer(final DataContainer data) {
    this.comboTimingProfile.setSelectedItem(data.timingProfile);
    this.checkEmulateFFport.setSelected(data.emulateFFport);
    this.checkInterlacedScan.setSelected(data.interlacedScan);
    this.checkSyncPaint.setSelected(data.syncPaint);
    this.checkOldTvFilter.setSelected(data.oldTvFilter);
    this.textCustomRomPath.setText(data.customRomPath);
    this.checkSoundSchemeACB.setSelected(data.soundSchemeAcb);
    this.checkGrabSound.setSelected(data.grabSound);
    this.checkVkbdApart.setSelected(data.vkdApart);
    this.checkCovoxFb.setSelected(data.covoxFb);
    this.checkUlaPlus.setSelected(data.ulaPlus);
    this.checkZx128ByDefault.setSelected(data.zx128byDefault);
    this.checkTurboSound.setSelected(data.turboSound);
    this.checkKempstonMouseAllowed.setSelected(data.kempstonMouseAllowed);
    this.spinnerPort.setValue(data.port);
    this.spinnerIntFrame.setValue(data.intPerFrame);
    this.textFfmpegPath.setText(data.ffmpegPath);
    this.comboNetAdddr.setSelectedItem(data.inetAddress);
    this.spinnerFramesPerSec.setValue(data.frameRate);
    this.comboRomSource.setSelectedItem(RomSource.findForLink(data.activeRom, RomSource.TEST).getTitle());
    this.comboKeyboardLook.setSelectedItem(data.keyboardLook);
    this.comboVolumeProfile.setSelectedItem(data.volumeProfile);
    this.comboBorderWidth.setSelectedItem(data.borderWidth);
    this.checkAutoiCsForCursorKeys.setSelected(data.autoCsForCursorKeys);

    this.keySelectorKempstonFire.setKey(data.kempstonKeyFire);
    this.keySelectorKempstonRight.setKey(data.kempstonKeyRight);
    this.keySelectorKempstonLeft.setKey(data.kempstonKeyLeft);
    this.keySelectorKempstonUp.setKey(data.kempstonKeyUp);
    this.keySelectorKempstonDown.setKey(data.kempstonKeyDown);

    this.keySelectorProtekJoystickUp.setKey(data.protekJoystickKeyUp);
    this.keySelectorProtekJoystickFire.setKey(data.protekJoystickKeyFire);
    this.keySelectorProtekJoystickRight.setKey(data.protekJoystickKeyRight);
    this.keySelectorProtekJoystickLeft.setKey(data.protekJoystickKeyLeft);
    this.keySelectorProtekJoystickDown.setKey(data.protekJoystickKeyDown);
  }

  private void initComponents() {
    GridBagConstraints gridBagConstraints;

    labelInterlacedScan = new JLabel();
    labelOldTvFilter = new JLabel();
    labelTimingProfile = new JLabel();
    labelTryLessResources = new JLabel();
    labelBorderWidth = new JLabel();
    checkInterlacedScan = new JCheckBox();
    labelFfMpegPath = new JLabel();
    labelNetInterface = new JLabel();
    textFfmpegPath = new JTextField();
    comboNetAdddr = new JComboBox<>();
    labelPort = new JLabel();
    spinnerPort = new JSpinner();
    labelSound = new JLabel();
    labelVolumeProfile = new JLabel();
    checkGrabSound = new JCheckBox();
    labelSoundSchemeACB = new JLabel();
    checkSoundSchemeACB = new JCheckBox();
    labelFrameRate = new JLabel();
    spinnerFramesPerSec = new JSpinner();
    labelRomSource = new JLabel();
    labelIntFrame = new JLabel();
    labelSyncPaint = new JLabel();
    comboRomSource = new JComboBox<>();
    spinnerIntFrame = new JSpinner();
    labelCovoxFb = new JLabel();
    checkCovoxFb = new JCheckBox();
    labelUlaPlus = new JLabel();
    checkUlaPlus = new JCheckBox();
    labelTurboSound = new JLabel();
    checkTurboSound = new JCheckBox();
    labelZx128ByDefault = new JLabel();
    checkZx128ByDefault = new JCheckBox();
    labelKempstonMouseAllowed = new JLabel();
    labelVirtualKbdApart = new JLabel();
    labelVirtualKbdLook = new JLabel();
    labelCustomRomPath = new JLabel();
    labelMacroCursorKeys = new JLabel();
    checkKempstonMouseAllowed = new JCheckBox();
    checkAutoiCsForCursorKeys = new JCheckBox();
    checkSyncPaint = new JCheckBox();
    checkOldTvFilter = new JCheckBox();
    checkTryLessResources = new JCheckBox();
    checkVkbdApart = new JCheckBox();
    comboKeyboardLook = new JComboBox<>(VirtualKeyboardLook.values());
    comboVolumeProfile = new JComboBox<>(VolumeProfile.values());
    comboTimingProfile = new JComboBox<>(TimingProfile.values());
    comboBorderWidth = new JComboBox<>(BorderWidth.values());
    textCustomRomPath = new JFilePathTextField();
    textCustomRomPath.setToolTipText("Provided file path overrides selected ROM, if empty then inactive");
    labelEmulateFFport = new JLabel();
    checkEmulateFFport = new JCheckBox();

    keySelectorKempstonDown = new KeyCodeChooser();
    keySelectorKempstonLeft = new KeyCodeChooser();
    keySelectorKempstonUp = new KeyCodeChooser();
    keySelectorKempstonRight = new KeyCodeChooser();
    keySelectorKempstonFire = new KeyCodeChooser();

    keySelectorProtekJoystickDown = new KeyCodeChooser();
    keySelectorProtekJoystickLeft = new KeyCodeChooser();
    keySelectorProtekJoystickRight = new KeyCodeChooser();
    keySelectorProtekJoystickFire = new KeyCodeChooser();
    keySelectorProtekJoystickUp = new KeyCodeChooser();

    final JPanel panelGeneral = new JPanel();
    final JPanel panelStreaming = new JPanel();
    final JPanel panelScreen = new JPanel();

    panelScreen.setLayout(new GridBagLayout());

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.EAST;

    labelIntFrame.setHorizontalAlignment(RIGHT);
    labelIntFrame.setText("INT/Frame:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelScreen.add(labelIntFrame, gridBagConstraints);

    spinnerIntFrame.setModel(new javax.swing.SpinnerNumberModel(1, 1, 100, 1));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelScreen.add(spinnerIntFrame, gridBagConstraints);

    labelSyncPaint.setHorizontalAlignment(RIGHT);
    labelSyncPaint.setText("Sync.paint (can be slow):");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelScreen.add(labelSyncPaint, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelScreen.add(checkSyncPaint, gridBagConstraints);

    labelInterlacedScan.setHorizontalAlignment(RIGHT);
    labelInterlacedScan.setText("Interlaced scan:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelScreen.add(labelInterlacedScan, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelScreen.add(checkInterlacedScan, gridBagConstraints);

    labelOldTvFilter.setHorizontalAlignment(RIGHT);
    labelOldTvFilter.setText("Default Old color TV:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelScreen.add(labelOldTvFilter, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelScreen.add(checkOldTvFilter, gridBagConstraints);

    labelBorderWidth.setHorizontalAlignment(RIGHT);
    labelBorderWidth.setText("Border width:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelScreen.add(labelBorderWidth, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelScreen.add(comboBorderWidth, gridBagConstraints);

    labelEmulateFFport.setHorizontalAlignment(RIGHT);
    labelEmulateFFport.setText("Port FF emulation:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelScreen.add(labelEmulateFFport, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelScreen.add(checkEmulateFFport, gridBagConstraints);

    labelUlaPlus.setHorizontalAlignment(RIGHT);
    labelUlaPlus.setText("ULA Plus emulation:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelScreen.add(labelUlaPlus, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelScreen.add(checkUlaPlus, gridBagConstraints);

    panelStreaming.setLayout(new GridBagLayout());

    labelFfMpegPath.setHorizontalAlignment(RIGHT);
    labelFfMpegPath.setText("FFmpeg path:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    panelStreaming.add(labelFfMpegPath, gridBagConstraints);

    textFfmpegPath.setColumns(24);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelStreaming.add(textFfmpegPath, gridBagConstraints);

    labelNetInterface.setHorizontalAlignment(RIGHT);
    labelNetInterface.setText("Net.interface:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    panelStreaming.add(labelNetInterface, gridBagConstraints);

    comboNetAdddr.setModel(new javax.swing.DefaultComboBoxModel<>());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelStreaming.add(comboNetAdddr, gridBagConstraints);

    labelPort.setHorizontalAlignment(RIGHT);
    labelPort.setText("Port:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    panelStreaming.add(labelPort, gridBagConstraints);

    spinnerPort.setModel(new javax.swing.SpinnerNumberModel(0, 0, 65535, 1));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelStreaming.add(spinnerPort, gridBagConstraints);

    labelSound.setHorizontalAlignment(RIGHT);
    labelSound.setText("Sound:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    panelStreaming.add(labelSound, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelStreaming.add(checkGrabSound, gridBagConstraints);

    labelFrameRate.setHorizontalAlignment(RIGHT);
    labelFrameRate.setText("Frame rate:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelStreaming.add(labelFrameRate, gridBagConstraints);

    spinnerFramesPerSec.setModel(new javax.swing.SpinnerNumberModel(25, 1, 50, 1));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelStreaming.add(spinnerFramesPerSec, gridBagConstraints);

    panelGeneral.setLayout(new GridBagLayout());

    labelRomSource.setHorizontalAlignment(RIGHT);
    labelRomSource.setText("Initial ROM load source:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelRomSource, gridBagConstraints);

    comboRomSource.setModel(new javax.swing.DefaultComboBoxModel<>());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(comboRomSource, gridBagConstraints);

    labelCustomRomPath.setHorizontalAlignment(RIGHT);
    labelCustomRomPath.setText("Custom ROM file:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelCustomRomPath, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    textCustomRomPath.setColumns(24);
    panelGeneral.add(textCustomRomPath, gridBagConstraints);

    labelTurboSound.setHorizontalAlignment(RIGHT);
    labelTurboSound.setText("TurboSound (NedoPC):");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelTurboSound, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(checkTurboSound, gridBagConstraints);

    labelCovoxFb.setHorizontalAlignment(RIGHT);
    labelCovoxFb.setText("Covox (#FB):");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelCovoxFb, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(checkCovoxFb, gridBagConstraints);

    labelSoundSchemeACB.setHorizontalAlignment(RIGHT);
    labelSoundSchemeACB.setText("Sound channels ACB:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelSoundSchemeACB, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(checkSoundSchemeACB, gridBagConstraints);

    labelVolumeProfile.setHorizontalAlignment(RIGHT);
    labelVolumeProfile.setText("Volume profile:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelVolumeProfile, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(comboVolumeProfile, gridBagConstraints);

    labelZx128ByDefault.setHorizontalAlignment(RIGHT);
    labelZx128ByDefault.setText("Default ZX Mode:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelZx128ByDefault, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(checkZx128ByDefault, gridBagConstraints);

    labelKempstonMouseAllowed.setHorizontalAlignment(RIGHT);
    labelKempstonMouseAllowed.setText("Kempston mouse allowed:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelKempstonMouseAllowed, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(checkKempstonMouseAllowed, gridBagConstraints);

    labelVirtualKbdApart.setHorizontalAlignment(RIGHT);
    labelVirtualKbdApart.setText("Virtual keyboard apart:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelVirtualKbdApart, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(checkVkbdApart, gridBagConstraints);

    labelVirtualKbdLook.setHorizontalAlignment(RIGHT);
    labelVirtualKbdLook.setText("Keyboard decoration:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelVirtualKbdLook, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 9;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(comboKeyboardLook, gridBagConstraints);

    labelMacroCursorKeys.setHorizontalAlignment(RIGHT);
    labelMacroCursorKeys.setText("Auto-CS for cursor keys:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelMacroCursorKeys, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 10;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(checkAutoiCsForCursorKeys, gridBagConstraints);

    labelTimingProfile.setHorizontalAlignment(RIGHT);
    labelTimingProfile.setText("Timing:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelTimingProfile, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 11;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(comboTimingProfile, gridBagConstraints);

    labelTryLessResources.setHorizontalAlignment(RIGHT);
    labelTryLessResources.setText("Try use less resources:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGeneral.add(labelTryLessResources, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 12;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGeneral.add(checkTryLessResources, gridBagConstraints);

    final JPanel panelKempston = new JPanel(new GridBagLayout());
    panelKempston.setBorder(createTitledBorder("Kempston joystick"));

    final GridBagConstraints gbx = new GridBagConstraints();
    gbx.gridx = 0;
    gbx.gridy = 0;
    gbx.fill = GridBagConstraints.HORIZONTAL;
    gbx.anchor = GridBagConstraints.WEST;

    JLabel kempstonLabel = new JLabel("LEFT:");
    kempstonLabel.setHorizontalAlignment(RIGHT);
    panelKempston.add(kempstonLabel, gbx);
    gbx.gridx = 1;
    panelKempston.add(this.keySelectorKempstonLeft, gbx);

    gbx.gridy = 1;
    gbx.gridx = 0;
    kempstonLabel = new JLabel("RIGHT:");
    kempstonLabel.setHorizontalAlignment(RIGHT);
    panelKempston.add(kempstonLabel, gbx);
    gbx.gridx = 1;
    panelKempston.add(this.keySelectorKempstonRight, gbx);

    gbx.gridy = 2;
    gbx.gridx = 0;
    kempstonLabel = new JLabel("DOWN:");
    kempstonLabel.setHorizontalAlignment(RIGHT);
    panelKempston.add(kempstonLabel, gbx);
    gbx.gridx = 1;
    panelKempston.add(this.keySelectorKempstonDown, gbx);

    gbx.gridy = 3;
    gbx.gridx = 0;
    kempstonLabel = new JLabel("UP:");
    kempstonLabel.setHorizontalAlignment(RIGHT);
    panelKempston.add(kempstonLabel, gbx);
    gbx.gridx = 1;
    panelKempston.add(this.keySelectorKempstonUp, gbx);


    gbx.gridy = 4;
    gbx.gridx = 0;
    kempstonLabel = new JLabel("FIRE:");
    kempstonLabel.setHorizontalAlignment(RIGHT);
    panelKempston.add(kempstonLabel, gbx);
    gbx.gridx = 1;
    panelKempston.add(this.keySelectorKempstonFire, gbx);

    final JPanel panelCursor = new JPanel(new GridBagLayout());
    panelCursor.setBorder(createTitledBorder("Protek joystick"));

    gbx.gridx = 0;
    gbx.gridy = 0;
    gbx.fill = GridBagConstraints.HORIZONTAL;
    gbx.anchor = GridBagConstraints.WEST;

    JLabel cursorLabel = new JLabel("LEFT:");
    cursorLabel.setHorizontalAlignment(RIGHT);
    panelCursor.add(cursorLabel, gbx);
    gbx.gridx = 1;
    panelCursor.add(this.keySelectorProtekJoystickLeft, gbx);

    gbx.gridy = 1;
    gbx.gridx = 0;
    cursorLabel = new JLabel("RIGHT:");
    cursorLabel.setHorizontalAlignment(RIGHT);
    panelCursor.add(cursorLabel, gbx);
    gbx.gridx = 1;
    panelCursor.add(this.keySelectorProtekJoystickRight, gbx);

    gbx.gridy = 2;
    gbx.gridx = 0;
    cursorLabel = new JLabel("DOWN:");
    cursorLabel.setHorizontalAlignment(RIGHT);
    panelCursor.add(cursorLabel, gbx);
    gbx.gridx = 1;
    panelCursor.add(this.keySelectorProtekJoystickDown, gbx);

    gbx.gridy = 3;
    gbx.gridx = 0;
    cursorLabel = new JLabel("UP:");
    cursorLabel.setHorizontalAlignment(RIGHT);
    panelCursor.add(cursorLabel, gbx);
    gbx.gridx = 1;
    panelCursor.add(this.keySelectorProtekJoystickUp, gbx);

    gbx.gridy = 4;
    gbx.gridx = 0;
    cursorLabel = new JLabel("FIRE:");
    cursorLabel.setHorizontalAlignment(RIGHT);
    panelCursor.add(cursorLabel, gbx);
    gbx.gridx = 1;
    panelCursor.add(this.keySelectorProtekJoystickFire, gbx);

    final JPanel joysticksPanel = new JPanel(new GridLayout(1, 2));
    joysticksPanel.add(panelKempston);
    joysticksPanel.add(panelCursor);

    this.addTab("General", panelGeneral);
    this.addTab("Screen", panelScreen);
    this.addTab("Joystick", joysticksPanel);
    this.addTab("Streaming", panelStreaming);
  }

  public DataContainer getData() {
    return new DataContainer(this);
  }

  public static final class DataContainer {

    public final String customRomPath;
    public final String ffmpegPath;
    public final String inetAddress;
    public final TimingProfile timingProfile;
    public final BorderWidth borderWidth;
    public final VirtualKeyboardLook keyboardLook;
    public final VolumeProfile volumeProfile;
    public final int port;
    public final boolean grabSound;
    public final String activeRom;
    public final int intPerFrame;
    public final int frameRate;
    public final boolean covoxFb;
    public final boolean ulaPlus;
    public final boolean turboSound;
    public final boolean kempstonMouseAllowed;
    public final boolean zx128byDefault;
    public final boolean vkdApart;
    public final boolean autoCsForCursorKeys;
    public final boolean interlacedScan;
    public final boolean tryLessResources;
    public final boolean syncPaint;
    public final boolean oldTvFilter;
    public final boolean emulateFFport;

    public final int kempstonKeyUp;
    public final int kempstonKeyDown;
    public final int kempstonKeyLeft;
    public final int kempstonKeyRight;
    public final int kempstonKeyFire;

    public final int protekJoystickKeyUp;
    public final int protekJoystickKeyDown;
    public final int protekJoystickKeyLeft;
    public final int protekJoystickKeyRight;
    public final int protekJoystickKeyFire;

    public final boolean soundSchemeAcb;

    public DataContainer() {
      final String customRomPath = AppOptions.getInstance().getCustomRomPath();
      this.borderWidth = AppOptions.getInstance().getBorderWidth();
      this.syncPaint = AppOptions.getInstance().isSyncPaint();
      this.emulateFFport = AppOptions.getInstance().isAttributePortFf();
      this.timingProfile = AppOptions.getInstance().getTimingProfile();
      this.customRomPath = customRomPath == null ? "" : customRomPath;
      this.interlacedScan = AppOptions.getInstance().isInterlacedScan();
      this.tryLessResources = AppOptions.getInstance().isTryLessResources();
      this.oldTvFilter = AppOptions.getInstance().isOldColorTvOnStart();
      this.soundSchemeAcb = AppOptions.getInstance().isSoundChannelsACB();
      this.autoCsForCursorKeys = AppOptions.getInstance().getAutoCsForCursorKeys();
      this.keyboardLook = AppOptions.getInstance().getKeyboardLook();
      this.vkdApart = AppOptions.getInstance().isVkbdApart();
      this.activeRom = AppOptions.getInstance().getActiveRom();
      this.intPerFrame = AppOptions.getInstance().getIntBetweenFrames();
      this.port = AppOptions.getInstance().getPort();
      this.volumeProfile = AppOptions.getInstance().getVolumeProfile();
      this.inetAddress = AppOptions.getInstance().getAddress();
      this.grabSound = AppOptions.getInstance().isGrabSound();
      this.ffmpegPath = AppOptions.getInstance().getFfmpegPath();
      this.frameRate = AppOptions.getInstance().getFrameRate();
      this.covoxFb = AppOptions.getInstance().isCovoxFb();
      this.ulaPlus = AppOptions.getInstance().isUlaPlus();
      this.turboSound = AppOptions.getInstance().isTurboSound();
      this.kempstonMouseAllowed = AppOptions.getInstance().isKempstonMouseAllowed();
      this.zx128byDefault = AppOptions.getInstance().getDefaultBoardMode() != BoardMode.ZXPOLY;
      this.kempstonKeyDown = AppOptions.getInstance().getKempstonVkDown();
      this.kempstonKeyUp = AppOptions.getInstance().getKempstonVkUp();
      this.kempstonKeyLeft = AppOptions.getInstance().getKempstonVkLeft();
      this.kempstonKeyRight = AppOptions.getInstance().getKempstonVkRight();
      this.kempstonKeyFire = AppOptions.getInstance().getKempstonVkFire();

      this.protekJoystickKeyDown = AppOptions.getInstance().getCursorJoystickDown();
      this.protekJoystickKeyUp = AppOptions.getInstance().getProtekJoystickUp();
      this.protekJoystickKeyLeft = AppOptions.getInstance().getProtekJoystickLeft();
      this.protekJoystickKeyRight = AppOptions.getInstance().getProtekJoystickRight();
      this.protekJoystickKeyFire = AppOptions.getInstance().getProtekJoystickFire();
    }

    public DataContainer(final OptionsPanel optionsPanel) {
      final RomSource rom =
              RomSource.findForTitle(requireNonNull(optionsPanel.comboRomSource.getSelectedItem()).toString(), RomSource.TEST);

      this.keyboardLook = (VirtualKeyboardLook) optionsPanel.comboKeyboardLook.getSelectedItem();
      this.volumeProfile = (VolumeProfile) optionsPanel.comboVolumeProfile.getSelectedItem();
      this.borderWidth = (BorderWidth) optionsPanel.comboBorderWidth.getSelectedItem();
      this.customRomPath = optionsPanel.textCustomRomPath.getText();

      this.interlacedScan = optionsPanel.checkInterlacedScan.isSelected();
      this.tryLessResources = optionsPanel.checkTryLessResources.isSelected();
      this.oldTvFilter = optionsPanel.checkOldTvFilter.isSelected();

      this.syncPaint = optionsPanel.checkSyncPaint.isSelected();

      this.emulateFFport = optionsPanel.checkEmulateFFport.isSelected();
      this.timingProfile = (TimingProfile) optionsPanel.comboTimingProfile.getSelectedItem();
      this.autoCsForCursorKeys = optionsPanel.checkAutoiCsForCursorKeys.isSelected();
      this.vkdApart = optionsPanel.checkVkbdApart.isSelected();
      this.activeRom = rom.getLink();
      this.intPerFrame = (Integer) optionsPanel.spinnerIntFrame.getValue();
      this.ffmpegPath = optionsPanel.textFfmpegPath.getText();
      this.port = (Integer) optionsPanel.spinnerPort.getValue();
      this.grabSound = optionsPanel.checkGrabSound.isSelected();
      this.soundSchemeAcb = optionsPanel.checkSoundSchemeACB.isSelected();
      this.inetAddress = requireNonNull(optionsPanel.comboNetAdddr.getSelectedItem()).toString();
      this.frameRate = (Integer) optionsPanel.spinnerFramesPerSec.getValue();
      this.covoxFb = optionsPanel.checkCovoxFb.isSelected();
      this.ulaPlus = optionsPanel.checkUlaPlus.isSelected();
      this.turboSound = optionsPanel.checkTurboSound.isSelected();
      this.kempstonMouseAllowed = optionsPanel.checkKempstonMouseAllowed.isSelected();
      this.zx128byDefault = rom != RomSource.TEST && optionsPanel.checkZx128ByDefault.isSelected();

      this.kempstonKeyDown = optionsPanel.keySelectorKempstonDown.getKey().orElse(-1);
      this.kempstonKeyUp = optionsPanel.keySelectorKempstonUp.getKey().orElse(-1);
      this.kempstonKeyLeft = optionsPanel.keySelectorKempstonLeft.getKey().orElse(-1);
      this.kempstonKeyRight = optionsPanel.keySelectorKempstonRight.getKey().orElse(-1);
      this.kempstonKeyFire = optionsPanel.keySelectorKempstonFire.getKey().orElse(-1);

      this.protekJoystickKeyFire = optionsPanel.keySelectorProtekJoystickFire.getKey().orElse(-1);
      this.protekJoystickKeyRight = optionsPanel.keySelectorProtekJoystickRight.getKey().orElse(-1);
      this.protekJoystickKeyLeft = optionsPanel.keySelectorProtekJoystickLeft.getKey().orElse(-1);
      this.protekJoystickKeyUp = optionsPanel.keySelectorProtekJoystickUp.getKey().orElse(-1);
      this.protekJoystickKeyDown = optionsPanel.keySelectorProtekJoystickDown.getKey().orElse(-1);
    }

    public void store() {
      AppOptions.getInstance().setSyncPaint(this.syncPaint);
      AppOptions.getInstance().setTimingProfile(this.timingProfile);
      AppOptions.getInstance().setBorderWidth(this.borderWidth);
      AppOptions.getInstance().setInterlacedScan(this.interlacedScan);
      AppOptions.getInstance().setTryLessResources(this.tryLessResources);
      AppOptions.getInstance().setOldColorTvOnStart(this.oldTvFilter);
      AppOptions.getInstance().setAutoCsForCursorKeys(this.autoCsForCursorKeys);
      AppOptions.getInstance().setCustomRomPath(this.customRomPath);
      AppOptions.getInstance().setKeyboardLook(this.keyboardLook);
      AppOptions.getInstance().setVolumeProfile(this.volumeProfile);
      AppOptions.getInstance().setVkbdApart(this.vkdApart);
      AppOptions.getInstance().setActiveRom(this.activeRom);
      AppOptions.getInstance().setFfmpegPath(this.ffmpegPath);
      AppOptions.getInstance().setGrabSound(this.grabSound);
      AppOptions.getInstance().setCovoxFb(this.covoxFb);
      AppOptions.getInstance().setUlaPlus(this.ulaPlus);
      AppOptions.getInstance().setIntBetweenFrames(this.intPerFrame);
      AppOptions.getInstance().setPort(this.port);
      AppOptions.getInstance().setAddress(this.inetAddress);
      AppOptions.getInstance().setFrameRate(this.frameRate);
      AppOptions.getInstance().setTurboSound(this.turboSound);
      AppOptions.getInstance().setAttributePortFf(this.emulateFFport);
      AppOptions.getInstance().setKempstonMouseAllowed(this.kempstonMouseAllowed);
      AppOptions.getInstance()
              .setDefaultBoardMode(this.zx128byDefault ? BoardMode.ZX128 : BoardMode.ZXPOLY);

      AppOptions.getInstance().setKempstonVkDown(this.kempstonKeyDown);
      AppOptions.getInstance().setKempstonVkLeft(this.kempstonKeyLeft);
      AppOptions.getInstance().setKempstonVkRight(this.kempstonKeyRight);
      AppOptions.getInstance().setKempstonVkUp(this.kempstonKeyUp);
      AppOptions.getInstance().setKempstonVkFire(this.kempstonKeyFire);

      AppOptions.getInstance().setProtekJoystickDown(this.protekJoystickKeyDown);
      AppOptions.getInstance().setProtekJoystickFire(this.protekJoystickKeyFire);
      AppOptions.getInstance().setProtekJoystickLeft(this.protekJoystickKeyLeft);
      AppOptions.getInstance().setProtekJoystickRight(this.protekJoystickKeyRight);
      AppOptions.getInstance().setProtekJoystickUp(this.protekJoystickKeyUp);

      AppOptions.getInstance().setSoundChannelsACB(this.soundSchemeAcb);

      try {
        AppOptions.getInstance().flush();
      } catch (BackingStoreException ex) {
        // DO NOTHING
      }
    }

  }
}
