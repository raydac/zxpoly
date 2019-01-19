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
package com.igormaznitsa.zxpoly.ui;

import com.igormaznitsa.zxpoly.components.TapeFileReader;
import javax.swing.ListSelectionModel;

public class SelectTapPosDialog extends javax.swing.JDialog {

  private static final long serialVersionUID = 3974803548217613782L;

  private int selectedIndex = -1;

  public SelectTapPosDialog(final java.awt.Frame parent, final TapeFileReader tap) {
    super(parent, true);
    this.setTitle(tap.getName());
    tap.stopPlay();
    initComponents();
    this.tapBlockList.setModel(tap);
    this.tapBlockList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    this.tapBlockList.setSelectedIndex(tap.getCurrent().getIndex());
    this.setLocationRelativeTo(parent);
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
    jScrollPane1 = new javax.swing.JScrollPane();
    tapBlockList = new javax.swing.JList();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);

    buttonCancel.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/cancel.png"))); // NOI18N
    buttonCancel.setText("Cancel");
    buttonCancel.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonCancelActionPerformed(evt);
      }
    });

    buttonOk.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/ok.png"))); // NOI18N
    buttonOk.setText("Ok");
    buttonOk.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonOkActionPerformed(evt);
      }
    });

    tapBlockList.setFont(new java.awt.Font("Courier New", 0, 12)); // NOI18N
    tapBlockList.setModel(new javax.swing.AbstractListModel() {
      String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
      public int getSize() { return strings.length; }
      public Object getElementAt(int i) { return strings[i]; }
    });
    tapBlockList.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mouseClicked(java.awt.event.MouseEvent evt) {
        tapBlockListMouseClicked(evt);
      }
    });
    jScrollPane1.setViewportView(tapBlockList);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
            .addGap(0, 202, Short.MAX_VALUE)
            .addComponent(buttonOk)
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addComponent(buttonCancel))
          .addComponent(jScrollPane1))
        .addContainerGap())
    );

    layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {buttonCancel, buttonOk});

    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 254, Short.MAX_VALUE)
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
          .addComponent(buttonCancel)
          .addComponent(buttonOk))
        .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  public int getSelectedIndex() {
    return this.selectedIndex;
  }

  private void buttonCancelActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonCancelActionPerformed
    this.selectedIndex = -1;
    setVisible(false);
  }//GEN-LAST:event_buttonCancelActionPerformed

  private void buttonOkActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonOkActionPerformed
    this.selectedIndex = this.tapBlockList.getSelectedIndex();
    setVisible(false);
  }//GEN-LAST:event_buttonOkActionPerformed

  private void tapBlockListMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_tapBlockListMouseClicked
    if (evt.getClickCount() > 1) {
      final int index = this.tapBlockList.locationToIndex(evt.getPoint());
      if (index >= 0) {
        this.tapBlockList.setSelectedIndex(index);
        buttonOkActionPerformed(null);
      }
    }
  }//GEN-LAST:event_tapBlockListMouseClicked

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JButton buttonCancel;
  private javax.swing.JButton buttonOk;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JList tapBlockList;
  // End of variables declaration//GEN-END:variables
}
