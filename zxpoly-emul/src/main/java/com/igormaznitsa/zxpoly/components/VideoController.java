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
package com.igormaznitsa.zxpoly.components;

import java.awt.*;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.image.*;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicIntegerArray;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;
import javax.swing.JComponent;

public final class VideoController extends JComponent implements ZXPoly, MouseWheelListener, IODevice {

  private static final Logger log = Logger.getLogger("VC");
  private static final long serialVersionUID = -6290427036692912036L;

  private final Motherboard board;
  private final ReentrantLock bufferLocker = new ReentrantLock();
  private final BufferedImage buffer;
  private final int[] dataBuffer;

  private final ZXPolyModule[] modules;
  private volatile int currentVideoMode = VIDEOMODE_RESERVED2;

  private Dimension size = new Dimension(512, 384);
  private volatile float zoom = 1.0f;
  private volatile int portFEw = 0;

  private volatile boolean holdMouse = false;

  public static final long CYCLES_BETWEEN_INT = 20000000L / (1000000000L / Motherboard.CPU_FREQ);
  private static final int BORDER_LINES = 64;
  private static final long MCYCLES_PER_BORDER_LINE = CYCLES_BETWEEN_INT / BORDER_LINES;
  private final byte[] borderLineColors = new byte[BORDER_LINES];

  private static final int[] ZXPALETTE = new int[]{
    0xFF000000,
    0xFF0000BE,
    0xFFBE0000,
    0xFFBE00BE,
    0xFF00BE00,
    0xFF00BEBE,
    0xFFBEBE00,
    0xFFBEBEBE,
    0xFF000000,
    0xFF0000FF,
    0xFFFF0000,
    0xFFFF00FF,
    0xFF00FF00,
    0xFF00FFFF,
    0xFFFFFF00,
    0xFFFFFFFF};

  public static final Color[] ZXPALETTE_AS_COLORS = new Color[]{
    // normal bright
    new Color(0, 0, 0), // Black
    new Color(0, 0, 190), // Blue
    new Color(190, 0, 0), // Red
    new Color(190, 0, 190),
    new Color(0, 190, 0), // Green
    new Color(0, 190, 190),
    new Color(190, 190, 0),
    new Color(190, 190, 190),
    // high bright  
    new Color(0, 0, 0),
    new Color(0, 0, 255),
    new Color(255, 0, 0),
    new Color(255, 0, 255),
    new Color(0, 255, 0),
    new Color(0, 255, 255),
    new Color(255, 255, 0),
    new Color(255, 255, 255)
  };

  public VideoController (final Motherboard board) {
    super();

    this.board = board;
    this.modules = board.getZXPolyModules();

    this.buffer = new BufferedImage(512, 384, BufferedImage.TYPE_INT_ARGB);
    this.dataBuffer = ((DataBufferInt) this.buffer.getRaster().getDataBuffer()).getData();

    this.addMouseWheelListener(this);
  }

  public void setHoldMouse (final boolean flag) {
    this.holdMouse = flag;
    if (flag) {
      setCursor(Toolkit.getDefaultToolkit().createCustomCursor(new BufferedImage(1, 1, BufferedImage.TRANSLUCENT), new Point(0, 0), "InvisibleCursor"));
    }
    else {
      setCursor(Cursor.getDefaultCursor());
    }
  }

  public boolean isHoldMouse () {
    return this.holdMouse;
  }

  @Override
  public void mouseWheelMoved (final MouseWheelEvent e) {
    if (e.isControlDown()) {
      final float newzoom;
      if (e.getPreciseWheelRotation() > 0) {
        newzoom = Math.max(1.0f, this.zoom - 0.2f);
      }
      else {
        newzoom = Math.min(5.0f, this.zoom + 0.2f);
      }
      if (newzoom != this.zoom) {
        updateZoom(newzoom);
      }
    }
  }

  @Override
  public Dimension getPreferredSize () {
    return this.size;
  }

  @Override
  public Dimension getMinimumSize () {
    return this.size;
  }

  private void updateZoom (final float value) {
    this.zoom = value;
    this.size = new Dimension(Math.round(512 * value), Math.round(384 * value));

    revalidate();
    repaint();
  }

  public static int extractYFromAddress (final int address) {
    return ((address & 0x1800) >> 5) | ((address & 0x700) >> 8) | ((address & 0xE0) >> 2);
  }

  public static int calcAttributeAddressZXMode (final int screenOffset) {
    final int line = ((screenOffset >>> 5) & 0x07) | ((screenOffset >>> 8) & 0x18);
    final int column = screenOffset & 0x1F;
    final int off = ((line >>> 3) << 8) | (((line & 0x07) << 5) | column);
    return 0x1800 + off;
  }

  private void drawBorder (final Graphics2D g, final int width, final int height) {
    int curindex = -1;
    int y = 0;
    int curheight = height;
    final int lineHeight = (height + (BORDER_LINES >> 1)) / BORDER_LINES;
    for (final byte c : this.borderLineColors) {
      if (curindex != c) {
        curindex = c;
        g.setColor(ZXPALETTE_AS_COLORS[c]);
        g.fillRect(0, y, width, curheight);
      }
      y += lineHeight;
      curheight -= lineHeight;
    }
    Arrays.fill(this.borderLineColors, (byte) (this.portFEw & 7));
  }

  @Override
  public void paintComponent (final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g;

    final Rectangle bounds = this.getBounds();

    final int width = bounds.width;
    final int height = bounds.height;

    final int xoff = (width - this.size.width) / 2;
    final int yoff = (height - this.size.height) / 2;

    if (xoff > 0 || yoff > 0) {
      drawBorder(g2, width, height);
    }
    this.drawBuffer(g2, xoff, yoff, this.zoom);
  }

  private int extractInkColor (final int attribute, final boolean flashActive) {
    final int bright = (attribute & 0x40) == 0 ? 0 : 0x08;
    final int inkColor = ZXPALETTE[(attribute & 0x07) | bright];
    final int paperColor = ZXPALETTE[((attribute >> 3) & 0x07) | bright];
    final boolean flash = (attribute & 0x80) != 0;

    final int result;

    if (flash) {
      if (flashActive) {
        result = paperColor;
      }
      else {
        result = inkColor;
      }
    }
    else {
      result = inkColor;
    }
    return result;
  }

  private int extractPaperColor (final int attribute, final boolean flashActive) {
    final int bright = (attribute & 0x40) == 0 ? 0 : 0x08;
    final int inkColor = ZXPALETTE[(attribute & 0x07) | bright];
    final int paperColor = ZXPALETTE[((attribute >> 3) & 0x07) | bright];
    final boolean flash = (attribute & 0x80) != 0;

    final int result;

    if (flash) {
      if (flashActive) {
        result = inkColor;
      }
      else {
        result = paperColor;
      }
    }
    else {
      result = paperColor;
    }
    return result;
  }

  private void refreshBufferData () {
    final boolean isflash = this.board.isFlashActive();

    switch (this.currentVideoMode) {
      case VIDEOMODE_RESERVED1:
      case VIDEOMODE_RESERVED2:
      case VIDEOMODE_ZX48_CPU0:
      case VIDEOMODE_ZX48_CPU1:
      case VIDEOMODE_ZX48_CPU2:
      case VIDEOMODE_ZX48_CPU3: {
        final ZXPolyModule sourceModule = this.modules[this.currentVideoMode & 0x3];

        int offset = 0;
        int attributeoffset = 0;

        for (int i = 0; i < 0x1800; i++) {
          if ((i & 0x1F) == 0) {
            offset = extractYFromAddress(i) << 10;
            attributeoffset = calcAttributeAddressZXMode(i);
          }

          final int attribute = sourceModule.readVideoMemory(attributeoffset++);
          final int inkColor = extractInkColor(attribute, isflash);
          final int paperColor = extractPaperColor(attribute, isflash);

          int videoValue = sourceModule.readVideoMemory(i);
          int x = 8;
          while (x-- > 0) {
            final int color = (videoValue & 0x80) == 0 ? paperColor : inkColor;
            videoValue <<= 1;

            this.dataBuffer[offset] = color;
            this.dataBuffer[offset + 512] = color;
            this.dataBuffer[++offset] = color;
            this.dataBuffer[offset++ + 512] = color;
          }
        }
      }
      break;
      case VIDEOMODE_ZXPOLY_256x192: {
        int offset = 0;

        for (int i = 0; i < 0x1800; i++) {
          if ((i & 0x1F) == 0) {
            offset = extractYFromAddress(i) << 10;
          }

          int videoValue0 = this.modules[0].readVideoMemory(i);
          int videoValue1 = this.modules[1].readVideoMemory(i);
          int videoValue2 = this.modules[2].readVideoMemory(i);
          int videoValue3 = this.modules[3].readVideoMemory(i);

          int x = 8;
          while (x-- > 0) {
            final int value = ((videoValue3 & 0x80) == 0 ? 0 : 0x08)
                | ((videoValue0 & 0x80) == 0 ? 0 : 0x04)
                | ((videoValue1 & 0x80) == 0 ? 0 : 0x02)
                | ((videoValue2 & 0x80) == 0 ? 0 : 0x01);

            videoValue0 <<= 1;
            videoValue1 <<= 1;
            videoValue2 <<= 1;
            videoValue3 <<= 1;

            final int color = ZXPALETTE[value];

            this.dataBuffer[offset] = color;
            this.dataBuffer[offset + 512] = color;
            this.dataBuffer[++offset] = color;
            this.dataBuffer[offset++ + 512] = color;
          }
        }
      }
      break;
      case VIDEOMODE_ZXPOLY_512x384: {
        int offset = 0;
        int attributeoffset = 0;

        for (int i = 0; i < 0x1800; i++) {
          if ((i & 0x1F) == 0) {
            offset = extractYFromAddress(i) << 10;
            attributeoffset = calcAttributeAddressZXMode(i);
          }

          int videoValue0 = this.modules[0].readVideoMemory(i);
          int videoValue1 = this.modules[1].readVideoMemory(i);
          int videoValue2 = this.modules[2].readVideoMemory(i);
          int videoValue3 = this.modules[3].readVideoMemory(i);

          final int attribute0 = this.modules[0].readVideoMemory(attributeoffset);
          final int attribute1 = this.modules[1].readVideoMemory(attributeoffset);
          final int attribute2 = this.modules[2].readVideoMemory(attributeoffset);
          final int attribute3 = this.modules[3].readVideoMemory(attributeoffset++);

          int x = 8;
          while (x-- > 0) {
            this.dataBuffer[offset] = (videoValue0 & 0x80) == 0 ? extractPaperColor(attribute0, isflash) : extractInkColor(attribute0, isflash);
            this.dataBuffer[offset + 512] = (videoValue2 & 0x80) == 0 ? extractPaperColor(attribute2, isflash) : extractInkColor(attribute2, isflash);
            this.dataBuffer[++offset] = (videoValue1 & 0x80) == 0 ? extractPaperColor(attribute1, isflash) : extractInkColor(attribute1, isflash);
            this.dataBuffer[offset++ + 512] = (videoValue3 & 0x80) == 0 ? extractPaperColor(attribute3, isflash) : extractInkColor(attribute3, isflash);
            videoValue0 <<= 1;
            videoValue1 <<= 1;
            videoValue2 <<= 1;
            videoValue3 <<= 1;

          }
        }
      }
      break;
      default:
        throw new Error("Unexpected video mode [" + this.currentVideoMode + ']');
    }
  }

  public void drawBuffer (final Graphics2D gfx, final int x, final int y, final float zoom) {
    lockBuffer();
    try {
      if (zoom == 1.0f) {
        gfx.drawImage(this.buffer, null, x, y);
      }
      else {
        final float nzoom = Math.max(1.0f, zoom);
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        gfx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        gfx.drawImage(this.buffer, x, y, Math.round(512 * nzoom), Math.round(384 * nzoom), null);
      }
    }
    finally {
      unlockBuffer();
    }
  }

  private static String decodeVideoModeCode (final int code) {
    switch (code) {
      case 0:
        return "ZX-Spectrum 0";
      case 1:
        return "ZX-Spectrum 1";
      case 2:
        return "ZX-Spectrum 2";
      case 3:
        return "ZX-Spectrum 3";
      case 4:
        return "ZX-Poly 256x192";
      case 5:
        return "ZX-Poly 512x384";
      case 6:
        return "Reserved 6";
      case 7:
        return "Reserved 7";
      default:
        return "Unknown [" + code + ']';
    }
  }

  public int getVideoMode () {
    return this.currentVideoMode;
  }

  public void setVideoMode (final int newVideoMode) {
    lockBuffer();
    try {
      if (this.currentVideoMode != newVideoMode) {
        log.info("mode set: " + decodeVideoModeCode(newVideoMode));
        this.currentVideoMode = newVideoMode;
        refreshBufferData();
      }
    }
    finally {
      unlockBuffer();
    }
  }

  public void setBorderColor (final int colorIndex) {
    this.portFEw |= (this.portFEw & 7) | (colorIndex & 0x07);
    Arrays.fill(this.borderLineColors, (byte) colorIndex);
  }

  public void lockBuffer () {
    bufferLocker.lock();
  }

  public void unlockBuffer () {
    bufferLocker.unlock();
  }

  public void updateBuffer () {
    lockBuffer();
    try {
      this.refreshBufferData();
    }
    finally {
      unlockBuffer();
    }
  }

  public RenderedImage makeCopyOfCurrentPicture () {
    this.lockBuffer();
    try {
      final BufferedImage result = new BufferedImage(this.buffer.getWidth(), this.buffer.getHeight(), BufferedImage.TYPE_INT_RGB);
      final Graphics g = result.getGraphics();
      g.drawImage(this.buffer, 0, 0, this);
      g.dispose();
      return result;
    }
    finally {
      this.unlockBuffer();
    }
  }

  @Override
  public Motherboard getMotherboard () {
    return this.board;
  }

  @Override
  public int readIO (final ZXPolyModule module, final int port) {
    return 0;
  }

  @Override
  public void writeIO (final ZXPolyModule module, final int port, final int value) {
    if (!module.isTRDOSActive() && (port & 0xFF) == 0xFE) {
      this.portFEw = value & 0xFF;

      int borderLineIndex;
      final long machineCycles = module.getCPU().getMachineCycles();
      if (module.isMaster()) {
        borderLineIndex = (int) (((machineCycles << 8) / MCYCLES_PER_BORDER_LINE) >> 8);
      }
      else {
        borderLineIndex = (int) (machineCycles % BORDER_LINES);
      }
      if (borderLineIndex >= 0 && borderLineIndex < BORDER_LINES) {
        this.borderLineColors[borderLineIndex] = (byte) (this.portFEw & 0x7);
      }
    }
  }

  @Override
  public void preStep (final boolean signalReset, final boolean signalInt) {
    if (signalReset) {
      this.portFEw = 0x00;
    }
  }

  @Override
  public void doReset () {
  }

  @Override
  public void postStep (long spentMachineCyclesForStep) {
  }

  public float getZoom () {
    return this.zoom;
  }

  public int getScrYForZXScr (final int zxY) {
    final int height = getHeight();
    final int yoff = (height - this.size.height) / 2;
    return (zxY * Math.round(this.zoom * 2)) + yoff;
  }

  public int getZXScrY (final int compoY) {
    final int height = getHeight();
    final int yoff = (height - this.size.height) / 2;

    final int result = (compoY - yoff) / Math.round(this.zoom * 2);
    return Math.max(0x00, Math.min(191, result));
  }

  public int getZXScrX (final int compoX) {
    final int width = getWidth();
    final int xoff = (width - this.size.width) / 2;

    final int result = (compoX - xoff) / Math.round(this.zoom * 2);
    return Math.max(0x00, Math.min(0xFF, result));
  }

  public int getScrXForZXScr (final int zxX) {
    final int width = getWidth();
    final int xoff = (width - this.size.width) / 2;
    return (zxX * Math.round(this.zoom * 2)) + xoff;
  }
}
