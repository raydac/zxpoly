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

package com.igormaznitsa.zxpspritecorrector.files;

import java.util.Properties;
import javax.swing.SwingUtilities;

public class Spec256ConfigEditorPanel extends javax.swing.JPanel {

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton buttonResetToDefault;
    private javax.swing.JCheckBox checkBkMixBkAttr;
    private javax.swing.JCheckBox checkBkMixed;
    private javax.swing.JCheckBox checkBkOverFF;
    private javax.swing.JCheckBox checkCpuMCPUPtrs;
    private javax.swing.JCheckBox checkCpuRegA;
    private javax.swing.JCheckBox checkCpuRegAltA;
    private javax.swing.JCheckBox checkCpuRegAltB;
    private javax.swing.JCheckBox checkCpuRegAltC;
    private javax.swing.JCheckBox checkCpuRegAltD;
    private javax.swing.JCheckBox checkCpuRegAltE;
    private javax.swing.JCheckBox checkCpuRegAltF;
    private javax.swing.JCheckBox checkCpuRegAltH;
    private javax.swing.JCheckBox checkCpuRegAltL;
    private javax.swing.JCheckBox checkCpuRegB;
    private javax.swing.JCheckBox checkCpuRegC;
    private javax.swing.JCheckBox checkCpuRegD;
    private javax.swing.JCheckBox checkCpuRegE;
    private javax.swing.JCheckBox checkCpuRegF;
    private javax.swing.JCheckBox checkCpuRegFaltExclC;
    private javax.swing.JCheckBox checkCpuRegFexclC;
    private javax.swing.JCheckBox checkCpuRegH;
    private javax.swing.JCheckBox checkCpuRegIXh;
    private javax.swing.JCheckBox checkCpuRegIXl;
    private javax.swing.JCheckBox checkCpuRegIYh;
    private javax.swing.JCheckBox checkCpuRegIYl;
    private javax.swing.JCheckBox checkCpuRegL;
    private javax.swing.JCheckBox checkCpuRegPc;
    private javax.swing.JCheckBox checkCpuRegSPh;
    private javax.swing.JCheckBox checkCpuRegSPl;
    private javax.swing.JCheckBox checkDownMixPaper;
    private javax.swing.JCheckBox checkGFXLeveledAND;
    private javax.swing.JCheckBox checkGFXLeveledOR;
    private javax.swing.JCheckBox checkGFXLeveledXOR;
    private javax.swing.JCheckBox checkGFXScreenXORBuffered;
    private javax.swing.JCheckBox checkGFXColors16;
    private javax.swing.JCheckBox checkPaper00InkFF;
    private javax.swing.JCheckBox checkUpMixPaper;
    private javax.swing.JCheckBox checkUseBrightInMix;
    private javax.swing.JCheckBox checkHideSameInkPaper;
    private javax.swing.Box.Filler filler1;
    private javax.swing.Box.Filler filler2;
    private javax.swing.Box.Filler filler3;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JSpinner spinnerDownColorsMixed;
    private javax.swing.JSpinner spinnerDownMixChgBright;
    private javax.swing.JSpinner spinnerUpColorsMixed;
    private javax.swing.JSpinner spinnerUpMixChgBright;
    private javax.swing.JPanel syncRegsPanel;

    public Spec256ConfigEditorPanel(final Properties config) {
        initComponents();
        this.fill(config == null ? makeDefault() : config);
    }

    public static Properties makeDefault() {
        final Properties result = new Properties();

        result.put("GFXLeveledXOR", "0");
        result.put("GFXLeveledOR", "0");
        result.put("GFXLeveledAND", "0");
        result.put("GFXScreenXORBuffered", "0");
        result.put("UpColorsMixed", "64");
        result.put("DownColorsMixed", "0");
        result.put("UpMixChgBright", "0");
        result.put("DownMixChgBright", "0");
        result.put("UseBrightInMix", "0");
        result.put("UpMixPaper", "0");
        result.put("DownMixPaper", "0");
        result.put("BkMixed", "0");
        result.put("BkMixBkAttr", "0");
        result.put("BkOverFF", "1");
        result.put("HideSameInkPaper", "1");
        result.put("zxpAlignRegs", "1PSsT");
        result.put("GFXColors16", "1");

        return result;
    }

    private static boolean isPropertySet(final String name, final Properties properties,
                                         final boolean dflt) {
        final String value = properties.getProperty(name, dflt ? "1" : "0");
        return !"0".equals(value.trim());
    }

    private static int getValue(final String name, final Properties properties, final int min,
                                final int max) {
        final String value = properties.getProperty(name);
        if (value == null) {
            return min;
        }
        try {
            return Math.max(min, Math.min(Integer.parseInt(value.trim()), max));
        } catch (NumberFormatException ex) {
            return min;
        }
    }

    private void fill(final Properties properties) {
        this.checkGFXLeveledXOR.setSelected(isPropertySet("GFXLeveledXOR", properties, false));
        this.checkGFXLeveledOR.setSelected(isPropertySet("GFXLeveledOR", properties, false));
        this.checkGFXLeveledAND.setSelected(isPropertySet("GFXLeveledAND", properties, false));
        this.checkGFXScreenXORBuffered
            .setSelected(isPropertySet("GFXScreenXORBuffered", properties, false));
        this.checkGFXColors16
            .setSelected(isPropertySet("GFXColors16", properties, false));
        this.checkPaper00InkFF
            .setSelected(isPropertySet("Paper00InkFF", properties, false));
        this.checkUseBrightInMix.setSelected(isPropertySet("UseBrightInMix", properties, false));
        this.checkHideSameInkPaper.setSelected(isPropertySet("HideSameInkPaper", properties, true));
        this.checkUpMixPaper.setSelected(isPropertySet("UpMixPaper", properties, false));
        this.checkDownMixPaper.setSelected(isPropertySet("DownMixPaper", properties, false));
        this.checkBkMixed.setSelected(isPropertySet("BkMixed", properties, false));
        this.checkBkMixBkAttr.setSelected(isPropertySet("BkMixBkAttr", properties, false));
        this.checkBkOverFF.setSelected(isPropertySet("BkOverFF", properties, true));

        final String gfxRegisters = properties.getProperty("zxpAlignRegs", "");

        this.checkCpuRegA.setSelected(gfxRegisters.contains("A"));
        this.checkCpuRegAltA.setSelected(gfxRegisters.contains("a"));
        this.checkCpuRegF.setSelected(gfxRegisters.contains("F"));
        this.checkCpuRegAltF.setSelected(gfxRegisters.contains("f"));
        this.checkCpuRegFexclC.setSelected(gfxRegisters.contains("1"));
        this.checkCpuRegFaltExclC.setSelected(gfxRegisters.contains("0"));
        this.checkCpuRegB.setSelected(gfxRegisters.contains("B"));
        this.checkCpuRegAltB.setSelected(gfxRegisters.contains("b"));
        this.checkCpuRegC.setSelected(gfxRegisters.contains("C"));
        this.checkCpuRegAltC.setSelected(gfxRegisters.contains("c"));
        this.checkCpuRegD.setSelected(gfxRegisters.contains("D"));
        this.checkCpuRegAltD.setSelected(gfxRegisters.contains("d"));
        this.checkCpuRegE.setSelected(gfxRegisters.contains("E"));
        this.checkCpuRegAltE.setSelected(gfxRegisters.contains("e"));
        this.checkCpuRegH.setSelected(gfxRegisters.contains("H"));
        this.checkCpuRegAltH.setSelected(gfxRegisters.contains("h"));
        this.checkCpuRegL.setSelected(gfxRegisters.contains("L"));
        this.checkCpuRegAltL.setSelected(gfxRegisters.contains("l"));

        this.checkCpuRegPc.setSelected(gfxRegisters.contains("P"));
        this.checkCpuRegSPh.setSelected(gfxRegisters.contains("S"));
        this.checkCpuRegSPl.setSelected(gfxRegisters.contains("s"));
        this.checkCpuRegIXh.setSelected(gfxRegisters.contains("X"));
        this.checkCpuRegIXl.setSelected(gfxRegisters.contains("x"));
        this.checkCpuRegIYh.setSelected(gfxRegisters.contains("Y"));
        this.checkCpuRegIYl.setSelected(gfxRegisters.contains("y"));

        this.checkCpuMCPUPtrs.setSelected(gfxRegisters.contains("T"));

        this.spinnerUpColorsMixed.setValue(getValue("UpColorsMixed", properties, 0, 64));
        this.spinnerDownColorsMixed.setValue(getValue("DownColorsMixed", properties, 0, 64));
        this.spinnerDownMixChgBright.setValue(getValue("DownMixChgBright", properties, 0, 100));
        this.spinnerUpMixChgBright.setValue(getValue("UpMixChgBright", properties, 0, 100));
    }

    public void reset() {
        SwingUtilities.invokeLater(() -> {
            this.fill(makeDefault());
            this.repaint();
        });
    }

    private String makeGfxRegistersSync() {
        final StringBuilder result = new StringBuilder();

        result.append(this.checkCpuRegA.isSelected() ? "A" : "");
        result.append(this.checkCpuRegAltA.isSelected() ? "a" : "");
        result.append(this.checkCpuRegF.isSelected() ? "F" : "");
        result.append(this.checkCpuRegAltF.isSelected() ? "f" : "");
        result.append(this.checkCpuRegFexclC.isSelected() ? "1" : "");
        result.append(this.checkCpuRegFaltExclC.isSelected() ? "0" : "");
        result.append(this.checkCpuRegB.isSelected() ? "B" : "");
        result.append(this.checkCpuRegAltB.isSelected() ? "b" : "");
        result.append(this.checkCpuRegC.isSelected() ? "C" : "");
        result.append(this.checkCpuRegAltC.isSelected() ? "c" : "");
        result.append(this.checkCpuRegD.isSelected() ? "D" : "");
        result.append(this.checkCpuRegAltD.isSelected() ? "d" : "");
        result.append(this.checkCpuRegE.isSelected() ? "E" : "");
        result.append(this.checkCpuRegAltE.isSelected() ? "e" : "");
        result.append(this.checkCpuRegH.isSelected() ? "H" : "");
        result.append(this.checkCpuRegAltH.isSelected() ? "h" : "");
        result.append(this.checkCpuRegL.isSelected() ? "L" : "");
        result.append(this.checkCpuRegAltL.isSelected() ? "l" : "");
        result.append(this.checkCpuRegPc.isSelected() ? "P" : "");

        result.append(this.checkCpuRegSPh.isSelected() ? "S" : "");
        result.append(this.checkCpuRegSPl.isSelected() ? "s" : "");
        result.append(this.checkCpuRegIXh.isSelected() ? "X" : "");
        result.append(this.checkCpuRegIXl.isSelected() ? "x" : "");
        result.append(this.checkCpuRegIYh.isSelected() ? "Y" : "");
        result.append(this.checkCpuRegIYl.isSelected() ? "y" : "");

        result.append(this.checkCpuMCPUPtrs.isSelected() ? "T" : "");

        return result.toString();
    }

    public Properties make() {
        final Properties properties = new Properties();

        properties.setProperty("zxpAlignRegs", makeGfxRegistersSync());

        properties.setProperty("GFXLeveledXOR", this.checkGFXLeveledXOR.isSelected() ? "1" : "0");
        properties.setProperty("GFXLeveledOR", this.checkGFXLeveledOR.isSelected() ? "1" : "0");
        properties.setProperty("GFXLeveledAND", this.checkGFXLeveledAND.isSelected() ? "1" : "0");
        properties.setProperty("GFXScreenXORBuffered",
            this.checkGFXScreenXORBuffered.isSelected() ? "1" : "0");
        properties.setProperty("GFXColors16",
            this.checkGFXColors16.isSelected() ? "1" : "0");
        properties.setProperty("Paper00InkFF",
            this.checkPaper00InkFF.isSelected() ? "1" : "0");
        properties.setProperty("UseBrightInMix", this.checkUseBrightInMix.isSelected() ? "1" : "0");
        properties
            .setProperty("HideSameInkPaper", this.checkHideSameInkPaper.isSelected() ? "1" : "0");
        properties.setProperty("UpMixPaper", this.checkUpMixPaper.isSelected() ? "1" : "0");
        properties.setProperty("DownMixPaper", this.checkDownMixPaper.isSelected() ? "1" : "0");
        properties.setProperty("BkMixed", this.checkBkMixed.isSelected() ? "1" : "0");
        properties.setProperty("BkMixBkAttr", this.checkBkMixBkAttr.isSelected() ? "1" : "0");
        properties.setProperty("BkOverFF", this.checkBkOverFF.isSelected() ? "1" : "0");

        properties.setProperty("UpColorsMixed", this.spinnerUpColorsMixed.getValue().toString());
        properties
            .setProperty("DownColorsMixed", this.spinnerDownColorsMixed.getValue().toString());
        properties
            .setProperty("DownMixChgBright", this.spinnerDownMixChgBright.getValue().toString());
        properties.setProperty("UpMixChgBright", this.spinnerUpMixChgBright.getValue().toString());

        return properties;
    }

    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {
        java.awt.GridBagConstraints gridBagConstraints;

        syncRegsPanel = new javax.swing.JPanel();
        checkCpuRegA = new javax.swing.JCheckBox();
        checkCpuRegAltA = new javax.swing.JCheckBox();
        checkCpuRegF = new javax.swing.JCheckBox();
        checkCpuRegAltF = new javax.swing.JCheckBox();
        checkCpuRegB = new javax.swing.JCheckBox();
        checkCpuRegAltB = new javax.swing.JCheckBox();
        checkCpuRegC = new javax.swing.JCheckBox();
        checkCpuRegAltC = new javax.swing.JCheckBox();
        checkCpuRegD = new javax.swing.JCheckBox();
        checkCpuRegAltD = new javax.swing.JCheckBox();
        checkCpuRegE = new javax.swing.JCheckBox();
        checkCpuRegAltE = new javax.swing.JCheckBox();
        checkCpuRegH = new javax.swing.JCheckBox();
        checkCpuRegAltH = new javax.swing.JCheckBox();
        checkCpuRegL = new javax.swing.JCheckBox();
        checkCpuRegAltL = new javax.swing.JCheckBox();
        checkCpuRegPc = new javax.swing.JCheckBox();
        checkCpuRegSPh = new javax.swing.JCheckBox();
        checkCpuRegSPl = new javax.swing.JCheckBox();
        checkCpuRegFexclC = new javax.swing.JCheckBox();
        checkCpuRegFaltExclC = new javax.swing.JCheckBox();
        checkCpuRegIXh = new javax.swing.JCheckBox();
        checkCpuRegIXl = new javax.swing.JCheckBox();
        checkCpuRegIYh = new javax.swing.JCheckBox();
        checkCpuRegIYl = new javax.swing.JCheckBox();
        checkCpuMCPUPtrs = new javax.swing.JCheckBox();
        jPanel1 = new javax.swing.JPanel();
        checkGFXLeveledXOR = new javax.swing.JCheckBox();
        checkGFXLeveledOR = new javax.swing.JCheckBox();
        checkGFXLeveledAND = new javax.swing.JCheckBox();
        checkGFXScreenXORBuffered = new javax.swing.JCheckBox();
        checkGFXColors16 = new javax.swing.JCheckBox();
        checkPaper00InkFF = new javax.swing.JCheckBox();
        checkBkMixed = new javax.swing.JCheckBox();
        checkBkMixBkAttr = new javax.swing.JCheckBox();
        checkBkOverFF = new javax.swing.JCheckBox();
        checkUseBrightInMix = new javax.swing.JCheckBox();
        checkHideSameInkPaper = new javax.swing.JCheckBox();
        checkUpMixPaper = new javax.swing.JCheckBox();
        checkDownMixPaper = new javax.swing.JCheckBox();
        filler1 =
            new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0),
                new java.awt.Dimension(0, 32767));
        jPanel2 = new javax.swing.JPanel();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        spinnerUpColorsMixed = new javax.swing.JSpinner();
        spinnerDownColorsMixed = new javax.swing.JSpinner();
        spinnerUpMixChgBright = new javax.swing.JSpinner();
        spinnerDownMixChgBright = new javax.swing.JSpinner();
        filler2 =
            new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0),
                new java.awt.Dimension(32767, 0));
        filler3 =
            new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0),
                new java.awt.Dimension(0, 32767));
        jPanel3 = new javax.swing.JPanel();
        buttonResetToDefault = new javax.swing.JButton();

        setLayout(new java.awt.GridBagLayout());

        syncRegsPanel
            .setBorder(javax.swing.BorderFactory.createTitledBorder("Sync GFX CPU registers"));
        syncRegsPanel.setLayout(new java.awt.GridBagLayout());

        checkCpuRegA.setText("A");
        checkCpuRegA.setToolTipText("Register A");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegA, gridBagConstraints);

        checkCpuRegAltA.setText("A'");
        checkCpuRegAltA.setToolTipText("Register alt A");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegAltA, gridBagConstraints);

        checkCpuRegF.setText("F");
        checkCpuRegF.setToolTipText("Register F");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegF, gridBagConstraints);

        checkCpuRegAltF.setText("F'");
        checkCpuRegAltF.setToolTipText("Register alt F");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegAltF, gridBagConstraints);

        checkCpuRegB.setText("B");
        checkCpuRegB.setToolTipText("Register B");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegB, gridBagConstraints);

        checkCpuRegAltB.setText("B'");
        checkCpuRegAltB.setToolTipText("Register alt B");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegAltB, gridBagConstraints);

        checkCpuRegC.setText("C");
        checkCpuRegC.setToolTipText("Register C");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegC, gridBagConstraints);

        checkCpuRegAltC.setText("C'");
        checkCpuRegAltC.setToolTipText("Register alt C");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegAltC, gridBagConstraints);

        checkCpuRegD.setText("D");
        checkCpuRegD.setToolTipText("Register D");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegD, gridBagConstraints);

        checkCpuRegAltD.setText("D'");
        checkCpuRegAltD.setToolTipText("Register alt D");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegAltD, gridBagConstraints);

        checkCpuRegE.setText("E");
        checkCpuRegE.setToolTipText("Register E");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegE, gridBagConstraints);

        checkCpuRegAltE.setText("E'");
        checkCpuRegAltE.setToolTipText("Register alt E");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegAltE, gridBagConstraints);

        checkCpuRegH.setText("H");
        checkCpuRegH.setToolTipText("Register H");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegH, gridBagConstraints);

        checkCpuRegAltH.setText("H'");
        checkCpuRegAltH.setToolTipText("Register alt H");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegAltH, gridBagConstraints);

        checkCpuRegL.setText("L");
        checkCpuRegL.setToolTipText("Register L");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegL, gridBagConstraints);

        checkCpuRegAltL.setText("L'");
        checkCpuRegAltL.setToolTipText("Register alt L");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegAltL, gridBagConstraints);

        checkCpuRegPc.setText("PC");
        checkCpuRegPc.setToolTipText("Register PC");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegPc, gridBagConstraints);

        checkCpuRegSPh.setText("SPh");
        checkCpuRegSPh.setToolTipText("High byte of SP register");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegSPh, gridBagConstraints);

        checkCpuRegSPl.setText("SPl");
        checkCpuRegSPl.setToolTipText("Low byte of SP register");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegSPl, gridBagConstraints);

        checkCpuRegFexclC.setText("F (excl. C)");
        checkCpuRegFexclC.setToolTipText("Register F excluding C flag");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegFexclC, gridBagConstraints);

        checkCpuRegFaltExclC.setText("F' (excl. C)");
        checkCpuRegFaltExclC.setToolTipText("Register alt F excluding C flag");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegFaltExclC, gridBagConstraints);

        checkCpuRegIXh.setText("IXh");
        checkCpuRegIXh.setToolTipText("High byte of IX register");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegIXh, gridBagConstraints);

        checkCpuRegIXl.setText("IXl");
        checkCpuRegIXl.setToolTipText("Low byte of IX register");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegIXl, gridBagConstraints);

        checkCpuRegIYh.setText("IYh");
        checkCpuRegIYh.setToolTipText("High byte of IY register");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegIYh, gridBagConstraints);

        checkCpuRegIYl.setText("IYl");
        checkCpuRegIYl.setToolTipText("Low byte of IY register");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuRegIYl, gridBagConstraints);

        checkCpuMCPUPtrs.setText("mCPU PTRs");
        checkCpuMCPUPtrs.setToolTipText(
            "GFX processors must read PTR values for memory operations from registers in main CPU (SP,HL,IX,IY,BC,DE)");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        syncRegsPanel.add(checkCpuMCPUPtrs, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(syncRegsPanel, gridBagConstraints);

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Extra flags"));
        jPanel1.setLayout(new java.awt.GridBagLayout());

        checkGFXLeveledXOR.setText("GFXLeveledXOR");
        checkGFXLeveledXOR
            .setToolTipText("while XORing two GFX bytes, MAX function used to get result.");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(checkGFXLeveledXOR, gridBagConstraints);

        checkGFXLeveledOR.setText("GFXLeveledOR");
        checkGFXLeveledOR
            .setToolTipText("while ORing two GFX bytes, MAX function used to get result");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(checkGFXLeveledOR, gridBagConstraints);

        checkGFXLeveledAND.setText("GFXLeveledAND");
        checkGFXLeveledAND
            .setToolTipText("while ANDing two GFX bytes, MIN function used to get result");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(checkGFXLeveledAND, gridBagConstraints);

        checkGFXScreenXORBuffered.setText("GFXScreenXORBuffered");
        checkGFXScreenXORBuffered.setToolTipText(
            "special sequence [XOR A,(HL);LD (HL),A] when HL is between 4000H and 57FFH is recognized as xor with screen");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(checkGFXScreenXORBuffered, gridBagConstraints);

        checkGFXColors16.setText("GFXColors16");
        checkGFXColors16.setToolTipText(
            "provides hint to emulator that only 16 colors of ZX-Palette in use");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(checkGFXColors16, gridBagConstraints);

        checkPaper00InkFF.setText("Paper00InkFF");
        checkPaper00InkFF.setToolTipText(
            "force drawing paper color over #00 and ink color over #FF palette indexes");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel1.add(checkPaper00InkFF, gridBagConstraints);

        checkBkMixed.setText("BkMixed");
        checkBkMixed.setToolTipText(
            "colors in the background with indeces under DownColorsMixed and above UpColorsMixed are are mixed with the attribute");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        jPanel1.add(checkBkMixed, gridBagConstraints);

        checkBkMixBkAttr.setText("BkMixBkAttr");
        checkBkMixBkAttr.setToolTipText(
            "background pixels with indeces under DownColorsMixed and above UpColorsMixed are mixed with back attribute rather then with fore attribute");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        jPanel1.add(checkBkMixBkAttr, gridBagConstraints);

        checkBkOverFF.setText("BkOverFF");
        checkBkOverFF.setToolTipText("background pixels are overriding also FF value from GFX");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        jPanel1.add(checkBkOverFF, gridBagConstraints);

        checkUseBrightInMix.setText("UseBrightInMix");
        checkUseBrightInMix.setToolTipText("then brightness bit also used to mix with");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 3;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        jPanel1.add(checkUseBrightInMix, gridBagConstraints);

        checkUpMixPaper.setText("UpMixPaper");
        checkUpMixPaper
            .setToolTipText("mixing with a paper color only - for upper mixed colors bank");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 4;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        jPanel1.add(checkUpMixPaper, gridBagConstraints);

        checkDownMixPaper.setText("DownMixPaper");
        checkDownMixPaper.setToolTipText("the same as UpMixPaper, but for down mixed colors bank");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        jPanel1.add(checkDownMixPaper, gridBagConstraints);

        checkHideSameInkPaper.setText("HideSameInkPaper");
        checkHideSameInkPaper
            .setToolTipText("force show ink color if attribute ink and paper has same value");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 6;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        jPanel1.add(checkHideSameInkPaper, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 7;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.weighty = 1000.0;
        jPanel1.add(filler1, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.NORTHWEST;
        add(jPanel1, gridBagConstraints);

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Spinners"));
        jPanel2.setLayout(new java.awt.GridBagLayout());

        jLabel1.setText("UpColorsMixed:");
        jLabel1.setToolTipText("mixing for up colors in the palette");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(jLabel1, gridBagConstraints);

        jLabel2.setText("DownColorsMixed:");
        jLabel2.setToolTipText("mixing for down colors in the palette");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        jPanel2.add(jLabel2, gridBagConstraints);

        jLabel3.setText("UpMixChgBright:");
        jLabel3.setToolTipText(
            "defines which part of mixed result is affected by the bright of an attribute rather then by its color");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        jPanel2.add(jLabel3, gridBagConstraints);

        jLabel4.setText("DownMixChgBright:");
        jLabel4.setToolTipText(
            "defines which part of mixed result is affected by the bright of an attribute rather then by its color");
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 2;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.EAST;
        gridBagConstraints.insets = new java.awt.Insets(0, 8, 0, 0);
        jPanel2.add(jLabel4, gridBagConstraints);

        spinnerUpColorsMixed.setModel(new javax.swing.SpinnerNumberModel(64, 0, 64, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(spinnerUpColorsMixed, gridBagConstraints);

        spinnerDownColorsMixed.setModel(new javax.swing.SpinnerNumberModel(0, 0, 64, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 1;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(spinnerDownColorsMixed, gridBagConstraints);

        spinnerUpMixChgBright.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(spinnerUpMixChgBright, gridBagConstraints);

        spinnerDownMixChgBright.setModel(new javax.swing.SpinnerNumberModel(0, 0, 100, 1));
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 3;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        jPanel2.add(spinnerDownMixChgBright, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 4;
        gridBagConstraints.gridy = 0;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weightx = 1000.0;
        jPanel2.add(filler2, gridBagConstraints);
        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 5;
        gridBagConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
        gridBagConstraints.anchor = java.awt.GridBagConstraints.WEST;
        gridBagConstraints.weighty = 1000.0;
        jPanel2.add(filler3, gridBagConstraints);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 1;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(jPanel2, gridBagConstraints);

        jPanel3.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

        buttonResetToDefault.setText("Reset to default");
        buttonResetToDefault.setToolTipText("Reset all values to default ones");
        buttonResetToDefault.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                buttonResetToDefaultActionPerformed(evt);
            }
        });
        jPanel3.add(buttonResetToDefault);

        gridBagConstraints = new java.awt.GridBagConstraints();
        gridBagConstraints.gridx = 0;
        gridBagConstraints.gridy = 2;
        gridBagConstraints.gridwidth = 2;
        gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
        add(jPanel3, gridBagConstraints);
    }// </editor-fold>//GEN-END:initComponents

    private void buttonResetToDefaultActionPerformed(
        java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonResetToDefaultActionPerformed
        this.reset();
    }//GEN-LAST:event_buttonResetToDefaultActionPerformed
    // End of variables declaration//GEN-END:variables
}
