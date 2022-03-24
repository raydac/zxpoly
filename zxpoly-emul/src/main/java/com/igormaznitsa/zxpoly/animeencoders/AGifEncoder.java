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

package com.igormaznitsa.zxpoly.animeencoders;

import com.igormaznitsa.zxpoly.MainForm;
import com.igormaznitsa.zxpoly.components.video.VideoController;
import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Duration;


public final class AGifEncoder implements AnimationEncoder {

  private final int intsBetweenFrames;
  private final AdaptedAnimatedGifEncoder gifEncoder;

  private final OutputStream outputStream;

  public AGifEncoder(final File targetFile, final int[] globalRgbPalette, final int frameRate, final boolean makeLoop) throws IOException {
    this.gifEncoder = new AdaptedAnimatedGifEncoder(VideoController.SCREEN_WIDTH, VideoController.SCREEN_HEIGHT, globalRgbPalette);
    this.intsBetweenFrames =
            (int) (1000L / MainForm.TIMER_INT_DELAY_MILLISECONDS.toMillis()) / frameRate;
    this.gifEncoder.setDelay(Duration.ofMillis(this.intsBetweenFrames * MainForm.TIMER_INT_DELAY_MILLISECONDS.toMillis()));
    this.outputStream = new FileOutputStream(targetFile);
    gifEncoder.setRepeat(makeLoop ? 0 : 1);
    gifEncoder.start(this.outputStream);
  }

  @Override
  public int getIntsBetweenFrames() {
    return this.intsBetweenFrames;
  }

  @Override
  public void saveFrame(final int[] rgbPixels) throws IOException {
    this.gifEncoder.addFrame(rgbPixels);
  }

  @Override
  public void close() throws IOException {
    try {
      this.gifEncoder.cleanUp();
    } finally {
      IOUtils.closeQuietly(this.outputStream);
    }
  }
}
