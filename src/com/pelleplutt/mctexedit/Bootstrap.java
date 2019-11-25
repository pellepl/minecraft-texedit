package com.pelleplutt.mctexedit;

import java.awt.*;
import java.awt.image.*;
import java.io.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.pelleplutt.mctexedit.Asset.*;
import com.pelleplutt.mctexedit.UIPainter.*;

public class Bootstrap {
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Throwable ignore) {}
    
    Assets assets = null; //new Assets("/home/petera/proj/minecraft-texedit/abbe-resource.zip");
    Asset asset = null;
    try {
      //asset = assets.scrape();
    } catch (Throwable e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }

    JFrame frame = new JFrame();
    Container cp = frame.getContentPane();
    JPanel editPane = new JPanel();
    UIPainter painter = new UIPainter();
    JScrollPane scrollPane = new JScrollPane(painter, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    painter.register(frame, scrollPane);
    scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // fix artefacts
    scrollPane.setWheelScrollingEnabled(false);
    editPane.setLayout(new BorderLayout());
    editPane.add(scrollPane, BorderLayout.CENTER);
    editPane.add(painter.getToolsPanel(), BorderLayout.WEST);
    
    UIAssetTree tree = new UIAssetTree(frame, assets, asset);
    tree.build();
    tree.setPreferredSize(new Dimension(300,300));
    tree.addTreeSelectionListener( new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        Object userObject = ((DefaultMutableTreeNode)e.getPath().getLastPathComponent()).getUserObject();
        if (userObject == null || !(userObject instanceof Asset)) return;
        Asset asset = (Asset)userObject;
        if (asset instanceof AssetPNG) {
          BufferedImage b = ((AssetPNG)asset).getPNG();
          painter.setImage(b, b.getWidth(), b.getHeight());
          ((AssetPNG)asset).setPNG(painter.getImage());
          painter.setUserObject(asset);
        } else if (asset instanceof AssetPack) {
          
        }
      }
    });
    painter.setListener(new PainterListener() {
      public void modified(Object user, Image img) {
        if (user != null) {
          ((Asset)user).setModified(true);
          if (user instanceof AssetPNG) {
            ((AssetPNG)user).setPNG(painter.getImage());
          }
          tree.repaint();
        }
      }

      public void history(Object user, Image img, int historyLength, int historyPos) {
      }
    });
    
    cp.setLayout(new BorderLayout());
    cp.add(editPane, BorderLayout.CENTER);
    cp.add(tree, BorderLayout.WEST);
    
    
    scrollPane.addMouseListener(painter);
    scrollPane.addMouseWheelListener(painter);
    scrollPane.addMouseMotionListener(painter);
    frame.setLocation(100, 100);
    frame.setSize(700, 400);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setVisible(true);
    tree.load(new File("/home/petera/proj/minecraft-texedit/abbe-resource.zip"));
    
  }
}
