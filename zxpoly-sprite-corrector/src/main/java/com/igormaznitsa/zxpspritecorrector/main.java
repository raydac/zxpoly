package com.igormaznitsa.zxpspritecorrector;

import java.util.Locale;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

public class main {

  public static void main(String[] _args) {
    java.util.Locale.setDefault(Locale.ROOT);

    try {
      for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    }
    catch (Exception e) {
        e.printStackTrace();
    }

    SwingUtilities.invokeLater(() -> {
        final JFrame frame = new MainFrame();
        frame.setVisible(true);
    });

  }
}
