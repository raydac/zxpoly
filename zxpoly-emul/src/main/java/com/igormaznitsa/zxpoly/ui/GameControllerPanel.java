package com.igormaznitsa.zxpoly.ui;

import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.java.games.input.Controller;

public class GameControllerPanel extends JPanel {

  private final KeyboardKempstonAndTapeIn module;

  public GameControllerPanel(final KeyboardKempstonAndTapeIn module) {
    super(new GridBagLayout());

    this.module = module;

    final List<Controller> controllers = module.getControllers();
    final List<KeyboardKempstonAndTapeIn.ControllerProcessor> processors = module.getActiveControllerProcessors();

    final GridBagConstraints constraints = new GridBagConstraints(0, GridBagConstraints.RELATIVE, 1, 1, 1000, 1000, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0);

    for (final Controller c : controllers) {
      final Record newRecord = new Record(c, processors);
      this.add(newRecord, constraints);
    }
  }

  public List<KeyboardKempstonAndTapeIn.ControllerProcessor> getSelected() {
    final List<KeyboardKempstonAndTapeIn.ControllerProcessor> result = new ArrayList<>();
    for (final java.awt.Component c : this.getComponents()) {
      if (c instanceof Record) {
        final Record record = (Record) c;
        final KeyboardKempstonAndTapeIn.ControllerDestination destination = (KeyboardKempstonAndTapeIn.ControllerDestination) record.destination.getSelectedItem();
        if (record.selected.isSelected() && destination != KeyboardKempstonAndTapeIn.ControllerDestination.NONE) {
          result.add(module.makeControllerProcessor(record.controller, destination));
        }
      }
    }
    return result;
  }

  private static class Record extends JPanel {
    private final JCheckBox selected;
    private final JLabel name;
    private final JComboBox<KeyboardKempstonAndTapeIn.ControllerDestination> destination;
    private final Controller controller;

    Record(final Controller controller, final List<KeyboardKempstonAndTapeIn.ControllerProcessor> processors) {
      super(new GridBagLayout());
      this.controller = controller;
      this.selected = new JCheckBox();
      this.name = new JLabel(controller.getName());
      this.destination = new JComboBox<>(KeyboardKempstonAndTapeIn.ControllerDestination.values());

      final GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
      this.add(this.selected, constraints);
      constraints.gridx = 1;
      this.add(this.name, constraints);
      constraints.gridx = 2;
      this.add(this.destination, constraints);
    }
  }
}
