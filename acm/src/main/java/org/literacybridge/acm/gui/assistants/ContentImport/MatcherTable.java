package org.literacybridge.acm.gui.assistants.ContentImport;

import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Matcher.MatchableImportableAudio;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
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

    public void setFilter(Predicate<MatchableImportableAudio> predicate) {
        filter.setPredicate(predicate);
    }

    void setRenderer(MatcherTableModel.Columns column, TableCellRenderer renderer) {
        TableColumn columnModel = getColumnModel().getColumn(column.ordinal());
        columnModel.setCellRenderer(renderer);
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
