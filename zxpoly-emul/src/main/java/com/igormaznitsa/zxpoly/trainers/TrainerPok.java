package com.igormaznitsa.zxpoly.trainers;

import com.igormaznitsa.zxpoly.components.Motherboard;
import com.igormaznitsa.zxpoly.components.ZxPolyModule;
import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

public class TrainerPok extends AbstractTrainer {
  public static final Logger LOGGER = Logger.getLogger("Trainer.POK");

  public TrainerPok() {
    super("POK file (*.pok)", "pok");
  }

  @Override
  public void apply(
          final Component component,
          final File file,
          final Motherboard motherboard
  ) {
    final String body;
    try {
      body = FileUtils.readFileToString(file, StandardCharsets.US_ASCII);
    } catch (IOException ex) {
      throw new RuntimeException("Can't load file", ex);
    }

    final List<PokeRecord> records = new ArrayList<>();

    PokeRecord currentRecord = null;


    for (String s : body.split("\\r?\\n")) {
      final String line = s.trim();
      if (line.startsWith("Y")) {
        break;
      } else if (line.startsWith("N")) {
        if (currentRecord != null) {
          records.add(currentRecord);
        }
        final String text = line.substring(1);
        currentRecord = new PokeRecord(text);
      } else if (line.startsWith("M") || line.startsWith("Z")) {
        final String pokeLine = line.substring(1).trim();
        if (currentRecord == null) {
          throw new IllegalArgumentException("Unexpected poke without title: " + line);
        }
        currentRecord.addPoke(pokeLine);
      } else {
        if (!line.isEmpty()) {
          throw new IllegalArgumentException("Can't recognize line: " + line);
        }
      }
    }
    if (currentRecord != null) {
      records.add(currentRecord);
    }

    if (records.isEmpty()) {
      throw new IllegalArgumentException("Can't find records int POKE file");
    }

    final JList<PokeRecord> listRecordsComponent = new JList<>(records.toArray(new PokeRecord[0]));
    listRecordsComponent.setVisibleRowCount(8);
    listRecordsComponent.addMouseListener(new MouseAdapter() {
      @Override
      @SuppressWarnings("unchecked")
      public void mouseClicked(MouseEvent e) {
        JList<PokeRecord> list = (JList<PokeRecord>) e.getSource();
        int index = list.locationToIndex(e.getPoint());
        if (index >= 0) {
          PokeRecord record = list.getModel().getElementAt(index);
          record.setSelected(!record.isSelected());
          list.repaint();
        }
      }
    });
    listRecordsComponent.setCellRenderer(new CheckBoxListCellRenderer());
    listRecordsComponent.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

    if (JOptionPane
            .showConfirmDialog(component, new JScrollPane(listRecordsComponent), "Found trainers",
                    JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION) {
      records.stream().filter(PokeRecord::isSelected).flatMap(x -> x.items.stream())
              .forEach(poke -> {
                int data = poke.value;
                if (data == 256) {
                  final JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
                  panel.add(new JLabel(String.format("Byte value (%s):", poke.parent.title)));
                  final JSpinner spinner = new JSpinner(new SpinnerNumberModel(0, 0, 255, 1));
                  panel.add(spinner);
                  if (JOptionPane
                          .showConfirmDialog(component, panel,
                                  String.format("%s, value for addr %d", poke.parent.title, poke.address),
                                  JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE)
                          == JOptionPane.CANCEL_OPTION) {
                    return;
                  }
                  data = (Integer) spinner.getValue();
                }
                for (final ZxPolyModule module : motherboard.getModules()) {
                  if ((poke.bank & 8) == 0 && poke.address >= 0xC000) {
                    if (module.getModuleIndex() == 0) {
                      LOGGER
                              .info(String.format("POKE %d:%d,%d", poke.bank, poke.address - 0xC000, data));
                    }
                    module.poke(poke.bank, poke.address - 0xC000, data);
                  } else {
                    if (module.getModuleIndex() == 0) {
                      LOGGER.info(String.format("POKE %d,%d", poke.address, data));
                    }
                    module.writeMemory(module.getCpu(), 0, poke.address, (byte) data);
                  }
                }
              });
    }
  }

  private final class CheckBoxListCellRenderer extends JCheckBox
          implements ListCellRenderer<PokeRecord> {

    public CheckBoxListCellRenderer() {
      super();
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends PokeRecord> list,
            PokeRecord value,
            int index,
            boolean isSelected,
            boolean cellHasFocus
    ) {
      this.setComponentOrientation(list.getComponentOrientation());
      this.setEnabled(list.isEnabled());
      this.setSelected(value.isSelected());
      this.setFont(list.getFont());
      this.setBackground(hasFocus() ? list.getSelectionBackground() : list.getBackground());
      this.setForeground(hasFocus() ? list.getSelectionForeground() : list.getForeground());
      this.setText(value.toString());

      this.setBorder(hasFocus() ? UIManager.getBorder("List.focusSelectedCellHighlightBorder") :
              UIManager.getBorder("List.cellNoFocusBorder"));

      return this;
    }

  }

  private class PokeRecord {
    private final String title;
    private final List<PokeItem> items = new ArrayList<>();
    private boolean selected;

    PokeRecord(final String title) {
      this.title = title;
    }

    boolean isSelected() {
      return this.selected;
    }

    void setSelected(final boolean value) {
      this.selected = value;
    }

    void addPoke(final String poke) {
      final String[] splitted = poke.split("\\s");
      if (splitted.length != 4) {
        throw new IllegalArgumentException("Illegal poke string: " + poke);
      }
      final int bank;
      final int address;
      final int value;
      final int original;

      try {
        bank = Integer.parseInt(splitted[0]);
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("Can't parse bank value: " + poke);
      }

      try {
        address = Integer.parseInt(splitted[1]);
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("Can't parse address: " + poke);
      }

      try {
        value = Integer.parseInt(splitted[2]);
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("Can't parse value: " + poke);
      }

      try {
        original = Integer.parseInt(splitted[3]);
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("Can't parse original value: " + poke);
      }

      this.items.add(new PokeRecord.PokeItem(this, bank, address, value, original));
    }

    @Override
    public String toString() {
      return this.title;
    }

    private class PokeItem {
      private final PokeRecord parent;
      private final int bank;
      private final int address;
      private final int value;
      private final int original;

      PokeItem(PokeRecord parent, final int bank, final int address, final int value,
               final int orig) {
        this.parent = parent;
        this.bank = bank;
        this.address = address;
        this.value = value;
        this.original = orig;
      }
    }
  }
}
