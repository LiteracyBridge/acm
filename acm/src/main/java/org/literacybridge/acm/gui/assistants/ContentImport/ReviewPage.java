package org.literacybridge.acm.gui.assistants.ContentImport;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.store.Playlist;

import javax.swing.*;
import javax.swing.table.TableCellRenderer;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreeNode;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public class ReviewPage extends ContentImportBase<ContentImportContext> {

//    private final DefaultListModel<String> importPreviewModel;
    private final DefaultMutableTreeTableNode importPreviewRoot;
    private final ImportPreviewTreeModel importPreviewTreeModel;
    private final ImportPreviewTree importPreviewTreeTable;
    private final ImportReminderLine importReminderLine;

    ReviewPage(PageHelper<ContentImportContext> listener) {
        super(listener);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Review & Import</span>"
                + "<br/><br/>When you are satisfied with these imports, click \"Finish\" to perform the import. "

                + "</html>");
        add(welcome, gbc);

        importReminderLine = new ImportReminderLine();
        add(importReminderLine.getLine(), gbc);

        // Title preview.
        JLabel importPreviewLabel = new JLabel("Files to be imported:");
        Insets insets = new Insets(0,0,0,0);
        gbc.insets = insets;
        add(importPreviewLabel, gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0,0));

        importPreviewRoot = new DefaultMutableTreeTableNode();
        importPreviewTreeModel = new ImportPreviewTreeModel(importPreviewRoot);
        importPreviewTreeTable = new ImportPreviewTree(importPreviewTreeModel);
        importPreviewTreeTable.setRootVisible(false);
        importPreviewTreeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        ImportPreviewTreeTableRenderer importPreviewTreeTableRenderer = new ImportPreviewTreeTableRenderer();
        importPreviewTreeTable.setDefaultRenderer(Object.class, importPreviewTreeTableRenderer);
        importPreviewTreeTable.setTreeCellRenderer(importPreviewTreeTableRenderer);

        // Set the operation column to be centered.
        ImportPreviewTreeTableRenderer centerRenderer = new ImportPreviewTreeTableRenderer();
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        importPreviewTreeTable.getColumnModel().getColumn(1).setCellRenderer( centerRenderer );

        JScrollPane importPreviewScroller = new JScrollPane(importPreviewTreeTable);
        panel.add(importPreviewScroller, BorderLayout.CENTER);

        gbc.ipadx = 10;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.bottom = 0;
        add(panel, gbc);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        String languagecode = context.languagecode;

        // Fill deployment and language
        importReminderLine.getDeployment().setText(Integer.toString(context.deploymentNo));
        importReminderLine.getLanguage().setText(languagecode);

        while (importPreviewTreeModel.getRoot().getChildCount() > 0) {
            MutableTreeTableNode node = (MutableTreeTableNode) importPreviewTreeModel.getRoot().getChildAt(0);
            importPreviewTreeModel.removeNodeFromParent(node);
        }

        // For the imports, create a "item from \n file" label, and add to the preview.
        List<AudioMatchable> importables = context.matcher.matchableItems.stream()
            .filter(item -> item.getMatch().isMatch() && item.getLeft().isImportable())
            .collect(Collectors.toList());

        Map<Playlist, PlaylistNode> playlistNodes = new HashMap<>();
        for (AudioMatchable importable : importables) {
            Playlist playlist = importable.getLeft().getPlaylist();
            PlaylistNode playlistNode = playlistNodes.get(playlist);
            if (playlistNode == null) {
                playlistNode = new PlaylistNode(playlist);
                playlistNodes.put(playlist, playlistNode);
                importPreviewTreeModel.insertNodeInto(playlistNode, importPreviewRoot, importPreviewTreeModel.getRoot().getChildCount());
            }
            AudioNode audioNode = new AudioNode(importable);
            playlistNode.add(audioNode);
            importPreviewTreeModel.insertNodeInto(audioNode, playlistNode, playlistNode.getChildCount());
        }
        importPreviewTreeTable.sizeColumns();
        importPreviewTreeTable.expandAll();
        setComplete();
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
    }

    @Override
    protected String getTitle() {
        return "Review Files to Import";
    }


    private class ImportPreviewTree extends JXTreeTable {
        ImportPreviewTree(ImportPreviewTreeModel fileTreeModel) {
            super(fileTreeModel);
        }

        void sizeColumns() {
            List<SizingParams> params = new ArrayList<>();

            // Set column 1 width (Status) on header & values.
            params.add(new SizingParams(1, SizingParams.IGNORE, 10, 10));

            // Set column 2 width (Size) on header & values.
            params.add(new SizingParams(2, SizingParams.IGNORE, 20, SizingParams.IGNORE));

            AssistantPage.sizeColumns(this, params);
            // The timestamp and size columns have been sized to fit themselves. Name will get the rest.
        }
    }


    private class PlaylistNode extends DefaultMutableTreeTableNode {
        PlaylistNode(Playlist playlist) {
            super(playlist, true);
        }

        @Override
        public Object getValueAt(int column) {
            if (column == 0) return ((Playlist)getUserObject()).getName();
            return "";
        }
    }
    private class AudioNode extends DefaultMutableTreeTableNode {
        AudioNode(AudioMatchable matchable) {
            super(matchable, false);
        }
        @Override
        public Object getValueAt(int column) {
            AudioMatchable me = (AudioMatchable)getUserObject();
            switch (column) {
            case 0:
                return me.getLeft().getTitle();
            case 1:
                return me.getOperation();
            case 2:
                return me.getRight().getFile().getName();
            default:
                return "";
            }
        }
    }

    /**
     * TreeTable model for the import preview.
     */
    private class ImportPreviewTreeModel extends DefaultTreeTableModel {
        String[] columns = { "Message", "Operation", "File" };

        ImportPreviewTreeModel(MutableTreeTableNode root) {
            super(root);
        }

        boolean isEmpty() {
            return getRoot().getChildCount() == 0;
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(Object node, int column) {
            return ((DefaultMutableTreeTableNode) node).getValueAt(column);
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent instanceof AudioNode) return null;
            if (parent instanceof AbstractMutableTreeTableNode) {
                return ((AbstractMutableTreeTableNode)parent).getChildAt(index);
            }
            return null;
        }

        @Override
        public int getChildCount(Object parent) {
            if (parent instanceof AbstractMutableTreeTableNode) {
                return ((AbstractMutableTreeTableNode)parent).getChildCount();
            }
            return 0;
        }

        @Override
        public int getIndexOfChild(Object parent, Object child) {
            if (parent instanceof AbstractMutableTreeTableNode && child instanceof TreeNode) {
                return ((AbstractMutableTreeTableNode)parent).getIndex((TreeNode)child);
            }
            return -1;
        }

        @Override
        public boolean isLeaf(Object node) {
            return node instanceof AudioNode;
        }
    }

    private class ImportPreviewTreeTableRenderer extends JLabel
        implements TreeCellRenderer, TableCellRenderer {

        private String renderValue(Object value, boolean isSelected, int column) {
            if (value == null) return "(null)";
            return value.toString();
        }

        ImportPreviewTreeTableRenderer() {
            super();
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
            Object value,
            boolean isSelected,
            boolean hasFocus,
            int row,
            int column)
        {
            String str = renderValue(value, isSelected, column);
            Color bg = (column!=1) ? bgColor : bgAlternateColor;
            bg = (isSelected) ? bgSelectionColor : bg;
            setBackground(bg);
            setText(str);
            return this;
        }

        @Override
        public Component getTreeCellRendererComponent(JTree tree,
            Object value,
            boolean selected,
            boolean expanded,
            boolean leaf,
            int row,
            boolean hasFocus)
        {
            // This method is only called to render the first column, column 0. Sometimes,
            // it is passed the LastComponent() from the tree path, not the actual  value,
            // so if we get a node, convert it to the value.
            if (value instanceof AbstractMutableTreeTableNode) value = ((AbstractMutableTreeTableNode) value).getValueAt(0);
            String str = renderValue(value, selected, 0);
//            Color bg = (row%2 == 0) ? bgColor : bgAlternateColor;
            Color bg = (selected) ? bgSelectionColor : bgColor;
            setBackground(bg);
            setText(str);
            return this;
        }

//        private Color lighten(Color color) {
//            double FACTOR = 1.04;
//            return new Color(Math.min((int) (color.getRed() * FACTOR), 255),
//                Math.min((int) (color.getGreen() * FACTOR), 255),
//                Math.min((int) (color.getBlue() * FACTOR), 255),
//                color.getAlpha());
//        }

    }

}
