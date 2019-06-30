package com.pelleplutt.mctexedit;

import java.io.*;
import java.nio.file.*;
import java.nio.file.FileSystem;
import java.util.*;

public class Assets {
  String path;
  FileSystem fs;
  
  public Assets(String path) {
    this.path = path;
  }
  
  void traverse(Path p, int d) throws IOException {
    if (Files.isDirectory(p)) {
      for (int k = 0; k < d; k++) System.out.print(". ");
      System.out.println(p.getFileName());
      Iterator<Path> i = Files.list(p).iterator();
      while (i.hasNext()) {
        traverse(i.next(), d+1);
      }
    } else {
      if (p.getFileName().toString().indexOf("png") > 0) {
        for (int k = 0; k < d; k++) System.out.print(". ");
        System.out.println(p.getFileName());
      }
    }
  }
  
  public void scrape() throws IOException {
    Path zipFilePath = Paths.get(path);
    fs = FileSystems.newFileSystem(zipFilePath, null);
    for (Path root : fs.getRootDirectories()) {
      traverse(root, 0);
    }
  }
}
