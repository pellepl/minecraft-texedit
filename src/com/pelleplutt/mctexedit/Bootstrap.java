package com.pelleplutt.mctexedit;

import java.awt.*;

import javax.swing.*;

public class Bootstrap {
  public static void main(String[] args) {
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    } catch (Throwable ignore) {}

    JFrame frame = new JFrame();
    Container cp = frame.getContentPane();
    UIPainter painter = new UIPainter();
    JScrollPane scrollPane = new JScrollPane(painter, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
    painter.registerScroller(scrollPane);
    scrollPane.getViewport().setScrollMode(JViewport.SIMPLE_SCROLL_MODE); // fix artefacts
    scrollPane.setWheelScrollingEnabled(false);
    cp.setLayout(new BorderLayout());
    cp.add(scrollPane, BorderLayout.CENTER);
    cp.add(painter.getToolsPanel(), BorderLayout.WEST);
    
    scrollPane.addMouseListener(painter);
    scrollPane.addMouseWheelListener(painter);
    scrollPane.addMouseMotionListener(painter);
    frame.setLocation(100, 100);
    frame.setSize(700, 400);
    frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
    frame.setVisible(true);
  }
}
