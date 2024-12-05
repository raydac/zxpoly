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

import static java.util.Objects.requireNonNullElseGet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static javax.swing.JOptionPane.OK_OPTION;
import static javax.swing.JOptionPane.showConfirmDialog;

import com.igormaznitsa.zxpspritecorrector.components.CpuRegProperties;
import com.igormaznitsa.zxpspritecorrector.components.CpuSnapshotParamsEditor;
import com.igormaznitsa.zxpspritecorrector.components.EditorComponent;
import com.igormaznitsa.zxpspritecorrector.components.InsideFileView;
import com.igormaznitsa.zxpspritecorrector.components.PenWidth;
import com.igormaznitsa.zxpspritecorrector.components.SelectInsideDataDialog;
import com.igormaznitsa.zxpspritecorrector.components.ZXColorSelector;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;
import com.igormaznitsa.zxpspritecorrector.files.Spec256ConfigEditorPanel;
import com.igormaznitsa.zxpspritecorrector.files.plugins.AbstractFilePlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.HOBETAPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.LegacySZEPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.SCLPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.SCRPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.SNAPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.SZEPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.Spec256ZipPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.TAPPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.TRDPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.Z80InZXPOutPlugin;
import com.igormaznitsa.zxpspritecorrector.tools.AbstractTool;
import com.igormaznitsa.zxpspritecorrector.tools.ToolButtonModel;
import com.igormaznitsa.zxpspritecorrector.tools.ToolColorizer;
import com.igormaznitsa.zxpspritecorrector.tools.ToolEraser;
import com.igormaznitsa.zxpspritecorrector.tools.ToolPencil;
import com.igormaznitsa.zxpspritecorrector.utils.GfxUtils;
import com.igormaznitsa.zxpspritecorrector.utils.TransferableImage;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsConfiguration;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.prefs.Preferences;
import javax.swing.BoundedRangeModel;
import javax.swing.Box.Filler;
import javax.swing.ButtonGroup;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu.Separator;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.JScrollBar;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.picocontainer.MutablePicoContainer;
import org.picocontainer.PicoBuilder;
import org.picocontainer.PicoContainer;
import org.picocontainer.injectors.ProviderAdapter;

public final class SpriteCorrectorMainFrame extends JFrame {

  private static final Logger LOGGER = Logger.getLogger("Sprite-Corrector");

  public static final String EXTRA_PROPERTY_DATA_ID = "spec256.config.properties";
  public static final String EXTRA_PROPERTY_OVERRIDE_CPU_DATA = "z80.override.cpu.properties";
  private static final long serialVersionUID = -5031012548284731523L;
  private static Properties lastSpec256Properties = new Properties();
  public final MutablePicoContainer container = new PicoBuilder()
          .withAutomatic()
          .withAnnotatedMethodInjection()
          .withAnnotatedFieldInjection()
          .withConstructorInjection()
          .withCaching()
          .build();
  final BoundedRangeModel SLIDER_ALL_MODEL = new DefaultBoundedRangeModel(32, 0, 1, 32);
  final BoundedRangeModel SLIDER_ODD_OR_EVEN_MODEL = new DefaultBoundedRangeModel(16, 0, 1, 16);
  final Dictionary<Integer, JLabel> SLIDER_ALL_LABELS;
  final Dictionary<Integer, JLabel> SLIDER_ODD_LABELS;
  final Dictionary<Integer, JLabel> SLIDER_EVEN_LABELS;
  private final Cursor CURSOR_BLANK = Toolkit.getDefaultToolkit()
          .createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0),
                  "blank cursor");
  private final AtomicReference<AbstractTool> currentAbstractTool = new AtomicReference<>();
  private File lastOpenedFile;
  private File lastExportedFile;
  private File szeFile;
  private boolean selectAreaMode = false;
  private ButtonGroup attributesButtonGroup;
  private JToggleButton buttonLock;
  private ZXColorSelector colorSelector;
  private ButtonGroup columnModeGroup;
  private Filler filler1;
  private JPanel jPanel2;
  private javax.swing.JScrollPane jScrollPane1;
  private JSeparator jSeparator1;
  private Separator jSeparator2;
  private Separator jSeparator3;
  private Separator jSeparator4;
  private Separator jSeparator5;
  private Separator jSeparator6;
  private Separator jSeparator7;
  private Separator jSeparator8;
  private JLabel labelAddress;
  private JLabel labelZoom;
  private EditorComponent mainEditor;
  private JPanel mainEditorPanel;
  private JMenuBar menuBar;
  private JMenu menuEdit;
  private JMenu menuView;
  private JMenu menuViewZoom;
  private JMenuItem menuViewZoomIn;
  private JMenuItem menuViewZoomOut;
  private JMenuItem menuEditClear;
  private JMenuItem menuEditCopyBaseToPlans;
  private JMenuItem menuEditCopySelectedBaseAsImage;
  private JMenuItem menuEditCopySelectedZxPolyAsImage;
  private JMenuItem menuEditPasteImage;
  private JMenuItem menuEditRedo;
  private JMenuItem menuEditSelectArea;
  private JMenuItem menuEditUndo;
  private JMenuItem menuEditEditStartParameters;
  private JMenu menuFile;
  private JMenuItem menuFileExit;
  private JMenu menuFileExportAs;
  private JMenu menuFileRecentFiles;
  private JMenuItem menuFileNew;
  private JMenuItem menuFileOpen;
  private JMenuItem menuFileSaveAs;
  private JMenu menuHelp;
  private JMenuItem menuHelpAbout;
  private JRadioButtonMenuItem menuOptionDontShowAttributes;
  private JMenu menuOptions;
  private JCheckBoxMenuItem menuOptionsColumns;
  private JRadioButtonMenuItem menuOptionsColumnsAll;
  private JRadioButtonMenuItem menuOptionsColumnsEven;
  private JRadioButtonMenuItem menuOptionsColumnsOdd;
  private JCheckBoxMenuItem menuOptionsGrid;
  private JCheckBoxMenuItem menuOptionsInvertBase;
  private JCheckBoxMenuItem menuOptionsMode512;
  private JRadioButtonMenuItem menuOptionsShow512x384Attributes;
  private JRadioButtonMenuItem menuOptionsShowBaseAttributes;
  private JCheckBoxMenuItem menuOptionsZXScreen;
  private JMenuItem menuSave;
  private JPanel panelTools;
  private JScrollBar scrollBarAddress;
  private JSlider sliderColumns;
  private PenWidth sliderPenWidth;
  private JSpinner spinnerCurrentAddress;
  private ButtonGroup toolsButtonGroup;
  private CpuRegProperties snapshotCpuOverrideValues = new CpuRegProperties();

  public SpriteCorrectorMainFrame(
          final GraphicsConfiguration graphicsConfig,
          final boolean standaloneApplication
  ) {
    super(graphicsConfig);
    SLIDER_ALL_LABELS = new Hashtable<>();
    SLIDER_ODD_LABELS = new Hashtable<>();
    SLIDER_EVEN_LABELS = new Hashtable<>();

    int even = 1;
    int odd = 1;
    for (int i = 1; i <= 32; i++) {
      final JLabel label = new JLabel(Integer.toString(i));
      label.setFont(label.getFont().deriveFont(Font.BOLD));
      SLIDER_ALL_LABELS.put(i, label);
      if ((i & 1) == 0) {
        SLIDER_EVEN_LABELS.put(even++, label);
      } else {
        SLIDER_ODD_LABELS.put(odd++, label);
      }
    }

    initComponents();

    this.sliderColumns.setSnapToTicks(true);
    this.sliderColumns.setModel(SLIDER_ALL_MODEL);
    this.sliderColumns.setLabelTable(SLIDER_ALL_LABELS);

    this.sliderColumns.setValue(this.mainEditor.getColumns());

    this.container.addAdapter(new ProviderAdapter(new ContextProvider(container)));
    this.container.addComponent(this);
    this.container.addComponent(this.colorSelector);

    this.container.addComponent(SZEPlugin.class);
    this.container.addComponent(HOBETAPlugin.class);
    this.container.addComponent(TAPPlugin.class);
    this.container.addComponent(TRDPlugin.class);
    this.container.addComponent(SCLPlugin.class);
    this.container.addComponent(SCRPlugin.class);
    this.container.addComponent(Z80InZXPOutPlugin.class);
    this.container.addComponent(SNAPlugin.class);
    this.container.addComponent(Spec256ZipPlugin.class);
    this.container.addComponent(LegacySZEPlugin.class);

    this.container.addComponent(ToolPencil.class);
    this.container.addComponent(ToolEraser.class);
    this.container.addComponent(ToolColorizer.class);

    this.container.start();

    this.container.getComponents(AbstractTool.class).forEach(tool -> {
              this.panelTools.add(tool);
              this.toolsButtonGroup.add(tool);

              tool.addItemListener((ItemEvent e) -> {
                if (e.getStateChange() == ItemEvent.SELECTED) {
                  this.selectAreaMode = false;
                  this.sliderPenWidth.setModel(((AbstractTool) e.getItem()).getScaleModel());
                  this.currentAbstractTool.set(tool);
                } else if (e.getStateChange() == ItemEvent.DESELECTED) {
                  if (this.currentAbstractTool.compareAndSet(tool, null)) {
                    this.sliderPenWidth.setModel(null);
                  }
                }
              });
            }
    );

    this.container.getComponents(AbstractFilePlugin.class).stream()
            .filter(AbstractFilePlugin::isExportable)
            .forEachOrdered(p -> {
              final JMenuItem menuItem = new JMenuItem(p.getPluginDescription(true));
              this.menuFileExportAs.add(menuItem);
              menuItem.setToolTipText(p.getToolTip(true));
              menuItem.addActionListener(e -> exportDataWithPlugin(p));
            });

    this.setLocationRelativeTo(null);
    updateAddressScrollBar();

    loadStateFromSession(new SessionData(this.mainEditor));
    setCurrentSZEFile(null);
    updateBottomBar();

    this.spinnerCurrentAddress.setModel(this.mainEditor);

    this.setIconImage(GfxUtils.loadImage("ico.png"));

    for (final Component j : this.menuBar.getComponents()) {
      if (j instanceof JMenu) {
        ((JMenu) j).addMenuListener(new MenuListener() {
          @Override
          public void menuSelected(MenuEvent e) {
            selectAreaMode = false;
            toolsButtonGroup.clearSelection();
          }

          @Override
          public void menuDeselected(MenuEvent e) {
          }

          @Override
          public void menuCanceled(MenuEvent e) {
          }
        });
      }
    }

    this.menuOptionsMode512.addActionListener(x -> {
      final boolean mode512 = this.menuOptionsMode512.isSelected();
      this.container.getComponents(AbstractTool.class)
              .forEach((t) -> t.setEnabled(!mode512 || t.doesSupport512x384()));
    });

    resetOptions();
  }

  public static String toHex(final int value) {
    final String h = Integer.toHexString(value).toUpperCase(Locale.ENGLISH);
    return '#' + (h.length() < 4 ? "0000".substring(0, 4 - h.length()) + h : h);
  }

  public static Properties deserializeProperties(final String data) {
    if (data == null) {
      return null;
    }
    final byte[] array = Base64.getDecoder().decode(data);
    final Properties result = new Properties();
    try {
      result.load(new StringReader(new String(array, StandardCharsets.UTF_8)));
    } catch (IOException ex) {
      throw new Error("Can't load properties", ex);
    }
    return result;
  }

  private static String serializeProperties(final Properties properties) {
    if (properties == null) {
      return null;
    }
    final StringWriter writer = new StringWriter();
    try {
      properties.store(writer, null);
    } catch (IOException ex) {
      throw new Error("Can't write properties", ex);
    }
    return Base64.getEncoder().encodeToString(writer.toString().getBytes(StandardCharsets.UTF_8));
  }

  public PicoContainer getPico() {
    return this.container;
  }

  private void deactivateCheckbox(final JCheckBoxMenuItem item) {
    if (item.isSelected()) {
      item.doClick();
    }
  }

  private void resetOptions() {
    deactivateCheckbox(this.menuOptionsMode512);
    deactivateCheckbox(this.menuOptionsZXScreen);
    this.spinnerCurrentAddress.setValue(0);
    deactivateCheckbox(this.menuOptionsGrid);
    deactivateCheckbox(this.menuOptionsColumns);
    deactivateCheckbox(this.menuOptionsInvertBase);
    this.menuOptionDontShowAttributes.doClick();
    this.menuOptionsColumnsAll.doClick();
    this.sliderColumns.setValue(32);
    refreshMenuAndToolState();
  }

  private void refreshMenuAndToolState() {
    this.menuEditSelectArea.setEnabled(this.mainEditor.hasData());
    this.menuEditPasteImage.setEnabled(this.mainEditor.hasData());
    this.menuEditCopySelectedBaseAsImage.setEnabled(this.mainEditor.hasSelectedArea());
    this.menuEditCopySelectedZxPolyAsImage.setEnabled(this.mainEditor.hasSelectedArea());

    final boolean m512 = this.mainEditor.isMode512();
    this.container.getComponents(AbstractTool.class).forEach(t -> {
      if (m512 && !t.doesSupport512x384()) {
        t.setEnabled(false);
      }
    });
  }

  private File ensureExtension(final File file, final AbstractFilePlugin plugin) {
    final String extension = plugin.getExtension(true);
    if (extension != null) {
      if (FilenameUtils.getExtension(file.getName()).isEmpty()) {
        return new File(file.getParent(), file.getName() + '.' + extension);
      }
    }
    return file;
  }

  private void exportDataWithPlugin(final AbstractFilePlugin plugin) {
    if (!this.mainEditor.hasData()) {
      JOptionPane.showMessageDialog(this, "There is no data to export!", "There is no data",
              JOptionPane.WARNING_MESSAGE);
      return;
    }

    final JFileChooser fileChooser = new JFileChooser(
            this.lastExportedFile == null ? null : this.lastExportedFile.getParentFile());
    fileChooser.setAcceptAllFileFilterUsed(false);
    fileChooser.addChoosableFileFilter(plugin.getExportFileFilter());
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      this.lastExportedFile = ensureExtension(fileChooser.getSelectedFile(), plugin);
      try {
        if (plugin instanceof Spec256ZipPlugin) {
          Properties properties = lastSpec256Properties;
          final Spec256ConfigEditorPanel configEditorPanel =
                  new Spec256ConfigEditorPanel(properties);
          if (JOptionPane.showConfirmDialog(this, configEditorPanel, "Spec256 properties",
                  JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == OK_OPTION) {
            properties = configEditorPanel.make();
            lastSpec256Properties = properties;
            final SessionData sessionDataToSave = makeCurrentSessionData();
            plugin.writeTo(this.lastExportedFile, this.mainEditor.getProcessingData(),
                    sessionDataToSave, properties);
          }
        } else {
          final SessionData sessionDataToSave = makeCurrentSessionData();
          plugin.writeTo(this.lastExportedFile, this.mainEditor.getProcessingData(),
                  sessionDataToSave);
        }
      } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane
                .showMessageDialog(this, "Can't export data for exception [" + ex.getMessage() + ']',
                        "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void updateBottomBar() {
    this.labelZoom.setText("x" + this.mainEditor.getZoom());
  }

  private void loadStateFromSession(final SessionData sessionData) {
    final int address = sessionData.getBaseAddress();

    sessionData.fill(this.mainEditor);

    this.snapshotCpuOverrideValues =
        new CpuRegProperties(
            deserializeProperties(sessionData.getExtraProperty(EXTRA_PROPERTY_OVERRIDE_CPU_DATA)));

    final Properties sessionSpec256Properties = requireNonNullElseGet(
        deserializeProperties(sessionData.getExtraProperty(EXTRA_PROPERTY_DATA_ID)),
        Spec256ConfigEditorPanel::makeDefault);

    lastSpec256Properties.clear();
    sessionSpec256Properties.stringPropertyNames().forEach(name -> {
      lastSpec256Properties.setProperty(name, sessionSpec256Properties.getProperty(name));
    });

    this.menuOptionsColumnsAll.setSelected(true);
    this.menuOptionsZXScreen.setSelected(sessionData.isZXAddressing());
    this.menuOptionsColumns.setSelected(sessionData.isShowColumns());
    this.menuOptionsGrid.setSelected(sessionData.isShowGrid());
    this.menuOptionsInvertBase.setSelected(sessionData.isInvertBaseShow());
    this.menuOptionsMode512.setSelected(sessionData.is512Mode());

    switch (sessionData.getAttributeMode()) {
      case DONT_SHOW:
        this.menuOptionDontShowAttributes.setSelected(true);
        break;
      case SHOW_BASE:
        this.menuOptionsShowBaseAttributes.setSelected(true);
        break;
      case SHOW_512x384_ZXPOLY_PLANES:
        this.menuOptionsShow512x384Attributes.setSelected(true);
        break;
    }

    updateAddressScrollBar();
    this.scrollBarAddress.setValue(address);

    this.menuOptionsColumnsAll.setSelected(true);
    this.mainEditor.setColumnMode(EditorComponent.ColumnMode.ALL);
    this.sliderColumns.setModel(SLIDER_ALL_MODEL);
    this.sliderColumns.setLabelTable(SLIDER_ALL_LABELS);
    this.sliderColumns.setValue(this.mainEditor.getColumns());

    updateBottomBar();
    refreshMenuAndToolState();
  }

  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    toolsButtonGroup = new ButtonGroup();
    attributesButtonGroup = new ButtonGroup();
    columnModeGroup = new ButtonGroup();
    scrollBarAddress = new JScrollBar();
    sliderColumns = new JSlider();
    buttonLock = new JToggleButton();
    panelTools = new JPanel();
    colorSelector = new ZXColorSelector();
    sliderPenWidth = new PenWidth();
    jScrollPane1 = new javax.swing.JScrollPane();
    mainEditorPanel = new JPanel();
    mainEditor = new EditorComponent();
    jPanel2 = new JPanel();
    labelZoom = new JLabel();
    filler1 = new Filler(new Dimension(0, 0), new Dimension(0, 0), new Dimension(32767, 0));
    labelAddress = new JLabel();
    spinnerCurrentAddress = new JSpinner();
    menuBar = new JMenuBar();
    menuFile = new JMenu();
    menuFileNew = new JMenuItem();
    menuFileOpen = new JMenuItem();
    menuSave = new JMenuItem();
    menuFileSaveAs = new JMenuItem();
    jSeparator4 = new Separator();
    menuFileExportAs = new JMenu();
    menuFileRecentFiles = new JMenu();
    jSeparator1 = new JSeparator();
    menuFileExit = new JMenuItem();
    menuEdit = new JMenu();
    menuView = new JMenu();
    menuViewZoom = new JMenu();
    menuViewZoomIn = new JMenuItem();
    menuViewZoomOut = new JMenuItem();
    menuEditUndo = new JMenuItem();
    menuEditRedo = new JMenuItem();
    menuEditEditStartParameters = new JMenuItem();
    jSeparator2 = new Separator();
    jSeparator8 = new Separator();
    menuEditSelectArea = new JMenuItem();
    menuEditCopySelectedZxPolyAsImage = new JMenuItem();
    menuEditCopySelectedBaseAsImage = new JMenuItem();
    menuEditPasteImage = new JMenuItem();
    jSeparator7 = new Separator();
    menuEditCopyBaseToPlans = new JMenuItem();
    menuEditClear = new JMenuItem();
    menuOptions = new JMenu();
    menuOptionsGrid = new JCheckBoxMenuItem();
    menuOptionsColumns = new JCheckBoxMenuItem();
    menuOptionsInvertBase = new JCheckBoxMenuItem();
    jSeparator5 = new Separator();
    menuOptionsZXScreen = new JCheckBoxMenuItem();
    menuOptionsMode512 = new JCheckBoxMenuItem();
    jSeparator6 = new Separator();
    menuOptionDontShowAttributes = new JRadioButtonMenuItem();
    menuOptionsShowBaseAttributes = new JRadioButtonMenuItem();
    menuOptionsShow512x384Attributes = new JRadioButtonMenuItem();
    jSeparator3 = new Separator();
    menuOptionsColumnsAll = new JRadioButtonMenuItem();
    menuOptionsColumnsOdd = new JRadioButtonMenuItem();
    menuOptionsColumnsEven = new JRadioButtonMenuItem();
    menuHelp = new JMenu();
    menuHelpAbout = new JMenuItem();

    setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    setTitle("ZX-Poly Sprite Corrector");
    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent evt) {
        applicationClosing(evt);
      }
    });

    scrollBarAddress.setToolTipText("Memory window position");
    scrollBarAddress.setFocusable(false);
    scrollBarAddress.addAdjustmentListener(this::scrollBarAddressAdjustmentValueChanged);

    sliderColumns.setMajorTickSpacing(1);
    sliderColumns.setMinorTickSpacing(1);
    sliderColumns.setPaintLabels(true);
    sliderColumns.setPaintTicks(true);
    sliderColumns.setSnapToTicks(true);
    sliderColumns.setToolTipText("Columns number");
    sliderColumns.setExtent(1);
    sliderColumns.setFocusable(false);
    sliderColumns.setValueIsAdjusting(true);
    sliderColumns.addChangeListener(this::sliderColumnsStateChanged);

    buttonLock.setText("LOCK");
    buttonLock.setToolTipText("To lock current memory position and cols number");
    buttonLock.setFocusable(false);
    buttonLock.addActionListener(this::buttonLockActionPerformed);

    panelTools.setBorder(javax.swing.BorderFactory.createTitledBorder("Tools"));
    panelTools.setFocusable(false);

    colorSelector.setToolTipText("Colors for paint (Lft btn - INK, Rght btn - PAPER)");

    javax.swing.GroupLayout colorSelectorLayout = new javax.swing.GroupLayout(colorSelector);
    colorSelector.setLayout(colorSelectorLayout);
    colorSelectorLayout.setHorizontalGroup(
            colorSelectorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGap(0, 0, Short.MAX_VALUE)
    );
    colorSelectorLayout.setVerticalGroup(
            colorSelectorLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGap(0, 110, Short.MAX_VALUE)
    );

    sliderPenWidth.setToolTipText("Width of an operation tool");
    sliderPenWidth.setDoubleBuffered(false);
    sliderPenWidth.setMaximumSize(new Dimension(96, 84));
    sliderPenWidth.setMinimumSize(new Dimension(96, 84));
    sliderPenWidth.setPreferredSize(new Dimension(96, 84));

    mainEditorPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.CROSSHAIR_CURSOR));
    mainEditorPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
      public void mouseMoved(java.awt.event.MouseEvent evt) {
        mainEditorPanelMouseMoved(evt);
      }

      public void mouseDragged(java.awt.event.MouseEvent evt) {
        mainEditorPanelMouseDragged(evt);
      }
    });
    mainEditorPanel.addMouseWheelListener(this::mainEditorPanelMouseWheelMoved);
    mainEditorPanel.addMouseListener(new java.awt.event.MouseAdapter() {
      public void mousePressed(java.awt.event.MouseEvent evt) {
        mainEditorPanelMousePressed(evt);
      }

      public void mouseReleased(java.awt.event.MouseEvent evt) {
        mainEditorPanelMouseReleased(evt);
      }

      public void mouseClicked(java.awt.event.MouseEvent evt) {
        mainEditorPanelMouseClicked(evt);
      }

      public void mouseExited(java.awt.event.MouseEvent evt) {
        mainEditorPanelMouseExited(evt);
      }

      public void mouseEntered(java.awt.event.MouseEvent evt) {
        mainEditorPanelMouseEntered(evt);
      }
    });

    javax.swing.GroupLayout mainEditorPanelLayout = new javax.swing.GroupLayout(mainEditorPanel);
    mainEditorPanel.setLayout(mainEditorPanelLayout);
    mainEditorPanelLayout.setHorizontalGroup(
            mainEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGap(0, 598, Short.MAX_VALUE)
                    .addGroup(
                            mainEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(mainEditorPanelLayout.createSequentialGroup()
                                            .addGap(0, 0, Short.MAX_VALUE)
                                            .addComponent(mainEditor, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                    javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, Short.MAX_VALUE)))
    );
    mainEditorPanelLayout.setVerticalGroup(
            mainEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGap(0, 394, Short.MAX_VALUE)
                    .addGroup(
                            mainEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(mainEditorPanelLayout.createSequentialGroup()
                                            .addGap(0, 0, Short.MAX_VALUE)
                                            .addComponent(mainEditor, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                    javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addGap(0, 0, Short.MAX_VALUE)))
    );

    jScrollPane1.setViewportView(mainEditorPanel);

    jPanel2.setBorder(javax.swing.BorderFactory.createEtchedBorder());
    jPanel2.setLayout(new java.awt.GridBagLayout());

    labelZoom.setText("Zoom");
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 2;
    gridBagConstraints.gridy = 0;
    jPanel2.add(labelZoom, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 1;
    gridBagConstraints.gridy = 0;
    gridBagConstraints.weightx = 1000.0;
    jPanel2.add(filler1, gridBagConstraints);
    gridBagConstraints = new java.awt.GridBagConstraints();
    gridBagConstraints.gridx = 0;
    gridBagConstraints.gridy = 0;
    jPanel2.add(labelAddress, gridBagConstraints);

    spinnerCurrentAddress.setFocusable(false);

    menuFile.setText("File");

    menuFileNew.setText("New");
    menuFileNew.addActionListener(this::menuFileNewActionPerformed);
    menuFile.add(menuFileNew);

    menuFileOpen.setText("Open");
    menuFileOpen.addActionListener(this::menuFileOpenActionPerformed);
    menuFile.add(menuFileOpen);

    menuSave.setAccelerator(javax.swing.KeyStroke
            .getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
    menuSave.setText("Save");
    menuSave.addActionListener(this::menuSaveActionPerformed);
    menuFile.add(menuSave);

    menuFileSaveAs.setText("Save As");
    menuFileSaveAs.addActionListener(this::menuFileSaveAsActionPerformed);
    menuFile.add(menuFileSaveAs);
    menuFile.add(jSeparator4);

    menuFileRecentFiles.setText("Recent projects");
    menuFile.add(menuFileRecentFiles);
    menuFileRecentFiles.addMenuListener(new MenuListener() {
      @Override
      public void menuSelected(MenuEvent e) {
        final List<String> recentProjects = getRecentProjects();
        menuFileRecentFiles.removeAll();
        for (final String path : recentProjects) {
          final JMenuItem projectItem = new JMenuItem(path);
          projectItem.addActionListener(x -> SpriteCorrectorMainFrame.this.openSzeFileForPath(path));
          menuFileRecentFiles.add(projectItem);
        }
      }

      @Override
      public void menuDeselected(MenuEvent e) {

      }

      @Override
      public void menuCanceled(MenuEvent e) {

      }
    });
    menuFile.add(new JSeparator());

    menuFileExportAs.setText("Export as..");
    menuFile.add(menuFileExportAs);
    menuFile.add(jSeparator1);

    menuFileExit.setText("Exit");
    menuFileExit.setToolTipText("Close application");
    menuFileExit.addActionListener(this::menuFileExitActionPerformed);
    menuFile.add(menuFileExit);

    menuBar.add(menuFile);
    menuView.setText("View");
    menuViewZoom.setText("Zoom");
    menuViewZoomIn.setText("Zoom In");
    menuViewZoomIn.addActionListener(e -> this.mainEditor.zoomIn());
    menuViewZoomIn.setAccelerator(KeyStroke
            .getKeyStroke(KeyEvent.VK_EQUALS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));
    menuViewZoomOut.setText("Zoom Out");
    menuViewZoomOut.addActionListener(e -> this.mainEditor.zoomOut());
    menuViewZoomOut.setAccelerator(KeyStroke
            .getKeyStroke(KeyEvent.VK_MINUS, Toolkit.getDefaultToolkit().getMenuShortcutKeyMask()));

    menuEdit.setText("Edit");
    menuEdit.addMenuListener(new javax.swing.event.MenuListener() {
      public void menuSelected(javax.swing.event.MenuEvent evt) {
        menuEditMenuSelected(evt);
      }

      public void menuDeselected(javax.swing.event.MenuEvent evt) {
        menuEditMenuDeselected(evt);
      }

      public void menuCanceled(javax.swing.event.MenuEvent evt) {
      }
    });

    menuEditUndo.setAccelerator(javax.swing.KeyStroke
            .getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
    menuEditUndo.setText("Undo");
    menuEditUndo.addActionListener(this::menuEditUndoActionPerformed);
    menuEdit.add(menuEditUndo);

    menuEditRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z,
            java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    menuEditRedo.setText("Redo");
    menuEditRedo.addActionListener(this::menuEditRedoActionPerformed);
    menuEdit.add(menuEditRedo);
    menuEdit.add(jSeparator2);

    menuEditEditStartParameters.setText("Change snapshot CPU values");
    menuEditEditStartParameters.addActionListener(this::onActionMenuEditEditStartParameters);
    menuEdit.add(menuEditEditStartParameters);
    menuEdit.add(jSeparator8);

    menuEditSelectArea.setAccelerator(javax.swing.KeyStroke
            .getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
    menuEditSelectArea.setText("Select area");
    menuEditSelectArea.addActionListener(this::menuEditSelectAreaActionPerformed);
    menuEdit.add(menuEditSelectArea);

    menuEditCopySelectedZxPolyAsImage.setAccelerator(javax.swing.KeyStroke
            .getKeyStroke(java.awt.event.KeyEvent.VK_C,
                    java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    menuEditCopySelectedZxPolyAsImage.setText("Copy selection (zxpoly)");
    menuEditCopySelectedZxPolyAsImage
            .addActionListener(this::menuEditCopySelectedZxPolyAsImageActionPerformed);
    menuEdit.add(menuEditCopySelectedZxPolyAsImage);

    menuEditCopySelectedBaseAsImage.setText("Copy selection (base)");
    menuEditCopySelectedBaseAsImage
            .addActionListener(this::menuEditCopySelectedBaseAsImageActionPerformed);
    menuEdit.add(menuEditCopySelectedBaseAsImage);

    menuEditPasteImage.setAccelerator(javax.swing.KeyStroke
            .getKeyStroke(java.awt.event.KeyEvent.VK_V,
                    java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    menuEditPasteImage.setText("Paste image");
    menuEditPasteImage.addActionListener(this::menuEditPasteImageActionPerformed);
    menuEdit.add(menuEditPasteImage);
    menuEdit.add(jSeparator7);

    menuEditCopyBaseToPlans.setText("Copy base to all plans");
    menuEditCopyBaseToPlans.addActionListener(this::menuEditCopyBaseToPlansActionPerformed);
    menuEdit.add(menuEditCopyBaseToPlans);

    menuEditClear.setText("Clear");
    menuEditClear.addActionListener(this::menuEditClearActionPerformed);
    menuEdit.add(menuEditClear);

    menuBar.add(menuEdit);

    menuViewZoom.add(menuViewZoomIn);
    menuViewZoom.add(menuViewZoomOut);
    menuView.add(menuViewZoom);

    menuBar.add(menuView);

    menuOptions.setText("Options");

    menuOptionsGrid.setSelected(true);
    menuOptionsGrid.setText("Grid");
    menuOptionsGrid.addActionListener(this::menuOptionsGridActionPerformed);
    menuOptions.add(menuOptionsGrid);

    menuOptionsColumns.setSelected(true);
    menuOptionsColumns.setText("Columns");
    menuOptionsColumns.addActionListener(this::menuOptionsColumnsActionPerformed);
    menuOptions.add(menuOptionsColumns);

    menuOptionsInvertBase.setSelected(true);
    menuOptionsInvertBase.setText("Invert base");
    menuOptionsInvertBase.addActionListener(this::menuOptionsInvertBaseActionPerformed);
    menuOptions.add(menuOptionsInvertBase);
    menuOptions.add(jSeparator5);

    menuOptionsZXScreen
            .setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, 0));
    menuOptionsZXScreen.setSelected(true);
    menuOptionsZXScreen.setText("ZX-Screen addressing");
    menuOptionsZXScreen.addChangeListener(this::menuOptionsZXScreenStateChanged);
    menuOptions.add(menuOptionsZXScreen);

    menuOptionsMode512.setSelected(true);
    menuOptionsMode512.setText("512 video mode");
    menuOptionsMode512.addChangeListener(this::menuOptionsMode512StateChanged);
    menuOptions.add(menuOptionsMode512);
    menuOptions.add(jSeparator6);

    menuOptionDontShowAttributes.setAccelerator(javax.swing.KeyStroke
            .getKeyStroke(java.awt.event.KeyEvent.VK_A,
                    java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    attributesButtonGroup.add(menuOptionDontShowAttributes);
    menuOptionDontShowAttributes.setSelected(true);
    menuOptionDontShowAttributes.setText("Don't show attribute colors");
    menuOptionDontShowAttributes
            .addActionListener(this::menuOptionDontShowAttributesActionPerformed);
    menuOptions.add(menuOptionDontShowAttributes);

    menuOptionsShowBaseAttributes.setAccelerator(javax.swing.KeyStroke
            .getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
    attributesButtonGroup.add(menuOptionsShowBaseAttributes);
    menuOptionsShowBaseAttributes.setText("Show attribute colors");
    menuOptionsShowBaseAttributes
            .addActionListener(this::menuOptionsShowBaseAttributesActionPerformed);
    menuOptions.add(menuOptionsShowBaseAttributes);

    menuOptionsShow512x384Attributes.setAccelerator(javax.swing.KeyStroke
            .getKeyStroke(java.awt.event.KeyEvent.VK_A,
                    java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    attributesButtonGroup.add(menuOptionsShow512x384Attributes);
    menuOptionsShow512x384Attributes.setText("Show 512x384 plane attributes");
    menuOptionsShow512x384Attributes
            .addActionListener(this::menuOptionsShow512x384AttributesActionPerformed);
    menuOptions.add(menuOptionsShow512x384Attributes);
    menuOptions.add(jSeparator3);

    columnModeGroup.add(menuOptionsColumnsAll);
    menuOptionsColumnsAll.setSelected(true);
    menuOptionsColumnsAll.setText("All columns");
    menuOptionsColumnsAll.addActionListener(this::menuOptionsColumnsAllActionPerformed);
    menuOptions.add(menuOptionsColumnsAll);

    columnModeGroup.add(menuOptionsColumnsOdd);
    menuOptionsColumnsOdd.setText("Odd columns");
    menuOptionsColumnsOdd.addActionListener(this::menuOptionsColumnsOddActionPerformed);
    menuOptions.add(menuOptionsColumnsOdd);

    columnModeGroup.add(menuOptionsColumnsEven);
    menuOptionsColumnsEven.setText("Even columns");
    menuOptionsColumnsEven.addActionListener(this::menuOptionsColumnsEvenActionPerformed);
    menuOptions.add(menuOptionsColumnsEven);

    menuBar.add(menuOptions);

    menuHelp.setText("Help");

    menuHelpAbout.setText("About");
    menuHelpAbout.addActionListener(this::menuHelpAboutActionPerformed);
    menuHelp.add(menuHelpAbout);

    menuBar.add(menuHelp);

    setJMenuBar(menuBar);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING,
                            javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                            Short.MAX_VALUE)
                    .addGroup(layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(layout.createSequentialGroup()
                                            .addGroup(
                                                    layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                            .addGroup(layout.createSequentialGroup()
                                                                    .addComponent(jScrollPane1)
                                                                    .addPreferredGap(
                                                                            javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                                                    .addComponent(scrollBarAddress,
                                                                            javax.swing.GroupLayout.PREFERRED_SIZE,
                                                                            javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                            javax.swing.GroupLayout.PREFERRED_SIZE))
                                                            .addComponent(colorSelector, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                                    javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                            .addGroup(layout
                                                    .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                    .addComponent(panelTools, javax.swing.GroupLayout.Alignment.TRAILING,
                                                            javax.swing.GroupLayout.PREFERRED_SIZE, 107,
                                                            javax.swing.GroupLayout.PREFERRED_SIZE)
                                                    .addComponent(sliderPenWidth, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                            javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                                    .addGroup(layout.createSequentialGroup()
                                            .addComponent(sliderColumns, javax.swing.GroupLayout.DEFAULT_SIZE,
                                                    javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                            .addGap(29, 29, 29)
                                            .addGroup(layout
                                                    .createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                                    .addComponent(buttonLock, javax.swing.GroupLayout.DEFAULT_SIZE, 107,
                                                            Short.MAX_VALUE)
                                                    .addComponent(spinnerCurrentAddress))))
                            .addContainerGap())
    );
    layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                            .addContainerGap()
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                    .addComponent(scrollBarAddress, javax.swing.GroupLayout.DEFAULT_SIZE,
                                            javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING,
                                            javax.swing.GroupLayout.DEFAULT_SIZE, 399, Short.MAX_VALUE)
                                    .addComponent(panelTools, javax.swing.GroupLayout.Alignment.TRAILING,
                                            javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
                                            Short.MAX_VALUE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                            .addComponent(sliderColumns, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                    javax.swing.GroupLayout.PREFERRED_SIZE)
                                            .addComponent(spinnerCurrentAddress, javax.swing.GroupLayout.PREFERRED_SIZE,
                                                    javax.swing.GroupLayout.DEFAULT_SIZE,
                                                    javax.swing.GroupLayout.PREFERRED_SIZE))
                                    .addGroup(layout.createSequentialGroup()
                                            .addComponent(buttonLock)
                                            .addGap(27, 27, 27)))
                            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                                    .addComponent(colorSelector, javax.swing.GroupLayout.PREFERRED_SIZE,
                                            javax.swing.GroupLayout.DEFAULT_SIZE,
                                            javax.swing.GroupLayout.PREFERRED_SIZE)
                                    .addComponent(sliderPenWidth, javax.swing.GroupLayout.PREFERRED_SIZE,
                                            javax.swing.GroupLayout.DEFAULT_SIZE,
                                            javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE,
                                    javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
    );

    layout.linkSize(javax.swing.SwingConstants.VERTICAL, colorSelector, sliderPenWidth);

    getAccessibleContext().setAccessibleName("ZX-Poly Sprite corrector");

    pack();
  }

  private void openSzeFileForPath(final String path) {
    final File file = new File(path);
    if (file.isFile()) {
      if (this.mainEditor.isChanged()) {
        if (showConfirmDialog(this, "Open file '" + file.getName() + "'?", "Confirmation",
                JOptionPane.OK_CANCEL_OPTION) == JOptionPane.CANCEL_OPTION) {
          return;
        }
      }

      final SZEPlugin szePlugin = container.getComponent(SZEPlugin.class);

      try {
        final String name = file.getName();
        loadFileWithPlugin(szePlugin, file, name, FileUtils.readFileToByteArray(file), -1);
      } catch (IOException ex) {
        JOptionPane.showMessageDialog(this, ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
      }
    } else {
      JOptionPane.showMessageDialog(this, "Can't find file '" + path + '\'', "Error",
              JOptionPane.ERROR_MESSAGE);
    }
  }

  private List<String> getRecentProjects() {
    final Preferences preferences = Preferences.userNodeForPackage(SpriteCorrectorMainFrame.class);
    final String list = preferences.get("recent-projects", "");
    return Arrays.stream(list.split("\\n"))
            .map(String::trim)
            .filter(x -> !x.isEmpty())
            .collect(toList());
  }

  private synchronized void addSzeProjectToRecentProjects(final File file) {
    try {
      List<String> recentProjects = new ArrayList<>(getRecentProjects());
      recentProjects.remove(file.getAbsolutePath());
      recentProjects.add(0, file.getAbsolutePath());
      final String newValue = recentProjects.stream().limit(10).collect(joining("\n"));

      final Preferences preferences = Preferences.userNodeForPackage(SpriteCorrectorMainFrame.class);
      preferences.put("recent-projects", newValue);
      preferences.flush();
    } catch (Exception ex) {
      LOGGER.log(Level.SEVERE, "Can't save recent project info: " + ex.getMessage(), ex);
    }
  }

  private void menuFileExitActionPerformed(java.awt.event.ActionEvent evt) {
    dispose();
  }

  private void applicationClosing(java.awt.event.WindowEvent evt) {
    if (this.mainEditor.hasData()) {
      if (showConfirmDialog(this, "Close application?", "Confirmation",
              JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
        return;
      }
    }
    dispose();
  }

  private void buttonLockActionPerformed(java.awt.event.ActionEvent evt) {
    if (this.buttonLock.isSelected()) {
      this.scrollBarAddress.setEnabled(false);
      this.sliderColumns.setEnabled(false);
    } else {
      if (this.mainEditor.getProcessingData() != null) {
        this.scrollBarAddress.setEnabled(true);
      }
      this.sliderColumns.setEnabled(true);
    }
  }

  private void menuHelpAboutActionPerformed(java.awt.event.ActionEvent evt) {
    new AboutDialog(this).setVisible(true);
  }

  private void mainEditorPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
    if (this.selectAreaMode) {
      this.selectAreaMode = false;
      this.mainEditor.resetSelectArea();
    } else if (evt.getModifiersEx() == MouseWheelEvent.CTRL_DOWN_MASK) {
      if (evt.getWheelRotation() < 0) {
        this.mainEditor.zoomIn();
      } else {
        this.mainEditor.zoomOut();
      }
    }
    updateBottomBar();
  }

  private void sliderColumnsStateChanged(javax.swing.event.ChangeEvent evt) {
    final int columnIndex = this.sliderColumns.getValue();
    final int columns;
    switch (this.mainEditor.getColumnMode()) {
      case EVEN:
      case ODD:
        columns = columnIndex * 2;
        break;
      case ALL:
      default:
        columns = columnIndex;
        break;
    }
    this.mainEditor.setColumns(columns);
    updateAddressScrollBar();
  }

  private void setCurrentSZEFile(final File file) {
    this.szeFile = file;
    this.menuSave.setEnabled(file != null);
  }

  public void loadFileWithPlugin(final AbstractFilePlugin plugin, final File sourceFile, final String name, final byte[] data,
                                 final int selected) throws IOException {
    final AbstractFilePlugin.ReadResult result = plugin.readFrom(name, data, selected);
    this.setTitle(name);
    this.mainEditor.setProcessingData(result.getData());
    if (result.getSessionData() != null) {
      loadStateFromSession(result.getSessionData());
    } else {
      resetOptions();
    }
    this.mainEditor.setChanged(false);

    setCurrentSZEFile(plugin instanceof SZEPlugin ? sourceFile : null);

    if ((plugin instanceof SCRPlugin) && !this.menuOptionsZXScreen.isSelected()) {
      this.menuOptionsZXScreen.setSelected(true);
    }
  }

  public Optional<AbstractFilePlugin> findImportFilePlugin(final String extension) {
    return this.container.getComponents(AbstractFilePlugin.class)
            .stream()
            .filter(x -> !(x instanceof LegacySZEPlugin))
            .filter(AbstractFilePlugin::isImportable)
            .filter(x -> extension.equalsIgnoreCase(x.getExtension(false)))
            .findFirst();
  }

  private void menuFileOpenActionPerformed(java.awt.event.ActionEvent evt) {
    try {
      this.toolsButtonGroup.clearSelection();

      final JFileChooser chooser = new JFileChooser(this.lastOpenedFile);
      chooser.setAcceptAllFileFilterUsed(false);

      container.getComponents(AbstractFilePlugin.class)
              .stream()
              .filter(AbstractFilePlugin::isImportable)
              .forEach((plugin) -> chooser.addChoosableFileFilter(plugin.getImportFileFilter()));

      final InsideFileView insideFileView = new InsideFileView(chooser);
      chooser.setAccessory(insideFileView);

      if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        final AbstractFilePlugin plugin = (AbstractFilePlugin) chooser.getFileFilter();
        final File selectedFile = chooser.getSelectedFile();
        this.lastOpenedFile = selectedFile;

        try {
          int selected = -1;
          if (plugin.doesContainInternalFileItems()) {
            final SelectInsideDataDialog itemSelector =
                    new SelectInsideDataDialog(this, selectedFile, plugin);
            itemSelector.setVisible(true);
            selected = itemSelector.getSelectedIndex();
            if (selected < 0) {
              return;
            }
          }
          final String name = selectedFile.getName();
          final byte[] data = FileUtils.readFileToByteArray(selectedFile);
          loadFileWithPlugin(plugin, selectedFile, name, data, selected);
          if (plugin instanceof SZEPlugin) {
            this.addSzeProjectToRecentProjects(selectedFile);
          }
        } catch (IllegalArgumentException ex) {
          ex.printStackTrace();
          JOptionPane
                  .showMessageDialog(this, ex.getMessage(), "Can't read", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
          ex.printStackTrace();
          JOptionPane
                  .showMessageDialog(this, "Can't read file or its part [" + ex.getMessage() + ']',
                          "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(this, "Unexpected exception! See log!", "Unexpected error",
                  JOptionPane.ERROR_MESSAGE);
        } finally {
          updateAddressScrollBar();
          updateRedoUndo();
        }
      }
    } finally {
      menuEditMenuSelected(null);
    }
  }

  public void updateAndResetEditMenu() {
    updateAddressScrollBar();
    updateRedoUndo();
    menuEditMenuSelected(null);
  }

  private void scrollBarAddressAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
    final int address = evt.getValue();
    this.mainEditor.setAddress(address);
  }

  private void processCurrentToolForPoint(final int buttons) {
    final Rectangle toolRect = this.mainEditor.getToolArea();

    if (toolRect != null) {
      final ToolButtonModel tool = (ToolButtonModel) this.toolsButtonGroup.getSelection();
      if (tool != null) {
        tool.getTool().process(this.mainEditor, toolRect, buttons);
      }
    }
  }

  private Point mouseCoord2EditorCoord(final MouseEvent evt) {
    return this.mainEditor.mousePoint2ScreenPoint(
            SwingUtilities.convertPoint(this.mainEditorPanel, evt.getPoint(), this.mainEditor));
  }

  private Rectangle updateToolRectangle(final Point editorPoint) {
    final int width = this.sliderPenWidth.getValue();
    final Rectangle rect;
    if (width <= 1) {
      rect = new Rectangle(editorPoint.x, editorPoint.y, 1, 1);
    } else {
      rect =
              new Rectangle(editorPoint.x - (width >> 1), editorPoint.y - (width >> 1), width, width);
    }

    if (this.currentAbstractTool.get() == null) {
      this.mainEditor.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
      this.mainEditor.setCursorPoint(rect.getLocation());
      this.mainEditor.setToolArea(null);
    } else {
      this.mainEditor.setCursor(CURSOR_BLANK);
      this.mainEditor.setCursorPoint(rect.getLocation());
      this.mainEditor.setToolArea(rect);
    }

    setLabelAddress(this.mainEditor.getZXGraphics().coordToAddress(rect.x, rect.y));

    return rect;
  }

  private void setLabelAddress(final int address) {
    if (address < 0) {
      this.labelAddress.setText("Addr: ----");
    } else {
      this.labelAddress.setText("Addr: " + address + " (" + toHex(address) + ')');
    }
  }

  private void updateRedoUndo() {
    this.menuEditRedo.setEnabled(this.mainEditor.hasRedo());
    this.menuEditUndo.setEnabled(this.mainEditor.hasUndo());
  }

  private int extractButtons(final MouseEvent event) {
    int result = AbstractTool.BUTTON_NONE;
    if ((event.getModifiersEx() & MouseEvent.CTRL_DOWN_MASK) != 0) {
      result |= AbstractTool.BUTTON_CTRL;
    }
    if ((event.getModifiersEx() & MouseEvent.ALT_DOWN_MASK) != 0) {
      result |= AbstractTool.BUTTON_ALT;
    }
    if ((event.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0) {
      result |= AbstractTool.BUTTON_SHIFT;
    }
    if ((event.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0) {
      result |= AbstractTool.BUTTON_MOUSE_LEFT;
    }
    if ((event.getModifiersEx() & MouseEvent.BUTTON2_DOWN_MASK) != 0) {
      result |= AbstractTool.BUTTON_MOUSE_MIDDLE;
    }
    if ((event.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
      result |= AbstractTool.BUTTON_MOUSE_RIGHT;
    }
    return result;
  }

  private void mainEditorPanelMousePressed(java.awt.event.MouseEvent evt) {
    this.mainEditor.addUndo();
    updateRedoUndo();

    if (this.selectAreaMode) {
      this.mainEditor.startSelectArea(mouseCoord2EditorCoord(evt));
    } else {
      updateToolRectangle(mouseCoord2EditorCoord(evt));
      processCurrentToolForPoint(extractButtons(evt));
    }
  }

  private void mainEditorPanelMouseMoved(java.awt.event.MouseEvent evt) {
    updateToolRectangle(mouseCoord2EditorCoord(evt));
  }

  private void mainEditorPanelMouseExited(java.awt.event.MouseEvent evt) {
    this.mainEditor.setToolArea(null);
  }

  private void mainEditorPanelMouseEntered(java.awt.event.MouseEvent evt) {
    updateToolRectangle(mouseCoord2EditorCoord(evt));
  }

  private void mainEditorPanelMouseDragged(java.awt.event.MouseEvent evt) {
    if (this.selectAreaMode) {
      this.mainEditor.updateSelectArea(mouseCoord2EditorCoord(evt));
    } else {
      updateToolRectangle(mouseCoord2EditorCoord(evt));
      processCurrentToolForPoint(extractButtons(evt));
    }
  }

  private void menuOptionsMode512StateChanged(javax.swing.event.ChangeEvent evt) {
    this.mainEditor.resetSelectArea();
    this.mainEditor.setMode512(this.menuOptionsMode512.isSelected());
  }

  private void menuOptionsZXScreenStateChanged(javax.swing.event.ChangeEvent evt) {
    this.mainEditor.setZXScreenMode(this.menuOptionsZXScreen.isSelected());
  }

  private void menuOptionsInvertBaseActionPerformed(java.awt.event.ActionEvent evt) {
    this.mainEditor.setInvertShowBaseData(this.menuOptionsInvertBase.isSelected());
  }

  private void menuOptionsColumnsActionPerformed(java.awt.event.ActionEvent evt) {
    this.mainEditor.setShowColumnBorders(this.menuOptionsColumns.isSelected());
  }

  private void menuOptionsGridActionPerformed(java.awt.event.ActionEvent evt) {
    this.mainEditor.setShowGrid(this.menuOptionsGrid.isSelected());
  }

  private void menuEditUndoActionPerformed(java.awt.event.ActionEvent evt) {
    this.mainEditor.undo();
    updateRedoUndo();
  }

  private void menuEditRedoActionPerformed(java.awt.event.ActionEvent evt) {
    this.mainEditor.redo();
    updateRedoUndo();
  }

  private void menuEditClearActionPerformed(java.awt.event.ActionEvent evt) {
    if (showConfirmDialog(this, "Clear ZX-Poly data?", "Confirmation",
            JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
      this.mainEditor.clear();
    }
  }

  private void menuOptionDontShowAttributesActionPerformed(java.awt.event.ActionEvent evt) {
    this.mainEditor.setShowAttributes(EditorComponent.AttributeMode.DONT_SHOW);
  }

  private void menuOptionsShowBaseAttributesActionPerformed(java.awt.event.ActionEvent evt) {
    this.mainEditor.setShowAttributes(EditorComponent.AttributeMode.SHOW_BASE);
  }

  private void menuFileSaveAsActionPerformed(java.awt.event.ActionEvent evt) {
    if (this.mainEditor.hasData()) {
      final ZXPolyData zxpolydata = this.mainEditor.getProcessingData();

      final JFileChooser fileChoolser = new JFileChooser(this.lastOpenedFile);
      fileChoolser.setAcceptAllFileFilterUsed(false);
      fileChoolser.addChoosableFileFilter(container.getComponent(SZEPlugin.class));
      if (fileChoolser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        try {
          final File thefile = ensureExtension(fileChoolser.getSelectedFile(),
                  container.getComponent(SZEPlugin.class));
          container.getComponent(SZEPlugin.class)
                  .writeTo(thefile, zxpolydata, makeCurrentSessionData());
          this.mainEditor.setChanged(false);
          this.setTitle(thefile.getAbsolutePath());
          setCurrentSZEFile(thefile);
        } catch (Exception ex) {
          ex.printStackTrace();
          JOptionPane
                  .showMessageDialog(this, "Error during operation [" + ex.getMessage() + ']', "Error",
                          JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }

  private void menuOptionsShow512x384AttributesActionPerformed(java.awt.event.ActionEvent evt) {
    this.mainEditor.setShowAttributes(EditorComponent.AttributeMode.SHOW_512x384_ZXPOLY_PLANES);
  }

  private void menuEditCopyBaseToPlansActionPerformed(java.awt.event.ActionEvent evt) {
    if (showConfirmDialog(this, "Do you really want to copy base data to all ZX-Poly planes?",
            "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
      this.mainEditor.copyPlansFromBase();
    }
  }

  private SessionData makeCurrentSessionData() {
    final SessionData result = new SessionData(this.mainEditor);
    result.setExtraProperty(EXTRA_PROPERTY_DATA_ID, serializeProperties(lastSpec256Properties));
    result.setExtraProperty(EXTRA_PROPERTY_OVERRIDE_CPU_DATA,
        serializeProperties(this.snapshotCpuOverrideValues));
    return result;
  }

  private void menuSaveActionPerformed(java.awt.event.ActionEvent evt) {
    try {
      container.getComponent(SZEPlugin.class)
              .writeTo(this.szeFile, this.mainEditor.getProcessingData(),
                      makeCurrentSessionData());
      this.mainEditor.setChanged(false);
    } catch (Exception ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(this, "Can't save file for exception [" + ex.getMessage() + ']',
              "Error", JOptionPane.ERROR_MESSAGE);
    }
  }

  private void menuFileNewActionPerformed(java.awt.event.ActionEvent evt) {
    try {
      if (this.mainEditor.hasData()) {
        if (showConfirmDialog(this, "Do you really want to create new data?", "Confirmation",
                JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
          return;
        }
      }

      final NewDataDialog dialog = new NewDataDialog(this);
      dialog.setVisible(true);
      final AbstractFilePlugin.ReadResult result = dialog.getResult();
      if (result != null) {
        this.mainEditor.setProcessingData(result.getData());
        setCurrentSZEFile(null);
      }
      repaint();
    } finally {
      menuEditMenuSelected(null);
    }
  }

  private void menuOptionsColumnsAllActionPerformed(java.awt.event.ActionEvent evt) {
    this.mainEditor.setColumnMode(EditorComponent.ColumnMode.ALL);
    this.sliderColumns.setModel(SLIDER_ALL_MODEL);
    this.sliderColumns.setLabelTable(SLIDER_ALL_LABELS);
    this.sliderColumns.setValue(this.mainEditor.getColumns());
  }

  private void menuOptionsColumnsOddActionPerformed(java.awt.event.ActionEvent evt) {
    this.mainEditor.setColumnMode(EditorComponent.ColumnMode.ODD);
    this.sliderColumns.setModel(SLIDER_ODD_OR_EVEN_MODEL);
    this.sliderColumns.setLabelTable(SLIDER_ODD_LABELS);
    this.sliderColumns.setValue(this.mainEditor.getColumns() / 2);
  }

  private void menuOptionsColumnsEvenActionPerformed(java.awt.event.ActionEvent evt) {
    this.mainEditor.setColumnMode(EditorComponent.ColumnMode.EVEN);
    this.sliderColumns.setModel(SLIDER_ODD_OR_EVEN_MODEL);
    this.sliderColumns.setLabelTable(SLIDER_EVEN_LABELS);
    this.sliderColumns.setValue(this.mainEditor.getColumns() / 2);
  }

  private void mainEditorPanelMouseReleased(java.awt.event.MouseEvent evt) {
    if (this.selectAreaMode) {
      this.selectAreaMode = false;
      this.mainEditor.endSelectArea(mouseCoord2EditorCoord(evt));
      this.menuEditMenuSelected(null);
    }
  }

  private void deactivateCurrentTool() {
    this.toolsButtonGroup.clearSelection();
  }

  private void menuEditSelectAreaActionPerformed(java.awt.event.ActionEvent evt) {
    deactivateCurrentTool();
    this.mainEditor.setDraggedImage(null);
    this.selectAreaMode = true;
    this.mainEditor.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    this.mainEditor.addUndo();
    this.mainEditor.resetSelectArea();
  }

  private void menuEditCopySelectedBaseAsImageActionPerformed(java.awt.event.ActionEvent evt) {
    final Image selectedAreaImage = this.mainEditor.getSelectedAreaAsImage(true);
    if (selectedAreaImage != null) {
      new TransferableImage(selectedAreaImage).toClipboard();
    }
  }

  private void menuEditCopySelectedZxPolyAsImageActionPerformed(java.awt.event.ActionEvent evt) {
    final Image selectedAreaImage = this.mainEditor.getSelectedAreaAsImage(false);
    if (selectedAreaImage != null) {
      new TransferableImage(selectedAreaImage).toClipboard();
    }
  }

  private void onActionMenuEditEditStartParameters(final ActionEvent actionEvent) {
    try {
      final CpuSnapshotParamsEditor panel =
          new CpuSnapshotParamsEditor(this.mainEditor.getProcessingData(),
              this.snapshotCpuOverrideValues);
      if (JOptionPane.showConfirmDialog(this, panel, "Edit start parameters",
          JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == OK_OPTION) {
        this.snapshotCpuOverrideValues = panel.asProperties();
      }
    } catch (IOException ex) {
      JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage(), "Error",
          JOptionPane.ERROR_MESSAGE);
    }
  }

  private void menuEditMenuSelected(javax.swing.event.MenuEvent evt) {
    this.toolsButtonGroup.clearSelection();
    this.menuEditSelectArea.setEnabled(this.mainEditor.hasData());
    this.menuEditPasteImage.setEnabled(GfxUtils.doesClipboardHasImage());
    this.menuEditCopySelectedBaseAsImage.setEnabled(this.mainEditor.hasSelectedArea());
    this.menuEditCopySelectedZxPolyAsImage.setEnabled(this.mainEditor.hasSelectedArea());
    this.menuEditEditStartParameters.setEnabled(this.mainEditor.getProcessingData() != null &&
        this.mainEditor.getProcessingData().getPlugin() instanceof Z80InZXPOutPlugin);
  }

  private void mainEditorPanelMouseClicked(java.awt.event.MouseEvent evt) {
    if (this.mainEditor.hasDraggedImage()) {
      if (evt.getClickCount() > 1 && evt.getButton() == MouseEvent.BUTTON1) {
        this.mainEditor.doStampDraggedImage();
        evt.consume();
      } else if (evt.getButton() == MouseEvent.BUTTON3) {
        this.mainEditor.setDraggedImage(null);
        evt.consume();
      }
    }
  }

  private void menuEditPasteImageActionPerformed(java.awt.event.ActionEvent evt) {
    final Image image = GfxUtils.getImageFromClipboard();
    if (image != null) {
      this.toolsButtonGroup.clearSelection();
      this.mainEditor.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
      this.mainEditor.setToolArea(null);
      this.mainEditor.setDraggedImage(image);
    }
  }

  private void menuEditMenuDeselected(javax.swing.event.MenuEvent evt) {
    this.menuEditSelectArea.setEnabled(this.mainEditor.hasData());
    this.menuEditPasteImage.setEnabled(this.mainEditor.hasData());
    this.menuEditCopySelectedBaseAsImage.setEnabled(this.mainEditor.hasData());
    this.menuEditCopySelectedZxPolyAsImage.setEnabled(this.mainEditor.hasData());
  }

  private void updateAddressScrollBar() {
    this.sliderColumns.setEnabled(true);
    this.scrollBarAddress.setMinimum(0);
    if (this.mainEditor.getProcessingData() == null) {
      this.scrollBarAddress.setEnabled(false);
    } else {
      this.scrollBarAddress
              .setMaximum(Math.max(0, this.mainEditor.getProcessingData().length() - 32));
      this.scrollBarAddress.setEnabled(true);
      this.scrollBarAddress.setValue(this.mainEditor.getAddress());
      this.scrollBarAddress.setUnitIncrement(this.mainEditor.getColumns());
      this.scrollBarAddress.setBlockIncrement(this.mainEditor.getColumns() * 96);
      this.scrollBarAddress.setVisibleAmount(this.mainEditor.getColumns() * 192);
    }
    this.scrollBarAddress.repaint();
  }

}
