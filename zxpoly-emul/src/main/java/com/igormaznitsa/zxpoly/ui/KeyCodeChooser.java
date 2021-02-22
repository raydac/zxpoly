package com.igormaznitsa.zxpoly.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.OptionalInt;

public class KeyCodeChooser extends JPanel {
  private static final Map<Integer, String> KEY_NAMES;

  static {
    final Map<Integer, String> names = new HashMap<>();
    for (final Field field : KeyEvent.class.getFields()) {
      if (field.getType() == int.class && Modifier.isPublic(field.getModifiers())
              && Modifier.isFinal(field.getModifiers())
              && Modifier.isStatic(field.getModifiers())
              && field.getName().startsWith("VK_")) {
        final String name = field.getName().substring(3);
        try {
          final int code = field.getInt(null);
          names.put(code, name);
        } catch (Exception ex) {
          throw new Error("Unexpected error during key event code extraction: " + field, ex);
        }
      }
    }
    KEY_NAMES = Collections.unmodifiableMap(names);
  }

  private final JTextField textField;
  private final JToggleButton buttonSelect;
  private OptionalInt key = OptionalInt.empty();

  public KeyCodeChooser() {
    super(new GridBagLayout());
    this.textField = new JTextField() {
      @Override
      public boolean isFocusable() {
        return false;
      }
    };
    this.textField.setEditable(false);
    this.textField.setColumns(16);
    this.buttonSelect = new JToggleButton("SELECT");
    this.buttonSelect.addKeyListener(new KeyAdapter() {
      @Override
      public void keyPressed(final KeyEvent e) {
        if (!e.isConsumed() && buttonSelect.isSelected()) {
          if (e.getKeyCode() != KeyEvent.VK_ESCAPE) {
            setKey(e.getKeyCode());
          }
          buttonSelect.setSelected(false);
          e.consume();
        }
      }
    });
    this.buttonSelect.setToolTipText("Activate and press key");
    this.buttonSelect.addFocusListener(new FocusAdapter() {
      @Override
      public void focusLost(final FocusEvent e) {
        if (buttonSelect.isSelected()) {
          buttonSelect.setSelected(false);
        }
      }
    });
    this.textField.addMouseListener(new MouseAdapter() {
      @Override
      public void mouseClicked(MouseEvent e) {
        if (buttonSelect.isSelected()) {
          buttonSelect.setSelected(false);
        } else {
          buttonSelect.setSelected(true);
          buttonSelect.requestFocus();
        }
      }
    });
    final GridBagConstraints gbc = new GridBagConstraints(0, 0, 1, 1, 1000, 1, GridBagConstraints.WEST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0);
    this.add(this.textField, gbc);
    gbc.weightx = 1;
    gbc.gridx = 1;
    this.add(this.buttonSelect, gbc);
  }

  public OptionalInt getKey() {
    return this.key;
  }

  public void setKey(final int key) {
    this.key = key < 0 ? OptionalInt.empty() : OptionalInt.of(key);
    final String name = KEY_NAMES.get(key);
    if (name == null) {
      this.textField.setText("<UNKNOWN>");
    } else {
      this.textField.setText(name);
    }

  }
}
