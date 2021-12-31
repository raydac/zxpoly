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

import com.igormaznitsa.z80.Z80;
import com.igormaznitsa.zxpoly.animeencoders.AGifEncoder;
import com.igormaznitsa.zxpoly.animeencoders.AnimatedGifTunePanel;
import com.igormaznitsa.zxpoly.animeencoders.AnimationEncoder;
import com.igormaznitsa.zxpoly.components.*;
import com.igormaznitsa.zxpoly.components.betadisk.BetaDiscInterface;
import com.igormaznitsa.zxpoly.components.betadisk.TrDosDisk;
import com.igormaznitsa.zxpoly.components.snd.Beeper;
import com.igormaznitsa.zxpoly.components.snd.SourceSoundPort;
import com.igormaznitsa.zxpoly.components.snd.VolumeProfile;
import com.igormaznitsa.zxpoly.components.tapereader.TapeContext;
import com.igormaznitsa.zxpoly.components.tapereader.TapeSource;
import com.igormaznitsa.zxpoly.components.tapereader.TapeSourceFactory;
import com.igormaznitsa.zxpoly.components.video.VideoController;
import com.igormaznitsa.zxpoly.components.video.VirtualKeyboardDecoration;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import com.igormaznitsa.zxpoly.components.video.tvfilters.TvFilterChain;
import com.igormaznitsa.zxpoly.formats.*;
import com.igormaznitsa.zxpoly.streamer.ZxVideoStreamer;
import com.igormaznitsa.zxpoly.tracer.TraceCpuForm;
import com.igormaznitsa.zxpoly.trainers.AbstractTrainer;
import com.igormaznitsa.zxpoly.trainers.TrainerPok;
import com.igormaznitsa.zxpoly.ui.*;
import com.igormaznitsa.zxpoly.utils.Timer;
import com.igormaznitsa.zxpoly.utils.*;
import com.igormaznitsa.zxpspritecorrector.SpriteCorrectorMainFrame;
import com.igormaznitsa.zxpspritecorrector.files.plugins.AbstractFilePlugin;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.SystemUtils;

import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.swing.*;
import javax.swing.Box.Filler;
import javax.swing.JPopupMenu.Separator;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.igormaznitsa.z80.Utils.toHex;
import static com.igormaznitsa.z80.Utils.toHexByte;
import static com.igormaznitsa.zxpoly.utils.Utils.assertUiThread;
import static javax.swing.JOptionPane.showConfirmDialog;
import static javax.swing.JOptionPane.showMessageDialog;
import static javax.swing.KeyStroke.getKeyStroke;
import static org.apache.commons.lang3.StringUtils.repeat;

public final class MainForm extends javax.swing.JFrame implements ActionListener, TapeContext {

  public static final Logger LOGGER = Logger.getLogger("UI");
  public static final Duration TIMER_INT_DELAY_MILLISECONDS = Duration.ofMillis(20);
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
  private static final Icon ICO_SPRITECORRECTOR = new ImageIcon(Utils.loadIcon("spritecorrector.png"));
  private static final String TEXT_START_ANIM_GIF = "Record AGIF";
  private static final String TEXT_STOP_ANIM_GIF = "Stop AGIF";
  private static final long serialVersionUID = 7309959798344327441L;
  private static final String ROM_BOOTSTRAP_FILE_NAME = "bootstrap.rom";
  private static final SclFileFilter FILTER_FORMAT_SCL = new SclFileFilter();
  private static final TrdFileFilter FILTER_FORMAT_TRD = new TrdFileFilter();
  private static final FileFilter FILTER_FORMAT_ALL_DISK = new FileFilter() {
    @Override
    public boolean accept(File f) {
      return FILTER_FORMAT_SCL.accept(f) || FILTER_FORMAT_TRD.accept(f);
    }

    @Override
    public String getDescription() {
      return "All supported disk images(*.scl,*.trd)";
    }
  };
  private static final Snapshot SNAPSHOT_FORMAT_Z80 = new FormatZ80();
  private static final Snapshot SNAPSHOT_FORMAT_SNA = new FormatSNA();
  private static final Snapshot SNAPSHOT_FORMAT_ZXP = new FormatZXP();
  private static final Snapshot SNAPSHOT_FORMAT_ROM = new FormatRom();
  private static final Snapshot SNAPSHOT_FORMAT_SPEC256 = new FormatSpec256();
  private static final FileFilter FILTER_FORMAT_ALL_SNAPSHOTS = new FileFilter() {
    @Override
    public boolean accept(File f) {
      return SNAPSHOT_FORMAT_Z80.accept(f)
              || SNAPSHOT_FORMAT_SPEC256.accept(f)
              || SNAPSHOT_FORMAT_SNA.accept(f)
              || SNAPSHOT_FORMAT_ZXP.accept(f)
              || SNAPSHOT_FORMAT_ROM.accept(f);
    }

    @Override
    public String getDescription() {
      return "All snapshots (*.z80, *.sna, *.zip, *.zxp, *.rom)";
    }
  };
  public static RomData BASE_ROM;
  private final AtomicReference<JFrame> currentFullScreen = new AtomicReference<>();
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
  private final Timer wallClock = new Timer(TIMER_INT_DELAY_MILLISECONDS);
  private final Runnable traceWindowsUpdater = new Runnable() {
    @Override
    public void run() {
      int index = 0;
      for (final TraceCpuForm form : cpuTracers) {
        if (form != null) {
          final Z80 cpu = board.getModules()[index++].getCpu();
          if (cpu.getPrefixInProcessing() == 0) {
            form.refresh();
          }
        }
      }
    }
  };
  private final KeyboardKempstonAndTapeIn keyboardAndTapeModule;
  private final KempstonMouse kempstonMouse;
  private final boolean interlaceScan;
  private final ReentrantLock stepSemaphor = new ReentrantLock();
  private final Thread mainCpuThread;
  private final javax.swing.Timer infoBarUpdateTimer;
  private final AtomicReference<SpriteCorrectorMainFrame> spriteCorrectorMainFrame = new AtomicReference<>();
  private final ImageIcon sysIcon;
  private final TimingProfile timingProfile;
  private volatile long lastFullScreenEventTime = 0L;
  private volatile boolean turboMode = false;
  private volatile boolean zxKeyboardProcessingAllowed = true;
  private AnimatedGifTunePanel.AnimGifOptions lastAnimGifOptions =
          new AnimatedGifTunePanel.AnimGifOptions("./zxpoly.gif", 10, false);
  private File lastTapFolder;
  private File lastFloppyFolder;
  private File lastSnapshotFolder;
  private File lastScreenshotFolder;
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
  private JMenuItem menuViewFullScreen;
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
  private JCheckBoxMenuItem menuOptionsOnlyJoystickEvents;
  private JMenu menuOptionsLookAndFeel;
  private JMenu menuOptionsJoystickSelect;
  private JRadioButtonMenuItem menuOptionsJoystickKempston;
  private JRadioButtonMenuItem menuOptionsJoystickProtek;
  private JCheckBoxMenuItem menuOptionsZX128Mode;
  private JMenu menuService;
  private JMenuItem menuServiceGameControllers;
  private JMenuItem menuServiceSaveScreen;
  private JMenuItem menuServiceSaveScreenAllVRAM;
  private JMenuItem menuServiceMakeSnapshot;
  private JMenuItem menuServiceStartEditor;
  private JMenu menuTap;
  private JMenu menuTapExportAs;
  private JMenuItem menuTapExportAsWav;
  private JMenuItem menuTapGotoBlock;
  private JMenuItem menuTapNextBlock;
  private JMenuItem menuTapThreshold;
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
  private JScrollPane scrollPanel;
  private File lastPokeFileFolder = null;
  private Optional<SourceSoundPort> preTurboSourceSoundPort = Optional.empty();
  private JMenu menuOptionsScaleUi;

  public MainForm(final String title, final String romPath) {
    Runtime.getRuntime().addShutdownHook(new Thread(this::doOnShutdown));

    this.sysIcon = new ImageIcon(Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/sys.png")));

    this.timingProfile = TimingProfile.SPEC128;

    final String ticks = System.getProperty("zxpoly.int.ticks", "");
    int intBetweenFrames = AppOptions.getInstance().getIntBetweenFrames();
    try {
      intBetweenFrames = ticks.isEmpty() ? intBetweenFrames : Integer.parseInt(ticks);
    } catch (NumberFormatException ex) {
      LOGGER.warning("Can't parse ticks: " + ticks);
    }
    expectedIntTicksBetweenFrames = intBetweenFrames;

    LOGGER.log(Level.INFO, "INT ticks between frames: " + expectedIntTicksBetweenFrames);

    byte[] bootstrapRom = null;
    final File bootstrapRomFile = new File(ROM_BOOTSTRAP_FILE_NAME);
    if (bootstrapRomFile.isFile()) {
      LOGGER.info("Detected bootstrap ROM file: " + bootstrapRomFile.getAbsolutePath());
      try {
        bootstrapRom = FileUtils.readFileToByteArray(bootstrapRomFile);
      } catch (IOException ex) {
        LOGGER.log(Level.SEVERE, ex,
                () -> "Can't load bootstrap rom: " + bootstrapRomFile.getAbsolutePath());
        showMessageDialog(this, "Can't load bootstrap rom: " + ex.getMessage());
        System.exit(-1);
      }
    }

    final RomSource rom = RomSource.findForLink(romPath, RomSource.UNKNOWN);
    try {
      BASE_ROM = loadRom(romPath, rom.getRom48names(), rom.getRom128names(), rom.getTrDosNames(), bootstrapRom);
    } catch (Exception ex) {
      showMessageDialog(this, "Can't load Spec128 ROM for error: " + ex.getMessage());
      try {
        BASE_ROM = loadRom(null, rom.getRom48names(), rom.getRom128names(), rom.getTrDosNames(), bootstrapRom);
      } catch (Exception exx) {
        ex.printStackTrace();
        showMessageDialog(this, "Can't load TEST ROM: " + ex.getMessage());
        System.exit(-1);
      }
    }

    initComponents(BASE_ROM.isTrdosPresented());

    this.interlaceScan = AppOptions.getInstance().isInterlacedScan();

    this.menuBar.add(Box.createHorizontalGlue());

    this.setTitle(title);

    this.menuActionAnimatedGIF.setText(TEXT_START_ANIM_GIF);
    this.menuActionAnimatedGIF.setIcon(ICO_AGIF_RECORD);

    this.getInputContext().selectInputMethod(Locale.ENGLISH);

    this.setIconImage(Utils.loadIcon("appico.png"));

    final boolean allowKempstonMouse = AppOptions.getInstance().isKempstonMouseAllowed();

    if (!allowKempstonMouse) {
      this.menuOptionsEnableTrapMouse.setEnabled(false);
      this.menuOptionsEnableTrapMouse.setVisible(false);
    }

    final VirtualKeyboardDecoration vkbdContainer;
    try {
      vkbdContainer = AppOptions.getInstance().getKeyboardLook().load();
      LOGGER.info("Virtual keyboard profile: " + vkbdContainer.getId());
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Can't load virtual keyboard: " + ex.getMessage(), ex);
      throw new Error("Can't load virtual keyboard");
    }

    final VolumeProfile volumeProfile = AppOptions.getInstance().getVolumeProfile();
    LOGGER.info("Selected volume profile: " + volumeProfile.name());

    this.board = new Motherboard(
            volumeProfile,
            this.timingProfile,
            BASE_ROM,
            AppOptions.getInstance().getDefaultBoardMode(),
            AppOptions.getInstance().isContendedRam(),
            AppOptions.getInstance().isSoundChannelsACB(),
            AppOptions.getInstance().isCovoxFb(),
            AppOptions.getInstance().isTurboSound(),
            allowKempstonMouse,
            vkbdContainer
    );
    this.board.reset();
    this.menuOptionsZX128Mode.setSelected(this.board.getBoardMode() != BoardMode.ZXPOLY);
    this.menuOptionsTurbo.setSelected(this.turboMode);

    LOGGER.info("Main form completed");
    this.board.reset();

    this.scrollPanel.getViewport().add(this.board.getVideoController());
    this.keyboardAndTapeModule = this.board.findIoDevice(KeyboardKempstonAndTapeIn.class);
    this.kempstonMouse = this.board.findIoDevice(KempstonMouse.class);

    this.menuOptionsOnlyJoystickEvents.setSelected(this.keyboardAndTapeModule.isOnlyJoystickEvents());
    if (this.keyboardAndTapeModule.isKempstonJoystickActivated()) {
      this.menuOptionsJoystickKempston.setSelected(true);
    } else {
      this.menuOptionsJoystickProtek.setSelected(true);
    }

    this.menuOptionsJoystickKempston.addActionListener(e -> keyboardAndTapeModule.setKempstonJoystickActivated(menuOptionsJoystickKempston.isSelected()));

    this.menuOptionsJoystickProtek.addActionListener(e -> keyboardAndTapeModule.setKempstonJoystickActivated(menuOptionsJoystickKempston.isSelected()));

    final KeyboardFocusManager manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(new KeyboardDispatcher(this));

    final GridBagConstraints cpuIndicatorConstraint = new GridBagConstraints();
    cpuIndicatorConstraint.ipadx = 5;

    this.panelIndicators.add(this.indicatorCpu0, cpuIndicatorConstraint, 0);
    this.panelIndicators.add(this.indicatorCpu1, cpuIndicatorConstraint, 1);
    this.panelIndicators.add(this.indicatorCpu2, cpuIndicatorConstraint, 2);
    this.panelIndicators.add(this.indicatorCpu3, cpuIndicatorConstraint, 3);

    this.menuOptionsEnableTrapMouse
            .setSelected(this.board.getVideoController().isMouseTrapEnabled());

    for (final Component item : this.menuBar.getComponents()) {
      if (item instanceof JMenu) {
        final JMenu menuItem = (JMenu) item;
        menuItem.addMenuListener(new MenuListener() {
          @Override
          public void menuSelected(MenuEvent e) {
            suspendSteps();
            MainForm.this.keyboardAndTapeModule.doReset();
            if (e.getSource() == menuOptions) {
              menuOptionsOnlyJoystickEvents.setState(keyboardAndTapeModule.isOnlyJoystickEvents());
              menuOptionsEnableSpeaker
                      .setEnabled(!turboMode && !menuOptionsEnableVideoStream.isSelected());
              menuOptionsEnableSpeaker.setState(board.getBeeper().isActive());
            }
            menuServiceGameControllers
                    .setEnabled(keyboardAndTapeModule.isControllerEngineAllowed());
          }

          @Override
          public void menuDeselected(MenuEvent e) {
            MainForm.this.resumeSteps();
          }

          @Override
          public void menuCanceled(MenuEvent e) {
            MainForm.this.resumeSteps();
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

    if (AppOptions.getInstance().isSoundTurnedOn()) {
      this.activateSoundIfPossible();
    }

    this.loadFastButtons();

    updateTapeMenu();

    pack();

    this.setLocationRelativeTo(null);

    this.mainCpuThread = new Thread(this::mainLoop, "zx-poly-main-cpu-thread");
    this.mainCpuThread.setPriority(Thread.NORM_PRIORITY);
    this.mainCpuThread.setDaemon(true);
    this.mainCpuThread.setUncaughtExceptionHandler((t, e) -> {
      LOGGER.severe("Detected exception in main thread, stopping application, see logs");
      e.printStackTrace(System.err);
      System.exit(666);
    });

    this.infoBarUpdateTimer =
            new javax.swing.Timer(1000, action -> updateInfoBar());
    this.infoBarUpdateTimer.setRepeats(true);
    this.infoBarUpdateTimer.setInitialDelay(1000);

    SwingUtilities.invokeLater(() -> {
      this.mainCpuThread.start();
      this.infoBarUpdateTimer.start();
    });

    this.board.findIoDevices().forEach(IoDevice::init);

    this.setDropTarget(new DropTarget() {
      @Override
      public void drop(final DropTargetDropEvent e) {
        try {
          e.acceptDrop(DnDConstants.ACTION_COPY | DnDConstants.ACTION_LINK);
          final java.util.List<File> files = (java.util.List<File>) e.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
          e.dropComplete(true);
          LOGGER.info("Got drop for file list: " + files);
          for (final File f : files) {
            if (f.isFile() && f.canRead()) {
              final String extension = FilenameUtils.getExtension(f.getName()).toLowerCase(Locale.ENGLISH);
              switch (extension) {
                case "wav":
                case "tzx":
                case "tap": {
                  LOGGER.info("Activating TAP file: " + f);
                  setTapFile(f);
                }
                break;
                case "rom":
                case "z80":
                case "zip":
                case "sna":
                case "zxp": {
                  LOGGER.info("Activating snapshot file: " + f);
                  setSnapshotFile(f, FILTER_FORMAT_ALL_SNAPSHOTS);
                }
                break;
                case "trd":
                case "scl": {
                  if (board.isBetaDiskPresented()) {
                    LOGGER.info("Activating disk file into drive A: " + f);
                    setDisk(BetaDiscInterface.DRIVE_A, f, FILTER_FORMAT_ALL_DISK);
                  }
                }
                break;
                default: {
                  LOGGER.warning("Ignored dropped file for unknown extension or not file: " + f);
                }
                break;
              }
            }
          }
        } catch (Exception ex) {
          LOGGER.warning("Can't process drop: " + ex.getMessage());
        }
        SwingUtilities.invokeLater(MainForm.this::requestFocus);
      }
    });

    if (AppOptions.getInstance().isOldColorTvOnStart()) {
      this.board.getVideoController().setTvFilterChain(TvFilterChain.OLDTV);
    }

    this.keyboardAndTapeModule.addTapeStateChangeListener(e -> {
      this.setFastButtonState(FastButton.TAPE_PLAY_STOP, e.getTap() != null && e.getTap().isPlaying());
    });
  }

  private static void setMenuEnable(final JMenuItem item, final boolean enable) {
    if (item instanceof JMenu) {
      final JMenu jm = (JMenu) item;
      jm.setEnabled(enable);
      for (int i = 0; i < jm.getItemCount(); i++) {
        setMenuEnable(jm.getItem(i), enable);
      }
    } else {
      if (item != null) {
        item.setEnabled(enable);
      }
    }
  }

  private void loadFastButtons() {
    final List<FastButton> fastButtonsInOptions = AppOptions.getInstance().getFastButtons();
    formFastButtons(this.menuBar,
            Arrays.stream(FastButton.values())
                    .filter(x -> !x.isOptional() || fastButtonsInOptions.contains(x))
                    .collect(Collectors.toList())
    );
  }

  private void formFastButtons(final JMenuBar menuBar, final List<FastButton> fastButtons) {
    Arrays.stream(menuBar.getComponents())
            .filter(c -> FastButton.findForComponentName(c.getName()) != null)
            .collect(Collectors.toList()).forEach(menuBar::remove);

    final JPopupMenu popupMenu = new JPopupMenu("Fast buttons");

    for (final FastButton fb : FastButton.values()) {
      final boolean selected = fastButtons.contains(fb) || !fb.isOptional();
      final JCheckBoxMenuItem menuItem = new JCheckBoxMenuItem(fb.getTitle(), selected);
      menuItem.setEnabled(fb.isOptional());

      if (fb.isOptional()) {
        menuItem.addActionListener(e -> {
          final List<FastButton> newList = new ArrayList<>(fastButtons);
          newList.remove(fb);
          if (((JCheckBoxMenuItem) e.getSource()).isSelected()) {
            newList.add(fb);
          }
          AppOptions.getInstance().setFastButtons(newList);
          loadFastButtons();
        });
      }
      popupMenu.add(menuItem);
    }

    menuBar.setComponentPopupMenu(popupMenu);

    fastButtons.forEach(b -> {
      final AbstractButton abstractButton;
      if (b.getButtonClass().isAssignableFrom(JButton.class)) {
        abstractButton = new JButton();
        abstractButton.setRolloverEnabled(false);
      } else if (b.getButtonClass().isAssignableFrom(JToggleButton.class)) {
        abstractButton = new JToggleButton();
        abstractButton.setRolloverEnabled(false);
      } else {
        throw new Error("Unexpected button class: " + b.getButtonClass());
      }
      abstractButton.setName(b.getComponentName());
      abstractButton.setIcon(b.getIcon());
      abstractButton.setSelectedIcon(b.getIconSelected());
      abstractButton.setToolTipText(b.getToolTip());
      abstractButton.setFocusable(false);

      switch (b) {
        case SOUND_ON_OFF: {
          abstractButton.setSelected(!this.board.getBeeper().isNullBeeper());
          abstractButton.addActionListener(e -> {
            if (((JToggleButton) e.getSource()).isSelected()) {
              if (!this.tryFastSpeakerActivation()) {
                this.setSoundActivate(true);
              }
            } else {
              this.setSoundActivate(false);
            }
          });
        }
        break;
        case RESET: {
          abstractButton.addActionListener(e -> this.makeReset());
        }
        break;
        case TAPE_PLAY_STOP: {
          abstractButton.setSelected(this.keyboardAndTapeModule.getTap().isPlaying());
          abstractButton.addActionListener(e -> {
            final JToggleButton source = (JToggleButton) e.getSource();
            if (source.isSelected()) {
              if (!this.setTapePlay(true)) {
                source.setSelected(false);
              }
            } else {
              this.setTapePlay(false);
            }
          });
        }
        break;
        case TURBO_MODE: {
          abstractButton.setSelected(this.turboMode);
          abstractButton.addActionListener(e -> {
            final JToggleButton source = (JToggleButton) e.getSource();
            this.setTurboMode(source.isSelected());
          });
        }
        break;
        case ZX_KEYBOARD_OFF: {
          abstractButton.setSelected(this.keyboardAndTapeModule.isOnlyJoystickEvents());
          abstractButton.addActionListener(e -> {
            final JToggleButton source = (JToggleButton) e.getSource();
            this.setDisableZxKeyboardEvents(source.isSelected());
          });
        }
        break;
        case START_PAUSE: {
          abstractButton.setSelected(MainForm.this.stepSemaphor.isHeldByCurrentThread());
          abstractButton.addActionListener(e -> {
            final JToggleButton source = (JToggleButton) e.getSource();
            if (source.isSelected()) {
              MainForm.this.stepSemaphor.lock();
            } else {
              MainForm.this.stepSemaphor.unlock();
            }
          });
        }
        break;
        case VIRTUAL_KEYBOARD: {
          abstractButton.setSelected(this.board.getVideoController().isVkbShow());
          abstractButton.addActionListener(e -> {
            final JToggleButton source = (JToggleButton) e.getSource();
            showVirtualKeyboard(source.isSelected());
          });
        }
        break;
        default: {
          throw new Error("Unexpected fast button: " + b);
        }
      }
      menuBar.add(abstractButton);
    });

    menuBar.revalidate();
    menuBar.repaint();
  }

  private void doOnShutdown() {
    this.videoStreamer.stop();
  }

  private Optional<SourceSoundPort> showSelectSoundLineDialog(
          final List<SourceSoundPort> variants, final String previouslySelected,
          final boolean showDialog) {
    assertUiThread();
    final JPanel panel = new JPanel(new FlowLayout(FlowLayout.TRAILING));

    final JComboBox<SourceSoundPort> comboBox =
            new JComboBox<>(variants.toArray(new SourceSoundPort[0]));
    comboBox.addActionListener(x -> comboBox.setToolTipText(comboBox.getSelectedItem().toString()));
    comboBox.setToolTipText(comboBox.getSelectedItem().toString());

    int maxStringLen = 0;
    int index = -1;
    for (int i = 0; i < comboBox.getItemCount(); i++) {
      final String str = comboBox.getItemAt(i).toString();
      if (str.equals(previouslySelected)) {
        index = i;
      }
      maxStringLen = Math.max(maxStringLen, str.length());
    }

    if (showDialog) {

      comboBox.setPrototypeDisplayValue(
              new SourceSoundPort(null, repeat('#', Math.min(40, maxStringLen)), null));

      comboBox.setSelectedIndex(Math.max(0, index));

      panel.add(new JLabel("Sound device:"));
      panel.add(comboBox);
      if (showConfirmDialog(
              this,
              panel,
              "Select sound device",
              JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.PLAIN_MESSAGE
      ) == JOptionPane.OK_OPTION) {
        final SourceSoundPort selected = (SourceSoundPort) comboBox.getSelectedItem();
        return Optional.of(selected);
      } else {
        return Optional.empty();
      }
    } else {
      return index < 0 ? Optional.empty() :
              Optional.of(comboBox.getItemAt(index));
    }
  }

  private Optional<SourceSoundPort> findAudioLine(final AudioFormat audioFormat,
                                                  final boolean interactive) {
    final List<SourceSoundPort> foundPorts = SourceSoundPort.findForFormat(audioFormat);
    LOGGER.info("Detected audio source lines: " + foundPorts);
    if (foundPorts.isEmpty()) {
      if (interactive) {
        showMessageDialog(this, "There is no detected audio devices!",
                "Can't find audio device",
                JOptionPane.WARNING_MESSAGE);
      }
      return Optional.empty();
    } else {
      if (foundPorts.size() == 1) {
        return Optional.of(foundPorts.get(0));
      } else {
        final Optional<SourceSoundPort> result = this.showSelectSoundLineDialog(foundPorts,
                AppOptions.getInstance().getLastSelectedAudioDevice(), interactive);
        if (interactive) {
          result.ifPresent(sourceSoundPort -> AppOptions.getInstance()
                  .setLastSelectedAudioDevice(sourceSoundPort.toString()));
        }
        return result;
      }
    }
  }

  private RomData loadRom(final String romPath, final Set<String> rom48names, final Set<String> rom128names, final Set<String> trdosNames, final byte[] predefinedRomData) throws Exception {
    if (predefinedRomData != null) {
      LOGGER.warning("Provided predefined ROM data, length " + predefinedRomData.length + " bytes");
      final byte[] normalized;
      if (predefinedRomData.length < 0x4000 * 3) {
        LOGGER.warning("Extend predefined ROM binary data to 3 ROM pages");
        normalized = Arrays.copyOf(predefinedRomData, 0x4000 * 3);
      } else if (predefinedRomData.length > 0x4000 * 3) {
        LOGGER.warning("Cutting predefined ROM binary data to 3 ROM pages");
        normalized = Arrays.copyOf(predefinedRomData, 0x4000 * 3);
      } else {
        normalized = predefinedRomData;
      }
      return new RomData("Predefined ROM binary", normalized);
    } else if (romPath != null) {
      if (romPath.contains("://")) {
        try {
          final String cached =
                  "loadedrom_" + Integer.toHexString(romPath.hashCode()).toUpperCase(Locale.ENGLISH)
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
            result = RomLoader.getROMFrom(romPath, rom48names, rom128names, trdosNames);
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
    final TapeSource reader = this.keyboardAndTapeModule.getTap();

    final boolean navigable;
    final boolean sensitivity;
    if (reader == null) {
      this.setFastButtonState(FastButton.TAPE_PLAY_STOP, false);
      this.menuTap.setEnabled(false);
      this.menuTapPlay.setSelected(false);
      this.menuTapExportAs.setEnabled(false);
      navigable = false;
      sensitivity = false;
    } else {
      this.menuTap.setEnabled(true);
      this.menuTapPlay.setSelected(reader.isPlaying());
      this.setFastButtonState(FastButton.TAPE_PLAY_STOP, reader.isPlaying());
      this.menuTapExportAs.setEnabled(reader.canGenerateWav());
      navigable = reader.isNavigable();
      sensitivity = reader.isThresholdAllowed();
    }

    this.menuTapGotoBlock.setEnabled(navigable);
    this.menuTapNextBlock.setEnabled(navigable);
    this.menuTapPrevBlock.setEnabled(navigable);

    this.menuTapThreshold.setEnabled(sensitivity);
  }

  private void mainLoop() {
    this.wallClock.next();
    int countdownToNotifyRepaint = 0;
    int countdownToAnimationSave = 0;

    final int VFLAG_BLINK_BORDER = 1;
    final int VFLAG_BLINK_SCREEN = 2;

    int viFlags = 0;

    long sessionIntCounter = 0;

    while (!Thread.currentThread().isInterrupted()) {
      final int prevViFlags = viFlags;
      boolean notifyRepaintScreen = false;
      if (stepSemaphor.tryLock()) {
        try {
          int frameTiStates = this.board.getFrameTiStates();
          final boolean inTurboMode = this.turboMode;
          final boolean tiStatesForIntExhausted = frameTiStates >= this.timingProfile.ulaFrameTact;
          final boolean intTickForWallClockReached = this.wallClock.completed();

          final boolean doCpuIntTick;
          if (intTickForWallClockReached) {
            if (tiStatesForIntExhausted) {
              sessionIntCounter++;
              doCpuIntTick = true;
              countdownToNotifyRepaint--;
              if (countdownToNotifyRepaint <= 0) {
                countdownToNotifyRepaint = expectedIntTicksBetweenFrames;
                notifyRepaintScreen = true;
              }
              countdownToAnimationSave--;
            } else {
              doCpuIntTick = false;
            }
            this.wallClock.next();
            if (!tiStatesForIntExhausted) {
              this.onSlownessDetected(this.timingProfile.ulaFrameTact - frameTiStates);
            }
            viFlags = 0;
          } else {
            doCpuIntTick = false;
          }

          final boolean executionEnabled = inTurboMode || !tiStatesForIntExhausted || doCpuIntTick;

          final int detectedTriggers = this.board.step(
                  tiStatesForIntExhausted,
                  intTickForWallClockReached,
                  doCpuIntTick,
                  executionEnabled);

          frameTiStates = this.board.getFrameTiStates();

          if (frameTiStates >= this.timingProfile.tstatesInBottomBorderStart) {
            viFlags |= VFLAG_BLINK_SCREEN;
          }

          if (frameTiStates >= this.timingProfile.ulaFrameTact) {
            viFlags |= VFLAG_BLINK_BORDER;
          }

          if (intTickForWallClockReached) {
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

          if (countdownToAnimationSave <= 0) {
            final AnimationEncoder theAnimationEncoder = this.currentAnimationEncoder.get();
            if (theAnimationEncoder == null) {
              countdownToAnimationSave = 0;
            } else {
              countdownToAnimationSave = theAnimationEncoder.getIntsBetweenFrames();
              try {
                theAnimationEncoder
                        .saveFrame(board.getVideoController().makeCopyOfVideoBuffer(true));
              } catch (IOException ex) {
                LOGGER.warning("Can't write animation frame: " + ex.getMessage());
              }
            }
          }
        } finally {
          stepSemaphor.unlock();
        }

        final int changedViFlags = prevViFlags ^ viFlags;

        if ((changedViFlags & VFLAG_BLINK_SCREEN) != 0 && (viFlags & VFLAG_BLINK_SCREEN) != 0) {
          this.blinkScreen(sessionIntCounter);
        }

        if ((changedViFlags & VFLAG_BLINK_BORDER) != 0 && (viFlags & VFLAG_BLINK_BORDER) != 0) {
          this.blinkBorder();
        }

        if (notifyRepaintScreen) {
          this.repaintScreen();
        }
      } else {
        final int frameTiStates = this.board.getFrameTiStates();
        if (this.wallClock.completed()) {
          this.wallClock.next();
          this.videoStreamer.onWallclockInt();
          this.board.dryIntTickOnWallClockTime(frameTiStates >= this.timingProfile.ulaFrameTact, true, frameTiStates);
          this.board.startNewFrame();
        } else {
          if (frameTiStates < this.timingProfile.ulaFrameTact) {
            this.board.doNop();
          }
          this.board.dryIntTickOnWallClockTime(frameTiStates >= this.timingProfile.ulaFrameTact, true, frameTiStates);
        }
      }
      if (this.activeTracerWindowCounter.get() > 0) {
        updateTracerWindowsForStep();
      }
      Thread.onSpinWait();
    }
  }

  private void onSlownessDetected(final long remainTstates) {
    LOGGER.warning(String.format("Slowness detected: %.02f%%",
            (float) remainTstates / (float) this.timingProfile.ulaFrameTact * 100.0f));
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
        showMessageDialog(MainForm.this, "Detected desync of module CPUs\n"
                        + makeInfoStringForRegister(cpuModuleStates, lastM1Address, null, Z80.REG_PC, false),
                "Triggered", JOptionPane.INFORMATION_MESSAGE);
      }

      if ((triggered & Motherboard.TRIGGER_DIFF_MEM_ADDR) != 0) {
        this.menuTriggerDiffMem.setSelected(false);
        showMessageDialog(MainForm.this,
                "Detected memory cell difference " + toHex(this.board.getMemTriggerAddress()) + "\n"
                        + makeInfoStringForRegister(cpuModuleStates, lastM1Address,
                        getCellContentForAddress(this.board.getMemTriggerAddress()), Z80.REG_PC, false),
                "Triggered", JOptionPane.INFORMATION_MESSAGE);
      }

      if ((triggered & Motherboard.TRIGGER_DIFF_EXE_CODE) != 0) {
        this.menuTriggerExeCodeDiff.setSelected(false);
        showMessageDialog(MainForm.this, "Detected EXE code difference\n"
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
    this.setFastButtonState(FastButton.TURBO_MODE, value);
    this.turboMode = value;
    LOGGER.info("Turbo-mode: " + value);
  }

  private void blinkScreen(final long sessionIntCounter) {
    if (this.interlaceScan) {
      this.board.getVideoController().syncUpdateBuffer((sessionIntCounter & 1) == 0 ? VideoController.LineRenderMode.EVEN : VideoController.LineRenderMode.ODD);
    } else {
      this.board.getVideoController().syncUpdateBuffer(VideoController.LineRenderMode.ALL);
    }
  }

  private void blinkBorder() {
    this.board.getVideoController().blinkBorder();
  }

  private void repaintScreen() {
    board.getVideoController().notifyRepaint();
  }

  private void menuOptionsEnableVideoStreamActionPerformed(final ActionEvent actionEvent) {
    this.suspendSteps();
    try {
      if (this.menuOptionsEnableVideoStream.isSelected()) {
        if (AppOptions.getInstance().isGrabSound()
                && !this.board.getBeeper().isNullBeeper()
                && showConfirmDialog(this,
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
          showMessageDialog(this, ex.getMessage(), "Error",
                  JOptionPane.ERROR_MESSAGE);
          this.menuOptionsEnableVideoStream.setSelected(false);
        }
      } else {
        this.videoStreamer.stop();
      }
    } finally {
      this.resumeSteps();
    }
  }

  private boolean tryFastSpeakerActivation() {
    if (this.board.getBeeper().isNullBeeper()) {
      final Optional<SourceSoundPort> port =
              this.findAudioLine(this.board.getBeeper().getAudioFormat(), false);
      port.ifPresent(sourceSoundPort -> this.board.getBeeper().setSourceSoundPort(sourceSoundPort));
      return !this.board.getBeeper().isNullBeeper();
    } else {
      return true;
    }
  }

  private void activateSoundIfPossible() {
    final boolean activated = tryFastSpeakerActivation();
    this.menuOptionsEnableSpeaker.setSelected(activated);
    this.setFastButtonState(FastButton.SOUND_ON_OFF, activated);
  }

  private void setSoundActivate(final boolean activate) {
    boolean activated = false;
    this.suspendSteps();
    try {
      if (activate) {
        final Optional<SourceSoundPort> port =
                this.findAudioLine(this.board.getBeeper().getAudioFormat(), true);
        if (port.isPresent()) {
          this.board.getBeeper().setSourceSoundPort(port.get());
          if (!this.board.getBeeper().isNullBeeper()) {
            activated = true;
          }
        }
      } else {
        this.board.getBeeper().setSourceSoundPort(null);
      }
    } finally {
      this.resumeSteps();
      this.menuOptionsEnableSpeaker.setSelected(activated);
      this.setFastButtonState(FastButton.SOUND_ON_OFF, activated);
    }
  }

  private void menuOptionsEnableSpeakerActionPerformed(final ActionEvent actionEvent) {
    this.setSoundActivate(this.menuOptionsEnableSpeaker.isSelected());
  }

  private void menuServiceGameControllerActionPerformed(final ActionEvent actionEvent) {
    this.suspendSteps();
    try {
      if (!this.keyboardAndTapeModule.isControllerEngineAllowed()) {
        showMessageDialog(this, "Can't init game controller engine!", "Error",
                JOptionPane.ERROR_MESSAGE);
      } else if (this.keyboardAndTapeModule.getDetectedControllers().isEmpty()) {
        showMessageDialog(this,
                "Can't find any game controller. Try restart the emulator if controller already connected.",
                "Can't find game controllers", JOptionPane.WARNING_MESSAGE);
      } else {
        final GameControllerPanel gameControllerPanel =
                new GameControllerPanel(this.keyboardAndTapeModule);
        if (showConfirmDialog(
                this,
                gameControllerPanel,
                "Detected game controllers",
                JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE
        ) == JOptionPane.OK_OPTION) {
          this.keyboardAndTapeModule.setActiveGameControllerAdapters(gameControllerPanel.getSelected());
        }
      }
    } finally {
      this.resumeSteps();
    }
  }

  private void makeReset() {
    this.board
            .setBoardMode(this.menuOptionsZX128Mode.isSelected() ? BoardMode.ZX128 : BoardMode.ZXPOLY,
                    false);
    this.board.resetAndRestoreRom(BASE_ROM);
  }

  private void menuFileResetActionPerformed(final ActionEvent evt) {
    this.makeReset();
  }

  private void menuOptionsShowIndicatorsActionPerformed(ActionEvent evt) {
    this.indicatorCpu0.clear();
    this.indicatorCpu1.clear();
    this.indicatorCpu2.clear();
    this.indicatorCpu3.clear();
    this.panelIndicators.setVisible(this.menuOptionsShowIndicators.isSelected());
  }

  private void setDisk(final int drive, File selectedFile, FileFilter filter) {
    this.stepSemaphor.lock();
    try {
      this.lastFloppyFolder = selectedFile.getParentFile();
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

      try {
        if (filter == FILTER_FORMAT_ALL_DISK) {
          if (FILTER_FORMAT_TRD.accept(selectedFile)) {
            filter = FILTER_FORMAT_TRD;
          } else {
            filter = FILTER_FORMAT_SCL;
          }
        }

        if (!selectedFile.isFile() && filter.getClass() == TrdFileFilter.class) {
          String name = selectedFile.getName();
          if (!name.contains(".")) {
            name += ".trd";
          }
          selectedFile = new File(selectedFile.getParentFile(), name);
          if (!selectedFile.isFile()) {
            if (showConfirmDialog(this, "Create TRD file: " + selectedFile.getName() + "?",
                    "Create TRD file", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
              LOGGER.log(Level.INFO, "Creating TRD disk: " + selectedFile.getAbsolutePath());
              FileUtils.writeByteArrayToFile(selectedFile, new TrDosDisk().getDiskData());
            } else {
              return;
            }
          }
        }

        final TrDosDisk floppy = new TrDosDisk(selectedFile,
                filter.getClass() == SclFileFilter.class ? TrDosDisk.SourceDataType.SCL :
                        TrDosDisk.SourceDataType.TRD, FileUtils.readFileToByteArray(selectedFile), false);
        this.board.getBetaDiskInterface().insertDiskIntoDrive(drive, floppy);
        LOGGER.log(Level.INFO,
                "Loaded drive " + diskName + " by floppy image file " + selectedFile);
      } catch (IOException ex) {
        LOGGER.log(Level.WARNING, "Can't read Floppy image file [" + selectedFile + ']', ex);
        showMessageDialog(this, "Can't read Floppy image file", "Error",
                JOptionPane.ERROR_MESSAGE);
      }
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  private void loadDiskIntoDrive(final int drive) {
    this.suspendSteps();
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

      File selectedFile =
              chooseFileForOpen("Select Disk " + diskName, this.lastFloppyFolder, filter,
                      FILTER_FORMAT_ALL_DISK, FILTER_FORMAT_SCL, FILTER_FORMAT_TRD);

      if (selectedFile != null) {
        this.setDisk(drive, selectedFile, filter.get());
      }
    } finally {
      this.resumeSteps();
    }
  }

  private void menuFileSelectDiskAActionPerformed(ActionEvent evt) {
    loadDiskIntoDrive(BetaDiscInterface.DRIVE_A);
  }

  private void formWindowLostFocus(WindowEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.keyboardAndTapeModule.doReset();
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  private void formWindowGainedFocus(WindowEvent evt) {
    this.stepSemaphor.lock();
    try {
      this.getInputContext().selectInputMethod(Locale.ENGLISH);
      this.keyboardAndTapeModule.doReset();
    } finally {
      this.stepSemaphor.unlock();
    }
  }

  private void menuFileLoadSnapshotActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
      if (AppOptions.getInstance().isTestRomActive()) {
        final JHtmlLabel label = new JHtmlLabel(
                "<html><body>ZX-Spectrum 128 ROM is required to load snapshots.<br>Go to menu <b><i><a href=\"rom\">File->Options</i></b></i> and choose ROM 128.</body></html>");
        label.addLinkListener((source, link) -> {
          if ("rom".equals(link)) {
            SwingUtilities.windowForComponent(source).setVisible(false);
            SwingUtilities.invokeLater(() -> menuFileOptions.doClick());
          }
        });
        showMessageDialog(MainForm.this, label, "ZX-Spectrum ROM 128 image is required",
                JOptionPane.WARNING_MESSAGE);
        return;
      }

      final AtomicReference<FileFilter> theFilter = new AtomicReference<>();
      final File selected =
              chooseFileForOpen("Select snapshot", this.lastSnapshotFolder, theFilter, FILTER_FORMAT_ALL_SNAPSHOTS,
                      SNAPSHOT_FORMAT_Z80, SNAPSHOT_FORMAT_SPEC256, SNAPSHOT_FORMAT_SNA, SNAPSHOT_FORMAT_ZXP, SNAPSHOT_FORMAT_ROM);

      if (selected != null) {
        this.setSnapshotFile(selected, theFilter.get());
      }
    } finally {
      this.resumeSteps();
    }
  }

  private void setSnapshotFile(final File selected, FileFilter theFilter) {
    this.stepSemaphor.lock();
    try {
      this.board.forceResetAllCpu();
      this.board.resetIoDevices();

      this.lastSnapshotFolder = selected.getParentFile();
      try {
        if (theFilter == FILTER_FORMAT_ALL_SNAPSHOTS) {
          if (SNAPSHOT_FORMAT_ROM.accept(selected)) {
            theFilter = SNAPSHOT_FORMAT_ROM;
          } else if (SNAPSHOT_FORMAT_Z80.accept(selected)) {
            theFilter = SNAPSHOT_FORMAT_Z80;
          } else if (SNAPSHOT_FORMAT_SNA.accept(selected)) {
            theFilter = SNAPSHOT_FORMAT_SNA;
          } else if (SNAPSHOT_FORMAT_ZXP.accept(selected)) {
            theFilter = SNAPSHOT_FORMAT_ZXP;
          } else {
            theFilter = SNAPSHOT_FORMAT_SPEC256;
          }
        }

        final Snapshot selectedFilter = (Snapshot) theFilter;
        LOGGER.log(Level.INFO, "Loading snapshot " + selectedFilter.getName());
        selectedFilter.loadFromArray(selected, this.board, this.board.getVideoController(),
                FileUtils.readFileToByteArray(selected));
        this.menuOptionsZX128Mode.setState(this.board.getBoardMode() != BoardMode.ZXPOLY);
      } catch (Exception ex) {
        ex.printStackTrace();
        LOGGER.log(Level.WARNING, "Can't read snapshot file [" + ex.getMessage() + ']', ex);
        showMessageDialog(this, "Can't read snapshot file [" + ex.getMessage() + ']',
                "Error", JOptionPane.ERROR_MESSAGE);
      }
    } finally {
      stepSemaphor.unlock();
    }
  }

  private void updateInfoBar() {
    assertUiThread();
    if (panelIndicators.isVisible()) {
      labelTurbo.setStatus(turboMode);

      final TapeSource tapeFileReader = keyboardAndTapeModule.getTap();
      labelTapeUsage.setStatus(tapeFileReader != null && tapeFileReader.isPlaying());
      labelMouseUsage.setStatus(board.getVideoController().isMouseTrapActive());
      labelDiskUsage.setStatus(board.isBetaDiskPresented() && board.getBetaDiskInterface().isActive());
      labelZX128.setStatus(board.getBoardMode() != BoardMode.ZXPOLY);

      indicatorCpu0.updateForState(board.getCpuActivity(0));
      indicatorCpu1.updateForState(board.getCpuActivity(1));
      indicatorCpu2.updateForState(board.getCpuActivity(2));
      indicatorCpu3.updateForState(board.getCpuActivity(3));
    }
    updateTracerCheckBoxes();
  }

  private void setMenuEnable(final boolean flag) {
    for (int i = 0; i < this.getJMenuBar().getMenuCount(); i++) {
      setMenuEnable(this.getJMenuBar().getMenu(i), flag);
    }
  }

  private void setFastButtonState(final FastButton fastButton, final boolean select) {
    Component component = null;
    for (final Component c : this.menuBar.getComponents()) {
      if (c.getName() != null && c.getName().equals(fastButton.getComponentName())) {
        component = c;
        break;
      }
    }

    if (component != null) {
      if (fastButton.getButtonClass().isAssignableFrom(JToggleButton.class)) {
        ((JToggleButton) component).setSelected(select);
      } else if (fastButton.getButtonClass().isAssignableFrom(JButton.class) && select) {
        ((JButton) component).doClick();
      }
    }
  }

  private void doFullScreen() {
    try {
      if (System.currentTimeMillis() - this.lastFullScreenEventTime > 1000L) {
        final GraphicsDevice gDevice = this.getGraphicsConfiguration().getDevice();
        LOGGER.info("FULL SCREEN called, device=" + gDevice.getIDstring() + " displayMode="
                + gDevice.getDisplayMode());

        JFrame lastFullScreen = this.currentFullScreen.getAndSet(null);

        if (lastFullScreen == null) {
          if (!gDevice.isFullScreenSupported()) {
            menuViewFullScreen.setEnabled(false);
            LOGGER.warning("Device doesn't support full screen: " + gDevice.getIDstring());
            return;
          }

          final VideoController vc = this.board.getVideoController();
          this.scrollPanel.getViewport().remove(vc);

          lastFullScreen = new JFrame("ZX-Poly FullScreen", gDevice.getDefaultConfiguration());
          lastFullScreen.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
          lastFullScreen.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
              vc.requestFocus();
              MainForm.this.formWindowGainedFocus(e);
            }

            @Override
            public void windowLostFocus(WindowEvent e) {
              MainForm.this.formWindowLostFocus(e);
            }
          });

          lastFullScreen.getContentPane().add(vc, BorderLayout.CENTER);
          lastFullScreen.setUndecorated(true);
          lastFullScreen.setResizable(false);

          this.currentFullScreen.set(lastFullScreen);

          this.setMenuEnable(false);
          this.setVisible(false);

          final boolean mouseTrapOptionActive = this.menuOptionsEnableTrapMouse.isSelected();

          vc.setEnableTrapMouse(mouseTrapOptionActive, false, mouseTrapOptionActive);
          vc.setFullScreenMode(true);

          gDevice.setFullScreenWindow(lastFullScreen);

          lastFullScreen.revalidate();
          lastFullScreen.doLayout();
          vc.zoomForSize(this.scrollPanel.getViewportBorderBounds());
          SwingUtilities.invokeLater(() -> {
            this.doVcSize();
            vc.setVkbShow(false);
            vc.zoomForSize(gDevice.getDefaultConfiguration().getBounds());
            setFastButtonState(FastButton.VIRTUAL_KEYBOARD, false);
          });
        } else {
          lastFullScreen.getContentPane().removeAll();
          lastFullScreen.dispose();

          final VideoController vc = this.board.getVideoController();

          final boolean mouseTrapOptionActive = this.menuOptionsEnableTrapMouse.isSelected();
          vc.setEnableTrapMouse(mouseTrapOptionActive, true, false);

          this.scrollPanel.getViewport().setView(vc);
          this.doVcSize();
          vc.setFullScreenMode(false);

          this.scrollPanel.revalidate();

          this.setMenuEnable(true);
          this.updateInfoBar();
          this.updateTapeMenu();
          this.updateTracerCheckBoxes();
          this.setVisible(true);
          this.pack();
          this.repaint();

          SwingUtilities.invokeLater(() -> {
            this.doVcSize();
            vc.setVkbShow(false);
            setFastButtonState(FastButton.VIRTUAL_KEYBOARD, false);
          });
        }
      } else {
        LOGGER.info("Ignoring FULL SCREEN because too often");
      }
    } finally {
      this.lastFullScreenEventTime = System.currentTimeMillis();
    }
  }

  private void doVcSize() {
    this.board.getVideoController().zoomForSize(this.scrollPanel.getBounds());
  }

  private void initComponents(final boolean trdosEnabled) {
    GridBagConstraints gridBagConstraints;

    scrollPanel = new JScrollPane();
    jSeparator2 = new JSeparator();
    panelIndicators = new javax.swing.JPanel();
    filler1 = new Filler(new Dimension(0, 0), new Dimension(0, 0),
            new Dimension(32767, 0));
    labelTurbo =
            new JIndicatorLabel(ICO_TURBO, ICO_TURBO_DIS, "Turbo-mode is ON", "Turbo-mode is OFF");
    labelMouseUsage =
            new JIndicatorLabel(ICO_MOUSE, ICO_MOUSE_DIS, "Mouse is caught", "Mouse is not active");
    labelZX128 = new JIndicatorLabel(ICO_ZX128, ICO_ZX128_DIS, "ZX mode is ON", "ZX mode is OFF");
    labelTapeUsage =
            new JIndicatorLabel(ICO_TAPE, ICO_TAPE_DIS, "Reading", "None");
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
    menuViewFullScreen = new JMenuItem();
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
    menuTapThreshold = new JMenuItem();
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
    menuOptionsOnlyJoystickEvents = new JCheckBoxMenuItem();
    menuOptionsJoystickSelect = new JMenu();
    menuOptionsLookAndFeel = new JMenu();
    menuOptionsScaleUi = new JMenu();
    menuOptionsJoystickKempston = new JRadioButtonMenuItem();
    menuOptionsJoystickProtek = new JRadioButtonMenuItem();
    menuOptionsEnableTrapMouse = new JCheckBoxMenuItem();
    menuOptionsEnableSpeaker = new JCheckBoxMenuItem();
    menuOptionsEnableVideoStream = new JCheckBoxMenuItem();
    menuHelp = new JMenu();
    menuHelpAbout = new JMenuItem();
    menuHelpDonation = new JMenuItem();

    setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    setLocationByPlatform(true);

    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(ComponentEvent e) {
        menuBar.repaint();
      }
    });

    this.addWindowFocusListener(new WindowFocusListener() {
      public void windowGainedFocus(WindowEvent evt) {
        formWindowGainedFocus(evt);
      }

      public void windowLostFocus(WindowEvent evt) {
        formWindowLostFocus(evt);
      }
    });

    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentMoved(ComponentEvent e) {
        if (MainForm.this.currentFullScreen.get() == null) {
          menuViewFullScreen.setEnabled(
                  MainForm.this.getGraphicsConfiguration().getDevice().isFullScreenSupported());
        }
      }
    });

    this.addWindowListener(new WindowAdapter() {
      @Override
      public void windowActivated(WindowEvent e) {
        final Window virtualKeyboard = board.getVideoController().getVirtualKeboardWindow();
        if (virtualKeyboard != null) {
          virtualKeyboard.toFront();
        }
      }

      public void windowClosed(WindowEvent evt) {
        formWindowClosed(evt);
      }

      public void windowClosing(WindowEvent evt) {
        formWindowClosing(evt);
      }
    });

    this.addComponentListener(new ComponentAdapter() {
      @Override
      public void componentResized(final ComponentEvent e) {
        if (MainForm.this.getState() == MAXIMIZED_BOTH || MainForm.this.getState() == NORMAL) {
          final Rectangle rectangle = e.getComponent().getBounds();
          doVcSize();
          MainForm.this.scrollPanel.revalidate();
          MainForm.this.repaint();
        }
      }
    });

    this.menuServiceStartEditor = new JMenuItem("ZX-Sprite corrector", ICO_SPRITECORRECTOR);
    this.menuServiceStartEditor.addActionListener(e -> {
      this.suspendSteps();
      try {
        SpriteCorrectorMainFrame spriteCorrector = this.spriteCorrectorMainFrame.get();
        if (spriteCorrector != null && spriteCorrector.isDisplayable()) {
          spriteCorrector.toFront();
          spriteCorrector.requestFocus();
        } else {
          if (spriteCorrector != null) {
            spriteCorrector.dispose();
          }
          spriteCorrector = new SpriteCorrectorMainFrame(this.getGraphicsConfiguration(), false);
          spriteCorrector.setVisible(true);
          spriteCorrector.setLocation(this.getLocation());
          spriteCorrector.toFront();
          spriteCorrector.requestFocus();
          this.spriteCorrectorMainFrame.set(spriteCorrector);

          try {
            final byte[] data = new FormatZ80().saveToArray(this.board, this.board.getVideoController());
            final Optional<AbstractFilePlugin> plugin = spriteCorrector.findImportFilePlugin("z80");
            if (data != null && plugin.isPresent()) {
              spriteCorrector.loadFileWithPlugin(plugin.get(), null, "emulator-data", data, -1);
              spriteCorrector.updateAndResetEditMenu();
            }
          } catch (IOException ex) {
            LOGGER.severe("Error during snapshot creation: " + ex.getMessage());
          }
        }
      } finally {
        this.resumeSteps();
      }
    });

    this.getContentPane().add(scrollPanel, BorderLayout.CENTER);

    panelIndicators.setBorder(BorderFactory.createEtchedBorder());
    panelIndicators.setLayout(new GridBagLayout());
    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 4;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridBagConstraints.weightx = 1000.0;
    panelIndicators.add(filler1, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 5;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelTurbo, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 6;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelMouseUsage, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 7;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelZX128, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 8;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelTapeUsage, gridBagConstraints);

    gridBagConstraints = new GridBagConstraints();
    gridBagConstraints.gridx = 9;
    gridBagConstraints.gridy = 0;
    panelIndicators.add(labelDiskUsage, gridBagConstraints);

    getContentPane().add(panelIndicators, BorderLayout.SOUTH);

    menuFile.setText("File");
    menuFile.addMenuListener(new MenuListener() {
      public void menuCanceled(MenuEvent evt) {
      }

      public void menuDeselected(MenuEvent evt) {
      }

      public void menuSelected(MenuEvent evt) {
        menuFileMenuSelected(evt);
      }
    });

    menuView.setText("View");

    menuViewFullScreen.setText("Full Screen");
    menuViewFullScreen.addActionListener(e -> this.doFullScreen());
    menuViewFullScreen
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_F11, SystemUtils.IS_OS_MAC ?
                    InputEvent.CTRL_DOWN_MASK | Toolkit.getDefaultToolkit().getMenuShortcutKeyMask() : 0));


    menuView.add(menuViewFullScreen);

    menuViewZoomIn.setText("Zoom In");
    menuViewZoomIn.addActionListener(e -> this.board.getVideoController().zoomIn());
    menuViewZoomIn
            .setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_EQUALS,
                    Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    menuViewZoomOut.setText("Zoom Out");
    menuViewZoomOut.addActionListener(e -> this.board.getVideoController().zoomOut());
    menuViewZoomOut
            .setAccelerator(KeyStroke
                    .getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    menuViewZoom.setText("Zoom");

    menuViewZoom.add(menuViewZoomIn);
    menuViewZoom.add(menuViewZoomOut);

    menuView.add(menuViewZoom);

    menuViewVideoFilter.setText("Video filter");
    final ButtonGroup tvFilterGroup = new ButtonGroup();

    final boolean oldTvFilterActivating = AppOptions.getInstance().isOldColorTvOnStart();

    for (final TvFilterChain chain : TvFilterChain.values()) {
      final JRadioButtonMenuItem tvFilterMenuItem =
              new JRadioButtonMenuItem(chain.getText(), oldTvFilterActivating ? chain == TvFilterChain.OLDTV : chain == TvFilterChain.NONE);
      tvFilterGroup.add(tvFilterMenuItem);
      tvFilterMenuItem.addActionListener(e -> {
        this.menuActionAnimatedGIF.setEnabled(chain.isGifCompatible());
        if (tvFilterMenuItem.isSelected()) {
          this.board.getVideoController().setTvFilterChain(chain);
        }
      });
      menuViewVideoFilter.add(tvFilterMenuItem);
    }

    menuView.add(menuViewVideoFilter);

    menuFileLoadSnapshot.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/snapshot.png")))); // NOI18N
    menuFileLoadSnapshot.setText("Load Snapshot");
    menuFileLoadSnapshot.addActionListener(this::menuFileLoadSnapshotActionPerformed);
    menuFile.add(menuFileLoadSnapshot);

    menuFileLoadPoke.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/poke.png")))); // NOI18N
    menuFileLoadPoke.setText("Load Poke");
    menuFileLoadPoke.addActionListener(this::menuFileLoadPokeActionPerformed);
    menuFile.add(menuFileLoadPoke);

    menuFileLoadTap.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/cassette.png")))); // NOI18N
    menuFileLoadTap.setText("Load TAPE");
    menuFileLoadTap.addActionListener(this::menuFileLoadTapActionPerformed);
    menuFile.add(menuFileLoadTap);

    menuLoadDrive.setIcon(
            new ImageIcon(Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/disk.png")))); // NOI18N
    menuLoadDrive.setText("Load Disk..");
    menuLoadDrive.addMenuListener(new MenuListener() {
      public void menuCanceled(MenuEvent evt) {
      }

      public void menuDeselected(MenuEvent evt) {
      }

      public void menuSelected(MenuEvent evt) {
        menuLoadDriveMenuSelected(evt);
      }
    });

    if (trdosEnabled) {
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
              Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/diskflush.png")))); // NOI18N
      menuFileFlushDiskChanges.setText("Flush disk changes");
      menuFileFlushDiskChanges.addActionListener(this::menuFileFlushDiskChangesActionPerformed);
      menuFile.add(menuFileFlushDiskChanges);
    }

    menuFile.add(jSeparator1);

    menuFileOptions.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/settings.png")))); // NOI18N
    menuFileOptions.setText("Preferences");
    menuFileOptions.addActionListener(this::menuFileOptionsActionPerformed);
    menuFile.add(menuFileOptions);
    menuFile.add(jSeparator3);

    menuFileExit.setAccelerator(
            getKeyStroke(KeyEvent.VK_F4, InputEvent.ALT_MASK));
    menuFileExit.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/reset.png")))); // NOI18N
    menuFileExit.setText("Exit");
    menuFileExit.addActionListener(this::menuFileExitActionPerformed);
    menuFile.add(menuFileExit);


    menuBar.add(menuFile);

    menuTap.setText("Tape");

    menuTapeRewindToStart.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_previous.png")))); // NOI18N
    menuTapeRewindToStart.setText("Rewind to start");
    menuTapeRewindToStart.addActionListener(this::menuTapeRewindToStartActionPerformed);
    menuTap.add(menuTapeRewindToStart);

    menuTapPrevBlock.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_backward.png")))); // NOI18N
    menuTapPrevBlock.setText("Prev block");
    menuTapPrevBlock.addActionListener(this::menuTapPrevBlockActionPerformed);
    menuTap.add(menuTapPrevBlock);

    menuTapPlay.setAccelerator(getKeyStroke(KeyEvent.VK_F4, 0));
    menuTapPlay.setText("Play");
    menuTapPlay.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_play.png")))); // NOI18N
    menuTapPlay.setInheritsPopupMenu(true);
    menuTapPlay.addActionListener(this::menuTapPlayActionPerformed);
    menuTap.add(menuTapPlay);

    menuTapNextBlock.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_forward.png")))); // NOI18N
    menuTapNextBlock.setText("Next block");
    menuTapNextBlock.addActionListener(this::menuTapNextBlockActionPerformed);
    menuTap.add(menuTapNextBlock);

    menuTapGotoBlock.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_pos.png")))); // NOI18N
    menuTapGotoBlock.setText("Go to block");
    menuTapGotoBlock.addActionListener(this::menuTapGotoBlockActionPerformed);
    menuTap.add(menuTapGotoBlock);

    menuTapThreshold.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_sens.png"))));
    menuTapThreshold.setText("Signal threshold");
    menuTapThreshold.addActionListener(this::menuTapThresholdActionPerformed);
    menuTap.add(menuTapThreshold);

    menuBar.add(menuTap);
    menuBar.add(menuView);

    menuService.setText("Service");

    menuFileReset.setAccelerator(getKeyStroke(KeyEvent.VK_F12, 0));
    menuFileReset.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/reset2.png")))); // NOI18N
    menuFileReset.setText("Reset");
    menuFileReset.addActionListener(this::menuFileResetActionPerformed);
    menuService.add(menuFileReset);

    menuServiceSaveScreen.setAccelerator(getKeyStroke(KeyEvent.VK_F8, 0));
    menuServiceSaveScreen.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/photo.png")))); // NOI18N
    menuServiceSaveScreen.setText("Make Screenshot");
    menuServiceSaveScreen.addActionListener(this::menuServiceSaveScreenActionPerformed);
    menuService.add(menuServiceSaveScreen);

    menuServiceSaveScreenAllVRAM.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/photom.png")))); // NOI18N
    menuServiceSaveScreenAllVRAM.setText("Make Screenshot of all VRAM");
    menuServiceSaveScreenAllVRAM
            .addActionListener(this::menuServiceSaveScreenAllVRAMActionPerformed);
    menuService.add(menuServiceSaveScreenAllVRAM);

    menuActionAnimatedGIF.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/file_gif.png")))); // NOI18N
    menuActionAnimatedGIF.setText("Make Animated GIF");
    menuActionAnimatedGIF.addActionListener(this::menuActionAnimatedGIFActionPerformed);
    menuService.add(menuActionAnimatedGIF);

    menuServiceMakeSnapshot.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/save_snapshot.png")))); // NOI18N
    menuServiceMakeSnapshot.setText("Save snapshot");
    menuServiceMakeSnapshot.addActionListener(this::menuServiceMakeSnapshotActionPerformed);
    menuService.add(menuServiceMakeSnapshot);

    menuTapExportAs.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/tape_record.png")))); // NOI18N
    menuTapExportAs.setText("Export TAPE as..");

    menuTapExportAsWav.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/file_wav.png")))); // NOI18N
    menuTapExportAsWav.setText("WAV file");
    menuTapExportAsWav.addActionListener(this::menuTapExportAsWavActionPerformed);
    menuTapExportAs.add(menuTapExportAsWav);

    menuService.add(menuTapExportAs);

    menuServiceGameControllers.setText("Game controllers");
    menuServiceGameControllers.setToolTipText("Turn on game controller");
    menuServiceGameControllers.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/gcontroller.png")))); // NOI18N
    menuServiceGameControllers.addActionListener(this::menuServiceGameControllerActionPerformed);

    menuService.add(menuServiceGameControllers);
    this.menuService.add(this.menuServiceStartEditor);

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

    menuOptionsJoystickSelect.setText("Joystick");
    menuOptionsJoystickSelect.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/protek.png")))); // NOI18N);

    menuOptionsJoystickSelect.setToolTipText("Select active joystick type");
    menuOptionsJoystickKempston.setText("Kempston");
    menuOptionsJoystickProtek.setText("Protek");

    menuOptionsJoystickSelect.add(menuOptionsJoystickKempston);
    menuOptionsJoystickSelect.add(menuOptionsJoystickProtek);

    final ButtonGroup joystickButtonGroup = new ButtonGroup();
    joystickButtonGroup.add(menuOptionsJoystickKempston);
    joystickButtonGroup.add(menuOptionsJoystickProtek);

    menuOptions.add(menuOptionsJoystickSelect);

    menuOptionsOnlyJoystickEvents.setAccelerator(getKeyStroke(KeyEvent.VK_F6, 0));
    menuOptionsOnlyJoystickEvents.setText("ZX-Keyboard Off");
    menuOptionsOnlyJoystickEvents.setToolTipText("Disable events from keyboard and allow events only from joystick");
    menuOptionsOnlyJoystickEvents.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/onlykempston.png")))); // NOI18N
    menuOptionsOnlyJoystickEvents.addActionListener(this::menuOptionsOnlyKempstonEvents);
    menuOptions.add(menuOptionsOnlyJoystickEvents);

    menuOptionsShowIndicators.setSelected(true);
    menuOptionsShowIndicators.setText("Indicator panel");
    menuOptionsShowIndicators.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/indicator.png")))); // NOI18N
    menuOptionsShowIndicators.addActionListener(this::menuOptionsShowIndicatorsActionPerformed);
    menuOptions.add(menuOptionsShowIndicators);

    menuOptionsZX128Mode.setSelected(true);
    menuOptionsZX128Mode.setText("ZX Mode");
    menuOptionsZX128Mode.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/zx128.png")))); // NOI18N
    menuOptionsZX128Mode.addActionListener(this::menuOptionsZX128ModeActionPerformed);
    menuOptions.add(menuOptionsZX128Mode);

    menuOptionsTurbo.setAccelerator(getKeyStroke(KeyEvent.VK_F3, 0));
    menuOptionsTurbo.setText("Turbo");
    menuOptionsTurbo.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/turbo.png")))); // NOI18N
    menuOptionsTurbo.addActionListener(this::menuOptionsTurboActionPerformed);
    menuOptions.add(menuOptionsTurbo);

    menuOptionsEnableTrapMouse.setText("Trap mouse");
    menuOptionsEnableTrapMouse.setToolTipText("Trap mouse as Kempston-mouse");
    menuOptionsEnableTrapMouse.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/pointer.png")))); // NOI18N
    menuOptionsEnableTrapMouse.addActionListener(this::menuOptionsEnableTrapMouseActionPerformed);
    menuOptions.add(menuOptionsEnableTrapMouse);

    menuOptionsEnableSpeaker.setText("Sound");
    menuOptionsEnableSpeaker.setToolTipText("Turn on beeper sound");
    menuOptionsEnableSpeaker.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/speaker.png")))); // NOI18N
    menuOptionsEnableSpeaker.addActionListener(this::menuOptionsEnableSpeakerActionPerformed);
    menuOptions.add(menuOptionsEnableSpeaker);

    menuOptionsEnableVideoStream.setText("Video stream (beta)");
    menuOptionsEnableVideoStream.setToolTipText("Turn on video streaming");
    menuOptionsEnableVideoStream.setIcon(new ImageIcon(
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/streaming.png")))); // NOI18N
    menuOptionsEnableVideoStream
            .addActionListener(this::menuOptionsEnableVideoStreamActionPerformed);
    menuOptions.add(menuOptionsEnableVideoStream);

    menuOptionsLookAndFeel.setText("Look & Feel");
    menuOptionsLookAndFeel.setIcon(this.sysIcon);
    fillLookAndFeelMenu(menuOptionsLookAndFeel);

    menuOptionsScaleUi.setText("App. UI scale");
    menuOptionsScaleUi.setIcon(this.sysIcon);
    fillUiScale(menuOptionsScaleUi);

    menuOptions.addSeparator();
    menuOptions.add(menuOptionsLookAndFeel);
    menuOptions.add(menuOptionsScaleUi);

    menuBar.add(menuOptions);

    menuHelp.setText("Help");

    menuHelpAbout.setAccelerator(getKeyStroke(KeyEvent.VK_F1, 0));
    menuHelpAbout.setIcon(
            new ImageIcon(Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/info.png")))); // NOI18N
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
            Objects.requireNonNull(getClass().getResource("/com/igormaznitsa/zxpoly/icons/donate.png")))); // NOI18N

    menuHelp.add(menuHelpDonation);
    menuBar.add(menuHelp);
    setJMenuBar(menuBar);

    pack();
  }

  private void fillUiScale(final JMenu menu) {
    final String selectedScale = AppOptions.getInstance().getUiScale();

    final ButtonGroup buttonGroup = new ButtonGroup();
    Stream.of("None", "1", "1.5", "2", "2.5", "3", "3.5", "4", "4.5", "5")
            .forEach(scale -> {
              final boolean none = scale.equalsIgnoreCase("none");
              final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(none ? scale : "x" + scale, (selectedScale == null && none) || scale.equalsIgnoreCase(selectedScale));
              menuItem.addItemListener(e -> {
                LOGGER.info("Select UI scale: " + scale);
                if (e.getStateChange() == ItemEvent.SELECTED) {
                  if (none) {
                    AppOptions.getInstance().setUiScale(null);
                  } else {
                    AppOptions.getInstance().setUiScale(scale);
                  }
                  JOptionPane.showMessageDialog(MainForm.this, "Application restart required!", "Restart required", JOptionPane.WARNING_MESSAGE);
                }
              });
              buttonGroup.add(menuItem);
              menu.add(menuItem);
            });
  }

  private void fillLookAndFeelMenu(final JMenu menu) {
    final String selectedClass = AppOptions.getInstance().getUiLfClass();
    final ButtonGroup buttonGroup = new ButtonGroup();
    final List<UIManager.LookAndFeelInfo> installedLookAndFeels = new ArrayList<>(List.of(UIManager.getInstalledLookAndFeels()));
    installedLookAndFeels.sort(Comparator.comparing(UIManager.LookAndFeelInfo::getName));

    installedLookAndFeels.forEach(lf -> {
      final JRadioButtonMenuItem menuItem = new JRadioButtonMenuItem(lf.getName(), lf.getClassName().equals(selectedClass));
      menuItem.addItemListener(e -> {
        if (e.getStateChange() == ItemEvent.SELECTED) {
          try {
            UIManager.setLookAndFeel(lf.getClassName());
            SwingUtilities.invokeLater(() -> {
              SwingUtilities.updateComponentTreeUI(MainForm.this);
            });
            AppOptions.getInstance().setUiLfClass(lf.getClassName());
            AppOptions.getInstance().flush();
          } catch (Exception ex) {
            LOGGER.warning("Can't change L&F: " + ex.getMessage());
          }
        }
      });
      buttonGroup.add(menuItem);
      menu.add(menuItem);
    });
  }

  private void setDisableZxKeyboardEvents(final boolean disable) {
    this.keyboardAndTapeModule.setOnlyJoystickEvents(disable);
    this.setFastButtonState(FastButton.ZX_KEYBOARD_OFF, disable);
    LOGGER.info("Only Kempston events: " + disable);
  }

  private void menuOptionsOnlyKempstonEvents(final ActionEvent actionEvent) {
    this.setDisableZxKeyboardEvents(this.menuOptionsOnlyJoystickEvents.isSelected());
  }

  private void menuTapThresholdActionPerformed(final ActionEvent actionEvent) {
    final TapeSource source = this.keyboardAndTapeModule.getTap();
    if (source != null) {
      final JSlider slider = new JSlider();
      slider.setMajorTickSpacing(100);
      slider.setMinorTickSpacing(10);
      slider.setPaintLabels(false);
      slider.setPaintTrack(true);
      slider.setSnapToTicks(true);
      slider.setPaintTicks(true);
      slider
              .setModel(new DefaultBoundedRangeModel((int) (source.getThreshold() * 1000), 0, 0, 1000));

      if (showConfirmDialog(this, slider, "Tape signal threshold", JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
        source.setThreshold((float) slider.getValue() / 1000);
      }
    }
  }

  private void menuFileLoadPokeActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
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
          showMessageDialog(this, ex.getMessage(), "Can't read or parse file",
                  JOptionPane.ERROR_MESSAGE);
        }
      }
    } finally {
      this.resumeSteps();
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

  public void showVirtualKeyboard(final boolean show) {
    this.board.getVideoController().setVkbShow(show);
    this.setFastButtonState(FastButton.VIRTUAL_KEYBOARD, show);
  }

  private void menuOptionsTurboActionPerformed(ActionEvent evt) {
    final boolean turboActivated = this.menuOptionsTurbo.isSelected();
    if (turboActivated) {
      this.preTurboSourceSoundPort = this.board.getBeeper().setSourceSoundPort(null);
      LOGGER.info("Saved sound port: " + this.preTurboSourceSoundPort);
      this.setTurboMode(true);
    } else {
      this.setTurboMode(false);
      this.board.getBeeper().setSourceSoundPort(this.preTurboSourceSoundPort.orElse(null));
      LOGGER.info("Restored sound port: " + this.preTurboSourceSoundPort.map(SourceSoundPort::getName).orElse("NONE"));
      this.preTurboSourceSoundPort = Optional.empty();
    }
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
    final TapeSource currentReader = this.keyboardAndTapeModule.getTap();
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

  private void setTapFile(final File tapFile) {
    this.lastTapFolder = tapFile.getParentFile();
    this.stepSemaphor.lock();
    try {
      if (this.keyboardAndTapeModule.getTap() != null) {
        this.keyboardAndTapeModule.getTap().removeActionListener(this);
      }

      final TapeSource source = TapeSourceFactory.makeSource(this, this.timingProfile, tapFile);
      source.addActionListener(this);
      this.keyboardAndTapeModule.setTap(source);
      LOGGER.info("Loaded TAP, total data size " + source.size() + " bytes");
      this.labelTapeUsage.setTooltips("Reading " + source.getName(), source.getName());
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Can't read " + tapFile + ": " + ex.getMessage(), ex);
      showMessageDialog(this, ex.getMessage(), "Error TAP loading",
              JOptionPane.ERROR_MESSAGE);
    } finally {
      updateTapeMenu();
      this.stepSemaphor.unlock();
    }
  }

  private void menuFileLoadTapActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
      final File selectedTapFile =
              chooseFileForOpen("Load Tape", this.lastTapFolder, null,
                      new TzxFileFilter(),
                      new TapFileFilter(),
                      new WavFileFilter());
      if (selectedTapFile != null) {
        this.setTapFile(selectedTapFile);
      }
    } finally {
      this.resumeSteps();
    }
  }

  private void menuTapExportAsWavActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
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
      showMessageDialog(this, "Can't export as WAV", ex.getMessage(),
              JOptionPane.ERROR_MESSAGE);
    } finally {
      this.resumeSteps();
    }
  }

  @Override
  public void onTapeSignal(final TapeSource tapeSource, final ControlSignal controlSignal) {
    switch (controlSignal) {
      case STOP_TAPE:
        this.keyboardAndTapeModule.getTap().stopPlay();
        break;
      case STOP_TAPE_IF_ZX48: {
        if (this.board.isMode48k()) {
          this.keyboardAndTapeModule.getTap().stopPlay();
        }
      }
      break;
    }
  }

  private boolean setTapePlay(final boolean play) {
    if (this.keyboardAndTapeModule.getTap() == null) return false;
    if (play && this.keyboardAndTapeModule.getTap().isPlaying()) return true;
    if (play) {
      this.keyboardAndTapeModule.getTap().startPlay();
    } else {
      this.keyboardAndTapeModule.getTap().stopPlay();
    }
    updateTapeMenu();
    return true;
  }

  private void menuTapPlayActionPerformed(ActionEvent evt) {
    this.setTapePlay(this.menuTapPlay.isSelected());
  }

  private void menuTapPrevBlockActionPerformed(ActionEvent evt) {
    final TapeSource tap = this.keyboardAndTapeModule.getTap();
    if (tap != null) {
      tap.rewindToPrevBlock();
    }
    updateTapeMenu();
  }

  private void menuTapNextBlockActionPerformed(ActionEvent evt) {
    final TapeSource tap = this.keyboardAndTapeModule.getTap();
    if (tap != null) {
      tap.rewindToNextBlock();
    }
    updateTapeMenu();
  }

  private void menuTapeRewindToStartActionPerformed(ActionEvent evt) {
    final TapeSource tap = this.keyboardAndTapeModule.getTap();
    if (tap != null) {
      tap.rewindToStart();
    }
    updateTapeMenu();
  }

  private void menuServiceSaveScreenActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
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
      showMessageDialog(this, "Can't save screenshot for error, see the log!", "Error",
              JOptionPane.ERROR_MESSAGE);
      LOGGER.log(Level.SEVERE, "Can't make screenshot", ex);
    } finally {
      this.resumeSteps();
    }

  }

  private void menuFileOptionsActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
      final OptionsPanel optionsPanel = new OptionsPanel(null);
      if (showConfirmDialog(this, new JScrollPane(optionsPanel), "Preferences", JOptionPane.OK_CANCEL_OPTION,
              JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
        optionsPanel.getData().store();
        showMessageDialog(this, "Restart the emulator for new options!",
                "Restart may required!",
                JOptionPane.WARNING_MESSAGE);
      }
    } finally {
      this.resumeSteps();
    }
  }

  private void menuHelpAboutActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
      new AboutDialog(this).setVisible(true);
    } finally {
      this.resumeSteps();
    }
  }

  private void menuTraceCpu0ActionPerformed(ActionEvent evt) {
    if (this.menuTraceCpu0.isSelected()) {
      activateTracerForCPUModule(0);
      this.board.getBeeper().setSourceSoundPort(null);
    } else {
      deactivateTracerForCPUModule(0);
    }
  }

  private void menuTraceCpu1ActionPerformed(ActionEvent evt) {
    if (this.menuTraceCpu1.isSelected()) {
      activateTracerForCPUModule(1);
      this.board.getBeeper().setSourceSoundPort(null);
    } else {
      deactivateTracerForCPUModule(1);
    }
  }

  private void menuTraceCpu2ActionPerformed(ActionEvent evt) {
    if (this.menuTraceCpu2.isSelected()) {
      activateTracerForCPUModule(2);
      this.board.getBeeper().setSourceSoundPort(null);
    } else {
      deactivateTracerForCPUModule(2);
    }
  }

  private void menuTraceCpu3ActionPerformed(ActionEvent evt) {
    if (this.menuTraceCpu3.isSelected()) {
      activateTracerForCPUModule(3);
      this.board.getBeeper().setSourceSoundPort(null);
    } else {
      deactivateTracerForCPUModule(3);
    }
  }

  private void menuServiceSaveScreenAllVRAMActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
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
      showMessageDialog(this, "Can't save screenshot for error, see the log!", "Error",
              JOptionPane.ERROR_MESSAGE);
      LOGGER.log(Level.SEVERE, "Can't make screenshot", ex);
    } finally {
      this.resumeSteps();
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
    this.suspendSteps();
    try {
      AnimationEncoder encoder = this.currentAnimationEncoder.get();
      if (encoder == null) {
        final AnimatedGifTunePanel panel = new AnimatedGifTunePanel(this.lastAnimGifOptions);
        final int result = showConfirmDialog(this, panel, "Options for Animated GIF",
                JOptionPane.OK_CANCEL_OPTION);
        if (result != JOptionPane.OK_OPTION) {
          return;
        }
        this.menuViewVideoFilter.setEnabled(false);
        this.lastAnimGifOptions = panel.getValue();
        try {
          encoder = new AGifEncoder(
                  new File(this.lastAnimGifOptions.filePath),
                  this.board.getVideoController().findCurrentPalette(),
                  this.lastAnimGifOptions.frameRate,
                  this.lastAnimGifOptions.repeat);
        } catch (IOException ex) {
          this.menuViewVideoFilter.setEnabled(true);
          LOGGER.log(Level.SEVERE, "Can't create GIF encoder: " + ex.getMessage(), ex);
          showMessageDialog(this, "Can't make GIF encoder: " + ex.getMessage(), "Error!",
                  JOptionPane.ERROR_MESSAGE);
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
      this.resumeSteps();
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

  private void formWindowClosed(WindowEvent evt) {
    closeAnimationSave();
  }

  private void menuTriggerModuleCPUDesyncActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
      if (this.menuTriggerModuleCPUDesync.isSelected()) {
        this.board.setTrigger(Motherboard.TRIGGER_DIFF_MODULESTATES);
      } else {
        this.board.resetTrigger(Motherboard.TRIGGER_DIFF_MODULESTATES);
      }
    } finally {
      this.resumeSteps();
    }
  }

  private void menuTriggerDiffMemActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
      if (this.menuTriggerDiffMem.isSelected()) {
        final AddressPanel panel = new AddressPanel(this.board.getMemTriggerAddress());
        if (showConfirmDialog(MainForm.this, panel, "Triggering address",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.OK_OPTION) {
          try {
            final int address = panel.extractAddressFromText();
            if (address < 0 || address > 0xFFFF) {
              showMessageDialog(MainForm.this, "Error address must be in #0000...#FFFF",
                      "Error address", JOptionPane.ERROR_MESSAGE);
            } else {
              this.board.setMemTriggerAddress(address);
              this.board.setTrigger(Motherboard.TRIGGER_DIFF_MEM_ADDR);
            }
          } catch (NumberFormatException ex) {
            showMessageDialog(MainForm.this,
                    "Error address format, use # for hexadecimal address (example #AA00)",
                    "Error address", JOptionPane.ERROR_MESSAGE);
          }
        }
      } else {
        this.board.resetTrigger(Motherboard.TRIGGER_DIFF_MEM_ADDR);
      }
    } finally {
      this.resumeSteps();
    }
  }

  private void menuTriggerExeCodeDiffActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
      if (this.menuTriggerExeCodeDiff.isSelected()) {
        this.board.setTrigger(Motherboard.TRIGGER_DIFF_EXE_CODE);
      } else {
        this.board.resetTrigger(Motherboard.TRIGGER_DIFF_EXE_CODE);
      }
    } finally {
      this.resumeSteps();
    }
  }

  private void suspendSteps() {
    this.turnZxKeyboardOff();
    this.stepSemaphor.lock();
  }

  private void resumeSteps() {
    this.turnZxKeyboardOn();
    this.stepSemaphor.unlock();
  }

  private void menuServiceMakeSnapshotActionPerformed(ActionEvent evt) {
    this.suspendSteps();
    try {
      final AtomicReference<FileFilter> theFilter = new AtomicReference<>();
      File selected = chooseFileForSave("Save snapshot", this.lastSnapshotFolder, theFilter, false,
              Stream.of(SNAPSHOT_FORMAT_SPEC256, SNAPSHOT_FORMAT_ZXP, SNAPSHOT_FORMAT_Z80, SNAPSHOT_FORMAT_SNA, SNAPSHOT_FORMAT_ROM).filter(x -> x.canMakeSnapshotForBoardMode(this.board.getBoardMode()))
                      .toArray(Snapshot[]::new)
      );

      if (selected != null) {
        this.lastSnapshotFolder = selected.getParentFile();
        try {
          final Snapshot selectedFilter = (Snapshot) theFilter.get();
          if (!selectedFilter.getExtension()
                  .equals(FilenameUtils.getExtension(selected.getName()).toLowerCase(Locale.ENGLISH))) {
            selected = new File(selected.getParentFile(),
                    selected.getName() + '.' + selectedFilter.getExtension());
          }

          if (selected.isFile() && showConfirmDialog(
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
          showMessageDialog(this, "Can't save snapshot file [" + ex.getMessage() + ']',
                  "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    } finally {
      this.resumeSteps();
    }
  }

  private void menuFileMenuSelected(final MenuEvent evt) {
    if (this.board.isBetaDiskPresented()) {
      boolean hasChangedDisk = false;
      for (int i = 0; i < 4; i++) {
        final TrDosDisk disk = this.board.getBetaDiskInterface().getDiskInDrive(i);
        hasChangedDisk |= (disk != null && disk.isChanged());
      }
      this.menuFileFlushDiskChanges.setEnabled(hasChangedDisk);
    }
  }

  private void menuFileFlushDiskChangesActionPerformed(ActionEvent evt) {
    for (int i = 0; i < 4; i++) {
      final TrDosDisk disk = this.board.getBetaDiskInterface().getDiskInDrive(i);
      if (disk != null && disk.isChanged()) {
        final int result = showConfirmDialog(this,
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
              showMessageDialog(this, "Can't save disk for IO error: " + ex.getMessage(),
                      "Error", JOptionPane.ERROR_MESSAGE);
            }
          }
        }
      }
    }
  }

  private void formWindowClosing(WindowEvent evt) {
    if (this.currentFullScreen.get() != null) {
      this.doFullScreen();
    }

    boolean hasChangedDisk = false;
    if (this.board.isBetaDiskPresented()) {
      for (int i = 0; i < 4; i++) {
        final TrDosDisk disk = this.board.getBetaDiskInterface().getDiskInDrive(i);
        hasChangedDisk |= (disk != null && disk.isChanged());
      }
    }

    boolean close = false;

    if (hasChangedDisk) {
      if (showConfirmDialog(this, "Emulator has unsaved disks, do you realy want to close it?",
              "Detected unsaved data", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
        close = true;
      }
    } else {
      close = true;
    }

    AppOptions.getInstance().setSoundTurnedOn(!this.board.getBeeper().isNullBeeper());

    this.board.dispose();

    if (close) {
      final SpriteCorrectorMainFrame spriteCorrector = this.spriteCorrectorMainFrame.get();
      if (spriteCorrector != null && spriteCorrector.isDisplayable()) {
        LOGGER.info("Detected active ZXPoly Sprite corrector");
        spriteCorrector.toFront();
        spriteCorrector.requestFocus();
        this.infoBarUpdateTimer.stop();
        this.board.dispose();
        this.mainCpuThread.interrupt();
        this.dispose();
      } else {
        System.exit(0);
      }
    }
  }

  private void menuLoadDriveMenuSelected(MenuEvent evt) {
    final JMenuItem[] menuItems = new JMenuItem[]{
            this.menuFileSelectDiskA, this.menuFileSelectDiskB,
            this.menuFileSelectDiskC, this.menuFileSelectDiskD
    };
    IntStream.range(0, 4).forEach(index -> {
      final TrDosDisk diskInDrive = this.board.getBetaDiskInterface().getDiskInDrive(index);
      final JMenuItem diskMenuItem = menuItems[index];
      if (diskInDrive == null) {
        diskMenuItem.setIcon(null);
        diskMenuItem.setToolTipText(null);
      } else {
        diskMenuItem.setIcon(ICO_MDISK);
        diskMenuItem.setToolTipText(diskInDrive.getSrcFile() == null ? null : diskInDrive.getSrcFile().getAbsolutePath());
      }
    });
  }

  private void menuOptionsEnableTrapMouseActionPerformed(ActionEvent evt) {
    this.board.getVideoController()
            .setEnableTrapMouse(this.menuOptionsEnableTrapMouse.isSelected(), true, false);
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
    Arrays.stream(this.cpuTracers)
            .filter(Objects::nonNull)
            .forEach(Window::dispose);
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
    if (filters.length != 0 && !allowAcceptAll) {
      chooser.setFileFilter(filters[0]);
    }
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
    if (e.getSource() instanceof TapeSource) {
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

  private static class TzxFileFilter extends FileFilter {

    @Override
    public boolean accept(final File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".tzx");
    }

    @Override
    public String getDescription() {
      return "TZX file (*.tzx)";
    }

  }

  private static class WavFileFilter extends FileFilter {

    @Override
    public boolean accept(final File f) {
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
    private final MainForm mainForm;

    KeyboardDispatcher(final MainForm mainForm) {
      this.mainForm = mainForm;
      this.keyboard = mainForm.keyboardAndTapeModule;
      this.videoController = mainForm.board.getVideoController();
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent e) {
      boolean consumed = false;
      if (!e.isConsumed() && MainForm.this.zxKeyboardProcessingAllowed) {
        if (e.getKeyCode() == KeyEvent.VK_F5) {
          if (e.getID() == KeyEvent.KEY_PRESSED) {
            this.mainForm.showVirtualKeyboard(!this.videoController.isVkbShow());
          }
          e.consume();
          consumed = true;
        }

        if (MainForm.this.currentFullScreen.get() != null) {
          // Full screen mode
          if (e.getID() == KeyEvent.KEY_RELEASED) {
            switch (e.getKeyCode()) {
              case KeyEvent.VK_F11:
              case KeyEvent.VK_ESCAPE: {
                e.consume();
                consumed = true;
                MainForm.this.doFullScreen();
              }
              break;
              case KeyEvent.VK_F12: {
                e.consume();
                consumed = true;
                MainForm.this.menuFileResetActionPerformed(new ActionEvent(this, 0, "reset"));
              }
              break;
            }
          }
        }

        if (!consumed) {
          consumed = this.keyboard.onKeyEvent(e);
          if (consumed) {
            e.consume();
          }
        }
      }
      return consumed;
    }
  }


}
