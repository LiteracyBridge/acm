package org.literacybridge.acm.gui.assistants.Matcher;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public abstract class AbstractMatchTableModel<TargetType extends Target, MatchableType extends MatchableItem>
    extends AbstractTableModel implements IMatcherTableModel<MatchableType> {

    private int statusColumnNo;
    private int replaceColumnNo;
    private int fileColumnNo;

    private ColumnProvider<TargetType> columnProvider;
    private List<String> columnNames = new ArrayList<>();

    public int getStatusColumnNo() {
        return statusColumnNo;
    }
    public int getReplaceColumnNo() {
        return replaceColumnNo;
    }
    public int getFileColumnNo() { return fileColumnNo; }

    /**
     * A base class for a matcher table.
     * @param columnProvider provides columns to the left, the "target" columns.
     */
    protected AbstractMatchTableModel(ColumnProvider<TargetType> columnProvider) {
        this.columnProvider = columnProvider;

        statusColumnNo = columnProvider.getColumnCount();
        IntStream.range(0, statusColumnNo)
            .forEach(i -> columnNames.add(columnProvider.getColumnName(i)));

        replaceColumnNo = statusColumnNo + 1;
        fileColumnNo = replaceColumnNo + 1;

        String[] NAMES = { "Status", "Replace?", "Audio File" };
        columnNames.addAll(Arrays.asList(NAMES));
    }

    @Override
    public Class<?> getColumnClass(int columnIndex) {
        if (columnIndex < statusColumnNo) {
            return columnProvider.getColumnClass(columnIndex);
        } else if (columnIndex == replaceColumnNo) {
            return Boolean.class;
        }
        return String.class;
    }

    @Override
    public String getColumnName(int columnIndex) {
        return columnNames.get(columnIndex);
    }

    @Override
    public boolean isLeftColumn(int columnIndex) {
        return columnIndex < statusColumnNo;
    }

    @Override
    public boolean isRightColumn(int columnIndex) {
        return columnIndex == fileColumnNo;
    }

    @Override
    public int getColumnCount() {
        return columnNames.size();
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        MatchableType item = getRowAt(rowIndex);

        // One of ours?
        if (columnIndex >= statusColumnNo) {
            columnIndex -= statusColumnNo;
            switch (columnIndex) {
            case 0:
                return item.getOperation();
            case 1:
                return item.getLeft() != null && item.getLeft().isReplaceOk();
            case 2:
                if (item.getMatch() == MATCH.LEFT_ONLY) return "";
                return item.getRight().toString();
            }
            return "???";
        }
        // Anything for the column provider to even get?
        if (item.getMatch() == MATCH.RIGHT_ONLY) return "";
        //noinspection unchecked
        return columnProvider.getValueAt((TargetType) item.getLeft(), columnIndex);
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        // Only the "Replace?" column is editable.
        if (columnIndex != replaceColumnNo) return false;
        MatchableType item = getRowAt(rowIndex);
        if (item == null) return false;
        // If the row is a match AND has an existing audio item, enable the [ ] Replace checkbox.
        return item.getMatch().isMatch() && item.getLeft().targetExists();
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        if (columnIndex != replaceColumnNo) return;
        MatchableType item = getRowAt(rowIndex);
        if (item == null) return;
        // If the row is a match AND has an existing audio item, enable the [ ] Replace checkbox.
        if (!(item.getMatch().isMatch() && item.getLeft().targetExists())) return;
        boolean v = (aValue != null && (Boolean) aValue);
        item.getLeft().setReplaceOk(v);
        super.fireTableRowsUpdated(rowIndex, rowIndex);
    }

}
