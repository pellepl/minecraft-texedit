package com.pelleplutt.mctexedit;

import java.awt.*;
import java.awt.image.*;
import java.io.*;
import java.util.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.pelleplutt.mctexedit.Asset.*;
import com.pelleplutt.mctexedit.UIPainter.*;
import com.pelleplutt.util.*;

public class Bootstrap {
  public static void main(String[] args) {
    UILog uilog = new UILog();
    Log.out = uilog.getPrintStream();
    for (String arg : args) {
      if (arg.equals("--dbgstdout")) {
        Log.out = System.out;
      }
    }
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Throwable ignore) {
    }

    Assets assets = null; // new Assets("/home/petera/proj/minecraft-texedit/abbe-resource.zip");
    Asset asset = null;
    try {
      // asset = assets.scrape();
    } catch (Throwable e) {
      Log.printStackTrace(e);
    }

    JFrame frame = new JFrame();
    Container cp = frame.getContentPane();
    JPanel editPane = new JPanel();
    UIPainter painter = new UIPainter();
    JScrollPane scrollPane = new JScrollPane(painter, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
        JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    painter.register(frame, scrollPane);
    scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // fix artefacts
    scrollPane.setWheelScrollingEnabled(false);
    editPane.setLayout(new BorderLayout());
    editPane.add(scrollPane, BorderLayout.CENTER);
    editPane.add(painter.getToolsPanel(), BorderLayout.WEST);

    JScrollPane log = new JScrollPane(uilog);
    uilog.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
    uilog.setBackground(new Color(12, 12, 12));
    uilog.setForeground(new Color(96, 255, 96));
    UIAssetTree tree = new UIAssetTree(frame, assets, asset, log);
    tree.build();
    tree.setPreferredSize(new Dimension(300, 300));
    tree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent e) {
        Object userObject = ((DefaultMutableTreeNode) e.getPath().getLastPathComponent()).getUserObject();
        if (userObject == null || !(userObject instanceof Asset))
          return;
        Asset asset = (Asset) userObject;
        if (asset instanceof AssetPNG) {
          BufferedImage b = ((AssetPNG) asset).getPNG();
          painter.setImage(b, b.getWidth(), b.getHeight());
          ((AssetPNG) asset).setPNG(painter.getImage());
          painter.setUserObject(asset);
        } else if (asset instanceof AssetPack) {

        }
      }
    });
    tree.setUserCallback(new UIAssetTree.UserCallback() {
      @Override
      public void importImage(Image i) {
        painter.replaceImage(i, i.getWidth(null), i.getHeight(null));
      }
    });
    painter.setListener(new PainterListener() {
      public void modified(Object user, Image img) {
        if (user != null) {
          ((Asset) user).setModified(true);
          if (user instanceof AssetPNG) {
            ((AssetPNG) user).setPNG(painter.getImage());
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
    log.setMaximumSize(new Dimension(100, 200));
    log.setPreferredSize(new Dimension(100, 200));
    log.setSize(new Dimension(100, 200));
    log.setVisible(false);
    cp.add(log, BorderLayout.SOUTH);

    scrollPane.addMouseListener(painter);
    scrollPane.addMouseWheelListener(painter);
    scrollPane.addMouseMotionListener(painter);
    frame.setLocation(100, 100);
    frame.setSize(700, 400);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setVisible(true);
    frame.setTitle("Supadupaawesome mc-pack-texedit v1.4");
    loadSettings();
    Log.println("Bootstrap done");
    /// tree.load(new
    /// File("/home/petera/proj/minecraft-texedit/abbe-resource.zip"));
  }

  public static final File settingsPath = new File(new File(System.getProperty("user.home"), ".mctexedit"),
      "settings.txt");
  public static final Properties settings = new Properties();

  public static void loadSettings() {
    if (settingsPath.exists()) {
      FileReader fr;
      try {
        fr = new FileReader(settingsPath);
        settings.load(fr);
        fr.close();
        Log.println("loaded settings");
        Log.println(settings.toString());
      } catch (Throwable e) {
        Log.println("failed loading settings " + e.getMessage());
        Log.printStackTrace(e);
      }
    }
    System.setProperty(UIUtil.PROP_DEFUALT_PATH, 
      settings.getProperty(UIUtil.PROP_DEFUALT_PATH,
        System.getProperty("user.home")));
  }

  public static void storeSettings() {
    settings.setProperty(UIUtil.PROP_DEFUALT_PATH,
      System.getProperty(UIUtil.PROP_DEFUALT_PATH, 
        System.getProperty("user.home")));
    Log.println("storing settings");
    Log.println(settings.toString());
    try {
      if (settingsPath.exists()) {
        settingsPath.delete();
      }
      settingsPath.getParentFile().mkdirs();
      settingsPath.createNewFile();
      FileWriter fw;
      fw = new FileWriter(settingsPath);
      settings.store(fw, null);
      fw.close();
    } catch (Throwable e) {
      Log.println("failed storing settings " + e.getMessage());
      Log.printStackTrace(e);
    }
  }
}
