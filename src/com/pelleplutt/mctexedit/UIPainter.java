package com.pelleplutt.mctexedit;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;

import javax.swing.*;

public class UIPainter extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
  static final int DRAG_UNDEF = 0;
  static final int DRAG_MOVE = 1;
  static final int DRAG_PAINT = 2;
  
  BufferedImage img = new BufferedImage(500, 500, BufferedImage.TYPE_INT_ARGB);
  double mag = 1;
  int dragState = DRAG_UNDEF;
  Point dragAnchor;
  Point dragPrev;
  JScrollPane scrl;
  
  public UIPainter() {
    Graphics g = img.getGraphics();
    g.setColor(Color.black);
    g.fillRect(0, 0, img.getWidth(), img.getHeight());
    g.setColor(Color.red);
    g.drawLine(0, 0, img.getWidth(), img.getHeight());
    g.drawLine(img.getWidth(), 0, 0, img.getHeight());
    g.dispose();
  }
  
  public void registerScroller(JScrollPane scrl) {
    this.scrl = scrl;
  }

  public void paint(Graphics gg) {
    Graphics2D g = (Graphics2D) gg;
    AffineTransform prevTransform = g.getTransform();
    AffineTransform t = (AffineTransform)prevTransform.clone();
    t.scale(mag, mag);
    g.setTransform(t);
    g.drawImage(img, 0, 0, this);
    g.setTransform(prevTransform);
  }

  public void setImage(BufferedImage img) {
    this.img = img;
    recalcSize();
  }

  void magResize(double newMag, Point pivot) {
    double offsH = (double) scrl.getHorizontalScrollBar().getValue() / (double)mag;
    double pivotH = offsH + (double) pivot.getX() / (double)mag;
    double nrangeH = (double) img.getWidth() / newMag;
    double portionH = (pivotH - offsH) / (double)(img.getWidth() / mag);
    double noffsH = pivotH - nrangeH * portionH;

    double offsV = (double) scrl.getVerticalScrollBar().getValue() / mag;
    double pivotV = offsV + (double) pivot.getY() / (double)mag;
    double nrangeV = (double) img.getHeight() / newMag;
    double portionV = (pivotV - offsV) / (double)(img.getHeight() / mag);
    double noffsV = pivotV - nrangeV * portionV;

    mag = newMag;
    recalcSize();

    scrl.getHorizontalScrollBar().setValue((int)Math.round(noffsH * newMag));
    scrl.getVerticalScrollBar().setValue((int)Math.round(noffsV * newMag));
  }
  
  
  private Dimension __d = new Dimension();
  void recalcSize() {
    __d.width = (int)(mag * img.getWidth());
    __d.height = (int)(mag * img.getHeight());
    setMinimumSize(__d);
    setPreferredSize(__d);
    setSize(__d);
    invalidate();
    repaint();
  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
  }

  public void mouseExited(MouseEvent e) {
  }

  public void mousePressed(MouseEvent e) {
    dragAnchor = (Point)e.getPoint().clone();
    dragPrev = dragAnchor;
    switch (e.getButton()) {
    case MouseEvent.BUTTON3:
      dragState = DRAG_MOVE;
      break;
    case MouseEvent.BUTTON1:
      dragState = DRAG_PAINT;
      paint((Point)e.getPoint().clone(), (Point)e.getPoint().clone());
    }
  }

  public void mouseReleased(MouseEvent e) {
    dragState = DRAG_UNDEF;
  }

  public void mouseWheelMoved(MouseWheelEvent e) {
    Point pivot = e.getPoint();
    if (e.getWheelRotation() > 0) {
      if (mag > 1) magResize(mag - 0.5, pivot);
    } else {
      if (mag < 32) magResize(mag + 0.5, pivot);
    }
  }
  
  void paint(Point from, Point to) {
    int ox = scrl.getHorizontalScrollBar().getValue();
    int oy = scrl.getVerticalScrollBar().getValue();
    from.translate(ox, oy);
    to.translate(ox, oy);
    Graphics g = img.getGraphics();
    g.setColor(Color.blue);
    g.drawLine((int)(from.x/mag), (int)(from.y/mag), (int)(to.x/mag), (int)(to.y/mag));
    g.dispose();
    repaint();
  }

  public void mouseDragged(MouseEvent e) {
    switch (dragState) {
    case DRAG_MOVE: {
      int dx = e.getPoint().x - dragPrev.x;
      int dy = e.getPoint().y - dragPrev.y;
      scrl.getHorizontalScrollBar().setValue(scrl.getHorizontalScrollBar().getValue() - dx);
      scrl.getVerticalScrollBar().setValue(scrl.getVerticalScrollBar().getValue() - dy);
      break;
    }
    case DRAG_PAINT: {
      paint(dragPrev, e.getPoint());
    }
    }
    dragPrev = (Point)e.getPoint().clone();
  }

  public void mouseMoved(MouseEvent e) {
  }

}
