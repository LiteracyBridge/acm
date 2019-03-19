package org.literacybridge.acm.gui.assistants.Matcher;

import org.literacybridge.acm.gui.Assistant.AssistantPage;
import org.literacybridge.acm.gui.assistants.Deployment.Issues;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public class MatcherTable extends JTable {

    private MatcherTableModel model;
    private TableRowSorter<MatcherTableModel> sorter;
    private MatcherFilter<MatchableImportableAudio> filter;

    public MatcherTable() {
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
        model.setupSorter(sorter);

        setPreferredScrollableViewportSize(new Dimension(500, 70));
    }

    public <T extends MatchableItem<?,?>> void setFilter(Predicate<T> predicate) {
        filter.setPredicate((Predicate) predicate);
    }

    public void setRenderer(MatcherTableModel.Columns column, TableCellRenderer renderer) {
        TableColumn columnModel = getColumnModel().getColumn(column.ordinal());
        columnModel.setCellRenderer(renderer);
    }


    private void tableModelListener(TableModelEvent tableModelEvent) {
        if (tableModelEvent.getFirstRow() == TableModelEvent.HEADER_ROW) {
            sizeColumns();
        }
    }

    void sizeColumns() {
        Map<Integer, Stream<String>> columnValues = new HashMap<>();
        // Set column 2 width (Status) on header & values.
        Stream<String> values = IntStream.range(0, this.getRowCount()).mapToObj(r->getValueAt(r,2).toString());
        columnValues.put(2, values);

        // Set column 1 width (Update?) on header only.
        values = Stream.empty();
        columnValues.put(1, values);

        AssistantPage.sizeColumns(this, columnValues);
    }

    @Override
    public MatcherTableModel getModel() {
        return model;
    }
}
