package org.literacybridge.acm.gui.assistants.common;

import org.jdesktop.swingx.JXTreeTable;
import org.jdesktop.swingx.treetable.AbstractMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultMutableTreeTableNode;
import org.jdesktop.swingx.treetable.DefaultTreeTableModel;
import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.ColumnProvider;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableFileItem;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;
import org.literacybridge.acm.gui.assistants.Matcher.Target;
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
import java.util.List;

import static org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;

public abstract class AbstractReviewPage<CONTEXT_T, MATCHABLE_T extends MatchableFileItem<? extends Target, ? extends ImportableFile>> extends AcmAssistantPage<CONTEXT_T> {

    private final ColumnProvider<MATCHABLE_T> targetColumnProvider;
    protected final DefaultMutableTreeTableNode importPreviewRoot;
    protected final ImportPreviewTreeTableModel importPreviewTreeTableModel;
    protected final ImportPreviewTreeTable importPreviewTreeTable;

    protected AbstractReviewPage(PageHelper<CONTEXT_T> listener) {
        super(listener);
        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        getPageIntro().forEach(comp -> add(comp, gbc));

        // Title preview.
        JLabel importPreviewLabel = new JLabel("Files to be imported:");
        gbc.insets = new Insets(0,0,0,0);
        add(importPreviewLabel, gbc);

        JPanel panel = new JPanel();
        panel.setLayout(new BorderLayout(0,0));

        targetColumnProvider = getColumnProvider();
        importPreviewRoot = new DefaultMutableTreeTableNode();
        importPreviewTreeTableModel = getTreeModel(importPreviewRoot);
        importPreviewTreeTable = new ImportPreviewTreeTable(importPreviewTreeTableModel);
        importPreviewTreeTable.setRootVisible(false);
        importPreviewTreeTable.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);

        AbstractTreeTableRenderer importPreviewTreeTableRenderer = getTreeTableRenderer(importPreviewTreeTableModel);
        importPreviewTreeTable.setDefaultRenderer(Object.class, importPreviewTreeTableRenderer);
        importPreviewTreeTable.setTreeCellRenderer(importPreviewTreeTableRenderer);

        // Set the operation column to be centered.
        AbstractTreeTableRenderer centerRenderer = getTreeTableRenderer(importPreviewTreeTableModel);
        centerRenderer.setHorizontalAlignment(JLabel.CENTER);
        int centerColumn = targetColumnProvider.getColumnCount();
        importPreviewTreeTable.getColumnModel().getColumn(centerColumn).setCellRenderer( centerRenderer );

        JScrollPane importPreviewScroller = new JScrollPane(importPreviewTreeTable);
        panel.add(importPreviewScroller, BorderLayout.CENTER);

        gbc.ipadx = 10;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.bottom = 0;
        add(panel, gbc);
    }

    protected abstract List<JComponent> getPageIntro();
    protected abstract ColumnProvider<MATCHABLE_T> getColumnProvider();
    protected abstract ImportPreviewTreeTableModel getTreeModel(MutableTreeTableNode root);
    protected AbstractTreeTableRenderer getTreeTableRenderer(ImportPreviewTreeTableModel model) {
        return new ImportPreviewTreeTableRenderer(model);
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        while (importPreviewTreeTableModel.getRoot().getChildCount() > 0) {
            MutableTreeTableNode node = (MutableTreeTableNode) importPreviewTreeTableModel.getRoot().getChildAt(0);
            importPreviewTreeTableModel.removeNodeFromParent(node);
        }

        fillTreeModel();

//        importPreviewTreeTable.sizeColumns();
        sizeColumns();
        importPreviewTreeTable.expandAll();
        setComplete();
    }

    protected abstract void fillTreeModel();

    @Override
    protected void onPageLeaving(boolean progressing) {
    }

    @Override
    protected String getTitle() {
        return "Review Files to Import";
    }

    protected void sizeColumns() {
        List<SizingParams> params = getSizingParams();

        AssistantPage.sizeColumns(importPreviewTreeTable, params);
        // The Operation and Audio File columns have been sized to fit themselves. The target will get the rest.
    }
    protected List<SizingParams> getSizingParams() {
        List<SizingParams> params = new ArrayList<>();
        int n = targetColumnProvider.getColumnCount();

        // Set column 1 width (Operation) on header & values.
        params.add(new SizingParams(n, SizingParams.IGNORE, 10, 10));

        // Set column 2 width (Audio File) on header & values.
        params.add(new SizingParams(n+1, SizingParams.IGNORE, 20, SizingParams.IGNORE));

        return params;
    }

    protected class ImportPreviewTreeTable extends JXTreeTable {
        ImportPreviewTreeTable(ImportPreviewTreeTableModel fileTreeModel) {
            super(fileTreeModel);
        }

//        void sizeColumns() {
//            List<SizingParams> params = new ArrayList<>();
//            int n = targetColumnProvider.getColumnCount();
//
//            // Set column 1 width (Operation) on header & values.
//            params.add(new SizingParams(n, SizingParams.IGNORE, 10, 10));
//
//            // Set column 2 width (Audio File) on header & values.
//            params.add(new SizingParams(n+1, SizingParams.IGNORE, 20, SizingParams.IGNORE));
//
//            AssistantPage.sizeColumns(this, params);
//            // The Operation and Audio File columns have been sized to fit themselves. The target will get the rest.
//        }
    }

    private class PlaylistNode extends DefaultMutableTreeTableNode {
        PlaylistNode(Playlist playlist) {
            super(playlist, true);
        }

        @Override
        public Object getValueAt(int column) {
            if (column == 0) return undecoratedPlaylistName(((Playlist)getUserObject()).getName());
            return "";
        }
    }
    protected static class PreviewTargetNode<U> extends DefaultMutableTreeTableNode {
        public PreviewTargetNode(U matchable) {
            super(matchable, false);
        }
        @Override
        public Object getValueAt(int column) {
            if (getUserObject() instanceof MatchableItem) {
                @SuppressWarnings("unchecked")
                MatchableItem<? extends Target, ? extends ImportableFile> matchable = (MatchableItem) getUserObject();
                switch (column) {
                case 0:
                    return matchable.getLeft().getTitle();
                case 1:
                    return matchable.getOperation();
                case 2:
                    return matchable.getRight().getTitle();
                default:
                    return "";
                }
            }
            return "";
        }
        public U getMatchable() {
            return (U)getUserObject();
        }
    }

    /**
     * TreeTable model for the import preview.
     */
    protected static class ImportPreviewTreeTableModel<U extends MatchableFileItem<? extends Target, ? extends ImportableFile>> extends DefaultTreeTableModel {
        String[] nonTargetColumns = { "Operation", "Audio File" };
        ColumnProvider<U> targetColumnProvider;


        public ImportPreviewTreeTableModel(MutableTreeTableNode root,
            ColumnProvider<U> targetColumnProvider) {
            super(root);
            this.targetColumnProvider = targetColumnProvider;
        }

        boolean isEmpty() {
            return getRoot().getChildCount() == 0;
        }

        @Override
        public int getColumnCount() {
            return targetColumnProvider.getColumnCount() + nonTargetColumns.length;
        }

        @Override
        public String getColumnName(int column) {
            if (column < targetColumnProvider.getColumnCount()) {
                return targetColumnProvider.getColumnName(column);
            }
            return nonTargetColumns[column - targetColumnProvider.getColumnCount()];
        }

        @Override
        public Object getValueAt(Object node, int column) {
            if (column < targetColumnProvider.getColumnCount()) {
                Object userObject = ((DefaultMutableTreeTableNode) node).getUserObject();
                if (userObject == null) return null;
                if (node instanceof AbstractReviewPage.PreviewTargetNode) {
                    U matchable = (U) userObject;
                    return targetColumnProvider.getValueAt(matchable, column);
                } else {
                    return userObject.toString();
                }
            }
            return ((DefaultMutableTreeTableNode) node).getValueAt(column - targetColumnProvider.getColumnCount() + 1);
        }

        public boolean isRightColumn(int column) {
            return column > targetColumnProvider.getColumnCount();
        }
        public boolean isCenterColumn(int column) {
            return column == targetColumnProvider.getColumnCount();
        }

        @Override
        public Object getChild(Object parent, int index) {
            if (parent instanceof AbstractReviewPage.PreviewTargetNode) return null;
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
            return node instanceof AbstractReviewPage.PreviewTargetNode;
        }
    }

    protected abstract class AbstractTreeTableRenderer extends JLabel implements TreeCellRenderer, TableCellRenderer {};

    protected class ImportPreviewTreeTableRenderer<U extends MatchableFileItem<? extends Target, ? extends ImportableFile>> extends AbstractTreeTableRenderer {
        protected ImportPreviewTreeTableModel<U> model;

        private String renderValue(Object value, boolean isSelected, int column) {
            if (value == null) return "(null)";
            return value.toString();
        }

        protected ImportPreviewTreeTableRenderer(ImportPreviewTreeTableModel<U> model) {
            super();
            setOpaque(true);
            this.model = model;
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
            int modelColumn = importPreviewTreeTable.convertColumnIndexToModel(column);
            Color bg = model.isCenterColumn(modelColumn) ? bgAlternateColor : bgColor;
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
            if (value instanceof AbstractMutableTreeTableNode) {
                value = model.getValueAt(value, 0);
            }
            String str = renderValue(value, selected, 0);
            Color bg = (selected) ? bgSelectionColor : bgColor;
            setBackground(bg);
            setText(str);
            return this;
        }

    }

}
