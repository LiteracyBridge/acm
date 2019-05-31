package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.Assistant.AssistantPage;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableRowSorter;
import java.awt.Dimension;
import java.util.function.Predicate;

public class MatcherTable extends JTable {

    private MatcherTableModel model;
    private MatcherFilter filter;

    MatcherTable() {
        super();

        model = new MatcherTableModel(this);
        setModel(model);
        TableRowSorter<MatcherTableModel> sorter = new TableRowSorter<>(model);
        filter = new MatcherFilter(model);
        sorter.setRowFilter(filter);

        model.addTableModelListener(tableModelEvent ->
            SwingUtilities.invokeLater(() -> tableModelListener(tableModelEvent)));

        setRowSorter(sorter);
        model.setupSorter(sorter);

        setPreferredScrollableViewportSize(new Dimension(500, 70));
    }

    public void setFilter(Predicate<AudioMatchable> predicate) {
        filter.setPredicate(predicate);
    }

    private void tableModelListener(TableModelEvent tableModelEvent) {
        if (tableModelEvent.getFirstRow() == TableModelEvent.HEADER_ROW) {
            sizeColumns();
        }
    }

    void sizeColumns() {
        // Set columns 1 & 2 width (Update? and Status) on header & values.
        AssistantPage.sizeColumns(this, MatcherTableModel.Columns.Status.ordinal(), MatcherTableModel.Columns.Update.ordinal());
    }

    @Override
    public MatcherTableModel getModel() {
        return model;
    }
}
