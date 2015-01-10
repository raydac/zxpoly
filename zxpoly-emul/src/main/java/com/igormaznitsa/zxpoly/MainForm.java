/*
 * Copyright (C) 2014 Raydac Research Group Ltd.
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
package com.igormaznitsa.zxpoly;

import com.igormaznitsa.zxpoly.components.*;
import com.igormaznitsa.zxpoly.components.betadisk.BetaDiscInterface;
import com.igormaznitsa.zxpoly.components.betadisk.TRDOSDisk;
import com.igormaznitsa.zxpoly.formats.*;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FileUtils;

public class MainForm extends javax.swing.JFrame implements Runnable {

  private static final long CYCLES_BETWEEN_INT = 64000L;
  private static final long TIMER_INT_DELAY_MILLISECONDS = 20L;
  private static final long SCREEN_REFRESH_DELAY_MILLISECONDS = 100L;

  private volatile boolean turboMode = false;

  private static class TRDFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".trd");
    }

    @Override
    public String getDescription() {
      return "TR-DOS image (*.trd)";
    }

  }

  private static final long serialVersionUID = 7309959798344327441L;
  public static final Logger log = Logger.getLogger("UI");

  private final Motherboard board;
  private final ReentrantLock stepSemaphor = new ReentrantLock();

  private class KeyboardDispatcher implements KeyEventDispatcher {

    private final KeyboardKempstonAndTapeIn keyboard;

    public KeyboardDispatcher(final KeyboardKempstonAndTapeIn kbd) {
      this.keyboard = kbd;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
      this.keyboard.onKeyEvent(e);
      return false;
    }
  }

  public MainForm(final String romResource) throws IOException {
    initComponents();
    this.getInputContext().selectInputMethod(Locale.ENGLISH);

    log.info("Loading test rom [" + romResource + ']');
    final RomData rom = RomData.read(Utils.findResourceOrError("com/igormaznitsa/zxpoly/rom/" + romResource));
    this.board = new Motherboard(rom);
    this.board.setZXPolyMode(true);
    this.menuOptionsZX128Mode.setSelected(!this.board.isZXPolyMode());
    this.menuOptionsTurbo.setSelected(this.turboMode);

    log.info("Main form completed");
    this.board.reset();

    this.scrollPanel.getViewport().add(this.board.getVideoController());

    final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyboardDispatcher(this.board.getKeyboard()));

    final Thread daemon = new Thread(this, "ZXPolyThread");
    daemon.setDaemon(true);
    daemon.start();

    pack();
  }

  @Override
  public void run() {
    long nextSystemInt = System.currentTimeMillis() + TIMER_INT_DELAY_MILLISECONDS;
    long nextScreenRefresh = System.currentTimeMillis() + SCREEN_REFRESH_DELAY_MILLISECONDS;

    while (!Thread.currentThread().isInterrupted()) {
      final long currentMachineCycleCounter = this.board.getCPU0().getMachineCycles();
      long currentTime = System.currentTimeMillis();
      
      stepSemaphor.lock();
      try {

        final boolean systemIntSignal;
        if (nextSystemInt <= currentTime) {
          systemIntSignal = currentMachineCycleCounter >= CYCLES_BETWEEN_INT;
          nextSystemInt = currentTime + TIMER_INT_DELAY_MILLISECONDS;
          this.board.getCPU0().resetMCycleCounter();
        }
        else {
          systemIntSignal = false;
        }

        
        this.board.step(systemIntSignal, this.turboMode ? true : systemIntSignal || currentMachineCycleCounter <= CYCLES_BETWEEN_INT);

        currentTime = System.currentTimeMillis();

        if (nextScreenRefresh <= currentTime) {
          updateScreen();
          nextScreenRefresh = currentTime + SCREEN_REFRESH_DELAY_MILLISECONDS;
        }
      }
      finally {
        stepSemaphor.unlock();
      }
    }
  }

  public void setTurboMode(final boolean value) {
    this.turboMode = value;
  }

  public boolean isTurboMode() {
    return this.isTurboMode();
  }

  private void updateScreen() {
    final VideoController vc = board.getVideoController();
    vc.updateBuffer();
    vc.repaint();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {

    scrollPanel = new javax.swing.JScrollPane();
    panelIndicators = new javax.swing.JPanel();
    menuBar = new javax.swing.JMenuBar();
    menuFile = new javax.swing.JMenu();
    menuFileReset = new javax.swing.JMenuItem();
    menuFileLoadSnapshot = new javax.swing.JMenuItem();
    jMenu1 = new javax.swing.JMenu();
    menuFileSelectDiskA = new javax.swing.JMenuItem();
    jMenuItem1 = new javax.swing.JMenuItem();
    jMenuItem2 = new javax.swing.JMenuItem();
    jMenuItem3 = new javax.swing.JMenuItem();
    jSeparator1 = new javax.swing.JPopupMenu.Separator();
    jMenuItem4 = new javax.swing.JMenuItem();
    menuOptions = new javax.swing.JMenu();
    menuOptionsShowIndicators = new javax.swing.JCheckBoxMenuItem();
    menuOptionsZX128Mode = new javax.swing.JCheckBoxMenuItem();
    menuOptionsTurbo = new javax.swing.JCheckBoxMenuItem();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    addWindowFocusListener(new java.awt.event.WindowFocusListener() {
      public void windowGainedFocus(java.awt.event.WindowEvent evt) {
        formWindowGainedFocus(evt);
      }
      public void windowLostFocus(java.awt.event.WindowEvent evt) {
        formWindowLostFocus(evt);
      }
    });
    getContentPane().add(scrollPanel, java.awt.BorderLayout.CENTER);

    panelIndicators.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    panelIndicators.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
    getContentPane().add(panelIndicators, java.awt.BorderLayout.SOUTH);

    menuFile.setText("File");

    menuFileReset.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F12, 0));
    menuFileReset.setText("Reset");
    menuFileReset.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileResetActionPerformed(evt);
      }
    });
    menuFile.add(menuFileReset);

    menuFileLoadSnapshot.setText("Load Snapshot");
    menuFileLoadSnapshot.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileLoadSnapshotActionPerformed(evt);
      }
    });
    menuFile.add(menuFileLoadSnapshot);

    jMenu1.setText("Load Disk..");

    menuFileSelectDiskA.setText("Drive A");
    menuFileSelectDiskA.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileSelectDiskAActionPerformed(evt);
      }
    });
    jMenu1.add(menuFileSelectDiskA);

    jMenuItem1.setText("Drive B");
    jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem1ActionPerformed(evt);
      }
    });
    jMenu1.add(jMenuItem1);

    jMenuItem2.setText("Drive C");
    jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem2ActionPerformed(evt);
      }
    });
    jMenu1.add(jMenuItem2);

    jMenuItem3.setText("Drive D");
    jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem3ActionPerformed(evt);
      }
    });
    jMenu1.add(jMenuItem3);

    menuFile.add(jMenu1);
    menuFile.add(jSeparator1);

    jMenuItem4.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
    jMenuItem4.setText("Exit");
    jMenuItem4.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem4ActionPerformed(evt);
      }
    });
    menuFile.add(jMenuItem4);

    menuBar.add(menuFile);

    menuOptions.setText("Options");

    menuOptionsShowIndicators.setSelected(true);
    menuOptionsShowIndicators.setText("Show indicator panel");
    menuOptionsShowIndicators.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsShowIndicatorsActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsShowIndicators);

    menuOptionsZX128Mode.setSelected(true);
    menuOptionsZX128Mode.setText("ZX 128 Mode");
    menuOptionsZX128Mode.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsZX128ModeActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsZX128Mode);

    menuOptionsTurbo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
    menuOptionsTurbo.setSelected(true);
    menuOptionsTurbo.setText("Turbo");
    menuOptionsTurbo.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsTurboActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsTurbo);

    menuBar.add(menuOptions);

    setJMenuBar(menuBar);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void menuFileResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileResetActionPerformed
    this.board.reset();
  }//GEN-LAST:event_menuFileResetActionPerformed

  private void menuOptionsShowIndicatorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsShowIndicatorsActionPerformed
    this.panelIndicators.setVisible(this.menuOptionsShowIndicators.isSelected());
  }//GEN-LAST:event_menuOptionsShowIndicatorsActionPerformed

  private void loadDiskIntoDrive(final int drive) {
    final char diskName;
    switch(drive){
      case BetaDiscInterface.DRIVE_A : diskName = 'A';break;
      case BetaDiscInterface.DRIVE_B : diskName = 'B';break;
      case BetaDiscInterface.DRIVE_C : diskName = 'C';break;
      case BetaDiscInterface.DRIVE_D : diskName = 'D';break;
      default: throw new Error("Unexpected drive index");
    }
    
    final File selectedFile = chooseFile("Select Disk "+diskName, null, null, new TRDFileFilter());
    if (selectedFile != null) {
      try {
        final TRDOSDisk floppy = new TRDOSDisk(FileUtils.readFileToByteArray(selectedFile), false);
        this.board.getBetaDiskInterface().insertDiskIntoDrive(drive, floppy);
        log.info("Loaded drive "+diskName + " by file " + selectedFile);
      }
      catch (IOException ex) {
        log.log(Level.WARNING, "Can't read TRD file [" + selectedFile + ']', ex);
        JOptionPane.showMessageDialog(this, "Can't read TRD file", "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }
  
  private void menuFileSelectDiskAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileSelectDiskAActionPerformed
    loadDiskIntoDrive(BetaDiscInterface.DRIVE_A);
  }//GEN-LAST:event_menuFileSelectDiskAActionPerformed

  private void formWindowLostFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowLostFocus
    this.board.getKeyboard().reset();
  }//GEN-LAST:event_formWindowLostFocus

  private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
    this.getInputContext().selectInputMethod(Locale.ENGLISH);
    this.board.getKeyboard().reset();
  }//GEN-LAST:event_formWindowGainedFocus

  private void menuFileLoadSnapshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileLoadSnapshotActionPerformed
    stepSemaphor.lock();
    try {
      final AtomicReference<FileFilter> theFilter = new AtomicReference<>();
      final File selected = chooseFile("Select snapshot", null, theFilter, new FormatZ80(), new FormatSNA());
      if (selected != null) {
        try {
          final Snapshot selectedFilter = (Snapshot) theFilter.get();
          selectedFilter.load(FileUtils.readFileToByteArray(selected));
          log.info("Loaded image from file " + selectedFilter.getName());
          stepSemaphor.lock();
          try {
            this.board.loadSnapshot(selectedFilter, false);
          }
          finally {
            stepSemaphor.unlock();
          }
        }
        catch (IOException ex) {
          log.log(Level.WARNING, "Can't read snapshot file [" + ex.getMessage() + ']', ex);
          JOptionPane.showMessageDialog(this, "Can't read snapshot file [" + ex.getMessage() + ']', "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
    finally {
      stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuFileLoadSnapshotActionPerformed

  private void menuOptionsZX128ModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsZX128ModeActionPerformed
    this.stepSemaphor.lock();
    try {
      this.board.setZXPolyMode(!this.menuOptionsZX128Mode.isSelected());
    }
    finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuOptionsZX128ModeActionPerformed

  private void menuOptionsTurboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsTurboActionPerformed
    this.turboMode = this.menuOptionsTurbo.isSelected();
  }//GEN-LAST:event_menuOptionsTurboActionPerformed

  private void jMenuItem2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem2ActionPerformed
    loadDiskIntoDrive(BetaDiscInterface.DRIVE_C);
  }//GEN-LAST:event_jMenuItem2ActionPerformed

  private void jMenuItem1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem1ActionPerformed
    loadDiskIntoDrive(BetaDiscInterface.DRIVE_B);
  }//GEN-LAST:event_jMenuItem1ActionPerformed

  private void jMenuItem3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem3ActionPerformed
    loadDiskIntoDrive(BetaDiscInterface.DRIVE_D);
  }//GEN-LAST:event_jMenuItem3ActionPerformed

  private void jMenuItem4ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jMenuItem4ActionPerformed
    this.dispose();
  }//GEN-LAST:event_jMenuItem4ActionPerformed

  private File chooseFile(final String title, final File initial, final AtomicReference<FileFilter> selectedFilter, final FileFilter... filter) {
    final JFileChooser chooser = new JFileChooser(initial);
    for (final FileFilter f : filter) {
      chooser.addChoosableFileFilter(f);
    }
    chooser.setAcceptAllFileFilterUsed(false);
    chooser.setMultiSelectionEnabled(false);
    chooser.setDialogTitle(title);
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    final File result;
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      result = chooser.getSelectedFile();
      if (selectedFilter != null) {
        selectedFilter.set(chooser.getFileFilter());
      }
    }
    else {
      result = null;
    }
    return result;
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.JMenu jMenu1;
  private javax.swing.JMenuItem jMenuItem1;
  private javax.swing.JMenuItem jMenuItem2;
  private javax.swing.JMenuItem jMenuItem3;
  private javax.swing.JMenuItem jMenuItem4;
  private javax.swing.JPopupMenu.Separator jSeparator1;
  private javax.swing.JMenuBar menuBar;
  private javax.swing.JMenu menuFile;
  private javax.swing.JMenuItem menuFileLoadSnapshot;
  private javax.swing.JMenuItem menuFileReset;
  private javax.swing.JMenuItem menuFileSelectDiskA;
  private javax.swing.JMenu menuOptions;
  private javax.swing.JCheckBoxMenuItem menuOptionsShowIndicators;
  private javax.swing.JCheckBoxMenuItem menuOptionsTurbo;
  private javax.swing.JCheckBoxMenuItem menuOptionsZX128Mode;
  private javax.swing.JPanel panelIndicators;
  private javax.swing.JScrollPane scrollPanel;
  // End of variables declaration//GEN-END:variables
}
