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
package com.igormaznitsa.zxpspritecorrector.cmdline;

import com.igormaznitsa.zxpspritecorrector.components.VideoMode;
import com.igormaznitsa.zxpspritecorrector.utils.ZXPalette;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

public class SliceImageCmd {

  public SliceImageCmd() {

  }

  public int process(final String[] args) {
    if (args.length > 0) {
      final String file = args[0];
      try {
        final File imgFile = new File(file);
        final BufferedImage img = loadARGBImage(imgFile);

        if (img.getWidth() == 256 && img.getHeight() == 192) {
          processScreen256x192(imgFile, img);
        } else if (img.getWidth() == 512 && img.getHeight() == 384) {
          processScreen512x384(imgFile, img);
        } else {
          System.err.print("Incompatible image resolution = " + img.getWidth() + 'x' + img.getHeight());
          return 1;
        }

      } catch (Exception ex) {
        ex.printStackTrace();
        return 1;
      }
    } else {
      System.err.println("Can't find file name to be sliced");
      return 1;
    }
    return 0;
  }

  private BufferedImage loadARGBImage(final File file) throws IOException {
    final BufferedImage img = ImageIO.read(file);
    final BufferedImage result = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
    final Graphics2D gfx = result.createGraphics();
    gfx.drawImage(img, 0, 0, null);
    gfx.dispose();
    return result;
  }

  public void processScreen512x384(final File srcFile, final BufferedImage image) throws IOException {
    System.out.println("Slicing image for 512x384");

    final int pixelsNumber = 256 * 192;

    int[] imgBufCpu0 = new int[pixelsNumber];
    int[] imgBufCpu1 = new int[pixelsNumber];
    int[] imgBufCpu2 = new int[pixelsNumber];
    int[] imgBufCpu3 = new int[pixelsNumber];

    byte[] dataCpu0 = new byte[32 * 192];
    byte[] dataCpu1 = new byte[32 * 192];
    byte[] dataCpu2 = new byte[32 * 192];
    byte[] dataCpu3 = new byte[32 * 192];

    final int[] srcImageBuffer = ((DataBufferInt) image.getRaster().getDataBuffer()).getData();

    byte[] attributesCpu0 = new byte[768];
    byte[] attributesCpu1 = new byte[768];
    byte[] attributesCpu2 = new byte[768];
    byte[] attributesCpu3 = new byte[768];

    int dstIndex = 0;
    for (int y = 0; y < 384; y += 2) {
      int yOffset = y * 512;

      for (int x = 0; x < 512; x += 2) {
        // CPU0
        imgBufCpu0[dstIndex] = srcImageBuffer[x + yOffset];

        // CPU1
        imgBufCpu1[dstIndex] = srcImageBuffer[x + yOffset + 1];

        // CPU2
        imgBufCpu2[dstIndex] = srcImageBuffer[x + yOffset + 512];

        // CPU3
        imgBufCpu3[dstIndex] = srcImageBuffer[x + yOffset + 512 + 1];

        dstIndex++;
      }
    }

    for (int li = 0; li < 768; li++) {
      attributesCpu0[li] = analyzeBlockForAttrib(li, imgBufCpu0, dataCpu0);
      attributesCpu1[li] = analyzeBlockForAttrib(li, imgBufCpu1, dataCpu1);
      attributesCpu2[li] = analyzeBlockForAttrib(li, imgBufCpu2, dataCpu2);
      attributesCpu3[li] = analyzeBlockForAttrib(li, imgBufCpu3, dataCpu3);
    }

    final File outFolder = srcFile.getParentFile();

    FileUtils.writeByteArrayToFile(new File(outFolder, FilenameUtils.getBaseName(srcFile.getName()) + "c0.c0"), packZxScreen(dataCpu0, attributesCpu0));
    FileUtils.writeByteArrayToFile(new File(outFolder, FilenameUtils.getBaseName(srcFile.getName()) + "c1.c1"), packZxScreen(dataCpu1, attributesCpu1));
    FileUtils.writeByteArrayToFile(new File(outFolder, FilenameUtils.getBaseName(srcFile.getName()) + "c2.c2"), packZxScreen(dataCpu2, attributesCpu2));
    FileUtils.writeByteArrayToFile(new File(outFolder, FilenameUtils.getBaseName(srcFile.getName()) + "c3.c3"), packZxScreen(dataCpu3, attributesCpu3));
  }

  public byte analyzeBlockForAttrib(final int attributeIndex, final int[] nonInterlacedArgbArray, final byte[] screenArray) {
    final int[] colorUse = new int[16];

    final int attrx = attributeIndex % 32;
    final int attry = attributeIndex / 32;

    final int xoffst = attrx * 8;
    final int yoffst = attry * 8;

    for (int y = 0; y < 8; y++) {
      final int offsty = (yoffst + y) * 256 + xoffst;

      for (int x = 0; x < 8; x++) {
        final int pixelArgb = nonInterlacedArgbArray[offsty + x];
        final int colorIndex;
        if (((pixelArgb >>> 24) & 0xFF) < 0x7F) {
          colorIndex = 0;
        } else {
          colorIndex = ZXPalette.findNearestColorIndex(pixelArgb);
        }

        nonInterlacedArgbArray[offsty + x] = colorIndex;

        colorUse[colorIndex]++;
      }
    }

    int foregroundIndex = 0;
    int max = 0;
    int backgroundIndex = 0;

    // find foreground
    for (int i = 0; i < 16; i++) {
      if (max < colorUse[i]) {
        foregroundIndex = i;
        max = colorUse[i];
      }
    }

    colorUse[foregroundIndex] = 0;
    max = 0;

    // find background
    for (int i = 0; i < 16; i++) {
      if (max < colorUse[i]) {
        backgroundIndex = i;
        max = colorUse[i];
      }
    }

    int outScrArrayIndex = attrx + ((attry * 8) * 32);

    // pack block 8x8
    for (int y = 0; y < 8; y++) {
      final int pointOffset = (yoffst + y) * 256 + xoffst;

      int bit = 1;
      int accum = 0;

      for (int x = 0; x < 8; x++) {
        int index = nonInterlacedArgbArray[pointOffset + x];
        if (index == foregroundIndex) {
          // foreground
          accum |= bit;
        }

        bit <<= 1;
      }

      screenArray[outScrArrayIndex] = invertBitOrder((byte) accum);
      outScrArrayIndex += 32;
    }

    // pack attribute
    final boolean hicolorFgr = foregroundIndex > 7;
    final boolean hicolorBgr = backgroundIndex > 7;

    final boolean bright = hicolorBgr || hicolorFgr;

    int result = (bright ? 64 : 0) | ((backgroundIndex & 7) << 3) | (foregroundIndex & 7);

    return (byte) result;
  }

  public void processScreen256x192(final File srcFile, final BufferedImage srcArgbImage) throws IOException {
    System.out.println("Slicing image for 256x192x16");

    final int totalPixels = 256 * 192;

    final byte[] planR = new byte[totalPixels];
    final byte[] planG = new byte[totalPixels];
    final byte[] planB = new byte[totalPixels];
    final byte[] planY = new byte[totalPixels];

    final int[] srcImageBuffer = ((DataBufferInt) srcArgbImage.getRaster().getDataBuffer()).getData();

    for (int pixelIndex = 0; pixelIndex < totalPixels; pixelIndex++) {
      final int argb = srcImageBuffer[pixelIndex];

      final int alpha = argb >>> 24;

      if (alpha < 0x4F) {
        planR[pixelIndex] = 0;
        planG[pixelIndex] = 0;
        planB[pixelIndex] = 0;
        planY[pixelIndex] = 0;
      } else {
        final int colorIndex = ZXPalette.findNearestColorIndex(argb);

        planR[pixelIndex] = (byte) (colorIndex & 2);
        planG[pixelIndex] = (byte) (colorIndex & 4);
        planB[pixelIndex] = (byte) (colorIndex & 1);
        planY[pixelIndex] = (byte) (colorIndex & 8);
      }
    }

    final File outFolder = srcFile.getParentFile();

    FileUtils.writeByteArrayToFile(new File(outFolder, FilenameUtils.getBaseName(srcFile.getName()) + "c1.c1"), packZxRester(planR));
    FileUtils.writeByteArrayToFile(new File(outFolder, FilenameUtils.getBaseName(srcFile.getName()) + "c0.c0"), packZxRester(planG));
    FileUtils.writeByteArrayToFile(new File(outFolder, FilenameUtils.getBaseName(srcFile.getName()) + "c2.c2"), packZxRester(planB));
    FileUtils.writeByteArrayToFile(new File(outFolder, FilenameUtils.getBaseName(srcFile.getName()) + "c3.c3"), packZxRester(planY));
  }

  private byte[] packZxScreen(final byte[] raster, final byte[] attributeArea) {
    final int height = 192;

    final byte[] result = new byte[32 * height + 768];
    int dataY = 0;

    for (int y = 0; y < 192; y++) {
      int offsety = VideoMode.y2zxy(y) * 32;
      for (int x = 0; x < 32; x++) {
        result[offsety + x] = raster[dataY + x];
      }
      dataY += 32;
    }

    int addr = 6144;
    for (int i = 0; i < 768; i++) {
      result[addr++] = attributeArea[i];
    }

    return result;
  }

  private byte[] packZxRester(final byte[] pixelData) {
    final int imageWidth = 256;
    final int imageHeight = 192;
    final int bytesPerLine = 32;
    final byte[] result = new byte[bytesPerLine * imageHeight];
    int posInSrc = 0;

    int initBit = 1;

    for (int y = 0; y < imageHeight; y++) {
      int acc = 0;
      int bit = initBit;
      int posInDst = VideoMode.y2zxy(y) * bytesPerLine;

      for (int x = 0; x < imageWidth; x++) {
        acc |= (pixelData[posInSrc++] != 0 ? bit : 0);
        bit <<= 1;
        if (bit > 0xFF) {
          result[posInDst++] = invertBitOrder((byte) acc);
          bit = initBit;
          acc = 0;
        }
      }
      if (bit != initBit) {
        result[posInDst++] = invertBitOrder((byte) acc);
      }
    }

    return result;
  }

  private byte invertBitOrder(final byte in) {
    int result = 0;
    int bit = 1;
    int bit2 = 0x80;

    while (bit2 != 0) {
      if ((in & bit) != 0) {
        result |= bit2;
      }
      bit2 >>>= 1;
      bit <<= 1;
    }

    return (byte) result;
  }
}
