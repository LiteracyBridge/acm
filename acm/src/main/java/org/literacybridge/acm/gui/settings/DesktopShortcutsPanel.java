package org.literacybridge.acm.gui.settings;

import it.cnr.imaa.essi.lablib.gui.checkboxtree.CheckboxTree;
import org.apache.commons.io.FilenameUtils;
import org.literacybridge.acm.gui.Application;
import org.literacybridge.acm.gui.Assistant.GBC;
import org.literacybridge.acm.gui.Assistant.RoundedLineBorder;
import org.literacybridge.acm.gui.resourcebundle.LabelProvider;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.literacybridge.acm.Constants.USER_HOME_DIR;
import static org.literacybridge.acm.gui.Assistant.AssistantPage.getGBC;

public class DesktopShortcutsPanel extends AbstractSettingsBase {
    private final List<File> shortcuts;

    private final DefaultMutableTreeNode shortcutsRootNode;
    private final DefaultTreeModel shortcutsTreeModel;
    private CheckboxTree shortcutsTree = null;
    private final TCheckBox selectDeselectAll;
    private final JButton deleteShortcuts;

    @Override
    public String getTitle() {
        return "Desktop Shortcuts";
    }

    public DesktopShortcutsPanel(AbstractSettingsDialog.SettingsHelper helper) {
        super(helper);

        // Set an empty border on the panel, to give some blank space around the content.
        setLayout(new BorderLayout());
        setBorder(new EmptyBorder(5, 10, 10, 10));

        add(new JLabel(LabelProvider.getLabel("Desktop shortcuts, applies to this computer only.")), BorderLayout.NORTH);

        // An intermediate panel, with a nice border.
        JPanel borderPanel = new JPanel(new BorderLayout());
        add(borderPanel, BorderLayout.CENTER);
        borderPanel.setBorder(new RoundedLineBorder(new Color(112, 154, 208), 1, 6));

        // The inner panel, to hold the grid. Also has an empty border, to give some blank space.
        JPanel gridPanel = new JPanel(new GridBagLayout());
        borderPanel.add(gridPanel, BorderLayout.CENTER);
        gridPanel.setBorder(new EmptyBorder(10, 10, 10, 10));

        // Constraints for the left column.
        GBC gbc = new GBC(getGBC());

        gbc.insets.bottom = 0; // tighter bottom spacing.
        Box hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalStrut(23));
        selectDeselectAll = new TCheckBox();
        selectDeselectAll.addActionListener(this::onSelectDeselectAll);
        Color fg = new DefaultTreeCellRenderer() {
            Color fg() {return textSelectionColor;}
        }.fg();
        selectDeselectAll.setForeground(fg);
        hBox.add(selectDeselectAll);

        hBox.add(Box.createHorizontalGlue());
        // Anonymous subclass to get to protected member property.
        Color bg = new DefaultTreeCellRenderer() {
            Color bg() {return backgroundSelectionColor;}
        }.bg();

        hBox.setOpaque(true);
        hBox.setBackground(bg);
        gridPanel.add(hBox, gbc);
        gbc.insets.bottom = 12; // normal bottom spacing.

        shortcutsRootNode = new DefaultMutableTreeNode();
        shortcutsTreeModel = new DefaultTreeModel(shortcutsRootNode);
        shortcutsTree = new CheckboxTree(shortcutsTreeModel);
        ToolTipManager.sharedInstance().registerComponent(shortcutsTree);
        JScrollPane shortcutScrollPane = new JScrollPane(shortcutsTree);

        shortcuts = getDesktopShortcuts();
        if (shortcuts.size() == 0) {
            helper.setEnabled(false);
            helper.setToolTip("No desktop shortcuts found.");
        }
        // Add the shortcuts to the list, initial state is checked.
        for (File shortcut : shortcuts) {
            DefaultMutableTreeNode node = new DefaultMutableTreeNode(FilenameUtils.removeExtension(shortcut.getName()));
            shortcutsTreeModel.insertNodeInto(node,
                shortcutsRootNode, shortcutsRootNode.getChildCount());
        }
        selectAll();

        shortcutsTree.setRootVisible(false);
        shortcutsTree.expandPath(new TreePath(shortcutsRootNode.getPath()));

        shortcutsTree.addTreeCheckingListener(e -> {
            checkSelections();
        });

        gridPanel.add(shortcutScrollPane, gbc.withWeighty(1.0).setFill(GridBagConstraints.BOTH));

        // Consume any blank space.
        gridPanel.add(new JLabel(), gbc.withWeighty(0.1));

        hBox = Box.createHorizontalBox();
        hBox.add(Box.createHorizontalGlue());
        deleteShortcuts = new JButton("Delete Selected Shortcuts");
        deleteShortcuts.addActionListener(this::deleteShortcuts);
        hBox.add(deleteShortcuts);
        hBox.add(Box.createHorizontalGlue());
        gridPanel.add(hBox, gbc);

        checkSelections();
    }

    /**
     * Removes the selected shortcuts from the Desktop.
     * @param actionEvent is ignored.
     */
    private void deleteShortcuts(ActionEvent actionEvent) {
        Map<String,File> nameMap = new HashMap<>();
        shortcuts.forEach(f->nameMap.put(FilenameUtils.removeExtension(f.getName()), f));
        TreePath[] tp = shortcutsTree.getCheckingPaths();
        for (TreePath aTp : tp) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) aTp.getLastPathComponent();
            if (node.getUserObject() != null) {
                String obj = node.getUserObject().toString();
                File file = nameMap.get(obj);
                if (file != null) {
                    if (!file.delete()) Application.getApplication()
                        .setStatusMessage(String.format("Couldn't remove shortcut file %s",
                            file.getName()), 5000);
                    // Removing the node from the model doesn't remove it from teh list of
                    // checked paths.
                    shortcutsTree.removeCheckingPath(aTp);
                    shortcutsTreeModel.removeNodeFromParent(node);
                }
            }
        }
    }

    /**
     * Handles checking and unchecking of the "select all" checkbox.
     * @param actionEvent is ignored.
     */
    private void onSelectDeselectAll(ActionEvent actionEvent) {
        if (selectDeselectAll.isIndeterminate()) return;
        boolean checking = selectDeselectAll.isSelected();

        if (!checking) {
            shortcutsTree.clearChecking();
        } else {
            selectAll();
        }
    }

    /**
     * Helper to select all of the nodes.
     */
    private void selectAll() {
        // One level, so depth first == breadth first.
        Enumeration en = shortcutsRootNode.depthFirstEnumeration();
        while (en.hasMoreElements()) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) en.nextElement();
            if (!node.equals(shortcutsRootNode))
                shortcutsTree.addCheckingPath(new TreePath(node.getPath()));
        }
    }

    /**
     * Sees which shortcuts are selected, and sets controls appropriately.
     */
    private void checkSelections() {
        Set<String> checked = new HashSet<>();
        TreePath[] tp = shortcutsTree.getCheckingPaths();
        for (TreePath aTp : tp) {
            DefaultMutableTreeNode node = (DefaultMutableTreeNode) aTp.getLastPathComponent();
            if (node.getUserObject() != null) {
                String obj = node.getUserObject().toString();
                checked.add(obj);
            }
        }
        if (checked.size() == 0) {
            selectDeselectAll.setSelected(false);
        } else if (checked.size() == shortcuts.size()) {
            selectDeselectAll.setSelected(true);
            // Seems a little nicer without text.
//            selectDeselectAll.setText("Choose none.");
        } else {
            selectDeselectAll.setIndeterminate(true);
//            selectDeselectAll.setText("Choose all.");
        }

        deleteShortcuts.setEnabled(checked.size() > 0);
    }

    @Override
    public void onCancel() {
        // Nothing to do.
    }

    @Override
    public void onOk() {
        // Nothing to do.
    }

    @Override
    public boolean settingsValid() {
        // No settings; always valid.
        return true;
    }

    /**
     * Gets a list of the .lnk files with "ACM *.lnk" or "TBL *.lnk" as title.
     * @return a list of the shortcut files.
     */
    private List<File> getDesktopShortcuts() {
        Set<String> EXTENSIONS_TO_CLEAN = Arrays.stream(new String[] { "lnk" })
            .collect(Collectors.toSet());
        File desktop = new File(USER_HOME_DIR, "Desktop");
        File[] shortcuts = desktop.listFiles((dir, name) -> {
            String ext = FilenameUtils.getExtension(name).toLowerCase();
//            if (ext.equals("png")) return true;
            if (!(EXTENSIONS_TO_CLEAN.contains(ext))) return false;
            return name.startsWith("ACM ") || name.startsWith("TBL ");
        });
        if (shortcuts != null) return Arrays.asList(shortcuts);
        return new ArrayList<>();
    }

    /**
     * A helper JCheckBox derivitive with an "indeterminate" state.
     */
    static class TCheckBox extends JCheckBox {

        final static boolean MIDasSELECTED = true;  //consider mid-state as selected ?
        final Color dashColor = new DefaultTreeCellRenderer() {
            Color bg() {return backgroundSelectionColor;}
        }.bg();

        boolean isIndeterminate = false;

        public TCheckBox() { this(""); }

        public TCheckBox(String text) {
            this(text, false);
        }

        public TCheckBox(String text, boolean selected) {
            this(text, selected, false);
        }

        public TCheckBox(String text, boolean selected, boolean indeterminate) {
            super(text, selected);
            this.isIndeterminate = indeterminate;
            setModel(new TCheckModel());
        }

        @Override
        public void setSelected(boolean selected) {
            isIndeterminate = false;
            super.setSelected(selected);
            repaint();
        }

        public boolean isIndeterminate() {
            return isIndeterminate;
        }

        public void setIndeterminate(boolean indeterminate) {
            isIndeterminate = indeterminate;
            repaint();
        }

        final static Icon icon = UIManager.getIcon("CheckBox.icon");

        public void paintComponent(Graphics g) {
            if (isIndeterminate()) {
                // The icon is the un-selected icon. Add a dash on top of it.
                icon.paintIcon(this, g, 0, 0);
                int w = icon.getIconWidth();
                int h = icon.getIconHeight();
                int m = h / 2;
                g.setColor(isEnabled() ? new Color(51, 51, 51) : new Color(122, 138, 153));
                g.fillRoundRect(4, m - 1, w - 8, 2, 3, 1);
            } else {
                super.paintComponent(g);
            }
        }

        public class TCheckModel extends JToggleButton.ToggleButtonModel {
            @Override
            public void setSelected(boolean b) {
                isIndeterminate = false;
                super.setSelected(b);
            }
        }
    }
}
