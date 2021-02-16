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

package com.igormaznitsa.zxpoly.components.video;

import com.igormaznitsa.zxpoly.components.*;
import com.igormaznitsa.zxpoly.components.video.tvfilters.TvFilter;
import com.igormaznitsa.zxpoly.components.video.tvfilters.TvFilterChain;
import com.igormaznitsa.zxpoly.formats.Spec256Arch;
import com.igormaznitsa.zxpoly.utils.Utils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.igormaznitsa.zxpoly.components.Motherboard.TSTATES_PER_INT;
import static java.util.Arrays.fill;

public final class VideoController extends JComponent
        implements ZxPolyConstants, MouseWheelListener, IoDevice {

  public static final int SCREEN_WIDTH = 512;
  public static final int SCREEN_HEIGHT = 384;

  private VkbdRender vkbdRender;

  public static final int[] PALETTE_ZXPOLY = new int[]{
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
  public static final Color[] PALETTE_ZXPOLY_COLORS = new Color[]{
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
  public static final int[] PALETTE_SPEC256 = Utils.readRawPalette(
          VideoController.class.getResourceAsStream("/com/igormaznitsa/zxpoly/pal/spec256.raw.pal"),
          true);
  private static final int PREFERRED_BORDER_WIDTH = 64;
  private static final Dimension MINIMUM_SIZE =
          new Dimension(SCREEN_WIDTH + (PREFERRED_BORDER_WIDTH << 1),
                  SCREEN_HEIGHT + (PREFERRED_BORDER_WIDTH << 1));
  private static final int[] PALETTE_ALIGNED_ZXPOLY =
          Utils.alignPaletteColors(PALETTE_ZXPOLY, PALETTE_SPEC256);

  private static final int ZXSPEC_PIXEL_AREA_SIZE = 0x1C00;

  private static final Logger log = Logger.getLogger("VC");
  private static final long serialVersionUID = -6290427036692912036L;
  private static final Image MOUSE_TRAPPED = Utils.loadIcon("escmouse.png");
  private static final int BORDER_LINES = 40;
  private static final int TSTATES_PER_LINE = TSTATES_PER_INT / BORDER_LINES;
  private static final RenderedImage[] EMPTY_ARRAY = new RenderedImage[0];
  private static volatile boolean gfxBackOverFF = false;
  private static volatile boolean gfxPaper00InkFF = false;
  private static volatile boolean gfxHideSameInkPaper = true;
  private static volatile int gfxUpColorsMixed = 64;
  private static volatile int gfxDownColorsMixed = 0;
  private static volatile int[] gfxPrerenderedBack = null;

  private final Motherboard board;
  private final ReentrantLock bufferLocker = new ReentrantLock();
  private final BufferedImage bufferImage;
  private final int[] bufferImageRgbData;
  private final ZxPolyModule[] modules;
  private final byte[] borderLineColors = new byte[BORDER_LINES];
  private final short[] lastRenderedZxData = new short[ZXSPEC_PIXEL_AREA_SIZE];
  private long changedBorderLines = 0L;
  private volatile int currentVideoMode = VIDEOMODE_ZXPOLY_256x192_FLASH_MASK;
  private Dimension size = new Dimension(SCREEN_WIDTH, SCREEN_HEIGHT);
  private volatile float zoom = 1.0f;
  private volatile int portFEw = 0;
  private volatile boolean mouseTrapActive = false;
  private volatile boolean mouseTrapEnabled = false;
  private volatile boolean showVkb = false;
  private volatile TvFilterChain tvFilterChain = TvFilterChain.NONE;
  private volatile boolean enableMouseTrapIndicator = false;
  private int tstatesCounter = 0;

  public VideoController(final Motherboard board) {
    super();

    this.board = board;
    this.modules = board.getModules();

    this.bufferImage = new BufferedImage(SCREEN_WIDTH, SCREEN_HEIGHT, BufferedImage.TYPE_INT_RGB);
    this.bufferImage.setAccelerationPriority(1.0f);
    this.bufferImageRgbData =
            ((DataBufferInt) this.bufferImage.getRaster().getDataBuffer()).getData();

    this.addMouseWheelListener(this);
    this.addMouseListener(new MouseAdapter() {

      @Override
      public void mousePressed(final MouseEvent e) {
        if (!e.isConsumed() && !mouseTrapActive && showVkb) {
          vkbdRender.setLastMouseEvent(e);
          e.consume();
        }
      }

      @Override
      public void mouseReleased(final MouseEvent e) {
        if (!e.isConsumed() && !mouseTrapActive && showVkb) {
          vkbdRender.setLastMouseEvent(e);
          e.consume();
        }
      }
    });
  }

  public static void setGfxBack(final Spec256Arch.Spec256Bkg bkg) {
    if (bkg == null) {
      gfxPrerenderedBack = null;
    } else {
      final int yoffset = (bkg.getHeight() - 192) / 2;
      final int xoffset = (bkg.getWidth() - 256) / 2;
      final int[] prerendered = new int[SCREEN_WIDTH * SCREEN_HEIGHT];
      final byte[] imgData = bkg.getData();
      for (int y = 0; y < 192; y++) {
        for (int x = 0; x < 256; x++) {
          final int color =
                  PALETTE_SPEC256[imgData[(y + yoffset) * bkg.getWidth() + x + xoffset] & 0xFF];
          int zxoffset = y * 512 * 2 + x * 2;
          prerendered[zxoffset] = color;
          prerendered[zxoffset++ + SCREEN_WIDTH] = color;
          prerendered[zxoffset] = color;
          prerendered[zxoffset + SCREEN_WIDTH] = color;
        }
      }
      gfxPrerenderedBack = prerendered;
    }
  }

  @Override
  public void init() {
    this.vkbdRender = new VkbdRender(this.board);
  }

  public static int toZxPolyIndex(final byte spec256PaletteIndex) {
    final int spec256argb = PALETTE_SPEC256[spec256PaletteIndex & 0xFF];
    final int sr = (spec256argb >>> 16) & 0xFF;
    final int sg = (spec256argb >>> 8) & 0xFF;
    final int sb = spec256argb & 0xFF;

    double minDistance = Double.MAX_VALUE;
    int zxPolyIndex = 0;
    for (int i = 0; i < PALETTE_ZXPOLY.length; i++) {
      final int zpolyArgb = PALETTE_ZXPOLY[i];
      final double dr = sr - ((zpolyArgb >>> 16) & 0xFF);
      final double dg = sg - ((zpolyArgb >>> 8) & 0xFF);
      final double db = sb - (zpolyArgb & 0xFF);

      final double dist = Math.sqrt(Math.pow(dr, 2) + Math.pow(dg, 2) + Math.pow(db, 2));
      if (Double.compare(dist, minDistance) < 0) {
        zxPolyIndex = i;
        minDistance = dist;
      }
    }
    return zxPolyIndex;
  }

  private static int mixRgb(final int rgb1, final int rgb2) {
    final int r1 = (rgb1 >>> 16) & 0xFF;
    final int g1 = (rgb1 >>> 8) & 0xFF;
    final int b1 = rgb1 & 0xFF;
    final int r2 = (rgb2 >>> 16) & 0xFF;
    final int g2 = (rgb2 >>> 8) & 0xFF;
    final int b2 = rgb2 & 0xFF;

    final int avgR = (r1 + r2) >> 1;
    final int avgG = (g1 + g2) >> 1;
    final int avgB = (b1 + b2) >> 1;

    return 0xFF000000 | (avgR << 16) | (avgG << 8) | avgB;
  }

  private static void fillDataBufferForSpec256VideoMode(
          final ZxPolyModule[] modules,
          final int[] pixelRgbBuffer,
          final boolean flashActive
  ) {
    final int[] preRenderedBack = gfxPrerenderedBack;
    final boolean bkOverFF = gfxBackOverFF;
    final boolean paper00inkFF = gfxPaper00InkFF;
    final boolean hideSameInkPaper = gfxHideSameInkPaper;

    if (preRenderedBack != null) {
      System.arraycopy(preRenderedBack, 0, pixelRgbBuffer, 0, preRenderedBack.length);
    }

    final int downAttrMixedIndex = gfxDownColorsMixed;
    final int upAttrMixedIndex = 0xFF - gfxUpColorsMixed;

    final ZxPolyModule sourceModule = modules[0];
    int offset = 0;
    int aoffset = 0;
    int coordY;
    for (int i = 0; i < 0x1800; i++) {
      if ((i & 0x1F) == 0) {
        // the first byte in the line
        coordY = extractYFromAddress(i);
        aoffset = calcAttributeAddressZxMode(i);
        offset = coordY << 10;
      }

      final int attrOffset = aoffset++;
      long pixelData = sourceModule.readGfxVideo(i);
      int origData = sourceModule.readVideo(i);

      final int attrData = sourceModule.readVideo(attrOffset);
      final int inkColor = extractInkColorSpec256(attrData, flashActive);
      final int paperColor = extractPaperColor(attrData, flashActive);

      int x = 8;
      while (x-- > 0) {
        final int colorIndex = (int) ((pixelData >>> 56) & 0xFF);
        final boolean origPixelSet = (origData & 0x80) != 0;

        int color = PALETTE_SPEC256[colorIndex];
        boolean draw = true;

        final boolean mixWithAttributes =
                colorIndex < downAttrMixedIndex || colorIndex > upAttrMixedIndex;

        if (preRenderedBack == null) {
          // No GFX Background
          if (hideSameInkPaper && inkColor == paperColor) {
            color = inkColor;
          } else if (paper00inkFF) {
            if (colorIndex == 0) {
              color = paperColor;
            } else if (colorIndex == 0xFF) {
              color = inkColor;
            }
          }
        } else {
          // GFX Background is presented
          final boolean backShouldBeShown = ((attrData & 0x80) != 0 && flashActive)
                  || (hideSameInkPaper && inkColor == paperColor);

          if (paper00inkFF) {
            if (colorIndex == 0) {
              color = paperColor;
            } else if (colorIndex == 0xFF) {
              color = inkColor;
            } else {
              draw = !backShouldBeShown;
            }
          } else {
            draw = !(backShouldBeShown || (colorIndex == 0 || (bkOverFF && colorIndex == 0xFF)));
          }
        }

        if (draw && mixWithAttributes) {
          color = mixRgb(origPixelSet ? inkColor : paperColor, color);
        }

        pixelData <<= 8;
        origData <<= 1;

        if (draw) {
          pixelRgbBuffer[offset++] = color;
          pixelRgbBuffer[offset] = color;
          offset += SCREEN_WIDTH;
          pixelRgbBuffer[offset--] = color;
          pixelRgbBuffer[offset] = color;
          offset -= SCREEN_WIDTH - 2;
        } else {
          offset += 2;
        }
      }
    }
  }

  private static void fillDataBufferForSpec256x16VideoMode(
          final ZxPolyModule[] modules,
          final int[] pixelRgbBuffer,
          final boolean flashActive
  ) {
    final int[] preRenderedBack = gfxPrerenderedBack;
    final boolean bkOverFF = gfxBackOverFF;
    final boolean paper00inkFF = gfxPaper00InkFF;
    final boolean hideSameInkPaper = gfxHideSameInkPaper;

    if (preRenderedBack != null) {
      System.arraycopy(preRenderedBack, 0, pixelRgbBuffer, 0, preRenderedBack.length);
    }

    final int downAttrMixedIndex = gfxDownColorsMixed;
    final int upAttrMixedIndex = 0xFF - gfxUpColorsMixed;

    final ZxPolyModule sourceModule = modules[0];
    int offset = 0;
    int aoffset = 0;
    int coordY;
    for (int i = 0; i < 0x1800; i++) {
      if ((i & 0x1F) == 0) {
        // the first byte in the line
        coordY = extractYFromAddress(i);
        aoffset = calcAttributeAddressZxMode(i);
        offset = coordY << 10;
      }

      final int attrOffset = aoffset++;
      long pixelData = sourceModule.readGfxVideo16(i);
      int origData = sourceModule.readVideo(i);

      final int attrData = sourceModule.readVideo(attrOffset);
      final int inkColor = extractInkColorSpec256(attrData, flashActive);
      final int paperColor = extractPaperColor(attrData, flashActive);

      int x = 8;
      while (x-- > 0) {
        final int colorIndex = (int) ((pixelData >>> 35) & 0x1F);
        final boolean origPixelSet = (origData & 0x80) != 0;

        int color = PALETTE_ZXPOLY[colorIndex & 0xF];
        boolean draw = true;

        final boolean mixWithAttributes =
                colorIndex < downAttrMixedIndex || colorIndex > upAttrMixedIndex;

        if (preRenderedBack == null) {
          // No GFX Background
          if (hideSameInkPaper && inkColor == paperColor) {
            color = inkColor;
          } else if (paper00inkFF) {
            if (colorIndex == 0) {
              color = paperColor;
            } else if (colorIndex == 0x1F) {
              color = inkColor;
            }
          }
        } else {
          // GFX Background is presented
          final boolean backShouldBeShown = ((attrData & 0x80) != 0 && flashActive)
                  || (hideSameInkPaper && inkColor == paperColor);

          if (paper00inkFF) {
            if (colorIndex == 0) {
              color = paperColor;
            } else if (colorIndex == 0x1F) {
              color = inkColor;
            } else {
              draw = !backShouldBeShown;
            }
          } else {
            draw = !(backShouldBeShown || (colorIndex == 0 || (bkOverFF && colorIndex == 0x1F)));
          }
        }

        if (draw && mixWithAttributes) {
          color = mixRgb(origPixelSet ? inkColor : paperColor, color);
        }

        pixelData <<= 5;
        origData <<= 1;

        if (draw) {
          pixelRgbBuffer[offset++] = color;
          pixelRgbBuffer[offset] = color;
          offset += SCREEN_WIDTH;
          pixelRgbBuffer[offset--] = color;
          pixelRgbBuffer[offset] = color;
          offset -= SCREEN_WIDTH - 2;
        } else {
          offset += 2;
        }
      }
    }
  }

  private static void fillDataBufferForZxSpectrum128Mode(
          final ZxPolyModule[] modules,
          final int[] pixelRgbBuffer,
          final short[] preRenderedBuffer,
          final boolean flashActive
  ) {
    final ZxPolyModule mainModule = modules[0];
    final byte[] heap = mainModule.getMotherboard().getHeapRam();

    final int videoRamHeapOffset;
    if ((mainModule.read7FFD() & PORTw_ZX128_SCREEN) == 0) {
      // RAM 5
      videoRamHeapOffset = mainModule.getHeapOffset() + 0x14000;
    } else {
      // RAM 7
      videoRamHeapOffset = mainModule.getHeapOffset() + 0x1C000;
    }

    int offset = 0;
    int aoffset = 0;

    for (int i = 0; i < 0x1800; i++) {
      if ((i & 0x1F) == 0) {
        // the first byte in the line
        offset = extractYFromAddress(i) << 10;
        aoffset = calcAttributeAddressZxMode(i);
      }

      final int attrOffset = aoffset++;

      final int preRenderedData = preRenderedBuffer[i] & 0xFFFF;

      int effectiveAttribute = heap[videoRamHeapOffset + attrOffset];
      effectiveAttribute =
              flashActive && ((effectiveAttribute & 0x80) != 0) ? (effectiveAttribute & 0b01_000_000)
                      | ((effectiveAttribute >> 3) & 7)
                      | ((effectiveAttribute & 7) << 3) : effectiveAttribute;

      int currentPixels = heap[videoRamHeapOffset + i] & 0xFF;

      if (preRenderedData == ((currentPixels << 8) | effectiveAttribute)) {
        offset += 16;
      } else {
        preRenderedBuffer[i] = (short) ((currentPixels << 8) | effectiveAttribute);

        final int inkColor = extractInkPaletteColor(effectiveAttribute);
        final int paperColor = extractPaperPaletteColor(effectiveAttribute);

        int x = 8;
        while (x-- > 0) {
          final int color = (currentPixels & 0x80) == 0 ? paperColor : inkColor;
          currentPixels <<= 1;

          pixelRgbBuffer[offset++] = color;
          pixelRgbBuffer[offset] = color;
          offset += SCREEN_WIDTH;
          pixelRgbBuffer[offset--] = color;
          pixelRgbBuffer[offset] = color;
          offset -= SCREEN_WIDTH - 2;
        }
      }
    }
  }

  private static void fillDataBufferForZxPolyVideoMode(
          final int zxPolyVideoMode,
          final ZxPolyModule[] modules,
          final int[] pixelRgbBuffer,
          final short[] preRenderedBuffer,
          final boolean flashActive,
          final boolean forceRender
  ) {
    switch (zxPolyVideoMode) {
      case VIDEOMODE_ZX48_CPU0:
      case VIDEOMODE_ZX48_CPU1:
      case VIDEOMODE_ZX48_CPU2:
      case VIDEOMODE_ZX48_CPU3: {
        final ZxPolyModule sourceModule = modules[zxPolyVideoMode & 0x3];

        int offset = 0;
        int aoffset = 0;

        for (int i = 0; i < 0x1800; i++) {
          if ((i & 0x1F) == 0) {
            // the first byte in the line
            offset = extractYFromAddress(i) << 10;
            aoffset = calcAttributeAddressZxMode(i);
          }

          final int attrOffset = aoffset++;

          final int preRenderedData = preRenderedBuffer[i] & 0xFFFF;
          int effectiveAttribute = sourceModule.readVideo(attrOffset);
          effectiveAttribute = flashActive && ((effectiveAttribute & 0x80) != 0)
                  ? (effectiveAttribute & 0b01_000_000)
                  | ((effectiveAttribute >> 3) & 7)
                  | ((effectiveAttribute & 7) << 3) : effectiveAttribute;

          int videoPixels = sourceModule.readVideo(i);

          if (forceRender
                  || preRenderedData != ((videoPixels << 8) | effectiveAttribute)
          ) {
            preRenderedBuffer[i] = (short) ((videoPixels << 8) | effectiveAttribute);

            final int inkColor = extractInkPaletteColor(effectiveAttribute);
            final int paperColor = extractPaperPaletteColor(effectiveAttribute);

            int x = 8;
            while (x-- > 0) {
              final int color = (videoPixels & 0x80) == 0 ? paperColor : inkColor;
              videoPixels <<= 1;

              pixelRgbBuffer[offset++] = color;
              pixelRgbBuffer[offset] = color;
              offset += SCREEN_WIDTH;
              pixelRgbBuffer[offset--] = color;
              pixelRgbBuffer[offset] = color;
              offset -= SCREEN_WIDTH - 2;
            }
          } else {
            offset += 16;
          }
        }
      }
      break;
      case VIDEOMODE_ZXPOLY_256x192_INKPAPER_MASK:
      case VIDEOMODE_ZXPOLY_256x192_FLASH_MASK:
      case VIDEOMODE_ZXPOLY_256x192: {
        int offset = 0;
        int attributeoffset = 0;

        final ZxPolyModule module0 = modules[0];
        final ZxPolyModule module1 = modules[1];
        final ZxPolyModule module2 = modules[2];
        final ZxPolyModule module3 = modules[3];

        for (int i = 0; i < 0x1800; i++) {
          if ((i & 0x1F) == 0) {
            // the first byte in the line
            offset = extractYFromAddress(i) << 10;
            attributeoffset = calcAttributeAddressZxMode(i);
          }

          int videoValue0 = module0.readVideo(i);
          int videoValue1 = module1.readVideo(i);
          int videoValue2 = module2.readVideo(i);
          int videoValue3 = module3.readVideo(i);

          switch (zxPolyVideoMode) {
            case VIDEOMODE_ZXPOLY_256x192_INKPAPER_MASK: {
              final int attrModule0 = module0.readVideo(attributeoffset++);

              final int inkColor = extractInkColor(attrModule0, flashActive);
              final int paperColor = extractPaperColor(attrModule0, flashActive);

              int x = 8;
              if (inkColor == paperColor) {
                while (x-- > 0) {
                  pixelRgbBuffer[offset++] = inkColor;
                  pixelRgbBuffer[offset] = inkColor;
                  offset += SCREEN_WIDTH;
                  pixelRgbBuffer[offset--] = inkColor;
                  pixelRgbBuffer[offset] = inkColor;
                  offset -= SCREEN_WIDTH - 2;
                }
              } else {
                while (x-- > 0) {
                  final int value = ((videoValue3 & 0x80) == 0 ? 0 : 0x08)
                          | ((videoValue0 & 0x80) == 0 ? 0 : 0x04)
                          | ((videoValue1 & 0x80) == 0 ? 0 : 0x02)
                          | ((videoValue2 & 0x80) == 0 ? 0 : 0x01);

                  videoValue0 <<= 1;
                  videoValue1 <<= 1;
                  videoValue2 <<= 1;
                  videoValue3 <<= 1;

                  final int color = PALETTE_ZXPOLY[value];

                  pixelRgbBuffer[offset++] = color;
                  pixelRgbBuffer[offset] = color;
                  offset += SCREEN_WIDTH;
                  pixelRgbBuffer[offset--] = color;
                  pixelRgbBuffer[offset] = color;
                  offset -= SCREEN_WIDTH - 2;
                }
              }
            }
            break;
            case VIDEOMODE_ZXPOLY_256x192_FLASH_MASK: {
              final int attrModule0 = module0.readVideo(attributeoffset++);

              final int inkColorMod0 = extractInkColor(attrModule0, false);
              final int paperColorMod0 = extractPaperColor(attrModule0, false);

              int x = 8;
              if ((attrModule0 & 0b1000_0000) == 0) {
                while (x-- > 0) {
                  pixelRgbBuffer[offset] =
                          (videoValue0 & 0x80) == 0 ? paperColorMod0 : inkColorMod0;
                  videoValue0 <<= 1;

                  pixelRgbBuffer[offset + SCREEN_WIDTH] =
                          (videoValue2 & 0x80) == 0 ? paperColorMod0 : inkColorMod0;
                  videoValue2 <<= 1;

                  pixelRgbBuffer[++offset] =
                          (videoValue1 & 0x80) == 0 ? paperColorMod0 : inkColorMod0;
                  videoValue1 <<= 1;

                  pixelRgbBuffer[offset++ + SCREEN_WIDTH] =
                          (videoValue3 & 0x80) == 0 ? paperColorMod0 : inkColorMod0;
                  videoValue3 <<= 1;
                }
              } else {
                if (inkColorMod0 == paperColorMod0) {
                  while (x-- > 0) {
                    pixelRgbBuffer[offset++] = inkColorMod0;
                    pixelRgbBuffer[offset] = inkColorMod0;
                    offset += SCREEN_WIDTH;
                    pixelRgbBuffer[offset--] = inkColorMod0;
                    pixelRgbBuffer[offset] = inkColorMod0;
                    offset -= SCREEN_WIDTH - 2;
                  }
                } else {
                  while (x-- > 0) {
                    final int value = ((videoValue3 & 0x80) == 0 ? 0 : 0x08)
                            | ((videoValue0 & 0x80) == 0 ? 0 : 0x04)
                            | ((videoValue1 & 0x80) == 0 ? 0 : 0x02)
                            | ((videoValue2 & 0x80) == 0 ? 0 : 0x01);

                    videoValue0 <<= 1;
                    videoValue1 <<= 1;
                    videoValue2 <<= 1;
                    videoValue3 <<= 1;

                    final int color = PALETTE_ZXPOLY[value];

                    pixelRgbBuffer[offset++] = color;
                    pixelRgbBuffer[offset] = color;
                    offset += SCREEN_WIDTH;
                    pixelRgbBuffer[offset--] = color;
                    pixelRgbBuffer[offset] = color;
                    offset -= SCREEN_WIDTH - 2;
                  }
                }
              }
            }
            break;
            default: {
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

                final int color = PALETTE_ZXPOLY[value];

                pixelRgbBuffer[offset++] = color;
                pixelRgbBuffer[offset] = color;
                offset += SCREEN_WIDTH;
                pixelRgbBuffer[offset--] = color;
                pixelRgbBuffer[offset] = color;
                offset -= SCREEN_WIDTH - 2;
              }
            }
            break;
          }
        }
      }
      break;
      case VIDEOMODE_ZXPOLY_512x384: {
        int offset = 0;
        int attributeoffset = 0;

        final ZxPolyModule module0 = modules[0];
        final ZxPolyModule module1 = modules[1];
        final ZxPolyModule module2 = modules[2];
        final ZxPolyModule module3 = modules[3];

        for (int i = 0; i < 0x1800; i++) {
          if ((i & 0x1F) == 0) {
            // the first byte in the line
            offset = extractYFromAddress(i) << 10;
            attributeoffset = calcAttributeAddressZxMode(i);
          }

          int videoValue0 = module0.readVideo(i);
          final int attribute0 = module0.readVideo(attributeoffset);

          int videoValue1 = module1.readVideo(i);
          final int attribute1 = module1.readVideo(attributeoffset);

          int videoValue2 = module2.readVideo(i);
          final int attribute2 = module2.readVideo(attributeoffset);

          int videoValue3 = module3.readVideo(i);
          final int attribute3 = module3.readVideo(attributeoffset++);

          int x = 8;
          while (x-- > 0) {
            pixelRgbBuffer[offset] =
                    (videoValue0 & 0x80) == 0 ? extractPaperColor(attribute0, flashActive) :
                            extractInkColor(attribute0, flashActive);
            videoValue0 <<= 1;

            pixelRgbBuffer[offset + SCREEN_WIDTH] =
                    (videoValue2 & 0x80) == 0 ? extractPaperColor(attribute2, flashActive) :
                            extractInkColor(attribute2, flashActive);
            videoValue2 <<= 1;

            pixelRgbBuffer[++offset] =
                    (videoValue1 & 0x80) == 0 ? extractPaperColor(attribute1, flashActive) :
                            extractInkColor(attribute1, flashActive);
            videoValue1 <<= 1;

            pixelRgbBuffer[offset++ + SCREEN_WIDTH] =
                    (videoValue3 & 0x80) == 0 ? extractPaperColor(attribute3, flashActive) :
                            extractInkColor(attribute3, flashActive);
            videoValue3 <<= 1;

          }
        }
      }
      break;
      default:
        throw new Error("Unexpected video mode [" + zxPolyVideoMode + ']');
    }
  }

  public static int preciseRgbColorToIndex(final int rgbColor) {
    switch (rgbColor | 0xFF000000) {
      case 0xFF000000:
        return 0;
      case 0xFF0000BE:
        return 1;
      case 0xFFBE0000:
        return 2;
      case 0xFFBE00BE:
        return 3;
      case 0xFF00BE00:
        return 4;
      case 0xFF00BEBE:
        return 5;
      case 0xFFBEBE00:
        return 6;
      case 0xFFBEBEBE:
        return 7;
      case 0xFF0000FF:
        return 9;
      case 0xFFFF0000:
        return 10;
      case 0xFFFF00FF:
        return 11;
      case 0xFF00FF00:
        return 12;
      case 0xFF00FFFF:
        return 13;
      case 0xFFFFFF00:
        return 14;
      case 0xFFFFFFFF:
        return 15;
      default:
        return -1;
    }
  }

  public static int extractYFromAddress(final int address) {
    return ((address & 0x1800) >> 5) | ((address & 0x700) >> 8) | ((address & 0xE0) >> 2);
  }

  public static int calcAttributeAddressZxMode(final int screenOffset) {
    final int line = ((screenOffset >>> 5) & 0x07) | ((screenOffset >>> 8) & 0x18);
    final int column = screenOffset & 0x1F;
    final int off = ((line >>> 3) << 8) | (((line & 0x07) << 5) | column);
    return 0x1800 + off;
  }

  private static int extractInkColor(final int attribute, final boolean flashActive) {
    final int bright = (attribute & 0x40) == 0 ? 0 : 0x08;
    final int inkColor = PALETTE_ZXPOLY[(attribute & 0x07) | bright];
    final int paperColor = PALETTE_ZXPOLY[((attribute >> 3) & 0x07) | bright];
    final boolean flash = (attribute & 0x80) != 0;

    final int result;

    if (flash) {
      if (flashActive) {
        result = paperColor;
      } else {
        result = inkColor;
      }
    } else {
      result = inkColor;
    }
    return result;
  }

  private static int extractPaperPaletteColor(final int attribute) {
    final int bright = (attribute & 0x40) == 0 ? 0 : 0x08;
    return PALETTE_ZXPOLY[((attribute >> 3) & 0x07) | bright];
  }

  private static int extractInkPaletteColor(final int attribute) {
    final int bright = (attribute & 0x40) == 0 ? 0 : 0x08;
    return PALETTE_ZXPOLY[(attribute & 0x07) | bright];
  }

  private static int extractInkColorSpec256(final int attribute, final boolean flashActive) {
    final int bright = (attribute & 0x40) == 0 ? 0 : 0x08;
    final int inkColor = PALETTE_ALIGNED_ZXPOLY[(attribute & 0x07) | bright];
    final int paperColor = PALETTE_ALIGNED_ZXPOLY[((attribute >> 3) & 0x07) | bright];
    final boolean flash = (attribute & 0x80) != 0;

    final int result;

    if (flash) {
      if (flashActive) {
        result = paperColor;
      } else {
        result = inkColor;
      }
    } else {
      result = inkColor;
    }
    return result;
  }

  private static int extractPaperColor(final int attribute, final boolean flashActive) {
    final int bright = (attribute & 0x40) == 0 ? 0 : 0x08;
    final int inkColor = PALETTE_ZXPOLY[(attribute & 0x07) | bright];
    final int paperColor = PALETTE_ZXPOLY[((attribute >> 3) & 0x07) | bright];
    final boolean flash = (attribute & 0x80) != 0;

    final int result;

    if (flash) {
      if (flashActive) {
        result = inkColor;
      } else {
        result = paperColor;
      }
    } else {
      result = paperColor;
    }
    return result;
  }

  public static void setGfxUpColorsMixed(final int value) {
    gfxUpColorsMixed = value;
  }

  public static void setGfxDownColorsMixed(final int value) {
    gfxDownColorsMixed = value;
  }

  public static void setGfxBackOverFF(final boolean flag) {
    gfxBackOverFF = flag;
  }

  public static void setGfxPaper00InkFF(final boolean flag) {
    gfxPaper00InkFF = flag;
  }

  public static void setGfxHideSameInkPaper(final boolean flag) {
    gfxHideSameInkPaper = flag;
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
        return "ZX-Poly 256x192M0";
      case 7:
        return "ZX-Poly 256x192M1";
      case VIDEOMODE_SPEC256:
        return "SPEC256 256x192";
      case VIDEOMODE_SPEC256_16:
        return "SPEC256 256x192 16 colors";
      default:
        return "Unknown [" + code + ']';
    }
  }

  public static int spec256rgbColorToIndex(int rgbPixel) {
    for (int i = 0; i < PALETTE_SPEC256.length; i++) {
      if (rgbPixel == PALETTE_SPEC256[i]) {
        return i;
      }
    }
    return -1;
  }


  public TvFilterChain getTvFilterChain() {
    return tvFilterChain;
  }

  public void setTvFilterChain(final TvFilterChain chain) {
    this.tvFilterChain = chain == null ? TvFilterChain.NONE : chain;
  }

  public boolean isVkbShow() {
    return this.showVkb;
  }

  public void setVkbShow(final boolean show) {
    if (!(show && this.showVkb)) {
      this.showVkb = show;
      this.vkbdRender.doReset();
    }
  }

  public void setEnableTrapMouse(
          final boolean flag,
          final boolean enableMouseTrapIndicator,
          final boolean activateMouseTrap) {
    this.enableMouseTrapIndicator = enableMouseTrapIndicator;
    this.mouseTrapEnabled = flag;
    this.setTrapMouseActive(flag);
  }

  public void setTrapMouseActive(final boolean flag) {
    this.mouseTrapActive = flag;
    this.setHideMouse(flag);
  }

  public boolean isMouseTrapEnabled() {
    return this.mouseTrapEnabled;
  }

  public boolean isMouseTrapActive() {
    return this.mouseTrapActive;
  }

  public void setHideMouse(final boolean doHide) {
    final Runnable runnable = () -> {
      if (doHide) {
        setCursor(Toolkit.getDefaultToolkit()
                .createCustomCursor(new BufferedImage(1, 1, BufferedImage.TRANSLUCENT), new Point(0, 0),
                        "InvisibleCursor"));
      } else {
        setCursor(Cursor.getDefaultCursor());
      }
    };
    Utils.safeSwingCall(runnable);
  }

  public void zoomIn() {
    updateZoom(Math.min(5.0f, this.zoom + 0.2f));
  }

  public void zoomOut() {
    updateZoom(Math.max(1.0f, this.zoom - 0.2f));
  }

  @Override
  public void mouseWheelMoved(final MouseWheelEvent e) {
    if (e.isControlDown()) {
      final float newzoom;
      if (e.getPreciseWheelRotation() > 0) {
        newzoom = Math.max(1.0f, this.zoom - 0.2f);
      } else {
        newzoom = Math.min(5.0f, this.zoom + 0.2f);
      }
      if (newzoom != this.zoom) {
        updateZoom(newzoom);
      }
    }
  }

  @Override
  public Dimension getPreferredSize() {
    return new Dimension(Math.max(this.size.width, MINIMUM_SIZE.width),
            Math.max(this.size.height, MINIMUM_SIZE.height));
  }

  @Override
  public Dimension getMinimumSize() {
    return MINIMUM_SIZE;
  }

  private void updateZoom(final float value) {
    this.zoom = value;
    this.size = new Dimension(Math.round(SCREEN_WIDTH * value), Math.round(SCREEN_HEIGHT * value));
    this.getParent().revalidate();
    this.getParent().repaint();
  }

  private void drawBorder(final Graphics2D g, final int width, final int height) {
    if (this.changedBorderLines != 0L) {
      final float lineHeight = Math.max(1, (float) height / BORDER_LINES) + 1;
      float y = 0.0f;
      final Rectangle2D.Float rectangle = new Rectangle2D.Float(0.0f, y, width, lineHeight);

      for (final byte c : this.borderLineColors) {
        g.setColor(this.tvFilterChain.applyBorderColor(PALETTE_ZXPOLY_COLORS[c]));
        rectangle.y = y;
        g.fill(rectangle);
        y += lineHeight;
      }
    } else {
      g.setColor(this.tvFilterChain.applyBorderColor(PALETTE_ZXPOLY_COLORS[this.portFEw & 7]));
      g.fill(new Rectangle2D.Float(0.0f, 0.0f, width, height));
    }
    this.changedBorderLines = 0L;
    fill(this.borderLineColors, (byte) (this.portFEw & 7));
  }

  @Override
  public void paintComponent(final Graphics g) {
    final Graphics2D g2 = (Graphics2D) g;

    final Rectangle bounds = this.getBounds();

    final int width = bounds.width;
    final int height = bounds.height;

    final int xoff = (width - this.size.width) / 2;
    final int yoff = (height - this.size.height) / 2;

    if (xoff > 0 || yoff > 0) {
      drawBorder(g2, width, height);
    }
    this.drawBuffer(g2, xoff, yoff, this.zoom, this.tvFilterChain);

    if (this.mouseTrapActive && this.enableMouseTrapIndicator) {
      g2.drawImage(MOUSE_TRAPPED, 2, 2, null);
    }

    if (this.showVkb) {
      final int imgWidth = this.vkbdRender.getImageWidth();
      final int imgHeight = this.vkbdRender.getImageHeight();

      final Rectangle renderRectangle;

      if (bounds.width >= imgWidth) {
        if (bounds.width >= imgWidth * 3) {
          final double scale = ((double) bounds.width / 3) / (double) imgWidth;
          final int newWidth = (int) Math.round(scale * imgWidth);
          final int newHeight = (int) Math.round(scale * imgHeight);
          renderRectangle = new Rectangle((bounds.width - newWidth) / 2, bounds.height - newHeight, newWidth, newHeight);
        } else {
          renderRectangle = new Rectangle((bounds.width - imgWidth) / 2, bounds.height - imgHeight, imgWidth, imgHeight);
        }
      } else {
        final double scale = (double) bounds.width / (double) imgWidth;
        final int newWidth = (int) Math.round(scale * imgWidth);
        final int newHeight = (int) Math.round(scale * imgHeight);
        renderRectangle = new Rectangle(0, bounds.height - newHeight, newWidth, newHeight);
      }

      this.vkbdRender.render(this, g2, renderRectangle, true);
    }
  }

  private void refreshBufferData(final int videoMode) {
    switch (videoMode) {
      case VIDEOMODE_ZX48_CPU0: {
        fillDataBufferForZxSpectrum128Mode(
                this.modules,
                this.bufferImageRgbData,
                this.lastRenderedZxData,
                this.board.isFlashActive()
        );
      }
      break;
      case VIDEOMODE_SPEC256: {
        fillDataBufferForSpec256VideoMode(
                this.modules,
                this.bufferImageRgbData,
                this.board.isFlashActive()
        );
      }
      break;
      case VIDEOMODE_SPEC256_16: {
        fillDataBufferForSpec256x16VideoMode(
                this.modules,
                this.bufferImageRgbData,
                this.board.isFlashActive()
        );
      }
      break;
      default: {
        fillDataBufferForZxPolyVideoMode(
                this.currentVideoMode,
                this.modules,
                this.bufferImageRgbData,
                this.lastRenderedZxData,
                this.board.isFlashActive(),
                false
        );
      }
      break;
    }
  }

  public byte[] grabRgb(final byte[] array) {
    lockBuffer();
    byte[] result;
    try {
      final int[] buffer = this.bufferImageRgbData;
      final int bufferLen = buffer.length;
      result = array == null ? new byte[bufferLen * 3] : array;
      int outIndex = 0;
      for (final int argb : buffer) {
        result[outIndex++] = (byte) (argb >> 16);
        result[outIndex++] = (byte) (argb >> 8);
        result[outIndex++] = (byte) argb;
      }
    } finally {
      unlockBuffer();
    }

    if (this.tvFilterChain != null) {
      final int argbBorderColor = PALETTE_ZXPOLY[this.portFEw & 7];
      for (final TvFilter f : this.tvFilterChain.getFilterChain()) {
        result = f.apply(false, result, argbBorderColor);
      }
    }
    return result;
  }

  public void drawBuffer(
          final Graphics2D gfx,
          final int x,
          final int y,
          final float zoom,
          final TvFilterChain filterChain
  ) {
    if (filterChain.isEmpty()) {
      lockBuffer();
      try {
        if (zoom == 1.0f) {
          gfx.drawImage(this.bufferImage, null, x, y);
        } else {
          final float nzoom = Math.max(1.0f, zoom);
          gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
          gfx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

          gfx.drawImage(this.bufferImage, x, y, Math.round(SCREEN_WIDTH * nzoom),
                  Math.round(SCREEN_HEIGHT * nzoom), null);
        }
      } finally {
        unlockBuffer();
      }
    } else {
      final int borderArgbColor = PALETTE_ZXPOLY[this.borderLineColors[BORDER_LINES - 1]];
      final Rectangle area;
      final TvFilter[] tvFilters = filterChain.getFilterChain();
      BufferedImage postprocessedImage;
      lockBuffer();
      try {
        postprocessedImage = tvFilters[0].apply(this.bufferImage, zoom, borderArgbColor, true);
      } finally {
        unlockBuffer();
      }
      for (int i = 1; i < tvFilters.length; i++) {
        postprocessedImage = tvFilters[i].apply(postprocessedImage, zoom, borderArgbColor, false);
      }
      if (zoom == 1.0f) {
        area = new Rectangle(x, y, 512, 384);
        gfx.drawImage(postprocessedImage, null, x, y);
      } else {
        final boolean sizeChangedDuringPostprocessing =
                postprocessedImage.getWidth() != this.bufferImage.getWidth();

        if (sizeChangedDuringPostprocessing) {
          gfx.drawImage(postprocessedImage, null, x, y);
          area = new Rectangle(x, y, 512, 384);
        } else {
          final float normalizedZoom = Math.max(1.0f, zoom);
          gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
          gfx.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_SPEED);

          area = new Rectangle(x, y, Math.round(SCREEN_WIDTH * normalizedZoom),
                  Math.round(SCREEN_HEIGHT * normalizedZoom));

          gfx.drawImage(postprocessedImage, x, y, area.width,
                  area.height, null);
        }
      }

      for (final TvFilter filter : tvFilters) {
        filter.apply(gfx, area, zoom);
      }
    }
  }

  public int getVideoMode() {
    return this.currentVideoMode;
  }

  public void setVideoMode(final int newVideoMode) {
    lockBuffer();
    try {
      Arrays.fill(this.lastRenderedZxData, (byte) 0xFF);
      if (this.currentVideoMode != newVideoMode) {
        if (newVideoMode != VIDEOMODE_SPEC256) {
          if ((newVideoMode & 0b11) == 0 || newVideoMode == 7) {
            this.resetInternalAlreadyRenderedBuffer(this.modules[newVideoMode & 0x3]);
          }
        }
        this.currentVideoMode = newVideoMode;
        log.log(Level.INFO, "mode set: " + decodeVideoModeCode(newVideoMode));
        refreshBufferData(this.currentVideoMode);
      }
    } finally {
      unlockBuffer();
    }
  }

  private void resetInternalAlreadyRenderedBuffer(final ZxPolyModule targetModule) {
    for (int i = 0; i < 0x1800; i++) {
      this.lastRenderedZxData[i] = (byte) (targetModule.readVideo(i) ^ 0xFF);
    }
    final boolean flashActive = this.board.isFlashActive();
    for (int i = 0x1800; i < 0x1B00; i++) {
      final int attr = targetModule.readVideo(i);
      final int inkColor = extractInkColor(attr, flashActive);
      final int paperColor = extractPaperColor(attr, flashActive);
      final int inkPaperColor = (inkColor << 4) | (paperColor);

      this.lastRenderedZxData[i] = (byte) (inkPaperColor ^ 0xFF);
    }
  }

  public void setBorderColor(final int colorIndex) {
    this.portFEw = (this.portFEw & 0xFFFFFFF8) | (colorIndex & 0x07);
    fill(this.borderLineColors, (byte) colorIndex);
  }

  public void lockBuffer() {
    bufferLocker.lock();
  }

  public void unlockBuffer() {
    bufferLocker.unlock();
  }

  public void updateBuffer() {
    lockBuffer();
    try {
      this.refreshBufferData(this.currentVideoMode);
    } finally {
      unlockBuffer();
    }
  }

  public RenderedImage[] renderAllModuleVideoMemoryInZx48Mode() {
    this.lockBuffer();
    try {
      final java.util.List<RenderedImage> result = new ArrayList<>();

      BufferedImage buffImage =
              new BufferedImage(this.bufferImage.getWidth(), this.bufferImage.getHeight(),
                      BufferedImage.TYPE_INT_ARGB);
      Graphics g = buffImage.getGraphics();
      fillDataBufferForZxPolyVideoMode(
              this.currentVideoMode,
              this.modules,
              this.bufferImageRgbData,
              this.lastRenderedZxData,
              this.board.isFlashActive(),
              true
      );
      g.drawImage(this.bufferImage, 0, 0, this);
      g.dispose();
      result.add(buffImage);

      buffImage = new BufferedImage(this.bufferImage.getWidth(), this.bufferImage.getHeight(),
              BufferedImage.TYPE_INT_ARGB);
      g = buffImage.getGraphics();
      fillDataBufferForZxPolyVideoMode(
              VIDEOMODE_ZX48_CPU0,
              this.modules,
              this.bufferImageRgbData,
              this.lastRenderedZxData,
              this.board.isFlashActive(),
              true
      );
      g.drawImage(this.bufferImage, 0, 0, this);
      g.dispose();
      result.add(buffImage);

      buffImage = new BufferedImage(this.bufferImage.getWidth(), this.bufferImage.getHeight(),
              BufferedImage.TYPE_INT_ARGB);
      g = buffImage.getGraphics();
      fillDataBufferForZxPolyVideoMode(
              VIDEOMODE_ZX48_CPU1,
              this.modules,
              this.bufferImageRgbData,
              this.lastRenderedZxData,
              this.board.isFlashActive(),
              true
      );
      g.drawImage(this.bufferImage, 0, 0, this);
      g.dispose();
      result.add(buffImage);

      buffImage = new BufferedImage(this.bufferImage.getWidth(), this.bufferImage.getHeight(),
              BufferedImage.TYPE_INT_ARGB);
      g = buffImage.getGraphics();
      fillDataBufferForZxPolyVideoMode(
              VIDEOMODE_ZX48_CPU2,
              this.modules,
              this.bufferImageRgbData,
              this.lastRenderedZxData,
              this.board.isFlashActive(),
              true
      );
      g.drawImage(this.bufferImage, 0, 0, this);
      g.dispose();
      result.add(buffImage);

      buffImage = new BufferedImage(this.bufferImage.getWidth(), this.bufferImage.getHeight(),
              BufferedImage.TYPE_INT_ARGB);
      g = buffImage.getGraphics();
      fillDataBufferForZxPolyVideoMode(
              VIDEOMODE_ZX48_CPU3,
              this.modules,
              this.bufferImageRgbData,
              this.lastRenderedZxData,
              this.board.isFlashActive(),
              true
      );
      g.drawImage(this.bufferImage, 0, 0, this);
      g.dispose();
      result.add(buffImage);

      return result.toArray(EMPTY_ARRAY);
    } finally {
      refreshBufferData(VIDEOMODE_ZX48_CPU0);
      this.unlockBuffer();
    }
  }

  public int[] makeCopyOfVideoBuffer(final boolean applyFilters) {
    int[] cloneOfBuffer;
    Color borderColor;
    this.lockBuffer();
    try {
      cloneOfBuffer = this.bufferImageRgbData.clone();
      borderColor = PALETTE_ZXPOLY_COLORS[this.portFEw & 7];
    } finally {
      this.unlockBuffer();
    }

    if (applyFilters) {
      byte[] rgb = argb2rgb(cloneOfBuffer);

      for (TvFilter f : this.tvFilterChain.getFilterChain()) {
        rgb = f.apply(false, rgb, borderColor.getRGB());
        borderColor = f.applyBorderColor(borderColor);
      }

      cloneOfBuffer = rgb2argb(rgb);
    }
    return cloneOfBuffer;
  }

  private byte[] argb2rgb(final int[] argb) {
    final byte[] result = new byte[argb.length * 3];
    int j = 0;
    for (int i = 0; i < argb.length; i++) {
      final int value = argb[i];
      result[j++] = (byte) (value >> 16);
      result[j++] = (byte) (value >> 8);
      result[j++] = (byte) value;
    }
    return result;
  }

  private int[] rgb2argb(final byte[] rgb) {
    final int[] result = new int[rgb.length / 3];
    int j = 0;
    for (int i = 0; i < rgb.length; ) {
      final int r = rgb[i++] & 0xFF;
      final int g = rgb[i++] & 0xFF;
      final int b = rgb[i++] & 0xFF;
      result[j++] = 0xFF000000 | (r << 16) | (g << 8) | b;
    }
    return result;
  }

  public RenderedImage makeCopyOfCurrentPicture() {
    final BufferedImage result =
            new BufferedImage(this.bufferImage.getWidth(), this.bufferImage.getHeight(),
                    BufferedImage.TYPE_INT_RGB);
    final Graphics2D gfx = result.createGraphics();
    try {
      drawBuffer(gfx, 0, 0, 1.0f, this.tvFilterChain);
    } finally {
      gfx.dispose();
    }
    return result;
  }

  @Override
  public Motherboard getMotherboard() {
    return this.board;
  }

  @Override
  public int readIo(final ZxPolyModule module, final int port) {
    return -1;
  }

  public int getPortFE() {
    return this.portFEw;
  }

  @Override
  public void writeIo(final ZxPolyModule module, final int port, final int value) {
    if (!module.isTrdosActive()) {
      final boolean zxPolyMode = module.getMotherboard().getBoardMode() == BoardMode.ZXPOLY;
      if ((zxPolyMode && (port & 0xFF) == 0xFE) || (!zxPolyMode && (port & 1) == 0)) {
        this.portFEw = value & 0xFF;

        int borderLineIndex = this.tstatesCounter / TSTATES_PER_LINE;
        if (borderLineIndex >= 0 && borderLineIndex < BORDER_LINES) {
          this.borderLineColors[borderLineIndex] = (byte) (this.portFEw & 0x7);
          this.changedBorderLines |= 1L << borderLineIndex;
        }
      }
    }
  }

  @Override
  public int getNotificationFlags() {
    return NOTIFICATION_PRESTEP | NOTIFICATION_POSTSTEP;
  }

  @Override
  public void preStep(final boolean signalReset, final boolean tstatesIntReached,
                      boolean wallclockInt) {
    if (signalReset) {
      this.portFEw = 0x00;
    }
    if (wallclockInt) {
      this.tstatesCounter = 0;
    }
  }

  @Override
  public void doReset() {
    this.vkbdRender.doReset();
    Arrays.fill(this.lastRenderedZxData, (byte) 0xFF);
  }

  @Override
  public void postStep(int spentTstates) {
    this.tstatesCounter += spentTstates;
  }

  public float getZoom() {
    return this.zoom;
  }

  public int getScrYForZxScr(final int zxY) {
    final int height = getHeight();
    final int yoff = (height - this.size.height) / 2;
    return (zxY * Math.round(this.zoom * 2)) + yoff;
  }

  public int getZxScrY(final int compoY) {
    final int height = getHeight();
    final int yoff = (height - this.size.height) / 2;

    final int result = (compoY - yoff) / Math.round(this.zoom * 2);
    return Math.max(0x00, Math.min(191, result));
  }

  public int getZxScrX(final int compoX) {
    final int width = getWidth();
    final int xoff = (width - this.size.width) / 2;

    final int result = (compoX - xoff) / Math.round(this.zoom * 2);
    return Math.max(0x00, Math.min(0xFF, result));
  }

  public int getScrXForZxScr(final int zxX) {
    final int width = getWidth();
    final int xoff = (width - this.size.width) / 2;
    return (zxX * Math.round(this.zoom * 2)) + xoff;
  }

  @Override
  public String toString() {
    return this.getName();
  }

  public void doAutoscaleForSize() {
    final Rectangle rectangle = this.getBounds();
    final int width = rectangle.width;
    final int height = rectangle.height;

    final int maxZoomW = width / SCREEN_WIDTH;
    final int maxZoomH = height / SCREEN_HEIGHT;

    updateZoom(Math.max(1.0f, Math.min(maxZoomH, maxZoomW)));
  }

  public long getVkbState() {
    return this.vkbdRender.getKeyState();
  }
}
