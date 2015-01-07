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

  private int zoom = 1;

  private volatile int portFEw = 0;

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

  public VideoController(final Motherboard board) {
    super();
    this.board = board;
    this.modules = board.getZXPolyModules();

    this.buffer = new BufferedImage(512, 384, BufferedImage.TYPE_INT_ARGB);
    this.dataBuffer = ((DataBufferInt) this.buffer.getRaster().getDataBuffer()).getData();

    this.addMouseWheelListener(this);
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent e) {
    final int newzoom;
    if (e.getPreciseWheelRotation() > 0) {
      newzoom = Math.max(1, this.zoom - 1);
    }
    else {
      newzoom = Math.min(4, this.zoom + 1);
    }
    if (newzoom != this.zoom) {
      updateZoom(newzoom);
    }
  }

  @Override
  public Dimension getPreferredSize() {
    return this.size;
  }

  @Override
  public Dimension getMinimumSize() {
    return this.size;
  }

  private void updateZoom(final int value) {
    this.zoom = value;
    this.size = new Dimension(512 * value, 384 * value);

    revalidate();
    repaint();
  }

  public static int extractYFromAddress(final int address) {
    return ((address & 0x1800) >> 5) | ((address & 0x700) >> 8) | ((address & 0xE0) >> 2);
  }

  public static int calcAttributeAddressZXMode(final int screenOffset) {
    final int line = ((screenOffset >>> 5) & 0x07) | ((screenOffset >>> 8) & 0x18);
    final int column = screenOffset & 0x1F;
    final int off = ((line >>> 3) << 8) | (((line & 0x07) << 5) | column);
    return 0x1800 + off;
  }

  @Override
  public void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g;

    final int width = getWidth();
    final int height = getHeight();

    final int xoff = (width - this.size.width) / 2;
    final int yoff = (height - this.size.height) / 2;

    if (xoff>0 || yoff>0){
      g2.setColor(ZXPALETTE_AS_COLORS[this.portFEw & 7]);
      g2.fillRect(0, 0, width, height);
    }
    this.drawBuffer(g2, xoff, yoff, this.zoom);
  }

  private int extractInkColor(final int attribute, final boolean flashActive) {
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

  private int extractPaperColor(final int attribute, final boolean flashActive) {
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

  private void refreshBufferData() {
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

  public void drawBuffer(final Graphics2D gfx, final int x, final int y, final int zoom) {
    lockBuffer();
    try {
      if (zoom < 2) {
        gfx.drawImage(this.buffer, null, x, y);
      }
      else {
        final int nzoom = Math.max(1, zoom);
        gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
        gfx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);
        gfx.drawImage(this.buffer, x, y, 512 * nzoom, 384 * nzoom, null);
      }
    }
    finally {
      unlockBuffer();
    }
  }

  private static String decodeVideoModeCode(final int code) {
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

  public int getVideoMode() {
    return this.currentVideoMode;
  }

  public void setVideoMode(final int newVideoMode) {
    lockBuffer();
    try {
      if (this.currentVideoMode != newVideoMode) {
        log.info("mode set: "+decodeVideoModeCode(newVideoMode));
        this.currentVideoMode = newVideoMode;
        refreshBufferData();
      }
    }
    finally {
      unlockBuffer();
    }
  }

  public void setBorderColor(final int colorIndex){
    this.portFEw |= (this.portFEw & 7) | (colorIndex & 0x07);
    repaint();
  }
  
  public void lockBuffer() {
    bufferLocker.lock();
  }

  public void unlockBuffer() {
    bufferLocker.unlock();
  }

  public void updateBuffer(){
    lockBuffer();
    try {
      this.refreshBufferData();
    }
    finally {
      unlockBuffer();
    }
  }
  
  @Override
  public Motherboard getMotherboard() {
    return this.board;
  }

  @Override
  public int readIO(final ZXPolyModule module, final int port) {
    return 0;
  }

  @Override
  public void writeIO(final ZXPolyModule module, final int port, final int value) {
    if (!module.isTRDOSActive() && (port & 0xFF) == 0xFE) {
      final int old = this.portFEw;
      this.portFEw = value & 0xFF;
      if (((this.portFEw ^ old) & 7) != 0) {
        repaint();
      }
    }
  }

  @Override
  public void preStep(final boolean signalReset, final boolean signalInt) {
    if (signalReset) {
      this.portFEw = 0x00;
    }
  }
}
