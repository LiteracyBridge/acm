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
        hbox.add(new JLabel("Importing system prompts. "));
        hbox.add(Box.createHorizontalGlue());
        components.add(hbox);

        return components;
    }

    @Override
    protected PromptTargetColumnProvider getColumnProvider() {
        return new PromptTargetColumnProvider();
    }

    @Override
    protected List<SizingParams> getSizingParams() {
        // We want to add to the columns to be sized.
        List<SizingParams> params = super.getSizingParams();

        // Set column 0 width (Operation) on header & values.
        params.add(new SizingParams(0, 10, 26, 26));

        return params;
    }

    @Override
    protected ImportPreviewTreeTableModel<PromptMatchable> getTreeModel(MutableTreeTableNode root) {
        return new PromptPreviewTreeModel(root, new PromptTargetColumnProvider());
    }

    @Override
    protected void fillTreeModel() {
        List<PromptMatchable> importables = context.matcher.matchableItems.stream()
            .filter((item) -> item.getMatch().isMatch())
            .filter((item) -> item.getLeft().isImportable())
            .collect(Collectors.toList());

        for (PromptMatchable importable : importables) {
            PreviewTargetNode<PromptMatchable> node = new PreviewTargetNode<>(importable);
            importPreviewTreeTableModel.insertNodeInto(node, importPreviewRoot, importPreviewRoot.getChildCount());
        }
    }

    private static class PromptPreviewTreeModel extends ImportPreviewTreeTableModel<PromptMatchable> {
         PromptPreviewTreeModel(MutableTreeTableNode root,  ColumnProvider<PromptMatchable> columnProvider) {
            super(root, columnProvider);
        }
    }

    private static class PromptTargetColumnProvider implements ColumnProvider<PromptMatchable> {

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
        public Class<String> getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public Object getValueAt(PromptMatchable data, int columnIndex) {
            if (data == null) return null;
            PromptTarget target = data.getLeft();
            if (columnIndex == 0) {
                return target.getPromptId();
            }
            return target.getPromptText();
        }
    }

}
