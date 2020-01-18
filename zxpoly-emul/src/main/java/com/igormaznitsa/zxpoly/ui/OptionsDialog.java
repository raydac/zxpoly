/*
 * Copyright (C) 2014-2019 Igor Maznitsa
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
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;
import javax.swing.Box.Filler;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingConstants;

@SuppressWarnings("unchecked")
public final class OptionsDialog extends javax.swing.JDialog {

  private static final long serialVersionUID = -8781309696283010727L;

  private final Preferences preferences = Preferences.userNodeForPackage(OptionsDialog.class);
  private JButton buttonCancel;
  private JButton buttonOk;
  private JComboBox comboRom;
  private Filler filler3;
  private JLabel jLabel1;
  private JLabel jLabel3;
  private JPanel mainPanel;
  private javax.swing.JSpinner spinnerScrRefreshIntTicks;

  public OptionsDialog(final java.awt.Frame parent) {
    super(parent, true);
    initComponents();

    this.comboRom.removeAllItems();

    this.comboRom.addItem(new RomUrl("ROM TEST", AppOptions.TEST_ROM));
    this.comboRom.addItem(new RomUrl("ROM ZX-128 TRDOS (WoS)", "http://wos.meulie.net/pub/sinclair/emulators/pc/russian/ukv12f5.zip"));
    this.comboRom.addItem(new RomUrl("ROM ZX-128 TRDOS (Archive.org)", "https://archive.org/download/World_of_Spectrum_June_2017_Mirror/World%20of%20Spectrum%20June%202017%20Mirror.zip/World%20of%20Spectrum%20June%202017%20Mirror/sinclair/emulators/pc/russian/ukv12f5.zip"));
    this.comboRom.addItem(new RomUrl("ROM ZX-128 TRDOS (Pdp-11.ru)", "http://mirrors.pdp-11.ru/_zx/vtrdos.ru/emulz/UKV12F5.ZIP"));
    this.comboRom.addItem(new RomUrl("ROM ZX-128 TRDOS (VTR-DOS)", "http://trd.speccy.cz/emulz/UKV12F5.ZIP"));

    this.spinnerScrRefreshIntTicks.setValue(AppOptions.getInstance().getIntBetweenFrames());
    final String activeLink = AppOptions.getInstance().getActiveRom();
    int selectedIndex = -1;
    for (int i = 0; i < this.comboRom.getItemCount(); i++) {
      if (((RomUrl) this.comboRom.getItemAt(i)).getLink().equals(activeLink)) {
        selectedIndex = i;
        break;
      }
    }
    this.comboRom.setSelectedIndex(Math.max(selectedIndex, 0));
    setLocationRelativeTo(parent);

    doLayout();
    pack();
  }

  @SuppressWarnings("unchecked")
  private void initComponents() {
    GridBagConstraints gridBagConstraints;

    comboRom = new JComboBox();
    jLabel1 = new JLabel();
    jLabel3 = new JLabel();
    spinnerScrRefreshIntTicks = new javax.swing.JSpinner();
    mainPanel = new JPanel();
    buttonCancel = new JButton();
    buttonOk = new JButton();
    filler3 = new Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(32767, 0));

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("Options");
    getContentPane().setLayout(new GridBagLayout());

    comboRom.setToolTipText("Selected ROM image");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = 2;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1000.0;
    gridBagConstraints.insets = new Insets(8, 0, 0, 8);
    getContentPane().add(comboRom, gridBagConstraints);

    jLabel1.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel1.setText("ROM Image:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(8, 8, 0, 0);
    getContentPane().add(jLabel1, gridBagConstraints);

    jLabel3.setHorizontalAlignment(SwingConstants.RIGHT);
    jLabel3.setText("INT/Frame:");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(8, 8, 0, 0);
    getContentPane().add(jLabel3, gridBagConstraints);

    spinnerScrRefreshIntTicks.setModel(new SpinnerNumberModel(3, 1, 50, 1));
    spinnerScrRefreshIntTicks.setToolTipText("INT ticks between refresh screen buffer requests");
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 1;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.ipadx = 3;
    gridBagConstraints.insets = new Insets(8, 0, 0, 0);
    getContentPane().add(spinnerScrRefreshIntTicks, gridBagConstraints);

    mainPanel.setLayout(new GridBagLayout());

    buttonCancel.setIcon(new ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/cancel.png"))); // NOI18N
    buttonCancel.setText("Cancel");
    buttonCancel.setToolTipText("Reject changes");
    buttonCancel.addActionListener(this::buttonCancelActionPerformed);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    mainPanel.add(buttonCancel, gridBagConstraints);

    buttonOk.setIcon(new ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/ok.png"))); // NOI18N
    buttonOk.setText("Ok");
    buttonOk.setToolTipText("Save changes");
    buttonOk.addActionListener(this::buttonOkActionPerformed);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.ipadx = 28;
    gridBagConstraints.anchor = GridBagConstraints.NORTHWEST;
    gridBagConstraints.insets = new Insets(0, 0, 0, 8);
    mainPanel.add(buttonOk, gridBagConstraints);
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1000.0;
    mainPanel.add(filler3, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 2;
    gridBagConstraints.gridwidth = 3;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.insets = new Insets(8, 8, 8, 8);
    getContentPane().add(mainPanel, gridBagConstraints);

    pack();
  }

  private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {
    final String selectedRom = ((RomUrl) this.comboRom.getSelectedItem()).getLink();
    try {
      AppOptions.getInstance().setActiveRom(selectedRom);
      AppOptions.getInstance().setIntBetweenFrames((Integer) this.spinnerScrRefreshIntTicks.getValue());
      AppOptions.getInstance().flush();
      JOptionPane.showMessageDialog(this, "Restart required to replace ROM image.", "Restart emulator", JOptionPane.WARNING_MESSAGE);
    } catch (BackingStoreException ee) {
      JOptionPane.showMessageDialog(this, "Can't save options!", "Error", JOptionPane.ERROR_MESSAGE);
    }

    setVisible(false);
  }

  private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {
    setVisible(false);
  }

  private static class RomUrl {

    private final String name;
    private final String link;

    private RomUrl(final String name, final String link) {
      this.name = name;
      this.link = link;
    }

    public String getLink() {
      return this.link;
    }

    @Override
    public String toString() {
      return this.name;
    }
  }
}
