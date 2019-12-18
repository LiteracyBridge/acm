package org.literacybridge.acm.gui.assistants.common;

import org.apache.commons.lang3.StringUtils;
import org.literacybridge.acm.gui.Assistant.Assistant.PageHelper;
import org.literacybridge.acm.gui.Assistant.PlaceholderTextField;
import org.literacybridge.acm.gui.assistants.Matcher.AbstractMatchTableModel;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableItem;
import org.literacybridge.acm.gui.assistants.Matcher.Matcher;
import org.literacybridge.acm.gui.assistants.Matcher.MatcherTableTransferHandler;
import org.literacybridge.acm.gui.assistants.Matcher.Target;

import javax.swing.*;
import javax.swing.table.TableRowSorter;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

/**
 * Implements the basic match dialog. Filtering, sorting, matching/unmatching, drag-and-drop matching
 * are provided.
 *
 * Subclasses provide the model, a filter (may be null), and can provide appropriate cell
 * renderers and column sizing.
 * @param <Context> The Context record for the Assistant family. Must implement AbstractMatchPage.MatchContext
 * @param <L> The "L" or Target type of the matchable.
 * @param <R> The "R" or Source type fo the matchable.
 * @param <M> The MatchableItem type.
 */
public abstract class AbstractMatchPage<Context extends AbstractMatchPage.MatchContext,
                                        L extends Target,
                                        R,
                                        M extends MatchableItem<L,R>> extends AcmAssistantPage<Context> {

    /**
     * The Context type must implement this, to provide access to the Matcher.
     * @param <L>
     * @param <R>
     * @param <M>
     */
    public interface MatchContext<L extends Target, R, M extends MatchableItem<L,R>> {
        Matcher<L,R,M> getMatcher();
    }

    // The abstract methods that subclasses must implement.
    protected abstract Component getWelcome();
    protected abstract String getFilterSwitchPrompt();
    protected abstract String getUnmatchTooltip();
    protected abstract String getMatchTooltip();
    protected abstract AbstractMatchTableModel<L, M> getModel();
    protected abstract RowFilter<AbstractMatchTableModel<L,M>, Integer> getFilter();
    protected abstract void setCellRenderers();
    protected abstract void sizeColumns();
    /**
     * Update the row filter, based on values in the "Show only Missing" and filter text controls.
     */
    protected abstract void updateFilter();
    protected abstract M onManualMatch(M selectedRow);

    // Data shared with subclasses.
    protected JTable table;
    protected AbstractMatchTableModel<L, M> tableModel;
    protected JCheckBox showOnlyMissing;
    protected PlaceholderTextField filterText;

    // Internal data.
    private JButton unMatch;
    private JButton manualMatch;

    protected AbstractMatchPage(PageHelper<Context> listener) {
        super(listener);

        setLayout(new GridBagLayout());

        GridBagConstraints gbc = getGBC();

        add(getWelcome(), gbc);

        gbc.insets.bottom = 0;
        Box hbox;
        String filterSwitchPrompt = getFilterSwitchPrompt();
        if (StringUtils.isNotBlank(filterSwitchPrompt)) {
            hbox = Box.createHorizontalBox();
            showOnlyMissing = new JCheckBox(filterSwitchPrompt, false);
            showOnlyMissing.addActionListener(ev-> updateFilter() );
            hbox.add(showOnlyMissing);
            hbox.add(Box.createHorizontalStrut(15));
            hbox.add(new JLabel("Filter: "));
            filterText = new PlaceholderTextField();
            filterText.setPlaceholder("Enter filter text");
            filterText.addKeyListener(new KeyAdapter() {
                @Override
                public void keyReleased(KeyEvent e) {
                    updateFilter();
                }
            });
            hbox.add(filterText);
            hbox.add(Box.createHorizontalStrut(15));
            hbox.add(Box.createHorizontalGlue());
            add(hbox, gbc);
        }

        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        add(makeTable(), gbc);

        gbc.weighty = 0;
        add(makeManualMatchButtons(), gbc);

        // Absorb any extra space.
        //gbc.weighty = 1.0;
        //add(new JLabel(), gbc);


    }

    /**
     * Creates the JTable for recipients and greetings, and the associated auxillary objects.
     * @return a JScrollPane containing the JTable.
     */
    private JScrollPane makeTable() {
        tableModel = getModel();
        table = new JTable(tableModel);

        // Sorting, filtering, re-arranging.
        TableRowSorter<AbstractMatchTableModel<L, M>> sorter = new TableRowSorter<>(tableModel);
        RowFilter<AbstractMatchTableModel<L, M>, Integer> rowFilter = getFilter();
        sorter.setRowFilter(rowFilter);
        table.setRowSorter(sorter);

        // Selection model
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setRowSelectionAllowed(false);
        table.setColumnSelectionAllowed(false);

        // Renderers
        setCellRenderers();

        // Drag and drop
        table.setDragEnabled(true);
        table.setDropMode(DropMode.ON);

        TransferHandler matchTableTransferHandler = new MatcherTableTransferHandler<M>(table,
            tableModel) {
            @SuppressWarnings("unchecked")
            public void onMatched(M sourceRow, M targetRow) {
                context.getMatcher().setMatch(sourceRow, targetRow);
                tableModel.fireTableDataChanged();
            }
        };
        table.setTransferHandler(matchTableTransferHandler);

        // Scroll pane
        JScrollPane scrollPane = new JScrollPane(table);
        table.setFillsViewportHeight(true);

        table.addMouseListener(tableMouseListener);

        return scrollPane;
    }

    private MouseListener tableMouseListener = new MouseAdapter() {
        @Override
        public void mouseClicked(MouseEvent e) {
            if (e.getClickCount() == 2) {
                M selectedRow = selectedRow();
                if (selectedRow != null && selectedRow.getMatch().isUnmatched()) {
                    onManualMatch(new ActionEvent(this, 0, "MouseClicked"));
                }
            }
        }
    };

    private Box makeManualMatchButtons() {
        // Control buttons
        unMatch = new JButton("Unmatch");
        unMatch.addActionListener(this::onUnMatch);
        unMatch.setToolTipText(getUnmatchTooltip());
        unMatch.setEnabled(false);

        manualMatch = new JButton("Manual Match");
        manualMatch.addActionListener(this::onManualMatch);
        manualMatch.setToolTipText(getMatchTooltip());
        manualMatch.setEnabled(false);

        Box hbox = Box.createHorizontalBox();
        hbox.add(unMatch);
        hbox.add(Box.createHorizontalStrut(5));
        hbox.add(manualMatch);
        hbox.add(Box.createHorizontalStrut(5));
        hbox.add(Box.createHorizontalGlue());

        table.getSelectionModel().addListSelectionListener(ev -> enableButtons());

        return hbox;
    }

    /**
     * Called when the manual unmatch button is clicked. If the selected row is currently
     * matched, it will be un-matched.
     * @param actionEvent is unused.
     */
    private void onUnMatch(@SuppressWarnings("unused") ActionEvent actionEvent) {
        int viewRow = table.getSelectionModel().getLeadSelectionIndex();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            M row = tableModel.getRowAt(modelRow);
            if (row != null && row.getMatch().isMatch()) {
                context.getMatcher().unMatch(modelRow);
                tableModel.fireTableDataChanged();
            }
        }
    }

    /**
     * Called when the manual match button is clicked. If appropriate, runs the manual match
     * dialog, and performs any match chosen.
     * @param actionEvent is ignored.
     */
    @SuppressWarnings({ "unused", "unchecked" })
    private void onManualMatch(ActionEvent actionEvent) {
        M selectedRow = selectedRow();
        M chosenMatch = null;

        if (selectedRow.getMatch().isUnmatched()) {
            chosenMatch = onManualMatch(selectedRow);
        }

        if (chosenMatch != null) {
            context.getMatcher().setMatch(chosenMatch, selectedRow);
            tableModel.fireTableDataChanged();
        }
    }

    /**
     * Enables / disables the manual match / unmatch buttons, based on the selection.
     */
    private void enableButtons() {
        M row = selectedRow();
        unMatch.setEnabled(row != null && row.getMatch().isMatch());
        manualMatch.setEnabled(row != null && !row.getMatch().isMatch());
    }

    /**
     * Retrieves the currently selected row.
     * @return the row.
     */
    private M selectedRow() {
        M row = null;
        int viewRow = table.getSelectionModel().getLeadSelectionIndex();
        if (viewRow >= 0) {
            int modelRow = table.convertRowIndexToModel(viewRow);
            row = tableModel.getRowAt(modelRow);
        }
        return row;
    }

}
