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

import com.igormaznitsa.zxpoly.Bounds;
import com.igormaznitsa.zxpoly.MainForm;
import com.igormaznitsa.zxpoly.MainFormParameters;
import com.igormaznitsa.zxpoly.Version;
import com.igormaznitsa.zxpoly.components.BoardMode;
import com.igormaznitsa.zxpoly.components.sound.VolumeProfile;
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
    version = Version.VERSION_MAJOR + "." + Version.VERSION_MINOR + "." + Version.VERSION_BUILD,
    description = "Emulator of ZXPoly platform (a multi-CPU ZX-Spectrum 128 clone)",
    showAtFileInUsageHelp = true
)
public class ZXPoly implements Runnable, Version {

  @CommandLine.Option(
      names = {"-r", "--rom"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "bootstrap ROM as a single file"
  )
  private String romFile = null;

  @CommandLine.Option(
      names = {"--preferences-file"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "file to keep preferences"
  )
  private File preferencesFile = null;

  @CommandLine.Option(
      names = {"--undecorated"},
      description = "make main window undecorated",
      defaultValue = CommandLine.Option.NULL_VALUE
  )
  private Boolean undecorated = null;

  @CommandLine.Option(
      names = {"--sound"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "activate sound"
  )
  private Boolean activateSound = null;

  @CommandLine.Option(
      names = {"--sound-acb"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "use ACB channel order sound"
  )
  private Boolean forceAcbSound = null;

  @CommandLine.Option(
      names = {"--try-less-resources"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "try use less system resources"
  )
  private Boolean tryLessResourcess = null;

  @CommandLine.Option(
      names = {"--sync-repaint"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "use sync repaint mechanism, can be slow"
  )
  private Boolean syncRepaint = null;

  @CommandLine.Option(
      names = {"--bounds"},
      description = "define main frame bounds as X,Y,W,H or W,H",
      defaultValue = CommandLine.Option.NULL_VALUE,
      converter = Bounds.class
  )
  private Bounds bounds = null;

  @CommandLine.Option(
      names = {"--keyboard-bounds"},
      description = "define keyboard bounds as X,Y,W,H or W,H",
      defaultValue = CommandLine.Option.NULL_VALUE,
      converter = Bounds.class
  )
  private Bounds keyboardBounds = null;

  @CommandLine.Option(
      names = {"--indicators"},
      description = "show indicator panel",
      defaultValue = CommandLine.Option.NULL_VALUE,
      arity = "1"
  )
  private Boolean showIndicators = null;

  @CommandLine.Option(
      names = {"--main-menu"},
      description = "show main menu",
      defaultValue = CommandLine.Option.NULL_VALUE,
      arity = "1"
  )
  private Boolean showMainMenu = null;

  @CommandLine.Option(
      names = {"--covox-fb"},
      description = "use FB port for Covox",
      defaultValue = CommandLine.Option.NULL_VALUE,
      arity = "1"
  )
  private Boolean covoxFb = null;

  @CommandLine.Option(
      names = {"--turbo-sound"},
      description = "turn on support of turbo sound",
      defaultValue = CommandLine.Option.NULL_VALUE,
      arity = "1"
  )
  private Boolean turboSound = null;

  @CommandLine.Option(
      names = {"--kempston-mouse"},
      description = "allow kempston mouse",
      defaultValue = CommandLine.Option.NULL_VALUE,
      arity = "1"
  )
  private Boolean allowKempstonMouse = null;

  @CommandLine.Option(
      names = {"--attribute-port-ff"},
      description = "turn on support for attribute port FF",
      defaultValue = CommandLine.Option.NULL_VALUE,
      arity = "1"
  )
  private Boolean attributePortFf = null;

  @CommandLine.Option(
      names = {"--ula-plus"},
      description = "turn on support for Ula Plus",
      defaultValue = CommandLine.Option.NULL_VALUE,
      arity = "1"
  )
  private Boolean ulaPlus = null;

  @CommandLine.Option(
      names = {"--interlace-scan"},
      description = "turn on interlacing",
      defaultValue = CommandLine.Option.NULL_VALUE,
      arity = "1"
  )
  private Boolean interlaceScan = null;

  @CommandLine.Option(
      names = {"-i", "--icon"},
      description = "application icon file",
      defaultValue = CommandLine.Option.NULL_VALUE
  )
  private String applicationIconFilePath = null;

  @CommandLine.Option(
      names = {"-l", "--look-and-feel"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "look and feel class canonical name"
  )
  private String lookAndFeelClass = null;

  @CommandLine.Option(
      names = {"-q", "--app-title"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "application title"
  )
  private String title = null;

  @CommandLine.Option(
      names = {"-b", "--border"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "border width, Valid values: ${COMPLETION-CANDIDATES}"
  )
  private BorderWidth borderWidth = null;

  @CommandLine.Option(
      names = {"-t", "--timing"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "timing profile, Valid values: ${COMPLETION-CANDIDATES}"
  )
  private TimingProfile timingProfile = null;

  @CommandLine.Option(
      names = {"-k", "--keyboard"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "virtual keyboard look, Valid values: ${COMPLETION-CANDIDATES}"
  )
  private VirtualKeyboardLook virtualKeyboardLook = null;

  @CommandLine.Option(
      names = {"--board-mode"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "motherboard mode, Valid values: ${COMPLETION-CANDIDATES}"
  )
  private BoardMode boardMode = null;

  @CommandLine.Option(
      names = {"--volume-profile"},
      defaultValue = CommandLine.Option.NULL_VALUE,
      description = "sound volume profile, Valid values: ${COMPLETION-CANDIDATES}"
  )
  private VolumeProfile volumeProfile = null;

  @CommandLine.Option(
      names = {"-s", "--snapshot"},
      defaultValue = CommandLine.Option.NULL_VALUE,
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

        parameters
            .setTitle(this.title)
            .setAppIconPath(this.applicationIconFilePath)
            .setRomPath(romPath)
            .setBounds(this.bounds)
            .setPreferencesFile(this.preferencesFile)
            .setKeyboardBounds(this.keyboardBounds)
            .setShowMainMenu(this.showMainMenu)
            .setUndecorated(this.undecorated)
            .setSyncRepaint(this.syncRepaint)
            .setForceAcbChannelSound(this.forceAcbSound)
            .setActivateSound(this.activateSound)
            .setTryUseLessSystemResources(this.tryLessResourcess)
            .setShowIndicatorPanel(this.showIndicators)
            .setVirtualKeyboardLook(this.virtualKeyboardLook)
            .setOpenSnapshot(this.snapshotFile)
            .setBorderWidth(this.borderWidth)
            .setInterlaceScan(this.interlaceScan)
            .setCovoxFb(this.covoxFb)
            .setAllowKempstonMouse(this.allowKempstonMouse)
            .setAttributePortFf(this.attributePortFf)
            .setUlaPlus(this.ulaPlus)
            .setVolumeProfile(this.volumeProfile)
            .setBoardMode(this.boardMode)
            .setTimingProfile(this.timingProfile);

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
