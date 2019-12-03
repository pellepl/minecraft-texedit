package com.pelleplutt.mctexedit;

import java.awt.image.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

import javax.imageio.*;
import javax.swing.*;

import com.pelleplutt.util.*;

public class Asset {
  Path assetPath;
  String name;
  List<Asset> children = new ArrayList<Asset>();
  Asset parent;
  boolean modified;
  
  Asset(Path path) {
    this.assetPath = path;
    this.name = path.getFileName() == null ? "<N/A>" : path.getFileName().toString();
  }
  
  void load() {
  }
  
  void save() {
  }
  
  void loadAll() {
    load();
    for (Asset c : children) {
      c.loadAll();
    }
  }
  
  void addChild(Asset a) {
    a.parent = this;
    children.add(a);
  }
  
  List<Asset> getChildren() {
    return children;
  }
  
  public String getName() {
    return name;
  }
  
  public ImageIcon getIcon() {
    return null;
  }
  
  public boolean isModified() {
    return modified;
  }
  
  public void setModified(boolean m) {
    if (modified == m) return;
    modified = m;
    updateParentModified(parent);
  }
  
  void updateParentModified(Asset a) {
    if (a == null) return;
    boolean hasModifiedChildren = false;
    for (Asset child : a.children) {
      if (child.modified) {
        hasModifiedChildren = true;
        break;
      }
    }
    a.modified = hasModifiedChildren;
    updateParentModified(a.parent);
  }

  public static class AssetDir extends Asset {
    AssetDir(Path path) {
      super(path);
    }
  }
  
  public static class AssetPNG extends Asset {
    BufferedImage png;
    ImageIcon thumb;
    AssetPNG(Path path) {
      super(path);
    }
    
    void load() {
      Log.println("loading " + assetPath);
      try (InputStream in = Files.newInputStream(assetPath)) {
        png = ImageIO.read(in);
        thumb = new ImageIcon(png.getScaledInstance(24, 24, BufferedImage.SCALE_SMOOTH));
      } catch (Throwable t) {
        Log.println(name);
        Log.printStackTrace(t);
      }
    }
    
    void save() {
      Log.println("storing " + getName());
      try {
        Files.delete(assetPath);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(png, "png", baos);
        Files.write(assetPath, baos.toByteArray());
        thumb = new ImageIcon(png.getScaledInstance(24, 24, BufferedImage.SCALE_SMOOTH));
        setModified(false);
      } catch (Throwable t) {
        Log.printStackTrace(t);
      }
    }
    
    public ImageIcon getIcon() {
      return thumb;
    }

    public BufferedImage getPNG() {
      return png;
    }

    public void setPNG(BufferedImage image) {
      png = image;
    }
  }
  
  public static class AssetPack extends Asset {
    AssetPack(Path path) {
      super(path);
    }
    
    void load() {
      try {
        List<String> lines = Files.readAllLines(assetPath);
        lines.forEach(line -> System.out.println(line));
        for (String l : lines) System.out.println(l);
      } catch (Throwable t) {
        Log.printStackTrace(t);
      }
    }

  }

}
