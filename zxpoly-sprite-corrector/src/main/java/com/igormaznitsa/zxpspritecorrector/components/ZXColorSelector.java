package com.igormaznitsa.zxpspritecorrector.components;

import com.igormaznitsa.zxpspritecorrector.utils.GfxUtils;
import com.igormaznitsa.zxpspritecorrector.utils.ZXPalette;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import javax.swing.*;

public final class ZXColorSelector extends JComponent implements MouseListener {

  private static final long serialVersionUID = 4513035804817773772L;
  private int selectedInk = 0;
  private int selectedPaint = 15;

  private final Image iconINK;
  private final Image iconPAINT;

  private final List<ActionListener> actionListeners = new ArrayList<ActionListener>();

  private final Dimension minSize;
  
  public ZXColorSelector() {
    super();

    iconINK = GfxUtils.loadImage("ink.gif");
    iconPAINT = GfxUtils.loadImage("paint.gif");

    selectedInk = 0;
    selectedPaint = 15;

    if (iconINK != null && iconPAINT!=null){
      this.minSize = new Dimension((Math.max(iconINK.getWidth(null), iconPAINT.getWidth(null))+5)*8, iconINK.getHeight(null)+5+iconPAINT.getHeight(null));
    }else{
      this.minSize = new Dimension(320, 40);
    }
    
    addMouseListener(this);
  }

  public int getSelectedInk() {
    return selectedInk;
  }

  public int getSelectedPaint() {
    return selectedPaint;
  }

  @Override
  public Dimension getMinimumSize() {
    return this.minSize;
  }

  @Override
  public void paintComponent(final Graphics gfx) {
    final int fullwidth = getWidth();
    final int fullheight = getHeight();

    final int cellwidth = fullwidth / 8;
    final int cellheight = fullheight / 2;

    if (cellwidth == 0 || cellheight == 0) {
      return;
    }

    for (int i = 0; i < 8; i++) {
      int indx = i;
      gfx.setColor(ZXPalette.COLORS[i]);

      final int cellx = i * cellwidth;

      gfx.fillRect(cellx, 0, cellwidth, cellheight);

      if (indx == selectedInk) {
        if (iconINK != null) {
          gfx.drawImage(iconINK, cellx, 0, null);
        }
      }

      if (indx == selectedPaint && iconPAINT != null) {
        gfx.drawImage(iconPAINT, cellx + (cellwidth - iconPAINT.getWidth(null)), cellheight - iconPAINT.getHeight(null), null);
      }

      indx += 8;
      gfx.setColor(ZXPalette.COLORS[indx]);

      gfx.fillRect(cellx, cellheight, cellwidth, cellheight);
      if (indx == selectedInk && iconINK != null) {
        gfx.drawImage(iconINK, cellx, cellheight, null);
      }

      if (indx == selectedPaint) {
        if (iconPAINT != null) {
          gfx.drawImage(iconPAINT, cellx + (cellwidth - iconPAINT.getWidth(null)), cellheight + (cellheight - iconPAINT.getHeight(null)), null);
        }
      }

    }
  }

  @Override
  public void mouseClicked(final MouseEvent e) {
    final int x = e.getX();
    final int y = e.getY();

    final int width = getWidth();
    final int height = getHeight();

    final int cellwidth = width / 8;
    final int cellheight = height / 2;

    final int column = x / cellwidth;
    final int row = y / cellheight;

    if (row < 0 || row > 1 || column < 0 || column > 7) {
      return;
    }

    switch (e.getButton()) {
      case MouseEvent.BUTTON1: {
        this.selectedInk = row * 8 + column;
        repaint();

        for (ActionListener p_listener : actionListeners) {
          p_listener.actionPerformed(new ActionEvent(this, 0, null));
        }

      }
      break;
      case MouseEvent.BUTTON3: {
        this.selectedPaint = row * 8 + column;
        repaint();

        for (final ActionListener l : this.actionListeners) {
          l.actionPerformed(new ActionEvent(this, 0, null));
        }
      }
      break;
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {

  }

  @Override
  public void mouseReleased(MouseEvent e) {

  }

  @Override
  public void mouseEntered(MouseEvent e) {

  }

  @Override
  public void mouseExited(MouseEvent e) {

  }

  public void addActionListener(final ActionListener l) {
    if (l != null) {
      actionListeners.add(l);
    }
  }

  public void removeActionListener(final ActionListener l) {
    actionListeners.remove(l);
  }

  public void setInkIndex(int colorIndex) {
    selectedInk = colorIndex & 0xF;
    repaint();
  }

  public void setPaintIndex(int colorIndex) {
    selectedPaint = colorIndex & 0xF;
    repaint();
  }

}
