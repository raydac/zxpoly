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
import com.igormaznitsa.zxpoly.components.video.VirtualKeyboardLook;
import com.igormaznitsa.zxpoly.utils.AppOptions;
import com.igormaznitsa.zxpoly.utils.AppOptions.Rom;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.List;
import java.util.*;
import java.util.logging.Logger;
import java.util.prefs.BackingStoreException;

import static javax.swing.BorderFactory.createTitledBorder;
import static javax.swing.SwingConstants.RIGHT;

public class OptionsPanel extends JPanel {

  private static final Logger LOGGER = Logger.getLogger("Options");

  private JCheckBox checkCovoxFb;
  private JCheckBox checkTurboSound;
  private JCheckBox checkZx128ByDefault;
  private JCheckBox checkKempstonMouseAllowed;
  private JLabel labelCovoxFb;
  private JLabel labelTurboSound;
  private JLabel labelZx128ByDefault;
  private JLabel labelKempstonMouseAllowed;
  private JLabel labelVirtualKbdApart;
  private JLabel labelVirtualKbdLook;
  private JLabel labelCustomRomPath;
  private JCheckBox checkGrabSound;
  private JCheckBox checkVkbdApart;
  private JComboBox<String> comboNetAdddr;
  private JComboBox<String> comboRomSource;
  private JComboBox<VirtualKeyboardLook> comboKeyboardLook;
  private JLabel labelFfMpegPath;
  private JLabel labelNetInterface;
  private JLabel labelPort;
  private JLabel labelSound;
  private JLabel labelRomSource;
  private JLabel labelIntFrame;
  private JLabel labelFrameRate;
  private JPanel panelStreaming;
  private JPanel panelGenmeral;
  private JSpinner spinnerFramesPerSec;
  private JSpinner spinnerIntFrame;
  private JSpinner spinnerPort;
  private JTextField textFfmpegPath;
  private JTextField textCustomRomPath;

  private KeyCodeSelector keySelectorKempstonLeft;
  private KeyCodeSelector keySelectorKempstonRight;
  private KeyCodeSelector keySelectorKempstonUp;
  private KeyCodeSelector keySelectorKempstonDown;
  private KeyCodeSelector keySelectorKempstonFire;

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

  private void fillByDataContainer(final DataContainer data) {
    this.textCustomRomPath.setText(data.customRomPath);
    this.checkGrabSound.setSelected(data.grabSound);
    this.checkVkbdApart.setSelected(data.vkdApart);
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
    this.comboKeyboardLook.setSelectedItem(data.keyboardLook);

    this.keySelectorKempstonFire.selectForCode(data.kempstonKeyFire);
    this.keySelectorKempstonRight.selectForCode(data.kempstonKeyRight);
    this.keySelectorKempstonLeft.selectForCode(data.kempstonKeyLeft);
    this.keySelectorKempstonUp.selectForCode(data.kempstonKeyUp);
    this.keySelectorKempstonDown.selectForCode(data.kempstonKeyDown);
  }

  @SuppressWarnings("unchecked")
  private void initComponents() {
    GridBagConstraints gridBagConstraints;

    panelStreaming = new JPanel();
    labelFfMpegPath = new JLabel();
    labelNetInterface = new JLabel();
    textFfmpegPath = new JTextField();
    comboNetAdddr = new JComboBox<>();
    labelPort = new JLabel();
    spinnerPort = new JSpinner();
    labelSound = new JLabel();
    checkGrabSound = new JCheckBox();
    labelFrameRate = new JLabel();
    spinnerFramesPerSec = new JSpinner();
    panelGenmeral = new JPanel();
    labelRomSource = new JLabel();
    labelIntFrame = new JLabel();
    comboRomSource = new JComboBox<>();
    spinnerIntFrame = new JSpinner();
    labelCovoxFb = new JLabel();
    checkCovoxFb = new JCheckBox();
    labelTurboSound = new JLabel();
    checkTurboSound = new JCheckBox();
    labelZx128ByDefault = new JLabel();
    checkZx128ByDefault = new JCheckBox();
    labelKempstonMouseAllowed = new JLabel();
    labelVirtualKbdApart = new JLabel();
    labelVirtualKbdLook = new JLabel();
    labelCustomRomPath = new JLabel();
    checkKempstonMouseAllowed = new JCheckBox();
    checkVkbdApart = new JCheckBox();
    comboKeyboardLook = new JComboBox<>(VirtualKeyboardLook.values());
    textCustomRomPath = new JTextField();

    keySelectorKempstonDown = new KeyCodeSelector();
    keySelectorKempstonLeft = new KeyCodeSelector();
    keySelectorKempstonUp = new KeyCodeSelector();
    keySelectorKempstonRight = new KeyCodeSelector();
    keySelectorKempstonFire = new KeyCodeSelector();

    setLayout(new GridBagLayout());

    panelStreaming.setBorder(createTitledBorder("Streaming"));
    panelStreaming.setLayout(new GridBagLayout());

    labelFfMpegPath.setHorizontalAlignment(RIGHT);
    labelFfMpegPath.setText("FFmpeg path:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    panelStreaming.add(labelFfMpegPath, gridBagConstraints);

    labelNetInterface.setHorizontalAlignment(RIGHT);
    labelNetInterface.setText("Net.interface:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    panelStreaming.add(labelNetInterface, gridBagConstraints);

    textFfmpegPath.setColumns(24);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelStreaming.add(textFfmpegPath, gridBagConstraints);

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
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.EAST;
    panelStreaming.add(labelSound, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelStreaming.add(checkGrabSound, gridBagConstraints);

    labelFrameRate.setHorizontalAlignment(RIGHT);
    labelFrameRate.setText("Frame rate:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelStreaming.add(labelFrameRate, gridBagConstraints);

    spinnerFramesPerSec.setModel(new javax.swing.SpinnerNumberModel(25, 1, 50, 1));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelStreaming.add(spinnerFramesPerSec, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    add(panelStreaming, gridBagConstraints);

    panelGenmeral.setBorder(createTitledBorder("Generat"));
    panelGenmeral.setLayout(new GridBagLayout());

    labelRomSource.setHorizontalAlignment(RIGHT);
    labelRomSource.setText("Load ROM source:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGenmeral.add(labelRomSource, gridBagConstraints);

    comboRomSource.setModel(new javax.swing.DefaultComboBoxModel<>());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGenmeral.add(comboRomSource, gridBagConstraints);

    labelCustomRomPath.setHorizontalAlignment(RIGHT);
    labelCustomRomPath.setText("Custom ROM file:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGenmeral.add(labelCustomRomPath, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    textCustomRomPath.setColumns(24);
    panelGenmeral.add(textCustomRomPath, gridBagConstraints);

    labelIntFrame.setHorizontalAlignment(RIGHT);
    labelIntFrame.setText("INT/Frame:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGenmeral.add(labelIntFrame, gridBagConstraints);

    spinnerIntFrame.setModel(new javax.swing.SpinnerNumberModel(1, 1, 100, 1));
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGenmeral.add(spinnerIntFrame, gridBagConstraints);

    labelTurboSound.setHorizontalAlignment(RIGHT);
    labelTurboSound.setText("TurboSound (NedoPC):");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGenmeral.add(labelTurboSound, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGenmeral.add(checkTurboSound, gridBagConstraints);

    labelCovoxFb.setHorizontalAlignment(RIGHT);
    labelCovoxFb.setText("Covox (#FB):");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGenmeral.add(labelCovoxFb, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 4;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGenmeral.add(checkCovoxFb, gridBagConstraints);

    labelZx128ByDefault.setHorizontalAlignment(RIGHT);
    labelZx128ByDefault.setText("Default ZX Mode:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGenmeral.add(labelZx128ByDefault, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 5;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGenmeral.add(checkZx128ByDefault, gridBagConstraints);

    labelKempstonMouseAllowed.setHorizontalAlignment(RIGHT);
    labelKempstonMouseAllowed.setText("Kempston mouse allowed:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGenmeral.add(labelKempstonMouseAllowed, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 6;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGenmeral.add(checkKempstonMouseAllowed, gridBagConstraints);

    labelVirtualKbdApart.setHorizontalAlignment(RIGHT);
    labelVirtualKbdApart.setText("Virtual keyboard apart:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGenmeral.add(labelVirtualKbdApart, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 7;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGenmeral.add(checkVkbdApart, gridBagConstraints);

    labelVirtualKbdLook.setHorizontalAlignment(RIGHT);
    labelVirtualKbdLook.setText("Keyboard decoration:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    panelGenmeral.add(labelVirtualKbdLook, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 8;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.anchor = GridBagConstraints.WEST;
    panelGenmeral.add(comboKeyboardLook, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.BOTH;
    add(panelGenmeral, gridBagConstraints);

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
    kempstonLabel = new JLabel("UP:");
    kempstonLabel.setHorizontalAlignment(RIGHT);
    panelKempston.add(kempstonLabel, gbx);
    gbx.gridx = 1;
    panelKempston.add(this.keySelectorKempstonUp, gbx);

    gbx.gridy = 3;
    gbx.gridx = 0;
    kempstonLabel = new JLabel("DOWN:");
    kempstonLabel.setHorizontalAlignment(RIGHT);
    panelKempston.add(kempstonLabel, gbx);
    gbx.gridx = 1;
    panelKempston.add(this.keySelectorKempstonDown, gbx);

    gbx.gridy = 4;
    gbx.gridx = 0;
    kempstonLabel = new JLabel("FIRE:");
    kempstonLabel.setHorizontalAlignment(RIGHT);
    panelKempston.add(kempstonLabel, gbx);
    gbx.gridx = 1;
    panelKempston.add(this.keySelectorKempstonFire, gbx);


    gridBagConstraints.gridy = 2;
    add(panelKempston, gridBagConstraints);

  }

  public DataContainer getData() {
    return new DataContainer(this);
  }

  private final static class NameKeyPair implements Comparable<NameKeyPair> {
    private final String name;
    private final int keyCode;

    NameKeyPair(final String name, final int keyCode) {
      this.name = name;
      this.keyCode = keyCode;
    }

    @Override
    public int compareTo(final NameKeyPair o) {
      return this.name.compareTo(o.name);
    }

    @Override
    public String toString() {
      return this.name;
    }
  }

  private static final class KeyCodeSelector extends JComboBox<NameKeyPair> {
    KeyCodeSelector() {
      super();

      final List<NameKeyPair> keyList = new ArrayList<>();
      keyList.add(new NameKeyPair("", -1));

      for (final Field field : KeyEvent.class.getFields()) {
        if (field.getType() == int.class && Modifier.isPublic(field.getModifiers())
                && Modifier.isFinal(field.getModifiers())
                && Modifier.isStatic(field.getModifiers())
                && field.getName().startsWith("VK_")) {
          final String name = field.getName().substring(3);
          try {
            final int code = field.getInt(null);
            keyList.add(new NameKeyPair(name, code));
          } catch (Exception ex) {
            throw new Error("Unexpected error during key event code extraction: " + field, ex);
          }
        }
      }

      Collections.sort(keyList);
      final ComboBoxModel<NameKeyPair> model = new DefaultComboBoxModel<>(keyList.toArray(new NameKeyPair[0]));
      this.setModel(model);
    }

    KeyCodeSelector selectForCode(final int keyCode) {
      final ComboBoxModel<NameKeyPair> model = this.getModel();
      NameKeyPair found = null;
      for (int i = 0; i < model.getSize(); i++) {
        final NameKeyPair item = model.getElementAt(i);
        if (item.keyCode == keyCode) {
          found = item;
          break;
        }
      }
      if (found == null) {
        this.setSelectedIndex(0);
      } else {
        this.setSelectedItem(found);
      }
      return this;
    }

    int getSelectedCode() {
      return ((NameKeyPair) Objects.requireNonNull(this.getSelectedItem())).keyCode;
    }
  }

  public static final class DataContainer {

    public final String customRomPath;
    public final String ffmpegPath;
    public final String inetAddress;
    public final VirtualKeyboardLook keyboardLook;
    public final int port;
    public final boolean grabSound;
    public final String activeRom;
    public final int intPerFrame;
    public final int frameRate;
    public final boolean covoxFb;
    public final boolean turboSound;
    public final boolean kempstonMouseAllowed;
    public final boolean zx128byDefault;
    public final boolean vkdApart;
    public final int kempstonKeyUp;
    public final int kempstonKeyDown;
    public final int kempstonKeyLeft;
    public final int kempstonKeyRight;
    public final int kempstonKeyFire;

    public DataContainer() {
      final String cromPath = AppOptions.getInstance().getCustomRomPath();
      this.customRomPath = cromPath == null ? "" : cromPath;
      this.keyboardLook = AppOptions.getInstance().getKeyboardLook();
      this.vkdApart = AppOptions.getInstance().isVkbdApart();
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
      this.kempstonKeyDown = AppOptions.getInstance().getKempstonVkDown();
      this.kempstonKeyUp = AppOptions.getInstance().getKempstonVkUp();
      this.kempstonKeyLeft = AppOptions.getInstance().getKempstonVkLeft();
      this.kempstonKeyRight = AppOptions.getInstance().getKempstonVkRight();
      this.kempstonKeyFire = AppOptions.getInstance().getKempstonVkFire();
    }

    public DataContainer(final OptionsPanel optionsPanel) {
      final Rom rom =
              Rom.findForTitle(optionsPanel.comboRomSource.getSelectedItem().toString(), Rom.TEST);

      this.keyboardLook = (VirtualKeyboardLook) optionsPanel.comboKeyboardLook.getSelectedItem();

      this.customRomPath = optionsPanel.textCustomRomPath.getText();

      this.vkdApart = optionsPanel.checkVkbdApart.isSelected();
      this.activeRom = rom.getLink();
      this.intPerFrame = (Integer) optionsPanel.spinnerIntFrame.getValue();
      this.ffmpegPath = optionsPanel.textFfmpegPath.getText();
      this.port = (Integer) optionsPanel.spinnerPort.getValue();
      this.grabSound = optionsPanel.checkGrabSound.isSelected();
      this.inetAddress = optionsPanel.comboNetAdddr.getSelectedItem().toString();
      this.frameRate = (Integer) optionsPanel.spinnerFramesPerSec.getValue();
      this.covoxFb = optionsPanel.checkCovoxFb.isSelected();
      this.turboSound = optionsPanel.checkTurboSound.isSelected();
      this.kempstonMouseAllowed = optionsPanel.checkKempstonMouseAllowed.isSelected();
      this.zx128byDefault = rom != Rom.TEST && optionsPanel.checkZx128ByDefault.isSelected();

      this.kempstonKeyDown = optionsPanel.keySelectorKempstonDown.getSelectedCode();
      this.kempstonKeyUp = optionsPanel.keySelectorKempstonUp.getSelectedCode();
      this.kempstonKeyLeft = optionsPanel.keySelectorKempstonLeft.getSelectedCode();
      this.kempstonKeyRight = optionsPanel.keySelectorKempstonRight.getSelectedCode();
      this.kempstonKeyFire = optionsPanel.keySelectorKempstonFire.getSelectedCode();
    }

    public void store() {
      AppOptions.getInstance().setCustomRomPath(this.customRomPath);
      AppOptions.getInstance().setKeyboardLook(this.keyboardLook);
      AppOptions.getInstance().setVkbdApart(this.vkdApart);
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

      AppOptions.getInstance().setKempstonVkDown(this.kempstonKeyDown);
      AppOptions.getInstance().setKempstonVkLeft(this.kempstonKeyLeft);
      AppOptions.getInstance().setKempstonVkRight(this.kempstonKeyRight);
      AppOptions.getInstance().setKempstonVkUp(this.kempstonKeyUp);
      AppOptions.getInstance().setKempstonVkFire(this.kempstonKeyFire);

      try {
        AppOptions.getInstance().flush();
      } catch (BackingStoreException ex) {
        // DO NOTHING
      }
    }

  }
}
