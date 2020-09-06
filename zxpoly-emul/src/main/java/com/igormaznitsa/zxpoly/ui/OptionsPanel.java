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

import com.igormaznitsa.zxpoly.components.BoardMode;
import com.igormaznitsa.zxpoly.utils.AppOptions;
import com.igormaznitsa.zxpoly.utils.AppOptions.Rom;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

public class OptionsPanel extends javax.swing.JPanel {

  private static final Logger LOGGER = Logger.getLogger("Options");

  private javax.swing.JCheckBox checkCovoxFb;
  private javax.swing.JCheckBox checkTurboSound;
  private javax.swing.JCheckBox checkZx128ByDefault;
  private javax.swing.JCheckBox checkKempstonMouseAllowed;
  private javax.swing.JLabel labelCovoxFb;
  private javax.swing.JLabel labelTurboSound;
  private javax.swing.JLabel labelZx128ByDefault;
  private javax.swing.JLabel labelKempstonMouseAllowed;
  private javax.swing.JCheckBox checkGrabSound;
  private javax.swing.JComboBox<String> comboNetAdddr;
  private javax.swing.JComboBox<String> comboRomSource;
  private javax.swing.JLabel labelFfMpegPath;
  private javax.swing.JLabel labelNetInterface;
  private javax.swing.JLabel labelPort;
  private javax.swing.JLabel labelSound;
  private javax.swing.JLabel labelRomSource;
  private javax.swing.JLabel labelIntFrame;
  private javax.swing.JLabel labelFrameRate;
  private javax.swing.JPanel jPanel1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JSpinner spinnerFramesPerSec;
  private javax.swing.JSpinner spinnerIntFrame;
  private javax.swing.JSpinner spinnerPort;
  private javax.swing.JTextField textFfmpegPath;

  public OptionsPanel(final DataContainer dataContainer) {
    initComponents();
    this.comboRomSource.removeAllItems();
    Arrays.stream(Rom.values()).forEach(x -> this.comboRomSource.addItem(x.getTitle()));

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
  }

  public DataContainer getData() {
    return new DataContainer(this);
  }

  private void fillByDataContainer(final DataContainer data) {
    this.checkGrabSound.setSelected(data.grabSound);
    this.checkCovoxFb.setSelected(data.covoxFb);
    this.checkZx128ByDefault.setSelected(data.zx128byDefault);
    this.checkTurboSound.setSelected(data.turboSound);
    this.checkKempstonMouseAllowed.setSelected(data.kempstonMouseAllowed);
    this.spinnerPort.setValue(data.port);
    this.spinnerIntFrame.setValue(data.intPerFrame);
    this.textFfmpegPath.setText(data.ffmpegPath);
    this.comboNetAdddr.setSelectedItem(data.inetAddress);
    this.spinnerFramesPerSec.setValue(data.frameRate);
    this.comboRomSource.setSelectedItem(Rom.findForLink(data.activeRom, Rom.TEST).getTitle());
  }

  @SuppressWarnings("unchecked")
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    jPanel1 = new javax.swing.JPanel();
    labelFfMpegPath = new javax.swing.JLabel();
    labelNetInterface = new javax.swing.JLabel();
    textFfmpegPath = new javax.swing.JTextField();
    comboNetAdddr = new javax.swing.JComboBox<>();
    labelPort = new javax.swing.JLabel();
    spinnerPort = new javax.swing.JSpinner();
    labelSound = new javax.swing.JLabel();
    checkGrabSound = new javax.swing.JCheckBox();
    labelFrameRate = new javax.swing.JLabel();
    spinnerFramesPerSec = new javax.swing.JSpinner();
    jPanel2 = new javax.swing.JPanel();
    labelRomSource = new javax.swing.JLabel();
    labelIntFrame = new javax.swing.JLabel();
    comboRomSource = new javax.swing.JComboBox<>();
    spinnerIntFrame = new javax.swing.JSpinner();
    labelCovoxFb = new javax.swing.JLabel();
    checkCovoxFb = new javax.swing.JCheckBox();
    labelTurboSound = new javax.swing.JLabel();
    checkTurboSound = new javax.swing.JCheckBox();
    labelZx128ByDefault = new javax.swing.JLabel();
    checkZx128ByDefault = new javax.swing.JCheckBox();
    labelKempstonMouseAllowed = new javax.swing.JLabel();
    checkKempstonMouseAllowed = new javax.swing.JCheckBox();

    setLayout(new java.awt.GridBagLayout());

    jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Streaming"));
    jPanel1.setLayout(new java.awt.GridBagLayout());

    labelFfMpegPath.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelFfMpegPath.setText("FFmpeg path:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    jPanel1.add(labelFfMpegPath, gridBagConstraints);

    labelNetInterface.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelNetInterface.setText("Net.interface:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    jPanel1.add(labelNetInterface, gridBagConstraints);

    textFfmpegPath.setColumns(24);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel1.add(textFfmpegPath, gridBagConstraints);

    comboNetAdddr.setModel(new javax.swing.DefaultComboBoxModel<>());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel1.add(comboNetAdddr, gridBagConstraints);

    labelPort.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelPort.setText("Port:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    jPanel1.add(labelPort, gridBagConstraints);

    spinnerPort.setModel(new javax.swing.SpinnerNumberModel(0, 0, 65535, 1));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel1.add(spinnerPort, gridBagConstraints);

    labelSound.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelSound.setText("Sound:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
    jPanel1.add(labelSound, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel1.add(checkGrabSound, gridBagConstraints);

    labelFrameRate.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelFrameRate.setText("Frame rate:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    jPanel1.add(labelFrameRate, gridBagConstraints);

    spinnerFramesPerSec.setModel(new javax.swing.SpinnerNumberModel(25, 1, 50, 1));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel1.add(spinnerFramesPerSec, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    add(jPanel1, gridBagConstraints);

    jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Generat"));
    jPanel2.setLayout(new java.awt.GridBagLayout());

    labelRomSource.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelRomSource.setText("ROM source:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    jPanel2.add(labelRomSource, gridBagConstraints);

    labelIntFrame.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelIntFrame.setText("INT/Frame:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    jPanel2.add(labelIntFrame, gridBagConstraints);

    comboRomSource.setModel(new javax.swing.DefaultComboBoxModel<>());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel2.add(comboRomSource, gridBagConstraints);

    spinnerIntFrame.setModel(new javax.swing.SpinnerNumberModel(1, 1, 100, 1));
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel2.add(spinnerIntFrame, gridBagConstraints);

    labelTurboSound.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelTurboSound.setText("TurboSound (NedoPC):");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    jPanel2.add(labelTurboSound, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel2.add(checkTurboSound, gridBagConstraints);

    labelCovoxFb.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelCovoxFb.setText("Covox (#FB):");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    jPanel2.add(labelCovoxFb, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel2.add(checkCovoxFb, gridBagConstraints);

    labelZx128ByDefault.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelZx128ByDefault.setText("Default ZX Mode:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    jPanel2.add(labelZx128ByDefault, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel2.add(checkZx128ByDefault, gridBagConstraints);

    labelKempstonMouseAllowed.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
    labelKempstonMouseAllowed.setText("Kempston mouse allowed:");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    jPanel2.add(labelKempstonMouseAllowed, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
    jPanel2.add(checkKempstonMouseAllowed, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
    add(jPanel2, gridBagConstraints);
  }

  public static final class DataContainer {

    public final String ffmpegPath;
    public final String inetAddress;
    public final int port;
    public final boolean grabSound;
    public final String activeRom;
    public final int intPerFrame;
    public final int frameRate;
    public final boolean covoxFb;
    public final boolean turboSound;
    public final boolean kempstonMouseAllowed;
    public final boolean zx128byDefault;

    public DataContainer() {
      this.activeRom = AppOptions.getInstance().getActiveRom();
      this.intPerFrame = AppOptions.getInstance().getIntBetweenFrames();
      this.port = AppOptions.getInstance().getPort();
      this.inetAddress = AppOptions.getInstance().getAddress();
      this.grabSound = AppOptions.getInstance().isGrabSound();
      this.ffmpegPath = AppOptions.getInstance().getFfmpegPath();
      this.frameRate = AppOptions.getInstance().getFrameRate();
      this.covoxFb = AppOptions.getInstance().isCovoxFb();
      this.turboSound = AppOptions.getInstance().isTurboSound();
      this.kempstonMouseAllowed = AppOptions.getInstance().isKempstonMouseAllowed();
      this.zx128byDefault = AppOptions.getInstance().getDefaultBoardMode() != BoardMode.ZXPOLY;
    }

    public DataContainer(final OptionsPanel optionsPanel) {
      this.activeRom =
          Rom.findForTitle(optionsPanel.comboRomSource.getSelectedItem().toString(), Rom.TEST)
              .getLink();
      this.intPerFrame = (Integer) optionsPanel.spinnerIntFrame.getValue();
      this.ffmpegPath = optionsPanel.textFfmpegPath.getText();
      this.port = (Integer) optionsPanel.spinnerPort.getValue();
      this.grabSound = optionsPanel.checkGrabSound.isSelected();
      this.inetAddress = optionsPanel.comboNetAdddr.getSelectedItem().toString();
      this.frameRate = (Integer) optionsPanel.spinnerFramesPerSec.getValue();
      this.covoxFb = optionsPanel.checkCovoxFb.isSelected();
      this.turboSound = optionsPanel.checkTurboSound.isSelected();
      this.kempstonMouseAllowed = optionsPanel.checkKempstonMouseAllowed.isSelected();
      this.zx128byDefault = optionsPanel.checkZx128ByDefault.isSelected();
    }

    public void store() {
      AppOptions.getInstance().setActiveRom(this.activeRom);
      AppOptions.getInstance().setFfmpegPath(this.ffmpegPath);
      AppOptions.getInstance().setGrabSound(this.grabSound);
      AppOptions.getInstance().setCovoxFb(this.covoxFb);
      AppOptions.getInstance().setIntBetweenFrames(this.intPerFrame);
      AppOptions.getInstance().setPort(this.port);
      AppOptions.getInstance().setAddress(this.inetAddress);
      AppOptions.getInstance().setFrameRate(this.frameRate);
      AppOptions.getInstance().setTurboSound(this.turboSound);
      AppOptions.getInstance().setKempstonMouseAllowed(this.kempstonMouseAllowed);
      AppOptions.getInstance()
          .setDefaultBoardMode(this.zx128byDefault ? BoardMode.ZX128 : BoardMode.ZXPOLY);
      try {
        AppOptions.getInstance().flush();
      } catch (BackingStoreException ex) {
        // DO NOTHING
      }
    }

  }
}
