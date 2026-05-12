package com.igormaznitsa.zxpoly.ui;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;

import com.igormaznitsa.zxpoly.components.KeyboardKempstonAndTapeIn;
import com.igormaznitsa.zxpoly.components.gadapter.GameControllerAdapter;
import com.igormaznitsa.zxpoly.components.gadapter.GameControllerAdapterType;
import de.gurkenlabs.input4j.InputDevice;
import java.awt.Component;
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

public final class GameControllerPanel extends JPanel {

  private final KeyboardKempstonAndTapeIn module;

  public GameControllerPanel(final KeyboardKempstonAndTapeIn module) {
    super(new GridBagLayout());

    this.module = module;

    final List<InputDevice> controllers = module.getDetectedControllers().stream()
        .sorted(comparing(InputDevice::getDisplayName))
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

    for (final InputDevice c : controllers) {
      final GadapterRecord newRecord = new GadapterRecord(c, activeGameControllerAdapters);
      this.add(newRecord, constraints);
    }
  }

  public List<GameControllerAdapter> getSelected() {
    final List<GameControllerAdapter> result = new ArrayList<>();

    final Set<GameControllerAdapterType> alreadySelected = new HashSet<>();

    for (final Component c : this.getComponents()) {
      if (c instanceof GadapterRecord) {
        final GadapterRecord record = (GadapterRecord) c;
        final GameControllerAdapterType gameControllerAdapterType =
            (GameControllerAdapterType) record.type.getSelectedItem();
        if (record.selected.isSelected()
            && gameControllerAdapterType != GameControllerAdapterType.NONE
            && !alreadySelected.contains(gameControllerAdapterType)) {
          result.add(
              this.module.makeGameControllerAdapter(record.inputDevice, gameControllerAdapterType));
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
    private final InputDevice inputDevice;

    GadapterRecord(final InputDevice inputDevice,
                   final List<GameControllerAdapter> activeGameControllerAdapters) {
      super(new GridBagLayout());
      this.inputDevice = inputDevice;
      this.selected = new JCheckBox();
      this.name = new JLabel(inputDevice.getDisplayName());
      this.type = new JComboBox<>(GameControllerAdapterType.values());

      final Optional<GameControllerAdapter> activeForController =
          activeGameControllerAdapters.stream()
              .filter(x -> x.getInputDevice() == inputDevice)
              .findFirst();

      if (activeForController.isPresent()) {
        this.selected.setSelected(true);
        this.type.setSelectedItem(activeForController.get().getType());
      }

      final GridBagConstraints constraints =
          new GridBagConstraints(0, 0, 1, 1, 1, 1, GridBagConstraints.CENTER,
              GridBagConstraints.BOTH,
              new Insets(2, 2, 2, 2), 0, 0);

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
