package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.AbstractMatchTableModel;
import org.literacybridge.acm.gui.assistants.Matcher.IMatcherTableModel;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchTableRenderers;
import org.literacybridge.acm.gui.assistants.common.AbstractMatchPage;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import java.awt.Component;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GreetingsMatchPage extends AbstractMatchPage<GreetingsImportContext, GreetingTarget, GreetingFile, GreetingMatchable> {

    private GreetingsMatchFilter greetingsMatchFilter;
    private GreetingsMatchModel greetingsMatchModel;

    GreetingsMatchPage(PageHelper<GreetingsImportContext> listener) {
        super(listener);
    }

    @Override
    protected Component getWelcome() {
        return new JLabel("<html>" + "<span style='font-size:2em'>Match Recipients with Greetings.</span>"
                + "<br/><br/><p>The Assistant has automatically matched files as possible with recipients. "
                + "Only high-confidence matches are performed, so manual matching may be required. "
                + "Perform any additional matching (or un-matching) as required, then click \"Next\" to continue.</p>"
                + "</html>");
    }
    @Override
    protected String getFilterSwitchPrompt() {
        return "Only show Recipients without recordings.";
    }

    protected String getUnmatchTooltip() {
        return "Unmatch the greeting from the file.";
    }
    protected String getMatchTooltip() {
        return "Manually match this file with a greeting.";
    }

    @Override
    protected AbstractMatchTableModel<GreetingTarget, GreetingMatchable> getModel() {
        if (greetingsMatchModel == null) {
            greetingsMatchModel = new GreetingsMatchModel();
        }
        return greetingsMatchModel;
    }

    @Override
    protected GreetingsMatchFilter getFilter() {
        if (greetingsMatchFilter == null) {
            greetingsMatchFilter = new GreetingsMatchFilter();
        }
        return greetingsMatchFilter;
    }

    @Override
    protected void setCellRenderers() {
        TableCellRenderers mtr = new TableCellRenderers(table, tableModel);
        TableCellRenderers.GreetingsMatchCellRenderer renderer = mtr.new GreetingsMatchCellRenderer();
        table.setDefaultRenderer(Object.class, renderer);
        TableColumn columnModel = table.getColumnModel().getColumn(tableModel.getReplaceColumnNo());
        columnModel.setCellRenderer(mtr.getUpdatableBooleanRenderer());
    }

    @Override
    protected void sizeColumns() {
        // Set columns 1 & 2 width (Update? and Status) on header & values.
        AssistantPage.sizeColumns(table,
            greetingsMatchModel.getStatusColumnNo(),
            greetingsMatchModel.getReplaceColumnNo());
    }

    /**
     * Update the row filter, based on values in the "Show only Missing" and filter text controls.
     */
    @Override
    protected void updateFilter() {
        if (greetingsMatchFilter == null) return;
        String filterText = this.filterText.getText().toLowerCase();
        boolean onlyMissing = this.showOnlyMissing.isSelected();

        if (!onlyMissing && filterText.length()==0) {
            greetingsMatchFilter.setPredicate(null);
            this.showOnlyMissing.setEnabled(true);
        } else if (filterText.length()!=0) {
            greetingsMatchFilter.setPredicate(item -> item.containsText(filterText));
            this.showOnlyMissing.setEnabled(false);
        } else {
            greetingsMatchFilter.setPredicate(item ->
                item.getLeft()==null || !context.recipientHasRecording.get(item.getLeft().getRecipient().recipientid));
            this.showOnlyMissing.setEnabled(true);
        }
        tableModel.fireTableDataChanged();
    }

    @Override
    protected GreetingMatchable onManualMatch(GreetingMatchable selectedRow) {
        ManualMatcherDialog dialog = new ManualMatcherDialog(this, selectedRow, context.matcher.matchableItems);
        return dialog.getSelectedItem();
    }

    @Override
    protected void onPageEntered(boolean progressing) {
        if (progressing) {
            // List of files (right side list)
            List<GreetingFile> files = context.importableFiles.stream()
                .map(GreetingFile::new)
                .collect(Collectors.toList());
            List<GreetingTarget> recipients = context.programSpec.getRecipients().stream()
                .map(recipientAdapter -> {
                    GreetingTarget target = new GreetingTarget(recipientAdapter);
                    target.setHasGreeting(context.recipientHasRecording.get(recipientAdapter.recipientid));
                    return target;
                })
                .collect(Collectors.toList());

            context.matcher.setData(recipients, files, GreetingMatchable::new);
            context.matcher.autoMatch(60);

        }
        tableModel.fireTableDataChanged();
        sizeColumns();
        setComplete();
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
    }

    @Override
    protected String getTitle() {
        return "Match Recipients & Greetings";
    }

    private class TableCellRenderers extends MatchTableRenderers<GreetingMatchable> {

        TableCellRenderers(JTable table, IMatcherTableModel<GreetingMatchable> model)
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
                    GreetingMatchable item = context.matcher.matchableItems.get(modelRow);
                    if (item != null && item.getLeft() != null) {
                        if (item.getLeft().targetExists() || item.getMatch().isMatch()) {
                            icon = AcmAssistantPage.soundImage;
                        } else {
                            icon = AcmAssistantPage.noSoundImage;
                        }
                    }
                }
                setIcon(icon);
                this.setBackground(getBG(row, column, isSelected));
                return this;
            }
        }
    }

    class GreetingsMatchFilter extends RowFilter<AbstractMatchTableModel<GreetingTarget, GreetingMatchable>, Integer> {
        private Predicate<GreetingMatchable> predicate;
        void setPredicate(Predicate<GreetingMatchable> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean include(Entry<? extends AbstractMatchTableModel<GreetingTarget, GreetingMatchable>, ? extends Integer> entry) {
            if (predicate == null) return true;
            int rowIx = entry.getIdentifier();
            AbstractMatchTableModel<GreetingTarget, GreetingMatchable> model = entry.getModel();
            GreetingMatchable item = model.getRowAt(rowIx);
            return predicate.test(item);
        }
    }

    class GreetingsMatchModel extends AbstractMatchTableModel<GreetingTarget, GreetingMatchable> {
        GreetingsMatchModel() {
            super(context.new GreetingTargetColumnProvider());
        }

        @Override
        public int getRowCount() {
            return context.matcher.matchableItems.size();
        }

        @Override
        public GreetingMatchable getRowAt(int rowIndex) {
            return context.matcher.matchableItems.get(rowIndex);
        }
    }

}
