package org.literacybridge.acm.gui.assistants.PromptsImport;

import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.assistants.Matcher.ColumnProvider;
import org.literacybridge.acm.gui.assistants.common.AbstractReviewPage;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReviewPage extends AbstractReviewPage<PromptImportContext, PromptMatchable> {

    protected ReviewPage(Assistant.PageHelper<PromptImportContext> listener) {
        super(listener);
    }

    @Override
    protected List<JComponent> getPageIntro() {
        List<JComponent> components = new ArrayList<>();
        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Review & Import</span>"
                + "<br/><br/>When you are satisfied with these imports, click \"Finish\" to import the prompts. "
                + "</html>");
        components.add(welcome);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Importing system language greetings. "));
        hbox.add(Box.createHorizontalGlue());
        components.add(hbox);

        return components;
    }

    @Override
    protected PromptTargetColumnProvider getColumnProvider() {
        return new PromptTargetColumnProvider();
    }

    @Override
    protected ImportPreviewTreeTableModel getTreeModel(MutableTreeTableNode root) {
        return new PromptPreviewTreeModel(root,  new PromptTargetColumnProvider());
    }

    @Override
    protected void fillTreeModel() {
        List<PromptMatchable> importables = context.matcher.matchableItems.stream()
            .filter((item) -> item.getMatch().isMatch())
            .filter((item) -> item.getLeft().isImportable())
            .collect(Collectors.toList());

        for (PromptMatchable importable : importables) {
            PreviewTargetNode node = new PreviewTargetNode(importable);
            importPreviewTreeTableModel.insertNodeInto(node, importPreviewRoot, importPreviewRoot.getChildCount());
        }
    }

    private class PromptPreviewTreeModel extends ImportPreviewTreeTableModel {
         PromptPreviewTreeModel(MutableTreeTableNode root,  ColumnProvider columnProvider) {
            super(root, columnProvider);
        }
    }

    private class PromptTargetColumnProvider implements ColumnProvider<PromptMatchable> {

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) {
                return "Id";
            }
            return "Description";
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            if (columnIndex == 0) {
                return Integer.class;
            }
            return String.class;
        }

        @Override
        public Object getValueAt(PromptMatchable data, int columnIndex) {
            if (data == null) return null;
            PromptTarget target = data.getLeft();
            if (columnIndex == 0) {
                return target.getPromptId();
            }
            return target.getPromptDefinition();
        }
    }

}
