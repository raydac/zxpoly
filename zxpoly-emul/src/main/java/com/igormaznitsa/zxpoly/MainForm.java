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
package com.igormaznitsa.zxpoly;

import static com.igormaznitsa.z80.Utils.toHex;
import static com.igormaznitsa.z80.Utils.toHexByte;
import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.utils.Utils;
import com.igormaznitsa.zxpoly.components.*;
import com.igormaznitsa.zxpoly.components.betadisk.BetaDiscInterface;
import com.igormaznitsa.zxpoly.components.betadisk.TRDOSDisk;
import com.igormaznitsa.zxpoly.formats.*;
import com.igormaznitsa.zxpoly.tracer.TraceCPUForm;
import com.igormaznitsa.zxpoly.ui.*;
import com.igormaznitsa.zxpoly.utils.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FileUtils;
import com.igormaznitsa.zxpoly.animeencoders.ZXPolyAGifEncoder;
import com.igormaznitsa.zxpoly.animeencoders.AnimatedGifTunePanel;
import com.igormaznitsa.zxpoly.animeencoders.AnimationEncoder;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.apache.commons.io.FilenameUtils;

public final class MainForm extends javax.swing.JFrame implements Runnable, ActionListener {

  private static final Icon ICO_MOUSE = new ImageIcon(Utils.loadIcon("mouse.png"));
  private static final Icon ICO_MOUSE_DIS = UIManager.getLookAndFeel().getDisabledIcon(null, ICO_MOUSE);
  private static final Icon ICO_DISK = new ImageIcon(Utils.loadIcon("disk.png"));
  private static final Icon ICO_DISK_DIS = UIManager.getLookAndFeel().getDisabledIcon(null, ICO_DISK);
  private static final Icon ICO_AGIF_RECORD = new ImageIcon(Utils.loadIcon("record.png"));
  private static final Icon ICO_AGIF_STOP = new ImageIcon(Utils.loadIcon("tape_stop.png"));
  private static final Icon ICO_TAPE = new ImageIcon(Utils.loadIcon("cassette.png"));
  private static final Icon ICO_TAPE_DIS = UIManager.getLookAndFeel().getDisabledIcon(null, ICO_TAPE);
  private static final Icon ICO_TURBO = new ImageIcon(Utils.loadIcon("turbo.png"));
  private static final Icon ICO_TURBO_DIS = UIManager.getLookAndFeel().getDisabledIcon(null, ICO_TURBO);
  private static final Icon ICO_ZX128 = new ImageIcon(Utils.loadIcon("zx128.png"));
  private static final Icon ICO_ZX128_DIS = UIManager.getLookAndFeel().getDisabledIcon(null, ICO_ZX128);

  private static final Icon ICO_EMUL_PLAY = new ImageIcon(Utils.loadIcon("emul_play.png"));
  private static final Icon ICO_EMUL_PAUSE = new ImageIcon(Utils.loadIcon("emul_pause.png"));

  public static final long TIMER_INT_DELAY_MILLISECONDS = 20L;
  private static final int INT_BETWEEN_FRAMES = AppOptions.getInstance().getIntBetweenFrames();

  private static final String TEXT_START_ANIM_GIF = "Record AGIF";
  private static final String TEXT_STOP_ANIM_GIF = "Stop AGIF";

  private final AtomicBoolean turboMode = new AtomicBoolean();

  private AnimatedGifTunePanel.AnimGifOptions lastAnimGifOptions = new AnimatedGifTunePanel.AnimGifOptions("./zxpoly.gif", 10, false);

  private File lastTapFolder;
  private File lastFloppyFolder;
  private File lastSnapshotFolder;
  private File lastScreenshotFolder;

  private final CPULoadIndicator indicatorCPU0 = new CPULoadIndicator(48, 14, 4, "CPU0", Color.GREEN, Color.DARK_GRAY, Color.WHITE);
  private final CPULoadIndicator indicatorCPU1 = new CPULoadIndicator(48, 14, 4, "CPU1", Color.GREEN, Color.DARK_GRAY, Color.WHITE);
  private final CPULoadIndicator indicatorCPU2 = new CPULoadIndicator(48, 14, 4, "CPU2", Color.GREEN, Color.DARK_GRAY, Color.WHITE);
  private final CPULoadIndicator indicatorCPU3 = new CPULoadIndicator(48, 14, 4, "CPU3", Color.GREEN, Color.DARK_GRAY, Color.WHITE);

  private final TraceCPUForm[] cpuTracers = new TraceCPUForm[4];
  private final AtomicInteger activeTracerWindowCounter = new AtomicInteger();

  public static final AtomicReference<MainForm> theInstance = new AtomicReference<>();

  private final AtomicReference<AnimationEncoder> currentAnimationEncoder = new AtomicReference<>();
  private final Runnable traceWindowsUpdater = new Runnable() {

    @Override
    public void run() {
      int index = 0;
      for (final TraceCPUForm form : cpuTracers) {
        if (form != null) {
          final Z80 cpu = board.getZXPolyModules()[index++].getCPU();
          if (cpu.getPrefixInProcessing() == 0 && !cpu.isInsideBlockLoop()) {
            form.refreshViewState();
          }
        }
      }
    }
  };

  private final Runnable infobarUpdater = new Runnable() {
    @Override
    public void run() {
      final Icon turboico = turboMode.get() ? ICO_TURBO : ICO_TURBO_DIS;
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

      updateTracerCheckBoxes();
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

    private final VideoController videoController;
    private final KeyboardKempstonAndTapeIn keyboard;

    public KeyboardDispatcher(final VideoController videoController, final KeyboardKempstonAndTapeIn kbd) {
      this.keyboard = kbd;
      this.videoController = videoController;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
      if (!e.isConsumed()) {
        if (e.getKeyCode() == KeyEvent.VK_F5) {
          this.videoController.setShowZxKeyboardLayout(e.getID() == KeyEvent.KEY_PRESSED);
          e.consume();
        } else {
          this.keyboard.onKeyEvent(e);
        }
      }
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
            log.log(Level.INFO, "Load cached ROM downloaded from '" + romPath + "' : " + cachedRom);
            result = new RomData(FileUtils.readFileToByteArray(cachedRom));
            load = false;
          }

          if (load) {
            log.log(Level.INFO, "Load ROM from external URL: " + romPath);
            result = ROMLoader.getROMFrom(romPath);
            if (cacheFolder.isDirectory() || cacheFolder.mkdirs()) {
              FileUtils.writeByteArrayToFile(cachedRom, result.getAsArray());
              log.log(Level.INFO, "Loaded ROM saved in cache as file : " + romPath);
            }
          }
          return result;
        } catch (Exception ex) {
          log.log(Level.WARNING, "Can't load ROM from '" + romPath + "\'", ex);
        }
      } else {
        log.log(Level.INFO, "Load ROM from embedded resource '" + romPath + "'");
        try (final InputStream in = Utils.findResourceOrError("com/igormaznitsa/zxpoly/rom/" + romPath)) {
          return RomData.read(in);
        }
      }
    }

    final String testRom = AppOptions.TEST_ROM;
    log.info("Load ROM from embedded resource '" + testRom + "'");
    try (final InputStream in = Utils.findResourceOrError("com/igormaznitsa/zxpoly/rom/" + testRom)) {
      return RomData.read(in);
    }
  }

  public MainForm(final String title, final String romPath) throws IOException {
    log.log(Level.INFO, "INT ticks between frames: " + INT_BETWEEN_FRAMES);
    initComponents();

    this.menuBar.add(Box.createHorizontalGlue());
    final JToggleButton buttonStartPause = new JToggleButton();
    buttonStartPause.setFocusable(false);

    buttonStartPause.setIcon(ICO_EMUL_PAUSE);
    buttonStartPause.setRolloverEnabled(false);
    buttonStartPause.setToolTipText("Play/Pause emulation");

    buttonStartPause.addActionListener((final ActionEvent event) -> {
      final JToggleButton source = (JToggleButton) event.getSource();
      if (source.isSelected()) {
        MainForm.this.stepSemaphor.lock();
        source.setIcon(ICO_EMUL_PLAY);
        log.info("Emulator is paused by PLAY/PAUSE button");
      } else {
        MainForm.this.stepSemaphor.unlock();
        source.setIcon(ICO_EMUL_PAUSE);
        log.info("Emulator is started by PLAY/PAUSE button");
      }
    });

    this.menuBar.add(buttonStartPause);

    this.setTitle(title);

    this.menuActionAnimatedGIF.setText(TEXT_START_ANIM_GIF);
    this.menuActionAnimatedGIF.setIcon(ICO_AGIF_RECORD);

    this.getInputContext().selectInputMethod(Locale.ENGLISH);

    setIconImage(Utils.loadIcon("appico.png"));

    final RomData rom = loadRom(romPath);

    this.board = new Motherboard(rom);
    this.board.setZXPolyMode(true);
    this.menuOptionsZX128Mode.setSelected(!this.board.isZXPolyMode());
    this.menuOptionsTurbo.setSelected(this.turboMode.get());

    log.info("Main form completed");
    this.board.reset();

    this.scrollPanel.getViewport().add(this.board.getVideoController());
    this.keyboardAndTapeModule = this.board.findIODevice(KeyboardKempstonAndTapeIn.class);
    this.kempstonMouse = this.board.findIODevice(KempstonMouse.class);

    final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyboardDispatcher(this.board.getVideoController(), this.keyboardAndTapeModule));

    final GridBagConstraints cpuIndicatorConstraint = new GridBagConstraints();
    cpuIndicatorConstraint.ipadx = 5;

    this.panelIndicators.add(this.indicatorCPU0, cpuIndicatorConstraint, 0);
    this.panelIndicators.add(this.indicatorCPU1, cpuIndicatorConstraint, 1);
    this.panelIndicators.add(this.indicatorCPU2, cpuIndicatorConstraint, 2);
    this.panelIndicators.add(this.indicatorCPU3, cpuIndicatorConstraint, 3);

    for (final Component item : this.menuBar.getComponents()) {
      if (item instanceof JMenu) {
        final JMenu menuItem = (JMenu) item;
        menuItem.addMenuListener(new MenuListener() {
          @Override
          public void menuSelected(MenuEvent e) {
            MainForm.this.stepSemaphor.lock();
          }

          @Override
          public void menuDeselected(MenuEvent e) {
            MainForm.this.stepSemaphor.unlock();
          }

          @Override
          public void menuCanceled(MenuEvent e) {
            MainForm.this.stepSemaphor.unlock();
          }
        });
      }
    }

    updateTapeMenu();

    updateInfoPanel();

    pack();

    this.setLocationRelativeTo(null);

    theInstance.set(this);

    SwingUtilities.invokeLater(() -> {
      final Thread daemon = new Thread(this, "ZXPolyThread");
      daemon.setDaemon(true);
      daemon.start();
    });
  }

  public static MainForm getInstance() {
    return theInstance.get();
  }

  private void updateTapeMenu() {
    final TapeFileReader reader = this.keyboardAndTapeModule.getTap();
    if (reader == null) {
      this.menuTap.setEnabled(false);
      this.menuTapPlay.setSelected(false);
      this.menuTapExportAs.setEnabled(false);
    } else {
      this.menuTap.setEnabled(true);
      this.menuTapPlay.setSelected(reader.isPlaying());
      this.menuTapExportAs.setEnabled(true);
    }

  }

  @Override
  public void run() {
    final int INT_TO_UPDATE_INFOPANEL = 20;

    long nextSystemInt = System.currentTimeMillis() + TIMER_INT_DELAY_MILLISECONDS;
    int countdownToPaint = 0;
    int countdownToAnimationSave = 0;

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
          countdownToAnimationSave--;
        } else {
          systemIntSignal = false;
        }

        final int triggers = this.board.step(systemIntSignal, this.turboMode.get() ? true : systemIntSignal || currentMachineCycleCounter <= VideoController.CYCLES_BETWEEN_INT);

        if (triggers != Motherboard.TRIGGER_NONE) {
          final Z80[] cpuStates = new Z80[4];
          final int lastM1Address = this.board.getZXPolyModules()[0].getLastM1Address();
          for (int i = 0; i < 4; i++) {
            cpuStates[i] = new Z80(this.board.getZXPolyModules()[i].getCPU());
          }
          SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
              onTrigger(triggers, lastM1Address, cpuStates);
            }
          });
        }

        if (countdownToPaint <= 0) {
          countdownToPaint = INT_BETWEEN_FRAMES;
          updateScreen();
        }

        if (countdownToAnimationSave <= 0) {
          final AnimationEncoder theAnimationEncoder = this.currentAnimationEncoder.get();
          if (theAnimationEncoder == null) {
            countdownToAnimationSave = 0;
          } else {
            countdownToAnimationSave = theAnimationEncoder.getIntsBetweenFrames();
            try {
              theAnimationEncoder.saveFrame(board.getVideoController().makeCopyOfVideoBuffer());
            } catch (IOException ex) {
              log.warning("Can't write animation frame");
            }
          }
        }

        if (countToUpdatePanel <= 0) {
          countToUpdatePanel = INT_TO_UPDATE_INFOPANEL;
          updateInfoPanel();
        }
      } finally {
        stepSemaphor.unlock();
      }

      if (this.activeTracerWindowCounter.get() > 0) {
        updateTracerWindowsForStep();
      }
    }
  }

  private void updateTracerWindowsForStep() {
    try {
      SwingUtilities.invokeAndWait(this.traceWindowsUpdater);
    } catch (InterruptedException ex) {
      log.log(Level.INFO, "Interrupted trace window updater");
    } catch (InvocationTargetException ex) {
      log.log(Level.SEVERE, "Error in trace window updater", ex);
    }
  }

  private String getCellContentForAddress(final int address) {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < 4; i++) {
      if (result.length() > 0) {
        result.append("  ");
      }
      result.append(toHexByte(this.board.getZXPolyModules()[i].readAddress(address)));
      result.append(" (").append(i).append(')');
    }

    return result.toString();
  }

  private void logTrigger(final int triggered, final int lastAddress, final Z80[] cpuModuleStates) {
    final StringBuilder buffer = new StringBuilder();
    buffer.append("TRIGGER: ");

    if ((triggered & Motherboard.TRIGGER_DIFF_MODULESTATES) != 0) {
      buffer.append("MODULE CPU DESYNCHRONIZATION");
    }

    if ((triggered & Motherboard.TRIGGER_DIFF_MEM_ADDR) != 0) {
      buffer.append("MEMORY CONTENT DIFFERENCE: ").append(toHex(this.board.getMemTriggerAddress()));
      buffer.append('\n').append(getCellContentForAddress(lastAddress)).append('\n');
    }

    if ((triggered & Motherboard.TRIGGER_DIFF_EXE_CODE) != 0) {
      buffer.append("EXE CODE DIFFERENCE");
    }

    buffer.append("\n\nDisasm since last executed address in CPU0 memory: ").append(toHex(lastAddress)).append('\n');

    buffer.append(this.board.getZXPolyModules()[0].toHexStringSinceAddress(lastAddress - 8, 8)).append("\n\n");

    this.board.getZXPolyModules()[0].disasmSinceAddress(lastAddress, 5).forEach((l) -> {
      buffer.append(l.toString()).append('\n');
    });

    buffer.append('\n');

    for (int i = 0; i < cpuModuleStates.length; i++) {
      buffer.append("CPU MODULE: ").append(i).append('\n');
      buffer.append(cpuModuleStates[i].getStateAsString());
      buffer.append("\n\n");
    }
    buffer.append('\n');
    log.info(buffer.toString());
  }

  private String makeInfoStringForRegister(final Z80[] cpuModuleStates, final int lastAddress, final String extraString, final int register, final boolean alt) {
    final StringBuilder result = new StringBuilder();

    if (extraString != null) {
      result.append(extraString).append('\n');
    }

    for (int i = 0; i < cpuModuleStates.length; i++) {
      if (i > 0) {
        result.append(", ");
      }
      result.append("CPU#").append(i).append('=').append(toHex(cpuModuleStates[i].getRegister(register, alt)));
    }

    result.append("\n\nLast executed address : ").append(toHex(lastAddress)).append("\n--------------\n\n");
    result.append(this.board.getZXPolyModules()[0].toHexStringSinceAddress(lastAddress - 8, 8)).append("\n\n");

    this.board.getZXPolyModules()[0].disasmSinceAddress(lastAddress, 5).forEach((l) -> {
      result.append(l.toString()).append('\n');
    });

    return result.toString();
  }

  public void onTrigger(final int triggered, final int lastM1Address, final Z80[] cpuModuleStates) {
    this.stepSemaphor.lock();
    try {
      logTrigger(triggered, lastM1Address, cpuModuleStates);

      if ((triggered & Motherboard.TRIGGER_DIFF_MODULESTATES) != 0) {
        this.menuTriggerModuleCPUDesync.setSelected(false);
        JOptionPane.showMessageDialog(theInstance.get(), "Detected desync of module CPUs\n" + makeInfoStringForRegister(cpuModuleStates, lastM1Address, null, Z80.REG_PC, false), "Triggered", JOptionPane.INFORMATION_MESSAGE);
      }

      if ((triggered & Motherboard.TRIGGER_DIFF_MEM_ADDR) != 0) {
        this.menuTriggerDiffMem.setSelected(false);
        JOptionPane.showMessageDialog(theInstance.get(), "Detected memory cell difference " + toHex(this.board.getMemTriggerAddress()) + "\n" + makeInfoStringForRegister(cpuModuleStates, lastM1Address, getCellContentForAddress(this.board.getMemTriggerAddress()), Z80.REG_PC, false), "Triggered", JOptionPane.INFORMATION_MESSAGE);
      }

      if ((triggered & Motherboard.TRIGGER_DIFF_EXE_CODE) != 0) {
        this.menuTriggerExeCodeDiff.setSelected(false);
        JOptionPane.showMessageDialog(theInstance.get(), "Detected EXE code difference\n" + makeInfoStringForRegister(cpuModuleStates, lastM1Address, null, Z80.REG_PC, false), "Triggered", JOptionPane.INFORMATION_MESSAGE);
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  public void setTurboMode(final boolean value) {
    this.turboMode.set(value);
  }

  public boolean isTurboMode() {
    return this.turboMode.get();
  }

  private void updateScreen() {
    final VideoController vc = board.getVideoController();
    vc.updateBuffer();
    vc.paintImmediately(0, 0, vc.getWidth(), vc.getHeight());
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
    menuFileFlushDiskChanges = new javax.swing.JMenuItem();
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
    menuServiceSaveScreenAllVRAM = new javax.swing.JMenuItem();
    menuActionAnimatedGIF = new javax.swing.JMenuItem();
    menuServicemakeSnapshot = new javax.swing.JMenuItem();
    menuTapExportAs = new javax.swing.JMenu();
    menuTapExportAsWav = new javax.swing.JMenuItem();
    menuCatcher = new javax.swing.JMenu();
    menuTriggerDiffMem = new javax.swing.JCheckBoxMenuItem();
    menuTriggerModuleCPUDesync = new javax.swing.JCheckBoxMenuItem();
    menuTriggerExeCodeDiff = new javax.swing.JCheckBoxMenuItem();
    menuTracer = new javax.swing.JMenu();
    menuTraceCPU0 = new javax.swing.JCheckBoxMenuItem();
    menuTraceCPU1 = new javax.swing.JCheckBoxMenuItem();
    menuTraceCPU2 = new javax.swing.JCheckBoxMenuItem();
    menuTraceCPU3 = new javax.swing.JCheckBoxMenuItem();
    menuOptions = new javax.swing.JMenu();
    menuOptionsShowIndicators = new javax.swing.JCheckBoxMenuItem();
    menuOptionsZX128Mode = new javax.swing.JCheckBoxMenuItem();
    menuOptionsTurbo = new javax.swing.JCheckBoxMenuItem();
    menuHelp = new javax.swing.JMenu();
    menuHelpAbout = new javax.swing.JMenuItem();

    setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    setLocationByPlatform(true);
    addWindowFocusListener(new java.awt.event.WindowFocusListener() {
      public void windowGainedFocus(java.awt.event.WindowEvent evt) {
        formWindowGainedFocus(evt);
      }
      public void windowLostFocus(java.awt.event.WindowEvent evt) {
        formWindowLostFocus(evt);
      }
    });
    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosed(java.awt.event.WindowEvent evt) {
        formWindowClosed(evt);
      }
      public void windowClosing(java.awt.event.WindowEvent evt) {
        formWindowClosing(evt);
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
    menuFile.addMenuListener(new javax.swing.event.MenuListener() {
      public void menuCanceled(javax.swing.event.MenuEvent evt) {
      }
      public void menuDeselected(javax.swing.event.MenuEvent evt) {
      }
      public void menuSelected(javax.swing.event.MenuEvent evt) {
        menuFileMenuSelected(evt);
      }
    });

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

    menuFileFlushDiskChanges.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/diskflush.png"))); // NOI18N
    menuFileFlushDiskChanges.setText("Flush disk changes");
    menuFileFlushDiskChanges.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileFlushDiskChangesActionPerformed(evt);
      }
    });
    menuFile.add(menuFileFlushDiskChanges);
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

    menuServiceSaveScreenAllVRAM.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/photom.png"))); // NOI18N
    menuServiceSaveScreenAllVRAM.setText("Make Screenshot of all VRAM");
    menuServiceSaveScreenAllVRAM.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuServiceSaveScreenAllVRAMActionPerformed(evt);
      }
    });
    menuService.add(menuServiceSaveScreenAllVRAM);

    menuActionAnimatedGIF.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/file_gif.png"))); // NOI18N
    menuActionAnimatedGIF.setText("Make Animated GIF");
    menuActionAnimatedGIF.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuActionAnimatedGIFActionPerformed(evt);
      }
    });
    menuService.add(menuActionAnimatedGIF);

    menuServicemakeSnapshot.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/save_snapshot.png"))); // NOI18N
    menuServicemakeSnapshot.setText("Save snapshot");
    menuServicemakeSnapshot.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuServicemakeSnapshotActionPerformed(evt);
      }
    });
    menuService.add(menuServicemakeSnapshot);

    menuTapExportAs.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_record.png"))); // NOI18N
    menuTapExportAs.setText("Export TAPE as..");

    menuTapExportAsWav.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/file_wav.png"))); // NOI18N
    menuTapExportAsWav.setText("WAV file");
    menuTapExportAsWav.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTapExportAsWavActionPerformed(evt);
      }
    });
    menuTapExportAs.add(menuTapExportAsWav);

    menuService.add(menuTapExportAs);

    menuCatcher.setText("Test triggers");

    menuTriggerDiffMem.setText("Diff mem.content");
    menuTriggerDiffMem.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTriggerDiffMemActionPerformed(evt);
      }
    });
    menuCatcher.add(menuTriggerDiffMem);

    menuTriggerModuleCPUDesync.setText("CPUs state desync");
    menuTriggerModuleCPUDesync.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTriggerModuleCPUDesyncActionPerformed(evt);
      }
    });
    menuCatcher.add(menuTriggerModuleCPUDesync);

    menuTriggerExeCodeDiff.setText("Exe code difference");
    menuTriggerExeCodeDiff.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTriggerExeCodeDiffActionPerformed(evt);
      }
    });
    menuCatcher.add(menuTriggerExeCodeDiff);

    menuService.add(menuCatcher);

    menuTracer.setText("Trace");

    menuTraceCPU0.setText("CPU0");
    menuTraceCPU0.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTraceCPU0ActionPerformed(evt);
      }
    });
    menuTracer.add(menuTraceCPU0);

    menuTraceCPU1.setText("CPU1");
    menuTraceCPU1.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTraceCPU1ActionPerformed(evt);
      }
    });
    menuTracer.add(menuTraceCPU1);

    menuTraceCPU2.setText("CPU2");
    menuTraceCPU2.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTraceCPU2ActionPerformed(evt);
      }
    });
    menuTracer.add(menuTraceCPU2);

    menuTraceCPU3.setText("CPU3");
    menuTraceCPU3.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuTraceCPU3ActionPerformed(evt);
      }
    });
    menuTracer.add(menuTraceCPU3);

    menuService.add(menuTracer);

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
    menuHelpAbout.setText("Help");
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
      File selectedFile = chooseFileForOpen("Select Disk " + diskName, this.lastFloppyFolder, filter, new SCLFileFilter(), new TRDFileFilter());
      if (selectedFile != null) {
        this.lastFloppyFolder = selectedFile.getParentFile();
        try {
          if (!selectedFile.isFile() && filter.get().getClass() == TRDFileFilter.class) {
            String name = selectedFile.getName();
            if (!name.contains(".")) {
              name += ".trd";
            }
            selectedFile = new File(selectedFile.getParentFile(), name);
            if (!selectedFile.isFile()) {
              if (JOptionPane.showConfirmDialog(this, "Create TRD file: " + selectedFile.getName() + "?", "Create TRD file", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                log.log(Level.INFO, "Creating TRD disk: " + selectedFile.getAbsolutePath());
                FileUtils.writeByteArrayToFile(selectedFile, new TRDOSDisk().getDiskData());
              } else {
                return;
              }
            }
          }

          final TRDOSDisk floppy = new TRDOSDisk(selectedFile, filter.get().getClass() == SCLFileFilter.class ? TRDOSDisk.SourceDataType.SCL : TRDOSDisk.SourceDataType.TRD, FileUtils.readFileToByteArray(selectedFile), false);
          this.board.getBetaDiskInterface().insertDiskIntoDrive(drive, floppy);
          log.log(Level.INFO, "Loaded drive " + diskName + " by floppy image file " + selectedFile);
        } catch (IOException ex) {
          log.log(Level.WARNING, "Can't read Floppy image file [" + selectedFile + ']', ex);
          JOptionPane.showMessageDialog(this, "Can't read Floppy image file", "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  private void updateInfoPanel() {
    if (SwingUtilities.isEventDispatchThread()) {
      infobarUpdater.run();
    } else {
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
    } finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_formWindowLostFocus

  private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowGainedFocus
    this.stepSemaphor.lock();
    try {
      this.getInputContext().selectInputMethod(Locale.ENGLISH);
      this.keyboardAndTapeModule.doReset();
    } finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_formWindowGainedFocus

  private void menuFileLoadSnapshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileLoadSnapshotActionPerformed
    stepSemaphor.lock();
    try {
      if (AppOptions.getInstance().isTestRomActive()) {
        JOptionPane.showMessageDialog(theInstance.get(), "<html><body><b>Test ROM is active!</b><br><br>ROM 128 is needed for snapshot loading.<br>Go to menu <b><i>File->Options</i></b> and choose ROM 128.</body></html>", "Test ROM detected", JOptionPane.ERROR_MESSAGE);
        return;
      }

      final AtomicReference<FileFilter> theFilter = new AtomicReference<>();
      final File selected = chooseFileForOpen("Select snapshot", this.lastSnapshotFolder, theFilter, new FormatZ80(), new FormatSNA(), new FormatZXP());

      if (selected != null) {
        this.board.forceResetCPUs();
        this.board.resetIODevices();

        this.lastSnapshotFolder = selected.getParentFile();
        try {
          final Snapshot selectedFilter = (Snapshot) theFilter.get();
          log.log(Level.INFO, "Loading snapshot " + selectedFilter.getName());
          selectedFilter.loadFromArray(selected, this.board, this.board.getVideoController(), FileUtils.readFileToByteArray(selected));
        } catch (Exception ex) {
          log.log(Level.WARNING, "Can't read snapshot file [" + ex.getMessage() + ']', ex);
          JOptionPane.showMessageDialog(this, "Can't read snapshot file [" + ex.getMessage() + ']', "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    } finally {
      stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuFileLoadSnapshotActionPerformed

  private void menuOptionsZX128ModeActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsZX128ModeActionPerformed
    this.stepSemaphor.lock();
    try {
      this.board.setZXPolyMode(!this.menuOptionsZX128Mode.isSelected());
    } finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuOptionsZX128ModeActionPerformed

  private void menuOptionsTurboActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsTurboActionPerformed
    this.setTurboMode(this.menuOptionsTurbo.isSelected());
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
    this.formWindowClosing(null);
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
    this.stepSemaphor.lock();
    try {
      final File selectedTapFile = chooseFileForOpen("Load Tape", this.lastTapFolder, null, new TapFileFilter());
      if (selectedTapFile != null) {
        this.lastTapFolder = selectedTapFile.getParentFile();
        try ( InputStream in = new BufferedInputStream(new FileInputStream(selectedTapFile))) {

          if (this.keyboardAndTapeModule.getTap() != null) {
            this.keyboardAndTapeModule.getTap().removeActionListener(this);
          }

          final TapeFileReader tapfile = new TapeFileReader(selectedTapFile.getAbsolutePath(), in);
          tapfile.addActionListener(this);
          this.keyboardAndTapeModule.setTap(tapfile);
        } catch (Exception ex) {
          log.log(Level.SEVERE, "Can't read " + selectedTapFile, ex);
          JOptionPane.showMessageDialog(this, "Can't load TAP file", ex.getMessage(), JOptionPane.ERROR_MESSAGE);
        } finally {
          updateTapeMenu();
        }
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuFileLoadTapActionPerformed

  private void menuTapExportAsWavActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTapExportAsWavActionPerformed
    this.stepSemaphor.lock();
    try {
      final byte[] wav = this.keyboardAndTapeModule.getTap().getAsWAV();
      File fileToSave = chooseFileForSave("Select WAV file", null, null, true, new WavFileFilter());
      if (fileToSave != null) {
        final String name = fileToSave.getName();
        if (!name.contains(".")) {
          fileToSave = new File(fileToSave.getParentFile(), name + ".wav");
        }
        FileUtils.writeByteArrayToFile(fileToSave, wav);
        log.log(Level.INFO, "Exported current TAP file as WAV file " + fileToSave + " size " + wav.length + " bytes");
      }
    } catch (Exception ex) {
      log.log(Level.WARNING, "Can't export as WAV", ex);
      JOptionPane.showMessageDialog(this, "Can't export as WAV", ex.getMessage(), JOptionPane.ERROR_MESSAGE);
    } finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuTapExportAsWavActionPerformed

  private void menuTapPlayActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTapPlayActionPerformed
    if (this.menuTapPlay.isSelected()) {
      this.keyboardAndTapeModule.getTap().startPlay();
    } else {
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
    this.stepSemaphor.lock();
    try {
      final RenderedImage img = this.board.getVideoController().makeCopyOfCurrentPicture();
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      ImageIO.write(img, "png", buffer);
      File pngFile = chooseFileForSave("Save screenshot", lastScreenshotFolder, null, true, new PNGFileFilter());
      if (pngFile != null) {
        final String fileName = pngFile.getName();
        if (!fileName.contains(".")) {
          pngFile = new File(pngFile.getParentFile(), fileName + ".png");
        }
        this.lastScreenshotFolder = pngFile.getParentFile();
        FileUtils.writeByteArrayToFile(pngFile, buffer.toByteArray());
      }
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Can't save screenshot for error, see the log!", "Error", JOptionPane.ERROR_MESSAGE);
      log.log(Level.SEVERE, "Can't make screenshot", ex);
    } finally {
      this.keyboardAndTapeModule.doReset();
      this.stepSemaphor.unlock();
    }

  }//GEN-LAST:event_menuServiceSaveScreenActionPerformed

  private void menuResetKeyboardActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuResetKeyboardActionPerformed
    this.keyboardAndTapeModule.doReset();
  }//GEN-LAST:event_menuResetKeyboardActionPerformed

  private void menuFileOptionsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileOptionsActionPerformed
    this.stepSemaphor.lock();
    try {
      final OptionsDialog dialog = new OptionsDialog(this);
      dialog.setVisible(true);
    } finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuFileOptionsActionPerformed

  private void menuHelpAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuHelpAboutActionPerformed
    this.stepSemaphor.lock();
    try {
      new AboutDialog(this).setVisible(true);
    } finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuHelpAboutActionPerformed

  private void menuTraceCPU0ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTraceCPU0ActionPerformed
    if (this.menuTraceCPU0.isSelected()) {
      activateTracerForCPUModule(0);
    } else {
      deactivateTracerForCPUModule(0);
    }
  }//GEN-LAST:event_menuTraceCPU0ActionPerformed

  private void menuTraceCPU1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTraceCPU1ActionPerformed
    if (this.menuTraceCPU1.isSelected()) {
      activateTracerForCPUModule(1);
    } else {
      deactivateTracerForCPUModule(1);
    }
  }//GEN-LAST:event_menuTraceCPU1ActionPerformed

  private void menuTraceCPU2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTraceCPU2ActionPerformed
    if (this.menuTraceCPU2.isSelected()) {
      activateTracerForCPUModule(2);
    } else {
      deactivateTracerForCPUModule(2);
    }
  }//GEN-LAST:event_menuTraceCPU2ActionPerformed

  private void menuTraceCPU3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTraceCPU3ActionPerformed
    if (this.menuTraceCPU3.isSelected()) {
      activateTracerForCPUModule(3);
    } else {
      deactivateTracerForCPUModule(3);
    }
  }//GEN-LAST:event_menuTraceCPU3ActionPerformed

  private void menuServiceSaveScreenAllVRAMActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuServiceSaveScreenAllVRAMActionPerformed
    this.stepSemaphor.lock();
    try {
      final RenderedImage[] images = this.board.getVideoController().renderAllModuleVideoMemoryInZX48Mode();

      final BufferedImage result = new BufferedImage(images[0].getWidth() * images.length, images[0].getHeight(), BufferedImage.TYPE_INT_RGB);
      final Graphics g = result.getGraphics();
      for (int i = 0; i < images.length; i++) {
        g.drawImage((Image) images[i], i * images[0].getWidth(), 0, null);
      }
      g.dispose();

      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      ImageIO.write(result, "png", buffer);
      File pngFile = chooseFileForSave("Save screenshot", lastScreenshotFolder, null, true, new PNGFileFilter());
      if (pngFile != null) {
        final String fileName = pngFile.getName();
        if (!fileName.contains(".")) {
          pngFile = new File(pngFile.getParentFile(), fileName + ".png");
        }
        this.lastScreenshotFolder = pngFile.getParentFile();
        FileUtils.writeByteArrayToFile(pngFile, buffer.toByteArray());
      }
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Can't save screenshot for error, see the log!", "Error", JOptionPane.ERROR_MESSAGE);
      log.log(Level.SEVERE, "Can't make screenshot", ex);
    } finally {
      this.keyboardAndTapeModule.doReset();
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuServiceSaveScreenAllVRAMActionPerformed

  private void menuActionAnimatedGIFActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuActionAnimatedGIFActionPerformed
    this.stepSemaphor.lock();
    try {
      AnimationEncoder encoder = this.currentAnimationEncoder.get();
      if (encoder == null) {
        final AnimatedGifTunePanel panel = new AnimatedGifTunePanel(this.lastAnimGifOptions);
        if (JOptionPane.showConfirmDialog(this, panel, "Options for Animated GIF", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
          return;
        }
        this.lastAnimGifOptions = panel.getValue();
        try {
          encoder = new ZXPolyAGifEncoder(new File(this.lastAnimGifOptions.filePath), this.lastAnimGifOptions.frameRate, this.lastAnimGifOptions.repeat);
        } catch (IOException ex) {
          log.log(Level.SEVERE, "Can't create GIF encoder", ex);
          return;
        }

        if (this.currentAnimationEncoder.compareAndSet(null, encoder)) {
          this.menuActionAnimatedGIF.setIcon(ICO_AGIF_STOP);
          this.menuActionAnimatedGIF.setText(TEXT_STOP_ANIM_GIF);
          log.info("Animated GIF recording has been started");
        }
      } else {
        closeAnimationSave();
        if (this.currentAnimationEncoder.compareAndSet(encoder, null)) {
          this.menuActionAnimatedGIF.setIcon(ICO_AGIF_RECORD);
          this.menuActionAnimatedGIF.setText(TEXT_START_ANIM_GIF);
          log.info("Animated GIF recording has been stopped");
        }
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuActionAnimatedGIFActionPerformed

  private void closeAnimationSave() {
    AnimationEncoder encoder = this.currentAnimationEncoder.get();
    if (encoder != null) {
      try {
        encoder.close();
      } catch (IOException ex) {
        log.warning("Error during animation file close");
      }
    }
  }

  private void formWindowClosed(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosed
    closeAnimationSave();
  }//GEN-LAST:event_formWindowClosed

  private void menuTriggerModuleCPUDesyncActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTriggerModuleCPUDesyncActionPerformed
    this.stepSemaphor.lock();
    try {
      if (this.menuTriggerModuleCPUDesync.isSelected()) {
        this.board.setTrigger(Motherboard.TRIGGER_DIFF_MODULESTATES);
      } else {
        this.board.resetTrigger(Motherboard.TRIGGER_DIFF_MODULESTATES);
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuTriggerModuleCPUDesyncActionPerformed

  private void menuTriggerDiffMemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTriggerDiffMemActionPerformed
    this.stepSemaphor.lock();
    try {
      if (this.menuTriggerDiffMem.isSelected()) {
        final AddressPanel panel = new AddressPanel(this.board.getMemTriggerAddress());
        if (JOptionPane.showConfirmDialog(theInstance.get(), panel, "Triggering address", JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
          try {
            final int addr = panel.extractAddressFromText();
            if (addr < 0 || addr > 0xFFFF) {
              JOptionPane.showMessageDialog(theInstance.get(), "Error address must be in #0000...#FFFF", "Error address", JOptionPane.ERROR_MESSAGE);
            } else {
              this.board.setMemTriggerAddress(addr);
              this.board.setTrigger(Motherboard.TRIGGER_DIFF_MEM_ADDR);
            }
          } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(theInstance.get(), "Error address format, use # for hexadecimal address (example #AA00)", "Error address", JOptionPane.ERROR_MESSAGE);
          }
        }
      } else {
        this.board.resetTrigger(Motherboard.TRIGGER_DIFF_MEM_ADDR);
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuTriggerDiffMemActionPerformed

  private void menuTriggerExeCodeDiffActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuTriggerExeCodeDiffActionPerformed
    this.stepSemaphor.lock();
    try {
      if (this.menuTriggerExeCodeDiff.isSelected()) {
        this.board.setTrigger(Motherboard.TRIGGER_DIFF_EXE_CODE);
      } else {
        this.board.resetTrigger(Motherboard.TRIGGER_DIFF_EXE_CODE);
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuTriggerExeCodeDiffActionPerformed

  private void menuServicemakeSnapshotActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuServicemakeSnapshotActionPerformed
    stepSemaphor.lock();
    try {
      final AtomicReference<FileFilter> theFilter = new AtomicReference<>();
      File selected = chooseFileForSave("Save snapshot", this.lastSnapshotFolder, theFilter, false, new FormatZXP(), new FormatZ80(), new FormatSNA());

      if (selected != null) {
        this.lastSnapshotFolder = selected.getParentFile();
        try {
          final Snapshot selectedFilter = (Snapshot) theFilter.get();
          if (!selectedFilter.getExtension().equals(FilenameUtils.getExtension(selected.getName()).toLowerCase(Locale.ENGLISH))) {
            selected = new File(selected.getParentFile(), selected.getName() + '.' + selectedFilter.getExtension());
          }

          if (selected.isFile() && JOptionPane.showConfirmDialog(this, "Do you want override file '" + selected.getName() + "\'?", "File exists", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
            return;
          }

          log.info("Saving snapshot " + selectedFilter.getName() + " as file " + selected.getName());
          final byte[] preparedSnapshotData = selectedFilter.saveToArray(this.board, this.board.getVideoController());
          log.info("Prepared snapshot data, size " + preparedSnapshotData.length + " bytes");
          FileUtils.writeByteArrayToFile(selected, preparedSnapshotData);
        } catch (Exception ex) {
          ex.printStackTrace();
          log.log(Level.WARNING, "Can't save snapshot file [" + ex.getMessage() + ']', ex);
          JOptionPane.showMessageDialog(this, "Can't save snapshot file [" + ex.getMessage() + ']', "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    } finally {
      stepSemaphor.unlock();
    }
  }//GEN-LAST:event_menuServicemakeSnapshotActionPerformed

  private void menuFileMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_menuFileMenuSelected
    boolean hasChangedDisk = false;
    for (int i = 0; i < 4; i++) {
      final TRDOSDisk disk = this.board.getBetaDiskInterface().getDiskInDrive(i);
      hasChangedDisk |= (disk != null && disk.isChanged());
    }
    this.menuFileFlushDiskChanges.setEnabled(hasChangedDisk);
  }//GEN-LAST:event_menuFileMenuSelected

  private void menuFileFlushDiskChangesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileFlushDiskChangesActionPerformed
    for (int i = 0; i < 4; i++) {
      final TRDOSDisk disk = this.board.getBetaDiskInterface().getDiskInDrive(i);
      if (disk != null && disk.isChanged()) {
        final int result = JOptionPane.showConfirmDialog(this, "Do you want flush disk data '" + disk.getSrcFile().getName() + "' ?", "Disk changed", JOptionPane.YES_NO_CANCEL_OPTION);
        if (result == JOptionPane.CANCEL_OPTION) {
          break;
        }
        if (result == JOptionPane.YES_OPTION) {
          final File destFile;
          if (disk.getType() != TRDOSDisk.SourceDataType.TRD) {
            final JFileChooser fileChooser = new JFileChooser(disk.getSrcFile().getParentFile());
            fileChooser.setFileFilter(new TRDFileFilter());
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setDialogTitle("Save disk as TRD file");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setSelectedFile(new File(disk.getSrcFile().getParentFile(), FilenameUtils.getBaseName(disk.getSrcFile().getName()) + ".trd"));
            if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
              destFile = fileChooser.getSelectedFile();
            } else {
              destFile = null;
            }
          } else {
            destFile = disk.getSrcFile();
          }
          if (destFile != null) {
            try {
              FileUtils.writeByteArrayToFile(destFile, disk.getDiskData());
              disk.replaceSrcFile(destFile, TRDOSDisk.SourceDataType.TRD, true);
              log.info("Changes for disk " + ('A' + i) + " is saved as file: " + destFile.getAbsolutePath());
            } catch (IOException ex) {
              log.warning("Can't write disk for error: " + ex.getMessage());
              JOptionPane.showMessageDialog(this, "Can't save disk for IO error: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        }
      }
    }
  }//GEN-LAST:event_menuFileFlushDiskChangesActionPerformed

  private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
    boolean hasChangedDisk = false;
    for (int i = 0; i < 4; i++) {
      final TRDOSDisk disk = this.board.getBetaDiskInterface().getDiskInDrive(i);
      hasChangedDisk |= (disk != null && disk.isChanged());
    }

    boolean close = false;

    if (hasChangedDisk) {
      if (JOptionPane.showConfirmDialog(this, "Emulator has unsaved disks, do you realy want to close it?", "Detected unsaved data", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        close = true;
      }
    } else {
      close = true;
    }

    if (close)
      System.exit(0);
  }//GEN-LAST:event_formWindowClosing

  private void activateTracerForCPUModule(final int index) {
    TraceCPUForm form = this.cpuTracers[index];
    if (form == null) {
      form = new TraceCPUForm(this, this.board, index);
      form.setVisible(true);
    }
    form.toFront();
    form.requestFocus();
  }

  private void deactivateTracerForCPUModule(final int index) {
    TraceCPUForm form = this.cpuTracers[index];
    if (form != null) {
      form.dispose();
    }
  }

  private void updateTracerCheckBoxes() {
    this.menuTraceCPU0.setSelected(this.cpuTracers[0] != null);
    this.menuTraceCPU1.setSelected(this.cpuTracers[1] != null);
    this.menuTraceCPU2.setSelected(this.cpuTracers[2] != null);
    this.menuTraceCPU3.setSelected(this.cpuTracers[3] != null);
  }

  public void onTracerActivated(final TraceCPUForm tracer) {
    final int index = tracer.getModuleIndex();
    this.cpuTracers[index] = tracer;
    updateTracerCheckBoxes();
    this.activeTracerWindowCounter.incrementAndGet();
  }

  public void onTracerDeactivated(final TraceCPUForm tracer) {
    final int index = tracer.getModuleIndex();
    this.cpuTracers[index] = null;
    updateTracerCheckBoxes();
    this.activeTracerWindowCounter.decrementAndGet();
  }

  private File chooseFileForOpen(final String title, final File initial, final AtomicReference<FileFilter> selectedFilter, final FileFilter... filter) {
    final JFileChooser chooser = new JFileChooser(initial);
    for (final FileFilter f : filter) {
      chooser.addChoosableFileFilter(f);
    }
    chooser.setAcceptAllFileFilterUsed(false);
    chooser.setMultiSelectionEnabled(false);
    chooser.setDialogTitle(title);
    chooser.setFileFilter(filter[0]);
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    final File result;
    if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
      result = chooser.getSelectedFile();
      if (selectedFilter != null) {
        selectedFilter.set(chooser.getFileFilter());
      }
    } else {
      result = null;
    }
    return result;
  }

  private File chooseFileForSave(final String title, final File initial, final AtomicReference<FileFilter> selectedFilter, final boolean allowAcceptAll, final FileFilter... filters) {
    final JFileChooser chooser = new JFileChooser(initial);
    for (final FileFilter f : filters) {
      chooser.addChoosableFileFilter(f);
    }
    chooser.setAcceptAllFileFilterUsed(allowAcceptAll);
    chooser.setMultiSelectionEnabled(false);
    chooser.setDialogTitle(title);
    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);

    final File result;
    if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      result = chooser.getSelectedFile();
      if (selectedFilter != null) {
        selectedFilter.set(chooser.getFileFilter());
      }
    } else {
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
  private javax.swing.JMenuItem menuActionAnimatedGIF;
  private javax.swing.JMenuBar menuBar;
  private javax.swing.JMenu menuCatcher;
  private javax.swing.JMenu menuFile;
  private javax.swing.JMenuItem menuFileExit;
  private javax.swing.JMenuItem menuFileFlushDiskChanges;
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
  private javax.swing.JMenuItem menuServiceSaveScreenAllVRAM;
  private javax.swing.JMenuItem menuServicemakeSnapshot;
  private javax.swing.JMenu menuTap;
  private javax.swing.JMenu menuTapExportAs;
  private javax.swing.JMenuItem menuTapExportAsWav;
  private javax.swing.JMenuItem menuTapGotoBlock;
  private javax.swing.JMenuItem menuTapNextBlock;
  private javax.swing.JCheckBoxMenuItem menuTapPlay;
  private javax.swing.JMenuItem menuTapPrevBlock;
  private javax.swing.JMenuItem menuTapeRewindToStart;
  private javax.swing.JCheckBoxMenuItem menuTraceCPU0;
  private javax.swing.JCheckBoxMenuItem menuTraceCPU1;
  private javax.swing.JCheckBoxMenuItem menuTraceCPU2;
  private javax.swing.JCheckBoxMenuItem menuTraceCPU3;
  private javax.swing.JMenu menuTracer;
  private javax.swing.JCheckBoxMenuItem menuTriggerDiffMem;
  private javax.swing.JCheckBoxMenuItem menuTriggerExeCodeDiff;
  private javax.swing.JCheckBoxMenuItem menuTriggerModuleCPUDesync;
  private javax.swing.JPanel panelIndicators;
  private javax.swing.JScrollPane scrollPanel;
  // End of variables declaration//GEN-END:variables
}
