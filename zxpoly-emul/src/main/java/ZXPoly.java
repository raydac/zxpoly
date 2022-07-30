/*
 * Copyright (C) 2014-2019 Igor Maznitsa
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

import com.igormaznitsa.zxpoly.Bounds;
import com.igormaznitsa.zxpoly.MainForm;
import com.igormaznitsa.zxpoly.MainFormParameters;
import com.igormaznitsa.zxpoly.components.video.BorderWidth;
import com.igormaznitsa.zxpoly.components.video.VirtualKeyboardLook;
import com.igormaznitsa.zxpoly.components.video.timings.TimingProfile;
import com.igormaznitsa.zxpoly.utils.AppOptions;
import java.io.File;
import java.util.Objects;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import picocli.CommandLine;

@CommandLine.Command(name = "zxpoly-emulator", mixinStandardHelpOptions = true,
    version = ZXPoly.APP_VERSION,
    description = "Emulator of ZXPoly platform (a multi-CPU ZX-Spectrum 128 clone)",
    showAtFileInUsageHelp = true
)
public class ZXPoly implements Runnable {

  public static final String APP_TITLE = "ZX-Poly emulator";
  public static final String APP_VERSION = "v 2.3.1";
  @CommandLine.Option(
      names = {"-r", "--rom"},
      description = "bootstrap ROM as a single file"
  )
  private String romFile = null;

  @CommandLine.Option(
      names = {"--preferences-file"},
      description = "file to keep preferences"
  )
  private File preferencesFile = null;

  @CommandLine.Option(
      names = {"--undecorated"},
      description = "make main window undecorated"
  )
  private boolean undecorated = false;

  @CommandLine.Option(
      names = {"--bounds"},
      description = "define main frame bounds as X,Y,W,H or W,H",
      converter = Bounds.class
  )
  private Bounds bounds = null;

  @CommandLine.Option(
      names = {"--keyboard-bounds"},
      description = "define keyboard bounds as X,Y,W,H or W,H",
      converter = Bounds.class
  )
  private Bounds keyboardBounds = null;

  @CommandLine.Option(
      names = {"--indicators"},
      description = "show indicator panel",
      defaultValue = "true",
      arity = "1"
  )
  private boolean showIndicators;

  @CommandLine.Option(
      names = {"--mainmenu"},
      description = "show main menu",
      defaultValue = "true",
      arity = "1"
  )
  private boolean showMainMenu;

  @CommandLine.Option(
      names = {"-i", "--icon"},
      description = "application icon file"
  )
  private String applicationIconFilePath = null;

  @CommandLine.Option(
      names = {"-l", "--lookandfeel"},
      description = "look and feel class canonical name"
  )
  private String lookAndFeelClass = null;

  @CommandLine.Option(
      names = {"-q", "--apptitle"},
      description = "application title"
  )
  private String title = null;

  @CommandLine.Option(
      names = {"-b", "--border"},
      description = "border width, Valid values: ${COMPLETION-CANDIDATES}"
  )
  private BorderWidth borderWidth = null;

  @CommandLine.Option(
      names = {"-t", "--timing"},
      description = "timing profile, Valid values: ${COMPLETION-CANDIDATES}"
  )
  private TimingProfile timingProfile = null;

  @CommandLine.Option(
      names = {"-k", "--keyboard"},
      description = "virtual keyboard look, Valid values: ${COMPLETION-CANDIDATES}"
  )
  private VirtualKeyboardLook virtualKeyboardLook = null;

  @CommandLine.Option(
      names = {"-s", "--snapshot"},
      description = "open snapshot file, type will be recognized by file extension"
  )
  private File snapshotFile = null;

  public ZXPoly() {
  }

  private static File findPropertiesFileAmongArgs(final String... args) {
    File result = null;
    for (int i = 0; i < args.length && result == null; i++) {
      final String name = args[i];
      if ("--preferences-file".equals(name) && i < args.length - 1) {
        result = new File(args[i + 1]);
      }
    }
    return result;
  }

  public static void main(final String... args) {
    // find it before parse cli because we must get some properties just on start
    AppOptions.setForceFile(findPropertiesFileAmongArgs(args));

    final String uiScale = AppOptions.getInstance().getUiScale();
    if (uiScale != null) {
      if (System.getProperty("sun.java2d.uiScale", null) == null) {
        System.out.println("Detected scale UI: " + uiScale);
        System.setProperty("sun.java2d.uiScale", uiScale.trim() + 'x');
        System.setProperty("sun.java2d.uiScale.enabled", "true");
      } else {
        System.out.println("Detected provided sun.java2d.uiScale property: " +
            System.getProperty("sun.java2d.uiScale", null));
      }
    }

    for (final Handler h : Logger.getLogger("").getHandlers()) {
      h.setFormatter(new Formatter() {

        @Override
        public String format(final LogRecord record) {
          return record.getLevel() + " [" + record.getLoggerName() + "] : " + record.getMessage() +
              '\n';
        }
      });
    }

    final int result = new CommandLine(new ZXPoly())
        .setExpandAtFiles(true)
        .execute(args);
    if (result != 0) {
      System.exit(result);
    }
  }

  @Override
  public void run() {
    SwingUtilities.invokeLater(() -> {
      final MainForm form;

      final String uiLfClass = Objects.requireNonNullElse(this.lookAndFeelClass,
          AppOptions.getInstance().getUiLfClass());
      try {
        UIManager.setLookAndFeel(uiLfClass);
      } catch (Exception ex) {
        System.err.println("Can't select L&F: " + uiLfClass);
      }

      try {
        String romPath = this.romFile;
        if (romPath == null) {
          romPath = AppOptions.getInstance().getCustomRomPath();
          if (romPath == null) {
            romPath =
                System.getProperty("zxpoly.rom.path", AppOptions.getInstance().getActiveRom());
          } else {
            System.out.println("Custom ROM path in use: " + romPath);
          }
        }

        final MainFormParameters parameters = new MainFormParameters();

        parameters.setTitle(requireNonNullElse(this.title, APP_TITLE + ' ' + APP_VERSION))
            .setAppIconPath(this.applicationIconFilePath)
            .setRomPath(romPath)
            .setBounds(this.bounds)
            .setKeyboardBounds(this.keyboardBounds)
            .setShowMainMenu(this.showMainMenu)
            .setUndecorated(this.undecorated)
            .setShowIndicatorPanel(this.showIndicators)
            .setVirtualKeyboardLook(requireNonNullElseGet(this.virtualKeyboardLook,
                () -> AppOptions.getInstance().getKeyboardLook()))
            .setOpenSnapshot(this.snapshotFile)
            .setBorderWidth(requireNonNullElseGet(this.borderWidth,
                () -> AppOptions.getInstance().getBorderWidth()))
            .setTimingProfile(requireNonNullElseGet(this.timingProfile,
                () -> AppOptions.getInstance().getTimingProfile()))
        ;

        form = new MainForm(parameters);
      } catch (Exception ex) {
        ex.printStackTrace();
        System.exit(1);
        return;
      }
      form.setVisible(true);
    });
  }
}
