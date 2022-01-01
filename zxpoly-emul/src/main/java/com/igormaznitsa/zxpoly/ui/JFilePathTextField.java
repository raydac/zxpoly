package com.igormaznitsa.zxpoly.ui;

import com.igormaznitsa.zxpoly.utils.AppOptions;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import java.awt.*;
import java.io.File;
import java.util.Locale;

public class JFilePathTextField extends JPanel {
  private static final FileFilter FILE_FILTER_ROM = new FileFilter() {
    @Override
    public boolean accept(final File f) {
      return f.isDirectory() || f.getName().toLowerCase(Locale.ENGLISH).endsWith(".rom");
    }

    @Override
    public String getDescription() {
      return "ROM file (*.rom)";
    }
  };
  private final JTextField textField;

  public JFilePathTextField() {
    super(new BorderLayout(0, 0));
    this.setBorder(null);
    this.textField = new JTextField();
    this.add(this.textField, BorderLayout.CENTER);

    final JButton pathSelector = new JButton(" ... ");
    pathSelector.setFocusable(false);
    pathSelector.addActionListener(e -> this.selectPath());

    this.add(pathSelector, BorderLayout.EAST);
  }

  @Override
  public void setToolTipText(final String text) {
    this.textField.setToolTipText(text);
  }

  @Override
  public Dimension getPreferredSize() {
    return this.textField.getPreferredSize();
  }

  public void setColumns(final int columns) {
    this.textField.setColumns(columns);
  }

  public String getText() {
    return this.textField.getText();
  }

  public void setText(final String text) {
    this.textField.setText(text == null ? "" : text);
  }

  private void selectPath() {
    String path = this.textField.getText();
    File folder;
    if (path.isBlank()) {
      folder = AppOptions.getInstance().getRomCacheFolder();
    } else {
      try {
        final File file = new File(path);
        folder = file.isFile() ? file.getParentFile() : AppOptions.getInstance().getRomCacheFolder();
      } catch (Exception ex) {
        folder = AppOptions.getInstance().getRomCacheFolder();
      }
    }

    final JFileChooser fileChooser = new JFileChooser(folder);
    fileChooser.setDialogTitle("Select custom bootstrap ROM file");
    fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
    fileChooser.setMultiSelectionEnabled(false);
    fileChooser.addChoosableFileFilter(FILE_FILTER_ROM);
    fileChooser.setFileFilter(FILE_FILTER_ROM);

    if (fileChooser.showOpenDialog(SwingUtilities.getWindowAncestor(this)) == JFileChooser.APPROVE_OPTION) {
      final File file = fileChooser.getSelectedFile();
      if (file.isFile()) {
        this.textField.setText(file.getAbsolutePath());
      }
    }
  }
}
