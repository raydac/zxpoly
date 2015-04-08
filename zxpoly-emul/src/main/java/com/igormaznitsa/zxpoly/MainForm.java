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

import com.igormaznitsa.zxpoly.ui.SelectTapPosDialog;
import com.igormaznitsa.zxpoly.utils.Utils;
import com.igormaznitsa.zxpoly.components.*;
import com.igormaznitsa.zxpoly.components.betadisk.BetaDiscInterface;
import com.igormaznitsa.zxpoly.components.betadisk.TRDOSDisk;
import com.igormaznitsa.zxpoly.formats.*;
import com.igormaznitsa.zxpoly.ui.*;
import com.igormaznitsa.zxpoly.utils.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.RenderedImage;
import java.io.*;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

public class MainForm extends javax.swing.JFrame implements Runnable, ActionListener {

  private static final Icon ICO_MOUSE = new ImageIcon(Utils.loadIcon("mouse.png"));
  private static final Icon ICO_MOUSE_DIS = UIManager.getLookAndFeel().getDisabledIcon(null, ICO_MOUSE);
  private static final Icon ICO_DISK = new ImageIcon(Utils.loadIcon("disk.png"));
  private static final Icon ICO_DISK_DIS = UIManager.getLookAndFeel().getDisabledIcon(null, ICO_DISK);
  private static final Icon ICO_TAPE = new ImageIcon(Utils.loadIcon("cassette.png"));
  private static final Icon ICO_TAPE_DIS = UIManager.getLookAndFeel().getDisabledIcon(null, ICO_TAPE);
  private static final Icon ICO_TURBO = new ImageIcon(Utils.loadIcon("turbo.png"));
  private static final Icon ICO_TURBO_DIS = UIManager.getLookAndFeel().getDisabledIcon(null, ICO_TURBO);
  private static final Icon ICO_ZX128 = new ImageIcon(Utils.loadIcon("zx128.png"));
  private static final Icon ICO_ZX128_DIS = UIManager.getLookAndFeel().getDisabledIcon(null, ICO_ZX128);

  private static final long TIMER_INT_DELAY_MILLISECONDS = 20L;
  private static final int HOW_MANY_INT_BETWEEN_SCREEN_REFRESH = 4;

  private volatile boolean turboMode = false;

  private File lastTapFolder;
  private File lastFloppyFolder;
  private File lastSnapshotFolder;
  private File lastScreenshotFolder;

  private final CPULoadIndicator indicatorCPU0 = new CPULoadIndicator(48, 14, 4, "CPU0", Color.GREEN, Color.DARK_GRAY, Color.WHITE);
  private final CPULoadIndicator indicatorCPU1 = new CPULoadIndicator(48, 14, 4, "CPU1", Color.GREEN, Color.DARK_GRAY, Color.WHITE);
  private final CPULoadIndicator indicatorCPU2 = new CPULoadIndicator(48, 14, 4, "CPU2", Color.GREEN, Color.DARK_GRAY, Color.WHITE);
  private final CPULoadIndicator indicatorCPU3 = new CPULoadIndicator(48, 14, 4, "CPU3", Color.GREEN, Color.DARK_GRAY, Color.WHITE);

  private final Runnable infobarUpdater = new Runnable() {
    @Override
    public void run() {
      final Icon turboico = turboMode ? ICO_TURBO : ICO_TURBO_DIS;
      if (labelTurbo.getIcon() != turboico) {
        labelTurbo.setIcon(turboico);
      }

      final TapeFileReader reader = keyboardAndTapeModule.getTap();
      final Icon tapico = reader != null && reader.isPlaying() ? ICO_TAPE : ICO_TAPE_DIS;
      if (labelTapeUsage.getIcon() != tapico) {
        labelTapeUsage.setIcon(tapico);
      }

      final Icon mouseIcon = board.getVideoController().isHoldMouse() ? ICO_MOUSE : ICO_MOUSE_DIS;
      if (labelMouseUsage.getIcon() != mouseIcon) {
        labelMouseUsage.setIcon(mouseIcon);
      }

      final Icon diskIcon = board.getBetaDiskInterface().isActive() ? ICO_DISK : ICO_DISK_DIS;
      if (labelDiskUsage.getIcon() != diskIcon) {
        labelDiskUsage.setIcon(diskIcon);
      }

      final Icon zx128Icon = board.isZXPolyMode() ? ICO_ZX128_DIS : ICO_ZX128;
      if (labelZX128.getIcon() != zx128Icon) {
        labelZX128.setIcon(zx128Icon);
      }

      if (panelIndicators.isVisible()) {
        indicatorCPU0.updateForState(board.getActivityCPU0());
        indicatorCPU1.updateForState(board.getActivityCPU1());
        indicatorCPU2.updateForState(board.getActivityCPU2());
        indicatorCPU3.updateForState(board.getActivityCPU3());
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

  private static class PNGFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".png");
    }

    @Override
    public String getDescription() {
      return "PNG image (*.png)";
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

  private RomData loadRom(final String romPath) throws IOException {
    if (romPath != null) {
      if (romPath.contains("://")) {
        try {
          final String cached = "loaded_" + Integer.toHexString(romPath.hashCode()).toUpperCase(Locale.ENGLISH) + ".rom";
          final File cacheFolder = new File(AppOptions.getInstance().getAppConfigFolder(), "cache");
          final File cachedRom = new File(cacheFolder, cached);
          RomData result = null;
          boolean load = true;
          if (cachedRom.isFile()) {
            log.info("Load cached ROM downloaded from '"+ romPath+"' : " + cachedRom);
            result = new RomData(FileUtils.readFileToByteArray(cachedRom));
            load = false;
          }

          if (load) {
            log.info("Load ROM from external URL: " + romPath);
            result = ROMLoader.getROMFrom(romPath);
            if (cacheFolder.isDirectory() || cacheFolder.mkdirs()) {
              FileUtils.writeByteArrayToFile(cachedRom, result.getAsArray());
              log.info("Loaded ROM saved in cache as file : " + romPath);
            }
          }
          return result;
        }
        catch (Exception ex) {
          log.log(Level.WARNING, "Can't load ROM from '" + romPath + "\'", ex);
        }
      }
      else {
        log.info("Load ROM from embedded resource '" + romPath + "'");
        return RomData.read(Utils.findResourceOrError("com/igormaznitsa/zxpoly/rom/" + romPath));
      }
    }

    final String testRom = "zxpolytest.rom";
    log.info("Load ROM from embedded resource '" + testRom + "'");
    return RomData.read(Utils.findResourceOrError("com/igormaznitsa/zxpoly/rom/" + testRom));
  }

  public MainForm(final String title, final String romPath) throws IOException {
    initComponents();
    this.setTitle(title);

    this.getInputContext().selectInputMethod(Locale.ENGLISH);

    setIconImage(Utils.loadIcon("appico.png"));

    final RomData rom = loadRom(romPath);

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

    final GridBagConstraints cpuIndicatorConstraint = new GridBagConstraints();
    cpuIndicatorConstraint.ipadx = 5;

    this.panelIndicators.add(this.indicatorCPU0, cpuIndicatorConstraint, 0);
    this.panelIndicators.add(this.indicatorCPU1, cpuIndicatorConstraint, 1);
    this.panelIndicators.add(this.indicatorCPU2, cpuIndicatorConstraint, 2);
    this.panelIndicators.add(this.indicatorCPU3, cpuIndicatorConstraint, 3);

    updateTapeMenu();

    final Thread daemon = new Thread(this, "ZXPolyThread");
    daemon.setDaemon(true);
    daemon.start();

    updateInfoPanel();

    pack();

    this.setLocationRelativeTo(null);
  }

  private void updateTapeMenu() {
    final TapeFileReader reader = this.keyboardAndTapeModule.getTap();
    if (reader == null) {
      this.menuTap.setEnabled(false);
      this.menuTapPlay.setSelected(false);
      this.menuTapExportAs.setEnabled(false);
    }
    else {
      this.menuTap.setEnabled(true);
      this.menuTapPlay.setSelected(reader.isPlaying());
      this.menuTapExportAs.setEnabled(true);
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

        if (countdownToPaint <= 0) {
          countdownToPaint = HOW_MANY_INT_BETWEEN_SCREEN_REFRESH;
          updateScreen();
        }

        if (countToUpdatePanel <= 0) {
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
    jSeparator2 = new javax.swing.JSeparator();
    panelIndicators = new javax.swing.JPanel();
    filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
    labelTurbo = new javax.swing.JLabel();
    labelMouseUsage = new javax.swing.JLabel();
    labelZX128 = new javax.swing.JLabel();
    labelTapeUsage = new javax.swing.JLabel();
    labelDiskUsage = new javax.swing.JLabel();
    menuBar = new javax.swing.JMenuBar();
    menuFile = new javax.swing.JMenu();
    menuFileLoadSnapshot = new javax.swing.JMenuItem();
    menuFileLoadTap = new javax.swing.JMenuItem();
    menuLoadDrive = new javax.swing.JMenu();
    menuFileSelectDiskA = new javax.swing.JMenuItem();
    jMenuItem1 = new javax.swing.JMenuItem();
    jMenuItem2 = new javax.swing.JMenuItem();
    jMenuItem3 = new javax.swing.JMenuItem();
    jSeparator1 = new javax.swing.JPopupMenu.Separator();
    menuFileOptions = new javax.swing.JMenuItem();
    jSeparator3 = new javax.swing.JPopupMenu.Separator();
    menuFileExit = new javax.swing.JMenuItem();
    menuTap = new javax.swing.JMenu();
    menuTapeRewindToStart = new javax.swing.JMenuItem();
    menuTapPrevBlock = new javax.swing.JMenuItem();
    menuTapPlay = new javax.swing.JCheckBoxMenuItem();
    menuTapNextBlock = new javax.swing.JMenuItem();
    menuTapGotoBlock = new javax.swing.JMenuItem();
    menuService = new javax.swing.JMenu();
    menuFileReset = new javax.swing.JMenuItem();
    menuResetKeyboard = new javax.swing.JMenuItem();
    menuServiceSaveScreen = new javax.swing.JMenuItem();
    menuTapExportAs = new javax.swing.JMenu();
    menuTapExportAsWav = new javax.swing.JMenuItem();
    menuOptions = new javax.swing.JMenu();
    menuOptionsShowIndicators = new javax.swing.JCheckBoxMenuItem();
    menuOptionsZX128Mode = new javax.swing.JCheckBoxMenuItem();
    menuOptionsTurbo = new javax.swing.JCheckBoxMenuItem();
    menuHelp = new javax.swing.JMenu();
    menuHelpAbout = new javax.swing.JMenuItem();

    setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
    setLocationByPlatform(true);
    addWindowFocusListener(new java.awt.event.WindowFocusListener() {
      public void windowGainedFocus(java.awt.event.WindowEvent evt) {
        formWindowGainedFocus(evt);
      }
      public void windowLostFocus(java.awt.event.WindowEvent evt) {
        formWindowLostFocus(evt);
      }
    });

    scrollPanel.setViewportView(jSeparator2);

    getContentPane().add(scrollPanel, java.awt.BorderLayout.CENTER);

    panelIndicators.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    panelIndicators.setLayout(new java.awt.GridBagLayout());
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1000.0;
    panelIndicators.add(filler1, gridBagConstraints);

    labelTurbo.setText(" ");
    labelTurbo.setToolTipText("Shows turbo mode on");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelTurbo, gridBagConstraints);

    labelMouseUsage.setText(" ");
    labelMouseUsage.setToolTipText("Indicates kempston mouse activation, ESC - deactivate mouse");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelMouseUsage, gridBagConstraints);

    labelZX128.setText(" ");
    labelZX128.setToolTipText("Shows that active ZX128 emulation mode");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelZX128, gridBagConstraints);

    labelTapeUsage.setText(" ");
    labelTapeUsage.setToolTipText("Shows tape activity");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelTapeUsage, gridBagConstraints);

    labelDiskUsage.setText(" ");
    labelDiskUsage.setToolTipText("Shows disk activity");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 9;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelDiskUsage, gridBagConstraints);

    getContentPane().add(panelIndicators, java.awt.BorderLayout.SOUTH);

    menuFile.setText("File");

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

    menuLoadDrive.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/disk.png"))); // NOI18N
    menuLoadDrive.setText("Load Disk..");

    menuFileSelectDiskA.setText("Drive A");
    menuFileSelectDiskA.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileSelectDiskAActionPerformed(evt);
      }
    });
    menuLoadDrive.add(menuFileSelectDiskA);

    jMenuItem1.setText("Drive B");
    jMenuItem1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem1ActionPerformed(evt);
      }
    });
    menuLoadDrive.add(jMenuItem1);

    jMenuItem2.setText("Drive C");
    jMenuItem2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem2ActionPerformed(evt);
      }
    });
    menuLoadDrive.add(jMenuItem2);

    jMenuItem3.setText("Drive D");
    jMenuItem3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        jMenuItem3ActionPerformed(evt);
      }
    });
    menuLoadDrive.add(jMenuItem3);

    menuFile.add(menuLoadDrive);
    menuFile.add(jSeparator1);

    menuFileOptions.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/settings.png"))); // NOI18N
    menuFileOptions.setText("Options");
    menuFileOptions.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileOptionsActionPerformed(evt);
      }
    });
    menuFile.add(menuFileOptions);
    menuFile.add(jSeparator3);

    menuFileExit.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
    menuFileExit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/reset.png"))); // NOI18N
    menuFileExit.setText("Exit");
    menuFileExit.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileExitActionPerformed(evt);
      }
    });
    menuFile.add(menuFileExit);

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

    menuBar.add(menuTap);

    menuService.setText("Service");

    menuFileReset.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F12, 0));
    menuFileReset.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/reset2.png"))); // NOI18N
    menuFileReset.setText("Reset");
    menuFileReset.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileResetActionPerformed(evt);
      }
    });
    menuService.add(menuFileReset);

    menuResetKeyboard.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/keyboard.png"))); // NOI18N
    menuResetKeyboard.setText("Reset keyboard");
    menuResetKeyboard.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuResetKeyboardActionPerformed(evt);
      }
    });
    menuService.add(menuResetKeyboard);

    menuServiceSaveScreen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F8, 0));
    menuServiceSaveScreen.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/photo.png"))); // NOI18N
    menuServiceSaveScreen.setText("Make Screenshot");
    menuServiceSaveScreen.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuServiceSaveScreenActionPerformed(evt);
      }
    });
    menuService.add(menuServiceSaveScreen);

    menuTapExportAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_record.png"))); // NOI18N
    menuTapExportAs.setText("Export TAPE as..");

    menuTapExportAsWav.setText("WAV file");
    menuTapExportAsWav.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTapExportAsWavActionPerformed(evt);
      }
    });
    menuTapExportAs.add(menuTapExportAsWav);

    menuService.add(menuTapExportAs);

    menuBar.add(menuService);

    menuOptions.setText("Options");

    menuOptionsShowIndicators.setSelected(true);
    menuOptionsShowIndicators.setText("Indicator panel");
    menuOptionsShowIndicators.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/indicator.png"))); // NOI18N
    menuOptionsShowIndicators.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsShowIndicatorsActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsShowIndicators);

    menuOptionsZX128Mode.setSelected(true);
    menuOptionsZX128Mode.setText("ZX 128 Mode");
    menuOptionsZX128Mode.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/zx128.png"))); // NOI18N
    menuOptionsZX128Mode.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsZX128ModeActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsZX128Mode);

    menuOptionsTurbo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
    menuOptionsTurbo.setSelected(true);
    menuOptionsTurbo.setText("Turbo");
    menuOptionsTurbo.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/turbo.png"))); // NOI18N
    menuOptionsTurbo.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsTurboActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsTurbo);

    menuBar.add(menuOptions);

    menuHelp.setText("Help");

    menuHelpAbout.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
    menuHelpAbout.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/info.png"))); // NOI18N
    menuHelpAbout.setText("About");
    menuHelpAbout.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuHelpAboutActionPerformed(evt);
      }
    });
    menuHelp.add(menuHelpAbout);

    menuBar.add(menuHelp);

    setJMenuBar(menuBar);

    pack();
  }// </editor-fold>//GEN-END:initComponents

  private void menuFileResetActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileResetActionPerformed
    this.board.reset();
  }//GEN-LAST:event_menuFileResetActionPerformed

  private void menuOptionsShowIndicatorsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsShowIndicatorsActionPerformed
    this.indicatorCPU0.clear();
    this.indicatorCPU1.clear();
    this.indicatorCPU2.clear();
    this.indicatorCPU3.clear();
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

  private void updateInfoPanel() {
    if (SwingUtilities.isEventDispatchThread()) {
      infobarUpdater.run();
    }
    else {
      SwingUtilities.invokeLater(infobarUpdater);
    }
  }

  private void menuFileSelectDiskAActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileSelectDiskAActionPerformed
    loadDiskIntoDrive(BetaDiscInterface.DRIVE_A);
  }//GEN-LAST:event_menuFileSelectDiskAActionPerformed

  private void formWindowLostFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowLostFocus
    this.stepSemaphor.lock();
    try {
      this.keyboardAndTapeModule.doReset();
    }
    finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_formWindowLostFocus

  private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
    this.stepSemaphor.lock();
    try {
      this.getInputContext().selectInputMethod(Locale.ENGLISH);
      this.keyboardAndTapeModule.doReset();
    }
    finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_formWindowGainedFocus

  private void menuFileLoadSnapshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileLoadSnapshotActionPerformed
    stepSemaphor.lock();
    try {
      this.board.forceResetCPUs();
      this.board.resetIODevices();

      final AtomicReference<FileFilter> theFilter = new AtomicReference<>();
      final File selected = chooseFileForOpen("Select snapshot", this.lastSnapshotFolder, theFilter, new FormatZXP(), new FormatZ80(), new FormatSNA());
      if (selected != null) {
        this.lastSnapshotFolder = selected.getParentFile();
        try {
          final Snapshot selectedFilter = (Snapshot) theFilter.get();
          log.info("Loading snapshot " + selectedFilter.getName());
          selectedFilter.loadFromArray(this.board, this.board.getVideoController(), FileUtils.readFileToByteArray(selected));
        }
        catch (Exception ex) {
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

  private void menuFileExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileExitActionPerformed
    this.dispose();
  }//GEN-LAST:event_menuFileExitActionPerformed

  private void menuTapGotoBlockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTapGotoBlockActionPerformed
    final TapeFileReader currentReader = this.keyboardAndTapeModule.getTap();
    if (currentReader != null) {
      currentReader.stopPlay();
      updateTapeMenu();
      final SelectTapPosDialog dialog = new SelectTapPosDialog(this, currentReader);
      dialog.setVisible(true);
      final int selected = dialog.getSelectedIndex();
      if (selected >= 0) {
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

        if (this.keyboardAndTapeModule.getTap() != null) {
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

  private void menuServiceSaveScreenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuServiceSaveScreenActionPerformed
    final RenderedImage img = this.board.getVideoController().makeCopyOfCurrentPicture();
    final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    this.stepSemaphor.lock();
    try {
      ImageIO.write(img, "png", buffer);
      final File thefile = chooseFileForSave("Save screenshot", lastScreenshotFolder, new PNGFileFilter());
      if (thefile != null) {
        this.lastScreenshotFolder = thefile.getParentFile();
        FileUtils.writeByteArrayToFile(thefile, buffer.toByteArray());
      }
    }
    catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Can't save screenshot for error, see the log!", "Error", JOptionPane.ERROR_MESSAGE);
      log.log(Level.SEVERE, "Can't make screenshot", ex);
    }
    finally {
      this.keyboardAndTapeModule.doReset();
      this.stepSemaphor.unlock();
    }

  }//GEN-LAST:event_menuServiceSaveScreenActionPerformed

  private void menuResetKeyboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuResetKeyboardActionPerformed
    this.keyboardAndTapeModule.doReset();
  }//GEN-LAST:event_menuResetKeyboardActionPerformed

  private void menuFileOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileOptionsActionPerformed
    final OptionsDialog dialog = new OptionsDialog(this);
    dialog.setVisible(true);
  }//GEN-LAST:event_menuFileOptionsActionPerformed

  private void menuHelpAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuHelpAboutActionPerformed
    new AboutDialog(this).setVisible(true);
  }//GEN-LAST:event_menuHelpAboutActionPerformed

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
    if (e.getSource() instanceof TapeFileReader) {
      updateTapeMenu();
    }
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.Box.Filler filler1;
  private javax.swing.JMenuItem jMenuItem1;
  private javax.swing.JMenuItem jMenuItem2;
  private javax.swing.JMenuItem jMenuItem3;
  private javax.swing.JPopupMenu.Separator jSeparator1;
  private javax.swing.JSeparator jSeparator2;
  private javax.swing.JPopupMenu.Separator jSeparator3;
  private javax.swing.JLabel labelDiskUsage;
  private javax.swing.JLabel labelMouseUsage;
  private javax.swing.JLabel labelTapeUsage;
  private javax.swing.JLabel labelTurbo;
  private javax.swing.JLabel labelZX128;
  private javax.swing.JMenuBar menuBar;
  private javax.swing.JMenu menuFile;
  private javax.swing.JMenuItem menuFileExit;
  private javax.swing.JMenuItem menuFileLoadSnapshot;
  private javax.swing.JMenuItem menuFileLoadTap;
  private javax.swing.JMenuItem menuFileOptions;
  private javax.swing.JMenuItem menuFileReset;
  private javax.swing.JMenuItem menuFileSelectDiskA;
  private javax.swing.JMenu menuHelp;
  private javax.swing.JMenuItem menuHelpAbout;
  private javax.swing.JMenu menuLoadDrive;
  private javax.swing.JMenu menuOptions;
  private javax.swing.JCheckBoxMenuItem menuOptionsShowIndicators;
  private javax.swing.JCheckBoxMenuItem menuOptionsTurbo;
  private javax.swing.JCheckBoxMenuItem menuOptionsZX128Mode;
  private javax.swing.JMenuItem menuResetKeyboard;
  private javax.swing.JMenu menuService;
  private javax.swing.JMenuItem menuServiceSaveScreen;
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
