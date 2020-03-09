package com.igormaznitsa.zxpoly.ui;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;


import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import com.igormaznitsa.zxpoly.components.gadapter.Gadapter;
import com.igormaznitsa.zxpoly.components.gadapter.GadapterType;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import net.java.games.input.Controller;

public final class GameControllerPanel extends JPanel {

  private final KeyboardKempstonAndTapeIn module;

  public GameControllerPanel(final KeyboardKempstonAndTapeIn module) {
    super(new GridBagLayout());

    this.module = module;

    final List<Controller> controllers = module.getDetectedControllers().stream()
        .sorted(comparing(Controller::getName))
        .collect(toList());

    final List<Gadapter> activeGadapters = module.getActiveGadapters();

    final GridBagConstraints constraints = new GridBagConstraints(
        0,
        GridBagConstraints.RELATIVE,
        1,
        1,
        1,
        1,
        GridBagConstraints.NORTHWEST,
        GridBagConstraints.HORIZONTAL,
        new Insets(0, 0, 0, 0),
        0,
        0);

    for (final Controller c : controllers) {
      final GadapterRecord newRecord = new GadapterRecord(c, activeGadapters);
      this.add(newRecord, constraints);
    }
  }

  public List<Gadapter> getSelected() {
    final List<Gadapter> result = new ArrayList<>();

    final Set<GadapterType> alreadySelected = new HashSet<>();

    for (final java.awt.Component c : this.getComponents()) {
      if (c instanceof GadapterRecord) {
        final GadapterRecord record = (GadapterRecord) c;
        final GadapterType gadapterType = (GadapterType) record.type.getSelectedItem();
        if (record.selected.isSelected() && gadapterType != GadapterType.NONE && !alreadySelected.contains(gadapterType)) {
          result.add(module.makeGadapter(record.controller, gadapterType));
          alreadySelected.add(gadapterType);
        }
      }
    }
    return result;
  }

  private static final class GadapterRecord extends JPanel {
    private final JCheckBox selected;
    private final JLabel name;
    private final JComboBox<GadapterType> type;
    private final Controller controller;

    GadapterRecord(final Controller controller, final List<Gadapter> activeGadapters) {
      super(new GridBagLayout());
      this.controller = controller;
      this.selected = new JCheckBox();
      this.name = new JLabel(controller.getName());
      this.type = new JComboBox<>(GadapterType.values());

      final Optional<Gadapter> activeForController = activeGadapters.stream().filter(x -> x.getController() == controller).findFirst();

      if (activeForController.isPresent()) {
        this.selected.setSelected(true);
        this.type.setSelectedItem(activeForController.get().getType());
      }

      final GridBagConstraints constraints = new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER, GridBagConstraints.BOTH, new Insets(2, 2, 2, 2), 0, 0);

      this.add(this.selected, constraints);

      constraints.gridx = 1;
      constraints.weightx = 1000;
      this.add(this.name, constraints);

      constraints.gridx = 2;
      constraints.weightx = 1;
      this.add(this.type, constraints);
    }
  }
}
