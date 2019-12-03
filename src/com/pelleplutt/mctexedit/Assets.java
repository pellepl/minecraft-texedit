package com.pelleplutt.mctexedit;

import java.io.*;
import java.net.*;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.util.*;

import com.pelleplutt.mctexedit.Asset.*;
import com.pelleplutt.util.*;

public class Assets {
  String path;
  FileSystem fs;
  Asset root;
  
  public Assets(String path) {
    this.path = path;
  }
  
  void traverseRead(Path path, Asset parent) throws IOException {
    if (Files.isDirectory(path)) {
      Asset dir;
      if (path.getFileName() == null) {
        dir = parent;
      } else {
        dir = new AssetDir(path);
        parent.addChild(dir);
      }
      Iterator<Path> i = Files.list(path).sorted().iterator();
      while (i.hasNext()) {
        traverseRead(i.next(), dir);
      }
    } else {
      if (path.getFileName().toString().endsWith(".png")) {
        Asset png = new AssetPNG(path);
        parent.addChild(png);
      } else if (path.getFileName().toString().toLowerCase().equals("pack.mcmeta")) {
        parent.addChild(new AssetPack(path));
      }
    }
  }
  
  public Asset scrape() throws IOException, URISyntaxException {
    Log.println("Scraping " + path);
    Path zipFilePath = Paths.get(path);
    final URI uri = URI.create("jar:file:" + zipFilePath.toUri().getPath());
    final Map<String, String> env = new HashMap<>();
    env.put("create", "true");
    fs = FileSystems.newFileSystem(uri, env);

    Asset newRoot = new Asset(zipFilePath);
    for (Path rootDir : fs.getRootDirectories()) {
      traverseRead(rootDir, newRoot);
    }
    Log.println("Scraping done, loading all");
    newRoot.loadAll();
    this.root = newRoot;
    return newRoot;
  }
  
  public void sync() throws IOException {
    Log.println("syncing");
    fs.close();
    Path zipFilePath = Paths.get(path);
    final URI uri = URI.create("jar:file:" + zipFilePath.toUri().getPath());
    final Map<String, String> env = new HashMap<>();
    env.put("create", "true");
    fs = FileSystems.newFileSystem(uri, env);
    traverseRefresh(root);
  }
  
  void traverseRefresh(Asset a) {
    a.assetPath = fs.getPath(a.assetPath.toString());
    for (Asset c : a.getChildren()) {
      traverseRefresh(c);
    }
  }
}

