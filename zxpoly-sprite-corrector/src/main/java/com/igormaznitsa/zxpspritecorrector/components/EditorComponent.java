package com.igormaznitsa.zxpspritecorrector.components;

import com.igormaznitsa.zxpspritecorrector.utils.ZXPalette;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

public final class EditorComponent extends JComponent {

  private static final long serialVersionUID = -6948149982924499351L;

  private static final Stroke GRID_STROKE = new BasicStroke(0.3f);
  private static final Stroke COLUMN_BORDER_STROKE = new BasicStroke(0.7f);
  private static final Stroke TOOL_AREA_STROKE = new BasicStroke(2.3f);

  private Color colorToolArea = Color.WHITE;

  private Color colorPixelOn = Color.GRAY;
  private Color colorPixelOff = Color.DARK_GRAY;
  private Color colorZX512On = Color.YELLOW;
  private Color colorZX512Off = Color.BLUE;
  private Color colorGrid = Color.ORANGE;
  private Color colorColumnBorder = Color.CYAN;

  private BufferedImage image;
  private boolean mode512;

  private boolean invertShowBaseData;
  private boolean showColumnBorders = true;
  private boolean showGrid = true;
  private boolean addressingModeZXScreen;
  private Dimension preferredSize;
  private int zoom = 1;
  private int columns = 32;
  private ZXPolyData processingData;
  private int startAddress;

  private Rectangle toolArea;

  private int gridStep = 1;

  private final ZXGraphics zxGraphics = new ZXGraphics();

  private static final RenderingHints RENDERING_IMAGE_HINTS = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
  private static final RenderingHints RENDERING_LINE_HINTS = new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

  static {
    RENDERING_IMAGE_HINTS.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_SPEED);
    RENDERING_IMAGE_HINTS.put(RenderingHints.KEY_DITHERING, RenderingHints.VALUE_DITHER_DISABLE);

    RENDERING_LINE_HINTS.put(RenderingHints.KEY_COLOR_RENDERING, RenderingHints.VALUE_COLOR_RENDER_QUALITY);
  }

  public Point mousePoint2ScreenPoint(final Point pointAtComponent) {
    if (pointAtComponent == null) {
      return null;
    }
    return new Point(pointAtComponent.x / this.zoom, pointAtComponent.y / this.zoom);
  }

  public class ZXGraphics {

    private ZXGraphics() {

    }

    private int coordToAddress(final int x, final int y) {
      final int result;

      if (processingData == null || x < 0 || y < 0) {
        result = -1;
      }
      else if (mode512) {
        final int theY = addressingModeZXScreen ? VideoMode.zxy2y(y >> 1) : y >> 1;
        final int rowAddress = theY * columns + startAddress;

        if (x >= (columns << 4) || rowAddress >= processingData.length()) {
          result = -1;
        }
        else {
          result = (x >>> 4) + rowAddress;
        }
      }
      else {
        final int theY = addressingModeZXScreen ? VideoMode.zxy2y(y) : y;
        final int rowAddress = theY * columns + startAddress;

        if (x >= (columns << 3) || rowAddress >= processingData.length()) {
          result = -1;
        }
        else {
          result = (x >>> 3) + rowAddress;
        }
      }

      return result;
    }

    private int makeXMask(final int x) {
      return 1 << (7 - ((mode512 ? (x >> 1) : x) & 0x7));
    }

    public ZXGraphics setPoint(final int x, final int y, final int cpu3012) {
      final int address = coordToAddress(x, y);
      if (address >= 0) {
        final int bitmask = makeXMask(x);
        final int mask = processingData.getMask(address);

        final int packed3012 = processingData.getPackedZxPolyData3012(address);
        final int invertedbitmask = ~bitmask;
        processingData.setZXPolyData(address, mask | bitmask,
                ((packed3012 >>> 16) & invertedbitmask) | (((cpu3012 & 4) == 0 ? 0 : 0xFF) & bitmask),
                ((packed3012 >>> 8) & invertedbitmask) | (((cpu3012 & 2) == 0 ? 0 : 0xFF) & bitmask),
                (packed3012 & invertedbitmask) | (((cpu3012 & 1) == 0 ? 0 : 0xFF) & bitmask),
                ((packed3012 >>> 24) & invertedbitmask) | (((cpu3012 & 8) == 0 ? 0 : 0xFF) & bitmask)
        );
      }

      return this;
    }

    public ZXGraphics resetPoint(final int x, final int y) {
      final int address = coordToAddress(x, y);
      if (address >= 0) {
        final int bitmask = makeXMask(x);
        final int mask = processingData.getMask(address);

        final int packed3012 = processingData.getPackedZxPolyData3012(address);
        final int invertedbitmask = ~bitmask;
        processingData.setZXPolyData(address, mask & invertedbitmask,
                (packed3012 >>> 16) & invertedbitmask,
                (packed3012 >>> 8) & invertedbitmask,
                packed3012 & invertedbitmask,
                (packed3012 >>> 24) & invertedbitmask
        );
      }

      return this;
    }

    public int getPoint3012(final int x, final int y) {
      final int address = coordToAddress(x, y);

      int result = 0;

      if (address >= 0) {
        final int bitmask = makeXMask(x);

        if ((processingData.getMask(address) & bitmask) == 0) {
          result = 0;
        }
        else {
          final int packed3012 = processingData.getPackedZxPolyData3012(address);
          result = ((packed3012 & bitmask) == 0 ? 0 : 1)
                  | ((packed3012 & (bitmask << 8)) == 0 ? 0 : 2)
                  | ((packed3012 & (bitmask << 16)) == 0 ? 0 : 4)
                  | ((packed3012 & (bitmask << 24)) == 0 ? 0 : 8);
        }
      }

      return result;
    }

    public boolean isBaseBitSet(final int x, final int y) {
      final int address = coordToAddress(x, y);
      if (address >= 0) {
        return (processingData.getBaseData(address) & makeXMask(x)) != 0;
      }

      return false;
    }

    public void flush() {
      _updatePictureInBuffer();
      repaint();
    }
  }

  public ZXGraphics getZXGraphics() {
    return this.zxGraphics;
  }

  public void setToolArea(final Rectangle rect) {
    this.toolArea = rect;
    repaint();
  }

  public Rectangle getToolArea() {
    return this.toolArea;
  }

  public ZXPolyData getProcessingData() {
    return this.processingData;
  }

  public void setProcessingData(final ZXPolyData data) {
    this.processingData = data;
    _updatePictureInBuffer();
    repaint();
  }

  public void setColumns(final int value) {
    this.columns = Math.max(1, Math.min(32, value));
    _updatePictureInBuffer();
    repaint();
  }

  public int getColumns() {
    return this.columns;
  }

  public void setInvertShowBaseData(final boolean flag) {
    this.invertShowBaseData = flag;
    _updatePictureInBuffer();
    repaint();
  }

  public boolean isInvertShowBaseData() {
    return this.invertShowBaseData;
  }

  public void setShowColumnBorders(final boolean flag) {
    this.showColumnBorders = flag;
    repaint();
  }

  public boolean isShowColumnBorders() {
    return this.showColumnBorders;
  }

  public boolean isShowGrid() {
    return this.showGrid;
  }

  public void setShowGrid(final boolean flag) {
    this.showGrid = flag;
    repaint();
  }

  public int getGridStep() {
    return this.gridStep;
  }

  public void setGridStep(final int step) {
    this.gridStep = Math.max(1, Math.min(128, step));
    repaint();
  }

  public void setAddress(final int address) {
    if (this.processingData == null) {
      this.startAddress = 0;
    }
    else {
      this.startAddress = Math.max(0, Math.min(this.processingData.length() - 1, address));
    }
    _updatePictureInBuffer();
    repaint();
  }

  public int getAddress() {
    return this.startAddress;
  }

  @Override
  public boolean isFocusable() {
    return false;
  }

  public void setZXScreenMode(final boolean flag) {
    this.addressingModeZXScreen = flag;
    _updatePictureInBuffer();
    repaint();
  }

  public boolean isZXScreenMode() {
    return this.addressingModeZXScreen;
  }

  public Color getToolAreaColor() {
    return this.colorToolArea;
  }

  public void setToolAreaColor(final Color color) {
    this.colorToolArea = color;
    repaint();
  }

  public Color getColorPixelOn() {
    return colorPixelOn;
  }

  public void setColorPixelOn(final Color colorPixelOn) {
    this.colorPixelOn = colorPixelOn;
    repaint();
  }

  public Color getColorPixelOff() {
    return colorPixelOff;
  }

  public void setColorPixelOff(final Color colorPixelOff) {
    this.colorPixelOff = colorPixelOff;
    repaint();
  }

  public Color getColorZX512On() {
    return colorZX512On;
  }

  public void setColorZX512On(Color colorZX512On) {
    this.colorZX512On = colorZX512On;
    _updatePictureInBuffer();
    repaint();
  }

  public Color getColorZX512Off() {
    return colorZX512Off;
  }

  public void setColorZX512Off(Color colorZX512Off) {
    this.colorZX512Off = colorZX512Off;
    _updatePictureInBuffer();
    repaint();
  }

  public Color getColorGrid() {
    return colorGrid;
  }

  public void setColorGrid(Color colorGrid) {
    this.colorGrid = colorGrid;
    repaint();
  }

  public Color getColorColumnBorder() {
    return colorColumnBorder;
  }

  public void setColorColumnBorder(Color colorColumnBorder) {
    this.colorColumnBorder = colorColumnBorder;
    repaint();
  }

  public boolean isMode512() {
    return this.mode512;
  }

  public void setMode512(final boolean flag) {
    this.mode512 = flag;

    final int width = flag ? 512 : 256;
    final int height = flag ? 384 : 192;

    final BufferedImage newImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    this.image = newImage;
    _updatePictureInBuffer();
    _updatePreferredSize();

    revalidate();
    repaint();
  }

  public int getZoom() {
    return this.zoom;
  }

  public void zoomIn() {
    this.zoom = Math.min(this.zoom + 1, 10);
    _updatePreferredSize();
    revalidate();
    repaint();
  }

  public void zoomOut() {
    this.zoom = Math.max(this.zoom - 1, 1);
    _updatePreferredSize();
    revalidate();
    repaint();
  }

  private void _updatePictureInBuffer() {
    final Graphics2D gfx = this.image.createGraphics();

    gfx.setColor(Color.black);
    gfx.fillRect(0, 0, this.image.getWidth(), this.image.getHeight());

    if (this.processingData != null) {
      int column = 0;

      int x = 0;
      int y = 0;
      final int step = this.mode512 ? 2 : 1;

      for (int addr = this.startAddress; addr < this.processingData.length(); addr++) {
        int basedata = this.processingData.getBaseData(addr);
        int mask = this.processingData.getMask(addr);

        final int cury;
        if (this.addressingModeZXScreen) {
          cury = VideoMode.y2zxy(this.mode512 ? y >> 1 : y) << (this.mode512 ? 1 : 0);
        }
        else {
          cury = y;
        }

        final int packedData3012 = this.processingData.getPackedZxPolyData3012(addr);

        int data0 = packedData3012 >>> 16;
        int data1 = packedData3012 >>> 8;
        int data2 = packedData3012;
        int data3 = packedData3012 >>> 24;

        for (int i = 0; i < 8; i++) {
          if ((mask & 0x80) == 0) {
            // point of base
            if (this.invertShowBaseData) {
              gfx.setColor((basedata & 0x80) == 0 ? this.colorPixelOn : this.colorPixelOff);
            }
            else {
              gfx.setColor((basedata & 0x80) == 0 ? this.colorPixelOff : this.colorPixelOn);
            }
            gfx.fillRect(x, cury, step, step);
          }
          else {
            // point of a zxpoly mode
            if (this.mode512) {
              // 512x384 mode
              gfx.setColor((data0 & 0x80) == 0 ? this.colorZX512Off : this.colorPixelOn);
              gfx.drawLine(x, y, x, y);
              gfx.setColor((data1 & 0x80) == 0 ? this.colorZX512Off : this.colorPixelOn);
              gfx.drawLine(x + 1, y, x + 1, y);
              gfx.setColor((data2 & 0x80) == 0 ? this.colorZX512Off : this.colorPixelOn);
              gfx.drawLine(x, y + 1, x, y + 1);
              gfx.setColor((data3 & 0x80) == 0 ? this.colorZX512Off : this.colorPixelOn);
              gfx.drawLine(x + 1, y + 1, x + 1, y + 1);
            }
            else {
              // zxpoly mode
              final int colorIndex = ((data3 & 0x80) >>> 4) | ((data0 & 0x80) >>> 5) | ((data1 & 0x80) >>> 6) | ((data2 & 0x80)>>>7);
              gfx.setColor(ZXPalette.COLORS[colorIndex]);
              gfx.fillRect(x, cury, step, step);
            }
          }

          basedata <<= 1;
          data0 <<= 1;
          data1 <<= 1;
          data2 <<= 1;
          data3 <<= 1;
          mask <<= 1;
          x += step;
        }

        column++;
        if (column >= this.columns) {
          x = 0;
          column = 0;
          y += step;
          if (y >= this.image.getHeight()) {
            break;
          }
        }
      }
    }
    gfx.dispose();
  }

  private void _updatePreferredSize() {
    this.preferredSize = new Dimension(getWidth(), getHeight());
  }

  @Override
  public Dimension getPreferredSize() {
    return this.preferredSize;
  }

  @Override
  public Dimension getMinimumSize() {
    return this.preferredSize;
  }

  @Override
  public Dimension getMaximumSize() {
    return this.preferredSize;
  }

  @Override
  public int getWidth() {
    return this.image.getWidth() * this.zoom;
  }

  @Override
  public int getHeight() {
    return this.image.getHeight() * this.zoom;
  }

  public EditorComponent() {
    super();
    setMode512(false);
  }

  @Override
  public void paintComponent(final Graphics g) {
    final Graphics2D gfx = (Graphics2D) g;
    gfx.setRenderingHints(RENDERING_IMAGE_HINTS);

    final Dimension size = this.getPreferredSize();

    gfx.drawImage(this.image, 0, 0, size.width, size.height, null);

    if (this.zoom > 1) {
      gfx.setRenderingHints(RENDERING_LINE_HINTS);
      final int columnBorder = this.columns * (this.zoom << (this.mode512 ? 4 : 3));

      if (this.showGrid) {

        gfx.setStroke(GRID_STROKE);
        gfx.setColor(this.colorGrid);
        final int step = this.gridStep * this.zoom * (this.mode512 ? 2 : 1);
        for (int i = 0; i <= columnBorder; i += step) {
          gfx.drawLine(i, 0, i, size.height);
        }
        for (int i = 0; i < size.height; i += step) {
          gfx.drawLine(0, i, columnBorder, i);
        }
      }

      if (this.showColumnBorders) {
        final int step = this.zoom << (this.mode512 ? 4 : 3);
        gfx.setStroke(COLUMN_BORDER_STROKE);
        gfx.setColor(this.colorColumnBorder);
        for (int i = 0; i <= columnBorder; i += step) {
          gfx.drawLine(i, 0, i, size.height);
        }
      }
    }

    if (this.toolArea != null) {
      gfx.setRenderingHints(RENDERING_LINE_HINTS);
      gfx.setStroke(TOOL_AREA_STROKE);
      gfx.setColor(this.colorToolArea);
      gfx.drawRect(this.toolArea.x * this.zoom, this.toolArea.y * this.zoom, this.toolArea.width * this.zoom, this.toolArea.height * this.zoom);
      gfx.setColor(this.colorToolArea.darker().darker().darker());
      gfx.drawRect(this.toolArea.x * this.zoom - 1, this.toolArea.y * this.zoom - 1, this.toolArea.width * this.zoom + 1, this.toolArea.height * this.zoom + 1);
    }
  }
}
