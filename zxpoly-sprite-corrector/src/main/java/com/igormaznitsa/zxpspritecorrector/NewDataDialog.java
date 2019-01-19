/* 
 * Copyright (C) 2019 Igor Maznitsa
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
package com.igormaznitsa.zxpspritecorrector;

import com.igormaznitsa.zxpspritecorrector.files.plugins.AbstractFilePlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.SCRPlugin;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.*;
import javax.swing.SpinnerNumberModel;

public final class NewDataDialog extends javax.swing.JDialog {

  private static final long serialVersionUID = 233857924099132156L;

  private AbstractFilePlugin.ReadResult result;
  private final MainFrame frame;

  public NewDataDialog(final MainFrame parent) {
    super(parent, true);
    this.frame = parent;
    initComponents();
    this.spinnerSize.setModel(new SpinnerNumberModel(6912, 1, 65536, 1));

    this.setLocationRelativeTo(parent);
  }

  public AbstractFilePlugin.ReadResult getResult() {
    return this.result;
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    buttonCancel = new javax.swing.JButton();
    buttonOk = new javax.swing.JButton();
    labelName = new javax.swing.JLabel();
    spinnerSize = new javax.swing.JSpinner();
    labelBytes = new javax.swing.JLabel();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("New data block");

    buttonCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpspritecorrector/icons/cross.png"))); // NOI18N
    buttonCancel.setText("Cancel");
    buttonCancel.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonCancelActionPerformed(evt);
      }
    });

    buttonOk.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpspritecorrector/icons/tick.png"))); // NOI18N
    buttonOk.setText("Ok");
    buttonOk.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonOkActionPerformed(evt);
      }
    });

    labelName.setText("Size of the data block ");

    labelBytes.setText("bytes");

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addComponent(buttonOk)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(buttonCancel)
        .addContainerGap())
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(labelName)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(spinnerSize, javax.swing.GroupLayout.PREFERRED_SIZE, 169, javax.swing.GroupLayout.PREFERRED_SIZE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(labelBytes)
        .addContainerGap(40, Short.MAX_VALUE))
    );

    layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonOk});

    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(labelName)
          .addComponent(spinnerSize, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(labelBytes))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(buttonCancel)
          .addComponent(buttonOk))
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
    final int len = ((Integer) spinnerSize.getValue());
    this.result = new AbstractFilePlugin.ReadResult(new ZXPolyData(new Info("Undefined", '$', 0x4000, len, 0), frame.getPico().getComponent(SCRPlugin.class), new byte[len]), null);
    dispose();
  }//GEN-LAST:event_buttonOkActionPerformed

  private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
    this.result = null;
    dispose();
  }//GEN-LAST:event_buttonCancelActionPerformed

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton buttonCancel;
  private javax.swing.JButton buttonOk;
  private javax.swing.JLabel labelBytes;
  private javax.swing.JLabel labelName;
  private javax.swing.JSpinner spinnerSize;
  // End of variables declaration//GEN-END:variables
}
