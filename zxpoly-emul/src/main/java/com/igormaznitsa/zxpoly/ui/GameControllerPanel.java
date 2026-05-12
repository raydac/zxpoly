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
import java.util.Objects;
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
      final GadapterRecord newRecord = new GadapterRecord(c.getID(), c.getDisplayName(),
          activeGameControllerAdapters);
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
          final InputDevice device = this.module.getDetectedControllers().stream()
              .filter(d -> Objects.equals(d.getID(), record.deviceId))
              .findFirst()
              .orElse(null);
          if (device != null) {
            result.add(this.module.makeGameControllerAdapter(device, gameControllerAdapterType));
            alreadySelected.add(gameControllerAdapterType);
          }
        }
      }
    }
    return result;
  }

  private static final class GadapterRecord extends JPanel {
    private final String deviceId;
    private final JCheckBox selected;
    private final JLabel name;
    private final JComboBox<GameControllerAdapterType> type;

    GadapterRecord(final String deviceId,
                   final String displayName,
                   final List<GameControllerAdapter> activeGameControllerAdapters) {
      super(new GridBagLayout());
      this.deviceId = deviceId;
      this.selected = new JCheckBox();
      this.name = new JLabel(displayName);
      this.type = new JComboBox<>(GameControllerAdapterType.values());

      final Optional<GameControllerAdapter> activeForController =
          activeGameControllerAdapters.stream()
              .filter(x -> Objects.equals(x.getInputDevice().getID(), deviceId))
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
