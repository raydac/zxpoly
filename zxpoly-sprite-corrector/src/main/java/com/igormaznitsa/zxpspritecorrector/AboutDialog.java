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

import javax.swing.JFrame;

public final class AboutDialog extends javax.swing.JDialog {

  private static final long serialVersionUID = 4732416792603520083L;
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JTextArea infoText;
  private javax.swing.JToggleButton jToggleButton1;

  public AboutDialog(final JFrame parent) {
    super(parent, true);
    initComponents();

    this.infoText.setText(this.infoText.getText().replace("${project.version}", "2.3.5"));

    this.setLocationRelativeTo(null);
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    jToggleButton1 = new javax.swing.JToggleButton();
    jScrollPane1 = new javax.swing.JScrollPane();
    infoText = new javax.swing.JTextArea();

    setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
    setTitle("About");

    jToggleButton1.setIcon(new javax.swing.ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpspritecorrector/icons/cross.png"))); // NOI18N
    jToggleButton1.setText("Close");
    jToggleButton1.addActionListener(this::jToggleButton1ActionPerformed);

    infoText.setEditable(false);
    infoText.setColumns(20);
    infoText.setFont(infoText.getFont().deriveFont(infoText.getFont().getSize() + 3f));
    infoText.setLineWrap(true);
    infoText.setRows(5);
    infoText.setText(
        "The ZX-Poly sprite corrector v. ${project.version}\n--------------------------------------\n(C) 2008-2025 Igor Maznitsa (igor.maznitsa@igormaznitsa.com)\n\n  The utility allows to load a Hobeta file and find sprites inside of opened binary block. Found sprites can be colorized with the editor and the result can be saved as separated Hobeta files.\n--------------------------------------\nSome icons from http://www.fatcow.com/free-icons");
    infoText.setWrapStyleWord(true);
    jScrollPane1.setViewportView(infoText);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 514,
                        Short.MAX_VALUE)
                    .addComponent(jToggleButton1, javax.swing.GroupLayout.DEFAULT_SIZE,
                        javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
    );
    layout.setVerticalGroup(
        layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 212,
                    Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jToggleButton1)
                .addContainerGap())
    );

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void jToggleButton1ActionPerformed(
      java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jToggleButton1ActionPerformed
    dispose();
  }//GEN-LAST:event_jToggleButton1ActionPerformed
  // End of variables declaration//GEN-END:variables
}
