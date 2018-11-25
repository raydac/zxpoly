package com.igormaznitsa.zxpspritecorrector;

import com.igormaznitsa.zxpspritecorrector.cmdline.SliceImageCmd;
import java.util.Arrays;
import java.util.Locale;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;

public class main {

  public static void main(final String[] args) {
    if (args.length > 0) {
      switch (args[0]) {
        case "sliceImage": {
          System.exit(new SliceImageCmd().process(Arrays.copyOfRange(args, 1, args.length)));
        }
        break;
        default: {
          System.err.println("Unexpected command : " + args[0]);
          System.exit(1);
        }
      }
    }

    java.util.Locale.setDefault(Locale.ROOT);

    try {
      for (final LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
        if ("Nimbus".equals(info.getName())) {
          UIManager.setLookAndFeel(info.getClassName());
          break;
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

    SwingUtilities.invokeLater(() -> {
      final JFrame frame = new MainFrame();
      frame.setVisible(true);
    });

  }
}
