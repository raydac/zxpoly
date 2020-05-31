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

import com.igormaznitsa.zxpoly.utils.AppOptions;
import com.igormaznitsa.zxpoly.utils.AppOptions.Rom;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.prefs.BackingStoreException;

public class OptionsPanel extends javax.swing.JPanel {

    public static final class DataContainer {

        public final String ffmpegPath;
        public final String inetAddress;
        public final int port;
        public final boolean grabSound;
        public final String activeRom;
        public final int intPerFrame;
        public final int frameRate;

        public DataContainer() {
            this.activeRom = AppOptions.getInstance().getActiveRom();
            this.intPerFrame = AppOptions.getInstance().getIntBetweenFrames();
            this.port = AppOptions.getInstance().getPort();
            this.inetAddress = AppOptions.getInstance().getAddress();
            this.grabSound = AppOptions.getInstance().isGrabSound();
            this.ffmpegPath = AppOptions.getInstance().getFfmpegPath();
            this.frameRate = AppOptions.getInstance().getFrameRate();
        }

        public DataContainer(final OptionsPanel optionsPanel) {
            this.activeRom = Rom.findForTitle(optionsPanel.comboRomSource.getSelectedItem().toString(),Rom.TEST).getLink();
            this.intPerFrame = (Integer) optionsPanel.spinnerIntFrame.getValue();
            this.ffmpegPath = optionsPanel.textFfmpegPath.getText();
            this.port = (Integer) optionsPanel.spinnerPort.getValue();
            this.grabSound = optionsPanel.checkGrabSound.isSelected();
            this.inetAddress = optionsPanel.comboNetAdddr.getSelectedItem().toString();
            this.frameRate = (Integer)optionsPanel.spinnerFramesPerSec.getValue();
        }

        public void store() {
            AppOptions.getInstance().setActiveRom(this.activeRom);
            AppOptions.getInstance().setFfmpegPath(this.ffmpegPath);
            AppOptions.getInstance().setGrabSound(this.grabSound);
            AppOptions.getInstance().setIntBetweenFrames(this.intPerFrame);
            AppOptions.getInstance().setPort(this.port);
            AppOptions.getInstance().setAddress(this.inetAddress);
            AppOptions.getInstance().setFrameRate(this.frameRate);
            try {
                AppOptions.getInstance().flush();
            } catch (BackingStoreException ex) {
                // DO NOTHING
            }
        }

    }

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
            // TODO
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
        this.checkGrabSound.setSelected(data.grabSound);
        this.spinnerPort.setValue(data.port);
        this.spinnerIntFrame.setValue(data.intPerFrame);
        this.textFfmpegPath.setText(data.ffmpegPath);
        this.comboNetAdddr.setSelectedItem(data.inetAddress);
        this.spinnerFramesPerSec.setValue(data.frameRate);
        this.comboRomSource.setSelectedItem(Rom.findForLink(data.activeRom, Rom.TEST).getTitle());
    }

    public DataContainer getData() {
        return new DataContainer(this);
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        jPanel1 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        textFfmpegPath = new javax.swing.JTextField();
        comboNetAdddr = new javax.swing.JComboBox<>();
        jLabel3 = new javax.swing.JLabel();
        spinnerPort = new javax.swing.JSpinner();
        jLabel4 = new javax.swing.JLabel();
        checkGrabSound = new javax.swing.JCheckBox();
        jLabel7 = new javax.swing.JLabel();
        spinnerFramesPerSec = new javax.swing.JSpinner();
        jPanel2 = new javax.swing.JPanel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        comboRomSource = new javax.swing.JComboBox<>();
        spinnerIntFrame = new javax.swing.JSpinner();

        setLayout(new java.awt.GridBagLayout());

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Streaming"));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        jLabel1.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel1.setText("FFmpeg path:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel1, gridBagConstraints);

        jLabel2.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel2.setText("Net.interface:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel2, gridBagConstraints);

        textFfmpegPath.setColumns(24);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(textFfmpegPath, gridBagConstraints);

        comboNetAdddr.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(comboNetAdddr, gridBagConstraints);

        jLabel3.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel3.setText("Port:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel3, gridBagConstraints);

        spinnerPort.setModel(new javax.swing.SpinnerNumberModel(0, 0, 65535, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(spinnerPort, gridBagConstraints);

        jLabel4.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel4.setText("Sound:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel1.add(jLabel4, gridBagConstraints);

        checkGrabSound.setEnabled(false);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(checkGrabSound, gridBagConstraints);

        jLabel7.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel7.setText("Frame rate:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel1.add(jLabel7, gridBagConstraints);

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

        jLabel5.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel5.setText("ROM source:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel2.add(jLabel5, gridBagConstraints);

        jLabel6.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
        jLabel6.setText("INT/Frame:");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        jPanel2.add(jLabel6, gridBagConstraints);

        comboRomSource.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));
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

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        add(jPanel2, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JCheckBox checkGrabSound;
    private javax.swing.JComboBox<String> comboNetAdddr;
    private javax.swing.JComboBox<String> comboRomSource;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JSpinner spinnerFramesPerSec;
    private javax.swing.JSpinner spinnerIntFrame;
    private javax.swing.JSpinner spinnerPort;
    private javax.swing.JTextField textFfmpegPath;
    // End of variables declaration//GEN-END:variables
}
