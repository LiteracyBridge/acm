package org.literacybridge.acm.gui.assistants.GreetingsImport;

import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.Assistant.PlaceholderTextField;
import org.literacybridge.acm.gui.assistants.Matcher.GreetingsTarget;
import org.literacybridge.acm.gui.assistants.Matcher.IMatcherTableModel;
import org.literacybridge.acm.gui.assistants.Matcher.ImportableFile;
import org.literacybridge.acm.gui.assistants.Matcher.MatchTableRenderers;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableGreeting;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.gui.assistants.Matcher.MatcherTableTransferHandler;
import org.literacybridge.acm.gui.assistants.common.AcmAssistantPage;
import org.literacybridge.core.spec.RecipientList;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class GreetingsMatchPage extends AcmAssistantPage<GreetingsImportContext> {
    private GreetingsMatchFilter greetingsMatchFilter;
    private GreetingsMatchTable greetingsMatchTable;
    private GreetingsMatchModel greetingsMatchModel;
    private final JCheckBox showOnlyMissing;
    private final PlaceholderTextField filterText;
    private JButton unMatch;
    private JButton manualMatch;

    GreetingsMatchPage(PageHelper<GreetingsImportContext> listener) {
        super(listener);

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        JLabel welcome = new JLabel("<html>"
                + "<span style='font-size:2em'>Match Recipients with Greetings.</span>"
                + "<br/><br/><p>The Assistant has automatically matched files as possible with recipients. "
                + "Only high-confidence matches are performed, so manual matching may be required. "
                + "Perform any additional matching (or un-matching) as required, then click \"Finish\" to perform the import.</p>"
                + "</html>");
        add(welcome, gbc);

        gbc.insets.bottom = 0;
        Box hbox = Box.createHorizontalBox();
        showOnlyMissing = new JCheckBox("Only show Recipients without recordings.", false);
        hbox.add(showOnlyMissing);
        hbox.add(Box.createHorizontalStrut(15));
        hbox.add(new JLabel("Filter: "));
        filterText = new PlaceholderTextField();
        filterText.setPlaceholder("Enter filter text");
        hbox.add(filterText);
        hbox.add(Box.createHorizontalStrut(15));
        hbox.add(Box.createHorizontalGlue());
        add(hbox, gbc);

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(makeTable(), gbc);

        gbc.weighty = 0;
        add(makeManualMatchButtons(), gbc);

        // Absorb any extra space.
        //gbc.weighty = 1.0;
        //add(new JLabel(), gbc);

        showOnlyMissing.addActionListener(ev-> updateFilter() );
        filterText.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                updateFilter();
            }
        });

    }

    /**
     * Creates the JTable for recipients and greetings, and the associated auxillary objects.
     * @return a JScrollPane containing the JTable.
     */
    private JScrollPane makeTable() {
        greetingsMatchModel = new GreetingsMatchModel(this, new String[]{"Community","Group"});
        greetingsMatchTable = new GreetingsMatchTable(greetingsMatchModel);

        // Sorting, filtering, re-arranging.
        TableRowSorter<GreetingsMatchModel> sorter = new TableRowSorter<>(greetingsMatchModel);
        greetingsMatchFilter = new GreetingsMatchFilter();
        sorter.setRowFilter(greetingsMatchFilter);
        greetingsMatchTable.setRowSorter(sorter);

        // Selection model
        greetingsMatchTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        greetingsMatchTable.setRowSelectionAllowed(false);
        greetingsMatchTable.setColumnSelectionAllowed(false);

        // Renderers
        TableCellRenderers mtr = new TableCellRenderers(greetingsMatchTable, greetingsMatchModel);
        TableCellRenderers.GreetingsMatchCellRenderer renderer = mtr.new GreetingsMatchCellRenderer();
        greetingsMatchTable.setDefaultRenderer(Object.class, renderer);
        TableColumn columnModel = greetingsMatchTable.getColumnModel().getColumn(greetingsMatchModel.replaceColumnNo);
        columnModel.setCellRenderer(mtr.getUpdatableBooleanRenderer());

        // Drag and drop
        greetingsMatchTable.setDragEnabled(true);
        greetingsMatchTable.setDropMode(DropMode.ON);

        TransferHandler matchTableTransferHandler = new MatcherTableTransferHandler<MatchableGreeting>(greetingsMatchTable, greetingsMatchModel) {
            public void onMatched(MatchableGreeting sourceRow, MatchableGreeting targetRow) {
                context.matcher.setMatch(sourceRow, targetRow);
                greetingsMatchModel.fireTableDataChanged();
            }
        };
        greetingsMatchTable.setTransferHandler(matchTableTransferHandler);

        // Scroll pane
        JScrollPane scrollPane = new JScrollPane(greetingsMatchTable);
        greetingsMatchTable.setFillsViewportHeight(true);

        greetingsMatchTable.addMouseListener(tableMouseListener);

        return scrollPane;
    }

    private MouseListener tableMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                MatchableGreeting selectedRow = selectedRow();
                if (selectedRow != null && selectedRow.getMatch().isUnmatched()) {
                    onManualMatch(null);
                }
            }
        }
    };

    private Box makeManualMatchButtons() {
        // Control buttons
        Box hbox = Box.createHorizontalBox();
        unMatch = new JButton("Unmatch");
        unMatch.addActionListener(this::onUnMatch);
        unMatch.setToolTipText("Unmatch the Audio Item from the File.");
        hbox.add(unMatch);
        hbox.add(Box.createHorizontalStrut(5));
        manualMatch = new JButton("Manual Match");
        manualMatch.addActionListener(this::onManualMatch);
        manualMatch.setToolTipText("Manually match this file with an message.");
        hbox.add(manualMatch);
        hbox.add(Box.createHorizontalStrut(5));

        unMatch.setEnabled(false);
        manualMatch.setEnabled(false);

        hbox.add(Box.createHorizontalGlue());

        greetingsMatchTable.getSelectionModel().addListSelectionListener(ev -> enableButtons());

        return hbox;
    }

    private void onUnMatch(ActionEvent actionEvent) {
        int viewRow = greetingsMatchTable.getSelectionModel().getLeadSelectionIndex();
        if (viewRow >= 0) {
            int modelRow = greetingsMatchTable.convertRowIndexToModel(viewRow);
            MatchableGreeting row = greetingsMatchModel.getRowAt(modelRow);
            if (row != null && row.getMatch().isMatch()) {
                context.matcher.unMatch(modelRow);
                greetingsMatchModel.fireTableDataChanged();
            }
        }
    }

    private void onManualMatch(ActionEvent actionEvent) {
        MatchableGreeting selectedRow = selectedRow();
        MatchableGreeting chosenMatch = null;

        ManualMatcherDialog dialog = new ManualMatcherDialog();
        if (selectedRow.getMatch().isUnmatched()) {
            chosenMatch = dialog.chooseMatchFor(selectedRow, context.matcher.matchableItems);
        }

        if (chosenMatch != null) {
            context.matcher.setMatch(chosenMatch, selectedRow);
            greetingsMatchModel.fireTableDataChanged();
        }
    }

    private void enableButtons() {
        MatchableGreeting row = selectedRow();
        unMatch.setEnabled(row != null && row.getMatch().isMatch());
        manualMatch.setEnabled(row != null && !row.getMatch().isMatch());
    }

    private MatchableGreeting selectedRow() {
        MatchableGreeting row = null;
        int viewRow = greetingsMatchTable.getSelectionModel().getLeadSelectionIndex();
        if (viewRow >= 0) {
            int modelRow = greetingsMatchTable.convertRowIndexToModel(viewRow);
            row = greetingsMatchModel.getRowAt(modelRow);
        }
        return row;
    }


    @Override
    protected void onPageEntered(boolean progressing) {
        if (progressing) {
            // List of files (right side list)
            List<ImportableFile> files = context.importableFiles.stream()
                .map(ImportableFile::new)
                .collect(Collectors.toList());
            List<GreetingsTarget> recipients = context.programSpec.getRecipients().stream()
                .map(recipientAdapter -> {
                    GreetingsTarget target = new GreetingsTarget(recipientAdapter);
                    target.setHasGreeting(context.recipientHasRecording.get(recipientAdapter.recipientid));
                    return target;
                })
                .collect(Collectors.toList());

            context.matcher.setData(recipients, files, MatchableGreeting::new);

            Matcher.MatchStats result = new Matcher.MatchStats();
            if (context.matcher != null) {
                result.add(context.matcher.findExactMatches());
                result.add(context.matcher.findFuzzyMatches(60));
                result.add(context.matcher.findTokenMatches(60));
                //context.matcher.sortByProgramSpecification();
                System.out.println(result.toString());
            }

            greetingsMatchTable.sizeColumns();
        }
        greetingsMatchModel.fireTableDataChanged();
        setComplete();
    }

    @Override
    protected void onPageLeaving(boolean progressing) {
    }

    @Override
    protected String getTitle() {
        return "Match Recipients & Greetings";
    }

    /**
     * Update the row filter, based on values in the "Show only Missing" and filter text controls.
     */
    private void updateFilter() {
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
        greetingsMatchModel.fireTableDataChanged();
    }

    List<MatchableGreeting> getMatchableItems() {
        return context.matcher.matchableItems;
    }

    RecipientList getRecipients() {
        return context.programSpec.getRecipients();
    }

    private class TableCellRenderers extends MatchTableRenderers<MatchableGreeting> {

        TableCellRenderers(JTable table, IMatcherTableModel<MatchableGreeting> model)
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
                if (column == 0) {
                    int modelRow = greetingsMatchTable.convertRowIndexToModel(row);
                    MatchableGreeting item = context.matcher.matchableItems.get(modelRow);
                    if (item.getLeft() != null) {
                        String recipientid = item.getLeft().getRecipient().recipientid;
                        icon = (context.recipientHasRecording.getOrDefault(recipientid, true) ?
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

    class GreetingsMatchFilter extends RowFilter<GreetingsMatchModel, Integer> {
        private Predicate<MatchableGreeting> predicate;
        void setPredicate(Predicate<MatchableGreeting> predicate) {
            this.predicate = predicate;
        }

        @Override
        public boolean include(Entry<? extends GreetingsMatchModel, ? extends Integer> entry) {
            if (predicate == null) return true;
            int rowIx = entry.getIdentifier();
            GreetingsMatchModel model = entry.getModel();
            MatchableGreeting item = model.getRowAt(rowIx);
            return predicate.test(item);
        }
    }

    class GreetingsMatchTable extends JTable {

        GreetingsMatchTable(TableModel dm) {
            super(dm);
        }

        void sizeColumns() {
            // Set columns 1 & 2 width (Update? and Status) on header & values.
            AssistantPage.sizeColumns(this, greetingsMatchModel.statusColumnNo, greetingsMatchModel.replaceColumnNo);
        }
    }

}
