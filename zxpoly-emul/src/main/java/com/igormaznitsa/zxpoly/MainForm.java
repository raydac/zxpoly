/*
 * Copyright (C) 2014-2020 Igor Maznitsa
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
import static com.igormaznitsa.zxpoly.components.VideoController.MCYCLES_PER_INT;
import static com.igormaznitsa.zxpoly.utils.Utils.assertUiThread;
import static javax.swing.KeyStroke.getKeyStroke;


import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.animeencoders.AnimatedGifTunePanel;
import com.igormaznitsa.zxpoly.animeencoders.AnimationEncoder;
import com.igormaznitsa.zxpoly.animeencoders.Spec256AGifEncoder;
import com.igormaznitsa.zxpoly.animeencoders.ZxPolyAGifEncoder;
import com.igormaznitsa.zxpoly.components.Beeper;
import com.igormaznitsa.zxpoly.components.BoardMode;
import com.igormaznitsa.zxpoly.components.KempstonMouse;
import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.RomData;
import com.igormaznitsa.zxpoly.components.SourceSoundPort;
import com.igormaznitsa.zxpoly.components.TapeFileReader;
import com.igormaznitsa.zxpoly.components.VideoController;
import com.igormaznitsa.zxpoly.components.betadisk.BetaDiscInterface;
import com.igormaznitsa.zxpoly.components.betadisk.TrDosDisk;
import com.igormaznitsa.zxpoly.components.tvfilters.TvFilterChain;
import com.igormaznitsa.zxpoly.formats.FormatSNA;
import com.igormaznitsa.zxpoly.formats.FormatSpec256;
import com.igormaznitsa.zxpoly.formats.FormatZ80;
import com.igormaznitsa.zxpoly.formats.FormatZXP;
import com.igormaznitsa.zxpoly.formats.Snapshot;
import com.igormaznitsa.zxpoly.streamer.ZxVideoStreamer;
import com.igormaznitsa.zxpoly.tracer.TraceCpuForm;
import com.igormaznitsa.zxpoly.trainers.AbstractTrainer;
import com.igormaznitsa.zxpoly.trainers.TrainerPok;
import com.igormaznitsa.zxpoly.ui.AboutDialog;
import com.igormaznitsa.zxpoly.ui.AddressPanel;
import com.igormaznitsa.zxpoly.ui.CpuLoadIndicator;
import com.igormaznitsa.zxpoly.ui.GameControllerPanel;
import com.igormaznitsa.zxpoly.ui.JIndicatorLabel;
import com.igormaznitsa.zxpoly.ui.OptionsPanel;
import com.igormaznitsa.zxpoly.ui.SelectTapPosDialog;
import com.igormaznitsa.zxpoly.utils.AppOptions;
import com.igormaznitsa.zxpoly.utils.JHtmlLabel;
import com.igormaznitsa.zxpoly.utils.RomLoader;
import com.igormaznitsa.zxpoly.utils.Utils;
import com.igormaznitsa.zxpoly.utils.Wallclock;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.Image;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.KeyEvent;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.swing.Box;
import javax.swing.Box.Filler;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JSeparator;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public final class MainForm extends javax.swing.JFrame implements Runnable, ActionListener {

  public static final long TIMER_INT_DELAY_MILLISECONDS = 20L;
  public static final Logger LOGGER = Logger.getLogger("UI");
  private static final Icon ICO_MOUSE = new ImageIcon(Utils.loadIcon("mouse.png"));
  private static final Icon ICO_MOUSE_DIS =
      UIManager.getLookAndFeel().getDisabledIcon(null, ICO_MOUSE);
  private static final Icon ICO_DISK = new ImageIcon(Utils.loadIcon("disk.png"));
  private static final Icon ICO_DISK_DIS =
      UIManager.getLookAndFeel().getDisabledIcon(null, ICO_DISK);
  private static final Icon ICO_AGIF_RECORD = new ImageIcon(Utils.loadIcon("record.png"));
  private static final Icon ICO_AGIF_STOP = new ImageIcon(Utils.loadIcon("tape_stop.png"));
  private static final Icon ICO_TAPE = new ImageIcon(Utils.loadIcon("cassette.png"));
  private static final Icon ICO_MDISK = new ImageIcon(Utils.loadIcon("mdisk.png"));
  private static final Icon ICO_TAPE_DIS =
      UIManager.getLookAndFeel().getDisabledIcon(null, ICO_TAPE);
  private static final Icon ICO_TURBO = new ImageIcon(Utils.loadIcon("turbo.png"));
  private static final Icon ICO_TURBO_DIS =
      UIManager.getLookAndFeel().getDisabledIcon(null, ICO_TURBO);
  private static final Icon ICO_ZX128 = new ImageIcon(Utils.loadIcon("zx128.png"));
  private static final Icon ICO_ZX128_DIS =
      UIManager.getLookAndFeel().getDisabledIcon(null, ICO_ZX128);
  private static final Icon ICO_EMUL_PLAY = new ImageIcon(Utils.loadIcon("emul_play.png"));
  private static final Icon ICO_EMUL_PAUSE = new ImageIcon(Utils.loadIcon("emul_pause.png"));
  private static final String TEXT_START_ANIM_GIF = "Record AGIF";
  private static final String TEXT_STOP_ANIM_GIF = "Stop AGIF";
  private static final long serialVersionUID = 7309959798344327441L;
  public static RomData BASE_ROM;
  private final int expectedIntTicksBetweenFrames;
  private final CpuLoadIndicator indicatorCpu0 =
      new CpuLoadIndicator(48, 14, 4, "CPU0", Color.GREEN, Color.DARK_GRAY, Color.WHITE);
  private final CpuLoadIndicator indicatorCpu1 =
      new CpuLoadIndicator(48, 14, 4, "CPU1", Color.GREEN, Color.DARK_GRAY, Color.WHITE);
  private final CpuLoadIndicator indicatorCpu2 =
      new CpuLoadIndicator(48, 14, 4, "CPU2", Color.GREEN, Color.DARK_GRAY, Color.WHITE);
  private final CpuLoadIndicator indicatorCpu3 =
      new CpuLoadIndicator(48, 14, 4, "CPU3", Color.GREEN, Color.DARK_GRAY, Color.WHITE);
  private final TraceCpuForm[] cpuTracers = new TraceCpuForm[4];
  private final AtomicInteger activeTracerWindowCounter = new AtomicInteger();
  private final AtomicReference<AnimationEncoder> currentAnimationEncoder = new AtomicReference<>();
  private final Motherboard board;
  private final ZxVideoStreamer videoStreamer;
  private final Wallclock wallclock = new Wallclock();
  private final Runnable traceWindowsUpdater = new Runnable() {

    @Override
    public void run() {
      int index = 0;
      for (final TraceCpuForm form : cpuTracers) {
        if (form != null) {
          final Z80 cpu = board.getModules()[index++].getCpu();
          if (cpu.getPrefixInProcessing() == 0 && !cpu.isInsideBlockLoop()) {
            form.refreshViewState();
          }
        }
      }
    }
  };
  private final KeyboardKempstonAndTapeIn keyboardAndTapeModule;
  private final KempstonMouse kempstonMouse;
  private final ReentrantLock stepSemaphor = new ReentrantLock();
  private volatile boolean turboMode = false;
  private volatile boolean zxKeyboardProcessingAllowed = true;
  private AnimatedGifTunePanel.AnimGifOptions lastAnimGifOptions =
      new AnimatedGifTunePanel.AnimGifOptions("./zxpoly.gif", 10, false);
  private File lastTapFolder;
  private File lastFloppyFolder;
  private File lastSnapshotFolder;
  private File lastScreenshotFolder;
  // Variables declaration - do not modify//GEN-BEGIN:variables
  private Filler filler1;
  private Separator jSeparator1;
  private JSeparator jSeparator2;
  private Separator jSeparator3;
  private JIndicatorLabel labelDiskUsage;
  private JIndicatorLabel labelMouseUsage;
  private JIndicatorLabel labelTapeUsage;
  private JIndicatorLabel labelTurbo;
  private JIndicatorLabel labelZX128;
  private JMenuItem menuActionAnimatedGIF;
  private JMenuBar menuBar;
  private JMenu menuCatcher;
  private JMenu menuFile;
  private JMenu menuView;
  private JMenu menuViewZoom;
  private JMenu menuViewVideoFilter;
  private JMenuItem menuViewZoomIn;
  private JMenuItem menuViewZoomOut;
  private JMenuItem menuFileExit;
  private JMenuItem menuFileFlushDiskChanges;
  private JMenuItem menuFileLoadSnapshot;
  private JMenuItem menuFileLoadPoke;
  private JMenuItem menuFileLoadTap;
  private JMenuItem menuFileOptions;
  private JMenuItem menuFileReset;
  private JMenuItem menuFileSelectDiskA;
  private JMenuItem menuFileSelectDiskB;
  private JMenuItem menuFileSelectDiskC;
  private JMenuItem menuFileSelectDiskD;
  private JMenu menuHelp;
  private JMenuItem menuHelpAbout;
  private JMenuItem menuHelpDonation;
  private JMenu menuLoadDrive;
  private JMenu menuOptions;
  private JCheckBoxMenuItem menuOptionsEnableTrapMouse;
  private JCheckBoxMenuItem menuOptionsEnableSpeaker;
  private JCheckBoxMenuItem menuOptionsEnableVideoStream;
  private JCheckBoxMenuItem menuOptionsShowIndicators;
  private JCheckBoxMenuItem menuOptionsTurbo;
  private JCheckBoxMenuItem menuOptionsZX128Mode;
  private JMenu menuService;
  private JMenuItem menuServiceGameControllers;
  private JMenuItem menuServiceSaveScreen;
  private JMenuItem menuServiceSaveScreenAllVRAM;
  private JMenuItem menuServiceMakeSnapshot;
  private JMenu menuTap;
  private JMenu menuTapExportAs;
  private JMenuItem menuTapExportAsWav;
  private JMenuItem menuTapGotoBlock;
  private JMenuItem menuTapNextBlock;
  private JCheckBoxMenuItem menuTapPlay;
  private JMenuItem menuTapPrevBlock;
  private JMenuItem menuTapeRewindToStart;
  private JCheckBoxMenuItem menuTraceCpu0;
  private JCheckBoxMenuItem menuTraceCpu1;
  private JCheckBoxMenuItem menuTraceCpu2;
  private JCheckBoxMenuItem menuTraceCpu3;
  private JMenu menuTracer;
  private JCheckBoxMenuItem menuTriggerDiffMem;
  private JCheckBoxMenuItem menuTriggerExeCodeDiff;
  private JCheckBoxMenuItem menuTriggerModuleCPUDesync;
  private javax.swing.JPanel panelIndicators;
  private javax.swing.JScrollPane scrollPanel;
  private File lastPokeFileFolder = null;

  public MainForm(final String title, final String romPath) {
    Runtime.getRuntime().addShutdownHook(new Thread(this::doOnShutdown));

    final String ticks = System.getProperty("zxpoly.int.ticks", "");
    int intBetweenFrames = AppOptions.getInstance().getIntBetweenFrames();
    try {
      intBetweenFrames = ticks.isEmpty() ? intBetweenFrames : Integer.parseInt(ticks);
    } catch (NumberFormatException ex) {
      LOGGER.warning("Can't parse ticks: " + ticks);
    }
    expectedIntTicksBetweenFrames = intBetweenFrames;

    LOGGER.log(Level.INFO, "INT ticks between frames: " + expectedIntTicksBetweenFrames);
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
        LOGGER.info("Emulator is paused by PLAY/PAUSE button");
      } else {
        MainForm.this.stepSemaphor.unlock();
        source.setIcon(ICO_EMUL_PAUSE);
        LOGGER.info("Emulator is started by PLAY/PAUSE button");
      }
    });

    this.menuBar.add(buttonStartPause);

    this.setTitle(title);

    this.menuActionAnimatedGIF.setText(TEXT_START_ANIM_GIF);
    this.menuActionAnimatedGIF.setIcon(ICO_AGIF_RECORD);

    this.getInputContext().selectInputMethod(Locale.ENGLISH);

    setIconImage(Utils.loadIcon("appico.png"));

    try {
      BASE_ROM = loadRom(romPath);
    } catch (Exception ex) {
      JOptionPane.showMessageDialog(this, "Can't load Spec128 ROM for error: " + ex.getMessage());
      try {
        BASE_ROM = loadRom(null);
      } catch (Exception exx) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Can't load TEST ROM: " + ex.getMessage());
        System.exit(-1);
      }
    }

    this.board = new Motherboard(BASE_ROM, AppOptions.getInstance().isCovoxFb());
    this.board.setBoardMode(BoardMode.ZXPOLY, true);
    this.menuOptionsZX128Mode.setSelected(this.board.getBoardMode() != BoardMode.ZXPOLY);
    this.menuOptionsTurbo.setSelected(this.turboMode);

    LOGGER.info("Main form completed");
    this.board.reset();

    this.scrollPanel.getViewport().add(this.board.getVideoController());
    this.keyboardAndTapeModule = this.board.findIoDevice(KeyboardKempstonAndTapeIn.class);
    this.kempstonMouse = this.board.findIoDevice(KempstonMouse.class);

    final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(
        new KeyboardDispatcher(this.board.getVideoController(), this.keyboardAndTapeModule));

    final GridBagConstraints cpuIndicatorConstraint = new GridBagConstraints();
    cpuIndicatorConstraint.ipadx = 5;

    this.panelIndicators.add(this.indicatorCpu0, cpuIndicatorConstraint, 0);
    this.panelIndicators.add(this.indicatorCpu1, cpuIndicatorConstraint, 1);
    this.panelIndicators.add(this.indicatorCpu2, cpuIndicatorConstraint, 2);
    this.panelIndicators.add(this.indicatorCpu3, cpuIndicatorConstraint, 3);

    this.menuOptionsEnableTrapMouse
        .setSelected(this.board.getVideoController().isTrapMouseEnabled());

    for (final Component item : this.menuBar.getComponents()) {
      if (item instanceof JMenu) {
        final JMenu menuItem = (JMenu) item;
        menuItem.addMenuListener(new MenuListener() {
          @Override
          public void menuSelected(MenuEvent e) {
            MainForm.this.stepSemaphor.lock();
            MainForm.this.keyboardAndTapeModule.doReset();
            if (e.getSource() == menuOptions) {
              menuOptionsEnableSpeaker
                  .setEnabled(!turboMode && !menuOptionsEnableVideoStream.isSelected());
              menuOptionsEnableSpeaker.setState(board.getBeeper().isActive());
            }
            menuServiceGameControllers
                .setEnabled(keyboardAndTapeModule.isControllerEngineAllowed());
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

    this.videoStreamer = new ZxVideoStreamer(
        this.board.getVideoController(),
        streamer -> {
          streamer.stop();
          SwingUtilities
              .invokeLater(() -> this.menuOptionsEnableVideoStream.setSelected(false));
        }
    );

    updateTapeMenu();

    pack();

    this.setLocationRelativeTo(null);

    SwingUtilities.invokeLater(() -> {
      final Thread mainCpuThread = new Thread(this, "zx-poly-main-cpu-thread");
      mainCpuThread.setPriority(Thread.MAX_PRIORITY);
      mainCpuThread.setDaemon(true);
      mainCpuThread.setUncaughtExceptionHandler((t, e) -> {
        LOGGER.severe("Detected exception in main thread, stopping application, see logs");
        e.printStackTrace(System.err);
        System.exit(666);
      });
      mainCpuThread.start();


      final Timer infobarUpdateTimer = new Timer(1000, action -> updateInfobar());
      infobarUpdateTimer.setRepeats(true);
      infobarUpdateTimer.setInitialDelay(1000);
      infobarUpdateTimer.start();
    });
  }

  private void updateInfobar() {
    assertUiThread();
    if (panelIndicators.isVisible()) {
      labelTurbo.setStatus(turboMode);

      final TapeFileReader tapeFileReader = keyboardAndTapeModule.getTap();
      labelTapeUsage.setStatus(tapeFileReader != null && tapeFileReader.isPlaying());
      labelMouseUsage.setStatus(board.getVideoController().isHoldMouse());
      labelDiskUsage.setStatus(board.getBetaDiskInterface().isActive());
      labelZX128.setStatus(board.getBoardMode() != BoardMode.ZXPOLY);

      indicatorCpu0.updateForState(board.getCpuActivity(0));
      indicatorCpu1.updateForState(board.getCpuActivity(1));
      indicatorCpu2.updateForState(board.getCpuActivity(2));
      indicatorCpu3.updateForState(board.getCpuActivity(3));
    }
    updateTracerCheckBoxes();
  }

  private void doOnShutdown() {
    this.videoStreamer.stop();
  }

  private Optional<SourceSoundPort> showSelectSoundLineDialog(
      final List<SourceSoundPort> variants) {
    assertUiThread();
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
    final String previouslySelectedDevice = AppOptions.getInstance().getLastSelectedAudioDevice();

    final JComboBox<SourceSoundPort> comboBox =
        new JComboBox<>(variants.toArray(new SourceSoundPort[0]));
    comboBox.setPrototypeDisplayValue(
        new SourceSoundPort(null, "some very long device name to be shown", null));
    comboBox.addActionListener(x -> {
      comboBox.setToolTipText(comboBox.getSelectedItem().toString());
    });
    comboBox.setToolTipText(comboBox.getSelectedItem().toString());

    int index = -1;
    for (int i = 0; i < comboBox.getItemCount(); i++) {
      if (comboBox.getItemAt(i).toString().equals(previouslySelectedDevice)) {
        index = i;
        break;
      }
    }

    comboBox.setSelectedIndex(Math.max(0, index));

    panel.add(new JLabel("Sound device:"));
    panel.add(comboBox);
    if (JOptionPane.showConfirmDialog(
        this,
        panel,
        "Select sound device",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.PLAIN_MESSAGE
    ) == JOptionPane.OK_OPTION) {
      final SourceSoundPort selected = (SourceSoundPort) comboBox.getSelectedItem();
      AppOptions.getInstance().setLastSelectedAudioDevice(selected.toString());
      return Optional.ofNullable(selected);
    } else {
      return Optional.empty();
    }

  }

  private Optional<SourceSoundPort> findAudioLine(final AudioFormat audioFormat) {
    final List<SourceSoundPort> foundPorts = SourceSoundPort.findForFormat(audioFormat);
    LOGGER.info("Detected audio source lines: " + foundPorts);
    if (foundPorts.isEmpty()) {
      JOptionPane
          .showMessageDialog(this, "There is no detected audio devices!", "Can't find audio device",
              JOptionPane.WARNING_MESSAGE);
      return Optional.empty();
    } else {
      if (foundPorts.size() == 1) {
        return Optional.of(foundPorts.get(0));
      } else {
        return this.showSelectSoundLineDialog(foundPorts);
      }
    }
  }

  private RomData loadRom(final String romPath) throws Exception {
    if (romPath != null) {
      if (romPath.contains("://")) {
        try {
          final String cached =
              "loaded_" + Integer.toHexString(romPath.hashCode()).toUpperCase(Locale.ENGLISH)
                  + ".rom";
          final File cacheFolder = new File(AppOptions.getInstance().getAppConfigFolder(), "cache");
          final File cachedRom = new File(cacheFolder, cached);
          RomData result = null;
          boolean load = true;
          if (cachedRom.isFile()) {
            LOGGER.log(Level.INFO,
                "Load cached ROM downloaded from '" + romPath + "' : " + cachedRom);
            result = new RomData(cachedRom.getName(), FileUtils.readFileToByteArray(cachedRom));
            load = false;
          }

          if (load) {
            LOGGER.log(Level.INFO, "Load ROM from external URL: " + romPath);
            result = RomLoader.getROMFrom(romPath);
            if (cacheFolder.isDirectory() || cacheFolder.mkdirs()) {
              FileUtils.writeByteArrayToFile(cachedRom, result.getAsArray());
              LOGGER.log(Level.INFO, "Loaded ROM saved in cache as file : " + romPath);
            }
          }
          return result;
        } catch (Exception ex) {
          LOGGER
              .log(Level.WARNING, "Can't load ROM from '" + romPath + "': " + ex.getMessage(), ex);
          throw ex;
        }
      } else {
        LOGGER.log(Level.INFO, "Load ROM from embedded resource '" + romPath + "'");
        try (final InputStream in = Utils
            .findResourceOrError("com/igormaznitsa/zxpoly/rom/" + romPath)) {
          return RomData.read(romPath, in);
        } catch (IllegalArgumentException ex) {
          final File file = new File(romPath);
          if (file.isFile()) {
            try (final InputStream in = new FileInputStream(file)) {
              return RomData.read(file.getName(), in);
            }
          } else {
            throw new IllegalArgumentException("Can't find ROM: " + romPath);
          }
        }
      }
    }

    final String testRom = AppOptions.TEST_ROM;
    LOGGER.info("Load ROM from embedded resource '" + testRom + "'");
    try (final InputStream in = Utils
        .findResourceOrError("com/igormaznitsa/zxpoly/rom/" + testRom)) {
      return RomData.read(testRom, in);
    }
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
    final int intTicksBetweenInfoPanelUpdate = 20;

    long nextWallclockIntTime =
        this.wallclock.getTimeInMilliseconds() + TIMER_INT_DELAY_MILLISECONDS;
    int countdownToPaint = 0;
    int countdownToAnimationSave = 0;

    while (!Thread.currentThread().isInterrupted()) {
      final long currentMachineCycleCounter = this.board.getMasterCpu().getMachineCycles();
      long wallclockTime = this.wallclock.getTimeInMilliseconds();

      if (stepSemaphor.tryLock()) {
        try {
          final boolean inTurboMode = this.turboMode;
          final boolean allMcyclesInIntCompleted = currentMachineCycleCounter >= MCYCLES_PER_INT;
          final boolean wallclockInt = nextWallclockIntTime <= wallclockTime;

          final boolean virtualIntTick;
          if (wallclockInt) {
            nextWallclockIntTime = wallclockTime + TIMER_INT_DELAY_MILLISECONDS;
            if (allMcyclesInIntCompleted) {
              this.board
                  .getMasterCpu()
                  .setMCycleCounter(currentMachineCycleCounter % MCYCLES_PER_INT);
            }
            countdownToPaint--;
            countdownToAnimationSave--;

            virtualIntTick = true;

            if (!allMcyclesInIntCompleted) {
              this.onSlownessDetected(MCYCLES_PER_INT - currentMachineCycleCounter);
            }
          } else {
            virtualIntTick = false;
          }

          final int detectedTriggers = this.board.step(
              virtualIntTick,
              wallclockInt,
              inTurboMode || !allMcyclesInIntCompleted || virtualIntTick);

          if (wallclockInt) {
            this.videoStreamer.onWallclockInt();
          }

          if (detectedTriggers != Motherboard.TRIGGER_NONE) {
            final Z80[] cpuStates = new Z80[4];
            final int lastM1Address = this.board.getModules()[0].getLastM1Address();
            for (int i = 0; i < 4; i++) {
              cpuStates[i] = new Z80(this.board.getModules()[i].getCpu());
            }
            SwingUtilities.invokeLater(() -> onTrigger(detectedTriggers, lastM1Address, cpuStates));
          }

          if (countdownToPaint <= 0) {
            countdownToPaint = expectedIntTicksBetweenFrames;
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
                LOGGER.warning("Can't write animation frame: " + ex.getMessage());
              }
            }
          }
        } finally {
          stepSemaphor.unlock();
        }

        if (this.activeTracerWindowCounter.get() > 0) {
          updateTracerWindowsForStep();
        }
      } else {
        final long timeDiff = wallclockTime - nextWallclockIntTime;
        if (timeDiff >= 0L) {
          nextWallclockIntTime = wallclockTime + TIMER_INT_DELAY_MILLISECONDS;
          this.videoStreamer.onWallclockInt();
          this.board.dryIntTickOnWallClockTime(
              timeDiff == 0 ? MCYCLES_PER_INT : Math.round(
                  MCYCLES_PER_INT * ((double) (timeDiff + TIMER_INT_DELAY_MILLISECONDS / 2)
                      / (double) TIMER_INT_DELAY_MILLISECONDS)
              )
          );
        }
      }
    }
  }

  private void onSlownessDetected(final long nonCompletedMcycles) {
    LOGGER.warning(String.format("Slowness detected: %.02f%%",
        (float) nonCompletedMcycles / (float) MCYCLES_PER_INT * 100.0f));
  }

  private void updateTracerWindowsForStep() {
    try {
      SwingUtilities.invokeAndWait(this.traceWindowsUpdater);
    } catch (InterruptedException ex) {
      LOGGER.log(Level.INFO, "Interrupted trace window updater");
    } catch (InvocationTargetException ex) {
      LOGGER.log(Level.SEVERE, "Error in trace window updater", ex);
    }
  }

  private String getCellContentForAddress(final int address) {
    final StringBuilder result = new StringBuilder();

    for (int i = 0; i < 4; i++) {
      if (result.length() > 0) {
        result.append("  ");
      }
      result.append(toHexByte(this.board.getModules()[i].readAddress(address)));
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

    buffer.append("\n\nDisasm since last executed address in CPU0 memory: ")
        .append(toHex(lastAddress)).append('\n');

    buffer.append(this.board.getModules()[0].toHexStringSinceAddress(lastAddress - 8, 8))
        .append("\n\n");

    this.board.getModules()[0].disasmSinceAddress(lastAddress, 5)
        .forEach((l) -> buffer.append(l.toString()).append('\n'));

    buffer.append('\n');

    for (int i = 0; i < cpuModuleStates.length; i++) {
      buffer.append("CPU MODULE: ").append(i).append('\n');
      buffer.append(cpuModuleStates[i].getStateAsString());
      buffer.append("\n\n");
    }
    buffer.append('\n');
    LOGGER.info(buffer.toString());
  }

  private String makeInfoStringForRegister(final Z80[] cpuModuleStates,
                                           final int lastAddress,
                                           final String extraString,
                                           final int register,
                                           final boolean alt) {
    final StringBuilder result = new StringBuilder();

    if (extraString != null) {
      result.append(extraString).append('\n');
    }

    for (int i = 0; i < cpuModuleStates.length; i++) {
      if (i > 0) {
        result.append(", ");
      }
      result.append("CPU#").append(i).append('=')
          .append(toHex(cpuModuleStates[i].getRegister(register, alt)));
    }

    result.append("\n\nLast executed address : ").append(toHex(lastAddress))
        .append("\n--------------\n\n");
    result.append(this.board.getModules()[0].toHexStringSinceAddress(lastAddress - 8, 8))
        .append("\n\n");

    this.board.getModules()[0].disasmSinceAddress(lastAddress, 5)
        .forEach((l) -> result.append(l.toString()).append('\n'));

    return result.toString();
  }

  void onTrigger(final int triggered, final int lastM1Address, final Z80[] cpuModuleStates) {
    this.stepSemaphor.lock();
    try {
      logTrigger(triggered, lastM1Address, cpuModuleStates);

      if ((triggered & Motherboard.TRIGGER_DIFF_MODULESTATES) != 0) {
        this.menuTriggerModuleCPUDesync.setSelected(false);
        JOptionPane.showMessageDialog(MainForm.this, "Detected desync of module CPUs\n"
                + makeInfoStringForRegister(cpuModuleStates, lastM1Address, null, Z80.REG_PC, false),
            "Triggered", JOptionPane.INFORMATION_MESSAGE);
      }

      if ((triggered & Motherboard.TRIGGER_DIFF_MEM_ADDR) != 0) {
        this.menuTriggerDiffMem.setSelected(false);
        JOptionPane.showMessageDialog(MainForm.this,
            "Detected memory cell difference " + toHex(this.board.getMemTriggerAddress()) + "\n"
                + makeInfoStringForRegister(cpuModuleStates, lastM1Address,
                getCellContentForAddress(this.board.getMemTriggerAddress()), Z80.REG_PC, false),
            "Triggered", JOptionPane.INFORMATION_MESSAGE);
      }

      if ((triggered & Motherboard.TRIGGER_DIFF_EXE_CODE) != 0) {
        this.menuTriggerExeCodeDiff.setSelected(false);
        JOptionPane.showMessageDialog(MainForm.this, "Detected EXE code difference\n"
                + makeInfoStringForRegister(cpuModuleStates, lastM1Address, null, Z80.REG_PC, false),
            "Triggered", JOptionPane.INFORMATION_MESSAGE);
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  public boolean isTurboMode() {
    return this.turboMode;
  }

  public void setTurboMode(final boolean value) {
    this.turboMode = value;
  }

  private void updateScreen() {
    final VideoController vc = board.getVideoController();
    vc.updateBuffer();
    vc.repaint();
  }

  private void menuOptionsEnableVideoStreamActionPerformed(final ActionEvent actionEvent) {
    this.stepSemaphor.lock();
    try {
      if (this.menuOptionsEnableVideoStream.isSelected()) {
        if (AppOptions.getInstance().isGrabSound()
            && !this.board.getBeeper().isNullBeeper()
            && JOptionPane.showConfirmDialog(this,
            "Beeper should be turned off for video sound. Ok?",
            "Beeper deactivation", JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE)
            != JOptionPane.OK_OPTION) {
          this.menuOptionsEnableVideoStream.setSelected(false);
          return;
        } else {
          this.board.getBeeper().setSourceSoundPort(null);
        }

        final Beeper beeper = this.board.getBeeper().isNullBeeper()
            && AppOptions.getInstance().isGrabSound() ? this.board.getBeeper() : null;

        try {
          final InetAddress interfaceAddress =
              InetAddress.getByName(AppOptions.getInstance().getAddress());
          this.videoStreamer.start(
              beeper,
              AppOptions.getInstance().getFfmpegPath(),
              interfaceAddress,
              AppOptions.getInstance().getPort(),
              AppOptions.getInstance().getFrameRate()
          );
        } catch (Exception ex) {
          JOptionPane.showMessageDialog(this, ex.getMessage(), "Error",
              JOptionPane.ERROR_MESSAGE);
          this.menuOptionsEnableVideoStream.setSelected(false);
        }
      } else {
        this.videoStreamer.stop();
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  private void menuOptionsEnableSpeakerActionPerformed(final ActionEvent actionEvent) {
    this.stepSemaphor.lock();
    try {
      if (this.menuOptionsEnableSpeaker.isSelected()) {
        final Optional<SourceSoundPort> port =
            this.findAudioLine(this.board.getBeeper().getAudioFormat());
        if (port.isPresent()) {
          this.board.getBeeper().setSourceSoundPort(port.get());
          if (this.board.getBeeper().isNullBeeper()) {
            this.menuOptionsEnableSpeaker.setSelected(false);
          }
        } else {
          this.menuOptionsEnableSpeaker.setSelected(false);
        }
      } else {
        this.board.getBeeper().setSourceSoundPort(null);
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  private void menuServiceGameControllerActionPerformed(final ActionEvent actionEvent) {
    this.stepSemaphor.lock();
    try {
      if (!this.keyboardAndTapeModule.isControllerEngineAllowed()) {
        JOptionPane.showMessageDialog(this, "Can't init game controller engine!", "Error",
            JOptionPane.ERROR_MESSAGE);
      } else if (this.keyboardAndTapeModule.getDetectedControllers().isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Can't find any game controller. Try restart the emulator if controller already connected.",
            "Can't find game controllers", JOptionPane.WARNING_MESSAGE);
      } else {
        final GameControllerPanel gameControllerPanel =
            new GameControllerPanel(this.keyboardAndTapeModule);
        if (JOptionPane.showConfirmDialog(
            this,
            gameControllerPanel,
            "Detected game controllers",
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.PLAIN_MESSAGE
        ) == JOptionPane.OK_OPTION) {
          this.keyboardAndTapeModule.setActiveGadapters(gameControllerPanel.getSelected());
        }
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  private void menuFileResetActionPerformed(ActionEvent evt) {
    this.board.resetAndRestoreRom(BASE_ROM);
  }

  private void menuOptionsShowIndicatorsActionPerformed(ActionEvent evt) {
    this.indicatorCpu0.clear();
    this.indicatorCpu1.clear();
    this.indicatorCpu2.clear();
    this.indicatorCpu3.clear();
    this.panelIndicators.setVisible(this.menuOptionsShowIndicators.isSelected());
  }

  private void loadDiskIntoDrive(final int drive) {
    this.stepSemaphor.lock();
    try {
      this.turnZxKeyboardOff();
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

      final SclFileFilter sclFileFilter = new SclFileFilter();
      final TrdFileFilter trdFileFilter = new TrdFileFilter();

      final FileFilter allSupportedFilters = new FileFilter() {
        @Override
        public boolean accept(File f) {
          return sclFileFilter.accept(f) || trdFileFilter.accept(f);
        }

        @Override
        public String getDescription() {
          return "All supported disk images(*.scl,*.trd)";
        }
      };

      File selectedFile =
          chooseFileForOpen("Select Disk " + diskName, this.lastFloppyFolder, filter,
              allSupportedFilters, sclFileFilter, trdFileFilter);
      if (selectedFile != null) {
        this.lastFloppyFolder = selectedFile.getParentFile();
        try {
          if (filter.get() == allSupportedFilters) {
            if (trdFileFilter.accept(selectedFile)) {
              filter.set(trdFileFilter);
            } else {
              filter.set(sclFileFilter);
            }
          }

          if (!selectedFile.isFile() && filter.get().getClass() == TrdFileFilter.class) {
            String name = selectedFile.getName();
            if (!name.contains(".")) {
              name += ".trd";
            }
            selectedFile = new File(selectedFile.getParentFile(), name);
            if (!selectedFile.isFile()) {
              if (JOptionPane
                  .showConfirmDialog(this, "Create TRD file: " + selectedFile.getName() + "?",
                      "Create TRD file", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
                LOGGER.log(Level.INFO, "Creating TRD disk: " + selectedFile.getAbsolutePath());
                FileUtils.writeByteArrayToFile(selectedFile, new TrDosDisk().getDiskData());
              } else {
                return;
              }
            }
          }

          final TrDosDisk floppy = new TrDosDisk(selectedFile,
              filter.get().getClass() == SclFileFilter.class ? TrDosDisk.SourceDataType.SCL :
                  TrDosDisk.SourceDataType.TRD, FileUtils.readFileToByteArray(selectedFile), false);
          this.board.getBetaDiskInterface().insertDiskIntoDrive(drive, floppy);
          LOGGER.log(Level.INFO,
              "Loaded drive " + diskName + " by floppy image file " + selectedFile);
        } catch (IOException ex) {
          LOGGER.log(Level.WARNING, "Can't read Floppy image file [" + selectedFile + ']', ex);
          JOptionPane.showMessageDialog(this, "Can't read Floppy image file", "Error",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    } finally {
      this.turnZxKeyboardOn();
      this.stepSemaphor.unlock();
    }
  }

  private void menuFileSelectDiskAActionPerformed(ActionEvent evt) {
    loadDiskIntoDrive(BetaDiscInterface.DRIVE_A);
  }

  private void formWindowLostFocus(java.awt.event.WindowEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.keyboardAndTapeModule.doReset();
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  private void formWindowGainedFocus(java.awt.event.WindowEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.getInputContext().selectInputMethod(Locale.ENGLISH);
      this.keyboardAndTapeModule.doReset();
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  private void menuFileLoadSnapshotActionPerformed(ActionEvent evt) {
    stepSemaphor.lock();
    try {
      this.turnZxKeyboardOff();
      if (AppOptions.getInstance().isTestRomActive()) {
        final JHtmlLabel label = new JHtmlLabel(
            "<html><body>ZX-Spectrum 128 ROM is required to load snapshots.<br>Go to menu <b><i><a href=\"rom\">File->Options</i></b></i> and choose ROM 128.</body></html>");
        label.addLinkListener((source, link) -> {
          if ("rom".equals(link)) {
            SwingUtilities.windowForComponent(source).setVisible(false);
            SwingUtilities.invokeLater(() -> menuFileOptions.doClick());
          }
        });
        JOptionPane.showMessageDialog(MainForm.this, label, "ZX-Spectrum ROM 128 image is required",
            JOptionPane.WARNING_MESSAGE);
        return;
      }

      final FileFilter formatZ80 = new FormatZ80();
      final FileFilter formatSna = new FormatSNA();
      final FileFilter formatZxp = new FormatZXP();
      final FileFilter formatSpec256 = new FormatSpec256();

      final FileFilter formatAll = new FileFilter() {
        @Override
        public boolean accept(File f) {
          return formatZ80.accept(f)
              || formatSpec256.accept(f)
              || formatSna.accept(f)
              || formatZxp.accept(f);
        }

        @Override
        public String getDescription() {
          return "All snapshots (*.z80, *.sna, *.zip, *.zxp)";
        }
      };

      final AtomicReference<FileFilter> theFilter = new AtomicReference<>();
      final File selected =
          chooseFileForOpen("Select snapshot", this.lastSnapshotFolder, theFilter, formatAll,
              formatZ80, formatSpec256, formatSna, formatZxp);

      if (selected != null) {
        this.board.forceResetAllCpu();
        this.board.resetIoDevices();

        this.lastSnapshotFolder = selected.getParentFile();
        try {
          if (theFilter.get() == formatAll) {
            if (formatZ80.accept(selected)) {
              theFilter.set(formatZ80);
            } else if (formatSna.accept(selected)) {
              theFilter.set(formatSna);
            } else if (formatZxp.accept(selected)) {
              theFilter.set(formatZxp);
            } else {
              theFilter.set(formatSpec256);
            }
          }

          final Snapshot selectedFilter = (Snapshot) theFilter.get();
          LOGGER.log(Level.INFO, "Loading snapshot " + selectedFilter.getName());
          selectedFilter.loadFromArray(selected, this.board, this.board.getVideoController(),
              FileUtils.readFileToByteArray(selected));
          this.menuOptionsZX128Mode.setState(this.board.getBoardMode() != BoardMode.ZXPOLY);
        } catch (Exception ex) {
          ex.printStackTrace();
          LOGGER.log(Level.WARNING, "Can't read snapshot file [" + ex.getMessage() + ']', ex);
          JOptionPane.showMessageDialog(this, "Can't read snapshot file [" + ex.getMessage() + ']',
              "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    } finally {
      this.turnZxKeyboardOn();
      stepSemaphor.unlock();
    }
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    scrollPanel = new javax.swing.JScrollPane();
    jSeparator2 = new JSeparator();
    panelIndicators = new javax.swing.JPanel();
    filler1 = new Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0),
        new java.awt.Dimension(32767, 0));
    labelTurbo =
        new JIndicatorLabel(ICO_TURBO, ICO_TURBO_DIS, "Turbo-mode is ON", "Turbo-mode is OFF");
    labelMouseUsage =
        new JIndicatorLabel(ICO_MOUSE, ICO_MOUSE_DIS, "Mouse is catched", "Mouse is not active");
    labelZX128 = new JIndicatorLabel(ICO_ZX128, ICO_ZX128_DIS, "ZX mode is ON", "ZX mode is OFF");
    labelTapeUsage =
        new JIndicatorLabel(ICO_TAPE, ICO_TAPE_DIS, "Reading tape", "No IO tape operations");
    labelDiskUsage = new JIndicatorLabel(ICO_DISK, ICO_DISK_DIS, "Some disk operation is active",
        "No IO disk operations");
    menuBar = new JMenuBar();
    menuFile = new JMenu();
    menuFileLoadSnapshot = new JMenuItem();
    menuFileLoadPoke = new JMenuItem();
    menuFileLoadTap = new JMenuItem();
    menuView = new JMenu();
    menuViewZoom = new JMenu();
    menuViewVideoFilter = new JMenu();
    menuViewZoomIn = new JMenuItem();
    menuViewZoomOut = new JMenuItem();
    menuLoadDrive = new JMenu();
    menuFileSelectDiskA = new JMenuItem();
    menuFileSelectDiskB = new JMenuItem();
    menuFileSelectDiskC = new JMenuItem();
    menuFileSelectDiskD = new JMenuItem();
    menuFileFlushDiskChanges = new JMenuItem();
    jSeparator1 = new Separator();
    menuFileOptions = new JMenuItem();
    jSeparator3 = new Separator();
    menuFileExit = new JMenuItem();
    menuTap = new JMenu();
    menuTapeRewindToStart = new JMenuItem();
    menuTapPrevBlock = new JMenuItem();
    menuTapPlay = new JCheckBoxMenuItem();
    menuTapNextBlock = new JMenuItem();
    menuTapGotoBlock = new JMenuItem();
    menuService = new JMenu();
    menuFileReset = new JMenuItem();
    menuServiceSaveScreen = new JMenuItem();
    menuServiceGameControllers = new JMenuItem();
    menuServiceSaveScreenAllVRAM = new JMenuItem();
    menuActionAnimatedGIF = new JMenuItem();
    menuServiceMakeSnapshot = new JMenuItem();
    menuTapExportAs = new JMenu();
    menuTapExportAsWav = new JMenuItem();
    menuCatcher = new JMenu();
    menuTriggerDiffMem = new JCheckBoxMenuItem();
    menuTriggerModuleCPUDesync = new JCheckBoxMenuItem();
    menuTriggerExeCodeDiff = new JCheckBoxMenuItem();
    menuTracer = new JMenu();
    menuTraceCpu0 = new JCheckBoxMenuItem();
    menuTraceCpu1 = new JCheckBoxMenuItem();
    menuTraceCpu2 = new JCheckBoxMenuItem();
    menuTraceCpu3 = new JCheckBoxMenuItem();
    menuOptions = new JMenu();
    menuOptionsShowIndicators = new JCheckBoxMenuItem();
    menuOptionsZX128Mode = new JCheckBoxMenuItem();
    menuOptionsTurbo = new JCheckBoxMenuItem();
    menuOptionsEnableTrapMouse = new JCheckBoxMenuItem();
    menuOptionsEnableSpeaker = new JCheckBoxMenuItem();
    menuOptionsEnableVideoStream = new JCheckBoxMenuItem();
    menuHelp = new JMenu();
    menuHelpAbout = new JMenuItem();
    menuHelpDonation = new JMenuItem();

    setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    setLocationByPlatform(true);

    addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        menuBar.repaint();
      }
    });

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

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelTurbo, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelMouseUsage, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelZX128, gridBagConstraints);

    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelTapeUsage, gridBagConstraints);

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

    menuView.setText("View");

    menuViewZoomIn.setText("Zoom In");
    menuViewZoomIn.addActionListener(e -> this.board.getVideoController().zoomIn());
    menuViewZoomIn
        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS, KeyEvent.CTRL_DOWN_MASK));
    menuViewZoomOut.setText("Zoom Out");
    menuViewZoomOut.addActionListener(e -> this.board.getVideoController().zoomOut());
    menuViewZoomOut
        .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_MINUS, KeyEvent.CTRL_DOWN_MASK));

    menuViewZoom.setText("Zoom");

    menuViewZoom.add(menuViewZoomIn);
    menuViewZoom.add(menuViewZoomOut);

    menuView.add(menuViewZoom);

    menuViewVideoFilter.setText("Video filter");
    final ButtonGroup tvFilterGroup = new ButtonGroup();
    for (final TvFilterChain chain : TvFilterChain.values()) {
      final JRadioButtonMenuItem tvFilterMenuItem =
          new JRadioButtonMenuItem(chain.getText(), chain == TvFilterChain.NONE);
      tvFilterGroup.add(tvFilterMenuItem);
      tvFilterMenuItem.addActionListener(e -> {
        this.menuActionAnimatedGIF.setEnabled(chain == TvFilterChain.NONE);
        if (tvFilterMenuItem.isSelected()) {
          this.board.getVideoController().setTvFilterChain(chain);
        }
      });
      menuViewVideoFilter.add(tvFilterMenuItem);
    }

    menuView.add(menuViewVideoFilter);

    menuFileLoadSnapshot.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/snapshot.png"))); // NOI18N
    menuFileLoadSnapshot.setText("Load Snapshot");
    menuFileLoadSnapshot.addActionListener(this::menuFileLoadSnapshotActionPerformed);
    menuFile.add(menuFileLoadSnapshot);

    menuFileLoadPoke.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/poke.png"))); // NOI18N
    menuFileLoadPoke.setText("Load Poke");
    menuFileLoadPoke.addActionListener(this::menuFileLoadPokeActionPerformed);
    menuFile.add(menuFileLoadPoke);

    menuFileLoadTap.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/cassette.png"))); // NOI18N
    menuFileLoadTap.setText("Load TAPE");
    menuFileLoadTap.addActionListener(this::menuFileLoadTapActionPerformed);
    menuFile.add(menuFileLoadTap);

    menuLoadDrive.setIcon(
        new ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/disk.png"))); // NOI18N
    menuLoadDrive.setText("Load Disk..");
    menuLoadDrive.addMenuListener(new javax.swing.event.MenuListener() {
      public void menuCanceled(javax.swing.event.MenuEvent evt) {
      }

      public void menuDeselected(javax.swing.event.MenuEvent evt) {
      }

      public void menuSelected(javax.swing.event.MenuEvent evt) {
        menuLoadDriveMenuSelected(evt);
      }
    });

    menuFileSelectDiskA.setText("Drive A");
    menuFileSelectDiskA.addActionListener(this::menuFileSelectDiskAActionPerformed);
    menuLoadDrive.add(menuFileSelectDiskA);

    menuFileSelectDiskB.setText("Drive B");
    menuFileSelectDiskB.addActionListener(this::menuFileSelectDiskBActionPerformed);
    menuLoadDrive.add(menuFileSelectDiskB);

    menuFileSelectDiskC.setText("Drive C");
    menuFileSelectDiskC.addActionListener(this::menuFileSelectDiskCActionPerformed);
    menuLoadDrive.add(menuFileSelectDiskC);

    menuFileSelectDiskD.setText("Drive D");
    menuFileSelectDiskD.addActionListener(this::menuFileSelectDiskDActionPerformed);
    menuLoadDrive.add(menuFileSelectDiskD);

    menuFile.add(menuLoadDrive);

    menuFileFlushDiskChanges.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/diskflush.png"))); // NOI18N
    menuFileFlushDiskChanges.setText("Flush disk changes");
    menuFileFlushDiskChanges.addActionListener(this::menuFileFlushDiskChangesActionPerformed);
    menuFile.add(menuFileFlushDiskChanges);
    menuFile.add(jSeparator1);

    menuFileOptions.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/settings.png"))); // NOI18N
    menuFileOptions.setText("Preferences");
    menuFileOptions.addActionListener(this::menuFileOptionsActionPerformed);
    menuFile.add(menuFileOptions);
    menuFile.add(jSeparator3);

    menuFileExit.setAccelerator(
        getKeyStroke(java.awt.event.KeyEvent.VK_F4, java.awt.event.InputEvent.ALT_MASK));
    menuFileExit.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/reset.png"))); // NOI18N
    menuFileExit.setText("Exit");
    menuFileExit.addActionListener(this::menuFileExitActionPerformed);
    menuFile.add(menuFileExit);


    menuBar.add(menuFile);

    menuTap.setText("Tape");

    menuTapeRewindToStart.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_previous.png"))); // NOI18N
    menuTapeRewindToStart.setText("Rewind to start");
    menuTapeRewindToStart.addActionListener(this::menuTapeRewindToStartActionPerformed);
    menuTap.add(menuTapeRewindToStart);

    menuTapPrevBlock.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_backward.png"))); // NOI18N
    menuTapPrevBlock.setText("Prev block");
    menuTapPrevBlock.addActionListener(this::menuTapPrevBlockActionPerformed);
    menuTap.add(menuTapPrevBlock);

    menuTapPlay.setAccelerator(getKeyStroke(java.awt.event.KeyEvent.VK_F4, 0));
    menuTapPlay.setText("Play");
    menuTapPlay.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_play.png"))); // NOI18N
    menuTapPlay.setInheritsPopupMenu(true);
    menuTapPlay.addActionListener(this::menuTapPlayActionPerformed);
    menuTap.add(menuTapPlay);

    menuTapNextBlock.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_forward.png"))); // NOI18N
    menuTapNextBlock.setText("Next block");
    menuTapNextBlock.addActionListener(this::menuTapNextBlockActionPerformed);
    menuTap.add(menuTapNextBlock);

    menuTapGotoBlock.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_pos.png"))); // NOI18N
    menuTapGotoBlock.setText("Go to block");
    menuTapGotoBlock.addActionListener(this::menuTapGotoBlockActionPerformed);
    menuTap.add(menuTapGotoBlock);

    menuBar.add(menuTap);
    menuBar.add(menuView);

    menuService.setText("Service");

    menuFileReset.setAccelerator(getKeyStroke(java.awt.event.KeyEvent.VK_F12, 0));
    menuFileReset.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/reset2.png"))); // NOI18N
    menuFileReset.setText("Reset");
    menuFileReset.addActionListener(this::menuFileResetActionPerformed);
    menuService.add(menuFileReset);

    menuServiceSaveScreen.setAccelerator(getKeyStroke(java.awt.event.KeyEvent.VK_F8, 0));
    menuServiceSaveScreen.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/photo.png"))); // NOI18N
    menuServiceSaveScreen.setText("Make Screenshot");
    menuServiceSaveScreen.addActionListener(this::menuServiceSaveScreenActionPerformed);
    menuService.add(menuServiceSaveScreen);

    menuServiceSaveScreenAllVRAM.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/photom.png"))); // NOI18N
    menuServiceSaveScreenAllVRAM.setText("Make Screenshot of all VRAM");
    menuServiceSaveScreenAllVRAM
        .addActionListener(this::menuServiceSaveScreenAllVRAMActionPerformed);
    menuService.add(menuServiceSaveScreenAllVRAM);

    menuActionAnimatedGIF.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/file_gif.png"))); // NOI18N
    menuActionAnimatedGIF.setText("Make Animated GIF");
    menuActionAnimatedGIF.addActionListener(this::menuActionAnimatedGIFActionPerformed);
    menuService.add(menuActionAnimatedGIF);

    menuServiceMakeSnapshot.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/save_snapshot.png"))); // NOI18N
    menuServiceMakeSnapshot.setText("Save snapshot");
    menuServiceMakeSnapshot.addActionListener(this::menuServicemakeSnapshotActionPerformed);
    menuService.add(menuServiceMakeSnapshot);

    menuTapExportAs.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_record.png"))); // NOI18N
    menuTapExportAs.setText("Export TAPE as..");

    menuTapExportAsWav.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/file_wav.png"))); // NOI18N
    menuTapExportAsWav.setText("WAV file");
    menuTapExportAsWav.addActionListener(this::menuTapExportAsWavActionPerformed);
    menuTapExportAs.add(menuTapExportAsWav);

    menuService.add(menuTapExportAs);

    menuServiceGameControllers.setText("Game controllers");
    menuServiceGameControllers.setToolTipText("Turn on game controller");
    menuServiceGameControllers.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/gcontroller.png"))); // NOI18N
    menuServiceGameControllers.addActionListener(this::menuServiceGameControllerActionPerformed);

    menuService.add(menuServiceGameControllers);

    menuCatcher.setText("Test triggers");

    menuTriggerDiffMem.setText("Diff mem.content");
    menuTriggerDiffMem.addActionListener(this::menuTriggerDiffMemActionPerformed);
    menuCatcher.add(menuTriggerDiffMem);

    menuTriggerModuleCPUDesync.setText("CPUs state desync");
    menuTriggerModuleCPUDesync.addActionListener(this::menuTriggerModuleCPUDesyncActionPerformed);
    menuCatcher.add(menuTriggerModuleCPUDesync);

    menuTriggerExeCodeDiff.setText("Exe code difference");
    menuTriggerExeCodeDiff.addActionListener(this::menuTriggerExeCodeDiffActionPerformed);
    menuCatcher.add(menuTriggerExeCodeDiff);

    menuService.add(menuCatcher);

    menuTracer.setText("Trace");

    menuTraceCpu0.setText("CPU0");
    menuTraceCpu0.addActionListener(this::menuTraceCpu0ActionPerformed);
    menuTracer.add(menuTraceCpu0);

    menuTraceCpu1.setText("CPU1");
    menuTraceCpu1.addActionListener(this::menuTraceCpu1ActionPerformed);
    menuTracer.add(menuTraceCpu1);

    menuTraceCpu2.setText("CPU2");
    menuTraceCpu2.addActionListener(this::menuTraceCpu2ActionPerformed);
    menuTracer.add(menuTraceCpu2);

    menuTraceCpu3.setText("CPU3");
    menuTraceCpu3.addActionListener(this::menuTraceCpu3ActionPerformed);
    menuTracer.add(menuTraceCpu3);

    menuService.add(menuTracer);

    menuBar.add(menuService);

    menuOptions.setText("Options");

    menuOptionsShowIndicators.setSelected(true);
    menuOptionsShowIndicators.setText("Indicator panel");
    menuOptionsShowIndicators.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/indicator.png"))); // NOI18N
    menuOptionsShowIndicators.addActionListener(this::menuOptionsShowIndicatorsActionPerformed);
    menuOptions.add(menuOptionsShowIndicators);

    menuOptionsZX128Mode.setSelected(true);
    menuOptionsZX128Mode.setText("ZX Mode");
    menuOptionsZX128Mode.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/zx128.png"))); // NOI18N
    menuOptionsZX128Mode.addActionListener(this::menuOptionsZX128ModeActionPerformed);
    menuOptions.add(menuOptionsZX128Mode);

    menuOptionsTurbo.setAccelerator(getKeyStroke(java.awt.event.KeyEvent.VK_F3, 0));
    menuOptionsTurbo.setSelected(true);
    menuOptionsTurbo.setText("Turbo");
    menuOptionsTurbo.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/turbo.png"))); // NOI18N
    menuOptionsTurbo.addActionListener(this::menuOptionsTurboActionPerformed);
    menuOptions.add(menuOptionsTurbo);

    menuOptionsEnableTrapMouse.setText("Trap mouse");
    menuOptionsEnableTrapMouse.setToolTipText("Trap mouse as kempston-mouse");
    menuOptionsEnableTrapMouse.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/pointer.png"))); // NOI18N
    menuOptionsEnableTrapMouse.addActionListener(this::menuOptionsEnableTrapMouseActionPerformed);
    menuOptions.add(menuOptionsEnableTrapMouse);

    menuOptionsEnableSpeaker.setText("Beeper");
    menuOptionsEnableSpeaker.setToolTipText("Turn on beeper sound");
    menuOptionsEnableSpeaker.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/speaker.png"))); // NOI18N
    menuOptionsEnableSpeaker.addActionListener(this::menuOptionsEnableSpeakerActionPerformed);
    menuOptions.add(menuOptionsEnableSpeaker);

    menuOptionsEnableVideoStream.setText("Video stream (beta)");
    menuOptionsEnableVideoStream.setToolTipText("Turn on video streaming");
    menuOptionsEnableVideoStream.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/streaming.png"))); // NOI18N
    menuOptionsEnableVideoStream
        .addActionListener(this::menuOptionsEnableVideoStreamActionPerformed);
    menuOptions.add(menuOptionsEnableVideoStream);

    menuBar.add(menuOptions);

    menuHelp.setText("Help");

    menuHelpAbout.setAccelerator(getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
    menuHelpAbout.setIcon(
        new ImageIcon(getClass().getResource("/com/igormaznitsa/zxpoly/icons/info.png"))); // NOI18N
    menuHelpAbout.setText("Help");
    menuHelpAbout.addActionListener(this::menuHelpAboutActionPerformed);
    menuHelp.add(menuHelpAbout);

    menuHelpDonation.setText("Make donation");
    menuHelpDonation.addActionListener(e -> {
      if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        try {
          Desktop.getDesktop().browse(new URI(
              "https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=AHWJHJFBAWGL2"));
        } catch (Exception ex) {
          LOGGER.warning("Can't open link: " + ex.getMessage());
        }
      }
    });
    menuHelpDonation.setIcon(new ImageIcon(
        getClass().getResource("/com/igormaznitsa/zxpoly/icons/donate.png"))); // NOI18N
    menuHelp.add(menuHelpDonation);

    menuBar.add(menuHelp);

    setJMenuBar(menuBar);

    pack();
  }

  private void menuFileLoadPokeActionPerformed(ActionEvent evt) {
    stepSemaphor.lock();
    try {
      this.turnZxKeyboardOff();

      final JFileChooser trainerFileChooser = new JFileChooser(this.lastPokeFileFolder);
      trainerFileChooser.setDialogTitle("Select trainer");
      trainerFileChooser.setMultiSelectionEnabled(false);
      trainerFileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
      trainerFileChooser.setAcceptAllFileFilterUsed(false);
      final FileFilter pokTrainer = new TrainerPok();
      trainerFileChooser.addChoosableFileFilter(pokTrainer);

      if (trainerFileChooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        final AbstractTrainer selectedTrainer =
            (AbstractTrainer) trainerFileChooser.getFileFilter();
        final File selectedFile = trainerFileChooser.getSelectedFile();
        this.lastPokeFileFolder = selectedFile.getParentFile();
        try {
          selectedTrainer.apply(this, selectedFile, this.board);
        } catch (Exception ex) {
          LOGGER.log(Level.WARNING, "Error during trainer processing: " + ex.getMessage(), ex);
          JOptionPane.showMessageDialog(this, ex.getMessage(), "Can't read or parse file",
              JOptionPane.ERROR_MESSAGE);
        }
      }
    } finally {
      this.turnZxKeyboardOn();
      stepSemaphor.unlock();
    }
  }

  private void menuOptionsZX128ModeActionPerformed(ActionEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.board.resetAndRestoreRom(BASE_ROM);
      this.board
          .setBoardMode(this.menuOptionsZX128Mode.isSelected() ? BoardMode.ZX128 : BoardMode.ZXPOLY,
              true);
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  private void menuOptionsTurboActionPerformed(ActionEvent evt) {
    final boolean turboActivated = this.menuOptionsTurbo.isSelected();
    this.board.getBeeper().setSourceSoundPort(null);
    this.setTurboMode(turboActivated);
  }

  private void menuFileSelectDiskCActionPerformed(ActionEvent evt) {
    loadDiskIntoDrive(BetaDiscInterface.DRIVE_C);
  }

  private void menuFileSelectDiskBActionPerformed(ActionEvent evt) {
    loadDiskIntoDrive(BetaDiscInterface.DRIVE_B);
  }

  private void menuFileSelectDiskDActionPerformed(ActionEvent evt) {
    loadDiskIntoDrive(BetaDiscInterface.DRIVE_D);
  }

  private void menuFileExitActionPerformed(ActionEvent evt) {
    this.formWindowClosing(null);
  }

  private void menuTapGotoBlockActionPerformed(ActionEvent evt) {
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
  }

  private void menuFileLoadTapActionPerformed(ActionEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.turnZxKeyboardOff();
      final File selectedTapFile =
          chooseFileForOpen("Load Tape", this.lastTapFolder, null, new TapFileFilter());
      if (selectedTapFile != null) {
        this.lastTapFolder = selectedTapFile.getParentFile();
        try (InputStream in = new BufferedInputStream(new FileInputStream(selectedTapFile))) {

          if (this.keyboardAndTapeModule.getTap() != null) {
            this.keyboardAndTapeModule.getTap().removeActionListener(this);
          }

          final TapeFileReader tapfile = new TapeFileReader(selectedTapFile.getAbsolutePath(), in);
          tapfile.addActionListener(this);
          this.keyboardAndTapeModule.setTap(tapfile);
        } catch (Exception ex) {
          LOGGER.log(Level.SEVERE, "Can't read " + selectedTapFile, ex);
          JOptionPane.showMessageDialog(this, "Can't load TAP file", ex.getMessage(),
              JOptionPane.ERROR_MESSAGE);
        } finally {
          updateTapeMenu();
        }
      }
    } finally {
      this.turnZxKeyboardOn();
      this.stepSemaphor.unlock();
    }
  }

  private void menuTapExportAsWavActionPerformed(ActionEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.turnZxKeyboardOff();
      final byte[] wav = this.keyboardAndTapeModule.getTap().getAsWAV();
      File fileToSave = chooseFileForSave("Select WAV file", null, null, true, new WavFileFilter());
      if (fileToSave != null) {
        final String name = fileToSave.getName();
        if (!name.contains(".")) {
          fileToSave = new File(fileToSave.getParentFile(), name + ".wav");
        }
        FileUtils.writeByteArrayToFile(fileToSave, wav);
        LOGGER.log(Level.INFO,
            "Exported current TAP file as WAV file " + fileToSave + " size " + wav.length
                + " bytes");
      }
    } catch (Exception ex) {
      LOGGER.log(Level.WARNING, "Can't export as WAV", ex);
      JOptionPane.showMessageDialog(this, "Can't export as WAV", ex.getMessage(),
          JOptionPane.ERROR_MESSAGE);
    } finally {
      this.turnZxKeyboardOn();
      this.stepSemaphor.unlock();
    }
  }

  private void menuTapPlayActionPerformed(ActionEvent evt) {
    if (this.menuTapPlay.isSelected()) {
      this.keyboardAndTapeModule.getTap().startPlay();
    } else {
      this.keyboardAndTapeModule.getTap().stopPlay();
    }
    updateTapeMenu();
  }

  private void menuTapPrevBlockActionPerformed(ActionEvent evt) {
    final TapeFileReader tap = this.keyboardAndTapeModule.getTap();
    if (tap != null) {
      tap.rewindToPrevBlock();
    }
    updateTapeMenu();
  }

  private void menuTapNextBlockActionPerformed(ActionEvent evt) {
    final TapeFileReader tap = this.keyboardAndTapeModule.getTap();
    if (tap != null) {
      tap.rewindToNextBlock();
    }
    updateTapeMenu();
  }

  private void menuTapeRewindToStartActionPerformed(ActionEvent evt) {
    final TapeFileReader tap = this.keyboardAndTapeModule.getTap();
    if (tap != null) {
      tap.rewindToStart();
    }
    updateTapeMenu();
  }

  private void menuServiceSaveScreenActionPerformed(ActionEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.turnZxKeyboardOff();
      final RenderedImage img = this.board.getVideoController().makeCopyOfCurrentPicture();
      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      ImageIO.write(img, "png", buffer);
      File pngFile = chooseFileForSave("Save screenshot", lastScreenshotFolder, null, true,
          new PngFileFilter());
      if (pngFile != null) {
        final String fileName = pngFile.getName();
        if (!fileName.contains(".")) {
          pngFile = new File(pngFile.getParentFile(), fileName + ".png");
        }
        this.lastScreenshotFolder = pngFile.getParentFile();
        FileUtils.writeByteArrayToFile(pngFile, buffer.toByteArray());
      }
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Can't save screenshot for error, see the log!", "Error",
          JOptionPane.ERROR_MESSAGE);
      LOGGER.log(Level.SEVERE, "Can't make screenshot", ex);
    } finally {
      this.turnZxKeyboardOn();
      this.stepSemaphor.unlock();
    }

  }

  private void menuFileOptionsActionPerformed(ActionEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.turnZxKeyboardOff();
      final OptionsPanel optionsPanel = new OptionsPanel(null);
      if (JOptionPane
          .showConfirmDialog(this, optionsPanel, "Preferences", JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
        optionsPanel.getData().store();
        JOptionPane
            .showMessageDialog(this, "Some options will be activated only after emulator restart!",
                "Restart may required!",
                JOptionPane.WARNING_MESSAGE);
      }
    } finally {
      this.turnZxKeyboardOn();
      this.stepSemaphor.unlock();
    }
  }

  private void menuHelpAboutActionPerformed(ActionEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.turnZxKeyboardOff();
      new AboutDialog(this).setVisible(true);
    } finally {
      this.turnZxKeyboardOn();
      this.stepSemaphor.unlock();
    }
  }

  private void menuTraceCpu0ActionPerformed(ActionEvent evt) {
    if (this.menuTraceCpu0.isSelected()) {
      activateTracerForCPUModule(0);
    } else {
      deactivateTracerForCPUModule(0);
    }
  }

  private void menuTraceCpu1ActionPerformed(ActionEvent evt) {
    if (this.menuTraceCpu1.isSelected()) {
      activateTracerForCPUModule(1);
    } else {
      deactivateTracerForCPUModule(1);
    }
  }

  private void menuTraceCpu2ActionPerformed(ActionEvent evt) {
    if (this.menuTraceCpu2.isSelected()) {
      activateTracerForCPUModule(2);
    } else {
      deactivateTracerForCPUModule(2);
    }
  }

  private void menuTraceCpu3ActionPerformed(ActionEvent evt) {
    if (this.menuTraceCpu3.isSelected()) {
      activateTracerForCPUModule(3);
    } else {
      deactivateTracerForCPUModule(3);
    }
  }

  private void menuServiceSaveScreenAllVRAMActionPerformed(ActionEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.turnZxKeyboardOff();
      final RenderedImage[] images =
          this.board.getVideoController().renderAllModuleVideoMemoryInZx48Mode();

      final BufferedImage result =
          new BufferedImage(images[0].getWidth() * images.length, images[0].getHeight(),
              BufferedImage.TYPE_INT_RGB);
      final Graphics g = result.getGraphics();
      for (int i = 0; i < images.length; i++) {
        g.drawImage((Image) images[i], i * images[0].getWidth(), 0, null);
      }
      g.dispose();

      final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
      ImageIO.write(result, "png", buffer);
      File pngFile = chooseFileForSave("Save screenshot", lastScreenshotFolder, null, true,
          new PngFileFilter());
      if (pngFile != null) {
        final String fileName = pngFile.getName();
        if (!fileName.contains(".")) {
          pngFile = new File(pngFile.getParentFile(), fileName + ".png");
        }
        this.lastScreenshotFolder = pngFile.getParentFile();
        FileUtils.writeByteArrayToFile(pngFile, buffer.toByteArray());
      }
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Can't save screenshot for error, see the log!", "Error",
          JOptionPane.ERROR_MESSAGE);
      LOGGER.log(Level.SEVERE, "Can't make screenshot", ex);
    } finally {
      this.turnZxKeyboardOn();
      this.stepSemaphor.unlock();
    }
  }

  private void turnZxKeyboardOff() {
    this.zxKeyboardProcessingAllowed = false;
  }

  private void turnZxKeyboardOn() {
    this.keyboardAndTapeModule.doReset();
    this.zxKeyboardProcessingAllowed = true;
  }

  private void menuActionAnimatedGIFActionPerformed(ActionEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.turnZxKeyboardOff();
      AnimationEncoder encoder = this.currentAnimationEncoder.get();
      if (encoder == null) {
        final AnimatedGifTunePanel panel = new AnimatedGifTunePanel(this.lastAnimGifOptions);
        final int result = JOptionPane.showConfirmDialog(this, panel, "Options for Animated GIF",
            JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
          return;
        }
        this.menuViewVideoFilter.setEnabled(false);
        this.lastAnimGifOptions = panel.getValue();
        try {
          if (this.board.getBoardMode() == BoardMode.SPEC256) {
            encoder = new Spec256AGifEncoder(new File(this.lastAnimGifOptions.filePath),
                this.lastAnimGifOptions.frameRate, this.lastAnimGifOptions.repeat);
          } else {
            encoder = new ZxPolyAGifEncoder(new File(this.lastAnimGifOptions.filePath),
                this.lastAnimGifOptions.frameRate, this.lastAnimGifOptions.repeat);
          }
        } catch (IOException ex) {
          this.menuViewVideoFilter.setEnabled(true);
          LOGGER.log(Level.SEVERE, "Can't create GIF encoder", ex);
          return;
        }

        if (this.currentAnimationEncoder.compareAndSet(null, encoder)) {
          this.menuActionAnimatedGIF.setIcon(ICO_AGIF_STOP);
          this.menuActionAnimatedGIF.setText(TEXT_STOP_ANIM_GIF);
          LOGGER.info("Animated GIF recording has been started");
        }
      } else {
        this.menuViewVideoFilter.setEnabled(true);
        closeAnimationSave();
        if (this.currentAnimationEncoder.compareAndSet(encoder, null)) {
          this.menuActionAnimatedGIF.setIcon(ICO_AGIF_RECORD);
          this.menuActionAnimatedGIF.setText(TEXT_START_ANIM_GIF);
          LOGGER.info("Animated GIF recording has been stopped");
        }
      }
    } finally {
      this.turnZxKeyboardOn();
      this.stepSemaphor.unlock();
    }
  }

  private void closeAnimationSave() {
    AnimationEncoder encoder = this.currentAnimationEncoder.get();
    if (encoder != null) {
      try {
        encoder.close();
      } catch (IOException ex) {
        LOGGER.warning("Error during animation file close");
      }
    }
  }

  private void formWindowClosed(java.awt.event.WindowEvent evt) {
    closeAnimationSave();
  }

  private void menuTriggerModuleCPUDesyncActionPerformed(ActionEvent evt) {
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
  }

  private void menuTriggerDiffMemActionPerformed(ActionEvent evt) {
    this.stepSemaphor.lock();
    try {
      if (this.menuTriggerDiffMem.isSelected()) {
        final AddressPanel panel = new AddressPanel(this.board.getMemTriggerAddress());
        if (JOptionPane.showConfirmDialog(MainForm.this, panel, "Triggering address",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
          try {
            final int addr = panel.extractAddressFromText();
            if (addr < 0 || addr > 0xFFFF) {
              JOptionPane.showMessageDialog(MainForm.this, "Error address must be in #0000...#FFFF",
                  "Error address", JOptionPane.ERROR_MESSAGE);
            } else {
              this.board.setMemTriggerAddress(addr);
              this.board.setTrigger(Motherboard.TRIGGER_DIFF_MEM_ADDR);
            }
          } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(MainForm.this,
                "Error address format, use # for hexadecimal address (example #AA00)",
                "Error address", JOptionPane.ERROR_MESSAGE);
          }
        }
      } else {
        this.board.resetTrigger(Motherboard.TRIGGER_DIFF_MEM_ADDR);
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  private void menuTriggerExeCodeDiffActionPerformed(ActionEvent evt) {
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
  }

  private void menuServicemakeSnapshotActionPerformed(ActionEvent evt) {
    stepSemaphor.lock();
    try {
      final AtomicReference<FileFilter> theFilter = new AtomicReference<>();
      File selected = chooseFileForSave("Save snapshot", this.lastSnapshotFolder, theFilter, false,
          new FormatZXP(), new FormatZ80(), new FormatSNA());

      if (selected != null) {
        this.lastSnapshotFolder = selected.getParentFile();
        try {
          final Snapshot selectedFilter = (Snapshot) theFilter.get();
          if (!selectedFilter.getExtension()
              .equals(FilenameUtils.getExtension(selected.getName()).toLowerCase(Locale.ENGLISH))) {
            selected = new File(selected.getParentFile(),
                selected.getName() + '.' + selectedFilter.getExtension());
          }

          if (selected.isFile() && JOptionPane
              .showConfirmDialog(
                  this,
                  String.format("Do you want override file '%s'?", selected.getName()),
                  "Found existing file",
                  JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
            return;
          }

          LOGGER.info(
              "Saving snapshot " + selectedFilter.getName() + " as file " + selected.getName());
          final byte[] preparedSnapshotData =
              selectedFilter.saveToArray(this.board, this.board.getVideoController());
          LOGGER.info("Prepared snapshot data, size " + preparedSnapshotData.length + " bytes");
          FileUtils.writeByteArrayToFile(selected, preparedSnapshotData);
        } catch (Exception ex) {
          ex.printStackTrace();
          LOGGER.log(Level.WARNING, "Can't save snapshot file [" + ex.getMessage() + ']', ex);
          JOptionPane.showMessageDialog(this, "Can't save snapshot file [" + ex.getMessage() + ']',
              "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    } finally {
      stepSemaphor.unlock();
    }
  }

  private void menuFileMenuSelected(final MenuEvent evt) {
    boolean hasChangedDisk = false;
    for (int i = 0; i < 4; i++) {
      final TrDosDisk disk = this.board.getBetaDiskInterface().getDiskInDrive(i);
      hasChangedDisk |= (disk != null && disk.isChanged());
    }
    this.menuFileFlushDiskChanges.setEnabled(hasChangedDisk);
  }

  private void menuFileFlushDiskChangesActionPerformed(ActionEvent evt) {
    for (int i = 0; i < 4; i++) {
      final TrDosDisk disk = this.board.getBetaDiskInterface().getDiskInDrive(i);
      if (disk != null && disk.isChanged()) {
        final int result = JOptionPane.showConfirmDialog(this,
            "Do you want flush disk data '" + disk.getSrcFile().getName() + "' ?", "Disk changed",
            JOptionPane.YES_NO_CANCEL_OPTION);
        if (result == JOptionPane.CANCEL_OPTION) {
          break;
        }
        if (result == JOptionPane.YES_OPTION) {
          final File destFile;
          if (disk.getType() != TrDosDisk.SourceDataType.TRD) {
            final JFileChooser fileChooser = new JFileChooser(disk.getSrcFile().getParentFile());
            fileChooser.setFileFilter(new TrdFileFilter());
            fileChooser.setAcceptAllFileFilterUsed(false);
            fileChooser.setDialogTitle("Save disk as TRD file");
            fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
            fileChooser.setSelectedFile(new File(disk.getSrcFile().getParentFile(),
                FilenameUtils.getBaseName(disk.getSrcFile().getName()) + ".trd"));
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
              disk.replaceSrcFile(destFile, TrDosDisk.SourceDataType.TRD, true);
              LOGGER.info("Changes for disk " + ('A' + i) + " is saved as file: "
                  + destFile.getAbsolutePath());
            } catch (IOException ex) {
              LOGGER.warning("Can't write disk for error: " + ex.getMessage());
              JOptionPane
                  .showMessageDialog(this, "Can't save disk for IO error: " + ex.getMessage(),
                      "Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        }
      }
    }
  }

  private void formWindowClosing(java.awt.event.WindowEvent evt) {
    boolean hasChangedDisk = false;
    for (int i = 0; i < 4; i++) {
      final TrDosDisk disk = this.board.getBetaDiskInterface().getDiskInDrive(i);
      hasChangedDisk |= (disk != null && disk.isChanged());
    }

    boolean close = false;

    if (hasChangedDisk) {
      if (JOptionPane
          .showConfirmDialog(this, "Emulator has unsaved disks, do you realy want to close it?",
              "Detected unsaved data", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        close = true;
      }
    } else {
      close = true;
    }

    this.board.dispose();

    if (close) {
      System.exit(0);
    }
  }

  private void menuLoadDriveMenuSelected(javax.swing.event.MenuEvent evt) {
    final JMenuItem[] disks = new JMenuItem[] {this.menuFileSelectDiskA, this.menuFileSelectDiskB,
        this.menuFileSelectDiskC, this.menuFileSelectDiskD};
    for (int i = 0; i < 4; i++) {
      final TrDosDisk disk = this.board.getBetaDiskInterface().getDiskInDrive(i);
      disks[i].setIcon(disk == null ? null : ICO_MDISK);
    }
  }

  private void menuOptionsEnableTrapMouseActionPerformed(ActionEvent evt) {
    this.board.getVideoController()
        .setEnableTrapMouse(this.menuOptionsEnableTrapMouse.isSelected());
  }

  private void activateTracerForCPUModule(final int index) {
    TraceCpuForm form = this.cpuTracers[index];
    if (form == null) {
      form = new TraceCpuForm(this, this.board, index);
      form.setVisible(true);
    }
    form.toFront();
    form.requestFocus();
  }

  private void deactivateTracerForCPUModule(final int index) {
    TraceCpuForm form = this.cpuTracers[index];
    if (form != null) {
      form.dispose();
    }
  }

  private void updateTracerCheckBoxes() {
    this.menuTraceCpu0.setSelected(this.cpuTracers[0] != null);
    this.menuTraceCpu1.setSelected(this.cpuTracers[1] != null);
    this.menuTraceCpu2.setSelected(this.cpuTracers[2] != null);
    this.menuTraceCpu3.setSelected(this.cpuTracers[3] != null);
  }

  public void onTracerActivated(final TraceCpuForm tracer) {
    final int index = tracer.getModuleIndex();
    this.cpuTracers[index] = tracer;
    updateTracerCheckBoxes();
    this.activeTracerWindowCounter.incrementAndGet();
  }

  public void onTracerDeactivated(final TraceCpuForm tracer) {
    final int index = tracer.getModuleIndex();
    this.cpuTracers[index] = null;
    updateTracerCheckBoxes();
    this.activeTracerWindowCounter.decrementAndGet();
  }

  private File chooseFileForOpen(final String title, final File initial,
                                 final AtomicReference<FileFilter> selectedFilter,
                                 final FileFilter... filter) {
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

  private File chooseFileForSave(final String title, final File initial,
                                 final AtomicReference<FileFilter> selectedFilter,
                                 final boolean allowAcceptAll, final FileFilter... filters) {
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

  private static class TrdFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".trd");
    }

    @Override
    public String getDescription() {
      return "TR-DOS image (*.trd)";
    }

  }

  private static class PngFileFilter extends FileFilter {

    @Override
    public boolean accept(File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".png");
    }

    @Override
    public String getDescription() {
      return "PNG image (*.png)";
    }

  }

  private static class SclFileFilter extends FileFilter {

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
    public boolean accept(final File f) {
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

  private final class KeyboardDispatcher implements KeyEventDispatcher {

    private final VideoController videoController;
    private final KeyboardKempstonAndTapeIn keyboard;

    KeyboardDispatcher(final VideoController videoController, final KeyboardKempstonAndTapeIn kbd) {
      this.keyboard = kbd;
      this.videoController = videoController;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
      boolean consumed = false;
      if (!e.isConsumed() && MainForm.this.zxKeyboardProcessingAllowed) {
        if (e.getKeyCode() == KeyEvent.VK_F5) {
          this.videoController.setShowZxKeyboardLayout(e.getID() == KeyEvent.KEY_PRESSED);
          e.consume();
          consumed = true;
        } else {
          consumed = this.keyboard.onKeyEvent(e);
        }
      }
      return consumed;
    }
  }
}
