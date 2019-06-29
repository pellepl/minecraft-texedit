package com.pelleplutt.mctexedit;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.image.BufferedImage;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class UIColorChooser extends JPanel {
  static BufferedImage imgPalette = new BufferedImage(256,256,BufferedImage.TYPE_4BYTE_ABGR);  
  static {
    int w = imgPalette.getWidth();
    int h = imgPalette.getHeight();
    Graphics2D g = (Graphics2D)imgPalette.getGraphics();
    int cols[] = {0x000000,0x0000ff, 0x00ffff, 0x00ff00, 0xffff00, 0xff0000, 0xff00ff, 0xffffff};
    final float pixPerCol = (float)w/(float)(cols.length-1);
    int rowCol[] = new int[w];
    for (int x = 0; x < w; x++) {
      int srcColIx = (int)(x  / pixPerCol) % cols.length;
      int dstColIx = (srcColIx + 1) % cols.length;
      float f = (float)(x - srcColIx * pixPerCol) / pixPerCol;
      int srcCol = cols[srcColIx];
      int dstCol = cols[dstColIx];
      int col = lerpRGB(srcCol, dstCol, f);
      rowCol[x] = col;
    }
    
    for (int y = 0; y < h; y++) {
    float l = 2f*(float)y/(float)h;
    for (int x = 0; x < w; x++) {
      g.setColor(new Color(liteRGB(rowCol[x], l)));
      g.drawLine(x, y, x, y);
    }
    }
    g.dispose();
  }
  
  static int liteRGB(int col, float l) {
    int r = (col & 0xff0000) >> 16;
    int g = (col & 0x00ff00) >>  8;
    int b = (col & 0x0000ff);
    if (l < 1f) {
      r = clamp((int)(r * l), 0, 0xff);
      g = clamp((int)(g * l), 0, 0xff);
      b = clamp((int)(b * l), 0, 0xff);
    } else if (l > 1f) {
      int a = (int)(0xff*(l-1f));
      r = clamp(r+a, 0, 0xff);
      g = clamp(g+a, 0, 0xff);
      b = clamp(b+a, 0, 0xff);
    }
    return (r<<16)|(g<<8)|b;
  }
  
  static int lerpRGB(int src, int dst, float f) {
    int rs = (src & 0xff0000) >> 16;
    int rd = (dst & 0xff0000) >> 16;
    int gs = (src & 0x00ff00) >>  8;
    int gd = (dst & 0x00ff00) >>  8;
    int bs = (src & 0x0000ff);
    int bd = (dst & 0x0000ff);
    int r = clamp(lerp(rs,rd,f), 0, 0xff);
    int g = clamp(lerp(gs,gd,f), 0, 0xff);
    int b = clamp(lerp(bs,bd,f), 0, 0xff);
    return (r<<16)|(g<<8)|b;
  }
  
  static int clamp(int x, int min, int max) {
    return Math.max(min, Math.min(max, x));
  }
  
  static int lerp(int src, int dst, float f) {
    return Math.round(src+f*(dst-src));
  }
  
  public static Color choose(Frame owner, Color def, UIPainter painter) {
    UIColorChooser ui = new UIColorChooser(def, painter);
    JDialog dialog = new JDialog(owner);
    dialog.setModalExclusionType(Dialog.ModalExclusionType.TOOLKIT_EXCLUDE);
    dialog.setResizable(false);
    dialog.add(ui);
    dialog.pack();
    dialog.setVisible(true);
    return null;
  }
  
  JLabel colLabel = new JLabel(new ImageIcon(imgPalette));
  JSlider colRSlide = new JSlider(SwingConstants.HORIZONTAL, 0,255,0);
  JSlider colGSlide = new JSlider(SwingConstants.HORIZONTAL, 0,255,0);
  JSlider colBSlide = new JSlider(SwingConstants.HORIZONTAL, 0,255,0);
  JTextField colText = new JTextField();
  UIPainter painter;
  
  void updateColor(int color) {
    painter.selectColor(color);
    colRSlide.setValue((color >> 16) & 0xff);
    colGSlide.setValue((color >> 8) & 0xff);
    colBSlide.setValue((color) & 0xff);
    colText.setText(String.format("%02x%02x%02x", (color>>16)&0xff, (color>>8)&0xff, color&0xff));
  }
  
  void selectFromImage(Point p) {
    p.x = clamp(p.x, 0, imgPalette.getWidth()-1);
    p.y = clamp(p.y, 0, imgPalette.getHeight()-1);
    updateColor(imgPalette.getRGB(p.x, p.y));
  }
  
  UIColorChooser(Color def, UIPainter painter) {
    this.painter = painter;
    colLabel.addMouseListener(new MouseListener() {
      public void mousePressed(MouseEvent e) {
        selectFromImage(e.getPoint());
      }
      public void mouseReleased(MouseEvent e) {}
      public void mouseExited(MouseEvent e) {}
      public void mouseEntered(MouseEvent e) {}
      public void mouseClicked(MouseEvent e) {}
    });
    colLabel.addMouseMotionListener(new MouseMotionListener() {
      public void mouseDragged(MouseEvent e) {
        selectFromImage(e.getPoint());
      }
      public void mouseMoved(MouseEvent e) {}
    });
    colLabel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

    setLayout(new BorderLayout());
    add(colLabel, BorderLayout.CENTER);
    JPanel settings = new JPanel();
    settings.setLayout(new BoxLayout(settings, BoxLayout.Y_AXIS));
    settings.add(colRSlide);
    settings.add(colGSlide);
    settings.add(colBSlide);
    settings.add(colText);
    
    updateColor(def.getRGB());
    
    listenToSlider(colRSlide);
    listenToSlider(colGSlide);
    listenToSlider(colBSlide);
    
    colText.getDocument().addDocumentListener(new DocumentListener() {
      void update() {
        String t = colText.getText();
        try {
          int col = Integer.parseInt(t, 16);
          updateColor(col);
        } catch (Throwable ignore) {}
      }
      public void removeUpdate(DocumentEvent e) {
        update();
      }
      public void insertUpdate(DocumentEvent e) {
        update();
      }
      public void changedUpdate(DocumentEvent e) {
        update();
      }
    });
    
    add(settings, BorderLayout.SOUTH);
  }
  
  void listenToSlider(JSlider s) {
    s.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        updateColor((colRSlide.getValue() << 16) | (colGSlide.getValue() << 8) | colBSlide.getValue());
      }
    });
  }
}
