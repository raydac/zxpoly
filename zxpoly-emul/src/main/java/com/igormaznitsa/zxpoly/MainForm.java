/*
 * Copyright (C) 2014 Raydac Research Group Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed getSignal the hope that it will be useful,
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
import java.awt.event.*;
import java.io.*;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class MainForm extends javax.swing.JFrame implements Runnable, ActionListener {

  private static final Icon ICO_EMPTY_16x16 = new ImageIcon(Utils.loadIcon("empty.png"));
  private static final Icon ICO_MOUSE = new ImageIcon(Utils.loadIcon("mouse.png"));
  private static final Icon ICO_DISK = new ImageIcon(Utils.loadIcon("disk.png"));
  private static final Icon ICO_TAPE = new ImageIcon(Utils.loadIcon("cassette.png"));
  private static final Icon ICO_TURBO = new ImageIcon(Utils.loadIcon("turbo.png"));
  private static final Icon ICO_ZX128 = new ImageIcon(Utils.loadIcon("zx128.png"));
  
  private static final long TIMER_INT_DELAY_MILLISECONDS = 20L;
  private static final int HOW_MANY_INT_BETWEEN_SCREEN_REFRESH = 4;

  private volatile boolean turboMode = false;

  private File lastTapFolder;
  private File lastFloppyFolder;
  private File lastSnapshotFolder;

  private final Runnable infobarUpdater = new Runnable() {
    @Override
    public void run() {
      final Icon turboico = turboMode ? ICO_TURBO : ICO_EMPTY_16x16;
      if (labelTurbo.getIcon() != turboico) {
        labelTurbo.setIcon(turboico);
      }

      final TapeFileReader reader = keyboardAndTapeModule.getTap();
      final Icon tapico = reader != null && reader.isPlaying() ? ICO_TAPE : ICO_EMPTY_16x16;
      if (labelTapeUsage.getIcon() != tapico) {
        labelTapeUsage.setIcon(tapico);
      }

      final Icon mouseIcon = board.getVideoController().isHoldMouse() ? ICO_MOUSE : ICO_EMPTY_16x16;
      if (labelMouseUsage.getIcon() != mouseIcon) {
        labelMouseUsage.setIcon(mouseIcon);
      }

      final Icon diskIcon = board.getBetaDiskInterface().isActive() ? ICO_DISK : ICO_EMPTY_16x16;
      if (labelDiskUsage.getIcon() != diskIcon) {
        labelDiskUsage.setIcon(diskIcon);
      }

      final Icon zx128Icon = board.isZXPolyMode()? ICO_EMPTY_16x16 : ICO_ZX128;
      if (labelZX128.getIcon() != zx128Icon) {
        labelZX128.setIcon(zx128Icon);
      }

    }
  };

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

  private static class SCLFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".scl");
    }

    @Override
    public String getDescription() {
      return "SCL image (*.scl)";
    }

  }

  private static class TapFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".tap");
    }

    @Override
    public String getDescription() {
      return "TAP file (*.tap)";
    }

  }

  private static class WavFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".wav");
    }

    @Override
    public String getDescription() {
      return "WAV file (*.wav)";
    }

  }

  private static final long serialVersionUID = 7309959798344327441L;
  public static final Logger log = Logger.getLogger("UI");

  private final Motherboard board;
  private final KeyboardKempstonAndTapeIn keyboardAndTapeModule;
  private final KempstonMouse kempstonMouse;

  private final ReentrantLock stepSemaphor = new ReentrantLock();

  private static class KeyboardDispatcher implements KeyEventDispatcher {

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

    setIconImage(Utils.loadIcon("appico.png"));
    
    log.info("Loading test rom [" + romResource + ']');
    final RomData rom = RomData.read(Utils.findResourceOrError("com/igormaznitsa/zxpoly/rom/" + romResource));
    this.board = new Motherboard(rom);
    this.board.setZXPolyMode(true);
    this.menuOptionsZX128Mode.setSelected(!this.board.isZXPolyMode());
    this.menuOptionsTurbo.setSelected(this.turboMode);

    log.info("Main form completed");
    this.board.reset();

    this.scrollPanel.getViewport().add(this.board.getVideoController());
    this.keyboardAndTapeModule = this.board.findIODevice(KeyboardKempstonAndTapeIn.class);
    this.kempstonMouse = this.board.findIODevice(KempstonMouse.class);
    
    final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyboardDispatcher(this.keyboardAndTapeModule));
    
    
    updateTapeMenu();

    final Thread daemon = new Thread(this, "ZXPolyThread");
    daemon.setDaemon(true);
    daemon.start();

    updateInfoPanel();
    
    pack();
  }

  private void updateTapeMenu() {
    final TapeFileReader reader = this.keyboardAndTapeModule.getTap();
    if (reader == null) {
      this.menuTap.setEnabled(false);
      this.menuTapPlay.setSelected(false);
    }
    else {
      this.menuTap.setEnabled(true);
      this.menuTapPlay.setSelected(reader.isPlaying());
    }

  }

  @Override
  public void run() {
    final int INT_TO_UPDATE_INFOPANEL = 10;
    
    long nextSystemInt = System.currentTimeMillis() + TIMER_INT_DELAY_MILLISECONDS;
    int countdownToPaint = 0;

    int countToUpdatePanel = INT_TO_UPDATE_INFOPANEL;
    
    while (!Thread.currentThread().isInterrupted()) {
      final long currentMachineCycleCounter = this.board.getCPU0().getMachineCycles();
      long currentTime = System.currentTimeMillis();

      stepSemaphor.lock();
      try {

        final boolean systemIntSignal;
        if (nextSystemInt <= currentTime) {
          systemIntSignal = currentMachineCycleCounter >= VideoController.CYCLES_BETWEEN_INT;
          nextSystemInt = currentTime + TIMER_INT_DELAY_MILLISECONDS;
          this.board.getCPU0().resetMCycleCounter();
          countdownToPaint--;
          countToUpdatePanel--;
        }
        else {
          systemIntSignal = false;
        }

        this.board.step(systemIntSignal, this.turboMode ? true : systemIntSignal || currentMachineCycleCounter <= VideoController.CYCLES_BETWEEN_INT);

        if (countdownToPaint<=0) {
          countdownToPaint = HOW_MANY_INT_BETWEEN_SCREEN_REFRESH;
          updateScreen();
        }
        
        if (countToUpdatePanel<=0){
          countToUpdatePanel = INT_TO_UPDATE_INFOPANEL;
          updateInfoPanel();
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
    vc.paintImmediately(0, 0, vc.getWidth(), vc.getHeight());;
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  @SuppressWarnings("unchecked")
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    scrollPanel = new javax.swing.JScrollPane();
    panelIndicators = new javax.swing.JPanel();
    filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
    labelTurbo = new javax.swing.JLabel();
    labelMouseUsage = new javax.swing.JLabel();
    labelZX128 = new javax.swing.JLabel();
    labelTapeUsage = new javax.swing.JLabel();
    labelDiskUsage = new javax.swing.JLabel();
    menuBar = new javax.swing.JMenuBar();
    menuFile = new javax.swing.JMenu();
    menuFileReset = new javax.swing.JMenuItem();
    menuFileLoadSnapshot = new javax.swing.JMenuItem();
    menuFileLoadTap = new javax.swing.JMenuItem();
    jMenu1 = new javax.swing.JMenu();
    menuFileSelectDiskA = new javax.swing.JMenuItem();
    jMenuItem1 = new javax.swing.JMenuItem();
    jMenuItem2 = new javax.swing.JMenuItem();
    jMenuItem3 = new javax.swing.JMenuItem();
    jSeparator1 = new javax.swing.JPopupMenu.Separator();
    jMenuItem4 = new javax.swing.JMenuItem();
    menuTap = new javax.swing.JMenu();
    menuTapeRewindToStart = new javax.swing.JMenuItem();
    menuTapPrevBlock = new javax.swing.JMenuItem();
    menuTapPlay = new javax.swing.JCheckBoxMenuItem();
    menuTapNextBlock = new javax.swing.JMenuItem();
    menuTapGotoBlock = new javax.swing.JMenuItem();
    jSeparator2 = new javax.swing.JPopupMenu.Separator();
    menuTapExportAs = new javax.swing.JMenu();
    menuTapExportAsWav = new javax.swing.JMenuItem();
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
    panelIndicators.setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1000.0;
    panelIndicators.add(filler1, gridBagConstraints);

    labelTurbo.setText(" ");
    labelTurbo.setToolTipText("Shows turbo mode on");
    panelIndicators.add(labelTurbo, new java.awt.GridBagConstraints());

    labelMouseUsage.setText(" ");
    labelMouseUsage.setToolTipText("Indicates kempston mouse activation, ESC - deactivate mouse");
    panelIndicators.add(labelMouseUsage, new java.awt.GridBagConstraints());

    labelZX128.setText(" ");
    labelZX128.setToolTipText("Shows that active ZX128 emulation mode");
    panelIndicators.add(labelZX128, new java.awt.GridBagConstraints());

    labelTapeUsage.setText(" ");
    labelTapeUsage.setToolTipText("Shows tape activity");
    panelIndicators.add(labelTapeUsage, new java.awt.GridBagConstraints());

    labelDiskUsage.setText(" ");
    labelDiskUsage.setToolTipText("Shows disk activity");
    panelIndicators.add(labelDiskUsage, new java.awt.GridBagConstraints());

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

    menuFileLoadSnapshot.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/snapshot.png"))); // NOI18N
    menuFileLoadSnapshot.setText("Load Snapshot");
    menuFileLoadSnapshot.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileLoadSnapshotActionPerformed(evt);
      }
    });
    menuFile.add(menuFileLoadSnapshot);

    menuFileLoadTap.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/cassette.png"))); // NOI18N
    menuFileLoadTap.setText("Load TAPE");
    menuFileLoadTap.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileLoadTapActionPerformed(evt);
      }
    });
    menuFile.add(menuFileLoadTap);

    jMenu1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/disk.png"))); // NOI18N
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

    menuTap.setText("Tape");

    menuTapeRewindToStart.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_previous.png"))); // NOI18N
    menuTapeRewindToStart.setText("Rewind to start");
    menuTapeRewindToStart.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTapeRewindToStartActionPerformed(evt);
      }
    });
    menuTap.add(menuTapeRewindToStart);

    menuTapPrevBlock.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_backward.png"))); // NOI18N
    menuTapPrevBlock.setText("Prev block");
    menuTapPrevBlock.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTapPrevBlockActionPerformed(evt);
      }
    });
    menuTap.add(menuTapPrevBlock);

    menuTapPlay.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
    menuTapPlay.setText("Play");
    menuTapPlay.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_play.png"))); // NOI18N
    menuTapPlay.setInheritsPopupMenu(true);
    menuTapPlay.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTapPlayActionPerformed(evt);
      }
    });
    menuTap.add(menuTapPlay);

    menuTapNextBlock.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_forward.png"))); // NOI18N
    menuTapNextBlock.setText("Next block");
    menuTapNextBlock.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTapNextBlockActionPerformed(evt);
      }
    });
    menuTap.add(menuTapNextBlock);

    menuTapGotoBlock.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_pos.png"))); // NOI18N
    menuTapGotoBlock.setText("Go to block");
    menuTapGotoBlock.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTapGotoBlockActionPerformed(evt);
      }
    });
    menuTap.add(menuTapGotoBlock);
    menuTap.add(jSeparator2);

    menuTapExportAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_record.png"))); // NOI18N
    menuTapExportAs.setText("Export as..");

    menuTapExportAsWav.setText("WAV file");
    menuTapExportAsWav.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTapExportAsWavActionPerformed(evt);
      }
    });
    menuTapExportAs.add(menuTapExportAsWav);

    menuTap.add(menuTapExportAs);

    menuBar.add(menuTap);

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
    this.stepSemaphor.lock();
    try {
      final char diskName;
      switch (drive) {
        case BetaDiscInterface.DRIVE_A:
          diskName = 'A';
          break;
        case BetaDiscInterface.DRIVE_B:
          diskName = 'B';
          break;
        case BetaDiscInterface.DRIVE_C:
          diskName = 'C';
          break;
        case BetaDiscInterface.DRIVE_D:
          diskName = 'D';
          break;
        default:
          throw new Error("Unexpected drive index");
      }
      final AtomicReference<FileFilter> filter = new AtomicReference<>();
      final File selectedFile = chooseFileForOpen("Select Disk " + diskName, this.lastFloppyFolder, filter, new SCLFileFilter(), new TRDFileFilter());
      if (selectedFile != null) {
        this.lastFloppyFolder = selectedFile.getParentFile();
        try {
          final TRDOSDisk floppy = new TRDOSDisk(filter.get().getClass() == SCLFileFilter.class ? TRDOSDisk.Source.SCL : TRDOSDisk.Source.TRD, FileUtils.readFileToByteArray(selectedFile), false);
          this.board.getBetaDiskInterface().insertDiskIntoDrive(drive, floppy);
          log.info("Loaded drive " + diskName + " by floppy image file " + selectedFile);
        }
        catch (IOException ex) {
          log.log(Level.WARNING, "Can't read Floppy image file [" + selectedFile + ']', ex);
          JOptionPane.showMessageDialog(this, "Can't read Floppy image file", "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
    finally {
      this.stepSemaphor.unlock();
    }
  }

  private void updateInfoPanel(){
    if (SwingUtilities.isEventDispatchThread()){
      infobarUpdater.run();
    }else{
      SwingUtilities.invokeLater(infobarUpdater);
    }
  }
  
  private void menuFileSelectDiskAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileSelectDiskAActionPerformed
    loadDiskIntoDrive(BetaDiscInterface.DRIVE_A);
  }//GEN-LAST:event_menuFileSelectDiskAActionPerformed

  private void formWindowLostFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowLostFocus
    this.stepSemaphor.lock();
    try{
    this.keyboardAndTapeModule.reset();
    }finally{
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_formWindowLostFocus

  private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
    this.stepSemaphor.lock();
    try{
    this.getInputContext().selectInputMethod(Locale.ENGLISH);
    this.keyboardAndTapeModule.reset();
    }finally{
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_formWindowGainedFocus

  private void menuFileLoadSnapshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileLoadSnapshotActionPerformed
    stepSemaphor.lock();
    try {
      final AtomicReference<FileFilter> theFilter = new AtomicReference<>();
      final File selected = chooseFileForOpen("Select snapshot", this.lastSnapshotFolder, theFilter, new FormatZ80(), new FormatSNA());
      if (selected != null) {
        this.lastSnapshotFolder = selected.getParentFile();
        try {
          final Snapshot selectedFilter = (Snapshot) theFilter.get();
          stepSemaphor.lock();
          try {
            log.info("Loading snapshot "+selectedFilter.getName());
            selectedFilter.loadFromArray(this.board, this.board.getVideoController(), FileUtils.readFileToByteArray(selected));
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
      this.keyboardAndTapeModule.reset();
      this.kempstonMouse.reset();
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

  private void menuTapGotoBlockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTapGotoBlockActionPerformed
    final TapeFileReader currentReader = this.keyboardAndTapeModule.getTap();
    if (currentReader!=null){
      currentReader.stopPlay();
      updateTapeMenu();
      final SelectTapPosDialog dialog = new SelectTapPosDialog(this, currentReader);
      dialog.setVisible(true);
      final int selected = dialog.getSelectedIndex();
      if (selected>=0){
        currentReader.setCurrent(selected);
      }
    }
  }//GEN-LAST:event_menuTapGotoBlockActionPerformed

  private void menuFileLoadTapActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileLoadTapActionPerformed
    final File selectedTapFile = chooseFileForOpen("Load Tape", this.lastTapFolder, null, new TapFileFilter());
    if (selectedTapFile != null) {
      this.lastTapFolder = selectedTapFile.getParentFile();
      InputStream in = null;
      try {
        in = new BufferedInputStream(new FileInputStream(selectedTapFile));
        
        if (this.keyboardAndTapeModule.getTap()!=null){
          this.keyboardAndTapeModule.getTap().removeActionListener(this);
        }
        
        final TapeFileReader tapfile = new TapeFileReader(selectedTapFile.getAbsolutePath(), in);
        tapfile.addActionListener(this);
        this.keyboardAndTapeModule.setTap(tapfile);
      }
      catch (Exception ex) {
        log.log(Level.SEVERE, "Can't read " + selectedTapFile, ex);
        JOptionPane.showMessageDialog(this, "Can't load TAP file", ex.getMessage(), JOptionPane.ERROR_MESSAGE);
      }
      finally {
        IOUtils.closeQuietly(in);
        updateTapeMenu();
      }
    }
  }//GEN-LAST:event_menuFileLoadTapActionPerformed

  private void menuTapExportAsWavActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTapExportAsWavActionPerformed
    this.stepSemaphor.lock();
    try {
      final byte[] wav = this.keyboardAndTapeModule.getTap().getAsWAV();
      final File fileToSave = chooseFileForSave("Select WAV file", null, new WavFileFilter());
      if (fileToSave != null) {
        FileUtils.writeByteArrayToFile(fileToSave, wav);
        log.info("Exported current TAP file as WAV file " + fileToSave + " size " + wav.length + " bytes");
      }
    }
    catch (Exception ex) {
      ex.printStackTrace();
      log.log(Level.WARNING, "Can't export as WAV", ex);
      JOptionPane.showMessageDialog(this, "Can't export as WAV", ex.getMessage(), JOptionPane.ERROR_MESSAGE);
    }
    finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuTapExportAsWavActionPerformed

  private void menuTapPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTapPlayActionPerformed
    if (this.menuTapPlay.isSelected()) {
      this.keyboardAndTapeModule.getTap().startPlay();
    }
    else {
      this.keyboardAndTapeModule.getTap().stopPlay();
    }
    updateTapeMenu();
  }//GEN-LAST:event_menuTapPlayActionPerformed

  private void menuTapPrevBlockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTapPrevBlockActionPerformed
    final TapeFileReader tap = this.keyboardAndTapeModule.getTap();
    if (tap != null) {
      tap.rewindToPrevBlock();
    }
    updateTapeMenu();
  }//GEN-LAST:event_menuTapPrevBlockActionPerformed

  private void menuTapNextBlockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTapNextBlockActionPerformed
    final TapeFileReader tap = this.keyboardAndTapeModule.getTap();
    if (tap != null) {
      tap.rewindToNextBlock();
    }
    updateTapeMenu();
  }//GEN-LAST:event_menuTapNextBlockActionPerformed

  private void menuTapeRewindToStartActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTapeRewindToStartActionPerformed
    final TapeFileReader tap = this.keyboardAndTapeModule.getTap();
    if (tap != null) {
      tap.rewindToStart();
    }
    updateTapeMenu();
  }//GEN-LAST:event_menuTapeRewindToStartActionPerformed

  private File chooseFileForOpen(final String title, final File initial, final AtomicReference<FileFilter> selectedFilter, final FileFilter... filter) {
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

  private File chooseFileForSave(final String title, final File initial, final FileFilter filter) {
    final JFileChooser chooser = new JFileChooser(initial);
    chooser.addChoosableFileFilter(filter);
    chooser.setAcceptAllFileFilterUsed(true);
    chooser.setMultiSelectionEnabled(false);
    chooser.setDialogTitle(title);
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    final File result;
    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      result = chooser.getSelectedFile();
    }
    else {
      result = null;
    }
    return result;
  }

  @Override
  public void actionPerformed(final ActionEvent e) {
    if (e.getSource() instanceof TapeFileReader){
      updateTapeMenu();
    }
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.Box.Filler filler1;
  private javax.swing.JMenu jMenu1;
  private javax.swing.JMenuItem jMenuItem1;
  private javax.swing.JMenuItem jMenuItem2;
  private javax.swing.JMenuItem jMenuItem3;
  private javax.swing.JMenuItem jMenuItem4;
  private javax.swing.JPopupMenu.Separator jSeparator1;
  private javax.swing.JPopupMenu.Separator jSeparator2;
  private javax.swing.JLabel labelDiskUsage;
  private javax.swing.JLabel labelMouseUsage;
  private javax.swing.JLabel labelTapeUsage;
  private javax.swing.JLabel labelTurbo;
  private javax.swing.JLabel labelZX128;
  private javax.swing.JMenuBar menuBar;
  private javax.swing.JMenu menuFile;
  private javax.swing.JMenuItem menuFileLoadSnapshot;
  private javax.swing.JMenuItem menuFileLoadTap;
  private javax.swing.JMenuItem menuFileReset;
  private javax.swing.JMenuItem menuFileSelectDiskA;
  private javax.swing.JMenu menuOptions;
  private javax.swing.JCheckBoxMenuItem menuOptionsShowIndicators;
  private javax.swing.JCheckBoxMenuItem menuOptionsTurbo;
  private javax.swing.JCheckBoxMenuItem menuOptionsZX128Mode;
  private javax.swing.JMenu menuTap;
  private javax.swing.JMenu menuTapExportAs;
  private javax.swing.JMenuItem menuTapExportAsWav;
  private javax.swing.JMenuItem menuTapGotoBlock;
  private javax.swing.JMenuItem menuTapNextBlock;
  private javax.swing.JCheckBoxMenuItem menuTapPlay;
  private javax.swing.JMenuItem menuTapPrevBlock;
  private javax.swing.JMenuItem menuTapeRewindToStart;
  private javax.swing.JPanel panelIndicators;
  private javax.swing.JScrollPane scrollPanel;
  // End of variables declaration//GEN-END:variables
}
