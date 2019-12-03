package com.pelleplutt.mctexedit;

import java.io.*;

import com.pelleplutt.util.*;

public class UILog extends FastTextPane {
  StringBuilder buf = new StringBuilder();
  boolean ignoreNewLine;
  PrintStream ps = new PrintStream(new OutputStream() {
    @Override
    public void write(int s) throws IOException {
      char c = (char)s;
      buf.append(c);
      if (c == '\n' && !ignoreNewLine) {
        UILog.this.addText(buf.toString());
        buf = new StringBuilder();
      }
      if (c == '\r') {
        UILog.this.addText(buf.toString());
        buf = new StringBuilder();
        ignoreNewLine = true;
      } else {
        ignoreNewLine = false;
      }
    }});
  public PrintStream getPrintStream() {
    return ps;
  }
}
