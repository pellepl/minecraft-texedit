package com.pelleplutt.mctexedit;

import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.util.*;
import java.util.List;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.tree.*;

import com.pelleplutt.mctexedit.Asset.*;
import com.pelleplutt.util.*;

public class UIAssetTree extends JPanel {
  Assets assets;
  Asset asset;
  JTree tree = new JTree();
  JTextField filter = new JTextField();
  InvisibleNode root;
  JFrame owner;

  public UIAssetTree(JFrame owner, Assets assets, Asset asset) {
    this.owner = owner;
    setPack(assets, asset);
  }
  
  void setPack(Assets assets, Asset asset) {
    this.assets = assets;
    this.asset = asset;
    repaint();
  }
  
  void load(File f) {
    try {
      if (assets != null) assets.fs.close();
    } catch (Throwable e) {
      e.printStackTrace();
    }
    try {
      Assets assets = new Assets(f.getAbsolutePath());
      Asset asset = assets.scrape();
      setPack(assets, asset);
      buildTree();
    } catch (Throwable e) {
      e.printStackTrace();
    }
  }

  public void save() throws IOException {
    traverseSave(root);
    repaint();
    assets.sync();
  }


  void traverseBuild(InvisibleNode parentNode, Asset parentAsset) {
  
    for (Asset a : parentAsset.children) {
      InvisibleNode child = new InvisibleNode(a);
      parentNode.add(child);
      traverseBuild(child, a);
    }
  }

  void traverseFilter(InvisibleNode node, String f) {
    Asset a = (Asset) node.getUserObject();
    if (a instanceof AssetPNG) {
      if (f == null)
        node.setVisible(true);
      else
        node.setVisible(a.getName().contains(f));
    }
    for (int i = 0; i < node.getChildCount(); i++) {
      traverseFilter((InvisibleNode) node.getChildAt(i), f);
    }
  }
  
  boolean traverseSweep(InvisibleNode node) {
    if (node.getChildCount() == 0) {
      if (node.isVisible() && !(node.getUserObject() instanceof AssetDir)) {
        return true;
      } else {
        node.setVisible(false);
        return false;
      }
    } else {
      boolean haveChildren = false;
      for (int i = 0; i < node.getChildCount(); i++) {
        haveChildren |= traverseSweep((InvisibleNode) node.getChildAt(i));
      }
      node.setVisible(haveChildren);
      
      return haveChildren;
    }    
  }
  
  
  void traverseSave(InvisibleNode n) {
    Asset a = (Asset)n.getUserObject();
    if (a.isModified()) a.save();
    for (int i = 0; i < n.getChildCount(); i++) {
      traverseSave((InvisibleNode)n.getChildAt(i));
    }
  }

  void updateFilter(InvisibleNode root) {
    if (root == null) return;
    List<TreePath> expanded = new ArrayList<>();
    for (int i = 0; i < tree.getRowCount() - 1; i++) {
      TreePath currPath = tree.getPathForRow(i);
      TreePath nextPath = tree.getPathForRow(i + 1);
      if (currPath.isDescendant(nextPath)) {
        expanded.add(currPath);
      }
    }
    TreePath selected = tree.getSelectionPath();

    String f = filter.getText();
    if (f == null || f.trim().length() == 0) {
      traverseFilter(root, null);
    } else {
      traverseFilter(root, f);
    }
    
    traverseSweep(root);
    
    DefaultTreeModel model = (DefaultTreeModel) tree.getModel();
    model.reload();
    
    for (TreePath path : expanded) {
      tree.expandPath(path);
    }
    tree.setSelectionPath(selected);
    tree.scrollPathToVisible(selected);
  }
  
  JDialog getWait(JFrame owner) {
    JDialog dialog = new JDialog(owner, true);
    dialog.setUndecorated(true);
    dialog.setResizable(false);
    JPanel panel = new JPanel();
    panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
    JLabel labelText = new JLabel("Please wait...");
    labelText.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    labelText.setAlignmentX(Component.CENTER_ALIGNMENT);
    panel.add(labelText);
    final JProgressBar progress = new JProgressBar(JProgressBar.HORIZONTAL);
    progress.setAlignmentX(Component.CENTER_ALIGNMENT);
    progress.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
    progress.setIndeterminate(true);
    panel.add(progress);
    panel.setBorder(BorderFactory.createLineBorder(panel.getForeground(), 1));
    dialog.add(panel);
    dialog.pack();
    dialog.setLocation(owner.getLocationOnScreen().x + (owner.getWidth() - dialog.getWidth())/2, owner.getLocationOnScreen().y + (owner.getHeight()- dialog.getHeight())/2);
    return dialog;
  }
  
  void buildTree() {
    root = new InvisibleNode(asset);
    traverseBuild(root, asset);
    traverseSweep(root);
    InvisibleTreeModel model = new InvisibleTreeModel(root);
    model.activateFilter(true);
    tree.setModel(model);
  }
  
  public void build() {
    setLayout(new BorderLayout());
    tree.setModel(null);
    tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
    tree.setCellRenderer(new AssetCellRenderer());
    JScrollPane scrl = new JScrollPane(tree);
    JPanel butPanel = new JPanel();
    butPanel.add(new JButton(new AbstractAction("Load") {
      public void actionPerformed(ActionEvent e) {
        File f = UIUtil.selectFile(owner, "Load a resource pack", "OK", true, false);
        if (f == null) return;
        JDialog wait = getWait(owner);
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            wait.setVisible(true);
          }
        });
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            new Thread(new Runnable() {
              public void run() {
                Log.println("loading");
                try {
                  load(f);
                } catch (Throwable t) {
                  t.printStackTrace();
                }
                Log.println("closing wait dialog");
                wait.setVisible(false);
                wait.dispose();
              }
            }).start();
          }
        });
      }
    }));
    butPanel.add(new JButton(new AbstractAction("Save") {
      public void actionPerformed(ActionEvent e) {
        JDialog wait = getWait(owner);
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            wait.setVisible(true);
          }
        });
        SwingUtilities.invokeLater(new Runnable() {
          public void run() {
            new Thread(new Runnable() {
              public void run() {
                Log.println("saving");
                try {
                  save();
                  Log.println("saved");
                } catch (Throwable t) {
                  t.printStackTrace();
                }
                Log.println("closing wait dialog");
                wait.setVisible(false);
                wait.dispose();
              }
            }).start();
          }
        });
      }
    }));

    add(butPanel, BorderLayout.NORTH);
    add(scrl, BorderLayout.CENTER);
    add(filter, BorderLayout.SOUTH);
    filter.getDocument().addDocumentListener(new DocumentListener() {
      public void removeUpdate(DocumentEvent e) {
        updateFilter(root);
      }
      public void insertUpdate(DocumentEvent e) {
        updateFilter(root);
      }
      public void changedUpdate(DocumentEvent e) {
        updateFilter(root);
      }
    });
  }

  class AssetCellRenderer extends DefaultTreeCellRenderer {
    public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded, boolean leaf,
        int row, boolean hasFocus) {
      super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
      DefaultMutableTreeNode node = (DefaultMutableTreeNode) value;
      Object userObject = node.getUserObject();
      if (userObject == null || !(userObject instanceof Asset)) return this;
      Asset asset = (Asset) userObject;
      String name = asset.getName();
      this.setFont(getFont().deriveFont(asset.isModified() ? Font.ITALIC : Font.PLAIN));
      this.setForeground(asset.isModified() ? Color.blue : Color.black);
      this.setText(name);
      this.setIcon(asset.getIcon());
      return this;
    }
  }

  public void addTreeSelectionListener(TreeSelectionListener treeSelectionListener) {
    tree.addTreeSelectionListener(treeSelectionListener);
  }

  // http://www.java2s.com/Code/Java/Swing-Components/InvisibleNodeTreeExample.htm
  class InvisibleTreeModel extends DefaultTreeModel {

    protected boolean filterIsActive;

    public InvisibleTreeModel(TreeNode root) {
      this(root, false);
    }

    public InvisibleTreeModel(TreeNode root, boolean asksAllowsChildren) {
      this(root, false, false);
    }

    public InvisibleTreeModel(TreeNode root, boolean asksAllowsChildren, boolean filterIsActive) {
      super(root, asksAllowsChildren);
      this.filterIsActive = filterIsActive;
    }

    public void activateFilter(boolean newValue) {
      filterIsActive = newValue;
    }

    public boolean isActivatedFilter() {
      return filterIsActive;
    }

    public Object getChild(Object parent, int index) {
      if (filterIsActive) {
        if (parent instanceof InvisibleNode) {
          return ((InvisibleNode) parent).getChildAt(index, filterIsActive);
        }
      }
      return ((TreeNode) parent).getChildAt(index);
    }

    public int getChildCount(Object parent) {
      if (filterIsActive) {
        if (parent instanceof InvisibleNode) {
          return ((InvisibleNode) parent).getChildCount(filterIsActive);
        }
      }
      return ((TreeNode) parent).getChildCount();
    }

  }

  class InvisibleNode extends DefaultMutableTreeNode {

    protected boolean isVisible;

    public InvisibleNode() {
      this(null);
    }

    public InvisibleNode(Object userObject) {
      this(userObject, true, true);
    }

    public InvisibleNode(Object userObject, boolean allowsChildren, boolean isVisible) {
      super(userObject, allowsChildren);
      this.isVisible = isVisible;
    }

    public TreeNode getChildAt(int index, boolean filterIsActive) {
      if (!filterIsActive) {
        return super.getChildAt(index);
      }
      if (children == null) {
        throw new ArrayIndexOutOfBoundsException("node has no children");
      }

      int realIndex = -1;
      int visibleIndex = -1;
      Enumeration<?> e = children.elements();
      while (e.hasMoreElements()) {
        InvisibleNode node = (InvisibleNode) e.nextElement();
        if (node.isVisible()) {
          visibleIndex++;
        }
        realIndex++;
        if (visibleIndex == index) {
          return (TreeNode) children.elementAt(realIndex);
        }
      }

      throw new ArrayIndexOutOfBoundsException("index unmatched");
      // return (TreeNode)children.elementAt(index);
    }

    public int getChildCount(boolean filterIsActive) {
      if (!filterIsActive) {
        return super.getChildCount();
      }
      if (children == null) {
        return 0;
      }

      int count = 0;
      Enumeration<?> e = children.elements();
      while (e.hasMoreElements()) {
        InvisibleNode node = (InvisibleNode) e.nextElement();
        if (node.isVisible()) {
          count++;
        }
      }

      return count;
    }

    public void setVisible(boolean visible) {
      this.isVisible = visible;
    }

    public boolean isVisible() {
      return isVisible;
    }
  }
}
