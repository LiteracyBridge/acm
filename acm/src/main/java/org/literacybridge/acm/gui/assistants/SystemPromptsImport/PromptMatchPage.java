package org.literacybridge.acm.gui.assistants.SystemPromptsImport;

import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.AbstractMatchTableModel;
import org.literacybridge.acm.gui.assistants.Matcher.IMatcherTableModel;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchTableRenderers;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.gui.assistants.Matcher.MatcherTableTransferHandler;
import org.literacybridge.acm.gui.assistants.Matcher.ColumnProvider;
import org.literacybridge.acm.gui.assistants.common.AbstractMatchPage;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableRowSorter;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class PromptMatchPage extends
                             AbstractMatchPage<PromptImportContext, PromptTarget, ImportableFile, PromptMatchable> {
    private PromptMatchFilter promptMatchFilter;
    private PromptMatchModel promptMatchModel;

    PromptMatchPage(PageHelper<PromptImportContext> listener) {
        super(listener);
    }

    @Override
    protected Component getWelcome() {
        JLabel welcome = new JLabel("<html>"
            + "<span style='font-size:2em'>Match Prompts with Audio.</span>"
            + "<br/><br/><p>The Assistant has automatically matched files as possible with prompts. "
            + "Only high-confidence matches are performed, so manual matching may be required. "
            + "Perform any additional matching (or un-matching) as required, then click \"Finish\" to perform the import.</p>"
            + "</html>");
        return welcome;
    }

    @Override
    protected String getFilterSwitchPrompt() {
        return "Only show Prompts without audio.";
    }

    @Override
    protected String getUnmatchTooltip() {
        return "Unmatch the Audio Item from the File.";
    }

    @Override
    protected String getMatchTooltip() {
        return "Manually match this file with an message.";
    }

    @Override
    protected AbstractMatchTableModel<PromptTarget, PromptMatchable> getModel() {
        if (promptMatchModel == null) {
            promptMatchModel = new PromptMatchModel(new PromptColumnProvider());
        }
        return promptMatchModel;
    }

    @Override
    protected PromptMatchFilter getFilter() {
        if (promptMatchFilter == null) {
            promptMatchFilter = new PromptMatchFilter();
        }
        return promptMatchFilter;
    }

    @Override
    protected void setCellRenderers() {
        TableCellRenderers mtr = new TableCellRenderers(table, promptMatchModel);
        TableCellRenderers.GreetingsMatchCellRenderer renderer = mtr.new GreetingsMatchCellRenderer();
        table.setDefaultRenderer(Object.class, renderer);
        TableColumn columnModel = table.getColumnModel().getColumn(promptMatchModel.getReplaceColumnNo());
        columnModel.setCellRenderer(mtr.getUpdatableBooleanRenderer());
    }

    @Override
    protected void sizeColumns() {
        // We don't want the prompt ID, status, or replace columns to grow much.
        AssistantPage.sizeColumns(table, 0, promptMatchModel.getStatusColumnNo(), promptMatchModel.getReplaceColumnNo());
    }

    @Override
    protected PromptMatchable onManualMatch(PromptMatchable selectedRow) {
        ManualMatcherDialog dialog = new ManualMatcherDialog();
        return dialog.chooseMatchFor(selectedRow, context.matcher.matchableItems);
    }


    @Override
    protected void onPageEntered(boolean progressing) {
        if (progressing) {
            // List of files (right side list)
            List<ImportableFile> files = context.importableFiles.stream()
                .map(ImportableFile::new)
                .collect(Collectors.toList());
            List<PromptTarget> recipients = context.promptIds.stream()
                .map(promptId -> {
                    PromptTarget target = new PromptTarget(promptId, context.promptDefinitions.get(promptId));
                    target.setHasPrompt(context.promptHasRecording.get(promptId));
                    return target;
                })
                .collect(Collectors.toList());

            context.matcher.setData(recipients, files, PromptMatchable::new);
            context.matcher.autoMatch(60);

        }
        promptMatchModel.fireTableDataChanged();
        sizeColumns();
        setComplete();
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
    }

    @Override
    protected String getTitle() {
        return "Match Prompts & Audio";
    }

    /**
     * Update the row filter, based on values in the "Show only Missing" and filter text controls.
     */
    protected void updateFilter() {
        String filterText = this.filterText.getText().toLowerCase();
        boolean onlyMissing = this.showOnlyMissing.isSelected();

        if (!onlyMissing && filterText.length()==0) {
            promptMatchFilter.setPredicate(null);
            this.showOnlyMissing.setEnabled(true);
        } else if (filterText.length()!=0) {
            promptMatchFilter.setPredicate(item -> item.containsText(filterText));
            this.showOnlyMissing.setEnabled(false);
        } else {
            promptMatchFilter.setPredicate(item ->
                item.getLeft()==null || !context.promptHasRecording.get(item.getLeft().getPromptId()));
            this.showOnlyMissing.setEnabled(true);
        }
        promptMatchModel.fireTableDataChanged();
    }

    private class TableCellRenderers extends MatchTableRenderers<PromptMatchable> {

        TableCellRenderers(JTable table, IMatcherTableModel<PromptMatchable> model)
        {
            super(table, model);
        }

        private class GreetingsMatchCellRenderer extends DefaultTableCellRenderer {
            GreetingsMatchCellRenderer() {
                super();
                setOpaque(true);
            }

            @Override
            public JLabel getTableCellRendererComponent(JTable table,
                Object value,
                boolean isSelected,
                boolean hasFocus,
                int row,
                int column)
            {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                Icon icon = null;
                // Put a speaker (with or without sound) on the leftmost column, whatever it contains.
                if (column == 0) {
                    int modelRow = table.convertRowIndexToModel(row);
                    PromptMatchable item = context.matcher.matchableItems.get(modelRow);
                    if (item.getLeft() != null) {
                        String id = item.getLeft().getPromptId();
                        icon = (context.promptHasRecording.getOrDefault(id, true) ?
                                soundImage :
                                noSoundImage);
                    }
                }
                setIcon(icon);
                this.setBackground(getBG(row, column, isSelected));
                return this;
            }
        }
    }

    class PromptMatchFilter extends RowFilter<AbstractMatchTableModel<PromptTarget, PromptMatchable>, Integer> {
        private Predicate<PromptMatchable> predicate;
        void setPredicate(Predicate<PromptMatchable> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean include(Entry<? extends AbstractMatchTableModel<PromptTarget, PromptMatchable>, ? extends Integer> entry) {
            if (predicate == null) return true;
            int rowIx = entry.getIdentifier();
            AbstractMatchTableModel<PromptTarget, PromptMatchable> model = entry.getModel();
            PromptMatchable item = model.getRowAt(rowIx);
            return predicate.test(item);
        }
    }

    public class PromptMatchModel extends AbstractMatchTableModel<PromptTarget, PromptMatchable> {

        PromptMatchModel(ColumnProvider<PromptTarget> columnProvider) {
            super(columnProvider);
        }

        @Override
        public int getRowCount() {
            return context.matcher.matchableItems.size();
        }

        @Override
        public PromptMatchable getRowAt(int rowIndex) {
            return context.matcher.matchableItems.get(rowIndex);
        }
    }

    public class PromptColumnProvider implements ColumnProvider<PromptTarget> {

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int columnIndex) {
            if (columnIndex == 0) return "Id";
            return "Definition";
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return String.class;
        }

        @Override
        public Object getValueAt(PromptTarget target, int columnIndex) {
            if (target == null) return null;
            if (columnIndex == 0) return target.getPromptId();
            return target.getPromptDefinition();
        }
    }
}
