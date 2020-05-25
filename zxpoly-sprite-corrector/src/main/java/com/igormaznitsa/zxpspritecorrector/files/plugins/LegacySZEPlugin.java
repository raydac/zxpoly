package com.igormaznitsa.zxpspritecorrector.files.plugins;

import com.igormaznitsa.zxpspritecorrector.components.EditorComponent;
import com.igormaznitsa.zxpspritecorrector.components.ZXPolyData;
import com.igormaznitsa.zxpspritecorrector.files.Info;
import com.igormaznitsa.zxpspritecorrector.files.SessionData;
import java.awt.Rectangle;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import javax.swing.filechooser.FileFilter;

public class LegacySZEPlugin extends AbstractFilePlugin {
  @Override
  public String getToolTip(boolean forExport) {
    return forExport ? "unsupported" : "Legacy ZXPoly corrector format";
  }

  @Override
  public boolean doesContainInternalFileItems() {
    return false;
  }

  @Override
  public FileFilter getImportFileFilter() {
    return this;
  }

  @Override
  public FileFilter getExportFileFilter() {
    return this;
  }

  @Override
  public String getPluginDescription(boolean forExport) {
    return null;
  }

  @Override
  public String getPluginUID() {
    return "LSZE";
  }

  @Override
  public List<Info> getImportingContainerFileList(File file) {
    return Collections.emptyList();
  }

  @Override
  public String getExtension(boolean forExport) {
    return "zxp";
  }

  @Override
  public ReadResult readFrom(final File file, final int index) throws IOException {
    try (final DataInputStream inStream = new DataInputStream(new FileInputStream(file))) {
      if (!"ZXEC".equals(inStream.readUTF())) {
        throw new IOException("Wrong file format");
      }

      int version = inStream.readInt();

      if (version > 0x00010001) {
        throw new IOException("Unsupported format version");
      }

      final int colorInkIndex = inStream.readInt();
      final int colorPaintIndex = inStream.readInt();
      final int selectedColumns = inStream.readInt();
      final int selectedPosition = inStream.readInt();
      final int selectedTool = inStream.readInt();
      final int zoom = inStream.readInt();

      final boolean gridState = inStream.readBoolean();
      final boolean showInverted = inStream.readBoolean();
      final boolean lockPosition = inStream.readBoolean();
      final boolean zxScreenMode = inStream.readBoolean();

      boolean showColumnBorders = false;

      if (version > 0x00010000) {
        showColumnBorders = inStream.readBoolean();
      }

      Rectangle selection = null;

      if (inStream.readBoolean()) {
        selection = new Rectangle();
        selection.x = inStream.readInt();
        selection.y = inStream.readInt();
        selection.width = inStream.readInt();
        selection.height = inStream.readInt();
      }


      int length = inStream.readInt();
      int[] paintData = new int[length];
      for (int li = 0; li < paintData.length; li++) {
        paintData[li] = inStream.readInt();
      }

      length = inStream.readInt();
      byte[] mask = new byte[length];
      inStream.readFully(mask);

      final int hobetaStartAddress = inStream.readInt();
      final int hobetaSectors = inStream.readInt();
      final int hobetaType = inStream.readInt();
      final byte[] hobetaName = new byte[8];
      inStream.readFully(hobetaName);
      final int hobetaLength = inStream.readInt();
      final byte[] hobetaData = new byte[hobetaLength];
      inStream.readFully(hobetaData);

      final Info info =
          new Info(new String(hobetaName, StandardCharsets.US_ASCII), (char) (hobetaType & 0xFF),
              hobetaStartAddress, hobetaLength, 0);

      final byte[][] decodedZxPolyPlanes = new byte[4][hobetaData.length];
      for (int a = 0; a < hobetaData.length; a++) {
        int data = paintData[a];

        int planeY = 0;
        int planeG = 0;
        int planeR = 0;
        int planeB = 0;

        for (int i = 0; i < 8; i++) {
          final int portion = (data >>> 28) & 0xF;

          planeY = (planeY << 1) | ((portion >> 3) & 1);
          planeG = (planeG << 1) | ((portion >> 2) & 1);
          planeR = (planeR << 1) | ((portion >> 1) & 1);
          planeB = (planeB << 1) | (portion & 1);

          data <<= 4;
        }

        decodedZxPolyPlanes[0][a] = (byte) planeG;
        decodedZxPolyPlanes[1][a] = (byte) planeR;
        decodedZxPolyPlanes[2][a] = (byte) planeB;
        decodedZxPolyPlanes[3][a] = (byte) planeY;
      }

      final ZXPolyData data = new ZXPolyData(info, this, hobetaData, mask, decodedZxPolyPlanes);

      final SessionData sessionData = new SessionData(
          selectedPosition,
          gridState,
          showColumnBorders,
          zxScreenMode,
          showInverted,
          false,
          selectedColumns,
          EditorComponent.AttributeMode.DONT_SHOW,
          zoom
      );

      final ReadResult result = new ReadResult(data, sessionData);

      return result;
    }
  }

  @Override
  public void writeTo(File file, ZXPolyData data, SessionData sessionData) throws IOException {
    throw new UnsupportedOperationException("Write is unsupported for the plugin");
  }

  @Override
  public boolean accept(File path) {
    return path != null &&
        (path.isDirectory() || path.getName().endsWith(this.getExtension(false)));
  }

  @Override
  public boolean isExportable() {
    return false;
  }

  @Override
  public String getDescription() {
    return getToolTip(false) + " (*.ZXP)";
  }
}
