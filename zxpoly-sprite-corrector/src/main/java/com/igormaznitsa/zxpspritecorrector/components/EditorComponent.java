package com.igormaznitsa.zxpspritecorrector.components;

import com.igormaznitsa.zxpspritecorrector.HobetaContainer;
import com.igormaznitsa.zxpspritecorrector.utils.ZXPalette;
import java.awt.*;
import java.awt.event.*;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.*;

public class EditorComponent extends JComponent implements BoundedRangeModel, MouseListener, MouseMotionListener, ChangeListener {

  private static final long serialVersionUID = -6948149982924499351L;

  protected final List<ChangeListener> p_ChangeListeners = new ArrayList<ChangeListener>();
  protected final Color COLOR_PIXEL_ON = Color.lightGray;
  protected final Color COLOR_PIXEL_OFF = Color.darkGray;
  protected final Color COLOR_ZX512x384_ON = Color.yellow;
  protected final Color COLOR_ZX512x384_OFF = Color.blue;
  protected final Color COLOR_GRID = Color.ORANGE.darker();
  protected final Color COLOR_COLUMN_BORDER = Color.CYAN;

  protected VideoMode graphicMode;
  protected Tool i_CurrentTool = Tool.NONE;
  protected int i_ColorInkIndex = 0;
  protected int i_ColorPaintIndex = 15;
  protected BufferedImage bufferImage;
  protected boolean flagZXVideoLineAddressing;
  protected boolean flagShowInverted;
  protected byte[] ab_DataSource;
  protected int[] ai_Painted;
  protected byte[] ab_PaintedMask;
  protected boolean lg_Changed;
  protected int i_CurrentPosition;
  protected int columnNumber;
  protected boolean flagShowGrid;
  protected boolean flagShowColumnBorder;
  protected int zoomFactor;
  protected boolean flagPositionLock;
  private final List<UndoStorage> p_UndoStack = new ArrayList<UndoStorage>();
  protected Rectangle p_SelectRectangle = null;
  protected Point p_SelectStartPoint = new Point(0, 0);
  protected int penWidth = 1;
  protected Point currentMousePoint = null;
  protected HobetaContainer p_HobetaContainer;

  public boolean hasInformation() {
    return p_HobetaContainer != null;
  }

  public HobetaContainer getHobetaContainer() {
    return p_HobetaContainer;
  }

  public void removeSelection() {
    p_SelectRectangle = null;
    lg_Changed = true;
    repaint();
  }

  public int[] getColorArray() {
    return ai_Painted;
  }

  public byte[] getMaskArray() {
    return ab_PaintedMask;
  }

  public byte[] getDataArray() {
    return ab_DataSource;
  }

  public void setShowColumnBorders(boolean _value) {
    flagShowColumnBorder = _value;
    repaint();
  }

  private void processSelecterToolMouseMove(int x, int y, int i_button) {
    lg_Changed = true;

    x /= zoomFactor;
    y /= zoomFactor;

    if (graphicMode == VideoMode.ZX_512x384) {
      x &= 0xFFFFFFFE;
      y &= 0xFFFFFFFE;
    }

    if (p_SelectRectangle != null) {
      p_SelectRectangle.setLocation(Math.min(x, p_SelectStartPoint.x), Math.min(y, p_SelectStartPoint.y));
      p_SelectRectangle.setSize(Math.max(x, p_SelectStartPoint.x) - Math.min(x, p_SelectStartPoint.x), Math.max(y, p_SelectStartPoint.y) - Math.min(y, p_SelectStartPoint.y));
    }
    repaint();
  }

  private class UndoStorage {

    protected int[] ai_Colors;
    protected byte[] ab_Mask;

    public UndoStorage(int[] _colors, byte[] _mask) {
      ai_Colors = _colors;
      ab_Mask = _mask;
    }
  }

  private static final int calculateZXYForAddressLine(int _address) {
    return (((_address & 0x00e0)) >> 2) + (((_address & 0x0700)) >> 8) + (((_address & 0x1800)) >> 5);
  }

  public int getColorInkIndex() {
    return i_ColorInkIndex;
  }

  public int getColorPaintIndex() {
    return i_ColorPaintIndex;
  }

  public Tool getTool() {
    return i_CurrentTool;
  }

  public void setTool(Tool _tool) {
    i_CurrentTool = _tool == null ? Tool.NONE : _tool;
  }

  @Override
  public Dimension getPreferredSize() {
    final Dimension dim = graphicMode.getSize();
    return new Dimension(dim.width * zoomFactor, dim.height * zoomFactor);
  }

  @Override
  public Dimension getMinimumSize() {
    return graphicMode.getSize();
  }

  @Override
  public Dimension getMaximumSize() {
    return getPreferredSize();
  }

  @Override
  public void paintComponent(final Graphics gfx) {
    final Graphics2D gfx2d = (Graphics2D) gfx;

    if (!hasInformation()) {
      return;
    }

    final int scaledWidth = graphicMode.getSize().width * zoomFactor;
    final int scaledHeight = graphicMode.getSize().height * zoomFactor;

    gfx2d.drawImage(bufferImage, 0, 0, scaledWidth, scaledHeight, null);

    if (flagShowGrid && zoomFactor > 1) {
      gfx2d.setColor(COLOR_GRID);

      switch (graphicMode) {
        case ZXPOLY: {
          for (int x = zoomFactor; x < scaledWidth; x += zoomFactor) {
            gfx2d.drawLine(x, 0, x, scaledHeight);
          }
          for (int y = zoomFactor; y < scaledHeight; y += zoomFactor) {
            gfx2d.drawLine(0, y, scaledWidth, y);
          }
        }
        break;
        case ZX_512x384: {
          final int i_z = zoomFactor * 2;
          for (int x = i_z; x < scaledWidth; x += i_z) {
            gfx2d.drawLine(x, 0, x, scaledHeight);
          }
          for (int y = i_z; y < scaledHeight; y += i_z) {
            gfx2d.drawLine(0, y, scaledWidth, y);
          }
        }
        break;
      }
    }

    if (flagShowColumnBorder && zoomFactor > 1) {
      gfx2d.setColor(COLOR_COLUMN_BORDER);

      switch (zoomFactor) {
        case 1:
          break;
        case 2:
          gfx2d.setStroke(new BasicStroke(2f));
          break;
        case 4:
          gfx2d.setStroke(new BasicStroke(3f));
          break;
        case 8:
          gfx2d.setStroke(new BasicStroke(4f));
          break;
        default:
          gfx2d.setStroke(new BasicStroke(5f));
          break;
      }

      final int columnWidth = (graphicMode == VideoMode.ZX_512x384 ? 16 : 8);
      int i_xoffset = zoomFactor * columnWidth;

      for (int li = 0; li < columnNumber; li++) {
        gfx2d.drawLine(i_xoffset, 0, i_xoffset, scaledHeight);

        i_xoffset += (zoomFactor * columnWidth);
      }
    }

    if (p_SelectRectangle != null || currentMousePoint != null) {
      gfx2d.setXORMode(Color.red);

      switch (zoomFactor) {
        case 1:
          break;
        case 2:
          gfx2d.setStroke(new BasicStroke(2f));
          break;
        case 4:
          gfx2d.setStroke(new BasicStroke(3f));
          break;
        case 8:
          gfx2d.setStroke(new BasicStroke(4f));
          break;
        default:
          gfx2d.setStroke(new BasicStroke(5f));
          break;
      }

      if (currentMousePoint != null) {
        int i_x = ((currentMousePoint.x / zoomFactor) * zoomFactor) - ((penWidth / 2) * zoomFactor);
        int i_y = ((currentMousePoint.y / zoomFactor) * zoomFactor) - ((penWidth / 2) * zoomFactor);

        gfx2d.setColor(Color.orange);
        gfx2d.drawRect(i_x, i_y, penWidth * zoomFactor, penWidth * zoomFactor);
      }

      if (p_SelectRectangle != null) {
        gfx2d.setColor(Color.blue);
        gfx2d.drawRect(p_SelectRectangle.x * zoomFactor, p_SelectRectangle.y * zoomFactor, p_SelectRectangle.width * zoomFactor, p_SelectRectangle.height * zoomFactor);
      }
    }
  }

  private void _draw512x384pixelBlock(final Graphics gfx, final int x, final int y, final int data) {
    final Color p_ink = flagShowInverted ? COLOR_ZX512x384_OFF : COLOR_ZX512x384_ON;
    final Color p_paper = flagShowInverted ? COLOR_ZX512x384_ON : COLOR_ZX512x384_OFF;

    final boolean lg_cpu0 = (data & 4) != 0;
    final boolean lg_cpu1 = (data & 2) != 0;
    final boolean lg_cpu2 = (data & 1) != 0;
    final boolean lg_cpu3 = (data & 8) != 0;

    gfx.setColor(lg_cpu0 ? p_ink : p_paper);
    gfx.drawLine(x, y, x, y);

    gfx.setColor(lg_cpu2 ? p_ink : p_paper);
    gfx.drawLine(x, y + 1, x, y + 1);

    gfx.setColor(lg_cpu1 ? p_ink : p_paper);
    gfx.drawLine(x + 1, y, x + 1, y);

    gfx.setColor(lg_cpu3 ? p_ink : p_paper);
    gfx.drawLine(x + 1, y + 1, x + 1, y + 1);

  }

  public void redrawImageBuffer() {
    final Graphics2D gfx2d = bufferImage.createGraphics();
    gfx2d.setColor(Color.black);
    final Dimension dim = graphicMode.getSize();
    gfx2d.fillRect(0, 0, dim.width, dim.height);

    switch (graphicMode) {
      case ZXPOLY: {
        final byte[] ab_buffer = ab_DataSource;
        final int[] ai_painted = ai_Painted;
        final byte[] ab_paintedMask = ab_PaintedMask;

        if (ab_buffer == null) {
          return;
        }

        final int i_DataLength = ab_buffer.length;

        final int i_calculatedWidth = columnNumber * 8;

        final Color colorInk = flagShowInverted ? COLOR_PIXEL_OFF : COLOR_PIXEL_ON;
        final Color colorPaper = flagShowInverted ? COLOR_PIXEL_ON : COLOR_PIXEL_OFF;

        int i_gy = 0;
        int i_x = 0;

        for (int li = i_CurrentPosition; li < i_DataLength; li++) {
          int i_val = ab_buffer[li];
          int i_valpainted = ai_painted[li];
          int i_valpaintedmask = ab_paintedMask[li];

          final int i_y = flagZXVideoLineAddressing ? VideoMode.linearYtoZXY(i_gy) : i_gy;

          for (int lx = 0; lx < 8; lx++) {
            Color p_color = (i_val & 0x80) == 0 ? colorPaper : colorInk;
            int i_colorindx = i_valpainted >>> 28;

            if ((i_valpaintedmask & 0x80) != 0) {
              p_color = ZXPalette.COLORS[i_colorindx];
            }

            i_val <<= 1;
            i_valpaintedmask <<= 1;
            i_valpainted <<= 4;

            gfx2d.setColor(p_color);
            gfx2d.drawLine(i_x, i_y, i_x, i_y);
            i_x++;
          }

          if (i_x >= i_calculatedWidth) {
            i_gy++;

            i_x = 0;

            if (i_gy >= dim.height) {
              break;
            }
          }
        }
      }
      break;
      case ZX_512x384: {
        final byte[] ab_buffer = ab_DataSource;
        final int[] ai_painted = ai_Painted;
        final byte[] ab_paintedMask = ab_PaintedMask;

        if (ab_buffer == null) {
          return;
        }

        final int i_DataLength = ab_buffer.length;

        final int i_calculatedWidth = columnNumber * 16;

        final Color colorNotChangedPaper = flagShowInverted ? COLOR_PIXEL_ON : COLOR_PIXEL_OFF;
        final Color colorNotChangedInk = flagShowInverted ? COLOR_PIXEL_OFF : COLOR_PIXEL_ON;

        int i_gy = 0;
        int i_x = 0;

        for (int li = i_CurrentPosition; li < i_DataLength; li++) {
          int i_val = ab_buffer[li];
          int i_valpainted = ai_painted[li];
          int i_valpaintedmask = ab_paintedMask[li];

          final int i_y = (flagZXVideoLineAddressing ? VideoMode.linearYtoZXY(i_gy) : i_gy) * 2;

          for (int lx = 0; lx < 8; lx++) {
            final boolean lg_originalBit = (i_val & 0x80) != 0;
            int i_pixelData = i_valpainted >>> 28;

            final boolean lg_changed = (i_valpaintedmask & 0x80) != 0;

            i_val <<= 1;
            i_valpaintedmask <<= 1;
            i_valpainted <<= 4;

            if (lg_changed) {
              _draw512x384pixelBlock(gfx2d, i_x, i_y, i_pixelData);
            }
            else {
              gfx2d.setColor(lg_originalBit ? colorNotChangedInk : colorNotChangedPaper);
              gfx2d.fillRect(i_x, i_y, 2, 2);
            }
            i_x += 2;
          }

          if (i_x >= i_calculatedWidth) {
            i_gy++;

            i_x = 0;

            if (i_gy >= dim.height) {
              break;
            }
          }
        }
      }
      break;
    }

    gfx2d.dispose();
  }

  public void setZXVideoLineAddressing(final boolean flag) {
    if (flag != this.flagZXVideoLineAddressing) {
      this.flagZXVideoLineAddressing = flag;
      redrawImageBuffer();
      invalidate();
      repaint();
    }
  }

  public boolean isZXVideoLineAddressing() {
    return flagZXVideoLineAddressing;
  }

  public int getColumnNumber() {
    return columnNumber;
  }

  public int getZoom() {
    return zoomFactor;
  }

  public void setZoom(int _value) {
    zoomFactor = Math.max(_value, 1);
    repaint();
  }

  public void setColumnNumber(final int number) {
    this.columnNumber = Math.min(32, Math.max(1, number));
    redrawImageBuffer();
    invalidate();
    repaint();
  }

  public int getPosition() {
    return i_CurrentPosition;
  }

  public void setPosition(int _position) {
    if (flagPositionLock) {
      return;
    }
    _position = Math.max(_position, 0);
    _position = Math.min(_position, ab_DataSource != null ? ab_DataSource.length : 0);

    i_CurrentPosition = _position;
    redrawImageBuffer();

    for (ChangeListener p_listener : p_ChangeListeners) {
      p_listener.stateChanged(new ChangeEvent(this));
    }

    repaint();
  }

  public void setData(HobetaContainer _hobetaContainer) {
    i_CurrentTool = Tool.NONE;
    p_SelectRectangle = null;

    p_HobetaContainer = _hobetaContainer;

    ab_DataSource = _hobetaContainer.getDataArray();
    ai_Painted = new int[ab_DataSource.length];
    ab_PaintedMask = new byte[ab_DataSource.length];

    lg_Changed = false;

    p_UndoStack.clear();

    setColumnNumber(32);
    setPosition(0);
    redrawImageBuffer();

    repaint();
  }

  public synchronized void startEdit() {
    // make undo
    if (ai_Painted == null) {
      return;
    }
    lg_Changed = true;

    int[] ai_copy = Arrays.copyOf(ai_Painted, ai_Painted.length);
    byte[] ab_copymask = Arrays.copyOf(ab_PaintedMask, ab_PaintedMask.length);
    if (p_UndoStack.size() >= 10) {
      p_UndoStack.remove(0);
    }
    p_UndoStack.add(new UndoStorage(ai_copy, ab_copymask));
  }

  public boolean hasUndo() {
    return !p_UndoStack.isEmpty();
  }

  public synchronized void undoEdit() {
    if (ai_Painted == null) {
      return;
    }
    if (!p_UndoStack.isEmpty()) {
      UndoStorage p_storage = p_UndoStack.remove(p_UndoStack.size() - 1);
      ai_Painted = p_storage.ai_Colors;
      ab_PaintedMask = p_storage.ab_Mask;
      redrawImageBuffer();
      repaint();
    }
  }

  public EditorComponent() {
    super();

    flagZXVideoLineAddressing = false;
    flagShowInverted = false;

    flagShowColumnBorder = false;
    flagShowGrid = false;

    columnNumber = 32;

    i_CurrentTool = Tool.NONE;
    p_SelectRectangle = null;

    zoomFactor = 1;

    setGraphicMode(VideoMode.ZXPOLY);
    ab_DataSource = null;
    lg_Changed = false;

    setFocusable(false);

    addMouseListener(this);
    addMouseMotionListener(this);

  }

  public VideoMode getGraphicMode() {
    return graphicMode;
  }

  public void setGraphicMode(final VideoMode _mode) {
    removeSelection();
    graphicMode = _mode;
    bufferImage = new BufferedImage(_mode.getSize().width, _mode.getSize().height, BufferedImage.TYPE_INT_RGB);
    redrawImageBuffer();
  }

  @Override
  public int getMinimum() {
    return 0;
  }

  @Override
  public void setMinimum(int newMinimum) {
  }

  @Override
  public int getMaximum() {
    if (ab_DataSource == null) {
      return 0;
    }
    else {
      return ab_DataSource.length;
    }
  }

  @Override
  public void setMaximum(int newMaximum) {
  }

  public void setInverted(boolean _value) {
    flagShowInverted = _value;
    redrawImageBuffer();
    repaint();
  }

  public boolean isInverted() {
    return flagShowInverted;
  }

  @Override
  public int getValue() {
    return getPosition();
  }

  @Override
  public void setValue(int newValue) {
    if (!flagPositionLock) {
      setPosition(newValue);
    }
  }

  @Override
  public void setValueIsAdjusting(boolean b) {
  }

  @Override
  public boolean getValueIsAdjusting() {
    return true;
  }

  @Override
  public int getExtent() {
    return columnNumber;
  }

  @Override
  public void setExtent(int newExtent) {
  }

  @Override
  public void setRangeProperties(int value, int extent, int min, int max, boolean adjusting) {
  }

  @Override
  public void addChangeListener(ChangeListener x) {
    if (!p_ChangeListeners.contains(x)) {
      p_ChangeListeners.add(x);
    }
  }

  @Override
  public void removeChangeListener(ChangeListener x) {
    p_ChangeListeners.remove(x);
  }

  public void clearAllPainted() {
    if (ai_Painted != null) {
      startEdit();
      Arrays.fill(ai_Painted, 0);
      Arrays.fill(ab_PaintedMask, (byte) 0);
    }
    redrawImageBuffer();
    repaint();
  }

  public void setGrid(boolean selected) {
    flagShowGrid = selected;
    repaint();
  }

  public boolean isGrid() {
    return flagShowGrid;
  }

  public boolean isColumnBordersShown() {
    return flagShowColumnBorder;
  }

  public void setLockFlag(boolean selected) {
    flagPositionLock = selected;
  }

  public boolean isLockFlag() {
    return flagPositionLock;
  }

  public boolean isZXScreen() {
    return flagZXVideoLineAddressing;
  }

  @Override
  public void mouseClicked(MouseEvent e) {
  }

  @Override
  public void mousePressed(MouseEvent e) {
    if (ab_DataSource != null && i_CurrentTool != Tool.NONE) {
      startEdit();

      switch (i_CurrentTool) {
        case SELECTOR: {
          currentMousePoint = null;
          startSelectorTool(e.getX(), e.getY(), e.getButton());
        }
        break;
        default: {
          _processToolPen(e.getX(), e.getY(), e.getButton());
        }
      }

      repaint();
    }
  }

  private void _processToolPen(int _x, int _y, int _button) {
    _x /= zoomFactor;
    _y /= zoomFactor;

    int i_pen = penWidth;

    int i_sx = _x - (i_pen / 2);
    int i_sy = _y - (i_pen / 2);

    for (int ly = 0; ly < i_pen; ly++) {
      int i_ly = i_sy + ly;

      for (int lx = 0; lx < i_pen; lx++) {
        int i_lx = i_sx + lx;

        switch (i_CurrentTool) {
          case PENCIL: {
            processPencilTool(i_lx, i_ly, _button);
          }
          break;
          case ERASER: {
            processEraserTool(i_lx, i_ly, _button);
          }
          break;
          case COLORREPLACER: {
            processColorReplacerTool(i_lx, i_ly, _button);
          }
          break;
        }
      }
    }
  }

  @Override
  public void mouseReleased(MouseEvent e) {
    currentMousePoint = e.getPoint();

    switch (i_CurrentTool) {
      case SELECTOR: {
        endSelectorTool(e.getX(), e.getY(), e.getButton());
      }
      break;
    }
    repaint();
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    currentMousePoint = e.getPoint();
    repaint();
  }

  @Override
  public void mouseExited(MouseEvent e) {
    currentMousePoint = null;
    repaint();
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    int i_button = MouseEvent.BUTTON1;
    if ((e.getModifiersEx() & MouseEvent.BUTTON3_DOWN_MASK) != 0) {
      i_button = MouseEvent.BUTTON3;
    }

    currentMousePoint = e.getPoint();

    switch (i_CurrentTool) {
      case SELECTOR: {
        processSelecterToolMouseMove(e.getX(), e.getY(), i_button);
      }
      break;
      default: {
        _processToolPen(e.getX(), e.getY(), i_button);
      }
    }
    repaint();
  }

  @Override
  public void mouseMoved(MouseEvent e) {
    currentMousePoint = e.getPoint();
    repaint();
  }

  public void setPaintColorIndex(int selectedPaint) {
    i_ColorPaintIndex = selectedPaint & 0xF;
  }

  public void setInkColorIndex(int selectedPaint) {
    i_ColorInkIndex = selectedPaint & 0xF;
  }

  private void processColorReplacerTool(final int x, final int y, int _button) {
    if (ab_DataSource == null) {
      return;
    }

    if (p_SelectRectangle != null) {
      if (!p_SelectRectangle.contains(x, y)) {
        return;
      }
    }

    switch (graphicMode) {
      case ZXPOLY: {
        switch (_getPointPositionZX(x, y)) {
          case 0: {
            if (_button == (flagShowInverted ? MouseEvent.BUTTON1 : MouseEvent.BUTTON3)) {
              _setPointPositionColorIndex(x, y, i_ColorPaintIndex);
            }
          }
          break;
          case 1: {
            if (_button == (flagShowInverted ? MouseEvent.BUTTON3 : MouseEvent.BUTTON1)) {
              _setPointPositionColorIndex(x, y, i_ColorInkIndex);
            }
          }
          break;
        }
      }
      break;
      case ZX_512x384: {
        switch (_getPointPositionZX(x, y)) {
          case 0: {
            if (_button == (flagShowInverted ? MouseEvent.BUTTON1 : MouseEvent.BUTTON3)) {
              _setPointPositionColorIndex(x, y, 0);
            }
          }
          break;
          case 1: {
            if (_button == (flagShowInverted ? MouseEvent.BUTTON3 : MouseEvent.BUTTON1)) {
              _setPointPositionColorIndex(x, y, 15);
            }
          }
          break;
        }
      }
      break;
    }
  }

  public void invertZXDataInSelectedRectangle() {
    if (p_SelectRectangle != null) {
      startEdit();

      int i_x = p_SelectRectangle.x;
      int i_y = p_SelectRectangle.y;
      int i_width = p_SelectRectangle.width;
      int i_height = p_SelectRectangle.height;

      int i_step = graphicMode == VideoMode.ZX_512x384 ? 2 : 1;

      for (int ly = 0; ly < i_height; ly += i_step) {
        int i_ly = i_y + ly;

        for (int lx = 0; lx < i_width; lx += i_step) {
          int i_lx = i_x + lx;
          int i_val = _getPointPositionZX(i_lx, i_ly);
          if (i_val < 0) {
            continue;
          }

          _setPointPositionZXData(i_lx, i_ly, i_val == 0 ? 1 : 0);
        }
      }
      repaint();
    }
  }

  private void processEraserTool(int x, int y, int _button) {
    if (ab_DataSource == null) {
      return;
    }

    if (p_SelectRectangle != null) {
      if (!p_SelectRectangle.contains(x, y)) {
        return;
      }
    }

    _setPointPositionColorIndex(x, y, -1);
  }

  private void processPencilTool(int _mousex, int _mousey, int _button) {
    if (ab_DataSource == null) {
      return;
    }

    if (p_SelectRectangle != null) {
      if (!p_SelectRectangle.contains(_mousex, _mousey)) {
        return;
      }
    }

    switch (graphicMode) {
      case ZXPOLY:
        _setPointPositionColorIndex(_mousex, _mousey, _button == MouseEvent.BUTTON1 ? i_ColorInkIndex : i_ColorPaintIndex);
        break;
      case ZX_512x384:
        _setPointPositionColorIndex(_mousex, _mousey, _button == MouseEvent.BUTTON1 ? 15 : 0);
        break;
    }
  }

  private void startSelectorTool(int x, int y, int _button) {
    x = x / zoomFactor;
    y = y / zoomFactor;

    if (graphicMode == VideoMode.ZX_512x384) {
      x &= 0xFFFFFFFE;
      y &= 0xFFFFFFFE;
    }

    p_SelectStartPoint.setLocation(x, y);
    p_SelectRectangle = new Rectangle(x, y, 0, 0);
  }

  private void endSelectorTool(int x, int y, int _button) {
    if (p_SelectRectangle != null) {
      int i_visW = graphicMode.getSize().width;
      int i_visH = graphicMode.getSize().height;

      if (graphicMode == VideoMode.ZX_512x384) {
        x &= 0xFFFFFFFE;
        y &= 0xFFFFFFFE;
      }

      p_SelectRectangle.x = Math.max(0, p_SelectRectangle.x);
      p_SelectRectangle.x = Math.max(0, p_SelectRectangle.x);

      if ((p_SelectRectangle.x + p_SelectRectangle.width) > i_visW) {
        p_SelectRectangle.width = i_visW - p_SelectRectangle.x;
      }
      if ((p_SelectRectangle.y + p_SelectRectangle.height) > i_visH) {
        p_SelectRectangle.height = i_visH - p_SelectRectangle.y;
      }

      i_CurrentTool = Tool.NONE;
      for (ChangeListener p_listener : p_ChangeListeners) {
        p_listener.stateChanged(new ChangeEvent(this));
      }
    }
  }

  private void _setPointPositionColorIndex(final int _x, final int _y, int _index) {
    if (_x < 0 || _y < 0) {
      return;
    }

    if (ai_Painted == null) {
      return;
    }

    switch (graphicMode) {
      case ZXPOLY: {
        int i_cx = _x / 8;
        if (i_cx >= columnNumber) {
          return;
        }

        int i_offset = 0;

        int i_y = _y;

        if (_x > 255 || _y > 191) {
          return;
        }

        if (flagZXVideoLineAddressing) {
          i_y = VideoMode.linearYtoZXY(_y);
        }

        i_offset = i_CurrentPosition + i_y * columnNumber + i_cx;

        if (i_offset < 0 || i_offset >= ai_Painted.length) {
          return;
        }

        Graphics p_g = bufferImage.getGraphics();

        int i_x = _x % 8;
        int i_sss = (28 - i_x * 4);
        int i_mask = 0xF << i_sss;

        int i_val = ai_Painted[i_offset];
        int i_valMask = ab_PaintedMask[i_offset];

        if (_index < 0) {
          i_valMask &= (~(0x80 >>> i_x));

          switch (_getPointPositionZX(_x, _y)) {
            case 0:
              p_g.setColor(flagShowInverted ? COLOR_PIXEL_ON : COLOR_PIXEL_OFF);
              break;
            case 1:
              p_g.setColor(flagShowInverted ? COLOR_PIXEL_OFF : COLOR_PIXEL_ON);
              break;
            default:
              p_g.setColor(Color.PINK);
          }
        }
        else {
          i_val = (i_val & ~i_mask) | (_index << i_sss);
          i_valMask |= (0x80 >> i_x);

          p_g.setColor(ZXPalette.COLORS[_index]);
        }

        ai_Painted[i_offset] = i_val;
        ab_PaintedMask[i_offset] = (byte) i_valMask;

        p_g.drawLine(_x, _y, _x, _y);
      }
      break;
      case ZX_512x384: {
        int i_cx = _x / 16;
        if (i_cx >= columnNumber) {
          return;
        }

        int i_offset = 0;

        int i_y = _y / 2;

        if (_x > 255 || _y > 191) {
          return;
        }

        if (flagZXVideoLineAddressing) {
          i_y = VideoMode.linearYtoZXY(i_y);
        }
        i_offset = i_CurrentPosition + i_y * columnNumber + i_cx;

        if (i_offset < 0 || i_offset >= ai_Painted.length) {
          return;
        }

        Graphics p_g = bufferImage.getGraphics();

        int i_x = (_x / 2) % 8;
        int i_sss = (28 - i_x * 4);
        int i_mask = 0xF << i_sss;

        int i_val = ai_Painted[i_offset];
        int i_valMask = ab_PaintedMask[i_offset];

        if (_index < 0) {
          i_valMask &= (~(0x80 >>> i_x));

          switch (_getPointPositionZX(_x, _y)) {
            case 0:
              p_g.setColor(flagShowInverted ? COLOR_PIXEL_ON : COLOR_PIXEL_OFF);
              break;
            case 1:
              p_g.setColor(flagShowInverted ? COLOR_PIXEL_OFF : COLOR_PIXEL_ON);
              break;
            default:
              p_g.setColor(Color.PINK);
          }

          p_g.fillRect(_x & 0xFFFFFFFE, _y & 0xFFFFFFFE, 2, 2);
        }
        else {
          int i_oldVal = (i_val >>> i_sss) & 0xF;
          int i_newVal = i_oldVal;

          if ((i_valMask & (0x80 >> i_x)) == 0) {
            i_newVal = 0;
          }

          int i_pixelIndex = 0;
          switch ((_x & 1) | ((_y & 1) << 1)) {
            case 0:
              i_pixelIndex = 2;
              break;
            case 1:
              i_pixelIndex = 1;
              break;
            case 2:
              i_pixelIndex = 0;
              break;
            case 3:
              i_pixelIndex = 3;
              break;
          }

          if (_index < 8) {
            // reset
            i_newVal &= (0xF ^ (1 << i_pixelIndex));
          }
          else {
            // set
            i_newVal |= (1 << i_pixelIndex);
          }

          i_val = (i_val & ~i_mask) | (i_newVal << i_sss);
          i_valMask |= (0x80 >> i_x);

          _draw512x384pixelBlock(p_g, _x & 0xFFFFFFFE, _y & 0xFFFFFFFE, i_newVal);
        }

        ai_Painted[i_offset] = i_val;
        ab_PaintedMask[i_offset] = (byte) i_valMask;
      }
      break;
    }

  }

  private void _setPointPositionZXData(final int _x, final int _y, int _value) {
    if (_x < 0 || _y < 0) {
      return;
    }

    if (ai_Painted == null) {
      return;
    }

    switch (graphicMode) {
      case ZXPOLY: {
        int i_cx = _x / 8;
        if (i_cx >= columnNumber) {
          return;
        }

        int i_offset = 0;

        int i_y = _y;

        if (flagZXVideoLineAddressing) {
          i_y = VideoMode.linearYtoZXY(_y);
        }
        i_offset = i_CurrentPosition + i_y * columnNumber + i_cx;

        if (i_offset < 0 || i_offset >= ai_Painted.length) {
          return;
        }

        Graphics p_g = bufferImage.getGraphics();

        int i_x = _x % 8;
        int i_sss = (28 - i_x * 4);
        int i_mask = 0xF << i_sss;

        int i_val = (ai_Painted[i_offset] >>> i_sss) & 0xF;
        int i_valMask = (ab_PaintedMask[i_offset] >>> (7 - i_x)) & 1;

        if (_value != 0) {
          ab_DataSource[i_offset] |= (1 << (7 - i_x));
        }
        else {
          ab_DataSource[i_offset] &= ~(1 << (7 - i_x));
        }

        if (i_valMask != 0) {
          p_g.setColor(ZXPalette.COLORS[i_val]);
        }
        else {
          switch (_getPointPositionZX(_x, _y)) {
            case 0:
              p_g.setColor(flagShowInverted ? COLOR_PIXEL_ON : COLOR_PIXEL_OFF);
              break;
            case 1:
              p_g.setColor(flagShowInverted ? COLOR_PIXEL_OFF : COLOR_PIXEL_ON);
              break;
            default:
              p_g.setColor(Color.PINK);
          }
        }

        p_g.drawLine(_x, _y, _x, _y);
      }
      break;
      case ZX_512x384: {
        int i_cx = (_x / 2) / 8;
        if (i_cx >= columnNumber) {
          return;
        }

        int i_y = _y / 2;

        if (flagZXVideoLineAddressing) {
          i_y = VideoMode.linearYtoZXY(i_y);
        }
        int i_offset = i_CurrentPosition + i_y * columnNumber + i_cx;

        if (i_offset < 0 || i_offset >= ai_Painted.length) {
          return;
        }

        Graphics p_g = bufferImage.getGraphics();

        int i_x = (_x / 2) % 8;
        int i_sss = (28 - i_x * 4);
        int i_mask = 0xF << i_sss;

        int i_val = (ai_Painted[i_offset] >>> i_sss) & 0xF;
        int i_valMask = (ab_PaintedMask[i_offset] >>> (7 - i_x)) & 1;

        if (_value != 0) {
          ab_DataSource[i_offset] |= (1 << (7 - i_x));
        }
        else {
          ab_DataSource[i_offset] &= ~(1 << (7 - i_x));
        }

        if (i_valMask != 0) {
          _draw512x384pixelBlock(p_g, _x & 0xFFFFFFFE, _y & 0xFFFFFFFE, i_val);
        }
        else {
          switch (_getPointPositionZX(_x, _y)) {
            case 0:
              p_g.setColor(flagShowInverted ? COLOR_PIXEL_ON : COLOR_PIXEL_OFF);
              break;
            case 1:
              p_g.setColor(flagShowInverted ? COLOR_PIXEL_OFF : COLOR_PIXEL_ON);
              break;
            default:
              p_g.setColor(Color.PINK);
          }

          p_g.fillRect(_x & 0xFFFFFFFE, _y & 0xFFFFFFFE, 2, 2);
        }
      }
      break;
    }

  }

  private int _getPointPositionZX(int _x, int _y) {
    if (_x < 0 || _y < 0 || ab_DataSource == null) {
      return -1;
    }

    if (graphicMode == VideoMode.ZX_512x384) {
      _x >>= 1;
      _y >>= 1;
    }

    int i_cx = _x / 8;

    if (i_cx >= columnNumber) {
      return -1;
    }

    int i_offset = 0;

    if (_x < 0 || _x > 255 || _y < 0 || _y > 191) {
      return -1;
    }

    if (flagZXVideoLineAddressing) {
      _y = VideoMode.linearYtoZXY(_y);
    }
    i_offset = i_CurrentPosition + _y * columnNumber + i_cx;

    if (i_offset < 0 || i_offset >= ab_DataSource.length) {
      return -1;
    }
    int i_mask = 0x80 >>> (_x % 8);
    int i_val = ab_DataSource[i_offset];
    return (i_val & i_mask) == 0 ? 0 : 1;

  }

  private int _getPointPositionPoly(int _x, int _y) {
    if (_x < 0 || _y < 0 || ab_DataSource == null) {
      return -1;
    }
    int i_cx = _x / 8;
    if (i_cx >= columnNumber) {
      return -1;
    }

    int i_offset = 0;

    if (flagZXVideoLineAddressing) {
      _y = VideoMode.linearYtoZXY(_y);
    }

    if (_x < 0 || _x > 255 || _y < 0 || _y > 191) {
      return -1;
    }

    i_offset = i_CurrentPosition + _y * columnNumber + i_cx;

    if (i_offset < 0 || i_offset >= ab_DataSource.length) {
      return -1;
    }
    int i_sdvig = _x % 8;
    int i_valcolors = ai_Painted[i_offset];
    int i_valmask = ab_PaintedMask[i_offset];
    if ((i_valmask & (0x80 >>> i_sdvig)) != 0) {
      return (i_valcolors >>> (28 - i_sdvig * 4)) & 0xF;
    }
    else {
      return -1;
    }
  }

  public void loadFromStream(InputStream _instr) throws IOException {
    DataInputStream p_inStream = new DataInputStream(_instr);

    if (!"ZXEC".equals(p_inStream.readUTF())) {
      throw new IOException("It's not a project file");
    }

    int i_version = p_inStream.readInt();

    if (i_version > 0x00010001) {
      throw new IOException("File has an unsupported format version");
    }

    int i_lColorInkIndex = p_inStream.readInt();
    int i_lColorPaintIndex = p_inStream.readInt();
    int i_lCurrentCols = p_inStream.readInt();
    int i_lCurrentPosition = p_inStream.readInt();
    Tool i_lCurrentTool = Tool.values()[p_inStream.readInt()];
    int i_lZoom = p_inStream.readInt();

    boolean lg_lGrid = p_inStream.readBoolean();
    boolean lg_lInverted = p_inStream.readBoolean();
    boolean lg_lPositionLocked = p_inStream.readBoolean();
    boolean lg_lZXscreenMode = p_inStream.readBoolean();

    boolean lg_lShowColumnBorders = false;

    if (i_version > 0x00010000) {
      lg_lShowColumnBorders = p_inStream.readBoolean();
    }

    Rectangle p_SelRect = null;

    if (p_inStream.readBoolean()) {
      p_SelRect = new Rectangle();
      p_SelRect.x = p_inStream.readInt();
      p_SelRect.y = p_inStream.readInt();
      p_SelRect.width = p_inStream.readInt();
      p_SelRect.height = p_inStream.readInt();
    }

    int i_len = p_inStream.readInt();
    int[] ai_lPainted = new int[i_len];
    for (int li = 0; li < ai_lPainted.length; li++) {
      ai_lPainted[li] = p_inStream.readInt();
    }

    i_len = p_inStream.readInt();
    byte[] ab_lPaintedMask = new byte[i_len];
    p_inStream.readFully(ab_lPaintedMask);

    HobetaContainer p_lHobetaContainer = new HobetaContainer();
    p_lHobetaContainer.loadFromStream(p_inStream);

    i_ColorInkIndex = i_lColorInkIndex;
    i_ColorPaintIndex = i_lColorPaintIndex;
    columnNumber = i_lCurrentCols;
    i_CurrentPosition = i_lCurrentPosition;
    i_CurrentTool = i_lCurrentTool;
    zoomFactor = i_lZoom;

    flagShowGrid = lg_lGrid;
    flagShowInverted = lg_lInverted;
    flagPositionLock = lg_lPositionLocked;
    flagZXVideoLineAddressing = lg_lZXscreenMode;

    flagShowColumnBorder = lg_lShowColumnBorders;

    ai_Painted = ai_lPainted;
    ab_PaintedMask = ab_lPaintedMask;

    p_HobetaContainer = p_lHobetaContainer;
    ab_DataSource = p_HobetaContainer.getDataArray();

    p_SelectRectangle = p_SelRect;

    redrawImageBuffer();
    repaint();
  }

  public void saveToOutputStream(OutputStream _outstr) throws IOException {
    DataOutputStream p_out = new DataOutputStream(_outstr);

    p_out.writeUTF("ZXEC");
    p_out.writeInt(0x00010001);

    p_out.writeInt(i_ColorInkIndex);
    p_out.writeInt(i_ColorPaintIndex);
    p_out.writeInt(columnNumber);
    p_out.writeInt(i_CurrentPosition);
    p_out.writeInt(i_CurrentTool.ordinal());
    p_out.writeInt(zoomFactor);

    p_out.writeBoolean(flagShowGrid);
    p_out.writeBoolean(flagShowInverted);
    p_out.writeBoolean(flagPositionLock);
    p_out.writeBoolean(flagZXVideoLineAddressing);

    p_out.writeBoolean(flagShowColumnBorder);

    if (p_SelectRectangle != null) {
      p_out.writeBoolean(true);
      p_out.writeInt(p_SelectRectangle.x);
      p_out.writeInt(p_SelectRectangle.y);
      p_out.writeInt(p_SelectRectangle.width);
      p_out.writeInt(p_SelectRectangle.height);
    }
    else {
      p_out.writeBoolean(false);
    }

    p_out.writeInt(ai_Painted.length);
    for (int li = 0; li < ai_Painted.length; li++) {
      p_out.writeInt(ai_Painted[li]);
    }

    p_out.writeInt(ab_PaintedMask.length);
    p_out.write(ab_PaintedMask);

    p_HobetaContainer.saveToStream(p_out);

    p_UndoStack.clear();

    p_out.flush();
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    penWidth = ((BoundedRangeModel) e.getSource()).getValue();
    repaint();
  }
}
