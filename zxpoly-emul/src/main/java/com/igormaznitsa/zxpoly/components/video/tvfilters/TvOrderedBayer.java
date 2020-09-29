package com.igormaznitsa.zxpoly.components.video.tvfilters;

import com.igormaznitsa.zxpoly.components.video.VideoController;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferInt;
import java.util.Arrays;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;

public final class TvOrderedBayer implements TvFilter {

  public static int THRESHOLD = 128;

  private static final int MATRIX[][] = {
      {0, 48, 12, 60, 3, 51, 15, 63},
      {32, 16, 44, 28, 35, 19, 47, 31},
      {8, 56, 4, 52, 11, 59, 7, 55},
      {40, 24, 36, 20, 43, 27, 39, 23},
      {2, 50, 14, 62, 1, 49, 13, 61},
      {34, 18, 46, 30, 33, 17, 45, 29},
      {10, 58, 6, 54, 9, 57, 5, 53},
      {42, 26, 38, 22, 41, 25, 37, 21}
  };

  private static final TvOrderedBayer INSTANCE = new TvOrderedBayer();

  public static TvOrderedBayer getInstance() {
    return INSTANCE;
  }

  public static float cr = 0.4047f;
  public static float cg = 0.5913f;
  public static float cb = 0.2537f;

  public static void main(String... args) {
    SwingUtilities.invokeLater(() -> {
      final JFrame frame = new JFrame("TEST");
      frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);

      final BufferedImage etalon = new BufferedImage(512, 64, BufferedImage.TYPE_INT_ARGB);

      final BufferedImage image = new BufferedImage(512, 384, BufferedImage.TYPE_INT_ARGB);
      final Graphics2D gfx = image.createGraphics();
      final Graphics2D gfx2 = etalon.createGraphics();
      try {
        final int w = image.getWidth() / VideoController.PALETTE_ZXPOLY_COLORS.length;
        for (int i = 0; i < VideoController.PALETTE_ZXPOLY_COLORS.length; i++) {
          final int x = w * i;
          gfx.setColor(VideoController.PALETTE_ZXPOLY_COLORS[i]);
          gfx.fillRect(x, 0, w, 384);
          gfx2.setColor(VideoController.PALETTE_ZXPOLY_COLORS[i]);
          gfx2.fillRect(x, 0, w, etalon.getHeight());
        }
      } finally {
        gfx.dispose();
        gfx2.dispose();
      }

      final JPanel panel = new JPanel(new BorderLayout());

      final JLabel label = new JLabel(new ImageIcon(image));


      panel.add(new JLabel(new ImageIcon(etalon)), BorderLayout.NORTH);
      panel.add(label, BorderLayout.CENTER);

      final JPanel control = new JPanel(new BorderLayout());

      final JPanel spinners = new JPanel(new FlowLayout(FlowLayout.CENTER));

      final JSpinner spinL =
          new JSpinner(new SpinnerNumberModel(TvOrderedBayer.THRESHOLD, 0, 255, 1));

      final JSpinner spinR =
          new JSpinner(new SpinnerNumberModel(TvOrderedBayer.cr, 0.0d, 1.0d, 0.001d));
      final JSpinner spinG =
          new JSpinner(new SpinnerNumberModel(TvOrderedBayer.cg, 0.0d, 1.0d, 0.001d));
      final JSpinner spinB =
          new JSpinner(new SpinnerNumberModel(TvOrderedBayer.cb, 0.0d, 1.0d, 0.001d));

      ((JSpinner.DefaultEditor) spinL.getEditor()).getTextField().setColumns(8);
      ((JSpinner.DefaultEditor) spinR.getEditor()).getTextField().setColumns(8);
      ((JSpinner.DefaultEditor) spinG.getEditor()).getTextField().setColumns(8);
      ((JSpinner.DefaultEditor) spinB.getEditor()).getTextField().setColumns(8);

      spinners.add(new JLabel("Level:"));
      spinners.add(spinL);
      spinners.add(new JLabel(" R:"));
      spinners.add(spinR);
      spinners.add(new JLabel(" G:"));
      spinners.add(spinG);
      spinners.add(new JLabel(" B:"));
      spinners.add(spinB);

      spinL.addChangeListener(e -> {
        int v = (Integer) ((JSpinner) e.getSource()).getValue();
        TvOrderedBayer.THRESHOLD = v;
        BufferedImage newImage = TvOrderedBayer.getInstance().apply(image, 1.0f, 0, true);
        label.setIcon(new ImageIcon(newImage));
        label.repaint();
      });

      spinR.addChangeListener(e -> {
        double v = (Double) ((JSpinner) e.getSource()).getValue();
        TvOrderedBayer.cr = (float) v;
        BufferedImage newImage = TvOrderedBayer.getInstance().apply(image, 1.0f, 0, true);
        label.setIcon(new ImageIcon(newImage));
        label.repaint();
      });

      spinG.addChangeListener(e -> {
        double v = (Double) ((JSpinner) e.getSource()).getValue();
        TvOrderedBayer.cg = (float) v;
        BufferedImage newImage = TvOrderedBayer.getInstance().apply(image, 1.0f, 0, true);
        label.setIcon(new ImageIcon(newImage));
        label.repaint();
      });

      spinB.addChangeListener(e -> {
        double v = (Double) ((JSpinner) e.getSource()).getValue();
        TvOrderedBayer.cb = (float) v;
        BufferedImage newImage = TvOrderedBayer.getInstance().apply(image, 1.0f, 0, true);
        label.setIcon(new ImageIcon(newImage));
        label.repaint();
      });


      control.add(spinners, BorderLayout.CENTER);

      panel.add(control, BorderLayout.SOUTH);

      frame.setContentPane(panel);

      frame.pack();

      frame.setVisible(true);
    });
  }

  private static int getPseudoGray(final int argb) {
    final int r = (argb >> 16) & 0xFF;
    final int g = (argb >> 8) & 0xFF;
    final int b = argb & 0xFF;
    return Math.min(Math.round(r * cr + g * cg + b * cb), 255);
  }


  @Override
  public Color applyBorderColor(final Color borderColor) {
    return borderColor.getRed() < THRESHOLD ? Color.BLACK : Color.WHITE;
  }

  @Override
  public BufferedImage apply(BufferedImage srcImageArgb512x384, float zoom,
                             int argbBorderColor, boolean firstInChain) {
    final int[] src = ((DataBufferInt) srcImageArgb512x384.getRaster().getDataBuffer()).getData();
    final int[] dst = SHARED_BUFFER_RASTER;

    for (int y = 0; y < RASTER_HEIGHT; y++) {
      for (int x = 0; x < RASTER_WIDTH_ARGB_INT; x++) {
        final int pos = y * RASTER_WIDTH_ARGB_INT + x;
        float level = getPseudoGray(src[pos]);
        level += level * MATRIX[x & 7][y & 7] / 65.0f;
        if (level < THRESHOLD) {
          dst[pos] = 0xFF000000;
        } else {
          dst[pos] = 0xFFFFFFFF;
        }
      }
    }

    return SHARED_BUFFER;
  }

  @Override
  public byte[] apply(
      final boolean forceCopy,
      final byte[] rgbArray512x384,
      final int argbBorderColor
  ) {
    final byte[] result =
        forceCopy ? Arrays.copyOf(rgbArray512x384, rgbArray512x384.length) : rgbArray512x384;

    for (int y = 0; y < RASTER_HEIGHT; y++) {
      for (int x = 0; x < RASTER_WIDTH_ARGB_INT; x += 3) {
        int pos = y * RASTER_WIDTH_RGB_BYTE + x * 3;
        int value = result[pos] & 0xFF;
        value += value * MATRIX[x % 4][y % 4] / 17;
        if (value < THRESHOLD) {
          value = 0;
        } else {
          value = 0xFF;
        }
        result[pos++] = (byte) value;
        result[pos++] = (byte) value;
        result[pos] = (byte) value;
      }
    }

    return result;
  }

  @Override
  public void apply(Graphics2D gfx, Rectangle imageArea, float zoom) {

  }
}
