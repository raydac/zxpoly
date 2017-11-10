/*
 * Copyright (C) 2017 Raydac Research Group Ltd.
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

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.commons.io.IOUtils;
import com.igormaznitsa.zxpoly.MainForm;
import com.madgag.gif.fmsware.AnimatedGifEncoder;

public class AnimGifEncoder implements AnimationEncoder {

  private final AtomicBoolean disposed = new AtomicBoolean();
  private final AnimatedGifEncoder baseEncoder;
  private final OutputStream theOutputStream;

  private final ReentrantLock locker = new ReentrantLock();
  private final int intSignalsBetweenFrames;
  
  public AnimGifEncoder(final File file, final int intSignalsBetweenFrames, final boolean repeat) throws IOException {
    this.intSignalsBetweenFrames = intSignalsBetweenFrames;
    this.baseEncoder = new AnimatedGifEncoder();
    this.baseEncoder.setFrameRate(1000.0f / (MainForm.TIMER_INT_DELAY_MILLISECONDS * intSignalsBetweenFrames));
    this.baseEncoder.setQuality(8);
    this.baseEncoder.setRepeat(repeat ? 0 : -1);
    this.theOutputStream = new BufferedOutputStream(new FileOutputStream(file));
    this.baseEncoder.start(theOutputStream);
  }

  @Override
  public int getIntsBetweenFrames() {
    return this.intSignalsBetweenFrames;
  }

  @Override
  public void dispose() {
      if (this.disposed.compareAndSet(false, true)) {
        this.locker.lock();
        try {
        try {
          this.baseEncoder.finish();
        }
        finally {
          IOUtils.closeQuietly(this.theOutputStream);
        }
      }finally{
        this.locker.unlock();
      }
    }
  }

  @Override
  public void addFrame(final RenderedImage currentScreen) {
    if (!(currentScreen instanceof BufferedImage)) throw new IllegalArgumentException("Image must be BufferedImage");
    if (!this.disposed.get()) {
      this.locker.lock();
      try{
        this.baseEncoder.addFrame((BufferedImage) currentScreen);
      }finally{
        this.locker.unlock();
      }
    }
  }
}
