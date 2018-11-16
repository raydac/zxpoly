package com.igormaznitsa.zxpspritecorrector;

import com.igormaznitsa.zxpspritecorrector.files.plugins.AbstractFilePlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.TAPPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.Z80Plugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.SCRPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.SCLPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.TRDPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.HOBETAPlugin;
import com.igormaznitsa.zxpspritecorrector.files.plugins.SZEPlugin;
import com.igormaznitsa.zxpspritecorrector.components.*;
import com.igormaznitsa.zxpspritecorrector.files.*;
import com.igormaznitsa.zxpspritecorrector.tools.*;
import com.igormaznitsa.zxpspritecorrector.utils.GfxUtils;
import java.awt.*;
import java.io.*;
import java.util.Locale;
import javax.swing.*;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import org.apache.commons.io.FilenameUtils;
import org.picocontainer.*;
import org.picocontainer.injectors.*;
import com.igormaznitsa.zxpspritecorrector.files.plugins.SNA48Plugin;
import com.igormaznitsa.zxpspritecorrector.utils.TransferableImage;
import java.awt.event.ItemEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

public final class MainFrame extends javax.swing.JFrame {

  public final MutablePicoContainer container = new PicoBuilder()
          .withAutomatic()
          .withAnnotatedMethodInjection()
          .withAnnotatedFieldInjection()
          .withConstructorInjection()
          .withCaching()
          .build();

  private static final long serialVersionUID = -5031012548284731523L;

  private File lastOpenedFile;
  private File lastExportedFile;
  private File szeFile;

  private boolean selectAreaMode = false;

  private final Cursor CURSOR_BLANK = Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB), new Point(0, 0), "blank cursor");

  private final AtomicReference<AbstractTool> currentAbstractTool = new AtomicReference<>();

  public MainFrame() {
    initComponents();

    this.sliderColumns.setModel(new DefaultBoundedRangeModel(32, 0, 1, 32));
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
    this.container.addComponent(Z80Plugin.class);
    this.container.addComponent(SNA48Plugin.class);

    this.container.addComponent(ToolPencil.class);
    this.container.addComponent(ToolEraser.class);
    this.container.addComponent(ToolColorizer.class);

    this.container.start();

    this.container.getComponents(AbstractTool.class).stream().forEachOrdered(tool -> {
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
            .filter((p) -> !(!p.allowsExport()))
            .forEachOrdered((p) -> {
              final JMenuItem menuItem = new JMenuItem(p.getPluginDescription(true));
              this.menuFileExportAs.add(menuItem);
              menuItem.setToolTipText(p.getToolTip(true));
              menuItem.addActionListener(e -> {
                exportDataWithPlugin(p);
              });
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
      this.container.getComponents(AbstractTool.class).forEach((t) -> {
        t.setEnabled(!mode512 || (mode512 && t.doesSupport512x384()));
      });
    });

    resetOptions();
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
      JOptionPane.showMessageDialog(this, "There is no data to export!", "There is no data", JOptionPane.WARNING_MESSAGE);
      return;
    }

    final JFileChooser fileChooser = new JFileChooser(this.lastExportedFile);
    fileChooser.setAcceptAllFileFilterUsed(false);
    fileChooser.addChoosableFileFilter(plugin.getExportFileFilter());
    if (fileChooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
      this.lastExportedFile = ensureExtension(fileChooser.getSelectedFile(), plugin);
      try {
        plugin.writeTo(this.lastExportedFile, this.mainEditor.getProcessingData(), new SessionData(this.mainEditor));
      } catch (Exception ex) {
        ex.printStackTrace();
        JOptionPane.showMessageDialog(this, "Can't export data for exception [" + ex.getMessage() + ']', "Error", JOptionPane.ERROR_MESSAGE);
      }
    }
  }

  private void updateBottomBar() {
    this.labelZoom.setText("x" + this.mainEditor.getZoom());
  }

  private void loadStateFromSession(final SessionData sessionData) {
    final int address = sessionData.getBaseAddress();

    sessionData.fill(this.mainEditor);

    this.menuOptionsColumnsAll.setSelected(true);
    this.menuOptionsZXScreen.setSelected(sessionData.isZXAddressing());
    this.menuOptionsColumns.setSelected(sessionData.isShowColumns());
    this.menuOptionsGrid.setSelected(sessionData.isShowGrid());
    this.menuOptionsInvertBase.setSelected(sessionData.isInvertBaseShow());
    this.menuOptionsMode512.setSelected(sessionData.is512Mode());
    this.sliderColumns.setValue(sessionData.getColumnNumber());
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

    this.scrollBarAddress.setValue(address);

    updateBottomBar();
    refreshMenuAndToolState();
  }

  /**
   * This method is called from within the constructor to initialize the form.
   * WARNING: Do NOT modify this code. The content of this method is always
   * regenerated by the Form Editor.
   */
  // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
  private void initComponents() {
    java.awt.GridBagConstraints gridBagConstraints;

    toolsButtonGroup = new javax.swing.ButtonGroup();
    attributesButtonGroup = new javax.swing.ButtonGroup();
    columnModeGroup = new javax.swing.ButtonGroup();
    scrollBarAddress = new javax.swing.JScrollBar();
    sliderColumns = new javax.swing.JSlider();
    buttonLock = new javax.swing.JToggleButton();
    panelTools = new javax.swing.JPanel();
    colorSelector = new com.igormaznitsa.zxpspritecorrector.components.ZXColorSelector();
    sliderPenWidth = new com.igormaznitsa.zxpspritecorrector.components.PenWidth();
    jScrollPane1 = new javax.swing.JScrollPane();
    mainEditorPanel = new javax.swing.JPanel();
    mainEditor = new com.igormaznitsa.zxpspritecorrector.components.EditorComponent();
    jPanel2 = new javax.swing.JPanel();
    labelZoom = new javax.swing.JLabel();
    filler1 = new javax.swing.Box.Filler(new java.awt.Dimension(0, 0), new java.awt.Dimension(0, 0), new java.awt.Dimension(32767, 0));
    labelAddress = new javax.swing.JLabel();
    spinnerCurrentAddress = new javax.swing.JSpinner();
    menuBar = new javax.swing.JMenuBar();
    menuFile = new javax.swing.JMenu();
    menuFileNew = new javax.swing.JMenuItem();
    menuFileOpen = new javax.swing.JMenuItem();
    menuSave = new javax.swing.JMenuItem();
    menuFileSaveAs = new javax.swing.JMenuItem();
    jSeparator4 = new javax.swing.JPopupMenu.Separator();
    menuFileExportAs = new javax.swing.JMenu();
    jSeparator1 = new javax.swing.JSeparator();
    menuFileExit = new javax.swing.JMenuItem();
    menuEdit = new javax.swing.JMenu();
    menuEditUndo = new javax.swing.JMenuItem();
    menuEditRedo = new javax.swing.JMenuItem();
    jSeparator2 = new javax.swing.JPopupMenu.Separator();
    menuEditSelectArea = new javax.swing.JMenuItem();
    menuEditCopySelectedZxPolyAsImage = new javax.swing.JMenuItem();
    menuEditCopySelectedBaseAsImage = new javax.swing.JMenuItem();
    menuEditPasteImage = new javax.swing.JMenuItem();
    jSeparator7 = new javax.swing.JPopupMenu.Separator();
    menuEditCopyBaseToPlans = new javax.swing.JMenuItem();
    menuEditClear = new javax.swing.JMenuItem();
    menuOptions = new javax.swing.JMenu();
    menuOptionsGrid = new javax.swing.JCheckBoxMenuItem();
    menuOptionsColumns = new javax.swing.JCheckBoxMenuItem();
    menuOptionsInvertBase = new javax.swing.JCheckBoxMenuItem();
    jSeparator5 = new javax.swing.JPopupMenu.Separator();
    menuOptionsZXScreen = new javax.swing.JCheckBoxMenuItem();
    menuOptionsMode512 = new javax.swing.JCheckBoxMenuItem();
    jSeparator6 = new javax.swing.JPopupMenu.Separator();
    menuOptionDontShowAttributes = new javax.swing.JRadioButtonMenuItem();
    menuOptionsShowBaseAttributes = new javax.swing.JRadioButtonMenuItem();
    menuOptionsShow512x384Attributes = new javax.swing.JRadioButtonMenuItem();
    jSeparator3 = new javax.swing.JPopupMenu.Separator();
    menuOptionsColumnsAll = new javax.swing.JRadioButtonMenuItem();
    menuOptionsColumnsOdd = new javax.swing.JRadioButtonMenuItem();
    menuOptionsColumnsEven = new javax.swing.JRadioButtonMenuItem();
    menuHelp = new javax.swing.JMenu();
    menuHelpAbout = new javax.swing.JMenuItem();

    setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
    setTitle("ZX-Poly Sprite Corrector");
    addWindowListener(new java.awt.event.WindowAdapter() {
      public void windowClosing(java.awt.event.WindowEvent evt) {
        applicationClosing(evt);
      }
    });

    scrollBarAddress.setToolTipText("Memory window position");
    scrollBarAddress.setFocusable(false);
    scrollBarAddress.addAdjustmentListener(new java.awt.event.AdjustmentListener() {
      public void adjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {
        scrollBarAddressAdjustmentValueChanged(evt);
      }
    });

    sliderColumns.setMajorTickSpacing(1);
    sliderColumns.setMinorTickSpacing(1);
    sliderColumns.setPaintLabels(true);
    sliderColumns.setPaintTicks(true);
    sliderColumns.setSnapToTicks(true);
    sliderColumns.setToolTipText("Columns number");
    sliderColumns.setExtent(1);
    sliderColumns.setFocusable(false);
    sliderColumns.setValueIsAdjusting(true);
    sliderColumns.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        sliderColumnsStateChanged(evt);
      }
    });

    buttonLock.setText("LOCK");
    buttonLock.setToolTipText("To lock current memory position and cols number");
    buttonLock.setFocusable(false);
    buttonLock.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        buttonLockActionPerformed(evt);
      }
    });

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
    sliderPenWidth.setMaximumSize(new java.awt.Dimension(96, 84));
    sliderPenWidth.setMinimumSize(new java.awt.Dimension(96, 84));
    sliderPenWidth.setPreferredSize(new java.awt.Dimension(96, 84));

    mainEditorPanel.setCursor(new java.awt.Cursor(java.awt.Cursor.CROSSHAIR_CURSOR));
    mainEditorPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
      public void mouseMoved(java.awt.event.MouseEvent evt) {
        mainEditorPanelMouseMoved(evt);
      }
      public void mouseDragged(java.awt.event.MouseEvent evt) {
        mainEditorPanelMouseDragged(evt);
      }
    });
    mainEditorPanel.addMouseWheelListener(new java.awt.event.MouseWheelListener() {
      public void mouseWheelMoved(java.awt.event.MouseWheelEvent evt) {
        mainEditorPanelMouseWheelMoved(evt);
      }
    });
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
      .addGroup(mainEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(mainEditorPanelLayout.createSequentialGroup()
          .addGap(0, 0, Short.MAX_VALUE)
          .addComponent(mainEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addGap(0, 0, Short.MAX_VALUE)))
    );
    mainEditorPanelLayout.setVerticalGroup(
      mainEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGap(0, 394, Short.MAX_VALUE)
      .addGroup(mainEditorPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
        .addGroup(mainEditorPanelLayout.createSequentialGroup()
          .addGap(0, 0, Short.MAX_VALUE)
          .addComponent(mainEditor, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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
    menuFileNew.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileNewActionPerformed(evt);
      }
    });
    menuFile.add(menuFileNew);

    menuFileOpen.setText("Open");
    menuFileOpen.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileOpenActionPerformed(evt);
      }
    });
    menuFile.add(menuFileOpen);

    menuSave.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
    menuSave.setText("Save");
    menuSave.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuSaveActionPerformed(evt);
      }
    });
    menuFile.add(menuSave);

    menuFileSaveAs.setText("Save As");
    menuFileSaveAs.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileSaveAsActionPerformed(evt);
      }
    });
    menuFile.add(menuFileSaveAs);
    menuFile.add(jSeparator4);

    menuFileExportAs.setText("Export as..");
    menuFile.add(menuFileExportAs);
    menuFile.add(jSeparator1);

    menuFileExit.setText("Exit");
    menuFileExit.setToolTipText("Close application");
    menuFileExit.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuFileExitActionPerformed(evt);
      }
    });
    menuFile.add(menuFileExit);

    menuBar.add(menuFile);

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

    menuEditUndo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.CTRL_MASK));
    menuEditUndo.setText("Undo");
    menuEditUndo.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuEditUndoActionPerformed(evt);
      }
    });
    menuEdit.add(menuEditUndo);

    menuEditRedo.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    menuEditRedo.setText("Redo");
    menuEditRedo.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuEditRedoActionPerformed(evt);
      }
    });
    menuEdit.add(menuEditRedo);
    menuEdit.add(jSeparator2);

    menuEditSelectArea.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_R, java.awt.event.InputEvent.CTRL_MASK));
    menuEditSelectArea.setText("Select area");
    menuEditSelectArea.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuEditSelectAreaActionPerformed(evt);
      }
    });
    menuEdit.add(menuEditSelectArea);

    menuEditCopySelectedZxPolyAsImage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_C, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    menuEditCopySelectedZxPolyAsImage.setText("Copy selection (zxpoly)");
    menuEditCopySelectedZxPolyAsImage.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuEditCopySelectedZxPolyAsImageActionPerformed(evt);
      }
    });
    menuEdit.add(menuEditCopySelectedZxPolyAsImage);

    menuEditCopySelectedBaseAsImage.setText("Copy selection (base)");
    menuEditCopySelectedBaseAsImage.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuEditCopySelectedBaseAsImageActionPerformed(evt);
      }
    });
    menuEdit.add(menuEditCopySelectedBaseAsImage);

    menuEditPasteImage.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_V, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    menuEditPasteImage.setText("Paste image");
    menuEditPasteImage.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuEditPasteImageActionPerformed(evt);
      }
    });
    menuEdit.add(menuEditPasteImage);
    menuEdit.add(jSeparator7);

    menuEditCopyBaseToPlans.setText("Copy base to all plans");
    menuEditCopyBaseToPlans.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuEditCopyBaseToPlansActionPerformed(evt);
      }
    });
    menuEdit.add(menuEditCopyBaseToPlans);

    menuEditClear.setText("Clear");
    menuEditClear.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuEditClearActionPerformed(evt);
      }
    });
    menuEdit.add(menuEditClear);

    menuBar.add(menuEdit);

    menuOptions.setText("Options");

    menuOptionsGrid.setSelected(true);
    menuOptionsGrid.setText("Grid");
    menuOptionsGrid.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsGridActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsGrid);

    menuOptionsColumns.setSelected(true);
    menuOptionsColumns.setText("Columns");
    menuOptionsColumns.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsColumnsActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsColumns);

    menuOptionsInvertBase.setSelected(true);
    menuOptionsInvertBase.setText("Invert base");
    menuOptionsInvertBase.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsInvertBaseActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsInvertBase);
    menuOptions.add(jSeparator5);

    menuOptionsZXScreen.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_Z, 0));
    menuOptionsZXScreen.setSelected(true);
    menuOptionsZXScreen.setText("ZX-Screen addressing");
    menuOptionsZXScreen.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        menuOptionsZXScreenStateChanged(evt);
      }
    });
    menuOptions.add(menuOptionsZXScreen);

    menuOptionsMode512.setSelected(true);
    menuOptionsMode512.setText("512 video mode");
    menuOptionsMode512.addChangeListener(new javax.swing.event.ChangeListener() {
      public void stateChanged(javax.swing.event.ChangeEvent evt) {
        menuOptionsMode512StateChanged(evt);
      }
    });
    menuOptions.add(menuOptionsMode512);
    menuOptions.add(jSeparator6);

    menuOptionDontShowAttributes.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.SHIFT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    attributesButtonGroup.add(menuOptionDontShowAttributes);
    menuOptionDontShowAttributes.setSelected(true);
    menuOptionDontShowAttributes.setText("Don't show attribute colors");
    menuOptionDontShowAttributes.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionDontShowAttributesActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionDontShowAttributes);

    menuOptionsShowBaseAttributes.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.CTRL_MASK));
    attributesButtonGroup.add(menuOptionsShowBaseAttributes);
    menuOptionsShowBaseAttributes.setText("Show attribute colors");
    menuOptionsShowBaseAttributes.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsShowBaseAttributesActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsShowBaseAttributes);

    menuOptionsShow512x384Attributes.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_A, java.awt.event.InputEvent.ALT_MASK | java.awt.event.InputEvent.CTRL_MASK));
    attributesButtonGroup.add(menuOptionsShow512x384Attributes);
    menuOptionsShow512x384Attributes.setText("Show 512x384 plane attributes");
    menuOptionsShow512x384Attributes.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsShow512x384AttributesActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsShow512x384Attributes);
    menuOptions.add(jSeparator3);

    columnModeGroup.add(menuOptionsColumnsAll);
    menuOptionsColumnsAll.setSelected(true);
    menuOptionsColumnsAll.setText("All columns");
    menuOptionsColumnsAll.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsColumnsAllActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsColumnsAll);

    columnModeGroup.add(menuOptionsColumnsOdd);
    menuOptionsColumnsOdd.setText("Odd columns");
    menuOptionsColumnsOdd.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsColumnsOddActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsColumnsOdd);

    columnModeGroup.add(menuOptionsColumnsEven);
    menuOptionsColumnsEven.setText("Even columns");
    menuOptionsColumnsEven.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuOptionsColumnsEvenActionPerformed(evt);
      }
    });
    menuOptions.add(menuOptionsColumnsEven);

    menuBar.add(menuOptions);

    menuHelp.setText("Help");

    menuHelpAbout.setText("About");
    menuHelpAbout.addActionListener(new java.awt.event.ActionListener() {
      public void actionPerformed(java.awt.event.ActionEvent evt) {
        menuHelpAboutActionPerformed(evt);
      }
    });
    menuHelp.add(menuHelpAbout);

    menuBar.add(menuHelp);

    setJMenuBar(menuBar);

    javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
    getContentPane().setLayout(layout);
    layout.setHorizontalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addComponent(jPanel2, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
      .addGroup(layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addGroup(layout.createSequentialGroup()
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
              .addGroup(layout.createSequentialGroup()
                .addComponent(jScrollPane1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(scrollBarAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
              .addComponent(colorSelector, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
              .addComponent(panelTools, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 107, javax.swing.GroupLayout.PREFERRED_SIZE)
              .addComponent(sliderPenWidth, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
          .addGroup(layout.createSequentialGroup()
            .addComponent(sliderColumns, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGap(29, 29, 29)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
              .addComponent(buttonLock, javax.swing.GroupLayout.DEFAULT_SIZE, 107, Short.MAX_VALUE)
              .addComponent(spinnerCurrentAddress))))
        .addContainerGap())
    );
    layout.setVerticalGroup(
      layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
      .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
        .addContainerGap()
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
          .addComponent(scrollBarAddress, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
          .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 399, Short.MAX_VALUE)
          .addComponent(panelTools, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
            .addComponent(sliderColumns, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
            .addComponent(spinnerCurrentAddress, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
          .addGroup(layout.createSequentialGroup()
            .addComponent(buttonLock)
            .addGap(27, 27, 27)))
        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
          .addComponent(colorSelector, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
          .addComponent(sliderPenWidth, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
        .addComponent(jPanel2, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
    );

    layout.linkSize(javax.swing.SwingConstants.VERTICAL, new java.awt.Component[] {colorSelector, sliderPenWidth});

    getAccessibleContext().setAccessibleName("ZX-Poly Sprite corrector");

    pack();
  }// </editor-fold>//GEN-END:initComponents
    private void menuFileExitActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileExitActionPerformed
      dispose();
    }//GEN-LAST:event_menuFileExitActionPerformed

    private void applicationClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_applicationClosing
      if (this.mainEditor.hasData()) {
        if (JOptionPane.showConfirmDialog(this, "Close application?", "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
          return;
        }
      }
      dispose();
    }//GEN-LAST:event_applicationClosing

        private void buttonLockActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_buttonLockActionPerformed
          if (this.buttonLock.isSelected()) {
            this.scrollBarAddress.setEnabled(false);
            this.sliderColumns.setEnabled(false);
          } else {
            if (this.mainEditor.getProcessingData() != null) {
              this.scrollBarAddress.setEnabled(true);
            }
            this.sliderColumns.setEnabled(true);
          }
}//GEN-LAST:event_buttonLockActionPerformed

        private void menuHelpAboutActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuHelpAboutActionPerformed
          new AboutDialog(this).setVisible(true);
        }//GEN-LAST:event_menuHelpAboutActionPerformed

  private void mainEditorPanelMouseWheelMoved(java.awt.event.MouseWheelEvent evt) {//GEN-FIRST:event_mainEditorPanelMouseWheelMoved
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
  }//GEN-LAST:event_mainEditorPanelMouseWheelMoved

  private void sliderColumnsStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_sliderColumnsStateChanged
    this.mainEditor.setColumns(this.sliderColumns.getValue());
    updateAddressScrollBar();
  }//GEN-LAST:event_sliderColumnsStateChanged

  private void setCurrentSZEFile(final File file) {
    this.szeFile = file;
    this.menuSave.setEnabled(file != null);
  }

  private void menuFileOpenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileOpenActionPerformed
    try {
      this.toolsButtonGroup.clearSelection();

      final JFileChooser chooser = new JFileChooser(this.lastOpenedFile);
      chooser.setAcceptAllFileFilterUsed(false);

      container.getComponents(AbstractFilePlugin.class).forEach((plugin) -> {
        chooser.addChoosableFileFilter(plugin.getImportFileFilter());
      });

      final InsideFileView insideFileView = new InsideFileView(chooser);
      chooser.setAccessory(insideFileView);

      if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
        final AbstractFilePlugin plugin = (AbstractFilePlugin) chooser.getFileFilter();
        final File selectedFile = chooser.getSelectedFile();
        this.lastOpenedFile = selectedFile;

        try {
          int selected = -1;
          if (plugin.doesImportContainInsideFileList()) {
            final SelectInsideDataDialog itemSelector = new SelectInsideDataDialog(this, selectedFile, plugin);
            itemSelector.setVisible(true);
            selected = itemSelector.getSelectedIndex();
            if (selected < 0) {
              return;
            }
          }
          final AbstractFilePlugin.ReadResult result = plugin.readFrom(selectedFile, selected);
          this.setTitle(selectedFile.getAbsolutePath());
          this.mainEditor.setProcessingData(result.getData());
          if (result.getSessionData() != null) {
            loadStateFromSession(result.getSessionData());
          } else {
            resetOptions();
          }

          setCurrentSZEFile(plugin instanceof SZEPlugin ? selectedFile : null);

          if ((plugin instanceof SCRPlugin) && !this.menuOptionsZXScreen.isSelected()) {
            this.menuOptionsZXScreen.setSelected(true);
          }
        } catch (IllegalArgumentException ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(this, ex.getMessage(), "Can't read", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(this, "Can't read file or its part [" + ex.getMessage() + ']', "Error", JOptionPane.ERROR_MESSAGE);
        } catch (Exception ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(this, "Unexpected exception! See log!", "Unexpected error", JOptionPane.ERROR_MESSAGE);
        } finally {
          updateAddressScrollBar();
          updateRedoUndo();
        }
      }
    } finally {
      menuEditMenuSelected(null);
    }
  }//GEN-LAST:event_menuFileOpenActionPerformed

  private void scrollBarAddressAdjustmentValueChanged(java.awt.event.AdjustmentEvent evt) {//GEN-FIRST:event_scrollBarAddressAdjustmentValueChanged
    final int address = evt.getValue();
    this.mainEditor.setAddress(address);
  }//GEN-LAST:event_scrollBarAddressAdjustmentValueChanged

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
    return this.mainEditor.mousePoint2ScreenPoint(SwingUtilities.convertPoint(this.mainEditorPanel, evt.getPoint(), this.mainEditor));
  }

  private Rectangle updateToolRectangle(final Point editorPoint) {
    final int width = this.sliderPenWidth.getValue();
    final Rectangle rect;
    if (width <= 1) {
      rect = new Rectangle(editorPoint.x, editorPoint.y, 1, 1);
    } else {
      rect = new Rectangle(editorPoint.x - (width >> 1), editorPoint.y - (width >> 1), width, width);
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

  public static String toHex(final int value) {
    final String h = Integer.toHexString(value).toUpperCase(Locale.ENGLISH);
    return '#' + (h.length() < 4 ? "0000".substring(0, 4 - h.length()) + h : h);
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

  private void mainEditorPanelMousePressed(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mainEditorPanelMousePressed
    this.mainEditor.addUndo();
    updateRedoUndo();

    if (this.selectAreaMode) {
      this.mainEditor.startSelectArea(mouseCoord2EditorCoord(evt));
    } else {
      updateToolRectangle(mouseCoord2EditorCoord(evt));
      processCurrentToolForPoint(extractButtons(evt));
    }
  }//GEN-LAST:event_mainEditorPanelMousePressed

  private void mainEditorPanelMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mainEditorPanelMouseMoved
    updateToolRectangle(mouseCoord2EditorCoord(evt));
  }//GEN-LAST:event_mainEditorPanelMouseMoved

  private void mainEditorPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mainEditorPanelMouseExited
    this.mainEditor.setToolArea(null);
  }//GEN-LAST:event_mainEditorPanelMouseExited

  private void mainEditorPanelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mainEditorPanelMouseEntered
    updateToolRectangle(mouseCoord2EditorCoord(evt));
  }//GEN-LAST:event_mainEditorPanelMouseEntered

  private void mainEditorPanelMouseDragged(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mainEditorPanelMouseDragged
    if (this.selectAreaMode) {
      this.mainEditor.updateSelectArea(mouseCoord2EditorCoord(evt));
    } else {
      updateToolRectangle(mouseCoord2EditorCoord(evt));
      processCurrentToolForPoint(extractButtons(evt));
    }
  }//GEN-LAST:event_mainEditorPanelMouseDragged

  private void menuOptionsMode512StateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuOptionsMode512StateChanged
    this.mainEditor.resetSelectArea();
    this.mainEditor.setMode512(this.menuOptionsMode512.isSelected());
  }//GEN-LAST:event_menuOptionsMode512StateChanged

  private void menuOptionsZXScreenStateChanged(javax.swing.event.ChangeEvent evt) {//GEN-FIRST:event_menuOptionsZXScreenStateChanged
    this.mainEditor.setZXScreenMode(this.menuOptionsZXScreen.isSelected());
  }//GEN-LAST:event_menuOptionsZXScreenStateChanged

  private void menuOptionsInvertBaseActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsInvertBaseActionPerformed
    this.mainEditor.setInvertShowBaseData(this.menuOptionsInvertBase.isSelected());
  }//GEN-LAST:event_menuOptionsInvertBaseActionPerformed

  private void menuOptionsColumnsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsColumnsActionPerformed
    this.mainEditor.setShowColumnBorders(this.menuOptionsColumns.isSelected());
  }//GEN-LAST:event_menuOptionsColumnsActionPerformed

  private void menuOptionsGridActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsGridActionPerformed
    this.mainEditor.setShowGrid(this.menuOptionsGrid.isSelected());
  }//GEN-LAST:event_menuOptionsGridActionPerformed

  private void menuEditUndoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEditUndoActionPerformed
    this.mainEditor.undo();
    updateRedoUndo();
  }//GEN-LAST:event_menuEditUndoActionPerformed

  private void menuEditRedoActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEditRedoActionPerformed
    this.mainEditor.redo();
    updateRedoUndo();
  }//GEN-LAST:event_menuEditRedoActionPerformed

  private void menuEditClearActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEditClearActionPerformed
    if (JOptionPane.showConfirmDialog(this, "Clear ZX-Poly data?", "Confirmation", JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION) {
      this.mainEditor.clear();
    }
  }//GEN-LAST:event_menuEditClearActionPerformed

  private void menuOptionDontShowAttributesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionDontShowAttributesActionPerformed
    this.mainEditor.setShowAttributes(EditorComponent.AttributeMode.DONT_SHOW);
  }//GEN-LAST:event_menuOptionDontShowAttributesActionPerformed

  private void menuOptionsShowBaseAttributesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsShowBaseAttributesActionPerformed
    this.mainEditor.setShowAttributes(EditorComponent.AttributeMode.SHOW_BASE);
  }//GEN-LAST:event_menuOptionsShowBaseAttributesActionPerformed

  private void menuFileSaveAsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileSaveAsActionPerformed
    if (this.mainEditor.hasData()) {
      final ZXPolyData zxpolydata = this.mainEditor.getProcessingData();

      final JFileChooser fileChoolser = new JFileChooser(this.lastOpenedFile);
      fileChoolser.setAcceptAllFileFilterUsed(false);
      fileChoolser.addChoosableFileFilter(container.getComponent(SZEPlugin.class));
      if (fileChoolser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
        try {
          final File thefile = ensureExtension(fileChoolser.getSelectedFile(), container.getComponent(SZEPlugin.class));
          container.getComponent(SZEPlugin.class).writeTo(thefile, zxpolydata, new SessionData(this.mainEditor));

          this.setTitle(thefile.getAbsolutePath());
          setCurrentSZEFile(thefile);
        } catch (Exception ex) {
          ex.printStackTrace();
          JOptionPane.showMessageDialog(this, "Error during operation [" + ex.getMessage() + ']', "Error", JOptionPane.ERROR_MESSAGE);
        }
      }
    }
  }//GEN-LAST:event_menuFileSaveAsActionPerformed

  private void menuOptionsShow512x384AttributesActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsShow512x384AttributesActionPerformed
    this.mainEditor.setShowAttributes(EditorComponent.AttributeMode.SHOW_512x384_ZXPOLY_PLANES);
  }//GEN-LAST:event_menuOptionsShow512x384AttributesActionPerformed

  private void menuEditCopyBaseToPlansActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEditCopyBaseToPlansActionPerformed
    if (JOptionPane.showConfirmDialog(this, "Do you really want to copy base data to all ZX-Poly planes?", "Confirmation", JOptionPane.YES_NO_OPTION) == JOptionPane.OK_OPTION) {
      this.mainEditor.copyPlansFromBase();
    }
  }//GEN-LAST:event_menuEditCopyBaseToPlansActionPerformed

  private void menuSaveActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuSaveActionPerformed
    try {
      container.getComponent(SZEPlugin.class).writeTo(this.szeFile, this.mainEditor.getProcessingData(), new SessionData(this.mainEditor));
    } catch (Exception ex) {
      ex.printStackTrace();
      JOptionPane.showMessageDialog(this, "Can't save file for exception [" + ex.getMessage() + ']', "Error", JOptionPane.ERROR_MESSAGE);
    }
  }//GEN-LAST:event_menuSaveActionPerformed

  private void menuFileNewActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuFileNewActionPerformed
    try {
      if (this.mainEditor.hasData()) {
        if (JOptionPane.showConfirmDialog(this, "Do you really want to create new data?", "Confirmation", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION) {
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
  }//GEN-LAST:event_menuFileNewActionPerformed

  private void menuOptionsColumnsAllActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsColumnsAllActionPerformed
    this.mainEditor.setColumnMode(EditorComponent.ColumnMode.ALL);
  }//GEN-LAST:event_menuOptionsColumnsAllActionPerformed

  private void menuOptionsColumnsOddActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsColumnsOddActionPerformed
    this.mainEditor.setColumnMode(EditorComponent.ColumnMode.ODD);
  }//GEN-LAST:event_menuOptionsColumnsOddActionPerformed

  private void menuOptionsColumnsEvenActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuOptionsColumnsEvenActionPerformed
    this.mainEditor.setColumnMode(EditorComponent.ColumnMode.EVEN);
  }//GEN-LAST:event_menuOptionsColumnsEvenActionPerformed

  private void mainEditorPanelMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mainEditorPanelMouseReleased
    if (this.selectAreaMode) {
      this.selectAreaMode = false;
      this.mainEditor.endSelectArea(mouseCoord2EditorCoord(evt));
      this.menuEditMenuSelected(null);
    }
  }//GEN-LAST:event_mainEditorPanelMouseReleased

  private void deactivateCurrentTool() {
    this.toolsButtonGroup.clearSelection();
  }

  private void menuEditSelectAreaActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEditSelectAreaActionPerformed
    deactivateCurrentTool();
    this.mainEditor.setDraggedImage(null);
    this.selectAreaMode = true;
    this.mainEditor.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
    this.mainEditor.addUndo();
    this.mainEditor.resetSelectArea();
  }//GEN-LAST:event_menuEditSelectAreaActionPerformed

  private void menuEditCopySelectedBaseAsImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEditCopySelectedBaseAsImageActionPerformed
    final Image selectedAreaImage = this.mainEditor.getSelectedAreaAsImage(true);
    if (selectedAreaImage != null) {
      new TransferableImage(selectedAreaImage).toClipboard();
    }
  }//GEN-LAST:event_menuEditCopySelectedBaseAsImageActionPerformed

  private void menuEditCopySelectedZxPolyAsImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEditCopySelectedZxPolyAsImageActionPerformed
    final Image selectedAreaImage = this.mainEditor.getSelectedAreaAsImage(false);
    if (selectedAreaImage != null) {
      new TransferableImage(selectedAreaImage).toClipboard();
    }
  }//GEN-LAST:event_menuEditCopySelectedZxPolyAsImageActionPerformed

  private void menuEditMenuSelected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_menuEditMenuSelected
    this.toolsButtonGroup.clearSelection();
    this.menuEditSelectArea.setEnabled(this.mainEditor.hasData());
    this.menuEditPasteImage.setEnabled(GfxUtils.doesClipboardHasImage());
    this.menuEditCopySelectedBaseAsImage.setEnabled(this.mainEditor.hasSelectedArea());
    this.menuEditCopySelectedZxPolyAsImage.setEnabled(this.mainEditor.hasSelectedArea());
  }//GEN-LAST:event_menuEditMenuSelected

  private void mainEditorPanelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_mainEditorPanelMouseClicked
    if (this.mainEditor.hasDraggedImage()) {
      if (evt.getClickCount() > 1 && evt.getButton() == MouseEvent.BUTTON1) {
        this.mainEditor.doStampDraggedImage();
        evt.consume();
      } else if (evt.getButton() == MouseEvent.BUTTON3) {
        this.mainEditor.setDraggedImage(null);
        evt.consume();
      }
    }
  }//GEN-LAST:event_mainEditorPanelMouseClicked

    private void menuEditPasteImageActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_menuEditPasteImageActionPerformed
      final Image image = GfxUtils.getImageFromClipboard();
      if (image != null) {
        this.toolsButtonGroup.clearSelection();
        this.mainEditor.setCursor(Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR));
        this.mainEditor.setToolArea(null);
        this.mainEditor.setDraggedImage(image);
      }
    }//GEN-LAST:event_menuEditPasteImageActionPerformed

    private void menuEditMenuDeselected(javax.swing.event.MenuEvent evt) {//GEN-FIRST:event_menuEditMenuDeselected
      this.menuEditSelectArea.setEnabled(this.mainEditor.hasData());
      this.menuEditPasteImage.setEnabled(this.mainEditor.hasData());
      this.menuEditCopySelectedBaseAsImage.setEnabled(this.mainEditor.hasData());
      this.menuEditCopySelectedZxPolyAsImage.setEnabled(this.mainEditor.hasData());
    }//GEN-LAST:event_menuEditMenuDeselected

  private void updateAddressScrollBar() {
    this.sliderColumns.setEnabled(true);
    this.scrollBarAddress.setMinimum(0);
    if (this.mainEditor.getProcessingData() == null) {
      this.scrollBarAddress.setEnabled(false);
    } else {
      this.scrollBarAddress.setMaximum(Math.max(0, this.mainEditor.getProcessingData().length() - 32));
      this.scrollBarAddress.setEnabled(true);
      this.scrollBarAddress.setValue(this.mainEditor.getAddress());
      this.scrollBarAddress.setUnitIncrement(this.mainEditor.getColumns());
      this.scrollBarAddress.setBlockIncrement(this.mainEditor.getColumns() * 96);
      this.scrollBarAddress.setVisibleAmount(this.mainEditor.getColumns() * 192);
    }
    this.scrollBarAddress.repaint();
  }

  // Variables declaration - do not modify//GEN-BEGIN:variables
  private javax.swing.ButtonGroup attributesButtonGroup;
  private javax.swing.JToggleButton buttonLock;
  private com.igormaznitsa.zxpspritecorrector.components.ZXColorSelector colorSelector;
  private javax.swing.ButtonGroup columnModeGroup;
  private javax.swing.Box.Filler filler1;
  private javax.swing.JPanel jPanel2;
  private javax.swing.JScrollPane jScrollPane1;
  private javax.swing.JSeparator jSeparator1;
  private javax.swing.JPopupMenu.Separator jSeparator2;
  private javax.swing.JPopupMenu.Separator jSeparator3;
  private javax.swing.JPopupMenu.Separator jSeparator4;
  private javax.swing.JPopupMenu.Separator jSeparator5;
  private javax.swing.JPopupMenu.Separator jSeparator6;
  private javax.swing.JPopupMenu.Separator jSeparator7;
  private javax.swing.JLabel labelAddress;
  private javax.swing.JLabel labelZoom;
  private com.igormaznitsa.zxpspritecorrector.components.EditorComponent mainEditor;
  private javax.swing.JPanel mainEditorPanel;
  private javax.swing.JMenuBar menuBar;
  private javax.swing.JMenu menuEdit;
  private javax.swing.JMenuItem menuEditClear;
  private javax.swing.JMenuItem menuEditCopyBaseToPlans;
  private javax.swing.JMenuItem menuEditCopySelectedBaseAsImage;
  private javax.swing.JMenuItem menuEditCopySelectedZxPolyAsImage;
  private javax.swing.JMenuItem menuEditPasteImage;
  private javax.swing.JMenuItem menuEditRedo;
  private javax.swing.JMenuItem menuEditSelectArea;
  private javax.swing.JMenuItem menuEditUndo;
  private javax.swing.JMenu menuFile;
  private javax.swing.JMenuItem menuFileExit;
  private javax.swing.JMenu menuFileExportAs;
  private javax.swing.JMenuItem menuFileNew;
  private javax.swing.JMenuItem menuFileOpen;
  private javax.swing.JMenuItem menuFileSaveAs;
  private javax.swing.JMenu menuHelp;
  private javax.swing.JMenuItem menuHelpAbout;
  private javax.swing.JRadioButtonMenuItem menuOptionDontShowAttributes;
  private javax.swing.JMenu menuOptions;
  private javax.swing.JCheckBoxMenuItem menuOptionsColumns;
  private javax.swing.JRadioButtonMenuItem menuOptionsColumnsAll;
  private javax.swing.JRadioButtonMenuItem menuOptionsColumnsEven;
  private javax.swing.JRadioButtonMenuItem menuOptionsColumnsOdd;
  private javax.swing.JCheckBoxMenuItem menuOptionsGrid;
  private javax.swing.JCheckBoxMenuItem menuOptionsInvertBase;
  private javax.swing.JCheckBoxMenuItem menuOptionsMode512;
  private javax.swing.JRadioButtonMenuItem menuOptionsShow512x384Attributes;
  private javax.swing.JRadioButtonMenuItem menuOptionsShowBaseAttributes;
  private javax.swing.JCheckBoxMenuItem menuOptionsZXScreen;
  private javax.swing.JMenuItem menuSave;
  private javax.swing.JPanel panelTools;
  private javax.swing.JScrollBar scrollBarAddress;
  private javax.swing.JSlider sliderColumns;
  private com.igormaznitsa.zxpspritecorrector.components.PenWidth sliderPenWidth;
  private javax.swing.JSpinner spinnerCurrentAddress;
  private javax.swing.ButtonGroup toolsButtonGroup;
  // End of variables declaration//GEN-END:variables

}
