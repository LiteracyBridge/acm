package org.literacybridge.acm.gui.assistants.Matcher;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.function.Predicate;

class MatcherTable extends JTable {

    private MatcherTableModel model;
    private TableRowSorter<MatcherTableModel> sorter;
    private MatcherFilter<MatchableImportableAudio> filter;

    private MatcherTable() {
        super();
        
        model = new MatcherTableModel(this);
        setModel(model);
        sorter = new TableRowSorter<>(model);
        filter = new MatcherFilter<>(model);
        sorter.setRowFilter(filter);

        model.addTableModelListener((final TableModelEvent tableModelEvent) -> {
            SwingUtilities.invokeLater(() -> {
                tableModelListener(tableModelEvent);
            });
        });

        setRowSorter(sorter);
        setPreferredScrollableViewportSize(new Dimension(500, 70));
//        setAutoResizeMode(JTable.AUTO_RESIZE_NEXT_COLUMN);
    }

    static MatcherTable factory() {
        return new MatcherTable();
    }

    <T extends MatchableItem<?,?>> void setFilter(Predicate<T> predicate) {
        filter.setPredicate((Predicate) predicate);
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
        int columnCount = this.getColumnModel().getColumnCount();
        if (columnCount < 3) return;
        TableModel model = this.getModel();
        TableCellRenderer headerRenderer = this.getTableHeader().getDefaultRenderer();
        String[] longValues = {"", "Import (F:100) from", "100", ""};
        // All interior columns.
        for (int i = 1; i < columnCount-1; i++) {
            TableColumn column = this.getColumnModel().getColumn(i);

            Component component = headerRenderer.getTableCellRendererComponent(
                null, column.getHeaderValue(),
                false, false, 0, 0);
            int headerWidth = component.getPreferredSize().width;

            component = this.getDefaultRenderer(model.getColumnClass(i)).
                getTableCellRendererComponent(
                    this, longValues[i],
                    false, false, 0, i);
            int cellWidth = component.getPreferredSize().width;

            int w = Math.max(headerWidth, cellWidth) + 2;
            column.setMaxWidth(w+20);
            column.setMinWidth(w);
            column.setPreferredWidth(w);
            column.setWidth(w);
        }
    }

    @Override
    public MatcherTableModel getModel() {
        return model;
    }
}
