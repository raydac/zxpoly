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
    }

    SwingUtilities.invokeLater(new Runnable() {

      @Override
      public void run() {
        final JFrame frame = new MainFrame();
        frame.invalidate();
        frame.repaint();
      }
    });

  }
}
