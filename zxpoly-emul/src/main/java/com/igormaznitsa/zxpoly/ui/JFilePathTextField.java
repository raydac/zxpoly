package com.igormaznitsa.zxpoly.ui;

import com.igormaznitsa.zxpoly.utils.AppOptions;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.util.Locale;
import java.util.function.Consumer;
import javax.swing.BorderFactory;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.border.BevelBorder;
import javax.swing.filechooser.FileFilter;

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
    super(new GridBagLayout());
    this.setBorder(null);
    this.textField = new JTextField();

    final GridBagConstraints gbc =
        new GridBagConstraints(0, 0, 1, 1, 1000, 1, GridBagConstraints.WEST,
            GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);

    this.add(this.textField, gbc);

    final LabelButton buttonSelect = new LabelButton(" … ", "Select file", e -> selectPath());
    final LabelButton buttonClear =
        new LabelButton(" X ", "Clear selection", e -> this.textField.setText(""));

    gbc.gridx = 1;
    gbc.weightx = 1;

    final JPanel panel = new JPanel(new GridLayout(1, 2, 4, 0));
    panel.add(buttonClear);
    panel.add(buttonSelect);

    this.add(panel, gbc);
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
        folder =
            file.isFile() ? file.getParentFile() : AppOptions.getInstance().getRomCacheFolder();
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

    if (fileChooser.showOpenDialog(SwingUtilities.getWindowAncestor(this)) ==
        JFileChooser.APPROVE_OPTION) {
      final File file = fileChooser.getSelectedFile();
      if (file.isFile()) {
        this.textField.setText(file.getAbsolutePath());
      }
    }
  }

  private static final class LabelButton extends JLabel {
    LabelButton(final String text, final String tooltip, final Consumer<LabelButton> listener) {
      super(text);
      this.setToolTipText(tooltip);
      this.setHorizontalAlignment(JLabel.CENTER);
      this.setVerticalAlignment(JLabel.CENTER);
      this.setOpaque(true);
      this.setBackground(UIManager.getColor("Button.background"));
      this.setForeground(UIManager.getColor("Button.foreground"));
      this.setFocusable(false);
      this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));

      this.setFont(this.getFont().deriveFont(Font.BOLD));

      this.addMouseListener(new MouseAdapter() {
        @Override
        public void mousePressed(MouseEvent e) {
          LabelButton.this.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
        }

        @Override
        public void mouseReleased(MouseEvent e) {
          LabelButton.this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        }

        @Override
        public void mouseExited(MouseEvent e) {
          LabelButton.this.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
          listener.accept(LabelButton.this);
        }
      });
    }
  }
}
