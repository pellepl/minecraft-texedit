package com.pelleplutt.mctexedit;

import java.awt.*;
import java.awt.datatransfer.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.imageio.*;
import javax.swing.*;

import com.pelleplutt.util.*;

public class UIPainter extends JPanel implements MouseListener, MouseMotionListener, MouseWheelListener {
  static final int USER_UNDEF = 0;
  static final int USER_MOVE = 10;
  static final int USER_PAINT = 20;
  static final int USER_PAINT_PEN = 21;
  static final int USER_PAINT_FILL = 22;
  static final int USER_PAINT_ERASE = 23;
  static final int USER_PICK = 30;
  
  final static Color colTrans1 = new Color(40,40,40,255);
  final static Color colTrans2 = new Color(45,45,45,255);
  final static Color colGrid = new Color(0,0,0);
  final static int ICON_W = 28;
  final static int ICON_H = 28;
  final static Image iconImageFill = AppSystem.loadImage("fill.png");
  final static Image iconImagePen = AppSystem.loadImage("pen.png");
  final static Image iconImageErase = AppSystem.loadImage("erase.png");
  final static Image iconImagePalette = AppSystem.loadImage("palette.png");
  final static Image iconImagePick = AppSystem.loadImage("colpick.png");
  final static Image iconImageThickness = AppSystem.loadImage("thickness.png");
  final static Image iconImageUndo = AppSystem.loadImage("undo.png");
  final static Image iconImageRedo = AppSystem.loadImage("redo.png");
  
  final static Cursor cursorPen = Toolkit.getDefaultToolkit().createCustomCursor(iconImagePen, new Point(0,ICON_H-1), "pen");
  final static Cursor cursorFill = Toolkit.getDefaultToolkit().createCustomCursor(iconImageFill, new Point(ICON_W-5,ICON_H-1), "fill");
  final static Cursor cursorErase = Toolkit.getDefaultToolkit().createCustomCursor(iconImageErase, new Point(ICON_W/3,ICON_H-1), "erase");
  final static Cursor cursorPick = Toolkit.getDefaultToolkit().createCustomCursor(iconImagePick, new Point(0,ICON_H-1), "pick");

  final static MultiplyComposite multiplyComposite = new MultiplyComposite();

  
  BufferedImage img = null;//new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
  Color penColor;
  Color eraseColor = new Color(0,0,0,0);
  Stroke stroke = new BasicStroke(1);
  int mag = 1;
  int toolState = USER_PAINT_PEN;
  int dragState = USER_PAINT;
  Point dragAnchor;
  Point dragPrev;
  JScrollPane scrl;
  PainterListener listener;
  Tools tools = new Tools();
  Object user;
  int offsX, offsY;
  
  List<BufferedImage> history = new ArrayList<BufferedImage>();
  int historyCursor = 0;
  
  public UIPainter() {
    selectColor(0xff000000);
    recalcSize();
    setTransferHandler(new ImageSelection());
  }
  
  public UIPainter(BufferedImage img) {
    this.img = img;
    selectColor(0xff000000);
    recalcSize();
    setTransferHandler(new ImageSelection());
  }
  
  static BufferedImage copy(BufferedImage bi) {
    if (bi == null) return null;
    BufferedImage c = new BufferedImage(bi.getWidth(), bi.getHeight(), BufferedImage.TYPE_INT_ARGB);
    c.getGraphics().drawImage(bi, 0, 0, null);
    return c;
  }
  
  JFrame owner;
  public void register(JFrame owner, JScrollPane scrl) {
    this.owner = owner;
    this.scrl = scrl;
  }

  public void paint(Graphics gg) {
    Graphics2D g = (Graphics2D) gg;
    int vpx = scrl.getHorizontalScrollBar().getValue();
    int vpy = scrl.getVerticalScrollBar().getValue();
    int vpw = scrl.getViewport().getWidth();
    int vph = scrl.getViewport().getHeight();
    if (img == null) {
      g.setColor(Color.darkGray);
      g.fillRect(vpx,vpy,vpw,vph);
      return;
    }
    int iw = img.getWidth() * mag;
    int ih = img.getHeight() * mag;
    g.setColor(Color.darkGray);
    g.fillRect(vpx,vpy,vpw,vph);
    
    offsX = 0;
    offsY = 0;
    if (iw < vpw) offsX = (vpw - iw) / 2;
    if (ih < vph) offsY = (vph - ih) / 2;
    
    {
      final int G = 16;
      int ox = vpx - (vpx % (2*G));
      int oy = vpy - (vpy % (2*G));
      boolean tr = false;
      int w = Math.min(iw, vpw+ox+2*G);
      int h = Math.min(ih, vph+oy+2*G);
      for (int y = oy; y < h; y += G) {
        boolean tc = tr;
        tr = !tr;
        int gh = y + G > ih ? ih-y : G;
        for (int x = ox; x < w; x += G) {
          int gw = x + G > iw ? iw-x : G;
          g.setColor(tc ? colTrans1 : colTrans2);
          g.fillRect(x + offsX, y + offsY, gw, gh);
          tc = !tc;
        }
      }
    }
    
    AffineTransform prevTransform = g.getTransform();
    AffineTransform t = (AffineTransform)prevTransform.clone();
    t.translate(offsX, offsY);
    t.scale(mag, mag);
    g.setTransform(t);
    g.drawImage(img, 0,0, this);
    g.setTransform(prevTransform);
    if (mag > 3) {
      int ox = vpx - (vpx % mag);
      int oy = vpy - (vpy % mag);
      g.setXORMode(colGrid);
      for (int y = oy; y < ih; y += mag) {
        g.drawLine(offsX,y+offsY,iw + offsX,y+offsY);
      }
      for (int x = ox; x < iw; x += mag) {
        g.drawLine(x+offsX,offsY,x+offsX,ih + offsY);
      }
      g.setPaintMode();
    }
  }

  public void setImage(Image newImg, int w, int h) {
    img = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    img.getGraphics().drawImage(newImg, 0, 0, null);
    history.clear();
    historyCursor = 0;
    updateHistoryState(false);
    recalcSize();
  }
  
  static int leastSideLog2(int w, int h) {
    int min = Math.min(w, h);
    int bit;
    for (bit = 16; bit > 1; bit--) {
      if ((min & (1<<bit)) != 0) {
        break;
      }
    }
    return (1<<bit);
  }
  
  public void pasteImage(Image newImg, int w, int h) {
    saveHistory();
    int sz = leastSideLog2(w,h);
    img = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
    Graphics2D g = (Graphics2D)img.getGraphics();
    g.scale((double)sz/(double)w, (double)sz/(double)h);
    g.drawImage(newImg, 0, 0, null);
    g.dispose();
    recalcSize();
    fireModifiedEvent();
  }
  
  public BufferedImage getImage() {
    return img;
  }
  
  public void setUserObject(Object u) {
    user = u;
  }

  void magResize(int newMag, Point pivot) {
    if (img == null) return;
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
    if (img == null) return;
    __d.width = (int)(mag * img.getWidth());
    __d.height = (int)(mag * img.getHeight());
    setMinimumSize(__d);
    setPreferredSize(__d);
    setSize(__d);
    invalidate();
    repaint();
  }
  
  void newToolState(int tool) {
    toolState = tool;
    updateMouseCursorToTool(tool);
  }
  
  void updateMouseCursorToTool(int tool) {
    switch (tool) {
    case USER_MOVE:
      setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      break;
    case USER_PAINT_PEN:
      setCursor(cursorPen);
      break;
    case USER_PAINT_FILL:
      setCursor(cursorFill);
      break;
    case USER_PAINT_ERASE:
      setCursor(cursorErase);
      break;
    case USER_PICK:
      setCursor(cursorPick);
      break;
    default:
      setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
      break;
    }

  }

  public void mouseClicked(MouseEvent e) {
  }

  public void mouseEntered(MouseEvent e) {
    updateMouseCursorToTool(toolState);
  }

  public void mouseExited(MouseEvent e) {
    updateMouseCursorToTool(USER_UNDEF);
  }
  
  public void undo() {
    if (historyCursor > 0) {
      if (historyCursor >= history.size()) {
        history.add(copy(img));
      }
      historyCursor--;
      Graphics2D g = (Graphics2D)img.getGraphics();
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
      g.fillRect(0,0,img.getWidth(), img.getHeight());
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
      g.drawImage(history.get(historyCursor), 0,0, null);
      g.dispose();
      fireModifiedEvent();
    }
    repaint();
    updateHistoryState(true);
  }
  
  public void redo() {
    if (historyCursor < history.size()-1) {
      historyCursor++;
      Graphics2D g = (Graphics2D)img.getGraphics();
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.CLEAR));
      g.fillRect(0,0,img.getWidth(), img.getHeight());
      g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));
      g.drawImage(history.get(historyCursor), 0,0, null);
      g.dispose();
      fireModifiedEvent();
    }
    repaint();
    updateHistoryState(true);
  }
  
  void updateHistoryState(boolean event) {
    tools.setUndoEnabled(historyCursor > 0);
    tools.setRedoEnabled(historyCursor < history.size()-1);
    if (event) fireHistoryEvent();
  }
  
  void saveHistory() {
    history.add(copy(img));
    historyCursor++;
    updateHistoryState(true);
  }

  int oldToolState; 
  public void mousePressed(MouseEvent e) {
    if (img == null) return;
    oldToolState = toolState;
    dragAnchor = (Point)e.getPoint().clone();
    dragPrev = dragAnchor;
    switch (e.getButton()) {
    case MouseEvent.BUTTON3:
      dragState = USER_MOVE;
      setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      break;
    case MouseEvent.BUTTON2:
      dragState = USER_PAINT;
      newToolState(USER_PICK);
      execute((Point)e.getPoint().clone(), (Point)e.getPoint().clone());
      break;
    case MouseEvent.BUTTON1:
      dragState = USER_PAINT;
      if (toolState == USER_PAINT_PEN || toolState == USER_PAINT_ERASE || toolState == USER_PAINT_FILL) {
        while (history.size() > historyCursor) {
          history.remove(historyCursor);
        }
        saveHistory();
      }
      execute((Point)e.getPoint().clone(), (Point)e.getPoint().clone());
    }
  }

  public void mouseReleased(MouseEvent e) {
    dragState = USER_UNDEF;
    newToolState(oldToolState);
    updateMouseCursorToTool(toolState);
  }
  
  public void mouseWheelMoved(MouseWheelEvent e) {
    Point pivot = e.getPoint();
    if (e.getWheelRotation() > 0) {
      if (mag > 1) magResize(mag - 1, pivot);
    } else {
      if (mag < 32) magResize(mag + 1, pivot);
    }
  }
  
  void execute(Point from, Point to) {
    if (img == null) return;
    int ox = scrl.getHorizontalScrollBar().getValue()-offsX;
    int oy = scrl.getVerticalScrollBar().getValue()-offsY;
    from.translate(ox, oy);
    to.translate(ox, oy);
    Graphics2D g = (Graphics2D)img.getGraphics();
    g.setStroke(stroke);
    if (toolState == USER_PAINT_PEN) {
      g.setColor(penColor);
      g.drawLine((int)(from.x/mag), (int)(from.y/mag), (int)(to.x/mag), (int)(to.y/mag));
      fireModifiedEvent();
    } else if (toolState == USER_PAINT_ERASE) {
      g.setColor(eraseColor);
      g.setComposite(multiplyComposite);
      g.drawLine((int)(from.x/mag), (int)(from.y/mag), (int)(to.x/mag), (int)(to.y/mag));
      fireModifiedEvent();
    } else if (toolState == USER_PICK) {
      if (to.x/mag >= 0 && to.y/mag >= 0 && to.x/mag < img.getWidth() && to.y/mag < img.getHeight()) {
        selectColor(img.getRGB(to.x/mag, to.y/mag));
      }
    } else if (toolState == USER_PAINT_FILL) {
      int x = (int)(to.x/mag);
      int y = (int)(to.y/mag);
      x = Math.max(0, Math.min(img.getWidth()-1, x));
      y = Math.max(0, Math.min(img.getHeight()-1, y));
      int c = img.getRGB(x, y);
      int nc = penColor.getRGB() | 0xff000000;
      if (c == nc) return;
      List<Point> visits = new ArrayList<Point>();
      visits.add(new Point(x,y));
      while (!visits.isEmpty()) {
        Point p = visits.remove(visits.size()-1);
        if (p.x < 0 || p.y < 0 || p.x >= img.getWidth() || p.y >= img.getHeight()) continue;
        int curC = img.getRGB(p.x, p.y);
        if (curC != c) continue;
        img.setRGB(p.x, p.y, nc | 0xff000000);
        visits.add(new Point(p.x-1,p.y));
        visits.add(new Point(p.x+1,p.y));
        visits.add(new Point(p.x,p.y-1));
        visits.add(new Point(p.x,p.y+1));
      }
      fireModifiedEvent();
    }
    g.dispose();
    repaint();
  }

  public void mouseDragged(MouseEvent e) {
    switch (dragState) {
    case USER_MOVE: {
      int dx = e.getPoint().x - dragPrev.x;
      int dy = e.getPoint().y - dragPrev.y;
      scrl.getHorizontalScrollBar().setValue(scrl.getHorizontalScrollBar().getValue() - dx);
      scrl.getVerticalScrollBar().setValue(scrl.getVerticalScrollBar().getValue() - dy);
      break;
    }
    case USER_PAINT: {
      execute(dragPrev, e.getPoint());
      break;
    }
    }
    dragPrev = (Point)e.getPoint().clone();
  }

  public void mouseMoved(MouseEvent e) {
  }
  
  public Tools getToolsPanel() {
    return tools;
  }
  
  public void setListener(PainterListener l) {
    this.listener = l;
  }
  
  void fireModifiedEvent() {
    if (listener != null) {
      listener.modified(user, img);
    }
  }

  void fireHistoryEvent() {
    if (listener != null) {
      listener.history(user, img, history.size(), historyCursor);
    }
  }
  
  void selectColor(int color) {
    tools.setColor(color);
    tools.repaint();
    penColor = new Color(color);
  }
  
  void setThickness(int thickness) {
    stroke = new BasicStroke(thickness);
  }
  
  public class Tools extends JPanel {
    JButton buts[] = {
        new JButton(new AbstractAction("", new ImageIcon(iconImagePen)) {
          public void actionPerformed(ActionEvent e) {
            toolState = USER_PAINT_PEN;
            updateMouseCursorToTool(toolState);
          }
        }),
        new JButton(new AbstractAction("", new ImageIcon(iconImageErase)) {
          public void actionPerformed(ActionEvent e) {
            toolState = USER_PAINT_ERASE;
            updateMouseCursorToTool(toolState);
          }
        }),
        new JButton(new AbstractAction("", new ImageIcon(iconImageFill)) {
          public void actionPerformed(ActionEvent e) {
            toolState = USER_PAINT_FILL;
            updateMouseCursorToTool(toolState);
          }
        }),
        new JButton(new AbstractAction("", new ImageIcon(iconImagePick)) {
          public void actionPerformed(ActionEvent e) {
            toolState = USER_PICK;
            updateMouseCursorToTool(toolState);
          }
        }),
        new JButton(new AbstractAction("", new ImageIcon(iconImageThickness)) {
          public void actionPerformed(ActionEvent e) {
            String choices[] = {"1","2","3","4","5","6","7","8","9","10","11","12","13","14","15","16"};
            MouseEvent me = new MouseEvent(Tools.this, 0, 0, 0,   28, 28*4,   0, true);
            UIUtil.showPopupMenu(me, choices, new ActionListener() {
              public void actionPerformed(ActionEvent ae) {
                try {
                  setThickness(Integer.parseInt(ae.getActionCommand()));
                } catch (Throwable t) {}
              }
            });
            
          }
        }),
        new JButton(new AbstractAction("", new ImageIcon(iconImagePalette)) {
          public void actionPerformed(ActionEvent e) {
            if (chooser == null) {
              chooser = UIColorChooser.choose(owner, penColor, UIPainter.this);
              chooser.addWindowListener(new WindowAdapter() {
                public void windowClosed(WindowEvent e) {
                  chooser = null;
                }
              });
            } else {
              chooser.setVisible(true);
              chooser.requestFocus();
            }
          }
        }),
        new JButton(new AbstractAction("", new ImageIcon(iconImageUndo)) {
          public void actionPerformed(ActionEvent e) {
            undo();
          }
        }),
        new JButton(new AbstractAction("", new ImageIcon(iconImageRedo)) {
          public void actionPerformed(ActionEvent e) {
            redo();
          }
        }),
    };
    volatile JDialog chooser;
    public void setUndoEnabled(boolean e) {
      buts[6].setEnabled(e);
    }
    public void setRedoEnabled(boolean e) {
      buts[7].setEnabled(e);
    }
    Tools() {
      setLayout(new BoxLayout(this,BoxLayout.Y_AXIS));
      for (JButton b : buts) {
        decorateButton(b);
        add(b);
      }
      setUndoEnabled(false);
      setRedoEnabled(false);
    }
    
    public void setColor(int color) {
      color |= 0xff000000;
      buts[5].setBorder(BorderFactory.createLineBorder(new Color(color), 4, true));
    }
    
    void decorateButton(JButton b) {
      b.setBackground(Color.white);
      b.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }
  }
  
  public static class MultiplyComposite implements Composite, CompositeContext {
    protected void checkRaster(Raster r) {
      if (r.getSampleModel().getDataType() != DataBuffer.TYPE_INT) {
        throw new IllegalStateException("Expected integer sample type");
      }
    }

    @Override
    public void compose(Raster src, Raster dstIn, WritableRaster dstOut) {
      checkRaster(src);
      checkRaster(dstIn);
      checkRaster(dstOut);

      int width = Math.min(src.getWidth(), dstIn.getWidth());
      int height = Math.min(src.getHeight(), dstIn.getHeight());
      int x, y;
      int[] srcPixels = new int[width];
      int[] dstPixels = new int[width];

      for (y = 0; y < height; y++) {
        src.getDataElements(0, y, width, 1, srcPixels);
        dstIn.getDataElements(0, y, width, 1, dstPixels);

        for (x = 0; x < width; x++) {
          dstPixels[x] = srcPixels[x];
        }

        dstOut.setDataElements(0, y, width, 1, dstPixels);
      }
    }

    @Override
    public CompositeContext createContext(ColorModel srcColorModel, ColorModel dstColorModel, RenderingHints hints) {
      return this;
    }

    @Override
    public void dispose() {
    }
  }
  
  interface PainterListener {
    public void modified(Object user, Image img);
    public void history(Object user, Image img, int historyLength, int historyPos);
  }



  // Stolen from
  // http://www.java2s.com/Tutorial/Java/0240__Swing/DragandDropSupportforImages.htm
  class ImageSelection extends TransferHandler implements Transferable {

    private final DataFlavor flavors[] = { DataFlavor.imageFlavor, DataFlavor.allHtmlFlavor };

    public int getSourceActions(JComponent c) {
      return TransferHandler.COPY;
    }

    public boolean canImport(JComponent comp, DataFlavor flavor[]) {
      for (int i = 0, n = flavor.length; i < n; i++) {
        for (int j = 0, m = flavors.length; j < m; j++) {
          if (flavor[i].equals(flavors[j])) {
            return true;
          }
        }
      }
      return false;
    }

    public Transferable createTransferable(JComponent comp) {
      return null;
    }

    public boolean importData(JComponent comp, Transferable t) {
      DataFlavor f[] = t.getTransferDataFlavors();
      // mimetype=application/octet-stream;representationclass=java.io.InputStream

      for (DataFlavor ff : f) {
        if (!ff.isRepresentationClassInputStream())
          continue;
        if (!ff.getMimeType().equals("application/octet-stream; class=java.io.InputStream"))
          continue;
        try {
          Image img = ImageIO.read((InputStream) t.getTransferData(ff));
          pasteImage(img, img.getWidth(null), img.getHeight(null));
          return true;
        } catch (UnsupportedFlavorException | IOException e) {
          e.printStackTrace();
        }
      }
      return false;
    }

    // Transferable
    public Object getTransferData(DataFlavor flavor) {
      if (isDataFlavorSupported(flavor)) {
        return null; // image
      }
      return null;
    }

    public DataFlavor[] getTransferDataFlavors() {
      return flavors;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
      return flavors[0].equals(flavor);
    }
  }

}
