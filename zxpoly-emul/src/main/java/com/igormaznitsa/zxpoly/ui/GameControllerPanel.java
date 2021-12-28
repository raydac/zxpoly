package com.igormaznitsa.zxpoly.ui;

import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import com.igormaznitsa.zxpoly.components.gadapter.GameControllerAdapter;
import com.igormaznitsa.zxpoly.components.gadapter.GameControllerAdapterType;
import net.java.games.input.Controller;

import javax.swing.*;
import java.awt.*;
import java.util.List;
import java.util.*;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

public final class GameControllerPanel extends JPanel {

  private final KeyboardKempstonAndTapeIn module;

  public GameControllerPanel(final KeyboardKempstonAndTapeIn module) {
    super(new GridBagLayout());

    this.module = module;

    final List<Controller> controllers = module.getDetectedControllers().stream()
            .sorted(comparing(Controller::getName))
            .collect(toList());

    final List<GameControllerAdapter> activeGameControllerAdapters = module.getActiveGadapters();

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
      final GadapterRecord newRecord = new GadapterRecord(c, activeGameControllerAdapters);
      this.add(newRecord, constraints);
    }
  }

  public List<GameControllerAdapter> getSelected() {
    final List<GameControllerAdapter> result = new ArrayList<>();

    final Set<GameControllerAdapterType> alreadySelected = new HashSet<>();

    for (final java.awt.Component c : this.getComponents()) {
      if (c instanceof GadapterRecord) {
        final GadapterRecord record = (GadapterRecord) c;
        final GameControllerAdapterType gameControllerAdapterType =
                (GameControllerAdapterType) record.type.getSelectedItem();
        if (record.selected.isSelected() &&
                gameControllerAdapterType != GameControllerAdapterType.NONE &&
                !alreadySelected.contains(
                        gameControllerAdapterType)) {
          result.add(module.makeGameControllerAdapter(record.controller, gameControllerAdapterType));
          alreadySelected.add(gameControllerAdapterType);
        }
      }
    }
    return result;
  }

  private static final class GadapterRecord extends JPanel {
    private final JCheckBox selected;
    private final JLabel name;
    private final JComboBox<GameControllerAdapterType> type;
    private final Controller controller;

    GadapterRecord(final Controller controller,
                   final List<GameControllerAdapter> activeGameControllerAdapters) {
      super(new GridBagLayout());
      this.controller = controller;
      this.selected = new JCheckBox();
      this.name = new JLabel(controller.getName());
      this.type = new JComboBox<>(GameControllerAdapterType.values());

      final Optional<GameControllerAdapter> activeForController =
              activeGameControllerAdapters.stream().filter(x -> x.getController() == controller)
                      .findFirst();

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
