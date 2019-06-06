package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.jdesktop.swingx.treetable.MutableTreeTableNode;
import org.literacybridge.acm.gui.Assistant.Assistant;
import org.literacybridge.acm.gui.assistants.common.AbstractReviewPage;

import javax.swing.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class ReviewPage extends AbstractReviewPage<GreetingsImportContext, GreetingMatchable> {

    protected ReviewPage(Assistant.PageHelper<GreetingsImportContext> listener) {
        super(listener);
    }

    @Override
    protected List<JComponent> getPageIntro() {
        List<JComponent> components = new ArrayList<>();
        JLabel welcome = new JLabel(
            "<html>" + "<span style='font-size:2.5em'>Review & Import</span>"
                + "<br/><br/>When you are satisfied with these imports, click \"Finish\" to import the greetings. "
                + "</html>");
        components.add(welcome);

        Box hbox = Box.createHorizontalBox();
        hbox.add(new JLabel("Importing custom recipient greetings. "));
        hbox.add(Box.createHorizontalGlue());
        components.add(hbox);

        return components;
    }

    @Override
    protected GreetingsImportContext.GreetingMatchableColumnProvider getColumnProvider() {
        return context.new GreetingMatchableColumnProvider();
    }

    @Override
    protected ImportPreviewTreeTableModel getTreeModel(MutableTreeTableNode root) {
        return new GreetingsPreviewTreeModel(root,  context.new GreetingMatchableColumnProvider());
    }

    @Override
    protected void fillTreeModel() {
        List<GreetingMatchable> importables = context.matcher.matchableItems.stream()
            .filter((item) -> item.getMatch().isMatch())
            .filter((item) -> item.getLeft().isImportable())
            .collect(Collectors.toList());

        for (GreetingMatchable importable : importables) {
            PreviewTargetNode node = new PreviewTargetNode(importable);
            importPreviewTreeTableModel.insertNodeInto(node, importPreviewRoot, importPreviewRoot.getChildCount());
        }
    }

    private  class GreetingsPreviewTreeModel extends ImportPreviewTreeTableModel {
         GreetingsPreviewTreeModel(MutableTreeTableNode root,  GreetingsImportContext.GreetingMatchableColumnProvider columnProvider) {
            super(root, columnProvider);
        }
    }

}
