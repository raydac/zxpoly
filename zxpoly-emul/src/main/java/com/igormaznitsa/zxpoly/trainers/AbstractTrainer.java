package com.igormaznitsa.zxpoly.trainers;

import com.igormaznitsa.zxpoly.components.Motherboard;
import java.awt.Component;
import java.io.File;
import java.util.Locale;
import javax.swing.filechooser.FileFilter;

public abstract class AbstractTrainer extends FileFilter {

  private final String description;
  private final String dotExtension;

  public AbstractTrainer(final String description, final String extension) {
    this.description = description;
    this.dotExtension = '.' + extension.toLowerCase(Locale.ENGLISH);
  }

  public abstract void apply(Component parent, File file, Motherboard motherboard);

  @Override
  public boolean accept(final File f) {
    return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(this.dotExtension);
  }

  @Override
  public String getDescription() {
    return this.description;
  }
}
